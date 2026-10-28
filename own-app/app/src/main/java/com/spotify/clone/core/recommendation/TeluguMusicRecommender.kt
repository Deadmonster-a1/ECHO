package com.spotify.clone.core.recommendation

import android.content.Context
import android.content.SharedPreferences
import com.spotify.clone.core.data.SampleCatalog
import com.spotify.clone.core.model.Artist
import com.spotify.clone.core.model.EditorialBanner
import com.spotify.clone.core.model.Playlist
import com.spotify.clone.core.model.Track
import java.util.Calendar

/**
 * Intelligent context-aware recommendation system tailored for Indian & Telugu music lovers.
 *
 * Capabilities:
 * 1. Time-of-Day Contextual Intelligence:
 *    - Morning (5-11 AM): Suprabhatam, Divine Ragas & Gentle Acoustic Melodies
 *    - Afternoon (11 AM-5 PM): Focus Flow, Acoustic Melodies & Midday Pop
 *    - Evening (5-9 PM): Tollywood Prime Mass Beats & High-Energy Anthems
 *    - Night (9 PM-5 AM): Late-Night Telugu Lo-Fi, Soulful Heartstrings & Sid Sriram Ballads
 *
 * 2. Mood & Taste Profiling:
 *    - Melody (Soulful romance: Sid Sriram, Shreya Ghoshal, Hesham Abdul Wahab)
 *    - Mass (High energy commercial beats: Thaman S, Devi Sri Prasad, Anirudh)
 *    - Lo-Fi (Late night slowed, acoustic chill & mellow vibes)
 *    - Indie (Contemporary Telugu indie & folk: Ram Miriyala, Vivek Sagar)
 *    - Devotional (Spiritual peace, Annamayya & sacred mantras)
 *
 * 3. Seed-based similarity scoring (Composer, Artist, Tempo, Genre).
 */
