package com.spotify.clone.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.spotify.clone.core.designsystem.LivingEqualizer
import com.spotify.clone.core.designsystem.echoPressable
import com.spotify.clone.core.model.MusicGenre
import com.spotify.clone.core.model.PlaybackState
import com.spotify.clone.core.model.Track
import com.spotify.clone.core.player.MusicController

@Composable
fun QueueSheetContent(
    playbackState: PlaybackState,
    musicController: MusicController,
    onTrackClick: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    val textPrimary = MaterialTheme.colorScheme.onSurface
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary
    val cardBg = MaterialTheme.colorScheme.surfaceContainerHigh
    val activeGenreLock by musicController.activeGenreLock.collectAsState()
    val autoMatchGenre by musicController.autoMatchGenre.collectAsState()
    var showGenreSheet by remember { mutableStateOf(false) }

    val currentTrack = playbackState.currentTrack
    val queue = playbackState.queue
    val currentQueueIndex = playbackState.queueIndex
    val effectiveGenre = remember(activeGenreLock, autoMatchGenre, currentTrack?.id) {
        musicController.getEffectiveGenre(currentTrack)
    }
    val orderSummary = remember(activeGenreLock, autoMatchGenre, effectiveGenre, currentTrack?.id, playbackState.shuffleMode) {
        buildString {
            if (activeGenreLock != MusicGenre.ALL) {
                append("Locked: ${activeGenreLock.shortLabel}")
            } else if (autoMatchGenre && currentTrack != null) {
                append("Auto-Genre: ${effectiveGenre.shortLabel}")
            } else {
                append("Smart Radio")
            }
            append(if (playbackState.shuffleMode) " • Unplayed-First Shuffle" else " • Artist-Balanced")
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Playing Next",
                    color = textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Text(
                    text = "${queue.size} tracks • $orderSummary",
                    color = textSecondary,
                    fontSize = 12.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .echoPressable(scaleOnPress = 0.94f, hapticOnClick = true) { showGenreSheet = true }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Radio,
                    contentDescription = "Genre Radio",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = effectiveGenre.shortLabel,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Session modifier controls
        if (queue.size > currentQueueIndex + 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(cardBg)
                        .echoPressable(scaleOnPress = 0.94f, hapticOnClick = true) {
                            musicController.shuffleUpcomingQueue()
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shuffle,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Shuffle Up Next",
                        color = textPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(cardBg)
                        .echoPressable(scaleOnPress = 0.94f, hapticOnClick = true) {
                            musicController.clearUpcomingQueue()
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteSweep,
                        contentDescription = null,
                        tint = textSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Clear Up Next",
                        color = textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (currentTrack != null) {
                item(key = "now_playing_header", contentType = "now_playing_header") {
                    Text(
                        text = "NOW PLAYING",
                        color = primaryColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = currentTrack.coverArtUrl,
                            contentDescription = currentTrack.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentTrack.title,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = currentTrack.artistName,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
                                fontSize = 12.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        LivingEqualizer(
                            isPlaying = playbackState.isPlaying,
                            barCount = 3,
                            barColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxHeight = 15.dp
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            item(key = "up_next_header", contentType = "section_header") {
                Text(
                    text = "QUEUE",
                    color = textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                )
            }

            itemsIndexed(
                items = queue,
                key = { index, track -> "${track.id}_$index" },
                contentType = { _, _ -> "queue_track_row" }
            ) { index, track ->
                val isCurrent = if (currentQueueIndex in queue.indices) {
                    index == currentQueueIndex
                } else {
                    track.id == currentTrack?.id
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isCurrent) cardBg else Color.Transparent)
                        .echoPressable(scaleOnPress = 0.985f) {
                            musicController.playQueueIndex(index)
                        }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = track.coverArtUrl,
                        contentDescription = track.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.title,
                            color = if (isCurrent) primaryColor else textPrimary,
                            fontSize = 14.sp,
                            fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = track.artistName,
                            color = textSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (index > 0) {
                        IconButton(
                            onClick = { musicController.moveQueueItem(index, index - 1) },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.KeyboardArrowUp,
                                contentDescription = "Move Up",
                                tint = textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    if (index < queue.size - 1) {
                        IconButton(
                            onClick = { musicController.moveQueueItem(index, index + 1) },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.KeyboardArrowDown,
                                contentDescription = "Move Down",
                                tint = textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = { musicController.removeFromQueue(index) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Remove",
                            tint = textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            item(key = "queue_bottom_spacer", contentType = "spacer") { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    if (showGenreSheet) {
        GenrePickerBottomSheet(
            musicController = musicController,
            onDismiss = { showGenreSheet = false }
        )
    }
}
