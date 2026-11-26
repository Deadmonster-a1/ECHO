package com.spotify.clone

import com.spotify.clone.core.data.SearchRankingEngine
import com.spotify.clone.core.lyrics.IndicRomanizer
import com.spotify.clone.core.lyrics.LyricsSyncEngine
import com.spotify.clone.core.model.LyricLine
import com.spotify.clone.core.model.LyricsResult
import com.spotify.clone.core.model.LyricsSyncType
import com.spotify.clone.core.model.MusicGenre
import com.spotify.clone.core.model.Track
import com.spotify.clone.core.player.CrossfadeCurve
import com.spotify.clone.core.player.SmartRadioQueue
import com.spotify.clone.core.recommendation.DailyFeedCoordinator
import com.spotify.clone.core.recommendation.EchoRecommendationEngine
import com.spotify.clone.core.recommendation.EchoTasteMlModel
import com.spotify.clone.core.recommendation.SongUpgradeEngine
import com.spotify.clone.core.recommendation.TasteSignalType
import com.spotify.clone.core.recommendation.TimeOfDayBucket
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random

class MlAndAlgorithmsStressUnitTest {

    @Before
    fun resetSingletons() {
        EchoTasteMlModel.resetForTests()
        EchoRecommendationEngine.clearTasteSignals()
        DailyFeedCoordinator.resetForTests()
        SongUpgradeEngine.clearCacheForTests()
    }

    private fun track(
        id: String,
        title: String = "Track $id",
        artist: String = "Artist $id",
        album: String = "Album $id",
        genre: String = "Melodies",
        durationMs: Long = 200_000L,
        isExplicit: Boolean = false,
        isFavorite: Boolean = false,
        coverArtUrl: String = "https://example.com/$id-150x150.jpg",
        mediaUri: String = "https://example.com/$id.mp3"
    ) = Track(
        id = id,
        title = title,
        artistName = artist,
        albumName = album,
        coverArtUrl = coverArtUrl,
        durationMs = durationMs,
        mediaUri = mediaUri,
        isExplicit = isExplicit,
        isFavorite = isFavorite,
        genre = genre
    )

    // -------------------------------------------------------------------------
    // Model 1: Feature Embeddings & Cosine Similarity
    // -------------------------------------------------------------------------

    @Test
    fun `embedTrack produces 12D unit vector and cosineSimilarity obeys metric bounds`() {
        val melody = track("m1", title = "Acoustic Soul Song", genre = "Melodies", durationMs = 240_000L)
        val hipHop = track("h1", title = "Fast Rap Beat", genre = "Hip-Hop", durationMs = 150_000L, isExplicit = true)

        val vecM = EchoTasteMlModel.embedTrack(melody)
        val vecH = EchoTasteMlModel.embedTrack(hipHop)

        assertEquals(EchoTasteMlModel.EMBEDDING_DIM, vecM.size)
        assertEquals(EchoTasteMlModel.EMBEDDING_DIM, vecH.size)

        val normM = sqrt(vecM.sumOf { (it * it).toDouble() }).toFloat()
        val normH = sqrt(vecH.sumOf { (it * it).toDouble() }).toFloat()
        assertEquals(1.0f, normM, 0.01f)
        assertEquals(1.0f, normH, 0.01f)

        assertEquals(1.0f, EchoTasteMlModel.cosineSimilarity(vecM, vecM), 0.001f)
        val crossSim = EchoTasteMlModel.cosineSimilarity(vecM, vecH)
        assertTrue("Distinct genres should have lower cross similarity ($crossSim)", crossSim < 0.9f)
        assertTrue(crossSim in -1.0f..1.0f)

        val zero = FloatArray(EchoTasteMlModel.EMBEDDING_DIM)
        assertEquals(0f, EchoTasteMlModel.cosineSimilarity(zero, vecM), 0.0001f)
        assertEquals(0f, EchoTasteMlModel.cosineSimilarity(zero, zero), 0.0001f)
    }

    @Test
    fun `cosineSimilarity handles NaN or infinite embeddings safely`() {
        val valid = EchoTasteMlModel.embedTrack(track("v1"))
        val bad = FloatArray(EchoTasteMlModel.EMBEDDING_DIM) { Float.NaN }
        val sim = EchoTasteMlModel.cosineSimilarity(valid, bad)
        assertFalse("Cosine similarity must never propagate NaN", sim.isNaN())
    }