class TeluguMusicRecommender(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("echo_recommendation_prefs", Context.MODE_PRIVATE)

    companion object {
        @Volatile
        private var instance: TeluguMusicRecommender? = null

        fun getInstance(context: Context): TeluguMusicRecommender {
            return instance ?: synchronized(this) {
                instance ?: TeluguMusicRecommender(context.applicationContext).also { instance = it }
            }
        }
    }

    /**
     * Records a play event to refine taste affinities over time.
     */
    fun recordTrackPlay(track: Track) {
        val artistKey = "play_count_artist_${track.artistName.lowercase().trim()}"
        val currentArtistCount = prefs.getInt(artistKey, 0)
        prefs.edit().putInt(artistKey, currentArtistCount + 1).apply()

        val mood = inferMoodForTrack(track)
        val moodKey = "play_count_mood_$mood"
        val currentMoodCount = prefs.getInt(moodKey, 0)
        prefs.edit().putInt(moodKey, currentMoodCount + 1).apply()
    }

    /**
     * Returns the current time of day bucket.
     */
    fun getTimeOfDay(): TimeContext {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..10 -> TimeContext.MORNING
            in 11..16 -> TimeContext.AFTERNOON
            in 17..20 -> TimeContext.EVENING
            else -> TimeContext.NIGHT
        }
    }

    /**
     * Generates a primary Spotlight Editorial Banner based on selected mood and time context.
     */
    fun getRecommendedHero(selectedMood: String): EditorialBanner {
        return when (selectedMood.lowercase()) {
            "melody" -> EditorialBanner(
                id = "hero_sita_ramam",
                eyebrow = "✦ TELUGU MELODY MAGIC",
                title = "Sita Ramam & Soulful Melodies",
                subtitle = "Inthandham, Samayama & timeless Telugu acoustic heartstrings.",
                coverArtUrl = "https://c.saavncdn.com/041/Sita-Ramam-Telugu-Original-Motion-Picture-Soundtrack-Telugu-2022-20220802140738-500x500.jpg",
                targetPlaylistId = "pl_sita_ramam",
                targetQuery = "Sita Ramam Inthandham"
            )
            "mass", "energy" -> EditorialBanner(
                id = "hero_pushpa2",
                eyebrow = "",
                title = "Pushpa 2: The Rule",
                subtitle = "Pushpa Pushpa, Kurchi Madathapetti & thunderous commercial anthems.",
                coverArtUrl = "https://c.saavncdn.com/366/Pushpa-2-The-Rule-Telugu-Telugu-2024-20241205211012-500x500.jpg",
                targetPlaylistId = "pl_pushpa2",
                targetQuery = "Pushpa 2 Pushpa Pushpa"
            )
            "lo-fi", "lofi", "chill" -> EditorialBanner(
                id = "hero_telugu_lofi",
                eyebrow = "✦ LATE NIGHT TELUGU LO-FI",
                title = "Hyderabad Midnight Chill",
                subtitle = "Slowed & reverb acoustics, gentle rain beats & mellow Telugu reverie.",
                coverArtUrl = "https://c.saavncdn.com/307/Samayama-From-Hi-Nanna-Telugu-2023-20230918164922-500x500.jpg",
                targetPlaylistId = "pl_telugu_lofi",
                targetQuery = "Telugu Lofi"
            )
            "indie" -> EditorialBanner(
                id = "hero_telugu_indie",
                eyebrow = "✦ TELUGU INDIE & FOLK WAVE",
                title = "Earthy Roots & Modern Folk",
                subtitle = "Ram Miriyala, Vivek Sagar, Chowraasta & authentic Telugu voices.",
                coverArtUrl = "https://c.saavncdn.com/000/Balagam-Telugu-2023-20230304184334-500x500.jpg",
                targetPlaylistId = "pl_telugu_indie",
                targetQuery = "Ram Miriyala Balagam"
            )
            "devotional" -> EditorialBanner(
                id = "hero_devotional",
                eyebrow = "✦ DIVINE PEACE & SUBLIME RAGAS",
                title = "Suprabhatam & Sacred Mantras",
                subtitle = "Annamayya sankeerthanas, peaceful morning chants & meditative calm.",
                coverArtUrl = "https://c.saavncdn.com/041/Sita-Ramam-Telugu-Original-Motion-Picture-Soundtrack-Telugu-2022-20220802140738-500x500.jpg",
                targetPlaylistId = "pl_devotional",
                targetQuery = "Annamayya Keerthanalu"
            )
            else -> {
                // "All" / Context-Aware Default
                when (getTimeOfDay()) {
                    TimeContext.MORNING -> EditorialBanner(
                        id = "hero_devara_morning",
                        eyebrow = "",
                        title = "Devara: Part 1 Hits",
                        subtitle = "Chuttamalle, Fear Song & Anirudh's grand coastal soundscapes.",
                        coverArtUrl = "https://c.saavncdn.com/313/Devara-Part-1-Telugu-Telugu-2024-20240926171010-500x500.jpg",
                        targetPlaylistId = "pl_devara",
                        targetQuery = "Chuttamalle Devara"
                    )
                    TimeContext.AFTERNOON -> EditorialBanner(
                        id = "hero_kalki",
                        eyebrow = "",
                        title = "Kalki 2898 AD",
                        subtitle = "Bhairava Anthem, Ta Takkara & Santhosh Narayanan's futuristic score.",
                        coverArtUrl = "https://c.saavncdn.com/888/Kalki-2898-Ad-Telugu-Telugu-2024-20240712063717-500x500.jpg",
                        targetPlaylistId = "pl_kalki_2898",
                        targetQuery = "Bhairava Anthem Kalki"
                    )
                    TimeContext.EVENING -> EditorialBanner(
                        id = "hero_pushpa2_evening",
                        eyebrow = "",
                        title = "Pushpa 2: The Rule",
                        subtitle = "Pushpa Pushpa, Sooseki & high-voltage Devi Sri Prasad beats.",
                        coverArtUrl = "https://c.saavncdn.com/366/Pushpa-2-The-Rule-Telugu-Telugu-2024-20241205211012-500x500.jpg",
                        targetPlaylistId = "pl_pushpa2",
                        targetQuery = "Pushpa 2 Pushpa Pushpa"
                    )
                    TimeContext.NIGHT -> EditorialBanner(
                        id = "hero_devara_night",
                        eyebrow = "",
                        title = "Devara: Part 1 Hits",
                        subtitle = "Chuttamalle, Daavudi & Anirudh's soulful ocean melodies.",
                        coverArtUrl = "https://c.saavncdn.com/313/Devara-Part-1-Telugu-Telugu-2024-20240926171010-500x500.jpg",
                        targetPlaylistId = "pl_devara",
                        targetQuery = "Chuttamalle Devara"
                    )
                }
            }
        }
    }

    /**
     * Curates prominent Indian & Telugu movie soundtrack albums for the "Sculptural Telugu Blockbusters" carousel.
     */
    fun getRecommendedTeluguAlbums(selectedMood: String, allPlaylists: List<Playlist>): List<Playlist> {
        val sampleTeluguAlbums = SampleCatalog.samplePlaylists.filter {
            it.id.startsWith("pl_devara") ||
            it.id.startsWith("pl_pushpa2") ||
            it.id.startsWith("pl_kalki") ||
            it.id.startsWith("pl_sita_ramam") ||
            it.id.startsWith("pl_ala_vaikunthapurramuloo") ||
            it.id.startsWith("pl_hi_nanna") ||
            it.id.startsWith("pl_rrr") ||
            it.id.startsWith("pl_guntur_kaaram") ||
            it.id.startsWith("pl_geetha_govindam") ||
            it.id.startsWith("pl_dasara") ||
            it.id.startsWith("pl_kancharapalem")
        }

        if (selectedMood == "All") {
            return sampleTeluguAlbums
        }

        val filtered = sampleTeluguAlbums.filter { album ->
            albumMatchesMood(album, selectedMood)
        }
        return if (filtered.isNotEmpty()) filtered else sampleTeluguAlbums
    }

    /**
     * Curates personalized "Made For You" packs specifically tailored for Telugu music lovers.
     */
    fun getMadeForYouPacks(selectedMood: String): List<Playlist> {
        val allPacks = listOf(
            SampleCatalog.samplePlaylists.find { it.id == "pl_daily_mix_telugu" } ?: SampleCatalog.samplePlaylists.first(),
            SampleCatalog.samplePlaylists.find { it.id == "pl_hyderabad_daily" } ?: SampleCatalog.samplePlaylists.first(),
            SampleCatalog.samplePlaylists.find { it.id == "pl_telugu_melodies" } ?: SampleCatalog.samplePlaylists.first(),
            SampleCatalog.samplePlaylists.find { it.id == "pl_tollywood_mass" } ?: SampleCatalog.samplePlaylists.first(),
            SampleCatalog.samplePlaylists.find { it.id == "pl_telugu_lofi" } ?: SampleCatalog.samplePlaylists.first(),
            SampleCatalog.samplePlaylists.find { it.id == "pl_telugu_indie" } ?: SampleCatalog.samplePlaylists.first()
        )

        return when (selectedMood.lowercase()) {
            "melody" -> allPacks.filter { it.id.contains("melod") || it.id.contains("daily") || it.id.contains("lofi") }
            "mass", "energy" -> allPacks.filter { it.id.contains("mass") || it.id.contains("hyderabad") || it.id.contains("daily") }
            "lo-fi", "lofi", "chill" -> allPacks.filter { it.id.contains("lofi") || it.id.contains("melod") }
            "indie" -> allPacks.filter { it.id.contains("indie") || it.id.contains("hyderabad") }
            else -> allPacks
        }
    }

    /**
     * Ranks tracks based on affinity to Telugu music and user listening context.
     */
    fun rankTracksForRecommendation(tracks: List<Track>, selectedMood: String): List<Track> {
        if (tracks.isEmpty()) return SampleCatalog.sampleTracks

        val scored = tracks.map { track ->
            var score = 0
            val titleLower = track.title.lowercase()
            val artistLower = track.artistName.lowercase()
            val albumLower = track.albumName.lowercase()

            // Telugu artist affinity
            if (artistLower.contains("anirudh") || artistLower.contains("sid sriram") ||
                artistLower.contains("thaman") || artistLower.contains("devi sri prasad") ||
                artistLower.contains("dsp") || artistLower.contains("keeravani") ||
                artistLower.contains("hesham") || artistLower.contains("ram miriyala") ||
                artistLower.contains("shilpa rao") || artistLower.contains("shreya ghoshal") ||
                artistLower.contains("spb") || artistLower.contains("anurag kulkarni")) {
                score += 50
            }

            // Telugu blockbuster title/album affinity
            if (albumLower.contains("devara") || albumLower.contains("pushpa") ||
                albumLower.contains("kalki") || albumLower.contains("sita ramam") ||
                albumLower.contains("vaikunthapurramuloo") || albumLower.contains("hi nanna") ||
                albumLower.contains("guntur kaaram") || albumLower.contains("rrr") ||
                albumLower.contains("geetha govindam") || albumLower.contains("dasara")) {
                score += 40
            }

            // Mood match
            when (selectedMood.lowercase()) {
                "melody" -> {
                    if (titleLower.contains("chuttamalle") || titleLower.contains("inthandham") ||
                        titleLower.contains("samayama") || titleLower.contains("samajavaragamana") ||
                        titleLower.contains("inkem") || titleLower.contains("prema")) {
                        score += 30
                    }
                }
                "mass", "energy" -> {
                    if (titleLower.contains("pushpa") || titleLower.contains("kurchi") ||
                        titleLower.contains("naatu") || titleLower.contains("bhairava") ||
                        titleLower.contains("fear") || titleLower.contains("daavudi")) {
                        score += 30
                    }
                }
                "lo-fi", "lofi", "chill" -> {
                    if (titleLower.contains("chuttamalle") || titleLower.contains("inthandham") ||
                        titleLower.contains("lo-fi") || titleLower.contains("silence")) {
                        score += 30
                    }
                }
                "indie" -> {
                    if (artistLower.contains("ram miriyala") || artistLower.contains("vivek sagar") ||
                        titleLower.contains("oorantha") || albumLower.contains("balagam")) {
                        score += 30
                    }
                }
            }

            track to score
        }

        return scored.sortedByDescending { it.second }.map { it.first }
    }

    private fun inferMoodForTrack(track: Track): String {
        val title = track.title.lowercase()
        val artist = track.artistName.lowercase()
        return when {
            title.contains("chuttamalle") || title.contains("inthandham") || title.contains("samayama") || artist.contains("sid sriram") -> "Melody"
            title.contains("pushpa") || title.contains("kurchi") || title.contains("naatu") || artist.contains("thaman") -> "Mass"
            artist.contains("ram miriyala") -> "Indie"
            else -> "Melody"
        }
    }

    private fun albumMatchesMood(album: Playlist, mood: String): Boolean {
        val name = album.name.lowercase()
        val desc = album.description.lowercase()
        val m = mood.lowercase()
        return when (m) {
            "melody" -> name.contains("sita ramam") || name.contains("hi nanna") || name.contains("ala vaikunthapurramuloo") || name.contains("geetha govindam") || desc.contains("melod") || desc.contains("romance")
            "mass", "energy" -> name.contains("pushpa") || name.contains("devara") || name.contains("guntur") || name.contains("rrr") || name.contains("kalki") || desc.contains("mass") || desc.contains("beat")
            "lo-fi", "lofi", "chill" -> name.contains("sita ramam") || desc.contains("ambient") || desc.contains("lo-fi") || desc.contains("soulful")
            "indie" -> name.contains("kancharapalem") || desc.contains("indie") || desc.contains("folk")
            "devotional" -> desc.contains("divine") || desc.contains("sacred")
            else -> true
        }
    }
}

enum class TimeContext {
    MORNING,
    AFTERNOON,
    EVENING,
    NIGHT
}
