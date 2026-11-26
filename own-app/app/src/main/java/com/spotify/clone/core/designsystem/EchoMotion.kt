package com.spotify.clone.core.designsystem

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
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
 * ECHO — Material 3 Expressive Motion Architecture
 * Implements M3 Expressive's Spatial vs. Effects spring physics system:
 * - Spatial springs: lively, bouncy physical motion (dampingRatio 0.72–0.82) for position, scale, and shape morphing
 * - Effects springs: critically damped (dampingRatio 1.0) for opacity, color, and elevation without overshoot
 */
object EchoMotion {
    // Timing Tokens (in ms)
    const val DurationPress = 95
    const val DurationMicro = 160
    const val DurationStandard = 260
    const val DurationExpressive = 380
    const val DurationMajor = 480

    // M3 Expressive Easing & Legacy Compatibility
    val EaseOutStrong = CubicBezierEasing(0.20f, 1.0f, 0.30f, 1.0f)
    val EaseInOutStrong = CubicBezierEasing(0.77f, 0.0f, 0.175f, 1.0f)
    val EaseDrawer = CubicBezierEasing(0.32f, 0.72f, 0.0f, 1.0f)

    val EncoreButtonEase = CubicBezierEasing(0.3f, 0.0f, 0.0f, 1.0f)
    val EncoreBoxEase = CubicBezierEasing(0.3f, 0.0f, 0.4f, 1.0f)
    val HeaderMotionEase = CubicBezierEasing(0.6f, 0.0f, 0.7f, 1.0f)

    val EaseEmphasized = EaseOutStrong
    val EaseStandard = FastOutSlowInEasing
    val EaseDecelerate = LinearOutSlowInEasing

    // M3 Expressive Spatial Springs (with physical overshoot)
    val SpatialFast = spring<Float>(dampingRatio = 0.72f, stiffness = 700f)
    val SpatialDefault = spring<Float>(dampingRatio = 0.76f, stiffness = 380f)
    val SpatialSlow = spring<Float>(dampingRatio = 0.80f, stiffness = 200f)

    // M3 Expressive Effects Springs (critically damped, zero overshoot)
    val EffectsFast = spring<Float>(dampingRatio = 1.0f, stiffness = 800f)
    val EffectsDefault = spring<Float>(dampingRatio = 1.0f, stiffness = 420f)

    // Retained Spring Aliases backed by M3 Expressive Physics
    val SpringCriticallyDamped = EffectsDefault
    val SpringMomentum = SpatialDefault
    val SpringBouncy = SpatialFast
    val SpringGentle = SpatialSlow
    val SpringSnappy = SpatialFast

    fun <T> spatialFast() = spring<T>(dampingRatio = 0.72f, stiffness = 700f)
    fun <T> spatialDefault() = spring<T>(dampingRatio = 0.76f, stiffness = 380f)
    fun <T> spatialSlow() = spring<T>(dampingRatio = 0.80f, stiffness = 200f)
    fun <T> effectsDefault() = spring<T>(dampingRatio = 1.0f, stiffness = 420f)

    fun <T> springCriticallyDamped() = effectsDefault<T>()
    fun <T> springMomentum() = spatialDefault<T>()
    fun <T> springBouncy() = spatialFast<T>()
    fun <T> springGentle() = spatialSlow<T>()
    fun <T> springSnappy() = spatialFast<T>()

    // Standard Tween Specs
    fun <T> tweenMicro() = tween<T>(DurationMicro, easing = EaseOutStrong)
    fun <T> tweenStandard() = tween<T>(DurationStandard, easing = EaseOutStrong)
    fun <T> tweenExpressive() = tween<T>(DurationExpressive, easing = EaseDrawer)
    fun <T> tweenMajor() = tween<T>(DurationMajor, easing = EaseDrawer)

    fun projectMomentum(velocityPxPerSec: Float, decelerationRate: Float = 0.996f): Float {
        return (velocityPxPerSec / 1000f) * decelerationRate / (1f - decelerationRate)
    }

    fun rubberBand(overshoot: Float, dimension: Float, constant: Float = 0.55f): Float {
        if (dimension <= 0f) return 0f
        return (overshoot * dimension * constant) / (dimension + constant * abs(overshoot))
    }
}

val LocalReducedMotion = compositionLocalOf { false }

/**
 * Material 3 Expressive physical press response with SpatialDefault spring overshoot on release.
 */
fun Modifier.echoPressable(
    scaleOnPress: Float = 0.96f,
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
            EchoMotion.SpatialDefault
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
 * Staggered expressive entrance using spring translation and crisp opacity reveal.
 */
fun Modifier.echoStaggeredEntrance(
    index: Int,
    riseDistance: Dp = 14.dp
): Modifier = composed {
    val isReducedMotion = LocalReducedMotion.current
    val density = LocalDensity.current
    val risePx = with(density) { riseDistance.toPx() }

    val delayMs = remember(index) {
        when (index.coerceAtLeast(0)) {
            0 -> 0L
            1 -> 40L
            2 -> 75L
            3 -> 105L
            4 -> 130L
            else -> 150L
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
        animationSpec = if (isReducedMotion) tween(140) else EchoMotion.SpatialDefault,
        label = "stagger_y"
    )

    this.graphicsLayer {
        this.alpha = alpha
        this.translationY = translationY
    }
}
