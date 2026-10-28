# ECHO — Fluid, Modern Android Music Player

[![Android](https://img.shields.io/badge/Platform-Android_8.0+_(API_26--35)-3DDC84?style=flat&logo=android&logoColor=white)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?style=flat&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack_Compose-Material_3-4285F4?style=flat&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Media3](https://img.shields.io/badge/AndroidX_Media3-1.4.1-FF6F00?style=flat)](https://developer.android.com/media/media3)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
[![CI/CD](https://github.com/Deadmonster-a1/ECHO/actions/workflows/android-release.yml/badge.svg)](https://github.com/Deadmonster-a1/ECHO/actions)

**ECHO** is a high-performance Android music player crafted with Jetpack Compose and AndroidX Media3. Designed with a liquid-inspired glassmorphism visual identity, robust background playback lifecycle safety, offline LRU caching, native home-screen launcher widgets, and a zero-cost automated over-the-air update pipeline.

---

## ✨ Features & Architecture

### 🎵 Authoritative Audio Playback Pipeline
- **Single Source of Truth:** Powered by AndroidX Media3 ExoPlayer unified under `PlaybackManager` and `MediaPlaybackService`.
- **Background Playback Lifecycle:** Foreground service handling with full audio focus management, noisy audio receiver (auto-pause on headphone disconnect), and phone call interruption recovery.
- **250 MB LRU Audio Cache:** Streamed tracks are buffered locally in an encrypted LRU disk cache to enable instant zero-bandwidth offline repeat plays.
- **Dynamic Waveform Visualizer:** Real-time soundwave seek bar synchronized with ExoPlayer playback progress.

### 📱 Android Platform Integration & Polish
- **Rich Media Notifications:** Native Android MediaStyle notifications with synchronized playback controls, dynamic album artwork, and lock-screen seek bars.
- **Native Home-Screen Widgets:** 
  - **Compact (3x1):** Quick play/pause and skip controls for minimal homescreens.
  - **Expanded (4x2):** Full track artwork, metadata, dynamic progress bar, and queue navigation.
- **Play Store & Android 15 Ready:**
  - `applicationId`: `com.echo.musicplayer`
  - Target SDK 35, Compile SDK 35, Minimum SDK 26 (Android 8.0 Oreo).
  - Strict compliance with Android 14/15 foreground service restrictions (`mediaPlayback` type).
  - Production R8 minification and resource shrinking enabled: Standalone APK is just **4.15 MB**; App Bundle is **7.75 MB**.

### 🚀 Zero-Cost In-App Auto-Updates
- **Dual-Strategy Update Engine (`AppUpdateManager.kt`):**
  1. *Fast Raw CDN Check:* Reads `version.json` via GitHub raw CDN (zero rate-limiting).
  2. *Fallback REST API:* Queries `api.github.com/repos/Deadmonster-a1/ECHO/releases/latest`.
- **In-App Package Installer:** Downloads APKs directly to secure cache storage and triggers native installation via Android `FileProvider` without leaving the app.
- **Settings Screen Integration:** Manual "Check for Updates" button under Settings > About & Architecture.

---

## 🛠️ Project Structure

```
ECHO/
├── .github/workflows/          # Automated GitHub Actions CI/CD release workflow
│   └── android-release.yml
├── own-app/                    # Native Android Studio Project
│   ├── app/
│   │   ├── src/main/java/com/spotify/clone/
│   │   │   ├── core/
│   │   │   │   ├── data/       # Audio catalog repository & Room database
│   │   │   │   ├── player/     # Media3 ExoPlayer, cache & MediaSessionService
│   │   │   │   └── update/     # In-app auto-updater & FileProvider integration
│   │   │   ├── ui/             # Jetpack Compose liquid design system & screens
│   │   │   └── widget/         # Native 3x1 and 4x2 home-screen widgets
│   │   └── build.gradle.kts    # App build configuration & dependencies
│   ├── build.gradle.kts        # Root Gradle build configuration
│   ├── settings.gradle.kts     # Project settings
│   └── gradlew / gradlew.bat   # Gradle wrappers for Linux/macOS and Windows
├── version.json                # Live update manifest for in-app updater
├── push-update.ps1             # 1-command release deployment script
├── FREE_HOSTING_AND_LIVE_UPDATES_GUIDE.md  # Comprehensive deployment guide
└── PLAYSTORE_DEPLOYMENT_METRICS.md         # Play Store compliance audit report
```

---

## 📦 How to Build & Run

### Prerequisites
- Android Studio Ladybug (2024.2.1) or newer
- JDK 17 (Temurin recommended)
- Android SDK with Platform 35 installed

### Local Build via Command Line
```powershell
cd own-app

# Run JVM Unit Tests
./gradlew test

# Run Android Lint Check
./gradlew lintRelease

# Assemble Release APK
./gradlew assembleRelease

# Build Google Play App Bundle (AAB)
./gradlew bundleRelease
```

---

## 🚀 How to Deploy Live Updates Directly from Code

Whenever you want to release a new version of ECHO to your users, simply run the included PowerShell script from the root directory:

```powershell
.\push-update.ps1 -Version "1.0.1" -Notes "Added sleep timer and improved audio caching"
```

### What this script does automatically:
1. Increments `versionCode` and updates `versionName` in `own-app/app/build.gradle.kts`.
2. Updates `version.json` with your changelog and download link.
3. Commits the changes to Git and creates a signed Git tag (`v1.0.1`).
4. Pushes the commit and tag to GitHub (`origin main --tags`).
5. **GitHub Actions CI/CD automatically:**
   - Compiles and minifies the APK and AAB.
   - Runs all unit and lint tests.
   - Creates a new GitHub Release under `Deadmonster-a1/ECHO`.
   - Attaches `echo-release.apk` and `echo-release.aab`.
6. Active ECHO users get an instant update notification inside the app!

---

## 📄 License
This project is licensed under the Apache 2.0 License.
