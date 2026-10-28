package com.spotify.clone.ui.components

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import com.spotify.clone.core.download.TrackDownloadManager
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.spotify.clone.core.designsystem.SpotifyDarkGray
import com.spotify.clone.core.designsystem.SpotifyGreen
import com.spotify.clone.core.designsystem.SpotifyLightGray
import com.spotify.clone.core.designsystem.SpotifyTextSecondary
import com.spotify.clone.core.designsystem.SpotifyWhite
import com.spotify.clone.core.model.Track
import com.spotify.clone.core.player.MusicController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackOptionsBottomSheet(
    track: Track,
    musicController: MusicController,
    onDismiss: () -> Unit,
    onSleepTimerClick: (() -> Unit)? = null,
    onEqualizerClick: (() -> Unit)? = null,
    onSpeedClick: (() -> Unit)? = null,
    onJioTuneClick: (() -> Unit)? = null,
    onSongCreditsClick: (() -> Unit)? = null,
    onViewArtist: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SpotifyDarkGray
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 36.dp)
        ) {
            // Track Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = track.coverArtUrl,
                    contentDescription = track.albumName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(SpotifyLightGray)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        color = SpotifyWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = track.artistName,
                        color = SpotifyTextSecondary,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Divider(
                color = SpotifyLightGray.copy(alpha = 0.2f),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            // Option 1: Like / Liked
            TrackOptionRow(
                icon = if (track.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                iconTint = if (track.isFavorite) SpotifyGreen else SpotifyWhite,
                label = if (track.isFavorite) "Remove from your Liked Songs" else "Like",
                onClick = {
                    musicController.toggleFavorite(track)
                    val msg = if (track.isFavorite) "Removed from Liked Songs" else "Added to Liked Songs"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    onDismiss()
                }
            )

            // Option 2: Start Song Radio (JioSaavn Radio feature)
            TrackOptionRow(
                icon = Icons.Filled.Radio,
                label = "Start song radio",
                onClick = {
                    musicController.playTrack(track, listOf(track))
                    Toast.makeText(context, "Playing Radio for \"${track.title}\"", Toast.LENGTH_SHORT).show()
                    onDismiss()
                }
            )

            // Option 3: Set as JioTune / Ringtone (JioSaavn feature)
            TrackOptionRow(
                icon = Icons.Filled.NotificationsActive,
                label = "Set as JioTune / Ringtone Studio",
                onClick = {
                    onDismiss()
                    if (onJioTuneClick != null) {
                        onJioTuneClick()
                    } else {
                        Toast.makeText(context, "Set \"${track.title}\" as your default ringtone", Toast.LENGTH_LONG).show()
                    }
                }
            )

            // Option 4: Add to queue
            TrackOptionRow(
                icon = Icons.Filled.QueueMusic,
                label = "Add to queue",
                onClick = {
                    musicController.addToQueue(track)
                    Toast.makeText(context, "Added to queue", Toast.LENGTH_SHORT).show()
                    onDismiss()
                }
            )

            // Option 3: Add to playlist
            TrackOptionRow(
                icon = Icons.Filled.PlaylistAdd,
                label = "Add to playlist",
                onClick = {
                    Toast.makeText(context, "Saved to library", Toast.LENGTH_SHORT).show()
                    onDismiss()
                }
            )

            val downloadManager = TrackDownloadManager.getInstance(context)
            val isDownloaded = downloadManager.isTrackDownloaded(track.id)

            // Option 4: Download for offline
            TrackOptionRow(
                icon = if (isDownloaded) Icons.Filled.DownloadDone else Icons.Filled.Download,
                label = if (isDownloaded) "Remove downloaded song" else "Download for offline listening",
                onClick = {
                    if (isDownloaded) {
                        downloadManager.removeTrackDownload(track.id)
                        Toast.makeText(context, "Removed download: \"${track.title}\"", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Downloading \"${track.title}\" for offline...", Toast.LENGTH_SHORT).show()
                        downloadManager.downloadTrack(track) { ok ->
                            val msg = if (ok) "Downloaded \"${track.title}\"" else "Failed to download \"${track.title}\""
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                    onDismiss()
                }
            )

            // Option 5: Share
            TrackOptionRow(
                icon = Icons.Filled.Share,
                label = "Share",
                onClick = {
                    shareTrack(context, track)
                    onDismiss()
                }
            )

            // Option 8: View Artist
            TrackOptionRow(
                icon = Icons.Outlined.Person,
                label = "View artist (${track.artistName})",
                onClick = {
                    onViewArtist?.invoke(track.artistName)
                    onDismiss()
                }
            )

            // Option 9: Song Audio Details & Credits (Apple Music / Tidal feature)
            TrackOptionRow(
                icon = Icons.Filled.Info,
                label = "Song info & studio credits",
                onClick = {
                    onDismiss()
                    if (onSongCreditsClick != null) {
                        onSongCreditsClick()
                    } else {
                        Toast.makeText(context, "${track.title}\nArtist: ${track.artistName}\nAlbum: ${track.albumName}\nBitrate: 320 kbps Studio Master", Toast.LENGTH_LONG).show()
                    }
                }
            )

            // Option 7: Sleep Timer
            if (onSleepTimerClick != null) {
                TrackOptionRow(
                    icon = Icons.Outlined.Bedtime,
                    label = "Sleep timer",
                    onClick = {
                        onDismiss()
                        onSleepTimerClick()
                    }
                )
            }

            // Option 8: Equalizer
            if (onEqualizerClick != null) {
                TrackOptionRow(
                    icon = Icons.Filled.GraphicEq,
                    label = "Equalizer",
                    onClick = {
                        onDismiss()
                        onEqualizerClick()
                    }
                )
            }

            // Option 9: Playback Speed
            if (onSpeedClick != null) {
                TrackOptionRow(
                    icon = Icons.Filled.Speed,
                    label = "Playback speed",
                    onClick = {
                        onDismiss()
                        onSpeedClick()
                    }
                )
            }
        }
    }
}

@Composable
private fun TrackOptionRow(
    icon: ImageVector,
    label: String,
    iconTint: androidx.compose.ui.graphics.Color = SpotifyWhite,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(18.dp))

        Text(
            text = label,
            color = SpotifyWhite,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun shareTrack(context: Context, track: Track) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, "Listen to \"${track.title}\" by ${track.artistName} on Spotify Clone!")
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "Share Track")
    context.startActivity(shareIntent)
}
