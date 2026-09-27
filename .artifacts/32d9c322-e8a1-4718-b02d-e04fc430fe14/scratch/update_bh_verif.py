path = r'C:\xampp\htdocs\Dormigo_Backend\api\boarding_houses.php'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

target = "$houseId = $stmt->fetchColumn();"
addition = """$houseId = $stmt->fetchColumn();

        $verifStmt = $pdo->prepare("INSERT INTO boarding_house_verification (house_id, verification_status) VALUES (:house_id, 'PENDING') ON CONFLICT DO NOTHING");
        $verifStmt->execute(['house_id' => $houseId]);"""

if target in content and "boarding_house_verification" not in content:
    content = content.replace(target, addition, 1)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)
    print("Updated boarding_houses.php with auto-verification insertion successfully!")
else:
    print("Target not found or already updated.")
