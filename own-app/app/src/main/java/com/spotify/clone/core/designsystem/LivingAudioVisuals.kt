package com.spotify.clone.core.designsystem

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spotify.clone.core.model.MusicGenre
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Reverse-engineered from Spotify's JellyfishView (com.spotify.endless.elements.jellyfish):
 * Multi-layered organic audio-reactive ambient orb aura that breathes and drifts
 * smoothly while audio is playing, creating a living listening atmosphere.
 */
@Composable
fun SpotifyJellyfishAura(
    artworkColors: ArtworkColors,
    isPlaying: Boolean,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier,
    intensity: Float = 1f
) {
    val isReducedMotion = LocalReducedMotion.current
    val staticZero = remember { mutableFloatStateOf(0f) }
    val staticOne = remember { mutableFloatStateOf(1f) }
    val phaseState: State<Float>
    val pulseState: State<Float>
    if (!isReducedMotion && isPlaying) {
        val transition = rememberInfiniteTransition(label = "jellyfish_aura")
        phaseState = transition.animateFloat(
            initialValue = 0f,
            targetValue = (2f * PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 12_000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "jellyfish_phase"
        )
        pulseState = transition.animateFloat(
            initialValue = 0.92f,
            targetValue = 1.12f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "jellyfish_pulse"
        )
    } else {
        phaseState = staticZero
        pulseState = staticOne
    }
    val activeMixState = animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.35f,
        animationSpec = tween(520, easing = EchoMotion.EncoreBoxEase),
        label = "jellyfish_active_mix"
    )

    val primaryBlobColorState = animateColorAsState(
        targetValue = artworkColors.vibrantAccent,
        animationSpec = tween(600),
        label = "jellyfish_primary"
    )
    val secondaryBlobColorState = animateColorAsState(
        targetValue = artworkColors.ambientGlow,
        animationSpec = tween(600),
        label = "jellyfish_secondary"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        val phase = phaseState.value
        val pulse = pulseState.value
        val activeMix = activeMixState.value
        val primaryBlobColor = primaryBlobColorState.value
        val secondaryBlobColor = secondaryBlobColorState.value

        val animFactor = if (isReducedMotion) 0f else activeMix
        val scale = if (isReducedMotion) 1f else (1f + (pulse - 1f) * animFactor)
        val baseAlpha = (if (isDarkMode) 0.28f else 0.13f) * activeMix * intensity

        val c1 = Offset(
            x = w * (0.48f + 0.14f * cos(phase) * animFactor),
            y = h * (0.36f + 0.10f * sin(phase * 1.3f) * animFactor)
        )
        val r1 = (w.coerceAtLeast(h) * 0.56f) * scale
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primaryBlobColor.copy(alpha = baseAlpha), Color.Transparent),
                center = c1,
                radius = r1
            ),
            radius = r1,
            center = c1
        )

        val c2 = Offset(
            x = w * (0.54f + 0.16f * sin(phase + 1.8f) * animFactor),
            y = h * (0.44f + 0.12f * cos(phase * 0.9f) * animFactor)
        )
        val r2 = (w.coerceAtLeast(h) * 0.48f) * (1.04f - (scale - 1f) * 0.5f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(secondaryBlobColor.copy(alpha = baseAlpha * 0.85f), Color.Transparent),
                center = c2,
                radius = r2
            ),
            radius = r2,
            center = c2
        )
    }
}

/**
 * GPU-accelerated multi-bar harmonic spectrum visualizer driven by playback state & genre energy.
 * Gives cards, mini-player, and Now Playing a live studio telemetry pulse with zero layout thrashing.
 */
