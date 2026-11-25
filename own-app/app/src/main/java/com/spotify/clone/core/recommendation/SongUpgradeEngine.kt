package com.spotify.clone.core.recommendation

import com.spotify.clone.core.data.FullTrackStreamResolver
import com.spotify.clone.core.model.MusicGenre
import com.spotify.clone.core.model.Track
import com.spotify.clone.core.player.SmartRadioQueue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

data class SongUpgradeStats(
    val totalScanned: Int = 0,
    val metadataUpgradedCount: Int = 0,
    val streamUpgradedCount: Int = 0,
    val isUpgrading: Boolean = false,
    val lastUpgradeTimestampMs: Long = 0L
) {
    val totalUpgraded: Int get() = maxOf(metadataUpgradedCount, streamUpgradedCount)
}

/**
 * Automatic Song Upgrade Engine for ECHO.
 *
 * Continuously upgrades songs across Home, Liked Songs, and Recently Played:
 * 1. Metadata & Artwork Upgrade: promotes 100x100 / 150x150 thumbnails to 500x500 / 600x600 HD,
 *    decodes HTML entities, cleans junk album labels, and stamps inferred [MusicGenre].
 * 2. Studio Stream Upgrade: resolves unresolved / preview-only / encrypted tracks to verified
 *    320kbps / 160kbps full-length studio streams via [FullTrackStreamResolver] and caches
 *    the upgraded [Track] objects so playback and Room DB rows stay upgraded.
 */
object SongUpgradeEngine {

    private val upgradedTrackCache = ConcurrentHashMap<String, Track>()
    private val streamSemaphore = Semaphore(permits = 3)

    private val _upgradeStats = MutableStateFlow(SongUpgradeStats())
    val upgradeStats: StateFlow<SongUpgradeStats> = _upgradeStats.asStateFlow()

    /**
     * Synchronous fast pass: upgrades artwork resolution, decodes HTML artifacts,
     * stamps inferred genre, and applies any previously resolved studio streams.
     */
    fun upgradeMetadataFast(track: Track): Track {
        upgradedTrackCache[track.id]?.let { cached ->
            return cached.copy(isFavorite = track.isFavorite || cached.isFavorite)
        }

        val cleanTitle = decodeBasicHtml(track.title).trim()
        val cleanArtist = decodeBasicHtml(track.artistName).trim()
        val cleanAlbum = decodeBasicHtml(track.albumName).trim().ifBlank { "Single" }
        val hdArtwork = upgradeArtworkUrl(track.coverArtUrl)
        val inferredGenre = if (track.genre.isBlank() || track.genre.equals("All", ignoreCase = true)) {
            MusicGenre.inferGenre(
                track.copy(title = cleanTitle, artistName = cleanArtist, albumName = cleanAlbum)
            ).shortLabel
        } else {
            track.genre
        }

        return track.copy(
            title = cleanTitle.ifBlank { track.title },
            artistName = cleanArtist.ifBlank { track.artistName },
            albumName = cleanAlbum,
            coverArtUrl = hdArtwork,
            genre = inferredGenre
        )
    }

    /**
     * Applies fast metadata upgrade across a list and deduplicates junk / lower-quality duplicates,
     * keeping the highest-quality (non-preview, studio-resolved, HD-cover) version of each song.
     */
    fun upgradeCatalogFast(tracks: List<Track>): List<Track> {
        if (tracks.isEmpty()) return emptyList()
        val bestBySongKey = LinkedHashMap<String, Track>()
        for (raw in tracks) {
            if (EchoRecommendationEngine.isJunkTitle(raw.title)) continue
            val upgraded = upgradeMetadataFast(raw)
            val songKey = SmartRadioQueue.normalizedSongKey(upgraded.title, upgraded.artistName)
            val existing = bestBySongKey[songKey]
            if (existing == null || qualityRank(upgraded) > qualityRank(existing)) {
                bestBySongKey[songKey] = upgraded.copy(
                    isFavorite = upgraded.isFavorite || (existing?.isFavorite == true)
                )
            }
        }
        return bestBySongKey.values.toList()
    }

