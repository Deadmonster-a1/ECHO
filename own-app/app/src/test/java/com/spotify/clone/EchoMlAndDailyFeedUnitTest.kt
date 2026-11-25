package com.spotify.clone

import com.spotify.clone.core.data.SampleCatalog
import com.spotify.clone.core.model.MusicGenre
import com.spotify.clone.core.model.Track
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
import kotlin.math.sqrt

class EchoMlAndDailyFeedUnitTest {

    @Before
    fun setup() {
        EchoTasteMlModel.resetForTests()
        EchoRecommendationEngine.clearTasteSignals()
        SongUpgradeEngine.clearCacheForTests()
        DailyFeedCoordinator.resetForTests()
    }

    private fun makeTrack(
        id: String,
        title: String,
        artist: String,
        album: String = "Single",
        cover: String = "https://c.saavncdn.com/313/cover-150x150.jpg",
        uri: String = "https://cdn.example.com/$id.mp3",
        isFavorite: Boolean = false
    ) = Track(
        id = id,
        title = title,
        artistName = artist,
        albumName = album,
        coverArtUrl = cover,
        durationMs = 210_000L,
        mediaUri = uri,
        isFavorite = isFavorite
    )

    @Test
    fun audioFeatureEmbedder_producesNormalized12DVectorsWithSemanticSimilarity() {
        val melody1 = makeTrack("m1", "Inthandham", "Sid Sriram", "Sita Ramam")
        val melody2 = makeTrack("m2", "Samajavaragamana", "Sid Sriram", "Ala Vaikunthapurramuloo")
        val rock = makeTrack("r1", "Master of Puppets Rock", "Metallica", "Rock Anthology")

        val v1 = EchoTasteMlModel.embedTrack(melody1)
        val v2 = EchoTasteMlModel.embedTrack(melody2)
        val vr = EchoTasteMlModel.embedTrack(rock)

        assertEquals(EchoTasteMlModel.EMBEDDING_DIM, v1.size)
        val norm1 = sqrt(v1.sumOf { (it * it).toDouble() }).toFloat()
        assertEquals(1.0f, norm1, 0.01f)

        val simMelodies = EchoTasteMlModel.cosineSimilarity(v1, v2)
        val simCross = EchoTasteMlModel.cosineSimilarity(v1, vr)
        assertTrue("Similar melodies ($simMelodies) should exceed cross-genre ($simCross)", simMelodies > simCross)
    }

    @Test
    fun mlTasteProfile_learnsFromLikesAndPenalizesEarlySkips() {
        val liked = makeTrack("l1", "Chuttamalle", "Shilpa Rao", "Devara")
        val skipped = makeTrack("s1", "Heavy Metal Noise", "Metallica", "Rock")

        val t0 = 1_700_000_000_000L
        EchoTasteMlModel.recordInteraction(liked, TasteSignalType.LIKE, nowMs = t0)
        EchoTasteMlModel.recordInteraction(liked, TasteSignalType.COMPLETION, nowMs = t0 + 1000L)
        EchoTasteMlModel.recordInteraction(skipped, TasteSignalType.EARLY_SKIP, nowMs = t0 + 2000L)

        val likedArtistAff = EchoTasteMlModel.getDecayedArtistAffinity("Shilpa Rao", nowMs = t0 + 3000L)
        val skippedArtistAff = EchoTasteMlModel.getDecayedArtistAffinity("Metallica", nowMs = t0 + 3000L)
        assertTrue(likedArtistAff > skippedArtistAff)

        // Temporal decay lowers old affinities after 14 days
        val after14Days = EchoTasteMlModel.getDecayedArtistAffinity("Shilpa Rao", nowMs = t0 + 14L * 86_400_000L)
        assertTrue("Decayed affinity ($after14Days) should be less than fresh ($likedArtistAff)", after14Days < likedArtistAff)
    }

    @Test
    fun contextualBandit_ucbExplorationBonusFavorsUnderExploredArms() {
        val unpulledBonus = EchoTasteMlModel.ucbBonus(totalPulls = 50, armPulls = 0)
        val heavilyPulledBonus = EchoTasteMlModel.ucbBonus(totalPulls = 50, armPulls = 25)
        assertTrue("Unpulled arm bonus ($unpulledBonus) should exceed heavily pulled ($heavilyPulledBonus)", unpulledBonus > heavilyPulledBonus)
    }

    @Test
    fun rankCandidatesWithMl_returnsExplainablePredictionsWithinLimit() {
        val fav = SampleCatalog.trackChuttamalle.copy(isFavorite = true)
        EchoTasteMlModel.recordInteraction(fav, TasteSignalType.LIKE)

        val predictions = EchoTasteMlModel.rankCandidatesWithMl(
            candidates = SampleCatalog.sampleTracks,
            favorites = listOf(fav),
            recentlyPlayed = listOf(SampleCatalog.trackFearSong),
            timeBucket = TimeOfDayBucket.EVENING,
            limit = 8,
            dailySeed = DailyFeedCoordinator.dailySeed("2026-11-25")
        )
        assertEquals(8, predictions.size)
        assertTrue(predictions.all { it.reasonTag.isNotBlank() && it.matchPercent in 72..99 })
        assertEquals(predictions.size, predictions.distinctBy { it.track.id }.size)
    }

