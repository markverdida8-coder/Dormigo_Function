<?php
require_once __DIR__ . '/db.php';
require_once __DIR__ . '/auth_helper.php';
require_once __DIR__ . '/subscription_model.php';

header('Content-Type: application/json; charset=UTF-8');

function subscriptionLoadConfig(PDO $pdo): array
{
    $stmt = $pdo->query(
        'SELECT qr_enabled, qr_code_path, account_name
         FROM subscription_payment_config WHERE config_id = 1'
    );
    $config = $stmt->fetch(PDO::FETCH_ASSOC) ?: [];

    return [
        'qr_enabled' => in_array($config['qr_enabled'] ?? false, [true, 't', '1', 1], true),
        'qr_code_path' => $config['qr_code_path'] ?? null,
        'account_name' => $config['account_name'] ?? null
    ];
}

function subscriptionHistory(PDO $pdo, int $landlordId): array
{
    $stmt = $pdo->prepare(
        'SELECT subscription_id, plan_name, amount, currency, payment_method, transaction_ref,
                proof_path, status, submitted_at, reviewed_at, rejection_reason, starts_at, expires_at
         FROM landlord_subscriptions
         WHERE landlord_id = :landlord_id
         ORDER BY submitted_at DESC, subscription_id DESC'
    );
    $stmt->execute(['landlord_id' => $landlordId]);
    return $stmt->fetchAll(PDO::FETCH_ASSOC);
}

function subscriptionSummary(?array $activeSubscription, array $history): array
{
    $latestPayment = $history[0] ?? null;
    if ($activeSubscription !== null) {
        $expiresAt = new DateTimeImmutable($activeSubscription['expires_at']);
        $today = new DateTimeImmutable('today');
        return [
            'current_status' => 'ACTIVE',
            'days_remaining' => max(0, (int)$today->diff($expiresAt)->format('%r%a')),
            'latest_payment' => $latestPayment,
            'admin_verification_status' => 'APPROVED'
        ];
    }

    if ($latestPayment === null) {
        return [
            'current_status' => 'NO_SUBSCRIPTION',
            'days_remaining' => null,
            'latest_payment' => null,
            'admin_verification_status' => 'NOT_SUBMITTED'
        ];
    }

    $latestStatus = strtoupper((string)$latestPayment['status']);
    return [
        'current_status' => $latestStatus === 'SUCCESSFUL' ? 'EXPIRED' : $latestStatus,
        'days_remaining' => null,
        'latest_payment' => $latestPayment,
        'admin_verification_status' => $latestStatus
    ];
}

$method = $_SERVER['REQUEST_METHOD'] ?? '';
if ($method === 'OPTIONS') {
    http_response_code(200);
    exit();
}
if (!in_array($method, ['GET', 'POST'], true)) {
    header('Allow: GET, POST, OPTIONS');
    subscriptionRespond(405, ['success' => false, 'message' => 'Method not allowed.']);
}

if (!subscriptionSchemaReady($pdo)) {
    subscriptionRespond(503, ['success' => false, 'message' => 'Subscription service is unavailable.']);
}

$user = authenticateUser($pdo);
$landlordId = requireSubscriptionRole($user, 'LANDLORD');

if ($method === 'GET') {
    $preference = $pdo->prepare(
        'SELECT introduction_acknowledged_at FROM landlord_subscription_preferences WHERE landlord_id = :landlord_id'
    );
    $preference->execute(['landlord_id' => $landlordId]);
    $introductionAcknowledged = $preference->fetchColumn() !== false;

    $activeSubscription = activeLandlordSubscription($pdo, $landlordId);
    $history = subscriptionHistory($pdo, $landlordId);
    subscriptionRespond(200, [
        'success' => true,
        'introduction_acknowledged' => $introductionAcknowledged,
        'active_subscription' => $activeSubscription,
        'subscription_summary' => subscriptionSummary($activeSubscription, $history),
        'payment_config' => subscriptionLoadConfig($pdo),
        'history' => $history
    ]);
}

