<?php
require_once 'db.php';
require_once 'auth_helper.php';

$method = $_SERVER['REQUEST_METHOD'];
if ($method !== 'POST') {
    echo json_encode(["success" => false, "message" => "Method not allowed."]);
    exit();
}

$authUser = authenticateUser($pdo);
if (strtoupper(trim((string)($authUser['user_type'] ?? ''))) !== 'LANDLORD') {
    http_response_code(403);
    echo json_encode(["success" => false, "message" => "Forbidden: Only landlords can submit landlord verification."]);
    exit();
}
$landlord_id = (int)$authUser['user_id'];

$uploadDir = '../uploads/verifications/';
if (!file_exists($uploadDir)) {
    mkdir($uploadDir, 0777, true);
}

$document_path = null;
if (isset($_FILES['document']) && $_FILES['document']['error'] === UPLOAD_ERR_OK) {
    $fileName = 'landlord_verif_' . $landlord_id . '_' . time() . '.pdf';
    $targetPath = $uploadDir . $fileName;
    if (move_uploaded_file($_FILES['document']['tmp_name'], $targetPath)) {
        $document_path = 'uploads/verifications/' . $fileName;
    }
}

if (empty($document_path)) {
    echo json_encode(["success" => false, "message" => "Failed to upload verification document."]);
    exit();
}

try {
    $pdo->beginTransaction();
    $stmt = $pdo->prepare("SELECT verification_id FROM landlord_verifications WHERE landlord_id = :landlord_id");
    $stmt->execute(['landlord_id' => $landlord_id]);
    $existing = $stmt->fetch();

    if ($existing) {
        $update = $pdo->prepare("UPDATE landlord_verifications SET verification_status = 'PENDING', rejection_reason = NULL, reviewed_by = NULL, reviewed_at = NULL, document_path = :path, submitted_at = CURRENT_TIMESTAMP WHERE landlord_id = :landlord_id");
        $update->execute(['path' => $document_path, 'landlord_id' => $landlord_id]);
        $verificationId = $existing['verification_id'];
    } else {
        $insert = $pdo->prepare("INSERT INTO landlord_verifications (landlord_id, document_path, verification_status) VALUES (:landlord_id, :path, 'PENDING')");
        $insert->execute(['landlord_id' => $landlord_id, 'path' => $document_path]);
        $verificationId = $pdo->lastInsertId();
    }

    // Existing listings remain published but lose their verified badge while review is pending.
    $pdo->prepare(
        "INSERT INTO boarding_house_verification (house_id, verification_status, submitted_at)
         SELECT house_id, 'UNVERIFIED', CURRENT_TIMESTAMP
         FROM boarding_houses
         WHERE landlord_id = :landlord_id
         ON CONFLICT DO NOTHING"
    )->execute(['landlord_id' => $landlord_id]);
    $pdo->prepare(
        "UPDATE boarding_house_verification bhv
         SET verification_status = 'UNVERIFIED', rejection_reason = NULL,
             reviewed_by = NULL, reviewed_at = NULL
         FROM boarding_houses bh
         WHERE bhv.house_id = bh.house_id AND bh.landlord_id = :landlord_id"
    )->execute(['landlord_id' => $landlord_id]);

    $nStmt = $pdo->prepare("INSERT INTO notifications (user_id, title, message, type, reference_id) VALUES (3, 'New Landlord Verification Request', 'Landlord submitted a verification request.', 'VERIFICATION', ?)");
    $nStmt->execute([$verificationId]);

    $pdo->commit();
    echo json_encode(["success" => true, "message" => "Verification document submitted successfully. Status: PENDING"]);
} catch (Throwable $e) {
    if ($pdo->inTransaction()) {
        $pdo->rollBack();
    }
    error_log('Landlord verification submission failed: ' . get_class($e));
    http_response_code(500);
    echo json_encode(["success" => false, "message" => "Unable to submit the verification document."]);
}
?>
