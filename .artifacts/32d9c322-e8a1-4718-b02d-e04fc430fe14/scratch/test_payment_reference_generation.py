import urllib.request
import json

base_url = "http://10.149.229.109/Dormigo_Backend/api/payments.php"

def create_payment(method, amount):
    payload = {
        "booking_id": 14,
        "payment_period": 1,
        "due_date": "2026-10-01",
        "amount": amount,
        "payment_method": method,
        "status": "PENDING",
        "payment_description": "Test Rent"
    }
    data = json.dumps(payload).encode('utf-8')
    req = urllib.request.Request(base_url, data=data, headers={'Content-Type': 'application/json'}, method='POST')
    with urllib.request.urlopen(req) as resp:
        return json.loads(resp.read().decode('utf-8'))

print("=== Testing Cash Payment Reference ===")
res_cash = create_payment("CASH", 5000.00)
print("Cash Response:", res_cash)

print("\n=== Testing GCash Payment Reference ===")
res_gcash = create_payment("GCASH", 5000.00)
print("GCash Response:", res_gcash)
