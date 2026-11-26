package com.spotify.clone.core.settings

import android.content.Context
import android.content.SharedPreferences
import com.spotify.clone.core.model.MusicGenre
import com.spotify.clone.core.player.AudioCacheManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Persisted audio, playback, and same-genre radio preferences for ECHO.
 */
object SettingsManager {
    private const val PREFS_NAME = "echo_app_settings"

    private const val KEY_AUDIO_NORMALIZATION = "audio_normalization"
    private const val KEY_PLAYBACK_SPEED = "playback_speed"
    private const val KEY_SMART_RADIO = "smart_radio_enabled"
    private const val KEY_GENRE_LOCK = "genre_lock_id"
    private const val KEY_AUTO_MATCH_GENRE = "auto_match_genre"
    private const val KEY_CROSSFADE_SECONDS = "crossfade_seconds"
    private const val KEY_AUTO_GENRE_EQ = "auto_genre_eq"
    private const val KEY_LYRICS_SIZE_TIER = "lyrics_size_tier"
    private const val KEY_LYRICS_OFFSET_MS = "lyrics_offset_ms"
    private const val KEY_LYRICS_USE_ROMANIZED = "lyrics_use_romanized"

    private var prefs: SharedPreferences? = null

    private val _audioNormalization = MutableStateFlow(true)
    val audioNormalization: StateFlow<Boolean> = _audioNormalization.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _smartRadioEnabled = MutableStateFlow(true)
    val smartRadioEnabled: StateFlow<Boolean> = _smartRadioEnabled.asStateFlow()

    private val _selectedGenreLock = MutableStateFlow(MusicGenre.ALL)
    val selectedGenreLock: StateFlow<MusicGenre> = _selectedGenreLock.asStateFlow()

    private val _autoMatchGenre = MutableStateFlow(true)
    val autoMatchGenre: StateFlow<Boolean> = _autoMatchGenre.asStateFlow()

    private val _crossfadeSeconds = MutableStateFlow(0)
    val crossfadeSeconds: StateFlow<Int> = _crossfadeSeconds.asStateFlow()

    private val _autoGenreEq = MutableStateFlow(false)
    val autoGenreEq: StateFlow<Boolean> = _autoGenreEq.asStateFlow()

    private val _cachedSizeBytes = MutableStateFlow(0L)
    val cachedSizeBytes: StateFlow<Long> = _cachedSizeBytes.asStateFlow()

    private val _eqUpdateSignal = MutableStateFlow(0L)
    val eqUpdateSignal: StateFlow<Long> = _eqUpdateSignal.asStateFlow()

    private val _lyricsSizeTier = MutableStateFlow("STANDARD")
    val lyricsSizeTier: StateFlow<String> = _lyricsSizeTier.asStateFlow()

    private val _lyricsOffsetMs = MutableStateFlow(0L)
    val lyricsOffsetMs: StateFlow<Long> = _lyricsOffsetMs.asStateFlow()

    private val _lyricsUseRomanized = MutableStateFlow(true)
    val lyricsUseRomanized: StateFlow<Boolean> = _lyricsUseRomanized.asStateFlow()

