<?php
/**
 * DORMIGO - Secure Admin Verification Document Serving Endpoint
 * Serves uploaded verification files exclusively to authenticated administrators.
 */

session_start();
require_once __DIR__ . '/db.php';

// Check if admin is authenticated
if (empty($_SESSION['admin_logged_in']) || $_SESSION['admin_logged_in'] !== true) {
    http_response_code(403);
    echo "Unauthorized access.";
    exit();
}

$filePath = $_GET['path'] ?? '';
if (empty($filePath)) {
    http_response_code(400);
    echo "File path required.";
    exit();
}

// Sanitize and validate path strictly within C:/xampp/htdocs/Dormigo_Backend/uploads/verifications/
$baseUploadDir = realpath('C:/xampp/htdocs/Dormigo_Backend/uploads/verifications/');
if ($baseUploadDir === false) {
    http_response_code(404);
    echo "Upload directory not found.";
    exit();
}

$filename = basename($filePath);
$targetFile = $baseUploadDir . DIRECTORY_SEPARATOR . $filename;
$realTarget = realpath($targetFile);

if ($realTarget === false || strpos($realTarget, $baseUploadDir) !== 0 || !file_exists($realTarget)) {
    http_response_code(404);
    echo "Verification document not found.";
    exit();
}

// Determine content type safely
$ext = strtolower(pathinfo($realTarget, PATHINFO_EXTENSION));
$contentType = 'application/octet-stream';
if ($ext === 'jpg' || $ext === 'jpeg') {
    $contentType = 'image/jpeg';
} elseif ($ext === 'png') {
    $contentType = 'image/png';
} elseif ($ext === 'pdf') {
    $contentType = 'application/pdf';
}

header('Content-Type: ' . $contentType);
header('Content-Length: ' . filesize($realTarget));
header('Content-Disposition: inline; filename="' . $filename . '"');
readfile($realTarget);
exit();
?>
