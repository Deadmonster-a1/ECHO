package com.spotify.clone.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.spotify.clone.core.designsystem.EchoAccentCoral
import com.spotify.clone.core.designsystem.EchoMotion
import com.spotify.clone.core.designsystem.EchoPrismAqua
import com.spotify.clone.core.designsystem.EchoPrismLavender
import com.spotify.clone.core.designsystem.EchoPrismRose
import com.spotify.clone.core.designsystem.EchoRadius
import com.spotify.clone.core.designsystem.LocalAppTheme
import com.spotify.clone.core.designsystem.LocalReducedMotion
import com.spotify.clone.core.designsystem.ThemeManager
import com.spotify.clone.core.designsystem.echoContrastOn
import com.spotify.clone.core.designsystem.echoGlassSurface
import com.spotify.clone.core.designsystem.echoPressable
import com.spotify.clone.core.designsystem.resolvedBorder
import com.spotify.clone.core.designsystem.resolvedCard
import com.spotify.clone.core.designsystem.tabularFigures
import com.spotify.clone.core.update.AppUpdateManager
import com.spotify.clone.core.update.UpdateInfo
import com.spotify.clone.ui.splash.EchoAcousticLogoMark
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Refined Liquid Glass OTA update popup with animated echo brand badge,
 * release notes card, smooth download progress, and resilient error fallback.
 */
