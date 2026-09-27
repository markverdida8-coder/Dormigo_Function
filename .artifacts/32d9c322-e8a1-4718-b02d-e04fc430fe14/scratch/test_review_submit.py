import urllib.request
import json

url = "http://localhost/Dormigo_Backend/api/reviews.php"
payload = {
    "user_id": 2,
    "house_id": 11,
    "rating": 5,
    "comment": "Clean room. Very friendly landlord. Near the university."
}
data = json.dumps(payload).encode('utf-8')
try:
    req = urllib.request.Request(url, data=data, headers={'Content-Type': 'application/json'}, method='POST')
    with urllib.request.urlopen(req) as resp:
        print(resp.read().decode('utf-8'))
except Exception as e:
    print(e)
