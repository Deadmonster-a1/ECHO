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
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spotify.clone.core.designsystem.ArtworkColors
import com.spotify.clone.core.designsystem.EchoMotion
import com.spotify.clone.core.designsystem.LocalReducedMotion
import com.spotify.clone.core.designsystem.ThemeManager
import com.spotify.clone.core.designsystem.echoPressable
import com.spotify.clone.core.designsystem.rememberAnimatedArtworkColors
import com.spotify.clone.core.designsystem.updateSystemBarsAppearance
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

    val primaryColor = MaterialTheme.colorScheme.primary
    val isReducedMotion = LocalReducedMotion.current
    val playbackState by musicController.playbackState.collectAsState()
    val positionState by musicController.positionState.collectAsState()
    val currentPos = positionState.currentPositionMs
    val trackDurationMs = if (positionState.durationMs > 0L) positionState.durationMs else playbackState.durationMs

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
        mutableStateOf(LyricsRepository.getCachedLyrics(track, playbackState.durationMs))
    }
    var isLoadingLyrics by remember(track.id) {
        mutableStateOf(lyricsResult.lines.isEmpty() && lyricsResult.syncType == LyricsSyncType.NONE)
    }

    LaunchedEffect(track.id, playbackState.durationMs) {
        isLoadingLyrics = lyricsResult.lines.isEmpty()
        val resolved = LyricsRepository.getOrFetchLyrics(track, playbackState.durationMs)
        if (resolved.lines.isNotEmpty() || lyricsResult.lines.isEmpty()) {
            lyricsResult = resolved
        }
        isLoadingLyrics = false
    }

    val lines = lyricsResult.lines
    val hasLyrics = lines.isNotEmpty()
    val isSynced = (lyricsResult.syncType == LyricsSyncType.LINE_SYNCED ||
        lyricsResult.syncType == LyricsSyncType.SYLLABLE_SYNCED) &&
        hasLyrics && lines.first().timestampMs >= 0L
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
    val fsScope = rememberCoroutineScope()
    var isUserScrolling by remember { mutableStateOf(false) }
    val hasPrelude = isSynced && activeIndex == -1

    suspend fun scrollToActive(animated: Boolean) {
        if (!isSynced || activeIndex !in lines.indices) return
        val listIdx = activeIndex + if (hasPrelude) 1 else 0
        val viewportH = listState.layoutInfo.viewportSize.height
        val offset = if (viewportH > 0) -(viewportH * 0.28f).toInt() else 0
        if (animated && !isReducedMotion) {
            listState.animateScrollToItem(listIdx, offset)
        } else {
            listState.scrollToItem(listIdx, offset)
        }
    }

    // Track explicit drag interactions so auto-scroll pauses cleanly while browsing
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
                    resumeJob = fsScope.launch {
                        delay(3000L)
                        isUserScrolling = false
                        scrollToActive(animated = true)
                    }
                }
            }
        }
    }

    // Auto-scroll centered when active line changes during playback
    LaunchedEffect(activeIndex, track.id) {
        if (isSynced && activeIndex in lines.indices && !isUserScrolling) {
            scrollToActive(animated = true)
        }
    }

    val dominantTopColor = remember(resolvedArtworkColors.backgroundTop) {
        resolvedArtworkColors.backgroundTop
    }
    val view = LocalView.current
    val isDarkMode by ThemeManager.isDarkMode.collectAsState()
    if (!view.isInEditMode) {
        DisposableEffect(isDarkMode) {
            updateSystemBarsAppearance(view = view, lightBars = false)
            onDispose {
                updateSystemBarsAppearance(view = view, lightBars = !isDarkMode)
            }
        }
    }

    val bgVerticalGradient = remember(dominantTopColor) {
        Brush.verticalGradient(
            listOf(
                dominantTopColor.copy(alpha = 0.90f),
                Color(0xFF161420),
                Color(0xFF0E0D14),
                Color(0xFF09080D)
            )
        )
    }
    val topMaskGradient = remember(dominantTopColor) {
        Brush.verticalGradient(
            listOf(
                dominantTopColor.copy(alpha = 0.9f),
                Color.Transparent
            )
        )
    }
    val bottomMaskGradient = remember {
        Brush.verticalGradient(
            listOf(
                Color.Transparent,
                Color(0xFF14131B)
            )
        )
    }
    val activeLineBgBrush = remember(primaryColor) {
        Brush.horizontalGradient(
            listOf(
                primaryColor.copy(alpha = 0.24f),
                primaryColor.copy(alpha = 0.08f),
                Color.Transparent
            )
        )
    }
    val activeLineBorderBrush = remember(primaryColor) {
        Brush.horizontalGradient(
            listOf(
                primaryColor.copy(alpha = 0.45f),
                Color.Transparent
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0C0B10))
            .background(bgVerticalGradient)
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
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.10f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
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
                            color = if (useEnglishScript) primaryColor.copy(alpha = 0.22f) else Color(0xFF262332),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (useEnglishScript) primaryColor.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.2f)
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
                                    text = if (useEnglishScript) "Aa English" else "Native",
                                    color = if (useEnglishScript) primaryColor else Color.White,
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
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (lyricsOffsetMs != 0L) primaryColor else Color.White.copy(alpha = 0.10f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Timer,
                                    contentDescription = "Sync Offset Calibration",
                                    tint = if (lyricsOffsetMs != 0L) MaterialTheme.colorScheme.onPrimary else Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Typography Sizing Button
                    IconButton(
                        onClick = { showSizeDialog = true },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.10f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FormatSize,
                                contentDescription = "Typography Scale",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Social Lyrics Quote Card Share Button
                    if (hasLyrics) {
                        IconButton(
                            onClick = { showShareLyricsDialog = true },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.10f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Share,
                                    contentDescription = "Share Quote Card",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
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
                            item(key = "lyrics_prelude", contentType = "lyrics_prelude") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(primaryColor.copy(alpha = 0.18f))
                                        .border(1.dp, primaryColor.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                                        .padding(horizontal = 18.dp, vertical = 14.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Rounded.MusicNote,
                                            contentDescription = null,
                                            tint = primaryColor,
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

                        itemsIndexed(
                            items = lines,
                            key = { index, line -> "${line.timestampMs}_$index" },
                            contentType = { _, _ -> "lyric_line" }
                        ) { index, line ->
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
                                    .background(activeLineBgBrush)
                                    .border(
                                        width = 1.dp,
                                        brush = activeLineBorderBrush,
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
                                                            color = primaryColor.copy(alpha = 0.48f),
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
                                            color = primaryColor.copy(alpha = 0.95f),
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
                            .background(topMaskGradient)
                    )

                    // Bottom Fading Scrim
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(32.dp)
                            .background(bottomMaskGradient)
                    )

                    // Floating Sync pill when user manually browses lyrics
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isSynced && isUserScrolling && activeIndex in lines.indices,
                        enter = fadeIn(),
                        exit = fadeOut(),
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
                                    fsScope.launch { scrollToActive(animated = true) }
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
                                    .background(primaryColor.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.GraphicEq,
                                    contentDescription = null,
                                    tint = primaryColor,
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
                // Scrubber with Living Wavy Scrubber & Duration Counters
                var isScrubbing by remember { mutableStateOf(false) }
                var scrubProgress by remember { mutableFloatStateOf(0f) }
                val progress = (if (isScrubbing) scrubProgress else positionState.progress).coerceIn(0f, 1f)
                val displayedPosMs = if (isScrubbing) (scrubProgress * trackDurationMs).toLong() else currentPos
                val remainingMs = (trackDurationMs - displayedPosMs).coerceAtLeast(0L)

                com.spotify.clone.core.designsystem.WavyAudioScrubber(
                    progress = progress,
                    isPlaying = playbackState.isPlaying,
                    isScrubbing = isScrubbing,
                    activeColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                    trackBgColor = Color.White.copy(alpha = 0.22f),
                    onScrubStart = { frac ->
                        isScrubbing = true
                        scrubProgress = frac.coerceIn(0f, 1f)
                    },
                    onScrubMove = { frac ->
                        scrubProgress = frac.coerceIn(0f, 1f)
                    },
                    onScrubEnd = { frac ->
                        if (trackDurationMs > 0L) {
                            musicController.seekTo((frac.coerceIn(0f, 1f) * trackDurationMs).toLong())
                        }
                        isScrubbing = false
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(displayedPosMs),
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
                            imageVector = Icons.Rounded.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (playbackState.shuffleMode) primaryColor else Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Skip Previous
                    IconButton(onClick = { musicController.skipPrevious() }) {
                        Icon(
                            imageVector = Icons.Rounded.SkipPrevious,
                            contentDescription = "Previous",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // M3 Expressive Shape-Morphing Play / Pause FAB
                    val lyricsPlayCorner by androidx.compose.animation.core.animateDpAsState(
                        targetValue = if (playbackState.isPlaying) 28.dp else 16.dp,
                        animationSpec = EchoMotion.spatialDefault(),
                        label = "lyrics_play_morph"
                    )
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(lyricsPlayCorner))
                            .background(androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer)
                            .echoPressable(scaleOnPress = 0.90f, hapticOnClick = true) {
                                musicController.togglePlayPause()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (playbackState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                            tint = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    // Skip Next
                    IconButton(onClick = { musicController.skipNext() }) {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = "Next",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Toggle Favorite / Like
                    IconButton(onClick = { musicController.toggleFavorite(track) }) {
                        Icon(
                            imageVector = if (track.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (track.isFavorite) primaryColor else Color.White.copy(alpha = 0.6f),
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
