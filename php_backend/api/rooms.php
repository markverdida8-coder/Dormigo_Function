<?php
require_once 'db.php';

$method = $_SERVER['REQUEST_METHOD'];

if ($method === 'GET') {
    $sql = "SELECT r.*, bh.house_name, bh.landlord_id, bh.address
            FROM rooms r
            JOIN boarding_houses bh ON r.house_id = bh.house_id";
    $params = [];
    $where = [];

    if (isset($_GET['house_id'])) {
        $where[] = "r.house_id = :house_id";
        $params['house_id'] = (int)$_GET['house_id'];
    }
    if (isset($_GET['room_id'])) {
        $where[] = "r.room_id = :room_id";
        $params['room_id'] = (int)$_GET['room_id'];
    }
    if (isset($_GET['landlord_id'])) {
        $where[] = "bh.landlord_id = :landlord_id";
        $params['landlord_id'] = (int)$_GET['landlord_id'];
    }
    if (isset($_GET['status'])) {
        $where[] = "r.status = :status::room_status_enum";
        $params['status'] = strtoupper(trim($_GET['status']));
    }

    if (!empty($where)) {
        $sql .= " WHERE " . implode(" AND ", $where);
    }
    $sql .= " ORDER BY r.room_id ASC";

    $stmt = $pdo->prepare($sql);
    $stmt->execute($params);
    echo json_encode(["success" => true, "data" => $stmt->fetchAll()]);

} elseif ($method === 'POST') {
    $data = json_decode(file_get_contents("php://input"), true);
    if (!isset($data['house_id']) || !isset($data['room_number']) || !isset($data['monthly_rent'])) {
        echo json_encode(["success" => false, "message" => "house_id, room_number, and monthly_rent are required."]);
        exit();
    }

    try {
        $stmt = $pdo->prepare("INSERT INTO rooms (house_id, room_number, room_type, capacity, monthly_rent, status, created_at)
                                VALUES (:house_id, :room_number, :room_type, :capacity, :monthly_rent, :status::room_status_enum, CURRENT_TIMESTAMP) RETURNING room_id");
        $stmt->execute([
            'house_id' => (int)$data['house_id'],
            'room_number' => trim($data['room_number']),
            'room_type' => trim($data['room_type'] ?? 'Single Room'),
            'capacity' => (int)($data['capacity'] ?? 1),
            'monthly_rent' => (float)$data['monthly_rent'],
            'status' => strtoupper($data['status'] ?? 'AVAILABLE')
        ]);
        $roomId = $stmt->fetchColumn();
        echo json_encode(["success" => true, "message" => "Room created successfully.", "room_id" => $roomId]);
    } catch (PDOException $e) {
        echo json_encode(["success" => false, "message" => "Database error: " . $e->getMessage()]);
    }

} elseif ($method === 'PATCH') {
    $data = json_decode(file_get_contents("php://input"), true);
    if (!isset($data['room_id'])) {
        echo json_encode(["success" => false, "message" => "Room ID is required."]);
        exit();
    }

    $roomId = (int)$data['room_id'];
    $fields = [];
    $params = ['room_id' => $roomId];

    if (isset($data['room_number'])) {
        $fields[] = "room_number = :room_number";
        $params['room_number'] = trim($data['room_number']);
    }
    if (isset($data['room_type'])) {
        $fields[] = "room_type = :room_type";
        $params['room_type'] = trim($data['room_type']);
    }
    if (isset($data['capacity'])) {
        $fields[] = "capacity = :capacity";
        $params['capacity'] = (int)$data['capacity'];
    }
    if (isset($data['monthly_rent'])) {
        $fields[] = "monthly_rent = :monthly_rent";
        $params['monthly_rent'] = (float)$data['monthly_rent'];
    }
    if (isset($data['status'])) {
        $fields[] = "status = :status::room_status_enum";
        $params['status'] = strtoupper(trim($data['status']));
    }

    if (empty($fields)) {
        echo json_encode(["success" => false, "message" => "No fields to update."]);
        exit();
    }

    $sql = "UPDATE rooms SET " . implode(", ", $fields) . " WHERE room_id = :room_id";
    $stmt = $pdo->prepare($sql);
    $stmt->execute($params);
    echo json_encode(["success" => true, "message" => "Room updated successfully."]);

} elseif ($method === 'DELETE') {
    $data = json_decode(file_get_contents("php://input"), true);
    $roomId = $data['room_id'] ?? ($_GET['room_id'] ?? null);
    if (!$roomId) {
        echo json_encode(["success" => false, "message" => "Room ID is required."]);
        exit();
    }

    $stmt = $pdo->prepare("DELETE FROM rooms WHERE room_id = :room_id");
    $stmt->execute(['room_id' => (int)$roomId]);
    echo json_encode(["success" => true, "message" => "Room deleted successfully."]);
}
?>
