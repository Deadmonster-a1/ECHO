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
    ECHO_LAVENDER(
        id = "echo_lavender",
        displayName = "ECHO Lavender",
        subtitle = "Signature atmospheric lavender & obsidian",
        primaryColor = Color(0xFFA79BFF),
        primaryPressed = Color(0xFF9183F2),
        darkBackgroundColor = Color(0xFF101016),
        darkSurfaceColor = Color(0xFF171720),
        darkCardColor = Color(0xFF1B1A25),
        lightBackgroundColor = Color(0xFFF5F2EE),
        lightSurfaceColor = Color(0xFFFFFFFF),
        lightCardColor = Color(0xFFFFFFFF),
        gradientStart = Color(0xFFA79BFF),
        gradientEnd = Color(0xFF6B58E6)
    ),
    ECHO_CORAL(
        id = "echo_coral",
        displayName = "ECHO Coral",
        subtitle = "Tactile warm coral & ambient glow",
        primaryColor = Color(0xFFFF9C88),
        primaryPressed = Color(0xFFEE846E),
        darkBackgroundColor = Color(0xFF120E11),
        darkSurfaceColor = Color(0xFF1D161A),
        darkCardColor = Color(0xFF281E24),
        lightBackgroundColor = Color(0xFFF7F2F0),
        lightSurfaceColor = Color(0xFFEFE7E4),
        lightCardColor = Color(0xFFE4D9D5),
        gradientStart = Color(0xFFFF9C88),
        gradientEnd = Color(0xFFE0533C)
    ),
    ECHO_SOFT_BLUE(
        id = "echo_soft_blue",
        displayName = "ECHO Soft Blue",
        subtitle = "Minimal celestial blue & twilight depth",
        primaryColor = Color(0xFFA8C9FF),
        primaryPressed = Color(0xFF8FB4F0),
        darkBackgroundColor = Color(0xFF0F1218),
        darkSurfaceColor = Color(0xFF161C26),
        darkCardColor = Color(0xFF1E2533),
        lightBackgroundColor = Color(0xFFF1F4F8),
        lightSurfaceColor = Color(0xFFE6ECF3),
        lightCardColor = Color(0xFFD9E2ED),
        gradientStart = Color(0xFFA8C9FF),
        gradientEnd = Color(0xFF4A7FD6)
    ),
    ECHO_MINT(
        id = "echo_mint",
        displayName = "ECHO Mint",
        subtitle = "Organic calm mint & mineral earth",
        primaryColor = Color(0xFF8AE0C8),
        primaryPressed = Color(0xFF72C7B0),
        darkBackgroundColor = Color(0xFF0F1413),
        darkSurfaceColor = Color(0xFF161F1D),
        darkCardColor = Color(0xFF1D2A27),
        lightBackgroundColor = Color(0xFFF1F6F4),
        lightSurfaceColor = Color(0xFFE5EEEB),
        lightCardColor = Color(0xFFD6E3DF),
        gradientStart = Color(0xFF8AE0C8),
        gradientEnd = Color(0xFF38967E)
    ),
    SPOTIFY_GREEN(
        id = "spotify_green",
        displayName = "Spotify Classic",
        subtitle = "Electric neon green & pitch dark",
        primaryColor = Color(0xFF1DB954),
        primaryPressed = Color(0xFF1AA34A),
        darkBackgroundColor = Color(0xFF121212),
        darkSurfaceColor = Color(0xFF181818),
        darkCardColor = Color(0xFF242424),
        lightBackgroundColor = Color(0xFFF5F5F5),
        lightSurfaceColor = Color(0xFFEEEEEE),
        lightCardColor = Color(0xFFE0E0E0),
        gradientStart = Color(0xFF1DB954),
        gradientEnd = Color(0xFF0D5E29)
    ),
    MIDNIGHT_BLUE(
        id = "midnight_blue",
        displayName = "Midnight Sapphire",
        subtitle = "Deep space sapphire & cobalt glow",
        primaryColor = Color(0xFF3B82F6),
        primaryPressed = Color(0xFF2563EB),
        darkBackgroundColor = Color(0xFF0A101D),
        darkSurfaceColor = Color(0xFF101B31),
        darkCardColor = Color(0xFF18294B),
        lightBackgroundColor = Color(0xFFF0F4FA),
        lightSurfaceColor = Color(0xFFE4ECF6),
        lightCardColor = Color(0xFFD7E2F0),
        gradientStart = Color(0xFF3B82F6),
        gradientEnd = Color(0xFF1D4ED8)
    ),
    ROYAL_AMETHYST(
        id = "royal_amethyst",
        displayName = "Royal Amethyst",
        subtitle = "Synthwave violet & nocturnal plum",
        primaryColor = Color(0xFFA855F7),
        primaryPressed = Color(0xFF9333EA),
        darkBackgroundColor = Color(0xFF120C1C),
        darkSurfaceColor = Color(0xFF1C122D),
        darkCardColor = Color(0xFF281A40),
        lightBackgroundColor = Color(0xFFF6F2F8),
        lightSurfaceColor = Color(0xFFECE4F1),
        lightCardColor = Color(0xFFDFD4E7),
        gradientStart = Color(0xFFA855F7),
        gradientEnd = Color(0xFF6B21A8)
    ),
    SUNSET_AMBER(
        id = "sunset_amber",
        displayName = "Sunset Amber",
        subtitle = "Warm citrus gold & twilight blaze",
        primaryColor = Color(0xFFFF7A00),
        primaryPressed = Color(0xFFE06B00),
        darkBackgroundColor = Color(0xFF170E08),
        darkSurfaceColor = Color(0xFF24160D),
        darkCardColor = Color(0xFF332013),
        lightBackgroundColor = Color(0xFFFBF4ED),
        lightSurfaceColor = Color(0xFFF3E8DD),
        lightCardColor = Color(0xFFEADBCB),
        gradientStart = Color(0xFFFF7A00),
        gradientEnd = Color(0xFFB45309)
    );

    // Dynamic resolution based on dark/light mode
    val backgroundColor: Color get() = if (ThemeManager.isDarkMode.value) darkBackgroundColor else lightBackgroundColor
    val surfaceColor: Color get() = if (ThemeManager.isDarkMode.value) darkSurfaceColor else lightSurfaceColor
    val cardColor: Color get() = if (ThemeManager.isDarkMode.value) darkCardColor else lightCardColor

    companion object {
        fun fromId(id: String): AppThemeOption {
            return entries.firstOrNull { it.id == id } ?: ECHO_LAVENDER
        }
    }
}

