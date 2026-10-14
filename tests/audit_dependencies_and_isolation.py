import os
import subprocess
import json

print("=== PHASE 2: EXACT ENVIRONMENT CONNECTION AUDIT ===")

# Read PHP db configs
def get_php_db_info(path):
    if not os.path.exists(path):
        return {"exists": False, "db": "FILE_NOT_FOUND"}
    with open(path, 'r') as f:
        content = f.read()
    dbname = "UNKNOWN"
    for line in content.splitlines():
        if "dbname" in line:
            dbname = line.strip()
    return {"exists": True, "db": dbname}

android_test_db = get_php_db_info(r"C:\xampp\htdocs\Dormigo_Backend_Test\api\db.php")
web_admin_db = get_php_db_info(r"C:\xampp\htdocs\Dormigo_Function-WEB-ADMIN\php_backend\api\db.php")
android_live_db = get_php_db_info(r"C:\xampp\htdocs\Dormigo_Backend\api\db.php")

# Read Android ApiClient.java
api_client_path = r"C:\Users\markv\AndroidStudioProjects\DormigoVA\app\src\main\java\com\dormigo\ApiClient.java"
android_url = "UNKNOWN"
if os.path.exists(api_client_path):
    with open(api_client_path, 'r') as f:
        for line in f:
            if "BASE_URL" in line and "http" in line:
                android_url = line.strip()

print("Android Test Backend DB Config:", android_test_db)
print("Web Admin Backend DB Config:", web_admin_db)
print("Android Live Backend DB Config:", android_live_db)
print("Android ApiClient BASE_URL:", android_url)

print("\n=== PHASE 1: FULL SYNTHETIC-RECORD DEPENDENCY INVENTORY (USERS 26–39) ===")

php_audit_sql = r"""
@ini_set('display_errors', '0');
error_reporting(0);
require 'C:/xampp/htdocs/Dormigo_Backend_Test/api/db.php';

$ids = "26,27,28,29,30,31,32,33,34,35,36,37,38,39";

$report = [];

// Users
$report['users'] = $pdo->query("SELECT user_id, full_name, email, user_type FROM users WHERE user_id IN ($ids) ORDER BY user_id ASC")->fetchAll(PDO::FETCH_ASSOC);

// Boarding houses owned by synthetic landlords
$report['boarding_houses'] = $pdo->query("SELECT house_id, house_name, landlord_id FROM boarding_houses WHERE landlord_id IN ($ids) ORDER BY house_id ASC")->fetchAll(PDO::FETCH_ASSOC);
$h_ids_arr = array_column($report['boarding_houses'], 'house_id');
$h_ids_str = implode(',', $h_ids_arr);

// Rooms
if (!empty($h_ids_str)) {
    $report['rooms'] = $pdo->query("SELECT room_id, house_id, room_number FROM rooms WHERE house_id IN ($h_ids_str) ORDER BY room_id ASC")->fetchAll(PDO::FETCH_ASSOC);
} else {
    $report['rooms'] = [];
}

// Bookings
$report['bookings'] = $pdo->query("SELECT booking_id, user_id, room_id, status FROM bookings WHERE user_id IN ($ids) ORDER BY booking_id ASC")->fetchAll(PDO::FETCH_ASSOC);
$b_ids_arr = array_column($report['bookings'], 'booking_id');
$b_ids_str = implode(',', $b_ids_arr);

// Pricing Snapshots
if (!empty($b_ids_str)) {
    $report['booking_pricing_snapshots'] = $pdo->query("SELECT booking_id, review_status, snapshot_version FROM booking_pricing_snapshots WHERE booking_id IN ($b_ids_str) ORDER BY booking_id ASC")->fetchAll(PDO::FETCH_ASSOC);
    $report['payments'] = $pdo->query("SELECT payment_id, booking_id, payment_period, payment_type, status FROM payments WHERE booking_id IN ($b_ids_str) ORDER BY payment_id ASC")->fetchAll(PDO::FETCH_ASSOC);
    $report['maintenance_requests'] = $pdo->query("SELECT request_id, booking_id, user_id, status FROM maintenance_requests WHERE booking_id IN ($b_ids_str) OR user_id IN ($ids) ORDER BY request_id ASC")->fetchAll(PDO::FETCH_ASSOC);
} else {
    $report['booking_pricing_snapshots'] = [];
    $report['payments'] = [];
    $report['maintenance_requests'] = [];
}

// Reviews & Reports
if (!empty($h_ids_str)) {
    $report['reviews'] = $pdo->query("SELECT review_id, house_id, user_id, rating FROM reviews WHERE house_id IN ($h_ids_str) OR user_id IN ($ids) ORDER BY review_id ASC")->fetchAll(PDO::FETCH_ASSOC);
    $rev_ids_arr = array_column($report['reviews'], 'review_id');
    $rev_ids_str = implode(',', $rev_ids_arr);
    if (!empty($rev_ids_str)) {
        $report['review_reports'] = $pdo->query("SELECT report_id, review_id, reporter_landlord_id, reporting_reason FROM review_reports WHERE review_id IN ($rev_ids_str) ORDER BY report_id ASC")->fetchAll(PDO::FETCH_ASSOC);
    } else {
        $report['review_reports'] = [];
    }
} else {
    $report['reviews'] = [];
    $report['review_reports'] = [];
}

// Verifications
$report['student_verifications'] = $pdo->query("SELECT verification_id, user_id, verification_status FROM student_verifications WHERE user_id IN ($ids) ORDER BY verification_id ASC")->fetchAll(PDO::FETCH_ASSOC);
$report['landlord_verifications'] = $pdo->query("SELECT verification_id, landlord_id, verification_status FROM landlord_verifications WHERE landlord_id IN ($ids) ORDER BY verification_id ASC")->fetchAll(PDO::FETCH_ASSOC);

// Messages & Notifications
$report['messages'] = $pdo->query("SELECT message_id, sender_id, receiver_id, house_id FROM messages WHERE sender_id IN ($ids) OR receiver_id IN ($ids) ORDER BY message_id ASC")->fetchAll(PDO::FETCH_ASSOC);
$report['notifications'] = $pdo->query("SELECT notification_id, user_id, type FROM notifications WHERE user_id IN ($ids) ORDER BY notification_id ASC")->fetchAll(PDO::FETCH_ASSOC);
$report['auth_tokens'] = $pdo->query("SELECT user_id, COUNT(*) AS token_count FROM auth_tokens WHERE user_id IN ($ids) GROUP BY user_id ORDER BY user_id ASC")->fetchAll(PDO::FETCH_ASSOC);

echo json_encode($report, JSON_PRETTY_PRINT);
"""

proc = subprocess.Popen(['php', '-r', php_audit_sql], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
out, err = proc.communicate()
print(out.decode('utf-8'))
if err:
    print("ERR:", err.decode('utf-8'))
