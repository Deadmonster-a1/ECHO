package com.spotify.clone.core.designsystem

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ==========================================
// ECHO — Material 3 Expressive Color System
// Vibrant emotional chromatic ramps + full tonal surface containers
// ==========================================

val EchoPrismRose = Color(0xFFFF6B9D)
val EchoPrismLavender = Color(0xFFA78BFA)
val EchoPrismSky = Color(0xFF60A5FA)
val EchoPrismAqua = Color(0xFF2DD4BF)
val EchoPrismPrimary = Color(0xFF6750A4)
val EchoPrismPrimaryPressed = Color(0xFF4F378B)

val EchoPrismGradientColors = listOf(
    EchoPrismRose,
    EchoPrismLavender,
    EchoPrismSky,
    EchoPrismAqua
)

fun echoPrismBrush(): Brush = Brush.linearGradient(EchoPrismGradientColors)

val EchoAccentCoral = Color(0xFFE11D48)      // Semantic love / error highlight
val EchoAccentOrange = Color(0xFFFF6B35)
val EchoAccentBlue = Color(0xFF2563EB)
val EchoAccentMint = Color(0xFF10B981)
val EchoAccentIndigo = Color(0xFF6750A4)

// Light Mode Expressive Tonal Surface Hierarchy (Chromatic Warm/Cool neutral ramp)
val EchoLightBackground = Color(0xFFFAF8FF)
val EchoLightGroupedBg = Color(0xFFF4EFFA)
val EchoLightSurface = Color(0xFFFAF8FF)
val EchoLightCard = Color(0xFFF1ECF8)
val EchoLightElevated = Color(0xFFEAE3F4)
val EchoLightBorder = Color(0xFFCEC4DD)

val EchoTextPrimaryLight = Color(0xFF1A1822)
val EchoTextSecondaryLight = Color(0xFF565166)
val EchoTextMutedLight = Color(0xFF7B758B)

// Dark Mode Expressive Tonal Surface Hierarchy (Deep Tinted Obsidian ramp)
val EchoDarkBackground = Color(0xFF0E0D14)
val EchoDarkSurface = Color(0xFF13121B)
val EchoDarkElevated = Color(0xFF1E1C2A)
val EchoDarkBorder = Color(0xFF343146)

val EchoTextPrimaryDark = Color(0xFFF6F2FF)
val EchoTextSecondaryDark = Color(0xFFC3BDD4)
val EchoTextMutedDark = Color(0xFF8E88A3)

// Compatibility aliases for retained subcomponents
val EchoOrange = EchoAccentOrange
val EchoLavender = EchoPrismLavender
val EchoCoral = EchoPrismRose

// Expressive Category Discovery Tones (Rich Duotone Tonal Ramp)
val CategoryPop = Color(0xFF7E57C2)
val CategoryHipHop = Color(0xFFE0406A)
val CategoryRock = Color(0xFFD84315)
val CategoryIndie = Color(0xFF00897B)
val CategoryDance = Color(0xFF5C6BC0)
val CategoryMood = Color(0xFF8E24AA)
val CategoryPodcasts = Color(0xFF1E88E5)
val CategoryDiscover = Color(0xFF00ACC1)

/**
 * Full Material 3 Expressive surface container ladder and chromatic accent container tokens.
 */
data class ExpressiveSurfaceTokens(
    val primaryContainerLight: Color,
    val onPrimaryContainerLight: Color,
    val secondaryLight: Color,
    val secondaryContainerLight: Color,
    val onSecondaryContainerLight: Color,
    val tertiaryLight: Color,
    val tertiaryContainerLight: Color,
    val onTertiaryContainerLight: Color,
    val surfaceLowestLight: Color,
    val surfaceLowLight: Color,
    val surfaceContainerLight: Color,
    val surfaceHighLight: Color,
    val surfaceHighestLight: Color,
    val outlineVariantLight: Color,

    val primaryContainerDark: Color,
    val onPrimaryContainerDark: Color,
    val secondaryDark: Color,
    val secondaryContainerDark: Color,
    val onSecondaryContainerDark: Color,
    val tertiaryDark: Color,
    val tertiaryContainerDark: Color,
    val onTertiaryContainerDark: Color,
    val surfaceLowestDark: Color,
    val surfaceLowDark: Color,
    val surfaceContainerDark: Color,
    val surfaceHighDark: Color,
    val surfaceHighestDark: Color,
    val outlineVariantDark: Color
)
