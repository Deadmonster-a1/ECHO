package com.spotify.clone.core.designsystem

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Material 3 Expressive morphing scalloped polygon loading indicator.
 * Smoothly rotates and breathes between a rounded 6-lobe flower and 4-lobe clover.
 */
@Composable
fun ExpressiveLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer
) {
    val isReducedMotion = LocalReducedMotion.current
    val staticAngle = remember { mutableFloatStateOf(0f) }
    val staticMorph = remember { mutableFloatStateOf(0.5f) }

    val angleState: State<Float>
    val morphState: State<Float>
    if (!isReducedMotion) {
        val transition = rememberInfiniteTransition(label = "m3_expressive_loader")
        angleState = transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2800, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "loader_rot"
        )
        morphState = transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "loader_morph"
        )
    } else {
        angleState = staticAngle
        morphState = staticMorph
    }

    val shapePath = remember { Path() }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(containerColor.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.68f)) {
            val w = this.size.width
            val h = this.size.height
            val cx = w / 2f
            val cy = h / 2f
            val baseR = minOf(w, h) * 0.42f
            val morph = morphState.value
            val lobes = 6
            val steps = 72

            shapePath.reset()
            for (i in 0..steps) {
                val theta = (i.toFloat() / steps) * (2f * PI.toFloat())
                val wave1 = cos(lobes * theta) * (0.14f + 0.08f * morph)
                val wave2 = cos((lobes - 2) * theta) * (0.06f * (1f - morph))
                val r = baseR * (1f + wave1 + wave2)
                val x = cx + r * cos(theta)
                val y = cy + r * sin(theta)
                if (i == 0) shapePath.moveTo(x, y) else shapePath.lineTo(x, y)
            }
            shapePath.close()

            rotate(degrees = angleState.value, pivot = Offset(cx, cy)) {
                drawPath(path = shapePath, color = color)
            }
        }
    }
}

/**
 * Material 3 Expressive continuous wavy progress bar for downloads, updates, and live streams.
 */
