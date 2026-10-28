# Spotify Android Clean-Room Clone

A complete, production-grade Android music streaming application in Jetpack Compose, faithfully reproducing Spotify's visual design, unidirectional state architecture, background audio playback subsystem, and offline database persistence.

---

## 1. Architectural Highlights

### Modern Architecture & Design Patterns
- **Presentation Layer**: 100% **Jetpack Compose** with Material 3, custom Spotify Dark Theme tokens (`#121212`, `#181818`), typography, and Encore-like components (`SpotifyPlayFab`, `SpotifyHeartButton`, `SpotifyFilterChip`).
- **Unidirectional Data Flow (MVI)**: Replicating Spotify's **Mobius Loop** state pattern via Kotlin `StateFlow` and pure functional state transitions `(State, Event) -> NewState`.
- **Audio Engine (Media3 & ExoPlayer)**:
  - `com.spotify.clone.core.player.PlaybackService`: AndroidX `MediaSessionService` foreground playback engine.
  - System lock-screen media controls, Wear OS integration, and Android Auto readiness.
  - Headset unplug auto-pause (`BecomingNoisyReceiver`) and audio focus management.
- **Persistent Storage**: **AndroidX Room** database caching tracks, custom user playlists, and favorites (`AppDatabase`, `TrackEntity`, `PlaylistEntity`).
- **Data Layer**: Multi-source repository scanning on-device audio files (`MediaStore.Audio.Media`) with an integrated royalty-free streaming catalog and real-time synchronized lyrics.

---

## 2. Implemented Screen Inventory

| Screen | Location | Key UI & Behavior Features |
|---|---|---|
| **Home Feed** | `ui/home/HomeScreen.kt` | User avatar, filter chips (All, Music, Podcasts), 6-card quick-play grid, "Made for You" horizontal playlist carousel, "Popular Artists" circular avatar carousel, and track recommendations. |
| **Search & Browse** | `ui/search/SearchScreen.kt` | Instant search input, debounced query pipeline, camera scanner trigger, and 2-column colored genre category cards (Pop, Hip-Hop, Rock, Indie, Dance). |
| **Your Library** | `ui/library/LibraryScreen.kt` | Filter chips, Grid/List view toggle, permanent "Liked Songs" gradient card, user playlist creator modal, and playlist track counts. |
| **Playlist Detail** | `ui/playlist/PlaylistDetailScreen.kt` | Collapsible gradient artwork banner, title, metadata, green Play/Shuffle FAB, and track list with durations. |
| **Persistent Mini-Player** | `ui/components/MiniPlayerBar.kt` | Docked bar above bottom nav with album thumbnail, title, artist, favorite toggle, Play/Pause button, and linear progress scrubber. Click expands to fullscreen. |
| **Fullscreen Now-Playing** | `ui/player/NowPlayingScreen.kt` | Header context, giant square album art, interactive seekbar with elapsed/remaining timers, transport controls (Shuffle, Previous, Big Play FAB, Next, Repeat), device picker button, and lyrics preview. |
| **Queue Bottom Sheet** | `ui/player/QueueSheetContent.kt` | Sticky header for current track and reorderable list of upcoming tracks. |

---

## 3. Project Directory Map

```
own-app/
├── gradle/
│   ├── libs.versions.toml             # Gradle Version Catalog (AGP, Compose, Media3, Room)
│   └── wrapper/
│       └── gradle-wrapper.properties  # Gradle 8.10.2 distribution
├── app/
│   ├── build.gradle.kts               # Module dependencies & compiler config
│   ├── src/main/
│   │   ├── AndroidManifest.xml        # Declarations, permissions, MediaSessionService
│   │   ├── java/com/spotify/clone/
│   │   │   ├── SpotifyCloneApp.kt     # Application class & notification channels
│   │   │   ├── MainActivity.kt        # Edge-to-edge Compose container & permissions
│   │   │   ├── core/
│   │   │   │   ├── model/             # Track, Album, Artist, Playlist, PlaybackState
│   │   │   │   ├── designsystem/      # Spotify colors, typography, Encore tokens
│   │   │   │   ├── mvi/               # MviViewModel unidirectional state loop
│   │   │   │   ├── database/          # Room AppDatabase, DAOs, Entities
│   │   │   │   ├── player/            # Media3 PlaybackService & MusicController
│   │   │   │   └── data/              # AudioRepository & SampleCatalog
│   │   │   └── ui/
│   │   │       ├── components/        # BottomNavBar, MiniPlayerBar, TrackRowItem, CategoryCard
│   │   │       ├── home/              # HomeScreen & HomeViewModel
│   │   │       ├── search/            # SearchScreen & SearchViewModel
│   │   │       ├── library/           # LibraryScreen & LibraryViewModel
│   │   │       ├── playlist/          # PlaylistDetailScreen
│   │   │       ├── player/            # NowPlayingScreen & QueueSheetContent
│   │   │       └── navigation/        # Screen routes & AppNavHost root scaffold
│   │   └── res/
│   │       ├── drawable/              # ic_spotify_logo.xml
│   │       └── values/                # strings.xml, colors.xml, themes.xml
```

---

## 4. How to Open and Build

### In Android Studio
1. Open Android Studio.
2. Select **File > Open** and choose:
   `P:\Reverse engineering applications\Spotify\own-app`
3. Allow Gradle to sync dependencies.
4. Select `app` and run on `emulator-5554` or any connected Android device (Android 8.0+ / API 26+).

### Via Command Line
```powershell
cd "P:\Reverse engineering applications\Spotify\own-app"
.\gradlew assembleDebug
```
The compiled APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`
