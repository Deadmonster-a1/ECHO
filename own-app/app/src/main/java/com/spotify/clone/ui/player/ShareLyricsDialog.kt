package com.spotify.clone.ui.player

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.spotify.clone.core.designsystem.ArtworkColors
import com.spotify.clone.core.designsystem.EchoRadius
import com.spotify.clone.core.designsystem.EchoSpacing
import com.spotify.clone.core.model.LyricLine
import com.spotify.clone.core.model.Track

/**
 * Social Lyrics Quote Card Generator & Sharing Dialog for ECHO.
 * Direct architectural adoption from LastWave-Native ShareLyricsDialog.
 */
@Composable
fun ShareLyricsDialog(
    track: Track,
    lines: List<LyricLine>,
    activeLineIndex: Int,
    artworkColors: ArtworkColors,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val selectedIndices = remember {
        mutableStateListOf<Int>().apply {
            if (activeLineIndex in lines.indices) {
                add(activeLineIndex)
            } else if (lines.isNotEmpty()) {
                add(0)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Share Lyrics Card",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(EchoSpacing.minTouchTarget)) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Preview Card
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF14121E)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(12.dp, RoundedCornerShape(22.dp), spotColor = artworkColors.ambientGlow)
                        .border(BorderStroke(1.dp, artworkColors.vibrantAccent.copy(alpha = 0.45f)), RoundedCornerShape(22.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        artworkColors.backgroundTop,
                                        artworkColors.backgroundMid,
                                        Color(0xFF0E0C16)
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column {
                            // Track Info Banner
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AsyncImage(
                                    model = track.coverArtUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = track.title,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = track.artistName,
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Selected Lyrics Quote
                            val quoteLines = selectedIndices.sorted().mapNotNull { lines.getOrNull(it)?.words }
                            Text(
                                text = if (quoteLines.isEmpty()) "Select lines to share" else quoteLines.joinToString("\n"),
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 24.sp,
                                textAlign = TextAlign.Start
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // Watermark
                            Text(
                                text = "echo by GRAVIT • Feel the sound",
                                color = artworkColors.vibrantAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Tap to select up to 4 lines (${selectedIndices.size}/4)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Lyric Line Selection List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                ) {
                    itemsIndexed(lines) { idx, line ->
                        val isSelected = idx in selectedIndices
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
                                .clickable {
                                    if (isSelected) {
                                        selectedIndices.remove(idx)
                                    } else if (selectedIndices.size < 4) {
                                        selectedIndices.add(idx)
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = line.words,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Share Action Button
                Button(
                    onClick = {
                        val quote = selectedIndices.sorted().mapNotNull { lines.getOrNull(it)?.words }.joinToString("\n")
                        val shareText = "\"$quote\"\n\n— ${track.title} by ${track.artistName} (via echo by GRAVIT)"
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share Lyrics Card"))
                        onDismiss()
                    },
                    enabled = selectedIndices.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = artworkColors.vibrantAccent,
                        contentColor = com.spotify.clone.core.designsystem.echoContrastOn(artworkColors.vibrantAccent)
                    )
                ) {
                    Icon(imageVector = Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Share Quote", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
