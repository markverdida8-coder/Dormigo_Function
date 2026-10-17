<?php
require_once 'db.php';
require_once 'auth_helper.php';
require_once 'subscription_model.php';

function propertyRespond(int $statusCode, array $payload): void {
    http_response_code($statusCode);
    echo json_encode($payload);
    exit();
}

function requirePropertyLandlord(PDO $pdo): int {
    $authUser = authenticateUser($pdo);
    if (strtoupper(trim((string)($authUser['user_type'] ?? ''))) !== 'LANDLORD') {
        propertyRespond(403, ["success" => false, "message" => "Forbidden: Only landlords can manage properties."]);
    }
    return (int)$authUser['user_id'];
}

function requireOwnedProperty(PDO $pdo, int $houseId, int $landlordId): void {
    $stmt = $pdo->prepare("SELECT landlord_id FROM boarding_houses WHERE house_id = :house_id");
    $stmt->execute(['house_id' => $houseId]);
    $house = $stmt->fetch();

    if (!$house) {
        propertyRespond(404, ["success" => false, "message" => "Boarding house not found."]);
    }
    if ((int)$house['landlord_id'] !== $landlordId) {
        propertyRespond(403, ["success" => false, "message" => "Forbidden: You do not own this property."]);
    }
}

function validatedUploadExtension(string $tmpName, array $allowedTypes, string $message): string {
    if (!is_uploaded_file($tmpName)) {
        propertyRespond(400, ["success" => false, "message" => $message]);
    }

    $finfo = new finfo(FILEINFO_MIME_TYPE);
    $mimeType = $finfo->file($tmpName);
    if (!is_string($mimeType) || !isset($allowedTypes[$mimeType])) {
        propertyRespond(400, ["success" => false, "message" => $message]);
    }
    return $allowedTypes[$mimeType];
}

function ensureUploadDirectory(string $uploadDir): void {
    if (!is_dir($uploadDir) && !mkdir($uploadDir, 0777, true) && !is_dir($uploadDir)) {
        propertyRespond(500, ["success" => false, "message" => "Unable to store the uploaded file."]);
    }
}

function saveHousePhotos(PDO $pdo, int $houseId, array $files): array {
    $tmpNames = $files['tmp_name'] ?? [];
    $errors = $files['error'] ?? [];
    if (!is_array($tmpNames)) {
        $tmpNames = [$tmpNames];
        $errors = [$errors];
    }
    if (count($tmpNames) === 0) {
        propertyRespond(400, ["success" => false, "message" => "At least one valid image is required."]);
    }

    $allowedImageTypes = [
        'image/jpeg' => 'jpg',
        'image/png' => 'png',
        'image/webp' => 'webp'
    ];
    $uploads = [];
    foreach ($tmpNames as $key => $tmpName) {
        if (($errors[$key] ?? UPLOAD_ERR_NO_FILE) !== UPLOAD_ERR_OK) {
            propertyRespond(400, ["success" => false, "message" => "Each photo must be a valid JPG, PNG, or WEBP image."]);
        }
        $uploads[] = [
            'tmp_name' => $tmpName,
            'extension' => validatedUploadExtension(
                $tmpName,
                $allowedImageTypes,
                "Each photo must be a valid JPG, PNG, or WEBP image."
            )
        ];
    }

    $uploadDir = '../uploads/';
    ensureUploadDirectory($uploadDir);
    $photoStmt = $pdo->prepare("INSERT INTO house_photos (house_id, photo_path) VALUES (:house_id, :photo_path)");
    $photoPaths = [];
    foreach ($uploads as $key => $upload) {
        $fileName = 'house_' . $houseId . '_' . bin2hex(random_bytes(8)) . '_' . $key . '.' . $upload['extension'];
        $targetFilePath = $uploadDir . $fileName;
        if (!move_uploaded_file($upload['tmp_name'], $targetFilePath)) {
            throw new RuntimeException('Unable to save uploaded photo.');
        }
        $webPath = 'uploads/' . $fileName;
        $photoStmt->execute(['house_id' => $houseId, 'photo_path' => $webPath]);
        $photoPaths[] = $webPath;
    }
    return $photoPaths;
}

$method = $_SERVER['REQUEST_METHOD'];

