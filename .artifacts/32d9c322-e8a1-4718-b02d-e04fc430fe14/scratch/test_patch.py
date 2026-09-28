import urllib.request
import json

url = "http://10.149.229.109/Dormigo_Backend/api/bookings.php"
payload = {
    "booking_id": 14,
    "initial_payment_completed": True,
    "status": "ACTIVE",
    "next_due_date": "2026-10-01"
}
data = json.dumps(payload).encode('utf-8')
try:
    req = urllib.request.Request(url, data=data, headers={'Content-Type': 'application/json'}, method='PATCH')
    with urllib.request.urlopen(req) as resp:
        print(resp.read().decode('utf-8'))
except Exception as e:
    print(e)
