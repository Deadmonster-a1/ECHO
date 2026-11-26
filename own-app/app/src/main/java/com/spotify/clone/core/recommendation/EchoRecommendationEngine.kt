package com.spotify.clone.core.recommendation

import android.content.Context
import android.content.SharedPreferences
import com.spotify.clone.core.data.SampleCatalog
import com.spotify.clone.core.data.WorldOnboardingCatalog
import com.spotify.clone.core.model.Artist
import com.spotify.clone.core.model.EditorialBanner
import com.spotify.clone.core.model.MusicGenre
import com.spotify.clone.core.model.Playlist
import com.spotify.clone.core.model.Track
import com.spotify.clone.core.player.SmartRadioQueue
import java.util.Calendar
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.random.Random

enum class TimeOfDayBucket(val label: String) {
    MORNING("Morning Flow"),
    AFTERNOON("Afternoon Focus"),
    EVENING("Evening Energy"),
    NIGHT("Late Night")
}

/**
 * Unified Recommendation & Spotify Queue-Ordering Engine for ECHO.
 *
 * Implements the workspace's reverse-engineered & ported algorithms:
 * 1. Local Taste Behavior Scoring (Nocturne / LastWave LocalTasteSuggestionEngine):
 *    play-time/count (+2.0), early-skip penalty (-3.0), liked bonus (+5.0),
 *    recency (+1.5), and time-of-day multiplier (1.3x).
 * 2. Multi-Source Candidate Scoring & Progressive Diversity Relaxation (RecommendationEngine):
 *    tiered source weights, multi-source consensus (+10/+20), novelty vs familiarity damping,
 *    and progressive relaxation of artist/album/genre caps.
 * 3. Time-of-Day & Genre Contextualization (TeluguMusicRecommender + MusicGenre).
 * 4. Spotify Queue Ordering Algorithms (MusicPlayerQueue + RecommendationEngine):
 *    - Balanced Artist-Spacing Interleave (prevents back-to-back tracks by same artist)
 *    - Round-Robin Genre/Tag Bucket Interleave
 *    - Opener-to-Closer Energy Arc (strongest match opens, second-strongest closes)
 *    - Unplayed-First Session Shuffle (unplayed tracks shuffled ahead of played ones)
 *    - Shuffle-Safe Play-Next & End-Append Queue Splicing
 */
object EchoRecommendationEngine {

    private const val PREFS_NAME = "echo_taste_signals"
    private const val KEY_PLAY_PREFIX = "play_track_"
    private const val KEY_SKIP_PREFIX = "skip_track_"
    private const val KEY_ARTIST_PREFIX = "play_artist_"
    private const val KEY_GENRE_PREFIX = "play_genre_"
    private const val KEY_SEED_ARTISTS = "onboarded_artists"
    private const val KEY_SEED_GENRES = "onboarded_genres"
    private const val KEY_SEED_LANGUAGES = "onboarded_languages"
    private const val KEY_SEED_SONGS = "onboarded_songs"

    @Volatile private var inMemorySeedArtists: Set<String> = emptySet()
    @Volatile private var inMemorySeedGenres: Set<String> = emptySet()
    @Volatile private var inMemorySeedLanguages: Set<String> = emptySet()
    @Volatile private var inMemorySeedSongs: Set<String> = emptySet()
    @Volatile private var inMemorySeedSongTracks: List<Track> = emptyList()

    private const val PLAY_WEIGHT = 2.0f
    private const val SKIP_PENALTY = 3.0f
    private const val LIKED_BONUS = 5.0f
    private const val RECENCY_BONUS = 1.5f
    private const val TIME_OF_DAY_MULTIPLIER = 1.3f

    private const val DEFAULT_ARTIST_CAP = 2
    private const val DEFAULT_ALBUM_CAP = 2
    private const val GENRE_CAP_RATIO = 0.35

