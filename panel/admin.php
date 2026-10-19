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
if (isset($_GET['logout'])) { session_destroy(); header('Location: admin.php'); exit; }
if (empty($_SESSION['admin'])) {
    $error = '';
    if ($_SERVER['REQUEST_METHOD'] === 'POST' && ($_POST['action'] ?? '') === 'login') {
        if (password_verify((string)($_POST['password'] ?? ''), (string)$stored)) {
            session_regenerate_id(true); $_SESSION['admin'] = true; header('Location: admin.php'); exit;
        }
        $error = 'بيانات الدخول غير صحيحة';
    }
    ?><!doctype html><html lang="ar" dir="rtl"><meta charset="utf-8"><title>Max Tunnel Admin</title>
    <style>body{font-family:Arial;background:#101827;color:#fff;max-width:420px;margin:12vh auto;padding:24px}input,button{box-sizing:border-box;width:100%;padding:12px;margin:8px 0;border-radius:8px;border:0}button{background:#16a34a;color:white;font-weight:bold}.err{color:#fca5a5}</style>
    <h2>Max Tunnel — لوحة الإدارة</h2><?php if ($error) echo '<p class="err">'.h($error).'</p>'; ?><form method="post"><input type="hidden" name="action" value="login"><input type="password" name="password" placeholder="كلمة مرور الإدارة" required autofocus><button>دخول آمن</button></form></html><?php exit;
}

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    check_csrf();
    $action = $_POST['action'] ?? '';
    if ($action === 'delete_server') {
        db()->prepare('DELETE FROM servers WHERE id=?')->execute([$_POST['id'] ?? '']); audit('server_delete'); header('Location: admin.php'); exit;
    }
    if ($action === 'delete_payload') {
        db()->prepare('DELETE FROM payloads WHERE id=?')->execute([$_POST['id'] ?? '']); audit('payload_delete'); header('Location: admin.php'); exit;
    }
    if ($action === 'save_payload') {
        $id = preg_match('/^[a-f0-9]{32}$/', $_POST['id'] ?? '') ? $_POST['id'] : random_hex(16);
        $network = trim((string)($_POST['network'] ?? ''));
        $subtitle = trim((string)($_POST['subtitle'] ?? ''));
        $config = trim((string)($_POST['config'] ?? ''));
        if ($network === '' || $config === '') exit('اسم الشبكة وPayload مطلوبان.');
        $q = db()->prepare('INSERT INTO payloads(id,network,subtitle,config,active,created_at,updated_at) VALUES(?,?,?,?,?,?,?) ON CONFLICT(id) DO UPDATE SET network=excluded.network,subtitle=excluded.subtitle,config=excluded.config,active=excluded.active,updated_at=excluded.updated_at');
        $q->execute([$id, $network, $subtitle ?: 'Authorized network payload', $config, (int)isset($_POST['active']), time(), time()]);
        audit('payload_save'); header('Location: admin.php'); exit;
    }
    if ($action === 'save_server') {
        $id = preg_match('/^[a-f0-9]{32}$/', $_POST['id'] ?? '') ? $_POST['id'] : random_hex(16);
        $public = str_replace('{ID}', $id, trim((string)($_POST['public_config'] ?? '')));
        if ($public === '' || stripos($public, 'AUTH') === false || preg_match('/(uuid|password|passwd)=/i', $public)) exit('رابط السيرفر العام يجب أن يحتوي AUTH ولا يحتوي كلمة المرور.');
        $values = [$id, trim((string)($_POST['country'] ?? 'Server')) ?: 'Server', trim((string)($_POST['subtitle'] ?? 'Secure server')) ?: 'Secure server', trim((string)($_POST['protocol'] ?? 'vless')), trim((string)($_POST['host'] ?? '')), (int)($_POST['port'] ?? 443), trim((string)($_POST['sni'] ?? '')), trim((string)($_POST['network'] ?? 'tcp')), trim((string)($_POST['path'] ?? '/')), master_encrypt((string)($_POST['secret'] ?? '')), master_encrypt((string)($_POST['ssh_user'] ?? '')), master_encrypt((string)($_POST['ssh_pass'] ?? '')), $public, (int)isset($_POST['active']), time(), time()];
        db()->prepare('INSERT INTO servers(id,country,subtitle,protocol,host,port,sni,network,path,secret_enc,ssh_user_enc,ssh_pass_enc,public_config,active,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT(id) DO UPDATE SET country=excluded.country,subtitle=excluded.subtitle,protocol=excluded.protocol,host=excluded.host,port=excluded.port,sni=excluded.sni,network=excluded.network,path=excluded.path,secret_enc=excluded.secret_enc,ssh_user_enc=excluded.ssh_user_enc,ssh_pass_enc=excluded.ssh_pass_enc,public_config=excluded.public_config,active=excluded.active,updated_at=excluded.updated_at')->execute($values);
        audit('server_save'); header('Location: admin.php'); exit;
    }
}

