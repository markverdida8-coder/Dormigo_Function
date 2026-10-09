<?php
/**
 * DORMIGO - Admin Session Authentication API
 * Provides server-side session management for the admin portal.
 * This ensures admin status is verified on the server, not just client-side.
 */

session_start();
require_once 'db.php';

$method = $_SERVER['REQUEST_METHOD'];

// ── POST: Admin Login ──────────────────────────────────────────────────────────
if ($method === 'POST') {
    $data = json_decode(file_get_contents("php://input"), true);

    if (!isset($data['email']) || !isset($data['password'])) {
        http_response_code(400);
        echo json_encode(["success" => false, "message" => "Email and password are required."]);
        exit();
    }

    $email    = trim($data['email']);
    $password = trim($data['password']);

    try {
        $stmt = $pdo->prepare(
            "SELECT user_id, full_name, email, password, phone, user_type, profile_image
             FROM users
             WHERE LOWER(email) = LOWER(:email)"
        );
        $stmt->execute(['email' => $email]);
        $user = $stmt->fetch();

        if (!$user) {
            http_response_code(401);
            echo json_encode(["success" => false, "message" => "Invalid email or password."]);
            exit();
        }

        // Only ADMIN users may log in through this endpoint
        if (strtoupper($user['user_type']) !== 'ADMIN') {
            http_response_code(403);
            echo json_encode(["success" => false, "message" => "Access denied. Administrator credentials required."]);
            exit();
        }

        // Verify password (supports plain text legacy passwords and bcrypt hashes)
        $passwordValid = ($password === $user['password']) || password_verify($password, $user['password']);

        if (!$passwordValid) {
            http_response_code(401);
            echo json_encode(["success" => false, "message" => "Invalid email or password."]);
            exit();
        }

        // Create server-side session
        session_regenerate_id(true);
        $_SESSION['admin_user_id']   = $user['user_id'];
        $_SESSION['admin_user_type'] = strtoupper($user['user_type']);
        $_SESSION['admin_email']     = $user['email'];
        $_SESSION['admin_name']      = $user['full_name'];
        $_SESSION['admin_logged_in'] = true;
        $_SESSION['admin_login_time'] = time();

        unset($user['password']);
        echo json_encode([
            "success" => true,
            "message" => "Login successful.",
            "user"    => $user
        ]);

    } catch (PDOException $e) {
        http_response_code(500);
        echo json_encode(["success" => false, "message" => "Database error: " . $e->getMessage()]);
    }

// ── GET: Check Session Status ─────────────────────────────────────────────────
} elseif ($method === 'GET') {
    $action = $_GET['action'] ?? 'check';

    if ($action === 'check') {
        if (isset($_SESSION['admin_logged_in']) && $_SESSION['admin_logged_in'] === true
            && strtoupper($_SESSION['admin_user_type'] ?? '') === 'ADMIN') {

            // Re-fetch fresh user data
            try {
                $stmt = $pdo->prepare(
                    "SELECT user_id, full_name, email, phone, user_type, profile_image, created_at
                     FROM users WHERE user_id = :id"
                );
                $stmt->execute(['id' => $_SESSION['admin_user_id']]);
                $user = $stmt->fetch();

                if ($user && strtoupper($user['user_type']) === 'ADMIN') {
                    echo json_encode(["success" => true, "authenticated" => true, "user" => $user]);
                } else {
                    // Revoke invalid session
                    session_destroy();
                    http_response_code(401);
                    echo json_encode(["success" => false, "authenticated" => false, "message" => "Session invalid."]);
                }
            } catch (PDOException $e) {
                http_response_code(500);
                echo json_encode(["success" => false, "message" => "Database error."]);
            }
        } else {
            http_response_code(401);
            echo json_encode(["success" => false, "authenticated" => false, "message" => "Not authenticated."]);
        }

    } elseif ($action === 'logout') {
        session_destroy();
        echo json_encode(["success" => true, "message" => "Logged out successfully."]);
    }

// ── PATCH: Update Admin Profile ───────────────────────────────────────────────
} elseif ($method === 'PATCH') {
    // Must be authenticated admin
    if (!isset($_SESSION['admin_logged_in']) || $_SESSION['admin_logged_in'] !== true
        || strtoupper($_SESSION['admin_user_type'] ?? '') !== 'ADMIN') {
        http_response_code(401);
        echo json_encode(["success" => false, "message" => "Unauthorized."]);
        exit();
    }

    $data   = json_decode(file_get_contents("php://input"), true);
    $userId = (int)$_SESSION['admin_user_id'];

    $fields = [];
    $params = ['user_id' => $userId];

    if (!empty($data['full_name'])) {
        $fields[] = "full_name = :full_name";
        $params['full_name'] = trim($data['full_name']);
    }
    if (!empty($data['phone'])) {
        $fields[] = "phone = :phone";
        $params['phone'] = trim($data['phone']);
    }
    if (!empty($data['profile_image'])) {
        $fields[] = "profile_image = :profile_image";
        $params['profile_image'] = trim($data['profile_image']);
    }
    if (!empty($data['new_password'])) {
        $fields[] = "password = :password";
        $params['password'] = trim($data['new_password']);
    }

    if (empty($fields)) {
        echo json_encode(["success" => false, "message" => "No fields to update."]);
        exit();
    }

    try {
        $sql  = "UPDATE users SET " . implode(", ", $fields) . " WHERE user_id = :user_id";
        $stmt = $pdo->prepare($sql);
        $stmt->execute($params);

        if (!empty($data['full_name'])) {
            $_SESSION['admin_name'] = trim($data['full_name']);
        }

        echo json_encode(["success" => true, "message" => "Profile updated successfully."]);
    } catch (PDOException $e) {
        http_response_code(500);
        echo json_encode(["success" => false, "message" => "Database error: " . $e->getMessage()]);
    }

} else {
    http_response_code(405);
    echo json_encode(["success" => false, "message" => "Method not allowed."]);
}
?>
