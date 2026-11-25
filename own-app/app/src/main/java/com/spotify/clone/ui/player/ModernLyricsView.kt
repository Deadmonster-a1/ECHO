package com.spotify.clone.ui.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.collectAsState
import com.spotify.clone.core.settings.SettingsManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spotify.clone.core.designsystem.ArtworkColors
import com.spotify.clone.core.designsystem.EchoMotion
import com.spotify.clone.core.designsystem.LocalAppTheme
import com.spotify.clone.core.model.LyricLine
import com.spotify.clone.core.model.LyricsResult
import com.spotify.clone.core.model.LyricsSyncType
import com.spotify.clone.core.model.Track
import kotlinx.coroutines.delay

/**
 * Modern High-Fidelity Millisecond Synced Lyrics View for ECHO.
 * Direct architectural synthesis of Spotify Live Lyrics & LastWave-Native ModernLyricsView:
 * - Real-time millisecond tracking with active vocal line expansion
 * - Live progressive karaoke sweep fill across active line
 * - Timing offset calibration dialog (+/- ms synchronization nudge)
 * - Multi-script phonetics / translation toggle
 * - Touch-suspension auto-scrolling (resumes after 2.5s drag)
 * - Interactive tap-to-seek timestamp navigation
 * - Social lyric quote card generator
 * - 4-tier typography sizing
 */
