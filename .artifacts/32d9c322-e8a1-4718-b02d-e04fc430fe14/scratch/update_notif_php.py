path = r'C:\xampp\htdocs\Dormigo_Backend\api\notifications.php'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

old_patch = '''} elseif ($method === 'PATCH') {
    $data = json_decode(file_get_contents("php://input"), true);
    if (isset($data['notification_id'])) {
        $stmt = $pdo->prepare("UPDATE notifications SET is_read = true WHERE notification_id = :notification_id");
        $stmt->execute(['notification_id' => $data['notification_id']]);
    } elseif (isset($data['user_id'])) {
        // Mark all as read for user
        $stmt = $pdo->prepare("UPDATE notifications SET is_read = true WHERE user_id = :user_id");
        $stmt->execute(['user_id' => $data['user_id']]);
    }
    echo json_encode(["success" => true]);'''

new_patch = '''} elseif ($method === 'PATCH') {
    $data = json_decode(file_get_contents("php://input"), true);
    if (isset($data['notification_id'])) {
        $stmt = $pdo->prepare("UPDATE notifications SET is_read = true WHERE notification_id = :notification_id");
        $stmt->execute(['notification_id' => $data['notification_id']]);
    } elseif (isset($data['user_id']) && isset($data['type'])) {
        $stmt = $pdo->prepare("UPDATE notifications SET is_read = true WHERE user_id = :user_id AND type = :type");
        $stmt->execute(['user_id' => $data['user_id'], 'type' => $data['type']]);
    } elseif (isset($data['user_id'])) {
        $stmt = $pdo->prepare("UPDATE notifications SET is_read = true WHERE user_id = :user_id");
        $stmt->execute(['user_id' => $data['user_id']]);
    }
    echo json_encode(["success" => true]);'''

content = content.replace(old_patch, new_patch)
with open(path, 'w', encoding='utf-8') as f:
    f.write(content)
print("Updated notifications.php")
