package com.spotify.clone.core.player

import android.util.Log
import com.spotify.clone.core.data.AudioRepository
import com.spotify.clone.core.data.SampleCatalog
import com.spotify.clone.core.model.MusicGenre
import com.spotify.clone.core.model.Track
import com.spotify.clone.core.recommendation.EchoRecommendationEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Same-Genre Smart Radio & Spotify-Ordered Queue Continuation Engine.
 *
 * Enforces:
 * 1. PureRadioQueue Deduplication Invariant (by track id, normalized song key, and
 *    sliding 80-track session history).
 * 2. Strict Same-Genre Locking (uses locked [MusicGenre] or auto-infers the seed track's
 *    genre so continuation stays in the exact same genre, widening to neighbor genres only
 *    if the strict pool is exhausted).
 * 3. Spotify Balanced Artist-Spacing Order so consecutive queued tracks avoid artist repeats.
 */
object SmartRadioQueue {

    private const val TAG = "SmartRadioQueue"
    private const val MAX_RECENT_HISTORY = 80
    private val recentlyPlayedKeys = LinkedHashSet<String>()

    fun normalizedSongKey(title: String, artist: String): String {
        val cleanTitle = title.trim().lowercase(Locale.ROOT)
            .replace(Regex("(?i)\\((?:feat\\.|remaster|live|deluxe).*?\\)|\\[(?:feat\\.|remaster|live|deluxe).*?\\]|- remastered.*|- single.*"), "")
            .trim()
        val cleanArtist = artist.trim().lowercase(Locale.ROOT)
            .split(',', '&', '/').firstOrNull()?.trim() ?: artist.trim().lowercase(Locale.ROOT)
        return "$cleanTitle|$cleanArtist"
    }

    fun recordTrackPlayed(track: Track) {
        val key = normalizedSongKey(track.title, track.artistName)
        synchronized(recentlyPlayedKeys) {
            recentlyPlayedKeys.add(key)
            if (recentlyPlayedKeys.size > MAX_RECENT_HISTORY) {
                val iterator = recentlyPlayedKeys.iterator()
                if (iterator.hasNext()) {
                    iterator.next()
                    iterator.remove()
                }
            }
        }
    }

    fun getRecentlyPlayedKeysSnapshot(): Set<String> = synchronized(recentlyPlayedKeys) {
        recentlyPlayedKeys.toHashSet()
    }

    fun filterDuplicates(
        candidates: List<Track>,
        currentQueue: List<Track>,
        seedTrackId: String? = null
    ): List<Track> {
        val existingKeys = currentQueue.mapTo(HashSet()) { normalizedSongKey(it.title, it.artistName) }
        val existingIds = currentQueue.mapTo(HashSet()) { it.id }
        if (seedTrackId != null) existingIds.add(seedTrackId)

        val result = mutableListOf<Track>()
        for (track in candidates) {
            if (EchoRecommendationEngine.isJunkTitle(track.title)) continue
            val key = normalizedSongKey(track.title, track.artistName)
            val isRecent = synchronized(recentlyPlayedKeys) { key in recentlyPlayedKeys }
            if (track.id !in existingIds && key !in existingKeys && !isRecent) {
                existingIds.add(track.id)
                existingKeys.add(key)
                result.add(track)
            }
        }
        return result
    }

    /**
     * Generates a strict same-genre continuation batch ordered with Spotify's artist-spacing
     * and taste scoring algorithms.
     */
    suspend fun generateRadioContinuationAsync(
        seedTrack: Track,
        currentQueue: List<Track>,
        repository: AudioRepository? = null,
        lockedGenre: MusicGenre = MusicGenre.ALL,
        autoMatchGenre: Boolean = true
    ): List<Track> = withContext(Dispatchers.IO) {
        val effectiveGenre = when {
            lockedGenre != MusicGenre.ALL -> lockedGenre
            autoMatchGenre -> MusicGenre.inferGenre(seedTrack)
            else -> MusicGenre.ALL
        }

        val primaryArtist = EchoRecommendationEngine.primaryArtistOf(seedTrack.artistName)
        val onlineMatches = mutableListOf<Track>()

        if (repository != null) {
            val seedArtist = primaryArtist.ifBlank {
                EchoRecommendationEngine.getOnboardedArtists().firstOrNull().orEmpty()
            }
            if (seedArtist.isNotBlank()) {
                runCatching { repository.search(seedArtist) }
                    .onSuccess { onlineMatches.addAll(it) }
                    .onFailure { Log.w(TAG, "Artist continuation miss: ${it.message}") }
            }
            if (effectiveGenre != MusicGenre.ALL) {
                val genreQuery = effectiveGenre.searchQueries.random()
                runCatching { repository.search(genreQuery) }
                    .onSuccess { onlineMatches.addAll(it) }
                    .onFailure { Log.w(TAG, "Genre continuation miss: ${it.message}") }
            }
        }

        val combinedPool = (onlineMatches + SampleCatalog.sampleTracks)
        val deduped = filterDuplicates(combinedPool, currentQueue, seedTrack.id)
        if (deduped.isEmpty()) return@withContext emptyList()

        // Step 1: Strict same-genre candidates first
        val strictGenrePool = if (effectiveGenre == MusicGenre.ALL) {
            deduped
        } else {
            deduped.filter { effectiveGenre.matchesTrack(it) || MusicGenre.inferGenre(it) == effectiveGenre }
        }

        // Step 2: Neighbor-genre fallback only if strict pool has fewer than 4 tracks
        val poolToOrder = if (strictGenrePool.size >= 4 || effectiveGenre == MusicGenre.ALL) {
            strictGenrePool
        } else {
            val neighbors = effectiveGenre.neighborGenres().toSet()
            val neighborMatches = deduped.filter {
                it !in strictGenrePool && MusicGenre.inferGenre(it) in neighbors
            }
            (strictGenrePool + neighborMatches).ifEmpty { deduped }
        }

        // Score & order with Spotify balanced artist-spacing interleave
        val ordered = EchoRecommendationEngine.buildSameGenreOrderedQueue(
            seedTrack = seedTrack,
            candidates = poolToOrder,
            targetGenre = effectiveGenre,
            limit = 7
        ).drop(1) // Drop seedTrack itself (already playing)

        ordered.take(6)
    }
}
