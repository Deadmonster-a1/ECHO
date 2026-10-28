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
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import com.spotify.clone.core.designsystem.LocalAppTheme
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    val sheetBg = if (isDark) Color(0xFF14131D) else Color(0xFFF9F7F4)
    val cardBg = if (isDark) Color(0xFF1F1D2B) else Color(0xFFFFFFFF)
    val cardBorder = if (isDark) Color(0xFF2E2C3D) else Color(0xFFE6E2DC)
    val textPrimary = if (isDark) Color(0xFFF6F3FA) else Color(0xFF17171C)
    val textSecondary = if (isDark) Color(0xFFA6A2B3) else Color(0xFF777680)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
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
                        text = "Studio fidelity recording details",
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

            // Sections
            CreditSection("PERFORMED BY", listOf(
                "Lead Vocals" to track.artistName,
                "Backing Choir" to "ECHO Regional Vocal Ensemble"
            ), textPrimary, textSecondary)

            CreditSection("WRITTEN & COMPOSED BY", listOf(
                "Composer / Music Director" to (track.artistName.split(",").lastOrNull()?.trim() ?: "Anirudh Ravichander"),
                "Lyrics" to "Ramajogayya Sastry, Sri Mani",
                "Arranger" to "Studio String Orchestra"
            ), textPrimary, textSecondary)

            CreditSection("AUDIO & PRODUCTION", listOf(
                "Mastering Engineer" to "Bob Ludwig (Gateway Mastering)",
                "Mixing Engineer" to "Suren G. & Alok Punjabi",
                "Audio Codec" to "ALAC Lossless 24-bit / 48.0 kHz",
                "Audio Dynamic Range" to "DR13 • Studio Reference",
                "Spatial Sound" to "Dolby Atmos Binaural Mix"
            ), textPrimary, textSecondary)

            CreditSection("COPYRIGHT & RELEASE", listOf(
                "Record Label" to "T-Series / Aditya Music / Sony Music India",
                "ISRC" to "IN-T10-24-00982",
                "Release Year" to "2024 / 2025"
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
                Text(
                    text = name,
                    color = textPrimary,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Divider(color = textSecondary.copy(alpha = 0.15f))
    }
}
