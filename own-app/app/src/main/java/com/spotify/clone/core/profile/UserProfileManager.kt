package com.spotify.clone.core.profile

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class PrismAvatarOption(
    val id: String,
    val label: String,
    val startColor: Color,
    val endColor: Color
) {
    ROSE("prism_rose", "Rose", Color(0xFFFF8FA3), Color(0xFFFFB3C1)),
    LAVENDER("prism_lavender", "Lavender", Color(0xFFC49BFF), Color(0xFFE0C8FF)),
    SKY("prism_sky", "Sky", Color(0xFF8FB8FF), Color(0xFFBBD4FF)),
    AQUA("prism_aqua", "Aqua", Color(0xFF6FE3E8), Color(0xFFA7F3F5)),
    PRISM("prism_aurora", "Prism", Color(0xFFFF8FA3), Color(0xFF6FE3E8)),
    TWILIGHT("prism_twilight", "Twilight", Color(0xFF8C72F5), Color(0xFF8FB8FF));

    companion object {
        fun fromId(id: String?): PrismAvatarOption =
            entries.find { it.id == id } ?: PRISM
    }
}

object UserProfileManager {
    private const val PREFS_NAME = "echo_user_profile"
    private const val KEY_USER_NAME = "profile_user_name"
    private const val KEY_AVATAR_ID = "profile_avatar_id"
    private const val KEY_ONBOARDING_DONE = "onboarding_completed"
    private const val KEY_LANGUAGES = "preferred_languages"
    private const val KEY_GENRES = "preferred_genres"
    private const val KEY_ARTISTS = "favorite_artists"
    private const val KEY_SONGS = "favorite_songs"
    private const val DEFAULT_FALLBACK = "Listener"

    private var prefs: SharedPreferences? = null

    private val _userName = MutableStateFlow(DEFAULT_FALLBACK)
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _avatarId = MutableStateFlow(PrismAvatarOption.PRISM.id)
    val avatarId: StateFlow<String> = _avatarId.asStateFlow()

    private val _hasCompletedOnboarding = MutableStateFlow(false)
    val hasCompletedOnboarding: StateFlow<Boolean> = _hasCompletedOnboarding.asStateFlow()

    private val _preferredLanguages = MutableStateFlow<Set<String>>(emptySet())
    val preferredLanguages: StateFlow<Set<String>> = _preferredLanguages.asStateFlow()

    private val _preferredGenres = MutableStateFlow<Set<String>>(emptySet())
    val preferredGenres: StateFlow<Set<String>> = _preferredGenres.asStateFlow()

    private val _favoriteArtists = MutableStateFlow<Set<String>>(emptySet())
    val favoriteArtists: StateFlow<Set<String>> = _favoriteArtists.asStateFlow()

    private val _favoriteSongIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteSongIds: StateFlow<Set<String>> = _favoriteSongIds.asStateFlow()

    fun init(context: Context) {
        if (prefs == null) {
            val p = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs = p
            val savedName = p.getString(KEY_USER_NAME, "")?.trim().orEmpty()
            _userName.value = savedName.ifBlank { DEFAULT_FALLBACK }
            _avatarId.value = p.getString(KEY_AVATAR_ID, PrismAvatarOption.PRISM.id) ?: PrismAvatarOption.PRISM.id
            _hasCompletedOnboarding.value = p.getBoolean(KEY_ONBOARDING_DONE, false)
            _preferredLanguages.value = p.getStringSet(KEY_LANGUAGES, emptySet())?.toSet() ?: emptySet()
            _preferredGenres.value = p.getStringSet(KEY_GENRES, emptySet())?.toSet() ?: emptySet()
            _favoriteArtists.value = p.getStringSet(KEY_ARTISTS, emptySet())?.toSet() ?: emptySet()
            _favoriteSongIds.value = p.getStringSet(KEY_SONGS, emptySet())?.toSet() ?: emptySet()
        }
    }

    fun setUserName(name: String) {
        val finalName = name.trim().ifBlank { DEFAULT_FALLBACK }
        _userName.value = finalName
        prefs?.edit()?.putString(KEY_USER_NAME, finalName)?.apply()
    }

    fun setAvatarId(id: String) {
        _avatarId.value = id
        prefs?.edit()?.putString(KEY_AVATAR_ID, id)?.apply()
    }

    fun completeOnboarding(
        name: String,
        avatarId: String,
        languages: Set<String>,
        genreIds: Set<String>,
        artists: Set<String>,
        songs: Set<String> = emptySet()
    ) {
        val finalName = name.trim().ifBlank { DEFAULT_FALLBACK }
        val cleanLangs = languages.map { it.trim() }.filter { it.isNotBlank() }.toSet()
        val cleanGenres = genreIds.map { it.trim() }.filter { it.isNotBlank() }.toSet()
        val cleanArtists = artists.map { it.trim() }.filter { it.isNotBlank() }.toSet()
        val cleanSongs = songs.map { it.trim() }.filter { it.isNotBlank() }.toSet()

        _userName.value = finalName
        _avatarId.value = avatarId
        _preferredLanguages.value = cleanLangs
        _preferredGenres.value = cleanGenres
        _favoriteArtists.value = cleanArtists
        _favoriteSongIds.value = cleanSongs
        _hasCompletedOnboarding.value = true

        prefs?.edit()
            ?.putString(KEY_USER_NAME, finalName)
            ?.putString(KEY_AVATAR_ID, avatarId)
            ?.putBoolean(KEY_ONBOARDING_DONE, true)
            ?.putStringSet(KEY_LANGUAGES, cleanLangs)
            ?.putStringSet(KEY_GENRES, cleanGenres)
            ?.putStringSet(KEY_ARTISTS, cleanArtists)
            ?.putStringSet(KEY_SONGS, cleanSongs)
            ?.apply()
    }

    fun resetOnboarding() {
        _hasCompletedOnboarding.value = false
        prefs?.edit()?.putBoolean(KEY_ONBOARDING_DONE, false)?.apply()
    }

    fun isFavoriteArtist(artistName: String): Boolean {
        val clean = artistName.trim()
        if (clean.isBlank()) return false
        return _favoriteArtists.value.any { it.equals(clean, ignoreCase = true) }
    }

    fun toggleFavoriteArtist(artistName: String): Boolean {
        val clean = artistName.trim()
        if (clean.isBlank()) return false
        val current = _favoriteArtists.value
        val existing = current.firstOrNull { it.equals(clean, ignoreCase = true) }
        val updated = if (existing != null) current - existing else current + clean
        _favoriteArtists.value = updated
        prefs?.edit()?.putStringSet(KEY_ARTISTS, updated)?.apply()
        return existing == null
    }

    fun getDisplayName(): String = _userName.value.ifBlank { DEFAULT_FALLBACK }
}
