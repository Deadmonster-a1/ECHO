package com.spotify.clone.ui.components

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.spotify.clone.core.data.AudioRepository
import com.spotify.clone.core.designsystem.LocalAppTheme
import com.spotify.clone.core.designsystem.ThemeManager
import com.spotify.clone.core.designsystem.echoContrastOn
import com.spotify.clone.core.designsystem.resolvedBorder
import com.spotify.clone.core.download.TrackDownloadManager
import com.spotify.clone.core.model.Playlist
import com.spotify.clone.core.model.Track
import com.spotify.clone.core.player.MusicController
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackOptionsBottomSheet(
    track: Track,
    musicController: MusicController,
    onDismiss: () -> Unit,
    onSleepTimerClick: (() -> Unit)? = null,
    onEqualizerClick: (() -> Unit)? = null,
    onSpeedClick: (() -> Unit)? = null,
    onGenreRadioClick: (() -> Unit)? = null,
    onSongCreditsClick: (() -> Unit)? = null,
    onViewArtist: ((String) -> Unit)? = null,
    onRemoveFromPlaylist: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val repository = remember { AudioRepository(context.applicationContext) }
    val activeTheme = LocalAppTheme.current
    val isDarkMode by ThemeManager.isDarkMode.collectAsState()
    val playbackSpeed by musicController.playbackSpeed.collectAsState()
    val sleepTimerRemaining by musicController.sleepTimerRemainingSeconds.collectAsState()

    val sheetBg = MaterialTheme.colorScheme.surface
    val textPrimary = MaterialTheme.colorScheme.onSurface
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val dividerColor = activeTheme.resolvedBorder(isDarkMode)

    var showPlaylistDialog by remember { mutableStateOf(false) }
    var userPlaylists by remember { mutableStateOf<List<Playlist>>(emptyList()) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistNameInput by remember { mutableStateOf("") }
    var showCreditsSheet by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = com.spotify.clone.core.designsystem.EchoRadius.xl, topEnd = com.spotify.clone.core.designsystem.EchoRadius.xl),
        containerColor = sheetBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 28.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = track.coverArtUrl,
                    contentDescription = track.albumName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        color = textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.artistName,
                        color = textSecondary,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            HorizontalDivider(
                color = dividerColor,
                thickness = 0.75.dp,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
            )

            // Section 1: Library & Queue
            OptionSectionLabel("QUEUE & LIBRARY", textSecondary)

            TrackOptionRow(
                icon = if (track.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                iconTint = if (track.isFavorite) activeTheme.primaryColor else textPrimary,
                label = if (track.isFavorite) "Remove from Liked Songs" else "Add to Liked Songs",
                onClick = {
                    musicController.toggleFavorite(track)
                    Toast.makeText(
                        context,
                        if (track.isFavorite) "Removed from Liked Songs" else "Added to Liked Songs",
                        Toast.LENGTH_SHORT
                    ).show()
                    onDismiss()
                }
            )

            TrackOptionRow(
                icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                label = "Add to Playlist",
                onClick = {
                    scope.launch {
                        userPlaylists = repository.getUserPlaylists().first()
                        showPlaylistDialog = true
                    }
                }
            )

            TrackOptionRow(
                icon = Icons.AutoMirrored.Filled.QueueMusic,
                label = "Play Next",
                iconTint = activeTheme.primaryColor,
                onClick = {
                    musicController.playNext(track)
                    Toast.makeText(context, "Playing next: \"${track.title}\"", Toast.LENGTH_SHORT).show()
                    onDismiss()
                }
            )

            TrackOptionRow(
                icon = Icons.AutoMirrored.Filled.QueueMusic,
                label = "Add to Queue",
                onClick = {
                    musicController.addToQueue(track)
                    Toast.makeText(context, "Added to Queue", Toast.LENGTH_SHORT).show()
                    onDismiss()
                }
            )

            val downloadManager = TrackDownloadManager.getInstance(context)
            val isDownloaded = downloadManager.isTrackDownloaded(track.id)
            TrackOptionRow(
                icon = if (isDownloaded) Icons.Filled.DownloadDone else Icons.Filled.Download,
                iconTint = if (isDownloaded) activeTheme.primaryColor else textPrimary,
                label = if (isDownloaded) "Remove Download" else "Download Offline",
                onClick = {
                    if (isDownloaded) {
                        downloadManager.removeTrackDownload(track.id)
                        Toast.makeText(context, "Removed download", Toast.LENGTH_SHORT).show()
                    } else {
                        val appCtx = context.applicationContext
                        Toast.makeText(context, "Downloading \"${track.title}\"...", Toast.LENGTH_SHORT).show()
                        downloadManager.downloadTrack(track) { ok ->
                            Toast.makeText(
                                appCtx,
                                if (ok) "Downloaded \"${track.title}\"" else "Download failed",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    onDismiss()
                }
            )

            if (onRemoveFromPlaylist != null) {
                TrackOptionRow(
                    icon = Icons.Filled.DeleteOutline,
                    iconTint = Color(0xFFE53935),
                    label = "Remove from this Playlist",
                    onClick = {
                        onDismiss()
                        onRemoveFromPlaylist()
                    }
                )
            }

            // Section 2: Audio & Playback
            HorizontalDivider(
                color = dividerColor,
                thickness = 0.75.dp,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
            )
            OptionSectionLabel("AUDIO & PLAYBACK", textSecondary)

            TrackOptionRow(
                icon = Icons.Filled.Radio,
                label = if (onGenreRadioClick != null) "Radio & Genre Station" else "Start Same-Genre Radio",
                onClick = {
                    onDismiss()
                    if (onGenreRadioClick != null) {
                        onGenreRadioClick()
                    } else {
                        musicController.playSameGenreRadio(track)
                        Toast.makeText(context, "Starting Same-Genre Radio", Toast.LENGTH_SHORT).show()
                    }
                }
            )

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

            if (onSpeedClick != null) {
                TrackOptionRow(
                    icon = Icons.Filled.Speed,
                    label = "Playback Speed",
                    trailingText = if (playbackSpeed != 1.0f) "${playbackSpeed}x" else null,
                    trailingTint = activeTheme.primaryColor,
                    onClick = {
                        onDismiss()
                        onSpeedClick()
                    }
                )
            }

            if (onSleepTimerClick != null) {
                val timerBadge = sleepTimerRemaining?.let { secs ->
                    String.format(Locale.US, "%d:%02d", secs / 60L, secs % 60L)
                }
                TrackOptionRow(
                    icon = Icons.Outlined.Bedtime,
                    label = "Sleep Timer",
                    trailingText = timerBadge,
                    trailingTint = activeTheme.primaryColor,
                    onClick = {
                        onDismiss()
                        onSleepTimerClick()
                    }
                )
            }

            // Section 3: Info & Share
            HorizontalDivider(
                color = dividerColor,
                thickness = 0.75.dp,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
            )
            OptionSectionLabel("TRACK INFO", textSecondary)

            if (onViewArtist != null) {
                TrackOptionRow(
                    icon = Icons.Outlined.Person,
                    label = "View Artist",
                    onClick = {
                        onDismiss()
                        onViewArtist(track.artistName)
                    }
                )
            }

            TrackOptionRow(
                icon = Icons.Filled.Info,
                label = "Song Credits",
                onClick = {
                    if (onSongCreditsClick != null) {
                        onDismiss()
                        onSongCreditsClick()
                    } else {
                        showCreditsSheet = true
                    }
                }
            )

            TrackOptionRow(
                icon = Icons.Filled.Share,
                label = "Share",
                onClick = {
                    shareTrack(context, track)
                    onDismiss()
                }
            )
        }
    }

    if (showPlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showPlaylistDialog = false },
            containerColor = sheetBg,
            title = { Text("Add to Playlist", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                showPlaylistDialog = false
                                showCreatePlaylistDialog = true
                            }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, tint = activeTheme.primaryColor)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("New Playlist", fontWeight = FontWeight.SemiBold, color = activeTheme.primaryColor)
                    }
                    HorizontalDivider(color = dividerColor, modifier = Modifier.padding(vertical = 6.dp))
                    if (userPlaylists.isEmpty()) {
                        Text(
                            text = "No custom playlists yet.",
                            color = textSecondary,
                            fontSize = 13.5.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 260.dp)
                        ) {
                            items(userPlaylists) { pl ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            scope.launch {
                                                repository.addTrackToPlaylist(track, pl.id)
                                                Toast.makeText(context, "Added to ${pl.name}", Toast.LENGTH_SHORT).show()
                                                showPlaylistDialog = false
                                                onDismiss()
                                            }
                                        }
                                        .padding(vertical = 10.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = pl.name,
                                        color = textPrimary,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPlaylistDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showCreatePlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            containerColor = sheetBg,
            title = { Text("New Playlist", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newPlaylistNameInput,
                    onValueChange = { newPlaylistNameInput = it },
                    placeholder = { Text("Playlist name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = newPlaylistNameInput.trim().ifBlank { "My Playlist" }
                        scope.launch {
                            val newPl = repository.createPlaylist(name)
                            repository.addTrackToPlaylist(track, newPl.id)
                            Toast.makeText(context, "Added to \"$name\"", Toast.LENGTH_SHORT).show()
                            newPlaylistNameInput = ""
                            showCreatePlaylistDialog = false
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = activeTheme.primaryColor,
                        contentColor = echoContrastOn(activeTheme.primaryColor)
                    )
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylistDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showCreditsSheet) {
        SongCreditsSheet(track = track, onDismiss = { showCreditsSheet = false })
    }
}

@Composable
private fun OptionSectionLabel(title: String, color: Color) {
    Text(
        text = title,
        color = color,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
    )
}

@Composable
private fun TrackOptionRow(
    icon: ImageVector,
    label: String,
    iconTint: Color? = null,
    trailingText: String? = null,
    trailingTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: () -> Unit
) {
    val textPrimary = MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconTint ?: textPrimary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            color = textPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        if (trailingText != null) {
            Text(
                text = trailingText,
                color = trailingTint,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun shareTrack(context: Context, track: Track) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, "Listen to \"${track.title}\" by ${track.artistName} on echo")
        type = "text/plain"
    }
    context.startActivity(Intent.createChooser(sendIntent, "Share Track"))
}
