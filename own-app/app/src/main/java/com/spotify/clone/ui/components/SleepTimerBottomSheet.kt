package com.spotify.clone.ui.components

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bedtime
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spotify.clone.core.designsystem.EchoAccentCoral
import com.spotify.clone.core.designsystem.EchoRadius
import com.spotify.clone.core.designsystem.ThemeManager
import com.spotify.clone.core.designsystem.resolvedBorder
import com.spotify.clone.core.designsystem.tabularFigures
import com.spotify.clone.core.player.MusicController
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimerBottomSheet(
    musicController: MusicController,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currentTheme by ThemeManager.currentTheme.collectAsState()
    val isDarkMode by ThemeManager.isDarkMode.collectAsState()

    val screenBg = MaterialTheme.colorScheme.surface
    val textPrimary = MaterialTheme.colorScheme.onSurface
    val dividerColor = currentTheme.resolvedBorder(isDarkMode)

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val remainingSeconds by musicController.sleepTimerRemainingSeconds.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = EchoRadius.xl, topEnd = EchoRadius.xl),
        containerColor = screenBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 36.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Bedtime,
                    contentDescription = null,
                    tint = currentTheme.primaryColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Stop audio in",
                    color = textPrimary,
                    style = MaterialTheme.typography.headlineMedium
                )
            }

            if (remainingSeconds != null) {
                val mins = remainingSeconds!! / 60
                val secs = remainingSeconds!! % 60
                Text(
                    text = "Timer active: ${String.format(Locale.US, "%02d:%02d", mins, secs)} remaining",
                    color = currentTheme.primaryColor,
                    style = MaterialTheme.typography.labelLarge.tabularFigures(),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                )
            }

            HorizontalDivider(
                color = dividerColor,
                thickness = 0.6.dp,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            TimerOptionRow(
                label = "End of track",
                textColor = textPrimary,
                onClick = {
                    musicController.setSleepTimerEndOfTrack()
                    Toast.makeText(context, "Sleep timer set: Audio will pause at end of track", Toast.LENGTH_SHORT).show()
                    onDismiss()
                }
            )

            val timerOptions = listOf(5, 10, 15, 30, 45, 60)
            timerOptions.forEach { minutes ->
                TimerOptionRow(
                    label = "$minutes minutes",
                    textColor = textPrimary,
                    onClick = {
                        musicController.setSleepTimer(minutes)
                        Toast.makeText(context, "Sleep timer set for $minutes minutes", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }
                )
            }

            if (remainingSeconds != null) {
                HorizontalDivider(
                    color = dividerColor,
                    thickness = 0.6.dp,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
                TimerOptionRow(
                    label = "Turn off timer",
                    textColor = EchoAccentCoral,
                    onClick = {
                        musicController.cancelSleepTimer()
                        Toast.makeText(context, "Sleep timer turned off", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
private fun TimerOptionRow(
    label: String,
    textColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = textColor,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium)
        )
    }
}
