import urllib.request
import json

url = "http://localhost/Dormigo_Backend/api/reviews.php"
try:
    with urllib.request.urlopen(url) as resp:
        data = json.loads(resp.read().decode('utf-8'))
        print("Reviews API data sample:", data.get('data', [])[:1])
except Exception as e:
    print("Error:", e)
