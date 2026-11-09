# Dormigo Website

Dormigo is a boarding-house discovery and management website with public pages and separate student, landlord, and administrator portals. The Android application is maintained on the `master` branch.

## Project Structure

| Component | Location | Purpose |
| --- | --- | --- |
| Public website | Root HTML files and `assets/` | Property discovery, registration, login, and property details. |
| Admin portal | `admin/` | Administration, verification, reporting, payments, and subscriptions. |
| Landlord portal | `landlord/` | Property, room, booking, tenant, payment, and profile management. |
| Student portal | `student/` | Bookings, messages, notifications, payments, reviews, and profile management. |
| PHP API | `php_backend/api/` | API endpoints used by the website and portals. |
| Database schema | `dormigo_database.sql` | Development schema and sample data. |

## Requirements

- Apache or another PHP-capable web server.
- PHP with the PDO PostgreSQL extension.
- PostgreSQL.

## Local Setup

1. Serve this directory through Apache/PHP, preserving its directory structure.
2. Set `DORMIGO_DB_HOST`, `DORMIGO_DB_PORT`, `DORMIGO_DB_NAME`, `DORMIGO_DB_USER`, and `DORMIGO_DB_PASSWORD` in the web-server environment.
3. Ensure the PHP process can create and write its runtime upload and log directories.
4. Open the public site or a portal using the corresponding path under your local web-server URL.

`dormigo_database.sql` contains destructive table-reset statements and development sample data. Do not run it against a production database or one containing live user data.
