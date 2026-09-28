import urllib.request
import json

url = "http://10.149.229.109/Dormigo_Backend/api/bookings.php"
try:
    with urllib.request.urlopen(url) as resp:
        data = json.loads(resp.read().decode('utf-8'))
        for b in data.get('data', []):
            if b.get('booking_id') == 14:
                print("Booking 14:")
                print("status:", b.get('status'))
                print("initial_payment_completed:", b.get('initial_payment_completed'))
                print("next_due_date:", b.get('next_due_date'))
except Exception as e:
    print("Error:", e)
