package com.spotify.clone.ui.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
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
import com.spotify.clone.core.designsystem.LocalAppTheme
import com.spotify.clone.core.designsystem.LocalReducedMotion
import com.spotify.clone.core.lyrics.LyricsSyncEngine
import com.spotify.clone.core.model.LyricsResult
import com.spotify.clone.core.model.LyricsSyncType
import com.spotify.clone.core.model.Track
import com.spotify.clone.core.settings.SettingsManager
import kotlinx.coroutines.isActive

/**
 * Real-time 60fps Synchronized English Lyrics View.
 * - Normalizes every lyric line into readable English / Romanized text
 * - Uses frame-accurate clock interpolation between player polls for smooth karaoke sweep
 * - Auto-scrolls smoothly to keep the singing line centered while pausing for manual browsing
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
    onSeekTo: (Long) -> Unit = {},
    onFullscreenClick: (() -> Unit)? = null
) {
    val activeTheme = LocalAppTheme.current
    val isReducedMotion = LocalReducedMotion.current
    val listState = rememberLazyListState()
    val isUserScrolling by listState.interactionSource.collectIsDraggedAsState()
    var userTouchActiveUntilMs by remember { mutableLongStateOf(0L) }

    val sizeTierName by SettingsManager.lyricsSizeTier.collectAsState()
    val sizeTier = remember(sizeTierName) {
        LyricsSizeTier.entries.find { it.name == sizeTierName } ?: LyricsSizeTier.STANDARD
    }
    val lyricsOffsetMs by SettingsManager.lyricsOffsetMs.collectAsState()

    val lines = lyricsResult.lines
    val isSynced = (lyricsResult.syncType == LyricsSyncType.LINE_SYNCED ||
        lyricsResult.syncType == LyricsSyncType.SYLLABLE_SYNCED) &&
        lines.isNotEmpty() &&
        lines.first().startTimeMs >= 0L

    // 60fps frame clock interpolation between ExoPlayer position polls
    var interpolatedPositionMs by remember(track?.id) { mutableLongStateOf(currentPositionMs) }
    LaunchedEffect(currentPositionMs, isPlaying, playbackSpeed, track?.id) {
        interpolatedPositionMs = currentPositionMs
        if (isPlaying && !isReducedMotion) {
            var anchorFrameMs = 0L
            while (isActive) {
                withFrameMillis { frameMs ->
                    if (anchorFrameMs == 0L) {
                        anchorFrameMs = frameMs
                    } else {
                        val elapsed = ((frameMs - anchorFrameMs) * playbackSpeed).toLong()
                        interpolatedPositionMs = currentPositionMs + elapsed.coerceIn(0L, 1200L)
                    }
                }
            }
        }
    }

    val effectivePositionMs = remember(interpolatedPositionMs, lyricsOffsetMs) {
        (interpolatedPositionMs + lyricsOffsetMs).coerceAtLeast(0L)
    }

    val activeIndex = remember(effectivePositionMs, lines, isSynced) {
        if (!isSynced) -1
        else if (effectivePositionMs < lines.first().startTimeMs) -1
        else lines.indexOfLast { it.startTimeMs <= effectivePositionMs }
    }

    LaunchedEffect(isUserScrolling) {
        if (isUserScrolling) {
            userTouchActiveUntilMs = System.currentTimeMillis() + 2500L
        }
    }

    LaunchedEffect(activeIndex) {
        if (isSynced && activeIndex >= 0 && activeIndex < lines.size) {
            val now = System.currentTimeMillis()
            if (now > userTouchActiveUntilMs && !isUserScrolling) {
                val targetIndex = (activeIndex - 1).coerceAtLeast(0)
                if (isReducedMotion) listState.scrollToItem(targetIndex)
                else listState.animateScrollToItem(targetIndex)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(EchoRadius.lg))
            .background(
                Brush.verticalGradient(
                    listOf(
                        artworkColors.backgroundTop.copy(alpha = 0.88f),
                        Color(0xEB100F16)
                    )
                )
            )
    ) {
        if (lines.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = artworkColors.vibrantAccent.copy(alpha = 0.7f),
                    modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Lyrics unavailable for this track",
                    color = Color.White.copy(alpha = 0.92f),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "No synchronized lyrics were found for this recording.",
                    color = Color.White.copy(alpha = 0.62f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 52.dp, bottom = 64.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                if (isSynced && activeIndex == -1) {
                    item(key = "lyrics_prelude") {
                        Text(
                            text = "♪  Instrumental intro…",
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                itemsIndexed(lines, key = { index, line -> "${line.startTimeMs}_$index" }) { index, line ->
                    val isActive = index == activeIndex
                    val lineScale by animateFloatAsState(
                        targetValue = if (isActive && !isReducedMotion) 1.03f else 0.98f,
                        animationSpec = EchoMotion.springSnappy(),
                        label = "lyric_scale"
                    )
                    val lineAlpha by animateFloatAsState(
                        targetValue = if (isActive) 1.0f else 0.40f,
                        animationSpec = EchoMotion.tweenStandard(),
                        label = "lyric_alpha"
                    )
                    val textColor by animateColorAsState(
                        targetValue = if (isActive) Color.White else Color.White.copy(alpha = 0.48f),
                        animationSpec = EchoMotion.tweenStandard(),
                        label = "lyric_color"
                    )

                    // Always display English (Romanized) text
                    val englishText = remember(line) {
                        LyricsSyncEngine.toEnglishText(line)
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
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = isSynced) {
                                onSeekTo(line.startTimeMs)
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = englishText,
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
                                            color = activeTheme.primaryColor.copy(alpha = 0.46f),
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

            // Top & Bottom gradient fade masks
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.60f), Color.Transparent)
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f))
                        )
                    )
            )

            // Clean top bar: English Sync badge + Fullscreen button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.42f))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = if (isSynced) "ENGLISH • SYNCED" else "ENGLISH",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp
                    )
                }

                if (onFullscreenClick != null) {
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.48f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        IconButton(
                            onClick = onFullscreenClick,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Fullscreen,
                                contentDescription = "Fullscreen Lyrics",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
