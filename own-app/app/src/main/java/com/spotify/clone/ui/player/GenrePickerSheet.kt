package com.spotify.clone.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spotify.clone.core.designsystem.EchoFilterChip
import com.spotify.clone.core.designsystem.EchoInsetCard
import com.spotify.clone.core.designsystem.EchoRadius
import com.spotify.clone.core.designsystem.EchoTonalIconBadge
import com.spotify.clone.core.designsystem.echoPressable
import com.spotify.clone.core.model.MusicGenre
import com.spotify.clone.core.player.MusicController

/**
 * Apple-style Same-Genre Radio & Spotify Queue Ordering Sheet.
 * Allows toggling Auto-Match Current Song's Genre or locking playback to any genre.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GenrePickerBottomSheet(
    musicController: MusicController,
    onDismiss: () -> Unit
) {
    val activeLock by musicController.activeGenreLock.collectAsState()
    val autoMatch by musicController.autoMatchGenre.collectAsState()
    val playbackState by musicController.playbackState.collectAsState()
    val currentTrack = playbackState.currentTrack
    val inferredGenre = currentTrack?.let { MusicGenre.inferGenre(it) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val primaryColor = MaterialTheme.colorScheme.primary
    val textPrimary = MaterialTheme.colorScheme.onSurface
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = EchoRadius.xl, topEnd = EchoRadius.xl),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EchoTonalIconBadge(icon = Icons.Filled.Radio)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Same-Genre Radio",
                        color = textPrimary,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Keeps queue in one genre with balanced order",
                        color = textSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            EchoInsetCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-Match Song Genre",
                            color = textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (inferredGenre != null) {
                                "Detected: ${inferredGenre.displayName}"
                            } else {
                                "Automatically match the current track's genre"
                            },
                            color = textSecondary,
                            fontSize = 12.5.sp
                        )
                    }
                    Switch(
                        checked = autoMatch && activeLock == MusicGenre.ALL,
                        onCheckedChange = { enabled ->
                            musicController.setAutoMatchGenre(enabled)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = primaryColor
                        )
                    )
                }

                if (inferredGenre != null) {
                    val activeTrack = currentTrack!!
                    HorizontalDivider(color = borderColor, thickness = 0.6.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .echoPressable(scaleOnPress = 1.0f, hapticOnClick = true) {
                                musicController.playSameGenreRadio(activeTrack, inferredGenre)
                                onDismiss()
                            }
                            .padding(horizontal = 16.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Start ${inferredGenre.shortLabel} Radio from \"${activeTrack.title}\"",
                            color = primaryColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "LOCK & PLAY GENRE STATION",
                color = textSecondary,
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.7.sp)
            )
            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MusicGenre.entries.forEach { genre ->
                    EchoFilterChip(
                        text = genre.displayName,
                        isSelected = activeLock == genre,
                        onClick = {
                            if (genre == MusicGenre.ALL) {
                                musicController.setAutoMatchGenre(true)
                            } else {
                                musicController.playGenreRadio(genre)
                            }
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}
