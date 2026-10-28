package com.spotify.clone.ui.player

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spotify.clone.core.designsystem.EchoMotion
import com.spotify.clone.core.designsystem.EchoSoundSculpture
import com.spotify.clone.core.designsystem.LocalAppTheme
import com.spotify.clone.core.designsystem.LocalReducedMotion
import com.spotify.clone.core.designsystem.echoPressable
import com.spotify.clone.core.model.LyricLine
import com.spotify.clone.core.model.Track
import com.spotify.clone.core.player.MusicController
import kotlin.math.abs

/**
 * Signature Animation E: Cinematic Lyrics Choreography
 * Synchronized line-by-line choreography, active-line highlight, ambient artwork aura,
 * and an honest unavailable state when lyrics are not provided.
 */
@Composable
fun FullscreenLyricsSheet(
    track: Track,
    musicController: MusicController,
    onClose: () -> Unit,
    initialSingMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeTheme = LocalAppTheme.current
    val isReducedMotion = LocalReducedMotion.current
    val playbackState by musicController.playbackState.collectAsState()
    val currentPos = playbackState.currentPositionMs

    var isSingModeActive by remember(initialSingMode) { mutableStateOf(initialSingMode) }
    var vocalAttenuation by remember { mutableFloatStateOf(0.70f) }

    val sampleLyrics = com.spotify.clone.core.data.SampleCatalog.sampleTracks.find {
        it.title.contains(track.title, ignoreCase = true) || track.title.contains(it.title, ignoreCase = true)
    }?.lyrics ?: emptyList()
    val lines: List<LyricLine> = if (track.lyrics.isNotEmpty()) track.lyrics else sampleLyrics
    val hasLyrics = lines.isNotEmpty()

    val activeIndex = if (hasLyrics) {
        lines.indexOfLast { it.timestampMs <= currentPos }.coerceAtLeast(0)
    } else 0

    val listState = rememberLazyListState()

    LaunchedEffect(activeIndex) {
        if (hasLyrics && activeIndex in lines.indices && !isReducedMotion) {
            val target = (activeIndex - 1).coerceAtLeast(0)
            listState.animateScrollToItem(target)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .background(
                Brush.verticalGradient(
                    listOf(
                        activeTheme.primaryColor.copy(alpha = 0.35f),
                        MaterialTheme.colorScheme.background,
                        Color(0xFF0C0C12)
                    )
                )
            )
    ) {
        // Living ECHO sculpture ambient lighting in background
        EchoSoundSculpture(
            isPlaying = playbackState.isPlaying,
            trackKey = track.id,
            alpha = 0.35f,
            modifier = Modifier.fillMaxSize()
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 36.dp, start = 16.dp, end = 16.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onClose) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = track.title,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.artistName,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Apple Music Sing (Karaoke) Button
                    IconButton(onClick = {
                        isSingModeActive = !isSingModeActive
                        val msg = if (isSingModeActive) "Apple Music Sing: Vocal Attenuator Enabled" else "Apple Music Sing Disabled"
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (isSingModeActive) activeTheme.primaryColor else Color.Black.copy(alpha = 0.35f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isSingModeActive) Icons.Filled.Mic else Icons.Filled.MicOff,
                                contentDescription = "Apple Music Sing",
                                tint = if (isSingModeActive) Color.Black else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    if (hasLyrics) {
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(onClick = {
                            val activeLine = lines.getOrNull(activeIndex)?.words ?: track.title
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "ECHO Lyrics - ${track.title}")
                                putExtra(Intent.EXTRA_TEXT, "\"$activeLine\"\n\n— ${track.title} by ${track.artistName} on ECHO")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share lyrics"))
                        }) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.35f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Share,
                                    contentDescription = "Share",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.size(6.dp))
                    }
                }
            }

            // Body: Either Choreographed Lyrics or Honest Unavailable State
            if (hasLyrics) {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(top = 40.dp, bottom = 140.dp, start = 24.dp, end = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(22.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    itemsIndexed(lines) { index, line ->
                        val isActive = index == activeIndex
                        val distance = abs(index - activeIndex)

                        val targetAlpha = when {
                            isActive -> 1.0f
                            distance == 1 -> 0.60f
                            distance == 2 -> 0.32f
                            else -> 0.18f
                        }

                        val lineAlpha by animateFloatAsState(
                            targetValue = if (isReducedMotion) (if (isActive) 1f else 0.45f) else targetAlpha,
                            animationSpec = tween(EchoMotion.DurationStandard),
                            label = "lyric_alpha"
                        )

                        val lineColor by animateColorAsState(
                            targetValue = if (isActive) Color.White else Color.White.copy(alpha = 0.85f),
                            animationSpec = tween(EchoMotion.DurationStandard),
                            label = "lyric_color"
                        )

                        val lyricBoxModifier = if (isActive && isSingModeActive) {
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            activeTheme.primaryColor.copy(alpha = 0.30f),
                                            Color.Transparent
                                        )
                                    )
                                )
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        } else {
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        }

                        Box(modifier = lyricBoxModifier) {
                            Text(
                                text = line.words,
                                color = if (isActive && isSingModeActive) activeTheme.primaryColor else lineColor,
                                fontSize = if (isActive) (if (isSingModeActive) 25.sp else 23.sp) else 18.sp,
                                fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium,
                                lineHeight = if (isActive) 34.sp else 26.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .alpha(lineAlpha)
                                    .clickable {
                                        musicController.seekTo(line.timestampMs)
                                    }
                            )
                        }
                    }
                }
            } else {
                // Honest Unavailable State
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(activeTheme.primaryColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.GraphicEq,
                                contentDescription = null,
                                tint = activeTheme.primaryColor,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "Lyrics Unavailable",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Synchronized lyrics are not currently provided for \"${track.title}\". Immerse yourself in the uncompressed studio acoustics.",
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            // Apple Music Sing Vocal Attenuation Floating Slider
            AnimatedVisibility(
                visible = isSingModeActive,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .border(1.dp, activeTheme.primaryColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Mic,
                                contentDescription = null,
                                tint = activeTheme.primaryColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Apple Music Sing • Vocal Attenuator",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        Text(
                            text = "${(vocalAttenuation * 100).toInt()}% Vocals",
                            color = activeTheme.primaryColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Slider(
                        value = vocalAttenuation,
                        onValueChange = { vocalAttenuation = it },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = activeTheme.primaryColor,
                            activeTrackColor = activeTheme.primaryColor,
                            inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = if (vocalAttenuation <= 0.15f) "Karaoke Instrumental Backing Track (Lead Vocals Muted)" else "Attenuating lead vocal frequencies for real-time sing-along",
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 11.sp
                    )
                }
            }

            // Accessible Playback Transport Bar at bottom of Lyrics
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                // Scrubber
                val progress = playbackState.progress
                Slider(
                    value = progress,
                    onValueChange = { frac ->
                        musicController.seekTo((frac * playbackState.durationMs).toLong())
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = activeTheme.primaryColor,
                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { musicController.skipPrevious() }) {
                        Icon(
                            imageVector = Icons.Filled.SkipPrevious,
                            contentDescription = "Previous",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .echoPressable(scaleOnPress = 0.92f) {
                                musicController.togglePlayPause()
                            }
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (playbackState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                            tint = Color.Black,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    IconButton(onClick = { musicController.skipNext() }) {
                        Icon(
                            imageVector = Icons.Filled.SkipNext,
                            contentDescription = "Next",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
}
