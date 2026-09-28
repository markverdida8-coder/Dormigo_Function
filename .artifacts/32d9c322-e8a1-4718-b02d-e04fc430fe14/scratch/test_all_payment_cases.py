import urllib.request
import json

base_url = "http://10.149.229.109/Dormigo_Backend/api/boarding_houses.php"

def update_house(house_id, cash_enabled, gcash_qr):
    payload = {"house_id": house_id, "cash_enabled": cash_enabled, "gcash_qr_code": gcash_qr}
    data = json.dumps(payload).encode('utf-8')
    req = urllib.request.Request(base_url, data=data, headers={'Content-Type': 'application/json'}, method='PATCH')
    with urllib.request.urlopen(req) as resp:
        return json.loads(resp.read().decode('utf-8'))

def get_house(house_id):
    req = urllib.request.Request(base_url, method='GET')
    with urllib.request.urlopen(req) as resp:
        res = json.loads(resp.read().decode('utf-8'))
        for h in res.get('data', []):
            if h.get('house_id') == house_id:
                return h
    return None

print("=== TEST CASE 1: Cash = True, GCash = Yes ===")
update_house(11, True, "uploads/gcash_qr/qr_test.jpg")
h = get_house(11)
print(f"Result: cash_enabled={h.get('cash_enabled')}, gcash_qr_code={h.get('gcash_qr_code')}")

print("\n=== TEST CASE 2: Cash = False, GCash = Yes ===")
update_house(11, False, "uploads/gcash_qr/qr_test.jpg")
h = get_house(11)
print(f"Result: cash_enabled={h.get('cash_enabled')}, gcash_qr_code={h.get('gcash_qr_code')}")

print("\n=== TEST CASE 3: Cash = True, GCash = None ===")
update_house(11, True, None)
h = get_house(11)
print(f"Result: cash_enabled={h.get('cash_enabled')}, gcash_qr_code={h.get('gcash_qr_code')}")

print("\n=== TEST CASE 4: Cash = False, GCash = None ===")
update_house(11, False, None)
h = get_house(11)
print(f"Result: cash_enabled={h.get('cash_enabled')}, gcash_qr_code={h.get('gcash_qr_code')}")

# Restore valid state for house 11
update_house(11, True, "uploads/gcash_qr/qr_house_11_test.jpg")
print("\nRestored House 11 state.")
