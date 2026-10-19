<?php
declare(strict_types=1);

// انسخ هذا الملف إلى config.php خارج Git واضبط القيم العشوائية.
$PANEL_CONFIG = [
    // يجب أن يطابق HANDSHAKE_KEY الموجود في SecureApi.java.
    'HANDSHAKE_KEY' => 'CHANGE_ME_HANDSHAKE_KEY',
    // يجب أن يطابق API_SECURE_KEY الموجود في SecureApi.java.
    'API_SECURE_KEY' => 'CHANGE_ME_API_SECURE_KEY',
    // مفتاح مختلف تماماً لتشفير كلمات المرور داخل SQLite.
    'APP_MASTER_KEY' => 'CHANGE_ME_RANDOM_64_CHAR_MASTER_KEY',
    // كلمة مرور الإدارة الأولى؛ غيّرها بعد أول دخول.
    'ADMIN_PASSWORD' => 'CHANGE_ME_LONG_ADMIN_PASSWORD',
];
