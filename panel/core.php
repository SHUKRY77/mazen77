<?php
declare(strict_types=1);
require __DIR__ . '/lib.php';

$action = (string)($_GET['action'] ?? '');
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
    $now = time();
    $pdo->prepare('DELETE FROM ssh_handles WHERE expires_at<?')->execute([$now]);
    $rows = $pdo->query("SELECT id,country,subtitle,carrier,protocol,host,port,sni,proxy,payload,public_config FROM servers WHERE active=1 AND lower(host) NOT IN ('193.106.249.79','abusultan.shukry.online') ORDER BY country,id")->fetchAll();
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
            $row['config'] = 'SSH_PAYLOAD|'.$type.'|'.str_replace('|', '', (string)$row['sni']).'|'.str_replace('|', '', $proxyHost).'|'.$proxyPort.'|'.str_replace('|', '', (string)$row['payload']).'|'.str_replace('|', '', $row['host'].':'.$row['port']).'|'.$handle;
        } else {
            $row['config'] = $row['public_config'];
        }
        unset($row['host'], $row['port'], $row['sni'], $row['proxy'], $row['payload'], $row['public_config']);
    }
    unset($row);
    $payloads = db()->query('SELECT network,subtitle,config FROM payloads WHERE active=1 ORDER BY network,id')->fetchAll();
    $serverPayloads = [];
    $topRows = [];
    foreach ($rows as $serverRow) {
        if (str_starts_with((string)$serverRow['config'], 'SSH_PAYLOAD|')) {
            $serverPayloads[] = [
                'name' => $serverRow['carrier'] !== '' ? $serverRow['carrier'] : $serverRow['country'],
                'info' => $serverRow['subtitle'] !== '' ? $serverRow['subtitle'] : $serverRow['protocol'],
                'config' => $serverRow['config'],
            ];
        } else {
            $networkName = $serverRow['carrier'] !== '' ? $serverRow['carrier'] : $serverRow['country'];
            unset($serverRow['carrier'], $serverRow['protocol']);
            $topRows[] = $serverRow;
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
    if (!preg_match('/^[a-f0-9]{48}$/', $handle) || !consume_request_nonce($token, $deviceNonce) || !verify_device_proof((string)$session['device_pub'], $deviceProof, $did.'|'.$token.'|'.$ts.'|creds|'.$handle.'|'.$deviceNonce)) json_out(['status'=>'error','message'=>'Signature rejected'], 403);
    $q = db()->prepare('SELECT s.* FROM ssh_handles h JOIN servers s ON s.id=h.server_id WHERE h.handle=? AND h.did=? AND h.expires_at>=? AND s.active=1'); $q->execute([$handle,$did,time()]); $server=$q->fetch();
    if (!$server) json_out(['status'=>'error','message'=>'Server unavailable'], 404);
    $secret = master_decrypt($server['secret_enc']); $user = master_decrypt($server['ssh_user_enc']); $pass = master_decrypt($server['ssh_pass_enc']);
    $payload = json_encode(['secret'=>$secret,'user'=>$user,'pass'=>$pass], JSON_UNESCAPED_SLASHES);
    json_out(['status'=>'success','data'=>encrypt_response($payload, session_key($session))]);
}
json_out(['status'=>'error','message'=>'Unknown action'], 404);
