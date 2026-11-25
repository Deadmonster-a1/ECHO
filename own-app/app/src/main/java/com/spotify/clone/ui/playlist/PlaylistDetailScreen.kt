package com.spotify.clone.ui.playlist

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.spotify.clone.core.data.AudioRepository
import com.spotify.clone.core.designsystem.EchoInsetCard
import com.spotify.clone.core.designsystem.EchoMotion
import com.spotify.clone.core.designsystem.LocalAppTheme
import com.spotify.clone.core.designsystem.LocalReducedMotion
import com.spotify.clone.core.designsystem.ThemeManager
import com.spotify.clone.core.designsystem.echoContrastOn
import com.spotify.clone.core.designsystem.echoPressable
import com.spotify.clone.core.designsystem.echoSpecularBorder
import com.spotify.clone.core.designsystem.rememberAnimatedArtworkColors
import com.spotify.clone.core.designsystem.resolvedBackground
import com.spotify.clone.core.designsystem.resolvedBorder
import com.spotify.clone.core.designsystem.resolvedCard
import com.spotify.clone.core.designsystem.tabularFigures
import com.spotify.clone.core.download.TrackDownloadManager
import com.spotify.clone.core.model.Playlist
import com.spotify.clone.core.model.Track
import com.spotify.clone.core.player.MusicController
import com.spotify.clone.ui.components.TrackOptionsBottomSheet
import com.spotify.clone.ui.components.TrackRowItem

