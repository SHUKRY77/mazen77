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

try {
    $pdo = db();
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
