package com.spotify.clone.ui.search

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
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
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.SearchOff
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
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
import com.spotify.clone.core.designsystem.EchoSectionHeader
import com.spotify.clone.core.designsystem.LocalAppTheme
import com.spotify.clone.core.designsystem.LocalReducedMotion
import com.spotify.clone.core.designsystem.SpotifyJellyfishAura
import com.spotify.clone.core.designsystem.ThemeManager
import com.spotify.clone.core.designsystem.echoContrastOn
import com.spotify.clone.core.designsystem.echoPressable
import com.spotify.clone.core.designsystem.echoSpecularBorder
import com.spotify.clone.core.designsystem.echoStaggeredEntrance
import com.spotify.clone.core.designsystem.rememberAnimatedArtworkColors
import com.spotify.clone.core.designsystem.resolvedBackground
import com.spotify.clone.core.designsystem.resolvedBorder
import com.spotify.clone.core.designsystem.resolvedCard
import com.spotify.clone.core.model.MusicGenre
import com.spotify.clone.core.model.Playlist
import com.spotify.clone.core.model.Track
import com.spotify.clone.core.player.MusicController
import com.spotify.clone.ui.components.CategoryCard
import com.spotify.clone.ui.components.TrackOptionsBottomSheet
import com.spotify.clone.ui.components.TrackRowItem

