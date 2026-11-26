package com.spotify.clone.core.designsystem

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

fun echoContrastOn(background: Color): Color =
    if (background.luminance() > 0.52f) Color(0xFF1A1822) else Color(0xFFFFFFFF)

fun AppThemeOption.resolvedBackground(isDark: Boolean): Color =
    if (isDark) darkBackgroundColor else lightBackgroundColor

fun AppThemeOption.resolvedSurface(isDark: Boolean): Color =
    if (isDark) darkSurfaceColor else lightSurfaceColor

fun AppThemeOption.resolvedCard(isDark: Boolean): Color =
    if (isDark) tokens.surfaceContainerDark else tokens.surfaceContainerLight

fun AppThemeOption.resolvedElevated(isDark: Boolean): Color =
    if (isDark) tokens.surfaceHighDark else tokens.surfaceHighLight

fun AppThemeOption.resolvedBorder(isDark: Boolean): Color =
    if (isDark) tokens.outlineVariantDark else tokens.outlineVariantLight

@Composable
fun EchoTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val activeTheme by ThemeManager.currentTheme.collectAsState()
    val isDarkMode by ThemeManager.isDarkMode.collectAsState()
    val isReducedMotion by ThemeManager.reducedMotion.collectAsState()
    val isDynamicColor by ThemeManager.dynamicColorEnabled.collectAsState()

    val animDuration = if (isReducedMotion) 0 else 240

    val useDynamic = isDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val baseScheme: ColorScheme = if (useDynamic) {
        if (isDarkMode) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        val t = activeTheme.tokens
        if (isDarkMode) {
            darkColorScheme(
                primary = activeTheme.primaryColorDark,
                onPrimary = echoContrastOn(activeTheme.primaryColorDark),
                primaryContainer = t.primaryContainerDark,
                onPrimaryContainer = t.onPrimaryContainerDark,
                secondary = t.secondaryDark,
                onSecondary = echoContrastOn(t.secondaryDark),
                secondaryContainer = t.secondaryContainerDark,
                onSecondaryContainer = t.onSecondaryContainerDark,
                tertiary = t.tertiaryDark,
                onTertiary = echoContrastOn(t.tertiaryDark),
                tertiaryContainer = t.tertiaryContainerDark,
                onTertiaryContainer = t.onTertiaryContainerDark,
                background = activeTheme.darkBackgroundColor,
                onBackground = EchoTextPrimaryDark,
                surface = activeTheme.darkSurfaceColor,
                onSurface = EchoTextPrimaryDark,
                surfaceVariant = t.surfaceHighDark,
                onSurfaceVariant = EchoTextSecondaryDark,
                surfaceContainerLowest = t.surfaceLowestDark,
                surfaceContainerLow = t.surfaceLowDark,
                surfaceContainer = t.surfaceContainerDark,
                surfaceContainerHigh = t.surfaceHighDark,
                surfaceContainerHighest = t.surfaceHighestDark,
                outline = EchoDarkBorder,
                outlineVariant = t.outlineVariantDark,
                error = EchoAccentCoral,
                onError = Color.White
            )
        } else {
            lightColorScheme(
                primary = activeTheme.primaryColor,
                onPrimary = echoContrastOn(activeTheme.primaryColor),
                primaryContainer = t.primaryContainerLight,
                onPrimaryContainer = t.onPrimaryContainerLight,
                secondary = t.secondaryLight,
                onSecondary = echoContrastOn(t.secondaryLight),
                secondaryContainer = t.secondaryContainerLight,
                onSecondaryContainer = t.onSecondaryContainerLight,
                tertiary = t.tertiaryLight,
                onTertiary = echoContrastOn(t.tertiaryLight),
                tertiaryContainer = t.tertiaryContainerLight,
                onTertiaryContainer = t.onTertiaryContainerLight,
                background = activeTheme.lightBackgroundColor,
                onBackground = EchoTextPrimaryLight,
                surface = activeTheme.lightSurfaceColor,
                onSurface = EchoTextPrimaryLight,
                surfaceVariant = t.surfaceHighLight,
                onSurfaceVariant = EchoTextSecondaryLight,
                surfaceContainerLowest = t.surfaceLowestLight,
                surfaceContainerLow = t.surfaceLowLight,
                surfaceContainer = t.surfaceContainerLight,
                surfaceContainerHigh = t.surfaceHighLight,
                surfaceContainerHighest = t.surfaceHighestLight,
                outline = EchoLightBorder,
                outlineVariant = t.outlineVariantLight,
                error = EchoAccentCoral,
                onError = Color.White
            )
        }
    }

    val primaryColor by animateColorAsState(baseScheme.primary, tween(animDuration), label = "thm_primary")
    val backgroundColor by animateColorAsState(baseScheme.background, tween(animDuration), label = "thm_bg")
    val surfaceColor by animateColorAsState(baseScheme.surface, tween(animDuration), label = "thm_surface")
    val containerColor by animateColorAsState(baseScheme.surfaceContainer, tween(animDuration), label = "thm_container")
    val primaryContainerColor by animateColorAsState(baseScheme.primaryContainer, tween(animDuration), label = "thm_prim_cont")

    val colorScheme = baseScheme.copy(
        primary = primaryColor,
        background = backgroundColor,
        surface = surfaceColor,
        surfaceContainer = containerColor,
        primaryContainer = primaryContainerColor
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        LaunchedEffect(isDarkMode) {
            updateSystemBarsAppearance(view = view, lightBars = !isDarkMode, updateEdgeToEdge = true)
        }
    }

    CompositionLocalProvider(
        LocalAppTheme provides activeTheme,
        LocalReducedMotion provides isReducedMotion
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = EchoExpressiveShapes,
            typography = EchoTypography,
            content = content
        )
    }
}

@Composable
fun SpotifyTheme(content: @Composable () -> Unit) {
    EchoTheme(content = content)
}

internal fun updateSystemBarsAppearance(
    view: View,
    lightBars: Boolean,
    updateEdgeToEdge: Boolean = false
) {
    val activity = view.context.findActivity() ?: return
    if (updateEdgeToEdge && activity is ComponentActivity) {
        if (lightBars) {
            activity.enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.light(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT
                ),
                navigationBarStyle = SystemBarStyle.light(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT
                )
            )
        } else {
            activity.enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
                navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            )
        }
    }
    val insetsController = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
    insetsController.isAppearanceLightStatusBars = lightBars
    insetsController.isAppearanceLightNavigationBars = lightBars
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
