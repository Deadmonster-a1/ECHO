package com.spotify.clone.core.data

import android.util.Base64
import com.spotify.clone.core.model.Playlist
import com.spotify.clone.core.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class SpotifyApiService(
    private val clientId: String = "8d9610bceb3a47fb9dd9bfdd5a112f1c",
    private val clientSecret: String = "aea93a8284dc4534bff9ac37c51b152f"
) {
    private var accessToken: String? = null
    private var tokenExpiryTime: Long = 0L

    /**
     * Authenticates with Spotify Accounts API to get an App Access Token.
     */
    private suspend fun getValidAccessToken(): String? = withContext(Dispatchers.IO) {
        if (clientId.isBlank() || clientSecret.isBlank()) return@withContext null
        if (accessToken != null && System.currentTimeMillis() < tokenExpiryTime) {
            return@withContext accessToken
        }

        try {
            val url = URL("https://accounts.spotify.com/api/token")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true

            val basicAuth = Base64.encodeToString(
                "$clientId:$clientSecret".toByteArray(),
                Base64.NO_WRAP
            )
            connection.setRequestProperty("Authorization", "Basic $basicAuth")
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

            val body = "grant_type=client_credentials"
            OutputStreamWriter(connection.outputStream).use { it.write(body) }

            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().readText()
                val json = JSONObject(response)
                val token = json.getString("access_token")
                val expiresIn = json.getLong("expires_in")
                accessToken = token
                tokenExpiryTime = System.currentTimeMillis() + (expiresIn - 60) * 1000L
                return@withContext token
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }

    /**
     * Searches Spotify's real 100M+ track catalog.
     */
    suspend fun searchTracks(query: String): List<Track> = withContext(Dispatchers.IO) {
        val token = getValidAccessToken() ?: return@withContext emptyList()
        val tracks = mutableListOf<Track>()

        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = URL("https://api.spotify.com/v1/search?q=$encodedQuery&type=track&limit=20")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Authorization", "Bearer $token")

            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().readText()
                val json = JSONObject(response)
                val items = json.getJSONObject("tracks").getJSONArray("items")

                for (i in 0 until items.length()) {
                    val item = items.getJSONObject(i)
                    val album = item.getJSONObject("album")
                    val images = album.getJSONArray("images")
                    val coverUrl = if (images.length() > 0) images.getJSONObject(0).getString("url") else ""
                    val artists = item.getJSONArray("artists")
                    val artistName = if (artists.length() > 0) artists.getJSONObject(0).getString("name") else "Unknown"

                    // Spotify provides an official preview stream URL for catalog items
                    val previewUrl = item.optString("preview_url", "")
                    val spotifyUri = item.getString("uri") // e.g. "spotify:track:..."

                    tracks.add(
                        Track(
                            id = item.getString("id"),
                            title = item.getString("name"),
                            artistName = artistName,
                            albumName = album.getString("name"),
                            coverArtUrl = coverUrl,
                            durationMs = item.getLong("duration_ms"),
                            mediaUri = previewUrl.ifEmpty { spotifyUri },
                            isExplicit = item.optBoolean("explicit", false)
                        )
                    )
                }
            } else {
                val err = connection.errorStream?.bufferedReader()?.readText()
                android.util.Log.w("SpotifyApiService", "Spotify API returned ${connection.responseCode}: $err")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        tracks
    }

    /**
     * Fetches an official Spotify playlist (e.g. Today's Top Hits: "37i9dQZF1DXcBWIGoYBM5M").
     */
    suspend fun getPlaylist(playlistId: String): Playlist? = withContext(Dispatchers.IO) {
        val token = getValidAccessToken() ?: return@withContext null

        try {
            val url = URL("https://api.spotify.com/v1/playlists/$playlistId")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Authorization", "Bearer $token")

            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().readText()
                val json = JSONObject(response)
                val images = json.getJSONArray("images")
                val coverUrl = if (images.length() > 0) images.getJSONObject(0).getString("url") else ""

                val tracksArray = json.getJSONObject("tracks").getJSONArray("items")
                val tracks = mutableListOf<Track>()

                for (i in 0 until tracksArray.length()) {
                    val trackObj = tracksArray.getJSONObject(i).optJSONObject("track") ?: continue
                    val album = trackObj.getJSONObject("album")
                    val albumImages = album.getJSONArray("images")
                    val artUrl = if (albumImages.length() > 0) albumImages.getJSONObject(0).getString("url") else ""
                    val artists = trackObj.getJSONArray("artists")
                    val artistName = if (artists.length() > 0) artists.getJSONObject(0).getString("name") else "Unknown"
                    val previewUrl = trackObj.optString("preview_url", "")
                    val spotifyUri = trackObj.getString("uri")

                    tracks.add(
                        Track(
                            id = trackObj.getString("id"),
                            title = trackObj.getString("name"),
                            artistName = artistName,
                            albumName = album.getString("name"),
                            coverArtUrl = artUrl,
                            durationMs = trackObj.getLong("duration_ms"),
                            mediaUri = previewUrl.ifEmpty { spotifyUri },
                            isExplicit = trackObj.optBoolean("explicit", false)
                        )
                    )
                }

                return@withContext Playlist(
                    id = json.getString("id"),
                    name = json.getString("name"),
                    description = json.optString("description", ""),
                    coverArtUrl = coverUrl,
                    ownerName = json.getJSONObject("owner").getString("display_name"),
                    tracks = tracks
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }
}
