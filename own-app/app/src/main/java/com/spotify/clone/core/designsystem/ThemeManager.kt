package com.spotify.clone.core.designsystem

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppThemeOption(
    val id: String,
    val displayName: String,
    val subtitle: String,
    val primaryColor: Color,
    val primaryPressed: Color,
    val darkBackgroundColor: Color,
    val darkSurfaceColor: Color,
    val darkCardColor: Color,
    val lightBackgroundColor: Color,
    val lightSurfaceColor: Color,
    val lightCardColor: Color,
    val gradientStart: Color,
    val gradientEnd: Color
) {
    PASTEL_PRISM(
        id = "pastel_prism",
        displayName = "Pastel Prism",
        subtitle = "Signature ECHO rose, lavender, sky & aqua palette",
        primaryColor = Color(0xFF7B6EF6),
        primaryPressed = Color(0xFF6254E8),
        darkBackgroundColor = Color(0xFF09090E),
        darkSurfaceColor = Color(0xFF12121A),
        darkCardColor = Color(0xFF1A1A25),
        lightBackgroundColor = Color(0xFFFBFBFD),
        lightSurfaceColor = Color(0xFFFFFFFF),
        lightCardColor = Color(0xFFFFFFFF),
        gradientStart = Color(0xFFFF8FA3),
        gradientEnd = Color(0xFF6FE3E8)
    )
}

val LocalAppTheme = staticCompositionLocalOf { AppThemeOption.PASTEL_PRISM }

object ThemeManager {
    private const val PREFS_NAME = "echo_theme_prefs"
    private const val KEY_DARK_MODE = "is_dark_mode"
    private const val KEY_REDUCED_MOTION = "reduced_motion"

    private var prefs: SharedPreferences? = null

    private val _currentTheme = MutableStateFlow(AppThemeOption.PASTEL_PRISM)
    val currentTheme: StateFlow<AppThemeOption> = _currentTheme.asStateFlow()

    // Airy Daylight default matching the white & light-pastel ECHO logo
    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _reducedMotion = MutableStateFlow(false)
    val reducedMotion: StateFlow<Boolean> = _reducedMotion.asStateFlow()

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            _currentTheme.value = AppThemeOption.PASTEL_PRISM
            _isDarkMode.value = prefs?.getBoolean(KEY_DARK_MODE, false) ?: false
            _reducedMotion.value = prefs?.getBoolean(KEY_REDUCED_MOTION, false) ?: false
        }
    }

    fun setDarkMode(dark: Boolean) {
        _isDarkMode.value = dark
        prefs?.edit()?.putBoolean(KEY_DARK_MODE, dark)?.apply()
    }

    fun toggleDarkMode() {
        setDarkMode(!_isDarkMode.value)
    }

    fun setReducedMotion(enabled: Boolean) {
        _reducedMotion.value = enabled
        prefs?.edit()?.putBoolean(KEY_REDUCED_MOTION, enabled)?.apply()
    }
}
