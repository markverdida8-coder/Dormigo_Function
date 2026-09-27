import urllib.request
import json

# Let's find a room_id for house_id 8 first or insert booking via SQL/PHP
# Since we have direct access to php/db, let's call a quick php script to insert a completed booking
script = '''<?php
require_once "db.php";
$room = $pdo->query("SELECT room_id FROM rooms WHERE house_id = 8 LIMIT 1")->fetch();
if ($room) {
    $roomId = $room['room_id'];
    $stmt = $pdo->prepare("INSERT INTO bookings (user_id, room_id, move_in_date, duration_months, status, agreed_monthly_rent, agreed_total_amount) VALUES (1, ?, '2026-09-01', 3, 'COMPLETED'::booking_status_enum, 5000, 15000) RETURNING booking_id");
    $stmt->execute([$roomId]);
    echo json_encode(["success" => true, "booking_id" => $stmt->fetchColumn()]);
} else {
    echo json_encode(["success" => false, "message" => "No room found for house 8"]);
}
?>'''

with open(r'C:\xampp\htdocs\Dormigo_Backend\api\insert_test_booking.php', 'w', encoding='utf-8') as f:
    f.write(script)

url = "http://localhost/Dormigo_Backend/api/insert_test_booking.php"
try:
    with urllib.request.urlopen(url) as resp:
        print(resp.read().decode('utf-8'))
except Exception as e:
    print(e)
