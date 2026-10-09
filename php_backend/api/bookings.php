<?php
require_once 'db.php';

$method = $_SERVER['REQUEST_METHOD'];

if ($method === 'GET') {
    $sql = "SELECT b.*, u.full_name, u.email, u.phone, u.profile_image,
                   r.room_number, r.room_type, r.monthly_rent,
                   bh.house_name, bh.address, bh.house_id, bh.landlord_id,
                   ll.full_name AS landlord_name, ll.phone AS landlord_phone
            FROM bookings b
            JOIN users u ON b.user_id = u.user_id
            JOIN rooms r ON b.room_id = r.room_id
            JOIN boarding_houses bh ON r.house_id = bh.house_id
            JOIN users ll ON bh.landlord_id = ll.user_id";
    $params = [];

    if (isset($_GET['booking_id'])) {
        $sql .= " WHERE b.booking_id = :booking_id";
        $params['booking_id'] = (int)$_GET['booking_id'];
    } elseif (isset($_GET['user_id'])) {
        $sql .= " WHERE b.user_id = :user_id";
        $params['user_id'] = (int)$_GET['user_id'];
    } elseif (isset($_GET['landlord_id'])) {
        $sql .= " WHERE bh.landlord_id = :landlord_id";
        $params['landlord_id'] = (int)$_GET['landlord_id'];
    } elseif (isset($_GET['house_id'])) {
        $sql .= " WHERE bh.house_id = :house_id";
        $params['house_id'] = (int)$_GET['house_id'];
    }
    $sql .= " ORDER BY b.booking_id DESC";

    $stmt = $pdo->prepare($sql);
    $stmt->execute($params);
    echo json_encode(["success" => true, "data" => $stmt->fetchAll()]);

} elseif ($method === 'POST') {
    $data = json_decode(file_get_contents("php://input"), true);
    if (!isset($data['user_id']) || !isset($data['room_id']) || !isset($data['move_in_date'])) {
        echo json_encode(["success" => false, "message" => "Required booking details missing."]);
        exit();
    }

    try {
        $pdo->beginTransaction();

        $stmt = $pdo->prepare("INSERT INTO bookings (user_id, room_id, move_in_date, duration_months, status, agreed_monthly_rent, agreed_total_amount, message_to_landlord, created_at)
                                VALUES (:user_id, :room_id, :move_in_date, :duration_months, :status::booking_status_enum, :agreed_monthly_rent, :agreed_total_amount, :message_to_landlord, CURRENT_TIMESTAMP) RETURNING booking_id");
        $stmt->execute([
            'user_id' => $data['user_id'],
            'room_id' => $data['room_id'],
            'move_in_date' => $data['move_in_date'],
            'duration_months' => $data['duration_months'] ?? 1,
            'status' => strtoupper($data['status'] ?? 'PENDING'),
            'agreed_monthly_rent' => $data['agreed_monthly_rent'] ?? 0,
            'agreed_total_amount' => $data['agreed_total_amount'] ?? 0,
            'message_to_landlord' => $data['message_to_landlord'] ?? ''
        ]);
        $bookingId = $stmt->fetchColumn();

        // Get landlord_id to create notification for landlord
        $llStmt = $pdo->prepare("SELECT bh.landlord_id, bh.house_name, r.room_number, u.full_name AS student_name
                                 FROM rooms r
                                 JOIN boarding_houses bh ON r.house_id = bh.house_id
                                 JOIN users u ON u.user_id = :uid
                                 WHERE r.room_id = :rid");
        $llStmt->execute(['uid' => $data['user_id'], 'rid' => $data['room_id']]);
        $bhInfo = $llStmt->fetch();

        if ($bhInfo) {
            $notif = $pdo->prepare("INSERT INTO notifications (user_id, title, message, type, is_read, reference_id, created_at)
                                    VALUES (:uid, :title, :msg, 'REQUEST', FALSE, :ref, CURRENT_TIMESTAMP)");
            $notif->execute([
                'uid' => $bhInfo['landlord_id'],
                'title' => 'New Booking Request',
                'msg' => "{$bhInfo['student_name']} submitted a booking request for {$bhInfo['room_number']} in {$bhInfo['house_name']}.",
                'ref' => $bookingId
            ]);
        }

        $pdo->commit();
        echo json_encode(["success" => true, "message" => "Booking request submitted successfully.", "booking_id" => $bookingId]);
    } catch (PDOException $e) {
        $pdo->rollBack();
        if (strpos($e->getMessage(), 'idx_unique_active_student_booking') !== false) {
            echo json_encode(["success" => false, "message" => "You already have an active or pending booking request. You can only maintain one active room reservation at a time."]);
        } else {
            echo json_encode(["success" => false, "message" => "Database error: " . $e->getMessage()]);
        }
    }

} elseif ($method === 'PATCH') {
    $data = json_decode(file_get_contents("php://input"), true);
    if (!isset($data['booking_id']) || !isset($data['status'])) {
        echo json_encode(["success" => false, "message" => "booking_id and status are required."]);
        exit();
    }

    $bookingId = (int)$data['booking_id'];
    $newStatus = strtoupper($data['status']);

    try {
        $pdo->beginTransaction();

        $stmt = $pdo->prepare("UPDATE bookings SET status = :status::booking_status_enum WHERE booking_id = :booking_id");
        $stmt->execute([
            'booking_id' => $bookingId,
            'status' => $newStatus
        ]);

        // Fetch booking info to notify student & create pending payment if approved
        $bStmt = $pdo->prepare("SELECT b.*, r.room_number, bh.house_name, r.house_id
                                FROM bookings b
                                JOIN rooms r ON b.room_id = r.room_id
                                JOIN boarding_houses bh ON r.house_id = bh.house_id
                                WHERE b.booking_id = :bid");
        $bStmt->execute(['bid' => $bookingId]);
        $booking = $bStmt->fetch();

        if ($booking) {
            $studentId = $booking['user_id'];
            $houseName = $booking['house_name'];
            $roomNum = $booking['room_number'];

            $notifTitle = "Booking Update: $newStatus";
            $notifMsg = "Your booking request for $roomNum at $houseName has been $newStatus.";
            
            $notif = $pdo->prepare("INSERT INTO notifications (user_id, title, message, type, is_read, reference_id, created_at)
                                    VALUES (:uid, :title, :msg, 'REQUEST', FALSE, :ref, CURRENT_TIMESTAMP)");
            $notif->execute([
                'uid' => $studentId,
                'title' => $notifTitle,
                'msg' => $notifMsg,
                'ref' => $bookingId
            ]);

            // If APPROVED, ensure an initial payment record exists
            if ($newStatus === 'APPROVED') {
                $payCheck = $pdo->prepare("SELECT payment_id FROM payments WHERE booking_id = :bid");
                $payCheck->execute(['bid' => $bookingId]);
                if (!$payCheck->fetch()) {
                    $pStmt = $pdo->prepare("INSERT INTO payments (booking_id, payment_period, due_date, amount, payment_method, status, created_at, payment_description)
                                            VALUES (:bid, 1, :due_date, :amt, 'GCash / Cash', 'PENDING', CURRENT_TIMESTAMP, 'Initial Move-in Payment')");
                    $pStmt->execute([
                        'bid' => $bookingId,
                        'due_date' => $booking['move_in_date'],
                        'amt' => $booking['agreed_monthly_rent']
                    ]);
                }
            }

            // If ACTIVE or APPROVED, update room status
            if ($newStatus === 'ACTIVE' || $newStatus === 'APPROVED') {
                $rStmt = $pdo->prepare("UPDATE rooms SET status = 'OCCUPIED' WHERE room_id = :rid");
                $rStmt->execute(['rid' => $booking['room_id']]);
            } elseif ($newStatus === 'CANCELLED' || $newStatus === 'DECLINED') {
                // Return room to AVAILABLE
                $rStmt = $pdo->prepare("UPDATE rooms SET status = 'AVAILABLE' WHERE room_id = :rid");
                $rStmt->execute(['rid' => $booking['room_id']]);
            }
        }

        $pdo->commit();
        echo json_encode(["success" => true, "message" => "Booking status updated successfully."]);
    } catch (PDOException $e) {
        $pdo->rollBack();
        echo json_encode(["success" => false, "message" => "Database error: " . $e->getMessage()]);
    }
}
?>
