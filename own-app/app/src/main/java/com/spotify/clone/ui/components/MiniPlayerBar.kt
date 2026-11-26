package com.spotify.clone.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spotify.clone.core.designsystem.EchoArtworkImage
import com.spotify.clone.core.designsystem.EchoMarqueeText
import com.spotify.clone.core.designsystem.EchoMotion
import com.spotify.clone.core.designsystem.EchoRadius
import com.spotify.clone.core.designsystem.EchoSpacing
import com.spotify.clone.core.designsystem.ExpressiveLoadingIndicator
import com.spotify.clone.core.designsystem.LocalReducedMotion
import com.spotify.clone.core.designsystem.echoPressable
import com.spotify.clone.core.model.PlaybackState
import com.spotify.clone.core.model.PositionState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Material 3 Expressive Floating Mini-Player Capsule:
 * - Elevated tonal `surfaceContainerHighest` pill with `24.dp` organic corners
 * - Shape-morphing Play/Pause tonal squircle (`14.dp` paused <-> `999.dp` circle playing)
 * - 1:1 finger-tracked horizontal swipe to skip & vertical drag-up to expand
 */
@Composable
fun MiniPlayerBar(
    playbackState: PlaybackState,
    onTogglePlayPause: () -> Unit,
    onClick: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit = {},
    onToggleFavorite: (() -> Unit)? = null,
    positionStateFlow: StateFlow<PositionState>? = null,
    modifier: Modifier = Modifier
) {
    val track = playbackState.currentTrack ?: return
    val isReducedMotion = LocalReducedMotion.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    val primaryText = MaterialTheme.colorScheme.onSurface
    val secondaryText = MaterialTheme.colorScheme.onSurfaceVariant
    val pillShape = RoundedCornerShape(EchoRadius.lg)
    val artCorner = EchoRadius.nested(EchoRadius.lg, 8.dp)

    val playButtonCorner by animateDpAsState(
        targetValue = if (playbackState.isPlaying) 22.dp else 13.dp,
        animationSpec = if (isReducedMotion) tween(100) else EchoMotion.spatialFast(),
        label = "mini_play_morph"
    )

    val offsetX = remember(track.id) { Animatable(0f) }
    val offsetY = remember(track.id) { Animatable(0f) }

    val horizontalThresholdPx = with(density) { 84.dp.toPx() }
    val verticalThresholdPx = with(density) { 40.dp.toPx() }
    val maxRubberPx = with(density) { 132.dp.toPx() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .graphicsLayer {
                    val tx = offsetX.value
                    val ty = offsetY.value
                    translationX = tx
                    translationY = ty
                    rotationZ = if (isReducedMotion) 0f else (tx / maxRubberPx) * 1.4f
                    val liftScale = if (ty < 0f && !isReducedMotion) {
                        1f + (-ty / maxRubberPx).coerceIn(0f, 1f) * 0.04f
                    } else 1f
                    scaleX = liftScale
                    scaleY = liftScale
                }
                .shadow(
                    elevation = 10.dp,
                    shape = pillShape,
                    ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                    spotColor = Color.Black.copy(alpha = 0.24f)
                )
                .pointerInput(track.id) {
                    val velocityTracker = VelocityTracker()
                    detectDragGestures(
                        onDragStart = { velocityTracker.resetTracking() },
                        onDragCancel = {
                            scope.launch {
                                launch { offsetX.animateTo(0f, EchoMotion.EffectsDefault) }
                                launch { offsetY.animateTo(0f, EchoMotion.EffectsDefault) }
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
                                        offsetX.animateTo(0f, EchoMotion.EffectsDefault)
                                        offsetY.animateTo(0f, EchoMotion.EffectsDefault)
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
                                        launch { offsetX.animateTo(0f, EchoMotion.SpatialDefault, initialVelocity = vel.x) }
                                        launch { offsetY.animateTo(0f, EchoMotion.SpatialDefault, initialVelocity = vel.y) }
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
                .echoPressable(scaleOnPress = 0.98f, hapticOnClick = true, onClick = onClick),
            shape = pillShape,
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                AnimatedContent(
                    targetState = track,
                    contentKey = { it.id },
                    transitionSpec = {
                        if (isReducedMotion) {
                            fadeIn(tween(140)) togetherWith fadeOut(tween(120))
                        } else {
                            (slideInHorizontally(tween(EchoMotion.DurationStandard, easing = EchoMotion.EaseOutStrong)) { it / 5 } +
                                fadeIn(tween(200, easing = EchoMotion.EaseOutStrong)) +
                                scaleIn(initialScale = 0.95f, animationSpec = EchoMotion.spatialFast())) togetherWith
                                (slideOutHorizontally(tween(180)) { -it / 6 } +
                                    fadeOut(tween(150)) +
                                    scaleOut(targetScale = 0.95f, animationSpec = tween(160)))
                        }
                    },
                    label = "mini_player_track_swap"
                ) { current ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 10.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        EchoArtworkImage(
                            imageUrl = current.coverArtUrl,
                            contentDescription = current.title,
                            seedKey = "${current.title}_${current.artistName}",
                            iconSize = 20.dp,
                            modifier = Modifier
                                .size(EchoSpacing.artworkMini)
                                .clip(RoundedCornerShape(artCorner))
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
                                color = if (playbackState.errorMessage != null) MaterialTheme.colorScheme.error else secondaryText,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (onToggleFavorite != null) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .echoPressable(scaleOnPress = 0.88f, hapticOnClick = true, onClick = onToggleFavorite),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (current.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                    contentDescription = if (current.isFavorite) "Unlike" else "Like",
                                    tint = if (current.isFavorite) MaterialTheme.colorScheme.tertiary else secondaryText,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Shape-Morphing M3 Expressive Play/Pause Squircle
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(playButtonCorner))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .echoPressable(scaleOnPress = 0.88f, hapticOnClick = true, onClick = onTogglePlayPause),
                            contentAlignment = Alignment.Center
                        ) {
                            if (playbackState.isLoading && !playbackState.isPlaying) {
                                ExpressiveLoadingIndicator(
                                    size = 26.dp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    containerColor = Color.Transparent
                                )
                            } else {
                                AnimatedContent(
                                    targetState = playbackState.isPlaying,
                                    transitionSpec = {
                                        (fadeIn(tween(130, easing = EchoMotion.EaseOutStrong)) +
                                            scaleIn(initialScale = 0.82f, animationSpec = EchoMotion.spatialFast())) togetherWith
                                            (fadeOut(tween(100)) + scaleOut(targetScale = 0.82f, animationSpec = tween(100)))
                                    },
                                    label = "mini_play_icon"
                                ) { playing ->
                                    Icon(
                                        imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                        contentDescription = if (playing) "Pause" else "Play",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(40.dp)
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

                MiniPlayerProgressHairline(
                    positionStateFlow = positionStateFlow,
                    fallbackProgress = playbackState.progress,
                    accentColor = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                )
            }
        }
    }
}

@Composable
private fun MiniPlayerProgressHairline(
    positionStateFlow: StateFlow<PositionState>?,
    fallbackProgress: Float,
    accentColor: Color,
    trackColor: Color
) {
    val targetProgress = if (positionStateFlow != null) {
        val posState by positionStateFlow.collectAsState()
        posState.progress
    } else {
        fallbackProgress
    }
    val smoothProgressState = animateFloatAsState(
        targetValue = targetProgress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 180, easing = LinearEasing),
        label = "mini_player_progress"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(3.dp)
            .background(trackColor)
            .drawBehind {
                val p = smoothProgressState.value.coerceIn(0f, 1f)
                if (p > 0f) {
                    drawRect(
                        color = accentColor,
                        size = Size(size.width * p, size.height)
                    )
                }
            }
    )
}
