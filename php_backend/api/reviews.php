<?php
require_once 'db.php';

$method = $_SERVER['REQUEST_METHOD'];

if ($method === 'GET') {
    $sql = "SELECT r.*, u.full_name, u.email, u.profile_image, bh.house_name, bh.address, bh.landlord_id
            FROM reviews r
            JOIN users u ON r.user_id = u.user_id
            JOIN boarding_houses bh ON r.house_id = bh.house_id";
    $params = [];

    if (isset($_GET['house_id'])) {
        $sql .= " WHERE r.house_id = :house_id";
        $params['house_id'] = (int)$_GET['house_id'];
    } elseif (isset($_GET['user_id'])) {
        $sql .= " WHERE r.user_id = :user_id";
        $params['user_id'] = (int)$_GET['user_id'];
    } elseif (isset($_GET['landlord_id'])) {
        $sql .= " WHERE bh.landlord_id = :landlord_id";
        $params['landlord_id'] = (int)$_GET['landlord_id'];
    }
    $sql .= " ORDER BY r.review_id DESC";

    $stmt = $pdo->prepare($sql);
    $stmt->execute($params);
    echo json_encode(["success" => true, "data" => $stmt->fetchAll()]);

} elseif ($method === 'POST') {
    $data = json_decode(file_get_contents("php://input"), true);
    if (!isset($data['user_id']) || !isset($data['house_id']) || !isset($data['rating'])) {
        echo json_encode(["success" => false, "message" => "user_id, house_id, and rating are required."]);
        exit();
    }

    try {
        $stmt = $pdo->prepare("INSERT INTO reviews (user_id, house_id, rating, comment, created_at)
                                VALUES (:user_id, :house_id, :rating, :comment, CURRENT_TIMESTAMP) RETURNING review_id");
        $stmt->execute([
            'user_id' => $data['user_id'],
            'house_id' => $data['house_id'],
            'rating' => (int)$data['rating'],
            'comment' => $data['comment'] ?? null
        ]);
        $reviewId = $stmt->fetchColumn();

        // Notify landlord of new review
        $ll = $pdo->prepare("SELECT landlord_id, house_name FROM boarding_houses WHERE house_id = :hid");
        $ll->execute(['hid' => $data['house_id']]);
        $bh = $ll->fetch();
        if ($bh) {
            $n = $pdo->prepare("INSERT INTO notifications (user_id, title, message, type, is_read, reference_id, created_at)
                                VALUES (:uid, 'New House Review', :msg, 'SYSTEM', FALSE, :ref, CURRENT_TIMESTAMP)");
            $n->execute([
                'uid' => $bh['landlord_id'],
                'msg' => "A student left a {$data['rating']}-star review for {$bh['house_name']}.",
                'ref' => $reviewId
            ]);
        }

        echo json_encode(["success" => true, "message" => "Review submitted successfully.", "review_id" => $reviewId]);
    } catch (PDOException $e) {
        echo json_encode(["success" => false, "message" => "Database error: " . $e->getMessage()]);
    }
}
?>
