# Dormigo - Android Studio Project

## Overview
Dormigo is a mobile application for boarding house discovery, room booking, tenant management, and rent payment tracking for both Students and Landlords.

## Project Structure
```
Dormigo_Android_Studio/
├── app/
│   ├── build.gradle             # App dependencies & Android configuration
│   └── src/main/
│       ├── AndroidManifest.xml   # Permissions, activities & intent filters
│       ├── java/com/dormigo/     # Activities & ApiClient (Java)
│       └── res/                  # Layouts, drawables, strings, themes
├── gradle/                      # Gradle wrapper & version catalogs
├── build.gradle                 # Project build script
├── settings.gradle              # Module settings
├── gradle.properties            # JVM & memory configuration
├── local.properties             # Android SDK path configuration
├── gradlew / gradlew.bat        # Gradle wrapper CLI
└── apk/
    └── app-debug.apk            # Pre-compiled debug APK (ready to install)
```

## How to Open in Android Studio
1. Launch **Android Studio**.
2. Click **File** -> **Open...**
3. Select this folder: `C:\Users\JOHN\Downloads\Dormigo_Android_Studio`.
4. Wait for Gradle to finish syncing dependencies.
5. Click the green **Run (▶)** button to launch the app on an Android Emulator or connected device.

## API Configuration
The app connects to the PHP/PostgreSQL backend via `ApiClient.java`:
- Default Base URL: `http://10.0.2.2/Dormigo/api/` (for Android Studio Emulator)
- If testing on a physical phone, replace `10.0.2.2` with your PC's local Wi-Fi IP address (e.g. `http://192.168.x.x/Dormigo/api/`) in `ApiClient.java`.

## Building via Command Line
To build the debug APK:
```bash
./gradlew assembleDebug
```
The output APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`.
