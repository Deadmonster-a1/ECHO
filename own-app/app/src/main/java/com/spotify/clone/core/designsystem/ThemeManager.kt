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
    val primaryColorDark: Color,
    val primaryPressed: Color,
    val darkBackgroundColor: Color,
    val darkSurfaceColor: Color,
    val darkCardColor: Color,
    val lightBackgroundColor: Color,
    val lightSurfaceColor: Color,
    val lightCardColor: Color,
    val gradientStart: Color,
    val gradientEnd: Color,
    val tokens: ExpressiveSurfaceTokens
) {
    PASTEL_PRISM(
        id = "pastel_prism",
        displayName = "Expressive Violet",
        subtitle = "Vibrant Material 3 expressive violet, rose & aqua tokens",
        primaryColor = Color(0xFF6750A4),
        primaryColorDark = Color(0xFFD0BCFF),
        primaryPressed = Color(0xFF4F378B),
        darkBackgroundColor = Color(0xFF0E0D14),
        darkSurfaceColor = Color(0xFF14131D),
        darkCardColor = Color(0xFF1E1C2A),
        lightBackgroundColor = Color(0xFFFAF8FF),
        lightSurfaceColor = Color(0xFFFAF8FF),
        lightCardColor = Color(0xFFF1ECF8),
        gradientStart = Color(0xFFFF6B9D),
        gradientEnd = Color(0xFF2DD4BF),
        tokens = ExpressiveSurfaceTokens(
            primaryContainerLight = Color(0xFFE9DDFF),
            onPrimaryContainerLight = Color(0xFF22005D),
            secondaryLight = Color(0xFF625B71),
            secondaryContainerLight = Color(0xFFE8DEF8),
            onSecondaryContainerLight = Color(0xFF1E192B),
            tertiaryLight = Color(0xFF984061),
            tertiaryContainerLight = Color(0xFFFFD9E2),
            onTertiaryContainerLight = Color(0xFF3E001D),
            surfaceLowestLight = Color(0xFFFFFFFF),
            surfaceLowLight = Color(0xFFF5F1FA),
            surfaceContainerLight = Color(0xFFEFE9F6),
            surfaceHighLight = Color(0xFFE9E2F1),
            surfaceHighestLight = Color(0xFFE2DBEC),
            outlineVariantLight = Color(0xFFCAC4D5),

            primaryContainerDark = Color(0xFF4F378B),
            onPrimaryContainerDark = Color(0xFFEADDFF),
            secondaryDark = Color(0xFFCCC2DC),
            secondaryContainerDark = Color(0xFF3A3549),
            onSecondaryContainerDark = Color(0xFFE8DEF8),
            tertiaryDark = Color(0xFFFFB0C8),
            tertiaryContainerDark = Color(0xFF632842),
            onTertiaryContainerDark = Color(0xFFFFD9E2),
            surfaceLowestDark = Color(0xFF09080E),
            surfaceLowDark = Color(0xFF14131D),
            surfaceContainerDark = Color(0xFF1B1926),
            surfaceHighDark = Color(0xFF252333),
            surfaceHighestDark = Color(0xFF302D41),
            outlineVariantDark = Color(0xFF464258)
        )
    ),
    ELECTRIC_CORAL(
        id = "electric_coral",
        displayName = "Electric Coral",
        subtitle = "Bold crimson-coral energy with warm peach containers",
        primaryColor = Color(0xFFC0294E),
        primaryColorDark = Color(0xFFFFB2BE),
        primaryPressed = Color(0xFF991B3A),
        darkBackgroundColor = Color(0xFF120C0F),
        darkSurfaceColor = Color(0xFF191115),
        darkCardColor = Color(0xFF24181E),
        lightBackgroundColor = Color(0xFFFFF8F8),
        lightSurfaceColor = Color(0xFFFFF8F8),
        lightCardColor = Color(0xFFFBECEF),
        gradientStart = Color(0xFFFF5A7A),
        gradientEnd = Color(0xFFFF9F68),
        tokens = ExpressiveSurfaceTokens(
            primaryContainerLight = Color(0xFFFFD9DE),
            onPrimaryContainerLight = Color(0xFF3F0014),
            secondaryLight = Color(0xFF76565B),
            secondaryContainerLight = Color(0xFFFFD9DF),
            onSecondaryContainerLight = Color(0xFF2C1519),
            tertiaryLight = Color(0xFF835425),
            tertiaryContainerLight = Color(0xFFFFDCBE),
            onTertiaryContainerLight = Color(0xFF2D1600),
            surfaceLowestLight = Color(0xFFFFFFFF),
            surfaceLowLight = Color(0xFFFFF0F2),
            surfaceContainerLight = Color(0xFFF9E6E9),
            surfaceHighLight = Color(0xFFF3DFE3),
            surfaceHighestLight = Color(0xFFEDD7DC),
            outlineVariantLight = Color(0xFFD8C1C6),

            primaryContainerDark = Color(0xFF8C1435),
            onPrimaryContainerDark = Color(0xFFFFD9DE),
            secondaryDark = Color(0xFFE5BDC2),
            secondaryContainerDark = Color(0xFF42282D),
            onSecondaryContainerDark = Color(0xFFFFD9DF),
            tertiaryDark = Color(0xFFF8BA82),
            tertiaryContainerDark = Color(0xFF663D10),
            onTertiaryContainerDark = Color(0xFFFFDCBE),
            surfaceLowestDark = Color(0xFF0C070A),
            surfaceLowDark = Color(0xFF191115),
            surfaceContainerDark = Color(0xFF20161B),
            surfaceHighDark = Color(0xFF2B1E25),
            surfaceHighestDark = Color(0xFF362730),
            outlineVariantDark = Color(0xFF523D43)
        )
    ),
    AURORA_TEAL(
        id = "aurora_teal",
        displayName = "Aurora Pulse",
        subtitle = "Crisp deep emerald-cyan paired with sky-indigo highlights",
        primaryColor = Color(0xFF006B63),
        primaryColorDark = Color(0xFF64E8DA),
        primaryPressed = Color(0xFF00504A),
        darkBackgroundColor = Color(0xFF091111),
        darkSurfaceColor = Color(0xFF10191A),
        darkCardColor = Color(0xFF172425),
        lightBackgroundColor = Color(0xFFF4FBFB),
        lightSurfaceColor = Color(0xFFF4FBFB),
        lightCardColor = Color(0xFFE5F4F3),
        gradientStart = Color(0xFF2DD4BF),
        gradientEnd = Color(0xFF60A5FA),
        tokens = ExpressiveSurfaceTokens(
            primaryContainerLight = Color(0xFF8AF6E8),
            onPrimaryContainerLight = Color(0xFF00201D),
            secondaryLight = Color(0xFF4A6360),
            secondaryContainerLight = Color(0xFFCCE8E4),
            onSecondaryContainerLight = Color(0xFF05201D),
            tertiaryLight = Color(0xFF48607B),
            tertiaryContainerLight = Color(0xFFD0E4FF),
            onTertiaryContainerLight = Color(0xFF001D35),
            surfaceLowestLight = Color(0xFFFFFFFF),
            surfaceLowLight = Color(0xFFEEF6F5),
            surfaceContainerLight = Color(0xFFE5F0EF),
            surfaceHighLight = Color(0xFFDEEBE9),
            surfaceHighestLight = Color(0xFFD7E5E3),
            outlineVariantLight = Color(0xFFBECAC8),

            primaryContainerDark = Color(0xFF00504A),
            onPrimaryContainerDark = Color(0xFF8AF6E8),
            secondaryDark = Color(0xFFB0CCC8),
            secondaryContainerDark = Color(0xFF233938),
            onSecondaryContainerDark = Color(0xFFCCE8E4),
            tertiaryDark = Color(0xFFB0C9E7),
            tertiaryContainerDark = Color(0xFF2D4660),
            onTertiaryContainerDark = Color(0xFFD0E4FF),
            surfaceLowestDark = Color(0xFF050B0B),
            surfaceLowDark = Color(0xFF10191A),
            surfaceContainerDark = Color(0xFF162122),
            surfaceHighDark = Color(0xFF1E2B2C),
            surfaceHighestDark = Color(0xFF283739),
            outlineVariantDark = Color(0xFF394A4B)
        )
    ),
    STUDIO_AMBER(
        id = "studio_amber",
        displayName = "Studio Gold",
        subtitle = "Warm editorial amber & vinyl bronze expressive surfaces",
        primaryColor = Color(0xFF8C5000),
        primaryColorDark = Color(0xFFFFB963),
        primaryPressed = Color(0xFF6B3C00),
        darkBackgroundColor = Color(0xFF120E09),
        darkSurfaceColor = Color(0xFF1A140E),
        darkCardColor = Color(0xFF261E15),
        lightBackgroundColor = Color(0xFFFFF8F2),
        lightSurfaceColor = Color(0xFFFFF8F2),
        lightCardColor = Color(0xFFFAEEE2),
        gradientStart = Color(0xFFFFB963),
        gradientEnd = Color(0xFFFF6B9D),
        tokens = ExpressiveSurfaceTokens(
            primaryContainerLight = Color(0xFFFFDCBA),
            onPrimaryContainerLight = Color(0xFF2D1600),
            secondaryLight = Color(0xFF725A42),
            secondaryContainerLight = Color(0xFFFEDDBE),
            onSecondaryContainerLight = Color(0xFF281806),
            tertiaryLight = Color(0xFF586339),
            tertiaryContainerLight = Color(0xFFDCE8B4),
            onTertiaryContainerLight = Color(0xFF161E01),
            surfaceLowestLight = Color(0xFFFFFFFF),
            surfaceLowLight = Color(0xFFFFF1E5),
            surfaceContainerLight = Color(0xFFF8E9DA),
            surfaceHighLight = Color(0xFFF2E2D2),
            surfaceHighestLight = Color(0xFFECDCCB),
            outlineVariantLight = Color(0xFFD7C5B4),

            primaryContainerDark = Color(0xFF6B3C00),
            onPrimaryContainerDark = Color(0xFFFFDCBA),
            secondaryDark = Color(0xFFE0C1A3),
            secondaryContainerDark = Color(0xFF3E2D1B),
            onSecondaryContainerDark = Color(0xFFFEDDBE),
            tertiaryDark = Color(0xFFC0CC99),
            tertiaryContainerDark = Color(0xFF3E4A24),
            onTertiaryContainerDark = Color(0xFFDCE8B4),
            surfaceLowestDark = Color(0xFF0B0805),
            surfaceLowDark = Color(0xFF1A140E),
            surfaceContainerDark = Color(0xFF221A12),
            surfaceHighDark = Color(0xFF2E241A),
            surfaceHighestDark = Color(0xFF392E22),
            outlineVariantDark = Color(0xFF524334)
        )
    );

    fun resolvedPrimary(isDark: Boolean): Color = if (isDark) primaryColorDark else primaryColor
}