    @Test
    fun tasteClusterer_createsCohesiveClustersForDailyMixes() {
        val clusters = EchoTasteMlModel.clusterTracksForDailyMixes(
            pool = SampleCatalog.sampleTracks,
            favorites = listOf(SampleCatalog.trackInthandham),
            recentlyPlayed = listOf(SampleCatalog.trackNaatuNaatu),
            k = 3
        )
        assertTrue(clusters.isNotEmpty())
        assertTrue(clusters.all { it.tracks.size >= 3 })
    }

    @Test
    fun songUpgradeEngine_upgradesArtworkDecodesHtmlTagsGenreAndFiltersJunk() {
        val lowRes = makeTrack(
            id = "raw_1",
            title = "Chuttamalle &amp; Friends",
            artist = "Shilpa Rao",
            album = "Devara",
            cover = "https://is1-ssl.mzstatic.com/image/thumb/100x100bb.jpg",
            uri = ""
        )
        val junk = makeTrack(
            id = "junk_1",
            title = "Chuttamalle Slowed + Reverb 8D Audio",
            artist = "Unknown"
        )
        val duplicateStudio = makeTrack(
            id = "studio_1",
            title = "Chuttamalle & Friends",
            artist = "Shilpa Rao",
            album = "Devara",
            cover = "https://c.saavncdn.com/313/cover-500x500.jpg",
            uri = "https://aac.saavncdn.com/full_320.mp4"
        )

        val upgraded = SongUpgradeEngine.upgradeCatalogFast(listOf(lowRes, junk, duplicateStudio))
        assertEquals(1, upgraded.size)
        val best = upgraded.first()
        assertEquals("studio_1", best.id)
        assertEquals("Chuttamalle & Friends", best.title)
        assertFalse(best.genre.equals("All", ignoreCase = true))

        val upgradedAppleCover = SongUpgradeEngine.upgradeArtworkUrl("https://mzstatic.com/art/100x100bb.jpg")
        assertTrue(upgradedAppleCover.contains("600x600bb"))
    }

    @Test
    fun dailyFeedCoordinator_producesDeterministicDailySeedsAndMidnightBounds() {
        val seedDay1 = DailyFeedCoordinator.dailySeed("2026-11-25")
        val seedDay1Again = DailyFeedCoordinator.dailySeed("2026-11-25")
        val seedDay2 = DailyFeedCoordinator.dailySeed("2026-11-26")

        assertEquals(seedDay1, seedDay1Again)
        assertNotEquals(seedDay1, seedDay2)

        val msUntilMidnight = DailyFeedCoordinator.msUntilNextMidnight()
        assertTrue(msUntilMidnight >= 60_000L)
        assertTrue(msUntilMidnight <= 86_405_000L)
        assertTrue(DailyFeedCoordinator.needsDailyRefresh())
        DailyFeedCoordinator.markDailyRefreshed()
        assertFalse(DailyFeedCoordinator.needsDailyRefresh())
    }

    @Test
    fun dailyGenerators_produceLikedMixGenreWorldsAndDiscoverDaily() {
        val favs = listOf(SampleCatalog.trackChuttamalle.copy(isFavorite = true))
        val recent = listOf(SampleCatalog.trackPushpaPushpa)

        val (likedLabel, likedMix) = EchoRecommendationEngine.generateLikedInspiredMix(
            favorites = favs,
            catalogPool = SampleCatalog.sampleTracks,
            limit = 8
        )
        assertTrue(likedLabel.contains("Chuttamalle"))
        assertTrue(likedMix.isNotEmpty())

        val genreMixes = EchoRecommendationEngine.generateGenreMixes(
            favorites = favs,
            recentlyPlayed = recent,
            catalogPool = SampleCatalog.sampleTracks,
            limit = 4
        )
        assertTrue(genreMixes.isNotEmpty())
        assertTrue(genreMixes.all { it.tracks.isNotEmpty() })

        val dailyMixes = EchoRecommendationEngine.generateDailyMixes(
            recentlyPlayed = recent,
            favorites = favs,
            catalogPool = SampleCatalog.sampleTracks
        )
        assertTrue(dailyMixes.any { it.id.startsWith("daily_mix_discover_") })

        val banners = EchoRecommendationEngine.generateDailySpotlightBanners(
            favorites = favs,
            newSongs = SampleCatalog.sampleTracks.take(4),
            dailyMixes = dailyMixes
        )
        assertTrue(banners.size >= 3)
        assertEquals(MusicGenre.TOL_TELUGU.shortLabel, MusicGenre.TOL_TELUGU.shortLabel)
    }
}
