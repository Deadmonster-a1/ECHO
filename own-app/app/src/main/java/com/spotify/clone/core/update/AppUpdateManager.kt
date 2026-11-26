package com.spotify.clone.core.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.spotify.clone.core.data.NetworkSecurity
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
    val isMandatory: Boolean = false,
    val sha256: String = ""
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
     * The entry URL must be a release asset of OUR repo. Host-only checks would
     * accept any github.com/raw.githubusercontent.com path from any account.
     * Redirect hops are still checked by host in [downloadApk].
     */
    fun isOwnReleaseAsset(url: String?): Boolean {
        val prefix = "https://github.com/$githubOwner/$githubRepo/releases/download/"
        return url != null && url.trim().startsWith(prefix, ignoreCase = true) &&
            !url.contains("/../") && NetworkSecurity.isTrustedUpdateUrl(url)
    }

    fun getInstalledVersionName(context: Context): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    fun getInstalledVersionCode(context: Context): Long {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode.toLong()
            }
        } catch (e: Exception) {
            1L
        }
    }

    suspend fun checkForUpdates(currentVersion: String): Result<UpdateInfo?> = checkForUpdates(null, currentVersion)

    /**
     * Checks for updates using a multi-tier fallback strategy:
     * 1. Ultra-fast global Edge CDN (jsDelivr - unblocked across all Indian & global ISPs, zero rate limits).
     * 2. Direct raw version.json from GitHub with anti-caching query tokens.
     * 3. Official GitHub Releases API (releases/latest).
     */
    private fun JSONObject.safeOptString(key: String, fallback: String = ""): String {
        if (isNull(key)) return fallback
        val value = optString(key, fallback).trim()
        return if (value.isEmpty() || value.equals("null", ignoreCase = true)) fallback else value
    }

    suspend fun checkForUpdates(
        context: Context? = null,
        currentVersion: String = "1.0.0"
    ): Result<UpdateInfo?> = withContext(Dispatchers.IO) {
        val cleanCurrent = currentVersion.removePrefix("v").trim()
        val currentCode = context?.let { getInstalledVersionCode(it) } ?: -1L
        val timestamp = System.currentTimeMillis()

        // Strategy 1: Ultra-fast Global Edge CDN (jsDelivr)
        try {
            val cdnUrl = "https://cdn.jsdelivr.net/gh/$githubOwner/$githubRepo@main/version.json?t=$timestamp"
            val cdnOutcome = fetchVersionFromJson(cdnUrl, cleanCurrent, currentCode)
            if (cdnOutcome != null) {
                Log.d(TAG, "Checked via jsDelivr CDN: update=${cdnOutcome.update?.versionName ?: "up-to-date"}")
                return@withContext Result.success(cdnOutcome.update)
            }
        } catch (e: Exception) {
            Log.d(TAG, "jsDelivr CDN check failed, trying raw GitHub: ${e.message}")
        }

        // Strategy 2: Fast raw version.json from GitHub
        try {
            val rawUrl = "https://raw.githubusercontent.com/$githubOwner/$githubRepo/main/version.json?t=$timestamp"
            val rawOutcome = fetchVersionFromJson(rawUrl, cleanCurrent, currentCode)
            if (rawOutcome != null) {
                Log.d(TAG, "Checked via raw GitHub: update=${rawOutcome.update?.versionName ?: "up-to-date"}")
                return@withContext Result.success(rawOutcome.update)
            }
        } catch (e: Exception) {
            Log.d(TAG, "Raw version.json not available, falling back to GitHub Releases API: ${e.message}")
        }

        // Strategy 3: GitHub Releases API
        var connection: HttpURLConnection? = null
        try {
            val apiUrl = "https://api.github.com/repos/$githubOwner/$githubRepo/releases/latest"
            val url = URL(apiUrl)
            connection = (url.openConnection() as HttpURLConnection).apply {
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

            val responseBody = NetworkSecurity.readBoundedText(connection.inputStream)
            val json = JSONObject(responseBody)

            val rawTagName = json.safeOptString("tag_name")
            val releaseTitle = json.safeOptString("name", rawTagName)
            val releaseNotes = json.safeOptString("body", "No changelog provided.")
            val publishedAt = json.safeOptString("published_at")
            val remoteVersion = rawTagName.removePrefix("v").trim()

            var apkDownloadUrl = ""
            var apkSize = 0L
            val assets = json.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.optJSONObject(i) ?: continue
                    val name = asset.safeOptString("name")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        val candidateUrl = asset.safeOptString("browser_download_url")
                        if (isOwnReleaseAsset(candidateUrl)) {
                            apkDownloadUrl = candidateUrl
                            apkSize = asset.optLong("size", 0L)
                            break
                        }
                    }
                }
            }

            if (isNewerVersion(remoteVersion, cleanCurrent) && isOwnReleaseAsset(apkDownloadUrl)) {
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
        } finally {
            connection?.disconnect()
        }
    }

    private data class JsonCheckOutcome(val update: UpdateInfo?)

    private fun fetchVersionFromJson(rawUrl: String, currentVersion: String, currentCode: Long = -1L): JsonCheckOutcome? {
        val connection = (URL(rawUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 7000
            readTimeout = 7000
            useCaches = false
            defaultUseCaches = false
            requestMethod = "GET"
            setRequestProperty("User-Agent", "ECHO-Android-Updater")
            setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
            setRequestProperty("Pragma", "no-cache")
        }

        try {
            if (connection.responseCode != 200) return null

            val response = NetworkSecurity.readBoundedText(connection.inputStream)
            val json = JSONObject(response)

            val remoteVersion = json.safeOptString("versionName").removePrefix("v").trim()
            val remoteCode = json.optLong("versionCode", 0L)
            val title = json.safeOptString("releaseTitle", "echo v$remoteVersion Update")
            val notes = json.safeOptString("releaseNotes", "Bug fixes and performance improvements.")
            val downloadUrl = json.safeOptString("downloadUrl")
            val size = json.optLong("apkSize", 0L)
            val mandatory = json.optBoolean("mandatory", false)
            val sha256 = json.safeOptString("sha256").lowercase()

            val isCodeNewer = currentCode > 0L && remoteCode > currentCode
            val isNameNewer = isNewerVersion(remoteVersion, currentVersion)

            val update = if ((isCodeNewer || isNameNewer) && isOwnReleaseAsset(downloadUrl)) {
                UpdateInfo(
                    versionName = remoteVersion,
                    releaseTitle = title,
                    releaseNotes = notes,
                    downloadUrl = downloadUrl,
                    apkSize = size,
                    isMandatory = mandatory,
                    sha256 = sha256
                )
            } else {
                null
            }
            return JsonCheckOutcome(update)
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Downloads the APK file into private internal cache (`cacheDir/updates`) with strict HTTPS
     * host verification on every redirect and package/signature validation before installation.
     */
    suspend fun downloadApk(
        context: Context,
        downloadUrl: String,
        expectedSha256: String = "",
        onProgress: (Float) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        if (!isOwnReleaseAsset(downloadUrl)) {
            Log.e(TAG, "Blocked untrusted OTA download URL")
            return@withContext null
        }

        val appContext = context.applicationContext
        var finalConnection: HttpURLConnection? = null
        var tempFile: File? = null
        try {
            var currentUrl = downloadUrl
            var redirectCount = 0
            val maxRedirects = 6

            while (redirectCount < maxRedirects) {
                if (!NetworkSecurity.isTrustedUpdateUrl(currentUrl, allowCdnRedirects = true)) {
                    throw SecurityException("Blocked untrusted redirect host or non-HTTPS scheme")
                }

                val url = URL(currentUrl)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 20000
                    readTimeout = 60000
                    instanceFollowRedirects = false // Handle explicitly to enforce HTTPS host policy per hop
                    setRequestProperty("User-Agent", "ECHO-Android-Updater")
                    setRequestProperty("Accept", "*/*")
                }

                try {
                    val status = conn.responseCode
                    if (status == HttpURLConnection.HTTP_MOVED_TEMP ||
                        status == HttpURLConnection.HTTP_MOVED_PERM ||
                        status == HttpURLConnection.HTTP_SEE_OTHER ||
                        status == 307 || status == 308
                    ) {
                        val location = conn.getHeaderField("Location")
                        conn.disconnect()
                        if (!location.isNullOrBlank()) {
                            val nextUrl = if (location.startsWith("http", ignoreCase = true)) {
                                location
                            } else {
                                URL(url, location).toString()
                            }
                            if (!NetworkSecurity.isTrustedUpdateUrl(nextUrl, allowCdnRedirects = true)) {
                                throw SecurityException("Blocked redirect to untrusted update host")
                            }
                            currentUrl = nextUrl
                            redirectCount++
                            continue
                        }
                    }

                    if (status == HttpURLConnection.HTTP_OK) {
                        finalConnection = conn
                        break
                    } else {
                        conn.disconnect()
                        throw Exception("HTTP Error: $status")
                    }
                } catch (e: Exception) {
                    if (finalConnection !== conn) conn.disconnect()
                    throw e
                }
            }

            val conn = finalConnection ?: throw Exception("Failed to follow download redirects")
            val totalSize = conn.contentLength.toLong()
            val updatesDir = File(appContext.cacheDir, "updates").apply { if (!exists()) mkdirs() }
            val destinationFile = File(updatesDir, "echo.apk")
            val stagingFile = File(updatesDir, "echo.apk.tmp").also { tempFile = it }
            if (stagingFile.exists()) stagingFile.delete()

            conn.inputStream.use { input ->
                FileOutputStream(stagingFile).use { output ->
                    val buffer = ByteArray(32768)
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
                    output.flush()
                }
            }

            val isValidSize = stagingFile.exists() && stagingFile.length() > 1_000_000L
            val isShaValid = expectedSha256.isBlank() ||
                NetworkSecurity.sha256Hex(stagingFile).equals(expectedSha256.trim(), ignoreCase = true)
            val isPackageValid = isValidSize && isShaValid && verifyApkArchive(appContext, stagingFile)

            if (isPackageValid) {
                if (destinationFile.exists()) destinationFile.delete()
                if (!stagingFile.renameTo(destinationFile)) {
                    stagingFile.copyTo(destinationFile, overwrite = true)
                    stagingFile.delete()
                }
                destinationFile
            } else {
                val len = stagingFile.length()
                stagingFile.delete()
                Log.e(TAG, "Downloaded APK rejected (size=$len, shaOk=$isShaValid, pkgOk=$isPackageValid)")
                null
            }
        } catch (e: Exception) {
            tempFile?.let { if (it.exists()) it.delete() }
            Log.e(TAG, "Download failed: ${e.message}", e)
            null
        } finally {
            finalConnection?.disconnect()
        }
    }

    /**
     * Verifies the downloaded file is a signed APK matching this app's package name.
     */
    @Suppress("DEPRECATION")
    private fun verifyApkArchive(context: Context, apkFile: File): Boolean {
        return try {
            val pm = context.packageManager
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageManager.GET_SIGNING_CERTIFICATES
            } else {
                PackageManager.GET_SIGNATURES
            }
            val archiveInfo = pm.getPackageArchiveInfo(apkFile.absolutePath, flags) ?: return false
            if (archiveInfo.packageName != context.packageName) return false

            // The update must be signed by the same certificate as the installed app,
            // otherwise reject before handing it to the installer.
            val installed = pm.getPackageInfo(context.packageName, flags)
            val (archiveCerts, installedCerts) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val a = archiveInfo.signingInfo ?: return false
                val i = installed.signingInfo ?: return false
                // apkContentsSigners for the archive; signingCertificateHistory lets a
                // future key rotation (v3 lineage) still match the installed signer.
                val history: List<android.content.pm.Signature> = i.signingCertificateHistory?.toList() ?: emptyList()
                val current: List<android.content.pm.Signature> = i.apkContentsSigners?.toList() ?: emptyList()
                (a.apkContentsSigners?.toList() ?: emptyList()) to (history + current)
            } else {
                (archiveInfo.signatures?.toList() ?: emptyList()) to (installed.signatures?.toList() ?: emptyList())
            }
            archiveCerts.isNotEmpty() && archiveCerts.any { it in installedCerts }
        } catch (e: Exception) {
            Log.e(TAG, "APK verification error: ${e.message}")
            false
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
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
            }
            context.startActivity(installIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch package installer: ${e.message}", e)
            openBrowserDownload(context, "https://github.com/$githubOwner/$githubRepo/releases/latest")
        }
    }

    fun openBrowserDownload(context: Context, url: String) {
        if (!NetworkSecurity.isTrustedUpdateUrl(url, allowCdnRedirects = true)) {
            Log.e(TAG, "Blocked untrusted browser download URL")
            return
        }
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open browser download: ${e.message}")
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
        val digitRegex = Regex("^\\d+")
        val rParts = remote.split(".").mapNotNull { digitRegex.find(it.trim())?.value?.toIntOrNull() }
        val cParts = current.split(".").mapNotNull { digitRegex.find(it.trim())?.value?.toIntOrNull() }
        if (rParts.isEmpty() || cParts.isEmpty()) return false

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
