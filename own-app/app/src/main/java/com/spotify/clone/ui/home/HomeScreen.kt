package com.spotify.clone.ui.home

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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

    // Live playing logo animation: dances like an equalizer when playing, gentle acoustic pulse when idle
    val logoTransition = rememberInfiniteTransition(label = "home_logo_motion")
    val logoWavePhase by logoTransition.animateFloat(
        initialValue = 0f,
        targetValue = (Math.PI * 2.0).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "home_logo_wave"
    )
    val logoShimmer by logoTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "home_logo_shimmer"
    )
    val playingWaveStrength by animateFloatAsState(
        targetValue = when {
            isReducedMotion -> 0f
            playbackState.isPlaying -> 1.0f
            else -> 0.45f
        },
        animationSpec = tween(durationMillis = EchoMotion.DurationStandard, easing = EchoMotion.EaseOutStrong),
        label = "home_logo_strength"
    )

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

    val screenBg = activeTheme.resolvedBackground(isDarkMode)
    val cardBg = activeTheme.resolvedCard(isDarkMode)
    val borderColor = activeTheme.resolvedBorder(isDarkMode)
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
                            containerColor = activeTheme.primaryColor,
                            contentColor = echoContrastOn(activeTheme.primaryColor)
                        ),
                        shape = RoundedCornerShape(EchoRadius.sm)
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
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding(),
                        contentPadding = PaddingValues(bottom = 168.dp)
                    ) {
                        // 1. Animated Playing Echo Logo + Personalized Greeting & User Name Header
                        item(key = "home_header") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.echoPressable(scaleOnPress = 0.96f, hapticOnClick = true) {
                                            viewModel.dispatch(HomeEvent.Refresh)
                                        }
                                    ) {
                                        EchoAcousticLogoMark(
                                            breathPhase = if (isReducedMotion) null else logoWavePhase,
                                            shimmerShift = if (isReducedMotion) 0f else logoShimmer,
                                            playingWaveStrength = playingWaveStrength,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(7.dp))
                                        Text(
                                            text = "echo",
                                            color = activeTheme.primaryColor,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.5.sp
                                            )
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = timeGreetingWithName(userName),
                                        color = textPrimary,
                                        style = MaterialTheme.typography.displayMedium.copy(
                                            fontSize = 26.sp,
                                            lineHeight = 31.sp
                                        ),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                EchoProfileButton(
                                    initial = avatarInitial,
                                    gradientColors = avatarColors,
                                    onClick = onSettingsClick
                                )
                            }
                        }

                        // 2. Interactive Filter Strip (Emil-style crisp tactile chips)
                        item(key = "home_filter_strip") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 20.dp, vertical = 4.dp),
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
                            Spacer(modifier = Modifier.height(14.dp))
                        }

                        // 3. Jump Back In — Quick Access Grid
                        if (selectedFilter == HomeFilter.ALL && state.quickPicks.isNotEmpty()) {
                            item(key = "quick_picks_section") {
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
                            item(key = "top_picks_section") {
                                Column(modifier = itemPlacementAnim().echoStaggeredEntrance(1)) {
                                    EchoSectionHeader(title = "Spotlight")
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
                            item(key = "recommended_for_you_section") {
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
                                        items(state.recommendedItems, key = { "rec_${it.track.id}" }) { pred ->
                                            val track = pred.track
                                            val isCurrent = playbackState.currentTrack?.id == track.id
                                            CleanSquareTrackCard(
                                                track = track,
                                                subtitleText = "${track.artistName} • ${pred.matchPercent}% match",
                                                isCurrent = isCurrent,
                                                isPlaying = isCurrent && playbackState.isPlaying,
                                                borderColor = borderColor,
                                                accentColor = activeTheme.primaryColor,
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
                            item(key = "new_songs_today_section") {
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
                                        items(state.newSongsToday, key = { "new_${it.id}" }) { track ->
                                            val isCurrent = playbackState.currentTrack?.id == track.id
                                            CleanSquareTrackCard(
                                                track = track,
                                                subtitleText = track.artistName,
                                                isCurrent = isCurrent,
                                                isPlaying = isCurrent && playbackState.isPlaying,
                                                borderColor = borderColor,
                                                accentColor = activeTheme.primaryColor,
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
                            item(key = "time_flow_section") {
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
                                        items(state.timeFlowTracks, key = { "flow_${it.id}" }) { track ->
                                            val isCurrent = playbackState.currentTrack?.id == track.id
                                            CleanSquareTrackCard(
                                                track = track,
                                                subtitleText = track.artistName,
                                                isCurrent = isCurrent,
                                                isPlaying = isCurrent && playbackState.isPlaying,
                                                borderColor = borderColor,
                                                accentColor = activeTheme.primaryColor,
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
                                item(key = "genre_worlds_section") {
                                    Column(modifier = itemPlacementAnim().echoStaggeredEntrance(3)) {
                                        EchoSectionHeader(title = "Genres For You")
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
                                        Spacer(modifier = Modifier.height(22.dp))
                                    }
                                }
                            }

                            item(key = "genre_radio_stations_section") {
                                val activeLock by musicController.activeGenreLock.collectAsState()
                                Column(modifier = itemPlacementAnim()) {
                                    EchoSectionHeader(title = "Genre Radio")
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 20.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        items(MusicGenre.selectableGenres, key = { "genre_station_${it.id}" }) { genre ->
                                            val isSelected = activeLock == genre
                                            val stationShape = RoundedCornerShape(EchoRadius.pill)
                                            val chipBg by animateColorAsState(
                                                targetValue = if (isSelected) activeTheme.primaryColor else cardBg,
                                                animationSpec = tween(EchoMotion.DurationMicro, easing = EchoMotion.EaseOutStrong),
                                                label = "genre_chip_bg"
                                            )
                                            val chipBorder by animateColorAsState(
                                                targetValue = if (isSelected) activeTheme.primaryColor else borderColor,
                                                animationSpec = tween(EchoMotion.DurationMicro),
                                                label = "genre_chip_border"
                                            )
                                            Row(
                                                modifier = Modifier
                                                    .clip(stationShape)
                                                    .background(chipBg)
                                                    .border(0.6.dp, chipBorder, stationShape)
                                                    .echoPressable(scaleOnPress = 0.95f, hapticOnClick = true) {
                                                        viewModel.dispatch(HomeEvent.PlayGenreRadio(genre))
                                                    }
                                                    .padding(horizontal = 14.dp, vertical = 9.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Radio,
                                                    contentDescription = null,
                                                    tint = if (isSelected) echoContrastOn(activeTheme.primaryColor) else activeTheme.primaryColor,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = genre.shortLabel,
                                                    color = if (isSelected) echoContrastOn(activeTheme.primaryColor) else textPrimary,
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
                            item(key = "mood_clusters_section") {
                                Column(modifier = itemPlacementAnim().echoStaggeredEntrance(3)) {
                                    EchoSectionHeader(title = "Mood Mixes")
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 20.dp),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        items(state.moodMixes, key = { "mood_${it.id}" }) { cluster ->
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
                            item(key = "explore_gems_section") {
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
                                        items(state.exploreGems, key = { "gem_${it.track.id}" }) { pred ->
                                            val track = pred.track
                                            val isCurrent = playbackState.currentTrack?.id == track.id
                                            CleanSquareTrackCard(
                                                track = track,
                                                subtitleText = "${track.artistName} • ${pred.reasonTag}",
                                                isCurrent = isCurrent,
                                                isPlaying = isCurrent && playbackState.isPlaying,
                                                borderColor = borderColor,
                                                accentColor = activeTheme.primaryColor,
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
                            item(key = "liked_inspired_section") {
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
                                        items(tracksToShow, key = { "liked_rec_${it.id}" }) { track ->
                                            val isCurrent = playbackState.currentTrack?.id == track.id
                                            CleanSquareTrackCard(
                                                track = track,
                                                subtitleText = track.artistName,
                                                isCurrent = isCurrent,
                                                isPlaying = isCurrent && playbackState.isPlaying,
                                                borderColor = borderColor,
                                                accentColor = EchoPrismRose,
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
                            item(key = "curated_mixes_section") {
                                Column(modifier = itemPlacementAnim().echoStaggeredEntrance(4)) {
                                    EchoSectionHeader(title = "Made For You")
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 20.dp),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        items(state.featuredPlaylists, key = { "mix_${it.id}" }) { playlist ->
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
                            item(key = "recently_played_section") {
                                Column(modifier = itemPlacementAnim().echoStaggeredEntrance(4)) {
                                    EchoSectionHeader(title = "Recently Played")
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 20.dp),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        items(state.recentlyPlayed.take(15), key = { "recent_${it.id}" }) { track ->
                                            val isCurrent = playbackState.currentTrack?.id == track.id
                                            CleanSquareTrackCard(
                                                track = track,
                                                subtitleText = track.artistName,
                                                isCurrent = isCurrent,
                                                isPlaying = isCurrent && playbackState.isPlaying,
                                                borderColor = borderColor,
                                                accentColor = activeTheme.primaryColor,
                                                cardWidth = 128.dp,
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
                            item(key = "top_charts_section") {
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
                            item(key = "new_releases_section") {
                                Column(modifier = itemPlacementAnim().echoStaggeredEntrance(5)) {
                                    EchoSectionHeader(title = "New Albums")
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 20.dp),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        items(state.newAlbums.take(12), key = { "album_${it.id}" }) { album ->
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
                            item(key = "artists_section") {
                                Column(modifier = itemPlacementAnim()) {
                                    EchoSectionHeader(title = "Artists")
                                    Spacer(modifier = Modifier.height(10.dp))
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 20.dp),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        items(state.artists, key = { "artist_${it.id}" }) { artist ->
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

@Composable
private fun GenreWorldCard(
    playlist: Playlist,
    paletteIndex: Int,
    isDarkMode: Boolean,
    borderColor: Color,
    onClick: () -> Unit,
    onPlay: () -> Unit
) {
    val activeAccent = LocalAppTheme.current.primaryColor
    val tints = listOf(EchoPrismRose, EchoPrismLavender, EchoPrismSky, EchoPrismAqua)
    val tint = tints[paletteIndex % tints.size]
    val shape = RoundedCornerShape(EchoRadius.md)
    val cardBg = LocalAppTheme.current.resolvedCard(isDarkMode)

    Box(
        modifier = Modifier
            .width(164.dp)
            .height(98.dp)
            .clip(shape)
            .background(cardBg)
            .background(tint.copy(alpha = if (isDarkMode) 0.13f else 0.09f))
            .border(0.6.dp, borderColor, shape)
            .echoPressable(scaleOnPress = 0.96f, hapticOnClick = true, onClick = onClick)
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.align(Alignment.TopStart)) {
            Text(
                text = playlist.name,
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${playlist.tracks.size} tracks",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            AsyncImage(
                model = playlist.coverArtUrl,
                contentDescription = playlist.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(EchoRadius.xs))
            )
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(activeAccent)
                    .echoPressable(scaleOnPress = 0.88f, hapticOnClick = true, onClick = onPlay),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Play ${playlist.name}",
                    tint = echoContrastOn(activeAccent),
                    modifier = Modifier.size(18.dp)
                )
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
    cardWidth: Dp = 148.dp,
    onClick: () -> Unit
) {
    val isReducedMotion = LocalReducedMotion.current
    val artShape = RoundedCornerShape(EchoRadius.md)
    val activeBorder by animateColorAsState(
        targetValue = if (isCurrent) accentColor else borderColor.copy(alpha = 0.7f),
        animationSpec = tween(EchoMotion.DurationMicro, easing = EchoMotion.EaseOutStrong),
        label = "square_track_border"
    )
    Column(
        modifier = Modifier
            .width(cardWidth)
            .echoPressable(scaleOnPress = 0.96f, hapticOnClick = true, onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(cardWidth)
                .clip(artShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(if (isCurrent) 1.5.dp else 0.6.dp, activeBorder, artShape)
        ) {
            AsyncImage(
                model = track.coverArtUrl,
                contentDescription = track.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            androidx.compose.animation.AnimatedVisibility(
                visible = isCurrent,
                enter = if (isReducedMotion) {
                    fadeIn(tween(120))
                } else {
                    fadeIn(tween(160, easing = EchoMotion.EaseOutStrong)) +
                        scaleIn(initialScale = 0.85f, animationSpec = tween(180, easing = EchoMotion.EaseOutStrong))
                },
                exit = fadeOut(tween(120)) + scaleOut(targetScale = 0.85f, animationSpec = tween(120)),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(EchoRadius.xs))
                        .background(Color.Black.copy(alpha = 0.72f))
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    LivingEqualizer(
                        isPlaying = isPlaying,
                        barCount = 3,
                        barColor = accentColor,
                        barWidth = 2.dp,
                        barSpacing = 1.5.dp,
                        maxHeight = 11.dp,
                        minHeight = 3.dp
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = track.title,
            color = if (isCurrent) accentColor else MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
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
}

@Composable
private fun CleanSquarePlaylistCard(
    playlist: Playlist,
    fallbackSubtitle: String,
    borderColor: Color,
    onClick: () -> Unit
) {
    val artShape = RoundedCornerShape(EchoRadius.md)
    Column(
        modifier = Modifier
            .width(148.dp)
            .echoPressable(scaleOnPress = 0.96f, hapticOnClick = true, onClick = onClick)
    ) {
        AsyncImage(
            model = playlist.coverArtUrl,
            contentDescription = playlist.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(148.dp)
                .clip(artShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(0.6.dp, borderColor.copy(alpha = 0.7f), artShape)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = playlist.name,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
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

private fun timeGreetingWithName(userName: String): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val base = when (hour) {
        in 5..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        in 17..21 -> "Good Evening"
        else -> "Good Night"
    }
    val cleanName = userName.trim().ifBlank { "Listener" }
    return "$base, $cleanName"
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

    val cardShape = RoundedCornerShape(EchoRadius.lg)
    Box(
        modifier = Modifier
            .width(296.dp)
            .height(210.dp)
            .graphicsLayer {
                scaleX = focusScale
                scaleY = focusScale
            }
            .clip(cardShape)
            .border(0.6.dp, borderColor, cardShape)
            .echoPressable(scaleOnPress = 0.96f, hapticOnClick = true, onClick = onClick)
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
                        0.0f to Color.Transparent,
                        0.42f to Color.Transparent,
                        0.72f to Color.Black.copy(alpha = 0.64f),
                        1.0f to Color.Black.copy(alpha = 0.90f)
                    )
                )
        )

        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (banner.eyebrow.isNotBlank()) {
                    Text(
                        text = banner.eyebrow,
                        color = Color.White.copy(alpha = 0.78f),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.6.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }
                Text(
                    text = banner.title,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = banner.subtitle,
                    color = Color.White.copy(alpha = 0.78f),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .size(EchoSpacing.minTouchTarget)
                    .clip(CircleShape)
                    .background(accentColor)
                    .echoPressable(scaleOnPress = 0.90f, hapticOnClick = true, onClick = onPlay),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Play ${banner.title}",
                    tint = echoContrastOn(accentColor),
                    modifier = Modifier.size(22.dp)
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
    val shape = RoundedCornerShape(EchoRadius.sm)
    val tileBorder by animateColorAsState(
        targetValue = if (isPlaying) accentColor.copy(alpha = 0.65f) else borderColor,
        animationSpec = tween(EchoMotion.DurationMicro, easing = EchoMotion.EaseOutStrong),
        label = "quick_pick_border"
    )
    Row(
        modifier = modifier
            .height(56.dp)
            .clip(shape)
            .background(cardBg)
            .border(0.6.dp, tileBorder, shape)
            .echoPressable(scaleOnPress = 0.96f, hapticOnClick = true, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = playlist.coverArtUrl,
            contentDescription = playlist.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(56.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = playlist.name,
            color = if (isPlaying) accentColor else MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        )
        AnimatedVisibility(
            visible = isPlaying,
            enter = if (isReducedMotion) fadeIn(tween(120)) else {
                fadeIn(tween(160, easing = EchoMotion.EaseOutStrong)) +
                    scaleIn(initialScale = 0.85f, animationSpec = tween(160, easing = EchoMotion.EaseOutStrong))
            },
            exit = fadeOut(tween(110)) + scaleOut(targetScale = 0.85f, animationSpec = tween(110))
        ) {
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
            .echoPressable(scaleOnPress = 1.0f, hapticOnClick = true, onClick = onClick)
            .padding(start = 12.dp, end = 6.dp, top = 9.dp, bottom = 9.dp),
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
                        style = MaterialTheme.typography.labelLarge.tabularFigures().copy(fontWeight = FontWeight.Bold)
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
                .clip(RoundedCornerShape(EchoRadius.xs))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                color = titleColor,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Medium
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
            .width(96.dp)
            .echoPressable(scaleOnPress = 0.96f, hapticOnClick = true, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = artist.avatarUrl,
            contentDescription = artist.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(92.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(0.6.dp, borderColor, CircleShape)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = artist.name,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