$csrf = csrf_token();
$editServer = null; if (!empty($_GET['edit_server'])) { $q=db()->prepare('SELECT * FROM servers WHERE id=?'); $q->execute([$_GET['edit_server']]); $editServer=$q->fetch() ?: null; }
$editPayload = null; if (!empty($_GET['edit_payload'])) { $q=db()->prepare('SELECT * FROM payloads WHERE id=?'); $q->execute([$_GET['edit_payload']]); $editPayload=$q->fetch() ?: null; }
$servers = db()->query('SELECT id,country,subtitle,protocol,host,port,active FROM servers ORDER BY updated_at DESC')->fetchAll();
$payloads = db()->query('SELECT id,network,subtitle,config,active FROM payloads ORDER BY updated_at DESC')->fetchAll();
?><!doctype html><html lang="ar" dir="rtl"><meta charset="utf-8"><title>Max Tunnel Admin</title>
<style>body{font-family:Arial;background:#f3f4f6;color:#111827;margin:0}.wrap{max-width:1100px;margin:auto;padding:24px}.card{background:white;border-radius:14px;padding:18px;margin:14px 0;box-shadow:0 2px 10px #0001}input,select,textarea{width:100%;box-sizing:border-box;padding:10px;margin:5px 0 12px;border:1px solid #d1d5db;border-radius:8px}textarea{min-height:80px;direction:ltr}button,.btn{background:#2563eb;color:white;border:0;border-radius:8px;padding:10px 16px;cursor:pointer;text-decoration:none}.danger{background:#dc2626}.muted{color:#6b7280;font-size:13px}table{width:100%;border-collapse:collapse}td,th{padding:9px;border-bottom:1px solid #e5e7eb;text-align:right}.inline{display:inline}</style>
<div class="wrap"><h1>Max Tunnel — لوحة السيرفرات والـ Payloads</h1><p><a class="btn" href="?logout=1">خروج</a></p>
<div class="card"><h2><?= $editServer ? 'تعديل سيرفر' : 'إضافة سيرفر V2Ray/SSH' ?></h2><form method="post"><input type="hidden" name="csrf" value="<?= h($csrf) ?>"><input type="hidden" name="action" value="save_server"><input type="hidden" name="id" value="<?= h($editServer['id'] ?? '') ?>"><label>الدولة/الاسم</label><input name="country" required value="<?= h($editServer['country'] ?? '') ?>"><label>الوصف</label><input name="subtitle" value="<?= h($editServer['subtitle'] ?? '') ?>"><label>النوع</label><select name="protocol"><option value="vless">VLESS / Xray</option><option value="vmess">VMess / Xray</option><option value="trojan">Trojan / Xray</option><option value="ssh">SSH</option></select><label>الرابط العام: استخدم AUTH بدل السر و{ID} مكان المعرف</label><textarea name="public_config" required placeholder="vless://AUTH@example.com:443?type=ws&security=tls&sni=example.com&_sid={ID}"><?= h($editServer['public_config'] ?? '') ?></textarea><label>Host / Port / SNI</label><input name="host" value="<?= h($editServer['host'] ?? '') ?>"><input type="number" name="port" value="<?= h((string)($editServer['port'] ?? 443)) ?>"><input name="sni" value="<?= h($editServer['sni'] ?? '') ?>"><label>Network / Path</label><input name="network" value="<?= h($editServer['network'] ?? 'tcp') ?>"><input name="path" value="<?= h($editServer['path'] ?? '/') ?>"><label>السر الحقيقي — لا يظهر في القائمة</label><input name="secret" type="password" autocomplete="new-password"><label>SSH user / password (اختياري)</label><input name="ssh_user"><input name="ssh_pass" type="password"><label><input type="checkbox" name="active" value="1" <?= (!$editServer || $editServer['active']) ? 'checked' : '' ?>> فعال</label><button>حفظ السيرفر مشفراً</button></form></div>
<div class="card"><h2><?= $editPayload ? 'تعديل Payload' : 'إضافة Payload مصرح' ?></h2><p class="muted">أضف Payloads التي تملك حق استخدامها فقط. لا توجد قائمة عالمية صالحة لكل شركات الاتصالات؛ تختلف حسب الشبكة والسيرفر.</p><form method="post"><input type="hidden" name="csrf" value="<?= h($csrf) ?>"><input type="hidden" name="action" value="save_payload"><input type="hidden" name="id" value="<?= h($editPayload['id'] ?? '') ?>"><label>اسم الشبكة/الشركة</label><input name="network" required value="<?= h($editPayload['network'] ?? '') ?>"><label>الوصف</label><input name="subtitle" value="<?= h($editPayload['subtitle'] ?? '') ?>"><label>Payload</label><textarea name="config" required placeholder="اكتب قالب الاتصال المصرح به هنا"><?= h($editPayload['config'] ?? '') ?></textarea><label><input type="checkbox" name="active" value="1" <?= (!$editPayload || $editPayload['active']) ? 'checked' : '' ?>> فعال</label><button>حفظ Payload</button></form></div>
<div class="card"><h2>السيرفرات</h2><table><tr><th>الاسم</th><th>النوع</th><th>Host</th><th>الحالة</th><th>إجراء</th></tr><?php foreach ($servers as $s): ?><tr><td><?= h($s['country']) ?></td><td><?= h($s['protocol']) ?></td><td><?= h($s['host']) ?>:<?= h((string)$s['port']) ?></td><td><?= $s['active'] ? 'فعال' : 'متوقف' ?></td><td><a class="btn" href="?edit_server=<?= h($s['id']) ?>">تعديل</a> <form class="inline" method="post"><input type="hidden" name="csrf" value="<?= h($csrf) ?>"><input type="hidden" name="action" value="delete_server"><input type="hidden" name="id" value="<?= h($s['id']) ?>"><button class="danger" onclick="return confirm('حذف السيرفر؟')">حذف</button></form></td></tr><?php endforeach; ?></table></div>
<div class="card"><h2>Payloads</h2><table><tr><th>الشبكة</th><th>الوصف</th><th>الحالة</th><th>إجراء</th></tr><?php foreach ($payloads as $p): ?><tr><td><?= h($p['network']) ?></td><td><?= h($p['subtitle']) ?></td><td><?= $p['active'] ? 'فعال' : 'متوقف' ?></td><td><a class="btn" href="?edit_payload=<?= h($p['id']) ?>">تعديل</a> <form class="inline" method="post"><input type="hidden" name="csrf" value="<?= h($csrf) ?>"><input type="hidden" name="action" value="delete_payload"><input type="hidden" name="id" value="<?= h($p['id']) ?>"><button class="danger" onclick="return confirm('حذف Payload؟')">حذف</button></form></td></tr><?php endforeach; ?></table><p class="muted">تصل الـ Payloads ضمن الاستجابة المشفرة، ولا تحتاج إلى وضع أسرار السيرفر فيها.</p></div></div></html>
