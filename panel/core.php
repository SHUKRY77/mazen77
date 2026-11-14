<?php
declare(strict_types=1);
require __DIR__ . '/lib.php';

/** Import the legacy panel's MySQL profiles into the secure API store. */
function sync_legacy_mysql_servers(PDO $sqlite): void {
    static $done = false;
    if ($done) return;
    $done = true;
    try {
        $host = getenv('PANEL_DB_HOST') ?: '127.0.0.1';
        $name = getenv('PANEL_DB_NAME') ?: 'vpn_db';
        $user = getenv('PANEL_DB_USER') ?: 'root';
        $pass = getenv('PANEL_DB_PASS') ?: '';
        $key  = getenv('PANEL_DB_ENCRYPT_KEY') ?: '';
        if ($pass === '' || $key === '') return;
        $mysql = new PDO("mysql:host={$host};dbname={$name};charset=utf8mb4", $user, $pass, [
            PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
            PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
        ]);
        $mysql->exec('SET NAMES utf8mb4');
        $decrypt = static function (string $value) use ($key): string {
            if ($value === '') return '';
            $raw = base64_decode($value, true);
            if ($raw !== false && strlen($raw) > 16) {
                $plain = openssl_decrypt(substr($raw, 16), 'aes-256-cbc', $key, OPENSSL_RAW_DATA, substr($raw, 0, 16));
                if ($plain !== false) return (string)$plain;
            }
            return $value;
        };
        $publicV2ray = static function (string $link, string &$secret): string {
            $link = trim($link);
            if (preg_match('~^([a-z][a-z0-9+.-]*)://([^@/?#]+)@~i', $link, $m)) {
                $secret = rawurldecode($m[2]);
                return substr_replace($link, 'AUTH@', strlen($m[1]) + 3, strlen($m[2]) + 1);
            }
            return $link;
        };
        $upsert = $sqlite->prepare("INSERT INTO servers(id,country,subtitle,protocol,host,port,sni,network,path,secret_enc,ssh_user_enc,ssh_pass_enc,public_config,active,created_at,updated_at,carrier,tls,proxy,payload) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT(id) DO UPDATE SET country=excluded.country,subtitle=excluded.subtitle,protocol=excluded.protocol,host=excluded.host,port=excluded.port,sni=excluded.sni,network=excluded.network,path=excluded.path,secret_enc=excluded.secret_enc,ssh_user_enc=excluded.ssh_user_enc,ssh_pass_enc=excluded.ssh_pass_enc,public_config=excluded.public_config,active=1,updated_at=excluded.updated_at,carrier=excluded.carrier,tls=excluded.tls,proxy=excluded.proxy,payload=excluded.payload");
        $now = time();
        $v2Rows = $mysql->query('SELECT id,country,network,config_url,subtitle FROM servers_max ORDER BY id')->fetchAll();
        $sshRows = $mysql->query('SELECT id,network_name,conn_type,ssh_host,subtitle,sni,ssl_port,payload,squid_port,squid_ip,ssh_user,ssh_pass FROM ssh_servers_max ORDER BY id')->fetchAll();
        $legacyIds = [];
        foreach ($v2Rows as $r) $legacyIds[] = 'legacy-v2ray-'.(int)$r['id'];
        foreach ($sshRows as $r) $legacyIds[] = 'legacy-ssh-'.(int)$r['id'];
        // MySQL is the authoritative panel source. Remove every API row that
        // is not represented by the current panel rows, including old rows
        // created before the legacy-* migration.
        if ($legacyIds) {
            $marks = implode(',', array_fill(0, count($legacyIds), '?'));
            $sqlite->prepare("DELETE FROM servers WHERE id NOT IN ($marks)")->execute($legacyIds);
        } else {
            $sqlite->exec('DELETE FROM servers');
        }
        foreach ($v2Rows as $r) {
            $secret = '';
            $link = $publicV2ray($decrypt((string)$r['config_url']), $secret);
            $parts = parse_url($link);
            $scheme = strtolower((string)($parts['scheme'] ?? 'vless'));
            $protocol = in_array($scheme, ['vless','vmess','trojan'], true) ? $scheme : 'vless';
            $hostName = (string)($parts['host'] ?? '');
            $port = (int)($parts['port'] ?? 443);
            $sni = '';
            if (!empty($parts['query'])) parse_str((string)$parts['query'], $q); else $q = [];
            $sni = (string)($q['sni'] ?? $q['host'] ?? $hostName);
            $id = 'legacy-v2ray-'.(int)$r['id'];
            $public = str_replace('AUTH@', rawurlencode('AUTH').'@', $link);
            $upsert->execute([$id,(string)$r['country'],(string)$r['subtitle'],$protocol,$hostName,$port,$sni,'tcp','/',master_encrypt($secret),master_encrypt(''),master_encrypt(''),$public,1,$now,$now,(string)$r['network'],!empty($q['security']) && strtolower((string)$q['security'])==='tls','',(string)($q['path'] ?? '')]);
        }
        foreach ($sshRows as $r) {
            $type = strtoupper((string)$r['conn_type']);
            $protocol = match ($type) {
                'DIRECT' => 'ssh', 'HTTP_PROXY' => 'ssh-proxy', 'HTTP_PAYLOAD' => 'ssh-proxy-payload',
                'TLS_DIRECT' => 'ssh-tls', 'SSL_PROXY' => 'ssh-tls-proxy', 'TLS_PAYLOAD' => 'ssh-tls-payload',
                'TLS_PROXY_PAYLOAD' => 'ssh-tls-proxy-payload', 'WS' => 'ssh-ws', 'WS_PAYLOAD' => 'ssh-ws-payload',
                default => 'ssh-payload',
            };
            $id = 'legacy-ssh-'.(int)$r['id'];
            $port = (int)$r['ssl_port']; if ($port < 1 || $port > 65535) $port = 22;
            $usesProxy = str_contains($type, 'PROXY');
            $usesPayload = str_contains($type, 'PAYLOAD');
            $usesTls = str_contains($type, 'TLS');
            $proxy = $usesProxy ? trim((string)$r['squid_ip']) : ''; $proxyPort = $usesProxy ? (int)$r['squid_port'] : 0;
            if ($proxy !== '' && $proxyPort > 0) $proxy .= ':'.$proxyPort;
            $sni = $usesTls ? (string)$r['sni'] : '';
            $payload = $usesPayload ? $decrypt((string)$r['payload']) : '';
            $public = 'ssh://AUTH@'.rawurlencode((string)$r['ssh_host']).':'.$port.'/?_sid='.$id;
            $upsert->execute([$id,(string)$r['network_name'],(string)$r['subtitle'],$protocol,(string)$r['ssh_host'],$port,$sni,'tcp','/',master_encrypt(''),master_encrypt((string)$r['ssh_user']),master_encrypt($decrypt((string)$r['ssh_pass'])),$public,1,$now,$now,(string)$r['network_name'],str_contains($type,'TLS'),$proxy,$payload]);
        }
    } catch (Throwable $ignored) {
        // The secure SQLite/API source remains available if the legacy DB is offline.
    }
}

