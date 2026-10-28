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
    private const val KEY_NAME_IS_SET = "profile_name_is_set"

    private const val DEFAULT_FALLBACK = "Listener"
    private const val DEFAULT_BIO = "Audiophile • Organic Sound & Studio Fidelity"

    private var prefs: SharedPreferences? = null

    private val _userName = MutableStateFlow("")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _userBio = MutableStateFlow(DEFAULT_BIO)
    val userBio: StateFlow<String> = _userBio.asStateFlow()

    private val _avatarUrl = MutableStateFlow<String?>(null)
    val avatarUrl: StateFlow<String?> = _avatarUrl.asStateFlow()

    private val _isNamePromptNeeded = MutableStateFlow(false)
    val isNamePromptNeeded: StateFlow<Boolean> = _isNamePromptNeeded.asStateFlow()

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val isExplicitlySet = prefs?.getBoolean(KEY_NAME_IS_SET, false) ?: false
            val savedName = prefs?.getString(KEY_USER_NAME, "") ?: ""
            val savedBio = prefs?.getString(KEY_USER_BIO, DEFAULT_BIO) ?: DEFAULT_BIO
            val savedAvatar = prefs?.getString(KEY_AVATAR_URL, null)

            // If name was legacy "Theja" or blank without being explicitly set, ask for user name
            val isClean = isExplicitlySet && savedName.isNotBlank() && !savedName.equals("Theja", ignoreCase = true)
            if (isClean) {
                _userName.value = savedName
                _isNamePromptNeeded.value = false
            } else {
                _userName.value = ""
                _isNamePromptNeeded.value = true
            }

            _userBio.value = savedBio
            _avatarUrl.value = savedAvatar
        }
    }

    fun setUserName(name: String, bio: String = _userBio.value) {
        val trimmed = name.trim()
        val finalName = if (trimmed.isNotBlank()) trimmed else DEFAULT_FALLBACK
        _userName.value = finalName
        _userBio.value = bio.trim()
        _isNamePromptNeeded.value = false

        prefs?.edit()?.apply {
            putString(KEY_USER_NAME, finalName)
            putString(KEY_USER_BIO, bio.trim())
            putBoolean(KEY_NAME_IS_SET, true)
            apply()
        }
    }

    fun updateProfile(name: String, bio: String) {
        setUserName(name, bio)
    }

    fun dismissNamePrompt() {
        _isNamePromptNeeded.value = false
        if (_userName.value.isBlank()) {
            _userName.value = DEFAULT_FALLBACK
        }
        val currentSaved = prefs?.getString(KEY_USER_NAME, "") ?: ""
        prefs?.edit()?.apply {
            putBoolean(KEY_NAME_IS_SET, true)
            if (currentSaved.isBlank()) {
                putString(KEY_USER_NAME, DEFAULT_FALLBACK)
            }
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
