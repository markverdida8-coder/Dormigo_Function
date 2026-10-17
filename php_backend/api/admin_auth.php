<?php
/**
 * DORMIGO - Admin Session & Two-Factor Authentication API
 * Provides server-side session management with Email OTP Two-Factor Authentication (2FA)
 * and secure authorization confirmation for password changes.
 */

// Set secure session cookie parameters
if (session_status() === PHP_SESSION_NONE) {
    session_set_cookie_params([
        'lifetime' => 0,
        'path'     => '/',
        'secure'   => isset($_SERVER['HTTPS']) && $_SERVER['HTTPS'] === 'on',
        'httponly' => true,
        'samesite' => 'Lax'
    ]);
    session_start();
}

require_once __DIR__ . '/db.php';
require_once __DIR__ . '/mail_helper.php';

$method = $_SERVER['REQUEST_METHOD'];

// Helper to mask email for security display (e.g. ad***@dormigo.com)
function maskEmail($email) {
    $parts = explode("@", $email);
    if (count($parts) !== 2) return $email;
    $name = $parts[0];
    $domain = $parts[1];
    $maskedName = strlen($name) <= 2 ? $name . '***' : substr($name, 0, 2) . '***' . substr($name, -1);
    return $maskedName . '@' . $domain;
}

// ── Inactivity Auto-Logout (30 Minutes) ─────────────────────────────────────────
if (isset($_SESSION['admin_logged_in']) && $_SESSION['admin_logged_in'] === true) {
    if (isset($_SESSION['last_activity']) && (time() - $_SESSION['last_activity'] > 1800)) {
        session_unset();
        session_destroy();
        if ($method === 'GET' && ($_GET['action'] ?? '') === 'check') {
            http_response_code(401);
            echo json_encode(["success" => false, "authenticated" => false, "message" => "Session expired due to inactivity."]);
            exit();
        }
    }
    $_SESSION['last_activity'] = time();
}

