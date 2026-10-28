package com.spotify.clone.core.data

import android.text.Html
import android.util.Log
import com.spotify.clone.core.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

data class StreamResolution(
    val streamUrl: String,
    val coverArtUrl: String? = null,
    val albumName: String? = null,
    val durationMs: Long? = null
)

/**
 * Resolves full-length studio audio streams (160kbps / 320kbps) for any song in the catalog,
 * including movie soundtracks (Baahubali, Devara, Pushpa, Kalki, etc.), regional melodies,
 * and private indie singles, completely eliminating 30-second promotional cutoffs and artwork mismatch.
 */
object FullTrackStreamResolver {

    private const val TAG = "StreamResolver"
    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
    private const val SAAVN_REFERER = "https://www.jiosaavn.com/"

    // In-memory cache mapping normalized "title_artist" -> full stream URL
    private val streamCache = ConcurrentHashMap<String, String>()

    fun needsResolution(track: Track): Boolean {
        val uri = track.mediaUri
        return uri.isBlank() ||
                uri.contains("preview") ||
                uri.contains("apple.com") ||
                uri.contains("mzstatic") ||
                uri.contains("codeskulptor") ||
                uri.contains("googleapis.com") ||
                uri.startsWith("saavn_enc:")
    }

    /**
     * Resolves a full-length un-truncated studio audio stream URL for a given track,
     * utilizing movie metadata, album context, and multi-candidate search.
     */
    suspend fun resolveStreamUrl(title: String, artistName: String, albumName: String = ""): String? = withContext(Dispatchers.IO) {
        val cacheKey = "${cleanTitle(title).lowercase()}_${cleanArtist(artistName).lowercase()}_${cleanAlbum(albumName).lowercase()}"
        streamCache[cacheKey]?.let { return@withContext it }

        val details = resolveTrackDetails(title, artistName, albumName)
        if (details != null && details.streamUrl.isNotBlank()) {
            streamCache[cacheKey] = details.streamUrl
            return@withContext details.streamUrl
        }

        Log.w(TAG, "Could not resolve external full stream for '$title', fallback will be used")
        null
    }

    /**
     * Resolves full studio stream URL along with authentic album art and title metadata.
     */
    suspend fun resolveTrackDetails(title: String, artistName: String, albumName: String = ""): StreamResolution? = withContext(Dispatchers.IO) {
        val candidates = generateCandidates(title, artistName, albumName)
        Log.d(TAG, "Resolving full track details for '$title' ($artistName) across ${candidates.size} candidates: $candidates")

        for (candidate in candidates) {
            val match = findEncryptedMediaMatch(candidate, expectedTitle = title)
            if (match != null) {
                val fullUrl = resolveAuthToken(match.streamUrl, bitrate = "320")
                    ?: resolveAuthToken(match.streamUrl, bitrate = "160")
                if (!fullUrl.isNullOrBlank()) {
                    Log.d(TAG, "Successfully resolved full studio stream for '$title' via candidate '$candidate': $fullUrl")
                    return@withContext match.copy(streamUrl = fullUrl)
                }
            }
        }

        // Secondary fallback: query Audius decentralized network for private/indie songs
        val audiusStream = resolveAudiusStream(title, artistName)
        if (!audiusStream.isNullOrBlank()) {
            return@withContext StreamResolution(streamUrl = audiusStream)
        }

        null
    }

