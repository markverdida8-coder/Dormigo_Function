<?php
require_once 'db.php';

$method = $_SERVER['REQUEST_METHOD'];

if ($method === 'GET') {
    $sql = "SELECT p.*,
                   u.user_id AS student_id,
                   u.full_name AS tenant_name,
                   u.email AS tenant_email,
                   u.phone AS tenant_phone,
                   bh.house_id,
                   bh.house_name,
                   bh.landlord_id,
                   r.room_id,
                   r.room_number,
                   r.room_type,
                   b.move_in_date,
                   b.duration_months
            FROM payments p
            JOIN bookings b ON p.booking_id = b.booking_id
            JOIN users u ON b.user_id = u.user_id
            JOIN rooms r ON b.room_id = r.room_id
            JOIN boarding_houses bh ON r.house_id = bh.house_id";
    $params = [];
    $where = [];

    if (isset($_GET['payment_id'])) {
        $where[] = "p.payment_id = :payment_id";
        $params['payment_id'] = (int)$_GET['payment_id'];
    }
    if (isset($_GET['booking_id'])) {
        $where[] = "p.booking_id = :booking_id";
        $params['booking_id'] = (int)$_GET['booking_id'];
    }
    if (isset($_GET['user_id'])) {
        $where[] = "b.user_id = :user_id";
        $params['user_id'] = (int)$_GET['user_id'];
    }
    if (isset($_GET['landlord_id'])) {
        $where[] = "bh.landlord_id = :landlord_id";
        $params['landlord_id'] = (int)$_GET['landlord_id'];
    }
    if (isset($_GET['house_id'])) {
        $where[] = "bh.house_id = :house_id";
        $params['house_id'] = (int)$_GET['house_id'];
    }
    if (isset($_GET['status'])) {
        $where[] = "p.status = :status::payment_status_enum";
        $params['status'] = strtoupper(trim($_GET['status']));
    }
    if (isset($_GET['date'])) {
        $where[] = "p.due_date = :date";
        $params['date'] = trim($_GET['date']);
    }

    if (!empty($where)) {
        $sql .= " WHERE " . implode(" AND ", $where);
    }
    $sql .= " ORDER BY p.payment_id DESC";

    $stmt = $pdo->prepare($sql);
    $stmt->execute($params);
    echo json_encode(["success" => true, "data" => $stmt->fetchAll()]);

} elseif ($method === 'POST') {
    $data = json_decode(file_get_contents("php://input"), true);
    if (!isset($data['booking_id']) || !isset($data['amount'])) {
        echo json_encode(["success" => false, "message" => "booking_id and amount are required."]);
        exit();
    }

    try {
        $stmt = $pdo->prepare("INSERT INTO payments (booking_id, payment_period, due_date, amount, payment_method, payment_date, status, transaction_ref, payment_description, created_at)
                                VALUES (:booking_id, :payment_period, :due_date, :amount, :payment_method, :payment_date, :status::payment_status_enum, :transaction_ref, :payment_description, CURRENT_TIMESTAMP) RETURNING payment_id");
        $stmt->execute([
            'booking_id' => $data['booking_id'],
            'payment_period' => $data['payment_period'] ?? 1,
            'due_date' => $data['due_date'] ?? date('Y-m-d'),
            'amount' => $data['amount'],
            'payment_method' => $data['payment_method'] ?? 'GCash',
            'payment_date' => $data['payment_date'] ?? date('Y-m-d'),
            'status' => strtoupper($data['status'] ?? 'PENDING'),
            'transaction_ref' => $data['transaction_ref'] ?? null,
            'payment_description' => $data['payment_description'] ?? 'Rent Payment'
        ]);
        $paymentId = $stmt->fetchColumn();

        // Notify landlord
        $bInfo = $pdo->prepare("SELECT bh.landlord_id, bh.house_name, u.full_name AS student_name
                                FROM bookings b
                                JOIN rooms r ON b.room_id = r.room_id
                                JOIN boarding_houses bh ON r.house_id = bh.house_id
                                JOIN users u ON b.user_id = u.user_id
                                WHERE b.booking_id = :bid");
        $bInfo->execute(['bid' => $data['booking_id']]);
        $b = $bInfo->fetch();
        if ($b) {
            $notif = $pdo->prepare("INSERT INTO notifications (user_id, title, message, type, is_read, reference_id, created_at)
                                    VALUES (:uid, 'New Payment Recorded', :msg, 'PAYMENT', FALSE, :ref, CURRENT_TIMESTAMP)");
            $notif->execute([
                'uid' => $b['landlord_id'],
                'msg' => "Payment of ₱" . number_format($data['amount'], 2) . " from {$b['student_name']} ({$b['house_name']}).",
                'ref' => $paymentId
            ]);
        }

        echo json_encode(["success" => true, "message" => "Payment created successfully.", "payment_id" => $paymentId]);
    } catch (PDOException $e) {
        echo json_encode(["success" => false, "message" => "Database error: " . $e->getMessage()]);
    }

} elseif ($method === 'PATCH') {
    $data = json_decode(file_get_contents("php://input"), true);
    if (!isset($data['payment_id']) || !isset($data['status'])) {
        echo json_encode(["success" => false, "message" => "payment_id and status are required."]);
        exit();
    }

    $paymentId = (int)$data['payment_id'];
    $newStatus = strtoupper(trim($data['status']));

    try {
        $stmt = $pdo->prepare("UPDATE payments SET 
                                status = :status::payment_status_enum,
                                payment_date = COALESCE(:payment_date, payment_date),
                                transaction_ref = COALESCE(:transaction_ref, transaction_ref),
                                payment_method = COALESCE(:payment_method, payment_method)
                                WHERE payment_id = :payment_id");
        $stmt->execute([
            'payment_id' => $paymentId,
            'status' => $newStatus,
            'payment_date' => $data['payment_date'] ?? date('Y-m-d'),
            'transaction_ref' => $data['transaction_ref'] ?? null,
            'payment_method' => $data['payment_method'] ?? null
        ]);

        // Notify student or landlord
        $pInfo = $pdo->prepare("SELECT p.amount, b.user_id AS student_id, bh.landlord_id, bh.house_name
                                FROM payments p
                                JOIN bookings b ON p.booking_id = b.booking_id
                                JOIN rooms r ON b.room_id = r.room_id
                                JOIN boarding_houses bh ON r.house_id = bh.house_id
                                WHERE p.payment_id = :pid");
        $pInfo->execute(['pid' => $paymentId]);
        $pi = $pInfo->fetch();
        if ($pi) {
            $studentNotif = $pdo->prepare("INSERT INTO notifications (user_id, title, message, type, is_read, reference_id, created_at)
                                          VALUES (:uid, :title, :msg, 'PAYMENT', FALSE, :ref, CURRENT_TIMESTAMP)");
            $studentNotif->execute([
                'uid' => $pi['student_id'],
                'title' => "Payment Status: {$newStatus}",
                'msg' => "Your payment of ₱" . number_format($pi['amount'], 2) . " for {$pi['house_name']} has been marked as {$newStatus}.",
                'ref' => $paymentId
            ]);
        }

        echo json_encode(["success" => true, "message" => "Payment updated successfully."]);
    } catch (PDOException $e) {
        echo json_encode(["success" => false, "message" => "Database error: " . $e->getMessage()]);
    }
}
?>
