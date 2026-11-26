package com.spotify.clone.core.data

import com.spotify.clone.core.model.Track
import com.spotify.clone.core.recommendation.EchoRecommendationEngine
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/**
 * Spotify-grade Fuzzy Search & Personalized Result Ranking Engine.
 *
 * Combines:
 * 1. Exact & Prefix Title/Artist Boosts
 * 2. Token-Overlap Jaccard/Coverage Scoring
 * 3. Normalized Levenshtein Edit Similarity (handles typos cleanly)
 * 4. Personal Taste Affinity Boost (surfaces artists the user listens to most)
 */
object SearchRankingEngine {

    private val WHITESPACE_SPLIT_REGEX = Regex("\\s+")

    fun levenshteinDistance(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length

        var prev = IntArray(b.length + 1) { it }
        var curr = IntArray(b.length + 1)

        for (i in 1..a.length) {
            curr[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                curr[j] = min(
                    min(curr[j - 1] + 1, prev[j] + 1),
                    prev[j - 1] + cost
                )
            }
            val tmp = prev
            prev = curr
            curr = tmp
        }
        return prev[b.length]
    }

    fun normalizedSimilarity(a: String, b: String): Double {
        val s1 = a.trim().lowercase(Locale.ROOT)
        val s2 = b.trim().lowercase(Locale.ROOT)
        if (s1.isEmpty() || s2.isEmpty()) return 0.0
        val maxLen = max(s1.length, s2.length)
        val dist = levenshteinDistance(s1, s2)
        return (1.0 - dist.toDouble() / maxLen.toDouble()).coerceIn(0.0, 1.0)
    }

    fun scoreTrackMatch(query: String, track: Track): Double {
        val q = query.trim().lowercase(Locale.ROOT)
        if (q.isEmpty()) return 0.0

        val title = track.title.trim().lowercase(Locale.ROOT)
        val artist = track.artistName.trim().lowercase(Locale.ROOT)
        val album = track.albumName.trim().lowercase(Locale.ROOT)
        val qTokens = q.split(WHITESPACE_SPLIT_REGEX).filter { it.isNotBlank() }

        var score = 0.0

        // 1. Exact / Prefix / Substring match on title & artist
        when {
            title == q -> score += 120.0
            title.startsWith(q) -> score += 80.0
            title.contains(q) -> score += 45.0
        }
        when {
            artist == q -> score += 90.0
            artist.startsWith(q) -> score += 55.0
            artist.contains(q) -> score += 35.0
        }
        if (album.contains(q)) score += 18.0

        // 2. Token coverage across title + artist + album
        if (qTokens.isNotEmpty()) {
            val haystack = "$title $artist $album"
            val matchedTokens = qTokens.count { tok -> haystack.contains(tok) }
            score += (matchedTokens.toDouble() / qTokens.size.toDouble()) * 42.0
        }

        // 3. Fuzzy Levenshtein similarity (captures misspellings like "chutamale" -> "Chuttamalle")
        val titleSim = normalizedSimilarity(q, title)
        val artistSim = normalizedSimilarity(q, EchoRecommendationEngine.primaryArtistOf(artist))
        score += max(titleSim, artistSim) * 38.0

        // 4. Personal taste & liked boost
        score += EchoRecommendationEngine.getArtistAffinity(track.artistName) * 18.0
        if (track.isFavorite) score += 8.0
        if (EchoRecommendationEngine.isJunkTitle(track.title)) score -= 25.0

        return score
    }

    fun rankTracks(query: String, tracks: List<Track>): List<Track> {
        if (query.isBlank() || tracks.size <= 1) return tracks
        return tracks
            .map { it to scoreTrackMatch(query, it) }
            .sortedByDescending { it.second }
            .map { it.first }
    }
}
