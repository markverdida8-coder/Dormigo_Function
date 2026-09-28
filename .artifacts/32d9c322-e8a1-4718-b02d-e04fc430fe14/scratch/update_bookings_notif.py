path = r'C:\xampp\htdocs\Dormigo_Backend\api\bookings.php'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

old_post = '''    $stmt->execute([
        'user_id' => $data['user_id'],
        'room_id' => $data['room_id'],
        'move_in_date' => $data['move_in_date'],
        'duration_months' => $data['duration_months'],
        'status' => strtoupper($data['status'] ?? 'PENDING'),
        'agreed_monthly_rent' => $data['agreed_monthly_rent'],
        'agreed_total_amount' => $data['agreed_total_amount'],
        'message_to_landlord' => $data['message_to_landlord'] ?? ''
    ]);
    echo json_encode(["success" => true, "message" => "Booking request submitted successfully.", "booking_id" => $stmt->fetchColumn()]);'''

new_post = '''    $stmt->execute([
        'user_id' => $data['user_id'],
        'room_id' => $data['room_id'],
        'move_in_date' => $data['move_in_date'],
        'duration_months' => $data['duration_months'],
        'status' => strtoupper($data['status'] ?? 'PENDING'),
        'agreed_monthly_rent' => $data['agreed_monthly_rent'],
        'agreed_total_amount' => $data['agreed_total_amount'],
        'message_to_landlord' => $data['message_to_landlord'] ?? ''
    ]);
    $bookingId = $stmt->fetchColumn();

    // Get info for notification
    $info = $pdo->prepare("SELECT u.full_name, bh.landlord_id, r.room_number, r.room_type FROM rooms r JOIN boarding_houses bh ON r.house_id = bh.house_id JOIN users u ON u.user_id = :uid WHERE r.room_id = :rid");
    $info->execute(['uid' => $data['user_id'], 'rid' => $data['room_id']]);
    $infoData = $info->fetch();
    if ($infoData && $infoData['landlord_id']) {
        $nStmt = $pdo->prepare("INSERT INTO notifications (user_id, title, message, type, reference_id) VALUES (?, ?, ?, 'BOOKING', ?)");
        $rn = !empty(trim($infoData['room_number'])) ? trim($infoData['room_number']) : trim($infoData['room_type']);
        $nStmt->execute([$infoData['landlord_id'], 'New Booking Request', $infoData['full_name'] . ' requested ' . $rn . '.', $bookingId]);
    }

    echo json_encode(["success" => true, "message" => "Booking request submitted successfully.", "booking_id" => $bookingId]);'''

old_patch = '''    if (!empty($fields)) {
        $sql = "UPDATE bookings SET " . implode(", ", $fields) . " WHERE booking_id = :booking_id";
        $stmt = $pdo->prepare($sql);
        $stmt->execute($params);
    }
    echo json_encode(["success" => true, "message" => "Booking updated successfully."]);'''

new_patch = '''    if (!empty($fields)) {
        $sql = "UPDATE bookings SET " . implode(", ", $fields) . " WHERE booking_id = :booking_id";
        $stmt = $pdo->prepare($sql);
        $stmt->execute($params);

        if (isset($data['status'])) {
            $status = strtoupper($data['status']);
            if ($status === 'APPROVED' || $status === 'REJECTED' || $status === 'DECLINED') {
                $info = $pdo->prepare("SELECT b.user_id, bh.house_name FROM bookings b JOIN rooms r ON b.room_id = r.room_id JOIN boarding_houses bh ON r.house_id = bh.house_id WHERE b.booking_id = ?");
                $info->execute([$data['booking_id']]);
                $infoData = $info->fetch();
                if ($infoData) {
                    $nStmt = $pdo->prepare("INSERT INTO notifications (user_id, title, message, type, reference_id) VALUES (?, ?, ?, 'BOOKING', ?)");
                    if ($status === 'APPROVED') {
                        $nStmt->execute([$infoData['user_id'], 'Booking Approved', 'Your booking for ' . $infoData['house_name'] . ' has been approved.', $data['booking_id']]);
                    } else {
                        $nStmt->execute([$infoData['user_id'], 'Booking Declined', 'Your booking request for ' . $infoData['house_name'] . ' has been declined.', $data['booking_id']]);
                    }
                }
            }
        }
    }
    echo json_encode(["success" => true, "message" => "Booking updated successfully."]);'''

content = content.replace(old_post, new_post)
content = content.replace(old_patch, new_patch)
with open(path, 'w', encoding='utf-8') as f:
    f.write(content)
print("Updated bookings.php")
