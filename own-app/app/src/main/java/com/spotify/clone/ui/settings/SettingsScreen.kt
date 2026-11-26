package com.spotify.clone.ui.settings

import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.Copyright
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Equalizer
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Radio
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Subtitles
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
import com.spotify.clone.core.designsystem.AppThemeOption
import com.spotify.clone.core.designsystem.EchoIconCircleButton
import com.spotify.clone.core.designsystem.EchoInsetCard
import com.spotify.clone.core.designsystem.EchoRadius
import com.spotify.clone.core.designsystem.EchoSegmentedControl
import com.spotify.clone.core.designsystem.EchoTonalIconBadge
import com.spotify.clone.core.designsystem.LocalAppTheme
import com.spotify.clone.core.designsystem.ThemeManager
import com.spotify.clone.core.designsystem.echoPressable
import com.spotify.clone.core.designsystem.tabularFigures
import com.spotify.clone.core.download.TrackDownloadManager
import com.spotify.clone.core.legal.GravitLegalCatalog
import com.spotify.clone.core.legal.LegalDocumentTab
import com.spotify.clone.core.player.MusicController
import com.spotify.clone.core.profile.PrismAvatarOption
import com.spotify.clone.core.profile.UserProfileManager
import com.spotify.clone.core.recommendation.EchoRecommendationEngine
import com.spotify.clone.core.settings.SettingsManager
import com.spotify.clone.core.update.AppUpdateManager
import com.spotify.clone.core.update.UpdateInfo
import com.spotify.clone.ui.components.AppUpdateDialog
import com.spotify.clone.ui.components.EqualizerBottomSheet
import com.spotify.clone.ui.components.LegalDocumentSheet
import com.spotify.clone.ui.components.PlaybackSpeedBottomSheet
import com.spotify.clone.ui.splash.EchoAcousticLogoMark
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

private enum class LyricsSizeOption(val tierKey: String, val label: String) {
    COMPACT("COMPACT", "Compact"),
    STANDARD("STANDARD", "Standard"),
    LARGE("LARGE", "Large"),
    EXTRA_LARGE("EXTRA_LARGE", "XL");

    companion object {
        fun fromKey(key: String): LyricsSizeOption =
            entries.find { it.tierKey == key } ?: STANDARD
    }
}

