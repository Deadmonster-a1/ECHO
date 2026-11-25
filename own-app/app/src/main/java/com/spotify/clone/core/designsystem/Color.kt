package com.spotify.clone.core.designsystem

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ==========================================
// ECHO — Pastel Prism & Airy Daylight Design System
// Derived directly from the ECHO Logo Spectrum
// ==========================================

val EchoPrismRose = Color(0xFFFF8FA3)
val EchoPrismLavender = Color(0xFFC49BFF)
val EchoPrismSky = Color(0xFF8FB8FF)
val EchoPrismAqua = Color(0xFF6FE3E8)
val EchoPrismPrimary = Color(0xFF7B6EF6)
val EchoPrismPrimaryPressed = Color(0xFF6254E8)

val EchoPrismGradientColors = listOf(
    EchoPrismRose,
    EchoPrismLavender,
    EchoPrismSky,
    EchoPrismAqua
)

fun echoPrismBrush(): Brush = Brush.linearGradient(EchoPrismGradientColors)

val EchoAccentCoral = Color(0xFFFA2D48)      // Apple Music Rose-Coral
val EchoAccentOrange = Color(0xFFFF5E3A)     // Warm ECHO Coral-Orange
val EchoAccentBlue = Color(0xFF007AFF)       // iOS System Blue
val EchoAccentMint = Color(0xFF34C759)       // iOS System Green
val EchoAccentIndigo = Color(0xFF5856D6)     // iOS System Indigo

// Light Mode Surfaces (Airy Porcelain Daylight matching the White Logo Tile)
val EchoLightBackground = Color(0xFFFBFBFD)
val EchoLightGroupedBg = Color(0xFFF2F3F9)
val EchoLightSurface = Color(0xFFFFFFFF)
val EchoLightCard = Color(0xFFFFFFFF)
val EchoLightBorder = Color(0xFFE7E8F2)

val EchoTextPrimaryLight = Color(0xFF16161D)
val EchoTextSecondaryLight = Color(0xFF5E5F6E)
val EchoTextMutedLight = Color(0xFF8E8F9D)

// Dark Mode Surfaces (Rich Tinted Obsidian — never flat #000000 void)
val EchoDarkBackground = Color(0xFF09090E)
val EchoDarkSurface = Color(0xFF12121A)
val EchoDarkElevated = Color(0xFF1B1B26)
val EchoDarkBorder = Color(0xFF2B2C3B)

val EchoTextPrimaryDark = Color(0xFFF8F8FC)
val EchoTextSecondaryDark = Color(0xFFA8A9B8)
val EchoTextMutedDark = Color(0xFF7D7E8D)

// Compatibility aliases for retained subcomponents
val EchoOrange = EchoAccentOrange
val EchoLavender = EchoPrismLavender
val EchoCoral = EchoPrismRose

// Category Discovery Tones (Soft Pastel Prism Aligned)
val CategoryPop = Color(0xFFAF6BFF)
val CategoryHipHop = Color(0xFFFF7A93)
val CategoryRock = Color(0xFFFF6B8A)
val CategoryIndie = Color(0xFF4ECDC4)
val CategoryDance = Color(0xFF7B6EF6)
val CategoryMood = Color(0xFFC49BFF)
val CategoryPodcasts = Color(0xFF68B6FF)
val CategoryDiscover = Color(0xFF6FE3E8)
