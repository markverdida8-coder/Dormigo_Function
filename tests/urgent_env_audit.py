import os
import re
import json

paths = {
    "test_backend_db": r"C:\xampp\htdocs\Dormigo_Backend_Test\api\db.php",
    "live_backend_db": r"C:\xampp\htdocs\Dormigo_Backend\api\db.php",
    "web_admin_db": r"C:\xampp\htdocs\Dormigo_Function-WEB-ADMIN\php_backend\api\db.php",
    "mail_config_web": r"C:\xampp\htdocs\Dormigo_Function-WEB-ADMIN\php_backend\api\mail_config.php",
    "mail_config_web_local": r"C:\xampp\htdocs\Dormigo_Function-WEB-ADMIN\php_backend\api\mail_config.local.php",
    "android_api_client": r"C:\Users\markv\AndroidStudioProjects\DormigoVA\app\src\main\java\com\dormigo\ApiClient.java"
}

report = {}

for key, path in paths.items():
    if not os.path.exists(path):
        report[key] = "FILE_NOT_FOUND"
        continue

    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()

    if "db.php" in path:
        match = re.search(r'\$dbname\s*=\s*["\']([^"\']+)["\']', content)
        report[key] = match.group(1) if match else "UNKNOWN"
    elif "mail_config" in path:
        match = re.search(r'[\'"]smtp_enabled[\'"]\s*=>\s*(true|false|1|0)', content, re.IGNORECASE)
        report[key] = "SMTP_ENABLED=" + (match.group(1) if match else "UNKNOWN")
    elif "ApiClient.java" in path:
        match = re.search(r'private static final String BASE_URL\s*=\s*["\']([^"\']+)["\']', content)
        report[key] = match.group(1) if match else "UNKNOWN"

print("=== PRIORITY 1: ENVIRONMENT CONNECTION AUDIT ===")
print(json.dumps(report, indent=2))
