package com.spotify.clone.ui.components

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spotify.clone.core.designsystem.SpotifyDarkGray
import com.spotify.clone.core.designsystem.SpotifyGreen
import com.spotify.clone.core.designsystem.SpotifyLightGray
import com.spotify.clone.core.designsystem.SpotifyTextMuted
import com.spotify.clone.core.designsystem.SpotifyTextSecondary
import com.spotify.clone.core.designsystem.SpotifyWhite

data class EqPreset(
    val name: String,
    val bands: List<Float>
)

val EQ_PRESETS = listOf(
    EqPreset("Normal", listOf(0f, 0f, 0f, 0f, 0f)),
    EqPreset("Bass Booster", listOf(6f, 4f, 1f, 0f, 0f)),
    EqPreset("Bass Reducer", listOf(-6f, -4f, -1f, 0f, 0f)),
    EqPreset("Vocal Booster", listOf(-2f, 0f, 5f, 4f, -1f)),
    EqPreset("Treble Booster", listOf(0f, 0f, 0f, 4f, 7f)),
    EqPreset("Rock", listOf(5f, 3f, -1f, 3f, 5f)),
    EqPreset("Pop", listOf(-1f, 2f, 5f, 2f, -2f)),
    EqPreset("Electronic", listOf(4f, 3f, 0f, 2f, 4f)),
    EqPreset("Jazz", listOf(3f, 2f, -1f, 2f, 4f)),
    EqPreset("Classical", listOf(4f, 3f, 0f, 3f, 4f))
)

val FREQ_LABELS = listOf("60 Hz", "230 Hz", "910 Hz", "3.6 kHz", "14 kHz")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerBottomSheet(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val prefs: SharedPreferences = remember {
        context.getSharedPreferences("spotify_eq_prefs", Context.MODE_PRIVATE)
    }

    var isEqEnabled by remember {
        mutableStateOf(prefs.getBoolean("eq_enabled", true))
    }
    var selectedPresetName by remember {
        mutableStateOf(prefs.getString("eq_preset", "Normal") ?: "Normal")
    }

    val bandLevels = remember {
        mutableStateListOf<Float>().apply {
            val preset = EQ_PRESETS.find { it.name == selectedPresetName } ?: EQ_PRESETS.first()
            preset.bands.forEachIndexed { i, defaultVal ->
                add(prefs.getFloat("band_$i", defaultVal))
            }
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SpotifyDarkGray
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.GraphicEq,
                        contentDescription = "Equalizer",
                        tint = if (isEqEnabled) SpotifyGreen else SpotifyTextMuted,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Equalizer",
                        color = SpotifyWhite,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Switch(
                    checked = isEqEnabled,
                    onCheckedChange = { enabled ->
                        isEqEnabled = enabled
                        prefs.edit().putBoolean("eq_enabled", enabled).apply()
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = SpotifyGreen,
                        checkedTrackColor = SpotifyGreen.copy(alpha = 0.4f),
                        uncheckedThumbColor = SpotifyLightGray,
                        uncheckedTrackColor = SpotifyDarkGray
                    )
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Presets Horizontal Row
            Text(
                text = "Presets",
                color = SpotifyTextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EQ_PRESETS.forEach { preset ->
                    val isSelected = selectedPresetName == preset.name
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isSelected && isEqEnabled) SpotifyGreen
                                else SpotifyLightGray.copy(alpha = 0.3f)
                            )
                            .clickable(enabled = isEqEnabled) {
                                selectedPresetName = preset.name
                                preset.bands.forEachIndexed { i, v ->
                                    if (i < bandLevels.size) {
                                        bandLevels[i] = v
                                        prefs.edit().putFloat("band_$i", v).apply()
                                    }
                                }
                                prefs.edit().putString("eq_preset", preset.name).apply()
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = preset.name,
                            color = if (isSelected && isEqEnabled) Color.Black else SpotifyWhite,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 5 Band Sliders
            Text(
                text = "Frequency Bands",
                color = SpotifyTextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            FREQ_LABELS.forEachIndexed { index, label ->
                val level = bandLevels.getOrElse(index) { 0f }
                val displayDb = when {
                    level > 0 -> "+${level.toInt()} dB"
                    level < 0 -> "${level.toInt()} dB"
                    else -> "0 dB"
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        color = if (isEqEnabled) SpotifyWhite else SpotifyTextMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.width(68.dp)
                    )

                    Slider(
                        value = level,
                        onValueChange = { newLevel ->
                            bandLevels[index] = newLevel
                            selectedPresetName = "Custom"
                            prefs.edit()
                                .putFloat("band_$index", newLevel)
                                .putString("eq_preset", "Custom")
                                .apply()
                        },
                        valueRange = -12f..12f,
                        enabled = isEqEnabled,
                        colors = SliderDefaults.colors(
                            thumbColor = if (isEqEnabled) SpotifyGreen else SpotifyTextMuted,
                            activeTrackColor = if (isEqEnabled) SpotifyGreen else SpotifyTextMuted,
                            inactiveTrackColor = SpotifyLightGray.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = displayDb,
                        color = if (isEqEnabled) SpotifyTextSecondary else SpotifyTextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.width(48.dp)
                    )
                }
            }
        }
    }
}
