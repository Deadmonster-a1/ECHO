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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Lyrics
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.spotify.clone.core.designsystem.EchoHeartButton
import com.spotify.clone.core.designsystem.EchoMarqueeText
import com.spotify.clone.core.designsystem.EchoMotion
import com.spotify.clone.core.designsystem.LiveAudioSpectrum
import com.spotify.clone.core.designsystem.LivingEqualizer
import com.spotify.clone.core.designsystem.LocalAppTheme
import com.spotify.clone.core.designsystem.LocalReducedMotion
import com.spotify.clone.core.designsystem.SpotifyJellyfishAura
import com.spotify.clone.core.designsystem.ThemeManager
import com.spotify.clone.core.designsystem.WavyAudioScrubber
import com.spotify.clone.core.designsystem.echoContrastOn
import com.spotify.clone.core.designsystem.echoPressable
import com.spotify.clone.core.designsystem.echoSpecularBorder
import com.spotify.clone.core.designsystem.rememberAnimatedArtworkColors
import com.spotify.clone.core.designsystem.resolvedBackground
import com.spotify.clone.core.designsystem.resolvedBorder
import com.spotify.clone.core.designsystem.resolvedCard
import com.spotify.clone.core.designsystem.tabularFigures
import com.spotify.clone.core.download.TrackDownloadManager
import com.spotify.clone.core.lyrics.LyricsRepository
import com.spotify.clone.core.model.MusicGenre
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
import kotlin.math.abs