$action = (string)($_GET['action'] ?? '');
$requestMethod = (string)($_SERVER['REQUEST_METHOD'] ?? 'GET');
if ($action === '' && $requestMethod === 'GET') {
    header('Location: /admin.php', true, 302);
    exit;
}
$did = trim((string)($_GET['did'] ?? ''));
$ts = (string)($_GET['ts'] ?? '');
$devicePub = (string)($_GET['pk'] ?? '');
$deviceProof = (string)($_GET['dp'] ?? '');
$deviceNonce = (string)($_GET['dn'] ?? '');
if ($did === '' || strlen($did) > 160 || !valid_time($ts)) json_out(['status'=>'error','message'=>'Invalid request'], 400);

if ($action === 'hs') {
    $nonce = strtolower((string)($_GET['n'] ?? ''));
    if (!preg_match('/^[a-f0-9]{32}$/', $nonce) || !verify_device_proof($devicePub, $deviceProof, $did.'||'.$ts.'|hs||'.$nonce)) json_out(['status'=>'error','message'=>'Handshake rejected'], 403);
    $deviceDer = base64_decode($devicePub, true);
    $devicePem = "-----BEGIN PUBLIC KEY-----\n" . chunk_split(base64_encode($deviceDer), 64, "\n") . "-----END PUBLIC KEY-----\n";
    $deviceKey = openssl_pkey_get_public($devicePem);
    $ephemeral = openssl_pkey_new(['private_key_type'=>OPENSSL_KEYTYPE_EC, 'curve_name'=>'prime256v1']);
    if ($deviceKey === false || $ephemeral === false || !openssl_pkey_export($ephemeral, $ephemeralPem)) json_out(['status'=>'error','message'=>'Handshake rejected'], 403);
    $ephemeralDetails = openssl_pkey_get_details($ephemeral);
    $ephemeralDer = base64_decode(preg_replace('/-----BEGIN PUBLIC KEY-----|-----END PUBLIC KEY-----|\s/', '', $ephemeralDetails['key']), true);
    $ephemeralPub = base64_encode($ephemeralDer);
    $shared = openssl_pkey_derive($deviceKey, $ephemeral, 32);
    if ($shared === false) json_out(['status'=>'error','message'=>'Handshake rejected'], 403);
    $sessionKey = hash('sha256', $shared, true);
    $token = random_hex(32); $snonce = random_hex(32); $now = time();
    db()->prepare('DELETE FROM sessions WHERE expires_at<?')->execute([$now]);
    db()->prepare('INSERT INTO sessions(token,did,ip_hash,snonce,device_pub,session_key_enc,expires_at,created_at) VALUES(?,?,?,?,?,?,?,?)')->execute([$token,$did,client_ip_hash(),$snonce,$devicePub,master_encrypt($sessionKey),$now+180,$now]);
    audit('handshake');
    json_out(['status'=>'ok','token'=>$token,'snonce'=>$snonce,'expires_in'=>180,'ephemeral_pub'=>$ephemeralPub,'server_sig'=>server_sign($did.'|'.$token.'|'.$snonce.'|180|'.$devicePub.'|'.$ephemeralPub)]);
}

