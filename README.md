# ECHO — Fluid, Modern Android Music Player

[![Android](https://img.shields.io/badge/Platform-Android_8.0+_(API_26--35)-3DDC84?style=flat&logo=android&logoColor=white)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?style=flat&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack_Compose-Material_3-4285F4?style=flat&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Media3](https://img.shields.io/badge/AndroidX_Media3-1.4.1-FF6F00?style=flat)](https://developer.android.com/media/media3)
[![License](https://img.shields.io/badge/Copyright-%C2%A9_2026_GRAVIT-blue.svg)](#legal--copyright)
[![CI/CD](https://github.com/Deadmonster-a1/ECHO/actions/workflows/android-release.yml/badge.svg)](https://github.com/Deadmonster-a1/ECHO/actions)

**ECHO v2.0** is a high-performance Android music player crafted with Jetpack Compose, AndroidX Media3, and reverse-engineered architectural blueprints from **LastWave-Native**. Designed with Liquid Glass frosted refraction surfaces, Material 3 Expressive spring physics, audiophile DSP controls (AutoEQ presets, independent tempo & pitch stretch, smart Automix crossfade, audio normalization), millisecond-synchronized lyrics, and dynamic Spotify Canvas visualizers.

---

## ✨ Features & Architecture (v2.0 Upgrade)

### 💎 Liquid Glass Design System & Expressive Motion
- **Liquid Glass Architecture:** Frosted translucent surfaces with chromatic specular highlight borders (`LiquidGlassSurface`, `LiquidGlassCard`, `LiquidGlassPill`), ambient artwork glow diffusion, and pure AMOLED pitch black mode.
- **Material 3 Expressive Springs:** Real-time spring physics (`ExpressiveMotion`) providing tactile button squish (`expressivePressScale`) and fluid bounce clicks (`expressiveBounceClickable`).
- **Floating Liquid Glass Mini-Player:** Gesture-driven pill with horizontal swipe-to-skip (left/right) and vertical swipe-up-to-expand to full Now Playing screen.

### 🎛️ Audiophile Acoustic Tuning & Playback Controls
- **Comprehensive Acoustic Tuning Sheet:** Real-time control of tempo & pitch stretch (0.50x to 1.50x, linked or free mode).
- **AutoEQ Parametric Headphone Profiling:** Mathematical biquad filters tuned to industry-standard Harman Target Curves for 2,500+ headphones (Sony WH-1000XM5/XM4, AirPods Pro 2 / Max, Sennheiser HD650, Moondrop Blessing 2, Samsung Buds 2 Pro, ATH-M50x).
- **Music Haptics Rhythm Feedback:** Android 12+ `VibratorManager` waveform synthesis translating sub-bass drops (40–130 Hz) and kick transients into synchronized tactile pulses (Whole Song, Bass & Beats, Vocals Only).
- **Harmonic Sine Wave Scrubber (`WavySeekBar`):** Dynamic rolling sine wave scrubber inspired by Android 13/14 Media Player with touch amplitude expansion and precision seeking.
- **In-App Tactile Software Volume Attenuator (`VolumeSlider`):** Logarithmic software volume control with single-tap mute and 100% boost shortcuts.
- **Continuous Genre Playback & Smart Endless Radio (`GenrePickerSheet` / `MusicGenre`):** Set preferred genres (Tollywood, Melodies, Mass Beats, Global Pop, Hip-Hop, EDM, Rock, Bollywood, Lo-Fi, Indie, Devotional) and lock playback so ECHO seamlessly auto-queues infinite tracks strictly within that chosen genre without drifting.
- **Auto-Match Current Track Genre:** 1-tap lock matching ongoing track metadata to continue playing in the exact same musical genre.
- **Equalizer Presets:** Studio Master (Flat), AutoEQ Headphone Profile, Bass Boost (+6dB), Vocal Clarity, Acoustic Warmth, Electronic Club.
- **Smart Automix & Crossfade:** Smooth DJ crossfade transitions between tracks with customizable duration (0s to 12s).
- **Direct Signal Path & Quality Badges:** Real-time stream telemetry (Hi-Res FLAC 24-bit/96kHz, Lossless, AAC 320kbps, direct hardware DAC sink routing, latency metrics).
- **Integrated Sleep Timer:** 15m, 30m, 45m, 60m timer with remaining time countdown and smooth audio fade-out.

### 🎤 Millisecond-Synchronized Live Lyrics (Spotify Color-Lyrics & LastWave)
- **Live Progressive Karaoke Sweep Fill:** Re-engineered progressive horizontal wipe mask (`Modifier.drawWithContent` with `BlendMode.SrcAtop`) filling vocal syllables smoothly in real-time according to `line.durationMs`.
- **Timing Offset Calibration Dialog (`LyricsOffsetDialog`):** Fine $\pm 250\text{ms}$, $\pm 1000\text{ms}$, and continuous slider ($-10\text{s} \dots +10\text{s}$) synchronization calibration with reset capability to compensate for Bluetooth/audio latency.
- **4-Tier Typography Sizing Dialog (`LyricsSizeDialog`):** Interactive scale switcher with `COMPACT` (17sp), `STANDARD` (22sp), `LARGE` (27sp), and `EXTRA_LARGE` (33sp stage monitor karaoke mode) with live preview box.
- **Multi-Script Dual Singing & Transliteration:** Instant toggle between English Romanized phonetics and native script subtitles (`Aa English` / `Native`).
- **Touch-Suspended Automated Glide-Back:** Pauses auto-scrolling when the user reads ahead or behind, and automatically glides back to the active vocal line after 2.2 seconds of inactivity without manual sync buttons.
- **Social Lyric Quote Card Generator (`ShareLyricsDialog`):** Styled Liquid Glass lyric card creator with native Android share intent support for Instagram Stories, WhatsApp, and social apps.
- **Fullscreen Immersive Studio Mode (`FullscreenLyricsSheet`):** Dynamic album art aura glow, top & bottom fade scrims, tap-to-seek timestamp navigation, and integrated bottom playback transport dock.
- **Dynamic Spotify Canvas Player:** Kinetic animated video loops with breathing ambient zoom, rotation, and cinematic film grain vignette scrims.

### 🔍 LastWave Multi-Tier Search Engine & Voice Discovery
- **Live Auto-Complete Suggestions:** Dual-tier debouncing (120ms suggestions debounce + 300ms search debounce) with real-time Google / YouTube live query completion and offline catalog fallbacks.
- **Categorized Multi-Tab Filtering:** Instant switching between `All`, `Songs`, `Artists`, `Albums`, and `Local Audio` with active theme color pill styling.
- **Persistent Search History:** SharedPreferences backed JSON array (`SearchHistoryRepository`) tracking up to 20 recent searches with reactive `StateFlow`, single-item `X` removal, and "Clear all" action.
- **Android Speech-to-Text Voice Recognition:** Hands-free voice search with native `RecognizerIntent.ACTION_RECOGNIZE_SPEECH`.
- **In-Memory LRU Search Cache:** 60-entry LRU cache for zero-latency recall when modifying queries or switching tabs.
- **Local Audio Indexer:** Sub-millisecond local device track scanner and instant playback.

### 🎨 Expanded Sound Colorway Theming Matrix & Dynamic Launcher Icons
- **12 Curated Palettes:** ECHO Lavender, ECHO Coral, ECHO Soft Blue, ECHO Mint, Spotify Classic, Midnight Sapphire, Royal Amethyst, Sunset Amber, plus newly added **Midnight AMOLED** (`#000000` pitch dark), **Cyberpunk Neon** (`#00F0FF` / `#FF0055`), **Sakura Blossom** (`#FF8DA1`), and **Solar Gold** (`#FFD166`).
- **Dynamic App Launcher Icon Aliases (Spotify & LastWave pattern):** 5 distinctive launcher styles (`Liquid Glass`, `Spotify Emerald`, `Pure Monochrome AMOLED`, `Cyberpunk Neon`, and `Sunset Amber`) dynamically switchable from Settings via `PackageManager.setComponentEnabledSetting` across declared activity aliases without interrupting ongoing audio.

### 📷 Scannables, Credits & Local Media Management
- **Interactive ECHO Sound Code Scanner (`SoundScannerDialog`):** Real-time animated camera viewfinder with laser sweeping beam and 1-tap clipboard URI auto-resolver for `echo:track:<id>`, `echo:artist:<name>`, and Spotify links.
- **Scannable Soundwave Barcodes (`SoundCodeDialog`):** Deterministic 28-bar audio waveform barcode generator matching Spotify Scannables (`ScannablesActivity`) with high-resolution artwork and instant share intent.
- **Studio Song Credits & Metadata Modal (`TrackCreditsDialog`):** Deep dive into track lineage: Performed by, Written by (Songwriters), Produced by (Mastering Engineers), Signal Path specifications (320 kbps / 24-bit 48kHz / -14.0 LUFS target), and Phonographic copyright.
- **Local Device Audio Files Indexer & Importer (`com.spotify.localfiles`):** Native `MediaStore.Audio` scanner and dedicated "Local Audio" tab in Library for zero-internet playback of device MP3, FLAC, and WAV audio tracks.

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

## Legal & Copyright

**© 2026 GRAVIT. All rights reserved.**

All application source code, `echo` branding, UI/UX design, Material 3 Expressive components, recommendation logic, and visual assets are the exclusive copyright and intellectual property of **GRAVIT**. Unauthorized redistribution, commercial exploitation, or removal of copyright notices is prohibited.

Third-party open-source dependencies (AndroidX, Jetpack Compose, Media3 ExoPlayer, Room, Coil, Kotlin/Coroutines) remain subject to their respective open-source licenses (Apache License 2.0), as listed inside the app under **Settings -> Legal & About -> Open-Source Licenses**.

Music metadata, artwork, lyrics, and third-party audio streams remain the copyrighted property of their respective artists, labels, and rights holders. For Privacy Policy, Terms & Conditions, or DMCA inquiries, see the in-app Legal Center or contact `legal@gravit.app`.
