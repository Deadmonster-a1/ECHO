package com.spotify.clone.core.data

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

    private fun openConnection(endpoint: String): HttpURLConnection {
        val url = URL(if (endpoint.startsWith("http")) endpoint else "$baseUrl$endpoint")
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
        try {
            val conn = openConnection("/v1/tracks/trending?app_name=$appName&limit=$limit")
            if (conn.responseCode == 200) {
                val json = JSONObject(conn.inputStream.bufferedReader().readText())
                val data = json.optJSONArray("data") ?: return@withContext emptyList()
                for (i in 0 until data.length()) {
                    val obj = data.optJSONObject(i) ?: continue
                    parseTrack(obj)?.let { tracks.add(it) }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        tracks
    }

    /**
     * Searches the live global music catalog by query string.
     */
    suspend fun searchTracks(query: String, limit: Int = 20): List<Track> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<Track>()
        try {
            val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
            val conn = openConnection("/v1/tracks/search?query=$encodedQuery&app_name=$appName&limit=$limit")
            if (conn.responseCode == 200) {
                val json = JSONObject(conn.inputStream.bufferedReader().readText())
                val data = json.optJSONArray("data") ?: return@withContext emptyList()
                for (i in 0 until data.length()) {
                    val obj = data.optJSONObject(i) ?: continue
                    parseTrack(obj)?.let { tracks.add(it) }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        tracks
    }

    /**
     * Fetches trending playlists with their tracks and high-res artwork.
     */
    suspend fun getTrendingPlaylists(limit: Int = 10): List<Playlist> = withContext(Dispatchers.IO) {
        val playlists = mutableListOf<Playlist>()
        try {
            val conn = openConnection("/v1/playlists/trending?app_name=$appName&limit=$limit")
            if (conn.responseCode == 200) {
                val json = JSONObject(conn.inputStream.bufferedReader().readText())
                val data = json.optJSONArray("data") ?: return@withContext emptyList()
                for (i in 0 until data.length()) {
                    val obj = data.optJSONObject(i) ?: continue
                    val id = obj.optString("id", "")
                    val name = obj.optString("playlist_name", "Featured Playlist")
                    val desc = obj.optString("description", "")
                    val user = obj.optJSONObject("user")
                    val owner = user?.optString("name") ?: "Curator"
                    val artwork = obj.optJSONObject("artwork")
                    val coverUrl = artwork?.optString("480x480")
                        ?.ifEmpty { artwork.optString("150x150") }
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
            e.printStackTrace()
        }
        playlists
    }

    /**
     * Fetches tracks for a specific playlist ID.
     */
    suspend fun getPlaylistTracks(playlistId: String): List<Track> = withContext(Dispatchers.IO) {
        val cleanId = playlistId.removePrefix("audius_pl_")
        val tracks = mutableListOf<Track>()
        try {
            val conn = openConnection("/v1/playlists/$cleanId/tracks?app_name=$appName")
            if (conn.responseCode == 200) {
                val json = JSONObject(conn.inputStream.bufferedReader().readText())
                val data = json.optJSONArray("data") ?: return@withContext emptyList()
                for (i in 0 until data.length()) {
                    val obj = data.optJSONObject(i) ?: continue
                    parseTrack(obj)?.let { tracks.add(it) }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        tracks
    }

    private fun parseTrack(obj: JSONObject): Track? {
        val id = obj.optString("id", "")
        if (id.isBlank()) return null

        val title = obj.optString("title", "Untitled")
        val user = obj.optJSONObject("user")
        val artist = user?.optString("name") ?: "Unknown Artist"
        val genre = obj.optString("genre", "Music")

        val artwork = obj.optJSONObject("artwork")
        val artUrl = artwork?.optString("480x480")
            ?.ifEmpty { artwork.optString("150x150") }
            ?: "https://c.saavncdn.com/041/Sita-Ramam-Telugu-Original-Motion-Picture-Soundtrack-Telugu-2022-20220802140738-500x500.jpg"

        val durationSec = obj.optLong("duration", 180)
        val streamUrl = "$baseUrl/v1/tracks/$id/stream?app_name=$appName"

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
}