/**
 * Apple Music-inspired Playlist / Album Detail Screen with parallax hero artwork,
 * smooth sticky top bar reveal, and tactile pill controls.
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
    val context = LocalContext.current
    val activeTheme = LocalAppTheme.current
    val isDarkMode by ThemeManager.isDarkMode.collectAsState()
    val isReducedMotion = LocalReducedMotion.current
    val playbackState by musicController.playbackState.collectAsState()

    val screenBg = activeTheme.resolvedBackground(isDarkMode)
    val cardBg = activeTheme.resolvedCard(isDarkMode)
    val borderColor = activeTheme.resolvedBorder(isDarkMode)
    val textPrimary = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant

    var playlist by remember { mutableStateOf<Playlist?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isDownloading by remember { mutableStateOf(false) }
    var selectedTrackForOptions by remember { mutableStateOf<Track?>(null) }
    var filterQuery by remember { mutableStateOf("") }

    val downloadManager = remember { TrackDownloadManager.getInstance(context) }
    val downloadedPlaylistIds by downloadManager.downloadedPlaylistIds.collectAsState()
    val downloadedTrackIds by downloadManager.downloadedTrackIds.collectAsState()

    LaunchedEffect(playlistId) {
        isLoading = true
        playlist = repository.getPlaylistById(playlistId)
        isLoading = false
    }

    if (isLoading) {
        Box(
            modifier = modifier.fillMaxSize().background(screenBg),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = activeTheme.primaryColor)
        }
        return
    }

    val currentPlaylist = playlist
    if (currentPlaylist == null) {
        Box(
            modifier = modifier.fillMaxSize().background(screenBg),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Playlist unavailable", color = textPrimary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = activeTheme.primaryColor,
                        contentColor = echoContrastOn(activeTheme.primaryColor)
                    )
                ) {
                    Text("Go Back")
                }
            }
        }
        return
    }

    val isDownloaded = downloadedPlaylistIds.contains(currentPlaylist.id) ||
        (currentPlaylist.tracks.isNotEmpty() && currentPlaylist.tracks.all { downloadedTrackIds.contains(it.id) })
    val totalMinutes = remember(currentPlaylist.tracks) {
        (currentPlaylist.tracks.sumOf { it.durationMs } / 60_000L).coerceAtLeast(if (currentPlaylist.tracks.isEmpty()) 0L else 1L)
    }
    val artworkSeed = remember(currentPlaylist.id, currentPlaylist.coverArtUrl) {
        Track(
            id = currentPlaylist.id,
            title = currentPlaylist.name,
            artistName = currentPlaylist.ownerName,
            albumName = currentPlaylist.name,
            coverArtUrl = currentPlaylist.coverArtUrl,
            durationMs = 0L,
            mediaUri = ""
        )
    }
    val artworkPalette = rememberAnimatedArtworkColors(artworkSeed)

    val listState = rememberLazyListState()
    val showStickyTitle by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 380 }
    }
    val heroScrollProgress by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 1f
            else (listState.firstVisibleItemScrollOffset / 520f).coerceIn(0f, 1f)
        }
    }

    val topBarBg by animateColorAsState(
        targetValue = if (showStickyTitle) screenBg else Color.Transparent,
        animationSpec = tween(EchoMotion.DurationMicro, easing = EchoMotion.EncoreButtonEase),
        label = "pl_topbar_bg"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(screenBg)
    ) {
        // Dynamic album-extracted atmospheric top backdrop
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(380.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            artworkPalette.vibrantAccent.copy(alpha = if (isDarkMode) 0.30f else 0.14f),
                            Color.Transparent
                        )
                    )
                )
        )

        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(bottom = 176.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. Centered Hero Artwork + Metadata + Apple-style Play / Shuffle Pill Buttons
            item {
                val artShape = RoundedCornerShape(18.dp)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(top = 52.dp, start = 20.dp, end = 20.dp, bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(216.dp)
                            .graphicsLayer {
                                if (!isReducedMotion) {
                                    val s = 1f - heroScrollProgress * 0.12f
                                    scaleX = s
                                    scaleY = s
                                    alpha = 1f - heroScrollProgress * 0.6f
                                    translationY = heroScrollProgress * 32f
                                }
                            }
                            .shadow(18.dp, artShape, ambientColor = Color.Black.copy(alpha = 0.22f))
                            .clip(artShape)
                            .background(cardBg)
                            .echoSpecularBorder(isDarkMode, artShape, borderColor),
                        contentAlignment = Alignment.Center
                    ) {
                        if (currentPlaylist.id == "pl_liked_songs") {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Brush.linearGradient(listOf(activeTheme.primaryColor, activeTheme.gradientEnd))),
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

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = currentPlaylist.name,
                        color = textPrimary,
                        style = MaterialTheme.typography.headlineLarge,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${currentPlaylist.ownerName} • ${currentPlaylist.tracks.size} songs" +
                            if (totalMinutes > 0L) " • $totalMinutes min" else "",
                        color = activeTheme.primaryColor,
                        style = MaterialTheme.typography.bodyMedium.tabularFigures().copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )

                    if (currentPlaylist.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = currentPlaylist.description,
                            color = textSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val hasTracks = currentPlaylist.tracks.isNotEmpty()
                        val actionShape = RoundedCornerShape(14.dp)

                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(actionShape)
                                .background(
                                    if (hasTracks) activeTheme.primaryColor
                                    else activeTheme.primaryColor.copy(alpha = 0.4f)
                                )
                                .echoPressable(
                                    scaleOnPress = 0.96f,
                                    enabled = hasTracks,
                                    hapticOnClick = true
                                ) {
                                    musicController.playTrack(currentPlaylist.tracks.first(), currentPlaylist.tracks)
                                },
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = echoContrastOn(activeTheme.primaryColor),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Play",
                                color = echoContrastOn(activeTheme.primaryColor),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                        }

                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(actionShape)
                                .background(cardBg)
                                .echoSpecularBorder(isDarkMode, actionShape, borderColor)
                                .echoPressable(
                                    scaleOnPress = 0.96f,
                                    enabled = hasTracks,
                                    hapticOnClick = true
                                ) {
                                    val shuffled = currentPlaylist.tracks.shuffled()
                                    musicController.playTrack(shuffled.first(), shuffled)
                                },
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Shuffle,
                                contentDescription = null,
                                tint = activeTheme.primaryColor,
                                modifier = Modifier.size(19.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Shuffle",
                                color = activeTheme.primaryColor,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(actionShape)
                                .background(cardBg)
                                .echoSpecularBorder(isDarkMode, actionShape, borderColor)
                                .echoPressable(
                                    scaleOnPress = 0.92f,
                                    enabled = !isDownloading && hasTracks,
                                    hapticOnClick = true
                                ) {
                                    if (isDownloaded) {
                                        downloadManager.removePlaylistDownload(currentPlaylist.id)
                                        Toast.makeText(context, "Removed playlist downloads", Toast.LENGTH_SHORT).show()
                                    } else {
                                        isDownloading = true
                                        Toast.makeText(context, "Downloading ${currentPlaylist.tracks.size} tracks...", Toast.LENGTH_SHORT).show()
                                        downloadManager.downloadPlaylist(
                                            playlist = currentPlaylist,
                                            onProgress = { _, _ -> },
                                            onComplete = { count ->
                                                isDownloading = false
                                                Toast.makeText(context, "Downloaded $count tracks", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isDownloading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = activeTheme.primaryColor
                                )
                            } else {
                                Icon(
                                    imageVector = if (isDownloaded) Icons.Filled.DownloadDone else Icons.Filled.Download,
                                    contentDescription = "Download",
                                    tint = if (isDownloaded) activeTheme.primaryColor else textPrimary
                                )
                            }
                        }
                    }
                }
            }

            if (currentPlaylist.tracks.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp, vertical = 36.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (currentPlaylist.id == "pl_liked_songs") "No liked songs yet" else "This playlist is empty",
                            color = textPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (currentPlaylist.id == "pl_liked_songs")
                                "Tap the heart button on any song to save it here."
                            else
                                "Use Search or any song's menu to add tracks.",
                            color = textSecondary,
                            fontSize = 13.5.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // Find-in-Playlist filter bar (Spotify FindInContextView pattern)
                if (currentPlaylist.tracks.size > 4) {
                    item(key = "playlist_find_bar") {
                        val barShape = RoundedCornerShape(14.dp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .height(42.dp)
                                .clip(barShape)
                                .background(cardBg)
                                .echoSpecularBorder(isDarkMode, barShape, borderColor)
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = null,
                                tint = textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            BasicTextField(
                                value = filterQuery,
                                onValueChange = { filterQuery = it },
                                singleLine = true,
                                textStyle = TextStyle(color = textPrimary, fontSize = 14.sp),
                                cursorBrush = SolidColor(activeTheme.primaryColor),
                                modifier = Modifier.weight(1f),
                                decorationBox = { inner ->
                                    if (filterQuery.isEmpty()) {
                                        Text(
                                            text = "Find in ${currentPlaylist.name}",
                                            color = textSecondary,
                                            fontSize = 13.5.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    inner()
                                }
                            )
                            if (filterQuery.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .echoPressable(scaleOnPress = 0.85f) { filterQuery = "" },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Clear filter",
                                        tint = textSecondary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                item(key = "playlist_tracks_card") {
                    val displayedTracks = remember(currentPlaylist.tracks, filterQuery) {
                        val q = filterQuery.trim().lowercase()
                        if (q.isEmpty()) currentPlaylist.tracks
                        else currentPlaylist.tracks.filter {
                            it.title.lowercase().contains(q) ||
                                it.artistName.lowercase().contains(q) ||
                                it.albumName.lowercase().contains(q)
                        }
                    }
                    EchoInsetCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        if (displayedTracks.isEmpty()) {
                            Text(
                                text = "No tracks matching \"$filterQuery\"",
                                color = textSecondary,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(20.dp)
                            )
                        } else {
                            displayedTracks.forEachIndexed { index, track ->
                                TrackRowItem(
                                    track = track,
                                    isPlaying = playbackState.currentTrack?.id == track.id && playbackState.isPlaying,
                                    onClick = { musicController.playTrack(track, currentPlaylist.tracks) },
                                    onMoreClick = { selectedTrackForOptions = track },
                                    onSwipeQueue = {
                                        musicController.playNext(track)
                                        Toast.makeText(context, "Playing next: ${track.title}", Toast.LENGTH_SHORT).show()
                                    },
                                    onSwipeFavorite = {
                                        musicController.toggleFavorite(track)
                                        Toast.makeText(
                                            context,
                                            if (track.isFavorite) "Removed from Liked Songs" else "Added to Liked Songs",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                )
                                if (index < displayedTracks.lastIndex) {
                                    HorizontalDivider(
                                        color = borderColor,
                                        thickness = 0.5.dp,
                                        modifier = Modifier.padding(start = 74.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Translucent Sticky Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(topBarBg)
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(cardBg.copy(alpha = 0.88f))
                    .echoSpecularBorder(isDarkMode, CircleShape, borderColor)
                    .echoPressable(scaleOnPress = 0.88f, hapticOnClick = true, onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            AnimatedVisibility(
                visible = showStickyTitle,
                enter = fadeIn(tween(180, easing = EchoMotion.EaseOutStrong)) +
                    slideInVertically(tween(180, easing = EchoMotion.EaseOutStrong)) { 8 },
                exit = fadeOut(tween(140)) + slideOutVertically(tween(140)) { 8 }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = currentPlaylist.name,
                        color = textPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }

    selectedTrackForOptions?.let { track ->
        TrackOptionsBottomSheet(
            track = track,
            musicController = musicController,
            onDismiss = { selectedTrackForOptions = null },
            onViewArtist = { artist -> onArtistClick?.invoke(artist) }
        )
    }
}
