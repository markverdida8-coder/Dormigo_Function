path = r'C:\xampp\htdocs\Dormigo_Backend\api\boarding_houses.php'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

# 1. In POST method, add the action check right at start of POST
old_post_start = "if (isset($_POST['payload'])) {"
new_post_start = """if (isset($_POST['action']) && $_POST['action'] === 'upload_gcash_qr') {
        $houseId = (int)($_POST['house_id'] ?? 0);
        if ($houseId <= 0 || !isset($_FILES['gcash_qr'])) {
            echo json_encode(["success" => false, "message" => "House ID and QR file required."]);
            exit();
        }
        $uploadDir = '../uploads/gcash_qr/';
        if (!file_exists($uploadDir)) {
            mkdir($uploadDir, 0777, true);
        }
        $tmpName = $_FILES['gcash_qr']['tmp_name'];
        $fileName = 'qr_house_' . $houseId . '_' . time() . '.jpg';
        $targetFilePath = $uploadDir . $fileName;

        if (move_uploaded_file($tmpName, $targetFilePath)) {
            $webPath = 'uploads/gcash_qr/' . $fileName;
            $stmt = $pdo->prepare("UPDATE boarding_houses SET gcash_qr_code = :path, gcash_updated_at = CURRENT_TIMESTAMP WHERE house_id = :house_id");
            $stmt->execute(['path' => $webPath, 'house_id' => $houseId]);

            echo json_encode([
                "success" => true,
                "message" => "GCash QR code uploaded successfully.",
                "gcash_qr_code" => $webPath
            ]);
        } else {
            echo json_encode(["success" => false, "message" => "Failed to save QR code image."]);
        }
        exit();
    }

    if (isset($_POST['payload'])) {"""

content = content.replace(old_post_start, new_post_start)

# 2. Flexible PATCH method
old_patch = """} elseif ($method === 'PATCH') {
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
    echo json_encode(["success" => true, "message" => "Boarding house updated successfully."]);"""

new_patch = """} elseif ($method === 'PATCH') {
    $data = json_decode(file_get_contents("php://input"), true);
    if (!isset($data['house_id'])) {
        echo json_encode(["success" => false, "message" => "House ID is required."]);
        exit();
    }
    $fields = [];
    $params = ['house_id' => (int)$data['house_id']];

    if (isset($data['house_name'])) {
        $fields[] = "house_name = :house_name";
        $params['house_name'] = $data['house_name'];
    }
    if (isset($data['description'])) {
        $fields[] = "description = :description";
        $params['description'] = $data['description'];
    }
    if (isset($data['address'])) {
        $fields[] = "address = :address";
        $params['address'] = $data['address'];
    }
    if (isset($data['house_rules'])) {
        $fields[] = "house_rules = :house_rules";
        $params['house_rules'] = $data['house_rules'];
    }
    if (isset($data['status'])) {
        $fields[] = "status = :status::house_status_enum";
        $params['status'] = strtoupper($data['status']);
    }
    if (isset($data['free_electricity'])) {
        $fields[] = "free_electricity = :free_electricity";
        $params['free_electricity'] = filter_var($data['free_electricity'], FILTER_VALIDATE_BOOLEAN) ? 1 : 0;
    }
    if (isset($data['electricity_rate'])) {
        $fields[] = "electricity_rate = :electricity_rate";
        $params['electricity_rate'] = $data['electricity_rate'];
    }
    if (isset($data['free_water'])) {
        $fields[] = "free_water = :free_water";
        $params['free_water'] = filter_var($data['free_water'], FILTER_VALIDATE_BOOLEAN) ? 1 : 0;
    }
    if (isset($data['water_rate'])) {
        $fields[] = "water_rate = :water_rate";
        $params['water_rate'] = $data['water_rate'];
    }
    if (isset($data['cash_enabled'])) {
        $fields[] = "cash_enabled = :cash_enabled";
        $params['cash_enabled'] = filter_var($data['cash_enabled'], FILTER_VALIDATE_BOOLEAN) ? 1 : 0;
    }
    if (array_key_exists('gcash_qr_code', $data)) {
        $fields[] = "gcash_qr_code = :gcash_qr_code";
        $params['gcash_qr_code'] = $data['gcash_qr_code'];
        $fields[] = "gcash_updated_at = CURRENT_TIMESTAMP";
    }

    if (!empty($fields)) {
        $sql = "UPDATE boarding_houses SET " . implode(", ", $fields) . " WHERE house_id = :house_id";
        $stmt = $pdo->prepare($sql);
        $stmt->execute($params);
    }
    echo json_encode(["success" => true, "message" => "Payment settings updated successfully."]);"""

content = content.replace(old_patch, new_patch)

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)

print("Updated boarding_houses.php successfully.")
