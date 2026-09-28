import os

directory = r'C:\Users\markv\AndroidStudioProjects\DormigoVA\app\src\main\java\com\dormigo'
for filename in os.listdir(directory):
    if filename.endswith('.java'):
        filepath = os.path.join(directory, filename)
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
        if '172.21.240.109' in content:
            content = content.replace('172.21.240.109', '10.149.229.109')
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(content)
            print(f"Updated IP in {filename}")
print("IP update complete!")
