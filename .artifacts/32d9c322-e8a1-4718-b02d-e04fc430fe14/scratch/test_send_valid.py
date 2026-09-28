import urllib.request
import json

base_url = "http://10.149.229.109/Dormigo_Backend/api/messages.php"

payload = {
    "sender_id": 8,
    "receiver_id": 9,
    "house_id": 15,
    "message_text": "Hello landlord test!"
}
data = json.dumps(payload).encode('utf-8')
req = urllib.request.Request(base_url, data=data, headers={'Content-Type': 'application/json'}, method='POST')
with urllib.request.urlopen(req) as resp:
    print("Send Response:", resp.read().decode('utf-8'))

url_thread = f"{base_url}?user_id=8&other_user_id=9"
with urllib.request.urlopen(url_thread) as resp:
    print("Get Thread:", resp.read().decode('utf-8'))
