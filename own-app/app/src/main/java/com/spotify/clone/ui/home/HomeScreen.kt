package com.spotify.clone.ui.home

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.spotify.clone.core.designsystem.AppThemeOption
import com.spotify.clone.core.designsystem.EchoFilterChip
import com.spotify.clone.core.designsystem.EchoInsetCard
import com.spotify.clone.core.designsystem.EchoMarqueeText
import com.spotify.clone.core.designsystem.EchoMotion
import com.spotify.clone.core.designsystem.EchoPrismAqua
import com.spotify.clone.core.designsystem.EchoPrismLavender
import com.spotify.clone.core.designsystem.EchoPrismRose
import com.spotify.clone.core.designsystem.EchoPrismSky
import com.spotify.clone.core.designsystem.EchoSectionHeader
import com.spotify.clone.core.designsystem.LiveAudioSpectrum
import com.spotify.clone.core.designsystem.LivingEqualizer
import com.spotify.clone.core.designsystem.LocalAppTheme
import com.spotify.clone.core.designsystem.LocalReducedMotion
import com.spotify.clone.core.designsystem.SpotifyJellyfishAura
import com.spotify.clone.core.designsystem.SwipeableTrackRow
import com.spotify.clone.core.designsystem.ThemeManager
import com.spotify.clone.core.designsystem.echoContrastOn
import com.spotify.clone.core.designsystem.echoPressable
import com.spotify.clone.core.designsystem.echoPrismBrush
import com.spotify.clone.core.designsystem.echoSpecularBorder
import com.spotify.clone.core.designsystem.echoStaggeredEntrance
import com.spotify.clone.core.designsystem.rememberAnimatedArtworkColors
import com.spotify.clone.core.designsystem.resolvedBackground
import com.spotify.clone.core.designsystem.resolvedBorder
import com.spotify.clone.core.designsystem.resolvedCard
import com.spotify.clone.core.designsystem.tabularFigures
import com.spotify.clone.core.model.Artist
import com.spotify.clone.core.model.EditorialBanner
import com.spotify.clone.core.model.MusicGenre
import com.spotify.clone.core.model.Playlist
import com.spotify.clone.core.model.Track
import com.spotify.clone.core.player.MusicController
import com.spotify.clone.core.profile.PrismAvatarOption
import com.spotify.clone.core.profile.UserProfileManager
import com.spotify.clone.ui.components.TrackOptionsBottomSheet
import com.spotify.clone.ui.splash.EchoAcousticLogoMark
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

private enum class HomeFilter(val label: String) {
    ALL("All"),
    FOR_YOU("For You"),
    NEW_SONGS("New Songs"),
    LIKED("Liked Vibes"),
    GENRES("Genres"),
    MIXES("Daily Mixes"),
    CHARTS("Top Charts"),
    ARTISTS("Artists")
}

