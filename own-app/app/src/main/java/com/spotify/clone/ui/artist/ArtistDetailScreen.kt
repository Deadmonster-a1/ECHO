package com.spotify.clone.ui.artist

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.spotify.clone.core.data.AudioRepository
import com.spotify.clone.core.designsystem.EchoPlayFab
import com.spotify.clone.core.designsystem.LocalAppTheme
import com.spotify.clone.core.designsystem.LocalReducedMotion
import com.spotify.clone.core.designsystem.echoPressable
import com.spotify.clone.core.model.Artist
import com.spotify.clone.core.model.Track
import com.spotify.clone.core.player.MusicController
import com.spotify.clone.ui.components.TrackOptionsBottomSheet

/**
 * Signature Animation B: Cinematic Collapsing Header for Artists
 * - Hero photo parallax & smooth scale settling driven by LazyListState
 * - Sticky compact header reveal
 * - Real playback state on Play FAB
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
    val isReducedMotion = LocalReducedMotion.current
    val playbackState by musicController.playbackState.collectAsState()

    var isLoading by remember { mutableStateOf(true) }
    var artist by remember { mutableStateOf<Artist?>(null) }
    var topTracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var isFollowing by remember { mutableStateOf(false) }
    var selectedTrackForOptions by remember { mutableStateOf<Track?>(null) }

    LaunchedEffect(artistName) {
        isLoading = true
        val homeArtists = repository.getHomeArtists()
        val matchedArtist = homeArtists.find { it.name.equals(artistName, ignoreCase = true) || it.name.contains(artistName, ignoreCase = true) }
            ?: com.spotify.clone.core.data.SampleCatalog.sampleArtists.find { it.name.contains(artistName, ignoreCase = true) || artistName.contains(it.name, ignoreCase = true) }
            ?: Artist(
                id = "art_${artistName.lowercase().replace(" ", "_")}",
                name = artistName,
                avatarUrl = "https://c.saavncdn.com/artists/Anirudh_Ravichander_003_20260121134149_500x500.jpg",
                monthlyListeners = 14500000
            )
        artist = matchedArtist

        val tracks = repository.search(artistName).take(12)
        topTracks = tracks
        isLoading = false
    }

    if (isLoading || artist == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = activeTheme.primaryColor)
        }
        return
    }

    val currentArtist = artist!!
    val listState = rememberLazyListState()

    val headerScrollOffset by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex == 0) {
                listState.firstVisibleItemScrollOffset.toFloat()
            } else 600f
        }
    }

    val photoParallaxY by remember {
        derivedStateOf {
            if (isReducedMotion) 0f
            else headerScrollOffset * 0.45f
        }
    }

    val photoScale by remember {
        derivedStateOf {
            if (isReducedMotion) 1f
            else (1f + (headerScrollOffset / 1200f)).coerceIn(1f, 1.25f)
        }
    }

    val compactHeaderAlpha by remember {
        derivedStateOf {
            ((headerScrollOffset - 240f) / 100f).coerceIn(0f, 1f)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(bottom = 140.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. Cinematic Hero Artwork Header with Parallax
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                ) {
                    AsyncImage(
                        model = currentArtist.avatarUrl,
                        contentDescription = currentArtist.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                translationY = photoParallaxY
                                scaleX = photoScale
                                scaleY = photoScale
                            }
                    )

                    // Ambient gradient scrim
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.5f),
                                        Color(0xFF101016)
                                    ),
                                    startY = 140f
                                )
                            )
                    )

                    // Artist Title & Verification
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Verified,
                                contentDescription = "Verified Artist",
                                tint = activeTheme.primaryColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Verified Artist",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = currentArtist.name,
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "${formatNumber(currentArtist.monthlyListeners)} monthly listeners",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // 2. Action Bar: Follow, Notifications & Play FAB
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            isFollowing = !isFollowing
                            val msg = if (isFollowing) "Following ${currentArtist.name}" else "Unfollowed ${currentArtist.name}"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        },
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.linearGradient(
                                listOf(
                                    if (isFollowing) activeTheme.primaryColor else MaterialTheme.colorScheme.outline,
                                    if (isFollowing) activeTheme.primaryColor else MaterialTheme.colorScheme.outline
                                )
                            )
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = if (isFollowing) "Following" else "Follow",
                            color = if (isFollowing) activeTheme.primaryColor else MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    IconButton(onClick = {
                        Toast.makeText(context, "Notifications enabled for ${currentArtist.name}", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = "Notifications",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    val isPlayingThisArtist = playbackState.isPlaying && playbackState.currentTrack?.artistName == currentArtist.name
                    EchoPlayFab(
                        isPlaying = isPlayingThisArtist,
                        onToggle = {
                            if (topTracks.isNotEmpty()) {
                                if (isPlayingThisArtist) {
                                    musicController.togglePlayPause()
                                } else {
                                    musicController.playTrack(topTracks.first(), topTracks)
                                }
                            }
                        },
                        size = 54.dp
                    )
                }
            }

            // 3. Popular Section Header
            item {
                Text(
                    text = "Popular Songs",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                )
            }

            // 4. Track List
            itemsIndexed(topTracks) { index, track ->
                val isCurrentlyPlaying = playbackState.currentTrack?.id == track.id
                ArtistTrackRow(
                    index = index + 1,
                    track = track,
                    isPlaying = isCurrentlyPlaying,
                    onClick = {
                        musicController.playTrack(track, topTracks)
                    },
                    onMoreClick = {
                        selectedTrackForOptions = track
                    }
                )
            }
        }

        // Sticky Compact Top Header Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .graphicsLayer { alpha = compactHeaderAlpha }
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                .padding(top = 18.dp, start = 60.dp, end = 20.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = currentArtist.name,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Top Back Button
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .statusBarsPadding()
                .padding(top = 8.dp, start = 8.dp)
                .size(48.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
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

@Composable
private fun ArtistTrackRow(
    index: Int,
    track: Track,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    val activeTheme = LocalAppTheme.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .echoPressable(scaleOnPress = 0.98f, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isPlaying) {
            Icon(
                imageVector = Icons.Filled.GraphicEq,
                contentDescription = "Playing",
                tint = activeTheme.primaryColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        } else {
            Text(
                text = "$index",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.width(24.dp)
            )
        }

        AsyncImage(
            model = track.coverArtUrl,
            contentDescription = track.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                color = if (isPlaying) activeTheme.primaryColor else MaterialTheme.colorScheme.onSurface,
                fontSize = 14.5.sp,
                fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.albumName,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(onClick = onMoreClick) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = "More",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private fun formatNumber(number: Long): String {
    return when {
        number >= 1_000_000 -> String.format("%.1fM", number / 1_000_000.0)
        number >= 1_000 -> String.format("%.1fK", number / 1_000.0)
        else -> number.toString()
    }
}
