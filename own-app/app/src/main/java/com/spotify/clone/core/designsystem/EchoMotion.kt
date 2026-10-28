package com.spotify.clone.core.designsystem

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

/**
 * ECHO Motion Architecture
 * Defines the physical and temporal motion principles of Organic Sound Architecture.
 */
object EchoMotion {
    // Timing Tokens (in ms)
    const val DurationMicro = 160        // Micro-interactions (taps, icon fills, badges)
    const val DurationStandard = 260     // Standard transitions (cards, tabs, list changes)
    const val DurationExpressive = 380   // Expressive transitions (sheets, modal reveals)
    const val DurationMajor = 520        // Major player transitions (album-to-player, full expansions)

    // Spring Specifications for natural, physical settling
    val SpringBouncy = spring<Float>(
        dampingRatio = 0.72f,
        stiffness = Spring.StiffnessMediumLow
    )

    val SpringGentle = spring<Float>(
        dampingRatio = 0.88f,
        stiffness = Spring.StiffnessLow
    )

    val SpringSnappy = spring<Float>(
        dampingRatio = 0.80f,
        stiffness = Spring.StiffnessMedium
    )

    fun <T> springBouncy() = spring<T>(
        dampingRatio = 0.72f,
        stiffness = Spring.StiffnessMediumLow
    )

    fun <T> springGentle() = spring<T>(
        dampingRatio = 0.88f,
        stiffness = Spring.StiffnessLow
    )

    fun <T> springSnappy() = spring<T>(
        dampingRatio = 0.80f,
        stiffness = Spring.StiffnessMedium
    )

    // Controlled Easings
    val EaseEmphasized = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val EaseStandard = FastOutSlowInEasing
    val EaseDecelerate = LinearOutSlowInEasing

    // Standard Tween Specs
    fun <T> tweenMicro() = tween<T>(DurationMicro, easing = EaseStandard)
    fun <T> tweenStandard() = tween<T>(DurationStandard, easing = EaseStandard)
    fun <T> tweenExpressive() = tween<T>(DurationExpressive, easing = EaseEmphasized)
    fun <T> tweenMajor() = tween<T>(DurationMajor, easing = EaseEmphasized)
}

val LocalReducedMotion = compositionLocalOf { false }

/**
 * Adds an organic physical press response (scale & settling) to any interactive element.
 * Automatically respects system reduced motion preference.
 */
fun Modifier.echoPressable(
    scaleOnPress: Float = 0.95f,
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val isReducedMotion = LocalReducedMotion.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val targetScale = if (isPressed && enabled && !isReducedMotion) scaleOnPress else 1.0f
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = if (isPressed) EchoMotion.SpringSnappy else EchoMotion.SpringBouncy,
        label = "echo_press_scale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
}
