import urllib.request
import json

script = '''<?php
require_once "db.php";
try {
    $stmt = $pdo->prepare("INSERT INTO notifications (user_id, title, message, type, reference_id) VALUES (1, 'Test', 'Msg', 'BOOKING', 1)");
    $stmt->execute();
    echo json_encode(["success" => true]);
} catch (Exception $e) {
    echo json_encode(["success" => false, "error" => $e->getMessage()]);
}
?>'''

with open(r'C:\xampp\htdocs\Dormigo_Backend\api\test_notif.php', 'w', encoding='utf-8') as f:
    f.write(script)

url = "http://localhost/Dormigo_Backend/api/test_notif.php"
try:
    with urllib.request.urlopen(url) as resp:
        print(resp.read().decode('utf-8'))
except Exception as e:
    print(e)