    // -------------------------------------------------------------------------
    // Model 2: Temporal Exponential Decay & Interaction Updates
    // -------------------------------------------------------------------------

    @Test
    fun `positive and negative taste signals update decayed affinities monotonically`() {
        val now = 1_750_000_000_000L
        val artist = "Sid Sriram"
        val likedTrack = track("t_like", title = "Samajavaragamana", artist = artist, genre = "Melodies")

        repeat(3) {
            EchoTasteMlModel.recordInteraction(
                track = likedTrack,
                signal = TasteSignalType.LIKE,
                timeBucket = TimeOfDayBucket.EVENING,
                nowMs = now
            )
            EchoTasteMlModel.recordInteraction(
                track = likedTrack,
                signal = TasteSignalType.COMPLETION,
                timeBucket = TimeOfDayBucket.EVENING,
                nowMs = now
            )
        }

        val immediateArtistAffinity = EchoTasteMlModel.getDecayedArtistAffinity(artist, now)
        val immediateGenreAffinity = EchoTasteMlModel.getDecayedGenreAffinity(MusicGenre.MELODIES, now)
        assertTrue("Immediate artist affinity should be positive ($immediateArtistAffinity)", immediateArtistAffinity > 0.1f)
        assertTrue("Immediate artist affinity must be <= 1f", immediateArtistAffinity <= 1.0f)
        assertTrue("Immediate genre affinity should be positive ($immediateGenreAffinity)", immediateGenreAffinity > 0.1f)

        val sevenDaysLater = now + 7L * 24L * 3600L * 1000L
        val thirtyDaysLater = now + 30L * 24L * 3600L * 1000L
        val weekAffinity = EchoTasteMlModel.getDecayedArtistAffinity(artist, sevenDaysLater)
        val monthAffinity = EchoTasteMlModel.getDecayedArtistAffinity(artist, thirtyDaysLater)

        assertTrue("Affinity should decay after 7 days ($weekAffinity < $immediateArtistAffinity)", weekAffinity < immediateArtistAffinity)
        assertTrue("Affinity should decay further after 30 days ($monthAffinity <= $weekAffinity)", monthAffinity <= weekAffinity)
        assertTrue("Decayed affinity stays >= 0f", monthAffinity >= 0f)

        // Repeated early skips & unlike should reduce affinity cleanly without NaN or < 0
        repeat(6) {
            EchoTasteMlModel.recordInteraction(
                track = likedTrack,
                signal = TasteSignalType.EARLY_SKIP,
                timeBucket = TimeOfDayBucket.EVENING,
                nowMs = now + 1000L
            )
        }
        val afterSkips = EchoTasteMlModel.getDecayedArtistAffinity(artist, now + 1000L)
        assertTrue("Artist affinity drops after skips ($afterSkips < $immediateArtistAffinity)", afterSkips < immediateArtistAffinity)
        assertTrue("Affinity stays bounded >= 0f", afterSkips >= 0f)
    }

    @Test
    fun `seedFromPreferences works cleanly with matching catalog and empty catalog`() {
        val now = 1_750_000_000_000L
        EchoTasteMlModel.seedFromPreferences(
            artists = setOf("A.R. Rahman"),
            genreIds = setOf(MusicGenre.MELODIES.id, MusicGenre.TOL_TELUGU.id),
            languages = setOf("Telugu"),
            catalogSeedTracks = emptyList(),
            nowMs = now
        )
        assertTrue(EchoTasteMlModel.getDecayedArtistAffinity("A.R. Rahman", now) > 0f)
        assertTrue(EchoTasteMlModel.getDecayedGenreAffinity(MusicGenre.MELODIES, now) > 0f)
    }

    // -------------------------------------------------------------------------
    // Model 3: SGD + LinUCB Exploration & Ranker Edge Cases
    // -------------------------------------------------------------------------

