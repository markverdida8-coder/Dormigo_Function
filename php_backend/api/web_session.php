<?php

function dormigoWebSessionStart(): void
{
    if (session_status() !== PHP_SESSION_NONE) return;
    session_set_cookie_params([
        'lifetime' => 0,
        'path' => '/',
        'secure' => isset($_SERVER['HTTPS']) && $_SERVER['HTTPS'] === 'on',
        'httponly' => true,
        'samesite' => 'Lax'
    ]);
    session_start();
}

function dormigoWebSessionSetUser(array $user): void
{
    $role = strtoupper((string)($user['user_type'] ?? ''));
    if (!in_array($role, ['STUDENT', 'LANDLORD'], true)) return;
    dormigoWebSessionStart();
    session_regenerate_id(true);
    $_SESSION['web_user_id'] = (int)$user['user_id'];
    $_SESSION['web_user_type'] = $role;
}

function dormigoNotificationSessionUser(PDO $pdo): ?array
{
    dormigoWebSessionStart();
    if (($_SESSION['admin_logged_in'] ?? false) === true && strtoupper((string)($_SESSION['admin_user_type'] ?? '')) === 'ADMIN') {
        $userId = (int)($_SESSION['admin_user_id'] ?? 0);
        $expectedRole = 'ADMIN';
    } else {
        $userId = (int)($_SESSION['web_user_id'] ?? 0);
        $expectedRole = strtoupper((string)($_SESSION['web_user_type'] ?? ''));
    }
    if ($userId <= 0 || !in_array($expectedRole, ['ADMIN', 'STUDENT', 'LANDLORD'], true)) return null;

    $stmt = $pdo->prepare('SELECT user_id, user_type FROM users WHERE user_id = :user_id');
    $stmt->execute(['user_id' => $userId]);
    $user = $stmt->fetch(PDO::FETCH_ASSOC);
    if (!$user || strtoupper((string)$user['user_type']) !== $expectedRole) return null;
    return $user;
}
