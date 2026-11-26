package com.spotify.clone.core.lyrics

import android.text.Html
import android.util.Log
import com.spotify.clone.core.model.LyricLine
import com.spotify.clone.core.model.LyricsResult
import com.spotify.clone.core.model.LyricsSyncType
import com.spotify.clone.core.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

/**
 * Spotify-Grade Synchronized Lyrics Engine for ECHO.
 *
 * Implements real-time synchronized line tracking based on Spotify's reverse-engineered
 * ColorLyrics architecture (ColorLyricsResponse / LyricsResponse):
 * 1. LINE_SYNCED: True millisecond-level vocal timestamps matching the audio recording.
 * 2. UNSYNCED: Clean static typography for plain lyrics without fabricated timings.
 * 3. NONE: Honest, respectful "Lyrics Unavailable" state without synthetic commentary.
 */
object LyricsRepository {

    private const val TAG = "EchoLyricsRepository"
    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    private val memoryCache = ConcurrentHashMap<String, LyricsResult>()
    private val lrcTagPattern = Pattern.compile("\\[(\\d{1,3}):(\\d{2}(?:\\.\\d{1,3})?)]")

    /**
     * Instantly retrieves cached or curated English-normalized synced lyrics if available (synchronous).
     */
    fun getCachedLyrics(track: Track, durationHintMs: Long = track.durationMs): LyricsResult {
        val effectiveDuration = maxOf(track.durationMs, durationHintMs)
        val cacheKey = getCacheKey(track)
        memoryCache[cacheKey]?.let { cached ->
            return if (effectiveDuration > 15_000L) {
                LyricsSyncEngine.prepareEnglishSyncedLyrics(cached, effectiveDuration)
            } else {
                cached
            }
        }

        // Check curated catalog
        LyricsCatalog.findCuratedLyrics(track.id, track.title)?.let { lines ->
            val rawResult = LyricsResult(LyricsSyncType.LINE_SYNCED, enrichDurations(lines), "Echo Verified Studio Sync")
            val result = LyricsSyncEngine.prepareEnglishSyncedLyrics(rawResult, effectiveDuration)
            memoryCache[cacheKey] = result
            return result
        }

        // Check if track object already has rich lyrics
        if (track.lyrics.isNotEmpty()) {
            val enriched = if (track.lyricsSyncType == LyricsSyncType.LINE_SYNCED) enrichDurations(track.lyrics) else track.lyrics
            val rawResult = LyricsResult(track.lyricsSyncType, enriched, "Embedded Metadata")
            val result = LyricsSyncEngine.prepareEnglishSyncedLyrics(rawResult, effectiveDuration)
            memoryCache[cacheKey] = result
            return result
        }

        return LyricsResult(LyricsSyncType.NONE, emptyList(), "")
    }

    /**
     * Resolves full lyrics asynchronously across authoritative tiers, normalizes every line
     * to English, and applies algorithmic vocal synchronization when timestamps are missing or drifted.
     */
    suspend fun getOrFetchLyrics(track: Track, durationHintMs: Long = track.durationMs): LyricsResult = withContext(Dispatchers.IO) {
        val effectiveDuration = maxOf(track.durationMs, durationHintMs)
        val cacheKey = getCacheKey(track)
        memoryCache[cacheKey]?.let { cached ->
            val updated = if (effectiveDuration > 15_000L) {
                LyricsSyncEngine.prepareEnglishSyncedLyrics(cached, effectiveDuration)
            } else {
                cached
            }
            memoryCache[cacheKey] = updated
            return@withContext updated
        }

        // Tier 1: Curated Studio Catalog
        LyricsCatalog.findCuratedLyrics(track.id, track.title)?.let { lines ->
            val rawResult = LyricsResult(LyricsSyncType.LINE_SYNCED, enrichDurations(lines), "Echo Verified Studio Sync")
            val result = LyricsSyncEngine.prepareEnglishSyncedLyrics(rawResult, effectiveDuration)
            memoryCache[cacheKey] = result
            return@withContext result
        }

        // Tier 2: LRCLIB API (Global Synced Lyrics Engine)
        try {
            val lrclibResult = fetchFromLrclib(track)
            if (lrclibResult != null && lrclibResult.lines.isNotEmpty()) {
                val syncedEnglish = LyricsSyncEngine.prepareEnglishSyncedLyrics(lrclibResult, effectiveDuration)
                Log.d(TAG, "Loaded ${syncedEnglish.lines.size} lines (${syncedEnglish.syncType}) from LRCLIB for '${track.title}'")
                memoryCache[cacheKey] = syncedEnglish
                return@withContext syncedEnglish
            }
        } catch (e: Exception) {
            Log.d(TAG, "LRCLIB lookup failed for '${track.title}': ${e.message}")
        }

        // Tier 3: JioSaavn Lyrics API + Algorithmic Sync & Romanization
        if (track.id.startsWith("saavn_") || track.mediaUri.contains("saavn")) {
            try {
                val saavnLines = fetchFromSaavn(track)
                if (saavnLines.isNotEmpty()) {
                    Log.d(TAG, "Loaded ${saavnLines.size} lines from Saavn for '${track.title}'")
                    val rawResult = LyricsResult(LyricsSyncType.UNSYNCED, saavnLines, "JioSaavn")
                    val result = LyricsSyncEngine.prepareEnglishSyncedLyrics(rawResult, effectiveDuration)
                    memoryCache[cacheKey] = result
                    return@withContext result
                }
            } catch (e: Exception) {
                Log.d(TAG, "Saavn lyrics lookup failed for '${track.title}': ${e.message}")
            }
        }

        // Tier 4: Existing track embedded lyrics if present
        if (track.lyrics.isNotEmpty()) {
            val rawResult = LyricsResult(track.lyricsSyncType, track.lyrics, "Embedded Metadata")
            val result = LyricsSyncEngine.prepareEnglishSyncedLyrics(rawResult, effectiveDuration)
            memoryCache[cacheKey] = result
            return@withContext result
        }

        // Tier 5: Honest Unavailable State (do not cache transient misses in memoryCache)
        return@withContext LyricsResult(LyricsSyncType.NONE, emptyList(), "")
    }

