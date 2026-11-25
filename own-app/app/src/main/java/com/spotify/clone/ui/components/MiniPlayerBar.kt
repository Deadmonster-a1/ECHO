package com.spotify.clone.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.spotify.clone.core.designsystem.AppThemeOption
import com.spotify.clone.core.designsystem.EchoMarqueeText
import com.spotify.clone.core.designsystem.EchoMotion
import com.spotify.clone.core.designsystem.EchoPrismGradientColors
import com.spotify.clone.core.designsystem.LiveAudioSpectrum
import com.spotify.clone.core.designsystem.LocalAppTheme
import com.spotify.clone.core.designsystem.LocalReducedMotion
import com.spotify.clone.core.designsystem.ThemeManager
import com.spotify.clone.core.designsystem.echoPressable
import com.spotify.clone.core.designsystem.echoSpecularBorder
import com.spotify.clone.core.designsystem.rememberAnimatedArtworkColors
import com.spotify.clone.core.designsystem.resolvedBorder
import com.spotify.clone.core.model.MusicGenre
import com.spotify.clone.core.model.PlaybackState
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Spotify MotionLayout-inspired floating MiniPlayer bar with:
 * - 1:1 finger-tracked horizontal swipe (with velocity handoff & rubber-banding) to skip next/prev
 * - 1:1 vertical drag-up with lift scale to expand Now Playing
 * - Live harmonic audio spectrum & artwork-derived ambient glow
 * - Quick Like heart button & Marquee track title
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

    val artworkColors = rememberAnimatedArtworkColors(track)
    val surfaceBg = if (isDarkMode) Color(0xFC17171C) else Color(0xFCFFFFFF)
    val borderColor = activeTheme.resolvedBorder(isDarkMode)
    val primaryText = MaterialTheme.colorScheme.onSurface
    val secondaryText = MaterialTheme.colorScheme.onSurfaceVariant
    val pillShape = RoundedCornerShape(18.dp)

    val offsetX = remember(track.id) { Animatable(0f) }
    val offsetY = remember(track.id) { Animatable(0f) }

    val horizontalThresholdPx = with(density) { 88.dp.toPx() }
    val verticalThresholdPx = with(density) { 44.dp.toPx() }
    val maxRubberPx = with(density) { 140.dp.toPx() }

    val smoothProgress by animateFloatAsState(
        targetValue = playbackState.progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 180, easing = LinearEasing),
        label = "mini_player_progress"
    )

    val ambientTint by animateColorAsState(
        targetValue = artworkColors.dominant.copy(alpha = if (isDarkMode) 0.22f else 0.10f),
        animationSpec = tween(420),
        label = "mini_ambient_tint"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .graphicsLayer {
                    val tx = offsetX.value
                    val ty = offsetY.value
                    translationX = tx
                    translationY = ty
                    rotationZ = if (isReducedMotion) 0f else (tx / maxRubberPx) * 1.4f
                    val liftScale = if (ty < 0f && !isReducedMotion) {
                        1f + (-ty / maxRubberPx).coerceIn(0f, 1f) * 0.03f
                    } else 1f
                    scaleX = liftScale
                    scaleY = liftScale
                }
                .shadow(
                    elevation = if (isDarkMode) 12.dp else 14.dp,
                    shape = pillShape,
                    ambientColor = artworkColors.dominant.copy(alpha = 0.18f),
                    spotColor = artworkColors.dominant.copy(alpha = 0.28f)
                )
                .clip(pillShape)
                .background(surfaceBg)
                .background(
                    Brush.horizontalGradient(
                        listOf(ambientTint, Color.Transparent, Color.Transparent)
                    )
                )
                .echoSpecularBorder(
                    isDarkMode = isDarkMode,
                    shape = pillShape,
                    fallbackBorder = borderColor
                )
                .pointerInput(track.id) {
                    val velocityTracker = VelocityTracker()
                    detectDragGestures(
                        onDragStart = {
                            velocityTracker.resetTracking()
                        },
                        onDragCancel = {
                            scope.launch {
                                launch { offsetX.animateTo(0f, EchoMotion.SpringMomentum) }
                                launch { offsetY.animateTo(0f, EchoMotion.SpringMomentum) }
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
                                            offsetX.animateTo(
                                                0f,
                                                EchoMotion.SpringMomentum,
                                                initialVelocity = vel.x
                                            )
                                        }
                                        launch {
                                            offsetY.animateTo(
                                                0f,
                                                EchoMotion.SpringMomentum,
                                                initialVelocity = vel.y
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            velocityTracker.addPosition(change.uptimeMillis, change.position)
                            scope.launch {
                                // Commit dominant axis cleanly while allowing subtle secondary feel
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
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        AsyncImage(
                            model = current.coverArtUrl,
                            contentDescription = current.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(44.dp)
                                .shadow(4.dp, RoundedCornerShape(10.dp), ambientColor = artworkColors.dominant.copy(alpha = 0.25f))
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                        if (playbackState.isPlaying) {
                            Box(
                                modifier = Modifier
                                    .padding(2.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.Black.copy(alpha = 0.72f))
                                    .padding(horizontal = 3.dp, vertical = 2.dp)
                            ) {
                                LiveAudioSpectrum(
                                    isPlaying = true,
                                    barCount = 4,
                                    genre = MusicGenre.inferGenre(current),
                                    barColor = artworkColors.vibrantAccent,
                                    barWidth = 1.8.dp,
                                    barSpacing = 1.4.dp,
                                    height = 9.dp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        EchoMarqueeText(
                            text = current.title,
                            color = primaryText,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            enabled = playbackState.isPlaying
                        )
                        Text(
                            text = current.artistName,
                            color = secondaryText,
                            fontSize = 12.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (onToggleFavorite != null) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .echoPressable(scaleOnPress = 0.84f, hapticOnClick = true, onClick = onToggleFavorite),
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
                            .size(40.dp)
                            .clip(CircleShape)
                            .echoPressable(scaleOnPress = 0.86f, hapticOnClick = true, onClick = onTogglePlayPause),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(
                            targetState = playbackState.isPlaying,
                            transitionSpec = {
                                (fadeIn(tween(130, easing = EchoMotion.EncoreButtonEase)) +
                                    scaleIn(initialScale = 0.85f, animationSpec = tween(130, easing = EchoMotion.EncoreButtonEase))) togetherWith
                                    (fadeOut(tween(100)) + scaleOut(targetScale = 0.85f, animationSpec = tween(100)))
                            },
                            label = "mini_play_icon"
                        ) { playing ->
                            Icon(
                                imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (playing) "Pause" else "Play",
                                tint = primaryText,
                                modifier = Modifier.size(25.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .echoPressable(scaleOnPress = 0.86f, hapticOnClick = true, onClick = onSkipNext),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SkipNext,
                            contentDescription = "Next",
                            tint = primaryText,
                            modifier = Modifier.size(23.dp)
                        )
                    }
                }
            }

            // Crisp animated progress hairline along bottom edge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .background(primaryText.copy(alpha = 0.08f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(smoothProgress)
                        .height(2.5.dp)
                        .background(
                            if (activeTheme == AppThemeOption.PASTEL_PRISM) {
                                Brush.horizontalGradient(EchoPrismGradientColors)
                            } else {
                                Brush.horizontalGradient(
                                    listOf(activeTheme.primaryColor, artworkColors.vibrantAccent)
                                )
                            }
                        )
                )
            }
        }
    }
}
