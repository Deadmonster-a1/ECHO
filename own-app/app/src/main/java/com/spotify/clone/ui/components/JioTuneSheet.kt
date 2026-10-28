package com.spotify.clone.ui.components

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.RingVolume
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.spotify.clone.core.data.FullTrackStreamResolver
import com.spotify.clone.core.designsystem.LocalAppTheme
import com.spotify.clone.core.download.TrackDownloadManager
import com.spotify.clone.core.model.Track
import com.spotify.clone.core.ringtone.RingtoneResult
import com.spotify.clone.core.ringtone.RingtoneStudioManager
import com.spotify.clone.core.ringtone.RingtoneTarget
import com.spotify.clone.core.ringtone.SnippetPreviewPlayer
import kotlinx.coroutines.launch

private data class SongPreset(
    val id: String,
    val name: String,
    val startPercent: Float
)

/**
 * Custom Ringtone Studio & JioTune Manager.
 * - Interactive custom time adjustment slider (e.g. 0:45 - 1:15)
 * - Preset chorus selectors (Opening Vocals, Main Hook, Dance Drop, Custom)
 * - Live snippet preview player with bounded playback
 * - Lossless export & set as: Phone Ringtone, Notification Tone, Alarm Tone, JioTune
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JioTuneSheet(
    track: Track,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activeTheme = LocalAppTheme.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val ringtoneManager = remember { RingtoneStudioManager.getInstance(context) }
    val downloadManager = remember { TrackDownloadManager.getInstance(context) }
    val previewPlayer = remember { SnippetPreviewPlayer(context) }

    val isPreviewPlaying by previewPlayer.isPlaying.collectAsState()
    val previewPositionMs by previewPlayer.currentPositionMs.collectAsState()

    DisposableEffect(Unit) {
        onDispose {
            previewPlayer.stop()
        }
    }

    val totalDurationMs = remember(track.id) {
        if (track.durationMs > 15_000L) track.durationMs else 210_000L // default 3:30 min
    }

    var snippetDurationSec by remember { mutableIntStateOf(30) }
    val snippetDurationMs = snippetDurationSec * 1000L

    val maxStartMs = (totalDurationMs - snippetDurationMs).coerceAtLeast(0L)

    val presets = remember(totalDurationMs) {
        listOf(
            SongPreset("hook_main", "Main Chorus", 0.28f),
            SongPreset("hook_intro", "Opening Vocals", 0.05f),
            SongPreset("hook_drop", "Dance Drop", 0.48f),
            SongPreset("hook_custom", "Custom Range", -1f)
        )
    }

    var selectedPresetId by remember { mutableStateOf("hook_main") }
    var startMs by remember { mutableLongStateOf((maxStartMs * 0.28f).toLong()) }
    val endMs = (startMs + snippetDurationMs).coerceAtMost(totalDurationMs)

    var isProcessing by remember { mutableStateOf(false) }
    var processingLabel by remember { mutableStateOf("") }

    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    val sheetBg = if (isDark) Color(0xFF13121A) else Color(0xFFF9F7F4)
    val cardBg = if (isDark) Color(0xFF1B1A24) else Color(0xFFFFFFFF)
    val cardBorder = if (isDark) Color(0xFF2C2A39) else Color(0xFFE5E2DC)
    val textPrimary = if (isDark) Color(0xFFF6F3FA) else Color(0xFF17171C)
    val textSecondary = if (isDark) Color(0xFFA6A2B3) else Color(0xFF777680)

    fun executeRingtoneAction(target: RingtoneTarget) {
        previewPlayer.stop()
        isProcessing = true
        processingLabel = "Preparing ${target.displayName}..."
        scope.launch {
            val result = ringtoneManager.trimAndSaveRingtone(
                track = track,
                startMs = startMs,
                endMs = endMs,
                target = target
            )
            isProcessing = false
            Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()

            if (result.requiresWriteSettings) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                } catch (ignored: Exception) {
                    try {
                        val soundIntent = Intent(Settings.ACTION_SOUND_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(soundIntent)
                    } catch (e: Exception) {}
                }
            }

            if (result.success) {
                onDismiss()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = sheetBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .padding(bottom = 36.dp)
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
                        .background(activeTheme.primaryColor.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.RingVolume,
                        contentDescription = null,
                        tint = activeTheme.primaryColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Ringtone Studio & JioTune",
                        color = textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Customize song snippet & set as phone ringtone",
                        color = textSecondary,
                        fontSize = 12.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Track summary card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(cardBg)
                    .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = track.coverArtUrl,
                        contentDescription = track.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.title,
                            color = textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${track.artistName} • ${track.albumName}",
                            color = textSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(activeTheme.primaryColor.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${snippetDurationSec}s Slice",
                            color = activeTheme.primaryColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Section 1: Snippet Duration Pills (15s, 30s, 45s)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "SNIPPET DURATION",
                    color = textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(15, 30, 45).forEach { dur ->
                        val isSelected = snippetDurationSec == dur
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) activeTheme.primaryColor else cardBorder.copy(alpha = 0.4f))
                                .clickable {
                                    snippetDurationSec = dur
                                    previewPlayer.stop()
                                    val newMax = (totalDurationMs - (dur * 1000L)).coerceAtLeast(0L)
                                    if (startMs > newMax) startMs = newMax
                                }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${dur}s",
                                color = if (isSelected) Color.White else textPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Section 2: Preset Hooks Row
            Text(
                text = "POPULAR HOOK PRESETS",
                color = textSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.forEach { preset ->
                    val isSelected = selectedPresetId == preset.id
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) activeTheme.primaryColor.copy(alpha = 0.14f) else cardBg)
                            .border(
                                1.dp,
                                if (isSelected) activeTheme.primaryColor else cardBorder,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                selectedPresetId = preset.id
                                previewPlayer.stop()
                                if (preset.startPercent >= 0f) {
                                    val newMax = (totalDurationMs - snippetDurationMs).coerceAtLeast(0L)
                                    startMs = (newMax * preset.startPercent).toLong()
                                }
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = preset.name,
                            color = if (isSelected) activeTheme.primaryColor else textPrimary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 11.5.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section 3: Time Slider Adjustment Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(cardBg)
                    .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "START: ${formatTime(startMs)}",
                            color = activeTheme.primaryColor,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )

                        Text(
                            text = "${snippetDurationSec}s Snippet",
                            color = textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            text = "END: ${formatTime(endMs)}",
                            color = activeTheme.primaryColor,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Slider to adjust start position
                    Slider(
                        value = if (maxStartMs > 0) startMs.toFloat() / maxStartMs else 0f,
                        onValueChange = { frac ->
                            startMs = (frac * maxStartMs).toLong()
                            selectedPresetId = "hook_custom"
                            if (isPreviewPlaying) {
                                previewPlayer.stop()
                            }
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = activeTheme.primaryColor,
                            activeTrackColor = activeTheme.primaryColor,
                            inactiveTrackColor = cardBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("0:00", color = textSecondary, fontSize = 11.sp)
                        Text("Drag slider to trim any part of song", color = textSecondary, fontSize = 11.sp)
                        Text(formatTime(totalDurationMs), color = textSecondary, fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section 4: Live Snippet Preview Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                activeTheme.primaryColor.copy(alpha = if (isPreviewPlaying) 0.25f else 0.12f),
                                activeTheme.gradientEnd.copy(alpha = if (isPreviewPlaying) 0.20f else 0.08f)
                            )
                        )
                    )
                    .border(
                        1.dp,
                        if (isPreviewPlaying) activeTheme.primaryColor else activeTheme.primaryColor.copy(alpha = 0.35f),
                        RoundedCornerShape(14.dp)
                    )
                    .clickable {
                        if (isPreviewPlaying) {
                            previewPlayer.stop()
                        } else {
                            scope.launch {
                                val localFile = downloadManager.getDownloadedTrackFile(track.id)
                                val playPath = if (localFile != null && localFile.exists()) {
                                    localFile.absolutePath
                                } else {
                                    val resolved = FullTrackStreamResolver.resolveStreamUrl(
                                        track.title,
                                        track.artistName,
                                        track.albumName
                                    )
                                    resolved ?: track.mediaUri
                                }

                                if (playPath.isNotBlank()) {
                                    previewPlayer.playSnippet(playPath, startMs, endMs)
                                } else {
                                    Toast.makeText(context, "Resolving preview audio...", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                    .padding(vertical = 12.dp, horizontal = 16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isPreviewPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = "Preview",
                                tint = activeTheme.primaryColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isPreviewPlaying) "Playing Snippet (${formatTime(startMs)} - ${formatTime(endMs)})"
                                else "Preview Snippet (${formatTime(startMs)} - ${formatTime(endMs)})",
                                color = textPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp
                            )
                        }

                        if (isPreviewPlaying) {
                            Text(
                                text = formatTime(previewPositionMs),
                                color = activeTheme.primaryColor,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.5.sp
                            )
                        }
                    }

                    if (isPreviewPlaying) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val progress = if (snippetDurationMs > 0) {
                            ((previewPositionMs - startMs).toFloat() / snippetDurationMs).coerceIn(0f, 1f)
                        } else 0f
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = activeTheme.primaryColor,
                            trackColor = cardBorder
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (isProcessing) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = activeTheme.primaryColor,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = processingLabel,
                        color = textPrimary,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                // Section 5: Action Buttons
                // Button 1: Set as Phone Ringtone
                ElevatedButton(
                    onClick = { executeRingtoneAction(RingtoneTarget.PHONE_RINGTONE) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = activeTheme.primaryColor,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Filled.MusicNote, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Set as Phone Ringtone (${formatTime(startMs)} - ${formatTime(endMs)})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Button 2 & 3: Notification and Alarm in 2 columns
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { executeRingtoneAction(RingtoneTarget.NOTIFICATION_SOUND) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Notification Tone", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = { executeRingtoneAction(RingtoneTarget.ALARM_SOUND) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.Alarm, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Alarm Sound", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Button 4: JioTune Activation
                OutlinedButton(
                    onClick = { executeRingtoneAction(RingtoneTarget.JIO_CALLER_TUNE) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF0078FF)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Activate as JioTune / Caller Tune", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                }
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
