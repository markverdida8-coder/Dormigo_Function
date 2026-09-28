import urllib.request
import json

def test():
    # Student = 6, Landlord = 3
    s_id = 6
    l_id = 3

    # 1. Student sends a message to Landlord
    req_send1 = urllib.request.Request('http://localhost/Dormigo_Backend/api/messages.php', data=json.dumps({'sender_id': s_id, 'receiver_id': l_id, 'message_text': 'Hi Landlord!'}).encode('utf-8'), method='POST')
    print('Send 1 response:', urllib.request.urlopen(req_send1).read().decode('utf-8'))

    # 2. Fetch list for Student 6 & Landlord 3
    r_s1 = json.loads(urllib.request.urlopen(f'http://localhost/Dormigo_Backend/api/messages.php?user_id={s_id}').read().decode('utf-8'))
    r_l1 = json.loads(urllib.request.urlopen(f'http://localhost/Dormigo_Backend/api/messages.php?user_id={l_id}').read().decode('utf-8'))
    print('Student chats count:', len(r_s1.get('data', [])))
    print('Landlord chats count:', len(r_l1.get('data', [])))

    # 3. Student deletes conversation with Landlord
    req_del = urllib.request.Request('http://localhost/Dormigo_Backend/api/messages.php', data=json.dumps({'user_id': s_id, 'other_user_id': l_id}).encode('utf-8'), method='DELETE')
    print('Student Delete response:', urllib.request.urlopen(req_del).read().decode('utf-8'))

    # 4. Verify Student NO LONGER sees conversation, but Landlord STILL sees conversation!
    r_s2 = json.loads(urllib.request.urlopen(f'http://localhost/Dormigo_Backend/api/messages.php?user_id={s_id}').read().decode('utf-8'))
    r_l2 = json.loads(urllib.request.urlopen(f'http://localhost/Dormigo_Backend/api/messages.php?user_id={l_id}').read().decode('utf-8'))
    print('Student chats count after delete:', len(r_s2.get('data', [])))
    print('Landlord chats count after student delete:', len(r_l2.get('data', [])))

    # 5. Landlord sends a new message to Student
    req_send2 = urllib.request.Request('http://localhost/Dormigo_Backend/api/messages.php', data=json.dumps({'sender_id': l_id, 'receiver_id': s_id, 'message_text': 'Hello student, replying back!'}).encode('utf-8'), method='POST')
    print('Landlord Send response:', urllib.request.urlopen(req_send2).read().decode('utf-8'))

    # 6. Verify conversation REAPPEARS for Student!
    r_s3 = json.loads(urllib.request.urlopen(f'http://localhost/Dormigo_Backend/api/messages.php?user_id={s_id}').read().decode('utf-8'))
    print('Student chats count after landlord new message:', len(r_s3.get('data', [])))

if __name__ == '__main__':
    test()
