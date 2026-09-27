import urllib.request
import json

url = "http://localhost/Dormigo_Backend/api/boarding_houses.php"
try:
    with urllib.request.urlopen(url) as resp:
        body = resp.read().decode('utf-8')
        json_data = json.loads(body)
        print("Success:", json_data.get("success"))
        data = json_data.get("data", [])
        print("Total houses:", len(data))
        for h in data:
            print("---")
            print("ID:", h.get("house_id"))
            print("Name:", h.get("house_name"))
            print("Address:", h.get("address"))
            print("Status:", h.get("status"))
            print("Verification Status:", h.get("verification_status"))
            print("Photo Paths:", h.get("photo_paths"))
except Exception as e:
    print("Error:", e)
