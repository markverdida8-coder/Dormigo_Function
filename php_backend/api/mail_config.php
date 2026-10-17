<?php
/**
 * DORMIGO - Mail & SMTP Configuration
 * Loads local credentials if available, otherwise defaults to template values.
 */

if (file_exists(__DIR__ . '/mail_config.local.php')) {
    return require __DIR__ . '/mail_config.local.php';
}

return [
    'smtp_enabled' => false,

    'host'       => 'smtp-relay.brevo.com',
    'port'       => 587,
    'secure'     => 'tls',
    'username'   => 'YOUR_BREVO_LOGIN',
    'password'   => 'YOUR_BREVO_SMTP_KEY',

    'from_email' => 'dormigohelp@gmail.com',
    'from_name'  => 'Dormigo Security',

    // Debug log location
    'debug_log'  => __DIR__ . '/../logs/otp_debug.log'
];
