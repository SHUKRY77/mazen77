<?php
declare(strict_types=1);
require __DIR__ . '/lib.php';

$action = (string)($_GET['action'] ?? '');
$did = trim((string)($_GET['did'] ?? ''));
$ts = (string)($_GET['ts'] ?? '');
$sig = strtolower((string)($_GET['sig'] ?? ''));
if ($did === '' || strlen($did) > 160 || !valid_time($ts)) json_out(['status'=>'error','message'=>'Invalid request'], 400);

if ($action === 'hs') {
    $nonce = strtolower((string)($_GET['n'] ?? ''));
    if (!preg_match('/^[a-f0-9]{32}$/', $nonce) || !timing_safe(hmac_for($did.'|'.$ts.'|'.$nonce), $sig)) json_out(['status'=>'error','message'=>'Handshake rejected'], 403);
    $token = random_hex(32); $snonce = random_hex(32); $now = time();
    db()->prepare('DELETE FROM sessions WHERE expires_at<?')->execute([$now]);
    db()->prepare('INSERT INTO sessions(token,did,ip_hash,snonce,expires_at,created_at) VALUES(?,?,?,?,?,?)')->execute([$token,$did,client_ip_hash(),$snonce,$now+300,$now]);
    audit('handshake');
    json_out(['status'=>'ok','token'=>$token,'snonce'=>$snonce,'expires_in'=>300]);
}

$token = (string)($_GET['token'] ?? '');
$session = ip_bound_token($token, $did);
if (!$session) json_out(['status'=>'error','message'=>'Session expired'], 401);
if ($action === 'get_max_app_data') {
    if (!timing_safe(hmac_for($did.'|'.$token.'|'.$ts), $sig)) json_out(['status'=>'error','message'=>'Signature rejected'], 403);
    $rows = db()->query('SELECT id,country,subtitle,carrier,protocol,host,port,sni,proxy,payload,public_config FROM servers WHERE active=1 ORDER BY country,id')->fetchAll();
    foreach ($rows as &$row) {
        if (str_starts_with((string)$row['protocol'], 'ssh') || $row['protocol'] === 'dnstt') {
            $type = match ($row['protocol']) {
                'ssh' => 'direct',
                'ssh-proxy' => 'http_proxy',
                'ssh-tls' => 'tls_direct',
                'ssh-tls-proxy' => 'ssl_proxy',
                'dnstt' => 'slowdns',
                default => 'ssl_payload',
            };
            $proxyHost = '';
            $proxyPort = '80';
            if (preg_match('/^([^:]+):(\d+)$/', (string)$row['proxy'], $pm)) {
                $proxyHost = $pm[1]; $proxyPort = $pm[2];
            }
            $row['config'] = 'SSH_PAYLOAD|'.$type.'|'.str_replace('|', '', (string)$row['sni']).'|'.str_replace('|', '', $proxyHost).'|'.$proxyPort.'|'.str_replace('|', '', (string)$row['payload']).'|'.str_replace('|', '', $row['host'].':'.$row['port']).'|'.$row['id'];
        } else {
            $row['config'] = $row['public_config'];
        }
        unset($row['host'], $row['port'], $row['sni'], $row['proxy'], $row['payload'], $row['public_config']);
    }
    unset($row);
    $payloads = db()->query('SELECT network,subtitle,config FROM payloads WHERE active=1 ORDER BY network,id')->fetchAll();
    $serverPayloads = [];
    foreach ($rows as $serverRow) {
        $serverPayloads[] = [
            'network' => $serverRow['carrier'] !== '' ? $serverRow['carrier'] : $serverRow['country'],
            'subtitle' => $serverRow['protocol'],
            'config' => $serverRow['config'],
        ];
        unset($serverRow['carrier'], $serverRow['protocol']);
    }
    foreach ($rows as &$row) { unset($row['carrier'], $row['protocol']); }
    unset($row);
    $payloads = array_merge($payloads, $serverPayloads);
    $rows = array_merge($rows, $payloads);
    json_out(['status'=>'success','data'=>encrypt_response(json_encode($rows, JSON_UNESCAPED_SLASHES), session_key($session['snonce']))]);
}
if ($action === 'creds') {
    $sid = trim((string)($_GET['sid'] ?? ''));
    if (!preg_match('/^[a-f0-9]{32}$/', $sid) || !timing_safe(hmac_for($did.'|'.$token.'|'.$ts.'|'.$sid), $sig)) json_out(['status'=>'error','message'=>'Signature rejected'], 403);
    $q = db()->prepare('SELECT * FROM servers WHERE id=? AND active=1'); $q->execute([$sid]); $server=$q->fetch();
    if (!$server) json_out(['status'=>'error','message'=>'Server unavailable'], 404);
    $secret = master_decrypt($server['secret_enc']); $user = master_decrypt($server['ssh_user_enc']); $pass = master_decrypt($server['ssh_pass_enc']);
    $payload = json_encode(['secret'=>$secret,'user'=>$user,'pass'=>$pass], JSON_UNESCAPED_SLASHES);
    json_out(['status'=>'success','data'=>encrypt_response($payload, session_key($session['snonce']))]);
}
json_out(['status'=>'error','message'=>'Unknown action'], 404);
