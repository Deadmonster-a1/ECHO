package com.spotify.clone.ui.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spotify.clone.core.designsystem.tabularFigures
import kotlin.math.roundToLong

/**
 * Lyrics Synchronization Offset Calibration Dialog for ECHO.
 * Directly adopted from LastWave-Native LyricsOffsetDialog.
 * Allows users to nudge lyrics timestamps forward or backward to achieve bit-perfect sync.
 */
@Composable
fun LyricsOffsetDialog(
    currentOffsetMs: Long,
    onOffsetChanged: (Long) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    var draftOffsetMs by remember(currentOffsetMs) { mutableLongStateOf(currentOffsetMs) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Timer,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Lyrics Sync Offset",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Nudges timestamps when a provider's lyrics run early or late. + displays lyrics earlier, − delays them.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                Text(
                    text = if (draftOffsetMs == 0L) "0 ms (Original Sync)" else "${if (draftOffsetMs > 0) "+" else ""}$draftOffsetMs ms",
                    color = primaryColor,
                    style = MaterialTheme.typography.headlineMedium.tabularFigures().copy(fontWeight = FontWeight.Bold)
                )

                Slider(
                    value = draftOffsetMs.toFloat().coerceIn(-10000f, 10000f),
                    onValueChange = {
                        val rounded = it.roundToLong()
                        draftOffsetMs = rounded
                        onOffsetChanged(rounded)
                    },
                    valueRange = -10000f..10000f,
                    colors = SliderDefaults.colors(
                        thumbColor = primaryColor,
                        activeTrackColor = primaryColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick Increment / Decrement Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            draftOffsetMs = (draftOffsetMs - 1000L).coerceIn(-10000L, 10000L)
                            onOffsetChanged(draftOffsetMs)
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Text("−1.0s", fontSize = 11.sp, maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = {
                            draftOffsetMs = (draftOffsetMs - 250L).coerceIn(-10000L, 10000L)
                            onOffsetChanged(draftOffsetMs)
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Text("−250ms", fontSize = 11.sp, maxLines = 1)
                    }

                    FilledTonalButton(
                        onClick = {
                            draftOffsetMs = 0L
                            onOffsetChanged(0L)
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.RestartAlt,
                            contentDescription = "Reset",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset", fontSize = 11.sp, maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = {
                            draftOffsetMs = (draftOffsetMs + 250L).coerceIn(-10000L, 10000L)
                            onOffsetChanged(draftOffsetMs)
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Text("+250ms", fontSize = 11.sp, maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = {
                            draftOffsetMs = (draftOffsetMs + 1000L).coerceIn(-10000L, 10000L)
                            onOffsetChanged(draftOffsetMs)
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Text("+1.0s", fontSize = 11.sp, maxLines = 1)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = primaryColor, fontWeight = FontWeight.Bold)
            }
        }
    )
}
