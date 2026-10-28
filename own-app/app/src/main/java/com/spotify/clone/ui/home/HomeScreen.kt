package com.spotify.clone.ui.home

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Adjust
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.spotify.clone.core.designsystem.EchoMotion
import com.spotify.clone.core.designsystem.LocalAppTheme
import com.spotify.clone.core.designsystem.LocalReducedMotion
import com.spotify.clone.core.designsystem.ThemeManager
import com.spotify.clone.core.designsystem.echoPressable
import com.spotify.clone.core.model.Artist
import com.spotify.clone.core.model.Playlist
import com.spotify.clone.core.model.Track
import com.spotify.clone.core.player.MusicController
import com.spotify.clone.core.profile.UserProfileManager
import com.spotify.clone.ui.components.EqualizerBottomSheet
import com.spotify.clone.ui.components.TrackOptionsBottomSheet
import java.util.Calendar

/**
 * ECHO — Modern Spotify 2025/2026 Home Screen
 * - Ambient dynamic mesh gradient header
 * - Contextual top content pills: All | Music | Podcasts
 * - Personalized, time-aware greeting
 * - Iconic 2x4 "Jump Back In" quick-access grid with 1-tap playback
 * - Dynamic Daylist & AI listening flow card
 * - Real-time mood filters with responsive catalog adaptation
 * - Blockbuster album carousels with floating Play FABs
 * - Studio master trending singles with playback visualizer
 * - Personalized mix packs and artist radio stations
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onPlaylistClick: (String) -> Unit,
    onArtistClick: (String) -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onCheckUpdateClick: () -> Unit = {},
    scrollToTopTrigger: Long = 0L,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeTheme = LocalAppTheme.current
    val isReducedMotion = LocalReducedMotion.current
    val isDarkMode by ThemeManager.isDarkMode.collectAsState()
    val musicController = remember { MusicController.getInstance(context) }
    val state by viewModel.uiState.collectAsState()
    val playbackState by musicController.playbackState.collectAsState()
    val userName by UserProfileManager.userName.collectAsState()

    var showEqualizerSheet by remember { mutableStateOf(false) }
    var selectedTrackForOptions by remember { mutableStateOf<Track?>(null) }

    val listState = rememberLazyListState()

    LaunchedEffect(scrollToTopTrigger) {
        if (scrollToTopTrigger > 0L) {
            listState.animateScrollToItem(0)
        }
    }


    if (state.isLoading) {
        HomeScreenSkeleton(
            isDarkMode = isDarkMode,
            modifier = modifier
        )
        return
    }

    val screenBg = if (isDarkMode) Color(0xFF121212) else Color(0xFFF7F5F2)

    Box(modifier = modifier.fillMaxSize().background(screenBg)) {
        // Atmospheric Ambient Aurora Mesh Gradient at top of feed
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            activeTheme.primaryColor.copy(alpha = if (isDarkMode) 0.32f else 0.16f),
                            activeTheme.gradientEnd.copy(alpha = if (isDarkMode) 0.14f else 0.06f),
                            Color.Transparent
                        )
                    )
                )
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 130.dp)
        ) {
            // A. Top Bar (Avatar + ECHO Branding + Notification + Theme Toggle)
            item(key = "top_brand_bar") {
                EchoTopAppBar(
                    onProfileClick = onProfileClick,
                    onNotificationClick = onCheckUpdateClick,
                    onToggleTheme = { ThemeManager.toggleDarkMode() },
                    isDarkMode = isDarkMode
                )
            }

            // B. Personalized Greeting Header with Display Name
            item(key = "personalized_greeting") {
                PersonalizedGreetingHeader(
                    userName = userName,
                    isReducedMotion = isReducedMotion
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // C. 2x4 "Jump Back In" (Quick Access Grid)
            if (state.quickPlayGrid.isNotEmpty()) {
                item(key = "quick_access_grid") {
                    QuickAccessSection(
                        items = state.quickPlayGrid,
                        currentPlayingId = playbackState.currentTrack?.id,
                        isPlaying = playbackState.isPlaying,
                        onItemClick = { playlist -> onPlaylistClick(playlist.id) },
                        onPlayClick = { playlist ->
                            if (playlist.id == "pl_liked_songs" && playlist.tracks.isEmpty()) {
                                Toast.makeText(context, "No liked songs yet", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.dispatch(HomeEvent.PlayPlaylist(playlist))
                            }
                        },
                        isDarkMode = isDarkMode
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            // D. Spotify Daylist & AI Listening Flow Card
            item(key = "daylist_flow_card") {
                DaylistCard(
                    onPlayClick = {
                        state.featuredPlaylists.firstOrNull()?.let {
                            viewModel.dispatch(HomeEvent.PlayPlaylist(it))
                        }
                    },
                    isDarkMode = isDarkMode
                )
                Spacer(modifier = Modifier.height(22.dp))
            }

            // E. Real-time Mood Selector (All, Melody, Mass, Lo-Fi, Indie, Devotional)
            item(key = "mood_selector") {
                MoodFilterRow(
                    selectedMood = state.selectedMood,
                    onSelectMood = { viewModel.dispatch(HomeEvent.SelectMood(it)) },
                    isDarkMode = isDarkMode
                )
                Spacer(modifier = Modifier.height(24.dp))
            }


            // G. Blockbuster Albums & Soundtracks with Floating Play FABs
            if (state.sculpturalAlbums.isNotEmpty()) {
                item(key = "sculptural_albums_section") {
                    SectionHeaderWithAction(
                        title = "Blockbuster Albums & Soundtracks",
                        subtitle = "Devara, Pushpa 2, Sita Ramam, Kalki & Ala Vaikunthapurramuloo",
                        onActionClick = {
                            state.sculpturalAlbums.firstOrNull()?.let { onPlaylistClick(it.id) }
                        }
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(top = 12.dp, bottom = 28.dp)
                    ) {
                        items(
                            items = state.sculpturalAlbums.take(8),
                            key = { "album_${it.id}" }
                        ) { playlist ->
                            SculpturalAlbumCard(
                                playlist = playlist,
                                onClick = { onPlaylistClick(playlist.id) },
                                onPlay = { viewModel.dispatch(HomeEvent.PlayPlaylist(playlist)) },
                                isDarkMode = isDarkMode
                            )
                        }
                    }
                }
            }

            // H. Made For You (Personalized Mixes & Radio)
            if (state.madeForYou.isNotEmpty()) {
                item(key = "made_for_you_section") {
                    SectionHeaderWithAction(
                        title = "Made For You • Personalized Mixes",
                        subtitle = "Daily Telugu Mix, Hyderabad Top 50 & Late Night Lo-Fi"
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.padding(top = 12.dp, bottom = 28.dp)
                    ) {
                        items(
                            items = state.madeForYou,
                            key = { "made_for_you_${it.id}" }
                        ) { playlist ->
                            MadeForYouCard(
                                playlist = playlist,
                                onClick = { onPlaylistClick(playlist.id) },
                                onPlay = { viewModel.dispatch(HomeEvent.PlayPlaylist(playlist)) },
                                isDarkMode = isDarkMode
                            )
                        }
                    }
                }
            }

            // H2. Popular Podcasts and Shows
            if (state.podcasts.isNotEmpty()) {
                item(key = "podcasts_shelf_section") {
                    SectionHeaderWithAction(
                        title = "Popular Podcasts & Shows",
                        subtitle = "Conversations, culture, health, technology and self-growth"
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(top = 12.dp, bottom = 28.dp)
                    ) {
                        items(
                            items = state.podcasts,
                            key = { "podcast_${it.id}" }
                        ) { podcast ->
                            EchoPodcastCard(
                                podcast = podcast,
                                onClick = { onPlaylistClick(podcast.id) },
                                onPlay = { viewModel.dispatch(HomeEvent.PlayPlaylist(podcast)) },
                                isDarkMode = isDarkMode
                            )
                        }
                    }
                }
            }

            // I. Trending Singles
            if (state.bestNewSongs.isNotEmpty()) {
                item(key = "best_new_songs_section") {
                    SectionHeaderWithAction(
                        title = "Trending Singles • Studio Master",
                        subtitle = "Top chart-toppers & high-fidelity audio"
                    )
                    BestNewSongsCarousel(
                        tracks = state.bestNewSongs,
                        currentTrackId = playbackState.currentTrack?.id,
                        isPlaying = playbackState.isPlaying,
                        onPlayTrack = { track ->
                            viewModel.dispatch(HomeEvent.PlayTrack(track, state.bestNewSongs))
                        },
                        onMoreClick = { selectedTrackForOptions = it },
                        isDarkMode = isDarkMode
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            // J. Artist Radio Stations (Top Music Directors & Vocalists)
            if (state.artistRadios.isNotEmpty()) {
                item(key = "artist_radios_section") {
                    SectionHeaderWithAction(
                        title = "Top Music Directors & Artists",
                        subtitle = "Non-stop stations: Anirudh, Sid Sriram, Thaman & DSP"
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(top = 12.dp, bottom = 28.dp)
                    ) {
                        items(
                            items = state.artistRadios.take(8),
                            key = { "artist_${it.id}" }
                        ) { artist ->
                            EchoArtistCard(
                                artist = artist,
                                onClick = { viewModel.dispatch(HomeEvent.StartArtistRadio(artist)) },
                                isDarkMode = isDarkMode
                            )
                        }
                    }
                }
            }
        }
    }

    if (showEqualizerSheet) {
        EqualizerBottomSheet(onDismiss = { showEqualizerSheet = false })
    }

    selectedTrackForOptions?.let { track ->
        TrackOptionsBottomSheet(
            track = track,
            musicController = musicController,
            onDismiss = { selectedTrackForOptions = null },
            onViewArtist = { artistName ->
                onArtistClick(artistName)
            }
        )
    }
}

/**
 * Top App Bar: Profile Avatar + ECHO Branding + Action icons
 */
