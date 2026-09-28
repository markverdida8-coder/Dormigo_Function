import urllib.request
import json

base_url = "http://10.149.229.109/Dormigo_Backend/api/"

# 1. Update house 11 cash_enabled to False
payload = {"house_id": 11, "cash_enabled": False}
data = json.dumps(payload).encode('utf-8')
req = urllib.request.Request(base_url + "boarding_houses.php", data=data, headers={'Content-Type': 'application/json'}, method='PATCH')
with urllib.request.urlopen(req) as resp:
    print("PATCH result:", resp.read().decode('utf-8'))

# 2. Query bookings.php?booking_id=14
req2 = urllib.request.Request(base_url + "bookings.php?booking_id=14", method='GET')
with urllib.request.urlopen(req2) as resp:
    res = json.loads(resp.read().decode('utf-8'))
    data = res.get('data', [])[0]
    print("\nbookings.php result for house 11:")
    print("  house_id:", data.get('house_id'))
    print("  cash_enabled:", data.get('cash_enabled'), "type:", type(data.get('cash_enabled')))
    print("  gcash_qr_code:", data.get('gcash_qr_code'))
