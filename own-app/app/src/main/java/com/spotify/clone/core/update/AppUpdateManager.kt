package com.spotify.clone.core.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val versionName: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val apkSize: Long = 0L,
    val publishedAt: String = "",
    val isMandatory: Boolean = false
)

object AppUpdateManager {

    private const val TAG = "EchoUpdateManager"
    private const val PREFS_NAME = "echo_updates"
    private const val KEY_LAST_CHECK = "last_check_timestamp"
    private const val CHECK_INTERVAL_MS = 24 * 60 * 60 * 1000L // 24 hours

    // Default GitHub repository for ECHO live releases (can be customized)
    var githubOwner: String = "Deadmonster-a1"
    var githubRepo: String = "ECHO"

    /**
     * Checks for updates using a dual-strategy:
     * 1. Direct CDN check from raw version.json (Zero rate limits, instant updates).
     * 2. Fallback to GitHub Releases API (releases/latest).
     */
    suspend fun checkForUpdates(currentVersion: String = "1.0.0"): Result<UpdateInfo?> = withContext(Dispatchers.IO) {
        val cleanCurrent = currentVersion.removePrefix("v").trim()

        // Strategy 1: Fast raw version.json (Zero rate limit)
        try {
            val rawUrl = "https://raw.githubusercontent.com/$githubOwner/$githubRepo/main/version.json"
            val rawResult = fetchVersionFromJson(rawUrl, cleanCurrent)
            if (rawResult != null) {
                return@withContext Result.success(rawResult)
            }
        } catch (e: Exception) {
            Log.d(TAG, "Raw version.json not available, falling back to GitHub Releases API")
        }

        // Strategy 2: GitHub Releases API
        try {
            val apiUrl = "https://api.github.com/repos/$githubOwner/$githubRepo/releases/latest"
            val url = URL(apiUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 10000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "ECHO-Android-Updater")
            }

            val responseCode = connection.responseCode
            if (responseCode == 404) {
                Log.d(TAG, "No GitHub releases found yet for $githubOwner/$githubRepo")
                return@withContext Result.success(null)
            }
            if (responseCode != 200) {
                return@withContext Result.failure(Exception("GitHub API error: HTTP $responseCode"))
            }

            val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(responseBody)

            val rawTagName = json.optString("tag_name", "")
            val releaseTitle = json.optString("name", rawTagName)
            val releaseNotes = json.optString("body", "No changelog provided.")
            val publishedAt = json.optString("published_at", "")
            val remoteVersion = rawTagName.removePrefix("v").trim()

            var apkDownloadUrl = ""
            var apkSize = 0L
            val assets = json.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkDownloadUrl = asset.optString("browser_download_url", "")
                        apkSize = asset.optLong("size", 0L)
                        break
                    }
                }
            }

            if (isNewerVersion(remoteVersion, cleanCurrent) && apkDownloadUrl.isNotBlank()) {
                val updateInfo = UpdateInfo(
                    versionName = remoteVersion,
                    releaseTitle = releaseTitle,
                    releaseNotes = releaseNotes,
                    downloadUrl = apkDownloadUrl,
                    apkSize = apkSize,
                    publishedAt = publishedAt,
                    isMandatory = false
                )
                Result.success(updateInfo)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Update check failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun fetchVersionFromJson(rawUrl: String, currentVersion: String): UpdateInfo? {
        val connection = (URL(rawUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 5000
            readTimeout = 5000
            requestMethod = "GET"
            setRequestProperty("User-Agent", "ECHO-Android-Updater")
        }

        if (connection.responseCode != 200) return null

        val response = connection.inputStream.bufferedReader().use { it.readText() }
        val json = JSONObject(response)

        val remoteVersion = json.optString("versionName", "").removePrefix("v").trim()
        val title = json.optString("releaseTitle", "ECHO v$remoteVersion Update")
        val notes = json.optString("releaseNotes", "Bug fixes and performance improvements.")
        val downloadUrl = json.optString("downloadUrl", "")
        val size = json.optLong("apkSize", 0L)
        val mandatory = json.optBoolean("mandatory", false)

        if (isNewerVersion(remoteVersion, currentVersion) && downloadUrl.isNotBlank()) {
            return UpdateInfo(
                versionName = remoteVersion,
                releaseTitle = title,
                releaseNotes = notes,
                downloadUrl = downloadUrl,
                apkSize = size,
                isMandatory = mandatory
            )
        }
        return null
    }

    /**
     * Downloads the APK file to the application's external cache directory.
     */
    suspend fun downloadApk(
        context: Context,
        downloadUrl: String,
        onProgress: (Float) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        try {
            val url = URL(downloadUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 30000
                instanceFollowRedirects = true
            }

            val totalSize = connection.contentLength
            val cacheDir = context.externalCacheDir ?: context.cacheDir
            val destinationFile = File(cacheDir, "echo_update.apk")
            if (destinationFile.exists()) destinationFile.delete()

            connection.inputStream.use { input ->
                FileOutputStream(destinationFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalRead = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        if (totalSize > 0) {
                            val progress = (totalRead.toFloat() / totalSize.toFloat()).coerceIn(0f, 1f)
                            onProgress(progress)
                        }
                    }
                }
            }

            destinationFile
        } catch (e: Exception) {
            Log.e(TAG, "Download failed: ${e.message}", e)
            null
        }
    }

    /**
     * Checks if the app has permission to install unknown APKs on Android 8.0+.
     */
    fun canRequestPackageInstalls(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    /**
     * Opens system Settings to allow installing unknown apps from ECHO.
     */
    fun openInstallPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val intent = Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open install settings: ${e.message}")
            }
        }
    }

    /**
     * Launches the Android PackageInstaller via FileProvider to install the downloaded APK.
     */
    fun installApk(context: Context, apkFile: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(installIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch package installer: ${e.message}", e)
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/$githubOwner/$githubRepo/releases/latest")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(browserIntent)
            } catch (ignored: Exception) {}
        }
    }

    /**
     * Returns true if 24 hours have passed since the last background check.
     */
    fun shouldCheckAutomatically(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastCheck = prefs.getLong(KEY_LAST_CHECK, 0L)
        val now = System.currentTimeMillis()
        return (now - lastCheck) > CHECK_INTERVAL_MS
    }

    /**
     * Records timestamp of successful update check.
     */
    fun recordUpdateCheck(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_LAST_CHECK, System.currentTimeMillis()).apply()
    }

    /**
     * Semantic version comparator: returns true if remote > current.
     */
    fun isNewerVersion(remote: String, current: String): Boolean {
        if (remote.isBlank() || current.isBlank()) return false
        val rParts = remote.split(".").mapNotNull { it.toIntOrNull() }
        val cParts = current.split(".").mapNotNull { it.toIntOrNull() }

        val maxLen = maxOf(rParts.size, cParts.size)
        for (i in 0 until maxLen) {
            val r = rParts.getOrElse(i) { 0 }
            val c = cParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }
}
