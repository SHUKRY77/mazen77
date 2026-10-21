<?php
declare(strict_types=1);
session_set_cookie_params(['httponly'=>true, 'secure'=>true, 'samesite'=>'Strict']);
session_start();
require __DIR__ . '/lib.php';

db()->exec('CREATE TABLE IF NOT EXISTS admin_meta (name TEXT PRIMARY KEY, value TEXT NOT NULL)');
$stored = db()->query("SELECT value FROM admin_meta WHERE name='password_hash'")->fetchColumn();
if (!$stored) {
    $stored = password_hash(cfg('ADMIN_PASSWORD'), PASSWORD_ARGON2ID);
    db()->prepare('INSERT INTO admin_meta(name,value) VALUES(?,?)')->execute(['password_hash', $stored]);
}
if (isset($_GET['logout'])) { session_destroy(); header('Location: /admin.php'); exit; }
if (empty($_SESSION['admin'])) {
    $error = '';
    if ($_SERVER['REQUEST_METHOD'] === 'POST' && ($_POST['action'] ?? '') === 'login') {
        if (hash_equals(cfg('ADMIN_USERNAME'), (string)($_POST['username'] ?? '')) && password_verify((string)($_POST['password'] ?? ''), (string)$stored)) {
            session_regenerate_id(true); $_SESSION['admin'] = true; header('Location: /admin.php'); exit;
        }
        $error = 'بيانات الدخول غير صحيحة';
    }
    ?><!doctype html><html lang="ar" dir="rtl"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Max Tunnel Admin</title>
    <style>body{font-family:Arial;background:#0b1220;color:#fff;max-width:430px;margin:12vh auto;padding:24px}input,button{box-sizing:border-box;width:100%;padding:13px;margin:8px 0;border-radius:9px;border:0}button{background:#2563eb;color:white;font-weight:bold}.err{color:#fca5a5}</style>
    <h2>Max Tunnel — تسجيل الدخول</h2><?php if ($error) echo '<p class="err">'.h($error).'</p>'; ?><form method="post"><input type="hidden" name="action" value="login"><input type="text" name="username" placeholder="اسم المستخدم" required autofocus><input type="password" name="password" placeholder="كلمة المرور" required><button>دخول آمن</button></form></html><?php exit;
}

function postv(string $key, string $default = ''): string { return trim((string)($_POST[$key] ?? $default)); }
function checked(string $key): int { return isset($_POST[$key]) ? 1 : 0; }
function normalize_v2ray_link(string $link, string &$secret): string {
    $link = trim($link);
    if (stripos($link, '://AUTH@') !== false) return $link;
    if (preg_match('~^([a-z][a-z0-9+.-]*)://([^@/?#]+)@~i', $link, $m)) {
        if ($secret === '') $secret = rawurldecode($m[2]);
        return substr_replace($link, 'AUTH@', strlen($m[1]) + 3, strlen($m[2]) + 1);
    }
    if (stripos($link, 'vmess://') === 0) {
        $encoded = substr($link, 8);
        $decoded = base64_decode(strtr($encoded, '-_', '+/'), true);
        if ($decoded !== false) {
            $data = json_decode($decoded, true);
            if (is_array($data)) {
                if ($secret === '') $secret = (string)($data['id'] ?? $data['password'] ?? '');
                $data['id'] = 'AUTH';
                unset($data['password']);
                return 'vmess://'.rtrim(strtr(base64_encode(json_encode($data, JSON_UNESCAPED_SLASHES)), '+/', '-_'), '=');
            }
        }
    }
    return $link;
}
function attach_server_sid(string $link, string $id): string {
    if (preg_match('/([?&])_sid=[a-f0-9]{32}/i', $link)) return $link;
    $fragment = '';
    $hashPos = strpos($link, '#');
    if ($hashPos !== false) { $fragment = substr($link, $hashPos); $link = substr($link, 0, $hashPos); }
    $link .= (str_contains($link, '?') ? '&' : '?') . '_sid=' . rawurlencode($id);
    return $link . $fragment;
}
$csrf = csrf_token();
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    check_csrf();
    $action = (string)($_POST['action'] ?? '');
    if ($action === 'delete_server') { $dq=db()->prepare('SELECT protocol FROM servers WHERE id=?'); $dq->execute([postv('id')]); $dp=(string)$dq->fetchColumn(); db()->prepare('DELETE FROM servers WHERE id=?')->execute([postv('id')]); audit('server_delete'); header('Location: /admin.php?tab='.(str_starts_with($dp,'ssh') || $dp==='dnstt' ? 'ssh' : 'v2ray')); exit; }
    if ($action === 'delete_payload') { db()->prepare('DELETE FROM payloads WHERE id=?')->execute([postv('id')]); audit('payload_delete'); header('Location: /admin.php?tab=payloads'); exit; }
    if ($action === 'save_payload') {
        $id = preg_match('/^[a-f0-9]{32}$/', postv('id')) ? postv('id') : random_hex(16);
        $network = postv('network'); $config = postv('config');
        if ($network === '' || $config === '') exit('اسم الشبكة وPayload مطلوبان.');
        db()->prepare('INSERT INTO payloads(id,network,subtitle,config,active,created_at,updated_at) VALUES(?,?,?,?,?,?,?) ON CONFLICT(id) DO UPDATE SET network=excluded.network,subtitle=excluded.subtitle,config=excluded.config,active=excluded.active,updated_at=excluded.updated_at')->execute([$id,$network,postv('subtitle','Payload'),$config,checked('active'),time(),time()]);
        audit('payload_save'); header('Location: /admin.php?tab=payloads'); exit;
    }
    if ($action === 'save_server') {
        $id = preg_match('/^[a-f0-9]{32}$/', postv('id')) ? postv('id') : random_hex(16);
        $isSshPost = str_starts_with(postv('protocol'), 'ssh') || postv('protocol') === 'dnstt';
        $secret = postv('secret');
        $public = str_replace('{ID}', $id, postv('public_config'));
        if (!$isSshPost) {
            $public = normalize_v2ray_link($public, $secret);
            $public = attach_server_sid($public, $id);
        }
        if ($isSshPost && $public === '') $public = 'ssh://AUTH@'.postv('host').':'.(int)postv('port','22').'/?_sid='.$id;
        if ($public === '' || stripos($public, 'AUTH') === false || preg_match('/(uuid|password|passwd)=/i', $public)) exit('الرابط العام يجب أن يحتوي AUTH ولا يحتوي كلمة المرور.');
        $values = [
            $id, postv('country','Server'), postv('subtitle','Secure server'), postv('protocol','vless'), postv('host'), (int)postv('port','443'), postv('sni'), postv('network','tcp'), postv('path','/'),
            master_encrypt($secret), master_encrypt(postv('ssh_user')), master_encrypt(postv('ssh_pass')), $public, checked('active'), time(), time(),
            postv('target_app','Max Tunnel'), postv('carrier'), checked('tls'), postv('tls_version','default'), postv('fingerprint','chrome'), postv('alpn'), postv('proxy'), postv('payload'), postv('service_name'), postv('reality_public_key'), postv('reality_short_id'), postv('reality_dest')
        ];
        db()->prepare('INSERT INTO servers(id,country,subtitle,protocol,host,port,sni,network,path,secret_enc,ssh_user_enc,ssh_pass_enc,public_config,active,created_at,updated_at,target_app,carrier,tls,tls_version,fingerprint,alpn,proxy,payload,service_name,reality_public_key,reality_short_id,reality_dest) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT(id) DO UPDATE SET country=excluded.country,subtitle=excluded.subtitle,protocol=excluded.protocol,host=excluded.host,port=excluded.port,sni=excluded.sni,network=excluded.network,path=excluded.path,secret_enc=excluded.secret_enc,ssh_user_enc=excluded.ssh_user_enc,ssh_pass_enc=excluded.ssh_pass_enc,public_config=excluded.public_config,active=excluded.active,updated_at=excluded.updated_at,target_app=excluded.target_app,carrier=excluded.carrier,tls=excluded.tls,tls_version=excluded.tls_version,fingerprint=excluded.fingerprint,alpn=excluded.alpn,proxy=excluded.proxy,payload=excluded.payload,service_name=excluded.service_name,reality_public_key=excluded.reality_public_key,reality_short_id=excluded.reality_short_id,reality_dest=excluded.reality_dest')->execute($values);
        audit('server_save'); header('Location: /admin.php?tab='.($isSshPost ? 'ssh' : 'v2ray')); exit;
    }
}

