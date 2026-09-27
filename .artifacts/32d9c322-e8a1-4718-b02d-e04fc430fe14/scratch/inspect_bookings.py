import urllib.request
import json

url = "http://localhost/Dormigo_Backend/api/bookings.php"
try:
    with urllib.request.urlopen(url) as resp:
        data = json.loads(resp.read().decode('utf-8'))
        print("Bookings API data:", data.get('data', []))
except Exception as e:
    print("Error:", e)
