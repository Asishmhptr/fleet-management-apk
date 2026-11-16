# Fleet Management - Android APK

Android wrapper for the **Fleet Management** web application (live at [fleet-booking-62ac4.web.app](https://fleet-booking-62ac4.web.app)), built using Capacitor.

## Architecture

- **Remote Mode**: The APK loads `https://fleet-booking-62ac4.web.app` directly over HTTPS. All web changes deploy instantly to users without requiring an APK update.
- **Offline Fallback**: A local `www/index.html` fallback displays a styled "No internet connection" screen with a Retry button when offline.
- **Native Plugins**:
  - `@capacitor/local-notifications` ("LocalNotifications") - Scheduled and instant heads-up alerts with sound and vibration.
  - `@capacitor-community/background-geolocation` ("BackgroundGeolocation") - Background tracking for driver trips via foreground service.
  - `@capacitor/app` ("App") - App state and lifecycle listeners.
  - `@capacitor/browser` ("Browser") - In-app external link handling.
  - `@capacitor/splash-screen` - Dark branded splash screen (`#1F2528`).
  - `@capacitor/status-bar` - Dark branded status bar (`#1F2528`).
- **Permissions**:
  - `INTERNET`, `POST_NOTIFICATIONS`, `SCHEDULE_EXACT_ALARM`, `USE_EXACT_ALARM`, `RECEIVE_BOOT_COMPLETED`, `ACCESS_COARSE_LOCATION`, `ACCESS_FINE_LOCATION`, `ACCESS_BACKGROUND_LOCATION`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_LOCATION`, `WAKE_LOCK`, `REQUEST_INSTALL_PACKAGES`.

---

## How to Publish an Update (Step-by-Step)

When you need to update the native shell (permissions, plugins, or native configuration):

### 1. Bump the Version
Open `android/app/build.gradle` and increment:
- `versionCode` (integer, e.g., `1` -> `2`)
- `versionName` (semver string, e.g., `"1.0.0"` -> `"1.0.1"`)

Also update `version` in `package.json`:
```bash
npm version 1.0.1 --no-git-tag-version
```

### 2. Sync Capacitor
Sync the native platform assets:
```bash
npx cap sync android
```

### 3. Commit, Tag, and Push
```bash
git add .
git commit -m "Release v1.0.1"
git tag v1.0.1
git push origin main
git push origin v1.0.1
```

### 4. Automatic Build & Release
The GitHub Actions workflow (`.github/workflows/build-release.yml`) will automatically:
1. Build the Android release APK with JDK 17 and Gradle.
2. Sign the APK using the stored repository secrets.
3. Name the artifact `fleet-management.apk`.
4. Create a new GitHub Release for tag `v1.0.1` and attach `fleet-management.apk`.
5. Users opening the app will automatically see the "Download update" prompt.

---

## Keystore & Signing Secrets

The release APK is signed with `fleet-management.jks`.

### GitHub Actions Secrets
The following secrets are configured in repository Settings -> Secrets and variables -> Actions:
- `KEYSTORE_BASE64`: Base64-encoded string of `fleet-management.jks`
- `KEYSTORE_PASSWORD`: Keystore password
- `KEY_ALIAS`: `fleetmanagement`
- `KEY_PASSWORD`: Key password

> ⚠️ **CRITICAL**: Keep a secure backup of `fleet-management.jks` and its passwords in a safe location (e.g. password manager, secure cloud storage). If this key is lost, future updates cannot be installed over existing installations!
