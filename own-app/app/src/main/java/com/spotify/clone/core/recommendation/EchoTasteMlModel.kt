package com.spotify.clone.core.recommendation

import android.content.Context
import android.content.SharedPreferences
import com.spotify.clone.core.model.MusicGenre
import com.spotify.clone.core.model.Track
import com.spotify.clone.core.player.SmartRadioQueue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Signal types emitted by user interactions to update the on-device ML taste models.
 */
enum class TasteSignalType(val weight: Float, val rewardLabel: Float) {
    PLAY_START(0.12f, 0.62f),
    COMPLETION(0.28f, 0.88f),
    LIKE(0.48f, 1.0f),
    REPLAY(0.32f, 0.92f),
    UNLIKE(-0.22f, 0.25f),
    EARLY_SKIP(-0.34f, 0.05f)
}

data class MlTrackPrediction(
    val track: Track,
    val enjoyProbability: Float,
    val cosineSimilarity: Float,
    val ucbExplorationBonus: Float,
    val coOccurrenceBoost: Float,
    val totalScore: Double,
    val matchPercent: Int,
    val reasonTag: String
)

data class TasteCluster(
    val id: String,
    val dominantGenre: MusicGenre,
    val centroid: FloatArray,
    val tracks: List<Track>
)

/**
 * Pure-Kotlin On-Device AI/ML Recommendation Engine for ECHO.
 *
 * Houses 6 cohesive ML models that run locally with zero cloud latency and persist
 * learned weights across sessions:
 * 1. AudioFeatureEmbedder: 12-dimensional L2-normalized latent audio/semantic embedding.
 * 2. UserTasteProfile: Exponentially time-decayed online Rocchio user taste vector.
 * 3. ContextualBanditRanker: Online SGD Logistic Regression + LinUCB exploration bandit.
 * 4. CoOccurrenceGraphModel: Markov item/artist transition & co-liked graph.
 * 5. TasteClusterer: Spherical K-Means in 12D cosine space for Daily Mixes.
 * 6. MmrDiversityRanker: Maximal Marginal Relevance (MMR) diversity re-ranker.
 */
object EchoTasteMlModel {

    const val EMBEDDING_DIM = 12
    private const val FEATURE_DIM = 8
    private const val PREFS_NAME = "echo_ml_taste_model_v1"
    private const val KEY_STATE_JSON = "ml_state_json"
    private const val DECAY_LAMBDA_PER_DAY = 0.085
    private const val MS_PER_DAY = 86_400_000.0
    private const val MAX_ARTIST_SCORES = 100
    private const val MAX_CO_GRAPH_SOURCES = 60
    private const val PERSIST_DEBOUNCE_MS = 500L

    private var prefs: SharedPreferences? = null
    private val persistScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var persistJob: Job? = null

    // 12D continuous user taste vector
    private val userTasteVector = FloatArray(EMBEDDING_DIM)

    // Exponentially decayed artist & genre affinity scores
    private val artistScores = HashMap<String, Float>()
    private val artistTimestamps = HashMap<String, Long>()
    private val genreScores = HashMap<String, Float>()
    private val genreTimestamps = HashMap<String, Long>()

    // UCB1 arm pull counts per genre
    private val genrePullCounts = HashMap<String, Int>()
    private var totalBanditPulls: Int = 0

    // Online SGD logistic regression weights & bias
    private val sgdWeights = floatArrayOf(
        2.35f, // 0: cosine similarity with user taste vector
        1.45f, // 1: decayed artist affinity
        1.15f, // 2: decayed genre affinity
        0.90f, // 3: time-of-day match
        0.85f, // 4: freshness / release score
        1.30f, // 5: liked-item or liked-artist affinity
        1.05f, // 6: co-occurrence graph boost
        0.45f  // 7: completion / quality prior
    )
    private var sgdBias: Float = -0.35f

    // Co-occurrence Markov transition graph (primaryArtist -> Map<neighborArtist, weight>)
    private val artistCoGraph = HashMap<String, HashMap<String, Float>>()
    private var lastTransitionTrack: Track? = null