    @Test
    fun `ucbBonus decays with more arm pulls and guards non-positive inputs`() {
        val unseen = EchoTasteMlModel.ucbBonus(totalPulls = 100, armPulls = 0, alpha = 0.2f)
        val lowPulls = EchoTasteMlModel.ucbBonus(totalPulls = 100, armPulls = 2, alpha = 0.2f)
        val highPulls = EchoTasteMlModel.ucbBonus(totalPulls = 100, armPulls = 50, alpha = 0.2f)
        val coldStart = EchoTasteMlModel.ucbBonus(totalPulls = 0, armPulls = 0, alpha = 0.2f)
        val zeroAlpha = EchoTasteMlModel.ucbBonus(totalPulls = 100, armPulls = 0, alpha = 0f)

        assertTrue(unseen >= lowPulls)
        assertTrue(lowPulls > highPulls)
        assertTrue(highPulls >= 0f)
        assertTrue(coldStart.isFinite() && coldStart >= 0f)
        assertEquals(0f, zeroAlpha, 0.001f)
    }

    @Test
    fun `rankCandidatesWithMl handles empty limit zero negative limit and junk filtering`() {
        val now = 1_750_000_000_000L
        val clean = (1..8).map {
            track(
                id = "c$it",
                title = "Pure Song $it",
                artist = "Artist ${(it % 3) + 1}",
                genre = if (it % 2 == 0) "Melodies" else "Global Pop"
            )
        }
        val withJunk = clean + listOf(
            track("j1", title = "Chill Lofi Karaoke Version"),
            track("j2", title = "Top Hit - Ringtone 2024"),
            track("j3", title = "8D Audio Slowed + Reverb Mashup")
        )

        val emptyRes = EchoTasteMlModel.rankCandidatesWithMl(emptyList(), emptyList(), emptyList(), TimeOfDayBucket.MORNING, 10)
        assertTrue(emptyRes.isEmpty())

        val zeroLimit = EchoTasteMlModel.rankCandidatesWithMl(withJunk, emptyList(), emptyList(), TimeOfDayBucket.MORNING, 0)
        assertTrue(zeroLimit.isEmpty())

        val negLimit = EchoTasteMlModel.rankCandidatesWithMl(withJunk, emptyList(), emptyList(), TimeOfDayBucket.MORNING, -5)
        assertTrue(negLimit.isEmpty())

        val ranked = EchoTasteMlModel.rankCandidatesWithMl(
            candidates = withJunk,
            favorites = listOf(clean.first()),
            recentlyPlayed = listOf(clean[1]),
            timeBucket = TimeOfDayBucket.EVENING,
            limit = 6,
            dailySeed = 42L,
            nowMs = now
        )
        assertTrue(ranked.size <= 6)
        assertFalse(ranked.any { it.track.id.startsWith("j") })
        assertTrue(ranked.all { it.track.id.startsWith("c") && it.reasonTag.isNotBlank() })
    }

    // -------------------------------------------------------------------------
    // Model 4: Collaborative Co-Occurrence Graph Idempotency & Markov Edges
    // -------------------------------------------------------------------------

    @Test
    fun `syncLikedCoOccurrence is idempotent across repeated calls`() {
        val liked = listOf(
            track("l1", title = "Kalaavathi", artist = "Sid Sriram", genre = "Tollywood"),
            track("l2", title = "Inkem Inkem", artist = "Sid Sriram", genre = "Melodies"),
            track("l3", title = "Srivalli", artist = "Sid Sriram", genre = "Tollywood")
        )
        val probe = track("p1", title = "Maguva Maguva", artist = "Sid Sriram", genre = "Melodies")

        EchoTasteMlModel.syncLikedCoOccurrence(liked)
        val boostOnce = EchoTasteMlModel.getCoOccurrenceBoost(probe, liked)

        repeat(20) {
            EchoTasteMlModel.syncLikedCoOccurrence(liked)
        }
        val boostRepeated = EchoTasteMlModel.getCoOccurrenceBoost(probe, liked)

        assertTrue("Artist co-occurrence boost should be positive ($boostOnce)", boostOnce > 0f)
        assertEquals("Repeated sync must remain idempotent without edge inflation", boostOnce, boostRepeated, 0.001f)
    }

    @Test
    fun `sequential play interactions reinforce transition co-occurrence`() {
        val now = 1_750_000_000_000L
        val anchor = track("a1", title = "Track A", artist = "Artist Alpha", genre = "Indie")
        val next = track("b1", title = "Track B", artist = "Artist Beta", genre = "Rock")

        val baseline = EchoTasteMlModel.getCoOccurrenceBoost(next, listOf(anchor))
        EchoTasteMlModel.recordInteraction(anchor, TasteSignalType.PLAY_START, TimeOfDayBucket.AFTERNOON, now)
        EchoTasteMlModel.recordInteraction(next, TasteSignalType.COMPLETION, TimeOfDayBucket.AFTERNOON, now + 30_000L)
        val reinforced = EchoTasteMlModel.getCoOccurrenceBoost(next, listOf(anchor))

        assertTrue("Sequential play should strengthen A->B transition boost ($reinforced > $baseline)", reinforced > baseline)
    }