/**
 * Clean, organized Material 3 Expressive Settings screen grouped into clear sections:
 * 1. Profile & Taste
 * 2. Appearance
 * 3. Audio & Playback
 * 4. Lyrics
 * 5. Storage & Updates
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
    val isDynamicColor by ThemeManager.dynamicColorEnabled.collectAsState()

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
    val autoGenreEq by SettingsManager.autoGenreEq.collectAsState()
    val lyricsUseRomanized by SettingsManager.lyricsUseRomanized.collectAsState()
    val lyricsSizeTier by SettingsManager.lyricsSizeTier.collectAsState()
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
    var activeLegalTab by remember { mutableStateOf<LegalDocumentTab?>(null) }

    val screenBg = MaterialTheme.colorScheme.background
    val primaryColor = MaterialTheme.colorScheme.primary
    val textPrimary = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val dividerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.38f)

    val cacheFormatted = remember(cachedSizeBytes) {
        val mb = cachedSizeBytes / (1024.0 * 1024.0)
        when {
            cachedSizeBytes <= 0L -> "0 MB"
            mb < 0.1 -> "%.0f KB".format(Locale.US, (cachedSizeBytes / 1024.0).coerceAtLeast(1.0))
            else -> "%.1f MB".format(Locale.US, mb)
        }
    }

    val navBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(screenBg)
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 140.dp + navBottomPadding)
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
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = "Settings",
                    color = textPrimary,
                    style = MaterialTheme.typography.displaySmall
                )
            }
        }

        // 1. Profile & Taste
        item {
            SettingsGroupTitle("PROFILE & TASTE", textSecondary)
            EchoInsetCard(
                modifier = Modifier.padding(horizontal = 20.dp),
                shape = RoundedCornerShape(EchoRadius.xl)
            ) {
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
                        val tasteParts = buildList {
                            if (preferredLanguages.isNotEmpty()) add("${preferredLanguages.size} languages")
                            if (favoriteArtists.isNotEmpty()) add("${favoriteArtists.size} artists")
                        }
                        val subtitle = if (tasteParts.isNotEmpty()) {
                            "${tasteParts.joinToString(" • ")} • Tap to edit"
                        } else {
                            "Edit display name & badge"
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
                SettingsDivider(dividerColor)
                SettingsRow(
                    icon = Icons.Outlined.AutoAwesome,
                    title = "Languages & Artists",
                    subtitle = "Update preferred languages, genres & artists",
                    onClick = onRestartOnboarding
                )
            }
        }

        // 2. Appearance
        item {
            SettingsGroupTitle("APPEARANCE", textSecondary)
            EchoInsetCard(
                modifier = Modifier.padding(horizontal = 20.dp),
                shape = RoundedCornerShape(EchoRadius.xl)
            ) {
                Column(modifier = Modifier.padding(top = 14.dp, bottom = 14.dp)) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        EchoTonalIconBadge(icon = Icons.Outlined.Palette)
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Color Theme",
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = if (isDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    "Material You Wallpaper Active"
                                } else {
                                    activeTheme.displayName
                                },
                                color = textSecondary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AppThemeOption.entries.forEach { option ->
                            val isSelected = activeTheme == option && !isDynamicColor
                            val swatchPrimary = option.resolvedPrimary(isDarkMode)
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(if (isSelected) 24.dp else 14.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceContainerHigh
                                    )
                                    .echoPressable(scaleOnPress = 0.95f, hapticOnClick = true) {
                                        ThemeManager.setDynamicColorEnabled(false)
                                        ThemeManager.setTheme(option)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(listOf(swatchPrimary, option.gradientEnd))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(11.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = option.displayName,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    SettingsDivider(dividerColor)
                    SettingsToggleRow(
                        icon = Icons.Outlined.ColorLens,
                        title = "Dynamic Wallpaper Color",
                        subtitle = "Match your Android wallpaper palette",
                        checked = isDynamicColor,
                        onCheckedChange = { ThemeManager.setDynamicColorEnabled(it) }
                    )
                }
                SettingsDivider(dividerColor)
                SettingsToggleRow(
                    icon = Icons.Outlined.DarkMode,
                    title = "Dark Mode",
                    subtitle = if (isDarkMode) "Dark canvas active" else "Light canvas active",
                    checked = isDarkMode,
                    onCheckedChange = { ThemeManager.setDarkMode(it) }
                )
                SettingsDivider(dividerColor)
                SettingsToggleRow(
                    icon = Icons.Outlined.RestartAlt,
                    title = "Reduce Motion",
                    subtitle = "Minimize transitions and spring effects",
                    checked = isReducedMotion,
                    onCheckedChange = { ThemeManager.setReducedMotion(it) }
                )
            }
        }

        // 3. Audio & Playback
        item {
            SettingsGroupTitle("AUDIO & PLAYBACK", textSecondary)
            EchoInsetCard(
                modifier = Modifier.padding(horizontal = 20.dp),
                shape = RoundedCornerShape(EchoRadius.xl)
            ) {
                SettingsRow(
                    icon = Icons.Outlined.Equalizer,
                    title = "Equalizer",
                    subtitle = "5-band EQ & acoustic presets",
                    onClick = { showEqualizerSheet = true }
                )
                SettingsDivider(dividerColor)
                SettingsToggleRow(
                    icon = Icons.Outlined.AutoAwesome,
                    title = "Auto-Match EQ to Genre",
                    subtitle = "Adapts EQ preset dynamically per song",
                    checked = autoGenreEq,
                    onCheckedChange = {
                        SettingsManager.setAutoGenreEq(it)
                        SettingsManager.notifyEqualizerChanged()
                    }
                )
                SettingsDivider(dividerColor)
                SettingsRow(
                    icon = Icons.Outlined.Speed,
                    title = "Playback Speed",
                    subtitle = if (playbackSpeed == 1.0f) "1.0x (Normal)" else "${playbackSpeed}x",
                    onClick = { showSpeedSheet = true }
                )
                SettingsDivider(dividerColor)
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        EchoTonalIconBadge(icon = Icons.Outlined.Tune)
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Crossfade",
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = if (crossfadeSeconds == 0) "Off" else "${crossfadeSeconds}s smooth transition",
                                color = textSecondary,
                                style = MaterialTheme.typography.bodySmall.tabularFigures()
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (crossfadeSeconds == 0) "Off" else "${crossfadeSeconds}s",
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                style = MaterialTheme.typography.labelMedium.tabularFigures()
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Slider(
                        value = crossfadeSeconds.toFloat(),
                        onValueChange = { SettingsManager.setCrossfadeSeconds(it.roundToInt()) },
                        valueRange = 0f..12f,
                        steps = 11,
                        colors = SliderDefaults.colors(
                            thumbColor = primaryColor,
                            activeTrackColor = primaryColor,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    )
                }
                SettingsDivider(dividerColor)
                SettingsToggleRow(
                    icon = Icons.Outlined.GraphicEq,
                    title = "Volume Normalization",
                    subtitle = "Consistent loudness across tracks",
                    checked = normalization,
                    onCheckedChange = { SettingsManager.setAudioNormalization(it) }
                )
                SettingsDivider(dividerColor)
                SettingsToggleRow(
                    icon = Icons.Outlined.MusicNote,
                    title = "Similar Song Queue",
                    subtitle = "Queue matching genre tracks automatically",
                    checked = autoMatchGenre,
                    onCheckedChange = { SettingsManager.setAutoMatchGenre(it) }
                )
                SettingsDivider(dividerColor)
                SettingsToggleRow(
                    icon = Icons.Outlined.Radio,
                    title = "Auto-Continue",
                    subtitle = "Keep playing when your queue ends",
                    checked = smartRadio,
                    onCheckedChange = { musicController.setSmartRadioEnabled(it) }
                )
            }
        }

        // 4. Lyrics
        item {
            SettingsGroupTitle("LYRICS", textSecondary)
            EchoInsetCard(
                modifier = Modifier.padding(horizontal = 20.dp),
                shape = RoundedCornerShape(EchoRadius.xl)
            ) {
                SettingsToggleRow(
                    icon = Icons.Outlined.Subtitles,
                    title = "Romanized English Lyrics",
                    subtitle = "Show transliterated phonetics for regional songs",
                    checked = lyricsUseRomanized,
                    onCheckedChange = { SettingsManager.setLyricsUseRomanized(it) }
                )
                SettingsDivider(dividerColor)
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        EchoTonalIconBadge(icon = Icons.Outlined.FormatSize)
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Lyrics Text Size",
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Choose readability scale in Now Playing",
                                color = textSecondary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    EchoSegmentedControl(
                        options = LyricsSizeOption.entries,
                        selectedOption = LyricsSizeOption.fromKey(lyricsSizeTier),
                        labelFor = { it.label },
                        onSelect = { SettingsManager.setLyricsSizeTier(it.tierKey) }
                    )
                }
            }
        }

        // 5. Storage & Updates
        item {
            SettingsGroupTitle("STORAGE & UPDATES", textSecondary)
            EchoInsetCard(
                modifier = Modifier.padding(horizontal = 20.dp),
                shape = RoundedCornerShape(EchoRadius.xl)
            ) {
                SettingsRow(
                    icon = Icons.Outlined.DeleteOutline,
                    title = "Clear Audio Cache",
                    subtitle = "$cacheFormatted cached • ${downloadedIds.size} offline songs kept",
                    onClick = { showClearCacheConfirm = true }
                )
                SettingsDivider(dividerColor)
                SettingsRow(
                    icon = Icons.Outlined.RestartAlt,
                    title = "Reset Taste Profile",
                    subtitle = "Recalibrate daily mixes and recommendations",
                    onClick = {
                        EchoRecommendationEngine.clearTasteSignals()
                        Toast.makeText(context, "Taste profile recalibrated", Toast.LENGTH_SHORT).show()
                    }
                )
                SettingsDivider(dividerColor)
                SettingsRow(
                    icon = Icons.Outlined.SystemUpdate,
                    title = "App Updates",
                    subtitle = if (isCheckingUpdate) {
                        "Checking for updates…"
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

        // 6. Legal & About
        item {
            SettingsGroupTitle("LEGAL & ABOUT", textSecondary)
            EchoInsetCard(
                modifier = Modifier.padding(horizontal = 20.dp),
                shape = RoundedCornerShape(EchoRadius.xl)
            ) {
                SettingsRow(
                    icon = Icons.Outlined.PrivacyTip,
                    title = "Privacy Policy",
                    subtitle = "Local-first data, network requests & erasure rights",
                    onClick = { activeLegalTab = LegalDocumentTab.PRIVACY }
                )
                SettingsDivider(dividerColor)
                SettingsRow(
                    icon = Icons.Outlined.Description,
                    title = "Terms & Conditions",
                    subtitle = "License grant, acceptable use & liability terms",
                    onClick = { activeLegalTab = LegalDocumentTab.TERMS }
                )
                SettingsDivider(dividerColor)
                SettingsRow(
                    icon = Icons.Outlined.Copyright,
                    title = "Copyright & DMCA Notice",
                    subtitle = "${GravitLegalCatalog.COPYRIGHT_NOTICE} • Content rights",
                    onClick = { activeLegalTab = LegalDocumentTab.COPYRIGHT }
                )
                SettingsDivider(dividerColor)
                SettingsRow(
                    icon = Icons.Outlined.Security,
                    title = "Data, Permissions & Security",
                    subtitle = "App permissions, HTTPS transport & offline storage",
                    onClick = { activeLegalTab = LegalDocumentTab.PERMISSIONS }
                )
                SettingsDivider(dividerColor)
                SettingsRow(
                    icon = Icons.Outlined.Code,
                    title = "Open-Source Licenses",
                    subtitle = "Third-party libraries & software attributions",
                    onClick = { activeLegalTab = LegalDocumentTab.LICENSES }
                )
            }
        }

        // Branded GRAVIT Copyright Footer
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                EchoAcousticLogoMark(modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${GravitLegalCatalog.APP_BRAND} • by ${GravitLegalCatalog.OWNER_NAME}",
                    color = textPrimary,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Version ${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                    color = textSecondary,
                    style = MaterialTheme.typography.bodySmall.tabularFigures()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = GravitLegalCatalog.COPYRIGHT_NOTICE,
                    color = textSecondary,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
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
                        text = "Badge Color",
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
                        containerColor = primaryColor,
                        contentColor = MaterialTheme.colorScheme.onPrimary
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
                        containerColor = primaryColor,
                        contentColor = MaterialTheme.colorScheme.onPrimary
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

    activeLegalTab?.let { tab ->
        LegalDocumentSheet(
            initialTab = tab,
            onDismiss = { activeLegalTab = null }
        )
    }
}

@Composable
private fun SettingsGroupTitle(title: String, color: Color) {
    Text(
        text = title,
        color = color,
        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.7.sp),
        modifier = Modifier.padding(start = 28.dp, top = 22.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsDivider(color: Color) {
    HorizontalDivider(
        color = color,
        thickness = 0.6.dp,
        modifier = Modifier.padding(start = 68.dp, end = 16.dp)
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .echoPressable(scaleOnPress = 0.99f, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        EchoTonalIconBadge(icon = icon)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        EchoTonalIconBadge(icon = icon)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}