    private val JUNK_TITLE_KEYWORDS = listOf(
        "mashup", "mash up", "mash-up", "jukebox", "mega mix", "megamix",
        "non stop", "nonstop", "audio jukebox", "full album", "full songs", "compilation",
        "slowed + reverb", "slowed reverb", "slowed & reverb", "bass boosted", "8d audio",
        "karaoke", "ringtone", "1 hour", "10 hour", "clean version", "sped up"
    )

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        EchoTasteMlModel.init(context)
        DailyFeedCoordinator.init(context)
        if (prefs == null) {
            val p = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs = p
            inMemorySeedArtists = p.getStringSet(KEY_SEED_ARTISTS, emptySet())?.toSet() ?: emptySet()
            inMemorySeedGenres = p.getStringSet(KEY_SEED_GENRES, emptySet())?.toSet() ?: emptySet()
            inMemorySeedLanguages = p.getStringSet(KEY_SEED_LANGUAGES, emptySet())?.toSet() ?: emptySet()
            inMemorySeedSongs = p.getStringSet(KEY_SEED_SONGS, emptySet())?.toSet() ?: emptySet()
            inMemorySeedSongTracks = inMemorySeedSongs.mapNotNull { WorldOnboardingCatalog.findSongById(it)?.toTrack() }
        }
    }

    @JvmOverloads
    fun seedOnboardingPreferences(
        languages: Set<String>,
        genreIds: Set<String>,
        artists: Set<String>,
        songIds: Set<String> = emptySet(),
        songTracks: List<Track> = emptyList()
    ) {
        val resolvedTracks = (songTracks + songIds.mapNotNull { WorldOnboardingCatalog.findSongById(it)?.toTrack() })
            .distinctBy { SmartRadioQueue.normalizedSongKey(it.title, it.artistName) }
        val songDerivedArtists = resolvedTracks.flatMap { WorldOnboardingCatalog.extractArtistTokens(it.artistName) }
        val cleanArtists = (artists + songDerivedArtists)
            .map { primaryArtistOf(it).lowercase(Locale.ROOT) }
            .filter { it.isNotBlank() }
            .toSet()
        val cleanGenres = genreIds.map { it.trim() }.filter { it.isNotBlank() }.toSet()
        val cleanLangs = languages.map { it.trim() }.filter { it.isNotBlank() }.toSet()
        val cleanSongIds = (songIds + resolvedTracks.map { it.id }).map { it.trim() }.filter { it.isNotBlank() }.toSet()

        inMemorySeedArtists = cleanArtists
        inMemorySeedGenres = cleanGenres
        inMemorySeedLanguages = cleanLangs
        inMemorySeedSongs = cleanSongIds
        inMemorySeedSongTracks = resolvedTracks
        EchoTasteMlModel.seedFromPreferences(
            artists = cleanArtists,
            genreIds = cleanGenres,
            languages = cleanLangs,
            catalogSeedTracks = (resolvedTracks + WorldOnboardingCatalog.allOnboardingTracks() + SampleCatalog.sampleTracks)
                .distinctBy { SmartRadioQueue.normalizedSongKey(it.title, it.artistName) },
            selectedSongTracks = resolvedTracks
        )

        val p = prefs ?: return
        val editor = p.edit()
            .putStringSet(KEY_SEED_ARTISTS, cleanArtists)
            .putStringSet(KEY_SEED_GENRES, cleanGenres)
            .putStringSet(KEY_SEED_LANGUAGES, cleanLangs)
            .putStringSet(KEY_SEED_SONGS, cleanSongIds)

        cleanArtists.forEach { ak ->
            val current = p.getInt("$KEY_ARTIST_PREFIX$ak", 0)
            if (current < 4) editor.putInt("$KEY_ARTIST_PREFIX$ak", 4)
        }
        cleanGenres.forEach { gid ->
            val current = p.getInt("$KEY_GENRE_PREFIX$gid", 0)
            if (current < 4) editor.putInt("$KEY_GENRE_PREFIX$gid", 4)
        }
        cleanLangs.forEach { lang ->
            WorldOnboardingCatalog.genresForLanguage(lang).forEach { gid ->
                if (p.getInt("$KEY_GENRE_PREFIX$gid", 0) < 3) editor.putInt("$KEY_GENRE_PREFIX$gid", 3)
            }
        }
        resolvedTracks.forEach { tr ->
            val sk = SmartRadioQueue.normalizedSongKey(tr.title, tr.artistName)
            if (p.getInt("$KEY_PLAY_PREFIX$sk", 0) < 2) editor.putInt("$KEY_PLAY_PREFIX$sk", 2)
            val gk = MusicGenre.inferGenre(tr).id
            if (p.getInt("$KEY_GENRE_PREFIX$gk", 0) < 3) editor.putInt("$KEY_GENRE_PREFIX$gk", 3)
        }
        editor.apply()
    }

    fun getOnboardedArtists(): Set<String> = inMemorySeedArtists

    fun getOnboardedSongs(): Set<String> = inMemorySeedSongs

    fun getOnboardedSongTracks(): List<Track> = inMemorySeedSongTracks

    fun getOnboardedGenres(): Set<MusicGenre> =
        inMemorySeedGenres.map { MusicGenre.fromId(it) }.filter { it != MusicGenre.ALL }.toSet()

    fun getOnboardedLanguages(): Set<String> = inMemorySeedLanguages

    fun getTimeOfDayBucket(hourOfDay: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)): TimeOfDayBucket {
        return when (hourOfDay) {
            in 5..11 -> TimeOfDayBucket.MORNING
            in 12..16 -> TimeOfDayBucket.AFTERNOON
            in 17..21 -> TimeOfDayBucket.EVENING
            else -> TimeOfDayBucket.NIGHT
        }
    }

    fun isJunkTitle(title: String): Boolean {
        val lower = title.lowercase(Locale.ROOT)
        return JUNK_TITLE_KEYWORDS.any { lower.contains(it) }
    }

    private val PRIMARY_ARTIST_SPLIT_REGEX =
        Regex("""[,&•/]|\b(?:feat\.?|ft\.?|featuring)\b""", RegexOption.IGNORE_CASE)

    fun primaryArtistOf(artistName: String): String =
        artistName.split(PRIMARY_ARTIST_SPLIT_REGEX)
            .firstOrNull()
            ?.trim()
            .orEmpty()
            .ifBlank { artistName.trim() }

    fun recordTrackPlay(track: Track) {
        EchoTasteMlModel.recordInteraction(track, TasteSignalType.PLAY_START)
        val p = prefs ?: return
        val songKey = SmartRadioQueue.normalizedSongKey(track.title, track.artistName)
        val artistKey = primaryArtistOf(track.artistName).lowercase(Locale.ROOT)
        val genreKey = MusicGenre.inferGenre(track).id
        p.edit()
            .putInt("$KEY_PLAY_PREFIX$songKey", p.getInt("$KEY_PLAY_PREFIX$songKey", 0) + 1)
            .putInt("$KEY_ARTIST_PREFIX$artistKey", p.getInt("$KEY_ARTIST_PREFIX$artistKey", 0) + 1)
            .putInt("$KEY_GENRE_PREFIX$genreKey", p.getInt("$KEY_GENRE_PREFIX$genreKey", 0) + 1)
            .apply()
    }

    fun recordEarlySkip(track: Track) {
        EchoTasteMlModel.recordInteraction(track, TasteSignalType.EARLY_SKIP)
        val p = prefs ?: return
        val songKey = SmartRadioQueue.normalizedSongKey(track.title, track.artistName)
        p.edit()
            .putInt("$KEY_SKIP_PREFIX$songKey", p.getInt("$KEY_SKIP_PREFIX$songKey", 0) + 1)
            .apply()
    }

    fun getArtistAffinity(artistName: String): Float {
        val mlAff = EchoTasteMlModel.getDecayedArtistAffinity(artistName)
        val p = prefs ?: return mlAff
        val artistKey = primaryArtistOf(artistName).lowercase(Locale.ROOT)
        val count = p.getInt("$KEY_ARTIST_PREFIX$artistKey", 0)
        return max(mlAff, (count / 10f).coerceIn(0f, 1f))
    }

    fun clearTasteSignals() {
        inMemorySeedArtists = emptySet()
        inMemorySeedGenres = emptySet()
        inMemorySeedLanguages = emptySet()
        inMemorySeedSongs = emptySet()
        inMemorySeedSongTracks = emptyList()
        EchoTasteMlModel.resetForTests()
        prefs?.edit()?.clear()?.apply()
    }

    fun getTopLearnedArtists(
        recentlyPlayed: List<Track>,
        favorites: List<Track>,
        limit: Int = 4
    ): List<String> {
        val combined = favorites + recentlyPlayed + inMemorySeedSongTracks
        val fromHistory = combined
            .map { primaryArtistOf(it.artistName) }
            .filter { it.isNotBlank() }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .map { it.key }
        val fromOnboarding = inMemorySeedArtists.map { name ->
            name.split(" ").joinToString(" ") { w -> w.replaceFirstChar { c -> c.titlecase(Locale.ROOT) } }
        }
        return (fromHistory + fromOnboarding)
            .distinctBy { it.lowercase(Locale.ROOT) }
            .take(limit)
    }

    /**
     * Returns a personalized, world-aware list of [Artist]s optimized for the current user
     * based on their selected onboarding songs, selected artists, preferred languages/genres,
     * and ongoing listening/favorite history.
     */
    fun getOptimizedHomeArtists(
        recentlyPlayed: List<Track>,
        favorites: List<Track>,
        limit: Int = 16
    ): List<Artist> {
        val songPool = (favorites + recentlyPlayed + inMemorySeedSongTracks)
            .distinctBy { SmartRadioQueue.normalizedSongKey(it.title, it.artistName) }
        val learnedArtists = getTopLearnedArtists(recentlyPlayed, favorites, limit = 12).toSet()
        val combinedArtists = (learnedArtists + inMemorySeedArtists).toSet()
        val genreIds = (getOnboardedGenres().map { it.id } +
            EchoTasteMlModel.getRankedPreferredGenres(favorites, recentlyPlayed).take(3).map { it.id }).toSet()

        val optimized = WorldOnboardingCatalog.optimizeArtistsForUser(
            selectedLanguages = inMemorySeedLanguages,
            selectedGenreIds = genreIds,
            selectedSongTracks = songPool,
            selectedArtists = combinedArtists,
            prioritizeSelectedInList = true
        )
        return optimized.take(limit).map { rec ->
            val base = rec.artist.toArtist()
            val sampleMatch = SampleCatalog.sampleArtists.firstOrNull {
                it.name.equals(base.name, ignoreCase = true)
            }
            sampleMatch ?: base
        }
    }

    /**
     * Algorithm 10 (Spotify Daily Mix + Spherical K-Means Clustering):
     * Uses 12D K-Means clustering + genre clustering to build cohesive Daily Mixes,
     * Discover Daily, and Release Radar playlists ordered with Opener-Closer Energy Arc.
     */
    @JvmOverloads
    fun generateDailyMixes(
        recentlyPlayed: List<Track>,
        favorites: List<Track>,
        catalogPool: List<Track>,
        dailySeed: Long = DailyFeedCoordinator.dailySeed()
    ): List<Playlist> {
        val allTracks = SongUpgradeEngine.upgradeCatalogFast(catalogPool + inMemorySeedSongTracks + SampleCatalog.sampleTracks)
        if (allTracks.isEmpty()) return emptyList()

        val preferred = getTopPreferredGenre(recentlyPlayed, favorites)
        val seededGenres = getOnboardedGenres().toList()
        val mlRankedGenres = EchoTasteMlModel.getRankedPreferredGenres(favorites, recentlyPlayed)
        val clusterGenres = (listOf(preferred) + seededGenres + mlRankedGenres + preferred.neighborGenres() + MusicGenre.selectableGenres)
            .distinct()
            .take(3)

        val genreMixes = clusterGenres.mapIndexedNotNull { idx, genre ->
            val genreTracks = allTracks.filter {
                genre.matchesTrack(it) || MusicGenre.inferGenre(it) == genre
            }
            if (genreTracks.size < 3) return@mapIndexedNotNull null
            val seed = genreTracks.firstOrNull {
                primaryArtistOf(it.artistName).lowercase(Locale.ROOT) in inMemorySeedArtists
            } ?: genreTracks.first()
            val ordered = buildSameGenreOrderedQueue(
                seedTrack = seed,
                candidates = genreTracks,
                targetGenre = genre,
                limit = 14
            )
            val featuredArtists = ordered
                .map { primaryArtistOf(it.artistName) }
                .distinct()
                .take(3)
                .joinToString(", ")

            Playlist(
                id = "daily_mix_${idx + 1}_${genre.id}",
                name = "Daily Mix ${idx + 1}",
                description = "${genre.displayName} • Featuring $featuredArtists",
                coverArtUrl = seed.coverArtUrl,
                ownerName = genre.shortLabel,
                tracks = ordered
            )
        }

        // Also build AI Discover Daily from unexplored high-affinity ML predictions
        val knownKeys = (recentlyPlayed + favorites)
            .map { SmartRadioQueue.normalizedSongKey(it.title, it.artistName) }
            .toSet()
        val discoverPool = allTracks.filter {
            SmartRadioQueue.normalizedSongKey(it.title, it.artistName) !in knownKeys
        }.ifEmpty { allTracks }
        val discoverRecs = EchoTasteMlModel.rankCandidatesWithMl(
            candidates = discoverPool,
            favorites = favorites,
            recentlyPlayed = recentlyPlayed,
            limit = 14,
            explorationAlpha = 0.42f,
            dailySeed = dailySeed
        ).map { it.track }

        val discoverPlaylist = if (discoverRecs.size >= 3) {
            val topArtists = discoverRecs.map { primaryArtistOf(it.artistName) }.distinct().take(3).joinToString(", ")
            Playlist(
                id = "daily_mix_discover_${DailyFeedCoordinator.formatDateKey()}",
                name = "Discover Daily",
                description = "AI-picked fresh tracks tailored to your taste • $topArtists",
                coverArtUrl = discoverRecs.first().coverArtUrl,
                ownerName = "echo AI",
                tracks = spotifyArtistInterleave(discoverRecs)
            )
        } else null

        // K-Means Taste Cluster pod if we have enough rich catalog tracks
        val mlClusters = EchoTasteMlModel.clusterTracksForDailyMixes(
            pool = allTracks,
            favorites = favorites,
            recentlyPlayed = recentlyPlayed,
            k = 2
        ).mapIndexedNotNull { idx, cluster ->
            if (cluster.tracks.size < 4) return@mapIndexedNotNull null
            Playlist(
                id = "daily_mix_ai_${idx + 1}_${cluster.dominantGenre.id}",
                name = "${cluster.dominantGenre.shortLabel} Pulse",
                description = "ML-clustered ${cluster.dominantGenre.displayName} flow • Updated today",
                coverArtUrl = cluster.tracks.first().coverArtUrl,
                ownerName = "AI Mood Mix",
                tracks = cluster.tracks
            )
        }

        return (genreMixes + listOfNotNull(discoverPlaylist) + mlClusters).distinctBy { it.id }
    }

    fun getTopPreferredGenre(recentlyPlayed: List<Track>, favorites: List<Track>): MusicGenre {
        val p = prefs
        if (p != null) {
            var bestGenre: MusicGenre? = null
            var bestPlays = 0
            for (g in MusicGenre.selectableGenres) {
                val count = p.getInt("$KEY_GENRE_PREFIX${g.id}", 0)
                if (count > bestPlays) {
                    bestPlays = count
                    bestGenre = g
                }
            }
            if (bestGenre != null && bestPlays > 0) return bestGenre
        }
        val firstSeededGenre = getOnboardedGenres().firstOrNull()
        if (firstSeededGenre != null) return firstSeededGenre
        val combined = favorites + recentlyPlayed
        if (combined.isNotEmpty()) {
            return combined.groupingBy { MusicGenre.inferGenre(it) }
                .eachCount()
                .maxByOrNull { it.value }
                ?.key ?: MusicGenre.TOL_TELUGU
        }
        return when (getTimeOfDayBucket()) {
            TimeOfDayBucket.MORNING -> MusicGenre.MELODIES
            TimeOfDayBucket.AFTERNOON -> MusicGenre.GLOBAL_POP
            TimeOfDayBucket.EVENING -> MusicGenre.MASS_BEATS
            TimeOfDayBucket.NIGHT -> MusicGenre.LO_FI
        }
    }

    /**
     * Algorithm 1: Local Taste Behavior Scoring (Nocturne / LastWave LocalTasteSuggestionEngine).
     */
    fun scoreLocalTasteSignal(
        track: Track,
        playCount: Int,
        skipCount: Int,
        isLiked: Boolean,
        isRecent: Boolean,
        timeBucket: TimeOfDayBucket = getTimeOfDayBucket()
    ): Float {
        var score = (playCount * PLAY_WEIGHT) - (skipCount * SKIP_PENALTY)
        if (isLiked) score += LIKED_BONUS
        if (isRecent) score += RECENCY_BONUS

        val genre = MusicGenre.inferGenre(track)
        val matchesTimeContext = when (timeBucket) {
            TimeOfDayBucket.MORNING -> genre == MusicGenre.MELODIES || genre == MusicGenre.DEVOTIONAL || genre == MusicGenre.INDIE || isRecent
            TimeOfDayBucket.AFTERNOON -> genre == MusicGenre.GLOBAL_POP || genre == MusicGenre.INDIE || genre == MusicGenre.BOLLYWOOD || genre == MusicGenre.TOL_TELUGU
            TimeOfDayBucket.EVENING -> genre == MusicGenre.MASS_BEATS || genre == MusicGenre.HIP_HOP || genre == MusicGenre.ELECTRONIC || genre == MusicGenre.TOL_TELUGU
            TimeOfDayBucket.NIGHT -> genre == MusicGenre.LO_FI || genre == MusicGenre.MELODIES || genre == MusicGenre.INDIE || isLiked
        }
        if (matchesTimeContext) {
            score *= TIME_OF_DAY_MULTIPLIER
        }
        return score.coerceAtLeast(0f)
    }

    data class CandidateEntry(
        val track: Track,
        val sourceWeight: Int,
        val sourceCount: Int = 1,
        val isPrimaryGenre: Boolean = false,
        val isFresh: Boolean = true,
        val isFamiliar: Boolean = false
    )

    /**
     * Algorithm 2: Multi-Source Candidate Scoring (RecommendationEngine _recoScore).
     */
    fun scoreCandidate(
        entry: CandidateEntry,
        topArtists: Set<String>,
        recentArtists: Set<String>,
        likedKeys: Set<String>,
        recentKeys: Set<String>,
        timeBucket: TimeOfDayBucket = getTimeOfDayBucket()
    ): Double {
        val track = entry.track
        val songKey = SmartRadioQueue.normalizedSongKey(track.title, track.artistName)
        val artistKey = primaryArtistOf(track.artistName).lowercase(Locale.ROOT)
        val p = prefs
        val playCount = p?.getInt("$KEY_PLAY_PREFIX$songKey", 0) ?: 0
        val skipCount = p?.getInt("$KEY_SKIP_PREFIX$songKey", 0) ?: 0
        val isLiked = track.isFavorite || songKey in likedKeys
        val isRecent = songKey in recentKeys

        var score = when {
            entry.sourceWeight >= 4 -> 50.0
            entry.sourceWeight == 3 -> 38.0
            entry.sourceWeight == 2 -> 26.0
            else -> 12.0
        }

        score += scoreLocalTasteSignal(track, playCount, skipCount, isLiked, isRecent, timeBucket)
        score += getArtistAffinity(track.artistName) * 30.0
        if (artistKey in topArtists) score += 16.0
        if (artistKey in recentArtists) score += 8.0
        if (artistKey !in topArtists && artistKey !in recentArtists) score += 3.0
        if (entry.isPrimaryGenre) score += 4.0

        if (entry.sourceCount >= 3) score += 20.0 else if (entry.sourceCount == 2) score += 10.0

        score += if (entry.isFresh) 6.0 else -20.0
        if (entry.isFamiliar) score -= 22.0
        if (skipCount >= 2 && playCount == 0) score -= 24.0
        return score
    }

    /**
     * Algorithm 3: Progressive Diversity Relaxation Selection (RecommendationEngine selectFinal).
     * Caps max tracks per artist (2), album (2), and genre bucket (35%), relaxing step-by-step
     * only if the fresh pool cannot reach [limit].
     */
    fun selectWithDiversityCaps(
        scoredTracks: List<Pair<Track, Double>>,
        limit: Int,
        enforceGenreCap: Boolean = true
    ): List<Track> {
        if (scoredTracks.isEmpty() || limit <= 0) return emptyList()
        val genreCap = max(2, ceil(limit * GENRE_CAP_RATIO).toInt())

        fun attempt(artistCap: Int, albumCapOn: Boolean, genreCapOn: Boolean): List<Track> {
            val artistCounts = HashMap<String, Int>()
            val albumCounts = HashMap<String, Int>()
            val genreCounts = HashMap<MusicGenre, Int>()
            val seenKeys = HashSet<String>()
            val picked = ArrayList<Track>(limit)

            for ((track, _) in scoredTracks) {
                if (isJunkTitle(track.title)) continue
                val key = SmartRadioQueue.normalizedSongKey(track.title, track.artistName)
                if (key in seenKeys) continue

                val ak = primaryArtistOf(track.artistName).lowercase(Locale.ROOT)
                if ((artistCounts[ak] ?: 0) >= artistCap) continue

                if (albumCapOn && track.albumName.isNotBlank() && !track.albumName.equals("Single", ignoreCase = true)) {
                    val albKey = track.albumName.trim().lowercase(Locale.ROOT)
                    if ((albumCounts[albKey] ?: 0) >= DEFAULT_ALBUM_CAP) continue
                }

                if (genreCapOn) {
                    val g = MusicGenre.inferGenre(track)
                    if ((genreCounts[g] ?: 0) >= genreCap) continue
                    genreCounts[g] = (genreCounts[g] ?: 0) + 1
                }

                artistCounts[ak] = (artistCounts[ak] ?: 0) + 1
                if (track.albumName.isNotBlank() && !track.albumName.equals("Single", ignoreCase = true)) {
                    val albKey = track.albumName.trim().lowercase(Locale.ROOT)
                    albumCounts[albKey] = (albumCounts[albKey] ?: 0) + 1
                }
                seenKeys.add(key)
                picked.add(track)
                if (picked.size >= limit) break
            }
            return picked
        }

        var result = attempt(DEFAULT_ARTIST_CAP, albumCapOn = true, genreCapOn = enforceGenreCap)
        if (result.size < limit) result = attempt(3, albumCapOn = true, genreCapOn = enforceGenreCap)
        if (result.size < limit) result = attempt(3, albumCapOn = true, genreCapOn = false)
        if (result.size < limit) result = attempt(4, albumCapOn = false, genreCapOn = false)
        if (result.size < limit) result = attempt(99, albumCapOn = false, genreCapOn = false)
        return result
    }

    /**
     * Algorithm 4 (Spotify Queue Order): Balanced Artist-Spacing Interleave.
     * Ensures tracks from the same primary artist are evenly distributed across the queue
     * so the same artist never plays consecutively when other artists are available.
     */
    fun spotifyArtistInterleave(tracks: List<Track>): List<Track> {
        if (tracks.size <= 2) return tracks

        val buckets = LinkedHashMap<String, ArrayDeque<Track>>()
        for (track in tracks) {
            val ak = primaryArtistOf(track.artistName).lowercase(Locale.ROOT).ifBlank { track.id }
            buckets.getOrPut(ak) { ArrayDeque() }.addLast(track)
        }

        // Order artist buckets largest-first so heavy artists spread evenly across slots
        val orderedKeys = buckets.keys.sortedByDescending { buckets[it]?.size ?: 0 }
        val result = ArrayList<Track>(tracks.size)
        var lastArtist: String? = null

        while (result.size < tracks.size) {
            // Pick the non-empty bucket with the most remaining tracks that differs from lastArtist
            val nextKey = orderedKeys
                .filter { (buckets[it]?.isNotEmpty() == true) && it != lastArtist }
                .maxByOrNull { buckets[it]?.size ?: 0 }
                ?: orderedKeys.firstOrNull { buckets[it]?.isNotEmpty() == true }
                ?: break

            val picked = buckets[nextKey]!!.removeFirst()
            result.add(picked)
            lastArtist = nextKey
        }
        return result
    }

    /**
     * Algorithm 5 (RecommendationEngine _recoInterleave): Round-Robin Genre Bucket Interleave.
     * Avoids consecutive tracks of the same genre/mood in multi-genre recommendation mixes.
     */
    fun roundRobinGenreInterleave(tracks: List<Track>): List<Track> {
        if (tracks.size <= 2) return tracks
        val buckets = LinkedHashMap<MusicGenre, ArrayDeque<Track>>()
        for (track in tracks) {
            val genre = MusicGenre.inferGenre(track)
            buckets.getOrPut(genre) { ArrayDeque() }.addLast(track)
        }
        val labels = buckets.keys.toList()
        val out = ArrayList<Track>(tracks.size)
        var lastGenre: MusicGenre? = null

        while (out.size < tracks.size) {
            // Pick the largest remaining bucket different from the previous genre so
            // multi-genre queues alternate evenly instead of starving later genres.
            val g = labels.filter { genre -> buckets[genre]!!.isNotEmpty() && genre != lastGenre }
                .maxByOrNull { genre -> buckets[genre]!!.size }
                ?: labels.filter { genre -> buckets[genre]!!.isNotEmpty() }
                    .maxByOrNull { genre -> buckets[genre]!!.size }
                ?: break
            out.add(buckets[g]!!.removeFirst())
            lastGenre = g
        }
        return out
    }

    /**
     * Algorithm 6 (RecommendationEngine Playlist Flow): Opener-to-Closer Energy Arc.
     * Strongest match opens the playlist; second-strongest closes it; middle is interleaved.
     */
    fun applyOpenerCloserArc(tracks: List<Track>, scoreByKey: Map<String, Double>): List<Track> {
        if (tracks.size < 3) return tracks
        val ranked = tracks.sortedByDescending {
            scoreByKey[SmartRadioQueue.normalizedSongKey(it.title, it.artistName)] ?: 0.0
        }
        val opener = ranked[0]
        val closer = ranked.firstOrNull { it.id != opener.id } ?: ranked[1]
        val middle = tracks.filter { it.id != opener.id && it.id != closer.id }
        return listOf(opener) + middle + listOf(closer)
    }

    /**
     * Algorithm 7 (Spotify / LastWave MusicPlayerQueue): Unplayed-First Session Shuffle.
     * Keeps [currentIndex] track first, places unplayed tracks in artist-spaced shuffled order,
     * and moves already-played tracks behind them so nothing repeats prematurely.
     */
    fun orderUnplayedFirstShuffle(
        queue: List<Track>,
        currentIndex: Int,
        playedSongKeys: Set<String>,
        random: Random = Random.Default
    ): List<Track> {
        if (queue.size <= 1) return queue
        val active = queue.getOrNull(currentIndex)
        val remaining = queue.filterIndexed { idx, _ -> idx != currentIndex }
        val (alreadyPlayed, unplayed) = remaining.partition {
            SmartRadioQueue.normalizedSongKey(it.title, it.artistName) in playedSongKeys
        }
        val orderedUnplayed = spotifyArtistInterleave(unplayed.shuffled(random))
        val orderedPlayed = spotifyArtistInterleave(alreadyPlayed.shuffled(random))
        return if (active != null) {
            listOf(active) + orderedUnplayed + orderedPlayed
        } else {
            orderedUnplayed + orderedPlayed
        }
    }

    /**
     * Algorithm 8 (MusicPlayerQueue.placeInsertedIndexInShuffleOrder):
     * Splices [track] right after [currentIndex] ("Play Next") or at the end ("Add to Queue"),
     * deduplicating if already upcoming.
     */
    fun insertPlayNext(queue: List<Track>, currentIndex: Int, track: Track): List<Track> {
        if (queue.isEmpty()) return listOf(track)
        val safeIdx = currentIndex.coerceIn(0, queue.lastIndex)
        val beforeAndCurrent = queue.subList(0, safeIdx + 1)
        val after = queue.subList(safeIdx + 1, queue.size).filter { it.id != track.id }
        return beforeAndCurrent + track + after
    }

    /**
     * Algorithm 9: Strict Same-Genre Seed Queue Builder.
     * Takes a seed track, infers (or uses locked) genre, scores all same-genre candidates
     * by seed artist match + taste score, enforces strict deduplication, and returns them
     * in Spotify artist-spaced order with the seed track at index 0.
     */
    fun buildSameGenreOrderedQueue(
        seedTrack: Track,
        candidates: List<Track>,
        targetGenre: MusicGenre = MusicGenre.inferGenre(seedTrack),
        limit: Int = 20
    ): List<Track> {
        val seedArtist = primaryArtistOf(seedTrack.artistName).lowercase(Locale.ROOT)
        val seedKey = SmartRadioQueue.normalizedSongKey(seedTrack.title, seedTrack.artistName)
        val neighbors = targetGenre.neighborGenres().toSet()

        val scored = candidates
            .filter { candidate ->
                candidate.id != seedTrack.id &&
                    !isJunkTitle(candidate.title) &&
                    SmartRadioQueue.normalizedSongKey(candidate.title, candidate.artistName) != seedKey
            }
            .mapNotNull { candidate ->
                val g = MusicGenre.inferGenre(candidate)
                val sameGenre = targetGenre == MusicGenre.ALL || targetGenre.matchesTrack(candidate) || g == targetGenre
                val neighborGenre = g in neighbors
                if (!sameGenre && !neighborGenre) return@mapNotNull null

                val candArtist = primaryArtistOf(candidate.artistName).lowercase(Locale.ROOT)
                val sameArtistBonus = if (candArtist == seedArtist) 24.0 else 0.0
                val sameAlbumBonus = if (
                    seedTrack.albumName.isNotBlank() &&
                    !seedTrack.albumName.equals("Single", ignoreCase = true) &&
                    candidate.albumName.equals(seedTrack.albumName, ignoreCase = true)
                ) 12.0 else 0.0
                val genreWeight = if (sameGenre) 4 else 1

                val score = scoreCandidate(
                    entry = CandidateEntry(
                        track = candidate,
                        sourceWeight = genreWeight,
                        isPrimaryGenre = sameGenre,
                        isFresh = true
                    ),
                    topArtists = setOf(seedArtist),
                    recentArtists = emptySet(),
                    likedKeys = emptySet(),
                    recentKeys = emptySet()
                ) + sameArtistBonus + sameAlbumBonus

                candidate to score
            }
            .sortedByDescending { it.second }

        if (limit <= 0) return emptyList()
        if (limit == 1) return listOf(seedTrack)
        val selected = selectWithDiversityCaps(
            scoredTracks = scored,
            limit = limit - 1,
            enforceGenreCap = false // Same-genre radio stays strictly within the target genre
        )
        return listOf(seedTrack) + spotifyArtistInterleave(selected)
    }

    /**
     * Builds the personalized Home "Recommended For You" feed using all recommendation
     * and Spotify ordering algorithms (multi-source scoring + ML Contextual Bandit + MMR +
     * diversity caps + round-robin genre interleave + artist spacing + opener/closer arc).
     */
    fun generateHomeRecommendations(
        recentlyPlayed: List<Track>,
        favorites: List<Track>,
        catalogPool: List<Track>,
        limit: Int = 12
    ): List<Track> = generateHomeRecommendationsWithReasons(
        recentlyPlayed = recentlyPlayed,
        favorites = favorites,
        catalogPool = catalogPool,
        limit = limit
    ).map { it.track }

    /**
     * AI/ML-powered recommendation generator returning explainable [MlTrackPrediction] entries
     * combining heuristic multi-source signals, 12D audio embeddings, Online SGD Logistic Ranker,
     * LinUCB exploration, and MMR diversity re-ranking.
     */
    fun generateHomeRecommendationsWithReasons(
        recentlyPlayed: List<Track>,
        favorites: List<Track>,
        catalogPool: List<Track>,
        limit: Int = 12,
        dailySeed: Long = DailyFeedCoordinator.dailySeed()
    ): List<MlTrackPrediction> {
        val timeBucket = getTimeOfDayBucket()
        val recentKeys = recentlyPlayed.map { SmartRadioQueue.normalizedSongKey(it.title, it.artistName) }.toSet()
        val likedKeys = favorites.map { SmartRadioQueue.normalizedSongKey(it.title, it.artistName) }.toSet()
        val topArtists = ((favorites + recentlyPlayed)
            .groupingBy { primaryArtistOf(it.artistName).lowercase(Locale.ROOT) }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(6)
            .map { it.key } + inMemorySeedArtists)
            .toSet()
        val recentArtists = recentlyPlayed.take(6)
            .map { primaryArtistOf(it.artistName).lowercase(Locale.ROOT) }
            .toSet()
        val preferredGenre = getTopPreferredGenre(recentlyPlayed, favorites)
        val seededGenres = getOnboardedGenres()

        val mergedPool = SongUpgradeEngine.upgradeCatalogFast(catalogPool + inMemorySeedSongTracks + SampleCatalog.sampleTracks)
        val catalogTrackIds = catalogPool.mapTo(HashSet()) { it.id }
        val catalogSongKeys = catalogPool.mapTo(HashSet()) {
            SmartRadioQueue.normalizedSongKey(it.title, it.artistName)
        }

        // Blend heuristic multi-source ranker + ML predictions
        val mlRanked = EchoTasteMlModel.rankCandidatesWithMl(
            candidates = mergedPool,
            favorites = favorites,
            recentlyPlayed = recentlyPlayed,
            timeBucket = timeBucket,
            limit = mergedPool.size.coerceAtLeast(16),
            dailySeed = dailySeed
        ).associateBy { SmartRadioQueue.normalizedSongKey(it.track.title, it.track.artistName) }

        val scoredEntries = mergedPool.map { track ->
            val key = SmartRadioQueue.normalizedSongKey(track.title, track.artistName)
            val artistKey = primaryArtistOf(track.artistName).lowercase(Locale.ROOT)
            val inferred = MusicGenre.inferGenre(track)
            val isPrimary = inferred == preferredGenre || inferred in seededGenres
            val isNeighbor = inferred in preferredGenre.neighborGenres()

            var sourceCount = 0
            if (artistKey in topArtists) sourceCount++
            if (isPrimary || isNeighbor) sourceCount++
            if (track.id in catalogTrackIds || key in catalogSongKeys) sourceCount++

            val weight = when {
                artistKey in topArtists && isPrimary -> 4
                isPrimary || artistKey in topArtists -> 3
                isNeighbor -> 2
                else -> 1
            }

            val entry = CandidateEntry(
                track = track,
                sourceWeight = weight,
                sourceCount = sourceCount.coerceAtLeast(1),
                isPrimaryGenre = isPrimary,
                isFresh = key !in recentKeys.take(4).toSet(),
                isFamiliar = key in likedKeys && key in recentKeys.take(2)
            )
            val heuristic = scoreCandidate(entry, topArtists, recentArtists, likedKeys, recentKeys, timeBucket)
            val mlScore = (mlRanked[key]?.totalScore ?: 0.5) * 35.0
            track to (heuristic + mlScore)
        }.sortedByDescending { it.second }

        val scoreByKey = scoredEntries.associate {
            SmartRadioQueue.normalizedSongKey(it.first.title, it.first.artistName) to it.second
        }
        val selected = selectWithDiversityCaps(scoredEntries, limit = limit, enforceGenreCap = true)
        val genreInterleaved = roundRobinGenreInterleave(selected)
        val artistSpaced = spotifyArtistInterleave(genreInterleaved)
        val finalOrdered = applyOpenerCloserArc(artistSpaced, scoreByKey)

        return finalOrdered.map { track ->
            val key = SmartRadioQueue.normalizedSongKey(track.title, track.artistName)
            mlRanked[key]?.copy(track = track) ?: MlTrackPrediction(
                track = track,
                enjoyProbability = 0.82f,
                cosineSimilarity = 0.78f,
                ucbExplorationBonus = 0.1f,
                coOccurrenceBoost = 0f,
                totalScore = (scoreByKey[key] ?: 0.0) / 100.0,
                matchPercent = 86,
                reasonTag = MusicGenre.inferGenre(track).shortLabel
            )
        }
    }

    /**
     * Generates a dedicated "From Your Likes" mix using Co-Occurrence Graph + 12D Cosine Similarity
     * anchored on the user's Liked Songs.
     */
    fun generateLikedInspiredMix(
        favorites: List<Track>,
        catalogPool: List<Track>,
        limit: Int = 10
    ): Pair<String, List<Track>> {
        if (favorites.isEmpty()) return "" to emptyList()
        EchoTasteMlModel.syncLikedCoOccurrence(favorites)
        val likedKeys = favorites.map { SmartRadioQueue.normalizedSongKey(it.title, it.artistName) }.toSet()
        val anchor = favorites.first()
        val pool = SongUpgradeEngine.upgradeCatalogFast(catalogPool + SampleCatalog.sampleTracks)
            .filter { SmartRadioQueue.normalizedSongKey(it.title, it.artistName) !in likedKeys }
            .ifEmpty { SongUpgradeEngine.upgradeCatalogFast(catalogPool + SampleCatalog.sampleTracks) }

        val anchorVec = EchoTasteMlModel.embedTrack(anchor)
        val scored = pool.map { cand ->
            val coBoost = EchoTasteMlModel.getCoOccurrenceBoost(cand, favorites.take(8))
            val cos = ((EchoTasteMlModel.cosineSimilarity(anchorVec, EchoTasteMlModel.embedTrack(cand)) + 1f) * 0.5f)
            val score = coBoost * 0.65f + cos * 0.35f
            cand to score.toDouble()
        }.sortedByDescending { it.second }

        val selected = selectWithDiversityCaps(scored, limit = limit, enforceGenreCap = true)
        val label = if (favorites.size == 1) {
            "Inspired by ${anchor.title}"
        } else {
            "Inspired by ${anchor.title} & ${favorites.size - 1} more likes"
        }
        return label to spotifyArtistInterleave(selected)
    }

    /**
     * Generates top Genre World playlists ranked by learned ML affinity + history.
     */
    fun generateGenreMixes(
        favorites: List<Track>,
        recentlyPlayed: List<Track>,
        catalogPool: List<Track>,
        limit: Int = 6
    ): List<Playlist> {
        val pool = SongUpgradeEngine.upgradeCatalogFast(catalogPool + SampleCatalog.sampleTracks)
        val rankedGenres = EchoTasteMlModel.getRankedPreferredGenres(favorites, recentlyPlayed)
        return rankedGenres.mapNotNull { genre ->
            val tracks = pool.filter { genre.matchesTrack(it) || MusicGenre.inferGenre(it) == genre }
            if (tracks.size < 2) return@mapNotNull null
            val ordered = spotifyArtistInterleave(tracks).take(15)
            Playlist(
                id = "genre_world_${genre.id}",
                name = genre.shortLabel,
                description = "${genre.displayName} • ${ordered.size} curated tracks",
                coverArtUrl = ordered.first().coverArtUrl,
                ownerName = "Genre Mix",
                tracks = ordered
            )
        }.take(limit)
    }

    /**
     * Builds daily-evolving Spotlight Editorial Banners incorporating Discover Daily,
     * Time-of-Day flow, Release Radar, and Liked Songs Radio.
     */
    fun generateDailySpotlightBanners(
        favorites: List<Track>,
        newSongs: List<Track>,
        dailyMixes: List<Playlist>,
        timeBucket: TimeOfDayBucket = getTimeOfDayBucket()
    ): List<EditorialBanner> {
        val list = mutableListOf<EditorialBanner>()
        val discoverMix = dailyMixes.firstOrNull { it.id.startsWith("daily_mix_discover_") }
        if (discoverMix != null && discoverMix.tracks.isNotEmpty()) {
            list.add(
                EditorialBanner(
                    id = discoverMix.id,
                    eyebrow = "AI DISCOVER DAILY • ${DailyFeedCoordinator.friendlyDateLabel().uppercase(Locale.US)}",
                    title = "Discover Daily",
                    subtitle = discoverMix.description,
                    coverArtUrl = discoverMix.coverArtUrl,
                    targetPlaylistId = discoverMix.id,
                    targetQuery = discoverMix.tracks.first().title
                )
            )
        }
        list.add(getContextualBanner(timeBucket))
        if (newSongs.isNotEmpty()) {
            val topNew = newSongs.first()
            list.add(
                EditorialBanner(
                    id = "hero_release_radar_${DailyFeedCoordinator.formatDateKey()}",
                    eyebrow = "RELEASE RADAR • NEW TODAY",
                    title = "Fresh Releases For You",
                    subtitle = "${topNew.title}, ${newSongs.getOrNull(1)?.title ?: "top hits"} & today's studio drops",
                    coverArtUrl = topNew.coverArtUrl,
                    targetPlaylistId = null,
                    targetQuery = "${topNew.title} ${topNew.artistName}"
                )
            )
        }
        if (favorites.isNotEmpty()) {
            val fav = favorites.first()
            list.add(
                EditorialBanner(
                    id = "hero_liked_vibes",
                    eyebrow = "LIKED RADIO • ${favorites.size} FAVORITES",
                    title = "Your Liked Vibes",
                    subtitle = "Built around ${fav.title} & your personal taste vector",
                    coverArtUrl = fav.coverArtUrl,
                    targetPlaylistId = "pl_liked_songs",
                    targetQuery = fav.title
                )
            )
        }
        return list.distinctBy { it.id }
    }

    fun toggleFollowedArtist(artistName: String, nowFollowed: Boolean) {
        val ak = primaryArtistOf(artistName).lowercase(Locale.ROOT)
        if (ak.isBlank()) return
        inMemorySeedArtists = if (nowFollowed) {
            inMemorySeedArtists + ak
        } else {
            inMemorySeedArtists - ak
        }
        val p = prefs ?: return
        val editor = p.edit().putStringSet(KEY_SEED_ARTISTS, inMemorySeedArtists)
        if (nowFollowed) {
            val current = p.getInt("$KEY_ARTIST_PREFIX$ak", 0)
            if (current < 5) editor.putInt("$KEY_ARTIST_PREFIX$ak", 5)
        }
        editor.apply()
    }

    /**
     * Time-of-day & user-taste editorial hero recommendation.
     * Adapts automatically to the user's onboarded Indian, English, and global preferences.
     */
    fun getContextualBanner(timeBucket: TimeOfDayBucket = getTimeOfDayBucket()): EditorialBanner {
        val langs = inMemorySeedLanguages.map { it.lowercase(Locale.ROOT) }.toSet()
        val prefersHindi = "hindi" in langs || "punjabi" in langs || "urdu" in langs
        val prefersGlobal = "english" in langs || "korean" in langs || "spanish" in langs ||
            "japanese" in langs || "global" in langs
        val prefersTelugu = "telugu" in langs || langs.isEmpty()

        if (!prefersTelugu && prefersHindi) {
            return when (timeBucket) {
                TimeOfDayBucket.MORNING -> EditorialBanner(
                    id = "ctx_morning_hi",
                    eyebrow = "MORNING FLOW",
                    title = "Soulful Bollywood & Indie",
                    subtitle = "Warm acoustic melodies by Arijit Singh, Anuv Jain & A.R. Rahman",
                    coverArtUrl = "https://c.saavncdn.com/871/Brahmastra-Original-Motion-Picture-Soundtrack-Hindi-2022-20221006155213-500x500.jpg",
                    targetPlaylistId = null,
                    targetQuery = "arijit singh melodies"
                )
                TimeOfDayBucket.AFTERNOON -> EditorialBanner(
                    id = "ctx_afternoon_hi",
                    eyebrow = "AFTERNOON FOCUS",
                    title = "Bollywood & Desi Pop",
                    subtitle = "Crisp hits from Pritam, Shilpa Rao & Sachin-Jigar",
                    coverArtUrl = "https://c.saavncdn.com/871/Brahmastra-Original-Motion-Picture-Soundtrack-Hindi-2022-20221006155213-500x500.jpg",
                    targetPlaylistId = null,
                    targetQuery = "hindi top hits"
                )
                TimeOfDayBucket.EVENING -> EditorialBanner(
                    id = "ctx_evening_hi",
                    eyebrow = "EVENING ENERGY",
                    title = "Punjabi & Party Anthems",
                    subtitle = "High-energy drops from Diljit Dosanjh, Karan Aujla & Badshah",
                    coverArtUrl = "https://c.saavncdn.com/888/Kalki-2898-Ad-Telugu-Telugu-2024-20240712063717-500x500.jpg",
                    targetPlaylistId = null,
                    targetQuery = "punjabi party hits"
                )
                TimeOfDayBucket.NIGHT -> EditorialBanner(
                    id = "ctx_night_hi",
                    eyebrow = "LATE NIGHT",
                    title = "Midnight Acoustic & Sufi",
                    subtitle = "Unplugged calm from Anuv Jain, Prateek Kuhad & Ali Sethi",
                    coverArtUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600&q=80",
                    targetPlaylistId = null,
                    targetQuery = "anuv jain acoustic"
                )
            }
        }

        if (!prefersTelugu && prefersGlobal) {
            return when (timeBucket) {
                TimeOfDayBucket.MORNING -> EditorialBanner(
                    id = "ctx_morning_en",
                    eyebrow = "MORNING FLOW",
                    title = "Global Acoustic & Chill",
                    subtitle = "Gentle starts with Ed Sheeran, Billie Eilish & Fujii Kaze",
                    coverArtUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&q=80",
                    targetPlaylistId = null,
                    targetQuery = "chill acoustic pop"
                )
                TimeOfDayBucket.AFTERNOON -> EditorialBanner(
                    id = "ctx_afternoon_en",
                    eyebrow = "AFTERNOON FOCUS",
                    title = "Global Pop & K-Pop Hits",
                    subtitle = "Today's biggest worldwide chart-toppers",
                    coverArtUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music125/v4/49/3d/ab/493dab54-f920-9043-6181-80993b8116c9/19UMGIM53909.rgb.jpg/600x600bb.jpg",
                    targetPlaylistId = null,
                    targetQuery = "top pop hits 2026"
                )
                TimeOfDayBucket.EVENING -> EditorialBanner(
                    id = "ctx_evening_en",
                    eyebrow = "EVENING ENERGY",
                    title = "Hip-Hop, Rock & EDM",
                    subtitle = "The Weeknd, Kendrick Lamar, Coldplay & Calvin Harris",
                    coverArtUrl = "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=600&q=80",
                    targetPlaylistId = null,
                    targetQuery = "the weeknd kendrick lamar"
                )
                TimeOfDayBucket.NIGHT -> EditorialBanner(
                    id = "ctx_night_en",
                    eyebrow = "LATE NIGHT",
                    title = "After Hours & Midnight Lo-Fi",
                    subtitle = "Dreamy late-night soundscapes and R&B",
                    coverArtUrl = "https://images.unsplash.com/photo-1498038432885-c6f3f1b912ee?w=600&q=80",
                    targetPlaylistId = null,
                    targetQuery = "after hours midnight chill"
                )
            }
        }

        return when (timeBucket) {
            TimeOfDayBucket.MORNING -> EditorialBanner(
                id = "ctx_morning",
                eyebrow = "MORNING FLOW",
                title = "Gentle Acoustic & Melodies",
                subtitle = "Warm vocals and serene melodies to ease into your day",
                coverArtUrl = "https://c.saavncdn.com/041/Sita-Ramam-Telugu-Original-Motion-Picture-Soundtrack-Telugu-2022-20220802140738-500x500.jpg",
                targetPlaylistId = "pl_sita_ramam",
                targetQuery = "Sita Ramam Inthandham"
            )
            TimeOfDayBucket.AFTERNOON -> EditorialBanner(
                id = "ctx_afternoon",
                eyebrow = "AFTERNOON FOCUS",
                title = "Kalki & Global Hits",
                subtitle = "Crisp momentum tracks and cinematic scores for midday flow",
                coverArtUrl = "https://c.saavncdn.com/888/Kalki-2898-Ad-Telugu-Telugu-2024-20240712063717-500x500.jpg",
                targetPlaylistId = "pl_kalki_2898",
                targetQuery = "Bhairava Anthem Kalki"
            )
            TimeOfDayBucket.EVENING -> EditorialBanner(
                id = "ctx_evening",
                eyebrow = "EVENING ENERGY",
                title = "Pushpa 2 & Mass Anthems",
                subtitle = "High-voltage beats and chartbusters for peak evening energy",
                coverArtUrl = "https://c.saavncdn.com/366/Pushpa-2-The-Rule-Telugu-Telugu-2024-20241205211012-500x500.jpg",
                targetPlaylistId = "pl_pushpa2",
                targetQuery = "Pushpa 2 Pushpa Pushpa"
            )
            TimeOfDayBucket.NIGHT -> EditorialBanner(
                id = "ctx_night",
                eyebrow = "LATE NIGHT",
                title = "Devara & Midnight Lo-Fi",
                subtitle = "Soulful coastal melodies and calm late-night vibes",
                coverArtUrl = "https://c.saavncdn.com/313/Devara-Part-1-Telugu-Telugu-2024-20240926171010-500x500.jpg",
                targetPlaylistId = "pl_devara",
                targetQuery = "Chuttamalle Devara"
            )
        }
    }
}
