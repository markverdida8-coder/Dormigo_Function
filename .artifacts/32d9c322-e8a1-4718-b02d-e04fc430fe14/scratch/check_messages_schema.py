import urllib.request
import json

script = '''<?php
require_once "db.php";
try {
    $stmt = $pdo->query("SELECT column_name, column_default, data_type FROM information_schema.columns WHERE table_name = 'messages'");
    $cols = $stmt->fetchAll(PDO::FETCH_ASSOC);
    echo json_encode(["success" => true, "columns" => $cols]);
} catch (Exception $e) {
    echo json_encode(["success" => false, "error" => $e->getMessage()]);
}
?>'''

with open(r'C:\xampp\htdocs\Dormigo_Backend\api\test_msg_schema.php', 'w', encoding='utf-8') as f:
    f.write(script)

url = "http://localhost/Dormigo_Backend/api/test_msg_schema.php"
try:
    with urllib.request.urlopen(url) as resp:
        print(resp.read().decode('utf-8'))
except Exception as e:
    print(e)
