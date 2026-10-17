<?php
require_once __DIR__ . '/web_session.php';
require_once 'db.php';

function notificationRespond(int $status, array $payload): void
{
    http_response_code($status);
    echo json_encode($payload);
    exit();
}

$recipient = dormigoNotificationSessionUser($pdo);
if (!$recipient) notificationRespond(401, ['success' => false, 'message' => 'Authentication is required to access notifications.']);
$recipientId = (int)$recipient['user_id'];
$method = $_SERVER['REQUEST_METHOD'] ?? '';

if ($method === 'GET') {
    $stmt = $pdo->prepare('SELECT notification_id, user_id, title, message, type, is_read, created_at, reference_id FROM notifications WHERE user_id = :user_id ORDER BY is_read ASC, created_at DESC, notification_id DESC');
    $stmt->execute(['user_id' => $recipientId]);
    $notifications = $stmt->fetchAll();
    foreach ($notifications as &$notification) {
        $notification['is_read'] = in_array($notification['is_read'], [true, 't', '1', 1], true);
    }
    unset($notification);
    $unread = $pdo->prepare('SELECT COUNT(*) FROM notifications WHERE user_id = :user_id AND is_read = FALSE');
    $unread->execute(['user_id' => $recipientId]);
    notificationRespond(200, ['success' => true, 'unread_count' => (int)$unread->fetchColumn(), 'data' => $notifications]);
}

if ($method !== 'PATCH') notificationRespond(405, ['success' => false, 'message' => 'Notification creation and deletion are managed by server-side events.']);
$data = json_decode(file_get_contents('php://input'), true);
if (!is_array($data)) notificationRespond(400, ['success' => false, 'message' => 'A valid JSON request body is required.']);

if (!empty($data['mark_all_read'])) {
    $update = $pdo->prepare('UPDATE notifications SET is_read = TRUE WHERE user_id = :user_id AND is_read = FALSE');
    $update->execute(['user_id' => $recipientId]);
} else {
    $notificationId = filter_var($data['notification_id'] ?? null, FILTER_VALIDATE_INT);
    if ($notificationId === false || $notificationId === null || $notificationId <= 0) notificationRespond(400, ['success' => false, 'message' => 'A valid notification_id is required.']);
    $update = $pdo->prepare('UPDATE notifications SET is_read = TRUE WHERE notification_id = :notification_id AND user_id = :user_id RETURNING notification_id');
    $update->execute(['notification_id' => $notificationId, 'user_id' => $recipientId]);
    if (!$update->fetchColumn()) notificationRespond(404, ['success' => false, 'message' => 'Notification was not found for this recipient.']);
}

$unread = $pdo->prepare('SELECT COUNT(*) FROM notifications WHERE user_id = :user_id AND is_read = FALSE');
$unread->execute(['user_id' => $recipientId]);
notificationRespond(200, ['success' => true, 'unread_count' => (int)$unread->fetchColumn()]);
