import urllib.request
import json

url = "http://10.149.229.109/Dormigo_Backend/api/bookings.php?booking_id=14"
try:
    with urllib.request.urlopen(url) as resp:
        data = json.loads(resp.read().decode('utf-8'))
        print(json.dumps(data, indent=2))
except Exception as e:
    print(e)
