import urllib.request
import json

url = "http://localhost/Dormigo_Backend/api/boarding_houses.php"
try:
    with urllib.request.urlopen(url) as resp:
        data = json.loads(resp.read().decode('utf-8'))
        for h in data.get('data', []):
            print(h.get('house_name'), h.get('photo_paths'))
except Exception as e:
    print(e)
