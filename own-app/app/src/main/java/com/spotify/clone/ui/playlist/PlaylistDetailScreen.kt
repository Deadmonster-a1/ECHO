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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.spotify.clone.core.data.AudioRepository
import com.spotify.clone.core.data.WorldOnboardingCatalog
import com.spotify.clone.core.designsystem.EchoArtworkImage
import com.spotify.clone.core.designsystem.EchoIconCircleButton
import com.spotify.clone.core.designsystem.EchoInsetCard
import com.spotify.clone.core.designsystem.EchoMotion
import com.spotify.clone.core.designsystem.EchoRadius
import com.spotify.clone.core.designsystem.EchoSpacing
import com.spotify.clone.core.designsystem.LocalReducedMotion
import com.spotify.clone.core.designsystem.ThemeManager
import com.spotify.clone.core.designsystem.echoPressable
import com.spotify.clone.core.designsystem.rememberAnimatedArtworkColors
import com.spotify.clone.core.designsystem.tabularFigures
import com.spotify.clone.core.download.TrackDownloadManager
import com.spotify.clone.core.model.Playlist
import com.spotify.clone.core.model.Track
import com.spotify.clone.core.player.MusicController
import com.spotify.clone.ui.components.TrackOptionsBottomSheet
import com.spotify.clone.ui.components.TrackRowItem
import kotlinx.coroutines.launch

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
    val isDarkMode by ThemeManager.isDarkMode.collectAsState()
    val isReducedMotion = LocalReducedMotion.current
    val playbackState by musicController.playbackState.collectAsState()

    val screenBg = MaterialTheme.colorScheme.background
    val cardBg = MaterialTheme.colorScheme.surfaceContainerLow
    val borderColor = MaterialTheme.colorScheme.outlineVariant
    val primaryColor = MaterialTheme.colorScheme.primary
    val textPrimary = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant

    var playlist by remember { mutableStateOf<Playlist?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isDownloading by remember { mutableStateOf(false) }
    var selectedTrackForOptions by remember { mutableStateOf<Track?>(null) }
    var filterQuery by remember { mutableStateOf("") }
    var showAddSongsSheet by remember { mutableStateOf(false) }
    var showEditPlaylistDialog by remember { mutableStateOf(false) }
    var editNameDraft by remember { mutableStateOf("") }
    var editDescDraft by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    val favoriteTracks by repository.getFavoriteTracks().collectAsState(initial = emptyList())
    val favIds = remember(favoriteTracks) { favoriteTracks.mapTo(HashSet()) { it.id } }

    val downloadManager = remember { TrackDownloadManager.getInstance(context) }
    val downloadedPlaylistIds by downloadManager.downloadedPlaylistIds.collectAsState()
    val downloadedTrackIds by downloadManager.downloadedTrackIds.collectAsState()

    LaunchedEffect(playlistId) {
        isLoading = true
        playlist = repository.getPlaylistById(playlistId)
        isLoading = false
    }

    LaunchedEffect(playlistId, favIds) {
        if (playlistId == "pl_liked_songs" && !isLoading) {
            playlist = repository.getPlaylistById(playlistId)
        }
    }

    if (isLoading) {
        Box(
            modifier = modifier.fillMaxSize().background(screenBg),
            contentAlignment = Alignment.Center
        ) {
            com.spotify.clone.core.designsystem.ExpressiveLoadingIndicator(size = 52.dp)
        }
        return
    }

    val rawPlaylist = playlist
    if (rawPlaylist == null) {
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
                        containerColor = primaryColor,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Go Back")
                }
            }
        }
        return
    }
    val currentPlaylist = remember(rawPlaylist, favIds) {
        rawPlaylist.copy(
            tracks = rawPlaylist.tracks.map { t ->
                val fav = t.id in favIds
                if (t.isFavorite == fav) t else t.copy(isFavorite = fav)
            }
        )
    }

    val isDownloaded = remember(currentPlaylist.id, currentPlaylist.tracks, downloadedPlaylistIds, downloadedTrackIds) {
        downloadedPlaylistIds.contains(currentPlaylist.id) ||
            (currentPlaylist.tracks.isNotEmpty() && currentPlaylist.tracks.all { downloadedTrackIds.contains(it.id) })
    }
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

    val backdropGradient = remember(artworkPalette.vibrantAccent, isDarkMode) {
        Brush.verticalGradient(
            colors = listOf(
                artworkPalette.vibrantAccent.copy(alpha = if (isDarkMode) 0.30f else 0.14f),
                Color.Transparent
            )
        )
    }

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
                .background(backdropGradient)
        )

        val navBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(bottom = 168.dp + navBottomPadding),
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. Centered Hero Artwork + Metadata + Play / Shuffle Pill Buttons
            item(key = "playlist_hero", contentType = "playlist_hero") {
                val artShape = RoundedCornerShape(EchoRadius.lg)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(top = 56.dp, start = 20.dp, end = 20.dp, bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(220.dp)
                            .graphicsLayer {
                                if (!isReducedMotion) {
                                    val s = 1f - heroScrollProgress * 0.12f
                                    scaleX = s
                                    scaleY = s
                                    alpha = 1f - heroScrollProgress * 0.6f
                                    translationY = heroScrollProgress * 32f
                                }
                            }
                            .shadow(16.dp, RoundedCornerShape(EchoRadius.xl), spotColor = Color.Black.copy(alpha = 0.24f))
                            .clip(RoundedCornerShape(EchoRadius.xl))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        if (currentPlaylist.id == "pl_liked_songs") {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(primaryColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Favorite,
                                    contentDescription = "Liked Songs",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(72.dp)
                                )
                            }
                        } else {
                            EchoArtworkImage(
                                imageUrl = currentPlaylist.coverArtUrl,
                                contentDescription = currentPlaylist.name,
                                seedKey = currentPlaylist.name,
                                iconSize = 64.dp,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

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
                        text = "${currentPlaylist.ownerName} • ${currentPlaylist.tracks.size} ${if (currentPlaylist.tracks.size == 1) "song" else "songs"}" +
                            if (totalMinutes > 0L) " • $totalMinutes min" else "",
                        color = primaryColor,
                        style = MaterialTheme.typography.bodyMedium.tabularFigures().copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
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
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val hasTracks = currentPlaylist.tracks.isNotEmpty()
                        com.spotify.clone.core.designsystem.EchoConnectedButtonGroup(
                            primaryLabel = "Play",
                            primaryIcon = Icons.Filled.PlayArrow,
                            onPrimaryClick = {
                                if (hasTracks) {
                                    musicController.playTrack(currentPlaylist.tracks.first(), currentPlaylist.tracks)
                                }
                            },
                            secondaryLabel = "Shuffle",
                            secondaryIcon = Icons.Filled.Shuffle,
                            onSecondaryClick = {
                                if (hasTracks) {
                                    val shuffled = currentPlaylist.tracks.shuffled()
                                    musicController.playTrack(shuffled.first(), shuffled)
                                }
                            },
                            primaryContainer = if (hasTracks) primaryColor else primaryColor.copy(alpha = 0.4f),
                            onPrimaryContainer = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.weight(1f)
                        )

                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(if (isDownloaded) 25.dp else 16.dp))
                                .background(
                                    if (isDownloaded) MaterialTheme.colorScheme.tertiaryContainer
                                    else MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                                .echoPressable(
                                    scaleOnPress = 0.94f,
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
                                com.spotify.clone.core.designsystem.ExpressiveLoadingIndicator(size = 28.dp)
                            } else {
                                Icon(
                                    imageVector = if (isDownloaded) Icons.Filled.DownloadDone else Icons.Filled.Download,
                                    contentDescription = "Download",
                                    tint = if (isDownloaded) MaterialTheme.colorScheme.onTertiaryContainer else textPrimary
                                )
                            }
                        }
                    }
                }
            }

            if (currentPlaylist.tracks.isEmpty()) {
                item(key = "playlist_empty", contentType = "empty_state") {
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
                                "Tap Add Songs below to search Indian, English & global tracks.",
                            color = textSecondary,
                            fontSize = 13.5.sp,
                            textAlign = TextAlign.Center
                        )
                        if (currentPlaylist.id.startsWith("pl_custom_")) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { showAddSongsSheet = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = primaryColor,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = RoundedCornerShape(EchoRadius.pill)
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Songs", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            } else {
                if (currentPlaylist.id.startsWith("pl_custom_")) {
                    item(key = "playlist_add_songs_bar", contentType = "action_row") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .clip(RoundedCornerShape(EchoRadius.pill))
                                .background(MaterialTheme.colorScheme.tertiaryContainer)
                                .echoPressable(scaleOnPress = 0.985f, hapticOnClick = true) {
                                    showAddSongsSheet = true
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Add Songs to Playlist",
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
                // Find-in-Playlist filter bar (Spotify FindInContextView pattern)
                if (currentPlaylist.tracks.size > 4) {
                    item(key = "playlist_find_bar", contentType = "search_bar") {
                        val barShape = RoundedCornerShape(EchoRadius.pill)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .height(46.dp)
                                .clip(barShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .padding(start = 16.dp, end = 6.dp),
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
                                textStyle = MaterialTheme.typography.bodyMedium.copy(color = textPrimary),
                                cursorBrush = SolidColor(primaryColor),
                                modifier = Modifier.weight(1f),
                                decorationBox = { inner ->
                                    if (filterQuery.isEmpty()) {
                                        Text(
                                            text = "Find in ${currentPlaylist.name}",
                                            color = textSecondary,
                                            style = MaterialTheme.typography.bodyMedium,
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
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .echoPressable(scaleOnPress = 0.88f) { filterQuery = "" },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Clear filter",
                                        tint = textSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                item(key = "playlist_tracks_card", contentType = "track_card_list") {
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
                                key(track.id) {
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
                                }
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

        // Frosted Sticky Top Bar with floating Liquid Glass back button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(topBarBg)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EchoIconCircleButton(
                onClick = onBack,
                floatingGlass = true
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
                exit = fadeOut(tween(140)) + slideOutVertically(tween(140)) { 8 },
                modifier = Modifier.weight(1f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = currentPlaylist.name,
                        color = textPrimary,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (!showStickyTitle) {
                Spacer(modifier = Modifier.weight(1f))
            }
            if (currentPlaylist.id.startsWith("pl_custom_")) {
                EchoIconCircleButton(
                    onClick = {
                        editNameDraft = currentPlaylist.name
                        editDescDraft = currentPlaylist.description
                        showEditPlaylistDialog = true
                    },
                    floatingGlass = true
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Edit Playlist",
                        tint = textPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }

    if (showEditPlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showEditPlaylistDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Edit Playlist", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editNameDraft,
                        onValueChange = { editNameDraft = it },
                        label = { Text("Playlist name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editDescDraft,
                        onValueChange = { editDescDraft = it },
                        label = { Text("Description (optional)") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clean = editNameDraft.trim()
                        if (clean.isNotBlank()) {
                            scope.launch {
                                repository.updatePlaylistDetails(currentPlaylist.id, clean, editDescDraft)
                                playlist = repository.getPlaylistById(currentPlaylist.id)
                                showEditPlaylistDialog = false
                                Toast.makeText(context, "Playlist updated", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryColor,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditPlaylistDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showAddSongsSheet) {
        AddSongsToPlaylistSheet(
            playlist = currentPlaylist,
            repository = repository,
            onDismiss = { showAddSongsSheet = false },
            onTrackAdded = { addedTrack ->
                scope.launch {
                    repository.addTrackToPlaylist(addedTrack, currentPlaylist.id)
                    playlist = repository.getPlaylistById(currentPlaylist.id)
                }
            }
        )
    }

    selectedTrackForOptions?.let { track ->
        val isCustomPlaylist = currentPlaylist.id.startsWith("pl_custom_")
        TrackOptionsBottomSheet(
            track = track,
            musicController = musicController,
            onDismiss = { selectedTrackForOptions = null },
            onViewArtist = { artist -> onArtistClick?.invoke(artist) },
            onRemoveFromPlaylist = if (isCustomPlaylist) {
                {
                    scope.launch {
                        repository.removeTrackFromPlaylist(currentPlaylist.id, track.id)
                        playlist = repository.getPlaylistById(currentPlaylist.id)
                        Toast.makeText(context, "Removed from ${currentPlaylist.name}", Toast.LENGTH_SHORT).show()
                    }
                }
            } else null
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSongsToPlaylistSheet(
    playlist: Playlist,
    repository: AudioRepository,
    onDismiss: () -> Unit,
    onTrackAdded: (Track) -> Unit
) {
    val textPrimary = MaterialTheme.colorScheme.onSurface
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var query by remember { mutableStateOf("") }
    var remoteResults by remember { mutableStateOf<List<Track>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var addedIds by remember(playlist.id, playlist.tracks) {
        mutableStateOf(playlist.tracks.map { it.id }.toSet())
    }

    LaunchedEffect(query) {
        val q = query.trim()
        if (q.length < 2) {
            remoteResults = emptyList()
            isSearching = false
            return@LaunchedEffect
        }
        isSearching = true
        kotlinx.coroutines.delay(280L)
        remoteResults = runCatching { repository.search(q) }.getOrDefault(emptyList())
        isSearching = false
    }

    val curatedCandidates = remember(query) {
        val q = query.trim().lowercase()
        val all = WorldOnboardingCatalog.allOnboardingTracks()
        if (q.isBlank()) all.take(25)
        else all.filter {
            it.title.lowercase().contains(q) ||
                it.artistName.lowercase().contains(q) ||
                it.albumName.lowercase().contains(q)
        }.take(20)
    }

    val displayTracks = remember(remoteResults, curatedCandidates) {
        (remoteResults + curatedCandidates).distinctBy {
            "${it.title.lowercase()}|${it.artistName.lowercase()}"
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = EchoRadius.xl, topEnd = EchoRadius.xl),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = "Add to ${playlist.name}",
                color = textPrimary,
                style = MaterialTheme.typography.headlineMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(10.dp))
            val searchShape = RoundedCornerShape(EchoRadius.pill)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(searchShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Search, contentDescription = null, tint = textSecondary, modifier = Modifier.size(19.dp))
                Spacer(modifier = Modifier.width(10.dp))
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = TextStyle(color = textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        if (query.isEmpty()) {
                            Text("Search any song worldwide...", color = textSecondary, fontSize = 14.5.sp)
                        }
                        inner()
                    }
                )
                if (isSearching) {
                    com.spotify.clone.core.designsystem.ExpressiveLoadingIndicator(size = 24.dp)
                } else if (query.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Clear",
                        tint = textSecondary,
                        modifier = Modifier
                            .size(18.dp)
                            .echoPressable(scaleOnPress = 0.88f) { query = "" }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(displayTracks, key = { "add_${it.id}_${it.title}" }) { tr ->
                    val alreadyAdded = tr.id in addedIds
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(if (alreadyAdded) EchoRadius.lg else EchoRadius.md))
                            .background(
                                if (alreadyAdded) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                                else MaterialTheme.colorScheme.surfaceContainerLow
                            )
                            .echoPressable(scaleOnPress = 0.985f, enabled = !alreadyAdded) {
                                addedIds = addedIds + tr.id
                                onTrackAdded(tr)
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        EchoArtworkImage(
                            imageUrl = tr.coverArtUrl,
                            contentDescription = tr.title,
                            seedKey = tr.title,
                            iconSize = 18.dp,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tr.title,
                                color = textPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = tr.artistName,
                                color = textSecondary,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    if (alreadyAdded) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.primaryContainer
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (alreadyAdded) Icons.Filled.Check else Icons.Filled.Add,
                                contentDescription = if (alreadyAdded) "Added" else "Add",
                                tint = if (alreadyAdded) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
