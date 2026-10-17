<?php
require_once 'db.php';
require_once 'auth_helper.php';

function propertyVerificationReviewRespond(int $statusCode, array $payload): void {
    http_response_code($statusCode);
    echo json_encode($payload);
    exit();
}

function authenticatePropertyVerificationAdmin(PDO $pdo): array {
    if (getBearerToken() !== null) {
        return authenticateUser($pdo);
    }

    if (session_status() === PHP_SESSION_NONE) {
        session_start();
    }
    if (
        ($_SESSION['admin_logged_in'] ?? false) !== true
        || strtoupper((string)($_SESSION['admin_user_type'] ?? '')) !== 'ADMIN'
    ) {
        propertyVerificationReviewRespond(401, [
            'success' => false,
            'message' => 'Unauthorized: Administrator authentication is required.'
        ]);
    }

    $adminStmt = $pdo->prepare(
        "SELECT user_id, user_type, email, full_name
         FROM users
         WHERE user_id = :user_id
           AND deactivated_at IS NULL"
    );
    $adminStmt->execute(['user_id' => (int)$_SESSION['admin_user_id']]);
    $admin = $adminStmt->fetch(PDO::FETCH_ASSOC);
    if (!$admin || strtoupper((string)$admin['user_type']) !== 'ADMIN') {
        propertyVerificationReviewRespond(401, [
            'success' => false,
            'message' => 'Unauthorized: Administrator session is invalid.'
        ]);
    }
    return $admin;
}

if ($_SERVER['REQUEST_METHOD'] !== 'PATCH') {
    propertyVerificationReviewRespond(405, [
        'success' => false,
        'message' => 'Method not allowed.'
    ]);
}

$authUser = authenticatePropertyVerificationAdmin($pdo);
if (strtoupper((string)($authUser['user_type'] ?? '')) !== 'ADMIN') {
    propertyVerificationReviewRespond(403, [
        'success' => false,
        'message' => 'Forbidden: Administrator access is required.'
    ]);
}

$data = json_decode(file_get_contents('php://input'), true);
$houseId = isset($data['house_id']) ? (int)$data['house_id'] : 0;
$status = strtoupper(trim((string)($data['status'] ?? '')));
$rejectionReason = trim((string)($data['rejection_reason'] ?? ''));

if ($houseId <= 0 || !in_array($status, ['VERIFIED', 'REJECTED'], true)) {
    propertyVerificationReviewRespond(400, [
        'success' => false,
        'message' => 'A valid house_id and VERIFIED or REJECTED status are required.'
    ]);
}
if ($status === 'REJECTED' && $rejectionReason === '') {
    propertyVerificationReviewRespond(400, [
        'success' => false,
        'message' => 'A rejection reason is required.'
    ]);
}

try {
    $pdo->beginTransaction();

    $houseStmt = $pdo->prepare(
        'SELECT house_id, landlord_id, house_name
         FROM boarding_houses
         WHERE house_id = :house_id
         FOR UPDATE'
    );
    $houseStmt->execute(['house_id' => $houseId]);
    $house = $houseStmt->fetch(PDO::FETCH_ASSOC);
    if (!$house) {
        $pdo->rollBack();
        propertyVerificationReviewRespond(404, [
            'success' => false,
            'message' => 'Boarding house not found.'
        ]);
    }

    $verificationStmt = $pdo->prepare(
        'SELECT verification_id
         FROM boarding_house_verification
         WHERE house_id = :house_id
         ORDER BY COALESCE(reviewed_at, submitted_at) DESC, verification_id DESC
         LIMIT 1
         FOR UPDATE'
    );
    $verificationStmt->execute(['house_id' => $houseId]);
    $verificationId = (int)$verificationStmt->fetchColumn();

    if ($verificationId > 0) {
        $update = $pdo->prepare(
            'UPDATE boarding_house_verification
             SET verification_status = :status,
                 rejection_reason = :rejection_reason,
                 reviewed_by = :reviewed_by,
                 reviewed_at = CURRENT_TIMESTAMP
             WHERE verification_id = :verification_id'
        );
        $update->execute([
            'status' => $status,
            'rejection_reason' => $status === 'REJECTED' ? $rejectionReason : null,
            'reviewed_by' => (int)$authUser['user_id'],
            'verification_id' => $verificationId
        ]);
    } else {
        $insert = $pdo->prepare(
            'INSERT INTO boarding_house_verification (
                house_id, verification_status, rejection_reason, reviewed_by, reviewed_at, submitted_at
             ) VALUES (
                :house_id, :status, :rejection_reason, :reviewed_by, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
             ) RETURNING verification_id'
        );
        $insert->execute([
            'house_id' => $houseId,
            'status' => $status,
            'rejection_reason' => $status === 'REJECTED' ? $rejectionReason : null,
            'reviewed_by' => (int)$authUser['user_id']
        ]);
        $verificationId = (int)$insert->fetchColumn();
    }

    $notification = $pdo->prepare(
        "INSERT INTO notifications (user_id, title, message, type, reference_id)
         VALUES (:user_id, :title, :message, 'VERIFICATION', :reference_id)"
    );
    $notification->execute([
        'user_id' => (int)$house['landlord_id'],
        'title' => $status === 'VERIFIED'
            ? 'Boarding House Verified'
            : 'Boarding House Verification Rejected',
        'message' => $status === 'VERIFIED'
            ? "Your property '{$house['house_name']}' has been verified by an administrator."
            : "Your property '{$house['house_name']}' was rejected: {$rejectionReason}",
        'reference_id' => $verificationId
    ]);

    $pdo->commit();
    echo json_encode([
        'success' => true,
        'message' => 'Boarding house verification updated successfully.',
        'house_id' => $houseId,
        'verification_status' => $status
    ]);
} catch (Throwable $e) {
    if ($pdo->inTransaction()) {
        $pdo->rollBack();
    }
    error_log('Property verification review failed: ' . get_class($e));
    propertyVerificationReviewRespond(500, [
        'success' => false,
        'message' => 'Unable to update boarding house verification.'
    ]);
}
