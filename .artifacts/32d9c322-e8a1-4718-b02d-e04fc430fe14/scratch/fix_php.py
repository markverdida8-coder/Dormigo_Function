path = r'C:\xampp\htdocs\Dormigo_Backend\api\boarding_houses.php'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

# Fix the syntax error on DELETE method
bad_str = 'echo json_encode(["success" => false, "message" => "House ID is required."});'
good_str = 'echo json_encode(["success" => false, "message" => "House ID is required."]);'

if bad_str in content:
    content = content.replace(bad_str, good_str)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)
    print("Successfully fixed boarding_houses.php!")
else:
    print("Could not find the target string.")
