import urllib.request
import json

url = "http://10.149.229.109/Dormigo_Backend/api/boarding_houses.php"
with urllib.request.urlopen(url) as resp:
    res = json.loads(resp.read().decode('utf-8'))
    for h in res.get('data', []):
        print(h.get('house_id'), h.get('house_name'), h.get('landlord_id'))
