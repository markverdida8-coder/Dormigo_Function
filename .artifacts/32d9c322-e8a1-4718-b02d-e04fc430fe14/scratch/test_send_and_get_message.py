import urllib.request
import json

base_url = "http://10.149.229.109/Dormigo_Backend/api/messages.php"

# 1. Send Message
payload = {
    "sender_id": 1,
    "receiver_id": 2,
    "house_id": 11,
    "message_text": "Hello, is this room still available?"
}
data = json.dumps(payload).encode('utf-8')
req = urllib.request.Request(base_url, data=data, headers={'Content-Type': 'application/json'}, method='POST')
with urllib.request.urlopen(req) as resp:
    print("Send Message Response:", resp.read().decode('utf-8'))

# 2. Get Thread
url_thread = f"{base_url}?user_id=1&other_user_id=2"
with urllib.request.urlopen(url_thread) as resp:
    print("Get Thread Response:", resp.read().decode('utf-8'))
