path = r'C:\xampp\htdocs\Dormigo_Backend\api\boarding_houses.php'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

old_get = '''if ($method === 'GET') {
    $stmt = $pdo->query("SELECT bh.*, (SELECT json_agg(hp.photo_path) FROM house_photos hp WHERE hp.house_id = bh.house_id) AS photo_paths,
                          bhv.verification_status, bhv.rejection_reason
                          FROM boarding_houses bh
                          LEFT JOIN boarding_house_verification bhv ON bh.house_id = bhv.house_id
                          ORDER BY bh.house_id DESC");
    $houses = $stmt->fetchAll();
    foreach ($houses as &$house) {
        if (isset($house['photo_paths']) && $house['photo_paths'] !== null) {
            $house['photo_paths'] = json_decode($house['photo_paths']);
        } else {
            $house['photo_paths'] = [];
        }
    }
    echo json_encode(["success" => true, "data" => $houses]);'''

new_get = '''if ($method === 'GET') {
    $stmt = $pdo->query("SELECT bh.*, COALESCE((SELECT json_agg(hp.photo_path) FROM house_photos hp WHERE hp.house_id = bh.house_id), '[]'::json) AS photo_paths,
                          bhv.verification_status, bhv.rejection_reason
                          FROM boarding_houses bh
                          LEFT JOIN boarding_house_verification bhv ON bh.house_id = bhv.house_id
                          ORDER BY bh.house_id DESC");
    $houses = $stmt->fetchAll();
    foreach ($houses as &$house) {
        if (is_string($house['photo_paths'])) {
            $house['photo_paths'] = json_decode($house['photo_paths'], true);
        }
        if (!is_array($house['photo_paths'])) {
            $house['photo_paths'] = [];
        }
    }
    echo json_encode(["success" => true, "data" => $houses]);'''

if old_get in content:
    content = content.replace(old_get, new_get)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)
    print("Fixed boarding_houses.php GET successfully!")
else:
    print("Old GET block not found.")
