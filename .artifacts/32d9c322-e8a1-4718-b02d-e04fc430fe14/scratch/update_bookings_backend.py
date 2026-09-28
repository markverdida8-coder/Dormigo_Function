path = r'C:\xampp\htdocs\Dormigo_Backend\api\bookings.php'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

old_select = """    $sql = "SELECT b.*, u.full_name, u.email, u.phone, r.room_number, r.room_type, r.advance_months, r.deposit_months, r.utility_deposit, r.other_fees, r.other_fees_description, bh.house_name, bh.landlord_id, bh.payment_due_day
            FROM bookings b
            JOIN users u ON b.user_id = u.user_id
            JOIN rooms r ON b.room_id = r.room_id
            JOIN boarding_houses bh ON r.house_id = bh.house_id";"""

new_select = """    $sql = "SELECT b.*, u.full_name, u.email, u.phone, r.room_number, r.room_type, r.advance_months, r.deposit_months, r.utility_deposit, r.other_fees, r.other_fees_description, bh.house_id, bh.house_name, bh.landlord_id, bh.payment_due_day, bh.cash_enabled, bh.gcash_qr_code, bh.gcash_updated_at
            FROM bookings b
            JOIN users u ON b.user_id = u.user_id
            JOIN rooms r ON b.room_id = r.room_id
            JOIN boarding_houses bh ON r.house_id = bh.house_id";"""

content = content.replace(old_select, new_select)

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)

print("Updated bookings.php successfully.")
