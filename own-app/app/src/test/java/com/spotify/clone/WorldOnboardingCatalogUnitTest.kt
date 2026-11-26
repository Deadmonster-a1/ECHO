package com.spotify.clone

import com.spotify.clone.core.data.SampleCatalog
import com.spotify.clone.core.data.WorldOnboardingCatalog
import com.spotify.clone.core.data.WorldRegion
import com.spotify.clone.core.model.MusicGenre
import com.spotify.clone.core.model.Track
import com.spotify.clone.core.recommendation.EchoRecommendationEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorldOnboardingCatalogUnitTest {

    @Test
    fun worldCatalog_coversAllIndianLanguagesEnglishAndGlobalRegions() {
        val languages = WorldOnboardingCatalog.ALL_LANGUAGES
        val expectedIndian = listOf(
            "Hindi", "Telugu", "Tamil", "Punjabi", "Malayalam",
            "Kannada", "Marathi", "Bengali", "Gujarati", "Bhojpuri", "Urdu"
        )
        val expectedWorld = listOf(
            "English", "Spanish", "Korean", "Japanese", "Arabic", "French", "Portuguese", "Global"
        )
        for (lang in expectedIndian + expectedWorld) {
            assertTrue("Missing language $lang", lang in languages)
            val artistsForLang = WorldOnboardingCatalog.ALL_ARTISTS.filter { lang in it.languages }
            assertTrue("Expected artists for language $lang", artistsForLang.isNotEmpty())
            val songsForLang = WorldOnboardingCatalog.ALL_SONGS.filter { it.language == lang }
            assertTrue("Expected songs for language $lang", songsForLang.isNotEmpty())
        }
    }

    @Test
    fun worldCatalog_containsAllFamousArtistsAndDistinctNames() {
        val artistNames = WorldOnboardingCatalog.ALL_ARTISTS.map { it.name.lowercase() }
        assertEquals(artistNames.size, artistNames.distinct().size)
        assertTrue("Expected at least 80 curated world artists", WorldOnboardingCatalog.ALL_ARTISTS.size >= 80)

        val expectedFamous = listOf(
            "Arijit Singh", "Anirudh Ravichander", "Sid Sriram", "A.R. Rahman",
            "Diljit Dosanjh", "Shreya Ghoshal", "The Weeknd", "Taylor Swift",
            "Kendrick Lamar", "Coldplay", "BTS", "Bad Bunny", "YOASOBI", "Stromae"
        )
        for (name in expectedFamous) {
            assertTrue(
                "Missing famous artist $name",
                WorldOnboardingCatalog.ALL_ARTISTS.any { it.name.equals(name, ignoreCase = true) }
            )
        }
    }

    @Test
    fun worldCatalog_everyArtistHasValidHttpsImage() {
        val missing = WorldOnboardingCatalog.ALL_ARTISTS.filter {
            it.avatarUrl.isBlank() || !it.avatarUrl.startsWith("https://")
        }
        assertTrue(
            "Every onboarding artist must have an HTTPS avatarUrl; missing: ${missing.map { it.name }}",
            missing.isEmpty()
        )
    }

    @Test
    fun optimizeArtistsForUser_prioritizesSelectedSongsAndRelatedArtistsPerUser() {
        // User A: Picks Telugu song "Chuttamalle" (Shilpa Rao, Anirudh Ravichander)
        val chuttamalle = SampleCatalog.trackChuttamalle
        val userARecs = WorldOnboardingCatalog.optimizeArtistsForUser(
            selectedLanguages = setOf("Telugu"),
            selectedGenreIds = setOf(MusicGenre.MELODIES.id),
            selectedSongTracks = listOf(chuttamalle),
            selectedArtists = emptySet()
        )
        val userATop5 = userARecs.take(5).map { it.artist.name }
        assertTrue("Direct song artists should be ranked in top 5: $userATop5", "Anirudh Ravichander" in userATop5)
        assertTrue("Direct song artists should be ranked in top 5: $userATop5", "Shilpa Rao" in userATop5)
        assertTrue(
            "Reason tag should cite selected song",
            userARecs.first { it.artist.name == "Anirudh Ravichander" }.reasonTag.contains("Chuttamalle")
        )

        // User B: Picks English Pop/Rock songs ("Blinding Lights", "Viva La Vida")
        val blindingLights = WorldOnboardingCatalog.ALL_SONGS.first { it.title == "Blinding Lights" }.toTrack()
        val vivaLaVida = WorldOnboardingCatalog.ALL_SONGS.first { it.title == "Viva La Vida" }.toTrack()
        val userBRecs = WorldOnboardingCatalog.optimizeArtistsForUser(
            selectedLanguages = setOf("English"),
            selectedGenreIds = setOf(MusicGenre.GLOBAL_POP.id, MusicGenre.ROCK.id),
            selectedSongTracks = listOf(blindingLights, vivaLaVida),
            selectedArtists = emptySet()
        )
        val userBTop5 = userBRecs.take(5).map { it.artist.name }
        assertTrue("User B top 5 should include The Weeknd: $userBTop5", "The Weeknd" in userBTop5)
        assertTrue("User B top 5 should include Coldplay: $userBTop5", "Coldplay" in userBTop5)
        assertNotEquals("Different users must receive different top artist rankings", userATop5, userBTop5)
    }

    @Test
    fun getSimilarArtistsStrip_adaptsToSelectedArtistsAndExcludesAlreadyPicked() {
        val pickedSongs = listOf(
            WorldOnboardingCatalog.ALL_SONGS.first { it.title == "Kesariya" }.toTrack()
        )
        val pickedArtists = setOf("Arijit Singh", "Pritam")
        val strip = WorldOnboardingCatalog.getSimilarArtistsStrip(
            selectedLanguages = setOf("Hindi"),
            selectedGenreIds = setOf(MusicGenre.BOLLYWOOD.id, MusicGenre.MELODIES.id),
            selectedSongTracks = pickedSongs,
            selectedArtists = pickedArtists,
            limit = 8
        )
        assertTrue(strip.isNotEmpty())
        val stripNames = strip.map { it.artist.name }
        assertFalse("Already selected artists must not appear in suggestion strip", "Arijit Singh" in stripNames)
        assertFalse("Already selected artists must not appear in suggestion strip", "Pritam" in stripNames)
        assertTrue(
            "Related Hindi melody artists should surface in strip: $stripNames",
            stripNames.any { it in setOf("Shreya Ghoshal", "Shilpa Rao", "Atif Aslam", "A.R. Rahman", "Mithoon") }
        )
    }

    @Test
    fun optimizeArtistsForUser_supportsLiveSearchAndUnknownSongArtists() {
        val customTrack = Track(
            id = "live_custom_1",
            title = "Custom Global Hit",
            artistName = "Indie World Collective, Anuv Jain",
            albumName = "Global Sessions",
            coverArtUrl = "",
            durationMs = 200_000L,
            mediaUri = "",
            genre = "Indie"
        )
        val recs = WorldOnboardingCatalog.optimizeArtistsForUser(
            selectedLanguages = setOf("Hindi", "English"),
            selectedGenreIds = setOf(MusicGenre.INDIE.id),
            selectedSongTracks = listOf(customTrack),
            selectedArtists = emptySet()
        )
        val topNames = recs.take(6).map { it.artist.name }
        assertTrue("Song-derived unknown artist should be surfaced: $topNames", "Indie World Collective" in topNames)
        assertTrue("Featured known artist should be surfaced: $topNames", "Anuv Jain" in topNames)

        val koreanOnly = WorldOnboardingCatalog.optimizeArtistsForUser(
            selectedLanguages = setOf("Korean"),
            selectedGenreIds = setOf(MusicGenre.GLOBAL_POP.id),
            selectedSongTracks = emptyList(),
            selectedArtists = emptySet(),
            query = "BTS",
            regionFilter = WorldRegion.WORLD
        )
        assertEquals("BTS", koreanOnly.first().artist.name)
    }

    @Test
    fun onboardingSeeding_withSelectedSongsDrivesOptimizedHomeArtistsAndRecommendations() {
        EchoRecommendationEngine.clearTasteSignals()
        val kpopSong = WorldOnboardingCatalog.ALL_SONGS.first { it.title == "Dynamite" }.toTrack()
        val hindiSong = WorldOnboardingCatalog.ALL_SONGS.first { it.title == "Kesariya" }.toTrack()

        EchoRecommendationEngine.seedOnboardingPreferences(
            languages = setOf("Korean", "Hindi"),
            genreIds = setOf(MusicGenre.GLOBAL_POP.id, MusicGenre.MELODIES.id),
            artists = setOf("BTS", "Arijit Singh", "Shreya Ghoshal"),
            songIds = setOf(kpopSong.id, hindiSong.id),
            songTracks = listOf(kpopSong, hindiSong)
        )

        val homeArtists = EchoRecommendationEngine.getOptimizedHomeArtists(
            recentlyPlayed = emptyList(),
            favorites = emptyList(),
            limit = 10
        ).map { it.name }

        assertTrue("Home artists should include user's onboarded BTS: $homeArtists", "BTS" in homeArtists)
        assertTrue("Home artists should include user's onboarded Arijit Singh: $homeArtists", "Arijit Singh" in homeArtists)
        assertTrue("Home artists should include song-derived Pritam: $homeArtists", "Pritam" in homeArtists)

        val recs = EchoRecommendationEngine.generateHomeRecommendations(
            recentlyPlayed = emptyList(),
            favorites = emptyList(),
            catalogPool = WorldOnboardingCatalog.allOnboardingTracks(),
            limit = 10
        )
        assertTrue(recs.isNotEmpty())
        assertTrue(
            "Recommendations should include world/Indian matches",
            recs.any { it.artistName.contains("Arijit", ignoreCase = true) || it.artistName.contains("BTS", ignoreCase = true) }
        )
    }

    @Test
    fun contextualBanner_adaptsToIndianAndGlobalUserPreferences() {
        EchoRecommendationEngine.clearTasteSignals()
        EchoRecommendationEngine.seedOnboardingPreferences(
            languages = setOf("Hindi", "Punjabi"),
            genreIds = setOf(MusicGenre.BOLLYWOOD.id),
            artists = setOf("Arijit Singh")
        )
        val hindiBanner = EchoRecommendationEngine.getContextualBanner(
            com.spotify.clone.core.recommendation.TimeOfDayBucket.MORNING
        )
        assertTrue("Expected Hindi contextual banner, got: ${hindiBanner.title}", hindiBanner.title.contains("Bollywood", ignoreCase = true))

        EchoRecommendationEngine.clearTasteSignals()
        EchoRecommendationEngine.seedOnboardingPreferences(
            languages = setOf("English", "Korean"),
            genreIds = setOf(MusicGenre.GLOBAL_POP.id),
            artists = setOf("The Weeknd", "BTS")
        )
        val globalBanner = EchoRecommendationEngine.getContextualBanner(
            com.spotify.clone.core.recommendation.TimeOfDayBucket.AFTERNOON
        )
        assertTrue("Expected Global contextual banner, got: ${globalBanner.title}", globalBanner.title.contains("Global", ignoreCase = true))
    }

    @Test
    fun toggleFollowedArtist_updatesSeededArtistsForRecommendations() {
        EchoRecommendationEngine.clearTasteSignals()
        EchoRecommendationEngine.toggleFollowedArtist("Taylor Swift", nowFollowed = true)
        assertTrue("taylor swift" in EchoRecommendationEngine.getOnboardedArtists())
        EchoRecommendationEngine.toggleFollowedArtist("Taylor Swift", nowFollowed = false)
        assertFalse("taylor swift" in EchoRecommendationEngine.getOnboardedArtists())
    }
}
