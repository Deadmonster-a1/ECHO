package com.spotify.clone.core.designsystem

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * ECHO Spacing & Geometry System
 * Strict 4pt-based grid aligning with Apple Human Interface Guidelines and reference geometry.
 */
object EchoSpacing {
    // 4pt Base Spacing Scale
    val space2: Dp = 2.dp
    val space4: Dp = 4.dp
    val space6: Dp = 6.dp
    val space8: Dp = 8.dp
    val space10: Dp = 10.dp
    val space12: Dp = 12.dp
    val space14: Dp = 14.dp
    val space16: Dp = 16.dp
    val space18: Dp = 18.dp
    val space20: Dp = 20.dp
    val space24: Dp = 24.dp
    val space28: Dp = 28.dp
    val space32: Dp = 32.dp
    val space40: Dp = 40.dp
    val space48: Dp = 48.dp
    val space56: Dp = 56.dp
    val space64: Dp = 64.dp
    val space80: Dp = 80.dp

    // Screen Margins & Whitespace
    val screenHorizontalMargin: Dp = 20.dp
    val screenTopMargin: Dp = 16.dp
    val sectionSpacing: Dp = 24.dp
    val cardSpacing: Dp = 14.dp
    val itemSpacing: Dp = 10.dp

    // Navigation & Persistent Bar Dimensions
    val bottomNavHeight: Dp = 64.dp
    val miniPlayerHeight: Dp = 64.dp
    val topAppBarHeight: Dp = 56.dp
    val searchBarHeight: Dp = 52.dp

    // Artwork Standard Sizes
    val artworkMini: Dp = 42.dp
    val artworkRow: Dp = 52.dp
    val artworkMedium: Dp = 72.dp
    val artworkCard: Dp = 140.dp
    val artworkCardLarge: Dp = 160.dp
    val artworkHero: Dp = 300.dp
    val artistAvatarSmall: Dp = 40.dp
    val artistAvatarMedium: Dp = 72.dp
    val artistAvatarLarge: Dp = 96.dp

    // Button Sizes
    val buttonSmall: Dp = 34.dp
    val buttonMedium: Dp = 44.dp
    val buttonLarge: Dp = 52.dp
    val playFabSmall: Dp = 38.dp
    val playFabStandard: Dp = 56.dp
    val playFabHero: Dp = 68.dp
}

/**
 * ECHO Corner Radii Scale
 * Organic, continuous Apple-style corner rounding.
 */
object EchoRadius {
    val none: Dp = 0.dp
    val xs: Dp = 6.dp
    val sm: Dp = 10.dp
    val md: Dp = 14.dp
    val lg: Dp = 18.dp
    val xl: Dp = 20.dp
    val xxl: Dp = 24.dp
    val pill: Dp = 999.dp
    val circular: Dp = 999.dp
}
