# messages.php
path_m = r'C:\xampp\htdocs\Dormigo_Backend\api\messages.php'
with open(path_m, 'r', encoding='utf-8') as f:
    content_m = f.read()

old_m = '''        $result = $stmt->fetch();

        echo json_encode(['''
new_m = '''        $result = $stmt->fetch();

        // Notification
        $info = $pdo->prepare("SELECT full_name FROM users WHERE user_id = ?");
        $info->execute([$data['sender_id']]);
        $senderName = $info->fetchColumn();
        if ($senderName) {
            $nStmt = $pdo->prepare("INSERT INTO notifications (user_id, title, message, type, reference_id) VALUES (?, ?, ?, 'CHAT', ?)");
            $nStmt->execute([$data['receiver_id'], 'New Message', 'You received a new message from ' . $senderName . '.', $data['sender_id']]);
        }

        echo json_encode(['''
content_m = content_m.replace(old_m, new_m)
with open(path_m, 'w', encoding='utf-8') as f:
    f.write(content_m)

# reviews.php
path_r = r'C:\xampp\htdocs\Dormigo_Backend\api\reviews.php'
with open(path_r, 'r', encoding='utf-8') as f:
    content_r = f.read()

old_r = '''        $stmt = $pdo->prepare("INSERT INTO reviews (user_id, house_id, rating, comment) VALUES (:user_id, :house_id, :rating, :comment) RETURNING review_id");
        $stmt->execute([
            'user_id' => $user_id,
            'house_id' => $house_id,
            'rating' => $rating,
            'comment' => $comment
        ]);
        echo json_encode(["success" => true, "message" => "Review submitted successfully.", "review_id" => $stmt->fetchColumn()]);'''
new_r = '''        $stmt = $pdo->prepare("INSERT INTO reviews (user_id, house_id, rating, comment) VALUES (:user_id, :house_id, :rating, :comment) RETURNING review_id");
        $stmt->execute([
            'user_id' => $user_id,
            'house_id' => $house_id,
            'rating' => $rating,
            'comment' => $comment
        ]);
        $reviewId = $stmt->fetchColumn();

        // Notification
        $info = $pdo->prepare("SELECT u.full_name, bh.landlord_id, bh.house_name FROM boarding_houses bh JOIN users u ON u.user_id = :uid WHERE bh.house_id = :hid");
        $info->execute(['uid' => $user_id, 'hid' => $house_id]);
        $infoData = $info->fetch();
        if ($infoData && $infoData['landlord_id']) {
            $nStmt = $pdo->prepare("INSERT INTO notifications (user_id, title, message, type, reference_id) VALUES (?, ?, ?, 'REVIEW', ?)");
            $nStmt->execute([$infoData['landlord_id'], 'New Review', $infoData['full_name'] . ' left a ' . $rating . '-star review for ' . $infoData['house_name'] . '.', $reviewId]);
        }

        echo json_encode(["success" => true, "message" => "Review submitted successfully.", "review_id" => $reviewId]);'''

content_r = content_r.replace(old_r, new_r)
with open(path_r, 'w', encoding='utf-8') as f:
    f.write(content_r)
print("Updated messages and reviews.")