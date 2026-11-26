package com.spotify.clone.core.designsystem

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * GPU-accelerated live equalizer bars using scaleY on graphicsLayer
 * so 60-120fps animations never trigger Compose layout/measure passes.
 */
@Composable
fun LivingEqualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 3,
    barColor: Color = LocalAppTheme.current.primaryColor,
    barWidth: Dp = 3.dp,
    barSpacing: Dp = 2.dp,
    maxHeight: Dp = 15.dp,
    minHeight: Dp = 3.dp
) {
    val isReducedMotion = LocalReducedMotion.current
    val infiniteTransition = rememberInfiniteTransition(label = "living_eq")

    val p0 by infiniteTransition.animateFloat(
        initialValue = 0.28f,
        targetValue = 0.96f,
        animationSpec = infiniteRepeatable(
            animation = tween(380, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eq_0"
    )
    val p1 by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 0.24f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eq_1"
    )
    val p2 by infiniteTransition.animateFloat(
        initialValue = 0.36f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(320, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eq_2"
    )

    val pausedFraction = (minHeight / maxHeight).coerceIn(0.18f, 0.35f)
    val playMix by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = EchoMotion.tweenMicro(),
        label = "eq_play_mix"
    )
    val fractions = listOf(p0, p1, p2)

    Row(
        modifier = modifier.height(maxHeight),
        horizontalArrangement = Arrangement.spacedBy(barSpacing),
        verticalAlignment = Alignment.Bottom
    ) {
        repeat(barCount) { idx ->
            val liveFrac = if (isReducedMotion) 0.65f else fractions[idx % fractions.size]
            val targetFrac = pausedFraction + (liveFrac - pausedFraction) * playMix
            Box(
                modifier = Modifier
                    .width(barWidth)
                    .height(maxHeight)
                    .graphicsLayer {
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
 * Specular edge highlight simulating light catching the top bevel of floating glass surfaces.
 */
fun Modifier.echoSpecularBorder(
    isDarkMode: Boolean,
    shape: Shape,
    fallbackBorder: Color
): Modifier {
    val borderBrush = if (isDarkMode) {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.18f),
                Color.White.copy(alpha = 0.06f),
                fallbackBorder.copy(alpha = 0.65f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.95f),
                fallbackBorder.copy(alpha = 0.85f)
            )
        )
    }
    return this.border(0.75.dp, borderBrush, shape)
}

/**
 * SwiftPieces Liquid Glass surface reserved strictly for the floating chrome layer
 * (floating dock, mini-player, floating pill controls, and floating action circles).
 * Never applied to content cards or list rows.
 */
fun Modifier.echoGlassSurface(
    isDarkMode: Boolean,
    shape: Shape,
    fallbackBorder: Color,
    tintColor: Color? = null,
    elevation: Dp = 12.dp
): Modifier {
    val baseGlass = if (isDarkMode) Color(0xF7151520) else Color(0xFAFFFFFF)
    return this
        .shadow(
            elevation = elevation,
            shape = shape,
            ambientColor = Color.Black.copy(alpha = if (isDarkMode) 0.28f else 0.10f),
            spotColor = Color.Black.copy(alpha = if (isDarkMode) 0.36f else 0.14f)
        )
        .clip(shape)
        .background(baseGlass)
        .then(
            if (tintColor != null) {
                Modifier.background(
                    Brush.horizontalGradient(
                        listOf(
                            tintColor.copy(alpha = if (isDarkMode) 0.16f else 0.08f),
                            Color.Transparent
                        )
                    )
                )
            } else {
                Modifier
            }
        )
        .echoSpecularBorder(
            isDarkMode = isDarkMode,
            shape = shape,
            fallbackBorder = fallbackBorder
        )
}

/**
 * Standard >= 44dp circular toolbar / floating action button with consistent tactile press physics.
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
    val activeTheme = LocalAppTheme.current
    val isDarkMode by ThemeManager.isDarkMode.collectAsState()
    val borderColor = activeTheme.resolvedBorder(isDarkMode)
    val fill = containerColor ?: activeTheme.resolvedCard(isDarkMode)

    Box(
        modifier = modifier
            .size(size)
            .then(
                if (floatingGlass) {
                    Modifier.echoGlassSurface(
                        isDarkMode = isDarkMode,
                        shape = CircleShape,
                        fallbackBorder = borderColor,
                        elevation = 8.dp
                    )
                } else {
                    Modifier
                        .clip(CircleShape)
                        .background(fill)
                        .border(0.6.dp, borderColor.copy(alpha = 0.8f), CircleShape)
                }
            )
            .echoPressable(scaleOnPress = 0.90f, hapticOnClick = true, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
fun EchoPlayFab(
    isPlaying: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    containerColor: Color = LocalAppTheme.current.primaryColor,
    contentColor: Color = echoContrastOn(containerColor)
) {
    val isReducedMotion = LocalReducedMotion.current
    val infiniteTransition = rememberInfiniteTransition(label = "fab_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isPlaying && !isReducedMotion) 1.14f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fab_aura_scale"
    )
    val auraAlpha by animateFloatAsState(
        targetValue = if (isPlaying && !isReducedMotion) 0.24f else 0f,
        animationSpec = EchoMotion.tweenStandard(),
        label = "fab_aura_alpha"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        if (auraAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .size(size)
                    .graphicsLayer {
                        scaleX = pulseScale
                        scaleY = pulseScale
                        alpha = auraAlpha
                    }
                    .clip(CircleShape)
                    .background(containerColor)
            )
        }
        Box(
            modifier = Modifier
                .size(size)
                .shadow(
                    elevation = if (isPlaying) 12.dp else 6.dp,
                    shape = CircleShape,
                    ambientColor = containerColor.copy(alpha = 0.24f),
                    spotColor = containerColor.copy(alpha = 0.34f)
                )
                .clip(CircleShape)
                .background(containerColor)
                .echoPressable(scaleOnPress = 0.92f, hapticOnClick = true, onClick = onToggle),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = isPlaying,
                transitionSpec = {
                    (fadeIn(tween(140, easing = EchoMotion.EaseOutStrong)) +
                        scaleIn(initialScale = 0.85f, animationSpec = tween(140, easing = EchoMotion.EaseOutStrong))) togetherWith
                        (fadeOut(tween(110)) + scaleOut(targetScale = 0.85f, animationSpec = tween(110)))
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
    val activeTheme = LocalAppTheme.current
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

    val tint by animateColorAsState(
        targetValue = if (isFavorite) activeTheme.primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(180, easing = EchoMotion.EaseOutStrong),
        label = "heart_tint"
    )
    val scale by animateFloatAsState(
        targetValue = when {
            isReducedMotion -> 1.0f
            showPulse -> 1.24f
            isFavorite -> 1.05f
            else -> 1.0f
        },
        animationSpec = EchoMotion.SpringBouncy,
        label = "heart_scale"
    )
    val ringScale by animateFloatAsState(
        targetValue = if (showPulse) 1.45f else 0.9f,
        animationSpec = tween(260, easing = EchoMotion.EaseOutStrong),
        label = "heart_ring_scale"
    )
    val ringAlpha by animateFloatAsState(
        targetValue = if (showPulse) 0.4f else 0f,
        animationSpec = tween(240),
        label = "heart_ring_alpha"
    )

    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
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
                    .border(1.5.dp, activeTheme.primaryColor, CircleShape)
            )
        }
        Icon(
            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
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

@Composable
fun EchoFilterChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeTheme = LocalAppTheme.current
    val isDarkMode by ThemeManager.isDarkMode.collectAsState()
    val unselectedBg = MaterialTheme.colorScheme.surface
    val borderBase = activeTheme.resolvedBorder(isDarkMode)

    val bg by animateColorAsState(
        targetValue = if (isSelected) activeTheme.primaryColor else unselectedBg,
        animationSpec = tween(180, easing = EchoMotion.EaseOutStrong),
        label = "chip_bg"
    )
    val fg by animateColorAsState(
        targetValue = if (isSelected) echoContrastOn(activeTheme.primaryColor) else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(180, easing = EchoMotion.EaseOutStrong),
        label = "chip_fg"
    )
    val borderTint by animateColorAsState(
        targetValue = if (isSelected) activeTheme.primaryColor else borderBase,
        animationSpec = tween(180),
        label = "chip_border"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(EchoRadius.pill))
            .background(bg)
            .border(0.6.dp, borderTint, RoundedCornerShape(EchoRadius.pill))
            .echoPressable(scaleOnPress = 0.96f, hapticOnClick = true, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = fg,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
            )
        )
    }
}

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
            .then(
                if (onActionClick != null) {
                    Modifier.echoPressable(scaleOnPress = 0.985f, onClick = onActionClick)
                } else Modifier
            )
            .padding(horizontal = EchoSpacing.screenHorizontalMargin, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.headlineMedium
        )
        if (onActionClick != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (actionLabel != null) {
                    Text(
                        text = actionLabel,
                        color = LocalAppTheme.current.primaryColor,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = LocalAppTheme.current.primaryColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun EchoInsetCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val activeTheme = LocalAppTheme.current
    val isDarkMode by ThemeManager.isDarkMode.collectAsState()
    val cardBg = activeTheme.resolvedCard(isDarkMode)
    val borderColor = activeTheme.resolvedBorder(isDarkMode)
    val shape = RoundedCornerShape(EchoRadius.md)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(cardBg)
            .border(0.6.dp, borderColor.copy(alpha = 0.85f), shape),
        content = content
    )
}

/**
 * Consistent 44dp circular profile avatar button used across top-level tabs
 * to open Profile & Settings.
 */
@Composable
fun EchoProfileButton(
    initial: String,
    gradientColors: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeTheme = LocalAppTheme.current
    val isDarkMode by ThemeManager.isDarkMode.collectAsState()
    val borderColor = activeTheme.resolvedBorder(isDarkMode)

    Box(
        modifier = modifier
            .size(EchoSpacing.minTouchTarget)
            .clip(CircleShape)
            .background(Brush.linearGradient(gradientColors))
            .border(0.8.dp, borderColor.copy(alpha = 0.9f), CircleShape)
            .echoPressable(scaleOnPress = 0.92f, hapticOnClick = true, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial.ifBlank { "E" },
            color = Color.White,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
    }
}
