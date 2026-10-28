package com.spotify.clone.ui.library

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import com.spotify.clone.core.download.TrackDownloadManager
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.List
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.spotify.clone.core.designsystem.EchoFilterChip
import com.spotify.clone.core.designsystem.EchoMotion
import com.spotify.clone.core.designsystem.EchoPlayFab
import com.spotify.clone.core.designsystem.LocalAppTheme
import com.spotify.clone.core.designsystem.LocalReducedMotion
import com.spotify.clone.core.designsystem.ThemeManager
import com.spotify.clone.core.designsystem.echoPressable
import com.spotify.clone.core.model.Track
import com.spotify.clone.core.model.Playlist
import com.spotify.clone.core.player.MusicController

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onPlaylistClick: (String) -> Unit,
    onProfileClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeTheme = LocalAppTheme.current
    val isReducedMotion = LocalReducedMotion.current
    val isDarkMode by ThemeManager.isDarkMode.collectAsState()
    val state by viewModel.uiState.collectAsState()
    val musicController = remember { MusicController.getInstance(context) }
    val playbackState by musicController.playbackState.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }
    var librarySearchQuery by remember { mutableStateOf("") }

    val downloadManager = remember { TrackDownloadManager.getInstance(context) }
    val downloadedTrackIds by downloadManager.downloadedTrackIds.collectAsState()
    val downloadedPlaylistIds by downloadManager.downloadedPlaylistIds.collectAsState()

    val isLikedTab = state.selectedFilter == "Liked"
    val isDownloadedTab = state.selectedFilter == "Downloaded"
    val downloadedTracks = remember(downloadedTrackIds) { downloadManager.getAllDownloadedTracks() }

    val allPlaylists = remember(state.playlists, downloadedPlaylistIds) {
        val extra = downloadManager.getAllDownloadedPlaylists().filter { dp -> state.playlists.none { it.id == dp.id } }
        state.playlists + extra
    }

    val filteredPlaylists = allPlaylists.filter {
        it.id != "pl_liked_songs" &&
        (librarySearchQuery.isBlank() || it.name.contains(librarySearchQuery, ignoreCase = true)) &&
        when (state.selectedFilter) {
            "Downloaded" -> downloadedPlaylistIds.contains(it.id) || it.tracks.any { t -> downloadedTrackIds.contains(t.id) }
            else -> true
        }
    }

    val filteredLikedTracks = state.likedTracks.filter {
        librarySearchQuery.isBlank() ||
        it.title.contains(librarySearchQuery, ignoreCase = true) ||
        it.artistName.contains(librarySearchQuery, ignoreCase = true) ||
        it.albumName.contains(librarySearchQuery, ignoreCase = true)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(activeTheme.primaryColor.copy(alpha = 0.2f))
                    .border(1.5.dp, activeTheme.primaryColor, CircleShape)
                    .echoPressable(scaleOnPress = 0.90f) { onProfileClick() },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "E", color = activeTheme.primaryColor, fontWeight = FontWeight.Black, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.width(14.dp))

            Text(
                text = "Your Library",
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                letterSpacing = (-0.5).sp,
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = {
                isSearchActive = !isSearchActive
                if (!isSearchActive) librarySearchQuery = ""
            }) {
                Icon(
                    imageVector = if (isSearchActive) Icons.Filled.Close else Icons.Filled.Search,
                    contentDescription = "Search Library",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            IconButton(onClick = { showCreateDialog = true }) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Create Playlist",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // Inline Search Field
        if (isSearchActive) {
            OutlinedTextField(
                value = librarySearchQuery,
                onValueChange = { librarySearchQuery = it },
                placeholder = { Text("Search your sound collection...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        // Filter Pills: All | Playlists | Liked | Downloaded
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("All", "Playlists", "Liked", "Downloaded").forEach { filter ->
                EchoFilterChip(
                    text = filter,
                    isSelected = state.selectedFilter == filter,
                    onClick = {
                        val next = if (state.selectedFilter == filter && filter != "All") "All" else filter
                        viewModel.dispatch(LibraryEvent.SelectFilter(next))
                    }
                )
            }
        }

        // If Liked tab is selected, show dedicated Liked Songs list view with Play/Shuffle
        if (isLikedTab) {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 120.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    // Liked Songs Header Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(activeTheme.primaryColor.copy(alpha = 0.85f), activeTheme.gradientEnd)
                                )
                            )
                            .padding(18.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.25f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Favorite,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Liked Songs",
                                            color = Color.White,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Text(
                                            text = "${state.likedTracks.size} favorite songs saved",
                                            color = Color.White.copy(alpha = 0.85f),
                                            fontSize = 12.5.sp
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    // Shuffle Button
                                    IconButton(
                                        onClick = {
                                            if (state.likedTracks.isNotEmpty()) {
                                                val shuffled = state.likedTracks.shuffled()
                                                musicController.playTrack(shuffled.first(), shuffled)
                                            }
                                        },
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.2f))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Shuffle,
                                            contentDescription = "Shuffle Liked",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    // Play FAB
                                    val isLikedPlaying = playbackState.isPlaying && playbackState.queue.any { q -> state.likedTracks.any { it.id == q.id } }
                                    EchoPlayFab(
                                        isPlaying = isLikedPlaying,
                                        onToggle = {
                                            if (state.likedTracks.isNotEmpty()) {
                                                if (isLikedPlaying) musicController.togglePlayPause()
                                                else musicController.playTrack(state.likedTracks.first(), state.likedTracks)
                                            }
                                        },
                                        size = 42.dp,
                                        containerColor = Color.White,
                                        contentColor = Color(0xFF15141D)
                                    )
                                }
                            }
                        }
                    }
                }

                if (filteredLikedTracks.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp, vertical = 48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(activeTheme.primaryColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Favorite,
                                    contentDescription = null,
                                    tint = activeTheme.primaryColor,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(18.dp))
                            Text(
                                text = "Songs you like will appear here",
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Save songs you love by tapping the heart icon while playing or browsing.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.5.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                lineHeight = 19.sp
                            )
                        }
                    }
                } else {
                    items(filteredLikedTracks) { track ->
                        val isPlaying = playbackState.currentTrack?.id == track.id && playbackState.isPlaying
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .echoPressable(scaleOnPress = 0.98f) {
                                    musicController.playTrack(track, state.likedTracks)
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = track.coverArtUrl,
                                contentDescription = track.title,
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
                                    color = if (isPlaying) activeTheme.primaryColor else MaterialTheme.colorScheme.onBackground,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isPlaying) {
                                        Icon(
                                            imageVector = Icons.Filled.GraphicEq,
                                            contentDescription = null,
                                            tint = activeTheme.primaryColor,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = "${track.artistName} • ${track.albumName}",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            IconButton(onClick = { musicController.toggleFavorite(track) }) {
                                Icon(
                                    imageVector = Icons.Filled.Favorite,
                                    contentDescription = "Unlike",
                                    tint = activeTheme.primaryColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        } else if (isDownloadedTab) {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 120.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (downloadedTracks.isEmpty() && filteredPlaylists.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp, vertical = 64.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(activeTheme.primaryColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Download,
                                    contentDescription = null,
                                    tint = activeTheme.primaryColor,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(18.dp))
                            Text(
                                text = "No downloaded music yet",
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Download songs or playlists by tapping the download icon to listen offline anywhere without internet.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.5.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                lineHeight = 19.sp
                            )
                        }
                    }
                } else {
                    if (downloadedTracks.isNotEmpty()) {
                        item {
                            // Downloaded Songs Header Card
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(activeTheme.primaryColor.copy(alpha = 0.85f), activeTheme.gradientEnd)
                                        )
                                    )
                                    .padding(18.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(46.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White.copy(alpha = 0.25f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.DownloadDone,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(26.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = "Downloaded Songs",
                                                    color = Color.White,
                                                    fontSize = 20.sp,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                                Text(
                                                    text = "${downloadedTracks.size} songs ready for offline listening",
                                                    color = Color.White.copy(alpha = 0.85f),
                                                    fontSize = 12.5.sp
                                                )
                                            }
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            // Shuffle Button
                                            IconButton(
                                                onClick = {
                                                    if (downloadedTracks.isNotEmpty()) {
                                                        val shuffled = downloadedTracks.shuffled()
                                                        musicController.playTrack(shuffled.first(), shuffled)
                                                    }
                                                },
                                                modifier = Modifier
                                                    .size(42.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White.copy(alpha = 0.2f))
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Shuffle,
                                                    contentDescription = "Shuffle Downloaded",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }

                                            // Play FAB
                                            val isDownloadedPlaying = playbackState.isPlaying && playbackState.queue.any { q -> downloadedTracks.any { it.id == q.id } }
                                            EchoPlayFab(
                                                isPlaying = isDownloadedPlaying,
                                                onToggle = {
                                                    if (downloadedTracks.isNotEmpty()) {
                                                        if (isDownloadedPlaying) musicController.togglePlayPause()
                                                        else musicController.playTrack(downloadedTracks.first(), downloadedTracks)
                                                    }
                                                },
                                                size = 42.dp,
                                                containerColor = Color.White,
                                                contentColor = Color(0xFF15141D)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        items(downloadedTracks) { track ->
                            val isPlaying = playbackState.currentTrack?.id == track.id && playbackState.isPlaying
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .echoPressable(scaleOnPress = 0.98f) {
                                        musicController.playTrack(track, downloadedTracks)
                                    }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = track.coverArtUrl,
                                    contentDescription = track.title,
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
                                        color = if (isPlaying) activeTheme.primaryColor else MaterialTheme.colorScheme.onBackground,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Filled.DownloadDone,
                                            contentDescription = "Downloaded",
                                            tint = activeTheme.primaryColor,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${track.artistName} • ${track.albumName}",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                IconButton(onClick = {
                                    downloadManager.removeTrackDownload(track.id)
                                    Toast.makeText(context, "Removed from downloads", Toast.LENGTH_SHORT).show()
                                }) {
                                    Icon(
                                        imageVector = Icons.Filled.DownloadDone,
                                        contentDescription = "Remove download",
                                        tint = activeTheme.primaryColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (filteredPlaylists.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Downloaded Playlists",
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }

                        items(filteredPlaylists) { playlist ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .echoPressable(scaleOnPress = 0.98f) { onPlaylistClick(playlist.id) }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = playlist.coverArtUrl,
                                    contentDescription = playlist.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                )

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = playlist.name,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Filled.DownloadDone,
                                            contentDescription = "Downloaded",
                                            tint = activeTheme.primaryColor,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Playlist • ${playlist.ownerName}",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 12.5.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Sort & View Mode Switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Most Recent",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = { viewModel.dispatch(LibraryEvent.ToggleViewMode) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (state.isGridView) Icons.Outlined.List else Icons.Outlined.GridView,
                        contentDescription = "Switch view mode",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            // Animated View Mode Switcher: Grid or List
            AnimatedContent(
                targetState = state.isGridView,
                transitionSpec = {
                    if (isReducedMotion) {
                        fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                    } else {
                        fadeIn(tween(EchoMotion.DurationStandard)) togetherWith fadeOut(tween(EchoMotion.DurationMicro))
                    }
                },
                label = "library_view_mode",
                modifier = Modifier.fillMaxSize()
            ) { isGrid ->
                if (isGrid) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 150.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize().padding(bottom = 120.dp)
                    ) {
                        // Liked Songs Card (Shown exclusively in "All" view as a pinned collection)
                        if (state.selectedFilter == "All") {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                                        .echoPressable(scaleOnPress = 0.96f) {
                                            viewModel.dispatch(LibraryEvent.SelectFilter("Liked"))
                                        }
                                        .padding(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(130.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(activeTheme.primaryColor, activeTheme.gradientEnd)
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Favorite,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(42.dp)
                                        )

                                        // Quick Play FAB in bottom-right corner of card
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(8.dp),
                                            contentAlignment = Alignment.BottomEnd
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    if (state.likedTracks.isNotEmpty()) {
                                                        musicController.playTrack(state.likedTracks.first(), state.likedTracks)
                                                    } else {
                                                        Toast.makeText(context, "No liked songs yet", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.PlayArrow,
                                                    contentDescription = "Play Liked",
                                                    tint = Color(0xFF15141D),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "Liked Songs",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.5.sp,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "Pinned • ${state.likedTracks.size} songs",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        if (filteredPlaylists.isEmpty()) {
                            item(span = { GridItemSpan(2) }) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 32.dp, vertical = 48.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(68.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Add,
                                            contentDescription = null,
                                            tint = activeTheme.primaryColor,
                                            modifier = Modifier.size(34.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(18.dp))
                                    Text(
                                        text = "Create your first playlist",
                                        color = MaterialTheme.colorScheme.onBackground,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "It's easy, we'll help you. Tap below to start your collection.",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 13.5.sp,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(20.dp))
                                    androidx.compose.material3.Button(
                                        onClick = { showCreateDialog = true },
                                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                            containerColor = if (isDarkMode) Color.White else activeTheme.primaryColor,
                                            contentColor = if (isDarkMode) Color(0xFF121212) else Color.White
                                        ),
                                        shape = RoundedCornerShape(24.dp)
                                    ) {
                                        Text(text = "Create playlist", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            items(filteredPlaylists) { playlist ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                                        .echoPressable(scaleOnPress = 0.96f) { onPlaylistClick(playlist.id) }
                                        .padding(12.dp)
                                ) {
                                    AsyncImage(
                                        model = playlist.coverArtUrl,
                                        contentDescription = playlist.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(130.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surface)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = playlist.name,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = playlist.ownerName,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 120.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Liked Songs Row (Shown exclusively in "All" view as a pinned collection)
                        if (state.selectedFilter == "All") {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .echoPressable(scaleOnPress = 0.98f) {
                                            viewModel.dispatch(LibraryEvent.SelectFilter("Liked"))
                                        }
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(62.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(activeTheme.primaryColor, activeTheme.gradientEnd)
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Favorite,
                                            contentDescription = "Liked Songs",
                                            tint = Color.White,
                                            modifier = Modifier.size(30.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Liked Songs",
                                            color = MaterialTheme.colorScheme.onBackground,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Pinned • ${state.likedTracks.size} favorite songs",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 13.sp
                                        )
                                    }

                                    // Quick Play Icon
                                    IconButton(
                                        onClick = {
                                            if (state.likedTracks.isNotEmpty()) {
                                                musicController.playTrack(state.likedTracks.first(), state.likedTracks)
                                            } else {
                                                Toast.makeText(context, "No liked songs yet", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(activeTheme.primaryColor.copy(alpha = 0.15f))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.PlayArrow,
                                            contentDescription = "Play Liked",
                                            tint = activeTheme.primaryColor,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (filteredPlaylists.isEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 32.dp, vertical = 48.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(68.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Add,
                                            contentDescription = null,
                                            tint = activeTheme.primaryColor,
                                            modifier = Modifier.size(34.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(18.dp))
                                    Text(
                                        text = "Create your first playlist",
                                        color = MaterialTheme.colorScheme.onBackground,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "It's easy, we'll help you. Tap below to start your collection.",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 13.5.sp,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(20.dp))
                                    androidx.compose.material3.Button(
                                        onClick = { showCreateDialog = true },
                                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                            containerColor = if (isDarkMode) Color.White else activeTheme.primaryColor,
                                            contentColor = if (isDarkMode) Color(0xFF121212) else Color.White
                                        ),
                                        shape = RoundedCornerShape(24.dp)
                                    ) {
                                        Text(text = "Create playlist", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            // User Playlists
                            items(filteredPlaylists) { playlist ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .echoPressable(scaleOnPress = 0.98f) { onPlaylistClick(playlist.id) }
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = playlist.coverArtUrl,
                                        contentDescription = playlist.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(60.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    )

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = playlist.name,
                                            color = MaterialTheme.colorScheme.onBackground,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 15.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Playlist • ${playlist.ownerName}",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name ->
                viewModel.dispatch(LibraryEvent.CreatePlaylist(name))
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val activeTheme = LocalAppTheme.current
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = "Create Sound Playlist",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Soundtrack Name", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { if (text.isNotBlank()) onConfirm(text) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = activeTheme.primaryColor,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Create", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}
