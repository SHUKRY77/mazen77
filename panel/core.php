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
    $rows = db()->query('SELECT id,country,subtitle,public_config AS config FROM servers WHERE active=1 ORDER BY country,id')->fetchAll();
    $payloads = db()->query('SELECT network,subtitle,config FROM payloads WHERE active=1 ORDER BY network,id')->fetchAll();
    $serverPayloads = db()->query("SELECT COALESCE(NULLIF(carrier,''), country) AS network, protocol AS subtitle, payload AS config FROM servers WHERE active=1 ORDER BY network,country,id")->fetchAll();
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
