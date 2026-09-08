# 🔨 MeeLano Builder

**Type one short sentence → get installable software.**
A YBee-style AI app-builder: Android client + build server.

- 📱 **Android app** (`android/`, Kotlin + Jetpack Compose) — same look as the
  YBee screenshot: *“What do you want to make today?”*, suggestion chips,
  idea box, live build logs, preview, **APK download + direct install**, QR share.
- 🖥️ **Build server** (`server/`, Python + FastAPI) — turns the idea into a real
  multi-platform project:
  - 🪟 Windows **`.exe`** (PyInstaller) + **`Setup.exe`** installer (NSIS)
  - 🤖 Android **`.apk`** (Gradle) — installs directly on any phone
  - 📦 Source bundle + 👁 live preview (always included)
  - 🌐 Built-in web UI (same screens, runs in a browser)
- ☁️ **Cloud builds** via GitHub Actions — real `.exe` / `Setup.exe` / `.apk`
  with zero local toolchains.

📖 **راهنمای کامل فارسی: [README_FA.md](README_FA.md)**

## Quick start (2 minutes)

```bash
cd server
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8000
```

Open http://localhost:8000 → type *“Create a Blog reader app”* → watch it build →
download sources + preview instantly.

## How real binaries are produced

| Machine | Windows `.exe` + `Setup.exe` | Android `.apk` |
|---|---|---|
| Server with toolchains | PyInstaller (+ NSIS if installed) | Gradle + Android SDK |
| **GitHub Actions cloud** (recommended) | ✅ automatic (`build-generated.yml`) | ✅ automatic |
| Plain server | one-click `build_exe.bat` + `installer.nsi` in the bundle | `gradlew assembleDebug` project in the bundle |

Enable cloud builds:

```bash
export GITHUB_REPO="Companymeelano/Server"
export GITHUB_TOKEN="ghp_..."   # repo + actions:write
```

Then every job automatically dispatches the workflow, waits, and attaches the
`.exe` / `Setup.exe` / `.apk` as downloads in the app. You can also trigger
`build-generated.yml` manually from the GitHub web UI (Actions tab).

Optional AI customisation (otherwise the built-in template engine is used):

```bash
export OPENAI_API_KEY="sk-..."
export OPENAI_BASE_URL="https://api.openai.com/v1"  # any OpenAI-compatible endpoint
```

…or enter the key in the Android app's **Settings** (sent per-job).

## Projects

| Folder | What | Build |
|---|---|---|
| `android/` | MeeLano Builder app (client) | Android Studio, or Actions → `android-builder.yml` → APK |
| `server/` | FastAPI build server + web UI | `pytest`, `uvicorn`, `Dockerfile` |

API overview: `POST /api/v1/jobs` → poll `GET /api/v1/jobs/{id}` (or SSE
`/logs/stream`) → `GET /api/v1/jobs/{id}/preview`, `GET /api/v1/artifacts/{id}/{file}`.

Health: `GET /api/v1/health` → `{ok, version, ai, cloud_build}`.

## License

Private project of MeeLano. All rights reserved.
