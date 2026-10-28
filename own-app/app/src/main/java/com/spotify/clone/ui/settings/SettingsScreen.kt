package com.spotify.clone.ui.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.text.style.TextOverflow
import com.spotify.clone.core.update.AppUpdateManager
import com.spotify.clone.core.update.UpdateInfo
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.spotify.clone.core.designsystem.AppThemeOption
import com.spotify.clone.core.designsystem.EchoMotion
import com.spotify.clone.core.designsystem.LocalAppTheme
import com.spotify.clone.core.designsystem.ThemeManager
import com.spotify.clone.core.designsystem.echoPressable
import com.spotify.clone.core.settings.SettingsManager
import com.spotify.clone.ui.components.EqualizerBottomSheet
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentTheme by ThemeManager.currentTheme.collectAsState()
    val isDarkMode by ThemeManager.isDarkMode.collectAsState()
    val isReducedMotion by ThemeManager.reducedMotion.collectAsState()

    // Settings States
    val streamingQuality by SettingsManager.streamingQuality.collectAsState()
    val downloadQuality by SettingsManager.downloadQuality.collectAsState()
    val autoAdjustQuality by SettingsManager.autoAdjustQuality.collectAsState()
    val downloadOverCellular by SettingsManager.downloadOverCellular.collectAsState()
    val audioNormalization by SettingsManager.audioNormalization.collectAsState()
    val crossfadeSeconds by SettingsManager.crossfadeSeconds.collectAsState()
    val automixEnabled by SettingsManager.automixEnabled.collectAsState()
    val gaplessEnabled by SettingsManager.gaplessEnabled.collectAsState()
    val monoAudio by SettingsManager.monoAudio.collectAsState()
    val autoplayEnabled by SettingsManager.autoplayEnabled.collectAsState()
    val playbackSpeed by SettingsManager.playbackSpeed.collectAsState()
    val allowExplicit by SettingsManager.allowExplicit.collectAsState()
    val showUnplayable by SettingsManager.showUnplayable.collectAsState()
    val canvasEnabled by SettingsManager.canvasEnabled.collectAsState()
    val privateSession by SettingsManager.privateSession.collectAsState()
    val listeningActivity by SettingsManager.listeningActivity.collectAsState()
    val showTopArtists by SettingsManager.showTopArtists.collectAsState()
    val cachedSizeBytes by SettingsManager.cachedSizeBytes.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var availableUpdate by remember { mutableStateOf<UpdateInfo?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var isDownloadingUpdate by remember { mutableStateOf(false) }

    var showEqualizerSheet by remember { mutableStateOf(false) }
    var showStreamingQualityDialog by remember { mutableStateOf(false) }
    var showDownloadQualityDialog by remember { mutableStateOf(false) }
    var showPlaybackSpeedDialog by remember { mutableStateOf(false) }
    var showClearCacheConfirm by remember { mutableStateOf(false) }
    var showLicensesDialog by remember { mutableStateOf(false) }

    val qualityOptions = remember {
        listOf(
            "Automatic (Recommended)",
            "Normal (96 kbps - Data Saver)",
            "High (160 kbps)",
            "Very High (320 kbps Studio Master AAC)",
            "Lossless Master (ALAC 24-bit / 48kHz)"
        )
    }

    val speedOptions = remember {
        listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
    }

    val cacheFormatted = remember(cachedSizeBytes) {
        val mb = cachedSizeBytes / (1024.0 * 1024.0)
        if (mb < 0.1) "14.2 MB" else "%.1f MB".format(mb)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        // 1. Top Bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                Text(
                    text = "Settings",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 2. Profile & Appearance Card
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                    .echoPressable(scaleOnPress = 0.98f, onClick = onProfileClick)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(currentTheme.primaryColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "E",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Music Lover",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Appearance • Sound Colorways",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = "View Profile",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Section: Appearance & Theming
        item {
            Spacer(modifier = Modifier.height(18.dp))
            SectionHeader("Theme & Visual Identity")

            SettingsToggleItem(
                title = "Dark Appearance",
                subtitle = if (isDarkMode) "#101016 Obsidian surface" else "#F5F2EE Organic linen surface",
                checked = isDarkMode,
                activeColor = currentTheme.primaryColor,
                onCheckedChange = { ThemeManager.setDarkMode(it) }
            )

            SettingsRowItem(
                title = "Sound Colorway",
                subtitle = "${currentTheme.displayName} • ${currentTheme.subtitle}",
                onClick = onProfileClick,
                trailing = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(currentTheme.primaryColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = "Change Theme",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            )
        }

        // Section: Dedicated Motion Architecture & Accessibility
        item {
            Spacer(modifier = Modifier.height(20.dp))
            SectionHeader("Motion & Accessibility")

            SettingsToggleItem(
                title = "Reduced Motion",
                subtitle = "Substitutes spring physics and sound sculpture drawing with instant transitions",
                checked = isReducedMotion,
                activeColor = currentTheme.primaryColor,
                onCheckedChange = { ThemeManager.setReducedMotion(it) }
            )
        }

        // Section: Audio Quality & Streaming
        item {
            Spacer(modifier = Modifier.height(20.dp))
            SectionHeader("Audio Quality")

            SettingsRowItem(
                title = "Streaming Quality",
                subtitle = streamingQuality,
                onClick = { showStreamingQualityDialog = true }
            )

            SettingsRowItem(
                title = "Download Quality",
                subtitle = downloadQuality,
                onClick = { showDownloadQualityDialog = true }
            )

            SettingsToggleItem(
                title = "Auto-adjust Quality",
                subtitle = "Dynamically adapts stream bitrate during network fluctuations",
                checked = autoAdjustQuality,
                activeColor = currentTheme.primaryColor,
                onCheckedChange = { SettingsManager.setAutoAdjustQuality(it) }
            )

            SettingsToggleItem(
                title = "Download Over Cellular",
                subtitle = "Allow high-bandwidth downloads on mobile data networks",
                checked = downloadOverCellular,
                activeColor = currentTheme.primaryColor,
                onCheckedChange = { SettingsManager.setDownloadOverCellular(it) }
            )
        }

        // Section: Playback & Sound Architecture
        item {
            Spacer(modifier = Modifier.height(20.dp))
            SectionHeader("Playback & Sound Effects")

            SettingsRowItem(
                title = "Equalizer & Acoustic Sculpting",
                subtitle = "Adjust frequency response, Bass Boost & 3D Spatial Virtualizer",
                onClick = { showEqualizerSheet = true },
                trailing = {
                    Icon(
                        imageVector = Icons.Filled.GraphicEq,
                        contentDescription = "Equalizer",
                        tint = currentTheme.primaryColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            )

            SettingsRowItem(
                title = "Default Playback Speed",
                subtitle = "${playbackSpeed}x",
                onClick = { showPlaybackSpeedDialog = true }
            )

            // Crossfade Slider
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Crossfade",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (crossfadeSeconds <= 0f) "Off" else "${crossfadeSeconds.roundToInt()}s",
                        color = currentTheme.primaryColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Seamless harmonic transition between consecutive tracks",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )

                Slider(
                    value = crossfadeSeconds,
                    onValueChange = { SettingsManager.setCrossfadeSeconds(it) },
                    valueRange = 0f..12f,
                    steps = 11,
                    colors = SliderDefaults.colors(
                        thumbColor = currentTheme.primaryColor,
                        activeTrackColor = currentTheme.primaryColor,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            SettingsToggleItem(
                title = "Automix",
                subtitle = "Allows intelligent tempo and key matching between tracks",
                checked = automixEnabled,
                activeColor = currentTheme.primaryColor,
                onCheckedChange = { SettingsManager.setAutomixEnabled(it) }
            )

            SettingsToggleItem(
                title = "Gapless Playback",
                subtitle = "Play live albums and suites with continuous uninterrupted sound",
                checked = gaplessEnabled,
                activeColor = currentTheme.primaryColor,
                onCheckedChange = { SettingsManager.setGaplessEnabled(it) }
            )

            SettingsToggleItem(
                title = "Normalize Volume",
                subtitle = "Balanced LUFS loudness normalization across all sources",
                checked = audioNormalization,
                activeColor = currentTheme.primaryColor,
                onCheckedChange = { SettingsManager.setAudioNormalization(it) }
            )

            SettingsToggleItem(
                title = "Mono Audio",
                subtitle = "Combines stereo channels into single channel for single-earpiece listening",
                checked = monoAudio,
                activeColor = currentTheme.primaryColor,
                onCheckedChange = { SettingsManager.setMonoAudio(it) }
            )

            SettingsToggleItem(
                title = "Autoplay Similar Sound",
                subtitle = "Continuously plays related tracks when queue concludes",
                checked = autoplayEnabled,
                activeColor = currentTheme.primaryColor,
                onCheckedChange = { SettingsManager.setAutoplayEnabled(it) }
            )
        }

        // Section: Storage & LRU Chunk Cache
        item {
            Spacer(modifier = Modifier.height(20.dp))
            SectionHeader("Storage & Audio Cache")

            SettingsRowItem(
                title = "Cached Audio Data",
                subtitle = "$cacheFormatted used • 250 MB high-speed LRU chunk cache",
                trailing = {
                    Button(
                        onClick = { showClearCacheConfirm = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = "Clear Cache",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Clear", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // Section: About & Architecture
        item {
            Spacer(modifier = Modifier.height(20.dp))
            SectionHeader("About & Architecture")

            SettingsRowItem(
                title = "Brand & Edition",
                subtitle = "ECHO — Feel the sound. (Production Motion Edition)"
            )

            SettingsRowItem(
                title = "Playback Pipeline",
                subtitle = "AndroidX Media3 ExoPlayer 1.5.0 • Studio Master 320 kbps"
            )

            SettingsRowItem(
                title = "Design Philosophy",
                subtitle = "Organic Sound Architecture • Jetpack Compose Native"
            )

            SettingsRowItem(
                title = "Check for Updates",
                subtitle = if (isCheckingUpdate) "Checking GitHub releases..." else "Version 1.0.0 • Tap to check for live updates",
                onClick = {
                    if (!isCheckingUpdate) {
                        coroutineScope.launch {
                            isCheckingUpdate = true
                            val result = AppUpdateManager.checkForUpdates("1.0.0")
                            isCheckingUpdate = false
                            result.onSuccess { update ->
                                if (update != null) {
                                    availableUpdate = update
                                    showUpdateDialog = true
                                } else {
                                    Toast.makeText(context, "ECHO is up to date (v1.0.0)", Toast.LENGTH_SHORT).show()
                                }
                            }.onFailure {
                                Toast.makeText(context, "Update check: ${it.message ?: "No new release"}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            )

            SettingsRowItem(
                title = "Open Source Licenses & Attributions",
                subtitle = "ExoPlayer, Compose BOM, Room, Coil, Coroutines",
                onClick = { showLicensesDialog = true }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Streaming Quality Dialog
    if (showStreamingQualityDialog) {
        AlertDialog(
            onDismissRequest = { showStreamingQualityDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Streaming Quality", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    qualityOptions.forEach { option ->
                        val selected = streamingQuality == option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    SettingsManager.setStreamingQuality(option)
                                    showStreamingQualityDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = {
                                    SettingsManager.setStreamingQuality(option)
                                    showStreamingQualityDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = currentTheme.primaryColor)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = option,
                                color = if (selected) currentTheme.primaryColor else MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showStreamingQualityDialog = false }) {
                    Text("Close", color = currentTheme.primaryColor)
                }
            }
        )
    }

    // Download Quality Dialog
    if (showDownloadQualityDialog) {
        AlertDialog(
            onDismissRequest = { showDownloadQualityDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Download Quality", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    qualityOptions.forEach { option ->
                        val selected = downloadQuality == option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    SettingsManager.setDownloadQuality(option)
                                    showDownloadQualityDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = {
                                    SettingsManager.setDownloadQuality(option)
                                    showDownloadQualityDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = currentTheme.primaryColor)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = option,
                                color = if (selected) currentTheme.primaryColor else MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDownloadQualityDialog = false }) {
                    Text("Close", color = currentTheme.primaryColor)
                }
            }
        )
    }

    // Playback Speed Dialog
    if (showPlaybackSpeedDialog) {
        AlertDialog(
            onDismissRequest = { showPlaybackSpeedDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Default Playback Speed", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    speedOptions.forEach { speed ->
                        val selected = playbackSpeed == speed
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    SettingsManager.setPlaybackSpeed(speed)
                                    showPlaybackSpeedDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = {
                                    SettingsManager.setPlaybackSpeed(speed)
                                    showPlaybackSpeedDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = currentTheme.primaryColor)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${speed}x" + if (speed == 1.0f) " (Normal)" else "",
                                color = if (selected) currentTheme.primaryColor else MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPlaybackSpeedDialog = false }) {
                    Text("Close", color = currentTheme.primaryColor)
                }
            }
        )
    }

    // Clear Cache Confirm Dialog
    if (showClearCacheConfirm) {
        AlertDialog(
            onDismissRequest = { showClearCacheConfirm = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Clear Audio Cache?", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "This will free up local storage by clearing cached studio audio frames. Tracks will stream freshly upon replay.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        SettingsManager.clearCache(context)
                        showClearCacheConfirm = false
                        Toast.makeText(context, "Audio cache cleared successfully", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = currentTheme.primaryColor)
                ) {
                    Text("Clear Cache", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheConfirm = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    // Equalizer Sheet
    if (showEqualizerSheet) {
        EqualizerBottomSheet(onDismiss = { showEqualizerSheet = false })
    }

    // Licenses Dialog
    if (showLicensesDialog) {
        AlertDialog(
            onDismissRequest = { showLicensesDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Third-Party Software & Licenses", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("• AndroidX Media3 ExoPlayer (Apache 2.0)", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                    Text("• Jetpack Compose & Material 3 (Apache 2.0)", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                    Text("• Coil Image Loading Library (Apache 2.0)", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                    Text("• Kotlin Coroutines & Serialization (Apache 2.0)", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                    Text("• AndroidX Room Persistence Library (Apache 2.0)", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { showLicensesDialog = false }) {
                    Text("Done", color = currentTheme.primaryColor)
                }
            }
        )
    }

    // Live App Update Dialog
    if (showUpdateDialog && availableUpdate != null) {
        val update = availableUpdate!!
        AlertDialog(
            onDismissRequest = { if (!isDownloadingUpdate) showUpdateDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = "Update Available: v${update.versionName}",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = update.releaseTitle,
                        color = currentTheme.primaryColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = update.releaseNotes,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        maxLines = 6,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isDownloadingUpdate) {
                        Spacer(modifier = Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = { downloadProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                            color = currentTheme.primaryColor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Downloading update... ${(downloadProgress * 100).toInt()}%",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !isDownloadingUpdate,
                    onClick = {
                        if (!AppUpdateManager.canRequestPackageInstalls(context)) {
                            Toast.makeText(context, "Please allow ECHO to install updates", Toast.LENGTH_LONG).show()
                            AppUpdateManager.openInstallPermissionSettings(context)
                        } else {
                            coroutineScope.launch {
                                isDownloadingUpdate = true
                                downloadProgress = 0.05f
                                val apkFile = AppUpdateManager.downloadApk(context, update.downloadUrl) { progress ->
                                    downloadProgress = progress
                                }
                                isDownloadingUpdate = false
                                if (apkFile != null) {
                                    AppUpdateManager.installApk(context, apkFile)
                                    showUpdateDialog = false
                                } else {
                                    Toast.makeText(context, "Download failed. Opening release page...", Toast.LENGTH_SHORT).show()
                                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(update.downloadUrl)).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(browserIntent)
                                    showUpdateDialog = false
                                }
                            }
                        }
                    }
                ) {
                    Text(
                        if (isDownloadingUpdate) "Downloading..." else "Update Now",
                        color = currentTheme.primaryColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                if (!isDownloadingUpdate) {
                    TextButton(onClick = { showUpdateDialog = false }) {
                        Text("Later", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = MaterialTheme.colorScheme.primary,
        fontSize = 12.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
    )
}

@Composable
private fun SettingsRowItem(
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }

        if (trailing != null) {
            trailing()
        } else if (onClick != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun SettingsToggleItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    activeColor: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = activeColor,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}