// ── POST ACTIONS: Login, OTP Verification, Resend, Password Confirmation ───────
if ($method === 'POST') {
    $action = $_GET['action'] ?? 'login';
    $data = json_decode(file_get_contents("php://input"), true) ?? [];

    // ── 1. POST ?action=login (Step 1: Check credentials & send 2FA OTP) ────────
    if ($action === 'login') {
        if (empty($data['email']) || empty($data['password'])) {
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

            // Only ADMIN accounts permitted
            if (strtoupper($user['user_type']) !== 'ADMIN') {
                http_response_code(403);
                echo json_encode(["success" => false, "message" => "Access denied. Administrator credentials required."]);
                exit();
            }

            // Verify password (plain text legacy or bcrypt)
            $passwordValid = ($password === $user['password']) || password_verify($password, $user['password']);
            if (!$passwordValid) {
                http_response_code(401);
                echo json_encode(["success" => false, "message" => "Invalid email or password."]);
                exit();
            }

            // Generate 6-digit cryptographically secure OTP
            $otpCode = str_pad(random_int(0, 999999), 6, '0', STR_PAD_LEFT);
            $otpHash = password_hash($otpCode, PASSWORD_BCRYPT);

            // Invalidate any older unused login OTPs for this admin
            $pdo->prepare("UPDATE admin_otps SET is_used = TRUE WHERE user_id = :uid AND purpose = 'LOGIN_2FA' AND is_used = FALSE")
                ->execute(['uid' => $user['user_id']]);

            // Insert new OTP record with 5-minute expiry
            $stmtOtp = $pdo->prepare(
                "INSERT INTO admin_otps (user_id, otp_code_hash, purpose, expires_at, attempts_remaining, is_used)
                 VALUES (:uid, :hash, 'LOGIN_2FA', CURRENT_TIMESTAMP + INTERVAL '5 minutes', 3, FALSE)"
            );
            $stmtOtp->execute([
                'uid'  => $user['user_id'],
                'hash' => $otpHash
            ]);

            // Send OTP via MailHelper
            $mailResult = MailHelper::sendOtp($user['email'], $user['full_name'], $otpCode, 'LOGIN_2FA');
            if (!$mailResult['success']) {
                http_response_code(500);
                echo json_encode([
                    "success" => false,
                    "message" => $mailResult['message'] ?? "Failed to send verification email."
                ]);
                exit();
            }

            // Store temporary pre-auth state in session
            $_SESSION['admin_pre_auth_user_id'] = $user['user_id'];
            $_SESSION['admin_pre_auth_email']   = $user['email'];
            $_SESSION['admin_pre_auth_time']    = time();

            $response = [
                "success"       => true,
                "requires_otp"  => true,
                "message"       => "A 6-digit verification code has been sent to your administrator email.",
                "email_masked"  => maskEmail($user['email'])
            ];

            echo json_encode($response);
            exit();

        } catch (Exception $e) {
            http_response_code(500);
            echo json_encode(["success" => false, "message" => "Server error: " . $e->getMessage()]);
            exit();
        }

    // ── 2. POST ?action=verify_login_otp (Step 2: Validate 2FA OTP) ─────────────
    } elseif ($action === 'verify_login_otp') {
        $otpCode = trim($data['otp_code'] ?? '');
        $userId  = $_SESSION['admin_pre_auth_user_id'] ?? null;

        if (empty($otpCode)) {
            http_response_code(400);
            echo json_encode(["success" => false, "message" => "Please enter the 6-digit verification code."]);
            exit();
        }

        if (!$userId) {
            http_response_code(401);
            echo json_encode(["success" => false, "message" => "Session expired. Please sign in again."]);
            exit();
        }

        try {
            // Find active, unexpired OTP
            $stmt = $pdo->prepare(
                "SELECT otp_id, otp_code_hash, attempts_remaining, expires_at
                 FROM admin_otps
                 WHERE user_id = :uid 
                   AND purpose = 'LOGIN_2FA' 
                   AND is_used = FALSE 
                   AND expires_at > CURRENT_TIMESTAMP
                 ORDER BY created_at DESC 
                 LIMIT 1"
            );
            $stmt->execute(['uid' => $userId]);
            $otpRecord = $stmt->fetch();

            if (!$otpRecord) {
                http_response_code(400);
                echo json_encode(["success" => false, "message" => "Verification code has expired. Please request a new one."]);
                exit();
            }

            // Verify code match
            if (password_verify($otpCode, $otpRecord['otp_code_hash'])) {
                // Success: Mark OTP as used
                $pdo->prepare("UPDATE admin_otps SET is_used = TRUE WHERE otp_id = :id")
                    ->execute(['id' => $otpRecord['otp_id']]);

                // Fetch admin user profile
                $userStmt = $pdo->prepare(
                    "SELECT user_id, full_name, email, phone, user_type, profile_image, created_at
                     FROM users WHERE user_id = :id"
                );
                $userStmt->execute(['id' => $userId]);
                $user = $userStmt->fetch();

                // Upgrade to full session
                session_regenerate_id(true);
                $_SESSION['admin_user_id']    = $user['user_id'];
                $_SESSION['admin_user_type']  = strtoupper($user['user_type']);
                $_SESSION['admin_email']      = $user['email'];
                $_SESSION['admin_name']       = $user['full_name'];
                $_SESSION['admin_logged_in']  = true;
                $_SESSION['last_activity']    = time();

                // Clear temporary pre-auth
                unset($_SESSION['admin_pre_auth_user_id'], $_SESSION['admin_pre_auth_email'], $_SESSION['admin_pre_auth_time']);

                echo json_encode([
                    "success" => true,
                    "message" => "Two-factor authentication verified.",
                    "user"    => $user
                ]);
                exit();

            } else {
                // Decrement attempts
                $remaining = max(0, $otpRecord['attempts_remaining'] - 1);
                if ($remaining === 0) {
                    $pdo->prepare("UPDATE admin_otps SET attempts_remaining = 0, is_used = TRUE WHERE otp_id = :id")
                        ->execute(['id' => $otpRecord['otp_id']]);
                    http_response_code(401);
                    echo json_encode(["success" => false, "message" => "Too many incorrect attempts. Code has been invalidated. Please request a new one."]);
                } else {
                    $pdo->prepare("UPDATE admin_otps SET attempts_remaining = :att WHERE otp_id = :id")
                        ->execute(['att' => $remaining, 'id' => $otpRecord['otp_id']]);
                    http_response_code(401);
                    echo json_encode(["success" => false, "message" => "Invalid verification code. {$remaining} attempt(s) remaining."]);
                }
                exit();
            }

        } catch (Exception $e) {
            http_response_code(500);
            echo json_encode(["success" => false, "message" => "Verification error: " . $e->getMessage()]);
            exit();
        }

    // ── 3. POST ?action=resend_otp (Resend OTP for Login or Password Change) ───
    } elseif ($action === 'resend_otp') {
        $purpose = $data['purpose'] ?? 'LOGIN_2FA';

        if ($purpose === 'LOGIN_2FA') {
            $userId = $_SESSION['admin_pre_auth_user_id'] ?? null;
        } else {
            $userId = $_SESSION['admin_user_id'] ?? null;
        }

        if (!$userId) {
            http_response_code(401);
            echo json_encode(["success" => false, "message" => "No pending verification session found."]);
            exit();
        }

        // Rate limit: Check if OTP was created in last 45 seconds
        $checkStmt = $pdo->prepare(
            "SELECT created_at FROM admin_otps 
             WHERE user_id = :uid AND purpose = :purpose 
             ORDER BY created_at DESC LIMIT 1"
        );
        $checkStmt->execute(['uid' => $userId, 'purpose' => $purpose]);
        $lastOtp = $checkStmt->fetch();

        if ($lastOtp && (time() - strtotime($lastOtp['created_at']) < 45)) {
            $wait = 45 - (time() - strtotime($lastOtp['created_at']));
            http_response_code(429);
            echo json_encode(["success" => false, "message" => "Please wait {$wait} seconds before requesting a new code."]);
            exit();
        }

        // Fetch user info
        $userStmt = $pdo->prepare("SELECT email, full_name FROM users WHERE user_id = :uid");
        $userStmt->execute(['uid' => $userId]);
        $user = $userStmt->fetch();

        if (!$user) {
            http_response_code(404);
            echo json_encode(["success" => false, "message" => "User not found."]);
            exit();
        }

        // Invalidate old OTPs for this purpose
        $pdo->prepare("UPDATE admin_otps SET is_used = TRUE WHERE user_id = :uid AND purpose = :purpose AND is_used = FALSE")
            ->execute(['uid' => $userId, 'purpose' => $purpose]);

        // Generate and insert fresh OTP
        $otpCode = str_pad(random_int(0, 999999), 6, '0', STR_PAD_LEFT);
        $otpHash = password_hash($otpCode, PASSWORD_BCRYPT);

        $pdo->prepare(
            "INSERT INTO admin_otps (user_id, otp_code_hash, purpose, expires_at, attempts_remaining, is_used)
             VALUES (:uid, :hash, :purpose, CURRENT_TIMESTAMP + INTERVAL '5 minutes', 3, FALSE)"
        )->execute([
            'uid'     => $userId,
            'hash'    => $otpHash,
            'purpose' => $purpose
        ]);

        $mailResult = MailHelper::sendOtp($user['email'], $user['full_name'], $otpCode, $purpose);

        $res = [
            "success" => true,
            "message" => "A new verification code has been dispatched to your email."
        ];
        if (!empty($mailResult['debug_code'])) {
            $res['debug_code'] = $mailResult['debug_code'];
        }

        echo json_encode($res);
        exit();

    // ── 4. POST ?action=request_password_otp (Step 1 for changing password) ────
    } elseif ($action === 'request_password_otp') {
        // Must be logged in admin
        if (!isset($_SESSION['admin_logged_in']) || $_SESSION['admin_logged_in'] !== true) {
            http_response_code(401);
            echo json_encode(["success" => false, "message" => "Unauthorized."]);
            exit();
        }

        $currentPassword = trim($data['current_password'] ?? '');
        $newPassword     = trim($data['new_password'] ?? '');
        $userId          = (int)$_SESSION['admin_user_id'];

        if (empty($currentPassword) || empty($newPassword)) {
            http_response_code(400);
            echo json_encode(["success" => false, "message" => "Current password and new password are required."]);
            exit();
        }

        if (strlen($newPassword) < 8) {
            http_response_code(400);
            echo json_encode(["success" => false, "message" => "New password must be at least 8 characters long."]);
            exit();
        }

        try {
            $stmt = $pdo->prepare("SELECT password, email, full_name FROM users WHERE user_id = :id");
            $stmt->execute(['id' => $userId]);
            $admin = $stmt->fetch();

            if (!$admin) {
                http_response_code(404);
                echo json_encode(["success" => false, "message" => "Admin user not found."]);
                exit();
            }

            // Verify CURRENT password
            $currentValid = ($currentPassword === $admin['password']) || password_verify($currentPassword, $admin['password']);
            if (!$currentValid) {
                http_response_code(401);
                echo json_encode(["success" => false, "message" => "Current password does not match our records."]);
                exit();
            }

            // Generate OTP for Password Change
            $otpCode = str_pad(random_int(0, 999999), 6, '0', STR_PAD_LEFT);
            $otpHash = password_hash($otpCode, PASSWORD_BCRYPT);

            // Invalidate older unused password change OTPs
            $pdo->prepare("UPDATE admin_otps SET is_used = TRUE WHERE user_id = :uid AND purpose = 'PASSWORD_CHANGE' AND is_used = FALSE")
                ->execute(['uid' => $userId]);

            // Save new OTP
            $pdo->prepare(
                "INSERT INTO admin_otps (user_id, otp_code_hash, purpose, expires_at, attempts_remaining, is_used)
                 VALUES (:uid, :hash, 'PASSWORD_CHANGE', CURRENT_TIMESTAMP + INTERVAL '5 minutes', 3, FALSE)"
            )->execute([
                'uid'  => $userId,
                'hash' => $otpHash
            ]);

            // Store pending new password hash in session securely
            $_SESSION['pending_new_password_hash'] = password_hash($newPassword, PASSWORD_BCRYPT);

            // Send Email OTP
            $mailResult = MailHelper::sendOtp($admin['email'], $admin['full_name'], $otpCode, 'PASSWORD_CHANGE');

            $res = [
                "success"      => true,
                "requires_otp" => true,
                "message"      => "An authorization code has been sent to {$admin['email']}. Enter it to confirm.",
                "email_masked" => maskEmail($admin['email'])
            ];
            if (!empty($mailResult['debug_code'])) {
                $res['debug_code'] = $mailResult['debug_code'];
            }

            echo json_encode($res);
            exit();

        } catch (Exception $e) {
            http_response_code(500);
            echo json_encode(["success" => false, "message" => "Error processing password change request: " . $e->getMessage()]);
            exit();
        }

    // ── 5. POST ?action=confirm_password_change (Step 2: Confirm OTP) ──────────
    } elseif ($action === 'confirm_password_change') {
        if (!isset($_SESSION['admin_logged_in']) || $_SESSION['admin_logged_in'] !== true) {
            http_response_code(401);
            echo json_encode(["success" => false, "message" => "Unauthorized."]);
            exit();
        }

        $otpCode = trim($data['otp_code'] ?? '');
        $userId  = (int)$_SESSION['admin_user_id'];
        $newHash = $_SESSION['pending_new_password_hash'] ?? null;

        if (empty($otpCode)) {
            http_response_code(400);
            echo json_encode(["success" => false, "message" => "Please provide the authorization code."]);
            exit();
        }

        if (!$newHash) {
            http_response_code(400);
            echo json_encode(["success" => false, "message" => "No pending password change found. Please submit your password again."]);
            exit();
        }

        try {
            $stmt = $pdo->prepare(
                "SELECT otp_id, otp_code_hash, attempts_remaining
                 FROM admin_otps
                 WHERE user_id = :uid 
                   AND purpose = 'PASSWORD_CHANGE' 
                   AND is_used = FALSE 
                   AND expires_at > CURRENT_TIMESTAMP
                 ORDER BY created_at DESC 
                 LIMIT 1"
            );
            $stmt->execute(['uid' => $userId]);
            $otpRecord = $stmt->fetch();

            if (!$otpRecord) {
                http_response_code(400);
                echo json_encode(["success" => false, "message" => "Authorization code has expired. Please try again."]);
                exit();
            }

            if (password_verify($otpCode, $otpRecord['otp_code_hash'])) {
                // Mark OTP as used
                $pdo->prepare("UPDATE admin_otps SET is_used = TRUE WHERE otp_id = :id")
                    ->execute(['id' => $otpRecord['otp_id']]);

                // Update Password in users table
                $updStmt = $pdo->prepare("UPDATE users SET password = :hash WHERE user_id = :id");
                $updStmt->execute(['hash' => $newHash, 'id' => $userId]);

                // Clear session pending hash
                unset($_SESSION['pending_new_password_hash']);

                echo json_encode([
                    "success" => true,
                    "message" => "Administrator password updated successfully and confirmed via email security."
                ]);
                exit();

            } else {
                $remaining = max(0, $otpRecord['attempts_remaining'] - 1);
                if ($remaining === 0) {
                    $pdo->prepare("UPDATE admin_otps SET attempts_remaining = 0, is_used = TRUE WHERE otp_id = :id")
                        ->execute(['id' => $otpRecord['otp_id']]);
                    http_response_code(401);
                    echo json_encode(["success" => false, "message" => "Too many incorrect authorization attempts. Code invalidated."]);
                } else {
                    $pdo->prepare("UPDATE admin_otps SET attempts_remaining = :att WHERE otp_id = :id")
                        ->execute(['att' => $remaining, 'id' => $otpRecord['otp_id']]);
                    http_response_code(401);
                    echo json_encode(["success" => false, "message" => "Invalid authorization code. {$remaining} attempt(s) remaining."]);
                }
                exit();
            }

        } catch (Exception $e) {
            http_response_code(500);
            echo json_encode(["success" => false, "message" => "Database error: " . $e->getMessage()]);
            exit();
        }

    } else {
        http_response_code(400);
        echo json_encode(["success" => false, "message" => "Unknown POST action."]);
        exit();
    }

// ── GET: Check Session Status or Logout ────────────────────────────────────────
} elseif ($method === 'GET') {
    $action = $_GET['action'] ?? 'check';

    if ($action === 'check') {
        if (isset($_SESSION['admin_logged_in']) && $_SESSION['admin_logged_in'] === true
            && strtoupper($_SESSION['admin_user_type'] ?? '') === 'ADMIN') {

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
                    session_unset();
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
        session_unset();
        session_destroy();
        echo json_encode(["success" => true, "message" => "Logged out successfully."]);
    } else {
        http_response_code(400);
        echo json_encode(["success" => false, "message" => "Unknown GET action."]);
    }

// ── PATCH: Update Admin Profile Details (Name, Phone, Image) ───────────────────
} elseif ($method === 'PATCH') {
    if (!isset($_SESSION['admin_logged_in']) || $_SESSION['admin_logged_in'] !== true
        || strtoupper($_SESSION['admin_user_type'] ?? '') !== 'ADMIN') {
        http_response_code(401);
        echo json_encode(["success" => false, "message" => "Unauthorized."]);
        exit();
    }

    $data   = json_decode(file_get_contents("php://input"), true) ?? [];
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

    if (empty($fields)) {
        echo json_encode(["success" => false, "message" => "No profile fields to update."]);
        exit();
    }

    try {
        $sql  = "UPDATE users SET " . implode(", ", $fields) . " WHERE user_id = :user_id";
        $stmt = $pdo->prepare($sql);
        $stmt->execute($params);

        if (!empty($data['full_name'])) {
            $_SESSION['admin_name'] = trim($data['full_name']);
        }

        echo json_encode(["success" => true, "message" => "Admin profile updated successfully."]);
    } catch (PDOException $e) {
        http_response_code(500);
        echo json_encode(["success" => false, "message" => "Database error: " . $e->getMessage()]);
    }

} else {
    http_response_code(405);
    echo json_encode(["success" => false, "message" => "Method not allowed."]);
}