    private fun findEncryptedMediaMatch(query: String, expectedTitle: String = ""): StreamResolution? {
        try {
            val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
            val searchUrl = "https://www.jiosaavn.com/api.php?__call=search.getResults&_format=json&_marker=0&api_version=4&ctx=web6dot0&n=10&p=1&q=$encodedQuery"
            val conn = openConnection(searchUrl)
            if (conn.responseCode == 200) {
                val respText = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(respText)
                val results = json.optJSONArray("results")
                if (results != null && results.length() > 0) {
                    val cleanExp = cleanTitle(expectedTitle).lowercase()

                    var bestItem: JSONObject? = null
                    var bestScore = -1

                    for (i in 0 until results.length()) {
                        val item = results.optJSONObject(i) ?: continue
                        val moreInfo = item.optJSONObject("more_info")
                        val enc = moreInfo?.optString("encrypted_media_url", "") ?: ""
                        if (enc.isBlank()) continue

                        val rawTitle = item.optString("title", "")
                        val itemTitle = Html.fromHtml(rawTitle, Html.FROM_HTML_MODE_LEGACY).toString()
                        val cleanItem = cleanTitle(itemTitle).lowercase()

                        val rawAlbum = moreInfo?.optString("album", "") ?: ""
                        val itemAlbum = Html.fromHtml(rawAlbum, Html.FROM_HTML_MODE_LEGACY).toString().lowercase()

                        val isTitleMatch = cleanExp.isBlank() ||
                                cleanItem == cleanExp ||
                                cleanItem.contains(cleanExp) ||
                                cleanExp.contains(cleanItem) ||
                                hasWordOverlap(cleanItem, cleanExp)

                        if (!isTitleMatch) continue

                        // Scoring: prefer original albums/singles over generic compilations
                        var score = 10
                        if (cleanItem == cleanExp) score += 20
                        if (itemAlbum.contains("superhits") ||
                            itemAlbum.contains("waves") ||
                            itemAlbum.contains("collection") ||
                            itemAlbum.contains("mashup") ||
                            itemAlbum.contains("top 10") ||
                            itemAlbum.contains("top 20") ||
                            itemAlbum.contains("romantic songs")) {
                            score -= 8
                        } else {
                            score += 10
                        }

                        if (score > bestScore) {
                            bestScore = score
                            bestItem = item
                        }
                    }

                    if (bestItem != null) {
                        val moreInfo = bestItem.optJSONObject("more_info")
                        val enc = moreInfo?.optString("encrypted_media_url", "") ?: ""
                        val rawCover = bestItem.optString("image", "").replace("150x150", "500x500")
                        val rawAlbum = moreInfo?.optString("album", "") ?: ""
                        val resolvedAlbum = Html.fromHtml(rawAlbum, Html.FROM_HTML_MODE_LEGACY).toString().trim()
                        val durationSec = moreInfo?.optLong("duration", 210L) ?: 210L

                        return StreamResolution(
                            streamUrl = enc,
                            coverArtUrl = rawCover.ifBlank { null },
                            albumName = resolvedAlbum.ifBlank { null },
                            durationMs = durationSec * 1000L
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "findEncryptedMediaMatch failed for query '$query'", e)
        }
        return null
    }

    private fun hasWordOverlap(a: String, b: String): Boolean {
        val wordsA = a.split(Regex("[^a-zA-Z0-9]+")).filter { it.length >= 3 }
        val wordsB = b.split(Regex("[^a-zA-Z0-9]+")).filter { it.length >= 3 }
        if (wordsA.isEmpty() || wordsB.isEmpty()) return false
        return wordsA.any { wa -> wordsB.any { wb -> wa == wb || wa.contains(wb) || wb.contains(wa) } }
    }

    private fun resolveAudiusStream(title: String, artistName: String): String? {
        try {
            val q = "${cleanTitle(title)} ${cleanArtist(artistName)}".trim()
            val encoded = URLEncoder.encode(q, "UTF-8")
            val urlStr = "https://api.audius.co/v1/tracks/search?query=$encoded&app_name=Echo&limit=3"
            val conn = URL(urlStr).openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 7000
            if (conn.responseCode == 200) {
                val json = JSONObject(conn.inputStream.bufferedReader().readText())
                val data = json.optJSONArray("data")
                if (data != null && data.length() > 0) {
                    val first = data.getJSONObject(0)
                    val trackId = first.optString("id", "")
                    if (trackId.isNotBlank()) {
                        return "https://api.audius.co/v1/tracks/$trackId/stream?app_name=Echo"
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore Audius fallback error
        }
        return null
    }

    /**
     * Resolves an auth_url directly from an encrypted_media_url string.
     */
    suspend fun resolveAuthToken(encryptedMediaUrl: String, bitrate: String = "160"): String? = withContext(Dispatchers.IO) {
        try {
            val encEncoded = URLEncoder.encode(encryptedMediaUrl, "UTF-8")
            val tokenUrl = "https://www.jiosaavn.com/api.php?__call=song.generateAuthToken&url=$encEncoded&bitrate=$bitrate&api_version=4&_format=json&ctx=web6dot0&_marker=0"
            val conn = openConnection(tokenUrl)
            if (conn.responseCode == 200) {
                val json = JSONObject(conn.inputStream.bufferedReader().readText())
                val authUrl = json.optString("auth_url", "")
                if (authUrl.isNotBlank() && authUrl != "false") {
                    return@withContext authUrl
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate auth token", e)
        }
        null
    }

    private fun openConnection(urlStr: String): HttpURLConnection {
        val url = URL(urlStr)
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 6000
        conn.readTimeout = 8000
        conn.setRequestProperty("User-Agent", USER_AGENT)
        conn.setRequestProperty("Referer", SAAVN_REFERER)
        conn.setRequestProperty("Accept", "application/json")
        return conn
    }

    private fun generateCandidates(title: String, artistName: String, albumName: String): List<String> {
        val candidates = mutableListOf<String>()
        val cTitle = cleanTitle(title)
        val cArtist = cleanArtist(artistName)
        val cAlbum = cleanAlbum(albumName)
        val movie = extractMovie(title)

        if (movie.isNotBlank()) {
            candidates.add("$cTitle $movie".trim())
        }
        if (cAlbum.isNotBlank() && !cAlbum.equals(cTitle, ignoreCase = true) && !cAlbum.lowercase().contains("single") && !cAlbum.lowercase().contains("unknown")) {
            candidates.add("$cTitle $cAlbum".trim())
        }
        if (cArtist.isNotBlank() && !cArtist.lowercase().contains("various") && !cArtist.lowercase().contains("unknown")) {
            candidates.add("$cTitle $cArtist".trim())
        }
        if (cTitle.isNotBlank()) {
            candidates.add(cTitle)
        }
        candidates.add("$title $artistName".trim())

        return candidates.distinctBy { it.lowercase() }.filter { it.length > 1 }
    }

    private fun extractMovie(title: String): String {
        val pattern = Pattern.compile("[\\[(]From [\"']?([^\"'()\\]]+)", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(title)
        if (matcher.find()) {
            val group = matcher.group(1) ?: return ""
            return group.split("-")[0].split(":")[0].trim()
        }
        return ""
    }

    private fun cleanTitle(title: String): String {
        var clean = Html.fromHtml(title, Html.FROM_HTML_MODE_LEGACY).toString()
        clean = clean.replace(Regex("[\\[(].*?[\\])]"), "")
        clean = clean.split("-")[0].trim()
        return clean.ifBlank { title.trim() }
    }

    private fun cleanArtist(artist: String): String {
        var clean = Html.fromHtml(artist, Html.FROM_HTML_MODE_LEGACY).toString()
        clean = clean.split(",")[0].split("&")[0].split("ft.")[0].split("feat.")[0].trim()
        return clean
    }

    private fun cleanAlbum(album: String): String {
        var clean = Html.fromHtml(album, Html.FROM_HTML_MODE_LEGACY).toString()
        clean = clean.replace(Regex("[\\[(].*?[\\])]"), "")
        clean = clean.split("-")[0].split(":")[0].trim()
        return clean
    }
}
