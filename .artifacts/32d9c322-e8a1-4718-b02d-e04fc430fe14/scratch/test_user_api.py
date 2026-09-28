import urllib.request
import json

url = "http://10.149.229.109/Dormigo_Backend/api/users.php?user_id=2"
try:
    with urllib.request.urlopen(url) as resp:
        print(resp.read().decode('utf-8'))
except Exception as e:
    print(e)
