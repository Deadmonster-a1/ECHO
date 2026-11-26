package com.spotify.clone.core.data

import android.util.Base64
import android.util.Log
import com.spotify.clone.BuildConfig
import com.spotify.clone.core.model.Playlist
import com.spotify.clone.core.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class SpotifyApiService(
    private val clientId: String = BuildConfig.SPOTIFY_CLIENT_ID,
    private val clientSecret: String = BuildConfig.SPOTIFY_CLIENT_SECRET
) {
    private val tokenMutex = Mutex()
    @Volatile private var accessToken: String? = null
    @Volatile private var tokenExpiryTime: Long = 0L

    private fun JSONObject.safeOptString(key: String, fallback: String = ""): String {
        if (isNull(key)) return fallback
        val value = optString(key, fallback).trim()
        return if (value.isEmpty() || value.equals("null", ignoreCase = true)) fallback else value
    }

    /**
     * Authenticates with Spotify Accounts API to get an App Access Token.
     */
    private suspend fun getValidAccessToken(forceRefresh: Boolean = false): String? = withContext(Dispatchers.IO) {
        if (clientId.isBlank() || clientSecret.isBlank()) return@withContext null
        if (!forceRefresh) {
            val cached = accessToken
            if (cached != null && System.currentTimeMillis() < tokenExpiryTime) {
                return@withContext cached
            }
        }

        tokenMutex.withLock {
            if (!forceRefresh) {
                val cached = accessToken
                if (cached != null && System.currentTimeMillis() < tokenExpiryTime) {
                    return@withLock cached
                }
            }

            var connection: HttpURLConnection? = null
            try {
                val url = URL("https://accounts.spotify.com/api/token")
                connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8000
                    readTimeout = 8000
                    requestMethod = "POST"
                    doOutput = true
                }

                val basicAuth = Base64.encodeToString(
                    "$clientId:$clientSecret".toByteArray(),
                    Base64.NO_WRAP
                )
                connection.setRequestProperty("Authorization", "Basic $basicAuth")
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

                val body = "grant_type=client_credentials"
                OutputStreamWriter(connection.outputStream).use { it.write(body) }

                if (connection.responseCode == 200) {
                    val response = NetworkSecurity.readBoundedText(connection.inputStream)
                    val json = JSONObject(response)
                    val token = json.safeOptString("access_token")
                    val expiresIn = json.optLong("expires_in", 3600L)
                    if (token.isNotBlank()) {
                        accessToken = token
                        tokenExpiryTime = System.currentTimeMillis() + (expiresIn - 60).coerceAtLeast(30) * 1000L
                        return@withLock token
                    }
                }
            } catch (e: Exception) {
                Log.w("SpotifyApiService", "Token request failed: ${e.message}")
            } finally {
                connection?.disconnect()
            }
            null
        }
    }

    /**
     * Searches Spotify's real 100M+ track catalog.
     */
    suspend fun searchTracks(query: String): List<Track> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        var token = getValidAccessToken() ?: return@withContext emptyList()
        val tracks = mutableListOf<Track>()

        for (attempt in 0..1) {
            var connection: HttpURLConnection? = null
            try {
                val encodedQuery = URLEncoder.encode(query, "UTF-8")
                val url = URL("https://api.spotify.com/v1/search?q=$encodedQuery&type=track&limit=20")
                connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8000
                    readTimeout = 8000
                    requestMethod = "GET"
                    setRequestProperty("Authorization", "Bearer $token")
                }

                val code = connection.responseCode
                if (code == 401 && attempt == 0) {
                    accessToken = null
                    tokenExpiryTime = 0L
                    token = getValidAccessToken(forceRefresh = true) ?: break
                    continue
                }
                if (code == 200) {
                    val response = NetworkSecurity.readBoundedText(connection.inputStream)
                    val json = JSONObject(response)
                    val items = json.optJSONObject("tracks")?.optJSONArray("items") ?: break

                    for (i in 0 until items.length()) {
                        val item = items.optJSONObject(i) ?: continue
                        val id = item.safeOptString("id")
                        val name = item.safeOptString("name")
                        if (id.isBlank() || name.isBlank()) continue

                        val album = item.optJSONObject("album")
                        val images = album?.optJSONArray("images")
                        val coverUrl = if (images != null && images.length() > 0) {
                            images.optJSONObject(0)?.safeOptString("url").orEmpty()
                        } else ""
                        val artists = item.optJSONArray("artists")
                        val artistName = if (artists != null && artists.length() > 0) {
                            artists.optJSONObject(0)?.safeOptString("name", "Unknown") ?: "Unknown"
                        } else "Unknown"

                        val previewUrl = item.safeOptString("preview_url")
                        val spotifyUri = item.safeOptString("uri")

                        tracks.add(
                            Track(
                                id = id,
                                title = name,
                                artistName = artistName,
                                albumName = album?.safeOptString("name", "Single") ?: "Single",
                                coverArtUrl = coverUrl,
                                durationMs = item.optLong("duration_ms", 0L),
                                mediaUri = previewUrl.ifEmpty { spotifyUri },
                                isExplicit = item.optBoolean("explicit", false)
                            )
                        )
                    }
                    break
                } else {
                    Log.w("SpotifyApiService", "Spotify API returned HTTP $code")
                    break
                }
            } catch (e: Exception) {
                Log.w("SpotifyApiService", "Spotify search failed: ${e.message}")
                break
            } finally {
                connection?.disconnect()
            }
        }
        tracks
    }

    /**
     * Fetches an official Spotify playlist (e.g. Today's Top Hits: "37i9dQZF1DXcBWIGoYBM5M").
     */
    suspend fun getPlaylist(playlistId: String): Playlist? = withContext(Dispatchers.IO) {
        if (playlistId.isBlank()) return@withContext null
        var token = getValidAccessToken() ?: return@withContext null

        for (attempt in 0..1) {
            var connection: HttpURLConnection? = null
            try {
                val encodedId = URLEncoder.encode(playlistId, "UTF-8")
                val url = URL("https://api.spotify.com/v1/playlists/$encodedId")
                connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8000
                    readTimeout = 8000
                    requestMethod = "GET"
                    setRequestProperty("Authorization", "Bearer $token")
                }

                val code = connection.responseCode
                if (code == 401 && attempt == 0) {
                    accessToken = null
                    tokenExpiryTime = 0L
                    token = getValidAccessToken(forceRefresh = true) ?: return@withContext null
                    continue
                }
                if (code == 200) {
                    val response = NetworkSecurity.readBoundedText(connection.inputStream)
                    val json = JSONObject(response)
                    val images = json.optJSONArray("images")
                    val coverUrl = if (images != null && images.length() > 0) {
                        images.optJSONObject(0)?.safeOptString("url").orEmpty()
                    } else ""

                    val tracksArray = json.optJSONObject("tracks")?.optJSONArray("items")
                    val tracks = mutableListOf<Track>()

                    if (tracksArray != null) {
                        for (i in 0 until tracksArray.length()) {
                            val trackObj = tracksArray.optJSONObject(i)?.optJSONObject("track") ?: continue
                            val id = trackObj.safeOptString("id")
                            val name = trackObj.safeOptString("name")
                            if (id.isBlank() || name.isBlank()) continue

                            val album = trackObj.optJSONObject("album")
                            val albumImages = album?.optJSONArray("images")
                            val artUrl = if (albumImages != null && albumImages.length() > 0) {
                                albumImages.optJSONObject(0)?.safeOptString("url").orEmpty()
                            } else ""
                            val artists = trackObj.optJSONArray("artists")
                            val artistName = if (artists != null && artists.length() > 0) {
                                artists.optJSONObject(0)?.safeOptString("name", "Unknown") ?: "Unknown"
                            } else "Unknown"
                            val previewUrl = trackObj.safeOptString("preview_url")
                            val spotifyUri = trackObj.safeOptString("uri")

                            tracks.add(
                                Track(
                                    id = id,
                                    title = name,
                                    artistName = artistName,
                                    albumName = album?.safeOptString("name", "Single") ?: "Single",
                                    coverArtUrl = artUrl,
                                    durationMs = trackObj.optLong("duration_ms", 0L),
                                    mediaUri = previewUrl.ifEmpty { spotifyUri },
                                    isExplicit = trackObj.optBoolean("explicit", false)
                                )
                            )
                        }
                    }

                    return@withContext Playlist(
                        id = json.safeOptString("id", playlistId),
                        name = json.safeOptString("name", "Playlist"),
                        description = json.safeOptString("description"),
                        coverArtUrl = coverUrl,
                        ownerName = json.optJSONObject("owner")?.safeOptString("display_name", "Spotify") ?: "Spotify",
                        tracks = tracks
                    )
                }
                break
            } catch (e: Exception) {
                Log.w("SpotifyApiService", "Spotify playlist fetch failed: ${e.message}")
                break
            } finally {
                connection?.disconnect()
            }
        }
        null
    }
}
