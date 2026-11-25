package com.spotify.clone.ui.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lyrics
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.spotify.clone.core.designsystem.ArtworkColors
import com.spotify.clone.core.designsystem.EchoMotion
import com.spotify.clone.core.designsystem.LiveAudioSpectrum
import com.spotify.clone.core.designsystem.LocalAppTheme
import com.spotify.clone.core.designsystem.echoPressable
import com.spotify.clone.core.designsystem.echoSpecularBorder
import com.spotify.clone.core.designsystem.tabularFigures
import com.spotify.clone.core.lyrics.LyricsRepository
import com.spotify.clone.core.model.LyricsSyncType
import com.spotify.clone.core.model.MusicGenre
import com.spotify.clone.core.model.PlaybackState
import com.spotify.clone.core.model.Track

/**
 * Reverse-engineered from Spotify's NowPlayingScrollView / Scrollsita (com.spotify.scrollsita.v1):
 * Live scrollable story/telemetry cards below the main player transport:
 * 1. Live Lyrics Preview Card (real-time synced lines + tap to expand)
 * 2. Song DNA & Audio Telemetry Card (genre energy bars, signal path, Credits)
 * 3. About the Artist Card (quick profile jump + Artist Radio)
 * 4. Up Next Peek Card (next 3 tracks + 1-tap play or open full queue)
 */
