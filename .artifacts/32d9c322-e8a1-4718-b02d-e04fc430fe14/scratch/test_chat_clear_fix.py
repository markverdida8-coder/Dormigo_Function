import urllib.request
import json
import time

def test():
    s_id = 6
    l_id = 3

    # Step 1: Send old message 1 from Student to Landlord
    req_send1 = urllib.request.Request('http://localhost/Dormigo_Backend/api/messages.php', data=json.dumps({'sender_id': s_id, 'receiver_id': l_id, 'message_text': 'OLD MESSAGE 1'}).encode('utf-8'), method='POST')
    print('Student Send 1:', urllib.request.urlopen(req_send1).read().decode('utf-8'))

    time.sleep(1)

    # Step 2: Landlord clears conversation
    req_clear = urllib.request.Request('http://localhost/Dormigo_Backend/api/messages.php', data=json.dumps({'user_id': l_id, 'other_user_id': s_id}).encode('utf-8'), method='DELETE')
    print('Landlord Clear:', urllib.request.urlopen(req_clear).read().decode('utf-8'))

    # Step 3: Check thread for Landlord (should be 0) & Student (should be >= 1)
    thread_l1 = json.loads(urllib.request.urlopen(f'http://localhost/Dormigo_Backend/api/messages.php?user_id={l_id}&other_user_id={s_id}').read().decode('utf-8'))
    thread_s1 = json.loads(urllib.request.urlopen(f'http://localhost/Dormigo_Backend/api/messages.php?user_id={s_id}&other_user_id={l_id}').read().decode('utf-8'))
    print('Landlord thread count right after clear:', len(thread_l1.get('data', [])))
    print('Student thread count right after clear:', len(thread_s1.get('data', [])))

    time.sleep(1)

    # Step 4: Student sends NEW message 2 later
    req_send2 = urllib.request.Request('http://localhost/Dormigo_Backend/api/messages.php', data=json.dumps({'sender_id': s_id, 'receiver_id': l_id, 'message_text': 'NEW MESSAGE 2'}).encode('utf-8'), method='POST')
    print('Student Send 2:', urllib.request.urlopen(req_send2).read().decode('utf-8'))

    # Step 5: Check thread for Landlord (MUST BE EXACTLY 1 message: 'NEW MESSAGE 2')
    thread_l2 = json.loads(urllib.request.urlopen(f'http://localhost/Dormigo_Backend/api/messages.php?user_id={l_id}&other_user_id={s_id}').read().decode('utf-8'))
    print('Landlord thread count after new message:', len(thread_l2.get('data', [])))
    if len(thread_l2.get('data', [])) > 0:
        print('Landlord visible messages:', [m['message_text'] for m in thread_l2.get('data', [])])

    # Step 6: Check thread for Student (sees all messages)
    thread_s2 = json.loads(urllib.request.urlopen(f'http://localhost/Dormigo_Backend/api/messages.php?user_id={s_id}&other_user_id={l_id}').read().decode('utf-8'))
    print('Student visible messages count:', len(thread_s2.get('data', [])))

if __name__ == '__main__':
    test()
