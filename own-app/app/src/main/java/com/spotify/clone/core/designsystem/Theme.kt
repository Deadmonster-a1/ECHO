package com.spotify.clone.core.designsystem

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

fun echoContrastOn(background: Color): Color =
    if (background.luminance() > 0.55f) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)

fun AppThemeOption.resolvedBackground(isDark: Boolean): Color =
    if (isDark) darkBackgroundColor else lightBackgroundColor

fun AppThemeOption.resolvedSurface(isDark: Boolean): Color =
    if (isDark) darkSurfaceColor else lightSurfaceColor

fun AppThemeOption.resolvedCard(isDark: Boolean): Color =
    if (isDark) darkCardColor else lightCardColor

fun AppThemeOption.resolvedElevated(isDark: Boolean): Color =
    if (isDark) EchoDarkElevated else EchoLightElevated

fun AppThemeOption.resolvedBorder(isDark: Boolean): Color =
    if (isDark) EchoDarkBorder else EchoLightBorder

@Composable
fun EchoTheme(content: @Composable () -> Unit) {
    val activeTheme by ThemeManager.currentTheme.collectAsState()
    val isDarkMode by ThemeManager.isDarkMode.collectAsState()
    val isReducedMotion by ThemeManager.reducedMotion.collectAsState()

    val animDuration = if (isReducedMotion) 0 else 220

    val primaryColor by animateColorAsState(activeTheme.primaryColor, tween(animDuration), label = "thm_primary")
    val backgroundColor by animateColorAsState(activeTheme.resolvedBackground(isDarkMode), tween(animDuration), label = "thm_bg")
    val surfaceColor by animateColorAsState(activeTheme.resolvedSurface(isDarkMode), tween(animDuration), label = "thm_surface")
    val cardColor by animateColorAsState(activeTheme.resolvedCard(isDarkMode), tween(animDuration), label = "thm_card")

    val onPrimaryColor = echoContrastOn(primaryColor)
    val onSecondaryColor = echoContrastOn(cardColor)
    val onBackground = if (isDarkMode) EchoTextPrimaryDark else EchoTextPrimaryLight
    val onSurface = if (isDarkMode) EchoTextPrimaryDark else EchoTextPrimaryLight
    val onSurfaceVariant = if (isDarkMode) EchoTextSecondaryDark else EchoTextSecondaryLight
    val outlineColor = activeTheme.resolvedBorder(isDarkMode)

    val view = LocalView.current
    if (!view.isInEditMode) {
        LaunchedEffect(isDarkMode) {
            updateSystemBarsAppearance(view = view, lightBars = !isDarkMode, updateEdgeToEdge = true)
        }
        SideEffect {
            updateSystemBarsAppearance(view = view, lightBars = !isDarkMode, updateEdgeToEdge = false)
        }
    }

    val colorScheme = if (isDarkMode) {
        darkColorScheme(
            primary = primaryColor,
            onPrimary = onPrimaryColor,
            secondary = cardColor,
            onSecondary = onSecondaryColor,
            background = backgroundColor,
            onBackground = onBackground,
            surface = surfaceColor,
            onSurface = onSurface,
            surfaceVariant = EchoDarkElevated,
            onSurfaceVariant = onSurfaceVariant,
            outline = outlineColor
        )
    } else {
        lightColorScheme(
            primary = primaryColor,
            onPrimary = onPrimaryColor,
            secondary = cardColor,
            onSecondary = onSecondaryColor,
            background = backgroundColor,
            onBackground = onBackground,
            surface = surfaceColor,
            onSurface = onSurface,
            surfaceVariant = Color(0xFFECEDF3),
            onSurfaceVariant = onSurfaceVariant,
            outline = outlineColor
        )
    }

    CompositionLocalProvider(
        LocalAppTheme provides activeTheme,
        LocalReducedMotion provides isReducedMotion
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = EchoTypography,
            content = content
        )
    }
}

@Composable
fun SpotifyTheme(content: @Composable () -> Unit) {
    EchoTheme(content = content)
}

/**
 * Applies light or dark system bar icon appearance consistently across API levels.
 */
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
