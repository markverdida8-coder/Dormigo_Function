path = r'C:\xampp\htdocs\Dormigo_Backend\api\boarding_houses.php'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

old_query = '''SELECT bh.*, (SELECT json_agg(hp.photo_path) FROM house_photos hp WHERE hp.house_id = bh.house_id) AS photo_paths
                          FROM boarding_houses bh'''

new_query = '''SELECT bh.*, (SELECT json_agg(hp.photo_path) FROM house_photos hp WHERE hp.house_id = bh.house_id) AS photo_paths,
                          bhv.verification_status, bhv.rejection_reason
                          FROM boarding_houses bh
                          LEFT JOIN boarding_house_verification bhv ON bh.house_id = bhv.house_id'''

if old_query in content:
    content = content.replace(old_query, new_query)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)
    print("Updated boarding_houses.php successfully!")
else:
    print("Query string not found.")
