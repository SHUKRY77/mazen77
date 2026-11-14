<?php
declare(strict_types=1);
/**
 * Compatibility feed for the new OpenVPN/Injector source.
 * It returns the same AES-CBC "google" encrypted Servers.js format
 * used by the application source, while reading the current panel SQLite.
 */
require __DIR__ . '/lib.php';

function app_encrypt(string $plain): string {
    $key = hash('sha256', 'google', true);
    $iv = str_repeat("\0", 16);
    $cipher = openssl_encrypt($plain, 'aes-256-cbc', $key, OPENSSL_RAW_DATA, $iv);
    if ($cipher === false) {
        http_response_code(500);
        exit('Encryption failed');
    }
    return base64_encode($cipher);
}

function enc_field(string $value): string { return app_encrypt($value); }
function safe_port(mixed $value, int $fallback): string {
    $port = (int)$value;
    return ($port >= 1 && $port <= 65535) ? (string)$port : (string)$fallback;
}
function protocol_mode(string $protocol): int {
    return match ($protocol) {
        'ssh' => 0,
        'ssh-payload', 'ssh-ws', 'ssh-ws-payload' => 1,
        'ssh-proxy', 'ssh-proxy-payload' => 2,
        'ssh-tls' => 3,
        'ssh-tls-payload' => 4,
        'ssh-tls-proxy', 'ssh-tls-proxy-payload' => 5,
        default => 0,
    };
}

/** Keep the compatibility feed aligned with the old panel's MySQL source. */
function sync_legacy_ssh_feed(PDO $sqlite): void {
    $pass = getenv('PANEL_DB_PASS') ?: '';
    $key = getenv('PANEL_DB_ENCRYPT_KEY') ?: '';
    if ($pass === '' || $key === '') return;
    $mysql = new PDO(
        'mysql:host='.(getenv('PANEL_DB_HOST') ?: '127.0.0.1').';dbname='.(getenv('PANEL_DB_NAME') ?: 'vpn_db').';charset=utf8mb4',
        getenv('PANEL_DB_USER') ?: 'root',
        $pass,
        [PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION, PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC]
    );
    $decrypt = static function (string $value) use ($key): string {
        if ($value === '') return '';
        $raw = base64_decode($value, true);
        if ($raw !== false && strlen($raw) > 16) {
            $plain = openssl_decrypt(substr($raw, 16), 'aes-256-cbc', $key, OPENSSL_RAW_DATA, substr($raw, 0, 16));
            if ($plain !== false) return (string)$plain;
        }
        return $value;
    };
    $rows = $mysql->query('SELECT id,network_name,conn_type,ssh_host,subtitle,sni,ssl_port,payload,squid_port,squid_ip,ssh_user,ssh_pass FROM ssh_servers_max ORDER BY id')->fetchAll();
    $sqlite->exec("DELETE FROM servers WHERE id LIKE 'legacy-ssh-%'");
    $upsert = $sqlite->prepare("INSERT INTO servers(id,country,subtitle,protocol,host,port,sni,network,path,secret_enc,ssh_user_enc,ssh_pass_enc,public_config,active,created_at,updated_at,carrier,tls,proxy,payload) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT(id) DO UPDATE SET country=excluded.country,subtitle=excluded.subtitle,protocol=excluded.protocol,host=excluded.host,port=excluded.port,sni=excluded.sni,ssh_user_enc=excluded.ssh_user_enc,ssh_pass_enc=excluded.ssh_pass_enc,public_config=excluded.public_config,active=1,updated_at=excluded.updated_at,carrier=excluded.carrier,tls=excluded.tls,proxy=excluded.proxy,payload=excluded.payload");
    $now = time();
    foreach ($rows as $r) {
        $type = strtoupper((string)$r['conn_type']);
        $protocol = match ($type) {
            'DIRECT' => 'ssh', 'HTTP_PROXY' => 'ssh-proxy', 'HTTP_PAYLOAD' => 'ssh-proxy-payload',
            'TLS_DIRECT' => 'ssh-tls', 'SSL_PROXY' => 'ssh-tls-proxy', 'TLS_PAYLOAD' => 'ssh-tls-payload',
            'TLS_PROXY_PAYLOAD' => 'ssh-tls-proxy-payload', 'WS' => 'ssh-ws', 'WS_PAYLOAD' => 'ssh-ws-payload',
            default => 'ssh-payload',
        };
        $usesProxy = str_contains($type, 'PROXY');
        $usesPayload = str_contains($type, 'PAYLOAD');
        $usesTls = str_contains($type, 'TLS');
        $proxy = $usesProxy ? trim((string)$r['squid_ip']) : '';
        $proxyPort = $usesProxy ? (int)$r['squid_port'] : 0;
        if ($proxy !== '' && $proxyPort > 0) $proxy .= ':'.$proxyPort;
        $port = (int)$r['ssl_port']; if ($port < 1 || $port > 65535) $port = 22;
        $id = 'legacy-ssh-'.(int)$r['id'];
        $upsert->execute([$id,(string)$r['network_name'],(string)$r['subtitle'],$protocol,(string)$r['ssh_host'],$port,$usesTls ? (string)$r['sni'] : '','tcp','/',master_encrypt(''),master_encrypt((string)$r['ssh_user']),master_encrypt($decrypt((string)$r['ssh_pass'])),'ssh://AUTH@'.rawurlencode((string)$r['ssh_host']).':'.$port.'/?_sid='.$id,1,$now,$now,(string)$r['network_name'],$usesTls,$proxy,$usesPayload ? $decrypt((string)$r['payload']) : '']);
    }
}