@Composable
fun ExpressiveWavyProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    strokeWidth: Dp = 4.dp,
    waveAmplitude: Dp = 2.6.dp,
    wavelength: Dp = 22.dp,
    animateWave: Boolean = true
) {
    val isReducedMotion = LocalReducedMotion.current
    val staticZero = remember { mutableFloatStateOf(0f) }
    val wavePhaseState: State<Float> = if (animateWave && !isReducedMotion) {
        val transition = rememberInfiniteTransition(label = "m3_wavy_progress")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = (2f * PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1300, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "m3_wavy_phase"
        )
    } else {
        staticZero
    }

    val wavePath = remember { Path() }
    val clamped = progress.coerceIn(0f, 1f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(14.dp)
    ) {
        val w = size.width
        val cy = size.height / 2f
        val strokePx = strokeWidth.toPx()
        val ampPx = if (isReducedMotion) 0f else waveAmplitude.toPx()
        val lambdaPx = wavelength.toPx().coerceAtLeast(10f)
        val activeEnd = (w * clamped).coerceIn(0f, w)
        val stopGap = strokePx * 1.3f

        // Inactive rounded track segment + Material 3 Expressive end-stop dot
        if (activeEnd + stopGap < w) {
            drawLine(
                color = trackColor,
                start = Offset((activeEnd + stopGap).coerceAtMost(w), cy),
                end = Offset(w, cy),
                strokeWidth = strokePx,
                cap = StrokeCap.Round
            )
            drawCircle(
                color = color,
                radius = strokePx * 0.55f,
                center = Offset(w - strokePx * 0.6f, cy)
            )
        }

        if (activeEnd > 1f) {
            if (ampPx < 0.3f) {
                drawLine(
                    color = color,
                    start = Offset(0f, cy),
                    end = Offset(activeEnd, cy),
                    strokeWidth = strokePx,
                    cap = StrokeCap.Round
                )
            } else {
                wavePath.reset()
                val step = 3.dp.toPx().coerceAtLeast(2f)
                var x = 0f
                wavePath.moveTo(0f, cy)
                while (x <= activeEnd) {
                    val edgeFade = ((x / 16f).coerceIn(0f, 1f)) * (((activeEnd - x) / 12f).coerceIn(0f, 1f))
                    val y = cy + sin((x / lambdaPx) * 2f * PI.toFloat() - wavePhaseState.value) * ampPx * edgeFade
                    wavePath.lineTo(x, y)
                    x += step
                }
                wavePath.lineTo(activeEnd, cy)
                drawPath(
                    path = wavePath,
                    color = color,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }
    }
}

/**
 * GPU-accelerated live equalizer bars using scaleY on graphicsLayer.
 */
@Composable
fun LivingEqualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 3,
    barColor: Color = MaterialTheme.colorScheme.primary,
    barWidth: Dp = 3.dp,
    barSpacing: Dp = 2.dp,
    maxHeight: Dp = 15.dp,
    minHeight: Dp = 3.dp
) {
    val isReducedMotion = LocalReducedMotion.current
    val pausedFraction = (minHeight / maxHeight).coerceIn(0.18f, 0.35f)
    val playMixState = animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = EchoMotion.tweenMicro(),
        label = "eq_play_mix"
    )
    val staticFracs = remember {
        listOf(
            mutableFloatStateOf(0.55f),
            mutableFloatStateOf(0.75f),
            mutableFloatStateOf(0.45f)
        )
    }
    val fractionStates: List<State<Float>> = if (!isReducedMotion && isPlaying) {
        val infiniteTransition = rememberInfiniteTransition(label = "living_eq")
        val p0 = infiniteTransition.animateFloat(
            initialValue = 0.28f,
            targetValue = 0.96f,
            animationSpec = infiniteRepeatable(
                animation = tween(380, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "eq_0"
        )
        val p1 = infiniteTransition.animateFloat(
            initialValue = 0.88f,
            targetValue = 0.24f,
            animationSpec = infiniteRepeatable(
                animation = tween(450, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "eq_1"
        )
        val p2 = infiniteTransition.animateFloat(
            initialValue = 0.36f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(320, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "eq_2"
        )
        listOf(p0, p1, p2)
    } else {
        staticFracs
    }

    Row(
        modifier = modifier.height(maxHeight),
        horizontalArrangement = Arrangement.spacedBy(barSpacing),
        verticalAlignment = Alignment.Bottom
    ) {
        repeat(barCount) { idx ->
            val fracState = fractionStates[idx % fractionStates.size]
            Box(
                modifier = Modifier
                    .width(barWidth)
                    .height(maxHeight)
                    .graphicsLayer {
                        val liveFrac = if (isReducedMotion) 0.65f else fracState.value
                        val targetFrac = pausedFraction + (liveFrac - pausedFraction) * playMixState.value
                        scaleY = targetFrac.coerceIn(0.15f, 1f)
                        transformOrigin = TransformOrigin(0.5f, 1f)
                    }
                    .clip(RoundedCornerShape(50))
                    .background(barColor)
            )
        }
    }
}

/**
 * Legacy helper retained for compatibility; maps to a Material 3 Expressive tonal highlight stroke.
 */
@Composable
fun Modifier.echoSpecularBorder(
    isDarkMode: Boolean,
    shape: Shape,
    fallbackBorder: Color
): Modifier {
    val strokeColor = if (isDarkMode) Color.White.copy(alpha = 0.08f) else fallbackBorder.copy(alpha = 0.32f)
    return this.border(0.75.dp, strokeColor, shape)
}

/**
 * Material 3 Expressive elevated tonal container surface for floating chrome (dock, mini-player, pills).
 */
@Composable
fun Modifier.echoGlassSurface(
    isDarkMode: Boolean,
    shape: Shape,
    fallbackBorder: Color,
    tintColor: Color? = null,
    elevation: Dp = 10.dp
): Modifier {
    val containerBase = MaterialTheme.colorScheme.surfaceContainerHighest
    val tinted = tintColor?.let {
        Brush.horizontalGradient(
            listOf(
                it.copy(alpha = if (isDarkMode) 0.16f else 0.10f),
                Color.Transparent
            )
        )
    }
    return this
        .shadow(
            elevation = elevation,
            shape = shape,
            ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = if (isDarkMode) 0.22f else 0.12f),
            spotColor = Color.Black.copy(alpha = if (isDarkMode) 0.42f else 0.16f)
        )
        .clip(shape)
        .background(containerBase)
        .then(if (tinted != null) Modifier.background(tinted) else Modifier)
        .border(0.75.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f), shape)
}

/**
 * Standard >= 48dp Material 3 Expressive tonal icon button with spring press physics.
 */
@Composable
fun EchoIconCircleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    floatingGlass: Boolean = false,
    size: Dp = 44.dp,
    containerColor: Color? = null,
    content: @Composable () -> Unit
) {
    val fill = containerColor ?: MaterialTheme.colorScheme.surfaceContainerHigh
    val shape = CircleShape

    Box(
        modifier = modifier
            .size(size)
            .then(
                if (floatingGlass) {
                    Modifier
                        .shadow(6.dp, shape, spotColor = Color.Black.copy(alpha = 0.18f))
                        .clip(shape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                } else {
                    Modifier
                        .clip(shape)
                        .background(fill)
                }
            )
            .echoPressable(scaleOnPress = 0.90f, hapticOnClick = true, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * Material 3 Expressive shape-morphing Play/Pause FAB:
 * Morphs smoothly between a bold rounded-squircle (18.dp) when paused and a full circle when playing,
 * paired with a soft ambient aura.
 */
@Composable
fun EchoPlayFab(
    isPlaying: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary
) {
    val isReducedMotion = LocalReducedMotion.current
    val cornerRadius by animateDpAsState(
        targetValue = if (isPlaying) size / 2 else 18.dp,
        animationSpec = if (isReducedMotion) tween(120) else EchoMotion.spatialDefault(),
        label = "m3_fab_morph"
    )
    val shape = RoundedCornerShape(cornerRadius)

    val staticOne = remember { mutableFloatStateOf(1.0f) }
    val pulseScaleState: State<Float> = if (isPlaying && !isReducedMotion) {
        val infiniteTransition = rememberInfiniteTransition(label = "fab_pulse")
        infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(1400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "fab_aura_scale"
        )
    } else {
        staticOne
    }
    val auraAlphaState = animateFloatAsState(
        targetValue = if (isPlaying && !isReducedMotion) 0.24f else 0f,
        animationSpec = EchoMotion.tweenStandard(),
        label = "fab_aura_alpha"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        if (!isReducedMotion) {
            Box(
                modifier = Modifier
                    .size(size)
                    .graphicsLayer {
                        val s = pulseScaleState.value
                        scaleX = s
                        scaleY = s
                        alpha = auraAlphaState.value
                    }
                    .clip(shape)
                    .background(containerColor)
            )
        }
        Box(
            modifier = Modifier
                .size(size)
                .shadow(
                    elevation = if (isPlaying) 14.dp else 8.dp,
                    shape = shape,
                    ambientColor = containerColor.copy(alpha = 0.28f),
                    spotColor = containerColor.copy(alpha = 0.36f)
                )
                .clip(shape)
                .background(containerColor)
                .echoPressable(scaleOnPress = 0.91f, hapticOnClick = true, onClick = onToggle),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = isPlaying,
                transitionSpec = {
                    (fadeIn(tween(150, easing = EchoMotion.EaseOutStrong)) +
                        scaleIn(initialScale = 0.80f, animationSpec = EchoMotion.spatialFast())) togetherWith
                        (fadeOut(tween(100)) + scaleOut(targetScale = 0.80f, animationSpec = tween(100)))
                },
                label = "play_fab_icon"
            ) { playing ->
                Icon(
                    imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (playing) "Pause" else "Play",
                    tint = contentColor,
                    modifier = Modifier.size(size * 0.48f)
                )
            }
        }
    }
}

@Composable
fun EchoHeartButton(
    isFavorite: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp
) {
    val isReducedMotion = LocalReducedMotion.current
    val haptic = LocalHapticFeedback.current
    var showPulse by remember { mutableStateOf(false) }

    LaunchedEffect(isFavorite) {
        if (isFavorite && !isReducedMotion) {
            showPulse = true
            delay(280)
            showPulse = false
        } else {
            showPulse = false
        }
    }

    val activeHeartColor = MaterialTheme.colorScheme.tertiary
    val tint by animateColorAsState(
        targetValue = if (isFavorite) activeHeartColor else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(180, easing = EchoMotion.EaseOutStrong),
        label = "heart_tint"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isFavorite) {
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.85f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f)
        },
        animationSpec = tween(180),
        label = "heart_bg"
    )
    val scale by animateFloatAsState(
        targetValue = when {
            isReducedMotion -> 1.0f
            showPulse -> 1.26f
            isFavorite -> 1.06f
            else -> 1.0f
        },
        animationSpec = EchoMotion.SpatialFast,
        label = "heart_scale"
    )
    val ringScale by animateFloatAsState(
        targetValue = if (showPulse) 1.48f else 0.9f,
        animationSpec = tween(260, easing = EchoMotion.EaseOutStrong),
        label = "heart_ring_scale"
    )
    val ringAlpha by animateFloatAsState(
        targetValue = if (showPulse) 0.42f else 0f,
        animationSpec = tween(240),
        label = "heart_ring_alpha"
    )

    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(bgColor)
            .echoPressable(scaleOnPress = 0.88f) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onToggle()
            },
        contentAlignment = Alignment.Center
    ) {
        if (ringAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .size(iconSize)
                    .graphicsLayer {
                        scaleX = ringScale
                        scaleY = ringScale
                        alpha = ringAlpha
                    }
                    .border(2.dp, activeHeartColor, CircleShape)
            )
        }
        Icon(
            imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            contentDescription = if (isFavorite) "Remove from Liked Songs" else "Like",
            tint = tint,
            modifier = Modifier
                .size(iconSize)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
        )
    }
}

