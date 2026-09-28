path = r'C:\xampp\htdocs\Dormigo_Backend\api\payments.php'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

old_post = '''    $stmt->execute([
        'booking_id' => $data['booking_id'],
        'payment_period' => $data['payment_period'] ?? 1,
        'due_date' => $data['due_date'],
        'amount' => $data['amount'],
        'payment_method' => $data['payment_method'],
        'payment_date' => $data['payment_date'] ?? null,
        'status' => strtoupper($data['status'] ?? 'PENDING'),
        'transaction_ref' => $data['transaction_ref'] ?? null,
        'payment_description' => $data['payment_description'] ?? 'Monthly Rent'
    ]);
    echo json_encode(["success" => true, "message" => "Payment created successfully.", "payment_id" => $stmt->fetchColumn()]);'''

new_post = '''    $stmt->execute([
        'booking_id' => $data['booking_id'],
        'payment_period' => $data['payment_period'] ?? 1,
        'due_date' => $data['due_date'],
        'amount' => $data['amount'],
        'payment_method' => $data['payment_method'],
        'payment_date' => $data['payment_date'] ?? null,
        'status' => strtoupper($data['status'] ?? 'PENDING'),
        'transaction_ref' => $data['transaction_ref'] ?? null,
        'payment_description' => $data['payment_description'] ?? 'Monthly Rent'
    ]);
    $paymentId = $stmt->fetchColumn();

    $info = $pdo->prepare("SELECT u.full_name, bh.landlord_id FROM bookings b JOIN users u ON b.user_id = u.user_id JOIN rooms r ON b.room_id = r.room_id JOIN boarding_houses bh ON r.house_id = bh.house_id WHERE b.booking_id = ?");
    $info->execute([$data['booking_id']]);
    $infoData = $info->fetch();
    if ($infoData && $infoData['landlord_id']) {
        $nStmt = $pdo->prepare("INSERT INTO notifications (user_id, title, message, type, reference_id) VALUES (?, 'Payment Submitted', ?, 'PAYMENT', ?)");
        $fmtAmount = number_format((float)$data['amount'], 2, '.', ',');
        $nStmt->execute([$infoData['landlord_id'], $infoData['full_name'] . ' submitted a payment of ₱' . $fmtAmount . '.', $paymentId]);
    }

    echo json_encode(["success" => true, "message" => "Payment created successfully.", "payment_id" => $paymentId]);'''

old_patch = '''    $stmt->execute([
        'payment_id' => $data['payment_id'],
        'status' => strtoupper($data['status']),
        'payment_date' => $data['payment_date'] ?? date('Y-m-d'),
        'transaction_ref' => $data['transaction_ref'] ?? null
    ]);
    echo json_encode(["success" => true, "message" => "Payment updated successfully."]);'''

new_patch = '''    $stmt->execute([
        'payment_id' => $data['payment_id'],
        'status' => strtoupper($data['status']),
        'payment_date' => $data['payment_date'] ?? date('Y-m-d'),
        'transaction_ref' => $data['transaction_ref'] ?? null
    ]);

    if (strtoupper($data['status']) === 'PAID' || strtoupper($data['status']) === 'CONFIRMED') {
        $info = $pdo->prepare("SELECT b.user_id FROM payments p JOIN bookings b ON p.booking_id = b.booking_id WHERE p.payment_id = ?");
        $info->execute([$data['payment_id']]);
        $infoData = $info->fetch();
        if ($infoData) {
            $nStmt = $pdo->prepare("INSERT INTO notifications (user_id, title, message, type, reference_id) VALUES (?, 'Payment Confirmed', 'Your payment has been confirmed.', 'PAYMENT', ?)");
            $nStmt->execute([$infoData['user_id'], $data['payment_id']]);
        }
    }
    echo json_encode(["success" => true, "message" => "Payment updated successfully."]);'''

content = content.replace(old_post, new_post)
content = content.replace(old_patch, new_patch)
with open(path, 'w', encoding='utf-8') as f:
    f.write(content)
print("Updated payments.php")
