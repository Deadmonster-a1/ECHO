package com.spotify.clone.core.designsystem

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color

@Composable
fun EchoTheme(content: @Composable () -> Unit) {
    val activeTheme by ThemeManager.currentTheme.collectAsState()
    val isDarkMode by ThemeManager.isDarkMode.collectAsState()
    val isReducedMotion by ThemeManager.reducedMotion.collectAsState()

    val animDuration = if (isReducedMotion) 0 else 300

    val primaryColor by animateColorAsState(activeTheme.primaryColor, tween(animDuration), label = "thm_primary")
    val backgroundColor by animateColorAsState(activeTheme.backgroundColor, tween(animDuration), label = "thm_bg")
    val surfaceColor by animateColorAsState(activeTheme.surfaceColor, tween(animDuration), label = "thm_surface")
    val cardColor by animateColorAsState(activeTheme.cardColor, tween(animDuration), label = "thm_card")

    val onBackground = if (isDarkMode) EchoTextPrimaryDark else EchoTextPrimaryLight
    val onSurface = if (isDarkMode) EchoTextPrimaryDark else EchoTextPrimaryLight
    val onSurfaceVariant = if (isDarkMode) EchoTextSecondaryDark else EchoTextSecondaryLight

    val colorScheme = if (isDarkMode) {
        darkColorScheme(
            primary = primaryColor,
            onPrimary = Color(0xFF101016),
            secondary = cardColor,
            onSecondary = onBackground,
            background = backgroundColor,
            onBackground = onBackground,
            surface = surfaceColor,
            onSurface = onSurface,
            surfaceVariant = cardColor,
            onSurfaceVariant = onSurfaceVariant,
            outline = EchoDarkBorder
        )
    } else {
        lightColorScheme(
            primary = primaryColor,
            onPrimary = Color(0xFFFFFFFF),
            secondary = cardColor,
            onSecondary = onBackground,
            background = backgroundColor,
            onBackground = onBackground,
            surface = surfaceColor,
            onSurface = onSurface,
            surfaceVariant = cardColor,
            onSurfaceVariant = onSurfaceVariant,
            outline = EchoLightBorder
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
