package com.spotify.clone.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spotify.clone.core.designsystem.LocalAppTheme
import com.spotify.clone.core.designsystem.echoContrastOn
import com.spotify.clone.core.update.AppUpdateManager
import com.spotify.clone.core.update.UpdateInfo
import kotlinx.coroutines.launch

/**
 * Shared in-app OTA update prompt with direct APK download progress and PackageInstaller launch.
 */
@Composable
fun AppUpdateDialog(
    update: UpdateInfo,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activeTheme = LocalAppTheme.current
    val scope = rememberCoroutineScope()
    var isDownloading by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }

    AlertDialog(
        onDismissRequest = { if (!isDownloading && !update.isMandatory) onDismiss() },
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = update.releaseTitle.ifBlank { "Update Available (v${update.versionName})" },
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Version ${update.versionName}",
                    color = activeTheme.primaryColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = update.releaseNotes,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.5.sp,
                    lineHeight = 19.sp
                )
                if (isDownloading) {
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = activeTheme.primaryColor
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Downloading update...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            color = activeTheme.primaryColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isDownloading,
                onClick = {
                    if (!AppUpdateManager.canRequestPackageInstalls(context)) {
                        Toast.makeText(context, "Allow echo to install app updates", Toast.LENGTH_LONG).show()
                        AppUpdateManager.openInstallPermissionSettings(context)
                        return@Button
                    }
                    isDownloading = true
                    progress = 0.05f
                    scope.launch {
                        val apkFile = AppUpdateManager.downloadApk(context, update.downloadUrl) { p ->
                            progress = p
                        }
                        isDownloading = false
                        if (apkFile != null && apkFile.exists() && apkFile.length() > 1_000_000L) {
                            onDismiss()
                            AppUpdateManager.installApk(context, apkFile)
                        } else {
                            Toast.makeText(context, "Download failed. Opening release link...", Toast.LENGTH_SHORT).show()
                            AppUpdateManager.openBrowserDownload(context, update.downloadUrl)
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = activeTheme.primaryColor,
                    contentColor = echoContrastOn(activeTheme.primaryColor)
                )
            ) {
                Text(if (isDownloading) "Downloading..." else "Update Now")
            }
        },
        dismissButton = {
            if (!isDownloading && !update.isMandatory) {
                TextButton(onClick = onDismiss) {
                    Text("Later")
                }
            }
        }
    )
}
