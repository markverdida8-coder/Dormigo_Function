# Update rooms.php
rooms_path = r'C:\xampp\htdocs\Dormigo_Backend\api\rooms.php'
with open(rooms_path, 'r', encoding='utf-8') as f:
    rooms_code = f.read()

old_r_ins = '''INSERT INTO rooms (house_id, room_number, room_type, capacity, monthly_rent, status, advance_months, deposit_months, other_fees, other_fees_description, deposit_refund_policy)
                            VALUES (:house_id, :room_number, :room_type, :capacity, :monthly_rent, :status::room_status_enum, :advance_months, :deposit_months, :other_fees, :other_fees_description, :deposit_refund_policy)'''

new_r_ins = '''INSERT INTO rooms (house_id, room_number, room_type, capacity, monthly_rent, status, advance_months, deposit_months, utility_deposit, other_fees, other_fees_description, deposit_refund_policy)
                            VALUES (:house_id, :room_number, :room_type, :capacity, :monthly_rent, :status::room_status_enum, :advance_months, :deposit_months, :utility_deposit, :other_fees, :other_fees_description, :deposit_refund_policy)'''

old_r_exec = '''    $stmt->execute([
        'house_id' => $data['house_id'],
        'room_number' => $data['room_number'] ?? 'Room 1',
        'room_type' => $data['room_type'] ?? 'Bedspace',
        'capacity' => $data['capacity'] ?? 1,
        'monthly_rent' => $data['monthly_rent'] ?? 0,
        'status' => strtoupper($data['status'] ?? 'AVAILABLE'),
        'advance_months' => $data['advance_months'] ?? 1,
        'deposit_months' => $data['deposit_months'] ?? 1,
        'other_fees' => $data['other_fees'] ?? 0.00,
        'other_fees_description' => $data['other_fees_description'] ?? '',
        'deposit_refund_policy' => $data['deposit_refund_policy'] ?? ''
    ]);'''

new_r_exec = '''    $stmt->execute([
        'house_id' => $data['house_id'],
        'room_number' => $data['room_number'] ?? 'Room 1',
        'room_type' => $data['room_type'] ?? 'Bedspace',
        'capacity' => $data['capacity'] ?? 1,
        'monthly_rent' => $data['monthly_rent'] ?? 0,
        'status' => strtoupper($data['status'] ?? 'AVAILABLE'),
        'advance_months' => $data['advance_months'] ?? 1,
        'deposit_months' => $data['deposit_months'] ?? 1,
        'utility_deposit' => $data['utility_deposit'] ?? 0.00,
        'other_fees' => $data['other_fees'] ?? 0.00,
        'other_fees_description' => $data['other_fees_description'] ?? '',
        'deposit_refund_policy' => $data['deposit_refund_policy'] ?? ''
    ]);'''

rooms_code = rooms_code.replace(old_r_ins, new_r_ins)
rooms_code = rooms_code.replace(old_r_exec, new_r_exec)

with open(rooms_path, 'w', encoding='utf-8') as f:
    f.write(rooms_code)
print("Updated rooms.php successfully!")

# Update boarding_houses.php
bh_path = r'C:\xampp\htdocs\Dormigo_Backend\api\boarding_houses.php'
with open(bh_path, 'r', encoding='utf-8') as f:
    bh_code = f.read()

old_bh_prep = '''$roomStmt = $pdo->prepare("INSERT INTO rooms (house_id, room_number, room_type, capacity, monthly_rent, status, advance_months, deposit_months, other_fees, other_fees_description, deposit_refund_policy)
                                   VALUES (:house_id, :room_number, :room_type, :capacity, :monthly_rent, :status::room_status_enum, :advance_months, :deposit_months, :other_fees, :other_fees_description, :deposit_refund_policy)");'''

new_bh_prep = '''$roomStmt = $pdo->prepare("INSERT INTO rooms (house_id, room_number, room_type, capacity, monthly_rent, status, advance_months, deposit_months, utility_deposit, other_fees, other_fees_description, deposit_refund_policy)
                                   VALUES (:house_id, :room_number, :room_type, :capacity, :monthly_rent, :status::room_status_enum, :advance_months, :deposit_months, :utility_deposit, :other_fees, :other_fees_description, :deposit_refund_policy)");'''

old_bh_exec = '''                $roomStmt->execute([
                    'house_id' => $houseId,
                    'room_number' => $room['room_number'] ?? 'Room 1',
                    'room_type' => $room['room_type'] ?? 'Bedspace',
                    'capacity' => $room['capacity'] ?? 1,
                    'monthly_rent' => $room['monthly_rent'] ?? 0,
                    'status' => strtoupper($room['status'] ?? 'AVAILABLE'),
                    'advance_months' => $room['advance_months'] ?? 1,
                    'deposit_months' => $room['deposit_months'] ?? 1,
                    'other_fees' => $room['other_fees'] ?? 0.00,
                    'other_fees_description' => $room['other_fees_description'] ?? '',
                    'deposit_refund_policy' => $room['deposit_refund_policy'] ?? ''
                ]);'''

new_bh_exec = '''                $roomStmt->execute([
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
                ]);'''

bh_code = bh_code.replace(old_bh_prep, new_bh_prep)
bh_code = bh_code.replace(old_bh_exec, new_bh_exec)

with open(bh_path, 'w', encoding='utf-8') as f:
    f.write(bh_code)
print("Updated boarding_houses.php successfully!")
