package com.spotify.clone.ui.home

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.spotify.clone.core.designsystem.EchoArtistAvatar
import com.spotify.clone.core.designsystem.EchoArtworkImage
import com.spotify.clone.core.designsystem.EchoFilterChip
import com.spotify.clone.core.designsystem.EchoInsetCard
import com.spotify.clone.core.designsystem.EchoMotion
import com.spotify.clone.core.designsystem.EchoPrismAqua
import com.spotify.clone.core.designsystem.EchoPrismLavender
import com.spotify.clone.core.designsystem.EchoPrismRose
import com.spotify.clone.core.designsystem.EchoPrismSky
import com.spotify.clone.core.designsystem.EchoProfileButton
import com.spotify.clone.core.designsystem.EchoRadius
import com.spotify.clone.core.designsystem.EchoSectionHeader
import com.spotify.clone.core.designsystem.EchoSpacing
import com.spotify.clone.core.designsystem.LivingEqualizer
import com.spotify.clone.core.designsystem.LocalAppTheme
import com.spotify.clone.core.designsystem.LocalReducedMotion
import com.spotify.clone.core.designsystem.SwipeableTrackRow
import com.spotify.clone.core.designsystem.ThemeManager
import com.spotify.clone.core.designsystem.echoContrastOn
import com.spotify.clone.core.designsystem.echoPressable
import com.spotify.clone.core.designsystem.echoStaggeredEntrance
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
    SONGS("Songs"),
    GENRES("Genres"),
    CHARTS("Charts")
}

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
    val activeAvatar = PrismAvatarOption.fromId(avatarId)
    val avatarInitial = remember(userName) {
        userName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "E"
    }
    val avatarColors = remember(activeAvatar) {
        listOf(activeAvatar.startColor, activeAvatar.endColor)
    }

    var selectedTrackForOptions by remember { mutableStateOf<Track?>(null) }
    var selectedFilter by remember { mutableStateOf(HomeFilter.ALL) }
    val listState = rememberLazyListState()
    val heroListState = rememberLazyListState()

    // Live playing logo animation: pass State<Float> down to Canvas so HomeScreen skips 60fps recomposition
    val staticZero = remember { mutableFloatStateOf(0f) }
    val logoWavePhaseState: State<Float>?
    val logoShimmerState: State<Float>
    if (!isReducedMotion) {
        val logoTransition = rememberInfiniteTransition(label = "home_logo_motion")
        logoWavePhaseState = logoTransition.animateFloat(
            initialValue = 0f,
            targetValue = (Math.PI * 2.0).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "home_logo_wave"
        )
        logoShimmerState = logoTransition.animateFloat(
            initialValue = -1f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2800, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "home_logo_shimmer"
        )
    } else {
        logoWavePhaseState = null
        logoShimmerState = staticZero
    }
    val playingWaveStrengthState = animateFloatAsState(
        targetValue = when {
            isReducedMotion -> 0f
            playbackState.isPlaying -> 1.0f
            else -> 0.45f
        },
        animationSpec = tween(durationMillis = EchoMotion.DurationStandard, easing = EchoMotion.EaseOutStrong),
        label = "home_logo_strength"
    )
    val greetingText = remember { timeGreeting() }

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

    val screenBg = MaterialTheme.colorScheme.background
    val cardBg = MaterialTheme.colorScheme.surfaceContainerLow
    val borderColor = MaterialTheme.colorScheme.outlineVariant
    val primaryColor = MaterialTheme.colorScheme.primary
    val textPrimary = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant

    val screenMode = when {
        state.isLoading -> 0
        state.errorMessage != null && state.featuredPlaylists.isEmpty() -> 1
        else -> 2
    }

    AnimatedContent(
        targetState = screenMode,
        transitionSpec = {
            fadeIn(tween(220, easing = EchoMotion.EaseOutStrong)) togetherWith fadeOut(tween(150))
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
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.dispatch(HomeEvent.Refresh) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(EchoRadius.pill)
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
                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = { viewModel.dispatch(HomeEvent.Refresh) },
                    modifier = Modifier.fillMaxSize()
                ) {
                    val navBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding(),
                        contentPadding = PaddingValues(bottom = 168.dp + navBottomPadding)
                    ) {
                        // 1. Compact Redesigned Home Header (Brand Mark + Greeting/Name + Profile)
                        item(key = "home_header", contentType = "home_header") {
                            HomeCompactHeader(
                                greeting = greetingText,
                                userName = userName,
                                avatarInitial = avatarInitial,
                                avatarColors = avatarColors,
                                isPlaying = playbackState.isPlaying,
                                breathPhaseState = logoWavePhaseState,
                                shimmerShiftState = logoShimmerState,
                                playingWaveStrengthState = playingWaveStrengthState,
                                cardBg = cardBg,
                                borderColor = borderColor,
                                accentColor = activeTheme.primaryColor,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary,
                                onRefresh = { viewModel.dispatch(HomeEvent.Refresh) },
                                onProfileClick = onSettingsClick
                            )
                        }

                        // 2. Interactive Filter Strip (Emil-style crisp tactile chips)
                        item(key = "home_filter_strip", contentType = "filter_strip") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 20.dp, vertical = 2.dp),
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

                        // 3. Jump Back In — Quick Access Grid
                        if (selectedFilter == HomeFilter.ALL && state.quickPicks.isNotEmpty()) {
                            item(key = "quick_picks_section", contentType = "quick_picks_grid") {
                                Column(modifier = itemPlacementAnim().echoStaggeredEntrance(0)) {
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
                                    Spacer(modifier = Modifier.height(26.dp))
                                }
                            }
                        }

                        // 4. Spotlight (AI & Editorial Showcase)
                        if ((selectedFilter == HomeFilter.ALL || selectedFilter == HomeFilter.CHARTS) &&
                            state.editorialBanners.isNotEmpty()
                        ) {
                            item(key = "top_picks_section", contentType = "hero_carousel") {
                                Column(modifier = itemPlacementAnim().echoStaggeredEntrance(1)) {
                                    EchoSectionHeader(title = "Spotlight")
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LazyRow(
                                        state = heroListState,
                                        flingBehavior = rememberSnapFlingBehavior(lazyListState = heroListState),
                                        contentPadding = PaddingValues(horizontal = 20.dp),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        itemsIndexed(
                                            items = state.editorialBanners,
                                            key = { _, b -> b.id },
                                            contentType = { _, _ -> "hero_banner" }
                                        ) { index, banner ->
                                            EditorialPosterCard(
                                                banner = banner,
                                                index = index,
                                                listState = heroListState,
                                                isReducedMotion = isReducedMotion,
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
                                    Spacer(modifier = Modifier.height(28.dp))
                                }
                            }
                        }

                        // 5. Picked For You (12D Cosine + LinUCB + MMR Recommender)
                        if ((selectedFilter == HomeFilter.ALL || selectedFilter == HomeFilter.FOR_YOU || selectedFilter == HomeFilter.SONGS) &&
                            state.recommendedItems.isNotEmpty()
                        ) {
                            item(key = "recommended_for_you_section", contentType = "track_carousel") {
                                val cleanUser = userName.trim()
                                val pickedTitle = if (cleanUser.isNotBlank() && !cleanUser.equals("Listener", ignoreCase = true)) {
                                    "Picked For $cleanUser"
                                } else {
                                    "Picked For You"
                                }
                                Column(modifier = itemPlacementAnim().echoStaggeredEntrance(2)) {
                                    EchoSectionHeader(
                                        title = pickedTitle,
                                        actionLabel = "Play Mix",
                                        onActionClick = {
                                            val first = state.recommendedTracks.firstOrNull() ?: return@EchoSectionHeader
                                            viewModel.dispatch(HomeEvent.PlayTrack(first, state.recommendedTracks))
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 20.dp),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        items(
                                            items = state.recommendedItems,
                                            key = { "rec_${it.track.id}" },
                                            contentType = { "track_card" }
                                        ) { pred ->
                                            val track = pred.track
                                            val isCurrent = playbackState.currentTrack?.id == track.id
                                            CleanSquareTrackCard(
                                                track = track,
                                                subtitleText = track.artistName,
                                                isCurrent = isCurrent,
                                                isPlaying = isCurrent && playbackState.isPlaying,
                                                borderColor = borderColor,
                                                accentColor = activeTheme.primaryColor,
                                                onMoreClick = { selectedTrackForOptions = track },
                                                onClick = {
                                                    viewModel.dispatch(HomeEvent.PlayTrack(track, state.recommendedTracks))
                                                }
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(28.dp))
                                }
                            }
                        }

                        // 6. New Songs Today (ML Taste Ranked)
                        if ((selectedFilter == HomeFilter.ALL || selectedFilter == HomeFilter.SONGS) &&
                            state.newSongsToday.isNotEmpty()
                        ) {
                            item(key = "new_songs_today_section", contentType = "track_carousel") {
                                Column(modifier = itemPlacementAnim().echoStaggeredEntrance(2)) {
                                    EchoSectionHeader(
                                        title = "New Songs",
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
                                        items(
                                            items = state.newSongsToday,
                                            key = { "new_${it.id}" },
                                            contentType = { "track_card" }
                                        ) { track ->
                                            val isCurrent = playbackState.currentTrack?.id == track.id
                                            CleanSquareTrackCard(
                                                track = track,
                                                subtitleText = track.artistName,
                                                isCurrent = isCurrent,
                                                isPlaying = isCurrent && playbackState.isPlaying,
                                                borderColor = borderColor,
                                                accentColor = activeTheme.primaryColor,
                                                onMoreClick = { selectedTrackForOptions = track },
                                                onClick = {
                                                    viewModel.dispatch(HomeEvent.PlayTrack(track, state.newSongsToday))
                                                }
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(28.dp))
                                }
                            }
                        }

                        // 7. Time-of-Day Contextual Flow (Local Taste Behavior Algorithm)
                        if ((selectedFilter == HomeFilter.ALL || selectedFilter == HomeFilter.FOR_YOU || selectedFilter == HomeFilter.SONGS) &&
                            state.timeFlowTracks.isNotEmpty()
                        ) {
                            item(key = "time_flow_section", contentType = "track_carousel") {
                                Column(modifier = itemPlacementAnim().echoStaggeredEntrance(3)) {
                                    EchoSectionHeader(
                                        title = state.timeFlowTitle,
                                        actionLabel = "Play All",
                                        onActionClick = {
                                            val first = state.timeFlowTracks.firstOrNull() ?: return@EchoSectionHeader
                                            viewModel.dispatch(HomeEvent.PlayTrack(first, state.timeFlowTracks))
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 20.dp),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        items(
                                            items = state.timeFlowTracks,
                                            key = { "flow_${it.id}" },
                                            contentType = { "track_card" }
                                        ) { track ->
                                            val isCurrent = playbackState.currentTrack?.id == track.id
                                            CleanSquareTrackCard(
                                                track = track,
                                                subtitleText = track.artistName,
                                                isCurrent = isCurrent,
                                                isPlaying = isCurrent && playbackState.isPlaying,
                                                borderColor = borderColor,
                                                accentColor = activeTheme.primaryColor,
                                                onMoreClick = { selectedTrackForOptions = track },
                                                onClick = {
                                                    viewModel.dispatch(HomeEvent.PlayTrack(track, state.timeFlowTracks))
                                                }
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(28.dp))
                                }
                            }
                        }

                        // 8. Browse Genres & Genre Radio (Always scrollable on Home + Genres Filter)
                        if (selectedFilter == HomeFilter.ALL || selectedFilter == HomeFilter.GENRES || selectedFilter == HomeFilter.FOR_YOU) {
                            if (state.genreHubs.isNotEmpty()) {
                                item(key = "genre_worlds_section", contentType = "genre_carousel") {
                                    Column(modifier = itemPlacementAnim().echoStaggeredEntrance(3)) {
                                        EchoSectionHeader(title = "Genres For You")
                                        Spacer(modifier = Modifier.height(8.dp))
                                        LazyRow(
                                            contentPadding = PaddingValues(horizontal = 20.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            itemsIndexed(
                                                items = state.genreHubs,
                                                key = { _, p -> "gh_${p.id}" },
                                                contentType = { _, _ -> "genre_card" }
                                            ) { idx, hub ->
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
                                        Spacer(modifier = Modifier.height(22.dp))
                                    }
                                }
                            }

                            item(key = "genre_radio_stations_section", contentType = "genre_radio_strip") {
                                val activeLock by musicController.activeGenreLock.collectAsState()
                                Column(modifier = itemPlacementAnim()) {
                                    EchoSectionHeader(title = "Genre Radio")
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 20.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        items(
                                            items = MusicGenre.selectableGenres,
                                            key = { "genre_station_${it.id}" },
                                            contentType = { "genre_chip" }
                                        ) { genre ->
                                            val isSelected = activeLock == genre
                                            val chipCorner by animateDpAsState(
                                                targetValue = if (isSelected) 24.dp else 14.dp,
                                                animationSpec = if (isReducedMotion) tween(100) else EchoMotion.spatialFast(),
                                                label = "genre_chip_corner"
                                            )
                                            val stationShape = RoundedCornerShape(chipCorner)
                                            val chipBg by animateColorAsState(
                                                targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                                animationSpec = tween(EchoMotion.DurationMicro, easing = EchoMotion.EaseOutStrong),
                                                label = "genre_chip_bg"
                                            )
                                            val chipFg by animateColorAsState(
                                                targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else textPrimary,
                                                animationSpec = tween(EchoMotion.DurationMicro),
                                                label = "genre_chip_fg"
                                            )
                                            Row(
                                                modifier = Modifier
                                                    .clip(stationShape)
                                                    .background(chipBg)
                                                    .echoPressable(scaleOnPress = 0.95f, hapticOnClick = true) {
                                                        viewModel.dispatch(HomeEvent.PlayGenreRadio(genre))
                                                    }
                                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Radio,
                                                    contentDescription = null,
                                                    tint = if (isSelected) chipFg else primaryColor,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = genre.shortLabel,
                                                    color = chipFg,
                                                    style = MaterialTheme.typography.labelLarge
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(28.dp))
                                }
                            }
                        }

                        // 9. Mood & Sound Clusters (12D Spherical K-Means Clustering)
                        if ((selectedFilter == HomeFilter.ALL || selectedFilter == HomeFilter.GENRES || selectedFilter == HomeFilter.FOR_YOU) &&
                            state.moodMixes.isNotEmpty()
                        ) {
                            item(key = "mood_clusters_section", contentType = "playlist_carousel") {
                                Column(modifier = itemPlacementAnim().echoStaggeredEntrance(3)) {
                                    EchoSectionHeader(title = "Mood Mixes")
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 20.dp),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        items(
                                            items = state.moodMixes,
                                            key = { "mood_${it.id}" },
                                            contentType = { "playlist_card" }
                                        ) { cluster ->
                                            CleanSquarePlaylistCard(
                                                playlist = cluster,
                                                fallbackSubtitle = "Mood Mix",
                                                borderColor = borderColor,
                                                onClick = { onPlaylistClick(cluster.id) }
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(28.dp))
                                }
                            }
                        }

                        // 10. Explore & Deep Cuts (LinUCB Exploration Bandit)
                        if ((selectedFilter == HomeFilter.ALL || selectedFilter == HomeFilter.FOR_YOU || selectedFilter == HomeFilter.SONGS) &&
                            state.exploreGems.isNotEmpty()
                        ) {
                            item(key = "explore_gems_section", contentType = "track_carousel") {
                                val gemTracks = remember(state.exploreGems) { state.exploreGems.map { it.track } }
                                Column(modifier = itemPlacementAnim().echoStaggeredEntrance(4)) {
                                    EchoSectionHeader(
                                        title = "Discover More",
                                        actionLabel = "Play Mix",
                                        onActionClick = {
                                            val first = gemTracks.firstOrNull() ?: return@EchoSectionHeader
                                            viewModel.dispatch(HomeEvent.PlayTrack(first, gemTracks))
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 20.dp),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        items(
                                            items = state.exploreGems,
                                            key = { "gem_${it.track.id}" },
                                            contentType = { "track_card" }
                                        ) { pred ->
                                            val track = pred.track
                                            val isCurrent = playbackState.currentTrack?.id == track.id
                                            CleanSquareTrackCard(
                                                track = track,
                                                subtitleText = track.artistName,
                                                isCurrent = isCurrent,
                                                isPlaying = isCurrent && playbackState.isPlaying,
                                                borderColor = borderColor,
                                                accentColor = activeTheme.primaryColor,
                                                onMoreClick = { selectedTrackForOptions = track },
                                                onClick = {
                                                    viewModel.dispatch(HomeEvent.PlayTrack(track, gemTracks))
                                                }
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(28.dp))
                                }
                            }
                        }

                        // 11. From Your Likes (Co-Occurrence Graph + Cosine Similarity)
                        if ((selectedFilter == HomeFilter.ALL || selectedFilter == HomeFilter.FOR_YOU || selectedFilter == HomeFilter.SONGS) &&
                            (state.likedInspiredTracks.isNotEmpty() || state.favoriteTracks.isNotEmpty())
                        ) {
                            item(key = "liked_inspired_section", contentType = "track_carousel") {
                                val tracksToShow = state.likedInspiredTracks.ifEmpty { state.favoriteTracks }
                                Column(modifier = itemPlacementAnim().echoStaggeredEntrance(3)) {
                                    EchoSectionHeader(
                                        title = "From Your Likes",
                                        actionLabel = "Play Mix",
                                        onActionClick = {
                                            val first = tracksToShow.firstOrNull() ?: return@EchoSectionHeader
                                            viewModel.dispatch(HomeEvent.PlayTrack(first, tracksToShow))
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 20.dp),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        items(
                                            items = tracksToShow,
                                            key = { "liked_rec_${it.id}" },
                                            contentType = { "track_card" }
                                        ) { track ->
                                            val isCurrent = playbackState.currentTrack?.id == track.id
                                            CleanSquareTrackCard(
                                                track = track,
                                                subtitleText = track.artistName,
                                                isCurrent = isCurrent,
                                                isPlaying = isCurrent && playbackState.isPlaying,
                                                borderColor = borderColor,
                                                accentColor = EchoPrismRose,
                                                onMoreClick = { selectedTrackForOptions = track },
                                                onClick = {
                                                    viewModel.dispatch(HomeEvent.PlayTrack(track, tracksToShow))
                                                }
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(28.dp))
                                }
                            }
                        }

                        // 12. Made For You (Daily Mixes)
                        if ((selectedFilter == HomeFilter.ALL || selectedFilter == HomeFilter.FOR_YOU) &&
                            state.featuredPlaylists.isNotEmpty()
                        ) {
                            item(key = "curated_mixes_section", contentType = "playlist_carousel") {
                                Column(modifier = itemPlacementAnim().echoStaggeredEntrance(4)) {
                                    EchoSectionHeader(title = "Made For You")
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 20.dp),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        items(
                                            items = state.featuredPlaylists,
                                            key = { "mix_${it.id}" },
                                            contentType = { "playlist_card" }
                                        ) { playlist ->
                                            CleanSquarePlaylistCard(
                                                playlist = playlist,
                                                fallbackSubtitle = "Daily Mix",
                                                borderColor = borderColor,
                                                onClick = { onPlaylistClick(playlist.id) }
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(28.dp))
                                }
                            }
                        }

                        // 13. Recently Played
                        if ((selectedFilter == HomeFilter.ALL || selectedFilter == HomeFilter.SONGS) &&
                            state.recentlyPlayed.isNotEmpty()
                        ) {
                            item(key = "recently_played_section", contentType = "track_carousel") {
                                Column(modifier = itemPlacementAnim().echoStaggeredEntrance(4)) {
                                    EchoSectionHeader(title = "Recently Played")
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 20.dp),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        items(
                                            items = state.recentlyPlayed.take(15),
                                            key = { "recent_${it.id}" },
                                            contentType = { "track_card" }
                                        ) { track ->
                                            val isCurrent = playbackState.currentTrack?.id == track.id
                                            CleanSquareTrackCard(
                                                track = track,
                                                subtitleText = track.artistName,
                                                isCurrent = isCurrent,
                                                isPlaying = isCurrent && playbackState.isPlaying,
                                                borderColor = borderColor,
                                                accentColor = activeTheme.primaryColor,
                                                cardWidth = 128.dp,
                                                onMoreClick = { selectedTrackForOptions = track },
                                                onClick = {
                                                    viewModel.dispatch(HomeEvent.PlayTrack(track, state.recentlyPlayed))
                                                }
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(28.dp))
                                }
                            }
                        }

                        // 14. Top Charts
                        if ((selectedFilter == HomeFilter.ALL || selectedFilter == HomeFilter.CHARTS || selectedFilter == HomeFilter.SONGS) &&
                            state.trendingTracks.isNotEmpty()
                        ) {
                            item(key = "top_charts_section", contentType = "top_charts_card") {
                                val chartTracks = if (selectedFilter == HomeFilter.CHARTS) {
                                    state.trendingTracks
                                } else {
                                    state.trendingTracks.take(8)
                                }
                                Column(modifier = itemPlacementAnim().echoStaggeredEntrance(4)) {
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
                                    Spacer(modifier = Modifier.height(28.dp))
                                }
                            }
                        }

                        // 15. New Albums
                        if ((selectedFilter == HomeFilter.ALL || selectedFilter == HomeFilter.CHARTS) &&
                            state.newAlbums.isNotEmpty()
                        ) {
                            item(key = "new_releases_section", contentType = "album_carousel") {
                                Column(modifier = itemPlacementAnim().echoStaggeredEntrance(5)) {
                                    EchoSectionHeader(title = "New Albums")
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 20.dp),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        items(
                                            items = state.newAlbums.take(12),
                                            key = { "album_${it.id}" },
                                            contentType = { "album_card" }
                                        ) { album ->
                                            CleanSquarePlaylistCard(
                                                playlist = album,
                                                fallbackSubtitle = "Album",
                                                borderColor = borderColor,
                                                onClick = { onPlaylistClick(album.id) }
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(28.dp))
                                }
                            }
                        }

                        // 16. Artists
                        if ((selectedFilter == HomeFilter.ALL || selectedFilter == HomeFilter.FOR_YOU) &&
                            state.artists.isNotEmpty()
                        ) {
                            item(key = "artists_section", contentType = "artist_carousel") {
                                Column(modifier = itemPlacementAnim()) {
                                    EchoSectionHeader(title = "Artists")
                                    Spacer(modifier = Modifier.height(10.dp))
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 20.dp),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        items(
                                            items = state.artists,
                                            key = { "artist_${it.id}" },
                                            contentType = { "artist_card" }
                                        ) { artist ->
                                            ArtistPortraitCard(
                                                artist = artist,
                                                borderColor = borderColor,
                                                onClick = { onArtistClick(artist.name) }
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

private fun LazyItemScope.itemPlacementAnim(modifier: Modifier = Modifier): Modifier =
    modifier.animateItem(
        fadeInSpec = tween(EchoMotion.DurationStandard, easing = EchoMotion.EaseOutStrong),
        fadeOutSpec = tween(140),
        placementSpec = EchoMotion.springCriticallyDamped()
    )

private val GENRE_WORLD_TINTS = listOf(EchoPrismRose, EchoPrismLavender, EchoPrismSky, EchoPrismAqua)

@Composable
private fun GenreWorldCard(
    playlist: Playlist,
    paletteIndex: Int,
    isDarkMode: Boolean,
    borderColor: Color,
    onClick: () -> Unit,
    onPlay: () -> Unit
) {
    val containerColor = when (paletteIndex % 3) {
        0 -> MaterialTheme.colorScheme.primaryContainer
        1 -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.tertiaryContainer
    }
    val onContainerColor = when (paletteIndex % 3) {
        0 -> MaterialTheme.colorScheme.onPrimaryContainer
        1 -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onTertiaryContainer
    }
    val shape = remember { RoundedCornerShape(EchoRadius.lg) }

    Surface(
        modifier = Modifier
            .width(172.dp)
            .height(106.dp)
            .echoPressable(scaleOnPress = 0.96f, hapticOnClick = true, onClick = onClick),
        shape = shape,
        color = containerColor
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            Column(modifier = Modifier.align(Alignment.TopStart)) {
                Text(
                    text = playlist.name,
                    color = onContainerColor,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${playlist.tracks.size} tracks",
                    color = onContainerColor.copy(alpha = 0.78f),
                    style = MaterialTheme.typography.bodySmall.tabularFigures()
                )
            }
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                EchoArtworkImage(
                    imageUrl = playlist.coverArtUrl,
                    contentDescription = playlist.name,
                    seedKey = playlist.name,
                    iconSize = 18.dp,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(EchoRadius.xs))
                )
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .echoPressable(scaleOnPress = 0.88f, hapticOnClick = true, onClick = onPlay),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Play ${playlist.name}",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CleanSquareTrackCard(
    track: Track,
    subtitleText: String,
    isCurrent: Boolean,
    isPlaying: Boolean,
    borderColor: Color,
    accentColor: Color,
    cardWidth: Dp = 152.dp,
    onMoreClick: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val isReducedMotion = LocalReducedMotion.current
    val cornerDp by animateDpAsState(
        targetValue = if (isCurrent) EchoRadius.lg else EchoRadius.md,
        animationSpec = if (isReducedMotion) tween(100) else EchoMotion.spatialFast(),
        label = "square_track_corner"
    )
    val artShape = RoundedCornerShape(cornerDp)
    val primaryColor = MaterialTheme.colorScheme.primary
    Column(
        modifier = Modifier
            .width(cardWidth)
            .echoPressable(scaleOnPress = 0.96f, hapticOnClick = true, onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(cardWidth)
                .clip(artShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .then(
                    if (isCurrent) Modifier.border(2.5.dp, primaryColor, artShape) else Modifier
                )
        ) {
            EchoArtworkImage(
                imageUrl = track.coverArtUrl,
                contentDescription = track.title,
                seedKey = "${track.title}_${track.artistName}",
                modifier = Modifier.fillMaxSize()
            )
            androidx.compose.animation.AnimatedVisibility(
                visible = isCurrent,
                enter = if (isReducedMotion) {
                    fadeIn(tween(120))
                } else {
                    fadeIn(tween(160, easing = EchoMotion.EaseOutStrong)) +
                        scaleIn(initialScale = 0.85f, animationSpec = EchoMotion.spatialFast())
                },
                exit = fadeOut(tween(120)) + scaleOut(targetScale = 0.85f, animationSpec = tween(120)),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    LivingEqualizer(
                        isPlaying = isPlaying,
                        barCount = 3,
                        barColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        barWidth = 2.dp,
                        barSpacing = 1.5.dp,
                        maxHeight = 11.dp,
                        minHeight = 3.dp
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    color = if (isCurrent) primaryColor else MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitleText,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (onMoreClick != null) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .echoPressable(scaleOnPress = 0.86f, onClick = onMoreClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreHoriz,
                        contentDescription = "Track options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CleanSquarePlaylistCard(
    playlist: Playlist,
    fallbackSubtitle: String,
    borderColor: Color,
    onClick: () -> Unit
) {
    val artShape = RoundedCornerShape(EchoRadius.lg)
    Column(
        modifier = Modifier
            .width(152.dp)
            .echoPressable(scaleOnPress = 0.96f, hapticOnClick = true, onClick = onClick)
    ) {
        EchoArtworkImage(
            imageUrl = playlist.coverArtUrl,
            contentDescription = playlist.name,
            seedKey = playlist.name,
            modifier = Modifier
                .size(152.dp)
                .clip(artShape)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = playlist.name,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = playlist.ownerName.ifBlank { fallbackSubtitle },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HomeCompactHeader(
    greeting: String,
    userName: String,
    avatarInitial: String,
    avatarColors: List<Color>,
    isPlaying: Boolean,
    breathPhaseState: State<Float>?,
    shimmerShiftState: State<Float>,
    playingWaveStrengthState: State<Float>,
    cardBg: Color,
    borderColor: Color,
    accentColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    onRefresh: () -> Unit,
    onProfileClick: () -> Unit
) {
    val cleanName = remember(userName) { userName.trim().ifBlank { "Listener" } }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Borderless floating animated ECHO acoustic logo mark
        Box(
            modifier = Modifier
                .size(34.dp)
                .echoPressable(scaleOnPress = 0.88f, hapticOnClick = true, onClick = onRefresh),
            contentAlignment = Alignment.Center
        ) {
            EchoAcousticLogoMark(
                breathPhaseState = breathPhaseState,
                shimmerShiftState = shimmerShiftState,
                playingWaveStrengthState = playingWaveStrengthState,
                modifier = Modifier.size(26.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = greeting,
                color = textSecondary,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = cleanName,
                color = textPrimary,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        EchoProfileButton(
            initial = avatarInitial,
            gradientColors = avatarColors,
            onClick = onProfileClick,
            visualSize = 40.dp
        )
    }
}

private fun timeGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        in 17..21 -> "Good Evening"
        else -> "Good Night"
    }
}

@Composable
private fun EditorialPosterCard(
    banner: EditorialBanner,
    index: Int,
    listState: LazyListState,
    isReducedMotion: Boolean,
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
                (1f - (dist / maxDist) * 0.06f).coerceIn(0.95f, 1f)
            }
        }
    }

    val cardShape = remember { RoundedCornerShape(EchoRadius.xl) }
    val posterScrim = remember {
        Brush.verticalGradient(
            0.0f to Color.Transparent,
            0.35f to Color.Transparent,
            0.68f to Color.Black.copy(alpha = 0.65f),
            1.0f to Color.Black.copy(alpha = 0.92f)
        )
    }
    Box(
        modifier = Modifier
            .width(304.dp)
            .height(216.dp)
            .graphicsLayer {
                scaleX = focusScale
                scaleY = focusScale
            }
            .clip(cardShape)
            .echoPressable(scaleOnPress = 0.96f, hapticOnClick = true, onClick = onClick)
    ) {
        EchoArtworkImage(
            imageUrl = banner.coverArtUrl,
            contentDescription = banner.title,
            seedKey = banner.title,
            iconSize = 48.dp,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(posterScrim)
        )

        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (banner.eyebrow.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = banner.eyebrow.uppercase(Locale.US),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(5.dp))
                }
                Text(
                    text = banner.title,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = banner.subtitle,
                    color = Color.White.copy(alpha = 0.82f),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .size(EchoSpacing.minTouchTarget)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .echoPressable(scaleOnPress = 0.90f, hapticOnClick = true, onClick = onPlay),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Play ${banner.title}",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
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
    cardBg: Color,
    borderColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val isReducedMotion = LocalReducedMotion.current
    val corner by animateDpAsState(
        targetValue = if (isPlaying) EchoRadius.lg else EchoRadius.md,
        animationSpec = if (isReducedMotion) tween(100) else EchoMotion.spatialFast(),
        label = "quick_pick_corner"
    )
    val shape = RoundedCornerShape(corner)
    val tileFill by animateColorAsState(
        targetValue = if (isPlaying) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = tween(EchoMotion.DurationMicro, easing = EchoMotion.EaseOutStrong),
        label = "quick_pick_fill"
    )
    val tileTextColor = if (isPlaying) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = modifier
            .height(60.dp)
            .clip(shape)
            .background(tileFill)
            .echoPressable(scaleOnPress = 0.95f, hapticOnClick = true, onClick = onClick)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        EchoArtworkImage(
            imageUrl = playlist.coverArtUrl,
            contentDescription = playlist.name,
            seedKey = playlist.name,
            iconSize = 20.dp,
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(EchoRadius.nested(corner, 4.dp)))
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = playlist.name,
            color = tileTextColor,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(end = 6.dp)
        )
        AnimatedVisibility(
            visible = isPlaying,
            enter = if (isReducedMotion) fadeIn(tween(120)) else {
                fadeIn(tween(160, easing = EchoMotion.EaseOutStrong)) +
                    scaleIn(initialScale = 0.85f, animationSpec = EchoMotion.spatialFast())
            },
            exit = fadeOut(tween(110)) + scaleOut(targetScale = 0.85f, animationSpec = tween(110))
        ) {
            LivingEqualizer(
                isPlaying = true,
                barCount = 3,
                barColor = MaterialTheme.colorScheme.onPrimaryContainer,
                barWidth = 2.dp,
                barSpacing = 1.5.dp,
                maxHeight = 12.dp,
                modifier = Modifier.padding(end = 10.dp)
            )
        }
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
    val primaryColor = MaterialTheme.colorScheme.primary
    val titleColor by animateColorAsState(
        targetValue = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(EchoMotion.DurationMicro),
        label = "chart_title_color"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f) else Color.Transparent)
            .echoPressable(scaleOnPress = 0.99f, hapticOnClick = true, onClick = onClick)
            .padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isPlaying -> MaterialTheme.colorScheme.primaryContainer
                        rank <= 3 -> MaterialTheme.colorScheme.tertiaryContainer
                        else -> Color.Transparent
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = isPlaying,
                transitionSpec = {
                    (fadeIn(tween(150)) + scaleIn(initialScale = 0.85f, animationSpec = EchoMotion.spatialFast())) togetherWith
                        (fadeOut(tween(110)) + scaleOut(targetScale = 0.85f, animationSpec = tween(110)))
                },
                label = "chart_rank_eq_$rank"
            ) { playing ->
                if (playing) {
                    LivingEqualizer(
                        isPlaying = true,
                        barCount = 3,
                        barColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        barWidth = 2.dp,
                        barSpacing = 1.5.dp,
                        maxHeight = 13.dp
                    )
                } else {
                    Text(
                        text = String.format(Locale.US, "%02d", rank),
                        color = if (rank <= 3) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelLarge.tabularFigures().copy(fontWeight = FontWeight.ExtraBold)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        EchoArtworkImage(
            imageUrl = track.coverArtUrl,
            contentDescription = track.title,
            seedKey = "${track.title}_${track.artistName}",
            iconSize = 18.dp,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(EchoRadius.xs))
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                color = titleColor,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.artistName,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Box(
            modifier = Modifier
                .size(EchoSpacing.minTouchTarget)
                .clip(CircleShape)
                .echoPressable(scaleOnPress = 0.88f, onClick = onMoreClick),
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
private fun ArtistPortraitCard(
    artist: Artist,
    borderColor: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(98.dp)
            .echoPressable(scaleOnPress = 0.95f, hapticOnClick = true, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        EchoArtistAvatar(
            name = artist.name,
            avatarUrl = artist.avatarUrl,
            size = 94.dp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = artist.name,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
