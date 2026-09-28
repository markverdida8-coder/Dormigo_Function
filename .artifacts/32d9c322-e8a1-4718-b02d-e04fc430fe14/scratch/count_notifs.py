import urllib.request
import json

script = '''<?php
require_once "db.php";
try {
    $stmt = $pdo->query("SELECT * FROM notifications");
    $cols = $stmt->fetchAll(PDO::FETCH_ASSOC);
    echo json_encode(["success" => true, "data" => $cols]);
} catch (Exception $e) {
    echo json_encode(["success" => false, "error" => $e->getMessage()]);
}
?>'''

with open(r'C:\xampp\htdocs\Dormigo_Backend\api\test_notif.php', 'w', encoding='utf-8') as f:
    f.write(script)

url = "http://localhost/Dormigo_Backend/api/test_notif.php"
try:
    with urllib.request.urlopen(url) as resp:
        data = json.loads(resp.read().decode('utf-8'))
        print("Total notifications:", len(data.get('data', [])))
        if len(data.get('data', [])) > 0:
            print("First notification:", data.get('data')[0])
except Exception as e:
    print(e)
