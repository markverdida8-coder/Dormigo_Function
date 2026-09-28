import urllib.request
import json

base_url = "http://10.149.229.109/Dormigo_Backend/api/boarding_houses.php"

# Test 1: Send cash_enabled as boolean false
payload = {"house_id": 11, "cash_enabled": False}
data = json.dumps(payload).encode('utf-8')
req = urllib.request.Request(base_url, data=data, headers={'Content-Type': 'application/json'}, method='PATCH')
try:
    with urllib.request.urlopen(req) as resp:
        print("PATCH result:", resp.read().decode('utf-8'))
except Exception as e:
    print("PATCH error:", e)
