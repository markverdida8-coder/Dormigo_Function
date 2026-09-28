import urllib.request
import json

url = "http://10.149.229.109/Dormigo_Backend/api/messages.php?user_id=5"
try:
    with urllib.request.urlopen(url) as resp:
        print(resp.read().decode('utf-8'))
except Exception as e:
    print(e)