@Composable
fun AppUpdateDialog(
    update: UpdateInfo,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activeTheme = LocalAppTheme.current
    val isDarkMode by ThemeManager.isDarkMode.collectAsState()
    val isReducedMotion = LocalReducedMotion.current
    val scope = rememberCoroutineScope()

    var isDownloading by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var downloadError by remember { mutableStateOf<String?>(null) }

    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 180, easing = EchoMotion.EaseOutStrong),
        label = "ota_progress"
    )

    val logoTransition = rememberInfiniteTransition(label = "ota_logo_pulse")
    val wavePhase by logoTransition.animateFloat(
        initialValue = 0f,
        targetValue = (Math.PI * 2.0).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ota_logo_wave"
    )

    val cardShape = RoundedCornerShape(EchoRadius.xl)
    val borderColor = activeTheme.resolvedBorder(isDarkMode)
    val innerCardBg = if (isDarkMode) Color(0xFF1A1B25) else Color(0xFFF3F4FA)
    val textPrimary = MaterialTheme.colorScheme.onSurface
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant

    val sizeLabel = remember(update.apkSize) {
        if (update.apkSize > 0L) {
            String.format(Locale.US, "%.1f MB", update.apkSize / (1024.0 * 1024.0))
        } else {
            null
        }
    }

    fun startUpdateDownload() {
        if (!AppUpdateManager.canRequestPackageInstalls(context)) {
            Toast.makeText(context, "Allow echo to install app updates", Toast.LENGTH_LONG).show()
            AppUpdateManager.openInstallPermissionSettings(context)
            return
        }
        downloadError = null
        isDownloading = true
        progress = 0.04f
        scope.launch {
            val apkFile = AppUpdateManager.downloadApk(context, update.downloadUrl) { p ->
                progress = p
            }
            isDownloading = false
            if (apkFile != null && apkFile.exists() && apkFile.length() > 1_000_000L) {
                onDismiss()
                AppUpdateManager.installApk(context, apkFile)
            } else {
                downloadError = "Couldn't finish download. Check connection or download directly."
            }
        }
    }

    Dialog(
        onDismissRequest = { if (!isDownloading && !update.isMandatory) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = !isDownloading && !update.isMandatory,
            dismissOnClickOutside = !isDownloading && !update.isMandatory,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 400.dp)
                    .fillMaxWidth()
                    .echoGlassSurface(
                        isDarkMode = isDarkMode,
                        shape = cardShape,
                        fallbackBorder = borderColor,
                        tintColor = activeTheme.primaryColor,
                        elevation = 18.dp
                    )
            ) {
                // Soft top Pastel Prism aura wash
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    EchoPrismLavender.copy(alpha = if (isDarkMode) 0.18f else 0.14f),
                                    EchoPrismAqua.copy(alpha = if (isDarkMode) 0.06f else 0.04f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp)
                ) {
                    // Header row: animated echo logo badge + version/size pills + optional close
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(activeTheme.resolvedCard(isDarkMode))
                                    .border(0.75.dp, borderColor, RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                EchoAcousticLogoMark(
                                    breathPhase = if (isReducedMotion) null else wavePhase,
                                    playingWaveStrength = if (isReducedMotion) 0f else if (isDownloading) 1.0f else 0.55f,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(activeTheme.primaryColor.copy(alpha = if (isDarkMode) 0.22f else 0.12f))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "v${update.versionName}",
                                        color = activeTheme.primaryColor,
                                        style = MaterialTheme.typography.labelMedium.tabularFigures().copy(
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                                if (sizeLabel != null) {
                                    Text(
                                        text = sizeLabel,
                                        color = textSecondary,
                                        style = MaterialTheme.typography.labelSmall.tabularFigures()
                                    )
                                }
                            }
                        }

                        if (!isDownloading && !update.isMandatory) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(innerCardBg)
                                    .echoPressable(scaleOnPress = 0.88f, hapticOnClick = true, onClick = onDismiss),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Close",
                                    tint = textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = update.releaseTitle.ifBlank { "Update Available" },
                        color = textPrimary,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.3).sp
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Clean "What's New" inset container
                    val notesShape = RoundedCornerShape(EchoRadius.md)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(notesShape)
                            .background(innerCardBg)
                            .border(0.6.dp, borderColor.copy(alpha = 0.8f), notesShape)
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.AutoAwesome,
                                contentDescription = null,
                                tint = activeTheme.primaryColor,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "What's New",
                                color = textPrimary,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 150.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = update.releaseNotes,
                                color = textSecondary,
                                style = MaterialTheme.typography.bodyMedium,
                                lineHeight = 20.sp
                            )
                        }
                    }

                    // Smooth Download Progress
                    AnimatedVisibility(
                        visible = isDownloading,
                        enter = fadeIn(tween(180, easing = EchoMotion.EaseOutStrong)) + expandVertically(tween(220, easing = EchoMotion.EaseOutStrong)),
                        exit = fadeOut(tween(130)) + shrinkVertically(tween(160))
                    ) {
                        Column(modifier = Modifier.padding(top = 14.dp)) {
                            LinearProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(CircleShape),
                                color = activeTheme.primaryColor,
                                trackColor = borderColor.copy(alpha = 0.65f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (animatedProgress >= 0.99f) "Preparing installer…" else "Downloading update…",
                                    color = textSecondary,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = "${(animatedProgress * 100).toInt()}%",
                                    color = activeTheme.primaryColor,
                                    style = MaterialTheme.typography.labelLarge.tabularFigures()
                                )
                            }
                        }
                    }

                    // Inline error message if download fails
                    AnimatedVisibility(
                        visible = downloadError != null && !isDownloading,
                        enter = fadeIn(tween(160, easing = EchoMotion.EaseOutStrong)) + expandVertically(tween(180)),
                        exit = fadeOut(tween(120)) + shrinkVertically(tween(140))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                                .clip(RoundedCornerShape(EchoRadius.sm))
                                .background(EchoAccentCoral.copy(alpha = if (isDarkMode) 0.16f else 0.10f))
                                .border(0.6.dp, EchoAccentCoral.copy(alpha = 0.35f), RoundedCornerShape(EchoRadius.sm))
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ErrorOutline,
                                contentDescription = null,
                                tint = EchoAccentCoral,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = downloadError.orEmpty(),
                                color = textPrimary,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Primary Pill Action + Secondary Action
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isDownloading) activeTheme.primaryColor.copy(alpha = 0.45f)
                                    else activeTheme.primaryColor
                                )
                                .echoPressable(
                                    scaleOnPress = 0.96f,
                                    enabled = !isDownloading,
                                    hapticOnClick = true,
                                    onClick = { startUpdateDownload() }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            AnimatedContent(
                                targetState = Pair(isDownloading, downloadError != null),
                                transitionSpec = {
                                    fadeIn(tween(160, easing = EchoMotion.EaseOutStrong)) togetherWith fadeOut(tween(110))
                                },
                                label = "ota_cta_label"
                            ) { (downloading, hasErr) ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.SystemUpdate,
                                        contentDescription = null,
                                        tint = echoContrastOn(activeTheme.primaryColor),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = when {
                                            downloading -> "Downloading…"
                                            hasErr -> "Retry Update"
                                            else -> "Update Now"
                                        },
                                        color = echoContrastOn(activeTheme.primaryColor),
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }

                        if (!isDownloading && downloadError != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                                    .clip(CircleShape)
                                    .background(innerCardBg)
                                    .border(0.6.dp, borderColor, CircleShape)
                                    .echoPressable(scaleOnPress = 0.97f, hapticOnClick = true) {
                                        AppUpdateManager.openBrowserDownload(context, update.downloadUrl)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Download in Browser",
                                    color = textPrimary,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        } else if (!isDownloading && !update.isMandatory) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .clip(CircleShape)
                                    .echoPressable(scaleOnPress = 0.97f, onClick = onDismiss),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Later",
                                    color = textSecondary,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
