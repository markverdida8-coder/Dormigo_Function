<?php
require_once 'db.php';

$method = $_SERVER['REQUEST_METHOD'];

if ($method === 'GET') {
    if (isset($_GET['user_id'])) {
        $userId = (int)$_GET['user_id'];
        $stmt = $pdo->prepare("SELECT user_id, full_name, email, phone, user_type, profile_image, created_at FROM users WHERE user_id = :user_id");
        $stmt->execute(['user_id' => $userId]);
        $user = $stmt->fetch();
        if ($user) {
            echo json_encode(["success" => true, "data" => [$user]]);
        } else {
            echo json_encode(["success" => false, "message" => "User not found."]);
        }
    } else {
        $sql = "SELECT user_id, full_name, email, phone, user_type, profile_image, created_at FROM users";
        $params = [];
        if (isset($_GET['user_type'])) {
            $sql .= " WHERE user_type = :user_type::user_type_enum";
            $params['user_type'] = strtoupper(trim($_GET['user_type']));
        }
        $sql .= " ORDER BY user_id DESC";
        $stmt = $pdo->prepare($sql);
        $stmt->execute($params);
        $users = $stmt->fetchAll();
        echo json_encode(["success" => true, "data" => $users]);
    }
} elseif ($method === 'PATCH') {
    $data = json_decode(file_get_contents("php://input"), true);
    if (!isset($data['user_id'])) {
        echo json_encode(["success" => false, "message" => "User ID is required."]);
        exit();
    }
    $userId = (int)$data['user_id'];
    $fields = [];
    $params = ['user_id' => $userId];

    if (isset($data['full_name'])) {
        $fields[] = "full_name = :full_name";
        $params['full_name'] = trim($data['full_name']);
    }
    if (isset($data['email'])) {
        $fields[] = "email = :email";
        $params['email'] = trim($data['email']);
    }
    if (isset($data['phone'])) {
        $fields[] = "phone = :phone";
        $params['phone'] = trim($data['phone']);
    }
    if (isset($data['profile_image'])) {
        $fields[] = "profile_image = :profile_image";
        $params['profile_image'] = trim($data['profile_image']);
    }
    if (isset($data['password']) && !empty(trim($data['password']))) {
        $fields[] = "password = :password";
        $params['password'] = trim($data['password']); // Can be plain or hashed, consistent with login.php
    }

    if (empty($fields)) {
        echo json_encode(["success" => false, "message" => "No fields provided to update."]);
        exit();
    }

    $sql = "UPDATE users SET " . implode(", ", $fields) . " WHERE user_id = :user_id";
    $stmt = $pdo->prepare($sql);
    $stmt->execute($params);

    echo json_encode(["success" => true, "message" => "User profile updated successfully."]);
}
?>
