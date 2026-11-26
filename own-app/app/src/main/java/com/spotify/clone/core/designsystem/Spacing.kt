package com.spotify.clone.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * ECHO — Material 3 Expressive Spacing & Geometry System
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
    val sectionSpacing: Dp = 26.dp
    val cardSpacing: Dp = 14.dp
    val itemSpacing: Dp = 10.dp

    // Navigation & Floating M3 Expressive Bar Dimensions
    val bottomNavHeight: Dp = 64.dp
    val miniPlayerHeight: Dp = 64.dp
    val topAppBarHeight: Dp = 60.dp
    val searchBarHeight: Dp = 54.dp
    val minTouchTarget: Dp = 48.dp

    // Artwork Standard Sizes
    val artworkMini: Dp = 46.dp
    val artworkRow: Dp = 52.dp
    val artworkMedium: Dp = 76.dp
    val artworkCard: Dp = 152.dp
    val artworkCardLarge: Dp = 168.dp
    val artworkHero: Dp = 308.dp
    val artistAvatarSmall: Dp = 44.dp
    val artistAvatarMedium: Dp = 76.dp
    val artistAvatarLarge: Dp = 98.dp

    // Button Sizes
    val buttonSmall: Dp = 40.dp
    val buttonMedium: Dp = 48.dp
    val buttonLarge: Dp = 56.dp
    val playFabSmall: Dp = 48.dp
    val playFabStandard: Dp = 56.dp
    val playFabHero: Dp = 72.dp
}

/**
 * ECHO — Material 3 Expressive Corner Radii Scale
 * Boldly rounded organic corners supporting shape morphing on press/selection.
 */
object EchoRadius {
    val none: Dp = 0.dp
    val xs: Dp = 10.dp
    val sm: Dp = 16.dp
    val md: Dp = 20.dp
    val lg: Dp = 24.dp
    val xl: Dp = 28.dp
    val xxl: Dp = 32.dp
    val pill: Dp = 999.dp
    val circular: Dp = 999.dp

    fun nested(outer: Dp, padding: Dp): Dp = (outer - padding).coerceAtLeast(xs)
}

/**
 * Material 3 Expressive Shape Scale applied globally via MaterialTheme.shapes.
 */
val EchoExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(EchoRadius.xs),
    small = RoundedCornerShape(EchoRadius.sm),
    medium = RoundedCornerShape(EchoRadius.md),
    large = RoundedCornerShape(EchoRadius.xl),
    extraLarge = RoundedCornerShape(EchoRadius.xxl)
)