@Composable
fun NowPlayingScrollCards(
    track: Track,
    playbackState: PlaybackState,
    artworkColors: ArtworkColors,
    crossfadeSeconds: Int,
    playbackSpeed: Float,
    isDarkMode: Boolean,
    cardBg: Color,
    borderColor: Color,
    onOpenFullscreenLyrics: () -> Unit,
    onOpenCredits: () -> Unit,
    onOpenQueue: () -> Unit,
    onViewArtist: ((String) -> Unit)?,
    onStartArtistRadio: () -> Unit,
    onPlayTrackFromQueue: (Track) -> Unit,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeTheme = LocalAppTheme.current
    val textPrimary = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val genre = remember(track.id) { MusicGenre.inferGenre(track) }

    var lyricsResult by remember(track.id) {
        mutableStateOf(LyricsRepository.getCachedLyrics(track))
    }
    LaunchedEffect(track.id) {
        if (lyricsResult.lines.isEmpty()) {
            lyricsResult = LyricsRepository.getOrFetchLyrics(track)
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Live Lyrics Card (Spotify Scrollsita: Lyrics)
        if (lyricsResult.lines.isNotEmpty()) {
            val isSynced = lyricsResult.syncType == LyricsSyncType.LINE_SYNCED
            val activeIdx = remember(playbackState.currentPositionMs, lyricsResult, isSynced) {
                if (!isSynced) 0
                else lyricsResult.lines.indexOfLast { it.timestampMs <= playbackState.currentPositionMs }.coerceAtLeast(0)
            }
            val visibleIndices = remember(activeIdx, lyricsResult.lines.size) {
                val start = activeIdx.coerceAtLeast(0)
                val end = (start + 3).coerceAtMost(lyricsResult.lines.lastIndex)
                (start..end).toList()
            }

            val cardShape = RoundedCornerShape(20.dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(cardShape)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                artworkColors.backgroundTop.copy(alpha = if (isDarkMode) 0.92f else 0.14f),
                                cardBg.copy(alpha = if (isDarkMode) 0.92f else 0.96f)
                            )
                        )
                    )
                    .echoSpecularBorder(isDarkMode, cardShape, borderColor)
                    .echoPressable(scaleOnPress = 0.985f, hapticOnClick = true, onClick = onOpenFullscreenLyrics)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Lyrics,
                            contentDescription = null,
                            tint = activeTheme.primaryColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isSynced) "LIVE LYRICS" else "LYRICS",
                            color = activeTheme.primaryColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    }
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(activeTheme.primaryColor.copy(alpha = 0.16f))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.OpenInFull,
                            contentDescription = "Expand",
                            tint = activeTheme.primaryColor,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Expand",
                            color = activeTheme.primaryColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                visibleIndices.forEach { idx ->
                    val line = lyricsResult.lines[idx]
                    val isActive = idx == activeIdx && isSynced
                    val lineColor by animateColorAsState(
                        targetValue = if (isActive) textPrimary else textSecondary.copy(alpha = 0.65f),
                        animationSpec = tween(220),
                        label = "scroll_lyric_color_$idx"
                    )
                    Text(
                        text = line.words,
                        color = lineColor,
                        fontSize = if (isActive) 17.sp else 14.5.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                        lineHeight = if (isActive) 23.sp else 20.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .echoPressable(scaleOnPress = 0.99f, enabled = isSynced) {
                                onSeekTo(line.timestampMs)
                            }
                            .padding(vertical = 3.dp)
                    )
                }
            }
        }

        // 2. Song DNA & Audio Telemetry Card (Spotify Scrollsita: SongDNA + Credits)
        val dnaShape = RoundedCornerShape(20.dp)
        val profile = remember(genre, track.id) { songDnaProfile(genre, track) }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(dnaShape)
                .background(cardBg.copy(alpha = if (isDarkMode) 0.86f else 0.96f))
                .echoSpecularBorder(isDarkMode, dnaShape, borderColor)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.GraphicEq,
                        contentDescription = null,
                        tint = activeTheme.primaryColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SONG DNA & SIGNAL PATH",
                        color = activeTheme.primaryColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(activeTheme.primaryColor.copy(alpha = 0.16f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = genre.shortLabel,
                        color = activeTheme.primaryColor,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4 Audio DNA meters
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                profile.forEach { (label, value) ->
                    val animVal by animateFloatAsState(
                        targetValue = value,
                        animationSpec = tween(420, easing = EchoMotion.EncoreBoxEase),
                        label = "dna_$label"
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = label,
                                color = textSecondary,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${(animVal * 100).toInt()}%",
                                color = textPrimary,
                                style = MaterialTheme.typography.labelSmall.tabularFigures().copy(
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(5.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(50))
                                .background(textPrimary.copy(alpha = 0.09f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(animVal.coerceIn(0.08f, 1f))
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(activeTheme.primaryColor, artworkColors.vibrantAccent)
                                        )
                                    )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = borderColor.copy(alpha = 0.6f), thickness = 0.6.dp)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "320 kbps AAC • AutoMix ${if (crossfadeSeconds > 0) "${crossfadeSeconds}s" else "Off"} • ${playbackSpeed}x",
                        color = textPrimary,
                        style = MaterialTheme.typography.bodySmall.tabularFigures().copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Cached LRU stream + 5-band hardware EQ pipeline",
                        color = textSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(activeTheme.primaryColor.copy(alpha = 0.14f))
                        .echoPressable(scaleOnPress = 0.94f, hapticOnClick = true, onClick = onOpenCredits)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = activeTheme.primaryColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Credits",
                        color = activeTheme.primaryColor,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // 3. About the Artist Card (Spotify Scrollsita: CreatorBio)
        val artistShape = RoundedCornerShape(20.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(artistShape)
                .background(cardBg.copy(alpha = if (isDarkMode) 0.86f else 0.96f))
                .echoSpecularBorder(isDarkMode, artistShape, borderColor)
                .echoPressable(scaleOnPress = 0.985f, hapticOnClick = true) {
                    onViewArtist?.invoke(track.artistName)
                }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = track.coverArtUrl,
                contentDescription = track.artistName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .border(1.2.dp, artworkColors.vibrantAccent.copy(alpha = 0.6f), CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                        tint = textSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "ABOUT THE ARTIST",
                        color = textSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.7.sp
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = track.artistName,
                    color = textPrimary,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Tap to explore top songs & discography",
                    color = textSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(activeTheme.primaryColor.copy(alpha = 0.14f))
                    .echoPressable(scaleOnPress = 0.88f, hapticOnClick = true, onClick = onStartArtistRadio),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Radio,
                    contentDescription = "Start Artist Radio",
                    tint = activeTheme.primaryColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // 4. Up Next Peek Card (Spotify Scrollsita: Queue)
        val upcoming = remember(playbackState.queue, playbackState.queueIndex) {
            val start = (playbackState.queueIndex + 1).coerceAtLeast(0)
            if (start < playbackState.queue.size) {
                playbackState.queue.subList(start, (start + 3).coerceAtMost(playbackState.queue.size))
            } else {
                emptyList()
            }
        }
        if (upcoming.isNotEmpty()) {
            val queueShape = RoundedCornerShape(20.dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(queueShape)
                    .background(cardBg.copy(alpha = if (isDarkMode) 0.86f else 0.96f))
                    .echoSpecularBorder(isDarkMode, queueShape, borderColor)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .echoPressable(scaleOnPress = 0.985f, onClick = onOpenQueue),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = null,
                            tint = activeTheme.primaryColor,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "UP NEXT IN QUEUE",
                            color = activeTheme.primaryColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "All (${playbackState.queue.size})",
                            color = activeTheme.primaryColor,
                            style = MaterialTheme.typography.labelMedium.tabularFigures().copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = null,
                            tint = activeTheme.primaryColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                upcoming.forEachIndexed { idx, next ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .echoPressable(scaleOnPress = 0.985f, hapticOnClick = true) {
                                onPlayTrackFromQueue(next)
                            }
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = next.coverArtUrl,
                            contentDescription = next.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = next.title,
                                color = textPrimary,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = next.artistName,
                                color = textSecondary,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (idx == 0 && playbackState.isPlaying) {
                            LiveAudioSpectrum(
                                isPlaying = false,
                                barCount = 3,
                                barColor = textSecondary.copy(alpha = 0.5f),
                                barWidth = 2.dp,
                                barSpacing = 1.5.dp,
                                height = 10.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun songDnaProfile(genre: MusicGenre, track: Track): List<Pair<String, Float>> {
    val jitter = ((track.title.hashCode() and 0x0F) / 100f)
    return when (genre) {
        MusicGenre.MASS_BEATS, MusicGenre.ELECTRONIC -> listOf(
            "Energy" to (0.88f + jitter).coerceIn(0.4f, 0.98f),
            "Bass" to (0.84f + jitter).coerceIn(0.4f, 0.98f),
            "Tempo" to 0.82f,
            "Vocals" to 0.58f
        )
        MusicGenre.HIP_HOP -> listOf(
            "Energy" to 0.78f,
            "Bass" to (0.86f + jitter).coerceIn(0.4f, 0.98f),
            "Tempo" to 0.70f,
            "Vocals" to 0.82f
        )
        MusicGenre.MELODIES, MusicGenre.BOLLYWOOD, MusicGenre.DEVOTIONAL -> listOf(
            "Energy" to 0.52f,
            "Bass" to 0.44f,
            "Tempo" to 0.48f,
            "Vocals" to (0.90f + jitter).coerceIn(0.4f, 0.98f)
        )
        MusicGenre.LO_FI, MusicGenre.INDIE -> listOf(
            "Energy" to 0.40f,
            "Bass" to 0.55f,
            "Tempo" to 0.38f,
            "Vocals" to 0.68f
        )
        else -> listOf(
            "Energy" to (0.72f + jitter).coerceIn(0.4f, 0.96f),
            "Bass" to 0.64f,
            "Tempo" to 0.68f,
            "Vocals" to 0.78f
        )
    }
}
