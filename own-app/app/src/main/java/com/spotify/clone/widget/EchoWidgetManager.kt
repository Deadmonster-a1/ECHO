package com.spotify.clone.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.spotify.clone.MainActivity
import com.spotify.clone.R
import com.spotify.clone.core.model.PlaybackState
import com.spotify.clone.core.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Authoritative Widget Manager for ECHO
 * Coordinates state updates, asynchronous artwork caching, and safe RemoteViews generation
 * across both Compact and Expanded home-screen widgets without battery-draining polling.
 */
object EchoWidgetManager {
    private const val TAG = "EchoWidgetManager"
    private const val PREFS_NAME = "echo_widget_state"

    const val ACTION_PLAY_PAUSE = "com.spotify.clone.widget.ACTION_PLAY_PAUSE"
    const val ACTION_NEXT = "com.spotify.clone.widget.ACTION_NEXT"
    const val ACTION_PREV = "com.spotify.clone.widget.ACTION_PREV"
    const val ACTION_OPEN_LIBRARY = "com.spotify.clone.widget.ACTION_OPEN_LIBRARY"

    private val scope = CoroutineScope(Dispatchers.IO + Job())

    fun updateAll(context: Context, state: PlaybackState) {
        val appContext = context.applicationContext
        // Persist state so widgets retain context across reboots and process recreation
        persistState(appContext, state)

        scope.launch {
            val appWidgetManager = AppWidgetManager.getInstance(appContext) ?: return@launch
            val compactComponent = ComponentName(appContext, EchoCompactWidgetProvider::class.java)
            val expandedComponent = ComponentName(appContext, EchoExpandedWidgetProvider::class.java)

            val compactIds = appWidgetManager.getAppWidgetIds(compactComponent)
            val expandedIds = appWidgetManager.getAppWidgetIds(expandedComponent)

            if (compactIds.isEmpty() && expandedIds.isEmpty()) {
                return@launch
            }

            val track = state.currentTrack ?: getPersistedTrack(appContext)
            val isPlaying = state.isPlaying
            val progress = state.progress

            // Asynchronously resolve album art bitmap with rounded corners
            val artBitmap = loadRoundedArtwork(appContext, track?.coverArtUrl)

            if (compactIds.isNotEmpty()) {
                val compactViews = buildCompactRemoteViews(appContext, track, isPlaying, artBitmap)
                appWidgetManager.updateAppWidget(compactComponent, compactViews)
            }

            if (expandedIds.isNotEmpty()) {
                val expandedViews = buildExpandedRemoteViews(appContext, track, isPlaying, progress, artBitmap)
                appWidgetManager.updateAppWidget(expandedComponent, expandedViews)
            }
        }
    }

