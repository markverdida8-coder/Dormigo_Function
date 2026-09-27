import urllib.request
import json

url = "http://localhost/Dormigo_Backend/api/boarding_houses.php"
try:
    with urllib.request.urlopen(url) as resp:
        data = json.loads(resp.read().decode('utf-8'))
        for h in data.get('data', []):
            print("House:", h.get('house_name'), "ID:", h.get('house_id'), "Photos:", h.get('photo_paths'))
except Exception as e:
    print(e)
