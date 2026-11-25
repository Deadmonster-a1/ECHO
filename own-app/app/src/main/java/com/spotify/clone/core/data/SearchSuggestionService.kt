package com.spotify.clone.core.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Real-time Auto-complete Search Suggestions Service for ECHO.
 * Directly adopted from LastWave-Native SearchRepository suggestion engine.
 * Fetches instant live completion queries from Google / YouTube Suggest API with offline fallback.
 */
object SearchSuggestionService {

    private const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    private val localFallbackCatalog = listOf(
        "Chuttamalle", "Fear Song", "Daavudi", "Ayudha Pooja", "Devara",
        "Pushpa Pushpa", "Angaaron", "Sooseki", "Pushpa 2 The Rule",
        "Kurchi Madathapetti", "Dum Masala", "Guntur Kaaram",
        "Hukum - Thalaivar Alappara", "Jailer", "Kaavaalaa",
        "Naa Ready", "Badass", "Leo",
        "Samayama", "Odiyamma", "Ammaadi", "Hi Nanna",
        "Inkem Inkem", "Whatey Beauty", "Geetha Govindam",
        "Butta Bomma", "Ramuloo Ramulaa", "Ala Vaikunthapurramuloo",
        "Kesariya", "Apna Bana Le", "Chaleya", "Jawan",
        "Coldplay", "Yellow", "Fix You", "Viva La Vida",
        "Taylor Swift", "Cruel Summer", "Anti-Hero", "Blank Space",
        "The Weeknd", "Starboy", "Blinding Lights", "Save Your Tears",
        "Eminem", "Houdini", "Lose Yourself", "Without Me",
        "Arijit Singh", "Anirudh Ravichander", "Sid Sriram", "Thaman S",
        "Devi Sri Prasad", "A.R. Rahman", "Shreya Ghoshal", "Armaan Malik"
    )

    suspend fun getSuggestions(query: String): List<String> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return@withContext emptyList()

        val liveResults = fetchLiveSuggestions(trimmed)
        if (liveResults.isNotEmpty()) {
            return@withContext liveResults
        }

        // Offline prefix fallback
        val q = trimmed.lowercase()
        return@withContext localFallbackCatalog.filter {
            it.lowercase().contains(q)
        }.take(8)
    }

    private fun fetchLiveSuggestions(query: String): List<String> {
        val urls = listOf(
            "https://suggestqueries.google.com/complete/search?client=firefox&ds=yt&q=${URLEncoder.encode(query, "UTF-8")}",
            "https://suggestqueries-clients6.youtube.com/complete/search?client=firefox&ds=yt&q=${URLEncoder.encode(query, "UTF-8")}"
        )

        for (urlStr in urls) {
            var conn: HttpURLConnection? = null
            try {
                val url = URL(urlStr)
                conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 2000
                conn.readTimeout = 2500
                conn.setRequestProperty("User-Agent", USER_AGENT)
                conn.setRequestProperty("Accept", "application/json")

                if (conn.responseCode == 200) {
                    val reader = BufferedReader(InputStreamReader(conn.inputStream))
                    val response = reader.use { it.readText() }
                    val rootArray = JSONArray(response)
                    if (rootArray.length() >= 2) {
                        val suggestionsArray = rootArray.getJSONArray(1)
                        val list = mutableListOf<String>()
                        for (i in 0 until suggestionsArray.length()) {
                            list.add(suggestionsArray.getString(i))
                        }
                        if (list.isNotEmpty()) {
                            return list.take(8)
                        }
                    }
                }
            } catch (_: Exception) {
                // Try next endpoint
            } finally {
                conn?.disconnect()
            }
        }
        return emptyList()
    }
}
