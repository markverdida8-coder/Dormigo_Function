path = r'C:\xampp\htdocs\Dormigo_Backend\api\boarding_houses.php'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

old_insert = '''        $stmt = $pdo->prepare("INSERT INTO boarding_houses (landlord_id, house_name, description, address, house_rules, status)
                                VALUES (:landlord_id, :house_name, :description, :address, :house_rules, :status::house_status_enum) RETURNING house_id");
        $stmt->execute([
            'landlord_id' => $data['landlord_id'],
            'house_name' => $data['house_name'],
            'description' => $data['description'] ?? '',
            'address' => $data['address'] ?? '',
            'house_rules' => $data['house_rules'] ?? '',
            'status' => strtoupper($data['status'] ?? 'ACTIVE')
        ]);'''

new_insert = '''        $stmt = $pdo->prepare("INSERT INTO boarding_houses (landlord_id, house_name, description, address, house_rules, status, free_electricity, electricity_rate, free_water, water_rate)
                                VALUES (:landlord_id, :house_name, :description, :address, :house_rules, :status::house_status_enum, :free_electricity, :electricity_rate, :free_water, :water_rate) RETURNING house_id");
        $stmt->execute([
            'landlord_id' => $data['landlord_id'],
            'house_name' => $data['house_name'],
            'description' => $data['description'] ?? '',
            'address' => $data['address'] ?? '',
            'house_rules' => $data['house_rules'] ?? '',
            'status' => strtoupper($data['status'] ?? 'ACTIVE'),
            'free_electricity' => isset($data['free_electricity']) ? ($data['free_electricity'] ? 1 : 0) : 0,
            'electricity_rate' => $data['electricity_rate'] ?? '0.00',
            'free_water' => isset($data['free_water']) ? ($data['free_water'] ? 1 : 0) : 0,
            'water_rate' => $data['water_rate'] ?? '0.00'
        ]);'''

if old_insert in content:
    content = content.replace(old_insert, new_insert)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)
    print("Updated boarding_houses.php insert successfully!")
else:
    print("Old insert block not found.")
