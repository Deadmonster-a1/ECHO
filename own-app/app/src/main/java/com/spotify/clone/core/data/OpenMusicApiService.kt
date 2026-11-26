package com.spotify.clone.core.data

import android.util.Log
import com.spotify.clone.core.model.Playlist
import com.spotify.clone.core.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Service integrating with open cloud music streaming infrastructure (Audius decentralized music network).
 * Provides access to millions of full-length tracks, trending playlists, artist catalogs,
 * and high-bitrate streaming audio with zero DRM restrictions.
 */
class OpenMusicApiService(
    private val appName: String = "SpotifyClone"
) {
    private val baseUrl = "https://api.audius.co"
    private val encodedAppName = URLEncoder.encode(appName, "UTF-8")

    private fun JSONObject.safeOptString(key: String, fallback: String = ""): String {
        if (isNull(key)) return fallback
        val value = optString(key, fallback).trim()
        return if (value.isEmpty() || value.equals("null", ignoreCase = true)) fallback else value
    }

    private fun openConnection(endpoint: String): HttpURLConnection {
        val target = if (endpoint.startsWith("http", ignoreCase = true)) endpoint else "$baseUrl$endpoint"
        val safeUrl = NetworkSecurity.enforceHttpsOrUpgradeKnownCdn(target)
            ?: throw SecurityException("Blocked non-HTTPS API endpoint")
        val url = URL(safeUrl)
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 10000
        connection.readTimeout = 15000
        connection.setRequestProperty("User-Agent", "SpotifyClone/1.0 (Android)")
        connection.setRequestProperty("Accept", "application/json")
        return connection
    }

    /**
     * Fetches globally trending tracks for the Home screen.
     */
    suspend fun getTrendingTracks(limit: Int = 25): List<Track> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<Track>()
        var conn: HttpURLConnection? = null
        try {
            conn = openConnection("/v1/tracks/trending?app_name=$encodedAppName&limit=$limit")
            if (conn.responseCode == 200) {
                val json = JSONObject(NetworkSecurity.readBoundedText(conn.inputStream))
                val data = json.optJSONArray("data") ?: return@withContext emptyList()
                for (i in 0 until data.length()) {
                    val obj = data.optJSONObject(i) ?: continue
                    parseTrack(obj)?.let { tracks.add(it) }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch trending tracks", e)
        } finally {
            conn?.disconnect()
        }
        tracks
    }

    /**
     * Searches the live global music catalog by query string.
     */
    suspend fun searchTracks(query: String, limit: Int = 20): List<Track> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<Track>()
        var conn: HttpURLConnection? = null
        try {
            val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
            conn = openConnection("/v1/tracks/search?query=$encodedQuery&app_name=$encodedAppName&limit=$limit")
            if (conn.responseCode == 200) {
                val json = JSONObject(NetworkSecurity.readBoundedText(conn.inputStream))
                val data = json.optJSONArray("data") ?: return@withContext emptyList()
                for (i in 0 until data.length()) {
                    val obj = data.optJSONObject(i) ?: continue
                    parseTrack(obj)?.let { tracks.add(it) }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to search tracks", e)
        } finally {
            conn?.disconnect()
        }
        tracks
    }

    /**
     * Fetches trending playlists with their tracks and high-res artwork.
     */
    suspend fun getTrendingPlaylists(limit: Int = 10): List<Playlist> = withContext(Dispatchers.IO) {
        val playlists = mutableListOf<Playlist>()
        var conn: HttpURLConnection? = null
        try {
            conn = openConnection("/v1/playlists/trending?app_name=$encodedAppName&limit=$limit")
            if (conn.responseCode == 200) {
                val json = JSONObject(NetworkSecurity.readBoundedText(conn.inputStream))
                val data = json.optJSONArray("data") ?: return@withContext emptyList()
                for (i in 0 until data.length()) {
                    val obj = data.optJSONObject(i) ?: continue
                    val id = obj.safeOptString("id")
                    if (id.isBlank()) continue
                    val name = obj.safeOptString("playlist_name", "Featured Playlist")
                    val desc = obj.safeOptString("description")
                    val user = obj.optJSONObject("user")
                    val owner = user?.safeOptString("name", "Curator") ?: "Curator"
                    val artwork = obj.optJSONObject("artwork")
                    val rawCover = artwork?.safeOptString("480x480")
                        ?.ifEmpty { artwork.safeOptString("150x150") }
                    val coverUrl = NetworkSecurity.enforceHttpsOrUpgradeKnownCdn(rawCover)
                        ?: "https://c.saavncdn.com/313/Devara-Part-1-Telugu-Telugu-2024-20240926171010-500x500.jpg"

                    val rawTracks = obj.optJSONArray("tracks")
                    val tracks = mutableListOf<Track>()
                    if (rawTracks != null) {
                        for (j in 0 until rawTracks.length()) {
                            val trackObj = rawTracks.optJSONObject(j) ?: continue
                            parseTrack(trackObj)?.let { tracks.add(it) }
                        }
                    }

                    playlists.add(
                        Playlist(
                            id = "audius_pl_$id",
                            name = name,
                            description = desc,
                            coverArtUrl = coverUrl,
                            ownerName = owner,
                            tracks = tracks
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch trending playlists", e)
        } finally {
            conn?.disconnect()
        }
        playlists
    }

    /**
     * Fetches tracks for a specific playlist ID.
     */
    suspend fun getPlaylistTracks(playlistId: String): List<Track> = withContext(Dispatchers.IO) {
        val cleanId = playlistId.removePrefix("audius_pl_").trim()
        if (cleanId.isBlank()) return@withContext emptyList()
        val tracks = mutableListOf<Track>()
        var conn: HttpURLConnection? = null
        try {
            val encodedId = URLEncoder.encode(cleanId, "UTF-8")
            conn = openConnection("/v1/playlists/$encodedId/tracks?app_name=$encodedAppName")
            if (conn.responseCode == 200) {
                val json = JSONObject(NetworkSecurity.readBoundedText(conn.inputStream))
                val data = json.optJSONArray("data") ?: return@withContext emptyList()
                for (i in 0 until data.length()) {
                    val obj = data.optJSONObject(i) ?: continue
                    parseTrack(obj)?.let { tracks.add(it) }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch playlist tracks", e)
        } finally {
            conn?.disconnect()
        }
        tracks
    }

    private fun parseTrack(obj: JSONObject): Track? {
        val id = obj.safeOptString("id")
        if (id.isBlank()) return null

        val title = obj.safeOptString("title", "Untitled")
        val user = obj.optJSONObject("user")
        val artist = user?.safeOptString("name", "Unknown Artist") ?: "Unknown Artist"
        val genre = obj.safeOptString("genre", "Music")

        val artwork = obj.optJSONObject("artwork")
        val rawArt = artwork?.safeOptString("480x480")
            ?.ifEmpty { artwork.safeOptString("150x150") }
        val artUrl = NetworkSecurity.enforceHttpsOrUpgradeKnownCdn(rawArt)
            ?: "https://c.saavncdn.com/041/Sita-Ramam-Telugu-Original-Motion-Picture-Soundtrack-Telugu-2022-20220802140738-500x500.jpg"

        val durationSec = obj.optLong("duration", 180)
        val encodedTrackId = URLEncoder.encode(id, "UTF-8")
        val streamUrl = "$baseUrl/v1/tracks/$encodedTrackId/stream?app_name=$encodedAppName"

        return Track(
            id = "audius_$id",
            title = title,
            artistName = artist,
            albumName = genre,
            coverArtUrl = artUrl,
            durationMs = durationSec * 1000L,
            mediaUri = streamUrl,
            isFavorite = false,
            isExplicit = false
        )
    }

    companion object {
        private const val TAG = "OpenMusicApiService"
    }
}
