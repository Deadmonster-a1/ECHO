package com.spotify.clone.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spotify.clone.core.designsystem.ArtworkColors
import com.spotify.clone.core.designsystem.EchoMotion
import com.spotify.clone.core.designsystem.EchoRadius
import com.spotify.clone.core.designsystem.LocalReducedMotion
import com.spotify.clone.core.designsystem.echoPressable
import com.spotify.clone.core.lyrics.LyricsSyncEngine
import com.spotify.clone.core.model.LyricsResult
import com.spotify.clone.core.model.LyricsSyncType
import com.spotify.clone.core.model.PositionState
import com.spotify.clone.core.model.Track
import com.spotify.clone.core.settings.SettingsManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Real-time 60fps Synchronized Lyrics View:
 * - Reads live position smoothly without restarting frame interpolation on every poll
 * - Tracks explicit DragInteraction so manual scrolling never fights auto-scroll
 * - Smoothly centers the singing line in the viewport and offers a 1-tap Sync pill
 */
@Composable
fun ModernLyricsView(
    lyricsResult: LyricsResult,
    currentPositionMs: Long,
    artworkColors: ArtworkColors,
    modifier: Modifier = Modifier,
    track: Track? = null,
    isPlaying: Boolean = true,
    playbackSpeed: Float = 1.0f,
    positionStateFlow: StateFlow<PositionState>? = null,
    onSeekTo: (Long) -> Unit = {},
    onFullscreenClick: (() -> Unit)? = null
) {
    val isReducedMotion = LocalReducedMotion.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var isUserScrolling by remember { mutableStateOf(false) }

    val sizeTierName by SettingsManager.lyricsSizeTier.collectAsState()
    val sizeTier = remember(sizeTierName) {
        LyricsSizeTier.entries.find { it.name == sizeTierName } ?: LyricsSizeTier.STANDARD
    }
    val lyricsOffsetMs by SettingsManager.lyricsOffsetMs.collectAsState()
    val useEnglishScript by SettingsManager.lyricsUseRomanized.collectAsState()

    val lines = lyricsResult.lines
    val isSynced = (lyricsResult.syncType == LyricsSyncType.LINE_SYNCED ||
        lyricsResult.syncType == LyricsSyncType.SYLLABLE_SYNCED) &&
        lines.isNotEmpty() &&
        lines.first().startTimeMs >= 0L

    // Single continuous 60fps frame clock anchored to playback position without cancelling on every poll
    var interpolatedPositionMs by remember(track?.id) {
        mutableLongStateOf(positionStateFlow?.value?.currentPositionMs ?: currentPositionMs)
    }
    LaunchedEffect(track?.id, isPlaying, playbackSpeed, positionStateFlow) {
        val base = positionStateFlow?.value?.currentPositionMs ?: currentPositionMs
        interpolatedPositionMs = base
        if (isPlaying && !isReducedMotion) {
            var lastPollMs = base
            var anchorFrameMs = 0L
            while (isActive) {
                withFrameMillis { frameMs ->
                    val latestPoll = positionStateFlow?.value?.currentPositionMs ?: currentPositionMs
                    if (latestPoll != lastPollMs || anchorFrameMs == 0L) {
                        lastPollMs = latestPoll
                        anchorFrameMs = frameMs
                        interpolatedPositionMs = latestPoll
                    } else {
                        val elapsed = ((frameMs - anchorFrameMs) * playbackSpeed).toLong()
                        interpolatedPositionMs = lastPollMs + elapsed.coerceIn(0L, 1200L)
                    }
                }
            }
        }
    }
    if (positionStateFlow == null && (!isPlaying || isReducedMotion)) {
        interpolatedPositionMs = currentPositionMs
    }

    val effectivePositionMs = if (positionStateFlow != null && (!isPlaying || isReducedMotion)) {
        val polled by positionStateFlow.collectAsState()
        (polled.currentPositionMs + lyricsOffsetMs).coerceAtLeast(0L)
    } else {
        (interpolatedPositionMs + lyricsOffsetMs).coerceAtLeast(0L)
    }

    val activeIndex = remember(effectivePositionMs, lines, isSynced) {
        if (!isSynced) -1
        else if (effectivePositionMs < lines.first().startTimeMs) -1
        else lines.indexOfLast { it.startTimeMs <= effectivePositionMs }
    }

    val hasPreludeItem = isSynced && activeIndex == -1
    suspend fun scrollToActiveLine(animated: Boolean) {
        if (!isSynced || activeIndex !in lines.indices) return
        val listIndex = activeIndex + if (hasPreludeItem) 1 else 0
        val viewportHeight = listState.layoutInfo.viewportSize.height
        val centerOffset = if (viewportHeight > 0) -(viewportHeight * 0.28f).toInt() else 0
        if (animated && !isReducedMotion) {
            listState.animateScrollToItem(index = listIndex, scrollOffset = centerOffset)
        } else {
            listState.scrollToItem(index = listIndex, scrollOffset = centerOffset)
        }
    }

    // Track explicit drag interactions so auto-scroll pauses cleanly while user browses
    LaunchedEffect(listState) {
        var resumeJob: kotlinx.coroutines.Job? = null
        listState.interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is DragInteraction.Start -> {
                    resumeJob?.cancel()
                    isUserScrolling = true
                }
                is DragInteraction.Stop, is DragInteraction.Cancel -> {
                    resumeJob?.cancel()
                    resumeJob = scope.launch {
                        delay(3000L)
                        isUserScrolling = false
                        scrollToActiveLine(animated = true)
                    }
                }
            }
        }
    }

    LaunchedEffect(activeIndex, track?.id) {
        if (isSynced && activeIndex in lines.indices && !isUserScrolling) {
            scrollToActiveLine(animated = true)
        }
    }

    val bgGradient = remember(artworkColors.backgroundTop) {
        Brush.verticalGradient(
            listOf(
                artworkColors.backgroundTop.copy(alpha = 0.85f),
                Color(0xF0111018)
            )
        )
    }
    val topMaskBrush = remember {
        Brush.verticalGradient(
            listOf(Color.Black.copy(alpha = 0.52f), Color.Transparent)
        )
    }
    val bottomMaskBrush = remember {
        Brush.verticalGradient(
            listOf(Color.Transparent, Color.Black.copy(alpha = 0.68f))
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(EchoRadius.xl))
            .background(bgGradient)
    ) {
        if (lines.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(30.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Lyrics unavailable",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "No synchronized lyrics were found for this track.",
                    color = Color.White.copy(alpha = 0.65f),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 58.dp, bottom = 72.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                if (hasPreludeItem) {
                    item(key = "lyrics_prelude", contentType = "lyrics_prelude") {
                        Text(
                            text = "♪  Instrumental intro…",
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                itemsIndexed(
                    items = lines,
                    key = { index, line -> "${line.startTimeMs}_$index" },
                    contentType = { _, _ -> "lyric_line" }
                ) { index, line ->
                    val isActive = isSynced && index == activeIndex
                    val lineScale by animateFloatAsState(
                        targetValue = if (isActive && !isReducedMotion) 1.02f else 0.98f,
                        animationSpec = EchoMotion.spatialFast(),
                        label = "lyric_scale"
                    )
                    val lineAlpha by animateFloatAsState(
                        targetValue = if (!isSynced) 0.88f else if (isActive) 1.0f else 0.44f,
                        animationSpec = EchoMotion.tweenStandard(),
                        label = "lyric_alpha"
                    )
                    val textColor by animateColorAsState(
                        targetValue = if (isActive) Color.White else Color.White.copy(alpha = 0.56f),
                        animationSpec = EchoMotion.tweenStandard(),
                        label = "lyric_color"
                    )

                    val displayText = remember(line, useEnglishScript) {
                        if (useEnglishScript) {
                            LyricsSyncEngine.toEnglishText(line)
                        } else {
                            line.words.ifBlank { LyricsSyncEngine.toEnglishText(line) }
                        }
                    }

                    val lineDuration = if (line.durationMs > 0L) line.durationMs else 3000L
                    val lineProgress = if (isActive && isSynced) {
                        ((effectivePositionMs - line.startTimeMs).toFloat() / lineDuration).coerceIn(0f, 1f)
                    } else 0f

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                scaleX = lineScale
                                scaleY = lineScale
                                alpha = lineAlpha
                            }
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(enabled = isSynced) {
                                isUserScrolling = false
                                onSeekTo(line.startTimeMs)
                            }
                            .padding(horizontal = 4.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = displayText,
                            color = textColor,
                            fontSize = sizeTier.fontSizeSp.sp,
                            fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.SemiBold,
                            lineHeight = (sizeTier.fontSizeSp * 1.36f).sp,
                            modifier = if (isActive && isSynced && lineProgress > 0f) {
                                Modifier
                                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                                    .drawWithContent {
                                        drawContent()
                                        val sweepWidth = size.width * lineProgress
                                        drawRect(
                                            color = primaryColor.copy(alpha = 0.45f),
                                            size = Size(sweepWidth, size.height),
                                            blendMode = BlendMode.SrcAtop
                                        )
                                    }
                            } else {
                                Modifier
                            }
                        )
                    }
                }
            }

            // Top & Bottom subtle gradient fade masks
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .align(Alignment.TopCenter)
                    .background(topMaskBrush)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .align(Alignment.BottomCenter)
                    .background(bottomMaskBrush)
            )

            // Clean top bar: Sync status pill + Fullscreen button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.38f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (isSynced) "SYNCED LYRICS" else "LYRICS",
                        color = Color.White.copy(alpha = 0.88f),
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.7.sp)
                    )
                }

                if (onFullscreenClick != null) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.42f))
                            .echoPressable(scaleOnPress = 0.90f, hapticOnClick = true, onClick = onFullscreenClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.OpenInFull,
                            contentDescription = "Fullscreen Lyrics",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Floating Sync-to-song pill when user manually scrolled away
            androidx.compose.animation.AnimatedVisibility(
                visible = isSynced && isUserScrolling && activeIndex in lines.indices,
                enter = fadeIn() + scaleIn(initialScale = 0.9f),
                exit = fadeOut() + scaleOut(targetScale = 0.9f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .echoPressable(scaleOnPress = 0.94f, hapticOnClick = true) {
                            isUserScrolling = false
                            scope.launch { scrollToActiveLine(animated = true) }
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MyLocation,
                        contentDescription = "Sync to current line",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Sync",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}
