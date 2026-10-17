<?php
require_once __DIR__ . '/db.php';
require_once __DIR__ . '/auth_helper.php';
require_once __DIR__ . '/subscription_model.php';

header('Content-Type: application/json; charset=UTF-8');
$method = $_SERVER['REQUEST_METHOD'] ?? '';
if ($method === 'OPTIONS') { http_response_code(200); exit(); }
if (!in_array($method, ['GET', 'PATCH'], true)) {
    header('Allow: GET, PATCH, OPTIONS');
    subscriptionRespond(405, ['success' => false, 'message' => 'Method not allowed.']);
}
if (!subscriptionSchemaReady($pdo)) {
    subscriptionRespond(503, ['success' => false, 'message' => 'Subscription service is unavailable.']);
}
$admin = authenticateUser($pdo);
$adminId = requireSubscriptionRole($admin, 'ADMIN');

if ($method === 'GET') {
    $status = strtoupper(trim((string)($_GET['status'] ?? '')));
    $where = '';
    $params = [];
    if ($status !== '') {
        if (!in_array($status, ['PENDING', 'SUCCESSFUL', 'REJECTED'], true)) {
            subscriptionRespond(400, ['success' => false, 'message' => 'Invalid subscription status.']);
        }
        $where = ' WHERE s.status = :status';
        $params['status'] = $status;
    }
    $stmt = $pdo->prepare(
        'SELECT s.*, u.full_name AS landlord_name, u.email AS landlord_email,
                reviewer.full_name AS reviewer_name
         FROM landlord_subscriptions s
         JOIN users u ON u.user_id = s.landlord_id
         LEFT JOIN users reviewer ON reviewer.user_id = s.reviewed_by' . $where .
        ' ORDER BY s.submitted_at DESC, s.subscription_id DESC'
    );
    $stmt->execute($params);
    subscriptionRespond(200, ['success' => true, 'data' => $stmt->fetchAll(PDO::FETCH_ASSOC)]);
}

$data = json_decode(file_get_contents('php://input'), true);
$subscriptionId = filter_var($data['subscription_id'] ?? null, FILTER_VALIDATE_INT);
$decision = strtoupper(trim((string)($data['status'] ?? '')));
$reason = trim((string)($data['rejection_reason'] ?? ''));
if (!$subscriptionId || !in_array($decision, ['SUCCESSFUL', 'REJECTED'], true)) {
    subscriptionRespond(400, ['success' => false, 'message' => 'A subscription and valid review decision are required.']);
}
if ($decision === 'REJECTED' && $reason === '') {
    subscriptionRespond(400, ['success' => false, 'message' => 'A rejection reason is required.']);
}

try {
    $pdo->beginTransaction();
    $lookup = $pdo->prepare('SELECT * FROM landlord_subscriptions WHERE subscription_id = :subscription_id FOR UPDATE');
    $lookup->execute(['subscription_id' => $subscriptionId]);
    $subscription = $lookup->fetch(PDO::FETCH_ASSOC);
    if (!$subscription) {
        $pdo->rollBack();
        subscriptionRespond(404, ['success' => false, 'message' => 'Subscription payment not found.']);
    }
    if ($subscription['status'] !== 'PENDING') {
        $pdo->rollBack();
        subscriptionRespond(409, ['success' => false, 'message' => 'This subscription payment has already been reviewed.']);
    }
    $startsAt = null;
    $expiresAt = null;
    if ($decision === 'SUCCESSFUL') {
        $active = $pdo->prepare(
            "SELECT expires_at FROM landlord_subscriptions
             WHERE landlord_id = :landlord_id AND status = 'SUCCESSFUL' AND expires_at > CURRENT_DATE
             ORDER BY expires_at DESC LIMIT 1 FOR UPDATE"
        );
        $active->execute(['landlord_id' => $subscription['landlord_id']]);
        $existingExpiry = $active->fetchColumn();
        $startsAt = $existingExpiry ?: date('Y-m-d');
        $expiresAt = (new DateTimeImmutable($startsAt))->modify('+' . (int)$subscription['duration_months'] . ' month')->format('Y-m-d');
    }
    $update = $pdo->prepare(
        'UPDATE landlord_subscriptions
         SET status = :status, reviewed_by = :reviewed_by, reviewed_at = CURRENT_TIMESTAMP,
             rejection_reason = :rejection_reason, starts_at = :starts_at, expires_at = :expires_at, updated_at = CURRENT_TIMESTAMP
         WHERE subscription_id = :subscription_id AND status = \'PENDING\''
    );
    $update->execute([
        'status' => $decision,
        'reviewed_by' => $adminId,
        'rejection_reason' => $decision === 'REJECTED' ? $reason : null,
        'starts_at' => $startsAt,
        'expires_at' => $expiresAt,
        'subscription_id' => $subscriptionId
    ]);
    if ($update->rowCount() !== 1) {
        $pdo->rollBack();
        subscriptionRespond(409, ['success' => false, 'message' => 'This subscription payment has already been reviewed.']);
    }
    $event = $pdo->prepare(
        'INSERT INTO landlord_subscription_review_events (subscription_id, previous_status, new_status, reviewed_by, reason)
         VALUES (:subscription_id, \'PENDING\', :new_status, :reviewed_by, :reason)'
    );
    $event->execute([
        'subscription_id' => $subscriptionId,
        'new_status' => $decision,
        'reviewed_by' => $adminId,
        'reason' => $decision === 'REJECTED' ? $reason : null
    ]);
    $notification = $pdo->prepare(
        "INSERT INTO notifications (user_id, title, message, type, reference_id)
         VALUES (:user_id, :title, :message, 'PAYMENT', :reference_id)"
    );
    $notification->execute([
        'user_id' => $subscription['landlord_id'],
        'title' => $decision === 'SUCCESSFUL' ? 'Subscription Activated' : 'Subscription Payment Rejected',
        'message' => $decision === 'SUCCESSFUL'
            ? 'Your Dormigo monthly subscription is active until ' . $expiresAt . '.'
            : 'Your subscription payment was rejected: ' . $reason,
        'reference_id' => $subscriptionId
    ]);
    $pdo->commit();
    subscriptionRespond(200, ['success' => true, 'status' => $decision, 'starts_at' => $startsAt, 'expires_at' => $expiresAt]);
} catch (Throwable $e) {
    if ($pdo->inTransaction()) { $pdo->rollBack(); }
    error_log('Subscription review failed: ' . get_class($e));
    subscriptionRespond(500, ['success' => false, 'message' => 'Unable to review subscription payment.']);
}
