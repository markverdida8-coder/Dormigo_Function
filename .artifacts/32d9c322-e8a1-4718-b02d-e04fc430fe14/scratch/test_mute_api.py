import urllib.request
import json

base_url = "http://10.149.229.109/Dormigo_Backend/api/messages.php"

# 1. Mute for 8 hours
payload = {"action": "mute_conversation", "user_id": 2, "other_user_id": 5, "duration_hours": 8}
req = urllib.request.Request(base_url, data=json.dumps(payload).encode('utf-8'), headers={'Content-Type': 'application/json'}, method='POST')
with urllib.request.urlopen(req) as resp:
    print("Mute 8h:", resp.read().decode('utf-8'))

# 2. Check get_mute_status
url_status = f"{base_url}?action=get_mute_status&user_id=2&other_user_id=5"
with urllib.request.urlopen(url_status) as resp:
    print("Get status:", resp.read().decode('utf-8'))

# 3. Check chat list
url_list = f"{base_url}?user_id=2"
with urllib.request.urlopen(url_list) as resp:
    res = json.loads(resp.read().decode('utf-8'))
    for item in res.get('data', []):
        if item.get('other_user_id') == 5:
            print("Chat list item for 5:", item)

# 4. Unmute
payload_unmute = {"action": "unmute_conversation", "user_id": 2, "other_user_id": 5}
req_unmute = urllib.request.Request(base_url, data=json.dumps(payload_unmute).encode('utf-8'), headers={'Content-Type': 'application/json'}, method='POST')
with urllib.request.urlopen(req_unmute) as resp:
    print("Unmute:", resp.read().decode('utf-8'))
