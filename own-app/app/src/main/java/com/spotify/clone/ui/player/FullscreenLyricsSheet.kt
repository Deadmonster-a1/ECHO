package com.spotify.clone.ui.player

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import com.spotify.clone.core.settings.SettingsManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spotify.clone.core.designsystem.ArtworkColors
import com.spotify.clone.core.designsystem.EchoMotion
import com.spotify.clone.core.designsystem.LocalAppTheme
import com.spotify.clone.core.designsystem.LocalReducedMotion
import com.spotify.clone.core.designsystem.echoPressable
import com.spotify.clone.core.designsystem.rememberAnimatedArtworkColors
import com.spotify.clone.core.lyrics.LyricsRepository
import com.spotify.clone.core.model.LyricsSyncType
import com.spotify.clone.core.model.Track
import com.spotify.clone.core.player.MusicController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Spotify-Grade Fullscreen Synchronized Lyrics Experience for ECHO.
 * Architectural synthesis of Spotify Color-Lyrics & LastWave-Native:
 *
 * 1. Multi-script typography engine: Instant toggle between English (Romanized) and Native scripts.
 * 2. Simultaneous dual-line singing: The active vocal line renders English phonetics with native subtitles.
 * 3. Live Progressive Karaoke Sweep: Smooth linear sweep highlight across the active singing line.
 * 4. Timing Offset Calibration: Dedicated +/- ms offset calibration dialog to correct sync lags.
 * 5. 4-Tier Typography Sizing: Compact, Standard, Large, Extra Large stage monitor karaoke typography.
 * 6. Social Lyrics Quote Card Generator: Export and share styled lyrics cards with album art backdrop.
 * 7. 100% Fully Automated Synchronization: Silently pauses when browsing, automatically glides back.
 * 8. Dynamic Album Art Aura Backdrop & Integrated Playback Transport Dock.
 */