@Composable
fun LiveAudioSpectrum(
    isPlaying: Boolean,
    barColor: Color,
    modifier: Modifier = Modifier,
    barCount: Int = 5,
    genre: MusicGenre = MusicGenre.ALL,
    barWidth: Dp = 3.dp,
    barSpacing: Dp = 2.5.dp,
    height: Dp = 18.dp
) {
    val isReducedMotion = LocalReducedMotion.current
    val staticZero = remember { mutableFloatStateOf(0f) }
    val phaseState: State<Float> = if (!isReducedMotion && isPlaying) {
        val transition = rememberInfiniteTransition(label = "live_spectrum")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = (2f * PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1150, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "spectrum_phase"
        )
    } else {
        staticZero
    }
    val energyState = animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = tween(220, easing = EchoMotion.EncoreButtonEase),
        label = "spectrum_energy"
    )

    val speedFactor = remember(genre) {
        when (genre) {
            MusicGenre.MASS_BEATS, MusicGenre.ELECTRONIC, MusicGenre.HIP_HOP -> 1.45f
            MusicGenre.ROCK, MusicGenre.GLOBAL_POP -> 1.2f
            MusicGenre.LO_FI, MusicGenre.MELODIES, MusicGenre.DEVOTIONAL -> 0.85f
            else -> 1.05f
        }
    }

    val totalWidth = (barWidth * barCount) + (barSpacing * (barCount - 1).coerceAtLeast(0))

    Canvas(
        modifier = modifier
            .width(totalWidth)
            .height(height)
    ) {
        val phase = phaseState.value
        val energy = energyState.value
        val bw = barWidth.toPx()
        val gap = barSpacing.toPx()
        val maxH = size.height
        val minH = (maxH * 0.22f).coerceAtLeast(2.dp.toPx())

        for (i in 0 until barCount) {
            val harmonic = if (isReducedMotion) {
                0.55f
            } else {
                val a = sin(phase * speedFactor + i * 1.15f)
                val b = cos(phase * speedFactor * 1.7f - i * 0.85f)
                ((a * 0.55f + b * 0.45f) * 0.5f + 0.5f).coerceIn(0.15f, 1f)
            }
            val barH = minH + (maxH - minH) * harmonic * energy
            val x = i * (bw + gap)
            val y = maxH - barH
            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, y),
                size = Size(bw, barH),
                cornerRadius = CornerRadius(bw / 2f, bw / 2f)
            )
        }
    }
}

/**
 * Reverse-engineered from Spotify's Encore MarqueeTextView:
 * Smoothly scrolls long track titles or artist credits horizontally when active,
 * eliminating truncated text while staying calm under Reduced Motion.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EchoMarqueeText(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    fontWeight: FontWeight? = null,
    enabled: Boolean = true
) {
    val isReducedMotion = LocalReducedMotion.current
    val marqueeModifier = if (enabled && !isReducedMotion) {
        modifier
            .clipToBounds()
            .basicMarquee(
                iterations = Int.MAX_VALUE,
                repeatDelayMillis = 1800,
                initialDelayMillis = 1200,
                velocity = 34.dp
            )
    } else {
        modifier
    }

    Text(
        text = text,
        color = color,
        style = style,
        fontWeight = fontWeight ?: style.fontWeight,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = marqueeModifier
    )
}

/**
 * Living Harmonic Sine-Wave Audio Scrubber:
 * - Ripples gently like live audio waves when playing
 * - Flattens smoothly when paused
 * - Expands stroke & glowing playhead under touch while scrubbing
 * - 1:1 finger tracking across full width
 */