/**
 * Material 3 Expressive Shape-Morphing Filter Chip:
 * Unselected: soft rounded square (14.dp) on tonal `surfaceContainerHigh`.
 * Selected: full capsule pill (999.dp) on `primaryContainer` with vibrant leading checkmark!
 */
@Composable
fun EchoFilterChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isReducedMotion = LocalReducedMotion.current
    val cornerDp by animateDpAsState(
        targetValue = if (isSelected) 24.dp else 14.dp,
        animationSpec = if (isReducedMotion) tween(120) else EchoMotion.spatialFast(),
        label = "m3_chip_shape"
    )
    val bg by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = tween(180, easing = EchoMotion.EaseOutStrong),
        label = "chip_bg"
    )
    val fg by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(180, easing = EchoMotion.EaseOutStrong),
        label = "chip_fg"
    )

    val chipShape = RoundedCornerShape(cornerDp)
    Row(
        modifier = modifier
            .clip(chipShape)
            .background(bg)
            .echoPressable(scaleOnPress = 0.95f, hapticOnClick = true, onClick = onClick)
            .padding(horizontal = if (isSelected) 16.dp else 15.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = fg,
                modifier = Modifier
                    .padding(end = 6.dp)
                    .size(15.dp)
            )
        }
        Text(
            text = text,
            color = fg,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
            )
        )
    }
}

