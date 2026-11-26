package com.spotify.clone.ui.settings

import android.widget.Toast
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Equalizer
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Radio
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spotify.clone.BuildConfig
import com.spotify.clone.core.designsystem.EchoIconCircleButton
import com.spotify.clone.core.designsystem.EchoInsetCard
import com.spotify.clone.core.designsystem.LocalAppTheme
import com.spotify.clone.core.designsystem.ThemeManager
import com.spotify.clone.core.designsystem.echoContrastOn
import com.spotify.clone.core.designsystem.echoPressable
import com.spotify.clone.core.designsystem.resolvedBackground
import com.spotify.clone.core.designsystem.resolvedBorder
import com.spotify.clone.core.designsystem.tabularFigures
import com.spotify.clone.core.download.TrackDownloadManager
import com.spotify.clone.core.player.MusicController
import com.spotify.clone.core.profile.PrismAvatarOption
import com.spotify.clone.core.profile.UserProfileManager
import com.spotify.clone.core.recommendation.EchoRecommendationEngine
import com.spotify.clone.core.settings.SettingsManager
import com.spotify.clone.core.update.AppUpdateManager
import com.spotify.clone.core.update.UpdateInfo
import com.spotify.clone.ui.components.AppUpdateDialog
import com.spotify.clone.ui.components.EqualizerBottomSheet
import com.spotify.clone.ui.components.PlaybackSpeedBottomSheet
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Simple, calm Profile & Settings screen grouped into clear sections.
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onRestartOnboarding: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeTheme = LocalAppTheme.current
    val isDarkMode by ThemeManager.isDarkMode.collectAsState()
    val isReducedMotion by ThemeManager.reducedMotion.collectAsState()
    val userName by UserProfileManager.userName.collectAsState()
    val avatarId by UserProfileManager.avatarId.collectAsState()
    val preferredLanguages by UserProfileManager.preferredLanguages.collectAsState()
    val activeAvatar = PrismAvatarOption.fromId(avatarId)

    val normalization by SettingsManager.audioNormalization.collectAsState()
    val smartRadio by SettingsManager.smartRadioEnabled.collectAsState()
    val playbackSpeed by SettingsManager.playbackSpeed.collectAsState()
    val crossfadeSeconds by SettingsManager.crossfadeSeconds.collectAsState()
    val autoMatchGenre by SettingsManager.autoMatchGenre.collectAsState()
    val cachedSizeBytes by SettingsManager.cachedSizeBytes.collectAsState()

    val musicController = remember { MusicController.getInstance(context) }
    val downloadManager = remember { TrackDownloadManager.getInstance(context) }
    val downloadedIds by downloadManager.downloadedTrackIds.collectAsState()

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var nameDraft by remember(userName) { mutableStateOf(userName) }
    var avatarDraftId by remember(avatarId) { mutableStateOf(avatarId) }
    var showEqualizerSheet by remember { mutableStateOf(false) }
    var showSpeedSheet by remember { mutableStateOf(false) }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var availableUpdate by remember { mutableStateOf<UpdateInfo?>(null) }
    val coroutineScope = rememberCoroutineScope()
    var showClearCacheConfirm by remember { mutableStateOf(false) }

    val screenBg = activeTheme.resolvedBackground(isDarkMode)
    val textPrimary = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val dividerColor = activeTheme.resolvedBorder(isDarkMode)

    val cacheFormatted = remember(cachedSizeBytes) {
        val mb = cachedSizeBytes / (1024.0 * 1024.0)
        when {
            cachedSizeBytes <= 0L -> "0 MB"
            mb < 0.1 -> "%.0f KB".format(Locale.US, (cachedSizeBytes / 1024.0).coerceAtLeast(1.0))
            else -> "%.1f MB".format(Locale.US, mb)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(screenBg)
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 140.dp)
    ) {
        // Top Bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                EchoIconCircleButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = activeTheme.primaryColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = "Settings",
                    color = textPrimary,
                    style = MaterialTheme.typography.headlineLarge
                )
            }
        }

        // Profile Card (Simple name, avatar & taste)
        item {
            SettingsGroupTitle("PROFILE", textSecondary)
            EchoInsetCard(modifier = Modifier.padding(horizontal = 20.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .echoPressable(scaleOnPress = 0.985f) {
                            nameDraft = userName
                            avatarDraftId = avatarId
                            showEditProfileDialog = true
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(activeAvatar.startColor, activeAvatar.endColor))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "E",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = userName,
                            color = textPrimary,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val subtitle = if (preferredLanguages.isNotEmpty()) {
                            "${preferredLanguages.take(2).joinToString(", ")} • Edit profile"
                        } else {
                            "Edit name & avatar"
                        }
                        Text(
                            text = subtitle,
                            color = textSecondary,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = textSecondary.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                HorizontalDivider(color = dividerColor, thickness = 0.6.dp, modifier = Modifier.padding(start = 52.dp))
                SettingsRow(
                    icon = Icons.Outlined.AutoAwesome,
                    title = "Taste & Languages",
                    subtitle = "Update preferred languages and artists",
                    onClick = onRestartOnboarding
                )
            }
        }

        // Appearance
        item {
            SettingsGroupTitle("APPEARANCE", textSecondary)
            EchoInsetCard(modifier = Modifier.padding(horizontal = 20.dp)) {
                SettingsToggleRow(
                    icon = Icons.Outlined.DarkMode,
                    title = "Dark Mode",
                    subtitle = if (isDarkMode) "On" else "Off",
                    checked = isDarkMode,
                    onCheckedChange = { ThemeManager.setDarkMode(it) }
                )
                HorizontalDivider(color = dividerColor, thickness = 0.6.dp, modifier = Modifier.padding(start = 52.dp))
                SettingsToggleRow(
                    icon = Icons.Outlined.RestartAlt,
                    title = "Reduce Motion",
                    subtitle = "Minimize screen animations",
                    checked = isReducedMotion,
                    onCheckedChange = { ThemeManager.setReducedMotion(it) }
                )
            }
        }

        // Playback
        item {
            SettingsGroupTitle("PLAYBACK", textSecondary)
            EchoInsetCard(modifier = Modifier.padding(horizontal = 20.dp)) {
                SettingsRow(
                    icon = Icons.Outlined.Equalizer,
                    title = "Equalizer",
                    subtitle = "5-band EQ & presets",
                    onClick = { showEqualizerSheet = true }
                )
                HorizontalDivider(color = dividerColor, thickness = 0.6.dp, modifier = Modifier.padding(start = 52.dp))
                SettingsRow(
                    icon = Icons.Outlined.Speed,
                    title = "Playback Speed",
                    subtitle = if (playbackSpeed == 1.0f) "1.0x (Normal)" else "${playbackSpeed}x",
                    onClick = { showSpeedSheet = true }
                )
                HorizontalDivider(color = dividerColor, thickness = 0.6.dp, modifier = Modifier.padding(start = 52.dp))
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Tune,
                            contentDescription = null,
                            tint = activeTheme.primaryColor,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Crossfade",
                                color = textPrimary,
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (crossfadeSeconds == 0) "Off" else "${crossfadeSeconds}s smooth fade",
                                color = textSecondary,
                                style = MaterialTheme.typography.bodySmall.tabularFigures()
                            )
                        }
                        Text(
                            text = if (crossfadeSeconds == 0) "Off" else "${crossfadeSeconds}s",
                            color = activeTheme.primaryColor,
                            style = MaterialTheme.typography.labelLarge.tabularFigures()
                        )
                    }
                    Slider(
                        value = crossfadeSeconds.toFloat(),
                        onValueChange = { SettingsManager.setCrossfadeSeconds(it.roundToInt()) },
                        valueRange = 0f..12f,
                        steps = 11,
                        colors = SliderDefaults.colors(
                            thumbColor = activeTheme.primaryColor,
                            activeTrackColor = activeTheme.primaryColor,
                            inactiveTrackColor = dividerColor
                        )
                    )
                }
                HorizontalDivider(color = dividerColor, thickness = 0.6.dp, modifier = Modifier.padding(start = 52.dp))
                SettingsToggleRow(
                    icon = Icons.Outlined.GraphicEq,
                    title = "Volume Normalization",
                    subtitle = "Consistent volume across tracks",
                    checked = normalization,
                    onCheckedChange = { SettingsManager.setAudioNormalization(it) }
                )
                HorizontalDivider(color = dividerColor, thickness = 0.6.dp, modifier = Modifier.padding(start = 52.dp))
                SettingsToggleRow(
                    icon = Icons.Outlined.MusicNote,
                    title = "Similar Song Queue",
                    subtitle = "Queue matching genre tracks automatically",
                    checked = autoMatchGenre,
                    onCheckedChange = { SettingsManager.setAutoMatchGenre(it) }
                )
                HorizontalDivider(color = dividerColor, thickness = 0.6.dp, modifier = Modifier.padding(start = 52.dp))
                SettingsToggleRow(
                    icon = Icons.Outlined.Radio,
                    title = "Auto-Continue",
                    subtitle = "Keep playing when queue ends",
                    checked = smartRadio,
                    onCheckedChange = { musicController.setSmartRadioEnabled(it) }
                )
            }
        }

        // Storage & App
        item {
            SettingsGroupTitle("STORAGE & APP", textSecondary)
            EchoInsetCard(modifier = Modifier.padding(horizontal = 20.dp)) {
                SettingsRow(
                    icon = Icons.Outlined.DeleteOutline,
                    title = "Clear Cache",
                    subtitle = "$cacheFormatted cached • ${downloadedIds.size} offline",
                    onClick = { showClearCacheConfirm = true }
                )
                HorizontalDivider(color = dividerColor, thickness = 0.6.dp, modifier = Modifier.padding(start = 52.dp))
                SettingsRow(
                    icon = Icons.Outlined.RestartAlt,
                    title = "Reset Taste Profile",
                    subtitle = "Recalibrate daily mixes & recommendations",
                    onClick = {
                        EchoRecommendationEngine.clearTasteSignals()
                        Toast.makeText(context, "Taste profile recalibrated", Toast.LENGTH_SHORT).show()
                    }
                )
                HorizontalDivider(color = dividerColor, thickness = 0.6.dp, modifier = Modifier.padding(start = 52.dp))
                SettingsRow(
                    icon = Icons.Outlined.SystemUpdate,
                    title = "App Updates",
                    subtitle = if (isCheckingUpdate) {
                        "Checking for updates..."
                    } else {
                        "Version ${BuildConfig.VERSION_NAME} • Tap to check"
                    },
                    onClick = {
                        if (!isCheckingUpdate) {
                            coroutineScope.launch {
                                isCheckingUpdate = true
                                val result = AppUpdateManager.checkForUpdates(context, BuildConfig.VERSION_NAME)
                                isCheckingUpdate = false
                                result.onSuccess { update ->
                                    if (update != null) {
                                        availableUpdate = update
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "You're on the latest version (v${BuildConfig.VERSION_NAME})",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }.onFailure {
                                    Toast.makeText(context, "Unable to check updates right now", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                )
            }
        }
    }

    if (showEditProfileDialog) {
        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Edit Profile", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    OutlinedTextField(
                        value = nameDraft,
                        onValueChange = { nameDraft = it },
                        label = { Text("Display Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Avatar Color",
                        color = textSecondary,
                        style = MaterialTheme.typography.labelLarge
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        PrismAvatarOption.entries.forEach { option ->
                            val selected = avatarDraftId == option.id
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(option.startColor, option.endColor)))
                                    .then(
                                        if (selected) {
                                            Modifier.border(2.dp, textPrimary, CircleShape)
                                        } else Modifier
                                    )
                                    .echoPressable(scaleOnPress = 0.90f, hapticOnClick = true) {
                                        avatarDraftId = option.id
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (selected) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = option.label,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        UserProfileManager.setUserName(nameDraft)
                        UserProfileManager.setAvatarId(avatarDraftId)
                        showEditProfileDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = activeTheme.primaryColor,
                        contentColor = echoContrastOn(activeTheme.primaryColor)
                    )
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showClearCacheConfirm) {
        AlertDialog(
            onDismissRequest = { showClearCacheConfirm = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Clear Audio Cache?", fontWeight = FontWeight.Bold) },
            text = { Text("This clears temporary streamed cache ($cacheFormatted). Offline songs are kept.") },
            confirmButton = {
                Button(
                    onClick = {
                        SettingsManager.clearCache(context)
                        Toast.makeText(context, "Cache cleared", Toast.LENGTH_SHORT).show()
                        showClearCacheConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = activeTheme.primaryColor,
                        contentColor = echoContrastOn(activeTheme.primaryColor)
                    )
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showEqualizerSheet) {
        EqualizerBottomSheet(onDismiss = { showEqualizerSheet = false })
    }

    if (showSpeedSheet) {
        PlaybackSpeedBottomSheet(
            musicController = musicController,
            onDismiss = { showSpeedSheet = false }
        )
    }

    availableUpdate?.let { update ->
        AppUpdateDialog(
            update = update,
            onDismiss = { availableUpdate = null }
        )
    }
}

@Composable
private fun SettingsGroupTitle(title: String, color: Color) {
    Text(
        text = title,
        color = color,
        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.6.sp),
        modifier = Modifier.padding(start = 28.dp, top = 20.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val accent = LocalAppTheme.current.primaryColor
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .echoPressable(scaleOnPress = 1.0f, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.5.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val accent = LocalAppTheme.current.primaryColor
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.5.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = echoContrastOn(accent),
                checkedTrackColor = accent
            )
        )
    }
}
