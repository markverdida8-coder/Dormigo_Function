import urllib.request
import json

url = "http://localhost/Dormigo_Backend/api/boarding_houses.php"
try:
    with urllib.request.urlopen(url) as resp:
        data = json.loads(resp.read().decode('utf-8'))
        for h in data.get('data', []):
            print("House ID:", h.get('house_id'), "Name:", h.get('house_name'))
            print("  free_electricity:", repr(h.get('free_electricity')), "electricity_rate:", repr(h.get('electricity_rate')))
            print("  free_water:", repr(h.get('free_water')), "water_rate:", repr(h.get('water_rate')))
except Exception as e:
    print("Error:", e)
