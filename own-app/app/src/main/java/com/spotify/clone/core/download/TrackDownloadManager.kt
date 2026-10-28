package com.spotify.clone.core.download

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Environment
import android.util.Log
import com.spotify.clone.core.data.FullTrackStreamResolver
import com.spotify.clone.core.model.Playlist
import com.spotify.clone.core.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * High-performance, offline audio download manager for ECHO.
 * Handles background streaming downloads, file storage, metadata caching,
 * and offline playback resolution.
 */
class TrackDownloadManager private constructor(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val prefs: SharedPreferences =
        context.getSharedPreferences("echo_offline_downloads", Context.MODE_PRIVATE)

    private val _downloadedTrackIds = MutableStateFlow<Set<String>>(emptySet())
    val downloadedTrackIds: StateFlow<Set<String>> = _downloadedTrackIds.asStateFlow()

    private val _downloadedPlaylistIds = MutableStateFlow<Set<String>>(emptySet())
    val downloadedPlaylistIds: StateFlow<Set<String>> = _downloadedPlaylistIds.asStateFlow()

    private val _activeDownloads = MutableStateFlow<Map<String, Float>>(emptyMap())
    val activeDownloads: StateFlow<Map<String, Float>> = _activeDownloads.asStateFlow()

    private val downloadDir: File
        get() {
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
                ?: File(context.filesDir, "offline_tracks")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    init {
        loadPersistedState()
    }

    private fun getTrackFileName(trackId: String): String =
        trackId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_") + ".mp3"

    private fun loadPersistedState() {
        val tracksSet = prefs.getStringSet(KEY_DOWNLOADED_TRACK_IDS, emptySet()) ?: emptySet()
        // Verify files actually exist on disk
        val validTrackIds = tracksSet.filter { trackId ->
            val file = File(downloadDir, getTrackFileName(trackId))
            file.exists() && file.length() > 50_000L
        }.toSet()

        if (validTrackIds.size != tracksSet.size) {
            prefs.edit().putStringSet(KEY_DOWNLOADED_TRACK_IDS, validTrackIds).apply()
        }
        _downloadedTrackIds.value = validTrackIds

        val playlistSet = prefs.getStringSet(KEY_DOWNLOADED_PLAYLIST_IDS, emptySet()) ?: emptySet()
        _downloadedPlaylistIds.value = playlistSet
    }

    fun isTrackDownloaded(trackId: String): Boolean {
        return _downloadedTrackIds.value.contains(trackId)
    }

    fun isPlaylistDownloaded(playlistId: String): Boolean {
        return _downloadedPlaylistIds.value.contains(playlistId)
    }

    fun isTrackDownloading(trackId: String): Boolean {
        return _activeDownloads.value.containsKey(trackId)
    }

    fun getDownloadedTrackFile(trackId: String): File? {
        val file = File(downloadDir, getTrackFileName(trackId))
        return if (file.exists() && file.length() > 50_000L) file else null
    }

    suspend fun ensureDownloadedTrackFile(track: Track): File? {
        val existing = getDownloadedTrackFile(track.id)
        if (existing != null) return existing
        val success = performTrackDownload(track)
        return if (success) getDownloadedTrackFile(track.id) else null
    }

    fun resolveTrackForPlayback(track: Track): Track {
        val localFile = getDownloadedTrackFile(track.id)
        return if (localFile != null && localFile.exists() && localFile.length() > 50_000L) {
            val localUri = Uri.fromFile(localFile).toString()
            track.copy(mediaUri = localUri)
        } else {
            track
        }
    }

    fun downloadTrack(track: Track, onComplete: ((Boolean) -> Unit)? = null) {
        if (isTrackDownloaded(track.id)) {
            onComplete?.invoke(true)
            return
        }

        scope.launch {
            val success = performTrackDownload(track)
            withContext(Dispatchers.Main) {
                onComplete?.invoke(success)
            }
        }
    }

    private fun openDownloadConnection(urlStr: String): HttpURLConnection {
        val url = URL(urlStr)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 15000
            readTimeout = 30000
            instanceFollowRedirects = true
            setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
            )
            if (urlStr.contains("saavncdn.com") || urlStr.contains("jiosaavn.com")) {
                setRequestProperty("Referer", "https://www.jiosaavn.com/")
            }
            setRequestProperty("Accept", "*/*")
        }
        return conn
    }

    private suspend fun performTrackDownload(track: Track): Boolean = withContext(Dispatchers.IO) {
        try {
            _activeDownloads.update { it + (track.id to 0.05f) }

            // 1. Resolve raw audio stream URL
            var streamUrl: String? = track.mediaUri
            if (FullTrackStreamResolver.needsResolution(track)) {
                streamUrl = if (track.mediaUri.startsWith("saavn_enc:")) {
                    val encUrl = track.mediaUri.removePrefix("saavn_enc:")
                    FullTrackStreamResolver.resolveAuthToken(encUrl, bitrate = "320")
                        ?: FullTrackStreamResolver.resolveAuthToken(encUrl, bitrate = "160")
                        ?: FullTrackStreamResolver.resolveStreamUrl(track.title, track.artistName, track.albumName)
                } else {
                    FullTrackStreamResolver.resolveStreamUrl(track.title, track.artistName, track.albumName)
                }
            }

            if (streamUrl.isNullOrBlank() || streamUrl == "false" || streamUrl.startsWith("saavn_enc:") || streamUrl.startsWith("spotify:")) {
                streamUrl = FullTrackStreamResolver.resolveStreamUrl(track.title, track.artistName, track.albumName)
            }

            if (streamUrl.isNullOrBlank() || streamUrl == "false") {
                Log.e(TAG, "Failed to resolve stream URL for track: ${track.title}")
                _activeDownloads.update { it - track.id }
                return@withContext false
            }

            // 2. Build candidate URLs for downloading (prefer 320kbps -> 160kbps -> base -> original)
            val candidateUrls = mutableListOf<String>()
            if (streamUrl.contains("saavncdn.com")) {
                val cleanBase = streamUrl.substringBefore("?")
                candidateUrls.add(cleanBase.replace(Regex("_(96|160)\\.mp4"), "_320.mp4"))
                candidateUrls.add(cleanBase.replace(Regex("_(96|320)\\.mp4"), "_160.mp4"))
                candidateUrls.add(cleanBase)
                candidateUrls.add(streamUrl)
            } else {
                candidateUrls.add(streamUrl)
            }

            // 3. Connect to working URL
            var activeConnection: HttpURLConnection? = null
            var chosenCandidate: String? = null
            for (candidate in candidateUrls.distinct()) {
                try {
                    val conn = openDownloadConnection(candidate)
                    val code = conn.responseCode
                    if (code in 200..299) {
                        activeConnection = conn
                        chosenCandidate = candidate
                        break
                    } else {
                        conn.disconnect()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Download connection attempt failed for $candidate: ${e.message}")
                }
            }

            if (activeConnection == null) {
                Log.e(TAG, "All download candidate URLs failed for track: ${track.title}")
                _activeDownloads.update { it - track.id }
                return@withContext false
            }

            val connection = activeConnection
            val safeFileName = getTrackFileName(track.id)
            val tempFile = File(downloadDir, "$safeFileName.tmp")
            val targetFile = File(downloadDir, safeFileName)

            val totalSize = connection.contentLength.toLong()

            connection.inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(32768)
                    var bytesRead: Int
                    var totalRead = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        if (totalSize > 0) {
                            val progress = (totalRead.toFloat() / totalSize.toFloat()).coerceIn(0f, 1f)
                            _activeDownloads.update { it + (track.id to progress) }
                        }
                    }
                    output.flush()
                }
            }
            connection.disconnect()

            if (tempFile.exists() && tempFile.length() > 50_000L) {
                if (targetFile.exists()) targetFile.delete()
                tempFile.renameTo(targetFile)
                targetFile.setReadable(true, false)

                // Save metadata
                saveTrackMetadata(track)

                // Update downloaded set
                val updated = _downloadedTrackIds.value + track.id
                _downloadedTrackIds.value = updated
                prefs.edit().putStringSet(KEY_DOWNLOADED_TRACK_IDS, updated).apply()

                _activeDownloads.update { it - track.id }
                Log.d(TAG, "Successfully downloaded: ${track.title} (${targetFile.length()} bytes) from $chosenCandidate")
                true
            } else {
                tempFile.delete()
                _activeDownloads.update { it - track.id }
                Log.e(TAG, "Downloaded track too small or corrupt: ${tempFile.length()} bytes")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading track ${track.title}: ${e.message}", e)
            _activeDownloads.update { it - track.id }
            false
        }
    }

    fun removeTrackDownload(trackId: String) {
        scope.launch {
            val file = File(downloadDir, getTrackFileName(trackId))
            if (file.exists()) file.delete()

            prefs.edit().remove("track_meta_$trackId").apply()

            val updated = _downloadedTrackIds.value - trackId
            _downloadedTrackIds.value = updated
            prefs.edit().putStringSet(KEY_DOWNLOADED_TRACK_IDS, updated).apply()
        }
    }

    fun downloadPlaylist(
        playlist: Playlist,
        onProgress: ((completed: Int, total: Int) -> Unit)? = null,
        onComplete: ((successCount: Int) -> Unit)? = null
    ) {
        scope.launch {
            savePlaylistMetadata(playlist)
            val updatedPlaylists = _downloadedPlaylistIds.value + playlist.id
            _downloadedPlaylistIds.value = updatedPlaylists
            prefs.edit().putStringSet(KEY_DOWNLOADED_PLAYLIST_IDS, updatedPlaylists).apply()

            var successCount = 0
            val total = playlist.tracks.size

            playlist.tracks.forEachIndexed { index, track ->
                val ok = if (isTrackDownloaded(track.id)) true else performTrackDownload(track)
                if (ok) successCount++
                withContext(Dispatchers.Main) {
                    onProgress?.invoke(index + 1, total)
                }
            }

            withContext(Dispatchers.Main) {
                onComplete?.invoke(successCount)
            }
        }
    }

    fun removePlaylistDownload(playlistId: String) {
        scope.launch {
            val playlist = getDownloadedPlaylist(playlistId)
            playlist?.tracks?.forEach { track ->
                // Remove individual track file
                removeTrackDownload(track.id)
            }

            prefs.edit().remove("playlist_meta_$playlistId").apply()

            val updatedPlaylists = _downloadedPlaylistIds.value - playlistId
            _downloadedPlaylistIds.value = updatedPlaylists
            prefs.edit().putStringSet(KEY_DOWNLOADED_PLAYLIST_IDS, updatedPlaylists).apply()
        }
    }

    private fun saveTrackMetadata(track: Track) {
        try {
            val json = JSONObject().apply {
                put("id", track.id)
                put("title", track.title)
                put("artistName", track.artistName)
                put("albumName", track.albumName)
                put("coverArtUrl", track.coverArtUrl)
                put("durationMs", track.durationMs)
                put("isExplicit", track.isExplicit)
                put("isFavorite", track.isFavorite)
            }
            prefs.edit().putString("track_meta_${track.id}", json.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save track metadata", e)
        }
    }

    private fun savePlaylistMetadata(playlist: Playlist) {
        try {
            val json = JSONObject().apply {
                put("id", playlist.id)
                put("name", playlist.name)
                put("description", playlist.description)
                put("coverArtUrl", playlist.coverArtUrl)
                put("ownerName", playlist.ownerName)
                val trackIds = JSONArray()
                playlist.tracks.forEach {
                    trackIds.put(it.id)
                    saveTrackMetadata(it)
                }
                put("trackIds", trackIds)
            }
            prefs.edit().putString("playlist_meta_${playlist.id}", json.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save playlist metadata", e)
        }
    }

    fun getDownloadedPlaylist(playlistId: String): Playlist? {
        val jsonStr = prefs.getString("playlist_meta_$playlistId", null) ?: return null
        return try {
            val json = JSONObject(jsonStr)
            val trackIds = json.optJSONArray("trackIds") ?: JSONArray()
            val tracks = mutableListOf<Track>()
            for (i in 0 until trackIds.length()) {
                val tid = trackIds.getString(i)
                getDownloadedTrack(tid)?.let { tracks.add(it) }
            }
            Playlist(
                id = json.getString("id"),
                name = json.getString("name"),
                description = json.optString("description", ""),
                coverArtUrl = json.optString("coverArtUrl", ""),
                ownerName = json.optString("ownerName", "ECHO"),
                tracks = tracks
            )
        } catch (e: Exception) {
            null
        }
    }

    fun getDownloadedTrack(trackId: String): Track? {
        val jsonStr = prefs.getString("track_meta_$trackId", null) ?: return null
        return try {
            val json = JSONObject(jsonStr)
            val localFile = getDownloadedTrackFile(trackId)
            val mediaUri = localFile?.let { Uri.fromFile(it).toString() } ?: ""
            Track(
                id = json.getString("id"),
                title = json.getString("title"),
                artistName = json.optString("artistName", "Unknown Artist"),
                albumName = json.optString("albumName", "Unknown Album"),
                coverArtUrl = json.optString("coverArtUrl", ""),
                durationMs = json.optLong("durationMs", 0L),
                mediaUri = mediaUri,
                isExplicit = json.optBoolean("isExplicit", false),
                isFavorite = json.optBoolean("isFavorite", false),
                lyrics = emptyList()
            )
        } catch (e: Exception) {
            null
        }
    }

    fun getAllDownloadedTracks(): List<Track> {
        val ids = _downloadedTrackIds.value
        return ids.mapNotNull { getDownloadedTrack(it) }
    }

    fun getAllDownloadedPlaylists(): List<Playlist> {
        val ids = _downloadedPlaylistIds.value
        return ids.mapNotNull { getDownloadedPlaylist(it) }
    }

    companion object {
        private const val TAG = "TrackDownloadManager"
        private const val KEY_DOWNLOADED_TRACK_IDS = "downloaded_track_ids"
        private const val KEY_DOWNLOADED_PLAYLIST_IDS = "downloaded_playlist_ids"

        @Volatile
        private var instance: TrackDownloadManager? = null

        fun getInstance(context: Context): TrackDownloadManager {
            return instance ?: synchronized(this) {
                instance ?: TrackDownloadManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
