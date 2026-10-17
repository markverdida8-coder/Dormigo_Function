<?php
session_start();

function proofRespond(int $status, string $message): void
{
    http_response_code($status);
    header('Content-Type: application/json; charset=UTF-8');
    echo json_encode(['success' => false, 'message' => $message]);
    exit();
}

if (!isset($_SESSION['admin_logged_in']) || $_SESSION['admin_logged_in'] !== true || strtoupper($_SESSION['admin_user_type'] ?? '') !== 'ADMIN') {
    proofRespond(401, 'Administrator authentication is required to view payment proofs.');
}
$id = filter_input(INPUT_GET, 'subscription_id', FILTER_VALIDATE_INT, ['options' => ['min_range' => 1]]);
if ($id === false || $id === null) proofRespond(400, 'A valid subscription payment identifier is required.');

require_once __DIR__ . '/db.php';

$stmt = $pdo->prepare('SELECT proof_path FROM landlord_subscriptions WHERE subscription_id = :id');
$stmt->execute(['id' => $id]);
$path = $stmt->fetchColumn();
if ($path === false) proofRespond(404, 'Subscription payment was not found.');
if ($path === null || $path === '') proofRespond(404, 'No payment proof is available for this subscription payment.');

// Android creates only subscription_<32 hexadecimal characters> image filenames.
if (!preg_match('#^uploads/subscription_proofs/subscription_[a-f0-9]{32}\.(jpg|png|webp)$#', $path)) {
    proofRespond(404, 'Payment proof is unavailable.');
}

$proofDirectory = realpath('C:/xampp/htdocs/Dormigo_Backend/uploads/subscription_proofs');
$file = realpath('C:/xampp/htdocs/Dormigo_Backend/' . $path);
if ($proofDirectory === false || $file === false || dirname($file) !== $proofDirectory || !is_file($file)) {
    proofRespond(404, 'Payment proof file was not found.');
}

$mime = (new finfo(FILEINFO_MIME_TYPE))->file($file);
if (!in_array($mime, ['image/jpeg', 'image/png', 'image/webp'], true)) proofRespond(404, 'Payment proof is unavailable.');

header('Content-Type: ' . $mime);
header('Content-Length: ' . filesize($file));
header('Content-Disposition: inline; filename="' . basename($file) . '"');
header('Cache-Control: private, no-store');
header('X-Content-Type-Options: nosniff');
readfile($file);
exit();
