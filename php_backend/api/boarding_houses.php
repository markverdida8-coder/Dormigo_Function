<?php
require_once 'db.php';

$method = $_SERVER['REQUEST_METHOD'];

if ($method === 'GET') {
    $houseId = isset($_GET['house_id']) ? (int)$_GET['house_id'] : null;
    $landlordId = isset($_GET['landlord_id']) ? (int)$_GET['landlord_id'] : null;
    $status = isset($_GET['status']) ? strtoupper(trim($_GET['status'])) : null;
    $search = isset($_GET['search']) ? trim($_GET['search']) : null;

    $where = [];
    $params = [];

    if ($houseId) {
        $where[] = "bh.house_id = :house_id";
        $params['house_id'] = $houseId;
    }
    if ($landlordId) {
        $where[] = "bh.landlord_id = :landlord_id";
        $params['landlord_id'] = $landlordId;
    }
    if ($status) {
        $where[] = "bh.status = :status::house_status_enum";
        $params['status'] = $status;
    }
    if ($search) {
        $where[] = "(LOWER(bh.house_name) LIKE LOWER(:search) OR LOWER(bh.address) LIKE LOWER(:search) OR LOWER(bh.description) LIKE LOWER(:search))";
        $params['search'] = '%' . $search . '%';
    }

    $whereClause = !empty($where) ? "WHERE " . implode(" AND ", $where) : "";

    $sql = "SELECT bh.*,
                   u.full_name AS landlord_name,
                   u.email AS landlord_email,
                   u.phone AS landlord_phone,
                   COALESCE(MIN(r.monthly_rent), 0) AS starting_rent,
                   COALESCE(MIN(r.monthly_rent), 0) AS min_rent,
                   COUNT(DISTINCT r.room_id) AS total_rooms,
                   COUNT(DISTINCT CASE WHEN r.status = 'AVAILABLE' THEN r.room_id END) AS available_rooms,
                   COUNT(DISTINCT CASE WHEN r.status = 'OCCUPIED' THEN r.room_id END) AS occupied_rooms,
                   COALESCE(ROUND(AVG(rev.rating), 1), 0) AS avg_rating,
                   COUNT(DISTINCT rev.review_id) AS reviews_count,
                   array_to_json(array_remove(array_agg(DISTINCT hp.photo_path), NULL)) AS photo_paths
            FROM boarding_houses bh
            JOIN users u ON bh.landlord_id = u.user_id
            LEFT JOIN rooms r ON bh.house_id = r.house_id
            LEFT JOIN house_photos hp ON bh.house_id = hp.house_id
            LEFT JOIN reviews rev ON bh.house_id = rev.house_id
            $whereClause
            GROUP BY bh.house_id, u.user_id
            ORDER BY bh.house_id DESC";

    $stmt = $pdo->prepare($sql);
    $stmt->execute($params);
    $houses = $stmt->fetchAll();

    foreach ($houses as &$house) {
        $hId = $house['house_id'];
        if (isset($house['photo_paths']) && is_string($house['photo_paths'])) {
            $house['photo_paths'] = json_decode($house['photo_paths']);
        }
        if (empty($house['photo_paths'])) {
            $house['photo_paths'] = [];
        }

        // Fetch amenities for each house
        $amStmt = $pdo->prepare("SELECT a.amenity_id, a.amenity_name
                                 FROM boarding_house_amenities bha
                                 JOIN amenities a ON bha.amenity_id = a.amenity_id
                                 WHERE bha.house_id = :hid");
        $amStmt->execute(['hid' => $hId]);
        $house['amenities'] = $amStmt->fetchAll();

        // If specific house requested, also attach full rooms list
        if ($houseId) {
            $rStmt = $pdo->prepare("SELECT * FROM rooms WHERE house_id = :hid ORDER BY room_id ASC");
            $rStmt->execute(['hid' => $hId]);
            $house['rooms'] = $rStmt->fetchAll();

            $revStmt = $pdo->prepare("SELECT rev.*, u.full_name AS student_name, u.profile_image
                                      FROM reviews rev
                                      JOIN users u ON rev.user_id = u.user_id
                                      WHERE rev.house_id = :hid
                                      ORDER BY rev.review_id DESC");
            $revStmt->execute(['hid' => $hId]);
            $house['reviews'] = $revStmt->fetchAll();
        }
    }

    echo json_encode(["success" => true, "data" => $houses]);

} elseif ($method === 'POST') {
    if (isset($_POST['payload'])) {
        $data = json_decode($_POST['payload'], true);
    } else {
        $data = json_decode(file_get_contents("php://input"), true);
    }

    if (!$data || !isset($data['landlord_id']) || !isset($data['house_name'])) {
        echo json_encode(["success" => false, "message" => "Invalid property details."]);
        exit();
    }

    try {
        $pdo->beginTransaction();

        $stmt = $pdo->prepare("INSERT INTO boarding_houses (landlord_id, house_name, description, address, house_rules, status, created_at)
                                VALUES (:landlord_id, :house_name, :description, :address, :house_rules, :status::house_status_enum, CURRENT_TIMESTAMP) RETURNING house_id");
        $stmt->execute([
            'landlord_id' => $data['landlord_id'],
            'house_name' => $data['house_name'],
            'description' => $data['description'] ?? '',
            'address' => $data['address'] ?? '',
            'house_rules' => $data['house_rules'] ?? '',
            'status' => strtoupper($data['status'] ?? 'ACTIVE')
        ]);
        $houseId = $stmt->fetchColumn();

        $roomStmt = $pdo->prepare("INSERT INTO rooms (house_id, room_number, room_type, capacity, monthly_rent, status, created_at)
                                   VALUES (:house_id, :room_number, :room_type, :capacity, :monthly_rent, :status::room_status_enum, CURRENT_TIMESTAMP)");

        if (isset($data['rooms']) && is_array($data['rooms'])) {
            foreach ($data['rooms'] as $room) {
                $roomStmt->execute([
                    'house_id' => $houseId,
                    'room_number' => $room['room_number'] ?? 'Room 1',
                    'room_type' => $room['room_type'] ?? 'Single Room',
                    'capacity' => (int)($room['capacity'] ?? 1),
                    'monthly_rent' => (float)($room['monthly_rent'] ?? 0),
                    'status' => strtoupper($room['status'] ?? 'AVAILABLE')
                ]);
            }
        } elseif (isset($data['room'])) {
            $room = $data['room'];
            $roomStmt->execute([
                'house_id' => $houseId,
                'room_number' => $room['room_number'] ?? 'Room 1',
                'room_type' => $room['room_type'] ?? 'Single Room',
                'capacity' => (int)($room['capacity'] ?? 1),
                'monthly_rent' => (float)($room['monthly_rent'] ?? 0),
                'status' => strtoupper($room['status'] ?? 'AVAILABLE')
            ]);
        }

        if (isset($data['amenity_ids']) && is_array($data['amenity_ids'])) {
            $amenityStmt = $pdo->prepare("INSERT INTO boarding_house_amenities (house_id, amenity_id) VALUES (:house_id, :amenity_id) ON CONFLICT DO NOTHING");
            foreach ($data['amenity_ids'] as $amenityId) {
                $amenityStmt->execute(['house_id' => $houseId, 'amenity_id' => (int)$amenityId]);
            }
        }

        $photoPaths = [];
        if (isset($_FILES['photos'])) {
            $uploadDir = '../uploads/';
            if (!file_exists($uploadDir)) {
                mkdir($uploadDir, 0777, true);
            }
            $photoStmt = $pdo->prepare("INSERT INTO house_photos (house_id, photo_path, created_at) VALUES (:house_id, :photo_path, CURRENT_TIMESTAMP)");
            foreach ($_FILES['photos']['tmp_name'] as $key => $tmpName) {
                if ($_FILES['photos']['error'][$key] === UPLOAD_ERR_OK) {
                    $fileName = 'house_' . $houseId . '_' . time() . '_' . $key . '.jpg';
                    $targetFilePath = $uploadDir . $fileName;
                    if (move_uploaded_file($tmpName, $targetFilePath)) {
                        $webPath = 'uploads/' . $fileName;
                        $photoStmt->execute(['house_id' => $houseId, 'photo_path' => $webPath]);
                        $photoPaths[] = $webPath;
                    }
                }
            }
        }

        // Support direct photo URLs
        if (isset($data['photo_urls']) && is_array($data['photo_urls'])) {
            $photoStmt = $pdo->prepare("INSERT INTO house_photos (house_id, photo_path, created_at) VALUES (:house_id, :photo_path, CURRENT_TIMESTAMP)");
            foreach ($data['photo_urls'] as $url) {
                if (trim($url) !== '') {
                    $photoStmt->execute(['house_id' => $houseId, 'photo_path' => trim($url)]);
                    $photoPaths[] = trim($url);
                }
            }
        }

        $pdo->commit();

        echo json_encode([
            "success" => true,
            "message" => "Boarding house saved successfully.",
            "house_id" => $houseId,
            "photo_paths" => $photoPaths
        ]);
    } catch (PDOException $e) {
        $pdo->rollBack();
        echo json_encode(["success" => false, "message" => "Database error: " . $e->getMessage()]);
    }

} elseif ($method === 'PATCH') {
    $data = json_decode(file_get_contents("php://input"), true);
    if (!isset($data['house_id'])) {
        echo json_encode(["success" => false, "message" => "House ID is required."]);
        exit();
    }
    $houseId = (int)$data['house_id'];

    try {
        $pdo->beginTransaction();

        $fields = [];
        $params = ['house_id' => $houseId];

        if (isset($data['house_name'])) {
            $fields[] = "house_name = :house_name";
            $params['house_name'] = trim($data['house_name']);
        }
        if (isset($data['description'])) {
            $fields[] = "description = :description";
            $params['description'] = trim($data['description']);
        }
        if (isset($data['address'])) {
            $fields[] = "address = :address";
            $params['address'] = trim($data['address']);
        }
        if (isset($data['house_rules'])) {
            $fields[] = "house_rules = :house_rules";
            $params['house_rules'] = trim($data['house_rules']);
        }
        if (isset($data['status'])) {
            $fields[] = "status = :status::house_status_enum";
            $params['status'] = strtoupper(trim($data['status']));
        }

        if (!empty($fields)) {
            $sql = "UPDATE boarding_houses SET " . implode(", ", $fields) . " WHERE house_id = :house_id";
            $stmt = $pdo->prepare($sql);
            $stmt->execute($params);
        }

        if (isset($data['amenity_ids']) && is_array($data['amenity_ids'])) {
            $delStmt = $pdo->prepare("DELETE FROM boarding_house_amenities WHERE house_id = :house_id");
            $delStmt->execute(['house_id' => $houseId]);

            $insStmt = $pdo->prepare("INSERT INTO boarding_house_amenities (house_id, amenity_id) VALUES (:house_id, :amenity_id) ON CONFLICT DO NOTHING");
            foreach ($data['amenity_ids'] as $amenityId) {
                $insStmt->execute(['house_id' => $houseId, 'amenity_id' => (int)$amenityId]);
            }
        }

        $pdo->commit();
        echo json_encode(["success" => true, "message" => "Boarding house updated successfully."]);
    } catch (PDOException $e) {
        $pdo->rollBack();
        echo json_encode(["success" => false, "message" => "Database error: " . $e->getMessage()]);
    }

} elseif ($method === 'DELETE') {
    $data = json_decode(file_get_contents("php://input"), true);
    $houseId = $data['house_id'] ?? ($_GET['house_id'] ?? null);
    if (!$houseId) {
        echo json_encode(["success" => false, "message" => "House ID is required."]);
        exit();
    }
    $stmt = $pdo->prepare("DELETE FROM boarding_houses WHERE house_id = :house_id");
    $stmt->execute(['house_id' => (int)$houseId]);

    echo json_encode(["success" => true, "message" => "Boarding house deleted successfully."]);
}
?>
