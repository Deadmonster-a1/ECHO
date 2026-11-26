package com.spotify.clone.core.designsystem

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ==========================================
// ECHO — SwiftPieces Liquid Glass & Restraint Design System
// Single locked accent hue + cool-neutral tonal ramp
// ==========================================

val EchoPrismRose = Color(0xFFFF8FA3)
val EchoPrismLavender = Color(0xFFC49BFF)
val EchoPrismSky = Color(0xFF8FB8FF)
val EchoPrismAqua = Color(0xFF6FE3E8)
val EchoPrismPrimary = Color(0xFF6C5CE7)
val EchoPrismPrimaryPressed = Color(0xFF5848D6)

val EchoPrismGradientColors = listOf(
    EchoPrismRose,
    EchoPrismLavender,
    EchoPrismSky,
    EchoPrismAqua
)

fun echoPrismBrush(): Brush = Brush.linearGradient(EchoPrismGradientColors)

val EchoAccentCoral = Color(0xFFFA2D48)      // Semantic love / error highlight
val EchoAccentOrange = Color(0xFFFF5E3A)
val EchoAccentBlue = Color(0xFF007AFF)
val EchoAccentMint = Color(0xFF34C759)
val EchoAccentIndigo = Color(0xFF6C5CE7)

// Light Mode Surfaces (Cool-neutral Apple grouped system ramp)
val EchoLightBackground = Color(0xFFF5F6FA)
val EchoLightGroupedBg = Color(0xFFF5F6FA)
val EchoLightSurface = Color(0xFFFFFFFF)
val EchoLightCard = Color(0xFFFFFFFF)
val EchoLightElevated = Color(0xFFFFFFFF)
val EchoLightBorder = Color(0xFFE4E6EF)

val EchoTextPrimaryLight = Color(0xFF12131A)
val EchoTextSecondaryLight = Color(0xFF5C5E6D)
val EchoTextMutedLight = Color(0xFF8B8D9C)

// Dark Mode Surfaces (Cool-neutral Tinted Obsidian ramp)
val EchoDarkBackground = Color(0xFF0A0A0F)
val EchoDarkSurface = Color(0xFF13131B)
val EchoDarkElevated = Color(0xFF1B1C26)
val EchoDarkBorder = Color(0xFF262734)

val EchoTextPrimaryDark = Color(0xFFF6F7FB)
val EchoTextSecondaryDark = Color(0xFFA2A4B4)
val EchoTextMutedDark = Color(0xFF737585)

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