    // -------------------------------------------------------------------------
    // Model 5 & 6: K-Means Clustering & MMR Re-Ranking Edge Cases
    // -------------------------------------------------------------------------

    @Test
    fun `clusterTracksForDailyMixes handles degenerate and diverse pools`() {
        assertTrue(EchoTasteMlModel.clusterTracksForDailyMixes(emptyList(), emptyList(), emptyList(), k = 4).isEmpty())
        assertTrue(
            EchoTasteMlModel.clusterTracksForDailyMixes(
                listOf(track("s1"), track("s2")),
                emptyList(),
                emptyList(),
                k = 4
            ).isEmpty()
        )
        assertTrue(
            EchoTasteMlModel.clusterTracksForDailyMixes(
                (1..6).map { track("x$it") },
                emptyList(),
                emptyList(),
                k = 0
            ).isEmpty()
        )

        val diversePool = (1..16).map { idx ->
            val genre = when (idx % 4) {
                0 -> "Melodies"
                1 -> "Hip-Hop"
                2 -> "Lo-Fi"
                else -> "Electronic"
            }
            track(
                id = "pool_$idx",
                title = "Cluster Track $idx",
                artist = "Creator ${(idx % 8) + 1}",
                genre = genre,
                durationMs = 150_000L + (idx % 4) * 35_000L,
                isExplicit = idx % 4 == 1
            )
        }
        val clusters = EchoTasteMlModel.clusterTracksForDailyMixes(
            pool = diversePool,
            favorites = diversePool.take(3),
            recentlyPlayed = diversePool.drop(4).take(2),
            k = 3
        )
        assertTrue("Should produce at least 1 valid cluster", clusters.isNotEmpty())
        assertTrue("Each returned cluster must contain >= 3 tracks", clusters.all { it.tracks.size >= 3 })
    }

    @Test
    fun `mmrRerank handles zero negative limit and promotes diversity over clones`() {
        val now = 1_750_000_000_000L
        val clone1 = track("cl1", title = "Clone Song", artist = "Same Artist", genre = "Melodies")
        val clone2 = track("cl2", title = "Clone Song II", artist = "Same Artist", genre = "Melodies")
        val fresh = track("fr1", title = "Fresh Beat", artist = "Other Artist", genre = "Electronic")

        // Give clone1 and fresh positive history so clone1 leads, while fresh is diverse in genre & artist.
        EchoTasteMlModel.recordInteraction(clone1, TasteSignalType.LIKE, TimeOfDayBucket.EVENING, now)
        EchoTasteMlModel.recordInteraction(fresh, TasteSignalType.COMPLETION, TimeOfDayBucket.EVENING, now)

        val embeddings = mapOf(
            clone1.id to EchoTasteMlModel.embedTrack(clone1),
            clone2.id to EchoTasteMlModel.embedTrack(clone2),
            fresh.id to EchoTasteMlModel.embedTrack(fresh)
        )
        // Pure relevance (lambda = 1.0f) vs strong diversity (lambda = 0.25f)
        val pureRel = EchoTasteMlModel.rankCandidatesWithMl(
            candidates = listOf(clone1, clone2, fresh),
            favorites = listOf(clone1),
            recentlyPlayed = emptyList(),
            timeBucket = TimeOfDayBucket.EVENING,
            limit = 3,
            lambdaMmr = 1.0f,
            dailySeed = 1L,
            nowMs = now
        )

        assertTrue(EchoTasteMlModel.mmrRerank(pureRel, embeddings, limit = 0).isEmpty())
        assertTrue(EchoTasteMlModel.mmrRerank(pureRel, embeddings, limit = -3).isEmpty())

        val reranked = EchoTasteMlModel.mmrRerank(pureRel, embeddings, limit = 3, lambda = 0.25f)
        assertEquals(3, reranked.size)
        assertEquals(pureRel.first().track.id, reranked.first().track.id)
        if (reranked.first().track.id == "cl1") {
            assertEquals("MMR should prefer diverse artist ahead of redundant clone", "fr1", reranked[1].track.id)
        }
    }

