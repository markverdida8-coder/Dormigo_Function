# Dormigo

Dormigo is a boarding-house discovery and management system. This repository contains an Android application, a PHP/PostgreSQL API, a public web site, and web portals for administrators, landlords, and students.

## Components

| Component | Location | Purpose |
| --- | --- | --- |
| Android application | `app/` | Native Java Android application for students, landlords, and administrators. |
| Public web site | Root `*.html` and `assets/` | Public browsing, login, registration, and property details. |
| Admin portal | `admin/` | Administration, verification, reporting, payments, and subscriptions. |
| Landlord portal | `landlord/` | Property, room, booking, tenant, payment, and profile management. |
| Student portal | `student/` | Bookings, messages, notifications, payments, reviews, and profile management. |
| PHP API | `php_backend/api/` | Session-aware REST-style endpoints used by the web portals. |
| PostgreSQL schema | `dormigo_database.sql` | Development schema and sample seed data. |

## Repository Layout

```text
.
|- app/                         Android app module
|- gradle/                      Gradle wrapper and version catalog
|- admin/                       Admin web portal
|- landlord/                    Landlord web portal
|- student/                     Student web portal
|- assets/                      Shared public-site assets
|- php_backend/
|  |- api/                      PHP endpoints and helpers
|  `- .env.example              Safe database environment reference
|- dormigo_database.sql         PostgreSQL schema and development seed data
`- gradlew / gradlew.bat        Gradle wrapper commands
```

## Prerequisites

- Android Studio with an Android SDK that supports `compileSdk 37`.
- JDK 11.
- PHP with the PDO PostgreSQL extension enabled.
- PostgreSQL.
- Apache or another PHP-capable web server. XAMPP on Windows is supported by the current deployment paths.

## Database Configuration

The PHP API reads all database connection settings from environment variables. It does not load `.env` files automatically.

1. Use `php_backend/.env.example` as a reference only. Do not copy real values into a tracked file.
2. Set these variables in your web-server or container environment:

```text
DORMIGO_DB_HOST
DORMIGO_DB_PORT
DORMIGO_DB_NAME
DORMIGO_DB_USER
DORMIGO_DB_PASSWORD
```

For Apache, define the variables in the applicable virtual-host or Apache configuration using `SetEnv`, then restart Apache. For local PHP CLI commands, define them in the shell environment before invoking PHP.

Example Apache configuration:

```apache
SetEnv DORMIGO_DB_HOST "localhost"
SetEnv DORMIGO_DB_PORT "5432"
SetEnv DORMIGO_DB_NAME "dormigo"
SetEnv DORMIGO_DB_USER "postgres"
SetEnv DORMIGO_DB_PASSWORD "use-a-local-secret"
```

The API returns a generic error when configuration or connection fails and writes technical details only to the server error log.

### Database Schema

`dormigo_database.sql` contains `DROP TABLE` statements and development sample data. Do not run it against a production database or a database containing live user, booking, payment, or verification data.

The schema currently needs reconciliation with the newer admin, verification, and subscription endpoints before a fresh installation can support every feature. See [Known Limitations](#known-limitations).

## Local Web Setup

1. Serve this directory through Apache/PHP, preserving the current directory structure.
2. Configure the database environment variables above.
3. Ensure the PHP process can create and write its runtime upload and log directories. These directories must remain untracked.
4. Open the appropriate portal in the browser. With an XAMPP document root, an example base URL is `http://localhost/Dormigo_Function-WEB-ADMIN/`.

| Portal | Example URL |
| --- | --- |
| Public site | `http://localhost/Dormigo_Function-WEB-ADMIN/` |
| Admin | `http://localhost/Dormigo_Function-WEB-ADMIN/admin/` |
| Landlord | `http://localhost/Dormigo_Function-WEB-ADMIN/landlord/` |
| Student | `http://localhost/Dormigo_Function-WEB-ADMIN/student/` |

## Android Setup

1. Open this repository root in Android Studio.
2. Allow Gradle to sync dependencies.
3. Configure the API base URLs for your environment before running the app. The main value is in `app/src/main/java/com/dormigo/ApiClient.java`; additional activities also contain legacy deployment URLs.
4. Connect a device or start an emulator, then run the `app` configuration.

Build a debug APK from the command line:

```powershell
.\gradlew.bat assembleDebug
```

The generated APK is placed under `app/build/outputs/apk/debug/` and is intentionally ignored by Git.

## Security and Git Hygiene

- Never commit real database passwords, SMTP credentials, OTP logs, Android signing keys, or user-uploaded files.
- Keep `php_backend/api/mail_config.local.php` local only. It is ignored by Git.
- Keep local database values in the host environment, not in source files.
- Rotate a credential immediately if it has been shared or committed.
- Review `dormigo_database.sql` sample accounts before sharing the repository publicly.

## Known Limitations

- Several Android, PHP, and web-portal paths still reference a separate `Dormigo_Backend` deployment and a development LAN address. Centralize these values before production deployment.
- The supplied SQL schema does not yet create every table, enum value, or column used by the admin, verification, notification, and subscription API endpoints.
- The Android test directories currently contain only stock template tests; application behavior is not yet covered by meaningful automated tests.
- The project has no license, contribution guide, or security-policy document yet.
