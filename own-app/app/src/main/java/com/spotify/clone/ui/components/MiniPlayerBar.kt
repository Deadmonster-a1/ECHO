package com.spotify.clone.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.spotify.clone.core.designsystem.EchoHeartButton
import com.spotify.clone.core.designsystem.LocalAppTheme
import com.spotify.clone.core.designsystem.LocalReducedMotion
import com.spotify.clone.core.designsystem.ThemeManager
import com.spotify.clone.core.designsystem.echoPressable
import com.spotify.clone.core.model.PlaybackState
import com.spotify.clone.core.sync.DeviceSyncManager

/**
 * Signature ECHO Floating Mini-Player
 * Reflects real playback state with:
 * - Fluid card with soft elevation and fine border
 * - High-resolution rounded album artwork
 * - Tactile play/pause circle control and next track action
 * - Integrated soundwave progress bar
 * - Tap and vertical swipe-up expansion to full Now Playing
 */
@Composable
fun MiniPlayerBar(
    playbackState: PlaybackState,
    onTogglePlayPause: () -> Unit,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit,
    onSkipNext: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val track = playbackState.currentTrack ?: return
    val context = LocalContext.current
    val activeTheme = LocalAppTheme.current
    val isDarkMode by ThemeManager.isDarkMode.collectAsState()
    val isReducedMotion = LocalReducedMotion.current
    val syncManager = remember { DeviceSyncManager.getInstance(context) }
    val syncState by syncManager.syncState.collectAsState()
    var verticalDragAccumulator by remember { mutableFloatStateOf(0f) }

    val cardBg = if (isDarkMode) Color(0xFF16151E) else Color(0xFFFFFFFF)
    val cardBorder = if (isDarkMode) Color(0xFF282736) else Color(0xFFE5E0E8)
    val playFabBg = if (isDarkMode) Color(0xFFFFFFFF) else Color(0xFF17171C)
    val playFabIconTint = if (isDarkMode) Color(0xFF101016) else Color(0xFFFFFFFF)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .shadow(
                elevation = if (isDarkMode) 3.dp else 8.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = Color.Black.copy(alpha = 0.08f),
                spotColor = Color.Black.copy(alpha = 0.12f)
            )
            .clip(RoundedCornerShape(18.dp))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(18.dp))
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { verticalDragAccumulator = 0f },
                    onDragEnd = {
                        if (verticalDragAccumulator < -35f) {
                            onClick()
                        }
                        verticalDragAccumulator = 0f
                    },
                    onVerticalDrag = { _, dragAmount ->
                        verticalDragAccumulator += dragAmount
                    }
                )
            }
            .echoPressable(scaleOnPress = 0.98f, onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Artwork Thumbnail with rounded contour
            AsyncImage(
                model = track.coverArtUrl,
                contentDescription = track.albumName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (isDarkMode) Color(0xFF222030) else Color(0xFFECE7E1))
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Title & Artist
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                if (syncState.isMultiRoomAudioEnabled || syncState.activeSession != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Devices,
                            contentDescription = null,
                            tint = activeTheme.primaryColor,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (syncState.activeSession != null) "ECHO Jam • Active" else "${syncState.activeDeviceCount} Devices Sync",
                            color = activeTheme.primaryColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Text(
                        text = track.artistName,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Play / Pause Circle Action
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .echoPressable(scaleOnPress = 0.90f, onClick = onTogglePlayPause)
                    .clip(CircleShape)
                    .background(playFabBg),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = playbackState.isPlaying,
                    transitionSpec = {
                        if (isReducedMotion) {
                            fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                        } else {
                            fadeIn(tween(120)) togetherWith fadeOut(tween(80))
                        }
                    },
                    label = "mini_play_pause"
                ) { playing ->
                    Icon(
                        imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (playing) "Pause" else "Play",
                        tint = playFabIconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Next Track Button
            if (onSkipNext != null) {
                IconButton(
                    onClick = onSkipNext,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = "Skip Next",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // Integrated Sleek Progress Indicator
        LinearProgressIndicator(
            progress = { playbackState.progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(2.5.dp),
            color = activeTheme.primaryColor,
            trackColor = if (isDarkMode) Color(0xFF222030) else Color(0xFFEDE9E3)
        )
    }
}