    // -------------------------------------------------------------------------
    // Recommendation Engine & Queue Algorithms
    // -------------------------------------------------------------------------

    @Test
    fun `recommendation interleaving opener-closer arc and unplayed shuffle preserve invariants`() {
        val tracks = listOf(
            track("1", title = "S1", artist = "Artist A", genre = "Melodies"),
            track("2", title = "S2", artist = "Artist A", genre = "Melodies"),
            track("3", title = "S3", artist = "Artist B", genre = "Hip-Hop"),
            track("4", title = "S4", artist = "Artist B", genre = "Hip-Hop"),
            track("5", title = "S5", artist = "Artist C", genre = "Rock"),
            track("6", title = "S6", artist = "Artist C", genre = "Rock")
        )

        val interleaved = EchoRecommendationEngine.spotifyArtistInterleave(tracks)
        assertEquals(tracks.size, interleaved.size)
        for (i in 1 until interleaved.size) {
            assertNotEquals(
                "Adjacent tracks should not share primary artist when balanced",
                EchoRecommendationEngine.primaryArtistOf(interleaved[i - 1].artistName),
                EchoRecommendationEngine.primaryArtistOf(interleaved[i].artistName)
            )
        }

        val genreMixed = EchoRecommendationEngine.roundRobinGenreInterleave(tracks)
        assertEquals(tracks.size, genreMixed.size)
        for (i in 1 until genreMixed.size) {
            assertNotEquals(
                "Adjacent tracks should alternate genres when balanced",
                MusicGenre.inferGenre(genreMixed[i - 1]),
                MusicGenre.inferGenre(genreMixed[i])
            )
        }

        val scores = mapOf(
            "s1|artist a" to 10.0,
            "s2|artist a" to 2.0,
            "s3|artist b" to 8.0,
            "s4|artist b" to 5.0
        )
        val arc = EchoRecommendationEngine.applyOpenerCloserArc(tracks.take(4), scores)
        assertEquals("1", arc.first().id)
        assertEquals("3", arc.last().id)

        val playedKeys = setOf(
            SmartRadioQueue.normalizedSongKey("S1", "Artist A"),
            SmartRadioQueue.normalizedSongKey("S3", "Artist B")
        )
        val shuffled = EchoRecommendationEngine.orderUnplayedFirstShuffle(
            queue = tracks,
            currentIndex = 0,
            playedSongKeys = playedKeys,
            random = Random(7)
        )
        assertEquals("Active track must remain at index 0", "1", shuffled.first().id)
        assertEquals(tracks.size, shuffled.size)
        // Played "S3" must appear after unplayed tracks (indexes 1..4 are unplayed 2,4,5,6)
        assertEquals("3", shuffled.last().id)

        // Out-of-bounds currentIndex should not crash
        val safeOut = EchoRecommendationEngine.orderUnplayedFirstShuffle(
            queue = tracks,
            currentIndex = 999,
            playedSongKeys = playedKeys,
            random = Random(1)
        )
        assertEquals(tracks.size, safeOut.size)
    }

    @Test
    fun `buildSameGenreOrderedQueue respects limit bounds and keeps seed first`() {
        val seed = track("seed", title = "Love Story", artist = "Taylor Swift", genre = "Global Pop")
        val candidates = (1..5).map {
            track("cand$it", title = "Pop Track $it", artist = "Artist $it", genre = "Global Pop")
        }

        assertTrue(
            EchoRecommendationEngine.buildSameGenreOrderedQueue(seed, candidates, MusicGenre.GLOBAL_POP, limit = 0)
                .isEmpty()
        )
        val single = EchoRecommendationEngine.buildSameGenreOrderedQueue(seed, candidates, MusicGenre.GLOBAL_POP, limit = 1)
        assertEquals(listOf(seed), single)

        val queue = EchoRecommendationEngine.buildSameGenreOrderedQueue(seed, candidates, MusicGenre.GLOBAL_POP, limit = 4)
        assertEquals(seed.id, queue.first().id)
        assertTrue(queue.size <= 4)
    }

    @Test
    fun `insertPlayNext splices immediately after active track and deduplicates later occurrence`() {
        val q = (1..4).map { track("$it") }
        val updated = EchoRecommendationEngine.insertPlayNext(q, currentIndex = 1, track = q[3])
        assertEquals(listOf("1", "2", "4", "3"), updated.map { it.id })
    }

