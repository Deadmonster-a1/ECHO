package com.spotify.clone.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode as AnimRepeatMode
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
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.spotify.clone.core.designsystem.EchoAccentCoral
import com.spotify.clone.core.designsystem.EchoArtworkImage
import com.spotify.clone.core.designsystem.EchoHeartButton
import com.spotify.clone.core.designsystem.EchoMarqueeText
import com.spotify.clone.core.designsystem.EchoMotion
import com.spotify.clone.core.designsystem.EchoRadius
import com.spotify.clone.core.designsystem.EchoSpacing
import com.spotify.clone.core.designsystem.ExpressiveLoadingIndicator
import com.spotify.clone.core.designsystem.LivingEqualizer
import com.spotify.clone.core.designsystem.LocalReducedMotion
import com.spotify.clone.core.designsystem.SpotifyJellyfishAura
import com.spotify.clone.core.designsystem.ThemeManager
import com.spotify.clone.core.designsystem.WavyAudioScrubber
import com.spotify.clone.core.designsystem.echoPressable
import com.spotify.clone.core.designsystem.rememberAnimatedArtworkColors
import com.spotify.clone.core.designsystem.tabularFigures
import com.spotify.clone.core.download.TrackDownloadManager
import com.spotify.clone.core.lyrics.LyricsRepository
import com.spotify.clone.core.model.PositionState
import com.spotify.clone.core.model.RepeatMode
import com.spotify.clone.core.player.MusicController
import com.spotify.clone.core.settings.SettingsManager
import com.spotify.clone.ui.components.EqualizerBottomSheet
import com.spotify.clone.ui.components.PlaybackSpeedBottomSheet
import com.spotify.clone.ui.components.SleepTimerBottomSheet
import com.spotify.clone.ui.components.SongCreditsSheet
import com.spotify.clone.ui.components.TrackOptionsBottomSheet
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Material 3 Expressive Fullscreen Now Playing Sheet:
 * - Page 0: Hero artwork, track metadata, wavy scrubber, M3E transport controls, and clean bottom utility bar
 * - Page 1: Independent full-height synchronized lyrics view (no nested scroll conflicts)
 * - 1:1 pull-down to dismiss from Page 0 and 1-tap or swipe transition between Player and Lyrics
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    musicController: MusicController,
    onCollapse: () -> Unit,
    onViewArtist: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDarkMode by ThemeManager.isDarkMode.collectAsState()
    val isReducedMotion = LocalReducedMotion.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    val state by musicController.playbackState.collectAsState()
    val playbackSpeed by musicController.playbackSpeed.collectAsState()
    val sleepTimerRemaining by musicController.sleepTimerRemainingSeconds.collectAsState()
    val lyricsOffsetMs by SettingsManager.lyricsOffsetMs.collectAsState()
    val sizeTierName by SettingsManager.lyricsSizeTier.collectAsState()
    val sizeTier = remember(sizeTierName) {
        LyricsSizeTier.entries.find { it.name == sizeTierName } ?: LyricsSizeTier.STANDARD
    }
    val track = state.currentTrack ?: return

    val downloadManager = remember { TrackDownloadManager.getInstance(context) }
    val downloadedTrackIds by downloadManager.downloadedTrackIds.collectAsState()
    val isDownloaded = remember(track.id, downloadedTrackIds) { track.id in downloadedTrackIds }

    var showQueueSheet by remember { mutableStateOf(false) }
    var showTrackOptionsSheet by remember { mutableStateOf(false) }
    var showSleepTimerSheet by remember { mutableStateOf(false) }
    var showEqualizerSheet by remember { mutableStateOf(false) }
    var showSpeedSheet by remember { mutableStateOf(false) }
    var showFullscreenLyrics by remember { mutableStateOf(false) }
    var showOffsetDialog by remember { mutableStateOf(false) }
    var showSizeDialog by remember { mutableStateOf(false) }
    var showShareLyricsDialog by remember { mutableStateOf(false) }
    var showSongCreditsSheet by remember { mutableStateOf(false) }
    var showGenreSheet by remember { mutableStateOf(false) }

    var isScrubbing by remember { mutableStateOf(false) }
    var scrubProgress by remember { mutableFloatStateOf(0f) }

    var lyricsResult by remember(track.id) {
        mutableStateOf(LyricsRepository.getCachedLyrics(track, state.durationMs))
    }
    LaunchedEffect(track.id, state.durationMs) {
        val fetched = LyricsRepository.getOrFetchLyrics(track, state.durationMs)
        if (fetched.lines.isNotEmpty() || lyricsResult.lines.isEmpty()) {
            lyricsResult = fetched
        }
    }

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
    val isAtLyrics = pagerState.currentPage == 1

    // 1:1 Vertical drag-to-dismiss from Page 0 without interfering with Page 1 lyrics scrolling
    val sheetOffsetY = remember { Animatable(0f) }
    val dismissThresholdPx = with(density) { 140.dp.toPx() }
    val maxSheetRadiusPx = with(density) { 180.dp.toPx() }

    val dismissNestedScroll = remember(pagerState) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (sheetOffsetY.value > 0f && available.y < 0f) {
                    val next = (sheetOffsetY.value + available.y).coerceAtLeast(0f)
                    val consumedY = next - sheetOffsetY.value
                    scope.launch { sheetOffsetY.snapTo(next) }
                    return Offset(0f, consumedY)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (pagerState.currentPage == 0 &&
                    pagerState.currentPageOffsetFraction <= 0.001f &&
                    available.y > 0f &&
                    source == NestedScrollSource.UserInput
                ) {
                    val next = sheetOffsetY.value + available.y
                    scope.launch { sheetOffsetY.snapTo(next) }
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (sheetOffsetY.value > 0f) {
                    val projectedY = sheetOffsetY.value + EchoMotion.projectMomentum(available.y, 0.994f)
                    if (projectedY > dismissThresholdPx || sheetOffsetY.value > dismissThresholdPx) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onCollapse()
                    } else {
                        sheetOffsetY.animateTo(
                            targetValue = 0f,
                            animationSpec = EchoMotion.SpringMomentum,
                            initialVelocity = available.y
                        )
                    }
                    return available
                }
                return Velocity.Zero
            }
        }
    }

    val artOffsetX = remember(track.id) { Animatable(0f) }
    val artSwipeThresholdPx = with(density) { 92.dp.toPx() }

    val artworkColors = rememberAnimatedArtworkColors(track)
    val screenBg = MaterialTheme.colorScheme.background
    val cardBg = MaterialTheme.colorScheme.surfaceContainerHigh
    val primaryColor = MaterialTheme.colorScheme.primary
    val contentPrimary = MaterialTheme.colorScheme.onBackground
    val contentSecondary = MaterialTheme.colorScheme.onSurfaceVariant

    val artworkScale by animateFloatAsState(
        targetValue = if (state.isPlaying) 1.0f else 0.92f,
        animationSpec = if (isReducedMotion) tween(160) else EchoMotion.SpringGentle,
        label = "np_artwork_scale"
    )

    val staticOne = remember { mutableFloatStateOf(1.0f) }
    val auraBreathScaleState: State<Float> = if (state.isPlaying && !isReducedMotion) {
        val infiniteTransition = rememberInfiniteTransition(label = "np_ambient_aura")
        infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 3600, easing = FastOutSlowInEasing),
                repeatMode = AnimRepeatMode.Reverse
            ),
            label = "np_aura_scale"
        )
    } else {
        staticOne
    }

    val ambientGradient = remember(
        isDarkMode,
        artworkColors.backgroundTop,
        artworkColors.backgroundMid,
        artworkColors.vibrantAccent,
        screenBg
    ) {
        if (isDarkMode) {
            Brush.verticalGradient(
                listOf(
                    artworkColors.backgroundTop.copy(alpha = 0.65f),
                    artworkColors.backgroundMid.copy(alpha = 0.40f),
                    screenBg
                )
            )
        } else {
            Brush.verticalGradient(
                listOf(
                    artworkColors.vibrantAccent.copy(alpha = 0.16f),
                    screenBg.copy(alpha = 0.94f),
                    screenBg
                )
            )
        }
    }

    val dragFraction = (sheetOffsetY.value / maxSheetRadiusPx).coerceIn(0f, 1f)
    val sheetCornerRadius = (dragFraction * 28f).dp

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = (1f - dragFraction * 0.5f) * 0.45f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = sheetOffsetY.value
                    val dragScale = 1f - dragFraction * 0.055f
                    scaleX = dragScale
                    scaleY = dragScale
                }
                .clip(RoundedCornerShape(topStart = sheetCornerRadius, topEnd = sheetCornerRadius))
                .background(screenBg)
                .background(ambientGradient)
                .nestedScroll(dismissNestedScroll),
            contentAlignment = Alignment.TopCenter
        ) {
            SpotifyJellyfishAura(
                artworkColors = artworkColors,
                isPlaying = state.isPlaying,
                isDarkMode = isDarkMode,
                intensity = if (isDarkMode) 1.15f else 0.75f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(440.dp)
                    .graphicsLayer {
                        val s = auraBreathScaleState.value
                        scaleX = s
                        scaleY = s
                    }
            )

            BoxWithConstraints(
                modifier = Modifier
                    .widthIn(max = 540.dp)
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                VerticalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    if (page == 0) {
                        // ==========================================
                        // PAGE 0: Material 3 Expressive Player Hero
                        // ==========================================
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(8.dp))

                            // Subtle Pill Drag Handle
                            Box(
                                modifier = Modifier
                                    .size(width = 36.dp, height = 4.dp)
                                    .clip(CircleShape)
                                    .background(contentSecondary.copy(alpha = 0.34f))
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Clean Tonal Top Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                PlayerHeaderIconButton(
                                    icon = Icons.Rounded.KeyboardArrowDown,
                                    contentDescription = "Minimize",
                                    tint = contentPrimary,
                                    onClick = onCollapse
                                )

                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 12.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (state.isPlaying) {
                                        LivingEqualizer(
                                            isPlaying = true,
                                            barCount = 3,
                                            barColor = primaryColor,
                                            barWidth = 2.dp,
                                            barSpacing = 1.5.dp,
                                            maxHeight = 10.dp,
                                            minHeight = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Text(
                                        text = track.albumName.ifBlank { "Now Playing" },
                                        color = contentSecondary,
                                        style = MaterialTheme.typography.labelLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                PlayerHeaderIconButton(
                                    icon = Icons.Rounded.MoreHoriz,
                                    contentDescription = "More Options",
                                    tint = contentPrimary,
                                    onClick = { showTrackOptionsSheet = true }
                                )
                            }

                            // Hero Album Artwork (swipe left/right to skip)
                            BoxWithConstraints(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                val artSize = min(min(maxWidth * 0.92f, maxHeight * 0.94f), 344.dp)

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .pointerInput(track.id) {
                                            val velocityTracker = VelocityTracker()
                                            detectHorizontalDragGestures(
                                                onDragStart = { velocityTracker.resetTracking() },
                                                onDragCancel = {
                                                    scope.launch { artOffsetX.animateTo(0f, EchoMotion.SpringMomentum) }
                                                },
                                                onDragEnd = {
                                                    val velX = velocityTracker.calculateVelocity().x
                                                    val projected = artOffsetX.value + EchoMotion.projectMomentum(velX, 0.992f)
                                                    when {
                                                        projected < -artSwipeThresholdPx || artOffsetX.value < -artSwipeThresholdPx -> {
                                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                            musicController.skipNext()
                                                            scope.launch { artOffsetX.animateTo(0f, EchoMotion.SpringMomentum) }
                                                        }
                                                        projected > artSwipeThresholdPx || artOffsetX.value > artSwipeThresholdPx -> {
                                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                            musicController.skipPrevious()
                                                            scope.launch { artOffsetX.animateTo(0f, EchoMotion.SpringMomentum) }
                                                        }
                                                        else -> {
                                                            scope.launch {
                                                                artOffsetX.animateTo(0f, EchoMotion.SpringMomentum, initialVelocity = velX)
                                                            }
                                                        }
                                                    }
                                                },
                                                onHorizontalDrag = { change, dragAmount ->
                                                    change.consume()
                                                    velocityTracker.addPosition(change.uptimeMillis, change.position)
                                                    scope.launch {
                                                        val damped = EchoMotion.rubberBand(artOffsetX.value + dragAmount, 240f, 0.7f)
                                                        artOffsetX.snapTo(damped)
                                                    }
                                                }
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    AnimatedContent(
                                        targetState = track,
                                        contentKey = { it.id },
                                        transitionSpec = {
                                            if (isReducedMotion) {
                                                fadeIn(tween(180)) togetherWith fadeOut(tween(140))
                                            } else {
                                                (slideInHorizontally(tween(300, easing = EchoMotion.EaseOutStrong)) { it / 4 } +
                                                    fadeIn(tween(240, easing = EchoMotion.EaseOutStrong)) +
                                                    scaleIn(initialScale = 0.94f, animationSpec = tween(280, easing = EchoMotion.EaseOutStrong))) togetherWith
                                                    (slideOutHorizontally(tween(220)) { -it / 5 } +
                                                        fadeOut(tween(180)) +
                                                        scaleOut(targetScale = 0.94f, animationSpec = tween(200)))
                                            }
                                        },
                                        label = "np_artwork_swap"
                                    ) { current ->
                                        Box(
                                            modifier = Modifier
                                                .size(artSize)
                                                .graphicsLayer {
                                                    scaleX = artworkScale
                                                    scaleY = artworkScale
                                                    translationX = artOffsetX.value
                                                    rotationZ = if (isReducedMotion) 0f else (artOffsetX.value / 240f) * 3.0f
                                                }
                                                .shadow(
                                                    elevation = if (state.isPlaying) 20.dp else 8.dp,
                                                    shape = RoundedCornerShape(EchoRadius.xl),
                                                    ambientColor = artworkColors.dominant.copy(alpha = 0.28f),
                                                    spotColor = artworkColors.dominant.copy(alpha = 0.40f)
                                                )
                                                .clip(RoundedCornerShape(EchoRadius.xl))
                                                .background(cardBg),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            EchoArtworkImage(
                                                imageUrl = current.coverArtUrl,
                                                contentDescription = current.albumName,
                                                seedKey = "${current.title}_${current.artistName}",
                                                iconSize = 72.dp,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }
                                }
                            }

                            // Title, Artist & Favorite
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AnimatedContent(
                                    targetState = track,
                                    contentKey = { it.id },
                                    transitionSpec = {
                                        if (isReducedMotion) {
                                            fadeIn(tween(140)) togetherWith fadeOut(tween(120))
                                        } else {
                                            (slideInVertically(tween(220, easing = EchoMotion.EaseOutStrong)) { it / 4 } +
                                                fadeIn(tween(200, easing = EchoMotion.EaseOutStrong))) togetherWith
                                                (slideOutVertically(tween(160)) { -it / 5 } + fadeOut(tween(140)))
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    label = "np_title_swap"
                                ) { current ->
                                    Column {
                                        EchoMarqueeText(
                                            text = current.title,
                                            color = contentPrimary,
                                            style = MaterialTheme.typography.headlineSmall.copy(
                                                fontSize = 22.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                letterSpacing = (-0.4).sp
                                            ),
                                            enabled = state.isPlaying
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.echoPressable(scaleOnPress = 0.97f) {
                                                if (current.artistName.isNotBlank()) {
                                                    onViewArtist?.invoke(current.artistName)
                                                }
                                            }
                                        ) {
                                            if (isDownloaded) {
                                                Icon(
                                                    imageVector = Icons.Rounded.DownloadDone,
                                                    contentDescription = "Downloaded",
                                                    tint = primaryColor,
                                                    modifier = Modifier
                                                        .padding(end = 6.dp)
                                                        .size(15.dp)
                                                )
                                            }
                                            EchoMarqueeText(
                                                text = current.artistName,
                                                color = contentSecondary,
                                                style = MaterialTheme.typography.bodyLarge.copy(
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Medium
                                                ),
                                                enabled = state.isPlaying,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            if (onViewArtist != null && current.artistName.isNotBlank()) {
                                                Icon(
                                                    imageVector = Icons.Rounded.ChevronRight,
                                                    contentDescription = "View Artist",
                                                    tint = contentSecondary.copy(alpha = 0.7f),
                                                    modifier = Modifier.size(17.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                EchoHeartButton(
                                    isFavorite = track.isFavorite,
                                    onToggle = { musicController.toggleFavorite(track) },
                                    iconSize = 26.dp
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Living Wavy Scrubber & Tabular Timestamps
                            NowPlayingScrubberSection(
                                positionStateFlow = musicController.positionState,
                                fallbackDurationMs = state.durationMs,
                                isPlaying = state.isPlaying,
                                isScrubbing = isScrubbing,
                                scrubProgress = scrubProgress,
                                activeColor = primaryColor,
                                contentPrimary = contentPrimary,
                                contentSecondary = contentSecondary,
                                onScrubStart = { p ->
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    isScrubbing = true
                                    scrubProgress = p
                                },
                                onScrubMove = { p -> scrubProgress = p },
                                onScrubEnd = { p, totalDuration ->
                                    if (totalDuration > 0L) {
                                        musicController.seekTo((p * totalDuration).toLong())
                                    }
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    isScrubbing = false
                                }
                            )

                            state.errorMessage?.let { errMsg ->
                                Spacer(modifier = Modifier.height(8.dp))
                                val errShape = RoundedCornerShape(EchoRadius.sm)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(errShape)
                                        .background(EchoAccentCoral.copy(alpha = if (isDarkMode) 0.18f else 0.12f))
                                        .echoPressable(scaleOnPress = 0.985f, hapticOnClick = true) {
                                            musicController.playTrack(track, state.queue)
                                        }
                                        .padding(horizontal = 14.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ErrorOutline,
                                        contentDescription = "Playback Error",
                                        tint = EchoAccentCoral,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = errMsg,
                                        color = contentPrimary,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Retry",
                                        color = primaryColor,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Material 3 Expressive Transport Cluster
                            val playFabCorner by animateDpAsState(
                                targetValue = if (state.isPlaying) 36.dp else 22.dp,
                                animationSpec = if (isReducedMotion) tween(100) else EchoMotion.spatialDefault(),
                                label = "np_fab_morph"
                            )
                            val playFabShape = RoundedCornerShape(playFabCorner)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TransportToggleIcon(
                                    icon = Icons.Rounded.Shuffle,
                                    contentDescription = "Shuffle",
                                    isActive = state.shuffleMode,
                                    onClick = { musicController.toggleShuffle() }
                                )

                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                        .echoPressable(scaleOnPress = 0.88f, hapticOnClick = true) {
                                            musicController.skipPrevious()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.SkipPrevious,
                                        contentDescription = "Previous",
                                        tint = contentPrimary,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(EchoSpacing.playFabHero)
                                        .shadow(
                                            elevation = if (state.isPlaying) 16.dp else 8.dp,
                                            shape = playFabShape,
                                            ambientColor = primaryColor.copy(alpha = 0.28f),
                                            spotColor = primaryColor.copy(alpha = 0.38f)
                                        )
                                        .clip(playFabShape)
                                        .background(primaryColor)
                                        .echoPressable(scaleOnPress = 0.90f, hapticOnClick = true) {
                                            musicController.togglePlayPause()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (state.isLoading && !state.isPlaying) {
                                        ExpressiveLoadingIndicator(
                                            size = 38.dp,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            containerColor = Color.Transparent
                                        )
                                    } else {
                                        AnimatedContent(
                                            targetState = state.isPlaying,
                                            transitionSpec = {
                                                (fadeIn(tween(140, easing = EchoMotion.EaseOutStrong)) +
                                                    scaleIn(initialScale = 0.82f, animationSpec = EchoMotion.spatialFast())) togetherWith
                                                    (fadeOut(tween(110)) + scaleOut(targetScale = 0.82f, animationSpec = tween(110)))
                                            },
                                            label = "np_play_pause_icon"
                                        ) { playing ->
                                            Icon(
                                                imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                                contentDescription = if (playing) "Pause" else "Play",
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(36.dp)
                                            )
                                        }
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                        .echoPressable(scaleOnPress = 0.88f, hapticOnClick = true) {
                                            musicController.skipNext()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.SkipNext,
                                        contentDescription = "Next",
                                        tint = contentPrimary,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }

                                TransportToggleIcon(
                                    icon = if (state.repeatMode == RepeatMode.ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                                    contentDescription = "Repeat",
                                    isActive = state.repeatMode != RepeatMode.OFF,
                                    onClick = { musicController.toggleRepeat() }
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Clean M3 Expressive Bottom Utility Dock
                            val nextTrack = state.queue.getOrNull(state.queueIndex + 1)
                                ?: if (state.repeatMode == RepeatMode.ALL && state.queue.size > 1) state.queue.firstOrNull() else null

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Open Lyrics Page
                                PlayerUtilitySquareButton(
                                    icon = Icons.Rounded.Lyrics,
                                    contentDescription = "Lyrics",
                                    isActive = isAtLyrics,
                                    onClick = { scope.launch { pagerState.animateScrollToPage(1) } }
                                )

                                // Center Contextual Status Pill
                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    when {
                                        sleepTimerRemaining != null -> {
                                            val mins = (sleepTimerRemaining ?: 0L) / 60L
                                            val secs = (sleepTimerRemaining ?: 0L) % 60L
                                            Row(
                                                modifier = Modifier
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                                    .echoPressable(scaleOnPress = 0.95f, hapticOnClick = true) {
                                                        showSleepTimerSheet = true
                                                    }
                                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Bedtime,
                                                    contentDescription = "Sleep Timer Active",
                                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = String.format(Locale.US, "%d:%02d", mins, secs),
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    style = MaterialTheme.typography.labelLarge.tabularFigures()
                                                )
                                            }
                                        }
                                        playbackSpeed != 1.0f -> {
                                            Row(
                                                modifier = Modifier
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                                    .echoPressable(scaleOnPress = 0.95f, hapticOnClick = true) {
                                                        showSpeedSheet = true
                                                    }
                                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Speed,
                                                    contentDescription = "Playback Speed",
                                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "${playbackSpeed}x",
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    style = MaterialTheme.typography.labelLarge.tabularFigures()
                                                )
                                            }
                                        }
                                        nextTrack != null -> {
                                            Row(
                                                modifier = Modifier
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                                    .echoPressable(scaleOnPress = 0.96f, hapticOnClick = true) {
                                                        showQueueSheet = true
                                                    }
                                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Up Next · ${nextTrack.title}",
                                                    color = contentSecondary,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                        else -> {
                                            Row(
                                                modifier = Modifier
                                                    .clip(CircleShape)
                                                    .echoPressable(scaleOnPress = 0.96f, hapticOnClick = true) {
                                                        scope.launch { pagerState.animateScrollToPage(1) }
                                                    }
                                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.KeyboardArrowUp,
                                                    contentDescription = null,
                                                    tint = contentSecondary.copy(alpha = 0.72f),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Lyrics",
                                                    color = contentSecondary.copy(alpha = 0.80f),
                                                    style = MaterialTheme.typography.labelMedium
                                                )
                                            }
                                        }
                                    }
                                }

                                // Quick Equalizer
                                PlayerUtilitySquareButton(
                                    icon = Icons.Rounded.Equalizer,
                                    contentDescription = "Equalizer",
                                    isActive = false,
                                    onClick = { showEqualizerSheet = true }
                                )

                                // Queue
                                PlayerUtilitySquareButton(
                                    icon = Icons.AutoMirrored.Rounded.QueueMusic,
                                    contentDescription = "Queue",
                                    isActive = false,
                                    onClick = { showQueueSheet = true }
                                )
                            }
                        }
                    } else {
                        // ==========================================
                        // PAGE 1: Dedicated Scrollable Lyrics Page
                        // ==========================================
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp)
                        ) {
                            Spacer(modifier = Modifier.height(8.dp))

                            // Clean header with Back-to-Player + Quick Lyrics Actions
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                        .echoPressable(scaleOnPress = 0.94f, hapticOnClick = true) {
                                            scope.launch { pagerState.animateScrollToPage(0) }
                                        }
                                        .padding(start = 10.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.KeyboardArrowDown,
                                        contentDescription = "Back to Player",
                                        tint = contentPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Player",
                                        color = contentPrimary,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (lyricsResult.lines.isNotEmpty()) {
                                        PlayerHeaderIconButton(
                                            icon = Icons.Rounded.Timer,
                                            contentDescription = "Sync Offset",
                                            tint = if (lyricsOffsetMs != 0L) MaterialTheme.colorScheme.onPrimaryContainer else contentPrimary,
                                            containerColor = if (lyricsOffsetMs != 0L) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                            onClick = { showOffsetDialog = true }
                                        )
                                        PlayerHeaderIconButton(
                                            icon = Icons.Rounded.FormatSize,
                                            contentDescription = "Text Size",
                                            tint = contentPrimary,
                                            onClick = { showSizeDialog = true }
                                        )
                                        PlayerHeaderIconButton(
                                            icon = Icons.Rounded.Share,
                                            contentDescription = "Share Lyrics",
                                            tint = contentPrimary,
                                            onClick = { showShareLyricsDialog = true }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Full-height independent scrollable lyrics container
                            ModernLyricsView(
                                lyricsResult = lyricsResult,
                                currentPositionMs = musicController.positionState.value.currentPositionMs,
                                artworkColors = artworkColors,
                                track = track,
                                isPlaying = state.isPlaying,
                                playbackSpeed = playbackSpeed,
                                positionStateFlow = musicController.positionState,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(bottom = 14.dp),
                                onSeekTo = { posMs -> musicController.seekTo(posMs) },
                                onFullscreenClick = { showFullscreenLyrics = true }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showQueueSheet) {
        ModalBottomSheet(
            onDismissRequest = { showQueueSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            QueueSheetContent(
                playbackState = state,
                musicController = musicController,
                onTrackClick = { selected -> musicController.playTrack(selected, state.queue) }
            )
        }
    }

    if (showTrackOptionsSheet) {
        TrackOptionsBottomSheet(
            track = track,
            musicController = musicController,
            onDismiss = { showTrackOptionsSheet = false },
            onSleepTimerClick = { showSleepTimerSheet = true },
            onEqualizerClick = { showEqualizerSheet = true },
            onSpeedClick = { showSpeedSheet = true },
            onGenreRadioClick = { showGenreSheet = true },
            onSongCreditsClick = { showSongCreditsSheet = true },
            onViewArtist = { artistName -> onViewArtist?.invoke(artistName) }
        )
    }

    if (showSongCreditsSheet) {
        SongCreditsSheet(track = track, onDismiss = { showSongCreditsSheet = false })
    }

    if (showSleepTimerSheet) {
        SleepTimerBottomSheet(
            musicController = musicController,
            onDismiss = { showSleepTimerSheet = false }
        )
    }

    if (showEqualizerSheet) {
        EqualizerBottomSheet(onDismiss = { showEqualizerSheet = false })
    }

    if (showSpeedSheet) {
        PlaybackSpeedBottomSheet(
            musicController = musicController,
            onDismiss = { showSpeedSheet = false }
        )
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

    if (showShareLyricsDialog) {
        ShareLyricsDialog(
            track = track,
            lines = lyricsResult.lines,
            activeLineIndex = 0,
            artworkColors = artworkColors,
            onDismiss = { showShareLyricsDialog = false }
        )
    }

    if (showFullscreenLyrics) {
        FullscreenLyricsSheet(
            track = track,
            musicController = musicController,
            onClose = { showFullscreenLyrics = false },
            artworkColors = artworkColors
        )
    }

    if (showGenreSheet) {
        GenrePickerBottomSheet(
            musicController = musicController,
            onDismiss = { showGenreSheet = false }
        )
    }
}

@Composable
private fun PlayerHeaderIconButton(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(containerColor)
            .echoPressable(scaleOnPress = 0.90f, hapticOnClick = true, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun PlayerUtilitySquareButton(
    icon: ImageVector,
    contentDescription: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(
        targetValue = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = tween(180),
        label = "util_btn_bg"
    )
    val tint by animateColorAsState(
        targetValue = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(180),
        label = "util_btn_tint"
    )
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .echoPressable(scaleOnPress = 0.88f, hapticOnClick = true, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(21.dp)
        )
    }
}

@Composable
private fun TransportToggleIcon(
    icon: ImageVector,
    contentDescription: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(
        targetValue = if (isActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        animationSpec = tween(EchoMotion.DurationMicro, easing = EchoMotion.EaseOutStrong),
        label = "transport_toggle_bg"
    )
    val tint by animateColorAsState(
        targetValue = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
        animationSpec = tween(EchoMotion.DurationMicro, easing = EchoMotion.EaseOutStrong),
        label = "transport_toggle_tint"
    )

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(bg)
            .echoPressable(scaleOnPress = 0.88f, hapticOnClick = true, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun NowPlayingScrubberSection(
    positionStateFlow: StateFlow<PositionState>,
    fallbackDurationMs: Long,
    isPlaying: Boolean,
    isScrubbing: Boolean,
    scrubProgress: Float,
    activeColor: Color,
    contentPrimary: Color,
    contentSecondary: Color,
    onScrubStart: (Float) -> Unit,
    onScrubMove: (Float) -> Unit,
    onScrubEnd: (Float, Long) -> Unit
) {
    val posState by positionStateFlow.collectAsState()
    val totalDuration = if (posState.durationMs > 0L) posState.durationMs else fallbackDurationMs
    val rawProgress = if (totalDuration > 0L) {
        (posState.currentPositionMs.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val displayProgress = (if (isScrubbing) scrubProgress else rawProgress).coerceIn(0f, 1f)

    WavyAudioScrubber(
        progress = displayProgress,
        isPlaying = isPlaying,
        isScrubbing = isScrubbing,
        activeColor = activeColor,
        trackBgColor = contentPrimary.copy(alpha = 0.16f),
        onScrubStart = onScrubStart,
        onScrubMove = onScrubMove,
        onScrubEnd = { p -> onScrubEnd(p, totalDuration) }
    )

    Spacer(modifier = Modifier.height(4.dp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val shownPosition = if (isScrubbing) (scrubProgress * totalDuration).toLong() else posState.currentPositionMs
        val remaining = (totalDuration - shownPosition).coerceAtLeast(0L)
        val timeStyle = MaterialTheme.typography.bodySmall.tabularFigures().copy(
            fontSize = 12.sp,
            fontWeight = if (isScrubbing) FontWeight.SemiBold else FontWeight.Medium
        )
        Text(
            text = formatTime(shownPosition),
            color = if (isScrubbing) contentPrimary else contentSecondary,
            style = timeStyle
        )
        Text(
            text = "-${formatTime(remaining)}",
            color = if (isScrubbing) contentPrimary else contentSecondary,
            style = timeStyle
        )
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
    val mins = totalSeconds / 60L
    val secs = totalSeconds % 60L
    return String.format(Locale.US, "%d:%02d", mins, secs)
}
