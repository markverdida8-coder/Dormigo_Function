import urllib.request
import json

url = "http://172.21.240.109/Dormigo_Backend/api/messages.php"
payload = {
    "sender_id": 1,
    "receiver_id": 2,
    "house_id": None,
    "message_text": "Hello test message without house"
}
req = urllib.request.Request(url, data=json.dumps(payload).encode('utf-8'), headers={'Content-Type': 'application/json'})
try:
    with urllib.request.urlopen(req) as resp:
        print(resp.read().decode('utf-8'))
except Exception as e:
    print(e)
