-- ==============================================================================
-- DORMIGO POSTGRESQL DATABASE SCHEMA & SEED DATA (pgAdmin 4)
-- Target DBMS: PostgreSQL
-- ==============================================================================

-- Drop tables if they exist
DROP TABLE IF EXISTS notifications CASCADE;
DROP TABLE IF EXISTS messages CASCADE;
DROP TABLE IF EXISTS reviews CASCADE;
DROP TABLE IF EXISTS payments CASCADE;
DROP TABLE IF EXISTS bookings CASCADE;
DROP TABLE IF EXISTS boarding_house_amenities CASCADE;
DROP TABLE IF EXISTS amenities CASCADE;
DROP TABLE IF EXISTS rooms CASCADE;
DROP TABLE IF EXISTS house_photos CASCADE;
DROP TABLE IF EXISTS boarding_houses CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- Drop custom enum types if they exist
DROP TYPE IF EXISTS user_type_enum CASCADE;
DROP TYPE IF EXISTS house_status_enum CASCADE;
DROP TYPE IF EXISTS room_status_enum CASCADE;
DROP TYPE IF EXISTS booking_status_enum CASCADE;
DROP TYPE IF EXISTS payment_status_enum CASCADE;
DROP TYPE IF EXISTS notif_type_enum CASCADE;

-- Create Enum Types
CREATE TYPE user_type_enum AS ENUM ('STUDENT', 'LANDLORD');
CREATE TYPE house_status_enum AS ENUM ('ACTIVE', 'INACTIVE', 'PENDING');
CREATE TYPE room_status_enum AS ENUM ('AVAILABLE', 'OCCUPIED', 'MAINTENANCE', 'INACTIVE');
CREATE TYPE booking_status_enum AS ENUM ('PENDING', 'APPROVED', 'DECLINED', 'CANCELLED', 'ACTIVE', 'COMPLETED');
CREATE TYPE payment_status_enum AS ENUM ('PENDING', 'PAID', 'FAILED', 'CANCELLED', 'CONFIRMED');
CREATE TYPE notif_type_enum AS ENUM ('REQUEST', 'PAYMENT', 'MESSAGE', 'SYSTEM');