if ($method === 'GET') {
    $availableOnly = isset($_GET['available_only']) && $_GET['available_only'] == '1';

    $sql = "
        SELECT bh.*,
               COALESCE((SELECT json_agg(hp.photo_path) FROM house_photos hp WHERE hp.house_id = bh.house_id), '[]'::json) AS photo_paths,
                COALESCE(
                    (SELECT lv.verification_status
                     FROM landlord_verifications lv
                     WHERE lv.landlord_id = bh.landlord_id
                     ORDER BY lv.submitted_at DESC LIMIT 1),
                    'UNVERIFIED'
                ) AS landlord_verification_status,
                COALESCE(bhv.verification_status, 'UNVERIFIED') AS property_verification_status,
                COALESCE(bhv.verification_status, 'UNVERIFIED') AS verification_status,
                bhv.rejection_reason AS property_rejection_reason
        FROM boarding_houses bh
        LEFT JOIN LATERAL (
            SELECT verification_status, rejection_reason
            FROM boarding_house_verification
            WHERE house_id = bh.house_id
            ORDER BY COALESCE(reviewed_at, submitted_at) DESC, verification_id DESC
            LIMIT 1
        ) bhv ON TRUE
    ";

    if ($availableOnly) {
        $sql .= " WHERE UPPER(bh.status::text) IN ('ACTIVE', 'PENDING') AND EXISTS (
            SELECT 1
            FROM rooms r
            WHERE r.house_id = bh.house_id
              AND UPPER(r.status::text) IN ('AVAILABLE', 'OCCUPIED')
              AND r.capacity > (
                  SELECT COUNT(*)
                  FROM bookings b
                  WHERE b.room_id = r.room_id
                    AND UPPER(b.status::text) IN ('APPROVED', 'ACTIVE')
                    AND b.move_in_date <= CURRENT_DATE
                    AND (b.move_in_date + (b.duration_months * INTERVAL '1 month'))::date > CURRENT_DATE
              )
        )";
    }

    $sql .= " ORDER BY bh.house_id DESC";

    $stmt = $pdo->query($sql);
    $houses = $stmt->fetchAll();
    foreach ($houses as &$house) {
        if (is_string($house['photo_paths'])) {
            $house['photo_paths'] = json_decode($house['photo_paths'], true);
        }
        if (!is_array($house['photo_paths'])) {
            $house['photo_paths'] = [];
        }
    }
    echo json_encode(["success" => true, "data" => $houses]);

} elseif ($method === 'POST') {
    $landlordId = requirePropertyLandlord($pdo);

    if (isset($_POST['action']) && $_POST['action'] === 'upload_photos') {
        $houseId = (int)($_POST['house_id'] ?? 0);
        if ($houseId <= 0 || !isset($_FILES['photos'])) {
            echo json_encode(["success" => false, "message" => "House ID and photos required."]);
            exit();
        }
        requireOwnedProperty($pdo, $houseId, $landlordId);
        try {
            $photoPaths = saveHousePhotos($pdo, $houseId, $_FILES['photos']);
        } catch (Throwable $e) {
            error_log('House photo upload failed: ' . get_class($e));
            propertyRespond(500, ["success" => false, "message" => "Unable to save the uploaded photos."]);
        }
        echo json_encode(["success" => true, "message" => "Photos uploaded successfully.", "photo_paths" => $photoPaths]);
        exit();
    }

    if (isset($_POST['action']) && $_POST['action'] === 'upload_gcash_qr') {
        $houseId = (int)($_POST['house_id'] ?? 0);
        if ($houseId <= 0 || !isset($_FILES['gcash_qr'])) {
            echo json_encode(["success" => false, "message" => "House ID and QR file required."]);
            exit();
        }
        requireOwnedProperty($pdo, $houseId, $landlordId);

        if ($_FILES['gcash_qr']['error'] !== UPLOAD_ERR_OK) {
            propertyRespond(400, ["success" => false, "message" => "GCash QR code must be a valid JPG, PNG, or WEBP image."]);
        }
        $allowedImageTypes = [
            'image/jpeg' => 'jpg',
            'image/png' => 'png',
            'image/webp' => 'webp'
        ];
        $extension = validatedUploadExtension(
            $_FILES['gcash_qr']['tmp_name'],
            $allowedImageTypes,
            "GCash QR code must be a valid JPG, PNG, or WEBP image."
        );
        $uploadDir = '../uploads/gcash_qr/';
        ensureUploadDirectory($uploadDir);
        $tmpName = $_FILES['gcash_qr']['tmp_name'];
        $fileName = 'qr_house_' . $houseId . '_' . bin2hex(random_bytes(8)) . '.' . $extension;
        $targetFilePath = $uploadDir . $fileName;

        if (move_uploaded_file($tmpName, $targetFilePath)) {
            $webPath = 'uploads/gcash_qr/' . $fileName;
            $stmt = $pdo->prepare("UPDATE boarding_houses SET gcash_qr_code = :path, gcash_updated_at = CURRENT_TIMESTAMP WHERE house_id = :house_id");
            $stmt->execute(['path' => $webPath, 'house_id' => $houseId]);

            echo json_encode([
                "success" => true,
                "message" => "GCash QR code uploaded successfully.",
                "gcash_qr_code" => $webPath
            ]);
        } else {
            echo json_encode(["success" => false, "message" => "Failed to save QR code image."]);
        }
        exit();
    }

    if (isset($_POST['payload'])) {
        $data = json_decode($_POST['payload'], true);
    } else {
        $data = json_decode(file_get_contents("php://input"), true);
    }

    if (!$data || !isset($data['house_name'])) {
        echo json_encode(["success" => false, "message" => "Invalid property details."]);
        exit();
    }

    try {
        $pdo->beginTransaction();

        if (!subscriptionSchemaReady($pdo)) {
            $pdo->rollBack();
            propertyRespond(503, [
                'success' => false,
                'message' => 'Subscription service is unavailable. Please contact Dormigo support.'
            ]);
        }
        $subscriptionGate = $pdo->prepare(
            "SELECT subscription_id
             FROM landlord_subscriptions
             WHERE landlord_id = :landlord_id
               AND status = 'SUCCESSFUL'
               AND starts_at <= CURRENT_DATE
               AND expires_at > CURRENT_DATE
             ORDER BY expires_at DESC
             LIMIT 1
             FOR UPDATE"
        );
        $subscriptionGate->execute(['landlord_id' => $landlordId]);
        if (!$subscriptionGate->fetchColumn()) {
            $pdo->rollBack();
            propertyRespond(403, [
                'success' => false,
                'subscription_required' => true,
                'message' => 'An active Dormigo monthly subscription is required before adding a boarding house.'
            ]);
        }

        $verificationStmt = $pdo->prepare(
            "SELECT verification_status
             FROM landlord_verifications
             WHERE landlord_id = :landlord_id
             ORDER BY submitted_at DESC
             LIMIT 1"
        );
        $verificationStmt->execute(['landlord_id' => $landlordId]);
        $landlordVerificationStatus = strtoupper(trim((string)$verificationStmt->fetchColumn()));
        $landlordIsVerified = in_array($landlordVerificationStatus, ['VERIFIED', 'APPROVED'], true);
        // Publication is independent of the landlord's verification review.
        $initialHouseStatus = 'ACTIVE';
        $initialPropertyVerificationStatus = $landlordIsVerified ? 'VERIFIED' : 'UNVERIFIED';

        $stmt = $pdo->prepare("INSERT INTO boarding_houses (landlord_id, house_name, description, address, house_rules, status, free_electricity, electricity_rate, free_water, water_rate, payment_due_day, advance_months, security_deposit_months, utility_deposit, other_fees)
                                VALUES (:landlord_id, :house_name, :description, :address, :house_rules, :status::house_status_enum, :free_electricity, :electricity_rate, :free_water, :water_rate, :payment_due_day, :advance_months, :security_deposit_months, :utility_deposit, :other_fees) RETURNING house_id");
        $stmt->execute([
            'landlord_id' => $landlordId,
            'house_name' => $data['house_name'],
            'description' => $data['description'] ?? '',
            'address' => $data['address'] ?? '',
            'house_rules' => $data['house_rules'] ?? '',
            'status' => $initialHouseStatus,
            'free_electricity' => filter_var($data['free_electricity'] ?? false, FILTER_VALIDATE_BOOLEAN) ? 1 : 0,
            'electricity_rate' => $data['electricity_rate'] ?? '0.00',
            'free_water' => filter_var($data['free_water'] ?? false, FILTER_VALIDATE_BOOLEAN) ? 1 : 0,
            'water_rate' => $data['water_rate'] ?? '0.00',
            'payment_due_day' => (int)($data['payment_due_day'] ?? 1),
            'advance_months' => (int)($data['advance_months'] ?? 1),
            'security_deposit_months' => (int)($data['security_deposit_months'] ?? 1),
            'utility_deposit' => $data['utility_deposit'] ?? '0.00',
            'other_fees' => $data['other_fees'] ?? '0.00'
        ]);
        $houseId = $stmt->fetchColumn();

        $roomStmt = $pdo->prepare("INSERT INTO rooms (house_id, room_number, room_type, capacity, monthly_rent, status, advance_months, deposit_months, utility_deposit, other_fees, other_fees_description, deposit_refund_policy)
                                   VALUES (:house_id, :room_number, :room_type, :capacity, :monthly_rent, :status::room_status_enum, :advance_months, :deposit_months, :utility_deposit, :other_fees, :other_fees_description, :deposit_refund_policy)
                                   RETURNING room_id");
        $termsSchemaReady = in_array($pdo->query(
            "SELECT to_regclass('public.room_payment_terms') IS NOT NULL"
        )->fetchColumn(), [true, 't', '1', 1], true);
        $termsStmt = $termsSchemaReady ? $pdo->prepare(
            "INSERT INTO room_payment_terms (
                room_id, advance_payment_amount, security_deposit_amount,
                garbage_fee_amount, terms_source
             ) VALUES (
                :room_id, :advance_payment_amount, :security_deposit_amount,
                :garbage_fee_amount, 'LANDLORD_CONFIGURATION'
             )"
        ) : null;

        if (isset($data['rooms']) && is_array($data['rooms'])) {
            foreach ($data['rooms'] as $room) {
                $roomStmt->execute([
                    'house_id' => $houseId,
                    'room_number' => $room['room_number'] ?? 'Room 1',
                    'room_type' => $room['room_type'] ?? 'Bedspace',
                    'capacity' => $room['capacity'] ?? 1,
                    'monthly_rent' => $room['monthly_rent'] ?? 0,
                    'status' => strtoupper($room['status'] ?? 'AVAILABLE'),
                    'advance_months' => $room['advance_months'] ?? 1,
                    'deposit_months' => $room['deposit_months'] ?? 1,
                    'utility_deposit' => $room['utility_deposit'] ?? 0.00,
                    'other_fees' => $room['other_fees'] ?? 0.00,
                    'other_fees_description' => $room['other_fees_description'] ?? '',
                    'deposit_refund_policy' => $room['deposit_refund_policy'] ?? ''
                ]);
                $roomId = (int)$roomStmt->fetchColumn();
                $terms = $room['payment_terms'] ?? null;
                if ($termsSchemaReady && is_array($terms)) {
                    $advance = $terms['advance_payment_amount'] ?? null;
                    $security = $terms['security_deposit_amount'] ?? null;
                    $garbage = $terms['garbage_fee_amount'] ?? null;
                    if (!is_numeric($advance) || !is_numeric($security) || !is_numeric($garbage)
                        || (float)$advance < 0 || (float)$security < 0 || (float)$garbage < 0) {
                        throw new InvalidArgumentException('Room payment terms must contain non-negative amounts.');
                    }
                    $termsStmt->execute([
                        'room_id' => $roomId,
                        'advance_payment_amount' => $advance,
                        'security_deposit_amount' => $security,
                        'garbage_fee_amount' => $garbage
                    ]);
                }
            }
        } elseif (isset($data['room'])) {
            $room = $data['room'];
            $roomStmt->execute([
                'house_id' => $houseId,
                'room_number' => $room['room_number'] ?? 'Room 1',
                'room_type' => $room['room_type'] ?? 'Single',
                'capacity' => $room['capacity'] ?? 1,
                'monthly_rent' => $room['monthly_rent'] ?? 0,
                'status' => strtoupper($room['status'] ?? 'AVAILABLE')
            ]);
        }

        if (isset($data['amenity_ids']) && is_array($data['amenity_ids'])) {
            $amenityStmt = $pdo->prepare("INSERT INTO boarding_house_amenities (house_id, amenity_id) VALUES (:house_id, :amenity_id) ON CONFLICT DO NOTHING");
            foreach ($data['amenity_ids'] as $amenityId) {
                $amenityStmt->execute(['house_id' => $houseId, 'amenity_id' => $amenityId]);
            }
        }

        $photoPaths = [];
        if (isset($_FILES['photos'])) {
            $photoPaths = saveHousePhotos($pdo, $houseId, $_FILES['photos']);
        }

        // Property verification inherits the authenticated owner's landlord verification state.
        $insBhVerif = $pdo->prepare("INSERT INTO boarding_house_verification (house_id, verification_status, submitted_at) VALUES (?, ?, CURRENT_TIMESTAMP) ON CONFLICT DO NOTHING");
        $insBhVerif->execute([$houseId, $initialPropertyVerificationStatus]);

        $pdo->commit();

        echo json_encode([
            "success" => true,
            "message" => "Boarding house saved successfully.",
            "house_id" => $houseId,
            "photo_paths" => $photoPaths
        ]);
    } catch (Throwable $e) {
        if ($pdo->inTransaction()) {
            $pdo->rollBack();
        }
        error_log('Boarding house creation failed: ' . get_class($e));
        echo json_encode(["success" => false, "message" => "Unable to save the boarding house."]);
    }
} elseif ($method === 'PATCH') {
    $landlordId = requirePropertyLandlord($pdo);
    $data = json_decode(file_get_contents("php://input"), true);
    $houseId = isset($data['house_id']) ? (int)$data['house_id'] : 0;
    if ($houseId <= 0) {
        propertyRespond(400, ["success" => false, "message" => "Valid house_id is required."]);
    }

    try {
        requireOwnedProperty($pdo, $houseId, $landlordId);

        // Keep accepting legacy client payloads, but never let landlords control activation status.
        unset($data['status']);

        $allowedFields = ['house_name', 'description', 'address', 'house_rules'];
        $updates = [];
        $params = ['house_id' => $houseId];
        foreach ($allowedFields as $field) {
            if (array_key_exists($field, $data)) {
                $updates[] = $field . ' = :' . $field;
                $params[$field] = is_string($data[$field]) ? trim($data[$field]) : '';
            }
        }
        if (count($updates) === 0) {
            propertyRespond(400, ["success" => false, "message" => "No supported property details were provided."]);
        }

        $update = $pdo->prepare(
            "UPDATE boarding_houses SET " . implode(', ', $updates) . " WHERE house_id = :house_id"
        );
        $update->execute($params);
        echo json_encode(["success" => true, "message" => "Boarding house updated successfully."]);
    } catch (Throwable $e) {
        error_log('Boarding house update failed: ' . get_class($e));
        echo json_encode(["success" => false, "message" => "Unable to update the boarding house."]);
    }
} elseif ($method === 'DELETE') {
    $landlordId = requirePropertyLandlord($pdo);
    $data = json_decode(file_get_contents("php://input"), true);
    $houseId = isset($data['house_id']) ? (int)$data['house_id'] : 0;

    if ($houseId <= 0) {
        echo json_encode(["success" => false, "message" => "Valid house_id is required."]);
        exit();
    }

    try {
        requireOwnedProperty($pdo, $houseId, $landlordId);

        if (($data['action'] ?? '') === 'delete_photo') {
            $photoPath = isset($data['photo_path']) ? (string)$data['photo_path'] : '';
            $uploadOffset = strpos($photoPath, 'uploads/');
            if ($uploadOffset !== false) {
                $photoPath = substr($photoPath, $uploadOffset);
            }
            if ($photoPath === '') {
                propertyRespond(400, ["success" => false, "message" => "A valid photo path is required."]);
            }

            $deletePhoto = $pdo->prepare("DELETE FROM house_photos WHERE house_id = :house_id AND photo_path = :photo_path");
            $deletePhoto->execute(['house_id' => $houseId, 'photo_path' => $photoPath]);
            if ($deletePhoto->rowCount() === 0) {
                propertyRespond(404, ["success" => false, "message" => "Property photo not found."]);
            }
            echo json_encode(["success" => true, "message" => "Property photo deleted successfully."]);
            exit();
        }

        $chkBookings = $pdo->prepare("
            SELECT 1
            FROM bookings b
            JOIN rooms r ON b.room_id = r.room_id
            WHERE r.house_id = ?
              AND UPPER(b.status::text) IN ('PENDING', 'APPROVED')
            LIMIT 1
        ");
        $chkBookings->execute([$houseId]);
        if ($chkBookings->fetch()) {
            http_response_code(409);
            echo json_encode([
                "success" => false,
                "message" => "This property cannot be deleted while it has active bookings."
            ]);
            exit();
        }

        $upd = $pdo->prepare("UPDATE boarding_houses SET status = 'INACTIVE'::house_status_enum WHERE house_id = ?");
        $upd->execute([$houseId]);

        echo json_encode([
            "success" => true,
            "message" => "Property deleted successfully."
        ]);
    } catch (Throwable $e) {
        error_log('Boarding house deletion failed: ' . get_class($e));
        echo json_encode(["success" => false, "message" => "Unable to delete the property."]);
    }
}
?>
