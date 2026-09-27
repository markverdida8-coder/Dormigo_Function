import re
path = r'C:\xampp\htdocs\Dormigo_Backend\api\boarding_houses.php'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

pattern = r'\$houseId\s*=\s*\$stmt->fetchColumn\(\);'
addition = """$houseId = $stmt->fetchColumn();

        $verifStmt = $pdo->prepare("INSERT INTO boarding_house_verification (house_id, verification_status) VALUES (:house_id, 'PENDING') ON CONFLICT DO NOTHING");
        $verifStmt->execute(['house_id' => $houseId]);"""

if re.search(pattern, content) and "boarding_house_verification" not in content:
    content = re.sub(pattern, addition, content, count=1)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)
    print("Updated boarding_houses.php via regex successfully!")
else:
    print("Pattern not found or already updated.")
