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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PlayArrow
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
import com.spotify.clone.core.designsystem.EchoInsetCard
import com.spotify.clone.core.designsystem.EchoMotion
import com.spotify.clone.core.designsystem.EchoSectionHeader
import com.spotify.clone.core.designsystem.LivingEqualizer
import com.spotify.clone.core.designsystem.LocalAppTheme
import com.spotify.clone.core.designsystem.LocalReducedMotion
import com.spotify.clone.core.designsystem.SwipeableTrackRow
import com.spotify.clone.core.designsystem.ThemeManager
import com.spotify.clone.core.designsystem.echoContrastOn
import com.spotify.clone.core.designsystem.echoPressable
import com.spotify.clone.core.designsystem.echoSpecularBorder
import com.spotify.clone.core.designsystem.rememberAnimatedArtworkColors
import com.spotify.clone.core.designsystem.resolvedBackground
import com.spotify.clone.core.designsystem.resolvedBorder
import com.spotify.clone.core.designsystem.resolvedCard
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

    var activeArtistName by remember(artistName) { mutableStateOf(artistName) }
    var isLoading by remember { mutableStateOf(true) }
    var artist by remember { mutableStateOf<Artist?>(null) }
    var topTracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var selectedTrackForOptions by remember { mutableStateOf<Track?>(null) }

    LaunchedEffect(activeArtistName) {
        isLoading = true
        val homeArtists = runCatching { repository.getHomeArtists() }.getOrDefault(emptyList())
        val tracks = runCatching { repository.search(activeArtistName).take(15) }.getOrDefault(emptyList())
        val matched = homeArtists.find {
            it.name.equals(activeArtistName, ignoreCase = true) ||
                it.name.contains(activeArtistName, ignoreCase = true)
        } ?: SampleCatalog.sampleArtists.find {
            it.name.contains(activeArtistName, ignoreCase = true)
        } ?: Artist(
            id = "art_${activeArtistName.lowercase().replace(" ", "_")}",
            name = activeArtistName,
            avatarUrl = tracks.firstOrNull()?.coverArtUrl.orEmpty(),
            monthlyListeners = 12_000_000L
        )
        artist = matched
        topTracks = tracks
        isLoading = false
    }

    if (isLoading || artist == null) {
        Box(
            modifier = modifier.fillMaxSize().background(screenBg),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = activeTheme.primaryColor)
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
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            artworkPalette.vibrantAccent.copy(alpha = if (isDarkMode) 0.24f else 0.12f),
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
            // 1. Parallax Artist Hero Header
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(310.dp)
                ) {
                    AsyncImage(
                        model = currentArtist.avatarUrl,
                        contentDescription = currentArtist.name,
                        contentScale = ContentScale.Crop,
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
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        screenBg.copy(alpha = 0.65f),
                                        screenBg
                                    )
                                )
                            )
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

            // 2. Tactile Play & Shuffle Buttons
            item {
                val hasTracks = topTracks.isNotEmpty()
                val actionShape = RoundedCornerShape(14.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
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
                                if (topTracks.isNotEmpty()) {
                                    musicController.playTrack(topTracks.first(), topTracks)
                                } else {
                                    Toast.makeText(context, "No tracks available", Toast.LENGTH_SHORT).show()
                                }
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
                                val shuffled = topTracks.shuffled()
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
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // 3. Top Songs
            if (topTracks.isNotEmpty()) {
                item { EchoSectionHeader(title = "Top Songs") }
                item(key = "artist_top_songs_card") {
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
                                    .background(if (isPlaying) activeTheme.primaryColor.copy(alpha = 0.08f) else Color.Transparent)
                                    .echoPressable(scaleOnPress = 0.985f) {
                                        musicController.playTrack(track, topTracks)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 9.dp),
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
                                                barColor = activeTheme.primaryColor,
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
                                AsyncImage(
                                    model = track.coverArtUrl,
                                    contentDescription = track.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = track.title,
                                        color = if (isPlaying) activeTheme.primaryColor else textPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
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
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .echoPressable(scaleOnPress = 0.86f) { selectedTrackForOptions = track },
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
                item {
                    Text(
                        text = "No tracks found for ${currentArtist.name}.",
                        color = textSecondary,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)
                    )
                }
            }

            // 4. Similar Artists
            item {
                Spacer(modifier = Modifier.height(16.dp))
                EchoSectionHeader(title = "Similar Artists")
                val related = SampleCatalog.sampleArtists.filter { !it.name.equals(currentArtist.name, ignoreCase = true) }
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(related, key = { "rel_${it.id}" }) { rel ->
                        Column(
                            modifier = Modifier
                                .width(92.dp)
                                .echoPressable(scaleOnPress = 0.95f) {
                                    activeArtistName = rel.name
                                },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AsyncImage(
                                model = rel.avatarUrl,
                                contentDescription = rel.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(84.dp)
                                    .clip(CircleShape)
                                    .background(cardBg)
                                    .echoSpecularBorder(isDarkMode, CircleShape, borderColor)
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
                        text = currentArtist.name,
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
            onDismiss = { selectedTrackForOptions = null }
        )
    }
}