val LocalAppTheme = staticCompositionLocalOf { AppThemeOption.ECHO_LAVENDER }

object ThemeManager {
    private const val PREFS_NAME = "echo_theme_prefs"
    private const val KEY_THEME = "selected_theme_id"
    private const val KEY_DARK_MODE = "is_dark_mode"
    private const val KEY_REDUCED_MOTION = "is_reduced_motion"

    private var prefs: SharedPreferences? = null

    private val _currentTheme = MutableStateFlow(AppThemeOption.ECHO_LAVENDER)
    val currentTheme: StateFlow<AppThemeOption> = _currentTheme.asStateFlow()

    val isDarkMode = MutableStateFlow(true)
    val reducedMotion = MutableStateFlow(false)

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val savedId = prefs?.getString(KEY_THEME, AppThemeOption.ECHO_LAVENDER.id) ?: AppThemeOption.ECHO_LAVENDER.id
            val resolvedTheme = if (savedId == "spotify_green") AppThemeOption.ECHO_LAVENDER else AppThemeOption.fromId(savedId)
            _currentTheme.value = resolvedTheme
            val savedDarkMode = prefs?.getBoolean(KEY_DARK_MODE, true) ?: true
            isDarkMode.value = savedDarkMode
            reducedMotion.value = prefs?.getBoolean(KEY_REDUCED_MOTION, false) ?: false
        }
    }

    fun setTheme(theme: AppThemeOption) {
        _currentTheme.value = theme
        prefs?.edit()?.putString(KEY_THEME, theme.id)?.apply()
    }

    fun setDarkMode(enabled: Boolean) {
        isDarkMode.value = enabled
        prefs?.edit()?.putBoolean(KEY_DARK_MODE, enabled)?.apply()
    }

    fun toggleDarkMode() {
        setDarkMode(!isDarkMode.value)
    }

    fun setReducedMotion(enabled: Boolean) {
        reducedMotion.value = enabled
        prefs?.edit()?.putBoolean(KEY_REDUCED_MOTION, enabled)?.apply()
    }
}
