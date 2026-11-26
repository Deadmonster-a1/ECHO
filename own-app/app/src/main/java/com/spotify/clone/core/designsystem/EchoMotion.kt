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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.abs

/**
 * ECHO Motion Architecture
 * Synthesizes Apple's Fluid Interfaces (interruptible springs, velocity handoff, rubber-banding)
 * with Emil Kowalski's Design Engineering principles (strong ease-out, under-300ms UI budgets,
 * asymmetric press/release, GPU-only transforms, and non-uniform organic staggers).
 */
object EchoMotion {
    // Timing Tokens (in ms) — UI stays under 300ms; sheets/major transitions 320-480ms
    const val DurationPress = 110        // Instant pointer-down tactile compression
    const val DurationMicro = 160        // Micro-interactions (taps, icon fills, badges)
    const val DurationStandard = 240     // Standard transitions (cards, tabs, state swaps)
    const val DurationExpressive = 360   // Expressive transitions (sheets, modal reveals)
    const val DurationMajor = 460        // Major player transitions (mini-to-full expansion)

    // Emil Kowalski's custom easing curves (stronger than platform defaults)
    val EaseOutStrong = CubicBezierEasing(0.23f, 1.0f, 0.32f, 1.0f)
    val EaseInOutStrong = CubicBezierEasing(0.77f, 0.0f, 0.175f, 1.0f)
    val EaseDrawer = CubicBezierEasing(0.32f, 0.72f, 0.0f, 1.0f)

    // Reverse-engineered Spotify Encore & MotionScene interpolator curves (from Spotify.apk)
    // res/anim/encore_button_animation_interpolator.xml: controlX1=0.3, controlY1=0.0, controlX2=0.0, controlY2=1.0
    val EncoreButtonEase = CubicBezierEasing(0.3f, 0.0f, 0.0f, 1.0f)
    // res/anim/encore_box_animation_interpolator.xml: controlX1=0.3, controlY1=0.0, controlX2=0.4, controlY2=1.0
    val EncoreBoxEase = CubicBezierEasing(0.3f, 0.0f, 0.4f, 1.0f)
    // res/xml/header_content_motion_layout_scene.xml: cubic(0.6, 0, 0.7, 1)
    val HeaderMotionEase = CubicBezierEasing(0.6f, 0.0f, 0.7f, 1.0f)

    // Compatibility aliases
    val EaseEmphasized = EaseOutStrong
    val EaseStandard = FastOutSlowInEasing
    val EaseDecelerate = LinearOutSlowInEasing

    // Apple-inspired Spring Specifications (critically damped default + subtle momentum bounce)
    val SpringCriticallyDamped = spring<Float>(
        dampingRatio = 1.0f,
        stiffness = 420f
    )

    val SpringMomentum = spring<Float>(
        dampingRatio = 0.82f,
        stiffness = 380f
    )

    val SpringBouncy = spring<Float>(
        dampingRatio = 0.74f,
        stiffness = 360f
    )

    val SpringGentle = spring<Float>(
        dampingRatio = 0.90f,
        stiffness = 220f
    )

    val SpringSnappy = spring<Float>(
        dampingRatio = 0.85f,
        stiffness = 460f
    )

    fun <T> springCriticallyDamped() = spring<T>(
        dampingRatio = 1.0f,
        stiffness = 420f
    )

    fun <T> springMomentum() = spring<T>(
        dampingRatio = 0.82f,
        stiffness = 380f
    )

    fun <T> springBouncy() = spring<T>(
        dampingRatio = 0.74f,
        stiffness = 360f
    )

    fun <T> springGentle() = spring<T>(
        dampingRatio = 0.90f,
        stiffness = 220f
    )

    fun <T> springSnappy() = spring<T>(
        dampingRatio = 0.85f,
        stiffness = 460f
    )

    // Standard Tween Specs
    fun <T> tweenMicro() = tween<T>(DurationMicro, easing = EaseOutStrong)
    fun <T> tweenStandard() = tween<T>(DurationStandard, easing = EaseOutStrong)
    fun <T> tweenExpressive() = tween<T>(DurationExpressive, easing = EaseDrawer)
    fun <T> tweenMajor() = tween<T>(DurationMajor, easing = EaseDrawer)

    /**
     * Apple's WWDC Fluid Interfaces exponential momentum projection.
     * Predicts where a flick gesture is going rather than snapping to the nearest edge.
     */
    fun projectMomentum(velocityPxPerSec: Float, decelerationRate: Float = 0.996f): Float {
        return (velocityPxPerSec / 1000f) * decelerationRate / (1f - decelerationRate)
    }

    /**
     * Apple's progressive rubber-banding resistance past natural boundaries.
     */
    fun rubberBand(overshoot: Float, dimension: Float, constant: Float = 0.55f): Float {
        if (dimension <= 0f) return 0f
        return (overshoot * dimension * constant) / (dimension + constant * abs(overshoot))
    }
}

val LocalReducedMotion = compositionLocalOf { false }

/**
 * Adds an instant, interruptible physical press response on pointer-down
 * with asymmetric release spring and optional subtle haptic feedback.
 * Automatically respects LocalReducedMotion.
 */
fun Modifier.echoPressable(
    scaleOnPress: Float = 0.97f,
    enabled: Boolean = true,
    hapticOnClick: Boolean = false,
    onClick: () -> Unit
): Modifier = composed {
    val isReducedMotion = LocalReducedMotion.current
    val haptic = LocalHapticFeedback.current
    val latestOnClick by rememberUpdatedState(onClick)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val targetScale = if (isPressed && enabled && !isReducedMotion) scaleOnPress else 1.0f
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = if (isPressed) {
            tween(durationMillis = EchoMotion.DurationPress, easing = EchoMotion.EaseOutStrong)
        } else {
            EchoMotion.SpringMomentum
        },
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
            onClick = {
                if (hapticOnClick) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                latestOnClick()
            }
        )
}

/**
 * Non-uniform organic staggered entrance for curated sections/cards.
 * Caps total stagger budget under ~240ms and never blocks user interaction.
 * Under reduced motion, skips translation and keeps a crisp opacity fade.
 */
fun Modifier.echoStaggeredEntrance(
    index: Int,
    riseDistance: Dp = 10.dp
): Modifier = composed {
    val isReducedMotion = LocalReducedMotion.current
    val density = LocalDensity.current
    val risePx = with(density) { riseDistance.toPx() }

    // Non-uniform shrinking stagger delays (0, 45, 80, 110, 135, 155ms)
    val delayMs = remember(index) {
        when (index.coerceAtLeast(0)) {
            0 -> 0L
            1 -> 45L
            2 -> 80L
            3 -> 110L
            4 -> 135L
            else -> 155L
        }
    }

    var visible by remember { mutableStateOf(delayMs == 0L) }
    LaunchedEffect(delayMs) {
        if (!visible) {
            delay(delayMs)
            visible = true
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (isReducedMotion) 140 else EchoMotion.DurationStandard,
            easing = EchoMotion.EaseOutStrong
        ),
        label = "stagger_alpha"
    )

    val translationY by animateFloatAsState(
        targetValue = if (visible || isReducedMotion) 0f else risePx,
        animationSpec = tween(
            durationMillis = EchoMotion.DurationStandard,
            easing = EchoMotion.EaseOutStrong
        ),
        label = "stagger_y"
    )

    this.graphicsLayer {
        this.alpha = alpha
        this.translationY = translationY
    }
}