    @Test
    fun `home recommendation pipeline generates deduplicated shelves daily mixes and banners`() {
        val fav = listOf(track("fv1", title = "Melodic Soul", artist = "Sid Sriram", genre = "Melodies", isFavorite = true))
        val recent = listOf(track("rc1", title = "Night Vibe", artist = "Anirudh", genre = "Lo-Fi"))
        val pool = (1..18).map { idx ->
            track(
                id = "p_$idx",
                title = "Catalog Song $idx",
                artist = "Singer ${(idx % 6) + 1}",
                genre = when (idx % 4) {
                    0 -> "Melodies"
                    1 -> "Global Pop"
                    2 -> "Lo-Fi"
                    else -> "Tollywood"
                }
            )
        }

        val homeRecs = EchoRecommendationEngine.generateHomeRecommendationsWithReasons(
            recentlyPlayed = recent,
            favorites = fav,
            catalogPool = pool,
            limit = 10
        )
        assertTrue(homeRecs.isNotEmpty())
        assertTrue(homeRecs.size <= 10)
        assertTrue(homeRecs.distinctBy { it.track.id }.size == homeRecs.size)

        val (_, likedMix) = EchoRecommendationEngine.generateLikedInspiredMix(
            favorites = fav,
            catalogPool = pool,
            limit = 8
        )
        assertTrue(likedMix.isNotEmpty() && likedMix.size <= 8)

        val genreMixes = EchoRecommendationEngine.generateGenreMixes(
            favorites = fav,
            recentlyPlayed = recent,
            catalogPool = pool,
            limit = 4
        )
        assertTrue(genreMixes.isNotEmpty())

        val dailyMixes = EchoRecommendationEngine.generateDailyMixes(
            recentlyPlayed = recent,
            favorites = fav,
            catalogPool = pool
        )
        assertTrue(dailyMixes.isNotEmpty())

        val banners = EchoRecommendationEngine.generateDailySpotlightBanners(
            favorites = fav,
            newSongs = pool.take(6),
            dailyMixes = dailyMixes
        )
        assertTrue(banners.isNotEmpty())

        val banner = EchoRecommendationEngine.getContextualBanner(TimeOfDayBucket.MORNING)
        assertTrue(banner.title.isNotBlank())
        assertEquals(TimeOfDayBucket.MORNING, EchoRecommendationEngine.getTimeOfDayBucket(8))
        assertEquals(TimeOfDayBucket.AFTERNOON, EchoRecommendationEngine.getTimeOfDayBucket(14))
        assertEquals(TimeOfDayBucket.EVENING, EchoRecommendationEngine.getTimeOfDayBucket(19))
        assertEquals(TimeOfDayBucket.NIGHT, EchoRecommendationEngine.getTimeOfDayBucket(1))
    }

    @Test
    fun `isJunkTitle and primaryArtistOf normalize edge strings`() {
        assertTrue(EchoRecommendationEngine.isJunkTitle("Naatu Naatu (Karaoke Version)"))
        assertTrue(EchoRecommendationEngine.isJunkTitle("Tum Hi Ho 8D Audio"))
        assertFalse(EchoRecommendationEngine.isJunkTitle("Tum Hi Ho"))
        assertEquals("Anirudh Ravichander", EchoRecommendationEngine.primaryArtistOf("Anirudh Ravichander feat. Jonita Gandhi"))
    }

    // -------------------------------------------------------------------------
    // SmartRadioQueue Sliding Deduplication Window
    // -------------------------------------------------------------------------

