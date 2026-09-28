import urllib.request
import json

def test():
    resp = urllib.request.urlopen('http://localhost/Dormigo_Backend/api/reviews.php?house_id=1').read().decode('utf-8')
    data = json.loads(resp)
    print("Reviews response success:", data.get("success"))
    print("Average rating:", data.get("average_rating"))
    print("Review count:", data.get("review_count"))
    print("Reviews rows count:", len(data.get("data", [])))

if __name__ == '__main__':
    test()