/**
 * Redesigned Daily Auto-Updating Home ("Listen Now") Screen:
 * - Automatic calendar-day rollover & foreground refresh via DailyFeedCoordinator
 * - On-device AI/ML personalization (12D audio embeddings, LinUCB bandit, Co-Occurrence, K-Means, MMR)
 * - Automatic Studio HD song upgrading (320kbps resolution + HD cover + genre tags)
 * - Dedicated daily shelves: Spotlight, New Songs Today, AI Picked For You, Inspired by Your Likes,
 *   Your Genre Worlds, Daily Mixes, Studio Upgrades, Top Charts, and Artists For You.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onPlaylistClick: (String) -> Unit,
    onArtistClick: (String) -> Unit = {},
    onSettingsClick: () -> Unit = {},
    scrollToTopTrigger: Long = 0L,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activeTheme = LocalAppTheme.current
    val isDarkMode by ThemeManager.isDarkMode.collectAsState()
    val isReducedMotion = LocalReducedMotion.current
    val musicController = remember { MusicController.getInstance(context) }
    val state by viewModel.uiState.collectAsState()
    val playbackState by musicController.playbackState.collectAsState()
    val userName by UserProfileManager.userName.collectAsState()
    val avatarId by UserProfileManager.avatarId.collectAsState()
    val favoriteArtists by UserProfileManager.favoriteArtists.collectAsState()
    val activeAvatar = PrismAvatarOption.fromId(avatarId)

    var selectedTrackForOptions by remember { mutableStateOf<Track?>(null) }
    var selectedFilter by remember { mutableStateOf(HomeFilter.ALL) }
    val listState = rememberLazyListState()
    val heroListState = rememberLazyListState()

    // Automatically check daily rollover whenever user returns to the app
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.dispatch(HomeEvent.CheckDailyRollover)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(scrollToTopTrigger) {
        if (scrollToTopTrigger > 0L) {
            listState.animateScrollToItem(0)
        }
    }

    val artworkColors = rememberAnimatedArtworkColors(playbackState.currentTrack)
    val screenBg = activeTheme.resolvedBackground(isDarkMode)
    val cardBg = activeTheme.resolvedCard(isDarkMode)
    val borderColor = activeTheme.resolvedBorder(isDarkMode)
    val textPrimary = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant

    val showStickyHeader by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 125
        }
    }
    val stickyHeaderBg by animateColorAsState(
        targetValue = if (showStickyHeader) screenBg else Color.Transparent,
        animationSpec = tween(EchoMotion.DurationMicro, easing = EchoMotion.EncoreButtonEase),
        label = "home_sticky_bg"
    )

    val screenMode = when {
        state.isLoading -> 0
        state.errorMessage != null && state.featuredPlaylists.isEmpty() -> 1
        else -> 2
    }

    AnimatedContent(
        targetState = screenMode,
        transitionSpec = {
            fadeIn(tween(240, easing = EchoMotion.EaseOutStrong)) togetherWith fadeOut(tween(160))
        },
        modifier = modifier.fillMaxSize(),
        label = "home_state_transition"
    ) { mode ->
        when (mode) {
            0 -> HomeScreenSkeleton(isDarkMode = isDarkMode)
            1 -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(screenBg)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = state.errorMessage ?: "Something went wrong",
                        color = textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.dispatch(HomeEvent.Refresh) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = activeTheme.primaryColor,
                            contentColor = echoContrastOn(activeTheme.primaryColor)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Try Again", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            else -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(screenBg)
            ) {
                if (activeTheme == AppThemeOption.PASTEL_PRISM && playbackState.currentTrack == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        EchoPrismRose.copy(alpha = if (isDarkMode) 0.14f else 0.16f),
                                        EchoPrismLavender.copy(alpha = if (isDarkMode) 0.08f else 0.10f),
                                        EchoPrismAqua.copy(alpha = if (isDarkMode) 0.04f else 0.06f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }
                SpotifyJellyfishAura(
                    artworkColors = artworkColors,
                    isPlaying = playbackState.isPlaying,
                    isDarkMode = isDarkMode,
                    intensity = if (playbackState.currentTrack != null) 0.95f else 0.55f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp)
                )

                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = { viewModel.dispatch(HomeEvent.Refresh) },
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding(),
                        contentPadding = PaddingValues(bottom = 188.dp)
                    ) {
                        // 1. Editorial Header
                        item(key = "home_header") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .shadow(4.dp, RoundedCornerShape(6.dp), ambientColor = EchoPrismLavender.copy(alpha = 0.25f))
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color.White)
                                                .border(0.6.dp, borderColor, RoundedCornerShape(6.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            EchoAcousticLogoMark(modifier = Modifier.size(14.dp))
                                        }
                                        Spacer(modifier = Modifier.width(7.dp))
                                        Text(
                                            text = timeGreeting(userName).uppercase(),
                                            color = activeTheme.primaryColor,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.9.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "Listen Now",
                                        color = textPrimary,
                                        style = MaterialTheme.typography.displayLarge
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(Brush.linearGradient(listOf(activeAvatar.startColor, activeAvatar.endColor)))
                                            .border(1.2.dp, Color.White.copy(alpha = 0.7f), CircleShape)
                                            .echoPressable(scaleOnPress = 0.90f, hapticOnClick = true, onClick = onSettingsClick),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = userName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "E",
                                            color = Color.White,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(cardBg.copy(alpha = 0.94f))
                                            .echoSpecularBorder(isDarkMode, CircleShape, borderColor)
                                            .echoPressable(scaleOnPress = 0.90f, hapticOnClick = true, onClick = onSettingsClick),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Settings,
                                            contentDescription = "Settings",
                                            tint = textPrimary,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // 2. Interactive Filter Strip
                        item(key = "home_filter_strip") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 20.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                HomeFilter.entries.forEach { filter ->
                                    EchoFilterChip(
                                        text = filter.label,
                                        isSelected = selectedFilter == filter,
                                        onClick = { selectedFilter = filter }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        // 2b. Live Listening Pulse Card
                        val activeTrack = playbackState.currentTrack
                        if (activeTrack != null && selectedFilter == HomeFilter.ALL) {
                            item(key = "live_listening_pulse") {
                                val pulseShape = RoundedCornerShape(16.dp)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp)
                                        .clip(pulseShape)
                                        .background(cardBg)
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(
                                                    artworkColors.vibrantAccent.copy(alpha = if (isDarkMode) 0.18f else 0.09f),
                                                    Color.Transparent
                                                )
                                            )
                                        )
                                        .echoSpecularBorder(isDarkMode, pulseShape, borderColor)
                                        .echoPressable(scaleOnPress = 0.985f, hapticOnClick = true) {
                                            musicController.playSameGenreRadio(activeTrack)
                                            Toast.makeText(context, "Radio tuned to ${MusicGenre.inferGenre(activeTrack).shortLabel}", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = activeTrack.coverArtUrl,
                                        contentDescription = activeTrack.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            LiveAudioSpectrum(
                                                isPlaying = playbackState.isPlaying,
                                                barCount = 4,
                                                genre = MusicGenre.inferGenre(activeTrack),
                                                barColor = artworkColors.vibrantAccent,
                                                barWidth = 2.dp,
                                                barSpacing = 1.5.dp,
                                                height = 10.dp
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (playbackState.isPlaying) "LIVE SESSION • TAP FOR RADIO" else "PAUSED • TAP FOR RADIO",
                                                color = artworkColors.vibrantAccent,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.7.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        EchoMarqueeText(
                                            text = "${activeTrack.title} — ${activeTrack.artistName}",
                                            color = textPrimary,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            enabled = playbackState.isPlaying
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(activeTheme.primaryColor)
                                            .echoPressable(scaleOnPress = 0.88f, hapticOnClick = true) {
                                                musicController.togglePlayPause()
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (playbackState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                            contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                                            tint = echoContrastOn(activeTheme.primaryColor),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(18.dp))
                            }
                        }

                        // 3. Jump Back In — 2-Column Bento Grid (Liked Songs + Discover Daily + Mixes)
                        if ((selectedFilter == HomeFilter.ALL || selectedFilter == HomeFilter.MIXES) && state.quickPicks.isNotEmpty()) {
                            item(key = "quick_picks_section") {
                                Column(modifier = Modifier.echoStaggeredEntrance(0)) {
                                    EchoSectionHeader(title = "Jump Back In")
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Column(
                                        modifier = Modifier.padding(horizontal = 20.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        state.quickPicks.take(6).chunked(2).forEach { rowItems ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                rowItems.forEach { playlist ->
                                                    val isPlaylistActive = playbackState.isPlaying &&
                                                        playlist.tracks.any { it.id == playbackState.currentTrack?.id }
                                                    QuickPickBentoTile(
                                                        playlist = playlist,
                                                        isPlaying = isPlaylistActive,
                                                        onClick = { onPlaylistClick(playlist.id) },
                                                        isDarkMode = isDarkMode,
                                                        cardBg = cardBg,
                                                        borderColor = borderColor,
                                                        accentColor = activeTheme.primaryColor,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                                if (rowItems.size == 1) {
                                                    Spacer(modifier = Modifier.weight(1f))
                                                }
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(24.dp))
                                }
                            }
                        }

                        // 4. Today's Spotlight — Snap-Scrolling AI & Editorial Showcase
                        if (selectedFilter == HomeFilter.ALL && state.editorialBanners.isNotEmpty()) {
                            item(key = "top_picks_section") {
                                Column(modifier = Modifier.echoStaggeredEntrance(1)) {
                                    EchoSectionHeader(title = "Today's Spotlight")
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LazyRow(
                                        state = heroListState,
                                        flingBehavior = rememberSnapFlingBehavior(lazyListState = heroListState),
                                        contentPadding = PaddingValues(horizontal = 20.dp),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        itemsIndexed(state.editorialBanners, key = { _, b -> b.id }) { index, banner ->
                                            EditorialPosterCard(
                                                banner = banner,
                                                index = index,
                                                listState = heroListState,
                                                isReducedMotion = isReducedMotion,
                                                isDarkMode = isDarkMode,
                                                borderColor = borderColor,
                                                accentColor = activeTheme.primaryColor,
                                                onClick = {
                                                    val targetId = banner.targetPlaylistId
                                                    if (!targetId.isNullOrBlank()) {
                                                        onPlaylistClick(targetId)
                                                    } else {
                                                        viewModel.dispatch(HomeEvent.PlayHero(banner))
                                                    }
                                                },
                                                onPlay = { viewModel.dispatch(HomeEvent.PlayHero(banner)) }
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(26.dp))
                                }
                            }
                        }

                        // 5. New Songs Today (Auto-Updated Daily in background)
                        if ((selectedFilter == HomeFilter.ALL || selectedFilter == HomeFilter.NEW_SONGS) && state.newSongsToday.isNotEmpty()) {
                            item(key = "new_songs_today_section") {
                                Column(modifier = Modifier.echoStaggeredEntrance(2)) {
                                    EchoSectionHeader(
                                        title = "New Songs Today",
                                        actionLabel = "Play All",
                                        onActionClick = {
                                            val first = state.newSongsToday.firstOrNull() ?: return@EchoSectionHeader
                                            viewModel.dispatch(HomeEvent.PlayTrack(first, state.newSongsToday))
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 20.dp),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        items(state.newSongsToday, key = { "new_${it.id}" }) { track ->
                                            val isCurrent = playbackState.currentTrack?.id == track.id
                                            DiscoveryTrackCard(
                                                track = track,
                                                topBadgeText = "",
                                                subtitleText = "${track.artistName} • ${MusicGenre.inferGenre(track).shortLabel}",
                                                reasonText = null,
                                                isCurrent = isCurrent,
                                                isPlaying = isCurrent && playbackState.isPlaying,
                                                isDarkMode = isDarkMode,
                                                cardBg = cardBg,
                                                borderColor = borderColor,
                                                accentColor = activeTheme.primaryColor,
                                                onClick = {
                                                    viewModel.dispatch(HomeEvent.PlayTrack(track, state.newSongsToday))
                                                },
                                                onStartRadio = {
                                                    viewModel.dispatch(HomeEvent.PlaySameGenreRadio(track))
                                                }
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(26.dp))
                                }
                            }
                        }

                        // 6. AI Picked For You (LinUCB + 12D Embeddings + MMR with Explainable Tags)
                        if ((selectedFilter == HomeFilter.ALL || selectedFilter == HomeFilter.FOR_YOU) && state.recommendedItems.isNotEmpty()) {
                            item(key = "recommended_for_you_section") {
                                val pickedTitle = if (userName.isNotBlank() && userName != "Listener") {
                                    "Picked For $userName"
                                } else {
                                    "Picked For You"
                                }
                                Column(modifier = Modifier.echoStaggeredEntrance(2)) {
                                    EchoSectionHeader(
                                        title = pickedTitle,
                                        actionLabel = "Play Mix",
                                        onActionClick = {
                                            val first = state.recommendedTracks.firstOrNull() ?: return@EchoSectionHeader
                                            viewModel.dispatch(HomeEvent.PlayTrack(first, state.recommendedTracks))
                                        }
                                    )
                                    if (state.recommendedSubtitle.isNotBlank()) {
                                        Text(
                                            text = state.recommendedSubtitle,
                                            color = textSecondary,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 20.dp)
                                        )
                                    }
                                    if (favoriteArtists.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        LazyRow(
                                            contentPadding = PaddingValues(horizontal = 20.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            items(favoriteArtists.toList(), key = { "fav_art_$it" }) { artistName ->
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(50))
                                                        .background(cardBg)
                                                        .border(0.85.dp, echoPrismBrush(), RoundedCornerShape(50))
                                                        .echoPressable(scaleOnPress = 0.94f, hapticOnClick = true) {
                                                            onArtistClick(artistName)
                                                        }
                                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                                ) {
                                                    Text(
                                                        text = artistName,
                                                        color = textPrimary,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 20.dp),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        items(state.recommendedItems, key = { "rec_${it.track.id}" }) { pred ->
                                            val track = pred.track
                                            val isCurrent = playbackState.currentTrack?.id == track.id
                                            DiscoveryTrackCard(
                                                track = track,
                                                topBadgeText = "",
                                                subtitleText = track.artistName,
                                                reasonText = pred.reasonTag,
                                                isCurrent = isCurrent,
                                                isPlaying = isCurrent && playbackState.isPlaying,
                                                isDarkMode = isDarkMode,
                                                cardBg = cardBg,
                                                borderColor = borderColor,
                                                accentColor = activeTheme.primaryColor,
                                                onClick = {
                                                    viewModel.dispatch(HomeEvent.PlayTrack(track, state.recommendedTracks))
                                                },
                                                onStartRadio = {
                                                    viewModel.dispatch(HomeEvent.PlaySameGenreRadio(track))
                                                }
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(26.dp))
                                }
                            }
                        }

                        // 7. Inspired By Your Likes (Co-Occurrence Graph + Cosine Neighborhood)
                        if ((selectedFilter == HomeFilter.ALL || selectedFilter == HomeFilter.LIKED) &&
                            (state.likedInspiredTracks.isNotEmpty() || state.favoriteTracks.isNotEmpty())
                        ) {
                            item(key = "liked_inspired_section") {
                                val tracksToShow = state.likedInspiredTracks.ifEmpty { state.favoriteTracks }
                                Column(modifier = Modifier.echoStaggeredEntrance(3)) {
                                    EchoSectionHeader(
                                        title = "From Your Likes",
                                        actionLabel = "Play Mix",
                                        onActionClick = {
                                            val first = tracksToShow.firstOrNull() ?: return@EchoSectionHeader
                                            viewModel.dispatch(HomeEvent.PlayTrack(first, tracksToShow))
                                        }
                                    )
                                    if (state.likedAnchorSubtitle.isNotBlank()) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 20.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Favorite,
                                                contentDescription = null,
                                                tint = EchoPrismRose,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = state.likedAnchorSubtitle,
                                                color = textSecondary,
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 20.dp),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        items(tracksToShow, key = { "liked_rec_${it.id}" }) { track ->
                                            val isCurrent = playbackState.currentTrack?.id == track.id
                                            DiscoveryTrackCard(
                                                track = track,
                                                topBadgeText = if (track.isFavorite) "LIKED" else "LIKE MATCH",
                                                subtitleText = "${track.artistName} • ${MusicGenre.inferGenre(track).shortLabel}",
                                                reasonText = null,
                                                isCurrent = isCurrent,
                                                isPlaying = isCurrent && playbackState.isPlaying,
                                                isDarkMode = isDarkMode,
                                                cardBg = cardBg,
                                                borderColor = borderColor,
                                                accentColor = EchoPrismRose,
                                                onClick = {
                                                    viewModel.dispatch(HomeEvent.PlayTrack(track, tracksToShow))
                                                },
                                                onStartRadio = {
                                                    viewModel.dispatch(HomeEvent.PlaySameGenreRadio(track))
                                                }
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(26.dp))
                                }
                            }
                        }

                        // 8. Your Genre Worlds & Same-Genre Radio
                        if (selectedFilter == HomeFilter.ALL || selectedFilter == HomeFilter.GENRES || selectedFilter == HomeFilter.FOR_YOU) {
                            if (state.genreHubs.isNotEmpty()) {
                                item(key = "genre_worlds_section") {
                                    Column(modifier = Modifier.echoStaggeredEntrance(3)) {
                                        EchoSectionHeader(title = "Your Genre Worlds")
                                        Spacer(modifier = Modifier.height(8.dp))
                                        LazyRow(
                                            contentPadding = PaddingValues(horizontal = 20.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            itemsIndexed(state.genreHubs, key = { _, p -> "gh_${p.id}" }) { idx, hub ->
                                                GenreWorldCard(
                                                    playlist = hub,
                                                    paletteIndex = idx,
                                                    isDarkMode = isDarkMode,
                                                    borderColor = borderColor,
                                                    onClick = { onPlaylistClick(hub.id) },
                                                    onPlay = { viewModel.dispatch(HomeEvent.PlayPlaylist(hub)) }
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(16.dp))
                                    }
                                }
                            }

                            item(key = "genre_radio_stations_section") {
                                val activeLock by musicController.activeGenreLock.collectAsState()
                                Column {
                                    EchoSectionHeader(title = "Same-Genre Radio")
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 20.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        items(MusicGenre.selectableGenres, key = { "genre_station_${it.id}" }) { genre ->
                                            val isSelected = activeLock == genre
                                            val stationShape = RoundedCornerShape(14.dp)
                                            Row(
                                                modifier = Modifier
                                                    .clip(stationShape)
                                                    .background(
                                                        if (isSelected) activeTheme.primaryColor
                                                        else cardBg
                                                    )
                                                    .echoSpecularBorder(
                                                        isDarkMode = isDarkMode,
                                                        shape = stationShape,
                                                        fallbackBorder = if (isSelected) activeTheme.primaryColor else borderColor
                                                    )
                                                    .echoPressable(scaleOnPress = 0.95f, hapticOnClick = true) {
                                                        viewModel.dispatch(HomeEvent.PlayGenreRadio(genre))
                                                    }
                                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Radio,
                                                    contentDescription = null,
                                                    tint = if (isSelected) echoContrastOn(activeTheme.primaryColor) else activeTheme.primaryColor,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(7.dp))
                                                Text(
                                                    text = genre.shortLabel,
                                                    color = if (isSelected) echoContrastOn(activeTheme.primaryColor) else textPrimary,
                                                    fontSize = 13.5.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(26.dp))
                                }
                            }
                        }

                        // 9. Made For You — K-Means Clustered Daily Mixes & Discover Daily
                        if ((selectedFilter == HomeFilter.ALL || selectedFilter == HomeFilter.MIXES || selectedFilter == HomeFilter.FOR_YOU) && state.featuredPlaylists.isNotEmpty()) {
                            item(key = "curated_mixes_section") {
                                Column(modifier = Modifier.echoStaggeredEntrance(4)) {
                                    EchoSectionHeader(title = "Made For You")
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 20.dp),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        items(state.featuredPlaylists, key = { "mix_${it.id}" }) { playlist ->
                                            CuratedMixCard(
                                                playlist = playlist,
                                                isDarkMode = isDarkMode,
                                                cardBg = cardBg,
                                                borderColor = borderColor,
                                                onClick = { onPlaylistClick(playlist.id) }
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(26.dp))
                                }
                            }
                        }

                        // 10. Recently Played (songs upgrade silently in the background)
                        if (selectedFilter == HomeFilter.ALL && state.recentlyPlayed.isNotEmpty()) {
                            item(key = "recently_played_section") {
                                Column(modifier = Modifier.echoStaggeredEntrance(2)) {
                                    EchoSectionHeader(title = "Recently Played")
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 20.dp),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        items(state.recentlyPlayed.take(12), key = { "recent_${it.id}" }) { track ->
                                            val isCurrent = playbackState.currentTrack?.id == track.id
                                            TrackRecentCard(
                                                track = track,
                                                isCurrent = isCurrent,
                                                isPlaying = isCurrent && playbackState.isPlaying,
                                                isDarkMode = isDarkMode,
                                                borderColor = borderColor,
                                                onClick = {
                                                    viewModel.dispatch(HomeEvent.PlayTrack(track, state.recentlyPlayed))
                                                }
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(26.dp))
                                }
                            }
                        }

                        // 12. Top Charts — Ranked Apple-Style Inset Card
                        if ((selectedFilter == HomeFilter.ALL || selectedFilter == HomeFilter.CHARTS) && state.trendingTracks.isNotEmpty()) {
                            item(key = "top_charts_section") {
                                val chartTracks = if (selectedFilter == HomeFilter.CHARTS) {
                                    state.trendingTracks
                                } else {
                                    state.trendingTracks.take(6)
                                }
                                Column(modifier = Modifier.echoStaggeredEntrance(3)) {
                                    EchoSectionHeader(
                                        title = "Top Charts",
                                        actionLabel = "Play All",
                                        onActionClick = {
                                            val first = chartTracks.firstOrNull() ?: return@EchoSectionHeader
                                            viewModel.dispatch(HomeEvent.PlayTrack(first, state.trendingTracks))
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    EchoInsetCard(modifier = Modifier.padding(horizontal = 20.dp)) {
                                        chartTracks.forEachIndexed { idx, track ->
                                            val isCurrent = playbackState.currentTrack?.id == track.id
                                            val isPlaying = isCurrent && playbackState.isPlaying
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
                                                RankedChartRow(
                                                    rank = idx + 1,
                                                    track = track,
                                                    isCurrent = isCurrent,
                                                    isPlaying = isPlaying,
                                                    accentColor = activeTheme.primaryColor,
                                                    onClick = {
                                                        viewModel.dispatch(HomeEvent.PlayTrack(track, state.trendingTracks))
                                                    },
                                                    onMoreClick = { selectedTrackForOptions = track }
                                                )
                                            }
                                            if (idx < chartTracks.lastIndex) {
                                                HorizontalDivider(
                                                    color = borderColor,
                                                    thickness = 0.6.dp,
                                                    modifier = Modifier.padding(start = 94.dp, end = 14.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(26.dp))
                                }
                            }
                        }

                        // 13. New Album Releases
                        if ((selectedFilter == HomeFilter.ALL || selectedFilter == HomeFilter.NEW_SONGS) && state.newAlbums.isNotEmpty()) {
                            item(key = "new_releases_section") {
                                Column(modifier = Modifier.echoStaggeredEntrance(5)) {
                                    EchoSectionHeader(title = "New Albums")
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 20.dp),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        items(state.newAlbums.take(10), key = { "album_${it.id}" }) { album ->
                                            AlbumReleaseCard(
                                                playlist = album,
                                                isDarkMode = isDarkMode,
                                                borderColor = borderColor,
                                                onClick = { onPlaylistClick(album.id) }
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(26.dp))
                                }
                            }
                        }

                        // 14. Artists For You
                        if ((selectedFilter == HomeFilter.ALL || selectedFilter == HomeFilter.ARTISTS) && state.artists.isNotEmpty()) {
                            item(key = "artists_section") {
                                EchoSectionHeader(title = "Artists For You")
                                Spacer(modifier = Modifier.height(10.dp))
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 20.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    items(state.artists, key = { "artist_${it.id}" }) { artist ->
                                        ArtistPortraitCard(
                                            artist = artist,
                                            isDarkMode = isDarkMode,
                                            borderColor = borderColor,
                                            onClick = { onArtistClick(artist.name) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Solid Collapsing Sticky Header on scroll
                if (showStickyHeader) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(stickyHeaderBg)
                            .statusBarsPadding()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Listen Now",
                                        color = textPrimary,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = (-0.3).sp
                                    )
                                    if (playbackState.isPlaying) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        LiveAudioSpectrum(
                                            isPlaying = true,
                                            barCount = 3,
                                            barColor = activeTheme.primaryColor,
                                            barWidth = 2.dp,
                                            barSpacing = 1.5.dp,
                                            height = 12.dp
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(cardBg)
                                        .echoSpecularBorder(isDarkMode, CircleShape, borderColor)
                                        .echoPressable(scaleOnPress = 0.90f, hapticOnClick = true, onClick = onSettingsClick),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Settings,
                                        contentDescription = "Settings",
                                        tint = textPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            HorizontalDivider(color = borderColor.copy(alpha = 0.8f), thickness = 0.6.dp)
                        }
                    }
                }
            }
        }
    }

    selectedTrackForOptions?.let { track ->
        TrackOptionsBottomSheet(
            track = track,
            musicController = musicController,
            onDismiss = { selectedTrackForOptions = null },
            onViewArtist = { artistName -> onArtistClick(artistName) }
        )
    }
}

@Composable
private fun GenreWorldCard(
    playlist: Playlist,
    paletteIndex: Int,
    isDarkMode: Boolean,
    borderColor: Color,
    onClick: () -> Unit,
    onPlay: () -> Unit
) {
    val gradients = listOf(
        listOf(EchoPrismRose, EchoPrismLavender),
        listOf(EchoPrismLavender, EchoPrismSky),
        listOf(EchoPrismSky, EchoPrismAqua),
        listOf(Color(0xFF7B6EF6), EchoPrismRose)
    )
    val colors = gradients[paletteIndex % gradients.size]
    val shape = RoundedCornerShape(18.dp)

    Box(
        modifier = Modifier
            .width(166.dp)
            .height(104.dp)
            .shadow(6.dp, shape, ambientColor = colors.first().copy(alpha = 0.22f))
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors.map { it.copy(alpha = if (isDarkMode) 0.34f else 0.24f) }
                )
            )
            .echoSpecularBorder(isDarkMode, shape, borderColor)
            .echoPressable(scaleOnPress = 0.96f, hapticOnClick = true, onClick = onClick)
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            Text(
                text = playlist.name,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 15.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${playlist.tracks.size} tracks",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            AsyncImage(
                model = playlist.coverArtUrl,
                contentDescription = playlist.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(colors.first())
                    .echoPressable(scaleOnPress = 0.88f, hapticOnClick = true, onClick = onPlay),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Play ${playlist.name}",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun DiscoveryTrackCard(
    track: Track,
    topBadgeText: String,
    subtitleText: String,
    reasonText: String?,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isDarkMode: Boolean,
    cardBg: Color,
    borderColor: Color,
    accentColor: Color,
    onClick: () -> Unit,
    onStartRadio: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .width(156.dp)
            .shadow(
                elevation = if (isCurrent) 10.dp else 6.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.12f),
                spotColor = if (isCurrent) accentColor.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.14f)
            )
            .clip(shape)
            .background(cardBg)
            .then(
                if (isCurrent) Modifier.border(1.5.dp, accentColor.copy(alpha = 0.8f), shape)
                else Modifier.echoSpecularBorder(isDarkMode, shape, borderColor)
            )
            .echoPressable(scaleOnPress = 0.96f, hapticOnClick = true, onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(142.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = track.coverArtUrl,
                contentDescription = track.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            if (topBadgeText.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.Black.copy(alpha = 0.62f))
                        .border(0.6.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(50))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = topBadgeText,
                        color = Color.White,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
            if (isCurrent) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.72f))
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    LiveAudioSpectrum(
                        isPlaying = isPlaying,
                        barCount = 4,
                        genre = MusicGenre.inferGenre(track),
                        barColor = accentColor,
                        barWidth = 2.dp,
                        barSpacing = 1.5.dp,
                        height = 11.dp
                    )
                }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 11.dp, vertical = 9.dp)
        ) {
            Text(
                text = track.title,
                color = if (isCurrent) accentColor else MaterialTheme.colorScheme.onSurface,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = subtitleText,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!reasonText.isNullOrBlank()) {
                        Text(
                            text = reasonText,
                            color = accentColor,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.14f))
                        .echoPressable(scaleOnPress = 0.85f, hapticOnClick = true, onClick = onStartRadio),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Radio,
                        contentDescription = "Start Same-Genre Radio",
                        tint = accentColor,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

private fun timeGreeting(userName: String): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val base = when (hour) {
        in 5..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        in 17..21 -> "Good Evening"
        else -> "Late Night"
    }
    return if (userName.isNotBlank() && !userName.equals("Listener", ignoreCase = true)) {
        "$base, $userName"
    } else {
        base
    }
}

@Composable
private fun EditorialPosterCard(
    banner: EditorialBanner,
    index: Int,
    listState: LazyListState,
    isReducedMotion: Boolean,
    isDarkMode: Boolean,
    borderColor: Color,
    accentColor: Color,
    onClick: () -> Unit,
    onPlay: () -> Unit
) {
    val focusScale by remember(index) {
        derivedStateOf {
            if (isReducedMotion) 1f
            else {
                val layoutInfo = listState.layoutInfo
                val itemInfo = layoutInfo.visibleItemsInfo.find { it.index == index } ?: return@derivedStateOf 0.96f
                val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
                val itemCenter = itemInfo.offset + itemInfo.size / 2f
                val dist = abs(itemCenter - viewportCenter)
                val maxDist = (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset).coerceAtLeast(1).toFloat()
                (1f - (dist / maxDist) * 0.08f).coerceIn(0.94f, 1f)
            }
        }
    }

    val cardShape = RoundedCornerShape(22.dp)
    Box(
        modifier = Modifier
            .width(304.dp)
            .height(234.dp)
            .graphicsLayer {
                scaleX = focusScale
                scaleY = focusScale
            }
            .shadow(
                elevation = 14.dp,
                shape = cardShape,
                ambientColor = Color.Black.copy(alpha = 0.18f),
                spotColor = accentColor.copy(alpha = 0.25f)
            )
            .clip(cardShape)
            .echoSpecularBorder(isDarkMode, cardShape, borderColor)
            .echoPressable(scaleOnPress = 0.97f, hapticOnClick = true, onClick = onClick)
    ) {
        AsyncImage(
            model = banner.coverArtUrl,
            contentDescription = banner.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Black.copy(alpha = 0.12f),
                        0.45f to Color.Transparent,
                        0.72f to Color.Black.copy(alpha = 0.62f),
                        1.0f to Color.Black.copy(alpha = 0.92f)
                    )
                )
        )

        if (banner.eyebrow.isNotBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(14.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.52f))
                    .border(0.75.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = banner.eyebrow,
                    color = Color.White,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = banner.title,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.4).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = banner.subtitle,
                    color = Color.White.copy(alpha = 0.78f),
                    fontSize = 12.5.sp,
                    lineHeight = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .shadow(8.dp, CircleShape, spotColor = accentColor.copy(alpha = 0.45f))
                    .clip(CircleShape)
                    .background(accentColor)
                    .border(0.75.dp, Color.White.copy(alpha = 0.28f), CircleShape)
                    .echoPressable(scaleOnPress = 0.88f, hapticOnClick = true, onClick = onPlay),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Play ${banner.title}",
                    tint = echoContrastOn(accentColor),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun QuickPickBentoTile(
    playlist: Playlist,
    isPlaying: Boolean,
    onClick: () -> Unit,
    isDarkMode: Boolean,
    cardBg: Color,
    borderColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .height(58.dp)
            .clip(shape)
            .background(cardBg)
            .echoSpecularBorder(isDarkMode, shape, borderColor)
            .echoPressable(scaleOnPress = 0.97f, hapticOnClick = true, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = playlist.coverArtUrl,
            contentDescription = playlist.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(58.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = playlist.name,
            color = if (isPlaying) accentColor else MaterialTheme.colorScheme.onSurface,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        )
        if (isPlaying) {
            LivingEqualizer(
                isPlaying = true,
                barCount = 3,
                barColor = accentColor,
                barWidth = 2.dp,
                barSpacing = 1.5.dp,
                maxHeight = 12.dp,
                modifier = Modifier.padding(end = 10.dp)
            )
        }
    }
}

@Composable
private fun TrackRecentCard(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isDarkMode: Boolean,
    borderColor: Color,
    onClick: () -> Unit
) {
    val accent = LocalAppTheme.current.primaryColor
    val artShape = RoundedCornerShape(16.dp)
    val activeBorder by animateColorAsState(
        targetValue = if (isCurrent) accent.copy(alpha = 0.8f) else borderColor,
        animationSpec = tween(EchoMotion.DurationMicro),
        label = "recent_border"
    )

    Column(
        modifier = Modifier
            .width(126.dp)
            .echoPressable(scaleOnPress = 0.96f, onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(126.dp)
                .shadow(
                    elevation = if (isCurrent) 10.dp else 6.dp,
                    shape = artShape,
                    ambientColor = Color.Black.copy(alpha = 0.12f),
                    spotColor = if (isCurrent) accent.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.14f)
                )
                .clip(artShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .then(
                    if (isCurrent) Modifier.border(1.5.dp, activeBorder, artShape)
                    else Modifier.echoSpecularBorder(isDarkMode, artShape, activeBorder)
                )
        ) {
            AsyncImage(
                model = track.coverArtUrl,
                contentDescription = track.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            if (isCurrent) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(7.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.72f))
                        .padding(horizontal = 5.dp, vertical = 4.dp)
                ) {
                    LivingEqualizer(
                        isPlaying = isPlaying,
                        barCount = 3,
                        barColor = accent,
                        barWidth = 2.dp,
                        barSpacing = 2.dp,
                        maxHeight = 11.dp,
                        minHeight = 3.dp
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(7.dp))
        Text(
            text = track.title,
            color = if (isCurrent) accent else MaterialTheme.colorScheme.onBackground,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = track.artistName,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun RankedChartRow(
    rank: Int,
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    val titleColor by animateColorAsState(
        targetValue = if (isCurrent) accentColor else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(EchoMotion.DurationMicro),
        label = "chart_title_color"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isCurrent) accentColor.copy(alpha = 0.06f) else Color.Transparent)
            .echoPressable(scaleOnPress = 0.985f, hapticOnClick = true, onClick = onClick)
            .padding(start = 12.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.width(28.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = isPlaying,
                transitionSpec = {
                    (fadeIn(tween(150)) + scaleIn(initialScale = 0.85f, animationSpec = tween(150))) togetherWith
                        (fadeOut(tween(110)) + scaleOut(targetScale = 0.85f, animationSpec = tween(110)))
                },
                label = "chart_rank_eq_$rank"
            ) { playing ->
                if (playing) {
                    LivingEqualizer(
                        isPlaying = true,
                        barCount = 3,
                        barColor = accentColor,
                        barWidth = 2.dp,
                        barSpacing = 1.5.dp,
                        maxHeight = 13.dp
                    )
                } else {
                    Text(
                        text = String.format(Locale.US, "%02d", rank),
                        color = if (rank <= 3) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelLarge.tabularFigures().copy(
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        AsyncImage(
            model = track.coverArtUrl,
            contentDescription = track.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                color = titleColor,
                fontSize = 15.sp,
                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.artistName,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .echoPressable(scaleOnPress = 0.86f, onClick = onMoreClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.MoreHoriz,
                contentDescription = "More options",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

@Composable
private fun CuratedMixCard(
    playlist: Playlist,
    isDarkMode: Boolean,
    cardBg: Color,
    borderColor: Color,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = Modifier
            .width(164.dp)
            .shadow(6.dp, shape, ambientColor = Color.Black.copy(alpha = 0.10f))
            .clip(shape)
            .background(cardBg)
            .echoSpecularBorder(isDarkMode, shape, borderColor)
            .echoPressable(scaleOnPress = 0.96f, hapticOnClick = true, onClick = onClick)
    ) {
        AsyncImage(
            model = playlist.coverArtUrl,
            contentDescription = playlist.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                text = playlist.name,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = playlist.ownerName.ifBlank { "Curated Mix" },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AlbumReleaseCard(
    playlist: Playlist,
    isDarkMode: Boolean,
    borderColor: Color,
    onClick: () -> Unit
) {
    val artShape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier
            .width(146.dp)
            .echoPressable(scaleOnPress = 0.96f, onClick = onClick)
    ) {
        AsyncImage(
            model = playlist.coverArtUrl,
            contentDescription = playlist.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(146.dp)
                .shadow(6.dp, artShape, ambientColor = Color.Black.copy(alpha = 0.12f))
                .clip(artShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .echoSpecularBorder(isDarkMode, artShape, borderColor)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = playlist.name,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = playlist.ownerName.ifBlank { "Album" },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ArtistPortraitCard(
    artist: Artist,
    isDarkMode: Boolean,
    borderColor: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(104.dp)
            .echoPressable(scaleOnPress = 0.95f, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = artist.avatarUrl,
            contentDescription = artist.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(98.dp)
                .shadow(8.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.14f))
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .echoSpecularBorder(isDarkMode, CircleShape, borderColor)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = artist.name,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        if (artist.monthlyListeners > 0L) {
            Text(
                text = formatListeners(artist.monthlyListeners),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall.tabularFigures().copy(fontSize = 11.5.sp),
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun formatListeners(count: Long): String {
    return when {
        count >= 1_000_000L -> "${(count / 100_000L) / 10.0}M listeners"
        count >= 1_000L -> "${count / 1_000L}K listeners"
        else -> "$count listeners"
    }
}