    @Test
    fun `SmartRadioQueue deduplicates queue seed variants and evicts oldest beyond 80 entries`() {
        val uniquePrefix = "UniqueStress${System.nanoTime()}"
        val seed = track("seed_r", title = "$uniquePrefix (feat. Shreya Ghoshal)", artist = "Arijit Singh feat. Friends")
        val dupOfSeed = track("dup_seed", title = uniquePrefix, artist = "Arijit Singh")
        val inQueue = track("in_q", title = "${uniquePrefix}_InQ", artist = "Shreya Ghoshal")
        val fresh = track("fresh_r", title = "${uniquePrefix}_Fresh", artist = "Pritam")

        val filtered = SmartRadioQueue.filterDuplicates(
            candidates = listOf(dupOfSeed, inQueue, fresh),
            currentQueue = listOf(inQueue),
            seedTrackId = seed.id
        )
        // inQueue must be removed; dupOfSeed may be kept unless it's played/in queue, let's also record seed played:
        SmartRadioQueue.recordTrackPlayed(seed)
        val afterPlayed = SmartRadioQueue.filterDuplicates(
            candidates = listOf(dupOfSeed, inQueue, fresh),
            currentQueue = listOf(inQueue),
            seedTrackId = seed.id
        )
        assertEquals(listOf("fresh_r"), afterPlayed.map { it.id })
        assertTrue(filtered.none { it.id == "in_q" })

        // Push > 80 distinct tracks to verify bounded recent history window
        repeat(85) { i ->
            SmartRadioQueue.recordTrackPlayed(track("hist_$i", title = "${uniquePrefix}_Hist_$i", artist = "HistArtist_$i"))
        }
        assertTrue(SmartRadioQueue.getRecentlyPlayedKeysSnapshot().size <= 80)
    }

    // -------------------------------------------------------------------------
    // SearchRankingEngine, CrossfadeCurve, DailyFeedCoordinator, Lyrics, Upgrade
    // -------------------------------------------------------------------------

    @Test
    fun `SearchRankingEngine satisfies edit-distance axioms and ranks exact ahead of fuzzy`() {
        assertEquals(0, SearchRankingEngine.levenshteinDistance("echo", "echo"))
        assertEquals(4, SearchRankingEngine.levenshteinDistance("echo", ""))
        assertEquals(
            SearchRankingEngine.levenshteinDistance("starboy", "straboy"),
            SearchRankingEngine.levenshteinDistance("straboy", "starboy")
        )
        assertEquals(1.0, SearchRankingEngine.normalizedSimilarity("blinding", "blinding"), 0.0001)
        assertEquals(0.0, SearchRankingEngine.normalizedSimilarity("", "abc"), 0.0001)

        val exact = track("e1", title = "Starboy", artist = "The Weeknd")
        val typoClose = track("e2", title = "Starboy Remix", artist = "DJ Test")
        val unrelated = track("e3", title = "Ocean Waves", artist = "Ambient")

        val ranked = SearchRankingEngine.rankTracks("starboy", listOf(unrelated, typoClose, exact))
        assertEquals("e1", ranked.first().id)
        assertTrue(SearchRankingEngine.rankTracks("   ", listOf(exact)).size == 1)
    }

    @Test
    fun `CrossfadeCurve maintains equal-power energy conservation and headroom safety`() {
        for (step in 0..20) {
            val p = step / 20f
            val outG = CrossfadeCurve.outgoingGain(p)
            val inG = CrossfadeCurve.incomingGain(p)
            val powerSum = outG * outG + inG * inG
            assertEquals("Equal-power invariant broken at p=$p", 1.0f, powerSum, 0.01f)
            assertTrue(CrossfadeCurve.headroom(inG, outG) in 0.7f..1.0f)
        }
        assertEquals(1.0f, CrossfadeCurve.outgoingGain(-0.5f), 0.001f)
        assertEquals(0.0f, CrossfadeCurve.incomingGain(-0.5f), 0.001f)
        assertEquals(0.0f, CrossfadeCurve.outgoingGain(1.5f), 0.001f)
        assertEquals(1.0f, CrossfadeCurve.incomingGain(1.5f), 0.001f)
    }

    @Test
    fun `DailyFeedCoordinator computes deterministic seeds rollover and midnight bounds`() {
        val now = 1_750_000_000_000L
        val key = DailyFeedCoordinator.formatDateKey(now)
        assertTrue(key.matches(Regex("""\d{4}-\d{2}-\d{2}""")))
        assertEquals(DailyFeedCoordinator.dailySeed(key), DailyFeedCoordinator.dailySeed(key))
        assertTrue(DailyFeedCoordinator.friendlyDateLabel(now).isNotBlank())
        assertTrue(DailyFeedCoordinator.needsDailyRefresh(now))

        DailyFeedCoordinator.markDailyRefreshed(now)
        assertFalse(DailyFeedCoordinator.needsDailyRefresh(now + 1000L))
        assertFalse(DailyFeedCoordinator.checkDayRollover(now + 1000L))

        val nextDay = now + 26L * 3600L * 1000L
        assertTrue(DailyFeedCoordinator.checkDayRollover(nextDay))

        val untilMidnight = DailyFeedCoordinator.msUntilNextMidnight(now)
        assertTrue(untilMidnight in 1L..86_400_000L)
    }

