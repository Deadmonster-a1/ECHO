package com.spotify.clone.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.material.icons.filled.DownloadDone
import com.spotify.clone.core.designsystem.EchoHeartButton
import com.spotify.clone.core.designsystem.EchoMotion
import com.spotify.clone.core.designsystem.EchoPlayFab
import com.spotify.clone.core.designsystem.LocalAppTheme
import com.spotify.clone.core.download.TrackDownloadManager
import com.spotify.clone.core.designsystem.LocalReducedMotion
import androidx.compose.ui.platform.LocalContext
import com.spotify.clone.core.designsystem.echoPressable
import com.spotify.clone.core.model.RepeatMode
import com.spotify.clone.core.player.MusicController
import com.spotify.clone.core.sync.DeviceSyncManager
import com.spotify.clone.ui.components.EchoDeviceSyncSheet
import com.spotify.clone.ui.components.EqualizerBottomSheet
import com.spotify.clone.ui.components.JioTuneSheet
import com.spotify.clone.ui.components.PlaybackSpeedBottomSheet
import com.spotify.clone.ui.components.SleepTimerBottomSheet
import com.spotify.clone.ui.components.SongCreditsSheet
import com.spotify.clone.ui.components.TrackOptionsBottomSheet

/**
 * Signature Animation A: Album-to-Player Transformation & Now Playing Screen
 * - Large visual anchor artwork with organic sound atmosphere
 * - Living ECHO Sculpture in background
 * - Downward swipe gesture to minimize back into MiniPlayer
 * - Real queue management, speed controls, and verified stream telemetry
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
    val isReducedMotion = LocalReducedMotion.current
    val state by musicController.playbackState.collectAsState()
    val playbackSpeed by musicController.playbackSpeed.collectAsState()
    val track = state.currentTrack ?: return

    val syncManager = remember { DeviceSyncManager.getInstance(context) }
    val syncState by syncManager.syncState.collectAsState()
    val downloadManager = remember { TrackDownloadManager.getInstance(context) }
    val isDownloaded = downloadManager.isTrackDownloaded(track.id)

    var showQueueSheet by remember { mutableStateOf(false) }
    var showTrackOptionsSheet by remember { mutableStateOf(false) }
    var showDevicesSheet by remember { mutableStateOf(false) }
    var showSleepTimerSheet by remember { mutableStateOf(false) }
    var showEqualizerSheet by remember { mutableStateOf(false) }
    var showSpeedSheet by remember { mutableStateOf(false) }
    var showFullscreenLyrics by remember { mutableStateOf(false) }
    var showJioTuneSheet by remember { mutableStateOf(false) }
    var showSongCreditsSheet by remember { mutableStateOf(false) }

    var isUserScrubbing by remember { mutableStateOf(false) }
    var scrubPosition by remember { mutableFloatStateOf(0f) }

    var dragDownAccumulator by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        activeTheme.primaryColor.copy(alpha = 0.38f),
                        activeTheme.darkBackgroundColor.copy(alpha = 0.88f),
                        Color(0xFF0F0E17),
                        Color(0xFF08070D)
                    )
                )
            )
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { dragDownAccumulator = 0f },
                    onDragEnd = {
                        if (dragDownAccumulator > 60f) {
                            onCollapse()
                        }
                        dragDownAccumulator = 0f
                    },
                    onVerticalDrag = { _, dragAmount ->
                        dragDownAccumulator += dragAmount
                    }
                )
            },
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 580.dp)
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(36.dp))

            // 1. Top Bar: Down Chevron, Context Header, Options
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onCollapse,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Collapse to mini-player",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "PLAYING FROM PLAYLIST",
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.4.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = track.albumName.ifBlank { "Now Playing" },
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = { showTrackOptionsSheet = true },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "Track Options",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 2. High-Aesthetic Album Artwork with Ambient Depth Glow
            Box(
                modifier = Modifier
                    .widthIn(max = 380.dp)
                    .fillMaxWidth(0.92f)
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                // Ambient color glow behind artwork
                Box(
                    modifier = Modifier
                        .fillMaxSize(0.96f)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    activeTheme.primaryColor.copy(alpha = 0.30f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // High-resolution Artwork Anchor
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .shadow(
                            elevation = 24.dp,
                            shape = RoundedCornerShape(20.dp),
                            ambientColor = activeTheme.primaryColor.copy(alpha = 0.45f),
                            spotColor = Color.Black.copy(alpha = 0.70f)
                        )
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                        .background(Color(0xFF1B1A25)),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = track.coverArtUrl,
                        contentDescription = track.albumName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            // 3. Track Title, Artist with Explicit Badge & Tactile Favorite
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isDownloaded) {
                            Icon(
                                imageVector = Icons.Filled.DownloadDone,
                                contentDescription = "Downloaded",
                                tint = activeTheme.primaryColor,
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .size(16.dp)
                            )
                        }
                        if (track.isExplicit) {
                            Box(
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color.White.copy(alpha = 0.45f))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "E",
                                    color = Color.Black,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                        Text(
                            text = track.artistName,
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clickable {
                                onViewArtist?.invoke(track.artistName)
                            }
                        )
                    }
                }

                EchoHeartButton(
                    isFavorite = track.isFavorite,
                    onToggle = { musicController.toggleFavorite(track) },
                    iconSize = 28.dp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4. Scrubber & Timers
            val currentPos = if (isUserScrubbing) (scrubPosition * state.durationMs).toLong() else state.currentPositionMs
            val remainingMs = (state.durationMs - currentPos).coerceAtLeast(0L)

            Slider(
                value = if (isUserScrubbing) scrubPosition else state.progress,
                onValueChange = {
                    isUserScrubbing = true
                    scrubPosition = it
                },
                onValueChangeFinished = {
                    isUserScrubbing = false
                    musicController.seekTo((scrubPosition * state.durationMs).toLong())
                },
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.White.copy(alpha = 0.18f)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime(currentPos),
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
                Text(
                    text = "-${formatTime(remainingMs)}",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 5. Coordinated Transport Controls
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
                        tint = if (state.shuffleMode) activeTheme.primaryColor else Color.White.copy(alpha = 0.45f),
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Previous
                IconButton(
                    onClick = { musicController.skipPrevious() },
                    modifier = Modifier.size(50.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipPrevious,
                        contentDescription = "Previous",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Play / Pause FAB
                EchoPlayFab(
                    isPlaying = state.isPlaying,
                    onToggle = { musicController.togglePlayPause() },
                    size = 68.dp,
                    containerColor = Color.White,
                    contentColor = Color(0xFF101016)
                )

                // Next
                IconButton(
                    onClick = { musicController.skipNext() },
                    modifier = Modifier.size(50.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Repeat
                IconButton(onClick = { musicController.toggleRepeat() }) {
                    Icon(
                        imageVector = if (state.repeatMode == RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                        contentDescription = "Repeat",
                        tint = if (state.repeatMode != RepeatMode.OFF) activeTheme.primaryColor else Color.White.copy(alpha = 0.45f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            // 6. Bottom Utility Controls: Devices, Speed Pill & Queue
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Device sync button with multi-room status badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (syncState.isMultiRoomAudioEnabled || syncState.activeSession != null)
                                activeTheme.primaryColor.copy(alpha = 0.22f)
                            else Color.White.copy(alpha = 0.08f)
                        )
                        .clickable { showDevicesSheet = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Devices,
                        contentDescription = "Devices",
                        tint = if (syncState.isMultiRoomAudioEnabled || syncState.activeSession != null)
                            activeTheme.primaryColor
                        else Color.White.copy(alpha = 0.75f),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (syncState.activeSession != null) "Jam Active" 
                               else if (syncState.isMultiRoomAudioEnabled) "${syncState.activeDeviceCount} Devices"
                               else (syncState.currentDevice?.name ?: "This Phone"),
                        color = if (syncState.isMultiRoomAudioEnabled || syncState.activeSession != null)
                            activeTheme.primaryColor
                        else Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Speed Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (playbackSpeed != 1.0f) activeTheme.primaryColor.copy(alpha = 0.25f)
                            else Color.White.copy(alpha = 0.10f)
                        )
                        .clickable { showSpeedSheet = true }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Speed,
                            contentDescription = "Playback Speed",
                            tint = if (playbackSpeed != 1.0f) activeTheme.primaryColor else Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = if (playbackSpeed == 1.0f) "1x" else "${playbackSpeed}x",
                            color = if (playbackSpeed != 1.0f) activeTheme.primaryColor else Color.White,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Queue button
                IconButton(
                    onClick = { showQueueSheet = true },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.QueueMusic,
                        contentDescription = "Queue",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // 7. Synchronized Lyrics Preview Card
            LyricsPreviewCard(
                track = track,
                onClick = {
                    showFullscreenLyrics = true
                }
            )

            Spacer(modifier = Modifier.height(36.dp))
        }
    }

    // Modal Bottom Sheets
    if (showQueueSheet) {
        ModalBottomSheet(
            onDismissRequest = { showQueueSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            QueueSheetContent(
                playbackState = state,
                musicController = musicController,
                onTrackClick = { selected ->
                    musicController.playTrack(selected, state.queue)
                }
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
            onJioTuneClick = { showJioTuneSheet = true },
            onSongCreditsClick = { showSongCreditsSheet = true },
            onViewArtist = { artistName ->
                showTrackOptionsSheet = false
                onViewArtist?.invoke(artistName)
            }
        )
    }

    if (showDevicesSheet) {
        EchoDeviceSyncSheet(
            onDismiss = { showDevicesSheet = false }
        )
    }

    if (showJioTuneSheet) {
        JioTuneSheet(
            track = track,
            onDismiss = { showJioTuneSheet = false }
        )
    }

    if (showSongCreditsSheet) {
        SongCreditsSheet(
            track = track,
            onDismiss = { showSongCreditsSheet = false }
        )
    }

    if (showSleepTimerSheet) {
        SleepTimerBottomSheet(
            musicController = musicController,
            onDismiss = { showSleepTimerSheet = false }
        )
    }

    if (showEqualizerSheet) {
        EqualizerBottomSheet(
            onDismiss = { showEqualizerSheet = false }
        )
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
            onClose = { showFullscreenLyrics = false }
        )
    }

}

@Composable
private fun LyricsPreviewCard(
    track: com.spotify.clone.core.model.Track,
    onClick: () -> Unit
) {
    val activeTheme = LocalAppTheme.current
    val sampleLyrics = com.spotify.clone.core.data.SampleCatalog.sampleTracks.find {
        it.title.contains(track.title, ignoreCase = true) || track.title.contains(it.title, ignoreCase = true)
    }?.lyrics ?: emptyList()
    val effectiveLyrics = if (track.lyrics.isNotEmpty()) track.lyrics else sampleLyrics
    val hasLyrics = effectiveLyrics.isNotEmpty()
    val previewLines = if (hasLyrics) effectiveLyrics.take(3).map { it.words } else listOf("Lyrics not available", "Tap to open acoustic sound visualizer")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.GraphicEq,
                    contentDescription = null,
                    tint = activeTheme.primaryColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Lyrics",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "EXPAND",
                    color = Color.White.copy(alpha = 0.90f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        previewLines.forEach { line ->
            Text(
                text = line,
                color = if (hasLyrics) Color.White.copy(alpha = 0.92f) else Color.White.copy(alpha = 0.55f),
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                lineHeight = 25.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
        }
    }
}
private fun formatTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