-- ==============================================================================
-- 1. USERS TABLE
-- ==============================================================================
CREATE TABLE users (
    user_id SERIAL PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(50) DEFAULT NULL,
    user_type user_type_enum NOT NULL,
    profile_image VARCHAR(255) DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ==============================================================================
-- 2. BOARDING HOUSES TABLE
-- ==============================================================================
CREATE TABLE boarding_houses (
    house_id SERIAL PRIMARY KEY,
    landlord_id INT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    house_name VARCHAR(255) NOT NULL,
    description TEXT DEFAULT NULL,
    address TEXT NOT NULL,
    house_rules TEXT DEFAULT NULL,
    status house_status_enum DEFAULT 'INACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ==============================================================================
-- 3. HOUSE PHOTOS TABLE
-- ==============================================================================
CREATE TABLE house_photos (
    photo_id SERIAL PRIMARY KEY,
    house_id INT NOT NULL REFERENCES boarding_houses(house_id) ON DELETE CASCADE,
    photo_path VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ==============================================================================
-- 4. ROOMS TABLE
-- ==============================================================================
CREATE TABLE rooms (
    room_id SERIAL PRIMARY KEY,
    house_id INT NOT NULL REFERENCES boarding_houses(house_id) ON DELETE CASCADE,
    room_number VARCHAR(50) NOT NULL,
    room_type VARCHAR(100) NOT NULL,
    capacity INT NOT NULL DEFAULT 1,
    monthly_rent NUMERIC(10,2) NOT NULL,
    status room_status_enum DEFAULT 'AVAILABLE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ==============================================================================
-- 5. AMENITIES MASTER TABLE
-- ==============================================================================
CREATE TABLE amenities (
    amenity_id SERIAL PRIMARY KEY,
    amenity_name VARCHAR(100) NOT NULL UNIQUE
);

-- ==============================================================================
-- 6. BOARDING HOUSE AMENITIES (JUNCTION TABLE)
-- ==============================================================================
CREATE TABLE boarding_house_amenities (
    house_id INT NOT NULL REFERENCES boarding_houses(house_id) ON DELETE CASCADE,
    amenity_id INT NOT NULL REFERENCES amenities(amenity_id) ON DELETE CASCADE,
    PRIMARY KEY (house_id, amenity_id)
);

-- ==============================================================================
-- 7. BOOKINGS TABLE
-- ==============================================================================
CREATE TABLE bookings (
    booking_id SERIAL PRIMARY KEY,
    user_id INT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    room_id INT NOT NULL REFERENCES rooms(room_id) ON DELETE CASCADE,
    move_in_date DATE NOT NULL,
    duration_months INT NOT NULL,
    status booking_status_enum DEFAULT 'PENDING',
    agreed_monthly_rent NUMERIC(10,2) NOT NULL,
    agreed_total_amount NUMERIC(10,2) NOT NULL,
    message_to_landlord TEXT DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ==============================================================================
-- 8. PAYMENTS TABLE
-- ==============================================================================
CREATE TABLE payments (
    payment_id SERIAL PRIMARY KEY,
    booking_id INT NOT NULL REFERENCES bookings(booking_id) ON DELETE CASCADE,
    payment_period INT NOT NULL DEFAULT 1,
    due_date DATE NOT NULL,
    amount NUMERIC(10,2) NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    payment_date DATE DEFAULT NULL,
    status payment_status_enum DEFAULT 'PENDING',
    transaction_ref VARCHAR(100) DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ==============================================================================
-- 9. REVIEWS TABLE
-- ==============================================================================
CREATE TABLE reviews (
    review_id SERIAL PRIMARY KEY,
    user_id INT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    house_id INT NOT NULL REFERENCES boarding_houses(house_id) ON DELETE CASCADE,
    rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment TEXT DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ==============================================================================
-- 10. MESSAGES TABLE (OPTIONAL - FOR CHAT FEATURE)
-- ==============================================================================
CREATE TABLE messages (
    message_id SERIAL PRIMARY KEY,
    sender_id INT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    receiver_id INT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    house_id INT DEFAULT NULL REFERENCES boarding_houses(house_id) ON DELETE CASCADE,
    message_text TEXT NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ==============================================================================
-- 11. NOTIFICATIONS TABLE (OPTIONAL - FOR IN-APP NOTIFICATIONS)
-- ==============================================================================
CREATE TABLE notifications (
    notification_id SERIAL PRIMARY KEY,
    user_id INT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    type notif_type_enum DEFAULT 'SYSTEM',
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


-- ==============================================================================
-- SAMPLE SEED DATA
-- ==============================================================================

-- 1. Pre-fill Master Amenities
INSERT INTO amenities (amenity_name) VALUES
('Wi-Fi'),
('Water Included'),
('Aircon'),
('Parking'),
('Curfew 10PM'),
('Kitchen Access'),
('Laundry Area');

-- 2. Add Test Users
INSERT INTO users (full_name, email, password, phone, user_type) VALUES
('Jimuel Destura', 'student@dormigo.com', 'password123', '+639123456789', 'STUDENT'),
('Alexxander Vince Rosales', 'landlord@dormigo.com', 'password123', '+639987654321', 'LANDLORD');

-- 3. Add Sample Boarding House (landlord_id = 2)
INSERT INTO boarding_houses (landlord_id, house_name, description, address, house_rules, status) VALUES
(2, 'Casa Amiga Boarding House', 'A quiet and clean boarding house near campus with free Wi-Fi and water.', 'Pelaez St., Brgy. Kalubihan, Cebu City', 'Curfew at 10:00 PM. No pets allowed.', 'ACTIVE');

-- 4. Link House to Amenities (house_id = 1, amenity_ids = 1, 2, 5)
INSERT INTO boarding_house_amenities (house_id, amenity_id) VALUES
(1, 1),
(1, 2),
(1, 5);

-- 5. Add Sample Rooms (house_id = 1)
INSERT INTO rooms (house_id, room_number, room_type, capacity, monthly_rent, status) VALUES
(1, 'Room 101', 'Solo room', 1, 2500.00, 'AVAILABLE'),
(1, 'Room 102', 'Shared room', 2, 1800.00, 'AVAILABLE');

-- 6. Add Sample Booking (user_id = 1, room_id = 1)
INSERT INTO bookings (user_id, room_id, move_in_date, duration_months, status, agreed_monthly_rent, agreed_total_amount, message_to_landlord) VALUES
(1, 1, '2026-09-01', 6, 'APPROVED', 2500.00, 15000.00, 'Hello! I am a BS IT student looking to move in next month.');

-- 7. Add Sample Payment (booking_id = 1)
INSERT INTO payments (booking_id, payment_period, due_date, amount, payment_method, payment_date, status, transaction_ref) VALUES
(1, 1, '2026-09-01', 2500.00, 'GCash', '2026-08-30', 'PAID', 'BHF-2026-0830-001');

-- 8. Add Sample Review (user_id = 1, house_id = 1)
INSERT INTO reviews (user_id, house_id, rating, comment) VALUES
(1, 1, 5, 'Great place! Very clean and accessible to the university.');
