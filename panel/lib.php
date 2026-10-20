<?php
declare(strict_types=1);

$configFile = __DIR__ . '/config.php';
if (!is_file($configFile)) {
    http_response_code(500);
    exit('Create panel/config.php from config.example.php first.');
}
require $configFile;

function cfg(string $key): string {
    global $PANEL_CONFIG;
    $value = $PANEL_CONFIG[$key] ?? getenv($key) ?: '';
    if ($value === '') throw new RuntimeException("Missing configuration: {$key}");
    return (string)$value;
}

function db(): PDO {
    static $pdo = null;
    if ($pdo instanceof PDO) return $pdo;
    $dir = __DIR__ . '/data';
    if (!is_dir($dir)) mkdir($dir, 0700, true);
    $pdo = new PDO('sqlite:' . $dir . '/panel.sqlite', null, null, [
        PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
    ]);
    $pdo->exec('PRAGMA journal_mode=WAL; PRAGMA foreign_keys=ON;');
    $pdo->exec('CREATE TABLE IF NOT EXISTS sessions (token TEXT PRIMARY KEY, did TEXT NOT NULL, ip_hash TEXT NOT NULL, snonce TEXT NOT NULL, expires_at INTEGER NOT NULL, created_at INTEGER NOT NULL)');
    try { $pdo->exec('ALTER TABLE sessions ADD COLUMN snonce TEXT NOT NULL DEFAULT ""'); } catch (Throwable $ignored) {}
    $pdo->exec('CREATE TABLE IF NOT EXISTS servers (id TEXT PRIMARY KEY, country TEXT NOT NULL, subtitle TEXT NOT NULL, protocol TEXT NOT NULL, host TEXT NOT NULL, port INTEGER NOT NULL, sni TEXT NOT NULL DEFAULT \'\', network TEXT NOT NULL DEFAULT \'tcp\', path TEXT NOT NULL DEFAULT \'/\', secret_enc TEXT NOT NULL DEFAULT \'\', ssh_user_enc TEXT NOT NULL DEFAULT \'\', ssh_pass_enc TEXT NOT NULL DEFAULT \'\', public_config TEXT NOT NULL, active INTEGER NOT NULL DEFAULT 1, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL)');
    $pdo->exec('CREATE TABLE IF NOT EXISTS audit (id INTEGER PRIMARY KEY AUTOINCREMENT, event TEXT NOT NULL, ip_hash TEXT NOT NULL, created_at INTEGER NOT NULL)');
    $pdo->exec('CREATE TABLE IF NOT EXISTS payloads (id TEXT PRIMARY KEY, network TEXT NOT NULL, subtitle TEXT NOT NULL, config TEXT NOT NULL, active INTEGER NOT NULL DEFAULT 1, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL)');
    foreach ([
        'target_app TEXT NOT NULL DEFAULT "Max Tunnel"',
        'carrier TEXT NOT NULL DEFAULT ""',
        'tls INTEGER NOT NULL DEFAULT 0',
        'tls_version TEXT NOT NULL DEFAULT "default"',
        'fingerprint TEXT NOT NULL DEFAULT "chrome"',
        'alpn TEXT NOT NULL DEFAULT ""',
        'proxy TEXT NOT NULL DEFAULT ""',
        'payload TEXT NOT NULL DEFAULT ""',
        'service_name TEXT NOT NULL DEFAULT ""',
        'reality_public_key TEXT NOT NULL DEFAULT ""',
        'reality_short_id TEXT NOT NULL DEFAULT ""',
        'reality_dest TEXT NOT NULL DEFAULT ""'
    ] as $column) {
        try { $pdo->exec('ALTER TABLE servers ADD COLUMN '.$column); } catch (Throwable $ignored) {}
    }
    return $pdo;
}

function client_ip_hash(): string { return hash('sha256', ($_SERVER['REMOTE_ADDR'] ?? 'unknown') . '|' . cfg('APP_MASTER_KEY')); }
function json_out(array $data, int $status = 200): never { http_response_code($status); header('Content-Type: application/json; charset=utf-8'); header('Cache-Control: no-store'); echo json_encode($data, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES); exit; }
function require_post(): void { if (($_SERVER['REQUEST_METHOD'] ?? 'GET') !== 'POST') json_out(['status'=>'error','message'=>'POST required'], 405); }
function random_hex(int $bytes = 32): string { return bin2hex(random_bytes($bytes)); }
function timing_safe(string $a, string $b): bool { return hash_equals($a, $b); }
function hmac_for(string $message): string { return hash_hmac('sha256', $message, cfg('HANDSHAKE_KEY')); }
function valid_time(string $ts): bool { return ctype_digit($ts) && abs(time() - (int)$ts) <= 90; }
function ip_bound_token(string $token, string $did): ?array {
    $q = db()->prepare('SELECT * FROM sessions WHERE token=? AND did=? AND ip_hash=? AND expires_at>=?');
    $q->execute([$token, $did, client_ip_hash(), time()]);
    $row = $q->fetch();
    if (!$row) return null;
    return $row;
}
function session_key(string $snonce): string { return hash('sha256', cfg('API_SECURE_KEY') . '|' . $snonce, true); }
function encrypt_response(string $plaintext, string $key): string {
    $iv = random_bytes(12); $tag = '';
    $ciphertext = openssl_encrypt($plaintext, 'aes-256-gcm', $key, OPENSSL_RAW_DATA, $iv, $tag, '', 16);
    if ($ciphertext === false) throw new RuntimeException('Encryption failed');
    return base64_encode($iv . $ciphertext . $tag);
}
function master_encrypt(string $plaintext): string {
    $key = hash('sha256', cfg('APP_MASTER_KEY'), true); $iv = random_bytes(12); $tag = '';
    $ct = openssl_encrypt($plaintext, 'aes-256-gcm', $key, OPENSSL_RAW_DATA, $iv, $tag, '', 16);
    if ($ct === false) throw new RuntimeException('Storage encryption failed');
    return base64_encode($iv . $ct . $tag);
}
function master_decrypt(string $encoded): string {
    $raw = base64_decode($encoded, true); if ($raw === false || strlen($raw) < 29) return '';
    $key = hash('sha256', cfg('APP_MASTER_KEY'), true); $iv = substr($raw, 0, 12); $tag = substr($raw, -16); $ct = substr($raw, 12, -16);
    $plain = openssl_decrypt($ct, 'aes-256-gcm', $key, OPENSSL_RAW_DATA, $iv, $tag, '');
    return $plain === false ? '' : $plain;
}
function csrf_token(): string { if (empty($_SESSION['csrf'])) $_SESSION['csrf'] = bin2hex(random_bytes(32)); return $_SESSION['csrf']; }
function check_csrf(): void { if (!isset($_POST['csrf']) || !hash_equals($_SESSION['csrf'] ?? '', $_POST['csrf'])) { http_response_code(419); exit('Invalid CSRF token'); } }
function h(string $v): string { return htmlspecialchars($v, ENT_QUOTES | ENT_SUBSTITUTE, 'UTF-8'); }
function audit(string $event): void { $q=db()->prepare('INSERT INTO audit(event,ip_hash,created_at) VALUES(?,?,?)'); $q->execute([$event,client_ip_hash(),time()]); }
