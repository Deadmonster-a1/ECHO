package com.spotify.clone.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.spotify.clone.core.designsystem.EchoAccentCoral
import com.spotify.clone.core.designsystem.EchoMarqueeText
import com.spotify.clone.core.designsystem.EchoMotion
import com.spotify.clone.core.designsystem.EchoRadius
import com.spotify.clone.core.designsystem.EchoSpacing
import com.spotify.clone.core.designsystem.LocalAppTheme
import com.spotify.clone.core.designsystem.LocalReducedMotion
import com.spotify.clone.core.designsystem.ThemeManager
import com.spotify.clone.core.designsystem.echoGlassSurface
import com.spotify.clone.core.designsystem.echoPressable
import com.spotify.clone.core.designsystem.resolvedBorder
import com.spotify.clone.core.model.PlaybackState
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * SwiftPieces NowPlaying-inspired floating Liquid Glass mini-player bar:
 * - Concentric frosted glass capsule aligned with the navigation dock
 * - 1:1 finger-tracked horizontal swipe to skip & vertical drag-up to expand
 * - Single locked accent progress hairline and >= 44dp touch targets
 */
@Composable
fun MiniPlayerBar(
    playbackState: PlaybackState,
    onTogglePlayPause: () -> Unit,
    onClick: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit = {},
    onToggleFavorite: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val track = playbackState.currentTrack ?: return
    val activeTheme = LocalAppTheme.current
    val isDarkMode by ThemeManager.isDarkMode.collectAsState()
    val isReducedMotion = LocalReducedMotion.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    val borderColor = activeTheme.resolvedBorder(isDarkMode)
    val primaryText = MaterialTheme.colorScheme.onSurface
    val secondaryText = MaterialTheme.colorScheme.onSurfaceVariant
    val pillShape = RoundedCornerShape(EchoRadius.lg)
    val artCorner = EchoRadius.nested(EchoRadius.lg, 8.dp)

    val offsetX = remember(track.id) { Animatable(0f) }
    val offsetY = remember(track.id) { Animatable(0f) }

    val horizontalThresholdPx = with(density) { 84.dp.toPx() }
    val verticalThresholdPx = with(density) { 40.dp.toPx() }
    val maxRubberPx = with(density) { 132.dp.toPx() }

    val smoothProgress by animateFloatAsState(
        targetValue = playbackState.progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 180, easing = LinearEasing),
        label = "mini_player_progress"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 500.dp)
                .fillMaxWidth()
                .graphicsLayer {
                    val tx = offsetX.value
                    val ty = offsetY.value
                    translationX = tx
                    translationY = ty
                    rotationZ = if (isReducedMotion) 0f else (tx / maxRubberPx) * 1.2f
                    val liftScale = if (ty < 0f && !isReducedMotion) {
                        1f + (-ty / maxRubberPx).coerceIn(0f, 1f) * 0.035f
                    } else 1f
                    scaleX = liftScale
                    scaleY = liftScale
                }
                .echoGlassSurface(
                    isDarkMode = isDarkMode,
                    shape = pillShape,
                    fallbackBorder = borderColor,
                    elevation = 10.dp
                )
                .pointerInput(track.id) {
                    val velocityTracker = VelocityTracker()
                    detectDragGestures(
                        onDragStart = { velocityTracker.resetTracking() },
                        onDragCancel = {
                            scope.launch {
                                launch { offsetX.animateTo(0f, EchoMotion.SpringCriticallyDamped) }
                                launch { offsetY.animateTo(0f, EchoMotion.SpringCriticallyDamped) }
                            }
                        },
                        onDragEnd = {
                            val vel = velocityTracker.calculateVelocity()
                            val projectedX = offsetX.value + EchoMotion.projectMomentum(vel.x, 0.992f)
                            val projectedY = offsetY.value + EchoMotion.projectMomentum(vel.y, 0.992f)

                            when {
                                projectedY < -verticalThresholdPx || offsetY.value < -verticalThresholdPx -> {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onClick()
                                    scope.launch {
                                        offsetX.animateTo(0f, EchoMotion.SpringCriticallyDamped)
                                        offsetY.animateTo(0f, EchoMotion.SpringCriticallyDamped)
                                    }
                                }
                                projectedX < -horizontalThresholdPx || offsetX.value < -horizontalThresholdPx -> {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSkipNext()
                                    scope.launch {
                                        offsetX.snapTo(0f)
                                        offsetY.snapTo(0f)
                                    }
                                }
                                projectedX > horizontalThresholdPx || offsetX.value > horizontalThresholdPx -> {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSkipPrevious()
                                    scope.launch {
                                        offsetX.snapTo(0f)
                                        offsetY.snapTo(0f)
                                    }
                                }
                                else -> {
                                    scope.launch {
                                        launch {
                                            offsetX.animateTo(0f, EchoMotion.SpringMomentum, initialVelocity = vel.x)
                                        }
                                        launch {
                                            offsetY.animateTo(0f, EchoMotion.SpringMomentum, initialVelocity = vel.y)
                                        }
                                    }
                                }
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            velocityTracker.addPosition(change.uptimeMillis, change.position)
                            scope.launch {
                                val rawX = offsetX.value + dragAmount.x
                                val rawY = offsetY.value + dragAmount.y
                                val dampedX = EchoMotion.rubberBand(rawX, maxRubberPx, 0.72f)
                                val dampedY = if (rawY < 0f) {
                                    -EchoMotion.rubberBand(abs(rawY), maxRubberPx * 0.65f, 0.65f)
                                } else {
                                    EchoMotion.rubberBand(rawY, maxRubberPx * 0.25f, 0.35f)
                                }
                                offsetX.snapTo(dampedX)
                                offsetY.snapTo(dampedY)
                            }
                        }
                    )
                }
                .echoPressable(scaleOnPress = 0.985f, hapticOnClick = true, onClick = onClick)
        ) {
            AnimatedContent(
                targetState = track,
                contentKey = { it.id },
                transitionSpec = {
                    if (isReducedMotion) {
                        fadeIn(tween(140)) togetherWith fadeOut(tween(120))
                    } else {
                        (slideInHorizontally(tween(EchoMotion.DurationStandard, easing = EchoMotion.EaseOutStrong)) { it / 5 } +
                            fadeIn(tween(200, easing = EchoMotion.EaseOutStrong)) +
                            scaleIn(initialScale = 0.96f, animationSpec = tween(220, easing = EchoMotion.EaseOutStrong))) togetherWith
                            (slideOutHorizontally(tween(180)) { -it / 6 } +
                                fadeOut(tween(150)) +
                                scaleOut(targetScale = 0.96f, animationSpec = tween(160)))
                    }
                },
                label = "mini_player_track_swap"
            ) { current ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 6.dp, top = 7.dp, bottom = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = current.coverArtUrl,
                        contentDescription = current.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(EchoSpacing.artworkMini)
                            .clip(RoundedCornerShape(artCorner))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        EchoMarqueeText(
                            text = current.title,
                            color = primaryText,
                            style = MaterialTheme.typography.titleMedium,
                            enabled = playbackState.isPlaying
                        )
                        Text(
                            text = playbackState.errorMessage ?: current.artistName,
                            color = if (playbackState.errorMessage != null) EchoAccentCoral else secondaryText,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (onToggleFavorite != null) {
                        Box(
                            modifier = Modifier
                                .size(EchoSpacing.minTouchTarget)
                                .clip(CircleShape)
                                .echoPressable(scaleOnPress = 0.88f, hapticOnClick = true, onClick = onToggleFavorite),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (current.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = if (current.isFavorite) "Unlike" else "Like",
                                tint = if (current.isFavorite) activeTheme.primaryColor else secondaryText,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(EchoSpacing.minTouchTarget)
                            .clip(CircleShape)
                            .echoPressable(scaleOnPress = 0.88f, hapticOnClick = true, onClick = onTogglePlayPause),
                        contentAlignment = Alignment.Center
                    ) {
                        if (playbackState.isLoading && !playbackState.isPlaying) {
                            CircularProgressIndicator(
                                color = activeTheme.primaryColor,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            AnimatedContent(
                                targetState = playbackState.isPlaying,
                                transitionSpec = {
                                    (fadeIn(tween(130, easing = EchoMotion.EaseOutStrong)) +
                                        scaleIn(initialScale = 0.85f, animationSpec = tween(130, easing = EchoMotion.EaseOutStrong))) togetherWith
                                        (fadeOut(tween(100)) + scaleOut(targetScale = 0.85f, animationSpec = tween(100)))
                                },
                                label = "mini_play_icon"
                            ) { playing ->
                                Icon(
                                    imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = if (playing) "Pause" else "Play",
                                    tint = primaryText,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(EchoSpacing.minTouchTarget)
                            .clip(CircleShape)
                            .echoPressable(scaleOnPress = 0.88f, hapticOnClick = true, onClick = onSkipNext),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SkipNext,
                            contentDescription = "Next",
                            tint = primaryText,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Single locked accent progress hairline along bottom edge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(primaryText.copy(alpha = 0.08f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(smoothProgress)
                        .height(2.dp)
                        .background(activeTheme.primaryColor)
                )
            }
        }
    }
}