    private fun getCacheKey(track: Track): String {
        return "${cleanQuery(track.title).lowercase()}_${cleanQuery(track.artistName).lowercase()}"
    }

    private fun cleanQuery(input: String): String {
        return input
            .replace(Regex("(?i)\\(.*?\\)|\\[.*?\\]"), "")
            .replace(Regex("(?i)\\s*-\\s*from\\s+\"?.*$"), "")
            .replace(Regex("(?i)\\b(official\\s*video|official\\s*audio|lyric\\s*video|full\\s*video|lyrical|video|audio|lyrics|4k|hd|remastered|remaster)\\b"), "")
            .trim()
    }

    private fun fetchFromLrclib(track: Track): LyricsResult? {
        val cleanTitle = cleanQuery(track.title)
        val primaryArtist = track.artistName.split(",", "&", "feat", "Feat").firstOrNull()?.trim() ?: track.artistName

        // Strategy A: Exact metadata match via /api/get
        var getConn: HttpURLConnection? = null
        try {
            val getUrl = buildString {
                append("https://lrclib.net/api/get?")
                append("track_name=").append(URLEncoder.encode(cleanTitle, "UTF-8"))
                append("&artist_name=").append(URLEncoder.encode(primaryArtist, "UTF-8"))
                if (track.durationMs > 10000L) {
                    append("&duration=").append(track.durationMs / 1000L)
                }
            }

            getConn = (URL(getUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 6000
                requestMethod = "GET"
                setRequestProperty("User-Agent", USER_AGENT)
            }

            if (getConn.responseCode == 200) {
                val respText = getConn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(respText)
                val synced = json.optString("syncedLyrics", "")
                if (synced.isNotBlank()) {
                    val parsed = parseLrc(synced)
                    if (parsed.isNotEmpty()) {
                        return LyricsResult(LyricsSyncType.LINE_SYNCED, parsed, "LRCLIB Synced")
                    }
                }
                val plain = json.optString("plainLyrics", "")
                if (plain.isNotBlank()) {
                    val plainLines = parsePlainLyrics(plain)
                    if (plainLines.isNotEmpty()) {
                        return LyricsResult(LyricsSyncType.UNSYNCED, plainLines, "LRCLIB Plain")
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "LRCLIB /api/get error: ${e.message}")
        } finally {
            getConn?.disconnect()
        }

        // Strategy B & C: Full text query via /api/search (title + artist, then title-only fallback)
        val queries = listOf("$cleanTitle $primaryArtist".trim(), cleanTitle).distinct().filter { it.isNotBlank() }
        for (query in queries) {
            var searchConn: HttpURLConnection? = null
            try {
                val searchUrl = "https://lrclib.net/api/search?q=" + URLEncoder.encode(query, "UTF-8")
                searchConn = (URL(searchUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 6000
                    readTimeout = 7000
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", USER_AGENT)
                }

                if (searchConn.responseCode == 200) {
                    val respText = searchConn.inputStream.bufferedReader().use { it.readText() }
                    val array = JSONArray(respText)
                    var bestResult: LyricsResult? = null
                    var bestScore = Double.NEGATIVE_INFINITY

                    for (i in 0 until array.length()) {
                        val item = array.getJSONObject(i)
                        val synced = item.optString("syncedLyrics", "")
                        val plain = item.optString("plainLyrics", "")
                        if (synced.isBlank() && plain.isBlank()) continue

                        val score = LyricsSyncEngine.scoreCandidateMatch(
                            queryTitle = cleanTitle,
                            queryArtist = primaryArtist,
                            targetDurationMs = track.durationMs,
                            candTitle = item.optString("trackName", ""),
                            candArtist = item.optString("artistName", ""),
                            candDurationSec = item.optDouble("duration", 0.0),
                            hasSynced = synced.isNotBlank()
                        )
                        if (score > bestScore) {
                            val candidate = if (synced.isNotBlank()) {
                                val parsed = parseLrc(synced)
                                if (parsed.isNotEmpty()) LyricsResult(LyricsSyncType.LINE_SYNCED, parsed, "LRCLIB Synced") else null
                            } else {
                                val plainLines = parsePlainLyrics(plain)
                                if (plainLines.isNotEmpty()) LyricsResult(LyricsSyncType.UNSYNCED, plainLines, "LRCLIB") else null
                            }
                            if (candidate != null) {
                                bestScore = score
                                bestResult = candidate
                            }
                        }
                    }
                    if (bestResult != null && bestScore >= 12.0) {
                        return bestResult
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "LRCLIB /api/search error: ${e.message}")
            } finally {
                searchConn?.disconnect()
            }
        }

        return null
    }

    private fun fetchFromSaavn(track: Track): List<LyricLine> {
        val saavnSongId = track.id.removePrefix("saavn_")
        val urlStr = "https://www.jiosaavn.com/api.php?__call=lyrics.getLyrics&ctx=web6dot0&api_version=4&_format=json&_marker=0&lyrics_id=$saavnSongId"

        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 6000
                requestMethod = "GET"
                setRequestProperty("Referer", "https://www.jiosaavn.com/")
                setRequestProperty("User-Agent", USER_AGENT)
            }

            if (conn.responseCode != 200) return emptyList()

            val response = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)
            val rawLyrics = json.optString("lyrics", "")
            if (rawLyrics.isBlank()) return emptyList()

            val cleanLyrics = Html.fromHtml(rawLyrics, Html.FROM_HTML_MODE_LEGACY).toString()
            parsePlainLyrics(cleanLyrics)
        } finally {
            conn?.disconnect()
        }
    }

    /**
     * Parses standard LRC format strings ([mm:ss.xx] words) into timestamped LyricLine objects.
     * Supports multiple timestamp tags per line, millisecond precision, and ignores metadata headers.
     */
    fun parseLrc(lrcContent: String): List<LyricLine> {
        val lines = mutableListOf<LyricLine>()
        val rawLines = lrcContent.split("\n", "\r\n")

        for (line in rawLines) {
            val trimmed = line.trim()
            if (trimmed.isBlank()) continue
            // Skip ID3 tags like [ar:...], [ti:...], [al:...]
            if (trimmed.matches(Regex("^\\[[a-zA-Z]+:.*?\\]$"))) continue

            val timestamps = mutableListOf<Long>()
            val matcher = lrcTagPattern.matcher(trimmed)
            while (matcher.find()) {
                val minStr = matcher.group(1) ?: continue
                val secStr = matcher.group(2) ?: continue
                val minutes = minStr.toLongOrNull() ?: 0L
                val seconds = secStr.toDoubleOrNull() ?: 0.0
                timestamps.add((minutes * 60L * 1000L) + kotlin.math.round(seconds * 1000.0).toLong())
            }

            if (timestamps.isNotEmpty()) {
                val words = lrcTagPattern.matcher(trimmed).replaceAll("").trim()
                if (words.isNotBlank()) {
                    for (ts in timestamps) {
                        lines.add(LyricLine(ts, words))
                    }
                }
            }
        }

        return enrichDurations(lines.sortedBy { it.timestampMs })
    }

    /**
     * Calculates authentic line duration in milliseconds from consecutive line timestamps.
     * Essential for high-fidelity progressive karaoke sweep rendering.
     */
    fun enrichDurations(lines: List<LyricLine>): List<LyricLine> {
        if (lines.isEmpty()) return lines
        return lines.mapIndexed { index, line ->
            if (line.durationMs > 0L) {
                line
            } else {
                val nextTimestamp = if (index < lines.size - 1) lines[index + 1].timestampMs else (line.timestampMs + 4000L)
                val duration = (nextTimestamp - line.timestampMs).coerceIn(1200L, 9500L)
                line.copy(durationMs = duration)
            }
        }
    }

    /**
     * Parses plain un-synchronized lyrics into LyricLine items with -1 timestamp.
     */
    fun parsePlainLyrics(plainLyrics: String): List<LyricLine> {
        return plainLyrics.split("\n", "\r\n")
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("[") }
            .map { LyricLine(-1L, it) }
    }
}
