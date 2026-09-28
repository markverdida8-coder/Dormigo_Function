import urllib.request
import json

base_url = "http://10.149.229.109/Dormigo_Backend/api/messages.php"

payload = {
    "sender_id": 8,
    "receiver_id": 9,
    "house_id": None,
    "message_text": "Hello with null house_id!"
}
data = json.dumps(payload).encode('utf-8')
req = urllib.request.Request(base_url, data=data, headers={'Content-Type': 'application/json'}, method='POST')
try:
    with urllib.request.urlopen(req) as resp:
        print("Response:", resp.read().decode('utf-8'))
except Exception as e:
    print("Error:", e)
