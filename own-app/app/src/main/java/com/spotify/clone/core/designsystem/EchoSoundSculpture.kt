package com.spotify.clone.core.designsystem

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * ECHO Living Sound Sculpture
 * A motion-first abstract organic sound sculpture that breathes, morphs,
 * and radiates soft lavender (#A79BFF), coral (#FF9C88), and soft blue (#A8C9FF).
 *
 * Behavior:
 * - Playing: Gentle organic contour movement.
 * - Paused: Settles smoothly into calm resting state.
 * - Track changed: Organic morphing transition.
 * - Touch: Subtle ambient harmonic reaction.
 * - Reduced motion: High-fidelity resting contours with zero draw animation.
 */
@Composable
fun EchoSoundSculpture(
    isPlaying: Boolean,
    trackKey: String = "",
    modifier: Modifier = Modifier,
    alpha: Float = 0.85f
) {
    val isReducedMotion = LocalReducedMotion.current
    val coroutineScope = rememberCoroutineScope()

    // Touch impulse ripple
    val touchImpulse = remember { Animatable(0f) }

    // Settling multiplier between playing and paused states
    val motionIntensity by animateFloatAsState(
        targetValue = if (isPlaying && !isReducedMotion) 1f else 0.15f,
        animationSpec = tween(durationMillis = 800, easing = EchoMotion.EaseStandard),
        label = "sculpture_intensity"
    )

    // Track transition morph offset
    var trackMorphPhase by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(trackKey) {
        if (!isReducedMotion && trackKey.isNotEmpty()) {
            trackMorphPhase += 0.85f
        }
    }

    // Continuous ambient phase
    val infiniteTransition = rememberInfiniteTransition(label = "sculpture_ambient")
    val ambientPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ambient_phase"
    )

    val effectivePhase = if (isReducedMotion) 0.5f else (ambientPhase + trackMorphPhase)

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(isReducedMotion) {
                if (!isReducedMotion) {
                    detectTapGestures(
                        onTap = {
                            coroutineScope.launch {
                                touchImpulse.snapTo(1f)
                                touchImpulse.animateTo(
                                    targetValue = 0f,
                                    animationSpec = tween(durationMillis = 650, easing = EchoMotion.EaseDecelerate)
                                )
                            }
                        }
                    )
                }
            }
    ) {
        val width = size.width
        val height = size.height
        val center = Offset(width / 2f, height / 2f)
        val baseRadius = (minOf(width, height) * 0.38f).coerceAtLeast(10f)

        val impulse = touchImpulse.value * 0.18f

        // Layer 1: Ethereal Soft Blue Outer Contour
        drawOrganicLayer(
            center = center,
            radius = baseRadius * 1.25f,
            phase = effectivePhase * 0.7f,
            intensity = motionIntensity + impulse,
            color = EchoSoftBlue.copy(alpha = 0.22f * alpha),
            fillColor = EchoSoftBlue.copy(alpha = 0.08f * alpha),
            points = 6,
            waviness = 0.12f
        )

        // Layer 2: Signature Warm Coral Mid-Layer
        drawOrganicLayer(
            center = center,
            radius = baseRadius * 1.05f,
            phase = -effectivePhase * 0.9f + 1.2f,
            intensity = motionIntensity + impulse,
            color = EchoCoral.copy(alpha = 0.35f * alpha),
            fillColor = EchoCoral.copy(alpha = 0.12f * alpha),
            points = 5,
            waviness = 0.16f
        )

        // Layer 3: Vibrant Lavender Inner Harmonic Core
        drawOrganicLayer(
            center = center,
            radius = baseRadius * 0.82f,
            phase = effectivePhase * 1.2f + 2.4f,
            intensity = motionIntensity + impulse,
            color = EchoLavender.copy(alpha = 0.55f * alpha),
            fillColor = EchoLavender.copy(alpha = 0.20f * alpha),
            points = 7,
            waviness = 0.14f
        )
    }
}

private fun DrawScope.drawOrganicLayer(
    center: Offset,
    radius: Float,
    phase: Float,
    intensity: Float,
    color: Color,
    fillColor: Color,
    points: Int,
    waviness: Float
) {
    val path = Path()
    val angleStep = (2f * PI / points).toFloat()

    val coords = mutableListOf<Offset>()
    for (i in 0 until points) {
        val angle = i * angleStep
        val wave = sin(angle * 2.5f + phase) * waviness * intensity
        val r = radius * (1f + wave)
        val x = center.x + r * cos(angle)
        val y = center.y + r * sin(angle)
        coords.add(Offset(x, y))
    }

    if (coords.isNotEmpty()) {
        path.moveTo(
            (coords[0].x + coords.last().x) / 2f,
            (coords[0].y + coords.last().y) / 2f
        )
        for (i in coords.indices) {
            val curr = coords[i]
            val next = coords[(i + 1) % coords.size]
            val midX = (curr.x + next.x) / 2f
            val midY = (curr.y + next.y) / 2f
            path.quadraticBezierTo(curr.x, curr.y, midX, midY)
        }
        path.close()

        // Draw soft ambient fill
        drawPath(path = path, color = fillColor)

        // Draw sleek organic contour stroke
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}
