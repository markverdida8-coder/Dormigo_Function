import urllib.request
import urllib.parse
import json

url = "http://localhost/Dormigo_Backend/api/submit_verification.php"
data = urllib.parse.urlencode({'house_id': 8, 'landlord_id': 2}).encode('utf-8')
try:
    req = urllib.request.Request(url, data=data, method='POST')
    with urllib.request.urlopen(req) as resp:
        print(resp.read().decode('utf-8'))
except Exception as e:
    print(e)
