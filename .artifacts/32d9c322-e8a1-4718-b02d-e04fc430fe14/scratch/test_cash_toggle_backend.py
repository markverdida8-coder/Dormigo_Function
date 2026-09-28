import urllib.request
import json

base_url = "http://10.149.229.109/Dormigo_Backend/api/boarding_houses.php"

def patch_cash(house_id, cash_enabled):
    payload = {"house_id": house_id, "cash_enabled": cash_enabled}
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

print("--- STEP 1: Setting cash_enabled = False ---")
res1 = patch_cash(11, False)
print("PATCH result:", res1)

h1 = get_house(11)
print("House 11 after setting False: cash_enabled =", h1.get('cash_enabled'))

print("\n--- STEP 2: Setting cash_enabled = True ---")
res2 = patch_cash(11, True)
print("PATCH result:", res2)

h2 = get_house(11)
print("House 11 after setting True: cash_enabled =", h2.get('cash_enabled'))
