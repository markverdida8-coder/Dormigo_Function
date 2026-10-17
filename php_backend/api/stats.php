<?php
require_once 'db.php';

$role = $_GET['role'] ?? 'admin';

if ($role === 'admin') {
    $students = (int)$pdo->query("SELECT COUNT(*) FROM users WHERE user_type = 'STUDENT'")->fetchColumn();
    $landlords = (int)$pdo->query("SELECT COUNT(*) FROM users WHERE user_type = 'LANDLORD'")->fetchColumn();
    $houses = (int)$pdo->query("SELECT COUNT(*) FROM boarding_houses")->fetchColumn();
    $rooms = (int)$pdo->query("SELECT COUNT(*) FROM rooms")->fetchColumn();
    $availRooms = (int)$pdo->query("SELECT COUNT(*) FROM rooms WHERE status = 'AVAILABLE'")->fetchColumn();
    $occupiedRooms = (int)$pdo->query("SELECT COUNT(*) FROM rooms WHERE status = 'OCCUPIED'")->fetchColumn();
    
    $pendingStudentVerif = (int)$pdo->query("SELECT COUNT(*) FROM student_verifications WHERE verification_status = 'PENDING'")->fetchColumn();
    $pendingLandlordVerif = (int)$pdo->query("SELECT COUNT(*) FROM landlord_verifications WHERE verification_status = 'PENDING'")->fetchColumn();
    
    $verifiedStudents = (int)$pdo->query("SELECT COUNT(*) FROM student_verifications WHERE verification_status = 'VERIFIED'")->fetchColumn();
    $verifiedLandlords = (int)$pdo->query("SELECT COUNT(*) FROM landlord_verifications WHERE verification_status = 'VERIFIED'")->fetchColumn();
    $verifiedUsers = $verifiedStudents + $verifiedLandlords;

    $rejectedStudentVerif = (int)$pdo->query("SELECT COUNT(*) FROM student_verifications WHERE verification_status = 'REJECTED'")->fetchColumn();
    $rejectedLandlordVerif = (int)$pdo->query("SELECT COUNT(*) FROM landlord_verifications WHERE verification_status = 'REJECTED'")->fetchColumn();
    $rejectedVerifications = $rejectedStudentVerif + $rejectedLandlordVerif;

    $totalBookings = (int)$pdo->query("SELECT COUNT(*) FROM bookings")->fetchColumn();
    $totalPayments = (float)$pdo->query("SELECT COALESCE(SUM(amount), 0) FROM payments WHERE status IN ('PAID', 'CONFIRMED')")->fetchColumn();

    echo json_encode([
        "success" => true,
        "data" => [
            "total_students" => $students,
            "total_landlords" => $landlords,
            "total_boarding_houses" => $houses,
            "total_rooms" => $rooms,
            "available_rooms" => $availRooms,
            "occupied_rooms" => $occupiedRooms,
            "pending_student_verifications" => $pendingStudentVerif,
            "pending_landlord_verifications" => $pendingLandlordVerif,
            "verified_users" => $verifiedUsers,
            "rejected_verifications" => $rejectedVerifications,
            "total_bookings" => $totalBookings,
            "total_revenue" => $totalPayments
        ]
    ]);

} elseif ($role === 'landlord') {
    if (!isset($_GET['landlord_id'])) {
        echo json_encode(["success" => false, "message" => "landlord_id is required."]);
        exit();
    }
    $lid = (int)$_GET['landlord_id'];

    $houses = (int)$pdo->prepare("SELECT COUNT(*) FROM boarding_houses WHERE landlord_id = ?");
    $houses->execute([$lid]);
    $totalHouses = (int)$houses->fetchColumn();

    $roomsStmt = $pdo->prepare("SELECT COUNT(r.room_id) AS total_rooms,
                                       COUNT(CASE WHEN r.status = 'AVAILABLE' THEN 1 END) AS available_rooms,
                                       COUNT(CASE WHEN r.status = 'OCCUPIED' THEN 1 END) AS occupied_rooms
                                FROM rooms r
                                JOIN boarding_houses bh ON r.house_id = bh.house_id
                                WHERE bh.landlord_id = ?");
    $roomsStmt->execute([$lid]);
    $roomStats = $roomsStmt->fetch();

    $pendingBookingsStmt = $pdo->prepare("SELECT COUNT(b.booking_id)
                                           FROM bookings b
                                           JOIN rooms r ON b.room_id = r.room_id
                                           JOIN boarding_houses bh ON r.house_id = bh.house_id
                                           WHERE bh.landlord_id = ? AND b.status = 'PENDING'");
    $pendingBookingsStmt->execute([$lid]);
    $pendingBookings = (int)$pendingBookingsStmt->fetchColumn();

    $tenantsStmt = $pdo->prepare("SELECT COUNT(DISTINCT b.user_id)
                                  FROM bookings b
                                  JOIN rooms r ON b.room_id = r.room_id
                                  JOIN boarding_houses bh ON r.house_id = bh.house_id
                                  WHERE bh.landlord_id = ? AND b.status IN ('APPROVED', 'ACTIVE')");
    $tenantsStmt->execute([$lid]);
    $currentTenants = (int)$tenantsStmt->fetchColumn();

    $paymentsStmt = $pdo->prepare("SELECT 
                                     COALESCE(SUM(CASE WHEN p.status IN ('PAID', 'CONFIRMED') THEN p.amount ELSE 0 END), 0) AS total_collected,
                                     COALESCE(SUM(CASE WHEN p.status = 'PENDING' THEN p.amount ELSE 0 END), 0) AS total_pending
                                   FROM payments p
                                   JOIN bookings b ON p.booking_id = b.booking_id
                                   JOIN rooms r ON b.room_id = r.room_id
                                   JOIN boarding_houses bh ON r.house_id = bh.house_id
                                   WHERE bh.landlord_id = ?");
    $paymentsStmt->execute([$lid]);
    $paymentStats = $paymentsStmt->fetch();

    echo json_encode([
        "success" => true,
        "data" => [
            "total_properties" => $totalHouses,
            "total_rooms" => (int)($roomStats['total_rooms'] ?? 0),
            "available_rooms" => (int)($roomStats['available_rooms'] ?? 0),
            "occupied_rooms" => (int)($roomStats['occupied_rooms'] ?? 0),
            "pending_booking_requests" => $pendingBookings,
            "current_tenants" => $currentTenants,
            "total_collected" => (float)($paymentStats['total_collected'] ?? 0),
            "total_pending" => (float)($paymentStats['total_pending'] ?? 0)
        ]
    ]);
}
?>
