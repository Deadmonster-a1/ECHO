package com.spotify.clone.ui.search

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Icon
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.spotify.clone.core.designsystem.EchoArtistAvatar
import com.spotify.clone.core.designsystem.EchoArtworkImage
import com.spotify.clone.core.designsystem.EchoFilterChip
import com.spotify.clone.core.designsystem.EchoInsetCard
import com.spotify.clone.core.designsystem.EchoMotion
import com.spotify.clone.core.designsystem.EchoRadius
import com.spotify.clone.core.designsystem.EchoSectionHeader
import com.spotify.clone.core.designsystem.EchoSpacing
import com.spotify.clone.core.designsystem.ExpressiveLoadingIndicator
import com.spotify.clone.core.designsystem.LocalReducedMotion
import com.spotify.clone.core.designsystem.echoPressable
import com.spotify.clone.core.designsystem.echoStaggeredEntrance
import com.spotify.clone.core.model.Playlist
import com.spotify.clone.core.model.Track
import com.spotify.clone.core.player.MusicController
import com.spotify.clone.ui.components.CategoryCard
import com.spotify.clone.ui.components.TrackOptionsBottomSheet
import com.spotify.clone.ui.components.TrackRowItem

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onArtistClick: ((String) -> Unit)? = null,
    onPlaylistClick: ((String) -> Unit)? = null,
    scrollToTopTrigger: Long = 0L,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isReducedMotion = LocalReducedMotion.current
    val focusManager = LocalFocusManager.current
    val musicController = remember { MusicController.getInstance(context) }
    val playbackState by musicController.playbackState.collectAsState()
    val state by viewModel.uiState.collectAsState()

    val screenBg = MaterialTheme.colorScheme.background
    val cardBg = MaterialTheme.colorScheme.surfaceContainerLow
    val borderColor = MaterialTheme.colorScheme.outlineVariant
    val primaryColor = MaterialTheme.colorScheme.primary
    val textPrimary = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant

    var selectedTrackForOptions by remember { mutableStateOf<Track?>(null) }
    var isSearchFocused by remember { mutableStateOf(false) }
    val searchContainerBg = if (isSearchFocused) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surfaceContainerHigh
    val idleListState = rememberLazyListState()
    val resultsListState = rememberLazyListState()

    fun submitQueryOrEchoUri(rawInput: String) {
        val raw = rawInput.trim()
        if (raw.isBlank()) return
        val normalized = raw.removePrefix("echo://").removePrefix("echo:")
        when {
            raw.startsWith("echo:artist:", ignoreCase = true) ||
                raw.startsWith("echo://artist/", ignoreCase = true) -> {
                val artist = normalized.substringAfter("artist").trimStart(':', '/').trim()
                if (artist.isNotBlank()) onArtistClick?.invoke(artist)
            }
            raw.startsWith("echo:playlist:", ignoreCase = true) ||
                raw.startsWith("echo://playlist/", ignoreCase = true) -> {
                val pl = normalized.substringAfter("playlist").trimStart(':', '/').trim()
                if (pl.isNotBlank()) onPlaylistClick?.invoke(pl)
            }
            raw.startsWith("echo:", ignoreCase = true) -> {
                val q = normalized.removePrefix("track:").removePrefix("track/").replace("_", " ").trim()
                if (q.isNotBlank()) viewModel.dispatch(SearchEvent.ExecuteSearch(q))
            }
            else -> viewModel.dispatch(SearchEvent.ExecuteSearch(raw))
        }
    }

    LaunchedEffect(scrollToTopTrigger) {
        if (scrollToTopTrigger > 0L) {
            idleListState.animateScrollToItem(0)
            resultsListState.animateScrollToItem(0)
        }
    }

    BackHandler(enabled = state.query.isNotBlank()) {
        focusManager.clearFocus()
        viewModel.dispatch(SearchEvent.ClearQuery)
    }

    val searchBorderColor by animateColorAsState(
        targetValue = if (isSearchFocused) primaryColor else borderColor,
        animationSpec = tween(EchoMotion.DurationMicro, easing = EchoMotion.EaseOutStrong),
        label = "search_focus_border"
    )

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                viewModel.dispatch(SearchEvent.ExecuteSearch(spoken))
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(screenBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // 1. Clean Search Header
            Text(
                text = "Search",
                color = textPrimary,
                style = MaterialTheme.typography.displayLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 12.dp)
            )

            // 2. Material 3 Expressive Search Pill Bar
            val searchShape = RoundedCornerShape(EchoRadius.xxl)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = EchoSpacing.screenHorizontalMargin)
                    .height(EchoSpacing.searchBarHeight)
                    .border(
                        width = if (isSearchFocused) 1.75.dp else 0.dp,
                        color = if (isSearchFocused) primaryColor else Color.Transparent,
                        shape = searchShape
                    ),
                shape = searchShape,
                color = searchContainerBg,
                tonalElevation = if (isSearchFocused) 4.dp else 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 18.dp, end = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = if (isSearchFocused) primaryColor else textSecondary,
                        modifier = Modifier.size(21.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    BasicTextField(
                        value = state.query,
                        onValueChange = { viewModel.dispatch(SearchEvent.QueryChanged(it)) },
                        singleLine = true,
                        textStyle = TextStyle(color = textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium),
                        cursorBrush = SolidColor(primaryColor),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                focusManager.clearFocus()
                                submitQueryOrEchoUri(state.query)
                            }
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .onFocusChanged { isSearchFocused = it.isFocused },
                        decorationBox = { innerTextField ->
                            if (state.query.isEmpty()) {
                                Text(
                                    text = "Songs, artists, or albums",
                                    color = textSecondary,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            innerTextField()
                        }
                    )

                    AnimatedContent(
                        targetState = state.query.isNotEmpty(),
                        transitionSpec = {
                            (fadeIn(tween(150)) + scaleIn(initialScale = 0.85f, animationSpec = EchoMotion.spatialFast())) togetherWith
                                (fadeOut(tween(110)) + scaleOut(targetScale = 0.85f, animationSpec = tween(110)))
                        },
                        label = "search_clear_or_mic"
                    ) { hasQuery ->
                        if (hasQuery) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                                    .echoPressable(scaleOnPress = 0.88f, hapticOnClick = true) {
                                        viewModel.dispatch(SearchEvent.ClearQuery)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Clear",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .echoPressable(scaleOnPress = 0.88f, hapticOnClick = true) {
                                        try {
                                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Search songs or artists...")
                                            }
                                            speechLauncher.launch(intent)
                                        } catch (_: ActivityNotFoundException) {
                                            Toast.makeText(context, "Voice search unavailable", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Mic,
                                    contentDescription = "Voice Search",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Search Content Modes
            val searchStateMode = when {
                state.query.isNotBlank() && state.isShowingSuggestions -> 1
                state.query.isNotBlank() -> 2
                else -> 0
            }

            AnimatedContent(
                targetState = searchStateMode,
                transitionSpec = {
                    if (isReducedMotion) {
                        fadeIn(tween(150)) togetherWith fadeOut(tween(120))
                    } else {
                        (fadeIn(tween(200, easing = EchoMotion.EaseOutStrong)) +
                            slideInVertically(tween(200, easing = EchoMotion.EaseOutStrong)) { 12 }) togetherWith
                            fadeOut(tween(130))
                    }
                },
                modifier = Modifier.fillMaxSize(),
                label = "search_mode_transition"
            ) { mode ->
                val navBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                when (mode) {
                    1 -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 176.dp + navBottomPadding)
                    ) {
                        item(key = "search_direct", contentType = "search_direct_action") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .echoPressable(scaleOnPress = 0.985f) {
                                        focusManager.clearFocus()
                                        submitQueryOrEchoUri(state.query)
                                    }
                                    .padding(horizontal = 20.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = null,
                                    tint = primaryColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Search for \"${state.query.trim()}\"",
                                    color = primaryColor,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (state.songRecommendations.isNotEmpty()) {
                            items(
                                items = state.songRecommendations,
                                key = { "instant_${it.id}" },
                                contentType = { "track_row" }
                            ) { track ->
                                TrackRowItem(
                                    track = track,
                                    isPlaying = playbackState.currentTrack?.id == track.id && playbackState.isPlaying,
                                    onClick = {
                                        focusManager.clearFocus()
                                        viewModel.dispatch(SearchEvent.PlayTrack(track, state.songRecommendations))
                                    },
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
                        }

                        items(
                            items = state.liveSuggestions.take(6),
                            key = { "sug_$it" },
                            contentType = { "search_suggestion" }
                        ) { suggestion ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .echoPressable(scaleOnPress = 0.985f) {
                                        focusManager.clearFocus()
                                        viewModel.dispatch(SearchEvent.SelectSuggestion(suggestion))
                                    }
                                    .padding(horizontal = 20.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = null,
                                    tint = textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = suggestion,
                                    color = textPrimary,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    2 -> Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 20.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SearchTab.entries.forEach { tab ->
                                EchoFilterChip(
                                    text = tab.title,
                                    isSelected = state.selectedTab == tab,
                                    onClick = { viewModel.dispatch(SearchEvent.TabSelected(tab)) }
                                )
                            }
                        }

                        val tracksToShow = when (state.selectedTab) {
                            SearchTab.LOCAL -> state.localAudioTracks
                            SearchTab.ALL, SearchTab.TRACKS -> state.searchResults
                            else -> emptyList()
                        }
                        val albumsToShow = when (state.selectedTab) {
                            SearchTab.ALL, SearchTab.ALBUMS -> state.searchAlbums
                            else -> emptyList()
                        }
                        val artistsToShow = when (state.selectedTab) {
                            SearchTab.ALL, SearchTab.ARTISTS -> state.searchArtists
                            else -> emptyList()
                        }

                        val resultMode = when {
                            state.isSearching -> 0
                            tracksToShow.isEmpty() && albumsToShow.isEmpty() && artistsToShow.isEmpty() -> 1
                            else -> 2
                        }

                        AnimatedContent(
                            targetState = resultMode,
                            transitionSpec = {
                                fadeIn(tween(180, easing = EchoMotion.EaseOutStrong)) togetherWith fadeOut(tween(120))
                            },
                            modifier = Modifier.fillMaxSize(),
                            label = "search_results_state"
                        ) { resMode ->
                            when (resMode) {
                                0 -> Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    ExpressiveLoadingIndicator(size = 52.dp)
                                }
                                1 -> Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Box(
                                            modifier = Modifier
                                                .size(64.dp)
                                                .clip(RoundedCornerShape(EchoRadius.lg))
                                                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.SearchOff,
                                                contentDescription = null,
                                                tint = primaryColor,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = "No results for \"${state.query.trim()}\"",
                                            color = textPrimary,
                                            style = MaterialTheme.typography.titleLarge,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Try a different keyword or browse a genre.",
                                            color = textSecondary,
                                            style = MaterialTheme.typography.bodyMedium,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                                else -> LazyColumn(
                                    state = resultsListState,
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(bottom = 176.dp + navBottomPadding)
                                ) {
                                    val topHit = tracksToShow.firstOrNull()
                                    if (topHit != null && state.selectedTab == SearchTab.ALL) {
                                        item(key = "top_result_card", contentType = "top_result") {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            EchoSectionHeader(title = "Top Result")
                                            Spacer(modifier = Modifier.height(6.dp))
                                            EchoInsetCard(
                                                modifier = Modifier.padding(horizontal = 20.dp),
                                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.52f),
                                                shape = RoundedCornerShape(EchoRadius.xl)
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .echoPressable(scaleOnPress = 0.985f, hapticOnClick = true) {
                                                            viewModel.dispatch(SearchEvent.PlayTrack(topHit, tracksToShow))
                                                        }
                                                        .padding(14.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    EchoArtworkImage(
                                                        imageUrl = topHit.coverArtUrl,
                                                        contentDescription = topHit.title,
                                                        seedKey = "${topHit.title}_${topHit.artistName}",
                                                        iconSize = 24.dp,
                                                        modifier = Modifier
                                                            .size(64.dp)
                                                            .clip(RoundedCornerShape(EchoRadius.md))
                                                    )
                                                    Spacer(modifier = Modifier.width(14.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = topHit.title,
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                            style = MaterialTheme.typography.titleLarge,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        Text(
                                                            text = "Song • ${topHit.artistName}",
                                                            color = textSecondary,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .size(EchoSpacing.minTouchTarget)
                                                            .clip(RoundedCornerShape(16.dp))
                                                            .background(primaryColor)
                                                            .echoPressable(scaleOnPress = 0.88f, hapticOnClick = true) {
                                                                viewModel.dispatch(SearchEvent.PlayTrack(topHit, tracksToShow))
                                                            },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Filled.PlayArrow,
                                                            contentDescription = "Play",
                                                            tint = MaterialTheme.colorScheme.onPrimary,
                                                            modifier = Modifier.size(22.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(14.dp))
                                        }
                                    }

                                    if (artistsToShow.isNotEmpty()) {
                                        item(key = "res_artists", contentType = "artist_chips_row") {
                                            EchoSectionHeader(title = "Artists")
                                            LazyRow(
                                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                items(
                                                    items = artistsToShow.take(12),
                                                    key = { "art_$it" },
                                                    contentType = { "artist_chip" }
                                                ) { artist ->
                                                    val pillShape = RoundedCornerShape(EchoRadius.pill)
                                                    val knownAvatar = remember(artist) {
                                                        com.spotify.clone.core.data.WorldOnboardingCatalog
                                                            .findOnboardingArtist(artist)?.avatarUrl.orEmpty()
                                                    }
                                                    Row(
                                                        modifier = Modifier
                                                            .clip(pillShape)
                                                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                                            .echoPressable(scaleOnPress = 0.96f) { onArtistClick?.invoke(artist) }
                                                            .padding(start = 6.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        EchoArtistAvatar(
                                                            name = artist,
                                                            avatarUrl = knownAvatar,
                                                            size = 30.dp
                                                        )
                                                        Spacer(modifier = Modifier.width(10.dp))
                                                        Text(
                                                            text = artist,
                                                            color = textPrimary,
                                                            style = MaterialTheme.typography.labelLarge
                                                        )
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(10.dp))
                                        }
                                    }

                                    if (albumsToShow.isNotEmpty()) {
                                        item(key = "res_albums", contentType = "album_carousel") {
                                            EchoSectionHeader(title = "Albums")
                                            LazyRow(
                                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                items(
                                                    items = albumsToShow,
                                                    key = { "alb_${it.id}" },
                                                    contentType = { "album_card" }
                                                ) { album ->
                                                    AlbumSearchCard(
                                                        album = album,
                                                        borderColor = borderColor,
                                                        onClick = { onPlaylistClick?.invoke(album.id) }
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(10.dp))
                                        }
                                    }

                                    if (tracksToShow.isNotEmpty()) {
                                        item(key = "res_songs_header", contentType = "section_header") {
                                            EchoSectionHeader(title = if (state.selectedTab == SearchTab.LOCAL) "Local Audio" else "Songs")
                                        }
                                        items(
                                            items = tracksToShow,
                                            key = { "res_track_${it.id}" },
                                            contentType = { "track_row" }
                                        ) { track ->
                                            TrackRowItem(
                                                track = track,
                                                isPlaying = playbackState.currentTrack?.id == track.id && playbackState.isPlaying,
                                                onClick = {
                                                    viewModel.dispatch(SearchEvent.PlayTrack(track, tracksToShow))
                                                },
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
                                    }
                                }
                            }
                        }
                    }

                    else -> LazyColumn(
                        state = idleListState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 168.dp + navBottomPadding)
                    ) {
                        // Quick Discovery Pills (tailored to user's preferred Indian, English & global scenes)
                        item(key = "quick_search_tags", contentType = "tag_strip") {
                            val userLangs by com.spotify.clone.core.profile.UserProfileManager.preferredLanguages.collectAsState()
                            val quickTags = remember(userLangs) {
                                val langTags = userLangs.mapNotNull { lang ->
                                    when (lang.lowercase()) {
                                        "hindi" -> "Hindi Top Hits"
                                        "telugu" -> "Telugu Hits"
                                        "tamil" -> "Tamil Hits"
                                        "punjabi" -> "Punjabi Hits"
                                        "malayalam" -> "Malayalam Hits"
                                        "kannada" -> "Kannada Hits"
                                        "korean" -> "K-Pop Hits"
                                        "japanese" -> "J-Pop Hits"
                                        "spanish" -> "Latin Hits"
                                        "english" -> "Global Pop"
                                        else -> null
                                    }
                                }
                                (listOf("Trending") + langTags + listOf("Melodies", "Lo-Fi", "Pop", "Hip-Hop", "Rock"))
                                    .distinct()
                                    .take(8)
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 20.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                quickTags.forEachIndexed { tagIdx, tag ->
                                    val chipShape = RoundedCornerShape(EchoRadius.pill)
                                    val pillBg = if (tagIdx % 2 == 0) {
                                        MaterialTheme.colorScheme.secondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.tertiaryContainer
                                    }
                                    val pillFg = if (tagIdx % 2 == 0) {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onTertiaryContainer
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(chipShape)
                                            .background(pillBg)
                                            .echoPressable(scaleOnPress = 0.95f, hapticOnClick = true) {
                                                viewModel.dispatch(SearchEvent.ExecuteSearch(tag))
                                            }
                                            .padding(horizontal = 16.dp, vertical = 9.dp)
                                    ) {
                                        Text(
                                            text = tag,
                                            color = pillFg,
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        if (state.recentSearches.isNotEmpty()) {
                            item(key = "recent_header", contentType = "section_header") {
                                EchoSectionHeader(
                                    title = "Recent",
                                    actionLabel = "Clear",
                                    onActionClick = {
                                        viewModel.dispatch(SearchEvent.ClearRecentSearches)
                                    }
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                            item(key = "recent_list", contentType = "recent_searches") {
                                val recents = state.recentSearches.take(5)
                                EchoInsetCard(modifier = Modifier.padding(horizontal = 20.dp)) {
                                    recents.forEachIndexed { idx, recent ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .echoPressable(scaleOnPress = 0.99f) {
                                                    viewModel.dispatch(SearchEvent.ExecuteSearch(recent))
                                                }
                                                .padding(start = 16.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.History,
                                                    contentDescription = null,
                                                    tint = textSecondary,
                                                    modifier = Modifier.size(17.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = recent,
                                                color = textPrimary,
                                                style = MaterialTheme.typography.bodyLarge,
                                                modifier = Modifier.weight(1f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .echoPressable(scaleOnPress = 0.88f) {
                                                        viewModel.dispatch(SearchEvent.RemoveRecentSearch(recent))
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Close,
                                                    contentDescription = "Remove",
                                                    tint = textSecondary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                        if (idx < recents.lastIndex) {
                                            androidx.compose.material3.HorizontalDivider(
                                                color = borderColor.copy(alpha = 0.35f),
                                                thickness = 0.5.dp,
                                                modifier = Modifier.padding(start = 60.dp, end = 16.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(20.dp))
                            }
                        }

                        item(key = "browse_header", contentType = "section_header") {
                            EchoSectionHeader(title = "Browse Genres")
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        val categoryRows = state.categories.chunked(2)
                        items(
                            count = categoryRows.size,
                            key = { idx -> categoryRows[idx].joinToString("_") { it.id } },
                            contentType = { "category_row" }
                        ) { rowIndex ->
                            val pair = categoryRows[rowIndex]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .echoStaggeredEntrance(rowIndex.coerceAtMost(5), riseDistance = 8.dp)
                                    .padding(horizontal = 20.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                pair.forEach { category ->
                                    CategoryCard(
                                        category = category,
                                        onClick = { viewModel.dispatch(SearchEvent.ExecuteSearch(category.searchQuery)) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (pair.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
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
            onViewArtist = { artist -> onArtistClick?.invoke(artist) }
        )
    }
}

@Composable
private fun AlbumSearchCard(
    album: Playlist,
    borderColor: Color,
    onClick: () -> Unit
) {
    val artShape = RoundedCornerShape(EchoRadius.md)
    Column(
        modifier = Modifier
            .width(142.dp)
            .echoPressable(scaleOnPress = 0.96f, hapticOnClick = true, onClick = onClick)
    ) {
        EchoArtworkImage(
            imageUrl = album.coverArtUrl,
            contentDescription = album.name,
            seedKey = album.name,
            modifier = Modifier
                .size(142.dp)
                .clip(artShape)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = album.name,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = album.ownerName.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) } ?: "Album",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
