import urllib.request
import json

url = "http://localhost/Dormigo_Backend/api/boarding_houses.php"
try:
    with urllib.request.urlopen(url) as resp:
        data = json.loads(resp.read().decode('utf-8'))
        if data.get('data'):
            print("Columns in first house record:")
            for k in data['data'][0].keys():
                print("  ", k)
except Exception as e:
    print("Error:", e)