@Composable
fun WavyAudioScrubber(
    progress: Float,
    isPlaying: Boolean,
    isScrubbing: Boolean,
    activeColor: Color,
    trackBgColor: Color,
    onScrubStart: (Float) -> Unit,
    onScrubMove: (Float) -> Unit,
    onScrubEnd: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val isReducedMotion = LocalReducedMotion.current
    val currentOnScrubStart by rememberUpdatedState(onScrubStart)
    val currentOnScrubMove by rememberUpdatedState(onScrubMove)
    val currentOnScrubEnd by rememberUpdatedState(onScrubEnd)

    val staticZero = remember { mutableFloatStateOf(0f) }
    val wavePhaseState: State<Float> = if (!isReducedMotion && isPlaying) {
        val transition = rememberInfiniteTransition(label = "wavy_scrubber")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = (2f * PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1600, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "wave_phase"
        )
    } else {
        staticZero
    }

    val waveAmpMixState = animateFloatAsState(
        targetValue = when {
            isReducedMotion -> 0f
            isScrubbing -> 0.25f
            isPlaying -> 1f
            else -> 0f
        },
        animationSpec = tween(260, easing = EchoMotion.EncoreBoxEase),
        label = "wave_amp_mix"
    )
    val thumbRadiusDpState = animateDpAsState(
        targetValue = if (isScrubbing) 8.5.dp else 5.5.dp,
        animationSpec = EchoMotion.springMomentum(),
        label = "scrubber_thumb_r"
    )
    val strokeWidthDpState = animateDpAsState(
        targetValue = if (isScrubbing) 5.dp else 3.2.dp,
        animationSpec = EchoMotion.springMomentum(),
        label = "scrubber_stroke"
    )

    var latestProgress by remember { mutableFloatStateOf(progress) }
    if (!isScrubbing) {
        latestProgress = progress
    }

    val clamped = progress.coerceIn(0f, 1f)
    val wavePath = remember { Path() }
    val density = LocalDensity.current
    val idleStroke = remember(density) {
        Stroke(
            width = with(density) { 3.2.dp.toPx() },
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(34.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val width = size.width.toFloat().coerceAtLeast(1f)
                    val p = (offset.x / width).coerceIn(0f, 1f)
                    currentOnScrubStart(p)
                    currentOnScrubEnd(p)
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        val width = size.width.toFloat().coerceAtLeast(1f)
                        val p = (offset.x / width).coerceIn(0f, 1f)
                        latestProgress = p
                        currentOnScrubStart(p)
                    },
                    onDragCancel = { currentOnScrubEnd(latestProgress) },
                    onDragEnd = { currentOnScrubEnd(latestProgress) },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        val width = size.width.toFloat().coerceAtLeast(1f)
                        val p = (change.position.x / width).coerceIn(0f, 1f)
                        latestProgress = p
                        currentOnScrubMove(p)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val cy = size.height / 2f
            val wavePhase = wavePhaseState.value
            val waveAmpMix = waveAmpMixState.value
            val strokePx = strokeWidthDpState.value.toPx()
            val thumbPx = thumbRadiusDpState.value.toPx()
            val activeEndX = (w * clamped).coerceIn(0f, w)

            // Inactive background track
            drawLine(
                color = trackBgColor,
                start = Offset(activeEndX, cy),
                end = Offset(w, cy),
                strokeWidth = strokePx * 0.85f,
                cap = StrokeCap.Round
            )

            // Active harmonic waveform path
            if (activeEndX > 1f) {
                val maxAmpPx = 3.2.dp.toPx() * waveAmpMix
                if (maxAmpPx < 0.25f) {
                    drawLine(
                        color = activeColor,
                        start = Offset(0f, cy),
                        end = Offset(activeEndX, cy),
                        strokeWidth = strokePx,
                        cap = StrokeCap.Round
                    )
                } else {
                    wavePath.reset()
                    val step = 4.dp.toPx().coerceAtLeast(3f)
                    val wavelength = 24.dp.toPx().coerceAtLeast(12f)
                    var x = 0f
                    wavePath.moveTo(0f, cy + sin(wavePhase) * maxAmpPx * 0.3f)
                    while (x <= activeEndX) {
                        val edgeFade = ((x / 20f).coerceIn(0f, 1f)) * (((activeEndX - x) / 14f).coerceIn(0f, 1f))
                        val y = cy + sin((x / wavelength) * 2f * PI.toFloat() - wavePhase) * maxAmpPx * edgeFade
                        wavePath.lineTo(x, y)
                        x += step
                    }
                    wavePath.lineTo(activeEndX, cy)
                    val strokeStyle = if (abs(strokePx - idleStroke.width) < 0.5f) {
                        idleStroke
                    } else {
                        Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    }
                    drawPath(
                        path = wavePath,
                        color = activeColor,
                        style = strokeStyle
                    )
                }
            }

            // Soft glow halo around playhead thumb while scrubbing or playing
            if (isScrubbing || isPlaying) {
                drawCircle(
                    color = activeColor.copy(alpha = if (isScrubbing) 0.28f else 0.14f),
                    radius = thumbPx * (if (isScrubbing) 2.1f else 1.7f),
                    center = Offset(activeEndX, cy)
                )
            }

            // Playhead thumb
            drawCircle(
                color = activeColor,
                radius = thumbPx,
                center = Offset(activeEndX, cy)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.9f),
                radius = thumbPx * 0.38f,
                center = Offset(activeEndX, cy)
            )
        }
    }
}