@Composable
private fun EchoTopAppBar(
    onProfileClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onToggleTheme: () -> Unit,
    isDarkMode: Boolean
) {
    val activeTheme = LocalAppTheme.current
    val circleBtnBg = if (isDarkMode) Color(0xFF202028) else Color(0xFFFFFFFF)
    val circleBorder = if (isDarkMode) Color(0xFF323040) else Color(0xFFE5E0E8)
    val iconTint = if (isDarkMode) Color(0xFFE8E5EE) else Color(0xFF17171C)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // User Profile Avatar on left
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(activeTheme.primaryColor.copy(alpha = 0.22f))
                .border(1.5.dp, activeTheme.primaryColor, CircleShape)
                .echoPressable(scaleOnPress = 0.90f, onClick = onProfileClick),
            contentAlignment = Alignment.Center
        ) {
            val userInitial = UserProfileManager.getDisplayName().firstOrNull()?.uppercase() ?: "E"
            Text(
                text = userInitial,
                color = activeTheme.primaryColor,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = "ECHO",
            color = if (isDarkMode) Color.White else Color(0xFF17171C),
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.2.sp
        )

        Spacer(modifier = Modifier.weight(1f))

        // Notification Bell ("What's New" / Live Update)
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(circleBtnBg)
                .border(1.dp, circleBorder, CircleShape)
                .echoPressable(scaleOnPress = 0.90f, onClick = onNotificationClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Notifications,
                contentDescription = "What's New",
                tint = iconTint,
                modifier = Modifier.size(19.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Dark / Light Theme Toggle
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(circleBtnBg)
                .border(1.dp, circleBorder, CircleShape)
                .echoPressable(scaleOnPress = 0.90f, onClick = onToggleTheme),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isDarkMode) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                contentDescription = "Toggle Appearance",
                tint = iconTint,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

/**
 * Personalized Greeting Header with Display Name & Time Context
 */
@Composable
private fun PersonalizedGreetingHeader(
    userName: String,
    isReducedMotion: Boolean
) {
    val isDarkMode by ThemeManager.isDarkMode.collectAsState()

    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val timeGreeting = remember(hour) {
        when (hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..21 -> "Good evening"
            else -> "Midnight Reverie"
        }
    }

    val subtitleText = remember(hour) {
        when (hour) {
            in 5..11 -> "Ready for your morning listening flow?"
            in 12..16 -> "Feel the sound with your afternoon soundtrack."
            in 17..21 -> "Unwind with golden hour melodies."
            else -> "Late night tranquility & acoustic vibes."
        }
    }

    val riseOffsetY = remember { Animatable(if (isReducedMotion) 0f else 12f) }
    val entryAlpha = remember { Animatable(if (isReducedMotion) 1f else 0f) }

    LaunchedEffect(Unit) {
        if (!isReducedMotion) {
            riseOffsetY.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing)
            )
            entryAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 320, easing = LinearOutSlowInEasing)
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .graphicsLayer {
                translationY = riseOffsetY.value
                alpha = entryAlpha.value
            }
    ) {
        Text(
            text = if (userName.isNotBlank()) "$timeGreeting, $userName" else timeGreeting,
            fontWeight = FontWeight.Bold,
            fontSize = 25.sp,
            color = if (isDarkMode) Color(0xFFF6F3FA) else Color(0xFF17171C),
            letterSpacing = (-0.5).sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = subtitleText,
            fontSize = 13.5.sp,
            color = if (isDarkMode) Color(0xFFA6A2B3) else Color(0xFF777680),
            letterSpacing = (-0.1).sp
        )
    }
}