/**
 * Apple Music-inspired Now Playing Screen with:
 * - 1:1 finger-tracked vertical drag-to-dismiss with momentum projection & spring handoff
 * - 1:1 horizontal artwork swipe to skip next/previous
 * - Living artwork-derived ambient aura glow
 * - Morphing segmented control between Artwork and Live Synced Lyrics
 * - Expand-on-touch fluid Apple Music scrubber + tabular timestamps
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
    val activeGenreLock by musicController.activeGenreLock.collectAsState()
    val autoMatchGenre by musicController.autoMatchGenre.collectAsState()
    val track = state.currentTrack ?: return

    val downloadManager = remember { TrackDownloadManager.getInstance(context) }
    val isDownloaded = downloadManager.isTrackDownloaded(track.id)

    var displayMode by remember { mutableIntStateOf(0) } // 0 = Artwork, 1 = Lyrics
    var showQueueSheet by remember { mutableStateOf(false) }
    var showTrackOptionsSheet by remember { mutableStateOf(false) }
    var showSleepTimerSheet by remember { mutableStateOf(false) }
    var showEqualizerSheet by remember { mutableStateOf(false) }
    var showSpeedSheet by remember { mutableStateOf(false) }
    var showFullscreenLyrics by remember { mutableStateOf(false) }
    var showSongCreditsSheet by remember { mutableStateOf(false) }
    var showGenreSheet by remember { mutableStateOf(false) }

    var isScrubbing by remember { mutableStateOf(false) }
    var scrubProgress by remember { mutableFloatStateOf(0f) }

    // 1:1 Vertical drag-to-dismiss state
    val sheetOffsetY = remember { Animatable(0f) }
    val dismissThresholdPx = with(density) { 140.dp.toPx() }
    val maxSheetRadiusPx = with(density) { 180.dp.toPx() }

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
        targetValue = if (state.isPlaying) 1.0f else 0.89f,
        animationSpec = if (isReducedMotion) tween(160) else EchoMotion.SpringGentle,
        label = "apple_artwork_scale"
    )

    // Subtle living ambient aura when playing
    val infiniteTransition = rememberInfiniteTransition(label = "np_ambient_aura")
    val auraBreathScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (state.isPlaying && !isReducedMotion) 1.09f else 1.0f,
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
                artworkColors.backgroundMid.copy(alpha = 0.42f),
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
                .pointerInput(Unit) {
                    val velocityTracker = VelocityTracker()
                    detectVerticalDragGestures(
                        onDragStart = {
                            velocityTracker.resetTracking()
                        },
                        onDragCancel = {
                            scope.launch {
                                sheetOffsetY.animateTo(0f, EchoMotion.SpringMomentum)
                            }
                        },
                        onDragEnd = {
                            val velY = velocityTracker.calculateVelocity().y
                            val projectedY = sheetOffsetY.value + EchoMotion.projectMomentum(velY, 0.994f)
                            if (projectedY > dismissThresholdPx || sheetOffsetY.value > dismissThresholdPx) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onCollapse()
                            } else {
                                scope.launch {
                                    sheetOffsetY.animateTo(
                                        targetValue = 0f,
                                        animationSpec = EchoMotion.SpringMomentum,
                                        initialVelocity = velY
                                    )
                                }
                            }
                        },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            velocityTracker.addPosition(change.uptimeMillis, change.position)
                            scope.launch {
                                val next = sheetOffsetY.value + dragAmount
                                val applied = if (next < 0f) {
                                    -EchoMotion.rubberBand(-next, 120f, 0.4f)
                                } else {
                                    next
                                }
                                sheetOffsetY.snapTo(applied)
                            }
                        }
                    )
                },
            contentAlignment = Alignment.TopCenter
        ) {
            // Living Spotify Jellyfish atmospheric aura behind artwork
            SpotifyJellyfishAura(
                artworkColors = artworkColors,
                isPlaying = state.isPlaying,
                isDarkMode = isDarkMode,
                intensity = if (isDarkMode) 1.25f else 0.85f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(460.dp)
                    .graphicsLayer {
                        scaleX = auraBreathScale
                        scaleY = auraBreathScale
                    }
            )

            Column(
                modifier = Modifier
                    .widthIn(max = 540.dp)
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Apple-style Grab Handle
                Box(
                    modifier = Modifier
                        .size(width = 40.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(contentSecondary.copy(alpha = 0.40f))
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 1. Top Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
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

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (state.isPlaying) {
                                LivingEqualizer(
                                    isPlaying = true,
                                    barCount = 3,
                                    barColor = artworkColors.vibrantAccent,
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
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        val effectiveGenre = musicController.getEffectiveGenre(track)
                        val badgeText = if (activeGenreLock != MusicGenre.ALL) {
                            "Radio • ${activeGenreLock.shortLabel}"
                        } else if (autoMatchGenre) {
                            "Auto • ${effectiveGenre.shortLabel}"
                        } else {
                            "All Genres"
                        }
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(activeTheme.primaryColor.copy(alpha = 0.14f))
                                .echoPressable(scaleOnPress = 0.94f, hapticOnClick = true) { showGenreSheet = true }
                                .padding(horizontal = 9.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Radio,
                                contentDescription = "Same-Genre Radio",
                                tint = activeTheme.primaryColor,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = badgeText,
                                color = activeTheme.primaryColor,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .echoPressable(scaleOnPress = 0.88f, hapticOnClick = true) { showTrackOptionsSheet = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreHoriz,
                            contentDescription = "Options",
                            tint = contentPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2. Morphing Artwork / Lyrics Segmented Switch
                val switchShape = RoundedCornerShape(50)
                BoxWithConstraints(
                    modifier = Modifier
                        .width(204.dp)
                        .height(36.dp)
                        .clip(switchShape)
                        .background(cardBg.copy(alpha = if (isDarkMode) 0.78f else 0.92f))
                        .echoSpecularBorder(isDarkMode = isDarkMode, shape = switchShape, fallbackBorder = borderColor)
                        .padding(3.dp)
                ) {
                    val segmentWidth = maxWidth / 2
                    val pillOffset by animateDpAsState(
                        targetValue = segmentWidth * displayMode,
                        animationSpec = if (isReducedMotion) tween(120) else EchoMotion.springMomentum(),
                        label = "np_segment_pill"
                    )

                    Box(
                        modifier = Modifier
                            .offset(x = pillOffset)
                            .width(segmentWidth)
                            .fillMaxHeight()
                            .clip(switchShape)
                            .background(activeTheme.primaryColor)
                    )

                    Row(modifier = Modifier.fillMaxSize()) {
                        listOf("Artwork", "Lyrics").forEachIndexed { idx, label ->
                            val selected = displayMode == idx
                            val labelColor by animateColorAsState(
                                targetValue = if (selected) echoContrastOn(activeTheme.primaryColor) else contentSecondary,
                                animationSpec = tween(160),
                                label = "np_segment_fg_$idx"
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(switchShape)
                                    .echoPressable(scaleOnPress = 0.95f, hapticOnClick = true) {
                                        displayMode = idx
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = labelColor,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // 3. Center Visual: Hero Artwork or Inline Synced Lyrics
                AnimatedContent(
                    targetState = displayMode,
                    transitionSpec = {
                        if (isReducedMotion) {
                            fadeIn(tween(150)) togetherWith fadeOut(tween(120))
                        } else {
                            (fadeIn(tween(240, easing = EchoMotion.EaseOutStrong)) +
                                scaleIn(initialScale = 0.95f, animationSpec = tween(260, easing = EchoMotion.EaseOutStrong))) togetherWith
                                (fadeOut(tween(160)) + scaleOut(targetScale = 0.96f, animationSpec = tween(160)))
                        }
                    },
                    label = "np_mode_switch"
                ) { mode ->
                    if (mode == 1) {
                        var lyricsResult by remember(track.id) {
                            mutableStateOf(LyricsRepository.getCachedLyrics(track))
                        }
                        LaunchedEffect(track.id) {
                            if (lyricsResult.lines.isEmpty()) {
                                lyricsResult = LyricsRepository.getOrFetchLyrics(track)
                            }
                        }
                        ModernLyricsView(
                            lyricsResult = lyricsResult,
                            currentPositionMs = state.currentPositionMs,
                            artworkColors = artworkColors,
                            track = track,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(340.dp),
                            onSeekTo = { posMs -> musicController.seekTo(posMs) },
                            onFullscreenClick = { showFullscreenLyrics = true }
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
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
                                                }
                                                projected > artSwipeThresholdPx || artOffsetX.value > artSwipeThresholdPx -> {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    musicController.skipPrevious()
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
                                        .widthIn(max = 360.dp)
                                        .fillMaxWidth(0.92f)
                                        .aspectRatio(1f)
                                        .graphicsLayer {
                                            scaleX = artworkScale
                                            scaleY = artworkScale
                                            translationX = artOffsetX.value
                                            rotationZ = if (isReducedMotion) 0f else (artOffsetX.value / 240f) * 3.5f
                                        }
                                        .shadow(
                                            elevation = if (state.isPlaying) 28.dp else 10.dp,
                                            shape = RoundedCornerShape(22.dp),
                                            ambientColor = artworkColors.dominant.copy(alpha = 0.35f),
                                            spotColor = artworkColors.dominant.copy(alpha = 0.55f)
                                        )
                                        .clip(RoundedCornerShape(22.dp))
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
                }

                Spacer(modifier = Modifier.height(28.dp))

                // 4. Title, Artist & Favorite Heart
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
                                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                                    enabled = state.isPlaying,
                                    modifier = Modifier.echoPressable(scaleOnPress = 0.97f) {
                                        onViewArtist?.invoke(current.artistName)
                                    }
                                )
                            }
                        }
                    }

                    EchoHeartButton(
                        isFavorite = track.isFavorite,
                        onToggle = { musicController.toggleFavorite(track) },
                        iconSize = 26.dp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 5. Living Harmonic Waveform Scrubber
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
                    onScrubMove = { p ->
                        scrubProgress = p
                    },
                    onScrubEnd = { p ->
                        if (state.durationMs > 0L) {
                            musicController.seekTo((p * state.durationMs).toLong())
                        }
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        isScrubbing = false
                    }
                )

                Spacer(modifier = Modifier.height(6.dp))

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

                Spacer(modifier = Modifier.height(18.dp))

                // 6. Primary Transport Row (tactile press physics + active indicators)
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
                            .size(54.dp)
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
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(72.dp)
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
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(54.dp)
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
                            modifier = Modifier.size(38.dp)
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

                Spacer(modifier = Modifier.height(22.dp))

                // 7. Utility Controls Dock with Specular Border
                val dockShape = RoundedCornerShape(18.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(dockShape)
                        .background(cardBg.copy(alpha = if (isDarkMode) 0.78f else 0.92f))
                        .echoSpecularBorder(isDarkMode = isDarkMode, shape = dockShape, fallbackBorder = borderColor)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .echoPressable(scaleOnPress = 0.88f, hapticOnClick = true) { showFullscreenLyrics = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lyrics,
                            contentDescription = "Fullscreen Lyrics",
                            tint = contentPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .echoPressable(scaleOnPress = 0.92f, hapticOnClick = true) { showSpeedSheet = true }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Speed,
                            contentDescription = "Playback Speed",
                            tint = if (playbackSpeed != 1.0f) activeTheme.primaryColor else contentPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (playbackSpeed == 1.0f) "1x" else "${playbackSpeed}x",
                            color = if (playbackSpeed != 1.0f) activeTheme.primaryColor else contentPrimary,
                            style = MaterialTheme.typography.labelLarge.tabularFigures().copy(
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .echoPressable(scaleOnPress = 0.88f, hapticOnClick = true) { showEqualizerSheet = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Equalizer,
                            contentDescription = "Equalizer",
                            tint = contentPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .echoPressable(scaleOnPress = 0.88f, hapticOnClick = true) { showSleepTimerSheet = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Bedtime,
                            contentDescription = "Sleep Timer",
                            tint = if (sleepTimerRemaining != null) activeTheme.primaryColor else contentPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .echoPressable(scaleOnPress = 0.88f, hapticOnClick = true) { showQueueSheet = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = "Queue",
                            tint = contentPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Compact Up Next + Automix Crossfade status pill
                val nextTrack = state.queue.getOrNull(state.queueIndex + 1)
                val crossfadeSeconds by SettingsManager.crossfadeSeconds.collectAsState()
                if (nextTrack != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    val nextPillShape = RoundedCornerShape(14.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(nextPillShape)
                            .background(cardBg.copy(alpha = if (isDarkMode) 0.56f else 0.78f))
                            .echoSpecularBorder(isDarkMode = isDarkMode, shape = nextPillShape, fallbackBorder = borderColor)
                            .echoPressable(scaleOnPress = 0.985f, hapticOnClick = true) {
                                showQueueSheet = true
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "UP NEXT",
                            color = activeTheme.primaryColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.7.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${nextTrack.title} • ${nextTrack.artistName}",
                            color = contentPrimary.copy(alpha = 0.9f),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (crossfadeSeconds > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(activeTheme.primaryColor.copy(alpha = 0.16f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Mix ${crossfadeSeconds}s",
                                    color = activeTheme.primaryColor,
                                    style = MaterialTheme.typography.labelSmall.tabularFigures().copy(
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 8. Spotify Scrollsita Story & Telemetry Cards (Live Lyrics, Song DNA, Artist, Queue Peek)
                NowPlayingScrollCards(
                    track = track,
                    playbackState = state,
                    artworkColors = artworkColors,
                    crossfadeSeconds = crossfadeSeconds,
                    playbackSpeed = playbackSpeed,
                    isDarkMode = isDarkMode,
                    cardBg = cardBg,
                    borderColor = borderColor,
                    onOpenFullscreenLyrics = { showFullscreenLyrics = true },
                    onOpenCredits = { showSongCreditsSheet = true },
                    onOpenQueue = { showQueueSheet = true },
                    onViewArtist = onViewArtist,
                    onStartArtistRadio = { musicController.playSameGenreRadio(track) },
                    onPlayTrackFromQueue = { selected -> musicController.playTrack(selected, state.queue) },
                    onSeekTo = { posMs -> musicController.seekTo(posMs) }
                )

                Spacer(modifier = Modifier.height(28.dp))
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