/**
 * Redesigned Apple Music-inspired Search Screen:
 * - Atmospheric top gradient matching active theme
 * - Focus-responsive glassmorphic search bar with quick voice/clear morph
 * - Instant mood/genre search chips when idle
 * - "Top Result" Hero Card ranked by Fuzzy Levenshtein + Personal Taste Affinity
 * - Framed inset tracklist & richer category tiles
 */
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onArtistClick: ((String) -> Unit)? = null,
    onPlaylistClick: ((String) -> Unit)? = null,
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
    var showCodeResolverDialog by remember { mutableStateOf(false) }
    var echoCodeInput by remember { mutableStateOf("") }
    val artworkColors = rememberAnimatedArtworkColors(playbackState.currentTrack)

    val searchBorderColor by animateColorAsState(
        targetValue = if (isSearchFocused) activeTheme.primaryColor.copy(alpha = 0.75f) else borderColor,
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
        // Living atmospheric top aura
        SpotifyJellyfishAura(
            artworkColors = artworkColors,
            isPlaying = playbackState.isPlaying,
            isDarkMode = isDarkMode,
            intensity = if (playbackState.currentTrack != null) 0.85f else 0.50f,
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // 1. Large Editorial Search Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Text(
                    text = "EXPLORE",
                    color = activeTheme.primaryColor,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.9.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Search",
                    color = textPrimary,
                    style = MaterialTheme.typography.displayLarge
                )
            }

            // 2. Focus-Responsive Search Bar
            val searchShape = RoundedCornerShape(16.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(50.dp)
                    .clip(searchShape)
                    .background(cardBg)
                    .border(if (isSearchFocused) 1.35.dp else 0.75.dp, searchBorderColor, searchShape)
                    .padding(horizontal = 14.dp),
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
                            viewModel.dispatch(SearchEvent.ExecuteSearch(state.query))
                        }
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { isSearchFocused = it.isFocused },
                    decorationBox = { innerTextField ->
                        if (state.query.isEmpty()) {
                            Text(
                                text = "Artists, Songs, Albums & Local",
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
                                .size(32.dp)
                                .clip(CircleShape)
                                .echoPressable(scaleOnPress = 0.86f, hapticOnClick = true) {
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .echoPressable(scaleOnPress = 0.86f, hapticOnClick = true) {
                                        showCodeResolverDialog = true
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.QrCodeScanner,
                                    contentDescription = "Echo Code / Link",
                                    tint = textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .echoPressable(scaleOnPress = 0.86f, hapticOnClick = true) {
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
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Animated Search Mode Switching
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
                            slideInVertically(tween(200, easing = EchoMotion.EaseOutStrong)) { 14 }) togetherWith
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
                                        viewModel.dispatch(SearchEvent.ExecuteSearch(state.query))
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
                                    fontSize = 15.5.sp,
                                    fontWeight = FontWeight.SemiBold,
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
                                    fontSize = 15.sp,
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
                                .padding(horizontal = 20.dp, vertical = 6.dp),
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
                                                .echoSpecularBorder(isDarkMode, CircleShape, borderColor),
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
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Try checking the spelling or tapping a category below.",
                                            color = textSecondary,
                                            fontSize = 13.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                                else -> LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(bottom = 176.dp)
                                ) {
                                    // Top Result Hero Card
                                    val topHit = tracksToShow.firstOrNull()
                                    if (topHit != null && state.selectedTab == SearchTab.ALL) {
                                        item(key = "top_result_card") {
                                            EchoSectionHeader(title = "Top Result")
                                            Spacer(modifier = Modifier.height(6.dp))
                                            EchoInsetCard(modifier = Modifier.padding(horizontal = 20.dp)) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .echoPressable(scaleOnPress = 0.985f, hapticOnClick = true) {
                                                            viewModel.dispatch(SearchEvent.PlayTrack(topHit, tracksToShow))
                                                        }
                                                        .padding(14.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    AsyncImage(
                                                        model = topHit.coverArtUrl,
                                                        contentDescription = topHit.title,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier
                                                            .size(68.dp)
                                                            .clip(RoundedCornerShape(14.dp))
                                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                                    )
                                                    Spacer(modifier = Modifier.width(14.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = "SONG • ${MusicGenre.inferGenre(topHit).shortLabel.uppercase()}",
                                                            color = activeTheme.primaryColor,
                                                            fontSize = 10.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            letterSpacing = 0.7.sp
                                                        )
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        Text(
                                                            text = topHit.title,
                                                            color = textPrimary,
                                                            fontSize = 17.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = topHit.artistName,
                                                            color = textSecondary,
                                                            fontSize = 13.5.sp,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .size(38.dp)
                                                            .clip(CircleShape)
                                                            .background(activeTheme.primaryColor.copy(alpha = 0.14f))
                                                            .echoPressable(scaleOnPress = 0.88f, hapticOnClick = true) {
                                                                musicController.playSameGenreRadio(topHit)
                                                            },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Filled.Radio,
                                                            contentDescription = "Same-Genre Radio",
                                                            tint = activeTheme.primaryColor,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .size(40.dp)
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
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                items(artistsToShow.take(10), key = { "art_$it" }) { artist ->
                                                    val pillShape = RoundedCornerShape(50)
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(pillShape)
                                                            .background(cardBg)
                                                            .echoSpecularBorder(isDarkMode, pillShape, borderColor)
                                                            .echoPressable(scaleOnPress = 0.95f) { onArtistClick?.invoke(artist) }
                                                            .padding(horizontal = 14.dp, vertical = 8.dp)
                                                    ) {
                                                        Text(
                                                            text = artist,
                                                            color = textPrimary,
                                                            fontSize = 13.5.sp,
                                                            fontWeight = FontWeight.Medium
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
                                                        isDarkMode = isDarkMode,
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
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 176.dp)
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
                                    val chipShape = RoundedCornerShape(50)
                                    Box(
                                        modifier = Modifier
                                            .clip(chipShape)
                                            .background(cardBg)
                                            .echoSpecularBorder(isDarkMode, chipShape, borderColor)
                                            .echoPressable(scaleOnPress = 0.95f, hapticOnClick = true) {
                                                viewModel.dispatch(SearchEvent.ExecuteSearch(tag))
                                            }
                                            .padding(horizontal = 14.dp, vertical = 7.dp)
                                    ) {
                                        Text(
                                            text = tag,
                                            color = textPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
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
                                        text = "Recent Searches",
                                        color = textPrimary,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Clear",
                                        color = activeTheme.primaryColor,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .echoPressable(scaleOnPress = 0.94f) {
                                                viewModel.dispatch(SearchEvent.ClearRecentSearches)
                                            }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            item(key = "recent_card") {
                                EchoInsetCard(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                                    val recents = state.recentSearches.take(5)
                                    recents.forEachIndexed { idx, recent ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .echoPressable(scaleOnPress = 0.985f) {
                                                    viewModel.dispatch(SearchEvent.ExecuteSearch(recent))
                                                }
                                                .padding(horizontal = 14.dp, vertical = 11.dp),
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
                                                fontSize = 14.5.sp,
                                                modifier = Modifier.weight(1f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .echoPressable(scaleOnPress = 0.85f) {
                                                        viewModel.dispatch(SearchEvent.RemoveRecentSearch(recent))
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Close,
                                                    contentDescription = "Remove",
                                                    tint = textSecondary,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                        }
                                        if (idx < recents.lastIndex) {
                                            HorizontalDivider(
                                                color = borderColor,
                                                thickness = 0.6.dp,
                                                modifier = Modifier.padding(start = 44.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                            }
                        }

                        item(key = "browse_header") {
                            EchoSectionHeader(title = "Browse Categories")
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

    if (showCodeResolverDialog) {
        AlertDialog(
            onDismissRequest = { showCodeResolverDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Echo Link & Sound Code", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Enter an Echo URI (e.g. echo:artist:Arijit Singh) or any song/artist query to jump straight in.",
                        color = textSecondary,
                        fontSize = 13.sp
                    )
                    OutlinedTextField(
                        value = echoCodeInput,
                        onValueChange = { echoCodeInput = it },
                        label = { Text("echo:track:... or query") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val raw = echoCodeInput.trim()
                        if (raw.startsWith("echo:artist:", ignoreCase = true)) {
                            val artist = raw.substringAfter("echo:artist:").trim()
                            showCodeResolverDialog = false
                            echoCodeInput = ""
                            if (artist.isNotBlank()) onArtistClick?.invoke(artist)
                        } else if (raw.startsWith("echo:playlist:", ignoreCase = true)) {
                            val pl = raw.substringAfter("echo:playlist:").trim()
                            showCodeResolverDialog = false
                            echoCodeInput = ""
                            if (pl.isNotBlank()) onPlaylistClick?.invoke(pl)
                        } else if (raw.isNotBlank()) {
                            val q = raw.removePrefix("echo:track:").replace("_", " ").trim()
                            showCodeResolverDialog = false
                            echoCodeInput = ""
                            viewModel.dispatch(SearchEvent.ExecuteSearch(q))
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = activeTheme.primaryColor,
                        contentColor = echoContrastOn(activeTheme.primaryColor)
                    )
                ) {
                    Text("Open")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCodeResolverDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun AlbumSearchCard(
    album: Playlist,
    isDarkMode: Boolean,
    borderColor: Color,
    onClick: () -> Unit
) {
    val artShape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier
            .width(136.dp)
            .echoPressable(scaleOnPress = 0.96f, onClick = onClick)
    ) {
        AsyncImage(
            model = album.coverArtUrl,
            contentDescription = album.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(136.dp)
                .shadow(6.dp, artShape, ambientColor = Color.Black.copy(alpha = 0.1f))
                .clip(artShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .echoSpecularBorder(isDarkMode, artShape, borderColor)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = album.name,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = album.ownerName,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
