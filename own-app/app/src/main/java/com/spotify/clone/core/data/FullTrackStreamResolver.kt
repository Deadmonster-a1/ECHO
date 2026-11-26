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

    private const val STREAM_CACHE_TTL_MS = 20L * 60L * 1000L // 20 minutes
    private const val MAX_STREAM_CACHE_ENTRIES = 200
    private const val MAX_DETAILS_CACHE_ENTRIES = 200

    private val WORDS_SPLIT_REGEX = Regex("[^a-zA-Z0-9]+")
    private val BRACKET_STRIP_REGEX = Regex("[\\[(].*?[\\])]")
    private val MOVIE_FROM_PATTERN = Pattern.compile("[\\[(]From [\"']?([^\"'()\\]]+)", Pattern.CASE_INSENSITIVE)

    private data class CachedStreamEntry(
        val url: String,
        val cachedAtMs: Long
    )

    // Bounded in-memory LRU+TTL caches mapping normalized "title_artist_album" -> full stream URL / details
    private val streamCache: MutableMap<String, CachedStreamEntry> = java.util.Collections.synchronizedMap(
        object : LinkedHashMap<String, CachedStreamEntry>(64, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CachedStreamEntry>?): Boolean =
                size > MAX_STREAM_CACHE_ENTRIES
        }
    )
    private val detailsCache: MutableMap<String, Pair<StreamResolution, Long>> = java.util.Collections.synchronizedMap(
        object : LinkedHashMap<String, Pair<StreamResolution, Long>>(64, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Pair<StreamResolution, Long>>?): Boolean =
                size > MAX_DETAILS_CACHE_ENTRIES
        }
    )

    fun needsResolution(track: Track): Boolean {
        val uri = track.mediaUri.trim()
        return uri.isBlank() ||
                uri.equals("null", ignoreCase = true) ||
                uri.contains("preview") ||
                uri.contains("apple.com") ||
                uri.contains("mzstatic") ||
                uri.contains("codeskulptor") ||
                uri.contains("googleapis.com") ||
                uri.startsWith("saavn_enc:") ||
                uri.startsWith("spotify:")
    }

    internal fun getCachedStreamIfFresh(key: String, nowMs: Long = System.currentTimeMillis()): String? {
        val entry = streamCache[key] ?: return null
        if (nowMs - entry.cachedAtMs > STREAM_CACHE_TTL_MS) {
            synchronized(streamCache) {
                if (streamCache[key] == entry) streamCache.remove(key)
            }
            return null
        }
        return entry.url
    }

    internal fun putCachedStream(key: String, url: String, nowMs: Long = System.currentTimeMillis()) {
        if (url.isNotBlank() && !url.equals("null", ignoreCase = true)) {
            streamCache[key] = CachedStreamEntry(url = url, cachedAtMs = nowMs)
        }
    }

    internal fun clearStreamCache() {
        streamCache.clear()
        detailsCache.clear()
    }

    private fun JSONObject.safeOptString(key: String, fallback: String = ""): String {
        if (isNull(key)) return fallback
        val value = optString(key, fallback).trim()
        return if (value.isEmpty() || value.equals("null", ignoreCase = true)) fallback else value
    }

    /**
     * Resolves a full-length un-truncated studio audio stream URL for a given track,
     * utilizing movie metadata, album context, and multi-candidate search.
     */
    suspend fun resolveStreamUrl(title: String, artistName: String, albumName: String = ""): String? = withContext(Dispatchers.IO) {
        val cacheKey = "${cleanTitle(title).lowercase()}_${cleanArtist(artistName).lowercase()}_${cleanAlbum(albumName).lowercase()}"
        getCachedStreamIfFresh(cacheKey)?.let { return@withContext it }

        val details = resolveTrackDetails(title, artistName, albumName)
        if (details != null && details.streamUrl.isNotBlank()) {
            putCachedStream(cacheKey, details.streamUrl)
            return@withContext details.streamUrl
        }

        Log.w(TAG, "Could not resolve external full stream for '$title', fallback will be used")
        null
    }

    /**
     * Resolves full studio stream URL along with authentic album art and title metadata.
     */
    suspend fun resolveTrackDetails(title: String, artistName: String, albumName: String = ""): StreamResolution? = withContext(Dispatchers.IO) {
        val cacheKey = "${cleanTitle(title).lowercase()}_${cleanArtist(artistName).lowercase()}_${cleanAlbum(albumName).lowercase()}"
        val now = System.currentTimeMillis()
        detailsCache[cacheKey]?.let { (cachedDetails, cachedAt) ->
            if (now - cachedAt <= STREAM_CACHE_TTL_MS) {
                return@withContext cachedDetails
            } else {
                detailsCache.remove(cacheKey)
            }
        }

        val candidates = generateCandidates(title, artistName, albumName)
        Log.d(TAG, "Resolving full track details for '$title' ($artistName) across ${candidates.size} candidates: $candidates")

        for (candidate in candidates) {
            val match = findEncryptedMediaMatch(candidate, expectedTitle = title)
            if (match != null) {
                val fullUrl = resolveAuthToken(match.streamUrl, bitrate = "320")
                    ?: resolveAuthToken(match.streamUrl, bitrate = "160")
                if (!fullUrl.isNullOrBlank()) {
                    Log.d(TAG, "Successfully resolved full studio stream for '$title' via candidate '$candidate': $fullUrl")
                    val resolved = match.copy(streamUrl = fullUrl)
                    detailsCache[cacheKey] = resolved to System.currentTimeMillis()
                    putCachedStream(cacheKey, fullUrl)
                    return@withContext resolved
                }
            }
        }

        // Secondary fallback: query Audius decentralized network for private/indie songs
        val audiusStream = resolveAudiusStream(title, artistName)
        if (!audiusStream.isNullOrBlank()) {
            val resolved = StreamResolution(streamUrl = audiusStream)
            detailsCache[cacheKey] = resolved to System.currentTimeMillis()
            putCachedStream(cacheKey, audiusStream)
            return@withContext resolved
        }

        null
    }

    private fun findEncryptedMediaMatch(query: String, expectedTitle: String = ""): StreamResolution? {
        var conn: HttpURLConnection? = null
        try {
            val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
            val searchUrl = "https://www.jiosaavn.com/api.php?__call=search.getResults&_format=json&_marker=0&api_version=4&ctx=web6dot0&n=10&p=1&q=$encodedQuery"
            conn = openConnection(searchUrl)
            if (conn.responseCode == 200) {
                val respText = NetworkSecurity.readBoundedText(conn.inputStream)
                val json = JSONObject(respText)
                val results = json.optJSONArray("results")
                if (results != null && results.length() > 0) {
                    val cleanExp = cleanTitle(expectedTitle).lowercase()

                    var bestItem: JSONObject? = null
                    var bestScore = -1

                    for (i in 0 until results.length()) {
                        val item = results.optJSONObject(i) ?: continue
                        val moreInfo = item.optJSONObject("more_info")
                        val enc = moreInfo?.safeOptString("encrypted_media_url") ?: ""
                        if (enc.isBlank()) continue

                        val rawTitle = item.safeOptString("title")
                        val itemTitle = Html.fromHtml(rawTitle, Html.FROM_HTML_MODE_LEGACY).toString()
                        val cleanItem = cleanTitle(itemTitle).lowercase()

                        val rawAlbum = moreInfo?.safeOptString("album") ?: ""
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
                        val enc = moreInfo?.safeOptString("encrypted_media_url") ?: ""
                        val rawCover = bestItem.safeOptString("image").replace("150x150", "500x500")
                        val safeCover = NetworkSecurity.enforceHttpsOrUpgradeKnownCdn(rawCover)
                        val rawAlbum = moreInfo?.safeOptString("album") ?: ""
                        val resolvedAlbum = Html.fromHtml(rawAlbum, Html.FROM_HTML_MODE_LEGACY).toString().trim()
                        val durationSec = moreInfo?.optLong("duration", 210L) ?: 210L

                        return StreamResolution(
                            streamUrl = enc,
                            coverArtUrl = safeCover,
                            albumName = resolvedAlbum.ifBlank { null },
                            durationMs = durationSec * 1000L
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "findEncryptedMediaMatch failed for query '$query'", e)
        } finally {
            conn?.disconnect()
        }
        return null
    }

    private fun hasWordOverlap(a: String, b: String): Boolean {
        val wordsA = a.split(WORDS_SPLIT_REGEX).filter { it.length >= 3 }
        val wordsB = b.split(WORDS_SPLIT_REGEX).filter { it.length >= 3 }
        if (wordsA.isEmpty() || wordsB.isEmpty()) return false
        return wordsA.any { wa -> wordsB.any { wb -> wa == wb || wa.contains(wb) || wb.contains(wa) } }
    }

    private fun resolveAudiusStream(title: String, artistName: String): String? {
        var conn: HttpURLConnection? = null
        try {
            val q = "${cleanTitle(title)} ${cleanArtist(artistName)}".trim()
            val encoded = URLEncoder.encode(q, "UTF-8")
            val urlStr = "https://api.audius.co/v1/tracks/search?query=$encoded&app_name=Echo&limit=3"
            conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 7000
            }
            if (conn.responseCode == 200) {
                val json = JSONObject(NetworkSecurity.readBoundedText(conn.inputStream))
                val data = json.optJSONArray("data")
                if (data != null && data.length() > 0) {
                    val first = data.optJSONObject(0)
                    val trackId = first?.safeOptString("id").orEmpty()
                    if (trackId.isNotBlank()) {
                        return "https://api.audius.co/v1/tracks/$trackId/stream?app_name=Echo"
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore Audius fallback error
        } finally {
            conn?.disconnect()
        }
        return null
    }

    /**
     * Resolves an auth_url directly from an encrypted_media_url string.
     */
    suspend fun resolveAuthToken(encryptedMediaUrl: String, bitrate: String = "160"): String? = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            val encEncoded = URLEncoder.encode(encryptedMediaUrl, "UTF-8")
            val tokenUrl = "https://www.jiosaavn.com/api.php?__call=song.generateAuthToken&url=$encEncoded&bitrate=$bitrate&api_version=4&_format=json&ctx=web6dot0&_marker=0"
            conn = openConnection(tokenUrl)
            if (conn.responseCode == 200) {
                val json = JSONObject(NetworkSecurity.readBoundedText(conn.inputStream))
                val authUrl = json.safeOptString("auth_url")
                if (authUrl.isNotBlank() && authUrl != "false") {
                    val safeAuthUrl = NetworkSecurity.enforceHttpsOrUpgradeKnownCdn(authUrl)
                    if (!safeAuthUrl.isNullOrBlank()) {
                        return@withContext safeAuthUrl
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate auth token", e)
        } finally {
            conn?.disconnect()
        }
        null
    }

    private fun openConnection(urlStr: String): HttpURLConnection {
        val safeUrl = NetworkSecurity.enforceHttpsOrUpgradeKnownCdn(urlStr)
            ?: throw SecurityException("Blocked non-HTTPS request")
        val url = URL(safeUrl)
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
        val matcher = MOVIE_FROM_PATTERN.matcher(title)
        if (matcher.find()) {
            val group = matcher.group(1) ?: return ""
            return group.split("-")[0].split(":")[0].trim()
        }
        return ""
    }

    private fun cleanTitle(title: String): String {
        var clean = Html.fromHtml(title, Html.FROM_HTML_MODE_LEGACY).toString()
        clean = clean.replace(BRACKET_STRIP_REGEX, "")
        clean = clean.split(" - ")[0].trim()
        return clean.ifBlank { title.trim() }
    }

    private fun cleanArtist(artist: String): String {
        var clean = Html.fromHtml(artist, Html.FROM_HTML_MODE_LEGACY).toString()
        clean = clean.split(",")[0].split("&")[0].split("ft.")[0].split("feat.")[0].trim()
        return clean
    }

    private fun cleanAlbum(album: String): String {
        var clean = Html.fromHtml(album, Html.FROM_HTML_MODE_LEGACY).toString()
        clean = clean.replace(BRACKET_STRIP_REGEX, "")
        clean = clean.split(" - ")[0].split(":")[0].trim()
        return clean
    }
}