    @Synchronized
    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = p
        loadState(p)
    }

    @Synchronized
    fun resetForTests() {
        persistJob?.cancel()
        persistJob = null
        userTasteVector.fill(0f)
        artistScores.clear()
        artistTimestamps.clear()
        genreScores.clear()
        genreTimestamps.clear()
        genrePullCounts.clear()
        totalBanditPulls = 0
        val defaultW = floatArrayOf(2.35f, 1.45f, 1.15f, 0.90f, 0.85f, 1.30f, 1.05f, 0.45f)
        defaultW.copyInto(sgdWeights)
        sgdBias = -0.35f
        artistCoGraph.clear()
        lastTransitionTrack = null
        prefs?.edit()?.clear()?.apply()
    }

    /**
     * Model 1: 12-Dimensional Latent Audio & Semantic Feature Embedder.
     *
     * Dimensions:
     * 0: Energy, 1: Valence/Positivity, 2: Acousticness, 3: Danceability,
     * 4: TempoIntensity, 5: RegionalTelugu, 6: RegionalHindi, 7: GlobalPop,
     * 8: HipHopElectronic, 9: VocalMelodic, 10: CinematicEpic, 11: Freshness
     */
    fun embedTrack(track: Track): FloatArray {
        val vec = FloatArray(EMBEDDING_DIM)
        val genre = MusicGenre.inferGenre(track)
        val text = "${track.title} ${track.artistName} ${track.albumName} ${track.genre}"
            .lowercase(Locale.ROOT)

        // Genre structural priors
        when (genre) {
            MusicGenre.TOL_TELUGU -> {
                vec[0] += 0.70f; vec[1] += 0.65f; vec[3] += 0.65f; vec[5] += 1.0f; vec[9] += 0.55f; vec[10] += 0.70f
            }
            MusicGenre.MELODIES -> {
                vec[0] += 0.32f; vec[1] += 0.62f; vec[2] += 0.78f; vec[5] += 0.45f; vec[6] += 0.35f; vec[9] += 0.98f
            }
            MusicGenre.MASS_BEATS -> {
                vec[0] += 0.98f; vec[1] += 0.80f; vec[3] += 0.96f; vec[4] += 0.92f; vec[5] += 0.85f; vec[10] += 0.50f
            }
            MusicGenre.GLOBAL_POP -> {
                vec[0] += 0.72f; vec[1] += 0.75f; vec[3] += 0.76f; vec[4] += 0.68f; vec[7] += 1.0f; vec[9] += 0.60f
            }
            MusicGenre.HIP_HOP -> {
                vec[0] += 0.82f; vec[3] += 0.84f; vec[4] += 0.78f; vec[7] += 0.35f; vec[8] += 0.98f
            }
            MusicGenre.ELECTRONIC -> {
                vec[0] += 0.92f; vec[1] += 0.65f; vec[3] += 0.94f; vec[4] += 0.95f; vec[8] += 1.0f
            }
            MusicGenre.ROCK -> {
                vec[0] += 0.88f; vec[4] += 0.80f; vec[7] += 0.55f; vec[10] += 0.60f
            }
            MusicGenre.BOLLYWOOD -> {
                vec[0] += 0.58f; vec[1] += 0.68f; vec[2] += 0.45f; vec[6] += 1.0f; vec[9] += 0.82f; vec[10] += 0.55f
            }
            MusicGenre.LO_FI -> {
                vec[0] += 0.18f; vec[1] += 0.45f; vec[2] += 0.92f; vec[4] += 0.22f; vec[9] += 0.50f
            }
            MusicGenre.INDIE -> {
                vec[0] += 0.42f; vec[1] += 0.58f; vec[2] += 0.86f; vec[9] += 0.78f
            }
            MusicGenre.DEVOTIONAL -> {
                vec[0] += 0.25f; vec[1] += 0.70f; vec[2] += 0.88f; vec[9] += 0.80f; vec[10] += 0.65f
            }
            MusicGenre.ALL -> {
                vec[0] += 0.55f; vec[1] += 0.55f; vec[7] += 0.55f
            }
        }

        // Semantic keyword adjustments
        if (containsAny(text, "remix", "dj", "club", "party", "dance", "anthem", "fear", "mass", "kissik", "naatu")) {
            vec[0] += 0.35f; vec[3] += 0.35f; vec[4] += 0.30f
        }
        if (containsAny(text, "acoustic", "unplugged", "chill", "lofi", "lo-fi", "midnight", "rain", "soft")) {
            vec[2] += 0.40f; vec[0] = (vec[0] - 0.15f).coerceAtLeast(0.05f)
        }
        if (containsAny(text, "theme", "kalki", "devara", "rrr", "pushpa", "baahubali", "symphony", "epic", "ost")) {
            vec[10] += 0.45f
        }
        if (containsAny(text, "2025", "2026", "new", "latest") || track.id.startsWith("apple_new_")) {
            vec[11] += 0.85f
        } else {
            vec[11] += 0.30f
        }
        if (track.isExplicit) {
            vec[8] += 0.28f
            vec[0] += 0.15f
        }
        // Duration prior: shorter (< 3m15s) tracks skew punchier; longer (> 4m15s) skew cinematic/melodic
        when {
            track.durationMs in 1L..195_000L -> vec[4] += 0.22f
            track.durationMs >= 255_000L -> {
                vec[9] += 0.22f
                vec[10] += 0.20f
            }
        }

        return l2Normalize(vec)
    }

    fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        var dot = 0f
        var normA = 0f
        var normB = 0f
        val n = minOf(a.size, b.size)
        for (i in 0 until n) {
            val ai = a[i]
            val bi = b[i]
            if (!ai.isFinite() || !bi.isFinite()) continue
            dot += ai * bi
            normA += ai * ai
            normB += bi * bi
        }
        if (normA <= 1e-6f || normB <= 1e-6f || !dot.isFinite() || !normA.isFinite() || !normB.isFinite()) {
            return 0f
        }
        return (dot / (sqrt(normA) * sqrt(normB))).coerceIn(-1f, 1f)
    }

    /**
     * Seeds the ML profile from onboarding selections or initial user likes/history.
     */
    @Synchronized
    fun seedFromPreferences(
        artists: Set<String>,
        genreIds: Set<String>,
        languages: Set<String>,
        catalogSeedTracks: List<Track>,
        selectedSongTracks: List<Track> = emptyList(),
        nowMs: Long = System.currentTimeMillis()
    ) {
        val artistLower = artists.mapTo(HashSet()) { it.lowercase(Locale.ROOT) }
        val matched = (selectedSongTracks + catalogSeedTracks.filter { track ->
            val ak = EchoRecommendationEngine.primaryArtistOf(track.artistName).lowercase(Locale.ROOT)
            val gId = MusicGenre.inferGenre(track).id
            ak in artistLower || gId in genreIds
        }).distinctBy { SmartRadioQueue.normalizedSongKey(it.title, it.artistName) }

        matched.take(16).forEach { track ->
            val vec = embedTrack(track)
            val weight = if (track in selectedSongTracks) 0.40f else 0.25f
            for (i in 0 until EMBEDDING_DIM) {
                userTasteVector[i] += vec[i] * weight
            }
        }
        val norm = l2Normalize(userTasteVector)
        norm.copyInto(userTasteVector)

        val allSeedArtists = LinkedHashSet<String>(artists)
        selectedSongTracks.forEach { track ->
            val primary = EchoRecommendationEngine.primaryArtistOf(track.artistName)
            if (primary.isNotBlank()) allSeedArtists.add(primary)
        }

        allSeedArtists.forEach { a ->
            val key = EchoRecommendationEngine.primaryArtistOf(a).lowercase(Locale.ROOT)
            if (key.isNotBlank()) {
                artistScores[key] = max(artistScores[key] ?: 0f, 3.5f)
                artistTimestamps[key] = nowMs
            }
        }
        trimArtistScoresIfNeeded()
        genreIds.forEach { gid ->
            if (gid.isNotBlank()) {
                genreScores[gid] = max(genreScores[gid] ?: 0f, 3.5f)
                genreTimestamps[gid] = nowMs
            }
        }
        languages.forEach { lang ->
            com.spotify.clone.core.data.WorldOnboardingCatalog.genresForLanguage(lang).forEach { gid ->
                genreScores[gid] = max(genreScores[gid] ?: 0f, 2.5f)
                genreTimestamps[gid] = nowMs
            }
        }
        if (selectedSongTracks.size >= 2) {
            syncLikedCoOccurrence(selectedSongTracks)
        }
        persistState()
    }

    /**
     * Model 2 + Model 3 + Model 4 online update:
     * Updates the 12D user taste vector, exponentially decayed artist/genre scores,
     * co-occurrence Markov graph, and SGD logistic ranker weights in real time.
     */
    @Synchronized
    fun recordInteraction(
        track: Track,
        signal: TasteSignalType,
        timeBucket: TimeOfDayBucket = EchoRecommendationEngine.getTimeOfDayBucket(),
        nowMs: Long = System.currentTimeMillis()
    ) {
        val trackVec = embedTrack(track)
        val artistKey = EchoRecommendationEngine.primaryArtistOf(track.artistName).lowercase(Locale.ROOT)
        val genre = MusicGenre.inferGenre(track)

        // 1. Update 12D user taste vector (Rocchio online step)
        for (i in 0 until EMBEDDING_DIM) {
            userTasteVector[i] = (userTasteVector[i] * 0.94f) + (trackVec[i] * signal.weight)
        }
        l2Normalize(userTasteVector).copyInto(userTasteVector)

        // 2. Exponentially decayed artist & genre scores
        if (artistKey.isNotBlank()) {
            val decayed = decayedScore(artistScores[artistKey] ?: 0f, artistTimestamps[artistKey], nowMs)
            val delta = when (signal) {
                TasteSignalType.LIKE -> 3.0f
                TasteSignalType.COMPLETION -> 1.8f
                TasteSignalType.REPLAY -> 2.0f
                TasteSignalType.PLAY_START -> 0.8f
                TasteSignalType.UNLIKE -> -1.5f
                TasteSignalType.EARLY_SKIP -> -1.4f
            }
            artistScores[artistKey] = (decayed + delta).coerceIn(0f, 25f)
            artistTimestamps[artistKey] = nowMs
            trimArtistScoresIfNeeded()
        }

        val decayedGenre = decayedScore(genreScores[genre.id] ?: 0f, genreTimestamps[genre.id], nowMs)
        val genreDelta = when (signal) {
            TasteSignalType.LIKE -> 2.6f
            TasteSignalType.COMPLETION -> 1.6f
            TasteSignalType.REPLAY -> 1.8f
            TasteSignalType.PLAY_START -> 0.7f
            TasteSignalType.UNLIKE -> -1.0f
            TasteSignalType.EARLY_SKIP -> -1.1f
        }
        genreScores[genre.id] = (decayedGenre + genreDelta).coerceIn(0f, 25f)
        genreTimestamps[genre.id] = nowMs

        // 3. Bandit arm pull tracking
        if (signal == TasteSignalType.PLAY_START || signal == TasteSignalType.COMPLETION || signal == TasteSignalType.LIKE) {
            genrePullCounts[genre.id] = (genrePullCounts[genre.id] ?: 0) + 1
            totalBanditPulls++
        }

        // 4. Online SGD weight update for Contextual Logistic Ranker
        val features = extractContextFeatures(
            track = track,
            trackVec = trackVec,
            likedArtistKeys = emptySet(),
            anchorTracks = emptyList(),
            timeBucket = timeBucket,
            nowMs = nowMs
        )
        updateSgdWeights(features, signal.rewardLabel)

        // 5. Update Markov Co-Occurrence Graph on positive sequential transitions
        val prev = lastTransitionTrack
        if (signal != TasteSignalType.EARLY_SKIP && signal != TasteSignalType.UNLIKE) {
            if (prev != null && prev.id != track.id) {
                val prevArtist = EchoRecommendationEngine.primaryArtistOf(prev.artistName).lowercase(Locale.ROOT)
                if (prevArtist.isNotBlank() && artistKey.isNotBlank() && prevArtist != artistKey) {
                    val edgeWeight = if (signal == TasteSignalType.COMPLETION || signal == TasteSignalType.LIKE) 1.4f else 0.7f
                    recordArtistEdge(prevArtist, artistKey, edgeWeight)
                    recordArtistEdge(artistKey, prevArtist, edgeWeight * 0.6f)
                }
            }
            lastTransitionTrack = track
        }

        persistState()
    }

    /**
     * Reinforces Co-Occurrence Graph from the user's current Liked Songs collection.
     */
    @Synchronized
    fun syncLikedCoOccurrence(likedTracks: List<Track>) {
        if (likedTracks.size < 2) return
        val artists = likedTracks.map {
            EchoRecommendationEngine.primaryArtistOf(it.artistName).lowercase(Locale.ROOT)
        }.filter { it.isNotBlank() }.distinct()
        for (i in artists.indices) {
            for (j in i + 1 until artists.size) {
                ensureArtistEdgeFloor(artists[i], artists[j], 0.5f)
                ensureArtistEdgeFloor(artists[j], artists[i], 0.5f)
            }
        }
    }

    /**
     * Returns exponentially decayed artist affinity normalized to [0, 1].
     */
    @Synchronized
    fun getDecayedArtistAffinity(artistName: String, nowMs: Long = System.currentTimeMillis()): Float {
        val key = EchoRecommendationEngine.primaryArtistOf(artistName).lowercase(Locale.ROOT)
        if (key.isBlank()) return 0f
        val raw = decayedScore(artistScores[key] ?: 0f, artistTimestamps[key], nowMs)
        return (raw / 10f).coerceIn(0f, 1f)
    }

    /**
     * Returns exponentially decayed genre affinity normalized to [0, 1].
     */
    @Synchronized
    fun getDecayedGenreAffinity(genre: MusicGenre, nowMs: Long = System.currentTimeMillis()): Float {
        val raw = decayedScore(genreScores[genre.id] ?: 0f, genreTimestamps[genre.id], nowMs)
        return (raw / 10f).coerceIn(0f, 1f)
    }

    /**
     * Returns top genres ranked dynamically by decayed ML preference + onboarding priors.
     */
    @Synchronized
    fun getRankedPreferredGenres(
        favorites: List<Track>,
        recentlyPlayed: List<Track>,
        nowMs: Long = System.currentTimeMillis()
    ): List<MusicGenre> {
        val historyBonus = HashMap<MusicGenre, Float>()
        favorites.forEach { t ->
            val g = MusicGenre.inferGenre(t)
            historyBonus[g] = (historyBonus[g] ?: 0f) + 2.0f
        }
        recentlyPlayed.forEachIndexed { idx, t ->
            val g = MusicGenre.inferGenre(t)
            val recencyWeight = 1.5f / (1f + idx * 0.15f)
            historyBonus[g] = (historyBonus[g] ?: 0f) + recencyWeight
        }

        return MusicGenre.selectableGenres.sortedByDescending { genre ->
            val mlScore = decayedScore(genreScores[genre.id] ?: 0f, genreTimestamps[genre.id], nowMs)
            val hist = historyBonus[genre] ?: 0f
            val onboard = if (genre in EchoRecommendationEngine.getOnboardedGenres()) 2.2f else 0f
            mlScore + hist + onboard
        }
    }

    /**
     * Model 3 + Model 4 + Model 6:
     * Scores and ranks candidate tracks with Online SGD Logistic Ranker, LinUCB exploration,
     * Co-Occurrence Graph boost, and MMR diversity re-ranking.
     */
    @Synchronized
    fun rankCandidatesWithMl(
        candidates: List<Track>,
        favorites: List<Track>,
        recentlyPlayed: List<Track>,
        timeBucket: TimeOfDayBucket = EchoRecommendationEngine.getTimeOfDayBucket(),
        limit: Int = 12,
        lambdaMmr: Float = 0.74f,
        explorationAlpha: Float = 0.28f,
        dailySeed: Long = 0L,
        nowMs: Long = System.currentTimeMillis()
    ): List<MlTrackPrediction> {
        val cleanPool = candidates
            .filter { !EchoRecommendationEngine.isJunkTitle(it.title) }
            .distinctBy { SmartRadioQueue.normalizedSongKey(it.title, it.artistName) }
        if (cleanPool.isEmpty()) return emptyList()

        syncLikedCoOccurrence(favorites)
        val anchorTracks = (favorites.take(8) + recentlyPlayed.take(6)).distinctBy { it.id }
        val likedArtistKeys = favorites.map {
            EchoRecommendationEngine.primaryArtistOf(it.artistName).lowercase(Locale.ROOT)
        }.toSet()
        val recentKeys = recentlyPlayed.take(4)
            .map { SmartRadioQueue.normalizedSongKey(it.title, it.artistName) }
            .toSet()
        val hasUserVector = userTasteVector.any { it != 0f }

        val embeddings = HashMap<String, FloatArray>(cleanPool.size)
        val anchorEmbeddings = HashMap<String, FloatArray>(anchorTracks.size)
        for (anchor in anchorTracks) {
            anchorEmbeddings[anchor.id] = embeddings.getOrPut(anchor.id) { embedTrack(anchor) }
        }
        val rawPredictions = cleanPool.map { track ->
            val trackVec = embeddings.getOrPut(track.id) { embedTrack(track) }

            val sim = if (hasUserVector) {
                ((cosineSimilarity(userTasteVector, trackVec) + 1f) * 0.5f).coerceIn(0f, 1f)
            } else {
                0.55f
            }

            val features = extractContextFeatures(
                track = track,
                trackVec = trackVec,
                likedArtistKeys = likedArtistKeys,
                anchorTracks = anchorTracks,
                anchorEmbeddings = anchorEmbeddings,
                timeBucket = timeBucket,
                nowMs = nowMs
            )
            val prob = predictProbability(features)
            val genre = MusicGenre.inferGenre(track)
            val genrePulls = genrePullCounts[genre.id] ?: 0
            val ucb = ucbBonus(totalBanditPulls, genrePulls, explorationAlpha)
            val coBoost = features[6]
            val songKey = SmartRadioQueue.normalizedSongKey(track.title, track.artistName)
            val freshPenalty = if (songKey in recentKeys) -0.18 else 0.0

            // Subtle deterministic daily jitter so daily home rotates thoughtfully without breaking top matches
            val dailyJitter = if (dailySeed != 0L) {
                val h = ((track.id.hashCode().toLong() * 6364136223846793005L) xor dailySeed)
                (((h ushr 16) and 0xFFL).toDouble() / 255.0 - 0.5) * 0.06
            } else 0.0

            val compositeScore = (prob * 1.0) + (ucb * 0.35) + (sim * 0.25) + freshPenalty + dailyJitter
            val matchPct = ((prob * 0.65f + sim * 0.35f) * 100f).toInt().coerceIn(72, 99)

            val reason = buildExplainableReason(
                track = track,
                favorites = favorites,
                genre = genre,
                sim = sim,
                coBoost = coBoost,
                timeBucket = timeBucket,
                ucb = ucb
            )

            MlTrackPrediction(
                track = track,
                enjoyProbability = prob,
                cosineSimilarity = sim,
                ucbExplorationBonus = ucb,
                coOccurrenceBoost = coBoost,
                totalScore = compositeScore,
                matchPercent = matchPct,
                reasonTag = reason
            )
        }.sortedByDescending { it.totalScore }

        return mmrRerank(rawPredictions, embeddings, limit = limit, lambda = lambdaMmr)
    }

    /**
     * Model 5: Spherical K-Means Clustering in 12D Cosine Embedding Space.
     * Groups tracks into cohesive taste clusters for automated Daily Mixes.
     */
    fun clusterTracksForDailyMixes(
        pool: List<Track>,
        favorites: List<Track>,
        recentlyPlayed: List<Track>,
        k: Int = 3,
        maxIterations: Int = 6
    ): List<TasteCluster> {
        if (k <= 0) return emptyList()
        val cleanPool = pool
            .filter { !EchoRecommendationEngine.isJunkTitle(it.title) }
            .distinctBy { SmartRadioQueue.normalizedSongKey(it.title, it.artistName) }
        if (cleanPool.size < 3) return emptyList()

        val actualK = minOf(k, cleanPool.size / 3).coerceAtLeast(1)
        val embeddings = cleanPool.associate { it.id to embedTrack(it) }

        // Seed centroids from top preferred genres / liked songs / diverse catalog anchors
        val rankedGenres = getRankedPreferredGenres(favorites, recentlyPlayed)
        val centroids = ArrayList<FloatArray>(actualK)
        for (genre in rankedGenres) {
            if (centroids.size >= actualK) break
            val anchor = cleanPool.firstOrNull { MusicGenre.inferGenre(it) == genre } ?: continue
            val vec = embeddings[anchor.id] ?: continue
            if (centroids.none { cosineSimilarity(it, vec) > 0.92f }) {
                centroids.add(vec.copyOf())
            }
        }
        for (track in cleanPool) {
            if (centroids.size >= actualK) break
            val vec = embeddings[track.id] ?: continue
            if (centroids.none { cosineSimilarity(it, vec) > 0.88f }) {
                centroids.add(vec.copyOf())
            }
        }
        while (centroids.size < actualK && cleanPool.size > centroids.size) {
            centroids.add((embeddings[cleanPool[centroids.size].id] ?: FloatArray(EMBEDDING_DIM)).copyOf())
        }

        val assignments = IntArray(cleanPool.size)
        repeat(maxIterations) {
            // Assignment step
            for (idx in cleanPool.indices) {
                val vec = embeddings[cleanPool[idx].id] ?: continue
                var bestC = 0
                var bestSim = Float.NEGATIVE_INFINITY
                for (c in centroids.indices) {
                    val sim = cosineSimilarity(vec, centroids[c])
                    if (sim > bestSim) {
                        bestSim = sim
                        bestC = c
                    }
                }
                assignments[idx] = bestC
            }
            // Update centroids
            for (c in centroids.indices) {
                val sum = FloatArray(EMBEDDING_DIM)
                var count = 0
                for (idx in cleanPool.indices) {
                    if (assignments[idx] == c) {
                        val vec = embeddings[cleanPool[idx].id] ?: continue
                        for (d in 0 until EMBEDDING_DIM) sum[d] += vec[d]
                        count++
                    }
                }
                if (count > 0) {
                    centroids[c] = l2Normalize(sum)
                }
            }
        }

        return centroids.indices.mapNotNull { c ->
            val members = cleanPool.indices
                .filter { assignments[it] == c }
                .map { cleanPool[it] }
                .sortedByDescending { t -> cosineSimilarity(embeddings[t.id] ?: FloatArray(EMBEDDING_DIM), centroids[c]) }
            if (members.size < 3) return@mapNotNull null

            val dominantGenre = members.groupingBy { MusicGenre.inferGenre(it) }
                .eachCount()
                .maxByOrNull { it.value }
                ?.key ?: MusicGenre.GLOBAL_POP

            TasteCluster(
                id = "cluster_${c + 1}_${dominantGenre.id}",
                dominantGenre = dominantGenre,
                centroid = centroids[c],
                tracks = EchoRecommendationEngine.spotifyArtistInterleave(members.take(15))
            )
        }
    }

    /**
     * Model 6: Maximal Marginal Relevance (MMR) Diversity Re-Ranker.
     */
    fun mmrRerank(
        predictions: List<MlTrackPrediction>,
        embeddings: Map<String, FloatArray>,
        limit: Int,
        lambda: Float = 0.74f
    ): List<MlTrackPrediction> {
        if (limit <= 0) return emptyList()
        if (predictions.size <= 1) return predictions
        val remaining = predictions.toMutableList()
        val selected = ArrayList<MlTrackPrediction>(limit)
        val artistCounts = HashMap<String, Int>()

        while (selected.size < limit && remaining.isNotEmpty()) {
            var bestIdx = 0
            var bestMmr = Double.NEGATIVE_INFINITY

            for (i in remaining.indices) {
                val cand = remaining[i]
                val ak = EchoRecommendationEngine.primaryArtistOf(cand.track.artistName).lowercase(Locale.ROOT)
                // Soft cap on same artist in top slots
                val artistCount = artistCounts[ak] ?: 0
                val artistPenalty = if (artistCount >= 2) 0.35 else artistCount * 0.08

                val candVec = embeddings[cand.track.id]
                val maxSimToSelected = if (selected.isEmpty() || candVec == null) {
                    0.0
                } else {
                    selected.maxOf { sel ->
                        val selVec = embeddings[sel.track.id]
                        if (selVec != null) cosineSimilarity(candVec, selVec).toDouble() else 0.0
                    }
                }

                val mmr = (lambda * cand.totalScore) - ((1.0 - lambda) * maxSimToSelected) - artistPenalty
                if (mmr > bestMmr) {
                    bestMmr = mmr
                    bestIdx = i
                }
            }

            val chosen = remaining.removeAt(bestIdx)
            val ak = EchoRecommendationEngine.primaryArtistOf(chosen.track.artistName).lowercase(Locale.ROOT)
            artistCounts[ak] = (artistCounts[ak] ?: 0) + 1
            selected.add(chosen)
        }
        return selected
    }

    /**
     * Retrieves co-occurrence score between candidate track and anchor tracks (e.g., Liked Songs).
     */
    @Synchronized
    fun getCoOccurrenceBoost(candidate: Track, anchorTracks: List<Track>): Float {
        if (anchorTracks.isEmpty()) return 0f
        return getCoOccurrenceBoostInternal(
            candidate = candidate,
            candVec = embedTrack(candidate),
            anchorTracks = anchorTracks,
            anchorEmbeddings = null
        )
    }

    private fun getCoOccurrenceBoostInternal(
        candidate: Track,
        candVec: FloatArray,
        anchorTracks: List<Track>,
        anchorEmbeddings: Map<String, FloatArray>?
    ): Float {
        if (anchorTracks.isEmpty()) return 0f
        val candArtist = EchoRecommendationEngine.primaryArtistOf(candidate.artistName).lowercase(Locale.ROOT)
        var maxEdge = 0f
        var maxCos = 0f

        for (anchor in anchorTracks) {
            val anchorArtist = EchoRecommendationEngine.primaryArtistOf(anchor.artistName).lowercase(Locale.ROOT)
            val graphScore = artistCoGraph[anchorArtist]?.get(candArtist) ?: 0f
            if (graphScore > maxEdge) maxEdge = graphScore
            if (anchorArtist == candArtist && anchor.id != candidate.id) {
                maxEdge = max(maxEdge, 1.8f)
            }
            val anchorVec = anchorEmbeddings?.get(anchor.id) ?: embedTrack(anchor)
            val cos = cosineSimilarity(candVec, anchorVec)
            if (cos > maxCos) maxCos = cos
        }
        val normalizedGraph = (maxEdge / 4.0f).coerceIn(0f, 1f)
        return (normalizedGraph * 0.65f + maxCos * 0.35f).coerceIn(0f, 1f)
    }

    fun ucbBonus(totalPulls: Int, armPulls: Int, alpha: Float = 0.28f): Float {
        val n = (totalPulls + 1).toDouble()
        val k = (armPulls + 1).toDouble()
        return (alpha * sqrt(ln(n + 1.0) / k)).toFloat().coerceIn(0f, 1f)
    }

    private fun extractContextFeatures(
        track: Track,
        trackVec: FloatArray,
        likedArtistKeys: Set<String>,
        anchorTracks: List<Track>,
        anchorEmbeddings: Map<String, FloatArray>? = null,
        timeBucket: TimeOfDayBucket,
        nowMs: Long
    ): FloatArray {
        val hasTaste = userTasteVector.any { it != 0f }
        val cosSim = if (hasTaste) {
            ((cosineSimilarity(userTasteVector, trackVec) + 1f) * 0.5f).coerceIn(0f, 1f)
        } else 0.55f

        val artistKey = EchoRecommendationEngine.primaryArtistOf(track.artistName).lowercase(Locale.ROOT)
        val artistAff = getDecayedArtistAffinity(track.artistName, nowMs)
        val genre = MusicGenre.inferGenre(track)
        val genreAff = getDecayedGenreAffinity(genre, nowMs)
        val timeMatch = if (matchesTimeBucket(genre, timeBucket)) 1f else 0.25f
        val freshness = trackVec[11].coerceIn(0f, 1f)
        val likedAff = if (track.isFavorite || artistKey in likedArtistKeys) 1f else 0f
        val coBoost = getCoOccurrenceBoostInternal(track, trackVec, anchorTracks, anchorEmbeddings)
        val qualityPrior = if (!com.spotify.clone.core.data.FullTrackStreamResolver.needsResolution(track)) 1f else 0.45f

        return floatArrayOf(cosSim, artistAff, genreAff, timeMatch, freshness, likedAff, coBoost, qualityPrior)
    }

    private fun predictProbability(features: FloatArray): Float {
        var z = sgdBias
        for (i in 0 until FEATURE_DIM) {
            z += sgdWeights[i] * features[i]
        }
        return (1.0f / (1.0f + exp(-z.coerceIn(-12f, 12f))))
    }

    private fun updateSgdWeights(features: FloatArray, targetReward: Float, lr: Float = 0.06f) {
        val pred = predictProbability(features)
        val error = targetReward - pred
        val l2 = 0.001f
        for (i in 0 until FEATURE_DIM) {
            sgdWeights[i] = (sgdWeights[i] + lr * (error * features[i] - l2 * sgdWeights[i]))
                .coerceIn(-4f, 5f)
        }
        sgdBias = (sgdBias + lr * error).coerceIn(-3f, 3f)
    }

    private fun trimArtistScoresIfNeeded() {
        while (artistScores.size > MAX_ARTIST_SCORES) {
            val weakest = artistScores.minByOrNull { it.value }?.key ?: break
            artistScores.remove(weakest)
            artistTimestamps.remove(weakest)
        }
    }

    private fun trimCoGraphIfNeeded() {
        while (artistCoGraph.size > MAX_CO_GRAPH_SOURCES) {
            val weakest = artistCoGraph.minByOrNull { entry -> entry.value.values.sum() }?.key ?: break
            artistCoGraph.remove(weakest)
        }
    }

    private fun ensureArtistEdgeFloor(fromArtist: String, toArtist: String, minWeight: Float) {
        val neighbors = artistCoGraph.getOrPut(fromArtist) { HashMap() }
        val current = neighbors[toArtist] ?: 0f
        if (current < minWeight) {
            neighbors[toArtist] = minWeight.coerceIn(0f, 15f)
            if (neighbors.size > 24) {
                val minKey = neighbors.minByOrNull { it.value }?.key
                if (minKey != null) neighbors.remove(minKey)
            }
        }
        trimCoGraphIfNeeded()
    }

    private fun recordArtistEdge(fromArtist: String, toArtist: String, delta: Float) {
        val neighbors = artistCoGraph.getOrPut(fromArtist) { HashMap() }
        neighbors[toArtist] = ((neighbors[toArtist] ?: 0f) + delta).coerceIn(0f, 15f)
        if (neighbors.size > 24) {
            val minKey = neighbors.minByOrNull { it.value }?.key
            if (minKey != null) neighbors.remove(minKey)
        }
        trimCoGraphIfNeeded()
    }

    private fun buildExplainableReason(
        track: Track,
        favorites: List<Track>,
        genre: MusicGenre,
        sim: Float,
        coBoost: Float,
        timeBucket: TimeOfDayBucket,
        ucb: Float
    ): String {
        val trackArtist = EchoRecommendationEngine.primaryArtistOf(track.artistName).lowercase(Locale.ROOT)
        val likedMatch = favorites.firstOrNull {
            EchoRecommendationEngine.primaryArtistOf(it.artistName).lowercase(Locale.ROOT) == trackArtist && it.id != track.id
        }
        return when {
            likedMatch != null -> "Because you liked ${likedMatch.title}"
            coBoost > 0.62f && favorites.isNotEmpty() -> "Matches your liked vibes"
            track.id.startsWith("apple_new_") -> "Fresh release • ${genre.shortLabel}"
            sim >= 0.80f -> "${(sim * 100).toInt().coerceIn(80, 99)}% Taste Match"
            ucb >= 0.20f -> "Discover • ${genre.shortLabel}"
            matchesTimeBucket(genre, timeBucket) -> "${timeBucket.label} • ${genre.shortLabel}"
            else -> genre.shortLabel
        }
    }

    private fun matchesTimeBucket(genre: MusicGenre, bucket: TimeOfDayBucket): Boolean = when (bucket) {
        TimeOfDayBucket.MORNING -> genre == MusicGenre.MELODIES || genre == MusicGenre.DEVOTIONAL || genre == MusicGenre.INDIE
        TimeOfDayBucket.AFTERNOON -> genre == MusicGenre.GLOBAL_POP || genre == MusicGenre.INDIE || genre == MusicGenre.BOLLYWOOD || genre == MusicGenre.TOL_TELUGU
        TimeOfDayBucket.EVENING -> genre == MusicGenre.MASS_BEATS || genre == MusicGenre.HIP_HOP || genre == MusicGenre.ELECTRONIC || genre == MusicGenre.TOL_TELUGU
        TimeOfDayBucket.NIGHT -> genre == MusicGenre.LO_FI || genre == MusicGenre.MELODIES || genre == MusicGenre.INDIE
    }

    private fun decayedScore(score: Float, lastMs: Long?, nowMs: Long): Float {
        if (score <= 0f || lastMs == null || lastMs <= 0L) return score
        val deltaDays = ((nowMs - lastMs).coerceAtLeast(0L) / MS_PER_DAY)
        val factor = exp(-DECAY_LAMBDA_PER_DAY * deltaDays).toFloat()
        return score * factor
    }

    private fun l2Normalize(vec: FloatArray): FloatArray {
        val sanitized = FloatArray(vec.size)
        var sumSq = 0f
        for (i in vec.indices) {
            val v = if (vec[i].isFinite()) vec[i] else 0f
            sanitized[i] = v
            sumSq += v * v
        }
        if (sumSq <= 1e-6f || !sumSq.isFinite()) return sanitized
        val inv = 1f / sqrt(sumSq)
        val out = FloatArray(vec.size)
        for (i in vec.indices) out[i] = sanitized[i] * inv
        return out
    }

    private fun containsAny(haystack: String, vararg terms: String): Boolean =
        terms.any { haystack.contains(it) }

    private fun persistState() {
        val p = prefs ?: return
        // Snapshot immutable state synchronously while under caller's lock, serialize & commit off Main thread.
        val tasteCopy = userTasteVector.copyOf()
        val weightsCopy = sgdWeights.copyOf()
        val biasCopy = sgdBias
        val pullsCopy = totalBanditPulls
        val artistsSnapshot = artistScores.entries.sortedByDescending { it.value }.take(40)
            .map { Triple(it.key, it.value, artistTimestamps[it.key] ?: 0L) }
        val genresSnapshot = genreScores.entries.map { (k, v) ->
            Triple(k, v, Pair(genreTimestamps[k] ?: 0L, genrePullCounts[k] ?: 0))
        }
        val coSnapshot = artistCoGraph.entries.take(30).map { (src, map) ->
            src to map.toMap()
        }

        persistJob?.cancel()
        persistJob = persistScope.launch {
            delay(PERSIST_DEBOUNCE_MS)
            runCatching {
                val root = JSONObject()
                root.put("tasteVec", JSONArray(tasteCopy.map { it.toDouble() }))
                root.put("weights", JSONArray(weightsCopy.map { it.toDouble() }))
                root.put("bias", biasCopy.toDouble())
                root.put("totalPulls", pullsCopy)

                val artistObj = JSONObject()
                artistsSnapshot.forEach { (k, s, t) ->
                    artistObj.put(k, JSONObject().put("s", s.toDouble()).put("t", t))
                }
                root.put("artists", artistObj)

                val genreObj = JSONObject()
                genresSnapshot.forEach { (k, v, meta) ->
                    genreObj.put(
                        k,
                        JSONObject()
                            .put("s", v.toDouble())
                            .put("t", meta.first)
                            .put("p", meta.second)
                    )
                }
                root.put("genres", genreObj)

                val coObj = JSONObject()
                coSnapshot.forEach { (src, map) ->
                    val inner = JSONObject()
                    map.forEach { (dst, w) -> inner.put(dst, w.toDouble()) }
                    coObj.put(src, inner)
                }
                root.put("coGraph", coObj)

                p.edit().putString(KEY_STATE_JSON, root.toString()).apply()
            }
        }
    }

    private fun loadState(p: SharedPreferences) {
        val raw = p.getString(KEY_STATE_JSON, null) ?: return
        runCatching {
            val root = JSONObject(raw)
            root.optJSONArray("tasteVec")?.let { arr ->
                for (i in 0 until minOf(EMBEDDING_DIM, arr.length())) {
                    userTasteVector[i] = arr.optDouble(i, 0.0).toFloat()
                }
            }
            root.optJSONArray("weights")?.let { arr ->
                for (i in 0 until minOf(FEATURE_DIM, arr.length())) {
                    sgdWeights[i] = arr.optDouble(i, sgdWeights[i].toDouble()).toFloat()
                }
            }
            sgdBias = root.optDouble("bias", sgdBias.toDouble()).toFloat()
            totalBanditPulls = root.optInt("totalPulls", 0)

            root.optJSONObject("artists")?.let { obj ->
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val item = obj.optJSONObject(k) ?: continue
                    artistScores[k] = item.optDouble("s", 0.0).toFloat()
                    artistTimestamps[k] = item.optLong("t", 0L)
                }
            }
            root.optJSONObject("genres")?.let { obj ->
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val item = obj.optJSONObject(k) ?: continue
                    genreScores[k] = item.optDouble("s", 0.0).toFloat()
                    genreTimestamps[k] = item.optLong("t", 0L)
                    genrePullCounts[k] = item.optInt("p", 0)
                }
            }
            root.optJSONObject("coGraph")?.let { obj ->
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val src = keys.next()
                    val inner = obj.optJSONObject(src) ?: continue
                    val map = HashMap<String, Float>()
                    val dKeys = inner.keys()
                    while (dKeys.hasNext()) {
                        val dst = dKeys.next()
                        map[dst] = inner.optDouble(dst, 0.0).toFloat()
                    }
                    artistCoGraph[src] = map
                }
            }
        }
    }
}
