package com.spotify.clone.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode as AnimRepeatMode
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
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lyrics
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.spotify.clone.core.designsystem.EchoAccentCoral
import com.spotify.clone.core.designsystem.EchoHeartButton
import com.spotify.clone.core.designsystem.EchoMarqueeText
import com.spotify.clone.core.designsystem.EchoMotion
import com.spotify.clone.core.designsystem.EchoRadius
import com.spotify.clone.core.designsystem.EchoSpacing
import com.spotify.clone.core.designsystem.LivingEqualizer
import com.spotify.clone.core.designsystem.LocalAppTheme
import com.spotify.clone.core.designsystem.LocalReducedMotion
import com.spotify.clone.core.designsystem.SpotifyJellyfishAura
import com.spotify.clone.core.designsystem.ThemeManager
import com.spotify.clone.core.designsystem.WavyAudioScrubber
import com.spotify.clone.core.designsystem.echoContrastOn
import com.spotify.clone.core.designsystem.echoGlassSurface
import com.spotify.clone.core.designsystem.echoPressable
import com.spotify.clone.core.designsystem.rememberAnimatedArtworkColors
import com.spotify.clone.core.designsystem.resolvedBackground
import com.spotify.clone.core.designsystem.resolvedBorder
import com.spotify.clone.core.designsystem.resolvedCard
import com.spotify.clone.core.designsystem.tabularFigures
import com.spotify.clone.core.download.TrackDownloadManager
import com.spotify.clone.core.lyrics.LyricsRepository
import com.spotify.clone.core.model.RepeatMode
import com.spotify.clone.core.player.MusicController
import com.spotify.clone.core.settings.SettingsManager
import com.spotify.clone.ui.components.EqualizerBottomSheet
import com.spotify.clone.ui.components.PlaybackSpeedBottomSheet
import com.spotify.clone.ui.components.SleepTimerBottomSheet
import com.spotify.clone.ui.components.SongCreditsSheet
import com.spotify.clone.ui.components.TrackOptionsBottomSheet
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Fullscreen Now Playing sheet with:
 * - Page 1: Album artwork, track info, wavy scrubber, and transport controls
 * - Scroll down: Synced English Lyrics section synced to real-time playback via LyricsSyncEngine
 * - Pull down from top: Smooth 1:1 drag-to-dismiss
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
    val activeTheme = LocalAppTheme.current
    val isDarkMode by ThemeManager.isDarkMode.collectAsState()
    val isReducedMotion = LocalReducedMotion.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    val state by musicController.playbackState.collectAsState()
    val playbackSpeed by musicController.playbackSpeed.collectAsState()
    val sleepTimerRemaining by musicController.sleepTimerRemainingSeconds.collectAsState()
    val lyricsOffsetMs by SettingsManager.lyricsOffsetMs.collectAsState()
    val track = state.currentTrack ?: return

    val downloadManager = remember { TrackDownloadManager.getInstance(context) }
    val isDownloaded = downloadManager.isTrackDownloaded(track.id)

    var showQueueSheet by remember { mutableStateOf(false) }
    var showTrackOptionsSheet by remember { mutableStateOf(false) }
    var showSleepTimerSheet by remember { mutableStateOf(false) }
    var showEqualizerSheet by remember { mutableStateOf(false) }
    var showSpeedSheet by remember { mutableStateOf(false) }
    var showFullscreenLyrics by remember { mutableStateOf(false) }
    var showOffsetDialog by remember { mutableStateOf(false) }
    var showSongCreditsSheet by remember { mutableStateOf(false) }
    var showGenreSheet by remember { mutableStateOf(false) }

    var isScrubbing by remember { mutableStateOf(false) }
    var scrubProgress by remember { mutableFloatStateOf(0f) }

    // Fetch & normalize English synced lyrics whenever track or duration updates
    var lyricsResult by remember(track.id) {
        mutableStateOf(LyricsRepository.getCachedLyrics(track, state.durationMs))
    }
    LaunchedEffect(track.id, state.durationMs) {
        val fetched = LyricsRepository.getOrFetchLyrics(track, state.durationMs)
        if (fetched.lines.isNotEmpty() || lyricsResult.lines.isEmpty()) {
            lyricsResult = fetched
        }
    }

    val scrollState = rememberScrollState()

    // 1:1 Vertical drag-to-dismiss state integrated with nested scroll at top
    val sheetOffsetY = remember { Animatable(0f) }
    val dismissThresholdPx = with(density) { 140.dp.toPx() }
    val maxSheetRadiusPx = with(density) { 180.dp.toPx() }

    val dismissNestedScroll = remember(scrollState) {
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
                if (scrollState.value == 0 && available.y > 0f && source == NestedScrollSource.UserInput) {
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

    // 1:1 Horizontal artwork swipe state
    val artOffsetX = remember(track.id) { Animatable(0f) }
    val artSwipeThresholdPx = with(density) { 92.dp.toPx() }

    val artworkColors = rememberAnimatedArtworkColors(track)
    val screenBg = activeTheme.resolvedBackground(isDarkMode)
    val cardBg = activeTheme.resolvedCard(isDarkMode)
    val borderColor = activeTheme.resolvedBorder(isDarkMode)
    val contentPrimary = MaterialTheme.colorScheme.onBackground
    val contentSecondary = MaterialTheme.colorScheme.onSurfaceVariant

    val artworkScale by animateFloatAsState(
        targetValue = if (state.isPlaying) 1.0f else 0.90f,
        animationSpec = if (isReducedMotion) tween(160) else EchoMotion.SpringGentle,
        label = "np_artwork_scale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "np_ambient_aura")
    val auraBreathScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (state.isPlaying && !isReducedMotion) 1.08f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = FastOutSlowInEasing),
            repeatMode = AnimRepeatMode.Reverse
        ),
        label = "np_aura_scale"
    )

    val ambientGradient = if (isDarkMode) {
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
                        scaleX = auraBreathScale
                        scaleY = auraBreathScale
                    }
            )

            BoxWithConstraints(
                modifier = Modifier
                    .widthIn(max = 540.dp)
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                val playerPageHeight = maxHeight
                val lyricsSectionTopPx = with(density) { (playerPageHeight - 16.dp).toPx().toInt() }
                val isAtLyrics = scrollState.value > (lyricsSectionTopPx / 2)

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // ==========================================
                    // SECTION 1: Main Music Player Hero Page
                    // ==========================================
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(playerPageHeight),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))

                        // Grab Handle
                        Box(
                            modifier = Modifier
                                .size(width = 38.dp, height = 4.dp)
                                .clip(CircleShape)
                                .background(contentSecondary.copy(alpha = 0.38f))
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Minimal Top Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(EchoSpacing.minTouchTarget)
                                    .clip(CircleShape)
                                    .echoPressable(scaleOnPress = 0.88f, hapticOnClick = true, onClick = onCollapse),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.KeyboardArrowDown,
                                    contentDescription = "Minimize",
                                    tint = contentPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

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
                                        barColor = activeTheme.primaryColor,
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
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(EchoSpacing.minTouchTarget)
                                    .clip(CircleShape)
                                    .echoPressable(scaleOnPress = 0.88f, hapticOnClick = true) { showTrackOptionsSheet = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.MoreHoriz,
                                    contentDescription = "More Options",
                                    tint = contentPrimary
                                )
                            }
                        }

                        // Hero Album Artwork (swipe left/right to skip)
                        BoxWithConstraints(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val artSize = min(min(maxWidth * 0.92f, maxHeight * 0.94f), 348.dp)

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
                                                elevation = if (state.isPlaying) 18.dp else 6.dp,
                                                shape = RoundedCornerShape(EchoRadius.lg),
                                                ambientColor = artworkColors.dominant.copy(alpha = 0.24f),
                                                spotColor = artworkColors.dominant.copy(alpha = 0.36f)
                                            )
                                            .clip(RoundedCornerShape(EchoRadius.lg))
                                            .background(cardBg),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AsyncImage(
                                            model = current.coverArtUrl,
                                            contentDescription = current.albumName,
                                            contentScale = ContentScale.Crop,
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
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = (-0.4).sp
                                        ),
                                        enabled = state.isPlaying
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (isDownloaded) {
                                            Icon(
                                                imageVector = Icons.Filled.DownloadDone,
                                                contentDescription = "Downloaded",
                                                tint = activeTheme.primaryColor,
                                                modifier = Modifier
                                                    .padding(end = 6.dp)
                                                    .size(15.dp)
                                            )
                                        }
                                        EchoMarqueeText(
                                            text = current.artistName,
                                            color = contentSecondary,
                                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.5.sp),
                                            enabled = state.isPlaying,
                                            modifier = Modifier.echoPressable(scaleOnPress = 0.97f) {
                                                onViewArtist?.invoke(current.artistName)
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            EchoHeartButton(
                                isFavorite = track.isFavorite,
                                onToggle = { musicController.toggleFavorite(track) },
                                iconSize = 26.dp
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Scrubber & Timestamps
                        val displayProgress = (if (isScrubbing) scrubProgress else state.progress).coerceIn(0f, 1f)
                        WavyAudioScrubber(
                            progress = displayProgress,
                            isPlaying = state.isPlaying,
                            isScrubbing = isScrubbing,
                            activeColor = activeTheme.primaryColor,
                            trackBgColor = contentPrimary.copy(alpha = 0.16f),
                            onScrubStart = { p ->
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                isScrubbing = true
                                scrubProgress = p
                            },
                            onScrubMove = { p -> scrubProgress = p },
                            onScrubEnd = { p ->
                                if (state.durationMs > 0L) {
                                    musicController.seekTo((p * state.durationMs).toLong())
                                }
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                isScrubbing = false
                            }
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val shownPosition = if (isScrubbing) (scrubProgress * state.durationMs).toLong() else state.currentPositionMs
                            val remaining = (state.durationMs - shownPosition).coerceAtLeast(0L)
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

                        state.errorMessage?.let { errMsg ->
                            Spacer(modifier = Modifier.height(8.dp))
                            val errShape = RoundedCornerShape(EchoRadius.sm)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(errShape)
                                    .background(EchoAccentCoral.copy(alpha = if (isDarkMode) 0.16f else 0.10f))
                                    .border(0.75.dp, EchoAccentCoral.copy(alpha = 0.38f), errShape)
                                    .echoPressable(scaleOnPress = 0.985f, hapticOnClick = true) {
                                        musicController.playTrack(track, state.queue)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ErrorOutline,
                                    contentDescription = "Playback Error",
                                    tint = EchoAccentCoral,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = errMsg,
                                    color = contentPrimary,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Retry",
                                    color = activeTheme.primaryColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Primary Transport Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TransportToggleIcon(
                                icon = Icons.Filled.Shuffle,
                                contentDescription = "Shuffle",
                                isActive = state.shuffleMode,
                                activeColor = activeTheme.primaryColor,
                                inactiveColor = contentSecondary.copy(alpha = 0.55f),
                                onClick = { musicController.toggleShuffle() }
                            )

                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .echoPressable(scaleOnPress = 0.86f, hapticOnClick = true) {
                                        musicController.skipPrevious()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.SkipPrevious,
                                    contentDescription = "Previous",
                                    tint = contentPrimary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .shadow(
                                        elevation = if (state.isPlaying) 14.dp else 6.dp,
                                        shape = CircleShape,
                                        ambientColor = activeTheme.primaryColor.copy(alpha = 0.25f),
                                        spotColor = activeTheme.primaryColor.copy(alpha = 0.35f)
                                    )
                                    .clip(CircleShape)
                                    .background(activeTheme.primaryColor)
                                    .echoPressable(scaleOnPress = 0.90f, hapticOnClick = true) {
                                        musicController.togglePlayPause()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (state.isLoading && !state.isPlaying) {
                                    CircularProgressIndicator(
                                        color = echoContrastOn(activeTheme.primaryColor),
                                        strokeWidth = 2.5.dp,
                                        modifier = Modifier.size(30.dp)
                                    )
                                } else {
                                    AnimatedContent(
                                        targetState = state.isPlaying,
                                        transitionSpec = {
                                            (fadeIn(tween(140, easing = EchoMotion.EaseOutStrong)) +
                                                scaleIn(initialScale = 0.85f, animationSpec = tween(140))) togetherWith
                                                (fadeOut(tween(110)) + scaleOut(targetScale = 0.85f, animationSpec = tween(110)))
                                        },
                                        label = "np_play_pause_icon"
                                    ) { playing ->
                                        Icon(
                                            imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                            contentDescription = if (playing) "Pause" else "Play",
                                            tint = echoContrastOn(activeTheme.primaryColor),
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .echoPressable(scaleOnPress = 0.86f, hapticOnClick = true) {
                                        musicController.skipNext()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.SkipNext,
                                    contentDescription = "Next",
                                    tint = contentPrimary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            TransportToggleIcon(
                                icon = if (state.repeatMode == RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                                contentDescription = "Repeat",
                                isActive = state.repeatMode != RepeatMode.OFF,
                                activeColor = activeTheme.primaryColor,
                                inactiveColor = contentSecondary.copy(alpha = 0.55f),
                                onClick = { musicController.toggleRepeat() }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Bottom Utility Row: Lyrics Scroll Jump | Contextual Pill | Queue
                        val nextTrack = state.queue.getOrNull(state.queueIndex + 1)
                        val lyricsBg by animateColorAsState(
                            targetValue = if (isAtLyrics) activeTheme.primaryColor.copy(alpha = 0.16f) else Color.Transparent,
                            animationSpec = tween(180),
                            label = "np_lyrics_btn_bg"
                        )
                        val lyricsTint by animateColorAsState(
                            targetValue = if (isAtLyrics) activeTheme.primaryColor else contentSecondary,
                            animationSpec = tween(180),
                            label = "np_lyrics_btn_tint"
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(EchoSpacing.minTouchTarget)
                                    .clip(CircleShape)
                                    .background(lyricsBg)
                                    .echoPressable(scaleOnPress = 0.88f, hapticOnClick = true) {
                                        scope.launch {
                                            if (isAtLyrics) {
                                                scrollState.animateScrollTo(0)
                                            } else {
                                                scrollState.animateScrollTo(lyricsSectionTopPx)
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Lyrics,
                                    contentDescription = "Scroll to Lyrics",
                                    tint = lyricsTint,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            when {
                                sleepTimerRemaining != null -> {
                                    val mins = (sleepTimerRemaining ?: 0L) / 60L
                                    val secs = (sleepTimerRemaining ?: 0L) % 60L
                                    val pillShape = RoundedCornerShape(EchoRadius.pill)
                                    Row(
                                        modifier = Modifier
                                            .echoGlassSurface(
                                                isDarkMode = isDarkMode,
                                                shape = pillShape,
                                                fallbackBorder = activeTheme.primaryColor.copy(alpha = 0.35f),
                                                tintColor = activeTheme.primaryColor,
                                                elevation = 6.dp
                                            )
                                            .echoPressable(scaleOnPress = 0.95f, hapticOnClick = true) {
                                                showSleepTimerSheet = true
                                            }
                                            .padding(horizontal = 14.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Bedtime,
                                            contentDescription = "Sleep Timer Active",
                                            tint = activeTheme.primaryColor,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = String.format(Locale.US, "%d:%02d", mins, secs),
                                            color = activeTheme.primaryColor,
                                            style = MaterialTheme.typography.labelMedium.tabularFigures().copy(
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        )
                                    }
                                }
                                playbackSpeed != 1.0f -> {
                                    val pillShape = RoundedCornerShape(EchoRadius.pill)
                                    Row(
                                        modifier = Modifier
                                            .echoGlassSurface(
                                                isDarkMode = isDarkMode,
                                                shape = pillShape,
                                                fallbackBorder = activeTheme.primaryColor.copy(alpha = 0.35f),
                                                tintColor = activeTheme.primaryColor,
                                                elevation = 6.dp
                                            )
                                            .echoPressable(scaleOnPress = 0.95f, hapticOnClick = true) {
                                                showSpeedSheet = true
                                            }
                                            .padding(horizontal = 14.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Speed,
                                            contentDescription = "Playback Speed",
                                            tint = activeTheme.primaryColor,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${playbackSpeed}x",
                                            color = activeTheme.primaryColor,
                                            style = MaterialTheme.typography.labelMedium.tabularFigures().copy(
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        )
                                    }
                                }
                                nextTrack != null -> {
                                    val pillShape = RoundedCornerShape(EchoRadius.pill)
                                    Row(
                                        modifier = Modifier
                                            .weight(1f, fill = false)
                                            .padding(horizontal = 12.dp)
                                            .echoGlassSurface(
                                                isDarkMode = isDarkMode,
                                                shape = pillShape,
                                                fallbackBorder = borderColor,
                                                elevation = 6.dp
                                            )
                                            .echoPressable(scaleOnPress = 0.96f, hapticOnClick = true) {
                                                showQueueSheet = true
                                            }
                                            .padding(horizontal = 14.dp, vertical = 8.dp),
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
                                else -> Spacer(modifier = Modifier.width(1.dp))
                            }

                            Box(
                                modifier = Modifier
                                    .size(EchoSpacing.minTouchTarget)
                                    .clip(CircleShape)
                                    .echoPressable(scaleOnPress = 0.88f, hapticOnClick = true) {
                                        showQueueSheet = true
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                    contentDescription = "Queue",
                                    tint = contentSecondary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        // Subtle Scroll-Down for Lyrics Indicator
                        Row(
                            modifier = Modifier
                                .padding(top = 6.dp, bottom = 10.dp)
                                .clip(CircleShape)
                                .echoPressable(scaleOnPress = 0.96f, hapticOnClick = true) {
                                    scope.launch { scrollState.animateScrollTo(lyricsSectionTopPx) }
                                }
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.KeyboardArrowUp,
                                contentDescription = null,
                                tint = contentSecondary.copy(alpha = 0.72f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Scroll down for English lyrics",
                                color = contentSecondary.copy(alpha = 0.78f),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium)
                            )
                        }
                    }

                    // ==========================================
                    // SECTION 2: Scroll-Down Synced English Lyrics
                    // ==========================================
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp, bottom = 28.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Lyrics",
                                    color = contentPrimary,
                                    style = MaterialTheme.typography.headlineMedium
                                )
                                Text(
                                    text = if (lyricsResult.lines.isNotEmpty()) {
                                        "English • Synced with playback"
                                    } else {
                                        track.artistName
                                    },
                                    color = contentSecondary,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (lyricsResult.lines.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .size(EchoSpacing.minTouchTarget)
                                        .clip(CircleShape)
                                        .background(
                                            if (lyricsOffsetMs != 0L) activeTheme.primaryColor.copy(alpha = 0.16f)
                                            else cardBg.copy(alpha = 0.7f)
                                        )
                                        .border(0.6.dp, borderColor, CircleShape)
                                        .echoPressable(scaleOnPress = 0.90f, hapticOnClick = true) {
                                            showOffsetDialog = true
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Timer,
                                        contentDescription = "Adjust Sync Offset",
                                        tint = if (lyricsOffsetMs != 0L) activeTheme.primaryColor else contentPrimary,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }
                        }

                        val lyricsCardHeight = (playerPageHeight * 0.76f).coerceAtLeast(420.dp)
                        ModernLyricsView(
                            lyricsResult = lyricsResult,
                            currentPositionMs = state.currentPositionMs,
                            artworkColors = artworkColors,
                            track = track,
                            isPlaying = state.isPlaying,
                            playbackSpeed = playbackSpeed,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(lyricsCardHeight),
                            onSeekTo = { posMs -> musicController.seekTo(posMs) },
                            onFullscreenClick = { showFullscreenLyrics = true }
                        )
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
private fun TransportToggleIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    isActive: Boolean,
    activeColor: Color,
    inactiveColor: Color,
    onClick: () -> Unit
) {
    val tint by animateColorAsState(
        targetValue = if (isActive) activeColor else inactiveColor,
        animationSpec = tween(EchoMotion.DurationMicro, easing = EchoMotion.EaseOutStrong),
        label = "transport_toggle_tint"
    )
    val dotScale by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.72f,
        animationSpec = EchoMotion.SpringBouncy,
        label = "transport_dot_scale"
    )
    val dotAlpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        animationSpec = tween(EchoMotion.DurationMicro),
        label = "transport_dot_alpha"
    )

    Column(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .echoPressable(scaleOnPress = 0.88f, hapticOnClick = true, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .size(4.dp)
                .graphicsLayer {
                    scaleX = dotScale
                    scaleY = dotScale
                    alpha = dotAlpha
                }
                .clip(CircleShape)
                .background(activeColor)
        )
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
    val mins = totalSeconds / 60L
    val secs = totalSeconds % 60L
    return String.format(Locale.US, "%d:%02d", mins, secs)
}