/**
 * Spotify-style Swipe-to-Queue & Swipe-to-Like Track Row Wrapper:
 * - Swipe right past threshold -> "Play Next" with spring snap & haptic detent
 * - Swipe left past threshold -> Toggle Favorite ("Like" / "Liked") with spring snap & haptic detent
 * Vertical scrolls pass cleanly to parent LazyColumn.
 */
@Composable
fun SwipeableTrackRow(
    isFavorite: Boolean,
    onSwipeQueue: () -> Unit,
    onSwipeFavorite: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val activeTheme = LocalAppTheme.current
    val isReducedMotion = LocalReducedMotion.current
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val currentOnSwipeQueue by rememberUpdatedState(onSwipeQueue)
    val currentOnSwipeFavorite by rememberUpdatedState(onSwipeFavorite)

    val triggerPx = with(density) { 74.dp.toPx() }
    val maxDragPx = with(density) { 104.dp.toPx() }
    val offsetX = remember { Animatable(0f) }
    var crossedTrigger by remember { mutableStateOf(false) }

    val queueRevealAlpha = (offsetX.value / triggerPx).coerceIn(0f, 1f)
    val likeRevealAlpha = (-offsetX.value / triggerPx).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
    ) {
        // Swipe Right Background -> Play Next (M3 Expressive primaryContainer)
        if (offsetX.value > 2f) {
            Row(
                modifier = Modifier
                    .matchParentSize()
                    .background(androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f + 0.50f * queueRevealAlpha))
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                    contentDescription = "Play Next",
                    tint = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer {
                            val s = 0.85f + 0.25f * queueRevealAlpha
                            scaleX = s
                            scaleY = s
                        }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (queueRevealAlpha >= 0.98f) "Release to Play Next" else "Play Next",
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.graphicsLayer { alpha = queueRevealAlpha }
                )
            }
        }

        // Swipe Left Background -> Favorite (M3 Expressive tertiaryContainer)
        if (onSwipeFavorite != null && offsetX.value < -2f) {
            Row(
                modifier = Modifier
                    .matchParentSize()
                    .background(androidx.compose.material3.MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f + 0.50f * likeRevealAlpha))
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = if (isFavorite) "Unlike" else "Like",
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onTertiaryContainer,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.graphicsLayer { alpha = likeRevealAlpha }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.FavoriteBorder else Icons.Filled.Favorite,
                    contentDescription = "Favorite",
                    tint = androidx.compose.material3.MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer {
                            val s = 0.85f + 0.25f * likeRevealAlpha
                            scaleX = s
                            scaleY = s
                        }
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(onSwipeFavorite != null) {
                    detectHorizontalDragGestures(
                        onDragStart = { crossedTrigger = false },
                        onDragCancel = {
                            crossedTrigger = false
                            scope.launch { offsetX.animateTo(0f, EchoMotion.SpringMomentum) }
                        },
                        onDragEnd = {
                            val finalX = offsetX.value
                            val favAction = currentOnSwipeFavorite
                            when {
                                finalX >= triggerPx -> {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    currentOnSwipeQueue()
                                }
                                favAction != null && finalX <= -triggerPx -> {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    favAction()
                                }
                            }
                            crossedTrigger = false
                            scope.launch { offsetX.animateTo(0f, EchoMotion.SpringMomentum) }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            scope.launch {
                                val raw = offsetX.value + dragAmount
                                val next = when {
                                    raw > 0f -> EchoMotion.rubberBand(raw, maxDragPx, 0.82f)
                                    onSwipeFavorite != null && raw < 0f -> -EchoMotion.rubberBand(abs(raw), maxDragPx, 0.82f)
                                    else -> 0f
                                }
                                offsetX.snapTo(if (isReducedMotion) next.coerceIn(-triggerPx, triggerPx) else next)
                                val nowCrossed = abs(offsetX.value) >= triggerPx
                                if (nowCrossed && !crossedTrigger) {
                                    crossedTrigger = true
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                } else if (!nowCrossed) {
                                    crossedTrigger = false
                                }
                            }
                        }
                    )
                }
        ) {
            content()
        }
    }
}
