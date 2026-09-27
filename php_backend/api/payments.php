<?php
require_once 'db.php';

$method = $_SERVER['REQUEST_METHOD'];

if ($method === 'GET') {
    $sql = "SELECT p.*, u.full_name AS tenant_name, bh.house_name, r.room_number, r.room_type
            FROM payments p
            JOIN bookings b ON p.booking_id = b.booking_id
            JOIN users u ON b.user_id = u.user_id
            JOIN rooms r ON b.room_id = r.room_id
            JOIN boarding_houses bh ON r.house_id = bh.house_id";
    $params = [];

    if (isset($_GET['payment_id'])) {
        $sql .= " WHERE p.payment_id = :payment_id";
        $params['payment_id'] = $_GET['payment_id'];
    } elseif (isset($_GET['booking_id'])) {
        $sql .= " WHERE p.booking_id = :booking_id";
        $params['booking_id'] = $_GET['booking_id'];
    } elseif (isset($_GET['user_id'])) {
        $sql .= " WHERE b.user_id = :user_id";
        $params['user_id'] = $_GET['user_id'];
    } elseif (isset($_GET['landlord_id'])) {
        $sql .= " WHERE bh.landlord_id = :landlord_id";
        $params['landlord_id'] = $_GET['landlord_id'];
    }
    $sql .= " ORDER BY p.payment_id DESC";

    $stmt = $pdo->prepare($sql);
    $stmt->execute($params);
    echo json_encode(["success" => true, "data" => $stmt->fetchAll()]);

} elseif ($method === 'POST') {
    $data = json_decode(file_get_contents("php://input"), true);
    $stmt = $pdo->prepare("INSERT INTO payments (booking_id, payment_period, due_date, amount, payment_method, payment_date, status, transaction_ref)
                            VALUES (:booking_id, :payment_period, :due_date, :amount, :payment_method, :payment_date, :status::payment_status_enum, :transaction_ref) RETURNING payment_id");
    $stmt->execute([
        'booking_id' => $data['booking_id'],
        'payment_period' => $data['payment_period'] ?? 1,
        'due_date' => $data['due_date'],
        'amount' => $data['amount'],
        'payment_method' => $data['payment_method'],
        'payment_date' => $data['payment_date'] ?? null,
        'status' => strtoupper($data['status'] ?? 'PENDING'),
        'transaction_ref' => $data['transaction_ref'] ?? null
    ]);
    echo json_encode(["success" => true, "message" => "Payment created successfully.", "payment_id" => $stmt->fetchColumn()]);

} elseif ($method === 'PATCH') {
    $data = json_decode(file_get_contents("php://input"), true);
    $stmt = $pdo->prepare("UPDATE payments SET status = :status::payment_status_enum, payment_date = :payment_date, transaction_ref = :transaction_ref WHERE payment_id = :payment_id");
    $stmt->execute([
        'payment_id' => $data['payment_id'],
        'status' => strtoupper($data['status']),
        'payment_date' => $data['payment_date'] ?? date('Y-m-d'),
        'transaction_ref' => $data['transaction_ref'] ?? null
    ]);
    echo json_encode(["success" => true, "message" => "Payment updated successfully."]);
}
?>
