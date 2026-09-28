import urllib.request
import json

script = '''<?php
require_once "db.php";
try {
    $stmt = $pdo->prepare("UPDATE boarding_houses SET gcash_qr_code = 'uploads/gcash_qr/qr_house_11_test.jpg', gcash_updated_at = CURRENT_TIMESTAMP WHERE house_id = 11");
    $stmt->execute();
    echo json_encode(["success" => true]);
} catch (Exception $e) {
    echo json_encode(["success" => false, "error" => $e->getMessage()]);
}
?>'''

with open(r'C:\xampp\htdocs\Dormigo_Backend\api\test_pop_qr.php', 'w', encoding='utf-8') as f:
    f.write(script)

url = "http://localhost/Dormigo_Backend/api/test_pop_qr.php"
try:
    with urllib.request.urlopen(url) as resp:
        print(resp.read().decode('utf-8'))
except Exception as e:
    print(e)
