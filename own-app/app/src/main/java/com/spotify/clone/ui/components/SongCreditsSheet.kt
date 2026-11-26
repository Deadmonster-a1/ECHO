package com.spotify.clone.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.spotify.clone.core.designsystem.EchoRadius
import com.spotify.clone.core.designsystem.LocalAppTheme
import com.spotify.clone.core.designsystem.ThemeManager
import com.spotify.clone.core.designsystem.resolvedBorder
import com.spotify.clone.core.designsystem.resolvedCard
import com.spotify.clone.core.model.Track

/**
 * Song Credits & Studio Metadata Sheet (Inspired by Apple Music & Tidal):
 * Displays detailed acoustic engineering, performance, writing, and production credits.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongCreditsSheet(
    track: Track,
    onDismiss: () -> Unit
) {
    val activeTheme = LocalAppTheme.current
    val isDarkMode by ThemeManager.isDarkMode.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val sheetBg = MaterialTheme.colorScheme.surface
    val cardBg = activeTheme.resolvedCard(isDarkMode)
    val cardBorder = activeTheme.resolvedBorder(isDarkMode)
    val textPrimary = MaterialTheme.colorScheme.onSurface
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = EchoRadius.xl, topEnd = EchoRadius.xl),
        containerColor = sheetBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(activeTheme.primaryColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = activeTheme.primaryColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Song Credits & Liner Notes",
                        color = textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Track metadata & playback source",
                        color = textSecondary,
                        fontSize = 12.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Track Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(cardBg)
                    .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = track.coverArtUrl,
                        contentDescription = track.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.title,
                            color = textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = track.artistName,
                            color = activeTheme.primaryColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = track.albumName,
                            color = textSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            val sourceLabel = when {
                track.id.startsWith("local_") || track.mediaUri.startsWith("content://") -> "Local Device Audio"
                track.mediaUri.startsWith("file://") -> "Downloaded Offline"
                track.id.startsWith("saavn_") || track.mediaUri.contains("saavn", ignoreCase = true) -> "JioSaavn Stream"
                track.id.startsWith("audius_") || track.mediaUri.contains("audius", ignoreCase = true) -> "Audius Network"
                else -> "Cloud Stream"
            }
            val formatLabel = when {
                track.mediaUri.contains(".m4a", ignoreCase = true) || track.mediaUri.contains(".mp4", ignoreCase = true) -> "AAC / MP4"
                track.mediaUri.contains(".mp3", ignoreCase = true) -> "MP3"
                track.mediaUri.startsWith("content://") -> "On-Device MediaStore"
                track.mediaUri.isBlank() -> "Pending Resolution"
                else -> "Adaptive Stream"
            }
            val durationLabel = if (track.durationMs > 0L) {
                val totalSec = track.durationMs / 1000L
                String.format(java.util.Locale.US, "%d:%02d", totalSec / 60L, totalSec % 60L)
            } else {
                "Unknown"
            }
            val syncLabel = when (track.lyricsSyncType) {
                com.spotify.clone.core.model.LyricsSyncType.SYLLABLE_SYNCED -> "Syllable-Synced"
                com.spotify.clone.core.model.LyricsSyncType.LINE_SYNCED -> "Line-Synced"
                com.spotify.clone.core.model.LyricsSyncType.UNSYNCED -> "Unsynced Text"
                com.spotify.clone.core.model.LyricsSyncType.NONE -> "Remote Lookup / Unavailable"
            }

            CreditSection("PERFORMED BY", listOf(
                "Artist(s)" to track.artistName.ifBlank { "Unknown Artist" },
                "Album / Release" to track.albumName.ifBlank { "Single" },
                "Content Rating" to if (track.isExplicit) "Explicit" else "Clean"
            ), textPrimary, textSecondary)

            CreditSection("SOURCE & PLAYBACK", listOf(
                "Stream Source" to sourceLabel,
                "Audio Format" to formatLabel,
                "Duration" to durationLabel,
                "Track ID" to track.id
            ), textPrimary, textSecondary)

            CreditSection("LYRICS & METADATA", listOf(
                "Lyrics Sync" to syncLabel,
                "Extended Credits" to "Not provided by source"
            ), textPrimary, textSecondary)
        }
    }
}

@Composable
private fun CreditSection(
    title: String,
    items: List<Pair<String, String>>,
    textPrimary: Color,
    textSecondary: Color
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            text = title,
            color = textSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        items.forEach { (role, name) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = role,
                    color = textSecondary,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = name,
                    color = textPrimary,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = textSecondary.copy(alpha = 0.15f))
    }
}
