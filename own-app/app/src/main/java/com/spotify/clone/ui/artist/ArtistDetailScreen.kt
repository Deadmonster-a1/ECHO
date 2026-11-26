package com.spotify.clone.ui.artist

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Shuffle
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
import com.spotify.clone.core.data.SampleCatalog
import com.spotify.clone.core.data.WorldOnboardingCatalog
import com.spotify.clone.core.designsystem.EchoArtistAvatar
import com.spotify.clone.core.designsystem.EchoArtworkImage
import com.spotify.clone.core.designsystem.EchoIconCircleButton
import com.spotify.clone.core.designsystem.EchoInsetCard
import com.spotify.clone.core.designsystem.EchoMotion
import com.spotify.clone.core.designsystem.EchoRadius
import com.spotify.clone.core.designsystem.EchoSectionHeader
import com.spotify.clone.core.designsystem.EchoSpacing
import com.spotify.clone.core.designsystem.LivingEqualizer
import com.spotify.clone.core.designsystem.LocalReducedMotion
import com.spotify.clone.core.designsystem.SwipeableTrackRow
import com.spotify.clone.core.designsystem.ThemeManager
import com.spotify.clone.core.designsystem.echoPressable
import com.spotify.clone.core.designsystem.rememberAnimatedArtworkColors
import com.spotify.clone.core.designsystem.tabularFigures
import com.spotify.clone.core.model.Artist
import com.spotify.clone.core.model.Track
import com.spotify.clone.core.player.MusicController
import com.spotify.clone.ui.components.TrackOptionsBottomSheet

/**
 * Apple Music-inspired Artist Detail Screen with parallax hero banner,
 * smooth sticky top bar reveal, tabular track numbers, and tactile pill actions.
 */
