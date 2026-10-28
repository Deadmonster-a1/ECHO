package com.spotify.clone.ui.playlist

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.spotify.clone.core.data.AudioRepository
import com.spotify.clone.core.designsystem.EchoHeartButton
import com.spotify.clone.core.designsystem.EchoMotion
import com.spotify.clone.core.designsystem.EchoPlayFab
import com.spotify.clone.core.designsystem.LocalAppTheme
import com.spotify.clone.core.designsystem.LocalReducedMotion
import com.spotify.clone.core.designsystem.echoPressable
import com.spotify.clone.core.download.TrackDownloadManager
import com.spotify.clone.core.model.Playlist
import com.spotify.clone.core.model.Track
import com.spotify.clone.core.player.MusicController
import com.spotify.clone.ui.components.TrackOptionsBottomSheet
import com.spotify.clone.ui.components.TrackRowItem

/**
 * Signature Animation B: Cinematic Collapsing Header for Playlists
 * - Parallax artwork scaling and elevation settling driven by LazyListState
 * - Sticky compact header reveal on scroll
 * - Play/Shuffle controls with spring interaction
 * - Native responsive scrolling
 */
@Composable
fun PlaylistDetailScreen(
    playlistId: String,
    onBack: () -> Unit,
    repository: AudioRepository,
    musicController: MusicController,
    onArtistClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val activeTheme = LocalAppTheme.current
    val isReducedMotion = LocalReducedMotion.current
    val playbackState by musicController.playbackState.collectAsState()

    var playlist by remember { mutableStateOf<Playlist?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(playlistId) {
        isLoading = true
        playlist = repository.getPlaylistById(playlistId)
        isLoading = false
    }

    if (isLoading) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = activeTheme.primaryColor)
        }
        return
    }

    if (playlist == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Playlist unavailable",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))
                androidx.compose.material3.Button(onClick = onBack) {
                    Text("Go Back")
                }
            }
        }
        return
    }

    val currentPlaylist = playlist!!
    val context = LocalContext.current
    val downloadManager = remember { TrackDownloadManager.getInstance(context) }
    val downloadedPlaylistIds by downloadManager.downloadedPlaylistIds.collectAsState()
    val downloadedTrackIds by downloadManager.downloadedTrackIds.collectAsState()
    val isDownloaded = downloadedPlaylistIds.contains(currentPlaylist.id) ||
        (currentPlaylist.tracks.isNotEmpty() && currentPlaylist.tracks.all { downloadedTrackIds.contains(it.id) })
    var isDownloading by remember { mutableStateOf(false) }
    var isSaved by remember { mutableStateOf(false) }
    var showPlaylistOptions by remember { mutableStateOf(false) }
    var selectedTrackForOptions by remember { mutableStateOf<Track?>(null) }

    val listState = rememberLazyListState()

    // Scroll-linked transformations (derivedStateOf prevents recomposition overhead)
    val headerScrollOffset by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex == 0) {
                listState.firstVisibleItemScrollOffset.toFloat()
            } else 500f
        }
    }

    val artworkScale by remember {
        derivedStateOf {
            if (isReducedMotion) 1f
            else (1f - (headerScrollOffset / 800f)).coerceIn(0.68f, 1f)
        }
    }

    val artworkParallaxY by remember {
        derivedStateOf {
            if (isReducedMotion) 0f
            else headerScrollOffset * 0.4f
        }
    }

    val compactHeaderAlpha by remember {
        derivedStateOf {
            ((headerScrollOffset - 220f) / 100f).coerceIn(0f, 1f)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(bottom = 140.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. Cinematic Parallax Hero Artwork
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    activeTheme.primaryColor.copy(alpha = 0.45f),
                                    activeTheme.darkBackgroundColor,
                                    MaterialTheme.colorScheme.background
                                )
                            )
                        )
                        .padding(top = 56.dp, bottom = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = artworkScale
                                scaleY = artworkScale
                                translationY = artworkParallaxY
                            }
                            .size(200.dp)
                            .shadow(elevation = 24.dp, shape = RoundedCornerShape(16.dp), ambientColor = activeTheme.primaryColor)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (currentPlaylist.id == "pl_liked_songs") {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF4B2AEB), Color(0xFF7A64EC), Color(0xFFA294F9))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Favorite,
                                    contentDescription = "Liked Songs",
                                    tint = Color.White,
                                    modifier = Modifier.size(72.dp)
                                )
                            }
                        } else {
                            AsyncImage(
                                model = currentPlaylist.coverArtUrl,
                                contentDescription = currentPlaylist.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            // 2. Playlist Metadata & Actions
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Text(
                        text = currentPlaylist.name,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        letterSpacing = (-0.5).sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = currentPlaylist.description,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.5.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Curated by ${currentPlaylist.ownerName} • ${currentPlaylist.tracks.size} tracks",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Transport Actions: Heart, Download, Options, Shuffle, and Big Play FAB
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            isSaved = !isSaved
                            val msg = if (isSaved) "Saved to your library" else "Removed from library"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(
                                imageVector = if (isSaved) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Save",
                                tint = if (isSaved) activeTheme.primaryColor else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            enabled = !isDownloading,
                            onClick = {
                                if (isDownloaded) {
                                    downloadManager.removePlaylistDownload(currentPlaylist.id)
                                    Toast.makeText(context, "Removed downloads for this playlist", Toast.LENGTH_SHORT).show()
                                } else {
                                    isDownloading = true
                                    Toast.makeText(context, "Downloading ${currentPlaylist.tracks.size} tracks for offline playback...", Toast.LENGTH_SHORT).show()
                                    downloadManager.downloadPlaylist(
                                        playlist = currentPlaylist,
                                        onProgress = { done, total -> },
                                        onComplete = { count ->
                                            isDownloading = false
                                            Toast.makeText(context, "Downloaded $count/${currentPlaylist.tracks.size} tracks", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            }
                        ) {
                            if (isDownloading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.dp,
                                    color = activeTheme.primaryColor
                                )
                            } else {
                                Icon(
                                    imageVector = if (isDownloaded) Icons.Filled.DownloadDone else Icons.Filled.Download,
                                    contentDescription = "Download",
                                    tint = if (isDownloaded) activeTheme.primaryColor else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(onClick = { showPlaylistOptions = true }) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = "Options",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Shuffle Action
                        IconButton(
                            onClick = {
                                if (currentPlaylist.tracks.isNotEmpty()) {
                                    val shuffled = currentPlaylist.tracks.shuffled()
                                    musicController.playTrack(shuffled.first(), shuffled)
                                }
                            },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Shuffle,
                                contentDescription = "Shuffle",
                                tint = activeTheme.primaryColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Play FAB
                        val isThisPlaylistPlaying = playbackState.isPlaying && playbackState.queue.any { it.albumName == currentPlaylist.name }
                        EchoPlayFab(
                            isPlaying = isThisPlaylistPlaying,
                            onToggle = {
                                if (currentPlaylist.tracks.isNotEmpty()) {
                                    if (isThisPlaylistPlaying) {
                                        musicController.togglePlayPause()
                                    } else {
                                        musicController.playTrack(currentPlaylist.tracks.first(), currentPlaylist.tracks)
                                    }
                                }
                            },
                            size = 54.dp
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                }
            }

            // 3. Track List
            if (currentPlaylist.tracks.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp, vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (currentPlaylist.id == "pl_liked_songs") "No liked songs yet" else "This playlist is empty",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (currentPlaylist.id == "pl_liked_songs")
                                "Save songs to your Liked Songs by tapping the heart icon while listening."
                            else
                                "Find songs to add to this playlist using Search.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.5.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                itemsIndexed(currentPlaylist.tracks) { index, track ->
                    val isCurrentlyPlaying = playbackState.currentTrack?.id == track.id
                    TrackRowItem(
                        track = track,
                        isPlaying = isCurrentlyPlaying,
                        onClick = {
                            musicController.playTrack(track, currentPlaylist.tracks)
                        },
                        onMoreClick = {
                            selectedTrackForOptions = track
                        }
                    )
                }
            }
        }

        // Sticky Compact Collapsing Top Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(64.dp)
                .graphicsLayer { alpha = compactHeaderAlpha }
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                .padding(start = 60.dp, end = 20.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = currentPlaylist.name,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Top Back Button (Always accessible)
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .statusBarsPadding()
                .padding(top = 8.dp, start = 8.dp)
                .size(48.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
        }
    }

    if (showPlaylistOptions) {
        AlertDialog(
            onDismissRequest = { showPlaylistOptions = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(currentPlaylist.name, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• ${currentPlaylist.tracks.size} tracks available in Studio Fidelity", color = MaterialTheme.colorScheme.onSurface)
                    Text("• LRU chunk cache active", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("• Curated by ${currentPlaylist.ownerName}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    Toast.makeText(context, "Playlist link copied", Toast.LENGTH_SHORT).show()
                    showPlaylistOptions = false
                }) {
                    Text("Share", color = activeTheme.primaryColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPlaylistOptions = false }) {
                    Text("Close", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    selectedTrackForOptions?.let { track ->
        TrackOptionsBottomSheet(
            track = track,
            musicController = musicController,
            onDismiss = { selectedTrackForOptions = null },
            onViewArtist = { artistName ->
                onArtistClick?.invoke(artistName)
            }
        )
    }
}
