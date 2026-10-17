<?php
require_once 'db.php';
require_once 'auth_helper.php';

function userRespond(int $statusCode, array $payload): void {
    http_response_code($statusCode);
    echo json_encode($payload);
    exit();
}

function userPasswordMatches(string $submittedPassword, string $storedPassword): bool {
    return hash_equals($storedPassword, $submittedPassword)
        || password_verify($submittedPassword, $storedPassword);
}

$method = $_SERVER['REQUEST_METHOD'];

if ($method === 'GET') {
    if (isset($_GET['user_id'])) {
        $userId = (int)$_GET['user_id'];
        $stmt = $pdo->prepare("SELECT user_id, full_name, email, phone, user_type, profile_image FROM public.users WHERE user_id = :user_id");
        $stmt->execute(['user_id' => $userId]);
        $user = $stmt->fetch();
        if ($user) {
            echo json_encode(["success" => true, "data" => [$user]]);
        } else {
            echo json_encode(["success" => false, "message" => "User not found."]);
        }
    } else {
        $sql = "SELECT user_id, full_name, email, phone, user_type, profile_image FROM public.users WHERE 1=1";
        $params = [];

        if (isset($_GET['search']) && !empty(trim($_GET['search']))) {
            $sql .= " AND (LOWER(full_name) LIKE LOWER(:search) OR LOWER(email) LIKE LOWER(:search))";
            $params['search'] = '%' . trim($_GET['search']) . '%';
        }

        if (isset($_GET['user_type']) && !empty(trim($_GET['user_type']))) {
            $sql .= " AND user_type = :user_type::user_type_enum";
            $params['user_type'] = strtoupper(trim($_GET['user_type']));
        }

        if (isset($_GET['exclude_user_id'])) {
            $sql .= " AND user_id != :exclude_user_id";
            $params['exclude_user_id'] = (int)$_GET['exclude_user_id'];
        }

        $sql .= " ORDER BY full_name ASC";

        $stmt = $pdo->prepare($sql);
        $stmt->execute($params);
        $users = $stmt->fetchAll();
        echo json_encode(["success" => true, "data" => $users]);
    }
} elseif ($method === 'PATCH') {
    $data = json_decode(file_get_contents("php://input"), true);
    if (!isset($data['user_id'])) {
        userRespond(400, ["success" => false, "message" => "User ID is required."]);
    }
    $userId = (int)$data['user_id'];
    $authUser = authenticateUser($pdo);
    if ((int)$authUser['user_id'] !== $userId) {
        userRespond(403, ["success" => false, "message" => "Forbidden: You can only update your own profile."]);
    }
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

    if (empty($fields)) {
        userRespond(400, ["success" => false, "message" => "No fields provided to update."]);
    }

    $sql = "UPDATE users SET " . implode(", ", $fields) . " WHERE user_id = :user_id";
    $stmt = $pdo->prepare($sql);
    $stmt->execute($params);

    echo json_encode(["success" => true, "message" => "User profile updated successfully."]);
} elseif ($method === 'DELETE') {
    $data = json_decode(file_get_contents("php://input"), true);
    $currentPassword = $data['current_password'] ?? ($data['password'] ?? null);
    if (!is_string($currentPassword) || $currentPassword === '') {
        userRespond(400, ["success" => false, "message" => "Your current password is required to deactivate the account."]);
    }

    $authUser = authenticateUser($pdo);
    $userId = (int)$authUser['user_id'];
    $userType = strtoupper((string)$authUser['user_type']);
    if (!in_array($userType, ['STUDENT', 'LANDLORD'], true)) {
        userRespond(403, ["success" => false, "message" => "Administrator accounts cannot be deactivated through this endpoint."]);
    }

    try {
        $pdo->beginTransaction();

        $accountStmt = $pdo->prepare(
            "SELECT password, deactivated_at
             FROM users
             WHERE user_id = :user_id
             FOR UPDATE"
        );
        $accountStmt->execute(['user_id' => $userId]);
        $account = $accountStmt->fetch(PDO::FETCH_ASSOC);
        if (!$account || $account['deactivated_at'] !== null) {
            $pdo->rollBack();
            userRespond(401, ["success" => false, "message" => "This account is no longer active."]);
        }
        if (!userPasswordMatches($currentPassword, (string)$account['password'])) {
            $pdo->rollBack();
            userRespond(401, ["success" => false, "message" => "Current password is incorrect."]);
        }

        $deactivate = $pdo->prepare(
            "UPDATE users
             SET deactivated_at = CURRENT_TIMESTAMP
             WHERE user_id = :user_id AND deactivated_at IS NULL
             RETURNING user_id"
        );
        $deactivate->execute(['user_id' => $userId]);
        if ($deactivate->fetchColumn() === false) {
            $pdo->rollBack();
            userRespond(409, ["success" => false, "message" => "Account deactivation could not be completed."]);
        }

        // Preserve audit records while making existing credentials unusable.
        $pdo->prepare(
            "UPDATE auth_tokens
             SET expires_at = CURRENT_TIMESTAMP
             WHERE user_id = :user_id AND expires_at > CURRENT_TIMESTAMP"
        )->execute(['user_id' => $userId]);
        $pdo->prepare(
            "UPDATE password_reset_tokens
             SET used = TRUE
             WHERE user_id = :user_id AND used IS DISTINCT FROM TRUE"
        )->execute(['user_id' => $userId]);

        $pdo->commit();
        echo json_encode(["success" => true, "message" => "Account deactivated successfully. Historical records were preserved."]);
    } catch (Throwable $e) {
        if ($pdo->inTransaction()) {
            $pdo->rollBack();
        }
        error_log('Account deactivation failed: ' . get_class($e));
        http_response_code(500);
        echo json_encode(["success" => false, "message" => "Unable to deactivate the account. Please try again later."]);
    }
}
?>
