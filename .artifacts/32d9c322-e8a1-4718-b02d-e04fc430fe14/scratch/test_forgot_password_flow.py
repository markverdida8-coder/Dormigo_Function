import urllib.request
import json
import psycopg2

base_url = "http://10.149.229.109/Dormigo_Backend/api/"

def post_json(endpoint, payload):
    data = json.dumps(payload).encode('utf-8')
    req = urllib.request.Request(base_url + endpoint, data=data, headers={'Content-Type': 'application/json'}, method='POST')
    with urllib.request.urlopen(req) as resp:
        return json.loads(resp.read().decode('utf-8'))

print("=== 1. Request Forgot Password ===")
res1 = post_json("forgot_password.php", {"email": "mark@gmail.com"})
print("Response:", res1)

# Fetch OTP from PostgreSQL database
conn = psycopg2.connect(dbname='dormigo', user='postgres', password='root', host='localhost', port='5432')
cur = conn.cursor()
cur.execute("SELECT otp_code FROM password_reset_tokens WHERE email = 'mark@gmail.com' AND used = false ORDER BY id DESC LIMIT 1;")
row = cur.fetchone()
otp_code = row[0] if row else None
print("Fetched OTP from DB:", otp_code)

print("\n=== 2. Verify OTP (Invalid) ===")
try:
    res2 = post_json("verify_otp.php", {"email": "mark@gmail.com", "otp_code": "000000"})
    print("Response:", res2)
except Exception as e:
    print("Expected fail error:", e)

print("\n=== 3. Verify OTP (Valid) ===")
res3 = post_json("verify_otp.php", {"email": "mark@gmail.com", "otp_code": otp_code})
print("Response:", res3)

print("\n=== 4. Reset Password ===")
res4 = post_json("reset_password.php", {"email": "mark@gmail.com", "otp_code": otp_code, "new_password": "NewPassword123!"})
print("Response:", res4)

print("\n=== 5. Login with New Password ===")
res5 = post_json("login.php", {"email": "mark@gmail.com", "password": "NewPassword123!"})
print("Response:", res5)

cur.close()
conn.close()