@Composable
fun ModernLyricsView(
    lyricsResult: LyricsResult,
    currentPositionMs: Long,
    artworkColors: ArtworkColors,
    modifier: Modifier = Modifier,
    track: Track? = null,
    onSeekTo: (Long) -> Unit = {},
    onFullscreenClick: (() -> Unit)? = null
) {
    val activeTheme = LocalAppTheme.current
    val listState = rememberLazyListState()
    val isUserScrolling by listState.interactionSource.collectIsDraggedAsState()
    var userTouchActiveUntilMs by remember { mutableStateOf(0L) }

    val sizeTierName by SettingsManager.lyricsSizeTier.collectAsState()
    val sizeTier = remember(sizeTierName) {
        LyricsSizeTier.entries.find { it.name == sizeTierName } ?: LyricsSizeTier.STANDARD
    }
    val lyricsOffsetMs by SettingsManager.lyricsOffsetMs.collectAsState()
    val useRomanizedScript by SettingsManager.lyricsUseRomanized.collectAsState()

    var showShareLyricsDialog by remember { mutableStateOf(false) }
    var showOffsetDialog by remember { mutableStateOf(false) }
    var showSizeDialog by remember { mutableStateOf(false) }

    val lines = lyricsResult.lines
    val isSynced = lyricsResult.syncType == LyricsSyncType.LINE_SYNCED && lines.isNotEmpty()

    // Effective position modulated by user offset calibration
    val effectivePositionMs = remember(currentPositionMs, lyricsOffsetMs) {
        (currentPositionMs + lyricsOffsetMs).coerceAtLeast(0L)
    }

    // Identify active vocal line based on ExoPlayer position
    val activeIndex = remember(effectivePositionMs, lines, isSynced) {
        if (!isSynced) -1
        else {
            val idx = lines.indexOfLast { it.startTimeMs <= effectivePositionMs }
            if (idx >= 0) idx else 0
        }
    }

    // Auto-scroll logic: stays locked to active line, pauses during user scroll, glides back
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
                listState.animateScrollToItem(targetIndex)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
    ) {
        if (lines.isEmpty()) {
            // Respectful empty state matching LastWave
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
                    tint = artworkColors.vibrantAccent.copy(alpha = 0.6f),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Lyrics not yet synced",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Enjoy the music while we resolve real-time lyrics for this track",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 60.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                itemsIndexed(lines, key = { index, line -> "${line.startTimeMs}_$index" }) { index, line ->
                    val isActive = index == activeIndex
                    val lineScale by animateFloatAsState(
                        targetValue = if (isActive) 1.04f else 0.98f,
                        animationSpec = EchoMotion.springSnappy(),
                        label = "lyric_scale"
                    )
                    val lineAlpha by animateFloatAsState(
                        targetValue = if (isActive) 1.0f else 0.38f,
                        animationSpec = EchoMotion.tweenStandard(),
                        label = "lyric_alpha"
                    )
                    val textColor by animateColorAsState(
                        targetValue = if (isActive) Color.White else Color.White.copy(alpha = 0.45f),
                        animationSpec = EchoMotion.tweenStandard(),
                        label = "lyric_color"
                    )

                    val sub = line.subScript
                    val displayText: String = if (useRomanizedScript && !sub.isNullOrBlank()) {
                        sub
                    } else {
                        line.words
                    }

                    // Progress sweep calculation for active line (Spotify / LastWave karaoke feel)
                    val lineDuration = if (line.durationMs > 0) line.durationMs else 3000L
                    val lineProgress = if (isActive) {
                        ((effectivePositionMs - line.startTimeMs).toFloat() / lineDuration).coerceIn(0f, 1f)
                    } else 0f

                    Column(
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
                        Box {
                            // Text layer with progressive karaoke sweep highlight
                            Text(
                                text = displayText,
                                color = textColor,
                                fontSize = sizeTier.fontSizeSp.sp,
                                fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.SemiBold,
                                lineHeight = (sizeTier.fontSizeSp * 1.35f).sp,
                                modifier = if (isActive && isSynced && lineProgress > 0f) {
                                    Modifier
                                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                                        .drawWithContent {
                                            drawContent()
                                            val sweepWidth = size.width * lineProgress
                                            drawRect(
                                                color = artworkColors.vibrantAccent.copy(alpha = 0.45f),
                                                size = Size(sweepWidth, size.height),
                                                blendMode = BlendMode.SrcAtop
                                            )
                                        }
                                } else {
                                    Modifier
                                }
                            )
                        }

                        // Subscript / Translation display
                        if (!sub.isNullOrBlank() && !useRomanizedScript) {
                            Text(
                                text = sub,
                                color = if (isActive) artworkColors.vibrantAccent else Color.White.copy(alpha = 0.35f),
                                fontSize = (sizeTier.fontSizeSp * 0.65f).sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }

            // Top & Bottom gradient fade masks for cinematic depth
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.65f), Color.Transparent)
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                        )
                    )
            )

            // Floating controls toolbar (LastWave & Spotify ergonomics)
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 10.dp, end = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Time Offset Calibration Button
                Surface(
                    shape = CircleShape,
                    color = if (lyricsOffsetMs != 0L) activeTheme.primaryColor else Color.Black.copy(alpha = 0.45f),
                    modifier = Modifier.size(36.dp)
                ) {
                    IconButton(
                        onClick = { showOffsetDialog = true },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Timer,
                            contentDescription = "Sync Offset",
                            tint = if (lyricsOffsetMs != 0L) Color.Black else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Typography Sizing Button
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.45f),
                    modifier = Modifier.size(36.dp)
                ) {
                    IconButton(
                        onClick = { showSizeDialog = true },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FormatSize,
                            contentDescription = "Font Size",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Script Translation Toggle
                Surface(
                    shape = CircleShape,
                    color = if (useRomanizedScript) artworkColors.vibrantAccent else Color.Black.copy(alpha = 0.45f),
                    modifier = Modifier.size(36.dp)
                ) {
                    IconButton(
                        onClick = { SettingsManager.setLyricsUseRomanized(!useRomanizedScript) },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Translate,
                            contentDescription = "Toggle Script",
                            tint = if (useRomanizedScript) Color.Black else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Share Lyrics Quote Button
                if (track != null && lines.isNotEmpty()) {
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.45f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        IconButton(
                            onClick = { showShareLyricsDialog = true },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Share,
                                contentDescription = "Share Lyrics",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Fullscreen Expand Button
                if (onFullscreenClick != null) {
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.45f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        IconButton(
                            onClick = onFullscreenClick,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Fullscreen,
                                contentDescription = "Fullscreen",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showOffsetDialog) {
        LyricsOffsetDialog(
            currentOffsetMs = lyricsOffsetMs,
            onOffsetChanged = { SettingsManager.setLyricsOffsetMs(it) },
            onDismiss = { showOffsetDialog = false }
        )
    }

    if (showSizeDialog) {
        LyricsSizeDialog(
            currentSizeTier = sizeTier,
            onSizeSelected = { SettingsManager.setLyricsSizeTier(it.name) },
            onDismiss = { showSizeDialog = false }
        )
    }

    if (showShareLyricsDialog && track != null) {
        ShareLyricsDialog(
            track = track,
            lines = lines,
            activeLineIndex = activeIndex,
            artworkColors = artworkColors,
            onDismiss = { showShareLyricsDialog = false }
        )
    }
}