val LocalAppTheme = staticCompositionLocalOf { AppThemeOption.PASTEL_PRISM }

object ThemeManager {
    private const val PREFS_NAME = "echo_theme_prefs"
    private const val KEY_THEME_ID = "theme_option_id"
    private const val KEY_DARK_MODE = "is_dark_mode"
    private const val KEY_REDUCED_MOTION = "reduced_motion"
    private const val KEY_DYNAMIC_COLOR = "dynamic_color"

    private var prefs: SharedPreferences? = null

    private val _currentTheme = MutableStateFlow(AppThemeOption.PASTEL_PRISM)
    val currentTheme: StateFlow<AppThemeOption> = _currentTheme.asStateFlow()

    // Light expressive canvas default
    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _reducedMotion = MutableStateFlow(false)
    val reducedMotion: StateFlow<Boolean> = _reducedMotion.asStateFlow()

    private val _dynamicColorEnabled = MutableStateFlow(false)
    val dynamicColorEnabled: StateFlow<Boolean> = _dynamicColorEnabled.asStateFlow()

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val savedThemeId = prefs?.getString(KEY_THEME_ID, AppThemeOption.PASTEL_PRISM.id)
            _currentTheme.value = AppThemeOption.entries.find { it.id == savedThemeId } ?: AppThemeOption.PASTEL_PRISM
            // Until the user picks in Settings, follow the system dark theme and
            // "Remove animations" accessibility setting.
            val systemDark = (context.resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
            val systemNoAnimations = try {
                android.provider.Settings.Global.getFloat(
                    context.contentResolver,
                    android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
                    1f
                ) == 0f
            } catch (_: Exception) { false }
            _isDarkMode.value = prefs?.getBoolean(KEY_DARK_MODE, systemDark) ?: systemDark
            _reducedMotion.value = prefs?.getBoolean(KEY_REDUCED_MOTION, systemNoAnimations) ?: systemNoAnimations
            _dynamicColorEnabled.value = prefs?.getBoolean(KEY_DYNAMIC_COLOR, false) ?: false
        }
    }

    fun setTheme(option: AppThemeOption) {
        _currentTheme.value = option
        prefs?.edit()?.putString(KEY_THEME_ID, option.id)?.apply()
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

    fun setDynamicColorEnabled(enabled: Boolean) {
        _dynamicColorEnabled.value = enabled
        prefs?.edit()?.putBoolean(KEY_DYNAMIC_COLOR, enabled)?.apply()
    }
}
