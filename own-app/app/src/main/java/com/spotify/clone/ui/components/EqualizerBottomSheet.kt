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
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.spotify.clone.core.designsystem.ThemeManager

data class EqPreset(
    val name: String,
    val bands: List<Float>
)

val EQ_PRESETS = listOf(
    EqPreset("Normal", listOf(0f, 0f, 0f, 0f, 0f)),
    EqPreset("Harman AirPods Pro 2", listOf(2f, 1f, 0f, 2f, 3f)),
    EqPreset("Harman Sony XM5", listOf(-3f, -2f, 1f, 3f, 2f)),
    EqPreset("Harman HD650", listOf(4f, 1f, 0f, 1f, 2f)),
    EqPreset("Samsung Buds 2 Pro", listOf(1f, 0f, 1f, -1f, 2f)),
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
    val currentTheme by ThemeManager.currentTheme.collectAsState()
    val isDarkMode by ThemeManager.isDarkMode.collectAsState()

    val screenBg = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerLow
    val textPrimary = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
    val textSecondary = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
    val chipInactiveBg = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHigh
    val primaryAccent = androidx.compose.material3.MaterialTheme.colorScheme.primary
    val onAccent = androidx.compose.material3.MaterialTheme.colorScheme.onPrimary

    val prefs: SharedPreferences = remember {
        context.getSharedPreferences("spotify_eq_prefs", Context.MODE_PRIVATE)
    }

    val autoGenreEq by com.spotify.clone.core.settings.SettingsManager.autoGenreEq.collectAsState()
    val eqUpdateSignal by com.spotify.clone.core.settings.SettingsManager.eqUpdateSignal.collectAsState()

    var isEqEnabled by remember {
        mutableStateOf(prefs.getBoolean("eq_enabled", true))
    }
    var selectedPresetName by remember {
        mutableStateOf(
            if (autoGenreEq) prefs.getString("auto_eq_preset", null) ?: prefs.getString("eq_preset", "Normal") ?: "Normal"
            else prefs.getString("eq_preset", "Normal") ?: "Normal"
        )
    }

    val bandLevels = remember {
        mutableStateListOf<Float>().apply {
            val useAuto = autoGenreEq && prefs.contains("auto_eq_preset")
            val prefix = if (useAuto) "auto_band_" else "band_"
            val preset = EQ_PRESETS.find { it.name == selectedPresetName } ?: EQ_PRESETS.first()
            preset.bands.forEachIndexed { i, defaultVal ->
                add(prefs.getFloat("$prefix$i", defaultVal))
            }
        }
    }

    androidx.compose.runtime.LaunchedEffect(autoGenreEq, eqUpdateSignal) {
        val useAuto = autoGenreEq && prefs.contains("auto_eq_preset")
        val prefix = if (useAuto) "auto_band_" else "band_"
        val activePreset = if (useAuto) {
            prefs.getString("auto_eq_preset", "Auto") ?: "Auto"
        } else {
            prefs.getString("eq_preset", "Normal") ?: "Normal"
        }
        selectedPresetName = activePreset
        val fallback = EQ_PRESETS.find { it.name == activePreset } ?: EQ_PRESETS.first()
        for (i in 0 until bandLevels.size) {
            bandLevels[i] = prefs.getFloat("$prefix$i", fallback.bands.getOrElse(i) { 0f })
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = com.spotify.clone.core.designsystem.EchoRadius.xl, topEnd = com.spotify.clone.core.designsystem.EchoRadius.xl),
        containerColor = screenBg
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
                        tint = if (isEqEnabled) currentTheme.primaryColor else textSecondary,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Equalizer",
                        color = textPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                    Switch(
                        checked = isEqEnabled,
                        onCheckedChange = { enabled ->
                            isEqEnabled = enabled
                            prefs.edit().putBoolean("eq_enabled", enabled).apply()
                            com.spotify.clone.core.settings.SettingsManager.notifyEqualizerChanged()
                        },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = onAccent,
                        checkedTrackColor = currentTheme.primaryColor,
                        uncheckedThumbColor = textSecondary,
                        uncheckedTrackColor = chipInactiveBg
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(chipInactiveBg.copy(alpha = 0.7f))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Auto-Match EQ to Song Genre",
                        color = textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Dynamically adapts EQ curve as genres change",
                        color = textSecondary,
                        fontSize = 11.5.sp
                    )
                }
                Switch(
                    checked = autoGenreEq,
                    onCheckedChange = {
                        com.spotify.clone.core.settings.SettingsManager.setAutoGenreEq(it)
                        com.spotify.clone.core.settings.SettingsManager.notifyEqualizerChanged()
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = onAccent,
                        checkedTrackColor = currentTheme.primaryColor,
                        uncheckedThumbColor = textSecondary,
                        uncheckedTrackColor = chipInactiveBg
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Presets Horizontal Row
            Text(
                text = "Presets",
                color = textSecondary,
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
                                if (isSelected && isEqEnabled) currentTheme.primaryColor
                                else chipInactiveBg
                            )
                            .clickable(enabled = isEqEnabled) {
                                if (autoGenreEq) {
                                    com.spotify.clone.core.settings.SettingsManager.setAutoGenreEq(false)
                                }
                                selectedPresetName = preset.name
                                val editor = prefs.edit().putString("eq_preset", preset.name)
                                preset.bands.forEachIndexed { i, v ->
                                    if (i < bandLevels.size) {
                                        bandLevels[i] = v
                                        editor.putFloat("band_$i", v)
                                    }
                                }
                                editor.apply()
                                com.spotify.clone.core.settings.SettingsManager.notifyEqualizerChanged()
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = preset.name,
                            color = if (isSelected && isEqEnabled) onAccent else textPrimary,
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
                color = textSecondary,
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
                        color = if (isEqEnabled) textPrimary else textSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.width(68.dp)
                    )

                    Slider(
                        value = level,
                        onValueChange = { newLevel ->
                            if (autoGenreEq) {
                                com.spotify.clone.core.settings.SettingsManager.setAutoGenreEq(false)
                            }
                            bandLevels[index] = newLevel
                            selectedPresetName = "Custom"
                        },
                        onValueChangeFinished = {
                            val editor = prefs.edit().putString("eq_preset", "Custom")
                            bandLevels.forEachIndexed { i, v -> editor.putFloat("band_$i", v) }
                            editor.apply()
                            com.spotify.clone.core.settings.SettingsManager.notifyEqualizerChanged()
                        },
                        valueRange = -12f..12f,
                        enabled = isEqEnabled,
                        colors = SliderDefaults.colors(
                            thumbColor = if (isEqEnabled) currentTheme.primaryColor else textSecondary,
                            activeTrackColor = if (isEqEnabled) currentTheme.primaryColor else textSecondary,
                            inactiveTrackColor = chipInactiveBg
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = displayDb,
                        color = if (isEqEnabled) textSecondary else textSecondary.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.width(48.dp)
                    )
                }
            }
        }
    }
}
