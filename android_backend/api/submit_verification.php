<?php
require_once 'db.php';
require_once 'auth_helper.php';
global $pdo;

function verificationRespond(int $statusCode, array $payload): void {
    http_response_code($statusCode);
    echo json_encode($payload);
    exit();
}

function saveVerificationDocument(string $fieldName, string $prefix, int $houseId): ?string {
    if (!isset($_FILES[$fieldName])) {
        return null;
    }
    $file = $_FILES[$fieldName];
    if (($file['error'] ?? UPLOAD_ERR_NO_FILE) !== UPLOAD_ERR_OK || !is_uploaded_file($file['tmp_name'] ?? '')) {
        verificationRespond(400, ["success" => false, "message" => "Verification documents must be valid JPG, PNG, WEBP, or PDF files."]);
    }

    $allowedTypes = [
        'image/jpeg' => 'jpg',
        'image/png' => 'png',
        'image/webp' => 'webp',
        'application/pdf' => 'pdf'
    ];
    $finfo = new finfo(FILEINFO_MIME_TYPE);
    $mimeType = $finfo->file($file['tmp_name']);
    if (!is_string($mimeType) || !isset($allowedTypes[$mimeType])) {
        verificationRespond(400, ["success" => false, "message" => "Verification documents must be valid JPG, PNG, WEBP, or PDF files."]);
    }

    $uploadDir = '../uploads/';
    if (!is_dir($uploadDir) && !mkdir($uploadDir, 0777, true) && !is_dir($uploadDir)) {
        verificationRespond(500, ["success" => false, "message" => "Unable to store the verification document."]);
    }
    $fileName = $prefix . '_' . $houseId . '_' . bin2hex(random_bytes(8)) . '.' . $allowedTypes[$mimeType];
    if (!move_uploaded_file($file['tmp_name'], $uploadDir . $fileName)) {
        verificationRespond(500, ["success" => false, "message" => "Unable to store the verification document."]);
    }
    return 'uploads/' . $fileName;
}

$method = $_SERVER['REQUEST_METHOD'];
if ($method !== 'POST') {
    echo json_encode(["success" => false, "message" => "Method not allowed."]);
    exit();
}

$authUser = authenticateUser($pdo);
if (strtoupper((string)($authUser['user_type'] ?? '')) !== 'LANDLORD') {
    verificationRespond(403, ["success" => false, "message" => "Forbidden: Only landlords can submit property verification."]);
}
$landlordId = (int)$authUser['user_id'];

$house_id = isset($_POST['house_id']) ? (int)$_POST['house_id'] : 0;

if ($house_id <= 0) {
    echo json_encode(["success" => false, "message" => "Valid house_id is required."]);
    exit();
}

try {
    // Verify ownership using the authenticated landlord, not request-provided IDs.
    $houseStmt = $pdo->prepare("SELECT house_id, landlord_id FROM boarding_houses WHERE house_id = ?");
    $houseStmt->execute([$house_id]);
    $house = $houseStmt->fetch();

    if (!$house) {
        echo json_encode(["success" => false, "message" => "Boarding house not found."]);
        exit();
    }

    if ((int)$house['landlord_id'] !== $landlordId) {
        verificationRespond(403, ["success" => false, "message" => "Forbidden: You do not own this property."]);
    }

    // 2. Check landlord account verification status in landlord_verifications
    $verifStmt = $pdo->prepare("SELECT verification_status, reviewed_by, reviewed_at FROM landlord_verifications WHERE landlord_id = ? ORDER BY submitted_at DESC LIMIT 1");
    $verifStmt->execute([$landlordId]);
    $verifRec = $verifStmt->fetch();

    $vStatus = $verifRec ? strtoupper(trim($verifRec['verification_status'])) : '';

    if ($vStatus !== 'VERIFIED' && $vStatus !== 'APPROVED') {
        echo json_encode([
            "success" => false,
            "message" => "Your landlord account must be verified by an administrator before you can submit a boarding house for verification."
        ]);
        exit();
    }

    // 3. Handle validated file uploads only after ownership is confirmed.
    $valid_id_path = saveVerificationDocument('valid_id', 'valid_id', $house_id);
    $proof_document_path = saveVerificationDocument('proof_document', 'proof', $house_id);

    $stmt = $pdo->prepare("SELECT verification_id FROM boarding_house_verification WHERE house_id = :house_id");
    $stmt->execute(['house_id' => $house_id]);
    $existing = $stmt->fetch();

    $pdo->beginTransaction();

    if ($existing) {
        $updateSql = "UPDATE boarding_house_verification SET verification_status = 'PENDING', rejection_reason = NULL, reviewed_by = NULL, reviewed_at = NULL, submitted_at = CURRENT_TIMESTAMP";
        $params = ['house_id' => $house_id];

        if ($valid_id_path) {
            $updateSql .= ", valid_id_path = :valid_id";
            $params['valid_id'] = $valid_id_path;
        }
        if ($proof_document_path) {
            $updateSql .= ", proof_document_path = :proof";
            $params['proof'] = $proof_document_path;
        }
        $updateSql .= " WHERE house_id = :house_id";

        $update = $pdo->prepare($updateSql);
        $update->execute($params);
    } else {
        $insert = $pdo->prepare("INSERT INTO boarding_house_verification (house_id, valid_id_path, proof_document_path, verification_status) VALUES (:house_id, :valid_id, :proof, 'PENDING')");
        $insert->execute([
            'house_id' => $house_id,
            'valid_id' => $valid_id_path,
            'proof' => $proof_document_path
        ]);
    }

    $pdo->commit();

    echo json_encode(["success" => true, "message" => "Verification documents submitted successfully and are pending administrator review."]);
} catch (Throwable $e) {
    if ($pdo->inTransaction()) {
        $pdo->rollBack();
    }
    error_log('Property verification submission failed: ' . get_class($e));
    echo json_encode(["success" => false, "message" => "Unable to submit property verification."]);
}
?>
