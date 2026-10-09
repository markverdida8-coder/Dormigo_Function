<?php
require_once 'db.php';

$method = $_SERVER['REQUEST_METHOD'];

if ($method === 'GET') {
    if (!isset($_GET['user_id'])) {
        echo json_encode(["success" => false, "message" => "user_id is required."]);
        exit();
    }
    $userId = (int)$_GET['user_id'];

    // user_id=0 is the admin "all notifications" view
    if ($userId === 0) {
        $limit = isset($_GET['limit']) ? (int)$_GET['limit'] : 100;
        $stmt = $pdo->prepare(
            "SELECT n.*, u.full_name, u.email, u.user_type
             FROM notifications n
             LEFT JOIN users u ON n.user_id = u.user_id
             ORDER BY n.created_at DESC
             LIMIT :limit"
        );
        $stmt->bindValue(':limit', $limit, PDO::PARAM_INT);
        $stmt->execute();
        $notifications = $stmt->fetchAll();
        $unreadCount = (int)$pdo->query("SELECT COUNT(*) FROM notifications WHERE is_read = FALSE")->fetchColumn();
        echo json_encode(["success" => true, "unread_count" => $unreadCount, "data" => $notifications]);
        exit();
    }

    $stmt = $pdo->prepare("SELECT * FROM notifications WHERE user_id = :user_id ORDER BY created_at DESC");
    $stmt->execute(['user_id' => $userId]);
    $notifications = $stmt->fetchAll();
    
    // Also count unread
    $unreadStmt = $pdo->prepare("SELECT COUNT(*) FROM notifications WHERE user_id = :user_id AND is_read = FALSE");
    $unreadStmt->execute(['user_id' => $userId]);
    $unreadCount = (int)$unreadStmt->fetchColumn();

    echo json_encode([
        "success" => true,
        "unread_count" => $unreadCount,
        "data" => $notifications
    ]);

} elseif ($method === 'POST') {
    $data = json_decode(file_get_contents("php://input"), true);
    if (!isset($data['user_id']) || !isset($data['title']) || !isset($data['message'])) {
        echo json_encode(["success" => false, "message" => "user_id, title, and message are required."]);
        exit();
    }

    $type = strtoupper($data['type'] ?? 'SYSTEM');
    $validTypes = ['REQUEST', 'PAYMENT', 'MESSAGE', 'SYSTEM'];
    if (!in_array($type, $validTypes)) {
        $type = 'SYSTEM';
    }

    try {
        $stmt = $pdo->prepare("INSERT INTO notifications (user_id, title, message, type, is_read, reference_id, created_at)
                                VALUES (:user_id, :title, :message, :type::notif_type_enum, FALSE, :reference_id, CURRENT_TIMESTAMP)
                                RETURNING notification_id, created_at");
        $stmt->execute([
            'user_id' => $data['user_id'],
            'title' => $data['title'],
            'message' => $data['message'],
            'type' => $type,
            'reference_id' => $data['reference_id'] ?? null
        ]);
        $res = $stmt->fetch();
        echo json_encode(["success" => true, "message" => "Notification created.", "notification_id" => $res['notification_id']]);
    } catch (PDOException $e) {
        echo json_encode(["success" => false, "message" => "Database error: " . $e->getMessage()]);
    }

} elseif ($method === 'PATCH') {
    $data = json_decode(file_get_contents("php://input"), true);
    if (isset($data['mark_all_read']) && $data['mark_all_read'] && isset($data['user_id'])) {
        $stmt = $pdo->prepare("UPDATE notifications SET is_read = TRUE WHERE user_id = :user_id");
        $stmt->execute(['user_id' => (int)$data['user_id']]);
        echo json_encode(["success" => true, "message" => "All notifications marked as read."]);
    } elseif (isset($data['notification_id'])) {
        $stmt = $pdo->prepare("UPDATE notifications SET is_read = TRUE WHERE notification_id = :notif_id");
        $stmt->execute(['notif_id' => (int)$data['notification_id']]);
        echo json_encode(["success" => true, "message" => "Notification marked as read."]);
    } else {
        echo json_encode(["success" => false, "message" => "notification_id or user_id required."]);
    }
}
?>
