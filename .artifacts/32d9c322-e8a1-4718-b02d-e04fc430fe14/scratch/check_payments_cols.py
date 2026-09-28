import urllib.request
import json

url = "http://localhost/Dormigo_Backend/api/payments.php"
try:
    with urllib.request.urlopen(url) as resp:
        data = json.loads(resp.read().decode('utf-8'))
        if data.get('data'):
            print("Columns in payments record:")
            for k in data['data'][0].keys():
                print("  ", k)
        else:
            print("Payments table is currently empty.")
except Exception as e:
    print("Error:", e)
