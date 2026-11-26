package com.spotify.clone.core.player

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.spotify.clone.MainActivity
import com.spotify.clone.R
import com.spotify.clone.SpotifyCloneApp
import com.spotify.clone.core.settings.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PlaybackService : MediaSessionService() {

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private var equalizer: Equalizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        SettingsManager.init(this)
        initializePlayer()
        observeDspSettings()
    }

    private fun observeDspSettings() {
        serviceScope.launch {
            SettingsManager.eqUpdateSignal.collectLatest { applyDspSettings() }
        }
        serviceScope.launch {
            SettingsManager.audioNormalization.collectLatest { applyDspSettings() }
        }
    }

    @OptIn(UnstableApi::class)
    private fun attachAudioEffects(audioSessionId: Int) {
        if (audioSessionId == C.AUDIO_SESSION_ID_UNSET || audioSessionId <= 0) return
        releaseAudioEffects()
        try {
            equalizer = Equalizer(0, audioSessionId)
        } catch (e: Exception) {
            Log.w(TAG, "Hardware Equalizer unavailable on session $audioSessionId: ${e.message}")
        }
        try {
            loudnessEnhancer = LoudnessEnhancer(audioSessionId)
        } catch (e: Exception) {
            Log.w(TAG, "LoudnessEnhancer unavailable on session $audioSessionId: ${e.message}")
        }
        applyDspSettings()
    }

    private fun applyDspSettings() {
        val prefs = getSharedPreferences("spotify_eq_prefs", Context.MODE_PRIVATE)
        val userEqEnabled = prefs.getBoolean("eq_enabled", true)
        val normEnabled = SettingsManager.audioNormalization.value
        val useAutoBands = SettingsManager.autoGenreEq.value && prefs.contains("auto_eq_preset")
        val bandPrefix = if (useAutoBands) "auto_band_" else "band_"
        val userBands = FloatArray(5) { i ->
            prefs.getFloat("$bandPrefix$i", prefs.getFloat("band_$i", 0f))
        }
        // Bypass hardware/HAL Equalizer when flat ("Normal" = all 0 dB) so Android's
        // AudioEffect mixer does not apply a -3..-6 dB preamp headroom cut unnecessarily.
        val enableEq = shouldEnableEqualizer(userEqEnabled, userBands)

        try {
            equalizer?.let { eq ->
                eq.enabled = enableEq
                if (enableEq) {
                    val range = eq.bandLevelRange
                    val minMb = range[0].toInt()
                    val maxMb = range[1].toInt()
                    val numBands = eq.numberOfBands.toInt()

                    for (b in 0 until numBands) {
                        val centerHz = eq.getCenterFreq(b.toShort()) / 1000f
                        val userBandIdx = when {
                            centerHz < 120f -> 0
                            centerHz < 450f -> 1
                            centerHz < 1800f -> 2
                            centerHz < 6500f -> 3
                            else -> 4
                        }
                        val levelMb = scaleEqMillibels(userBands[userBandIdx], minMb, maxMb)
                        eq.setBandLevel(b.toShort(), levelMb)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed applying EQ settings: ${e.message}")
        }

        try {
            loudnessEnhancer?.let { le ->
                le.enabled = normEnabled
                le.setTargetGain(if (normEnabled) NORMALIZATION_TARGET_GAIN_MB else 0)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed applying LoudnessEnhancer: ${e.message}")
        }
    }

    private fun releaseAudioEffects() {
        try { equalizer?.release() } catch (_: Exception) {}
        equalizer = null
        try { loudnessEnhancer?.release() } catch (_: Exception) {}
        loudnessEnhancer = null
    }

    @OptIn(UnstableApi::class)
    private fun initializePlayer() {
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36")
            .setDefaultRequestProperties(mapOf("Referer" to "https://www.jiosaavn.com/"))
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(20000)
            .setAllowCrossProtocolRedirects(true)

        val cachedDataSourceFactory = AudioCacheManager.createCacheDataSourceFactory(this, httpDataSourceFactory)
        val mediaSourceFactory = DefaultMediaSourceFactory(cachedDataSourceFactory)
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                15_000, // minBufferMs: 15s audio cushion
                45_000, // maxBufferMs: 45s max RAM/disk streaming buffer
                750,    // bufferForPlaybackMs: fast start (<1s)
                1_500   // bufferForPlaybackAfterRebufferMs
            )
            .setBackBuffer(15_000, true)
            .build()

        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build().apply {
                addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        Log.e(TAG, "ExoPlayer error: ${error.errorCodeName} - ${error.message}", error)
                    }

                    override fun onAudioSessionIdChanged(audioSessionId: Int) {
                        attachAudioEffects(audioSessionId)
                    }
                })
                attachAudioEffects(audioSessionId)
            }

        val sessionActivityIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            sessionActivityIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val callback = object : MediaSession.Callback {
            private fun sanitizeExternalMediaItems(
                controller: MediaSession.ControllerInfo,
                items: MutableList<MediaItem>
            ): MutableList<MediaItem> {
                if (controller.packageName == packageName) return items
                return items.filterTo(mutableListOf()) { item ->
                    val uri = item.localConfiguration?.uri ?: item.requestMetadata.mediaUri
                    val scheme = uri?.scheme?.lowercase()
                    scheme == "https" || scheme == "http"
                }
            }

            override fun onSetMediaItems(
                mediaSession: MediaSession,
                controller: MediaSession.ControllerInfo,
                mediaItems: MutableList<MediaItem>,
                startIndex: Int,
                startPositionMs: Long
            ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
                val safeItems = sanitizeExternalMediaItems(controller, mediaItems)
                val safeStart = startIndex.coerceIn(0, (safeItems.size - 1).coerceAtLeast(0))
                return Futures.immediateFuture(
                    MediaSession.MediaItemsWithStartPosition(safeItems, safeStart, startPositionMs)
                )
            }

            override fun onAddMediaItems(
                mediaSession: MediaSession,
                controller: MediaSession.ControllerInfo,
                mediaItems: MutableList<MediaItem>
            ): ListenableFuture<MutableList<MediaItem>> {
                return Futures.immediateFuture(sanitizeExternalMediaItems(controller, mediaItems))
            }
        }

        mediaSession = MediaSession.Builder(this, player!!)
            .setSessionActivity(pendingIntent)
            .setCallback(callback)
            .build()

        try {
            val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
                .setChannelId(SpotifyCloneApp.PLAYBACK_CHANNEL_ID)
                .setChannelName(R.string.app_name)
                .build()
            setMediaNotificationProvider(notificationProvider)
        } catch (e: Exception) {
            Log.w(TAG, "Notification provider fallback: ${e.message}")
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val p = mediaSession?.player
        if (p == null || !p.playWhenReady || p.mediaItemCount == 0 || p.playbackState == Player.STATE_ENDED || p.playbackState == Player.STATE_IDLE) {
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        serviceScope.cancel()
        releaseAudioEffects()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        player = null
        super.onDestroy()
    }

    companion object {
        private const val TAG = "EchoPlaybackService"
        const val NORMALIZATION_TARGET_GAIN_MB = 600

        /**
         * Returns true only when EQ is enabled AND at least one band deviates from 0 dB,
         * avoiding unnecessary Android AudioEffect headroom attenuation on flat presets.
         */
        fun shouldEnableEqualizer(userEqEnabled: Boolean, bands: FloatArray): Boolean {
            return userEqEnabled && bands.any { kotlin.math.abs(it) >= 0.05f }
        }

        /**
         * Maps a user dB setting in [-12, +12] into hardware millibels respecting
         * asymmetric min/max device ranges.
         */
        fun scaleEqMillibels(userDb: Float, minMb: Int, maxMb: Int): Short {
            val clampedDb = userDb.coerceIn(-12f, 12f)
            val scaled = if (clampedDb >= 0f) {
                (clampedDb / 12f) * maxMb
            } else {
                (clampedDb / 12f) * (-minMb)
            }
            return scaled.toInt().coerceIn(minMb, maxMb).toShort()
        }
    }
}
