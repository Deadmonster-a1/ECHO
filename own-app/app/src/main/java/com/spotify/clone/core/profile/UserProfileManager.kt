package com.spotify.clone.core.profile

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Reactive User Profile Manager for ECHO
 * Stores and emits real-time profile updates (display name, bio, avatar)
 * backed by persistent SharedPreferences.
 */
object UserProfileManager {
    private const val PREFS_NAME = "echo_user_profile"
    private const val KEY_USER_NAME = "profile_user_name"
    private const val KEY_USER_BIO = "profile_user_bio"
    private const val KEY_AVATAR_URL = "profile_avatar_url"

    private const val DEFAULT_NAME = "Theja"
    private const val DEFAULT_FALLBACK = "Music Lover"
    private const val DEFAULT_BIO = "Audiophile • Organic Sound & Studio Fidelity"

    private var prefs: SharedPreferences? = null

    private val _userName = MutableStateFlow(DEFAULT_NAME)
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _userBio = MutableStateFlow(DEFAULT_BIO)
    val userBio: StateFlow<String> = _userBio.asStateFlow()

    private val _avatarUrl = MutableStateFlow<String?>(null)
    val avatarUrl: StateFlow<String?> = _avatarUrl.asStateFlow()

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val savedName = prefs?.getString(KEY_USER_NAME, DEFAULT_NAME) ?: DEFAULT_NAME
            val savedBio = prefs?.getString(KEY_USER_BIO, DEFAULT_BIO) ?: DEFAULT_BIO
            val savedAvatar = prefs?.getString(KEY_AVATAR_URL, null)

            _userName.value = if (savedName.isNotBlank()) savedName else DEFAULT_FALLBACK
            _userBio.value = savedBio
            _avatarUrl.value = savedAvatar
        }
    }

    fun updateProfile(name: String, bio: String) {
        val resolvedName = if (name.isNotBlank()) name.trim() else DEFAULT_FALLBACK
        _userName.value = resolvedName
        _userBio.value = bio.trim()

        prefs?.edit()?.apply {
            putString(KEY_USER_NAME, resolvedName)
            putString(KEY_USER_BIO, bio.trim())
            apply()
        }
    }

    fun updateAvatar(url: String?) {
        _avatarUrl.value = url
        prefs?.edit()?.apply {
            if (url != null) putString(KEY_AVATAR_URL, url) else remove(KEY_AVATAR_URL)
            apply()
        }
    }

    fun getDisplayName(): String {
        return _userName.value.ifBlank { DEFAULT_FALLBACK }
    }
}