    @Test
    fun `LyricsSyncEngine rescales drifted timestamps aligns dense unsynced lines and transliterates Indic scripts`() {
        // Dense unsynced lines inside short track must stay strictly < trackDurationMs
        val denseLines = (1..60).map { LyricLine(-1L, "Wordy phrase number $it for density check") }
        val alignedDense = LyricsSyncEngine.alignUnsyncedLines(denseLines, trackDurationMs = 90_000L)
        assertEquals(60, alignedDense.size)
        assertTrue(alignedDense.last().timestampMs < 90_000L)
        for (i in 1 until alignedDense.size) {
            assertTrue(alignedDense[i].timestampMs > alignedDense[i - 1].timestampMs)
        }

        // LRC extending beyond track duration should be scaled proportionally back inside track
        val overflowLrc = listOf(
            LyricLine(10_000L, "Line 1"),
            LyricLine(60_000L, "Line 2"),
            LyricLine(120_000L, "Line 3"),
            LyricLine(180_000L, "Line 4"),
            LyricLine(220_000L, "Line 5")
        )
        val normalized = LyricsSyncEngine.normalizeSyncedLines(overflowLrc, trackDurationMs = 150_000L)
        assertTrue("Scaled timestamp must fit within track duration", normalized.last().timestampMs < 150_000L)

        // Indic script transliteration for Tamil, Telugu, Devanagari
        val tamil = IndicRomanizer.transliterate("வணக்கம்")
        val telugu = IndicRomanizer.transliterate("హలో ప్రపంచం")
        val devanagari = IndicRomanizer.transliterate("दिल से")
        assertTrue(tamil.isNotBlank() && tamil.none { it.code in 0x0900..0x0D7F })
        assertTrue(telugu.isNotBlank() && telugu.none { it.code in 0x0900..0x0D7F })
        assertTrue(devanagari.isNotBlank() && devanagari.none { it.code in 0x0900..0x0D7F })

        val matchExact = LyricsSyncEngine.scoreCandidateMatch(
            queryTitle = "Blinding Lights",
            queryArtist = "The Weeknd",
            targetDurationMs = 200_000L,
            candTitle = "Blinding Lights",
            candArtist = "The Weeknd",
            candDurationSec = 200.0,
            hasSynced = true
        )
        val matchWrong = LyricsSyncEngine.scoreCandidateMatch(
            queryTitle = "Blinding Lights",
            queryArtist = "The Weeknd",
            targetDurationMs = 200_000L,
            candTitle = "Completely Different",
            candArtist = "Other Singer",
            candDurationSec = 400.0,
            hasSynced = false
        )
        assertTrue(matchExact > matchWrong)

        // prepareEnglishSyncedLyrics on NONE returns untouched syncType
        val noneLyrics = LyricsResult(LyricsSyncType.NONE, emptyList(), "None")
        assertEquals(LyricsSyncType.NONE, LyricsSyncEngine.prepareEnglishSyncedLyrics(noneLyrics, 180_000L).syncType)
    }

    @Test
    fun `SongUpgradeEngine upgrades HTML and artwork without overriding unliked favorite status`() {
        assertEquals("Tom & Jerry's \"Mix\"", SongUpgradeEngine.decodeBasicHtml("Tom &amp; Jerry&#039;s &quot;Mix&quot;"))
        assertEquals(
            "http://c.saavncdn.com/123/art-500x500.jpg",
            SongUpgradeEngine.upgradeArtworkUrl("http://c.saavncdn.com/123/art-150x150.jpg")
        )

        val likedCached = track("fav_preserve", title = "Tum &amp; Hi Ho", isFavorite = true, coverArtUrl = "http://c.saavncdn.com/1/a-150x150.jpg")
        SongUpgradeEngine.upgradeMetadataFast(likedCached)

        val unlikedInput = likedCached.copy(isFavorite = false)
        val upgraded = SongUpgradeEngine.upgradeMetadataFast(unlikedInput)
        assertFalse("Unliked input must never be flipped back to favorite by cache", upgraded.isFavorite)
        assertEquals("Tum & Hi Ho", upgraded.title)
        assertTrue(abs(upgraded.durationMs - 200_000L) == 0L)
    }
}
