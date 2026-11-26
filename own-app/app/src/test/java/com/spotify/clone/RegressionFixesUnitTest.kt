package com.spotify.clone

import androidx.compose.ui.graphics.Color
import com.spotify.clone.core.designsystem.AppThemeOption
import com.spotify.clone.core.designsystem.echoContrastOn
import com.spotify.clone.core.lyrics.LyricsRepository
import com.spotify.clone.core.model.MusicGenre
import com.spotify.clone.core.model.Track
import com.spotify.clone.core.player.SmartRadioQueue
import com.spotify.clone.core.recommendation.EchoRecommendationEngine
import com.spotify.clone.core.recommendation.TimeOfDayBucket
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class RegressionFixesUnitTest {

    private fun sampleTrack(
        id: String = "t1",
        title: String,
        artist: String = "Unknown Artist",
        album: String = "Single",
        uri: String = "https://example.com/audio.mp3"
    ) = Track(
        id = id,
        title = title,
        artistName = artist,
        albumName = album,
        coverArtUrl = "",
        durationMs = 180_000L,
        mediaUri = uri
    )

    @Test
    fun lyricsParser_supportsMultiTimestampAndSkipsMetadataHeaders() {
        val lrc = """
            [ti:Test Song]
            [ar:Test Artist]
            [00:10.50][00:45.00]Repeating chorus line
            [00:22.00]Second verse line
        """.trimIndent()

        val lines = LyricsRepository.parseLrc(lrc)
        assertEquals(3, lines.size)
        assertEquals(10_500L, lines[0].timestampMs)
        assertEquals("Repeating chorus line", lines[0].words)
        assertEquals(22_000L, lines[1].timestampMs)
        assertEquals("Second verse line", lines[1].words)
        assertEquals(45_000L, lines[2].timestampMs)
        assertEquals("Repeating chorus line", lines[2].words)
        assertTrue(lines.all { it.durationMs > 0L })
    }

    @Test
    fun smartRadioQueue_deduplicatesByIdAndNormalizedTitleArtist() {
        val seed = sampleTrack(id = "s1", title = "Blinding Lights", artist = "The Weeknd")
        val dup = sampleTrack(id = "s2", title = "Blinding Lights (Remastered)", artist = "The Weeknd")
        val fresh = sampleTrack(id = "s3", title = "Starboy", artist = "The Weeknd")

        val filtered = SmartRadioQueue.filterDuplicates(
            candidates = listOf(seed, dup, fresh),
            currentQueue = listOf(seed),
            seedTrackId = seed.id
        )
        assertEquals(1, filtered.size)
        assertEquals("s3", filtered.first().id)
    }

    @Test
    fun themeOptions_distinctIdsAndContrastReadable() {
        assertEquals(
            AppThemeOption.entries.size,
            AppThemeOption.entries.distinctBy { it.id }.size
        )

        val onWhite = echoContrastOn(Color.White)
        val onBlack = echoContrastOn(Color.Black)
        assertNotEquals(Color.White, onWhite)
        assertEquals(Color.White, onBlack)
    }

    @Test
    fun spotifyArtistInterleave_preventsConsecutiveSameArtist() {
        val tracks = listOf(
            sampleTrack(id = "a1", title = "Song 1", artist = "Anirudh"),
            sampleTrack(id = "a2", title = "Song 2", artist = "Anirudh"),
            sampleTrack(id = "a3", title = "Song 3", artist = "Anirudh"),
            sampleTrack(id = "b1", title = "Song 4", artist = "Sid Sriram"),
            sampleTrack(id = "b2", title = "Song 5", artist = "Sid Sriram"),
            sampleTrack(id = "c1", title = "Song 6", artist = "Devi Sri Prasad")
        )
        val ordered = EchoRecommendationEngine.spotifyArtistInterleave(tracks)
        assertEquals(tracks.size, ordered.size)
        for (i in 0 until ordered.lastIndex) {
            assertNotEquals(
                "Consecutive same artist at $i",
                EchoRecommendationEngine.primaryArtistOf(ordered[i].artistName),
                EchoRecommendationEngine.primaryArtistOf(ordered[i + 1].artistName)
            )
        }
    }

    @Test
    fun unplayedFirstShuffle_putsUnplayedBeforePlayedAndKeepsCurrentFirst() {
        val current = sampleTrack(id = "cur", title = "Current Song", artist = "Artist A")
        val played = sampleTrack(id = "old", title = "Old Song", artist = "Artist B")
        val fresh1 = sampleTrack(id = "f1", title = "Fresh One", artist = "Artist C")
        val fresh2 = sampleTrack(id = "f2", title = "Fresh Two", artist = "Artist D")

        val playedKeys = setOf(SmartRadioQueue.normalizedSongKey(played.title, played.artistName))
        val ordered = EchoRecommendationEngine.orderUnplayedFirstShuffle(
            queue = listOf(played, current, fresh1, fresh2),
            currentIndex = 1,
            playedSongKeys = playedKeys,
            random = Random(42)
        )
        assertEquals("cur", ordered.first().id)
        assertEquals("old", ordered.last().id)
    }

    @Test
    fun sameGenreQueueBuilder_keepsStrictGenreAndPlacesSeedFirst() {
        val seed = sampleTrack(id = "seed", title = "Inthandham", artist = "Sid Sriram", album = "Sita Ramam")
        val sameGenre1 = sampleTrack(id = "m1", title = "Samajavaragamana", artist = "Sid Sriram", album = "Ala Vaikunthapurramuloo")
        val sameGenre2 = sampleTrack(id = "m2", title = "Kesariya", artist = "Arijit Singh", album = "Brahmastra")
        val wrongGenre = sampleTrack(id = "r1", title = "Master of Puppets", artist = "Metallica", album = "Metal")

        val queue = EchoRecommendationEngine.buildSameGenreOrderedQueue(
            seedTrack = seed,
            candidates = listOf(wrongGenre, sameGenre1, sameGenre2),
            targetGenre = MusicGenre.MELODIES,
            limit = 10
        )
        assertEquals("seed", queue.first().id)
        assertTrue(queue.any { it.id == "m1" || it.id == "m2" })
        assertFalse(queue.any { it.id == "r1" })
    }

    @Test
    fun localTasteScoring_rewardsLikesAndPenalizesEarlySkips() {
        val track = sampleTrack(id = "t_taste", title = "Chuttamalle", artist = "Shilpa Rao", album = "Devara")
        val likedScore = EchoRecommendationEngine.scoreLocalTasteSignal(
            track = track,
            playCount = 3,
            skipCount = 0,
            isLiked = true,
            isRecent = true,
            timeBucket = TimeOfDayBucket.NIGHT
        )
        val skippedScore = EchoRecommendationEngine.scoreLocalTasteSignal(
            track = track,
            playCount = 1,
            skipCount = 3,
            isLiked = false,
            isRecent = false,
            timeBucket = TimeOfDayBucket.AFTERNOON
        )
        assertTrue(likedScore > skippedScore)
    }

    @Test
    fun openerCloserArc_putsTopTwoAtStartAndEnd() {
        val t1 = sampleTrack(id = "1", title = "One", artist = "A")
        val t2 = sampleTrack(id = "2", title = "Two", artist = "B")
        val t3 = sampleTrack(id = "3", title = "Three", artist = "C")
        val t4 = sampleTrack(id = "4", title = "Four", artist = "D")
        val scores = mapOf(
            SmartRadioQueue.normalizedSongKey(t1.title, t1.artistName) to 10.0,
            SmartRadioQueue.normalizedSongKey(t2.title, t2.artistName) to 95.0,
            SmartRadioQueue.normalizedSongKey(t3.title, t3.artistName) to 72.0,
            SmartRadioQueue.normalizedSongKey(t4.title, t4.artistName) to 30.0
        )
        val ordered = EchoRecommendationEngine.applyOpenerCloserArc(listOf(t1, t2, t3, t4), scores)
        assertEquals("2", ordered.first().id)
        assertEquals("3", ordered.last().id)
    }

    @Test
    fun insertPlayNext_splicesDirectlyAfterCurrentWithoutDuplicates() {
        val t1 = sampleTrack(id = "1", title = "One", artist = "A")
        val t2 = sampleTrack(id = "2", title = "Two", artist = "B")
        val t3 = sampleTrack(id = "3", title = "Three", artist = "C")
        val updated = EchoRecommendationEngine.insertPlayNext(listOf(t1, t2, t3), currentIndex = 0, track = t3)
        assertEquals(listOf("1", "3", "2"), updated.map { it.id })
    }

    @Test
    fun crossfadeCurve_maintainsConstantEqualPowerAndCapsHeadroom() {
        val steps = listOf(0f, 0.25f, 0.5f, 0.75f, 1f)
        for (p in steps) {
            val outG = com.spotify.clone.core.player.CrossfadeCurve.outgoingGain(p)
            val inG = com.spotify.clone.core.player.CrossfadeCurve.incomingGain(p)
            val powerSum = outG * outG + inG * inG
            assertTrue("Equal-power sum out-of-bounds at p=$p: $powerSum", powerSum in 0.98f..1.02f)
            val combined = com.spotify.clone.core.player.CrossfadeCurve.headroom(inG, outG)
            assertTrue("Combined gain exceeded 1.0 at p=$p: $combined", combined <= 1.0f)
        }
    }

    @Test
    fun searchRankingEngine_prioritizesExactOverTypoAndFiltersUnrelated() {
        val seed = sampleTrack(id = "seed", title = "Seed", artist = "Anuv Jain")
        val exact = sampleTrack(id = "exact", title = "Midnight Echo", artist = "Arijit Singh")
        val typo = sampleTrack(id = "typo", title = "Midniight Ecoh", artist = "Arijit Singh")
        val unrelated = sampleTrack(id = "other", title = "Heavy Thunder", artist = "Random")
        EchoRecommendationEngine.clearTasteSignals()
        EchoRecommendationEngine.recordTrackPlay(seed)

        val ranked = com.spotify.clone.core.data.SearchRankingEngine.rankTracks(
            query = "Midnight Echo",
            tracks = listOf(unrelated, typo, exact)
        )
        assertEquals("exact", ranked.first().id)
        assertEquals(listOf("exact", "typo", "other"), ranked.map { it.id })
        assertTrue(
            com.spotify.clone.core.data.SearchRankingEngine.scoreTrackMatch("Midnight Echo", typo) >
                com.spotify.clone.core.data.SearchRankingEngine.scoreTrackMatch("Midnight Echo", unrelated)
        )
    }

    @Test
    fun generateDailyMixes_createsTasteClusteredMixesFromCatalog() {
        val tracks = listOf(
            sampleTrack(id = "p1", title = "Neon Lofi", artist = "KSHMR"),
            sampleTrack(id = "p2", title = "Sunset Chill", artist = "Anuv Jain"),
            sampleTrack(id = "p3", title = "Romantic Dil", artist = "Arijit Singh"),
            sampleTrack(id = "p4", title = "Bolly Dance", artist = "Pritam"),
            sampleTrack(id = "p5", title = "Gym Phonk", artist = "KR\$NA")
        )
        EchoRecommendationEngine.clearTasteSignals()
        val mixes = EchoRecommendationEngine.generateDailyMixes(
            recentlyPlayed = listOf(tracks.first()),
            favorites = listOf(tracks[2]),
            catalogPool = tracks
        )
        assertTrue(mixes.isNotEmpty())
        assertTrue(mixes.all { it.id.startsWith("daily_mix_") && it.tracks.isNotEmpty() })
    }

    @Test
    fun spotifyEncoreMotionCurves_satisfyMonotonicBounds() {
        val curves = listOf(
            com.spotify.clone.core.designsystem.EchoMotion.EncoreButtonEase,
            com.spotify.clone.core.designsystem.EchoMotion.EncoreBoxEase,
            com.spotify.clone.core.designsystem.EchoMotion.HeaderMotionEase
        )
        for (curve in curves) {
            assertEquals(0f, curve.transform(0f), 0.001f)
            assertEquals(1f, curve.transform(1f), 0.001f)
            val mid = curve.transform(0.5f)
            assertTrue("Midpoint should be in (0, 1): $mid", mid > 0f && mid < 1f)
        }
        // EncoreButtonEase reaches >0.85 quickly at t=0.5 due to (0.3, 0, 0, 1)
        assertTrue(com.spotify.clone.core.designsystem.EchoMotion.EncoreButtonEase.transform(0.5f) > 0.85f)
    }

    @Test
    fun scaleEqMillibels_handlesAsymmetricDeviceRanges() {
        val minMb = -1500
        val maxMb = 1200
        assertEquals((-1500).toShort(), com.spotify.clone.core.player.PlaybackService.scaleEqMillibels(-12f, minMb, maxMb))
        assertEquals(1200.toShort(), com.spotify.clone.core.player.PlaybackService.scaleEqMillibels(12f, minMb, maxMb))
        assertEquals(0.toShort(), com.spotify.clone.core.player.PlaybackService.scaleEqMillibels(0f, minMb, maxMb))
        assertEquals((-750).toShort(), com.spotify.clone.core.player.PlaybackService.scaleEqMillibels(-6f, minMb, maxMb))
        assertEquals(600.toShort(), com.spotify.clone.core.player.PlaybackService.scaleEqMillibels(6f, minMb, maxMb))
    }

    @Test
    fun streamResolverCache_expiresEntriesAfterTtl() {
        val resolver = com.spotify.clone.core.data.FullTrackStreamResolver
        resolver.clearStreamCache()
        val key = "test_song_artist"
        val t0 = 1_000_000L
        resolver.putCachedStream(key, "https://cdn.example.com/audio.mp4", nowMs = t0)
        assertEquals("https://cdn.example.com/audio.mp4", resolver.getCachedStreamIfFresh(key, nowMs = t0 + 60_000L))
        // After 21 minutes (> 20 minute TTL), entry should be evicted
        assertEquals(null, resolver.getCachedStreamIfFresh(key, nowMs = t0 + 21L * 60L * 1000L))
    }

    @Test
    fun downloadCleanup_preservesSharedAndStandaloneTracks() {
        val deletable = com.spotify.clone.core.download.TrackDownloadManager.computeDeletableTrackIds(
            playlistTrackIds = listOf("t1", "t2", "t3"),
            otherPlaylistTrackIds = setOf("t2"),
            standaloneTrackIds = setOf("t3")
        )
        assertEquals(setOf("t1"), deletable)
    }

    @Test
    fun settingsManager_clampsLyricsOffsetAndUpdatesFlows() {
        val sm = com.spotify.clone.core.settings.SettingsManager
        sm.setLyricsOffsetMs(25_000L)
        assertEquals(10_000L, sm.lyricsOffsetMs.value)
        sm.setLyricsOffsetMs(-25_000L)
        assertEquals(-10_000L, sm.lyricsOffsetMs.value)
        sm.setLyricsSizeTier("LARGE")
        assertEquals("LARGE", sm.lyricsSizeTier.value)
        sm.setLyricsUseRomanized(false)
        assertFalse(sm.lyricsUseRomanized.value)
        sm.resetDefaults()
        assertEquals(0L, sm.lyricsOffsetMs.value)
        assertEquals("STANDARD", sm.lyricsSizeTier.value)
        assertTrue(sm.lyricsUseRomanized.value)
    }

    @Test
    fun onboardingSeeding_drivesDailyMixesAndHomeRecommendationsWithoutHistory() {
        EchoRecommendationEngine.clearTasteSignals()
        EchoRecommendationEngine.seedOnboardingPreferences(
            languages = setOf("Telugu", "English"),
            genreIds = setOf(MusicGenre.MELODIES.id),
            artists = setOf("Sid Sriram", "Anirudh Ravichander", "Shilpa Rao")
        )

        val preferredGenre = EchoRecommendationEngine.getTopPreferredGenre(emptyList(), emptyList())
        assertEquals(MusicGenre.MELODIES, preferredGenre)

        val recs = EchoRecommendationEngine.generateHomeRecommendations(
            recentlyPlayed = emptyList(),
            favorites = emptyList(),
            catalogPool = com.spotify.clone.core.data.SampleCatalog.sampleTracks,
            limit = 8
        )
        assertTrue(recs.isNotEmpty())

        val mixes = EchoRecommendationEngine.generateDailyMixes(
            recentlyPlayed = emptyList(),
            favorites = emptyList(),
            catalogPool = com.spotify.clone.core.data.SampleCatalog.sampleTracks
        )
        assertTrue(mixes.isNotEmpty())
        assertTrue(mixes.first().ownerName == MusicGenre.MELODIES.shortLabel)
    }

    @Test
    fun pastelPrismTheme_isDefaultAndDaylightByDefault() {
        assertEquals(
            AppThemeOption.PASTEL_PRISM,
            com.spotify.clone.core.designsystem.ThemeManager.currentTheme.value
        )
        assertFalse(com.spotify.clone.core.designsystem.ThemeManager.isDarkMode.value)
        assertEquals(
            com.spotify.clone.core.profile.PrismAvatarOption.PRISM,
            com.spotify.clone.core.profile.PrismAvatarOption.fromId("unknown_id")
        )
    }

    @Test
    fun streamResolver_requiresResolutionForSpotifyUrisAndLiteralNull() {
        val resolver = com.spotify.clone.core.data.FullTrackStreamResolver
        assertTrue(resolver.needsResolution(sampleTrack(id = "s1", title = "Song 1", uri = "spotify:track:12345")))
        assertTrue(resolver.needsResolution(sampleTrack(id = "s2", title = "Song 2", uri = "null")))
        assertFalse(resolver.needsResolution(sampleTrack(id = "s3", title = "Song 3", uri = "https://aac.saavncdn.com/123/song_320.mp4")))
    }

    @Test
    fun appUpdateManager_comparesSuffixedSemverCorrectly() {
        val mgr = com.spotify.clone.core.update.AppUpdateManager
        assertTrue(mgr.isNewerVersion("2.1.1-beta1", "2.1.0"))
        assertFalse(mgr.isNewerVersion("2.1.0-rc1", "2.1.0"))
        assertTrue(mgr.isNewerVersion("2.2.0", "2.1.9-hotfix"))
    }

    @Test
    fun lyricsCatalog_doesNotHijackAmmaSubstrings() {
        assertEquals(null, com.spotify.clone.core.lyrics.LyricsCatalog.findCuratedLyrics("t1", "Ammaadi"))
        assertEquals(null, com.spotify.clone.core.lyrics.LyricsCatalog.findCuratedLyrics("t2", "Mamma Mia"))
        assertTrue(com.spotify.clone.core.lyrics.LyricsCatalog.findCuratedLyrics("t3", "Amma Song")?.isNotEmpty() == true)
    }

    @Test
    fun lyricsSyncEngine_scalesDenseUnsyncedLyricsWithinTrackDuration() {
        val denseLines = (1..80).map { com.spotify.clone.core.model.LyricLine(-1L, "Line number $it") }
        val aligned = com.spotify.clone.core.lyrics.LyricsSyncEngine.alignUnsyncedLines(denseLines, trackDurationMs = 60_000L)
        assertEquals(80, aligned.size)
        // Trailing lines should remain strictly increasing rather than piling onto the same final timestamp
        assertTrue(aligned.last().timestampMs > aligned[aligned.size - 2].timestampMs)
        assertTrue(aligned.last().timestampMs < 60_000L)
    }
}
