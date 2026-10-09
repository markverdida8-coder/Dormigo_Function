<?php
require_once 'db.php';

$method = $_SERVER['REQUEST_METHOD'];

if ($method === 'GET') {
    $type = $_GET['type'] ?? 'student';

    if ($type === 'student') {
        $sql = "SELECT sv.*, u.full_name, u.email, u.phone, u.profile_image,
                       reviewer.full_name AS reviewer_name
                FROM student_verifications sv
                JOIN users u ON sv.user_id = u.user_id
                LEFT JOIN users reviewer ON sv.reviewed_by = reviewer.user_id";
        $params = [];
        if (isset($_GET['user_id'])) {
            $sql .= " WHERE sv.user_id = :user_id";
            $params['user_id'] = (int)$_GET['user_id'];
        } elseif (isset($_GET['status'])) {
            $sql .= " WHERE sv.verification_status = :status";
            $params['status'] = strtoupper(trim($_GET['status']));
        }
        $sql .= " ORDER BY sv.verification_id DESC";
        $stmt = $pdo->prepare($sql);
        $stmt->execute($params);
        echo json_encode(["success" => true, "data" => $stmt->fetchAll()]);

    } elseif ($type === 'landlord') {
        $sql = "SELECT lv.*, u.full_name, u.email, u.phone, u.profile_image,
                       reviewer.full_name AS reviewer_name
                FROM landlord_verifications lv
                JOIN users u ON lv.landlord_id = u.user_id
                LEFT JOIN users reviewer ON lv.reviewed_by = reviewer.user_id";
        $params = [];
        if (isset($_GET['landlord_id'])) {
            $sql .= " WHERE lv.landlord_id = :landlord_id";
            $params['landlord_id'] = (int)$_GET['landlord_id'];
        } elseif (isset($_GET['status'])) {
            $sql .= " WHERE lv.verification_status = :status";
            $params['status'] = strtoupper(trim($_GET['status']));
        }
        $sql .= " ORDER BY lv.verification_id DESC";
        $stmt = $pdo->prepare($sql);
        $stmt->execute($params);
        echo json_encode(["success" => true, "data" => $stmt->fetchAll()]);

    } elseif ($type === 'house') {
        $sql = "SELECT bhv.*, bh.house_name, bh.address, u.full_name AS landlord_name
                FROM boarding_house_verification bhv
                JOIN boarding_houses bh ON bhv.house_id = bh.house_id
                JOIN users u ON bh.landlord_id = u.user_id
                ORDER BY bhv.verification_id DESC";
        $stmt = $pdo->query($sql);
        echo json_encode(["success" => true, "data" => $stmt->fetchAll()]);
    } else {
        echo json_encode(["success" => false, "message" => "Invalid verification type."]);
    }

} elseif ($method === 'POST') {
    // Handle submission by student or landlord
    $type = $_POST['type'] ?? 'student';
    $docPath = null;

    if (isset($_FILES['document']) && $_FILES['document']['error'] === UPLOAD_ERR_OK) {
        $uploadDir = '../uploads/verifications/';
        if (!file_exists($uploadDir)) {
            mkdir($uploadDir, 0777, true);
        }
        $ext = pathinfo($_FILES['document']['name'], PATHINFO_EXTENSION);
        $fileName = ($type === 'student' ? 'student_verif_' : 'landlord_verif_') . time() . '_' . rand(100, 999) . '.' . $ext;
        $targetFile = $uploadDir . $fileName;
        if (move_uploaded_file($_FILES['document']['tmp_name'], $targetFile)) {
            $docPath = 'uploads/verifications/' . $fileName;
        }
    } elseif (isset($_POST['document_path'])) {
        $docPath = trim($_POST['document_path']);
    }

    if ($type === 'student') {
        $userId = isset($_POST['user_id']) ? (int)$_POST['user_id'] : 0;
        if (!$userId) {
            echo json_encode(["success" => false, "message" => "user_id is required."]);
            exit();
        }
        if (!$docPath) {
            $docPath = 'uploads/verifications/default_student_id.jpg';
        }

        // Insert or update existing
        $stmt = $pdo->prepare("INSERT INTO student_verifications (user_id, student_id, student_id_path, verification_status, submitted_at)
                                VALUES (:user_id, :student_id, :path, 'PENDING', CURRENT_TIMESTAMP) RETURNING verification_id");
        $stmt->execute([
            'user_id' => $userId,
            'student_id' => $userId,
            'path' => $docPath
        ]);
        echo json_encode(["success" => true, "message" => "Student verification submitted successfully."]);

    } elseif ($type === 'landlord') {
        $landlordId = isset($_POST['landlord_id']) ? (int)$_POST['landlord_id'] : 0;
        if (!$landlordId) {
            echo json_encode(["success" => false, "message" => "landlord_id is required."]);
            exit();
        }
        if (!$docPath) {
            $docPath = 'uploads/verifications/default_landlord_doc.pdf';
        }

        $stmt = $pdo->prepare("INSERT INTO landlord_verifications (landlord_id, document_path, verification_status, submitted_at)
                                VALUES (:landlord_id, :path, 'PENDING', CURRENT_TIMESTAMP) RETURNING verification_id");
        $stmt->execute([
            'landlord_id' => $landlordId,
            'path' => $docPath
        ]);
        echo json_encode(["success" => true, "message" => "Landlord verification submitted successfully."]);
    }

} elseif ($method === 'PATCH') {
    $data = json_decode(file_get_contents("php://input"), true);
    if (!isset($data['verification_id']) || !isset($data['status'])) {
        echo json_encode(["success" => false, "message" => "verification_id and status are required."]);
        exit();
    }

    $verifId = (int)$data['verification_id'];
    $status = strtoupper(trim($data['status'])); // 'VERIFIED' or 'REJECTED'
    $rejectionReason = $data['rejection_reason'] ?? null;
    $reviewedBy = isset($data['reviewed_by']) ? (int)$data['reviewed_by'] : 1;
    $type = $data['type'] ?? 'student';

    if ($type === 'student') {
        $stmt = $pdo->prepare("UPDATE student_verifications
                                SET verification_status = :status,
                                    rejection_reason = :rejection_reason,
                                    reviewed_by = :reviewed_by,
                                    reviewed_at = CURRENT_TIMESTAMP
                                WHERE verification_id = :id");
        $stmt->execute([
            'status' => $status,
            'rejection_reason' => $status === 'REJECTED' ? $rejectionReason : null,
            'reviewed_by' => $reviewedBy,
            'id' => $verifId
        ]);

        // Also create notification for student
        $st = $pdo->prepare("SELECT user_id FROM student_verifications WHERE verification_id = :id");
        $st->execute(['id' => $verifId]);
        $uid = $st->fetchColumn();
        if ($uid) {
            $notifTitle = $status === 'VERIFIED' ? "Verification Approved" : "Verification Rejected";
            $notifMsg = $status === 'VERIFIED' ? "Congratulations! Your student ID has been verified by the administrator." : "Your verification request was rejected: " . ($rejectionReason ?: 'Please re-submit a valid ID.');
            $nStmt = $pdo->prepare("INSERT INTO notifications (user_id, title, message, type, is_read, created_at) VALUES (:u, :t, :m, 'SYSTEM', FALSE, CURRENT_TIMESTAMP)");
            $nStmt->execute(['u' => $uid, 't' => $notifTitle, 'm' => $notifMsg]);
        }

        echo json_encode(["success" => true, "message" => "Student verification updated successfully."]);

    } elseif ($type === 'landlord') {
        $stmt = $pdo->prepare("UPDATE landlord_verifications
                                SET verification_status = :status,
                                    rejection_reason = :rejection_reason,
                                    reviewed_by = :reviewed_by,
                                    reviewed_at = CURRENT_TIMESTAMP
                                WHERE verification_id = :id");
        $stmt->execute([
            'status' => $status,
            'rejection_reason' => $status === 'REJECTED' ? $rejectionReason : null,
            'reviewed_by' => $reviewedBy,
            'id' => $verifId
        ]);

        // Also create notification for landlord
        $st = $pdo->prepare("SELECT landlord_id FROM landlord_verifications WHERE verification_id = :id");
        $st->execute(['id' => $verifId]);
        $lid = $st->fetchColumn();
        if ($lid) {
            $notifTitle = $status === 'VERIFIED' ? "Landlord Verification Approved" : "Landlord Verification Rejected";
            $notifMsg = $status === 'VERIFIED' ? "Congratulations! Your landlord verification documents have been approved." : "Your landlord verification was rejected: " . ($rejectionReason ?: 'Please re-submit valid documents.');
            $nStmt = $pdo->prepare("INSERT INTO notifications (user_id, title, message, type, is_read, created_at) VALUES (:u, :t, :m, 'SYSTEM', FALSE, CURRENT_TIMESTAMP)");
            $nStmt->execute(['u' => $lid, 't' => $notifTitle, 'm' => $notifMsg]);
        }

        echo json_encode(["success" => true, "message" => "Landlord verification updated successfully."]);
    }
}
?>