@Composable
fun FullscreenLyricsSheet(
    track: Track,
    musicController: MusicController,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    artworkColors: ArtworkColors? = null
) {
    val context = LocalContext.current
    BackHandler(onBack = onClose)

    val activeTheme = LocalAppTheme.current
    val isReducedMotion = LocalReducedMotion.current
    val playbackState by musicController.playbackState.collectAsState()
    val currentPos = playbackState.currentPositionMs

    val resolvedArtworkColors = artworkColors ?: rememberAnimatedArtworkColors(track)

    // User settings & dialog states (synced with SettingsManager)
    val sizeTierName by SettingsManager.lyricsSizeTier.collectAsState()
    val sizeTier = remember(sizeTierName) {
        LyricsSizeTier.entries.find { it.name == sizeTierName } ?: LyricsSizeTier.STANDARD
    }
    val lyricsOffsetMs by SettingsManager.lyricsOffsetMs.collectAsState()
    val useEnglishScript by SettingsManager.lyricsUseRomanized.collectAsState()

    var showOffsetDialog by remember { mutableStateOf(false) }
    var showSizeDialog by remember { mutableStateOf(false) }
    var showShareLyricsDialog by remember { mutableStateOf(false) }

    var lyricsResult by remember(track.id) {
        mutableStateOf(LyricsRepository.getCachedLyrics(track))
    }
    var isLoadingLyrics by remember(track.id) {
        mutableStateOf(lyricsResult.lines.isEmpty() && lyricsResult.syncType == LyricsSyncType.NONE)
    }

    LaunchedEffect(track.id) {
        if (lyricsResult.lines.isEmpty() || lyricsResult.syncType == LyricsSyncType.NONE) {
            isLoadingLyrics = true
            val resolved = LyricsRepository.getOrFetchLyrics(track)
            lyricsResult = resolved
            isLoadingLyrics = false
        }
    }

    val lines = lyricsResult.lines
    val hasLyrics = lines.isNotEmpty()
    val isSynced = lyricsResult.syncType == LyricsSyncType.LINE_SYNCED && hasLyrics && lines.first().timestampMs >= 0L
    val hasRomanized = lyricsResult.hasRomanized

    // Modulated effective position incorporating timing calibration offset
    val effectivePos = remember(currentPos, lyricsOffsetMs) {
        (currentPos + lyricsOffsetMs).coerceAtLeast(0L)
    }

    val activeIndex = remember(effectivePos, lines, isSynced) {
        if (!isSynced || lines.isEmpty()) {
            0
        } else if (effectivePos < lines.first().timestampMs) {
            -1 // Song is in musical prelude before vocals begin
        } else {
            lines.indexOfLast { it.timestampMs <= effectivePos }
        }
    }

    val listState = rememberLazyListState()
    var isUserScrolling by remember { mutableStateOf(false) }

    // Automated smooth sync glide back after user scrolling
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            isUserScrolling = true
        } else if (isUserScrolling) {
            delay(2200L)
            isUserScrolling = false
            if (isSynced && activeIndex in lines.indices) {
                val target = (activeIndex - 1).coerceAtLeast(0)
                listState.animateScrollToItem(target)
            }
        }
    }

    // Auto-scroll when active line changes during playback
    LaunchedEffect(activeIndex) {
        if (isSynced && activeIndex in lines.indices && !isUserScrolling && !isReducedMotion) {
            val target = (activeIndex - 1).coerceAtLeast(0)
            listState.animateScrollToItem(target)
        }
    }

    val dominantTopColor = remember(resolvedArtworkColors.backgroundTop) {
        resolvedArtworkColors.backgroundTop
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0C0B10))
            .background(
                Brush.verticalGradient(
                    listOf(
                        dominantTopColor.copy(alpha = 0.90f),
                        Color(0xFF161420),
                        Color(0xFF0E0D14),
                        Color(0xFF09080D)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Enhanced Header Bar with Action Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = "Collapse",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = track.title,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isSynced) {
                            "${track.artistName} • Synced Lyrics"
                        } else if (hasLyrics) {
                            "${track.artistName} • Lyrics"
                        } else {
                            track.artistName
                        },
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // English / Native Script Language Switcher Pill
                    if (hasRomanized) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (useEnglishScript) activeTheme.primaryColor.copy(alpha = 0.22f) else Color(0xFF262332),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (useEnglishScript) activeTheme.primaryColor.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { SettingsManager.setLyricsUseRomanized(!useEnglishScript) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (useEnglishScript) "Aa English" else "తెలుగు Native",
                                    color = if (useEnglishScript) activeTheme.primaryColor else Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Timing Offset Calibration Button
                    if (isSynced) {
                        IconButton(
                            onClick = { showOffsetDialog = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (lyricsOffsetMs != 0L) activeTheme.primaryColor else Color.White.copy(alpha = 0.08f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Timer,
                                    contentDescription = "Sync Offset Calibration",
                                    tint = if (lyricsOffsetMs != 0L) Color.Black else Color.White,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }

                    // Typography Sizing Button
                    IconButton(
                        onClick = { showSizeDialog = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FormatSize,
                                contentDescription = "Typography Scale",
                                tint = Color.White,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }

                    // Social Lyrics Quote Card Share Button
                    if (hasLyrics) {
                        IconButton(
                            onClick = { showShareLyricsDialog = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.08f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Share,
                                    contentDescription = "Share Quote Card",
                                    tint = Color.White,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Main Lyrics Scroll Area with Top & Bottom Fade Masks
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (hasLyrics) {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(top = 28.dp, bottom = 64.dp, start = 20.dp, end = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Song Intro / Prelude Banner when isSynced and vocal hasn't begun
                        if (isSynced && activeIndex == -1) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(activeTheme.primaryColor.copy(alpha = 0.18f))
                                        .border(1.dp, activeTheme.primaryColor.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                                        .padding(horizontal = 18.dp, vertical = 14.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Filled.MusicNote,
                                            contentDescription = null,
                                            tint = activeTheme.primaryColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "♪ (Instrumental Prelude)",
                                                color = Color.White,
                                                fontSize = 17.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Vocals starting soon...",
                                                color = Color.White.copy(alpha = 0.65f),
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        itemsIndexed(lines) { index, line ->
                            val isActive = isSynced && index == activeIndex
                            val distance = if (activeIndex >= 0) abs(index - activeIndex) else (index + 1)

                            val targetAlpha = when {
                                !isSynced -> 0.88f
                                isActive -> 1.0f
                                distance == 1 -> 0.55f
                                distance == 2 -> 0.35f
                                else -> 0.22f
                            }

                            val lineAlpha by animateFloatAsState(
                                targetValue = if (isReducedMotion) (if (isActive) 1f else 0.45f) else targetAlpha,
                                animationSpec = tween(EchoMotion.DurationStandard),
                                label = "lyric_alpha"
                            )

                            // Resolve primary and secondary texts based on language toggle
                            val primaryText = if (useEnglishScript && !line.romanized.isNullOrBlank()) {
                                line.romanized
                            } else {
                                line.words
                            }

                            val secondaryText = if (useEnglishScript && !line.romanized.isNullOrBlank()) {
                                line.words // Show original script subtitle
                            } else if (!line.romanized.isNullOrBlank() && line.romanized != line.words) {
                                line.romanized // Show Romanized pronunciation subtitle
                            } else {
                                null
                            }

                            // Dynamic typography sizing from user size tier
                            val baseFontSize = sizeTier.fontSizeSp
                            val activeFontSize = (baseFontSize * 1.15f).sp
                            val inactiveFontSize = (baseFontSize * 0.88f).sp

                            // Progressive karaoke sweep progress calculation
                            val lineDuration = if (line.durationMs > 0L) line.durationMs else 3200L
                            val lineProgress = if (isActive && isSynced) {
                                ((effectivePos - line.timestampMs).toFloat() / lineDuration.toFloat()).coerceIn(0f, 1f)
                            } else 0f

                            val lyricBoxModifier = if (isActive) {
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                activeTheme.primaryColor.copy(alpha = 0.24f),
                                                activeTheme.primaryColor.copy(alpha = 0.08f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                                    .border(
                                        width = 1.dp,
                                        brush = Brush.horizontalGradient(
                                            listOf(
                                                activeTheme.primaryColor.copy(alpha = 0.45f),
                                                Color.Transparent
                                            )
                                        ),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            } else {
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            }

                            Box(
                                modifier = lyricBoxModifier
                                    .clickable {
                                        if (isSynced && line.timestampMs >= 0L) {
                                            musicController.seekTo(line.timestampMs)
                                            isUserScrolling = false
                                        }
                                    }
                            ) {
                                Column(modifier = Modifier.fillMaxWidth().alpha(lineAlpha)) {
                                    Box {
                                        // Main Vocal Line with progressive karaoke sweep highlight
                                        Text(
                                            text = primaryText,
                                            color = if (isActive) Color.White else Color.White.copy(alpha = 0.9f),
                                            fontSize = if (isActive) activeFontSize else inactiveFontSize,
                                            fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.SemiBold,
                                            lineHeight = if (isActive) (activeFontSize.value * 1.45f).sp else (inactiveFontSize.value * 1.35f).sp,
                                            modifier = if (isActive && isSynced && lineProgress > 0f) {
                                                Modifier
                                                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                                                    .drawWithContent {
                                                        drawContent()
                                                        val sweepWidth = size.width * lineProgress
                                                        drawRect(
                                                            color = activeTheme.primaryColor.copy(alpha = 0.48f),
                                                            size = Size(sweepWidth, size.height),
                                                            blendMode = BlendMode.SrcAtop
                                                        )
                                                    }
                                            } else {
                                                Modifier
                                            }
                                        )
                                    }

                                    // Display complimentary pronunciation / native script for active line
                                    if (isActive && !secondaryText.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = secondaryText,
                                            color = activeTheme.primaryColor.copy(alpha = 0.95f),
                                            fontSize = (baseFontSize * 0.65f).sp,
                                            fontWeight = FontWeight.Medium,
                                            lineHeight = (baseFontSize * 0.95f).sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Top Fading Scrim
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .height(24.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        dominantTopColor.copy(alpha = 0.9f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    // Bottom Fading Scrim
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(32.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Transparent,
                                        Color(0xFF14131B)
                                    )
                                )
                            )
                    )
                } else if (!isLoadingLyrics) {
                    // Respectful Honest Unavailable State
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .background(activeTheme.primaryColor.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.GraphicEq,
                                    contentDescription = null,
                                    tint = activeTheme.primaryColor,
                                    modifier = Modifier.size(34.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Text(
                                text = "Lyrics Unavailable",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Synchronized lyrics are not currently provided for \"${track.title}\". Enjoy the uncompressed studio acoustics.",
                                color = Color.White.copy(alpha = 0.65f),
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }

            // Accessible Playback Transport Dock at bottom of Lyrics
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF15141D))
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                // Scrubber with Duration Counters
                val progress = playbackState.progress
                val remainingMs = (playbackState.durationMs - currentPos).coerceAtLeast(0L)

                Slider(
                    value = progress,
                    onValueChange = { frac ->
                        musicController.seekTo((frac * playbackState.durationMs).toLong())
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = activeTheme.primaryColor,
                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(currentPos),
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                    Text(
                        text = "-${formatTime(remainingMs)}",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Transport Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Shuffle
                    IconButton(onClick = { musicController.toggleShuffle() }) {
                        Icon(
                            imageVector = Icons.Filled.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (playbackState.shuffleMode) activeTheme.primaryColor else Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Skip Previous
                    IconButton(onClick = { musicController.skipPrevious() }) {
                        Icon(
                            imageVector = Icons.Filled.SkipPrevious,
                            contentDescription = "Previous",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Play / Pause FAB
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .echoPressable(scaleOnPress = 0.92f) {
                                musicController.togglePlayPause()
                            }
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (playbackState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                            tint = Color.Black,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Skip Next
                    IconButton(onClick = { musicController.skipNext() }) {
                        Icon(
                            imageVector = Icons.Filled.SkipNext,
                            contentDescription = "Next",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Toggle Favorite / Like
                    IconButton(onClick = { musicController.toggleFavorite(track) }) {
                        Icon(
                            imageVector = if (track.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (track.isFavorite) activeTheme.primaryColor else Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }

    // Modal Dialogs
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

    if (showShareLyricsDialog) {
        ShareLyricsDialog(
            track = track,
            lines = lines,
            activeLineIndex = activeIndex,
            artworkColors = resolvedArtworkColors,
            onDismiss = { showShareLyricsDialog = false }
        )
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(java.util.Locale.US, "%d:%02d", minutes, seconds)
}
