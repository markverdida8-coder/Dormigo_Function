import urllib.request
import json

url = "http://10.149.229.109/Dormigo_Backend/api/boarding_houses.php"
try:
    with urllib.request.urlopen(url) as resp:
        data = json.loads(resp.read().decode('utf-8'))
        for h in data.get('data', []):
            print("House:", h.get('house_name'))
            print("  free_electricity:", h.get('free_electricity'))
            print("  electricity_rate:", h.get('electricity_rate'))
            print("  free_water:", h.get('free_water'))
            print("  water_rate:", h.get('water_rate'))
except Exception as e:
    print("Error:", e)
