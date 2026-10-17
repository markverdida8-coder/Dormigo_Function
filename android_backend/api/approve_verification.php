<?php
require_once 'db.php';
require_once 'auth_helper.php';

function landlordApprovalRespond(int $statusCode, array $payload): void {
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
    landlordApprovalRespond(403, ["success" => false, "message" => "Forbidden: Administrator access is required."]);
}
$adminId = (int)$authUser['user_id'];

$data = json_decode(file_get_contents("php://input"), true);
$verification_id = isset($data['verification_id']) ? (int)$data['verification_id'] : 0;

if ($verification_id <= 0) {
    landlordApprovalRespond(400, ["success" => false, "message" => "A valid verification ID is required."]);
}

try {
    $pdo->beginTransaction();

    $stmt = $pdo->prepare("UPDATE landlord_verifications SET verification_status = 'VERIFIED', rejection_reason = NULL, reviewed_by = :admin_id, reviewed_at = CURRENT_TIMESTAMP WHERE verification_id = :verification_id RETURNING landlord_id");
    $stmt->execute(['admin_id' => $adminId, 'verification_id' => $verification_id]);
    $landlordId = (int)$stmt->fetchColumn();

    if ($landlordId <= 0) {
        $pdo->rollBack();
        landlordApprovalRespond(404, ["success" => false, "message" => "Landlord verification not found."]);
    }

    // Property verification is derived from the approved landlord account.
    $insertProperties = $pdo->prepare(
        "INSERT INTO boarding_house_verification (house_id, verification_status, reviewed_by, reviewed_at, submitted_at)
         SELECT house_id, 'VERIFIED', :admin_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
         FROM boarding_houses
         WHERE landlord_id = :landlord_id
         ON CONFLICT DO NOTHING"
    );
    $insertProperties->execute(['admin_id' => $adminId, 'landlord_id' => $landlordId]);

    $syncProperties = $pdo->prepare(
        "UPDATE boarding_house_verification bhv
         SET verification_status = 'VERIFIED', rejection_reason = NULL,
             reviewed_by = :admin_id, reviewed_at = CURRENT_TIMESTAMP
         FROM boarding_houses bh
         WHERE bhv.house_id = bh.house_id AND bh.landlord_id = :landlord_id"
    );
    $syncProperties->execute(['admin_id' => $adminId, 'landlord_id' => $landlordId]);

    $nStmt = $pdo->prepare("INSERT INTO notifications (user_id, title, message, type, reference_id) VALUES (?, 'Verification Approved', 'Your landlord verification has been approved. You are now a Verified Landlord and all your properties are verified.', 'VERIFICATION', ?)");
    $nStmt->execute([$landlordId, $verification_id]);

    $pdo->commit();
    echo json_encode(["success" => true, "message" => "Landlord verification approved successfully. All owned properties are now verified.", "landlord_id" => $landlordId]);
} catch (Throwable $e) {
    if ($pdo->inTransaction()) {
        $pdo->rollBack();
    }
    error_log('Landlord approval failed: ' . get_class($e));
    echo json_encode(["success" => false, "message" => "Unable to approve landlord verification."]);
}
?>
