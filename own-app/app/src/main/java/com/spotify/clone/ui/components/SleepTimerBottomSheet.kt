package com.spotify.clone.ui.components

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.sp
import com.spotify.clone.core.designsystem.SpotifyDarkGray
import com.spotify.clone.core.designsystem.SpotifyGreen
import com.spotify.clone.core.designsystem.SpotifyLightGray
import com.spotify.clone.core.designsystem.SpotifyTextSecondary
import com.spotify.clone.core.designsystem.SpotifyWhite
import com.spotify.clone.core.player.MusicController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimerBottomSheet(
    musicController: MusicController,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val remainingSeconds by musicController.sleepTimerRemainingSeconds.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SpotifyDarkGray
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
                    tint = SpotifyGreen,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Stop audio in",
                    color = SpotifyWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (remainingSeconds != null) {
                val mins = remainingSeconds!! / 60
                val secs = remainingSeconds!! % 60
                Text(
                    text = "Timer active: ${String.format("%02d:%02d", mins, secs)} remaining",
                    color = SpotifyGreen,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                )
            }

            Divider(
                color = SpotifyLightGray.copy(alpha = 0.2f),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            val timerOptions = listOf(5, 10, 15, 30, 45, 60)
            timerOptions.forEach { minutes ->
                TimerOptionRow(
                    label = "$minutes minutes",
                    onClick = {
                        musicController.setSleepTimer(minutes)
                        Toast.makeText(context, "Sleep timer set for $minutes minutes", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }
                )
            }

            if (remainingSeconds != null) {
                Divider(
                    color = SpotifyLightGray.copy(alpha = 0.2f),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
                TimerOptionRow(
                    label = "Turn off timer",
                    textColor = Color(0xFFFF5252),
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
    textColor: Color = SpotifyWhite,
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
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
