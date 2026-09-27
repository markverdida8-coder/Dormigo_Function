import urllib.request
import json

url = "http://172.21.240.109/Dormigo_Backend/api/get_verifications.php?status=PENDING"
try:
    with urllib.request.urlopen(url) as resp:
        print(resp.read().decode('utf-8'))
except Exception as e:
    print(e)
