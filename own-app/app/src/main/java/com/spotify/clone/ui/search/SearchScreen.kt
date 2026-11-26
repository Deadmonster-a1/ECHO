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
import androidx.compose.material3.HorizontalDivider
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
import com.spotify.clone.core.designsystem.EchoFilterChip
import com.spotify.clone.core.designsystem.EchoInsetCard
import com.spotify.clone.core.designsystem.EchoMotion
import com.spotify.clone.core.designsystem.EchoRadius
import com.spotify.clone.core.designsystem.EchoSectionHeader
import com.spotify.clone.core.designsystem.EchoSpacing
import com.spotify.clone.core.designsystem.LocalAppTheme
import com.spotify.clone.core.designsystem.LocalReducedMotion
import com.spotify.clone.core.designsystem.ThemeManager
import com.spotify.clone.core.designsystem.echoContrastOn
import com.spotify.clone.core.designsystem.echoPressable
import com.spotify.clone.core.designsystem.echoStaggeredEntrance
import com.spotify.clone.core.designsystem.resolvedBackground
import com.spotify.clone.core.designsystem.resolvedBorder
import com.spotify.clone.core.designsystem.resolvedCard
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
    val activeTheme = LocalAppTheme.current
    val isDarkMode by ThemeManager.isDarkMode.collectAsState()
    val isReducedMotion = LocalReducedMotion.current
    val focusManager = LocalFocusManager.current
    val musicController = remember { MusicController.getInstance(context) }
    val playbackState by musicController.playbackState.collectAsState()
    val state by viewModel.uiState.collectAsState()

    val screenBg = activeTheme.resolvedBackground(isDarkMode)
    val cardBg = activeTheme.resolvedCard(isDarkMode)
    val borderColor = activeTheme.resolvedBorder(isDarkMode)
    val textPrimary = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant

    var selectedTrackForOptions by remember { mutableStateOf<Track?>(null) }
    var isSearchFocused by remember { mutableStateOf(false) }
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
        targetValue = if (isSearchFocused) activeTheme.primaryColor else borderColor,
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

            // 2. Uncluttered Search Bar
            val searchShape = RoundedCornerShape(EchoRadius.md)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = EchoSpacing.screenHorizontalMargin)
                    .height(EchoSpacing.searchBarHeight)
                    .clip(searchShape)
                    .background(cardBg)
                    .border(if (isSearchFocused) 1.5.dp else 0.6.dp, searchBorderColor, searchShape)
                    .padding(start = 14.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    tint = if (isSearchFocused) activeTheme.primaryColor else textSecondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                BasicTextField(
                    value = state.query,
                    onValueChange = { viewModel.dispatch(SearchEvent.QueryChanged(it)) },
                    singleLine = true,
                    textStyle = TextStyle(color = textPrimary, fontSize = 15.5.sp),
                    cursorBrush = SolidColor(activeTheme.primaryColor),
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
                                fontSize = 15.sp,
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
                        (fadeIn(tween(150)) + scaleIn(initialScale = 0.85f, animationSpec = tween(150))) togetherWith
                            (fadeOut(tween(110)) + scaleOut(targetScale = 0.85f, animationSpec = tween(110)))
                    },
                    label = "search_clear_or_mic"
                ) { hasQuery ->
                    if (hasQuery) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .echoPressable(scaleOnPress = 0.88f, hapticOnClick = true) {
                                    viewModel.dispatch(SearchEvent.ClearQuery)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Clear",
                                tint = textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
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
                                tint = activeTheme.primaryColor,
                                modifier = Modifier.size(19.dp)
                            )
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
                when (mode) {
                    1 -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 176.dp)
                    ) {
                        item(key = "search_direct") {
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
                                    tint = activeTheme.primaryColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Search for \"${state.query.trim()}\"",
                                    color = activeTheme.primaryColor,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (state.songRecommendations.isNotEmpty()) {
                            items(state.songRecommendations, key = { "instant_${it.id}" }) { track ->
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

                        items(state.liveSuggestions.take(6), key = { "sug_$it" }) { suggestion ->
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
                                    CircularProgressIndicator(color = activeTheme.primaryColor)
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
                                                .size(56.dp)
                                                .clip(CircleShape)
                                                .background(cardBg)
                                                .border(0.6.dp, borderColor, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.SearchOff,
                                                contentDescription = null,
                                                tint = textSecondary,
                                                modifier = Modifier.size(26.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(14.dp))
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
                                    contentPadding = PaddingValues(bottom = 168.dp)
                                ) {
                                    val topHit = tracksToShow.firstOrNull()
                                    if (topHit != null && state.selectedTab == SearchTab.ALL) {
                                        item(key = "top_result_card") {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            EchoSectionHeader(title = "Top Result")
                                            Spacer(modifier = Modifier.height(6.dp))
                                            EchoInsetCard(modifier = Modifier.padding(horizontal = 20.dp)) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .echoPressable(scaleOnPress = 0.985f, hapticOnClick = true) {
                                                            viewModel.dispatch(SearchEvent.PlayTrack(topHit, tracksToShow))
                                                        }
                                                        .padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    AsyncImage(
                                                        model = topHit.coverArtUrl,
                                                        contentDescription = topHit.title,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier
                                                            .size(60.dp)
                                                            .clip(RoundedCornerShape(EchoRadius.sm))
                                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                                    )
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = topHit.title,
                                                            color = textPrimary,
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
                                                            .clip(CircleShape)
                                                            .background(activeTheme.primaryColor)
                                                            .echoPressable(scaleOnPress = 0.88f, hapticOnClick = true) {
                                                                viewModel.dispatch(SearchEvent.PlayTrack(topHit, tracksToShow))
                                                            },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Filled.PlayArrow,
                                                            contentDescription = "Play",
                                                            tint = echoContrastOn(activeTheme.primaryColor),
                                                            modifier = Modifier.size(22.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(14.dp))
                                        }
                                    }

                                    if (artistsToShow.isNotEmpty()) {
                                        item(key = "res_artists") {
                                            EchoSectionHeader(title = "Artists")
                                            LazyRow(
                                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                items(artistsToShow.take(10), key = { "art_$it" }) { artist ->
                                                    val pillShape = RoundedCornerShape(EchoRadius.pill)
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(pillShape)
                                                            .background(cardBg)
                                                            .border(0.6.dp, borderColor, pillShape)
                                                            .echoPressable(scaleOnPress = 0.96f) { onArtistClick?.invoke(artist) }
                                                            .padding(horizontal = 14.dp, vertical = 9.dp)
                                                    ) {
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
                                        item(key = "res_albums") {
                                            EchoSectionHeader(title = "Albums")
                                            LazyRow(
                                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                items(albumsToShow, key = { "alb_${it.id}" }) { album ->
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
                                        item(key = "res_songs_header") {
                                            EchoSectionHeader(title = if (state.selectedTab == SearchTab.LOCAL) "Local Audio" else "Songs")
                                        }
                                        items(tracksToShow, key = { "res_track_${it.id}" }) { track ->
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
                        contentPadding = PaddingValues(bottom = 168.dp)
                    ) {
                        // Quick Discovery Pills
                        item(key = "quick_search_tags") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 20.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("Trending", "Telugu Hits", "Melodies", "Lo-Fi", "Pop", "Hip-Hop").forEach { tag ->
                                    val chipShape = RoundedCornerShape(EchoRadius.pill)
                                    Box(
                                        modifier = Modifier
                                            .clip(chipShape)
                                            .background(cardBg)
                                            .border(0.6.dp, borderColor, chipShape)
                                            .echoPressable(scaleOnPress = 0.96f, hapticOnClick = true) {
                                                viewModel.dispatch(SearchEvent.ExecuteSearch(tag))
                                            }
                                            .padding(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = tag,
                                            color = textPrimary,
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                        }

                        if (state.recentSearches.isNotEmpty()) {
                            item(key = "recent_header") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Recent",
                                        color = textPrimary,
                                        style = MaterialTheme.typography.headlineMedium
                                    )
                                    Text(
                                        text = "Clear",
                                        color = activeTheme.primaryColor,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(EchoRadius.xs))
                                            .echoPressable(scaleOnPress = 0.94f) {
                                                viewModel.dispatch(SearchEvent.ClearRecentSearches)
                                            }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            item(key = "recent_list") {
                                val recents = state.recentSearches.take(5)
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    recents.forEachIndexed { idx, recent ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .echoPressable(scaleOnPress = 1.0f) {
                                                    viewModel.dispatch(SearchEvent.ExecuteSearch(recent))
                                                }
                                                .padding(start = 20.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.History,
                                                contentDescription = null,
                                                tint = textSecondary,
                                                modifier = Modifier.size(18.dp)
                                            )
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
                                            HorizontalDivider(
                                                color = borderColor.copy(alpha = 0.65f),
                                                thickness = 0.5.dp,
                                                modifier = Modifier.padding(start = 50.dp, end = 20.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(18.dp))
                            }
                        }

                        item(key = "browse_header") {
                            EchoSectionHeader(title = "Browse Genres")
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        val categoryRows = state.categories.chunked(2)
                        items(
                            count = categoryRows.size,
                            key = { idx -> categoryRows[idx].joinToString("_") { it.id } }
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
            .width(136.dp)
            .echoPressable(scaleOnPress = 0.97f, onClick = onClick)
    ) {
        AsyncImage(
            model = album.coverArtUrl,
            contentDescription = album.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(136.dp)
                .clip(artShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(0.6.dp, borderColor, artShape)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = album.name,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
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
