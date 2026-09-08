# MeeLano Builder — Android app

Kotlin + Jetpack Compose (Material 3) client for the Builder server.
Replicates the YBee home screen: headline, suggestion chips, idea box, footer.

## Features

- 🏠 YBee-style home (EN/FA, RTL-aware)
- 🔨 Live build screen: progress, streaming logs, preview
- 🤖 APK download + **direct install** (`REQUEST_INSTALL_PACKAGES` + FileProvider)
- 🪟 Windows `.exe` / `Setup.exe` downloads
- 📷 QR share of the installable file
- ⭐ Templates, 📱 My apps, ⚙️ Settings (server URL, language, AUTO, AI key)

## Build

Easiest: GitHub **Actions → android-builder → Artifacts → app-debug.apk**.

Locally (Android Studio / command line, needs JDK 17 + Android SDK):

```bash
cd android
gradle :app:assembleDebug
# apk: app/build/outputs/apk/debug/app-debug.apk
```

## Connect to your server

Settings → Server URL:

- Emulator: `http://10.0.2.2:8000`
- Real phone on the same Wi-Fi: `http://<your-pc-lan-ip>:8000`

(Cleartext HTTP is allowed for self-hosted LAN servers.)
