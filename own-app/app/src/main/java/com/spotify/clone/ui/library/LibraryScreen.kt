package com.spotify.clone.ui.library

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.DownloadForOffline
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.spotify.clone.core.designsystem.EchoAccentCoral
import com.spotify.clone.core.designsystem.EchoFilterChip
import com.spotify.clone.core.designsystem.EchoInsetCard
import com.spotify.clone.core.designsystem.EchoMotion
import com.spotify.clone.core.designsystem.EchoProfileButton
import com.spotify.clone.core.designsystem.EchoRadius
import com.spotify.clone.core.designsystem.EchoSectionHeader
import com.spotify.clone.core.designsystem.EchoSpacing
import com.spotify.clone.core.designsystem.LiveAudioSpectrum
import com.spotify.clone.core.designsystem.LivingEqualizer
import com.spotify.clone.core.designsystem.LocalAppTheme
import com.spotify.clone.core.designsystem.LocalReducedMotion
import com.spotify.clone.core.designsystem.ThemeManager
import com.spotify.clone.core.designsystem.echoContrastOn
import com.spotify.clone.core.designsystem.echoPressable
import com.spotify.clone.core.designsystem.resolvedBackground
import com.spotify.clone.core.designsystem.resolvedBorder
import com.spotify.clone.core.designsystem.resolvedCard
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
    val activeTheme = LocalAppTheme.current
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
    var playlistToDelete by remember { mutableStateOf<Playlist?>(null) }
    var selectedTrackForOptions by remember { mutableStateOf<Track?>(null) }

    LaunchedEffect(Unit) {
        if (state.localTracks.isEmpty() && !state.isScanningLocal) {
            viewModel.scanLocalFiles(forceRefresh = false)
        }
    }

    val screenBg = activeTheme.resolvedBackground(isDarkMode)
    val cardBg = activeTheme.resolvedCard(isDarkMode)
    val borderColor = activeTheme.resolvedBorder(isDarkMode)
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
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(bottom = 168.dp)
        ) {
            // 1. Clean Header (Title + New Playlist + Profile)
            item(key = "library_header") {
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

                    Box(
                        modifier = Modifier
                            .size(EchoSpacing.minTouchTarget)
                            .clip(CircleShape)
                            .background(activeTheme.primaryColor.copy(alpha = 0.14f))
                            .echoPressable(scaleOnPress = 0.90f, hapticOnClick = true) {
                                showCreatePlaylistDialog = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "New Playlist",
                            tint = activeTheme.primaryColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    EchoProfileButton(
                        initial = avatarInitial,
                        gradientColors = avatarColors,
                        onClick = onSettingsClick
                    )
                }
            }

            // 2. Filter Chips
            item(key = "library_filters") {
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
                Spacer(modifier = Modifier.height(14.dp))
            }

            // 3. Liked Songs Featured Card
            if (showLiked) {
                item(key = "liked_songs_card") {
                    val likedShape = RoundedCornerShape(EchoRadius.md)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = EchoSpacing.screenHorizontalMargin, vertical = 4.dp)
                            .clip(likedShape)
                            .background(cardBg)
                            .border(0.6.dp, borderColor, likedShape)
                            .echoPressable(scaleOnPress = 0.985f, hapticOnClick = true) {
                                onPlaylistClick("pl_liked_songs")
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(EchoRadius.sm))
                                .background(activeTheme.primaryColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = null,
                                tint = echoContrastOn(activeTheme.primaryColor),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Liked Songs",
                                color = textPrimary,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "${state.likedTracks.size} ${if (state.likedTracks.size == 1) "song" else "songs"}",
                                color = textSecondary,
                                style = MaterialTheme.typography.bodySmall.tabularFigures()
                            )
                        }
                        if (isLikedPlaying) {
                            LivingEqualizer(
                                isPlaying = true,
                                barCount = 3,
                                barColor = activeTheme.primaryColor,
                                maxHeight = 15.dp,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                        if (state.likedTracks.isNotEmpty()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(EchoSpacing.minTouchTarget)
                                        .clip(CircleShape)
                                        .background(activeTheme.primaryColor.copy(alpha = 0.12f))
                                        .echoPressable(scaleOnPress = 0.88f, hapticOnClick = true) {
                                            val shuffled = state.likedTracks.shuffled()
                                            musicController.playTrack(shuffled.first(), shuffled)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Shuffle,
                                        contentDescription = "Shuffle Liked",
                                        tint = activeTheme.primaryColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(EchoSpacing.minTouchTarget)
                                        .clip(CircleShape)
                                        .background(activeTheme.primaryColor)
                                        .echoPressable(scaleOnPress = 0.88f, hapticOnClick = true) {
                                            musicController.playTrack(state.likedTracks.first(), state.likedTracks)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PlayArrow,
                                        contentDescription = "Play Liked",
                                        tint = echoContrastOn(activeTheme.primaryColor),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                if (state.selectedFilter == "Liked Songs" && state.likedTracks.isNotEmpty()) {
                    items(state.likedTracks, key = { "liked_${it.id}" }) { track ->
                        TrackRowItem(
                            track = track,
                            isPlaying = playbackState.currentTrack?.id == track.id && playbackState.isPlaying,
                            onClick = { musicController.playTrack(track, state.likedTracks) },
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

            // 4. Personal Mix Card (Clean Sound Capsule)
            if (state.selectedFilter == "All") {
                item(key = "sound_capsule_card") {
                    val capsuleShape = RoundedCornerShape(EchoRadius.md)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = EchoSpacing.screenHorizontalMargin)
                            .clip(capsuleShape)
                            .background(cardBg)
                            .border(0.6.dp, borderColor, capsuleShape)
                            .echoPressable(scaleOnPress = 0.985f, hapticOnClick = true) {
                                musicController.playGenreRadio(topGenre)
                                Toast.makeText(context, "Playing ${topGenre.shortLabel} Mix", Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(activeTheme.primaryColor.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (playbackState.isPlaying) {
                                LiveAudioSpectrum(
                                    isPlaying = true,
                                    barCount = 4,
                                    genre = topGenre,
                                    barColor = activeTheme.primaryColor,
                                    barWidth = 2.dp,
                                    barSpacing = 1.5.dp,
                                    height = 14.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    tint = activeTheme.primaryColor,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${topGenre.displayName} Mix",
                                color = textPrimary,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Made from your listening",
                                color = textSecondary,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(EchoRadius.pill))
                                .background(activeTheme.primaryColor)
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Play",
                                color = echoContrastOn(activeTheme.primaryColor),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(22.dp))
                }
            }

            // 5. Playlists
            if (showPlaylists) {
                item(key = "playlists_header") {
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
                    item(key = "empty_playlists") {
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
                                        .background(activeTheme.primaryColor.copy(alpha = 0.14f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.LibraryMusic,
                                        contentDescription = null,
                                        tint = activeTheme.primaryColor
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
                    item(key = "playlists_content") {
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
                                    state.playlists.chunked(2).forEach { pair ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 20.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                                        ) {
                                            pair.forEach { playlist ->
                                                val tileShape = RoundedCornerShape(EchoRadius.md)
                                                Column(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .echoPressable(scaleOnPress = 0.97f) { onPlaylistClick(playlist.id) }
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .aspectRatio(1f)
                                                            .clip(tileShape)
                                                            .background(cardBg)
                                                            .border(0.6.dp, borderColor, tileShape),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        if (playlist.coverArtUrl.isNotBlank()) {
                                                            AsyncImage(
                                                                model = playlist.coverArtUrl,
                                                                contentDescription = playlist.name,
                                                                contentScale = ContentScale.Crop,
                                                                modifier = Modifier.fillMaxSize()
                                                            )
                                                        } else {
                                                            Icon(
                                                                imageVector = Icons.Outlined.LibraryMusic,
                                                                contentDescription = null,
                                                                tint = textSecondary,
                                                                modifier = Modifier.size(36.dp)
                                                            )
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = playlist.name,
                                                                color = textPrimary,
                                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
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
                                                                .size(32.dp)
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
                                    state.playlists.forEach { playlist ->
                                        val thumbShape = RoundedCornerShape(EchoRadius.sm)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .echoPressable(scaleOnPress = 1.0f) { onPlaylistClick(playlist.id) }
                                                .padding(start = 20.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(54.dp)
                                                    .clip(thumbShape)
                                                    .background(cardBg)
                                                    .border(0.6.dp, borderColor, thumbShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (playlist.coverArtUrl.isNotBlank()) {
                                                    AsyncImage(
                                                        model = playlist.coverArtUrl,
                                                        contentDescription = playlist.name,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Outlined.LibraryMusic,
                                                        contentDescription = null,
                                                        tint = textSecondary
                                                    )
                                                }
                                            }
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
                                            Box(
                                                modifier = Modifier
                                                    .size(EchoSpacing.minTouchTarget)
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
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }

            // 6. Offline Downloads
            if (showDownloads) {
                item(key = "downloads_header") {
                    EchoSectionHeader(
                        title = "Downloads (${downloadedTracks.size})",
                        actionLabel = if (downloadedTracks.isNotEmpty()) "Play All" else null,
                        onActionClick = {
                            if (downloadedTracks.isNotEmpty()) {
                                musicController.playTrack(downloadedTracks.first(), downloadedTracks)
                            }
                        }
                    )
                }
                if (downloadedTracks.isEmpty()) {
                    item(key = "empty_downloads") {
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
                                    text = "Downloaded songs for offline listening appear here.",
                                    color = textSecondary,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                } else {
                    item(key = "downloads_card") {
                        EchoInsetCard(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                            downloadedTracks.forEachIndexed { index, track ->
                                val isCurrentPlaying = playbackState.currentTrack?.id == track.id && playbackState.isPlaying
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .echoPressable(scaleOnPress = 0.985f) {
                                            musicController.playTrack(track, downloadedTracks)
                                        }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = track.coverArtUrl,
                                        contentDescription = track.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(EchoRadius.xs))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (isCurrentPlaying) {
                                                LivingEqualizer(
                                                    isPlaying = true,
                                                    barCount = 3,
                                                    barColor = activeTheme.primaryColor,
                                                    maxHeight = 12.dp,
                                                    barWidth = 2.dp
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                            }
                                            Text(
                                                text = track.title,
                                                color = if (playbackState.currentTrack?.id == track.id) activeTheme.primaryColor else textPrimary,
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Filled.DownloadDone,
                                                contentDescription = null,
                                                tint = activeTheme.primaryColor,
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
                                if (index < downloadedTracks.lastIndex) {
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
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }

            // 7. Local Device Audio
            if (showLocal) {
                item(key = "local_header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "On This Device (${state.localTracks.size})",
                            color = textPrimary,
                            style = MaterialTheme.typography.headlineMedium.tabularFigures()
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (state.localTracks.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(EchoRadius.pill))
                                        .background(activeTheme.primaryColor.copy(alpha = 0.14f))
                                        .echoPressable(scaleOnPress = 0.94f, hapticOnClick = true) {
                                            musicController.playTrack(state.localTracks.first(), state.localTracks)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "Play All",
                                        color = activeTheme.primaryColor,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                            if (state.isScanningLocal) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = activeTheme.primaryColor
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(EchoSpacing.minTouchTarget)
                                        .clip(CircleShape)
                                        .echoPressable(scaleOnPress = 0.88f, hapticOnClick = true) {
                                            viewModel.dispatch(LibraryEvent.ScanLocalAudio)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Refresh,
                                        contentDescription = "Rescan local audio",
                                        tint = activeTheme.primaryColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (state.localTracks.isEmpty() && !state.isScanningLocal) {
                    item(key = "empty_local") {
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
                                    text = "No local audio files found on this device.",
                                    color = textSecondary,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                } else {
                    item(key = "local_tracks_card") {
                        EchoInsetCard(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                            state.localTracks.forEachIndexed { index, track ->
                                TrackRowItem(
                                    track = track,
                                    isPlaying = playbackState.currentTrack?.id == track.id && playbackState.isPlaying,
                                    onClick = { musicController.playTrack(track, state.localTracks) },
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
                                if (index < state.localTracks.lastIndex) {
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
                        containerColor = activeTheme.primaryColor,
                        contentColor = echoContrastOn(activeTheme.primaryColor)
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
