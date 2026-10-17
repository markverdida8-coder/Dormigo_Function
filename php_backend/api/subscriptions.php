<?php
session_start();
require_once 'db.php';

function respond(int $code, array $data): void { http_response_code($code); echo json_encode($data); exit(); }
if (!isset($_SESSION['admin_logged_in']) || $_SESSION['admin_logged_in'] !== true || strtoupper($_SESSION['admin_user_type'] ?? '') !== 'ADMIN') {
    respond(401, ['success' => false, 'message' => 'Unauthorized.']);
}
$adminId = (int)($_SESSION['admin_user_id'] ?? 0);
$method = $_SERVER['REQUEST_METHOD'] ?? '';
if ($method === 'GET') {
    $status = strtoupper(trim((string)($_GET['status'] ?? '')));
    $where = ''; $params = [];
    if ($status !== '') { $where = ' WHERE s.status = :status'; $params['status'] = $status; }
    $stmt = $pdo->prepare('SELECT s.*, u.full_name AS landlord_name, u.email AS landlord_email FROM landlord_subscriptions s JOIN users u ON u.user_id = s.landlord_id' . $where . ' ORDER BY s.submitted_at DESC');
    $stmt->execute($params);
    respond(200, ['success' => true, 'data' => $stmt->fetchAll()]);
}
if ($method !== 'PATCH') respond(405, ['success' => false, 'message' => 'Method not allowed.']);
$data = json_decode(file_get_contents('php://input'), true) ?? [];
$id = filter_var($data['subscription_id'] ?? null, FILTER_VALIDATE_INT);
$status = strtoupper(trim((string)($data['status'] ?? '')));
$reason = trim((string)($data['rejection_reason'] ?? ''));
if (!$id || !in_array($status, ['SUCCESSFUL', 'REJECTED'], true) || ($status === 'REJECTED' && $reason === '')) respond(400, ['success' => false, 'message' => 'A valid decision and rejection reason are required.']);
try {
    $pdo->beginTransaction();
    $lookup = $pdo->prepare('SELECT * FROM landlord_subscriptions WHERE subscription_id = :id FOR UPDATE');
    $lookup->execute(['id' => $id]); $row = $lookup->fetch();
    if (!$row || $row['status'] !== 'PENDING') { $pdo->rollBack(); respond(409, ['success' => false, 'message' => 'This payment has already been reviewed.']); }
    $start = null; $expiry = null;
    if ($status === 'SUCCESSFUL') {
        $active = $pdo->prepare("SELECT expires_at FROM landlord_subscriptions WHERE landlord_id = :landlord_id AND status = 'SUCCESSFUL' AND expires_at > CURRENT_DATE ORDER BY expires_at DESC LIMIT 1 FOR UPDATE");
        $active->execute(['landlord_id' => $row['landlord_id']]);
        $start = $active->fetchColumn() ?: date('Y-m-d');
        $expiry = (new DateTimeImmutable($start))->modify('+1 month')->format('Y-m-d');
    }
    $update = $pdo->prepare("UPDATE landlord_subscriptions SET status = :status, reviewed_by = :admin_id, reviewed_at = CURRENT_TIMESTAMP, rejection_reason = :reason, starts_at = :start, expires_at = :expiry, updated_at = CURRENT_TIMESTAMP WHERE subscription_id = :id AND status = 'PENDING'");
    $update->execute(['status' => $status, 'admin_id' => $adminId, 'reason' => $status === 'REJECTED' ? $reason : null, 'start' => $start, 'expiry' => $expiry, 'id' => $id]);
    $event = $pdo->prepare("INSERT INTO landlord_subscription_review_events (subscription_id, previous_status, new_status, reviewed_by, reason) VALUES (:id, 'PENDING', :status, :admin_id, :reason)");
    $event->execute(['id' => $id, 'status' => $status, 'admin_id' => $adminId, 'reason' => $status === 'REJECTED' ? $reason : null]);
    $pdo->commit();
    respond(200, ['success' => true, 'status' => $status, 'starts_at' => $start, 'expires_at' => $expiry]);
} catch (Throwable $e) { if ($pdo->inTransaction()) $pdo->rollBack(); error_log('Subscription review failed: ' . get_class($e)); respond(500, ['success' => false, 'message' => 'Unable to review subscription payment.']); }
