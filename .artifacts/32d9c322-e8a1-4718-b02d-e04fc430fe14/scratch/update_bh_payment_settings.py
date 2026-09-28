path = r'C:\xampp\htdocs\Dormigo_Backend\api\boarding_houses.php'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

old_insert = '''        $stmt = $pdo->prepare("INSERT INTO boarding_houses (landlord_id, house_name, description, address, house_rules, status, free_electricity, electricity_rate, free_water, water_rate)
                                VALUES (:landlord_id, :house_name, :description, :address, :house_rules, :status::house_status_enum, :free_electricity, :electricity_rate, :free_water, :water_rate) RETURNING house_id");
        $stmt->execute([
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

new_insert = '''        $stmt = $pdo->prepare("INSERT INTO boarding_houses (landlord_id, house_name, description, address, house_rules, status, free_electricity, electricity_rate, free_water, water_rate, payment_due_day, advance_months, security_deposit_months, utility_deposit, other_fees)
                                VALUES (:landlord_id, :house_name, :description, :address, :house_rules, :status::house_status_enum, :free_electricity, :electricity_rate, :free_water, :water_rate, :payment_due_day, :advance_months, :security_deposit_months, :utility_deposit, :other_fees) RETURNING house_id");
        $stmt->execute([
            'landlord_id' => $data['landlord_id'],
            'house_name' => $data['house_name'],
            'description' => $data['description'] ?? '',
            'address' => $data['address'] ?? '',
            'house_rules' => $data['house_rules'] ?? '',
            'status' => strtoupper($data['status'] ?? 'ACTIVE'),
            'free_electricity' => filter_var($data['free_electricity'] ?? false, FILTER_VALIDATE_BOOLEAN) ? 1 : 0,
            'electricity_rate' => $data['electricity_rate'] ?? '0.00',
            'free_water' => filter_var($data['free_water'] ?? false, FILTER_VALIDATE_BOOLEAN) ? 1 : 0,
            'water_rate' => $data['water_rate'] ?? '0.00',
            'payment_due_day' => (int)($data['payment_due_day'] ?? 1),
            'advance_months' => (int)($data['advance_months'] ?? 1),
            'security_deposit_months' => (int)($data['security_deposit_months'] ?? 1),
            'utility_deposit' => $data['utility_deposit'] ?? '0.00',
            'other_fees' => $data['other_fees'] ?? '0.00'
        ]);'''

old_patch = '''} elseif ($method === 'PATCH') {
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

new_patch = '''} elseif ($method === 'PATCH') {
    $data = json_decode(file_get_contents("php://input"), true);
    if (!isset($data['house_id'])) {
        echo json_encode(["success" => false, "message" => "House ID is required."]);
        exit();
    }
    $stmt = $pdo->prepare("UPDATE boarding_houses SET house_name = :house_name, description = :description, address = :address, house_rules = :house_rules, status = :status::house_status_enum, free_electricity = :free_electricity, electricity_rate = :electricity_rate, free_water = :free_water, water_rate = :water_rate, payment_due_day = :payment_due_day, advance_months = :advance_months, security_deposit_months = :security_deposit_months, utility_deposit = :utility_deposit, other_fees = :other_fees WHERE house_id = :house_id");
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
        'water_rate' => $data['water_rate'] ?? '0.00',
        'payment_due_day' => (int)($data['payment_due_day'] ?? 1),
        'advance_months' => (int)($data['advance_months'] ?? 1),
        'security_deposit_months' => (int)($data['security_deposit_months'] ?? 1),
        'utility_deposit' => $data['utility_deposit'] ?? '0.00',
        'other_fees' => $data['other_fees'] ?? '0.00'
    ]);
    echo json_encode(["success" => true, "message" => "Boarding house updated successfully."]);'''

if old_insert in content:
    content = content.replace(old_insert, new_insert)
if old_patch in content:
    content = content.replace(old_patch, new_patch)

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)

print("Updated boarding_houses.php with payment settings successfully!")