/**
 * Material 3 Expressive Section Header with tonal secondary-container action pill.
 */
@Composable
fun EchoSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = EchoSpacing.screenHorizontalMargin, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.weight(1f, fill = false)
        )
        if (onActionClick != null) {
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .echoPressable(scaleOnPress = 0.94f, hapticOnClick = true, onClick = onActionClick)
                    .padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (actionLabel != null) {
                    Text(
                        text = actionLabel,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Material 3 Expressive borderless tonal container card using `surfaceContainerLow` / `surfaceContainer`.
 */
@Composable
fun EchoInsetCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    shape: Shape = RoundedCornerShape(EchoRadius.lg),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = containerColor,
        tonalElevation = 1.dp,
        content = {
            Column(content = content)
        }
    )
}

/**
 * Material 3 Expressive Connected Pill Action Button Pair (asymmetric inner 6.dp / outer 999.dp corners).
 */
@Composable
fun EchoConnectedButtonGroup(
    primaryLabel: String,
    primaryIcon: androidx.compose.ui.graphics.vector.ImageVector,
    onPrimaryClick: () -> Unit,
    secondaryLabel: String,
    secondaryIcon: androidx.compose.ui.graphics.vector.ImageVector,
    onSecondaryClick: () -> Unit,
    modifier: Modifier = Modifier,
    primaryContainer: Color = MaterialTheme.colorScheme.primary,
    onPrimaryContainer: Color = MaterialTheme.colorScheme.onPrimary,
    secondaryContainer: Color = MaterialTheme.colorScheme.secondaryContainer,
    onSecondaryContainer: Color = MaterialTheme.colorScheme.onSecondaryContainer
) {
    val leftShape = RoundedCornerShape(
        topStart = 28.dp,
        bottomStart = 28.dp,
        topEnd = 8.dp,
        bottomEnd = 8.dp
    )
    val rightShape = RoundedCornerShape(
        topStart = 8.dp,
        bottomStart = 8.dp,
        topEnd = 28.dp,
        bottomEnd = 28.dp
    )
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .height(50.dp)
                .clip(leftShape)
                .background(primaryContainer)
                .echoPressable(scaleOnPress = 0.96f, hapticOnClick = true, onClick = onPrimaryClick),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = primaryIcon,
                contentDescription = primaryLabel,
                tint = onPrimaryContainer,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = primaryLabel,
                color = onPrimaryContainer,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .height(50.dp)
                .clip(rightShape)
                .background(secondaryContainer)
                .echoPressable(scaleOnPress = 0.96f, hapticOnClick = true, onClick = onSecondaryClick),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = secondaryIcon,
                contentDescription = secondaryLabel,
                tint = onSecondaryContainer,
                modifier = Modifier.size(19.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = secondaryLabel,
                color = onSecondaryContainer,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}

/**
 * Consistent 48dp circular profile avatar button used across top-level tabs
 * to open Profile & Settings.
 */
@Composable
fun EchoProfileButton(
    initial: String,
    gradientColors: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    visualSize: Dp = 40.dp
) {
    val avatarBrush = remember(gradientColors) { Brush.linearGradient(gradientColors) }

    Box(
        modifier = modifier
            .size(EchoSpacing.minTouchTarget)
            .clip(CircleShape)
            .echoPressable(scaleOnPress = 0.92f, hapticOnClick = true, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(visualSize)
                .clip(CircleShape)
                .background(avatarBrush),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial.ifBlank { "E" },
                color = Color.White,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = if (visualSize < 40.dp) 14.sp else 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            )
        }
    }
}

private val FALLBACK_AVATAR_PALETTES = listOf(
    Color(0xFF6750A4) to Color(0xFF9A82DB),
    Color(0xFFC0294E) to Color(0xFFFF6B9D),
    Color(0xFF006B63) to Color(0xFF2DD4BF),
    Color(0xFF2563EB) to Color(0xFF60A5FA),
    Color(0xFF8C5000) to Color(0xFFFFB963),
    Color(0xFF984061) to Color(0xFFA78BFA)
)

/**
 * Circular artist portrait with deterministic gradient monogram fallback whenever [avatarUrl] is blank.
 */
@Composable
fun EchoArtistAvatar(
    name: String,
    avatarUrl: String,
    modifier: Modifier = Modifier,
    size: Dp = 92.dp,
    borderColor: Color = Color.Transparent,
    borderWidth: Dp = 0.dp
) {
    val palette = remember(name) {
        val idx = kotlin.math.abs(name.lowercase().hashCode()) % FALLBACK_AVATAR_PALETTES.size
        FALLBACK_AVATAR_PALETTES[idx]
    }
    val initials = remember(name) {
        name.split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .joinToString("")
            .ifBlank { "A" }
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(palette.first, palette.second)))
            .then(
                if (borderWidth > 0.dp && borderColor != Color.Transparent) {
                    Modifier.border(borderWidth, borderColor, CircleShape)
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            color = Color.White,
            fontSize = (size.value * 0.28f).sp,
            fontWeight = FontWeight.ExtraBold
        )
        if (avatarUrl.isNotBlank()) {
            val context = LocalContext.current
            val req = remember(avatarUrl, context) {
                ImageRequest.Builder(context)
                    .data(avatarUrl)
                    .crossfade(true)
                    .build()
            }
            AsyncImage(
                model = req,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .clip(CircleShape)
            )
        }
    }
}

