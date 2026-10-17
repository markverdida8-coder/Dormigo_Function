<?php
session_start();
require_once 'db.php';
function configRespond(int $code, array $data): void { http_response_code($code); echo json_encode($data); exit(); }
if (!isset($_SESSION['admin_logged_in']) || $_SESSION['admin_logged_in'] !== true || strtoupper($_SESSION['admin_user_type'] ?? '') !== 'ADMIN') configRespond(401, ['success' => false, 'message' => 'Unauthorized.']);
$method = $_SERVER['REQUEST_METHOD'] ?? '';
if ($method === 'GET') {
    $config = $pdo->query('SELECT cash_enabled, qr_enabled, qr_code_path, account_name, updated_at FROM subscription_payment_config WHERE config_id = 1')->fetch();
    configRespond(200, ['success' => true, 'data' => $config ?: null]);
}
if ($method !== 'POST') configRespond(405, ['success' => false, 'message' => 'Method not allowed.']);
$accountName = trim((string)($_POST['account_name'] ?? ''));
if ($accountName === '' || !isset($_FILES['qr_code']) || ($_FILES['qr_code']['error'] ?? UPLOAD_ERR_NO_FILE) !== UPLOAD_ERR_OK || !is_uploaded_file($_FILES['qr_code']['tmp_name'])) configRespond(400, ['success' => false, 'message' => 'An account name and QR image are required.']);
if ((int)$_FILES['qr_code']['size'] > 10 * 1024 * 1024) configRespond(400, ['success' => false, 'message' => 'QR image exceeds 10MB.']);
$mime = (new finfo(FILEINFO_MIME_TYPE))->file($_FILES['qr_code']['tmp_name']);
$extensions = ['image/jpeg' => 'jpg', 'image/png' => 'png', 'image/webp' => 'webp'];
if (!isset($extensions[$mime])) configRespond(400, ['success' => false, 'message' => 'QR image must be JPG, PNG, or WEBP.']);
$directory = 'C:/xampp/htdocs/Dormigo_Backend/uploads/subscription_qr/';
if (!is_dir($directory) && !mkdir($directory, 0750, true) && !is_dir($directory)) configRespond(500, ['success' => false, 'message' => 'Unable to prepare QR storage.']);
$filename = 'dormigo_subscription_qr_' . bin2hex(random_bytes(16)) . '.' . $extensions[$mime];
if (!move_uploaded_file($_FILES['qr_code']['tmp_name'], $directory . $filename)) configRespond(500, ['success' => false, 'message' => 'Unable to store QR image.']);
$path = 'uploads/subscription_qr/' . $filename;
$stmt = $pdo->prepare('UPDATE subscription_payment_config SET qr_enabled = TRUE, qr_code_path = :path, account_name = :account_name, updated_by = :admin_id, updated_at = CURRENT_TIMESTAMP WHERE config_id = 1');
$stmt->execute(['path' => $path, 'account_name' => $accountName, 'admin_id' => (int)$_SESSION['admin_user_id']]);
configRespond(200, ['success' => true, 'qr_code_path' => $path, 'account_name' => $accountName]);
