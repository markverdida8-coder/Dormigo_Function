import urllib.request
import json

def test():
    url = 'http://localhost/Dormigo_Backend/api/reject_verification.php'
    payload = {
        'verification_id': 1,
        'admin_id': 1,
        'rejection_reason': 'Business permit expired.'
    }
    req = urllib.request.Request(url, data=json.dumps(payload).encode('utf-8'), headers={'Content-Type': 'application/json'}, method='POST')
    resp = urllib.request.urlopen(req).read().decode('utf-8')
    print('Reject Landlord Verification Response:', resp)

if __name__ == '__main__':
    test()
