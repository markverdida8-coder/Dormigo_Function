path = r'C:\xampp\htdocs\Dormigo_Backend\api\boarding_houses.php'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

old_prep = '''$roomStmt = $pdo->prepare("INSERT INTO rooms (house_id, room_number, room_type, capacity, monthly_rent, status)
                                   VALUES (:house_id, :room_number, :room_type, :capacity, :monthly_rent, :status::room_status_enum)");'''

new_prep = '''$roomStmt = $pdo->prepare("INSERT INTO rooms (house_id, room_number, room_type, capacity, monthly_rent, status, advance_months, deposit_months, other_fees, other_fees_description, deposit_refund_policy)
                                   VALUES (:house_id, :room_number, :room_type, :capacity, :monthly_rent, :status::room_status_enum, :advance_months, :deposit_months, :other_fees, :other_fees_description, :deposit_refund_policy)");'''

old_exec = '''                $roomStmt->execute([
                    'house_id' => $houseId,
                    'room_number' => $room['room_number'] ?? 'Room 1',
                    'room_type' => $room['room_type'] ?? 'Single',
                    'capacity' => $room['capacity'] ?? 1,
                    'monthly_rent' => $room['monthly_rent'] ?? 0,
                    'status' => strtoupper($room['status'] ?? 'AVAILABLE')
                ]);'''

new_exec = '''                $roomStmt->execute([
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

content = content.replace(old_prep, new_prep)
content = content.replace(old_exec, new_exec)

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)

print("Updated boarding_houses.php successfully!")
