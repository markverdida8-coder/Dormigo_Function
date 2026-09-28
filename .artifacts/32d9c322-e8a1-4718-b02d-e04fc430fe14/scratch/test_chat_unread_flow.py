import urllib.request
import json

base_url = "http://10.149.229.109/Dormigo_Backend/api/messages.php"

def send_msg(sender, receiver, text):
    payload = {"sender_id": sender, "receiver_id": receiver, "message_text": text}
    data = json.dumps(payload).encode('utf-8')
    req = urllib.request.Request(base_url, data=data, headers={'Content-Type': 'application/json'}, method='POST')
    with urllib.request.urlopen(req) as resp:
        return json.loads(resp.read().decode('utf-8'))

def get_conversations(user_id):
    url = f"{base_url}?user_id={user_id}"
    req = urllib.request.Request(url, method='GET')
    with urllib.request.urlopen(req) as resp:
        return json.loads(resp.read().decode('utf-8'))

def get_thread(user_id, other_id):
    url = f"{base_url}?user_id={user_id}&other_user_id={other_id}"
    req = urllib.request.Request(url, method='GET')
    with urllib.request.urlopen(req) as resp:
        return json.loads(resp.read().decode('utf-8'))

print("--- STEP 1: Sending message from 2 to 5 ---")
res1 = send_msg(2, 5, "Hello user 5, initial unread test message")
print("Send result:", res1)

print("\n--- STEP 2: Checking user 5's conversation list ---")
convs1 = get_conversations(5)
for c in convs1.get('data', []):
    if c.get('other_user_id') == 2:
        print(f"User 2 -> User 5: unread_count={c.get('unread_count')}, is_read={c.get('is_read')}, text='{c.get('message_text')}'")

print("\n--- STEP 3: User 5 opens thread with User 2 ---")
thread = get_thread(5, 2)
print("Thread fetched, messages count:", len(thread.get('data', [])))

print("\n--- STEP 4: Checking user 5's conversation list after opening thread ---")
convs2 = get_conversations(5)
for c in convs2.get('data', []):
    if c.get('other_user_id') == 2:
        print(f"User 2 -> User 5: unread_count={c.get('unread_count')}, is_read={c.get('is_read')}, text='{c.get('message_text')}'")

print("\n--- STEP 5: Sending ANOTHER message from 2 to 5 later ---")
res2 = send_msg(2, 5, "Hello user 5, second unread message later!")
print("Send result:", res2)

print("\n--- STEP 6: Checking user 5's conversation list again ---")
convs3 = get_conversations(5)
for c in convs3.get('data', []):
    if c.get('other_user_id') == 2:
        print(f"User 2 -> User 5: unread_count={c.get('unread_count')}, is_read={c.get('is_read')}, text='{c.get('message_text')}'")
