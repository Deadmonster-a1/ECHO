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
    private const val KEY_HAS_SEEN_NAME_PROMPT = "profile_has_seen_name_prompt"

    private const val DEFAULT_FALLBACK = "Listener"
    private const val DEFAULT_BIO = "Audiophile • Organic Sound & Studio Fidelity"

    private var prefs: SharedPreferences? = null

    private val _userName = MutableStateFlow(DEFAULT_FALLBACK)
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _userBio = MutableStateFlow(DEFAULT_BIO)
    val userBio: StateFlow<String> = _userBio.asStateFlow()

    private val _avatarUrl = MutableStateFlow<String?>(null)
    val avatarUrl: StateFlow<String?> = _avatarUrl.asStateFlow()

    private val _isNamePromptNeeded = MutableStateFlow(false)
    val isNamePromptNeeded: StateFlow<Boolean> = _isNamePromptNeeded.asStateFlow()

    fun init(context: Context) {
        if (prefs == null) {
            val p = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs = p
            val hasSeenPrompt = p.getBoolean(KEY_HAS_SEEN_NAME_PROMPT, false)
            val isExplicitlySet = p.getBoolean(KEY_NAME_IS_SET, false)
            val savedName = p.getString(KEY_USER_NAME, "") ?: ""
            val savedBio = p.getString(KEY_USER_BIO, DEFAULT_BIO) ?: DEFAULT_BIO
            val savedAvatar = p.getString(KEY_AVATAR_URL, null)

            // If prompt was already seen, or if user already has a valid name, NEVER prompt again!
            val hasValidSavedName = savedName.isNotBlank() && !savedName.equals("Theja", ignoreCase = true)
            if (hasSeenPrompt || isExplicitlySet || hasValidSavedName) {
                _userName.value = if (hasValidSavedName) savedName else DEFAULT_FALLBACK
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
            putBoolean(KEY_HAS_SEEN_NAME_PROMPT, true)
            putBoolean(KEY_NAME_IS_SET, true)
            commit()
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
            putBoolean(KEY_HAS_SEEN_NAME_PROMPT, true)
            putBoolean(KEY_NAME_IS_SET, true)
            if (currentSaved.isBlank() || currentSaved.equals("Theja", ignoreCase = true)) {
                putString(KEY_USER_NAME, DEFAULT_FALLBACK)
            }
            commit()
        }
    }

    fun updateAvatar(url: String?) {
        _avatarUrl.value = url
        prefs?.edit()?.apply {
            if (url != null) putString(KEY_AVATAR_URL, url) else remove(KEY_AVATAR_URL)
            commit()
        }
    }

    fun getDisplayName(): String {
        return _userName.value.ifBlank { DEFAULT_FALLBACK }
    }
}