/**
 * 2x4 "Jump Back In" Quick Access Shortcuts Grid (Signature Spotify pattern)
 */
@Composable
private fun QuickAccessSection(
    items: List<Playlist>,
    currentPlayingId: String?,
    isPlaying: Boolean,
    onItemClick: (Playlist) -> Unit,
    onPlayClick: (Playlist) -> Unit,
    isDarkMode: Boolean
) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val columns = if (screenWidth >= 840) 4 else if (screenWidth >= 600) 3 else 2
    val chunked = items.take(columns * 2).chunked(columns)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        chunked.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { playlist ->
                    val isCurrentPlaylistPlaying = playlist.tracks.any { it.id == currentPlayingId } && isPlaying
                    Box(modifier = Modifier.weight(1f)) {
                        QuickAccessCard(
                            playlist = playlist,
                            isPlaying = isCurrentPlaylistPlaying,
                            onClick = { onItemClick(playlist) },
                            onPlay = { onPlayClick(playlist) },
                            isDarkMode = isDarkMode
                        )
                    }
                }
                // If fewer items than columns, fill empty weights
                for (i in 0 until (columns - rowItems.size)) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * Single Quick-Access Item Card with 1-Tap Playback FAB
 */
@Composable
private fun QuickAccessCard(
    playlist: Playlist,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    isDarkMode: Boolean
) {
    val activeTheme = LocalAppTheme.current
    val isLikedSongs = playlist.id == "pl_liked_songs"
    val cardBg = if (isDarkMode) Color(0xFF282828).copy(alpha = 0.70f) else Color.White
    val cardBorder = if (isDarkMode) Color(0xFF383838).copy(alpha = 0.5f) else Color(0xFFE5E0E8)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .shadow(
                elevation = if (isDarkMode) 2.dp else 4.dp,
                shape = RoundedCornerShape(8.dp),
                ambientColor = Color.Black.copy(alpha = 0.08f)
            )
            .clip(RoundedCornerShape(8.dp))
            .background(cardBg)
            .border(0.5.dp, cardBorder, RoundedCornerShape(8.dp))
            .echoPressable(scaleOnPress = 0.98f, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail Artwork / Gradient Heart
        if (isLikedSongs) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF5A44E2), Color(0xFF8A77FA))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = "Liked Songs",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        } else {
            AsyncImage(
                model = playlist.coverArtUrl,
                contentDescription = playlist.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .background(if (isDarkMode) Color(0xFF1E1E24) else Color(0xFFECE7E1))
            )
        }

        Spacer(modifier = Modifier.width(9.dp))

        // Title
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.name,
                color = if (isPlaying) activeTheme.primaryColor else if (isDarkMode) Color(0xFFF6F3FA) else Color(0xFF17171C),
                fontWeight = FontWeight.Bold,
                fontSize = 12.5.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 15.sp
            )
        }

        // Quick Play FAB / Visualizer
        IconButton(
            onClick = onPlay,
            modifier = Modifier
                .padding(end = 4.dp)
                .size(36.dp)
        ) {
            if (isPlaying) {
                Icon(
                    imageVector = Icons.Filled.GraphicEq,
                    contentDescription = "Playing",
                    tint = activeTheme.primaryColor,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(activeTheme.primaryColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Play",
                        tint = Color(0xFF121212),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Spotify Daylist Card with dynamic time-of-day listening vibes
 */
@Composable
private fun DaylistCard(
    onPlayClick: () -> Unit,
    isDarkMode: Boolean
) {
    val activeTheme = LocalAppTheme.current
    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }

    val (vibeTitle, vibeSub) = remember(hour) {
        when (hour) {
            in 5..11 -> "morning boost" to "uplifting acoustic melodies • energizing fresh start"
            in 12..16 -> "afternoon mass" to "high-octane Telugu blockbusters • rhythmic beats & pump"
            in 17..21 -> "golden hour sunset" to "soulful strings • acoustic breeze & heartwarming tunes"
            else -> "midnight reverie" to "late night lo-fi • mellow acoustic & ambient tranquility"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(150.dp)
            .shadow(
                elevation = if (isDarkMode) 6.dp else 10.dp,
                shape = RoundedCornerShape(22.dp),
                ambientColor = activeTheme.primaryColor.copy(alpha = 0.25f)
            )
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        activeTheme.primaryColor.copy(alpha = 0.85f),
                        Color(0xFF2C1E4A),
                        Color(0xFF140F24)
                    )
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(22.dp))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "DAYLIST • PERSONALIZED FLOW",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    repeat(4) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height((8 + it * 4).dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(alpha = 0.85f))
                        )
                    }
                }
            }

            Column {
                Text(
                    text = vibeTitle,
                    color = Color.White,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    letterSpacing = (-0.3).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = vibeSub,
                    color = Color.White.copy(alpha = 0.82f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Updates throughout your day",
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 11.sp
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White)
                        .echoPressable(scaleOnPress = 0.94f, onClick = onPlayClick)
                        .padding(horizontal = 16.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Play Daylist",
                            tint = Color(0xFF15141D),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Play Flow",
                            color = Color(0xFF15141D),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Mood Selector Row: All, Melody, Mass, Lo-Fi, Indie, Devotional
 */
@Composable
private fun MoodFilterRow(
    selectedMood: String,
    onSelectMood: (String) -> Unit,
    isDarkMode: Boolean
) {
    val activeTheme = LocalAppTheme.current

    val moods = listOf(
        "All" to Icons.Outlined.AutoAwesome,
        "Melody" to Icons.Outlined.MusicNote,
        "Mass" to Icons.Outlined.Bolt,
        "Lo-Fi" to Icons.Outlined.DarkMode,
        "Indie" to Icons.Outlined.GraphicEq,
        "Devotional" to Icons.Outlined.Spa
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        moods.forEach { (mood, icon) ->
            val isSelected = selectedMood == mood

            val animatedBg by animateColorAsState(
                targetValue = if (isSelected) {
                    activeTheme.primaryColor
                } else if (isDarkMode) {
                    Color(0xFF201F2C)
                } else {
                    Color(0xFFFFFFFF)
                },
                animationSpec = tween(220),
                label = "mood_bg"
            )

            val animatedBorder by animateColorAsState(
                targetValue = if (isSelected) {
                    activeTheme.primaryColor
                } else if (isDarkMode) {
                    Color(0xFF323042)
                } else {
                    Color(0xFFE5E0E8)
                },
                animationSpec = tween(220),
                label = "mood_border"
            )

            val animatedContentColor by animateColorAsState(
                targetValue = if (isSelected) {
                    Color.White
                } else if (isDarkMode) {
                    Color(0xFFE8E5EE)
                } else {
                    Color(0xFF17171C)
                },
                animationSpec = tween(220),
                label = "mood_text"
            )

            Box(
                modifier = Modifier
                    .shadow(
                        elevation = if (isSelected) 4.dp else if (isDarkMode) 1.dp else 2.dp,
                        shape = RoundedCornerShape(20.dp),
                        ambientColor = if (isSelected) activeTheme.primaryColor.copy(alpha = 0.25f) else Color.Transparent
                    )
                    .clip(RoundedCornerShape(20.dp))
                    .background(animatedBg)
                    .border(1.dp, animatedBorder, RoundedCornerShape(20.dp))
                    .echoPressable(scaleOnPress = 0.94f) { onSelectMood(mood) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = mood,
                        tint = animatedContentColor,
                        modifier = Modifier.size(15.dp)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = mood,
                        color = animatedContentColor,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}


/**
 * Sculptural Album Card with Floating Play FAB
 */
@Composable
private fun SculpturalAlbumCard(
    playlist: Playlist,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    isDarkMode: Boolean
) {
    val activeTheme = LocalAppTheme.current

    Column(
        modifier = Modifier
            .width(154.dp)
            .echoPressable(scaleOnPress = 0.96f, onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(154.dp)
                .shadow(
                    elevation = if (isDarkMode) 4.dp else 8.dp,
                    shape = RoundedCornerShape(18.dp),
                    ambientColor = Color.Black.copy(alpha = 0.15f)
                )
                .clip(RoundedCornerShape(18.dp))
                .background(if (isDarkMode) Color(0xFF1E1D2A) else Color(0xFFEBE7E1))
        ) {
            AsyncImage(
                model = playlist.coverArtUrl,
                contentDescription = playlist.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Floating Play FAB at bottom right of album art
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .size(38.dp)
                    .shadow(elevation = 6.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .background(activeTheme.primaryColor)
                    .echoPressable(scaleOnPress = 0.90f, onClick = onPlay),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Play ${playlist.name}",
                    tint = Color(0xFF121212),
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = playlist.name,
            color = if (isDarkMode) Color(0xFFF6F3FA) else Color(0xFF17171C),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = playlist.description.ifBlank { playlist.ownerName },
            color = if (isDarkMode) Color(0xFFA6A2B3) else Color(0xFF777680),
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Made For You Recommendation Card
 */
@Composable
private fun MadeForYouCard(
    playlist: Playlist,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    isDarkMode: Boolean
) {
    val activeTheme = LocalAppTheme.current
    val cardBg = if (isDarkMode) Color(0xFF1E1D28) else Color(0xFFFFFFFF)
    val cardBorder = if (isDarkMode) Color(0xFF2E2C3D) else Color(0xFFE5E0E8)

    Row(
        modifier = Modifier
            .width(260.dp)
            .height(84.dp)
            .shadow(
                elevation = if (isDarkMode) 2.dp else 6.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = Color.Black.copy(alpha = 0.08f)
            )
            .clip(RoundedCornerShape(18.dp))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(18.dp))
            .echoPressable(scaleOnPress = 0.96f, onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = playlist.coverArtUrl,
            contentDescription = playlist.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (isDarkMode) Color(0xFF222030) else Color(0xFFECE7E1))
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.name,
                color = if (isDarkMode) Color(0xFFF6F3FA) else Color(0xFF17171C),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${playlist.tracks.size.coerceAtLeast(12)} tracks",
                color = if (isDarkMode) Color(0xFFA6A2B3) else Color(0xFF777680),
                fontSize = 11.5.sp
            )
        }

        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(activeTheme.primaryColor)
                .echoPressable(scaleOnPress = 0.90f, onClick = onPlay),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = "Play",
                tint = Color(0xFF121212),
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

/**
 * Best New Songs Carousel with Playback Indicator & More Options
 */
@Composable
private fun BestNewSongsCarousel(
    tracks: List<Track>,
    currentTrackId: String?,
    isPlaying: Boolean,
    onPlayTrack: (Track) -> Unit,
    onMoreClick: (Track) -> Unit,
    isDarkMode: Boolean
) {
    val activeTheme = LocalAppTheme.current
    val cardBg = if (isDarkMode) Color(0xFF1E1D28) else Color(0xFFFFFFFF)
    val cardBorder = if (isDarkMode) Color(0xFF2E2C3D) else Color(0xFFE5E0E8)
    val chunked = tracks.take(12).chunked(2)

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.padding(top = 10.dp, bottom = 24.dp)
    ) {
        items(
            items = chunked,
            key = { pair -> pair.joinToString("_") { it.id } }
        ) { pair ->
            Column(
                modifier = Modifier.width(290.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                pair.forEach { track ->
                    val isTrackPlaying = track.id == currentTrackId && isPlaying

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(68.dp)
                            .shadow(
                                elevation = if (isDarkMode) 2.dp else 5.dp,
                                shape = RoundedCornerShape(16.dp),
                                ambientColor = Color.Black.copy(alpha = 0.05f)
                            )
                            .clip(RoundedCornerShape(16.dp))
                            .background(cardBg)
                            .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
                            .echoPressable(scaleOnPress = 0.98f) { onPlayTrack(track) }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = track.coverArtUrl,
                            contentDescription = track.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(if (isDarkMode) Color(0xFF222030) else Color(0xFFECE7E1))
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.title,
                                color = if (isTrackPlaying) activeTheme.primaryColor else if (isDarkMode) Color(0xFFF6F3FA) else Color(0xFF17171C),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(activeTheme.primaryColor.copy(alpha = 0.15f))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "320k",
                                        color = activeTheme.primaryColor,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = track.artistName,
                                    color = if (isDarkMode) Color(0xFFA6A2B3) else Color(0xFF777680),
                                    fontSize = 11.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (isTrackPlaying) {
                            Icon(
                                imageVector = Icons.Filled.GraphicEq,
                                contentDescription = "Playing",
                                tint = activeTheme.primaryColor,
                                modifier = Modifier
                                    .size(28.dp)
                                    .padding(4.dp)
                            )
                        } else {
                            IconButton(onClick = { onPlayTrack(track) }, modifier = Modifier.size(34.dp)) {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = "Play",
                                    tint = activeTheme.primaryColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        IconButton(onClick = { onMoreClick(track) }, modifier = Modifier.size(30.dp)) {
                            Icon(
                                imageVector = Icons.Outlined.MoreVert,
                                contentDescription = "More",
                                tint = if (isDarkMode) Color(0xFFA6A2B3) else Color(0xFF777680),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Echo Artist Radio Card
 */
@Composable
private fun EchoArtistCard(
    artist: Artist,
    onClick: () -> Unit,
    isDarkMode: Boolean
) {
    val activeTheme = LocalAppTheme.current

    Column(
        modifier = Modifier
            .width(115.dp)
            .echoPressable(scaleOnPress = 0.94f, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(110.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            AsyncImage(
                model = artist.avatarUrl,
                contentDescription = artist.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(if (isDarkMode) Color(0xFF222030) else Color(0xFFECE7E1))
            )
            Box(
                modifier = Modifier
                    .padding(bottom = 2.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(activeTheme.primaryColor)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "RADIO",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = artist.name,
            color = if (isDarkMode) Color(0xFFF6F3FA) else Color(0xFF17171C),
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "Artist Radio",
            color = if (isDarkMode) Color(0xFFA6A2B3) else Color(0xFF777680),
            fontSize = 11.sp
        )
    }
}

/**
 * Section Header with title and optional action icon
 */
@Composable
private fun SectionHeaderWithAction(
    title: String,
    subtitle: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    val isDarkMode by ThemeManager.isDarkMode.collectAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (isDarkMode) Color(0xFFF6F3FA) else Color(0xFF17171C),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                letterSpacing = (-0.3).sp
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = if (isDarkMode) Color(0xFFA6A2B3) else Color(0xFF777680),
                    fontSize = 12.sp
                )
            }
        }

        if (onActionClick != null) {
            IconButton(onClick = onActionClick, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Adjust,
                    contentDescription = "Explore",
                    tint = if (isDarkMode) Color(0xFFA6A2B3) else Color(0xFF777680),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}


/**
 * Curated Podcast Show Card for Home Screen
 */
@Composable
private fun EchoPodcastCard(
    podcast: Playlist,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    isDarkMode: Boolean
) {
    val activeTheme = LocalAppTheme.current

    Column(
        modifier = Modifier
            .width(154.dp)
            .echoPressable(scaleOnPress = 0.96f, onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(154.dp)
                .shadow(
                    elevation = if (isDarkMode) 4.dp else 8.dp,
                    shape = RoundedCornerShape(18.dp),
                    ambientColor = Color.Black.copy(alpha = 0.15f)
                )
                .clip(RoundedCornerShape(18.dp))
                .background(if (isDarkMode) Color(0xFF1E1D2A) else Color(0xFFEBE7E1))
        ) {
            AsyncImage(
                model = podcast.coverArtUrl,
                contentDescription = podcast.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Category/Podcast badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.GraphicEq,
                        contentDescription = null,
                        tint = activeTheme.primaryColor,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "SHOW",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Quick Play Floating Circle Button
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp)
                    .size(38.dp)
                    .shadow(6.dp, CircleShape)
                    .clip(CircleShape)
                    .background(activeTheme.primaryColor)
                    .clickable { onPlay() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Play Show",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = podcast.name,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = if (isDarkMode) Color(0xFFF5F2F9) else Color(0xFF15141D),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = podcast.ownerName,
            fontSize = 12.sp,
            color = if (isDarkMode) Color(0xFFA5A1B2) else Color(0xFF6F6D79),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Podcast Episode Row for Home Screen
 */
@Composable
private fun PodcastEpisodeRow(
    episode: Track,
    isPlaying: Boolean,
    onClick: () -> Unit,
    isDarkMode: Boolean
) {
    val activeTheme = LocalAppTheme.current
    val rowBg = if (isDarkMode) Color(0xFF1C1B26) else Color(0xFFFFFFFF)
    val rowBorder = if (isDarkMode) Color(0xFF2C2B3A) else Color(0xFFE5E0E8)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(rowBg)
            .border(1.dp, rowBorder, RoundedCornerShape(14.dp))
            .echoPressable(scaleOnPress = 0.98f, onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = episode.coverArtUrl,
            contentDescription = episode.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = episode.title,
                color = if (isPlaying) activeTheme.primaryColor else MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "${episode.artistName} • ${(episode.durationMs / 60000)} min",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (isPlaying) activeTheme.primaryColor else activeTheme.primaryColor.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = "Play Episode",
                tint = if (isPlaying) Color.White else activeTheme.primaryColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
