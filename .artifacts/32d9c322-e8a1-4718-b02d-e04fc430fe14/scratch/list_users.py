import urllib.request
import json

url = "http://10.149.229.109/Dormigo_Backend/api/users.php"
with urllib.request.urlopen(url) as resp:
    res = json.loads(resp.read().decode('utf-8'))
    for u in res.get('data', []):
        print(u.get('user_id'), u.get('full_name'), u.get('email'))