    private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            loadPreferences()
            refreshCacheSize(context)
        }
    }

    private fun loadPreferences() {
        prefs?.let { p ->
            _audioNormalization.value = p.getBoolean(KEY_AUDIO_NORMALIZATION, true)
            _playbackSpeed.value = p.getFloat(KEY_PLAYBACK_SPEED, 1.0f).coerceIn(0.5f, 2.0f)
            _smartRadioEnabled.value = p.getBoolean(KEY_SMART_RADIO, true)
            _selectedGenreLock.value = MusicGenre.fromId(p.getString(KEY_GENRE_LOCK, MusicGenre.ALL.id))
            _autoMatchGenre.value = p.getBoolean(KEY_AUTO_MATCH_GENRE, true)
            _crossfadeSeconds.value = p.getInt(KEY_CROSSFADE_SECONDS, 0).coerceIn(0, 12)
            _autoGenreEq.value = p.getBoolean(KEY_AUTO_GENRE_EQ, false)
            _lyricsSizeTier.value = p.getString(KEY_LYRICS_SIZE_TIER, "STANDARD") ?: "STANDARD"
            _lyricsOffsetMs.value = p.getLong(KEY_LYRICS_OFFSET_MS, 0L).coerceIn(-10_000L, 10_000L)
            _lyricsUseRomanized.value = p.getBoolean(KEY_LYRICS_USE_ROMANIZED, true)
        }
    }

    fun setAudioNormalization(value: Boolean) {
        _audioNormalization.value = value
        prefs?.edit()?.putBoolean(KEY_AUDIO_NORMALIZATION, value)?.apply()
    }

    fun setPlaybackSpeed(value: Float) {
        val clamped = value.coerceIn(0.5f, 2.0f)
        _playbackSpeed.value = clamped
        prefs?.edit()?.putFloat(KEY_PLAYBACK_SPEED, clamped)?.apply()
    }

    fun setSmartRadioEnabled(value: Boolean) {
        _smartRadioEnabled.value = value
        prefs?.edit()?.putBoolean(KEY_SMART_RADIO, value)?.apply()
    }

    fun setGenreLock(genre: MusicGenre) {
        _selectedGenreLock.value = genre
        prefs?.edit()?.putString(KEY_GENRE_LOCK, genre.id)?.apply()
    }

    fun setAutoMatchGenre(enabled: Boolean) {
        _autoMatchGenre.value = enabled
        prefs?.edit()?.putBoolean(KEY_AUTO_MATCH_GENRE, enabled)?.apply()
    }

    fun setCrossfadeSeconds(seconds: Int) {
        val clamped = seconds.coerceIn(0, 12)
        _crossfadeSeconds.value = clamped
        prefs?.edit()?.putInt(KEY_CROSSFADE_SECONDS, clamped)?.apply()
    }

    fun setAutoGenreEq(enabled: Boolean) {
        _autoGenreEq.value = enabled
        prefs?.edit()?.putBoolean(KEY_AUTO_GENRE_EQ, enabled)?.apply()
    }

    fun setLyricsSizeTier(tierName: String) {
        _lyricsSizeTier.value = tierName
        prefs?.edit()?.putString(KEY_LYRICS_SIZE_TIER, tierName)?.apply()
    }

    fun setLyricsOffsetMs(offsetMs: Long) {
        val clamped = offsetMs.coerceIn(-10_000L, 10_000L)
        _lyricsOffsetMs.value = clamped
        prefs?.edit()?.putLong(KEY_LYRICS_OFFSET_MS, clamped)?.apply()
    }

    fun setLyricsUseRomanized(useRomanized: Boolean) {
        _lyricsUseRomanized.value = useRomanized
        prefs?.edit()?.putBoolean(KEY_LYRICS_USE_ROMANIZED, useRomanized)?.apply()
    }

    fun notifyEqualizerChanged() {
        _eqUpdateSignal.value = System.currentTimeMillis()
    }

    fun refreshCacheSize(context: Context) {
        val appContext = context.applicationContext
        ioScope.launch {
            _cachedSizeBytes.value = AudioCacheManager.getCacheSizeBytes(appContext)
        }
    }

    fun clearCache(context: Context) {
        val appContext = context.applicationContext
        ioScope.launch {
            AudioCacheManager.clearCache(appContext)
            _cachedSizeBytes.value = 0L
        }
    }

    fun resetDefaults() {
        setAudioNormalization(true)
        setPlaybackSpeed(1.0f)
        setSmartRadioEnabled(true)
        setGenreLock(MusicGenre.ALL)
        setAutoMatchGenre(true)
        setCrossfadeSeconds(0)
        setAutoGenreEq(false)
        setLyricsSizeTier("STANDARD")
        setLyricsOffsetMs(0L)
        setLyricsUseRomanized(true)
    }
}