@Composable
fun ArtistDetailScreen(
    artistName: String,
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

    var activeArtistName by remember(artistName) { mutableStateOf(artistName) }
    var isLoading by remember { mutableStateOf(true) }
    var artist by remember { mutableStateOf<Artist?>(null) }
    var rawTopTracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var selectedTrackForOptions by remember { mutableStateOf<Track?>(null) }

    val favoriteTracks by repository.getFavoriteTracks().collectAsState(initial = emptyList())
    val favIds = remember(favoriteTracks) { favoriteTracks.mapTo(HashSet()) { it.id } }
    val topTracks = remember(rawTopTracks, favIds) {
        rawTopTracks.map { t ->
            val fav = t.id in favIds
            if (t.isFavorite == fav) t else t.copy(isFavorite = fav)
        }
    }

    LaunchedEffect(activeArtistName) {
        isLoading = true
        val primaryName = activeArtistName.split(",", "&", "•", "/").firstOrNull()?.trim()
            .orEmpty().ifBlank { activeArtistName }
        val homeArtists = runCatching { repository.getHomeArtists() }.getOrDefault(emptyList())
        val initialTracks = runCatching { repository.search(activeArtistName).take(15) }.getOrDefault(emptyList())
        val tracks = if (initialTracks.isNotEmpty() || primaryName.equals(activeArtistName, ignoreCase = true)) {
            initialTracks
        } else {
            runCatching { repository.search(primaryName).take(15) }.getOrDefault(emptyList())
        }
        val matched = homeArtists.find {
            it.name.equals(activeArtistName, ignoreCase = true) ||
                it.name.equals(primaryName, ignoreCase = true) ||
                it.name.contains(primaryName, ignoreCase = true)
        } ?: SampleCatalog.sampleArtists.find {
            it.name.contains(primaryName, ignoreCase = true)
        } ?: WorldOnboardingCatalog.findOnboardingArtist(primaryName)?.toArtist()?.let { onb ->
            if (onb.avatarUrl.isBlank() && tracks.isNotEmpty()) {
                onb.copy(avatarUrl = tracks.first().coverArtUrl)
            } else {
                onb
            }
        } ?: Artist(
            id = "art_${primaryName.lowercase().replace(" ", "_")}",
            name = activeArtistName,
            avatarUrl = tracks.firstOrNull()?.coverArtUrl.orEmpty(),
            monthlyListeners = 12_000_000L
        )
        artist = matched
        rawTopTracks = tracks
        isLoading = false
    }

    if (isLoading || artist == null) {
        Box(
            modifier = modifier.fillMaxSize().background(screenBg),
            contentAlignment = Alignment.Center
        ) {
            com.spotify.clone.core.designsystem.ExpressiveLoadingIndicator(size = 52.dp)
        }
        return
    }

    val currentArtist = artist!!
    val artistSeed = remember(currentArtist.id, currentArtist.avatarUrl) {
        Track(
            id = currentArtist.id,
            title = currentArtist.name,
            artistName = currentArtist.name,
            albumName = currentArtist.name,
            coverArtUrl = currentArtist.avatarUrl,
            durationMs = 0L,
            mediaUri = ""
        )
    }
    val artworkPalette = rememberAnimatedArtworkColors(artistSeed)

    val listState = rememberLazyListState()
    val showStickyTitle by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 360 }
    }
    val heroScrollProgress by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 1f
            else (listState.firstVisibleItemScrollOffset / 500f).coerceIn(0f, 1f)
        }
    }
    val topBarBg by animateColorAsState(
        targetValue = if (showStickyTitle) screenBg else Color.Transparent,
        animationSpec = tween(EchoMotion.DurationMicro, easing = EchoMotion.EncoreButtonEase),
        label = "art_topbar_bg"
    )
    val backdropGradient = remember(artworkPalette.vibrantAccent, isDarkMode) {
        Brush.verticalGradient(
            colors = listOf(
                artworkPalette.vibrantAccent.copy(alpha = if (isDarkMode) 0.24f else 0.12f),
                Color.Transparent
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(screenBg)
    ) {
        // Dynamic atmospheric tint from artist portrait
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp)
                .background(backdropGradient)
        )

        val navBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(bottom = 168.dp + navBottomPadding),
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. Parallax Artist Hero Header
            item(key = "artist_hero", contentType = "artist_hero") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(310.dp)
                ) {
                    EchoArtworkImage(
                        imageUrl = currentArtist.avatarUrl,
                        contentDescription = currentArtist.name,
                        seedKey = currentArtist.name,
                        iconSize = 72.dp,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                if (!isReducedMotion) {
                                    val s = 1f + heroScrollProgress * 0.08f
                                    scaleX = s
                                    scaleY = s
                                    translationY = heroScrollProgress * 60f
                                    alpha = 1f - heroScrollProgress * 0.65f
                                }
                            }
                    )
                    val heroScrim = remember(screenBg) {
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                screenBg.copy(alpha = 0.65f),
                                screenBg
                            )
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(heroScrim)
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                    ) {
                        Text(
                            text = currentArtist.name,
                            color = textPrimary,
                            style = MaterialTheme.typography.displayLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${topTracks.size} top songs",
                            color = textSecondary,
                            style = MaterialTheme.typography.bodyMedium.tabularFigures().copy(fontSize = 13.5.sp)
                        )
                    }
                }
            }

            // 2. M3 Expressive Connected Pill Play, Shuffle & Morphing Follow Button
            item(key = "artist_actions", contentType = "artist_actions") {
                val hasTracks = topTracks.isNotEmpty()
                val favoriteArtists by com.spotify.clone.core.profile.UserProfileManager.favoriteArtists.collectAsState()
                val isFollowed = remember(favoriteArtists, currentArtist.name) {
                    favoriteArtists.any { it.equals(currentArtist.name, ignoreCase = true) }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    com.spotify.clone.core.designsystem.EchoConnectedButtonGroup(
                        primaryLabel = "Play",
                        primaryIcon = Icons.Filled.PlayArrow,
                        onPrimaryClick = {
                            if (topTracks.isNotEmpty()) {
                                musicController.playTrack(topTracks.first(), topTracks)
                            } else {
                                Toast.makeText(context, "No tracks available", Toast.LENGTH_SHORT).show()
                            }
                        },
                        secondaryLabel = "Shuffle",
                        secondaryIcon = Icons.Filled.Shuffle,
                        onSecondaryClick = {
                            if (topTracks.isNotEmpty()) {
                                val shuffled = topTracks.shuffled()
                                musicController.playTrack(shuffled.first(), shuffled)
                            }
                        },
                        primaryContainer = if (hasTracks) primaryColor else primaryColor.copy(alpha = 0.4f),
                        onPrimaryContainer = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.weight(1f)
                    )

                    val followCorner = if (isFollowed) 25.dp else 16.dp
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(followCorner))
                            .background(
                                if (isFollowed) MaterialTheme.colorScheme.tertiaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                            .echoPressable(scaleOnPress = 0.92f, hapticOnClick = true) {
                                val nowFollowed = com.spotify.clone.core.profile.UserProfileManager
                                    .toggleFavoriteArtist(currentArtist.name)
                                com.spotify.clone.core.recommendation.EchoRecommendationEngine
                                    .toggleFollowedArtist(currentArtist.name, nowFollowed)
                                Toast.makeText(
                                    context,
                                    if (nowFollowed) "Following ${currentArtist.name}" else "Unfollowed ${currentArtist.name}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isFollowed) Icons.Filled.Check else Icons.Filled.Radio,
                            contentDescription = if (isFollowed) "Following" else "Follow",
                            tint = if (isFollowed) MaterialTheme.colorScheme.onTertiaryContainer else textPrimary,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // 3. Top Songs
            if (topTracks.isNotEmpty()) {
                item(key = "artist_top_songs_header", contentType = "section_header") {
                    EchoSectionHeader(title = "Top Songs")
                }
                item(key = "artist_top_songs_card", contentType = "track_card_list") {
                    EchoInsetCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        topTracks.forEachIndexed { index, track ->
                            val isPlaying = playbackState.currentTrack?.id == track.id && playbackState.isPlaying
                            SwipeableTrackRow(
                                isFavorite = track.isFavorite,
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
                            ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isPlaying) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f) else Color.Transparent)
                                    .echoPressable(scaleOnPress = 1.0f) {
                                        musicController.playTrack(track, topTracks)
                                    }
                                    .padding(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.width(26.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AnimatedContent(
                                        targetState = isPlaying,
                                        transitionSpec = {
                                            (fadeIn(tween(150)) + scaleIn(initialScale = 0.85f, animationSpec = tween(150))) togetherWith
                                                (fadeOut(tween(110)) + scaleOut(targetScale = 0.85f, animationSpec = tween(110)))
                                        },
                                        label = "artist_track_idx_$index"
                                    ) { playing ->
                                        if (playing) {
                                            LivingEqualizer(
                                                isPlaying = true,
                                                barCount = 3,
                                                barColor = primaryColor,
                                                maxHeight = 14.dp
                                            )
                                        } else {
                                            Text(
                                                text = "${index + 1}",
                                                color = textSecondary,
                                                style = MaterialTheme.typography.bodyMedium.tabularFigures().copy(
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                EchoArtworkImage(
                                    imageUrl = track.coverArtUrl,
                                    contentDescription = track.title,
                                    seedKey = "${track.title}_${track.artistName}",
                                    iconSize = 18.dp,
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = track.title,
                                        color = if (isPlaying) primaryColor else textPrimary,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.SemiBold
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = track.albumName,
                                        color = textSecondary,
                                        fontSize = 12.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(EchoSpacing.minTouchTarget)
                                        .clip(CircleShape)
                                        .echoPressable(scaleOnPress = 0.88f) { selectedTrackForOptions = track },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.MoreHoriz,
                                        contentDescription = "More",
                                        tint = textSecondary
                                    )
                                }
                            }
                            }
                            if (index < topTracks.lastIndex) {
                                HorizontalDivider(
                                    color = borderColor,
                                    thickness = 0.5.dp,
                                    modifier = Modifier.padding(start = 94.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                item(key = "artist_no_tracks", contentType = "empty_state") {
                    Text(
                        text = "No tracks found for ${currentArtist.name}.",
                        color = textSecondary,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)
                    )
                }
            }

            // 4. Similar Artists
            item(key = "similar_artists", contentType = "artist_carousel") {
                Spacer(modifier = Modifier.height(16.dp))
                EchoSectionHeader(title = "Similar Artists")
                val related = remember(currentArtist.name) {
                    val onbMatch = WorldOnboardingCatalog.findOnboardingArtist(currentArtist.name)
                    val relatedFromGraph = if (onbMatch != null) {
                        WorldOnboardingCatalog.getSimilarArtistsStrip(
                            selectedLanguages = onbMatch.languages,
                            selectedGenreIds = onbMatch.genreIds,
                            selectedSongTracks = emptyList(),
                            selectedArtists = setOf(onbMatch.name),
                            limit = 8
                        ).map { it.artist.toArtist() }
                    } else {
                        emptyList()
                    }
                    (relatedFromGraph + SampleCatalog.sampleArtists)
                        .filter { !it.name.equals(currentArtist.name, ignoreCase = true) }
                        .distinctBy { it.name.lowercase() }
                        .take(8)
                }
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(
                        items = related,
                        key = { "rel_${it.id}" },
                        contentType = { "artist_card" }
                    ) { rel ->
                        Column(
                            modifier = Modifier
                                .width(92.dp)
                                .echoPressable(scaleOnPress = 0.95f) {
                                    if (onArtistClick != null) {
                                        onArtistClick(rel.name)
                                    } else {
                                        activeArtistName = rel.name
                                    }
                                },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            EchoArtistAvatar(
                                name = rel.name,
                                avatarUrl = rel.avatarUrl,
                                size = 84.dp,
                                borderColor = borderColor,
                                borderWidth = 0.6.dp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = rel.name,
                                color = textPrimary,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
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
                exit = fadeOut(tween(140)) + slideOutVertically(tween(140)) { 8 }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = currentArtist.name,
                        color = textPrimary,
                        style = MaterialTheme.typography.titleLarge,
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
            onDismiss = { selectedTrackForOptions = null }
        )
    }
}