    fun updateSingleProvider(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray, isCompact: Boolean) {
        if (appWidgetIds.isEmpty()) return
        val appContext = context.applicationContext

        scope.launch {
            val track = getPersistedTrack(appContext)
            val isPlaying = getPersistedIsPlaying(appContext)
            val progress = getPersistedProgress(appContext)
            val artBitmap = loadRoundedArtwork(appContext, track?.coverArtUrl)

            val views = if (isCompact) {
                buildCompactRemoteViews(appContext, track, isPlaying, artBitmap)
            } else {
                buildExpandedRemoteViews(appContext, track, isPlaying, progress, artBitmap)
            }

            for (widgetId in appWidgetIds) {
                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }
    }

    private fun buildCompactRemoteViews(
        context: Context,
        track: Track?,
        isPlaying: Boolean,
        artBitmap: Bitmap?
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_compact)

        // Set metadata
        views.setTextViewText(R.id.widget_track_title, track?.title ?: "ECHO")
        views.setTextViewText(R.id.widget_artist_name, track?.artistName ?: "Feel the sound")

        // Artwork
        if (artBitmap != null) {
            views.setImageViewBitmap(R.id.widget_album_art, artBitmap)
        } else {
            views.setImageViewResource(R.id.widget_album_art, R.drawable.ic_echo_symbol)
        }

        // Play/Pause icon
        val playPauseIcon = if (isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play
        views.setImageViewResource(R.id.widget_btn_play_pause, playPauseIcon)

        // Pending Intents
        views.setOnClickPendingIntent(R.id.widget_compact_root, createOpenAppPendingIntent(context))
        views.setOnClickPendingIntent(R.id.widget_btn_play_pause, createBroadcastPendingIntent(context, ACTION_PLAY_PAUSE, 101))
        views.setOnClickPendingIntent(R.id.widget_btn_next, createBroadcastPendingIntent(context, ACTION_NEXT, 102))
        views.setOnClickPendingIntent(R.id.widget_btn_prev, createBroadcastPendingIntent(context, ACTION_PREV, 103))

        return views
    }

    private fun buildExpandedRemoteViews(
        context: Context,
        track: Track?,
        isPlaying: Boolean,
        progress: Float,
        artBitmap: Bitmap?
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_expanded)

        // Set metadata
        views.setTextViewText(R.id.widget_track_title, track?.title ?: "ECHO")
        views.setTextViewText(R.id.widget_artist_name, track?.artistName ?: "Feel the sound")
        views.setTextViewText(R.id.widget_album_name, track?.albumName ?: "Studio Master")

        // Progress bar
        val progressPercent = (progress * 100f).toInt().coerceIn(0, 100)
        views.setProgressBar(R.id.widget_progress_bar, 100, progressPercent, false)

        // Artwork
        if (artBitmap != null) {
            views.setImageViewBitmap(R.id.widget_album_art, artBitmap)
        } else {
            views.setImageViewResource(R.id.widget_album_art, R.drawable.ic_echo_symbol)
        }

        // Play/Pause icon
        val playPauseIcon = if (isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play
        views.setImageViewResource(R.id.widget_btn_play_pause, playPauseIcon)

        // Pending Intents
        views.setOnClickPendingIntent(R.id.widget_expanded_root, createOpenAppPendingIntent(context))
        views.setOnClickPendingIntent(R.id.widget_btn_play_pause, createBroadcastPendingIntent(context, ACTION_PLAY_PAUSE, 201))
        views.setOnClickPendingIntent(R.id.widget_btn_next, createBroadcastPendingIntent(context, ACTION_NEXT, 202))
        views.setOnClickPendingIntent(R.id.widget_btn_prev, createBroadcastPendingIntent(context, ACTION_PREV, 203))
        views.setOnClickPendingIntent(R.id.widget_btn_library, createOpenAppPendingIntent(context, "tab_library"))

        return views
    }

    private fun createOpenAppPendingIntent(context: Context, extraTab: String? = null): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (extraTab != null) {
                putExtra("nav_tab", extraTab)
            }
        }
        return PendingIntent.getActivity(
            context,
            if (extraTab != null) 301 else 300,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createBroadcastPendingIntent(context: Context, action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, EchoWidgetActionReceiver::class.java).apply {
            this.action = action
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private suspend fun loadRoundedArtwork(context: Context, url: String?): Bitmap? = withContext(Dispatchers.IO) {
        if (url.isNullOrBlank()) return@withContext null
        try {
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(url)
                .size(300, 300)
                .allowHardware(false)
                .build()
            val result = loader.execute(request)
            if (result is SuccessResult) {
                val original = result.drawable.toBitmap()
                return@withContext getRoundedCornerBitmap(original, 24f)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load artwork for widget: ${e.message}")
        }
        null
    }

    private fun getRoundedCornerBitmap(bitmap: Bitmap, cornerRadius: Float): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint().apply {
            isAntiAlias = true
            color = -0x1
        }
        val rect = Rect(0, 0, bitmap.width, bitmap.height)
        val rectF = RectF(rect)
        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)
        return output
    }

    private fun persistState(context: Context, state: PlaybackState) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val track = state.currentTrack
        prefs.edit().apply {
            if (track != null) {
                putString("track_id", track.id)
                putString("track_title", track.title)
                putString("artist_name", track.artistName)
                putString("album_name", track.albumName)
                putString("cover_art_url", track.coverArtUrl)
                putLong("duration_ms", track.durationMs)
            }
            putBoolean("is_playing", state.isPlaying)
            putFloat("progress", state.progress)
            apply()
        }
    }

    private fun getPersistedTrack(context: Context): Track? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val id = prefs.getString("track_id", null) ?: return null
        return Track(
            id = id,
            title = prefs.getString("track_title", "ECHO") ?: "ECHO",
            artistName = prefs.getString("artist_name", "Feel the sound") ?: "Feel the sound",
            albumName = prefs.getString("album_name", "Studio Master") ?: "Studio Master",
            coverArtUrl = prefs.getString("cover_art_url", "") ?: "",
            durationMs = prefs.getLong("duration_ms", 210000L),
            mediaUri = ""
        )
    }

    private fun getPersistedIsPlaying(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean("is_playing", false)
    }

    private fun getPersistedProgress(context: Context): Float {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getFloat("progress", 0f)
    }
}
