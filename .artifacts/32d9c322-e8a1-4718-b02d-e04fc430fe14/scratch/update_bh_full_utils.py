path = r'C:\xampp\htdocs\Dormigo_Backend\api\boarding_houses.php'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

# Update INSERT execution
old_exec = '''        $stmt->execute([
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

new_exec = '''        $stmt->execute([
            'landlord_id' => $data['landlord_id'],
            'house_name' => $data['house_name'],
            'description' => $data['description'] ?? '',
            'address' => $data['address'] ?? '',
            'house_rules' => $data['house_rules'] ?? '',
            'status' => strtoupper($data['status'] ?? 'ACTIVE'),
            'free_electricity' => filter_var($data['free_electricity'] ?? false, FILTER_VALIDATE_BOOLEAN) ? 1 : 0,
            'electricity_rate' => $data['electricity_rate'] ?? '0.00',
            'free_water' => filter_var($data['free_water'] ?? false, FILTER_VALIDATE_BOOLEAN) ? 1 : 0,
            'water_rate' => $data['water_rate'] ?? '0.00'
        ]);'''

if old_exec in content:
    content = content.replace(old_exec, new_exec)

# Update PATCH block
old_patch = '''} elseif ($method === 'PATCH') {
    $data = json_decode(file_get_contents("php://input"), true);
    if (!isset($data['house_id'])) {
        echo json_encode(["success" => false, "message" => "House ID is required."]);
        exit();
    }
    $stmt = $pdo->prepare("UPDATE boarding_houses SET house_name = :house_name, description = :description, address = :address, house_rules = :house_rules, status = :status::house_status_enum WHERE house_id = :house_id");
    $stmt->execute([
        'house_id' => $data['house_id'],
        'house_name' => $data['house_name'],
        'description' => $data['description'] ?? '',
        'address' => $data['address'] ?? '',
        'house_rules' => $data['house_rules'] ?? '',
        'status' => strtoupper($data['status'] ?? 'ACTIVE')
    ]);
    echo json_encode(["success" => true, "message" => "Boarding house updated successfully."]);'''

new_patch = '''} elseif ($method === 'PATCH') {
    $data = json_decode(file_get_contents("php://input"), true);
    if (!isset($data['house_id'])) {
        echo json_encode(["success" => false, "message" => "House ID is required."]);
        exit();
    }
    $stmt = $pdo->prepare("UPDATE boarding_houses SET house_name = :house_name, description = :description, address = :address, house_rules = :house_rules, status = :status::house_status_enum, free_electricity = :free_electricity, electricity_rate = :electricity_rate, free_water = :free_water, water_rate = :water_rate WHERE house_id = :house_id");
    $stmt->execute([
        'house_id' => $data['house_id'],
        'house_name' => $data['house_name'],
        'description' => $data['description'] ?? '',
        'address' => $data['address'] ?? '',
        'house_rules' => $data['house_rules'] ?? '',
        'status' => strtoupper($data['status'] ?? 'ACTIVE'),
        'free_electricity' => filter_var($data['free_electricity'] ?? false, FILTER_VALIDATE_BOOLEAN) ? 1 : 0,
        'electricity_rate' => $data['electricity_rate'] ?? '0.00',
        'free_water' => filter_var($data['free_water'] ?? false, FILTER_VALIDATE_BOOLEAN) ? 1 : 0,
        'water_rate' => $data['water_rate'] ?? '0.00'
    ]);
    echo json_encode(["success" => true, "message" => "Boarding house updated successfully."]);'''

if old_patch in content:
    content = content.replace(old_patch, new_patch)

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)

print("Updated boarding_houses.php fully!")
