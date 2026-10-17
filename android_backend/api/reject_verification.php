<?php
require_once 'db.php';
require_once 'auth_helper.php';

function landlordRejectionRespond(int $statusCode, array $payload): void {
    http_response_code($statusCode);
    echo json_encode($payload);
    exit();
}

$method = $_SERVER['REQUEST_METHOD'];
if ($method !== 'POST' && $method !== 'PATCH') {
    echo json_encode(["success" => false, "message" => "Method not allowed."]);
    exit();
}

$authUser = authenticateUser($pdo);
if (strtoupper((string)($authUser['user_type'] ?? '')) !== 'ADMIN') {
    landlordRejectionRespond(403, ["success" => false, "message" => "Forbidden: Administrator access is required."]);
}
$adminId = (int)$authUser['user_id'];

$data = json_decode(file_get_contents("php://input"), true);
$verification_id = isset($data['verification_id']) ? (int)$data['verification_id'] : 0;
$rejection_reason = isset($data['rejection_reason']) && is_string($data['rejection_reason']) ? trim($data['rejection_reason']) : '';

if ($verification_id <= 0 || $rejection_reason === '') {
    landlordRejectionRespond(400, ["success" => false, "message" => "Verification ID and rejection reason are required."]);
}

try {
    $pdo->beginTransaction();

    $stmt = $pdo->prepare("UPDATE landlord_verifications SET verification_status = 'REJECTED', rejection_reason = :reason, reviewed_by = :admin_id, reviewed_at = CURRENT_TIMESTAMP WHERE verification_id = :verification_id RETURNING landlord_id");
    $stmt->execute(['reason' => $rejection_reason, 'admin_id' => $adminId, 'verification_id' => $verification_id]);
    $landlordId = (int)$stmt->fetchColumn();

    if ($landlordId <= 0) {
        $pdo->rollBack();
        landlordRejectionRespond(404, ["success" => false, "message" => "Landlord verification not found."]);
    }

    $insertProperties = $pdo->prepare(
        "INSERT INTO boarding_house_verification (house_id, verification_status, rejection_reason, reviewed_by, reviewed_at, submitted_at)
         SELECT house_id, 'REJECTED', :reason, :admin_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
         FROM boarding_houses
         WHERE landlord_id = :landlord_id
         ON CONFLICT DO NOTHING"
    );
    $insertProperties->execute(['reason' => $rejection_reason, 'admin_id' => $adminId, 'landlord_id' => $landlordId]);

    $syncProperties = $pdo->prepare(
        "UPDATE boarding_house_verification bhv
         SET verification_status = 'REJECTED', rejection_reason = :reason,
             reviewed_by = :admin_id, reviewed_at = CURRENT_TIMESTAMP
         FROM boarding_houses bh
         WHERE bhv.house_id = bh.house_id AND bh.landlord_id = :landlord_id"
    );
    $syncProperties->execute(['reason' => $rejection_reason, 'admin_id' => $adminId, 'landlord_id' => $landlordId]);

    $deactivateProperties = $pdo->prepare(
        "UPDATE boarding_houses
         SET status = 'INACTIVE'::house_status_enum
         WHERE landlord_id = :landlord_id"
    );
    $deactivateProperties->execute(['landlord_id' => $landlordId]);

    $nStmt = $pdo->prepare("INSERT INTO notifications (user_id, title, message, type, reference_id) VALUES (?, 'Verification Rejected', 'Your landlord verification was rejected. Reason: ' || ?, 'VERIFICATION', ?)");
    $nStmt->execute([$landlordId, $rejection_reason, $verification_id]);

    $pdo->commit();
    echo json_encode(["success" => true, "message" => "Landlord verification rejected. Owned properties are now inactive.", "landlord_id" => $landlordId]);
} catch (Throwable $e) {
    if ($pdo->inTransaction()) {
        $pdo->rollBack();
    }
    error_log('Landlord rejection failed: ' . get_class($e));
    echo json_encode(["success" => false, "message" => "Unable to reject landlord verification."]);
}
?>