$tab = (string)($_GET['tab'] ?? 'dashboard');
$mode = $tab === 'ssh' ? 'ssh' : 'v2ray';
$editServer = null; if (!empty($_GET['edit_server'])) { $q=db()->prepare('SELECT * FROM servers WHERE id=?'); $q->execute([$_GET['edit_server']]); $editServer=$q->fetch() ?: null; }
$editPayload = null; if (!empty($_GET['edit_payload'])) { $q=db()->prepare('SELECT * FROM payloads WHERE id=?'); $q->execute([$_GET['edit_payload']]); $editPayload=$q->fetch() ?: null; $tab='payloads'; }
$serverWhere = $mode === 'ssh' ? "WHERE protocol LIKE 'ssh%' OR protocol='dnstt'" : "WHERE protocol NOT LIKE 'ssh%' AND protocol!='dnstt'";
$servers = db()->query('SELECT id,country,subtitle,protocol,host,port,network,tls,active,updated_at FROM servers '.$serverWhere.' ORDER BY updated_at DESC')->fetchAll();
$payloads = db()->query('SELECT id,network,subtitle,active,updated_at FROM payloads ORDER BY updated_at DESC')->fetchAll();
$online = (int)db()->query('SELECT COUNT(DISTINCT did) FROM sessions WHERE expires_at>= '.time())->fetchColumn();
$esc = fn($v) => h((string)$v);
$sv = fn($k,$d='') => $esc($editServer[$k] ?? $d);
$pv = fn($k,$d='') => $esc($editPayload[$k] ?? $d);
?><!doctype html><html lang="ar" dir="rtl"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Max Tunnel Admin</title>
<style>
:root{--bg:#f3f6fb;--nav:#101827;--blue:#2563eb;--muted:#64748b}*{box-sizing:border-box}body{margin:0;font-family:Arial;background:var(--bg);color:#152033}.top{background:var(--nav);color:#fff;padding:15px 22px;display:flex;justify-content:space-between;align-items:center}.top a{color:#fff;text-decoration:none}.layout{display:flex;min-height:calc(100vh - 58px)}aside{width:230px;background:#172235;color:#dbeafe;padding:18px}aside a{display:block;color:#dbeafe;text-decoration:none;padding:12px;border-radius:8px;margin:4px 0}aside a:hover,aside a.active{background:#263b5b;color:#fff}.main{flex:1;padding:22px;max-width:1400px}.cards{display:grid;grid-template-columns:repeat(3,1fr);gap:15px}.card,.stat{background:#fff;border-radius:14px;padding:18px;margin:0 0 18px;box-shadow:0 3px 14px #0f172a12}.stat strong{font-size:28px;display:block;color:var(--blue);margin-top:8px}h1{margin-top:0}h2{margin-top:0}.grid{display:grid;grid-template-columns:repeat(2,1fr);gap:12px}.grid .full{grid-column:1/-1}label{display:block;font-weight:bold;font-size:13px;color:#334155;margin-top:5px}input,select,textarea{width:100%;padding:10px;border:1px solid #cbd5e1;border-radius:8px;margin:5px 0 10px;background:#fff}textarea{min-height:80px;direction:ltr}button,.btn{border:0;border-radius:8px;background:var(--blue);color:#fff;padding:10px 15px;cursor:pointer;text-decoration:none;display:inline-block}.danger{background:#dc2626}.muted{color:var(--muted);font-size:13px}.tablewrap{overflow:auto}table{width:100%;border-collapse:collapse;background:#fff}th,td{padding:10px;border-bottom:1px solid #e2e8f0;text-align:right;white-space:nowrap}.pill{background:#dbeafe;color:#1d4ed8;padding:4px 8px;border-radius:20px;font-size:12px}.check{display:flex;gap:8px;align-items:center;margin:5px 0 12px}.check input{width:auto}.section{border-top:1px solid #e2e8f0;margin-top:12px;padding-top:12px}.actions{display:flex;gap:8px;flex-wrap:wrap}@media(max-width:800px){aside{width:58px;padding:8px}aside a{font-size:0;text-align:center}aside a:before{content:'•';font-size:25px}.grid,.cards{grid-template-columns:1fr}.main{padding:12px}}
</style></head><body data-mode="<?= $mode ?>"><header class="top"><b>Max Tunnel</b><a href="?logout=1">تسجيل الخروج</a></header><div class="layout"><aside>
<a class="<?= $tab==='dashboard'?'active':'' ?>" href="/admin.php">لوحة القيادة</a><a class="<?= $tab==='v2ray'?'active':'' ?>" href="?tab=v2ray">V2Ray / Xray Servers</a><a class="<?= $tab==='ssh'?'active':'' ?>" href="?tab=ssh">SSH Payloads</a><a href="?tab=v2ray#form">إضافة سيرفر V2Ray</a><a href="?tab=ssh#form">إضافة SSH</a><a href="?tab=monitor">المراقب المباشر</a></aside><main class="main">
<?php if ($tab==='dashboard'): ?><h1>لوحة القيادة</h1><div class="cards"><div class="stat">إجمالي السيرفرات<strong><?=count($servers)?></strong></div><div class="stat">إجمالي Payloads<strong><?=count($payloads)?></strong></div><div class="stat">جلسات التطبيق الحالية<strong><?=$online?></strong></div></div><div class="card"><h2>إجراءات سريعة</h2><div class="actions"><a class="btn" href="?tab=v2ray#form">إضافة سيرفر V2Ray / SSH</a><a class="btn" href="?tab=ssh#form">إضافة SSH Payload</a></div><p class="muted">اختر WebSocket أو gRPC أو HTTP/2 أو TCP من نموذج السيرفر، ثم احفظ الرابط العام مع AUTH بدل الأسرار.</p></div>
<?php elseif ($tab==='monitor'): ?><h1>المراقب المباشر</h1><div class="card"><p>الجلسات النشطة الآن: <b><?=$online?></b></p><p class="muted">يتم تحديث الجلسات تلقائيًا عند مصافحة التطبيق، ولا تُعرض أسرار السيرفرات في هذه الصفحة.</p></div>
<?php elseif ($tab==='payloads'): ?><h1>SSH Payloads</h1><div class="card" id="form"><h2><?=$editPayload?'تعديل Payload':'إضافة Payload جديد'?></h2><form method="post"><input type="hidden" name="csrf" value="<?=$csrf?>"><input type="hidden" name="action" value="save_payload"><input type="hidden" name="id" value="<?=$pv('id')?>"><div class="grid"><div><label>اسم الشبكة</label><input name="network" required value="<?=$pv('network')?>"></div><div><label>الوصف</label><input name="subtitle" value="<?=$pv('subtitle','SSH Payload')?>"></div><div class="full"><label>Payload / Injector</label><textarea name="config" required placeholder="اكتب Payload المصرح به هنا، ويمكن استخدام [method] و[host] و[protocol]"><?=$pv('config')?></textarea></div></div><label class="check"><input type="checkbox" name="active" value="1" <?=(!$editPayload||$editPayload['active'])?'checked':''?>> فعال</label><button>حفظ Payload</button></form></div><div class="card"><h2>Payloads المحفوظة</h2><div class="tablewrap"><table><tr><th>الشبكة</th><th>الوصف</th><th>الحالة</th><th>إجراء</th></tr><?php foreach($payloads as $p):?><tr><td><?=$esc($p['network'])?></td><td><?=$esc($p['subtitle'])?></td><td><span class="pill"><?=$p['active']?'فعال':'متوقف'?></span></td><td><a class="btn" href="?tab=payloads&edit_payload=<?=$esc($p['id'])?>#form">تعديل</a> <form style="display:inline" method="post"><input type="hidden" name="csrf" value="<?=$csrf?>"><input type="hidden" name="action" value="delete_payload"><input type="hidden" name="id" value="<?=$esc($p['id'])?>"><button class="danger">حذف</button></form></td></tr><?php endforeach;?></table></div></div>
<?php else: ?>
<h1><?= $mode==='ssh' ? 'SSH Payloads' : 'V2Ray / Xray Servers' ?></h1>
<div class="card" id="form">
<h2><?=$editServer?'تعديل الإعداد':'ADD PROTOCOL '.($mode==='ssh'?'SSH':'V2RAY')?></h2>
<form method="post">
<input type="hidden" name="csrf" value="<?=$csrf?>"><input type="hidden" name="action" value="save_server"><input type="hidden" name="id" value="<?=$sv('id')?>">
<div class="grid">
<div><label>التطبيق المستهدف</label><select name="target_app"><option>Max Tunnel</option><option>Vortex Pro</option></select></div>
<?php if ($mode==='ssh'): ?>
<div><label>Protocol</label><select name="protocol"><option value="ssh">SSH-Direct</option><option value="ssh-proxy">SSH-Proxy</option><option value="ssh-payload">SSH-Payload</option><option value="ssh-proxy-payload">SSH-Proxy-Payload</option><option value="ssh-tls">SSH-TLS</option><option value="ssh-tls-proxy">SSH-TLS-Proxy</option><option value="ssh-tls-payload">SSH-TLS-Payload</option><option value="ssh-tls-proxy-payload">SSH-TLS-Proxy-Payload</option><option value="dnstt">SSH-DNSTT</option></select></div>
<div><label>Remarks</label><input name="country" required value="<?=$sv('country')?>" placeholder="STC Fast 2026"></div>
<div><label>Network Name</label><input name="carrier" value="<?=$sv('carrier')?>" placeholder="STC"></div>
<div><label>Subtitle</label><input name="subtitle" value="<?=$sv('subtitle')?>"></div>
<div><label>SSH Host</label><input name="host" required value="<?=$sv('host')?>"></div>
<div><label>SSH Port</label><input type="number" name="port" value="<?=$sv('port','22')?>"></div>
<div><label>Udpgw Port</label><input type="number" name="udpgw_port" value="<?=$sv('udpgw_port','7300')?>"></div>
<div><label>Proxy host:port</label><input name="proxy" value="<?=$sv('proxy')?>"></div>
<div><label>TLS Version</label><select name="tls_version"><option value="default">Default</option><option>1.1</option><option>1.2</option><option>1.3</option></select></div>
<div><label>SNI / Server Name Indication</label><input name="sni" value="<?=$sv('sni')?>"></div>
<div class="full"><label>Payload template</label><textarea name="payload" placeholder="اكتب Payload المصرح به هنا"><?=$sv('payload')?></textarea></div>
<div><label>SSH Username</label><input name="ssh_user" value=""></div>
<div><label>SSH Password</label><input type="password" name="ssh_pass" value=""></div>
<input type="hidden" name="network" value="tcp"><input type="hidden" name="public_config" value="<?=$sv('public_config')?>"><input type="hidden" name="secret" value="<?=$sv('secret')?>">
<?php else: ?>
<div><label>Protocol</label><select name="protocol"><option value="vless">VLESS</option><option value="vmess">VMess</option><option value="trojan">Trojan</option></select></div>
<div><label>Remarks</label><input name="country" required value="<?=$sv('country')?>" placeholder="اسم الدولة أو السيرفر"></div>
<div><label>Network Name</label><input name="carrier" value="<?=$sv('carrier')?>" placeholder="STC / Zain / Mobily"></div>
<div><label>Subtitle</label><input name="subtitle" value="<?=$sv('subtitle')?>"></div>
<div class="full"><label>Config URL</label><textarea name="public_config" required placeholder="vless://AUTH@example.com:443?...&_sid={ID}"><?=$sv('public_config')?></textarea></div>
<div><label>UUID / Password</label><input type="password" name="secret" autocomplete="new-password"></div>
<input type="hidden" name="host" value="<?=$sv('host')?>"><input type="hidden" name="port" value="<?=$sv('port','443')?>"><input type="hidden" name="network" value="<?=$sv('network','tcp')?>">
<?php endif; ?>
</div>
<label class="check"><input type="checkbox" name="active" value="1" <?=(!$editServer||$editServer['active'])?'checked':''?>> فعال</label>
<div class="actions"><button>SAVE CONFIG</button><a class="btn" href="?tab=<?=$mode?>">إلغاء</a></div>
</form></div>
<div class="card"><h2><?=$mode==='ssh'?'SSH Configurations':'V2Ray Configurations'?></h2><div class="tablewrap"><table><tr><th>Remarks</th><th>Protocol</th><th>Host</th><th>Status</th><th>Action</th></tr><?php foreach($servers as $s):?><tr><td><?=$esc($s['country'])?></td><td><?=$esc($s['protocol'])?></td><td><?=$esc($s['host'])?>:<?=$esc($s['port'])?></td><td><span class="pill"><?=$s['active']?'فعال':'متوقف'?></span></td><td><a class="btn" href="?tab=<?=$mode?>&edit_server=<?=$esc($s['id'])?>#form">تعديل</a> <form style="display:inline" method="post"><input type="hidden" name="csrf" value="<?=$csrf?>"><input type="hidden" name="action" value="delete_server"><input type="hidden" name="id" value="<?=$esc($s['id'])?>"><button class="danger">حذف</button></form></td></tr><?php endforeach;?></table></div></div>
<?php endif; ?></main></div></body></html>
