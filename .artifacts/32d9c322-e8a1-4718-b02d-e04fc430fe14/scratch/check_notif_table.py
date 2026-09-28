import urllib.request
import json

url = "http://localhost/Dormigo_Backend/api/notifications.php"
try:
    req = urllib.request.Request(url, method='GET')
    with urllib.request.urlopen(req) as resp:
        print(resp.read().decode('utf-8'))
except Exception as e:
    print("Error:", e)
