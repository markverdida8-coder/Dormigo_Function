import os
import glob

old_ip = '10.149.229.109'
new_ip = '10.149.229.109'

def replace_in_file(filepath):
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
        if old_ip in content:
            content = content.replace(old_ip, new_ip)
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(content)
            print(f"Updated: {filepath}")
    except Exception as e:
        print(f"Error reading {filepath}: {e}")

project_dir = r"C:\Users\markv\AndroidStudioProjects\DormigoVA"
for root, _, files in os.walk(project_dir):
    for file in files:
        if file.endswith('.java') or file.endswith('.py') or file.endswith('.xml'):
            filepath = os.path.join(root, file)
            replace_in_file(filepath)

print("IP update complete!")