try {
    $pdo = db();
    sync_legacy_ssh_feed($pdo);
    $rows = $pdo->query("SELECT * FROM servers WHERE active=1 ORDER BY updated_at DESC, id ASC")->fetchAll();
    $servers = [];
    $networks = [];
    $sslNetworks = [];

    foreach ($rows as $row) {
        $protocol = strtolower((string)($row['protocol'] ?? 'ssh'));
        if (!str_starts_with($protocol, 'ssh')) continue;
        $host = trim((string)($row['host'] ?? ''));
        if ($host === '') continue;
        $port = safe_port($row['port'] ?? 22, 22);
        $name = trim((string)($row['country'] ?? 'Server'));
        $subtitle = trim((string)($row['subtitle'] ?? ''));
        $usesProxy = str_contains($protocol, 'proxy');
        $usesPayload = str_contains($protocol, 'payload') || str_contains($protocol, 'ws-payload');
        $usesTls = str_contains($protocol, 'tls');
        // TLS Direct is SNI-only in the Android engine; never reuse a stale proxy.
        $proxy = $usesProxy ? trim((string)($row['proxy'] ?? '')) : '';
        $proxyHost = $proxy;
        $proxyPort = '8080';
        if (preg_match('/^([^:]+):(\d+)$/', $proxy, $m)) {
            $proxyHost = $m[1];
            $proxyPort = safe_port($m[2], 8080);
        }
        $sni = $usesTls ? trim((string)($row['sni'] ?? '')) : '';
        $payload = $usesPayload ? (string)($row['payload'] ?? '') : '';
        $mode = protocol_mode($protocol);
        $serverIndex = count($servers) + 1;
        $servers[] = [
            'Name' => $name . ($subtitle !== '' ? ' - ' . $subtitle : ''),
            'Flag' => 'random.png',
            'ServerIPHost' => $host,
            'ServerCloudFrontHost' => $proxyHost,
            'ServerHTTPHost' => $proxyHost,
            'Category' => 'PREMIUM',
            'OpenVPNTCPPort' => $port,
            'OpenVPNSSLPort' => $port,
        ];

        $network = [
            'Name' => $name . ($subtitle !== '' ? ' - ' . $subtitle : ''),
            'Payload' => enc_field($payload),
            'Info' => $subtitle,
            'TunnelType' => $mode,
            'FrontQuery' => '',
            'BackQuery' => '',
            'Priority' => 0,
            'ServerHostDNS' => $proxy !== '' ? 'http' : 'cf',
            'ProxySettings' => [
                'Squid' => enc_field($proxyHost),
                'Port' => $proxyPort,
            ],
        ];
        if ($mode >= 3) {
            $sslNetworks[] = [
                'Name' => $name . ($subtitle !== '' ? ' - ' . $subtitle : ''),
                'SNIHost' => enc_field($sni !== '' ? $sni : $host),
                'Payload' => enc_field($payload),
                'Info' => $subtitle,
                'TunnelType' => $mode,
                'Priority' => 0,
                'CustomSSLPort' => $port,
                'ServerHostDNS' => $proxy !== '' ? 'http' : 'cf',
                'ProxySettings' => [
                    'Squid' => enc_field($proxyHost),
                    'Port' => $proxyPort,
                ],
            ];
        } else {
            $networks[] = $network;
        }
    }

    $document = json_encode([
        'Version' => date('Y.m.d.His'),
        'Servers' => $servers,
        'Networks' => $networks,
        'SSLNetworks' => $sslNetworks,
    ], JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
    if ($document === false) throw new RuntimeException('JSON encoding failed');
    header('Content-Type: text/plain; charset=UTF-8');
    header('Cache-Control: no-store, no-cache, must-revalidate, max-age=0');
    header('Pragma: no-cache');
    echo app_encrypt($document);
} catch (Throwable $e) {
    http_response_code(500);
    header('Content-Type: text/plain; charset=UTF-8');
    echo 'Server feed error';
}