    /**
     * Asynchronous deep upgrade pass: resolves full-length 320kbps/160kbps studio streams
     * and album metadata for tracks that still have blank, preview, or encrypted URIs.
     * Returns only the tracks whose stream or metadata improved, and updates [upgradeStats].
     */
    suspend fun upgradeTracksDeep(
        tracks: List<Track>,
        maxConcurrentBatch: Int = 8,
        onTrackUpgraded: (suspend (Track) -> Unit)? = null
    ): List<Track> = withContext(Dispatchers.IO) {
        if (tracks.isEmpty()) return@withContext emptyList()
        _upgradeStats.value = _upgradeStats.value.copy(isUpgrading = true)

        var metadataChanged = 0
        var streamUpgraded = 0
        val fastUpgraded = tracks.map { raw ->
            val fast = upgradeMetadataFast(raw)
            if (fast != raw) metadataChanged++
            fast
        }

        val candidatesForStream = fastUpgraded
            .filter { FullTrackStreamResolver.needsResolution(it) }
            .distinctBy { it.id }
            .take(maxConcurrentBatch)

        val resolvedList = coroutineScope {
            candidatesForStream.map { track ->
                async {
                    streamSemaphore.withPermit {
                        val upgraded = resolveStudioTrack(track)
                        if (upgraded != null && upgraded.mediaUri != track.mediaUri && upgraded.mediaUri.isNotBlank()) {
                            upgradedTrackCache[upgraded.id] = upgraded
                            onTrackUpgraded?.invoke(upgraded)
                            upgraded
                        } else {
                            null
                        }
                    }
                }
            }.awaitAll().filterNotNull()
        }

        streamUpgraded = resolvedList.size
        _upgradeStats.value = SongUpgradeStats(
            totalScanned = tracks.size,
            metadataUpgradedCount = metadataChanged + streamUpgraded,
            streamUpgradedCount = (_upgradeStats.value.streamUpgradedCount + streamUpgraded).coerceAtLeast(streamUpgraded),
            isUpgrading = false,
            lastUpgradeTimestampMs = System.currentTimeMillis()
        )
        resolvedList
    }

    fun isTrackStudioUpgraded(track: Track): Boolean =
        upgradedTrackCache.containsKey(track.id) || !FullTrackStreamResolver.needsResolution(track)

    fun getCachedUpgradedTrack(trackId: String): Track? = upgradedTrackCache[trackId]

    fun clearCacheForTests() {
        upgradedTrackCache.clear()
        _upgradeStats.value = SongUpgradeStats()
    }

    internal fun upgradeArtworkUrl(url: String): String {
        if (url.isBlank()) return url
        return url
            .replace("50x50", "500x500")
            .replace("150x150", "500x500")
            .replace("100x100bb", "600x600bb")
            .replace("200x200bb", "600x600bb")
    }

    internal fun decodeBasicHtml(text: String): String {
        if (!text.contains('&')) return text
        return text
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#039;", "'")
            .replace("&apos;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
    }

    private fun qualityRank(track: Track): Int {
        var score = 0
        if (!FullTrackStreamResolver.needsResolution(track)) score += 10
        if (track.mediaUri.startsWith("saavn_enc:")) score += 4
        if (track.coverArtUrl.contains("500x500") || track.coverArtUrl.contains("600x600")) score += 3
        if (track.lyrics.isNotEmpty()) score += 2
        if (track.isFavorite) score += 2
        if (track.durationMs > 60_000L) score += 1
        return score
    }

    private suspend fun resolveStudioTrack(track: Track): Track? {
        return runCatching {
            if (track.mediaUri.startsWith("saavn_enc:")) {
                val enc = track.mediaUri.removePrefix("saavn_enc:")
                val streamUrl = FullTrackStreamResolver.resolveAuthToken(enc, bitrate = "320")
                    ?: FullTrackStreamResolver.resolveAuthToken(enc, bitrate = "160")
                if (!streamUrl.isNullOrBlank()) {
                    track.copy(mediaUri = streamUrl)
                } else {
                    null
                }
            } else {
                val details = FullTrackStreamResolver.resolveTrackDetails(
                    title = track.title,
                    artistName = track.artistName,
                    albumName = track.albumName
                )
                if (details != null && details.streamUrl.isNotBlank()) {
                    val upgradedCover = upgradeArtworkUrl(
                        details.coverArtUrl?.takeIf { it.isNotBlank() } ?: track.coverArtUrl
                    )
                    val upgradedAlbum = if (
                        track.albumName.isBlank() ||
                        track.albumName.equals("Single", ignoreCase = true) ||
                        track.albumName.contains("New Releases", ignoreCase = true)
                    ) {
                        details.albumName?.takeIf { it.isNotBlank() } ?: track.albumName
                    } else {
                        track.albumName
                    }
                    track.copy(
                        mediaUri = details.streamUrl,
                        coverArtUrl = upgradedCover,
                        albumName = upgradedAlbum,
                        durationMs = details.durationMs?.takeIf { it > 0L } ?: track.durationMs
                    )
                } else {
                    null
                }
            }
        }.getOrNull()
    }
}
