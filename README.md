# Dormigo

Dormigo is a boarding-house discovery and management system. This repository contains an Android application, separate PHP backends for Android and web portals, a public web site, and web portals for administrators, landlords, and students.

## Project Contributors

| Group Member | GitHub Account |
| --- | --- |
| John Villegas Cabansag | [Ryudev23](https://github.com/Ryudev23) |
| Mark Levi Canales | [markverdida8-coder](https://github.com/markverdida8-coder) |
| Mark Levi Canales | [mavecanalesswu-cpu](https://github.com/mavecanalesswu-cpu) |

Mark Levi Canales uses two GitHub accounts for contributions to this repository.

## Repository Components

| Component | Location | Purpose |
| --- | --- | --- |
| Android application | `app/` | Native Android application. |
| Android PHP backend | `android_backend/api/` | PHP API currently used by the Android application deployment. |
| Android PHP dependencies | `android_backend/composer.json` | Composer dependency definition for the Android backend. |
| Web PHP backend | `php_backend/api/` | PHP API used by the web portals. |
| Web database template | `php_backend/.env.example` | Example PostgreSQL environment variables only. |
| Admin web portal | `admin/` | Web administration portal. |
| Landlord web portal | `landlord/` | Web portal for landlords. |
| Student web portal | `student/` | Web portal for students. |
| Public web assets | `assets/` | Public website JavaScript, CSS, and images. |
| Gradle wrapper | `gradle/` | Gradle wrapper files and version catalog. |

## Repository Layout

```text
.
|- app/                         Android application module
|- android_backend/
|  |- api/                      PHP backend for Android
|  |- composer.json             PHP dependency definition
|  `- composer.lock             Locked PHP dependency versions
|- php_backend/
|  |- api/                      PHP backend for web portals
|  `- .env.example              PostgreSQL configuration template
|- admin/                       Admin web portal
|- landlord/                    Landlord web portal
|- student/                     Student web portal
|- assets/                      Public website assets
|- gradle/                      Gradle wrapper files and version catalog
`- gradlew / gradlew.bat        Gradle wrapper commands
```

## Database Initialization Status

The local development file named `dormigo_database.sql` is intentionally excluded from GitHub. It contains destructive `DROP` statements and sample data, so it is not safe as a shared installation script.

A complete, safe PostgreSQL database initialization script is not yet provided. Do not assume that cloning this repository is sufficient to create a working database, and do not run local SQL reset scripts against databases containing real data.

## Prerequisites

- Android Studio with Android SDK Platform 37. The Android module uses `compileSdk 37` and `targetSdk 37`.
- JDK 17 or newer for Gradle 9.6 and Android Gradle Plugin 9.4.1. The Android source is configured with Java 11 source and target compatibility.
- PHP with the PDO PostgreSQL extension enabled.
- PostgreSQL.
- Apache or XAMPP for serving the PHP and web portal directories.
- Composer when working with `android_backend/`; its `composer.json` currently declares PHPMailer.

## Clone And Build Android

Clone the repository:

```bash
git clone <repository-url>
cd Dormigo_GitHub_Cleanup
```

Open the cloned repository root in Android Studio, allow Gradle to sync, select an Android device or emulator, and run the `app` configuration.

To build a debug APK from the command line on Windows:

```powershell
.\gradlew.bat assembleDebug
```

The debug APK is generated under `app/build/outputs/apk/debug/`.

## PHP Backends And Deployment

The repository contains separate PHP backend directories:

- `android_backend/api/` contains Android-oriented endpoints and its Composer configuration is in `android_backend/composer.json`.
- `php_backend/api/` contains endpoints used by the web portals and has `php_backend/.env.example` for database configuration guidance.

These backends are separate code paths. They are not documented as fully integrated or interchangeable deployments. Before deployment, configure the intended server document root, PHP dependencies, database environment, upload directories, and endpoint URLs for the selected backend.

## PostgreSQL Environment Configuration

`php_backend/.env.example` is a template only. The web backend connection code in `php_backend/api/db.php` reads the variables from the server environment; PHP does not automatically load `.env` files.

Configure these environment variables in Apache or the server environment instead of committing credentials:

```text
DORMIGO_DB_HOST
DORMIGO_DB_PORT
DORMIGO_DB_NAME
DORMIGO_DB_USER
DORMIGO_DB_PASSWORD
```

For Apache, set the variables in the relevant virtual-host or Apache configuration, then restart Apache. Example names only:

```apache
SetEnv DORMIGO_DB_HOST "database-host"
SetEnv DORMIGO_DB_PORT "5432"
SetEnv DORMIGO_DB_NAME "database-name"
SetEnv DORMIGO_DB_USER "database-user"
SetEnv DORMIGO_DB_PASSWORD "set-this-in-the-server-environment"
```

For PHP CLI work, export the same variables in the shell session before invoking PHP. Keep real values out of tracked files and Git history.

## API URL Configuration

The Android API base URL requires environment-specific configuration. Review `app/src/main/java/com/dormigo/ApiClient.java` before running the Android application.

Some Android activities and web assets still contain legacy deployment URLs. API URLs are not fully centralized, so changing one base URL may not update every request path. Review and configure the required URLs for the target environment before deployment.

## Security And Git Hygiene

- Never commit database passwords, SMTP credentials, OTP logs, signing keys, or uploaded user files.
- Keep local `.env` files and server-specific configuration outside version control.
- Do not commit database reset scripts containing destructive statements or sample accounts.
- Rotate a credential immediately if it has been committed or shared.

## Known Limitations

- A complete, safe database initialization and migration path is not yet included in GitHub.
- The Android and web PHP backends are separate and are not yet documented as a fully integrated deployment.
- Android and web endpoint URLs still require deployment-specific configuration; legacy URLs remain in some files.
- The repository does not yet include comprehensive automated application tests.
- The project has no license, contribution guide, or security-policy document yet.