/**
 * Square/rounded artwork container with gradient + MusicNote fallback when [imageUrl] is blank.
 */
@Composable
fun EchoArtworkImage(
    imageUrl: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    seedKey: String = contentDescription.orEmpty(),
    iconSize: Dp = 28.dp
) {
    val palette = remember(seedKey) {
        val idx = kotlin.math.abs(seedKey.lowercase().hashCode()) % FALLBACK_AVATAR_PALETTES.size
        FALLBACK_AVATAR_PALETTES[idx]
    }
    Box(
        modifier = modifier.background(Brush.linearGradient(listOf(palette.first, palette.second))),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.MusicNote,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.88f),
            modifier = Modifier.size(iconSize)
        )
        if (imageUrl.isNotBlank()) {
            val context = LocalContext.current
            val req = remember(imageUrl, context) {
                ImageRequest.Builder(context)
                    .data(imageUrl)
                    .crossfade(true)
                    .build()
            }
            AsyncImage(
                model = req,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        }
    }
}

/**
 * Material 3 Expressive rounded tonal icon container used in Settings and structured lists.
 */
@Composable
fun EchoTonalIconBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    iconTint: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    size: Dp = 38.dp,
    iconSize: Dp = 20.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * Clean Apple HIG + Material 3 Expressive pill segmented control.
 */
@Composable
fun <T> EchoSegmentedControl(
    options: List<T>,
    selectedOption: T,
    labelFor: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { option ->
            val selected = option == selectedOption
            val bg by animateColorAsState(
                targetValue = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
                animationSpec = tween(EchoMotion.DurationMicro, easing = EchoMotion.EaseOutStrong),
                label = "seg_bg"
            )
            val fg by animateColorAsState(
                targetValue = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = tween(EchoMotion.DurationMicro),
                label = "seg_fg"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(CircleShape)
                    .then(
                        if (selected) {
                            Modifier.shadow(2.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.12f))
                        } else Modifier
                    )
                    .background(bg)
                    .echoPressable(scaleOnPress = 0.97f, hapticOnClick = true) { onSelect(option) }
                    .padding(vertical = 9.dp, horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = labelFor(option),
                    color = fg,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
