package com.spotify.clone.core.settings

import android.content.Context
import android.content.SharedPreferences
import com.spotify.clone.core.player.AudioCacheManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object SettingsManager {
    private const val PREFS_NAME = "spotify_app_settings"

    private const val KEY_STREAMING_QUALITY = "streaming_quality"
    private const val KEY_DOWNLOAD_QUALITY = "download_quality"
    private const val KEY_AUTO_ADJUST_QUALITY = "auto_adjust_quality"
    private const val KEY_DOWNLOAD_OVER_CELLULAR = "download_over_cellular"
    private const val KEY_AUDIO_NORMALIZATION = "audio_normalization"
    private const val KEY_CROSSFADE_SECONDS = "crossfade_seconds"
    private const val KEY_AUTOMIX = "automix_enabled"
    private const val KEY_GAPLESS = "gapless_enabled"
    private const val KEY_MONO_AUDIO = "mono_audio"
    private const val KEY_AUTOPLAY = "autoplay_enabled"
    private const val KEY_PLAYBACK_SPEED = "playback_speed"
    private const val KEY_ALLOW_EXPLICIT = "allow_explicit"
    private const val KEY_SHOW_UNPLAYABLE = "show_unplayable"
    private const val KEY_CANVAS = "canvas_enabled"
    private const val KEY_PRIVATE_SESSION = "private_session"
    private const val KEY_LISTENING_ACTIVITY = "listening_activity"
    private const val KEY_SHOW_TOP_ARTISTS = "show_top_artists"

    private var prefs: SharedPreferences? = null

    private val _streamingQuality = MutableStateFlow("Very High (320 kbps)")
    val streamingQuality: StateFlow<String> = _streamingQuality.asStateFlow()

    private val _downloadQuality = MutableStateFlow("Very High (320 kbps)")
    val downloadQuality: StateFlow<String> = _downloadQuality.asStateFlow()

    private val _autoAdjustQuality = MutableStateFlow(true)
    val autoAdjustQuality: StateFlow<Boolean> = _autoAdjustQuality.asStateFlow()

    private val _downloadOverCellular = MutableStateFlow(false)
    val downloadOverCellular: StateFlow<Boolean> = _downloadOverCellular.asStateFlow()

    private val _audioNormalization = MutableStateFlow(true)
    val audioNormalization: StateFlow<Boolean> = _audioNormalization.asStateFlow()

    private val _crossfadeSeconds = MutableStateFlow(3.0f)
    val crossfadeSeconds: StateFlow<Float> = _crossfadeSeconds.asStateFlow()

    private val _automixEnabled = MutableStateFlow(true)
    val automixEnabled: StateFlow<Boolean> = _automixEnabled.asStateFlow()

    private val _gaplessEnabled = MutableStateFlow(true)
    val gaplessEnabled: StateFlow<Boolean> = _gaplessEnabled.asStateFlow()

    private val _monoAudio = MutableStateFlow(false)
    val monoAudio: StateFlow<Boolean> = _monoAudio.asStateFlow()

    private val _autoplayEnabled = MutableStateFlow(true)
    val autoplayEnabled: StateFlow<Boolean> = _autoplayEnabled.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _allowExplicit = MutableStateFlow(true)
    val allowExplicit: StateFlow<Boolean> = _allowExplicit.asStateFlow()

    private val _showUnplayable = MutableStateFlow(false)
    val showUnplayable: StateFlow<Boolean> = _showUnplayable.asStateFlow()

    private val _canvasEnabled = MutableStateFlow(true)
    val canvasEnabled: StateFlow<Boolean> = _canvasEnabled.asStateFlow()

    private val _privateSession = MutableStateFlow(false)
    val privateSession: StateFlow<Boolean> = _privateSession.asStateFlow()

    private val _listeningActivity = MutableStateFlow(true)
    val listeningActivity: StateFlow<Boolean> = _listeningActivity.asStateFlow()

    private val _showTopArtists = MutableStateFlow(true)
    val showTopArtists: StateFlow<Boolean> = _showTopArtists.asStateFlow()

    private val _cachedSizeBytes = MutableStateFlow(0L)
    val cachedSizeBytes: StateFlow<Long> = _cachedSizeBytes.asStateFlow()

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            loadPreferences()
            refreshCacheSize(context)
        }
    }

    private fun loadPreferences() {
        prefs?.let { p ->
            _streamingQuality.value = p.getString(KEY_STREAMING_QUALITY, "Very High (320 kbps)") ?: "Very High (320 kbps)"
            _downloadQuality.value = p.getString(KEY_DOWNLOAD_QUALITY, "Very High (320 kbps)") ?: "Very High (320 kbps)"
            _autoAdjustQuality.value = p.getBoolean(KEY_AUTO_ADJUST_QUALITY, true)
            _downloadOverCellular.value = p.getBoolean(KEY_DOWNLOAD_OVER_CELLULAR, false)
            _audioNormalization.value = p.getBoolean(KEY_AUDIO_NORMALIZATION, true)
            _crossfadeSeconds.value = p.getFloat(KEY_CROSSFADE_SECONDS, 3.0f)
            _automixEnabled.value = p.getBoolean(KEY_AUTOMIX, true)
            _gaplessEnabled.value = p.getBoolean(KEY_GAPLESS, true)
            _monoAudio.value = p.getBoolean(KEY_MONO_AUDIO, false)
            _autoplayEnabled.value = p.getBoolean(KEY_AUTOPLAY, true)
            _playbackSpeed.value = p.getFloat(KEY_PLAYBACK_SPEED, 1.0f)
            _allowExplicit.value = p.getBoolean(KEY_ALLOW_EXPLICIT, true)
            _showUnplayable.value = p.getBoolean(KEY_SHOW_UNPLAYABLE, false)
            _canvasEnabled.value = p.getBoolean(KEY_CANVAS, true)
            _privateSession.value = p.getBoolean(KEY_PRIVATE_SESSION, false)
            _listeningActivity.value = p.getBoolean(KEY_LISTENING_ACTIVITY, true)
            _showTopArtists.value = p.getBoolean(KEY_SHOW_TOP_ARTISTS, true)
        }
    }

    fun setStreamingQuality(value: String) {
        _streamingQuality.value = value
        prefs?.edit()?.putString(KEY_STREAMING_QUALITY, value)?.apply()
    }

    fun setDownloadQuality(value: String) {
        _downloadQuality.value = value
        prefs?.edit()?.putString(KEY_DOWNLOAD_QUALITY, value)?.apply()
    }

    fun setAutoAdjustQuality(value: Boolean) {
        _autoAdjustQuality.value = value
        prefs?.edit()?.putBoolean(KEY_AUTO_ADJUST_QUALITY, value)?.apply()
    }

    fun setDownloadOverCellular(value: Boolean) {
        _downloadOverCellular.value = value
        prefs?.edit()?.putBoolean(KEY_DOWNLOAD_OVER_CELLULAR, value)?.apply()
    }

    fun setAudioNormalization(value: Boolean) {
        _audioNormalization.value = value
        prefs?.edit()?.putBoolean(KEY_AUDIO_NORMALIZATION, value)?.apply()
    }

    fun setCrossfadeSeconds(value: Float) {
        _crossfadeSeconds.value = value
        prefs?.edit()?.putFloat(KEY_CROSSFADE_SECONDS, value)?.apply()
    }

    fun setAutomixEnabled(value: Boolean) {
        _automixEnabled.value = value
        prefs?.edit()?.putBoolean(KEY_AUTOMIX, value)?.apply()
    }

    fun setGaplessEnabled(value: Boolean) {
        _gaplessEnabled.value = value
        prefs?.edit()?.putBoolean(KEY_GAPLESS, value)?.apply()
    }

    fun setMonoAudio(value: Boolean) {
        _monoAudio.value = value
        prefs?.edit()?.putBoolean(KEY_MONO_AUDIO, value)?.apply()
    }

    fun setAutoplayEnabled(value: Boolean) {
        _autoplayEnabled.value = value
        prefs?.edit()?.putBoolean(KEY_AUTOPLAY, value)?.apply()
    }

    fun setPlaybackSpeed(value: Float) {
        _playbackSpeed.value = value
        prefs?.edit()?.putFloat(KEY_PLAYBACK_SPEED, value)?.apply()
    }

    fun setAllowExplicit(value: Boolean) {
        _allowExplicit.value = value
        prefs?.edit()?.putBoolean(KEY_ALLOW_EXPLICIT, value)?.apply()
    }

    fun setShowUnplayable(value: Boolean) {
        _showUnplayable.value = value
        prefs?.edit()?.putBoolean(KEY_SHOW_UNPLAYABLE, value)?.apply()
    }

    fun setCanvasEnabled(value: Boolean) {
        _canvasEnabled.value = value
        prefs?.edit()?.putBoolean(KEY_CANVAS, value)?.apply()
    }

    fun setPrivateSession(value: Boolean) {
        _privateSession.value = value
        prefs?.edit()?.putBoolean(KEY_PRIVATE_SESSION, value)?.apply()
    }

    fun setListeningActivity(value: Boolean) {
        _listeningActivity.value = value
        prefs?.edit()?.putBoolean(KEY_LISTENING_ACTIVITY, value)?.apply()
    }

    fun setShowTopArtists(value: Boolean) {
        _showTopArtists.value = value
        prefs?.edit()?.putBoolean(KEY_SHOW_TOP_ARTISTS, value)?.apply()
    }

    fun refreshCacheSize(context: Context) {
        val size = AudioCacheManager.getCacheSizeBytes(context)
        _cachedSizeBytes.value = size
    }

    fun clearCache(context: Context) {
        AudioCacheManager.clearCache(context)
        _cachedSizeBytes.value = 0L
    }
}