$token = (string)($_GET['token'] ?? '');
$session = ip_bound_token($token, $did);
if (!$session) json_out(['status'=>'error','message'=>'Session expired'], 401);
if ($devicePub !== '' && !hash_equals((string)$session['device_pub'], $devicePub)) json_out(['status'=>'error','message'=>'Device binding rejected'], 403);
if ($action === 'get_max_app_data') {
    if (!consume_request_nonce($token, $deviceNonce) || !verify_device_proof((string)$session['device_pub'], $deviceProof, $did.'|'.$token.'|'.$ts.'|get_max_app_data||'.$deviceNonce)) json_out(['status'=>'error','message'=>'Signature rejected'], 403);
    $pdo = db();
    sync_legacy_mysql_servers($pdo);
    $now = time();
    $pdo->prepare('DELETE FROM ssh_handles WHERE expires_at<?')->execute([$now]);
    $rows = $pdo->query("SELECT id,country,subtitle,carrier,protocol,host,port,sni,proxy,payload,public_config,ssh_user_enc,udpgw_port,udpgw_enabled FROM servers WHERE active=1 AND lower(host) NOT IN ('193.106.249.79','abusultan.shukry.online') ORDER BY country,id")->fetchAll();
    foreach ($rows as &$row) {
        if (str_starts_with((string)$row['protocol'], 'ssh') || $row['protocol'] === 'dnstt') {
            $type = match ($row['protocol']) {
                'ssh' => 'direct',
                'ssh-proxy' => 'http_proxy',
                'ssh-payload' => 'payload',
                'ssh-proxy-payload' => 'http_payload',
                'ssh-tls' => 'tls_direct',
                'ssh-tls-proxy' => 'ssl_proxy',
                'ssh-tls-payload' => 'tls_payload',
                'ssh-tls-proxy-payload' => 'tls_proxy_payload',
                'ssh-ws' => 'ws',
                'ssh-ws-payload' => 'ws_payload',
                'dnstt' => 'slowdns',
                default => 'payload',
            };
            $proxyHost = '';
            $proxyPort = '80';
            if (preg_match('/^([^:]+):(\d+)$/', (string)$row['proxy'], $pm)) {
                $proxyHost = $pm[1]; $proxyPort = $pm[2];
            }
            // Opaque, short-lived handle: the real server ID never leaves the API response.
            $handle = random_hex(24);
            $pdo->prepare('INSERT INTO ssh_handles(handle,did,server_id,expires_at,created_at) VALUES(?,?,?,?,?)')->execute([$handle,$did,(string)$row['id'],$now+180,$now]);
            $sshUser = master_decrypt((string)$row['ssh_user_enc']);
            $udpPort = max(1, min(65535, (int)($row['udpgw_port'] ?? 7300)));
            $udpEnabled = !empty($row['udpgw_enabled']) ? '1' : '0';
            $row['config'] = 'SSH_PAYLOAD|'.$type.'|'.str_replace('|', '', (string)$row['sni']).'|'.str_replace('|', '', $proxyHost).'|'.$proxyPort.'|'.str_replace('|', '', (string)$row['payload']).'|'.str_replace('|', '', $row['host'].':'.$row['port']).'|'.$handle.'|'.str_replace('|', '', $sshUser).'|'.$udpPort.'|'.$udpEnabled;
        } else {
            // V2Ray links also receive a short-lived credential handle.
            // The old public server ID must not be sent as a creds handle.
            $handle = random_hex(24);
            $pdo->prepare('INSERT INTO ssh_handles(handle,did,server_id,expires_at,created_at) VALUES(?,?,?,?,?)')->execute([$handle,$did,(string)$row['id'],$now+180,$now]);
            $publicConfig = (string)$row['public_config'];
            // Replace any legacy/internal sid, not only the old 32-hex format.
            // The app's creds endpoint accepts only this fresh 48-hex handle.
            $updatedConfig = preg_replace('/([?&])_sid=[^&#]+/i', '$1_sid='.$handle, $publicConfig);
            if ($updatedConfig === null || $updatedConfig === $publicConfig) {
                $fragment = '';
                $base = $publicConfig;
                $hash = strpos($base, '#');
                if ($hash !== false) { $fragment = substr($base, $hash); $base = substr($base, 0, $hash); }
                $updatedConfig = $base . (str_contains($base, '?') ? '&' : '?') . '_sid=' . $handle . $fragment;
            }
            $row['config'] = $updatedConfig;
        }
        unset($row['host'], $row['port'], $row['sni'], $row['proxy'], $row['payload'], $row['public_config'], $row['ssh_user_enc'], $row['udpgw_port'], $row['udpgw_enabled']);
    }
    unset($row);
    $payloads = db()->query('SELECT network,subtitle,config FROM payloads WHERE active=1 ORDER BY network,id')->fetchAll();
    $serverPayloads = [];
    $topRows = [];
    foreach ($rows as $serverRow) {
        // Every panel server must appear in the app's Servers tab.
        // SSH entries may additionally appear in Networks for payload selection.
        $displayRow = $serverRow;
        $protocolLabel = (string)$serverRow['protocol'];
        if ($protocolLabel !== '') {
            $displayRow['subtitle'] = trim((string)$serverRow['subtitle']) . ' · ' . $protocolLabel;
        }
        $topRows[] = $displayRow;
        if (str_starts_with((string)$serverRow['config'], 'SSH_PAYLOAD|')) {
            $serverPayloads[] = [
                'name' => $serverRow['carrier'] !== '' ? $serverRow['carrier'] : $serverRow['country'],
                'info' => $serverRow['subtitle'] !== '' ? $serverRow['subtitle'] : $serverRow['protocol'],
                'config' => $serverRow['config'],
            ];
        } else {
            $networkName = $serverRow['carrier'] !== '' ? $serverRow['carrier'] : $serverRow['country'];
            $serverPayloads[] = [
                'network' => $networkName,
                'subtitle' => $serverRow['subtitle'],
                'config' => $serverRow['config'],
            ];
        }
    }
    $rows = $topRows;
    $payloads = array_merge($payloads, $serverPayloads);
    $rows = array_merge($rows, $payloads);
    // إعدادات النفق غير السرية تُعاد كما هي؛ بيانات الدخول تبقى في action=creds المشفّر.
    json_out(['status'=>'success','data'=>json_encode($rows, JSON_UNESCAPED_SLASHES)]);
}
if ($action === 'creds') {
    $handle = trim((string)($_GET['handle'] ?? ''));
    // The response stays AES-GCM encrypted; only public connection fields are
    // exposed. Password/secret remains protected in the encrypted response.
    if (!preg_match('/^[a-f0-9]{48}$/', $handle)) json_out(['status'=>'error','message'=>'Invalid credential handle'], 403);
    $q = db()->prepare('SELECT s.* FROM ssh_handles h JOIN servers s ON s.id=h.server_id WHERE h.handle=? AND h.did=? AND h.expires_at>=? AND s.active=1'); $q->execute([$handle,$did,time()]); $server=$q->fetch();
    if (!$server) json_out(['status'=>'error','message'=>'Server unavailable'], 404);
    $secret = master_decrypt($server['secret_enc']); $user = master_decrypt($server['ssh_user_enc']); $pass = master_decrypt($server['ssh_pass_enc']);
    $payload = json_encode(['secret'=>$secret,'user'=>$user,'pass'=>$pass], JSON_UNESCAPED_SLASHES);
    json_out(['status'=>'success','data'=>encrypt_response($payload, session_key($session))]);
}
json_out(['status'=>'error','message'=>'Unknown action'], 404);
