# ProGuard / R8 Rules for ECHO Music Player

# Keep Room Database DAOs and Entities
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keepclassmembers class * {
    @androidx.room.Query *;
    @androidx.room.Insert *;
    @androidx.room.Delete *;
    @androidx.room.Update *;
}

# Keep Media3 ExoPlayer & Session components
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**
-keep interface androidx.media3.common.Player$Listener { *; }
-keep interface androidx.media3.session.MediaSession$Callback { *; }
-keep class com.google.common.util.concurrent.ListenableFuture { *; }

# Keep Coil Image Loading
-keep class coil.** { *; }
-dontwarn coil.**

# Keep App Data & Domain Models
-keep class com.spotify.clone.core.model.** { *; }
-keep class com.spotify.clone.core.database.** { *; }
-keep class com.spotify.clone.core.mvi.** { *; }
-keep class com.spotify.clone.widget.** { *; }

# Kotlin Coroutines & Flow
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**

# Compose runtime and state
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}
