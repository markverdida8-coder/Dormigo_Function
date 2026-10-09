<?php
require_once 'db.php';

$method = $_SERVER['REQUEST_METHOD'];

if ($method === 'GET') {
    if (isset($_GET['user_id']) && isset($_GET['other_user_id'])) {
        // Fetch full message thread between two users
        $userId = (int)$_GET['user_id'];
        $otherUserId = (int)$_GET['other_user_id'];

        $stmt = $pdo->prepare("SELECT m.*,
                                       u1.full_name AS sender_name,
                                       u2.full_name AS receiver_name
                                FROM messages m
                                JOIN users u1 ON m.sender_id = u1.user_id
                                JOIN users u2 ON m.receiver_id = u2.user_id
                                WHERE (m.sender_id = :u1 AND m.receiver_id = :u2)
                                   OR (m.sender_id = :u2 AND m.receiver_id = :u1)
                                ORDER BY m.created_at ASC");
        $stmt->execute(['u1' => $userId, 'u2' => $otherUserId]);
        
        // Auto mark received messages in this thread as read
        $markStmt = $pdo->prepare("UPDATE messages SET is_read = TRUE WHERE receiver_id = :u1 AND sender_id = :u2");
        $markStmt->execute(['u1' => $userId, 'u2' => $otherUserId]);

        echo json_encode(["success" => true, "data" => $stmt->fetchAll()]);

    } elseif (isset($_GET['user_id'])) {
        // Fetch list of recent chat conversations for a user
        $userId = (int)$_GET['user_id'];

        $sql = "SELECT DISTINCT ON (other_user_id)
                       m.message_id,
                       m.message_text,
                       m.created_at,
                       m.is_read,
                       m.sender_id,
                       u.user_id AS other_user_id,
                       u.full_name AS other_user_name,
                       u.user_type AS other_user_type,
                       u.profile_image AS other_user_image
                FROM (
                    SELECT *,
                           CASE WHEN sender_id = :user_id THEN receiver_id ELSE sender_id END AS other_user_id
                    FROM messages
                    WHERE sender_id = :user_id OR receiver_id = :user_id
                ) m
                JOIN users u ON m.other_user_id = u.user_id
                ORDER BY other_user_id, m.created_at DESC";

        $stmt = $pdo->prepare($sql);
        $stmt->execute(['user_id' => $userId]);
        $convos = $stmt->fetchAll();

        // Calculate unread count for each conversation
        foreach ($convos as &$convo) {
            $unStmt = $pdo->prepare("SELECT COUNT(*) FROM messages WHERE sender_id = :other AND receiver_id = :uid AND is_read = FALSE");
            $unStmt->execute(['other' => $convo['other_user_id'], 'uid' => $userId]);
            $convo['unread_count'] = (int)$unStmt->fetchColumn();
        }

        echo json_encode(["success" => true, "data" => $convos]);
    } else {
        echo json_encode(["success" => false, "message" => "user_id is required."]);
    }

} elseif ($method === 'POST') {
    $data = json_decode(file_get_contents("php://input"), true);

    if (!isset($data['sender_id']) || !isset($data['receiver_id']) || !isset($data['message_text'])) {
        echo json_encode(["success" => false, "message" => "sender_id, receiver_id, and message_text are required."]);
        exit();
    }

    try {
        $stmt = $pdo->prepare("INSERT INTO messages (sender_id, receiver_id, house_id, message_text, is_read, created_at)
                                VALUES (:sender_id, :receiver_id, :house_id, :message_text, FALSE, CURRENT_TIMESTAMP) RETURNING message_id, created_at");
        $stmt->execute([
            'sender_id' => $data['sender_id'],
            'receiver_id' => $data['receiver_id'],
            'house_id' => $data['house_id'] ?? null,
            'message_text' => trim($data['message_text'])
        ]);

        $result = $stmt->fetch();

        // Get sender name for notification
        $sndr = $pdo->prepare("SELECT full_name FROM users WHERE user_id = :uid");
        $sndr->execute(['uid' => $data['sender_id']]);
        $senderName = $sndr->fetchColumn() ?: "User";

        $notif = $pdo->prepare("INSERT INTO notifications (user_id, title, message, type, is_read, reference_id, created_at)
                                VALUES (:uid, 'New Message', :msg, 'MESSAGE', FALSE, :ref, CURRENT_TIMESTAMP)");
        $snippet = strlen($data['message_text']) > 40 ? substr($data['message_text'], 0, 37) . '...' : $data['message_text'];
        $notif->execute([
            'uid' => $data['receiver_id'],
            'msg' => "{$senderName}: {$snippet}",
            'ref' => $result['message_id']
        ]);

        echo json_encode([
            "success" => true,
            "message" => "Message sent successfully.",
            "message_id" => $result['message_id'],
            "created_at" => $result['created_at']
        ]);
    } catch (PDOException $e) {
        echo json_encode(["success" => false, "message" => "Database error: " . $e->getMessage()]);
    }

} elseif ($method === 'PATCH') {
    $data = json_decode(file_get_contents("php://input"), true);
    if (isset($data['receiver_id']) && isset($data['sender_id'])) {
        $stmt = $pdo->prepare("UPDATE messages SET is_read = TRUE WHERE receiver_id = :rec AND sender_id = :snd");
        $stmt->execute(['rec' => (int)$data['receiver_id'], 'snd' => (int)$data['sender_id']]);
        echo json_encode(["success" => true, "message" => "Messages marked as read."]);
    } else {
        echo json_encode(["success" => false, "message" => "receiver_id and sender_id are required."]);
    }
}
?>
