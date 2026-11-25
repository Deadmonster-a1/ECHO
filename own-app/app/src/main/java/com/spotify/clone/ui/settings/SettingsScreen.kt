package com.spotify.clone.ui.settings

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Equalizer
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Radio
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.SystemUpdate
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spotify.clone.BuildConfig
import com.spotify.clone.core.data.AudioRepository
import com.spotify.clone.core.designsystem.AppThemeOption
import com.spotify.clone.core.designsystem.EchoInsetCard
import com.spotify.clone.core.designsystem.EchoMotion
import com.spotify.clone.core.designsystem.LocalAppTheme
import com.spotify.clone.core.designsystem.ThemeManager
import com.spotify.clone.core.designsystem.echoContrastOn
import com.spotify.clone.core.designsystem.echoPressable
import com.spotify.clone.core.designsystem.echoPrismBrush
import com.spotify.clone.core.designsystem.echoSpecularBorder
import com.spotify.clone.core.designsystem.resolvedBackground
import com.spotify.clone.core.designsystem.resolvedBorder
import com.spotify.clone.core.designsystem.resolvedCard
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
 * Apple Inset-Grouped Settings Screen with tactile swatch selection,
 * specular surface borders, and smooth theme feedback.
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
    val favoriteArtists by UserProfileManager.favoriteArtists.collectAsState()
    val activeAvatar = PrismAvatarOption.fromId(avatarId)

    val normalization by SettingsManager.audioNormalization.collectAsState()
    val smartRadio by SettingsManager.smartRadioEnabled.collectAsState()
    val playbackSpeed by SettingsManager.playbackSpeed.collectAsState()
    val crossfadeSeconds by SettingsManager.crossfadeSeconds.collectAsState()
    val autoMatchGenre by SettingsManager.autoMatchGenre.collectAsState()
    val cachedSizeBytes by SettingsManager.cachedSizeBytes.collectAsState()

    val musicController = remember { MusicController.getInstance(context) }
    val repository = remember { AudioRepository(context) }
    val recentlyPlayed by repository.getRecentlyPlayedTracks().collectAsState(initial = emptyList())
    val favorites by repository.getFavoriteTracks().collectAsState(initial = emptyList())
    var tasteSignalRevision by remember { mutableStateOf(0) }
    val topGenre = remember(recentlyPlayed, favorites, tasteSignalRevision) {
        EchoRecommendationEngine.getTopPreferredGenre(recentlyPlayed, favorites)
    }
    val topArtists = remember(recentlyPlayed, favorites, tasteSignalRevision) {
        EchoRecommendationEngine.getTopLearnedArtists(recentlyPlayed, favorites, limit = 3)
    }

    val downloadManager = remember { TrackDownloadManager.getInstance(context) }
    val downloadedIds by downloadManager.downloadedTrackIds.collectAsState()

    var showEditNameDialog by remember { mutableStateOf(false) }
    var nameDraft by remember(userName) { mutableStateOf(userName) }
    var showEqualizerSheet by remember { mutableStateOf(false) }
    var showSpeedSheet by remember { mutableStateOf(false) }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var availableUpdate by remember { mutableStateOf<UpdateInfo?>(null) }
    val coroutineScope = rememberCoroutineScope()
    var showClearCacheConfirm by remember { mutableStateOf(false) }

    val screenBg = activeTheme.resolvedBackground(isDarkMode)
    val cardBg = activeTheme.resolvedCard(isDarkMode)
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
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(cardBg)
                        .echoSpecularBorder(isDarkMode, CircleShape, dividerColor)
                        .echoPressable(scaleOnPress = 0.88f, hapticOnClick = true, onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = activeTheme.primaryColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Settings",
                    color = textPrimary,
                    style = MaterialTheme.typography.headlineLarge
                )
            }
        }

        // Profile Section
        item {
            SettingsGroupTitle("PROFILE", textSecondary)
            EchoInsetCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .echoPressable(scaleOnPress = 0.985f) {
                            nameDraft = userName
                            showEditNameDialog = true
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(activeAvatar.startColor, activeAvatar.endColor))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "E",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = userName,
                            color = textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        val prefSummary = buildList {
                            if (preferredLanguages.isNotEmpty()) add(preferredLanguages.take(2).joinToString(", "))
                            if (favoriteArtists.isNotEmpty()) add("${favoriteArtists.size} favorite artists")
                        }.joinToString(" • ").ifBlank { "Tap to edit display name" }
                        Text(
                            text = prefSummary,
                            color = textSecondary,
                            fontSize = 12.5.sp,
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
                HorizontalDivider(color = dividerColor, thickness = 0.6.dp, modifier = Modifier.padding(start = 54.dp))
                SettingsRow(
                    icon = Icons.Outlined.AutoAwesome,
                    title = "Tune Your Taste & Setup",
                    subtitle = "Re-run languages, genres & artist setup",
                    onClick = onRestartOnboarding
                )
            }
        }

        // Listener Taste & Algorithm Profile
        item {
            SettingsGroupTitle("LISTENER TASTE & ALGORITHMS", textSecondary)
            EchoInsetCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    activeTheme.primaryColor.copy(alpha = if (isDarkMode) 0.14f else 0.08f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            tint = activeTheme.primaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Taste Engine Profile",
                            color = textPrimary,
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(activeTheme.primaryColor.copy(alpha = 0.16f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = topGenre.shortLabel,
                                color = activeTheme.primaryColor,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (topArtists.isNotEmpty()) {
                            "Top learned artists: ${topArtists.joinToString(" • ")}"
                        } else {
                            "Play or favorite tracks to calibrate Daily Mix & Same-Genre Radio."
                        },
                        color = textSecondary,
                        fontSize = 13.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                HorizontalDivider(color = dividerColor, thickness = 0.6.dp, modifier = Modifier.padding(start = 54.dp))
                SettingsRow(
                    icon = Icons.Outlined.RestartAlt,
                    title = "Reset Taste Signals",
                    subtitle = "Clear session affinity weights & re-seed Daily Mixes",
                    onClick = {
                        EchoRecommendationEngine.clearTasteSignals()
                        tasteSignalRevision++
                        Toast.makeText(context, "Taste profile recalibrated", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        // Appearance Section
        item {
            SettingsGroupTitle("APPEARANCE", textSecondary)
            EchoInsetCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                SettingsToggleRow(
                    icon = Icons.Outlined.DarkMode,
                    title = "Dark Appearance",
                    subtitle = if (isDarkMode) "Obsidian dark canvas" else "Airy pastel daylight canvas",
                    checked = isDarkMode,
                    onCheckedChange = { ThemeManager.setDarkMode(it) }
                )
                HorizontalDivider(color = dividerColor, thickness = 0.6.dp, modifier = Modifier.padding(start = 54.dp))
                SettingsToggleRow(
                    icon = Icons.Outlined.RestartAlt,
                    title = "Reduce Motion",
                    subtitle = "Minimize animations across screens",
                    checked = isReducedMotion,
                    onCheckedChange = { ThemeManager.setReducedMotion(it) }
                )
            }
        }

        // Audio & Playback Section
        item {
            SettingsGroupTitle("AUDIO & PLAYBACK", textSecondary)
            EchoInsetCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                SettingsRow(
                    icon = Icons.Outlined.Equalizer,
                    title = "Equalizer",
                    subtitle = "5-band EQ & sound presets",
                    onClick = { showEqualizerSheet = true }
                )
                HorizontalDivider(color = dividerColor, thickness = 0.6.dp, modifier = Modifier.padding(start = 54.dp))
                SettingsRow(
                    icon = Icons.Outlined.Speed,
                    title = "Playback Speed",
                    subtitle = if (playbackSpeed == 1.0f) "1.0x (Normal)" else "${playbackSpeed}x",
                    onClick = { showSpeedSheet = true }
                )
                HorizontalDivider(color = dividerColor, thickness = 0.6.dp, modifier = Modifier.padding(start = 54.dp))
                SettingsToggleRow(
                    icon = Icons.Outlined.GraphicEq,
                    title = "Loudness Normalization",
                    subtitle = "Balance volume levels across tracks",
                    checked = normalization,
                    onCheckedChange = { SettingsManager.setAudioNormalization(it) }
                )
                HorizontalDivider(color = dividerColor, thickness = 0.6.dp, modifier = Modifier.padding(start = 54.dp))
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
                                text = "Automix Crossfade",
                                color = textPrimary,
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (crossfadeSeconds == 0) "Off (Gapless cut)" else "Smooth fade transition • ${crossfadeSeconds}s",
                                color = textSecondary,
                                style = MaterialTheme.typography.bodySmall.tabularFigures().copy(fontSize = 13.sp)
                            )
                        }
                        Text(
                            text = "${crossfadeSeconds}s",
                            color = activeTheme.primaryColor,
                            style = MaterialTheme.typography.labelLarge.tabularFigures().copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
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
                HorizontalDivider(color = dividerColor, thickness = 0.6.dp, modifier = Modifier.padding(start = 54.dp))
                SettingsToggleRow(
                    icon = Icons.Outlined.MusicNote,
                    title = "Auto-Match Same-Genre Queue",
                    subtitle = "Automatically queue matching genre songs when playing tracks",
                    checked = autoMatchGenre,
                    onCheckedChange = { SettingsManager.setAutoMatchGenre(it) }
                )
                HorizontalDivider(color = dividerColor, thickness = 0.6.dp, modifier = Modifier.padding(start = 54.dp))
                SettingsToggleRow(
                    icon = Icons.Outlined.Radio,
                    title = "Auto-Continue Queue",
                    subtitle = "Queue similar tracks when your list ends",
                    checked = smartRadio,
                    onCheckedChange = { musicController.setSmartRadioEnabled(it) }
                )
            }
        }

        // Storage Section
        item {
            SettingsGroupTitle("STORAGE", textSecondary)
            EchoInsetCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                SettingsRow(
                    icon = Icons.Outlined.DeleteOutline,
                    title = "Clear Audio Cache",
                    subtitle = "Cached: $cacheFormatted • Offline songs: ${downloadedIds.size}",
                    onClick = { showClearCacheConfirm = true }
                )
            }
        }

        // About & Updates Section
        item {
            SettingsGroupTitle("ABOUT & UPDATES", textSecondary)
            EchoInsetCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                SettingsRow(
                    icon = Icons.Outlined.SystemUpdate,
                    title = "Check for Updates",
                    subtitle = if (isCheckingUpdate) {
                        "Checking for direct updates..."
                    } else {
                        "Version ${BuildConfig.VERSION_NAME} • Tap to check for updates"
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
                                        Toast.makeText(context, "You're on the latest version (v${BuildConfig.VERSION_NAME})", Toast.LENGTH_SHORT).show()
                                    }
                                }.onFailure {
                                    Toast.makeText(context, "Unable to check updates right now", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                )
                HorizontalDivider(color = dividerColor, thickness = 0.6.dp, modifier = Modifier.padding(start = 54.dp))
                SettingsRow(
                    icon = Icons.Outlined.Info,
                    title = "ECHO Music",
                    subtitle = "Version ${BuildConfig.VERSION_NAME} • Direct OTA updates enabled",
                    onClick = {}
                )
            }
        }
    }

    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Edit Display Name", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = nameDraft,
                    onValueChange = { nameDraft = it },
                    label = { Text("Your Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        UserProfileManager.setUserName(nameDraft)
                        showEditNameDialog = false
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
                TextButton(onClick = { showEditNameDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showClearCacheConfirm) {
        AlertDialog(
            onDismissRequest = { showClearCacheConfirm = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Clear Audio Cache?", fontWeight = FontWeight.Bold) },
            text = { Text("This clears temporary streamed cache ($cacheFormatted). Your downloaded offline songs are kept.") },
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
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.6.sp,
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
            .echoPressable(scaleOnPress = 0.985f, onClick = onClick)
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
                maxLines = 2,
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
