package com.spotify.clone.ui.library

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.DownloadForOffline
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.spotify.clone.core.designsystem.EchoAccentCoral
import com.spotify.clone.core.designsystem.EchoArtworkImage
import com.spotify.clone.core.designsystem.EchoFilterChip
import com.spotify.clone.core.designsystem.EchoInsetCard
import com.spotify.clone.core.designsystem.EchoMotion
import com.spotify.clone.core.designsystem.EchoProfileButton
import com.spotify.clone.core.designsystem.EchoRadius
import com.spotify.clone.core.designsystem.EchoSectionHeader
import com.spotify.clone.core.designsystem.EchoSpacing
import com.spotify.clone.core.designsystem.LiveAudioSpectrum
import com.spotify.clone.core.designsystem.LivingEqualizer
import com.spotify.clone.core.designsystem.LocalReducedMotion
import com.spotify.clone.core.designsystem.ThemeManager
import com.spotify.clone.core.designsystem.echoPressable
import com.spotify.clone.core.designsystem.tabularFigures
import com.spotify.clone.core.download.TrackDownloadManager
import com.spotify.clone.core.model.Playlist
import com.spotify.clone.core.model.Track
import com.spotify.clone.core.player.MusicController
import com.spotify.clone.core.profile.PrismAvatarOption
import com.spotify.clone.core.profile.UserProfileManager
import com.spotify.clone.core.recommendation.EchoRecommendationEngine
import com.spotify.clone.ui.components.TrackOptionsBottomSheet
import com.spotify.clone.ui.components.TrackRowItem

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onPlaylistClick: (String) -> Unit,
    onSettingsClick: () -> Unit = {},
    onArtistClick: ((String) -> Unit)? = null,
    scrollToTopTrigger: Long = 0L,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDarkMode by ThemeManager.isDarkMode.collectAsState()
    val isReducedMotion = LocalReducedMotion.current
    val state by viewModel.uiState.collectAsState()

    val userName by UserProfileManager.userName.collectAsState()
    val avatarId by UserProfileManager.avatarId.collectAsState()
    val activeAvatar = PrismAvatarOption.fromId(avatarId)
    val avatarInitial = remember(userName) {
        userName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "E"
    }
    val avatarColors = remember(activeAvatar) {
        listOf(activeAvatar.startColor, activeAvatar.endColor)
    }

    val musicController = remember { MusicController.getInstance(context) }
    val playbackState by musicController.playbackState.collectAsState()
    val downloadManager = remember { TrackDownloadManager.getInstance(context) }
    val downloadedTrackIds by downloadManager.downloadedTrackIds.collectAsState()
    val favIds = remember(state.likedTracks) { state.likedTracks.mapTo(HashSet()) { it.id } }
    val downloadedTracks = remember(downloadedTrackIds, favIds) {
        downloadManager.getAllDownloadedTracks().map { t ->
            val fav = t.id in favIds
            if (t.isFavorite == fav) t else t.copy(isFavorite = fav)
        }
    }

    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var playlistToEdit by remember { mutableStateOf<Playlist?>(null) }
    var editPlaylistName by remember { mutableStateOf("") }
    var editPlaylistDesc by remember { mutableStateOf("") }
    var playlistToDelete by remember { mutableStateOf<Playlist?>(null) }
    var selectedTrackForOptions by remember { mutableStateOf<Track?>(null) }

    val libQuery = state.searchQuery.trim().lowercase()
    val filteredPlaylists = remember(state.playlists, libQuery) {
        if (libQuery.isBlank()) state.playlists
        else state.playlists.filter {
            it.name.lowercase().contains(libQuery) ||
                it.description.lowercase().contains(libQuery) ||
                it.tracks.any { t -> t.title.lowercase().contains(libQuery) || t.artistName.lowercase().contains(libQuery) }
        }
    }
    val filteredLiked = remember(state.likedTracks, libQuery) {
        if (libQuery.isBlank()) state.likedTracks
        else state.likedTracks.filter {
            it.title.lowercase().contains(libQuery) || it.artistName.lowercase().contains(libQuery)
        }
    }
    val filteredDownloads = remember(downloadedTracks, libQuery) {
        if (libQuery.isBlank()) downloadedTracks
        else downloadedTracks.filter {
            it.title.lowercase().contains(libQuery) || it.artistName.lowercase().contains(libQuery)
        }
    }
    val filteredLocal = remember(state.localTracks, libQuery) {
        if (libQuery.isBlank()) state.localTracks
        else state.localTracks.filter {
            it.title.lowercase().contains(libQuery) || it.artistName.lowercase().contains(libQuery)
        }
    }

    val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            com.spotify.clone.core.data.AudioRepository.notifyAudioPermissionGranted()
            viewModel.dispatch(LibraryEvent.ScanLocalAudio)
        } else {
            Toast.makeText(context, "Audio storage permission is needed to scan local tracks", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        if (state.localTracks.isEmpty() && !state.isScanningLocal) {
            viewModel.scanLocalFiles(forceRefresh = false)
        }
    }

    val screenBg = MaterialTheme.colorScheme.background
    val cardBg = MaterialTheme.colorScheme.surfaceContainerLow
    val borderColor = MaterialTheme.colorScheme.outlineVariant
    val primaryColor = MaterialTheme.colorScheme.primary
    val textPrimary = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val listState = rememberLazyListState()
    val topGenre = remember(state.likedTracks) {
        EchoRecommendationEngine.getTopPreferredGenre(emptyList(), state.likedTracks)
    }

    LaunchedEffect(scrollToTopTrigger) {
        if (scrollToTopTrigger > 0L) {
            listState.animateScrollToItem(0)
        }
    }

    val filterTabs = listOf(
        "All" to "All",
        "Playlists" to "Playlists",
        "Liked Songs" to "Liked",
        "Downloads" to "Downloads",
        "Local" to "Local"
    )
    val showLiked = state.selectedFilter == "All" || state.selectedFilter == "Liked Songs"
    val showPlaylists = state.selectedFilter == "All" || state.selectedFilter == "Playlists"
    val showDownloads = (state.selectedFilter == "All" && downloadedTracks.isNotEmpty()) || state.selectedFilter == "Downloads"
    val showLocal = (state.selectedFilter == "All" && state.localTracks.isNotEmpty()) || state.selectedFilter == "Local"

    val isLikedPlaying = playbackState.isPlaying &&
        state.likedTracks.any { it.id == playbackState.currentTrack?.id }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(screenBg)
    ) {
        val navBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(bottom = 168.dp + navBottomPadding)
        ) {
            // 1. M3 Expressive Header (Display Title + Extended Create Pill + Profile)
            item(key = "library_header", contentType = "library_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Library",
                        color = textPrimary,
                        style = MaterialTheme.typography.displayLarge,
                        modifier = Modifier.weight(1f)
                    )

                    Row(
                        modifier = Modifier
                            .height(44.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .echoPressable(scaleOnPress = 0.93f, hapticOnClick = true) {
                                showCreatePlaylistDialog = true
                            }
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "New Playlist",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "New",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    EchoProfileButton(
                        initial = avatarInitial,
                        gradientColors = avatarColors,
                        onClick = onSettingsClick
                    )
                }
            }

            // 2. M3 Expressive Filter Chips + Tonal Search Pill
            item(key = "library_filters", contentType = "filter_strip") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filterTabs.forEach { (value, label) ->
                        EchoFilterChip(
                            text = label,
                            isSelected = state.selectedFilter == value,
                            onClick = { viewModel.dispatch(LibraryEvent.SelectFilter(value)) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                val searchShape = RoundedCornerShape(EchoRadius.xxl)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = EchoSpacing.screenHorizontalMargin)
                        .height(46.dp),
                    shape = searchShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 16.dp, end = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = textSecondary,
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        BasicTextField(
                            value = state.searchQuery,
                            onValueChange = { viewModel.dispatch(LibraryEvent.SearchQueryChanged(it)) },
                            singleLine = true,
                            textStyle = TextStyle(color = textPrimary, fontSize = 14.5.sp, fontWeight = FontWeight.Medium),
                            cursorBrush = SolidColor(primaryColor),
                            modifier = Modifier.weight(1f),
                            decorationBox = { inner ->
                                if (state.searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search in your library",
                                        color = textSecondary,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                inner()
                            }
                        )
                        if (state.searchQuery.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                                    .echoPressable(scaleOnPress = 0.88f) {
                                        viewModel.dispatch(LibraryEvent.SearchQueryChanged(""))
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Clear",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 3. M3 Expressive Liked Songs Featured Container
            if (showLiked) {
                item(key = "liked_songs_card", contentType = "featured_card") {
                    val likedShape = RoundedCornerShape(EchoRadius.xl)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = EchoSpacing.screenHorizontalMargin, vertical = 4.dp)
                            .echoPressable(scaleOnPress = 0.985f, hapticOnClick = true) {
                                onPlaylistClick("pl_liked_songs")
                            },
                        shape = likedShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(EchoRadius.md))
                                    .background(primaryColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Favorite,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Liked Songs",
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                                )
                                Text(
                                    text = "${state.likedTracks.size} ${if (state.likedTracks.size == 1) "song" else "songs"}",
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
                                    style = MaterialTheme.typography.bodySmall.tabularFigures()
                                )
                            }
                            if (isLikedPlaying) {
                                LivingEqualizer(
                                    isPlaying = true,
                                    barCount = 3,
                                    barColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    maxHeight = 15.dp,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }
                            if (state.likedTracks.isNotEmpty()) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(topStart = 22.dp, bottomStart = 22.dp, topEnd = 8.dp, bottomEnd = 8.dp))
                                            .background(MaterialTheme.colorScheme.surface)
                                            .echoPressable(scaleOnPress = 0.88f, hapticOnClick = true) {
                                                val shuffled = state.likedTracks.shuffled()
                                                musicController.playTrack(shuffled.first(), shuffled)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Shuffle,
                                            contentDescription = "Shuffle Liked",
                                            tint = primaryColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp, topEnd = 22.dp, bottomEnd = 22.dp))
                                            .background(primaryColor)
                                            .echoPressable(scaleOnPress = 0.88f, hapticOnClick = true) {
                                                musicController.playTrack(state.likedTracks.first(), state.likedTracks)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.PlayArrow,
                                            contentDescription = "Play Liked",
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(21.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                if (state.selectedFilter == "Liked Songs" && filteredLiked.isNotEmpty()) {
                    items(
                        items = filteredLiked,
                        key = { "liked_${it.id}" },
                        contentType = { "track_row" }
                    ) { track ->
                        TrackRowItem(
                            track = track,
                            isPlaying = playbackState.currentTrack?.id == track.id && playbackState.isPlaying,
                            onClick = { musicController.playTrack(track, filteredLiked) },
                            onMoreClick = { selectedTrackForOptions = track },
                            onSwipeQueue = {
                                musicController.playNext(track)
                                Toast.makeText(context, "Playing next: ${track.title}", Toast.LENGTH_SHORT).show()
                            },
                            onSwipeFavorite = {
                                musicController.toggleFavorite(track)
                                Toast.makeText(context, "Removed from Liked Songs", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }

            // 4. Personal Mix Card (M3 Expressive Tertiary Sound Capsule)
            if (state.selectedFilter == "All") {
                item(key = "sound_capsule_card", contentType = "sound_capsule") {
                    val capsuleShape = RoundedCornerShape(EchoRadius.xl)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = EchoSpacing.screenHorizontalMargin)
                            .echoPressable(scaleOnPress = 0.985f, hapticOnClick = true) {
                                musicController.playGenreRadio(topGenre)
                                Toast.makeText(context, "Playing ${topGenre.shortLabel} Mix", Toast.LENGTH_SHORT).show()
                            },
                        shape = capsuleShape,
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(15.dp))
                                    .background(MaterialTheme.colorScheme.tertiary),
                                contentAlignment = Alignment.Center
                            ) {
                                if (playbackState.isPlaying) {
                                    LiveAudioSpectrum(
                                        isPlaying = true,
                                        barCount = 4,
                                        genre = topGenre,
                                        barColor = MaterialTheme.colorScheme.onTertiary,
                                        barWidth = 2.dp,
                                        barSpacing = 1.5.dp,
                                        height = 14.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onTertiary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${topGenre.displayName} Mix",
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Made from your listening",
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.78f),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(EchoRadius.pill))
                                    .background(MaterialTheme.colorScheme.tertiary)
                                    .padding(horizontal = 16.dp, vertical = 9.dp)
                            ) {
                                Text(
                                    text = "Play",
                                    color = MaterialTheme.colorScheme.onTertiary,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(22.dp))
                }
            }

            // 5. Playlists
            if (showPlaylists) {
                item(key = "playlists_header", contentType = "section_header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 14.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Playlists",
                            color = textPrimary,
                            style = MaterialTheme.typography.headlineMedium
                        )
                        if (state.playlists.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .echoPressable(scaleOnPress = 0.88f, hapticOnClick = true) {
                                        viewModel.dispatch(LibraryEvent.ToggleViewMode)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                AnimatedContent(
                                    targetState = state.isGridView,
                                    transitionSpec = {
                                        (fadeIn(tween(150)) + scaleIn(initialScale = 0.85f, animationSpec = tween(150))) togetherWith
                                            (fadeOut(tween(110)) + scaleOut(targetScale = 0.85f, animationSpec = tween(110)))
                                    },
                                    label = "lib_view_icon"
                                ) { isGrid ->
                                    Icon(
                                        imageVector = if (isGrid) Icons.AutoMirrored.Filled.FormatListBulleted else Icons.Outlined.GridView,
                                        contentDescription = "Toggle View",
                                        tint = textSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (state.playlists.isEmpty()) {
                    item(key = "empty_playlists", contentType = "empty_card") {
                        EchoInsetCard(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .echoPressable(scaleOnPress = 0.985f) { showCreatePlaylistDialog = true }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(EchoRadius.sm))
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.LibraryMusic,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Create your first playlist",
                                        color = textPrimary,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "Tap to organize your favorite tracks.",
                                        color = textSecondary,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                } else {
                    item(key = "playlists_content", contentType = "playlist_collection") {
                        AnimatedContent(
                            targetState = state.isGridView,
                            transitionSpec = {
                                if (isReducedMotion) {
                                    fadeIn(tween(140)) togetherWith fadeOut(tween(110))
                                } else {
                                    (fadeIn(tween(220, easing = EchoMotion.EaseOutStrong)) +
                                        scaleIn(initialScale = 0.96f, animationSpec = tween(220, easing = EchoMotion.EaseOutStrong))) togetherWith
                                        (fadeOut(tween(140)) + scaleOut(targetScale = 0.96f, animationSpec = tween(140)))
                                }
                            },
                            label = "library_playlists_layout"
                        ) { isGrid ->
                            Column {
                                if (isGrid) {
                                    filteredPlaylists.chunked(2).forEach { pair ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 20.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                                        ) {
                                            pair.forEach { playlist ->
                                                val tileShape = RoundedCornerShape(EchoRadius.lg)
                                                Column(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(EchoRadius.xl))
                                                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                                        .echoPressable(scaleOnPress = 0.96f) { onPlaylistClick(playlist.id) }
                                                        .padding(10.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .aspectRatio(1f)
                                                            .clip(tileShape)
                                                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        val cover = playlist.coverArtUrl.ifBlank {
                                                            playlist.tracks.firstOrNull { it.coverArtUrl.isNotBlank() }?.coverArtUrl.orEmpty()
                                                        }
                                                        EchoArtworkImage(
                                                            imageUrl = cover,
                                                            contentDescription = playlist.name,
                                                            seedKey = playlist.name,
                                                            iconSize = 36.dp,
                                                            modifier = Modifier.fillMaxSize()
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.height(10.dp))
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = playlist.name,
                                                                color = textPrimary,
                                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                            Text(
                                                                text = "${playlist.tracks.size} ${if (playlist.tracks.size == 1) "song" else "songs"}",
                                                                color = textSecondary,
                                                                style = MaterialTheme.typography.bodySmall.tabularFigures()
                                                            )
                                                        }
                                                        Box(
                                                            modifier = Modifier
                                                                .size(30.dp)
                                                                .clip(CircleShape)
                                                                .echoPressable(scaleOnPress = 0.88f) {
                                                                    editPlaylistName = playlist.name
                                                                    editPlaylistDesc = playlist.description
                                                                    playlistToEdit = playlist
                                                                },
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Filled.Edit,
                                                                contentDescription = "Rename Playlist",
                                                                tint = textSecondary,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                        Box(
                                                            modifier = Modifier
                                                                .size(30.dp)
                                                                .clip(CircleShape)
                                                                .echoPressable(scaleOnPress = 0.88f) {
                                                                    playlistToDelete = playlist
                                                                },
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Filled.DeleteOutline,
                                                                contentDescription = "Delete Playlist",
                                                                tint = textSecondary,
                                                                modifier = Modifier.size(17.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                            if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                } else {
                                    filteredPlaylists.forEach { playlist ->
                                        val thumbShape = RoundedCornerShape(EchoRadius.sm)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 20.dp, vertical = 4.dp)
                                                .clip(RoundedCornerShape(EchoRadius.lg))
                                                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                                .echoPressable(scaleOnPress = 0.99f) { onPlaylistClick(playlist.id) }
                                                .padding(start = 10.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val cover = playlist.coverArtUrl.ifBlank {
                                                playlist.tracks.firstOrNull { it.coverArtUrl.isNotBlank() }?.coverArtUrl.orEmpty()
                                            }
                                            EchoArtworkImage(
                                                imageUrl = cover,
                                                contentDescription = playlist.name,
                                                seedKey = playlist.name,
                                                iconSize = 22.dp,
                                                modifier = Modifier
                                                    .size(56.dp)
                                                    .clip(thumbShape)
                                            )
                                            Spacer(modifier = Modifier.width(14.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = playlist.name,
                                                    color = textPrimary,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${playlist.tracks.size} ${if (playlist.tracks.size == 1) "song" else "songs"}",
                                                    color = textSecondary,
                                                    style = MaterialTheme.typography.bodySmall.tabularFigures()
                                                )
                                            }
                                            if (playlist.tracks.isNotEmpty()) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(CircleShape)
                                                        .echoPressable(scaleOnPress = 0.88f, hapticOnClick = true) {
                                                            musicController.playTrack(playlist.tracks.first(), playlist.tracks)
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.PlayArrow,
                                                        contentDescription = "Play Playlist",
                                                        tint = primaryColor,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(CircleShape)
                                                    .echoPressable(scaleOnPress = 0.88f) {
                                                        editPlaylistName = playlist.name
                                                        editPlaylistDesc = playlist.description
                                                        playlistToEdit = playlist
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Edit,
                                                    contentDescription = "Rename Playlist",
                                                    tint = textSecondary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(CircleShape)
                                                    .echoPressable(scaleOnPress = 0.88f) { playlistToDelete = playlist },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.DeleteOutline,
                                                    contentDescription = "Delete Playlist",
                                                    tint = textSecondary,
                                                    modifier = Modifier.size(19.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                item(key = "playlists_spacer", contentType = "spacer") { Spacer(modifier = Modifier.height(16.dp)) }
            }

            // 6. Offline Downloads
            if (showDownloads) {
                item(key = "downloads_header", contentType = "section_header") {
                    EchoSectionHeader(
                        title = "Downloads (${filteredDownloads.size})",
                        actionLabel = if (filteredDownloads.isNotEmpty()) "Play All" else null,
                        onActionClick = {
                            if (filteredDownloads.isNotEmpty()) {
                                musicController.playTrack(filteredDownloads.first(), filteredDownloads)
                            }
                        }
                    )
                }
                if (filteredDownloads.isEmpty()) {
                    item(key = "empty_downloads", contentType = "empty_card") {
                        EchoInsetCard(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.DownloadForOffline,
                                    contentDescription = null,
                                    tint = textSecondary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = if (libQuery.isNotBlank()) "No downloads matching \"${state.searchQuery.trim()}\"" else "Downloaded songs for offline listening appear here.",
                                    color = textSecondary,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                } else {
                    item(key = "downloads_card", contentType = "track_card_list") {
                        EchoInsetCard(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                            filteredDownloads.forEachIndexed { index, track ->
                                val isCurrentPlaying = playbackState.currentTrack?.id == track.id && playbackState.isPlaying
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .echoPressable(scaleOnPress = 0.985f) {
                                            musicController.playTrack(track, filteredDownloads)
                                        }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    EchoArtworkImage(
                                        imageUrl = track.coverArtUrl,
                                        contentDescription = track.title,
                                        seedKey = track.title,
                                        iconSize = 20.dp,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(EchoRadius.xs))
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (isCurrentPlaying) {
                                                LivingEqualizer(
                                                    isPlaying = true,
                                                    barCount = 3,
                                                    barColor = primaryColor,
                                                    maxHeight = 12.dp,
                                                    barWidth = 2.dp
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                            }
                                            Text(
                                                text = track.title,
                                                color = if (playbackState.currentTrack?.id == track.id) primaryColor else textPrimary,
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Filled.DownloadDone,
                                                contentDescription = null,
                                                tint = primaryColor,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = track.artistName,
                                                color = textSecondary,
                                                style = MaterialTheme.typography.bodySmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(EchoSpacing.minTouchTarget)
                                            .clip(CircleShape)
                                            .echoPressable(scaleOnPress = 0.88f) {
                                                downloadManager.removeTrackDownload(track.id)
                                                Toast.makeText(context, "Removed from downloads", Toast.LENGTH_SHORT).show()
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.DeleteOutline,
                                            contentDescription = "Remove download",
                                            tint = textSecondary,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                }
                                if (index < filteredDownloads.lastIndex) {
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
                item(key = "downloads_spacer", contentType = "spacer") { Spacer(modifier = Modifier.height(16.dp)) }
            }

            // 7. Local Device Audio
            if (showLocal) {
                item(key = "local_header", contentType = "section_header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "On This Device (${filteredLocal.size})",
                            color = textPrimary,
                            style = MaterialTheme.typography.headlineMedium.tabularFigures()
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (filteredLocal.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(EchoRadius.pill))
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .echoPressable(scaleOnPress = 0.94f, hapticOnClick = true) {
                                            musicController.playTrack(filteredLocal.first(), filteredLocal)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "Play All",
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                            if (state.isScanningLocal) {
                                com.spotify.clone.core.designsystem.ExpressiveLoadingIndicator(size = 30.dp)
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(EchoSpacing.minTouchTarget)
                                        .clip(CircleShape)
                                        .echoPressable(scaleOnPress = 0.88f, hapticOnClick = true) {
                                            val hasPerm = androidx.core.content.ContextCompat.checkSelfPermission(
                                                context,
                                                audioPermission
                                            ) == PackageManager.PERMISSION_GRANTED
                                            if (hasPerm) {
                                                viewModel.dispatch(LibraryEvent.ScanLocalAudio)
                                            } else {
                                                audioPermissionLauncher.launch(audioPermission)
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Refresh,
                                        contentDescription = "Rescan local audio",
                                        tint = primaryColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (filteredLocal.isEmpty() && !state.isScanningLocal) {
                    item(key = "empty_local", contentType = "empty_card") {
                        EchoInsetCard(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AudioFile,
                                    contentDescription = null,
                                    tint = textSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (libQuery.isNotBlank()) "No local songs matching \"${state.searchQuery.trim()}\"" else "No local audio files found on this device.",
                                    color = textSecondary,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                } else {
                    item(key = "local_tracks_card", contentType = "track_card_list") {
                        EchoInsetCard(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                            filteredLocal.forEachIndexed { index, track ->
                                TrackRowItem(
                                    track = track,
                                    isPlaying = playbackState.currentTrack?.id == track.id && playbackState.isPlaying,
                                    onClick = { musicController.playTrack(track, filteredLocal) },
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
                                if (index < filteredLocal.lastIndex) {
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
    }

    if (showCreatePlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("New Playlist", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    label = { Text("Playlist name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            viewModel.dispatch(LibraryEvent.CreatePlaylist(newPlaylistName))
                            newPlaylistName = ""
                            showCreatePlaylistDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryColor,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylistDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    playlistToEdit?.let { pl ->
        AlertDialog(
            onDismissRequest = { playlistToEdit = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Edit Playlist", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editPlaylistName,
                        onValueChange = { editPlaylistName = it },
                        label = { Text("Playlist name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editPlaylistDesc,
                        onValueChange = { editPlaylistDesc = it },
                        label = { Text("Description (optional)") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editPlaylistName.isNotBlank()) {
                            viewModel.dispatch(LibraryEvent.RenamePlaylist(pl.id, editPlaylistName, editPlaylistDesc))
                            playlistToEdit = null
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
                TextButton(onClick = { playlistToEdit = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    playlistToDelete?.let { pl ->
        AlertDialog(
            onDismissRequest = { playlistToDelete = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Delete Playlist?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete \"${pl.name}\"?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.dispatch(LibraryEvent.DeletePlaylist(pl.id))
                        playlistToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EchoAccentCoral)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { playlistToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    selectedTrackForOptions?.let { track ->
        TrackOptionsBottomSheet(
            track = track,
            musicController = musicController,
            onDismiss = { selectedTrackForOptions = null },
            onViewArtist = onArtistClick
        )
    }
}