$action = $_POST['action'] ?? '';
if ($action === 'acknowledge_introduction') {
    $stmt = $pdo->prepare(
        'INSERT INTO landlord_subscription_preferences (landlord_id, introduction_acknowledged_at, updated_at)
         VALUES (:landlord_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
         ON CONFLICT (landlord_id) DO UPDATE
         SET introduction_acknowledged_at = COALESCE(landlord_subscription_preferences.introduction_acknowledged_at, CURRENT_TIMESTAMP),
             updated_at = CURRENT_TIMESTAMP'
    );
    $stmt->execute(['landlord_id' => $landlordId]);
    subscriptionRespond(200, ['success' => true]);
}

$paymentMethod = strtoupper(trim((string)($_POST['payment_method'] ?? '')));
if ($paymentMethod !== 'QR') {
    subscriptionRespond(400, ['success' => false, 'message' => 'Only QR Payment is accepted for new landlord subscription payments.']);
}
$config = subscriptionLoadConfig($pdo);
if (!$config['qr_enabled'] || empty($config['qr_code_path'])) {
    subscriptionRespond(409, ['success' => false, 'message' => 'Dormigo subscription QR Payment is not available yet.']);
}

$transactionRef = trim((string)($_POST['transaction_ref'] ?? ''));
$proofPath = null;
if ($transactionRef === '' || !isset($_FILES['proof_image'])) {
    subscriptionRespond(400, ['success' => false, 'message' => 'A transaction reference and payment proof are required for QR Payment.']);
}
$proof = $_FILES['proof_image'];
if (($proof['error'] ?? UPLOAD_ERR_NO_FILE) !== UPLOAD_ERR_OK || (int)($proof['size'] ?? 0) > 10 * 1024 * 1024 || !is_uploaded_file($proof['tmp_name'] ?? '')) {
    subscriptionRespond(400, ['success' => false, 'message' => 'Upload a valid payment proof image up to 10MB.']);
}
$finfo = new finfo(FILEINFO_MIME_TYPE);
$mime = $finfo->file($proof['tmp_name']);
$extensions = ['image/jpeg' => 'jpg', 'image/png' => 'png', 'image/webp' => 'webp'];
if (!isset($extensions[$mime])) {
    subscriptionRespond(400, ['success' => false, 'message' => 'Payment proof must be a JPG, PNG, or WEBP image.']);
}
$directory = __DIR__ . '/../uploads/subscription_proofs/';
if (!is_dir($directory) && !mkdir($directory, 0750, true) && !is_dir($directory)) {
    subscriptionRespond(500, ['success' => false, 'message' => 'Unable to store payment proof.']);
}
$filename = 'subscription_' . bin2hex(random_bytes(16)) . '.' . $extensions[$mime];
if (!move_uploaded_file($proof['tmp_name'], $directory . $filename)) {
    subscriptionRespond(500, ['success' => false, 'message' => 'Unable to store payment proof.']);
}
$proofPath = 'uploads/subscription_proofs/' . $filename;

try {
    $pdo->beginTransaction();
    $plan = $pdo->query(
        "SELECT plan_id, code, name, amount, currency, duration_months
         FROM subscription_plans
         WHERE code = 'LANDLORD_MONTHLY_399' AND is_active = TRUE
         FOR SHARE"
    )->fetch(PDO::FETCH_ASSOC);
    if (!$plan) {
        throw new RuntimeException('The monthly subscription plan is unavailable.');
    }
    $pending = $pdo->prepare(
        "SELECT subscription_id FROM landlord_subscriptions
         WHERE landlord_id = :landlord_id AND status = 'PENDING' FOR UPDATE"
    );
    $pending->execute(['landlord_id' => $landlordId]);
    if ($pending->fetch()) {
        $pdo->rollBack();
        subscriptionRespond(409, ['success' => false, 'message' => 'A subscription payment is already awaiting Admin review.']);
    }
    $insert = $pdo->prepare(
        'INSERT INTO landlord_subscriptions
         (landlord_id, plan_id, plan_code, plan_name, amount, currency, duration_months, payment_method, transaction_ref, proof_path, status)
         VALUES (:landlord_id, :plan_id, :plan_code, :plan_name, :amount, :currency, :duration_months, :payment_method, :transaction_ref, :proof_path, \'PENDING\')
         RETURNING subscription_id'
    );
    $insert->execute([
        'landlord_id' => $landlordId,
        'plan_id' => $plan['plan_id'],
        'plan_code' => $plan['code'],
        'plan_name' => $plan['name'],
        'amount' => $plan['amount'],
        'currency' => $plan['currency'],
        'duration_months' => $plan['duration_months'],
        'payment_method' => $paymentMethod,
        'transaction_ref' => $transactionRef !== '' ? $transactionRef : null,
        'proof_path' => $proofPath
    ]);
    $subscriptionId = (int)$insert->fetchColumn();
    $pdo->commit();
    subscriptionRespond(201, ['success' => true, 'subscription_id' => $subscriptionId, 'status' => 'PENDING']);
} catch (Throwable $e) {
    if ($pdo->inTransaction()) {
        $pdo->rollBack();
    }
    if ($proofPath !== null) {
        @unlink(__DIR__ . '/../' . $proofPath);
    }
    if ($e instanceof PDOException && $e->getCode() === '23505') {
        subscriptionRespond(409, ['success' => false, 'message' => 'This subscription payment is already awaiting review or uses an existing transaction reference.']);
    }
    error_log('Subscription payment submission failed: ' . get_class($e));
    subscriptionRespond(500, ['success' => false, 'message' => 'Unable to submit subscription payment.']);
}
