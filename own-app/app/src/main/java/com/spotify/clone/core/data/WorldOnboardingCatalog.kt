package com.spotify.clone.core.data

import com.spotify.clone.core.model.Artist
import com.spotify.clone.core.model.MusicGenre
import com.spotify.clone.core.model.Track
import com.spotify.clone.core.recommendation.EchoRecommendationEngine
import com.spotify.clone.core.recommendation.EchoTasteMlModel
import java.util.Locale

enum class WorldRegion(val label: String) {
    ALL("All"),
    INDIA("India"),
    ENGLISH("English"),
    WORLD("World")
}

data class OnboardingArtist(
    val name: String,
    val avatarUrl: String = "",
    val languages: Set<String>,
    val genreIds: Set<String>,
    val region: WorldRegion,
    val popularityScore: Int = 80,
    val relatedArtists: Set<String> = emptySet(),
    val accentHex: Long = 0xFF6C63FF
) {
    fun toArtist(): Artist = Artist(
        id = "onb_art_${name.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]+"), "_").trim('_')}",
        name = name,
        avatarUrl = avatarUrl,
        monthlyListeners = popularityScore.toLong() * 500_000L,
        isVerified = true
    )
}

data class OnboardingSong(
    val id: String,
    val title: String,
    val artistName: String,
    val albumName: String,
    val coverArtUrl: String = "",
    val language: String,
    val genreId: String,
    val region: WorldRegion,
    val popularityScore: Int = 85,
    val durationMs: Long = 215_000L
) {
    fun toTrack(): Track = Track(
        id = id,
        title = title,
        artistName = artistName,
        albumName = albumName,
        coverArtUrl = coverArtUrl,
        durationMs = durationMs,
        mediaUri = "",
        genre = MusicGenre.fromId(genreId).shortLabel
    )
}

data class OptimizedArtistRecommendation(
    val artist: OnboardingArtist,
    val score: Double,
    val reasonTag: String
)

/**
 * World-view onboarding catalog and per-user artist/song optimization engine.
 *
 * Covers Indian languages (Hindi, Telugu, Tamil, Punjabi, Malayalam, Kannada, Marathi,
 * Bengali, Gujarati, Bhojpuri, Urdu), English, and famous worldwide scenes (Spanish/Latin,
 * Korean/K-Pop, Japanese/J-Pop, Arabic, French, Portuguese, Global).
 */
object WorldOnboardingCatalog {

    val INDIAN_LANGUAGES: List<String> = listOf(
        "Hindi",
        "Telugu",
        "Tamil",
        "Punjabi",
        "Malayalam",
        "Kannada",
        "Marathi",
        "Bengali",
        "Gujarati",
        "Bhojpuri",
        "Urdu"
    )

    val WORLD_LANGUAGES: List<String> = listOf(
        "English",
        "Spanish",
        "Korean",
        "Japanese",
        "Arabic",
        "French",
        "Portuguese",
        "Global"
    )

    val ALL_LANGUAGES: List<String> = listOf(
        "English",
        "Hindi",
        "Telugu",
        "Tamil",
        "Punjabi",
        "Malayalam",
        "Kannada",
        "Marathi",
        "Bengali",
        "Gujarati",
        "Bhojpuri",
        "Urdu",
        "Spanish",
        "Korean",
        "Japanese",
        "Arabic",
        "French",
        "Portuguese",
        "Global"
    )

    private const val ART_DEVARA = "https://c.saavncdn.com/313/Devara-Part-1-Telugu-Telugu-2024-20240926171010-500x500.jpg"
    private const val ART_PUSHPA2 = "https://c.saavncdn.com/366/Pushpa-2-The-Rule-Telugu-Telugu-2024-20241205211012-500x500.jpg"
    private const val ART_KALKI = "https://c.saavncdn.com/888/Kalki-2898-Ad-Telugu-Telugu-2024-20240712063717-500x500.jpg"
    private const val ART_GUNTUR = "https://c.saavncdn.com/000/Guntur-Kaaram-Telugu-2023-20240126145901-500x500.jpg"
    private const val ART_ALVP = "https://c.saavncdn.com/517/Ala-Vaikunthapurramuloo-Telugu-2019-20200116144338-500x500.jpg"
    private const val ART_SITA_RAMAM = "https://c.saavncdn.com/041/Sita-Ramam-Telugu-Original-Motion-Picture-Soundtrack-Telugu-2022-20220802140738-500x500.jpg"
    private const val ART_RRR = "https://c.saavncdn.com/683/RRR-Telugu-Telugu-2022-20250828171313-500x500.jpg"
    private const val ART_HI_NANNA = "https://c.saavncdn.com/253/Hi-Nanna-Telugu-Telugu-2023-20240109191037-500x500.jpg"
    private const val ART_GEETHA = "https://c.saavncdn.com/237/Geetha-Govindam-Telugu-2018-20180921-500x500.jpg"
    private const val ART_BRAHMASTRA = "https://c.saavncdn.com/871/Brahmastra-Original-Motion-Picture-Soundtrack-Hindi-2022-20221006155213-500x500.jpg"
    private const val ART_WEEKND = "https://is1-ssl.mzstatic.com/image/thumb/Music125/v4/49/3d/ab/493dab54-f920-9043-6181-80993b8116c9/19UMGIM53909.rgb.jpg/600x600bb.jpg"
    private const val ART_ANIRUDH = "https://c.saavncdn.com/artists/Anirudh_Ravichander_003_20260121134149_500x500.jpg"
    private const val ART_DSP = "https://c.saavncdn.com/artists/Devi_Sri_Prasad_008_20250619062824_500x500.jpg"
    private const val ART_THAMAN = "https://c.saavncdn.com/artists/Thaman_S__007_20231106094011_500x500.jpg"
    private const val ART_SID_SRIRAM = "https://c.saavncdn.com/artists/Sid_Sriram_005_20240425180600_500x500.jpg"
    private const val ART_KEERAVANI = "https://c.saavncdn.com/artists/M__M__Keeravani_002_20240129101710_500x500.jpg"

    val ALL_ARTISTS: List<OnboardingArtist> by lazy {
        listOf(
            // --- TELUGU / SOUTH INDIA ---
            OnboardingArtist(
                name = "Anirudh Ravichander",
                avatarUrl = ART_ANIRUDH,
                languages = setOf("Telugu", "Tamil", "Hindi"),
                genreIds = setOf(MusicGenre.TOL_TELUGU.id, MusicGenre.MASS_BEATS.id, MusicGenre.MELODIES.id),
                region = WorldRegion.INDIA,
                popularityScore = 99,
                relatedArtists = setOf("S.S. Thaman", "Devi Sri Prasad", "Shilpa Rao", "Sid Sriram", "A.R. Rahman", "Santhosh Narayanan"),
                accentHex = 0xFFE55B3C
            ),
            OnboardingArtist(
                name = "Sid Sriram",
                avatarUrl = ART_SID_SRIRAM,
                languages = setOf("Telugu", "Tamil", "Malayalam", "Kannada"),
                genreIds = setOf(MusicGenre.MELODIES.id, MusicGenre.TOL_TELUGU.id, MusicGenre.INDIE.id),
                region = WorldRegion.INDIA,
                popularityScore = 96,
                relatedArtists = setOf("A.R. Rahman", "Anirudh Ravichander", "Shreya Ghoshal", "Hesham Abdul Wahab", "S.S. Thaman"),
                accentHex = 0xFF7B61FF
            ),
            OnboardingArtist(
                name = "Devi Sri Prasad",
                avatarUrl = ART_DSP,
                languages = setOf("Telugu", "Tamil", "Hindi"),
                genreIds = setOf(MusicGenre.TOL_TELUGU.id, MusicGenre.MASS_BEATS.id),
                region = WorldRegion.INDIA,
                popularityScore = 95,
                relatedArtists = setOf("S.S. Thaman", "Anirudh Ravichander", "Shreya Ghoshal", "M.M. Keeravani"),
                accentHex = 0xFFFF6B35
            ),
            OnboardingArtist(
                name = "S.S. Thaman",
                avatarUrl = ART_THAMAN,
                languages = setOf("Telugu", "Tamil"),
                genreIds = setOf(MusicGenre.TOL_TELUGU.id, MusicGenre.MASS_BEATS.id, MusicGenre.MELODIES.id),
                region = WorldRegion.INDIA,
                popularityScore = 95,
                relatedArtists = setOf("Devi Sri Prasad", "Anirudh Ravichander", "Sid Sriram", "Armaan Malik", "Anurag Kulkarni"),
                accentHex = 0xFFE84393
            ),
            OnboardingArtist(
                name = "M.M. Keeravani",
                avatarUrl = ART_KEERAVANI,
                languages = setOf("Telugu", "Hindi", "Tamil"),
                genreIds = setOf(MusicGenre.TOL_TELUGU.id, MusicGenre.MELODIES.id, MusicGenre.DEVOTIONAL.id),
                region = WorldRegion.INDIA,
                popularityScore = 94,
                relatedArtists = setOf("Kaala Bhairava", "A.R. Rahman", "S.P. Balasubrahmanyam", "Devi Sri Prasad"),
                accentHex = 0xFFD97706
            ),
            OnboardingArtist(
                name = "Hesham Abdul Wahab",
                avatarUrl = "https://c.saavncdn.com/artists/Hesham_Abdul_Wahab_001_20220919094035_500x500.jpg",
                languages = setOf("Telugu", "Malayalam"),
                genreIds = setOf(MusicGenre.MELODIES.id, MusicGenre.TOL_TELUGU.id, MusicGenre.INDIE.id),
                region = WorldRegion.INDIA,
                popularityScore = 88,
                relatedArtists = setOf("Sid Sriram", "Vishal Chandrashekhar", "Vineeth Sreenivasan", "Sushin Shyam"),
                accentHex = 0xFF14B8A6
            ),
            OnboardingArtist(
                name = "Santhosh Narayanan",
                avatarUrl = "https://c.saavncdn.com/artists/Santhosh_Narayanan_002_20250527101718_500x500.jpg",
                languages = setOf("Tamil", "Telugu"),
                genreIds = setOf(MusicGenre.TOL_TELUGU.id, MusicGenre.MASS_BEATS.id, MusicGenre.INDIE.id),
                region = WorldRegion.INDIA,
                popularityScore = 90,
                relatedArtists = setOf("Anirudh Ravichander", "A.R. Rahman", "Yuvan Shankar Raja", "Diljit Dosanjh"),
                accentHex = 0xFF6366F1
            ),
            OnboardingArtist(
                name = "Vishal Chandrashekhar",
                avatarUrl = "https://c.saavncdn.com/artists/Vishal_Chandrashekhar_500x500.jpg",
                languages = setOf("Telugu", "Tamil"),
                genreIds = setOf(MusicGenre.MELODIES.id, MusicGenre.TOL_TELUGU.id),
                region = WorldRegion.INDIA,
                popularityScore = 84,
                relatedArtists = setOf("Hesham Abdul Wahab", "Sid Sriram", "S.P. Charan", "M.M. Keeravani"),
                accentHex = 0xFFEC4899
            ),
            OnboardingArtist(
                name = "Anurag Kulkarni",
                avatarUrl = "https://c.saavncdn.com/artists/Anurag_Kulkarni_004_20251029091123_500x500.jpg",
                languages = setOf("Telugu", "Kannada"),
                genreIds = setOf(MusicGenre.TOL_TELUGU.id, MusicGenre.MELODIES.id),
                region = WorldRegion.INDIA,
                popularityScore = 85,
                relatedArtists = setOf("S.S. Thaman", "Ram Miriyala", "Sid Sriram", "Mangli"),
                accentHex = 0xFF3B82F6
            ),
            OnboardingArtist(
                name = "Ram Miriyala",
                avatarUrl = "https://c.saavncdn.com/artists/Ram_Miriyala_001_20250911063558_500x500.jpg",
                languages = setOf("Telugu"),
                genreIds = setOf(MusicGenre.INDIE.id, MusicGenre.TOL_TELUGU.id),
                region = WorldRegion.INDIA,
                popularityScore = 84,
                relatedArtists = setOf("Vivek Sagar", "Anurag Kulkarni", "Mangli", "Sid Sriram"),
                accentHex = 0xFF8B5CF6
            ),
            OnboardingArtist(
                name = "Kaala Bhairava",
                avatarUrl = "https://c.saavncdn.com/artists/Kaala_Bhairava_20181003123414_500x500.jpg",
                languages = setOf("Telugu"),
                genreIds = setOf(MusicGenre.TOL_TELUGU.id, MusicGenre.MASS_BEATS.id),
                region = WorldRegion.INDIA,
                popularityScore = 85,
                relatedArtists = setOf("M.M. Keeravani", "Rahul Sipligunj", "Anirudh Ravichander"),
                accentHex = 0xFFEF4444
            ),
            OnboardingArtist(
                name = "S.P. Balasubrahmanyam",
                avatarUrl = "https://c.saavncdn.com/artists/S_P_Balasubrahmanyam_500x500.jpg",
                languages = setOf("Telugu", "Tamil", "Kannada", "Hindi", "Malayalam"),
                genreIds = setOf(MusicGenre.MELODIES.id, MusicGenre.DEVOTIONAL.id),
                region = WorldRegion.INDIA,
                popularityScore = 95,
                relatedArtists = setOf("Ilaiyaraaja", "K.S. Chithra", "K.J. Yesudas", "M.M. Keeravani", "Lata Mangeshkar"),
                accentHex = 0xFFF59E0B
            ),
            OnboardingArtist(
                name = "K.S. Chithra",
                avatarUrl = "https://c.saavncdn.com/artists/K_S_Chithra_002_20190906071921_500x500.jpg",
                languages = setOf("Malayalam", "Tamil", "Telugu", "Kannada", "Hindi"),
                genreIds = setOf(MusicGenre.MELODIES.id, MusicGenre.DEVOTIONAL.id),
                region = WorldRegion.INDIA,
                popularityScore = 91,
                relatedArtists = setOf("S.P. Balasubrahmanyam", "K.J. Yesudas", "Ilaiyaraaja", "A.R. Rahman", "Shreya Ghoshal"),
                accentHex = 0xFFEC4899
            ),
            OnboardingArtist(
                name = "Vivek Sagar",
                avatarUrl = "https://c.saavncdn.com/artists/Vivek_Sagar_001_20180706155214_500x500.jpg",
                languages = setOf("Telugu"),
                genreIds = setOf(MusicGenre.INDIE.id, MusicGenre.LO_FI.id),
                region = WorldRegion.INDIA,
                popularityScore = 80,
                relatedArtists = setOf("Ram Miriyala", "Prateek Kuhad", "Anuv Jain"),
                accentHex = 0xFF10B981
            ),
            OnboardingArtist(
                name = "Gopi Sundar",
                avatarUrl = "https://c.saavncdn.com/artists/Gopi_Sundar_001_20190827112215_500x500.jpg",
                languages = setOf("Malayalam", "Telugu"),
                genreIds = setOf(MusicGenre.MELODIES.id, MusicGenre.TOL_TELUGU.id),
                region = WorldRegion.INDIA,
                popularityScore = 84,
                relatedArtists = setOf("Sid Sriram", "Hesham Abdul Wahab", "Vineeth Sreenivasan"),
                accentHex = 0xFF06B6D4
            ),
            OnboardingArtist(
                name = "Mangli",
                avatarUrl = "https://c.saavncdn.com/artists/Mangli_Satyavathi_001_20240701121028_500x500.jpg",
                languages = setOf("Telugu"),
                genreIds = setOf(MusicGenre.MASS_BEATS.id, MusicGenre.TOL_TELUGU.id, MusicGenre.DEVOTIONAL.id),
                region = WorldRegion.INDIA,
                popularityScore = 82,
                relatedArtists = setOf("Anurag Kulkarni", "Ram Miriyala", "Devi Sri Prasad"),
                accentHex = 0xFFF97316
            ),

            // --- HINDI / BOLLYWOOD / INDIAN INDIE & HIP-HOP ---
            OnboardingArtist(
                name = "Arijit Singh",
                avatarUrl = "https://c.saavncdn.com/artists/Arijit_Singh_004_20241118063717_500x500.jpg",
                languages = setOf("Hindi", "Bengali", "Marathi", "Urdu"),
                genreIds = setOf(MusicGenre.BOLLYWOOD.id, MusicGenre.MELODIES.id, MusicGenre.LO_FI.id),
                region = WorldRegion.INDIA,
                popularityScore = 100,
                relatedArtists = setOf("Pritam", "Shreya Ghoshal", "Shilpa Rao", "Atif Aslam", "A.R. Rahman", "Mithoon", "Jubin Nautiyal"),
                accentHex = 0xFF6366F1
            ),
            OnboardingArtist(
                name = "Shreya Ghoshal",
                avatarUrl = "https://c.saavncdn.com/artists/Shreya_Ghoshal_007_20241101074144_500x500.jpg",
                languages = setOf("Hindi", "Bengali", "Telugu", "Tamil", "Kannada", "Malayalam", "Marathi", "Gujarati", "Bhojpuri"),
                genreIds = setOf(MusicGenre.MELODIES.id, MusicGenre.BOLLYWOOD.id, MusicGenre.DEVOTIONAL.id),
                region = WorldRegion.INDIA,
                popularityScore = 99,
                relatedArtists = setOf("Arijit Singh", "Sonu Nigam", "A.R. Rahman", "Shilpa Rao", "Sid Sriram", "Sunidhi Chauhan"),
                accentHex = 0xFFEC4899
            ),
            OnboardingArtist(
                name = "A.R. Rahman",
                avatarUrl = "https://c.saavncdn.com/artists/AR_Rahman_002_20210120084455_500x500.jpg",
                languages = setOf("Hindi", "Tamil", "Telugu", "Malayalam", "English", "Urdu"),
                genreIds = setOf(MusicGenre.BOLLYWOOD.id, MusicGenre.MELODIES.id, MusicGenre.DEVOTIONAL.id, MusicGenre.INDIE.id),
                region = WorldRegion.INDIA,
                popularityScore = 99,
                relatedArtists = setOf("Anirudh Ravichander", "Sid Sriram", "Arijit Singh", "Shreya Ghoshal", "Ilaiyaraaja", "Mohit Chauhan"),
                accentHex = 0xFF8B5CF6
            ),
            OnboardingArtist(
                name = "Pritam",
                avatarUrl = "https://c.saavncdn.com/artists/Pritam_Chakraborty-20170711073326_500x500.jpg",
                languages = setOf("Hindi", "Bengali"),
                genreIds = setOf(MusicGenre.BOLLYWOOD.id, MusicGenre.MELODIES.id, MusicGenre.MASS_BEATS.id),
                region = WorldRegion.INDIA,
                popularityScore = 97,
                relatedArtists = setOf("Arijit Singh", "Shilpa Rao", "Vishal-Shekhar", "Amit Trivedi", "KK", "Mohit Chauhan"),
                accentHex = 0xFF3B82F6
            ),
            OnboardingArtist(
                name = "Shilpa Rao",
                avatarUrl = "https://c.saavncdn.com/artists/Shilpa_Rao_002_20230321111415_500x500.jpg",
                languages = setOf("Hindi", "Telugu", "Tamil"),
                genreIds = setOf(MusicGenre.MELODIES.id, MusicGenre.BOLLYWOOD.id, MusicGenre.TOL_TELUGU.id),
                region = WorldRegion.INDIA,
                popularityScore = 94,
                relatedArtists = setOf("Anirudh Ravichander", "Arijit Singh", "Pritam", "Shreya Ghoshal", "Vishal-Shekhar"),
                accentHex = 0xFFF43F5E
            ),
            OnboardingArtist(
                name = "Sonu Nigam",
                avatarUrl = "https://c.saavncdn.com/artists/Sonu_Nigam_003_20260813182013_500x500.jpg",
                languages = setOf("Hindi", "Kannada", "Bengali", "Marathi", "Bhojpuri"),
                genreIds = setOf(MusicGenre.BOLLYWOOD.id, MusicGenre.MELODIES.id, MusicGenre.DEVOTIONAL.id),
                region = WorldRegion.INDIA,
                popularityScore = 94,
                relatedArtists = setOf("Shreya Ghoshal", "Arijit Singh", "Udit Narayan", "Alka Yagnik", "KK"),
                accentHex = 0xFFF59E0B
            ),
            OnboardingArtist(
                name = "Atif Aslam",
                avatarUrl = "https://cdn-images.dzcdn.net/images/artist/0ea90444148fff9c11d77f06a344724e/500x500-000000-80-0-0.jpg",
                languages = setOf("Hindi", "Urdu", "Punjabi"),
                genreIds = setOf(MusicGenre.BOLLYWOOD.id, MusicGenre.MELODIES.id, MusicGenre.ROCK.id),
                region = WorldRegion.INDIA,
                popularityScore = 93,
                relatedArtists = setOf("Arijit Singh", "Rahat Fateh Ali Khan", "Pritam", "Mithoon"),
                accentHex = 0xFF0EA5E9
            ),
            OnboardingArtist(
                name = "Armaan Malik",
                avatarUrl = "https://c.saavncdn.com/artists/Armaan_Malik_006_20260813132832_500x500.jpg",
                languages = setOf("Hindi", "Telugu", "English", "Kannada", "Marathi"),
                genreIds = setOf(MusicGenre.MELODIES.id, MusicGenre.BOLLYWOOD.id, MusicGenre.GLOBAL_POP.id),
                region = WorldRegion.INDIA,
                popularityScore = 90,
                relatedArtists = setOf("Arijit Singh", "S.S. Thaman", "Darshan Raval", "Ed Sheeran"),
                accentHex = 0xFF6366F1
            ),
            OnboardingArtist(
                name = "Jubin Nautiyal",
                avatarUrl = "https://c.saavncdn.com/artists/Jubin_Nautiyal_003_20231130204020_500x500.jpg",
                languages = setOf("Hindi", "Urdu"),
                genreIds = setOf(MusicGenre.BOLLYWOOD.id, MusicGenre.MELODIES.id, MusicGenre.LO_FI.id),
                region = WorldRegion.INDIA,
                popularityScore = 91,
                relatedArtists = setOf("Arijit Singh", "B Praak", "Darshan Raval", "Tanishk Bagchi"),
                accentHex = 0xFF8B5CF6
            ),
            OnboardingArtist(
                name = "Sunidhi Chauhan",
                avatarUrl = "https://c.saavncdn.com/artists/Sunidhi_Chauhan_005_20250515061617_500x500.jpg",
                languages = setOf("Hindi", "Punjabi", "Marathi"),
                genreIds = setOf(MusicGenre.BOLLYWOOD.id, MusicGenre.MASS_BEATS.id),
                region = WorldRegion.INDIA,
                popularityScore = 91,
                relatedArtists = setOf("Shreya Ghoshal", "Vishal-Shekhar", "Badshah", "Pritam"),
                accentHex = 0xFFEF4444
            ),
            OnboardingArtist(
                name = "Vishal-Shekhar",
                avatarUrl = "https://c.saavncdn.com/artists/Vishal-Shekhar_20191130071357_500x500.jpg",
                languages = setOf("Hindi", "Marathi"),
                genreIds = setOf(MusicGenre.BOLLYWOOD.id, MusicGenre.MASS_BEATS.id),
                region = WorldRegion.INDIA,
                popularityScore = 90,
                relatedArtists = setOf("Pritam", "Shilpa Rao", "Arijit Singh", "Amit Trivedi"),
                accentHex = 0xFFF97316
            ),
            OnboardingArtist(
                name = "Amit Trivedi",
                avatarUrl = "https://c.saavncdn.com/artists/Amit_Trivedi_007_20241118063149_500x500.jpg",
                languages = setOf("Hindi", "Gujarati", "Marathi"),
                genreIds = setOf(MusicGenre.BOLLYWOOD.id, MusicGenre.INDIE.id),
                region = WorldRegion.INDIA,
                popularityScore = 89,
                relatedArtists = setOf("A.R. Rahman", "Pritam", "Sachin-Jigar", "Prateek Kuhad"),
                accentHex = 0xFF14B8A6
            ),
            OnboardingArtist(
                name = "Sachin-Jigar",
                avatarUrl = "https://c.saavncdn.com/artists/Sachin_Jigar_003_20251222093820_500x500.jpg",
                languages = setOf("Hindi", "Gujarati"),
                genreIds = setOf(MusicGenre.BOLLYWOOD.id, MusicGenre.MASS_BEATS.id),
                region = WorldRegion.INDIA,
                popularityScore = 89,
                relatedArtists = setOf("Pritam", "Amit Trivedi", "Jigardan Gadhavi", "Arijit Singh"),
                accentHex = 0xFF06B6D4
            ),
            OnboardingArtist(
                name = "Kishore Kumar",
                avatarUrl = "https://c.saavncdn.com/artists/Kishore_Kumar_500x500.jpg",
                languages = setOf("Hindi", "Bengali"),
                genreIds = setOf(MusicGenre.BOLLYWOOD.id, MusicGenre.MELODIES.id),
                region = WorldRegion.INDIA,
                popularityScore = 95,
                relatedArtists = setOf("Lata Mangeshkar", "Mohammed Rafi", "R.D. Burman", "Sonu Nigam"),
                accentHex = 0xFFD97706
            ),
            OnboardingArtist(
                name = "Lata Mangeshkar",
                avatarUrl = "https://c.saavncdn.com/artists/Lata_Mangeshkar_004_20230623105323_500x500.jpg",
                languages = setOf("Hindi", "Marathi", "Bengali"),
                genreIds = setOf(MusicGenre.BOLLYWOOD.id, MusicGenre.MELODIES.id, MusicGenre.DEVOTIONAL.id),
                region = WorldRegion.INDIA,
                popularityScore = 96,
                relatedArtists = setOf("Kishore Kumar", "Mohammed Rafi", "Asha Bhosle", "S.P. Balasubrahmanyam", "Shreya Ghoshal"),
                accentHex = 0xFFE11D48
            ),
            OnboardingArtist(
                name = "KK",
                avatarUrl = "https://c.saavncdn.com/artists/KK_500x500.jpg",
                languages = setOf("Hindi", "Telugu", "Tamil"),
                genreIds = setOf(MusicGenre.BOLLYWOOD.id, MusicGenre.MELODIES.id, MusicGenre.ROCK.id),
                region = WorldRegion.INDIA,
                popularityScore = 92,
                relatedArtists = setOf("Pritam", "Mohit Chauhan", "Arijit Singh", "Sonu Nigam"),
                accentHex = 0xFF3B82F6
            ),
            OnboardingArtist(
                name = "Mohit Chauhan",
                avatarUrl = "https://c.saavncdn.com/artists/Mohit_Chauhan_500x500.jpg",
                languages = setOf("Hindi"),
                genreIds = setOf(MusicGenre.BOLLYWOOD.id, MusicGenre.INDIE.id, MusicGenre.MELODIES.id),
                region = WorldRegion.INDIA,
                popularityScore = 88,
                relatedArtists = setOf("A.R. Rahman", "KK", "Pritam", "Arijit Singh"),
                accentHex = 0xFF6366F1
            ),
            OnboardingArtist(
                name = "Anuv Jain",
                avatarUrl = "https://c.saavncdn.com/artists/Anuv_Jain_001_20231206073013_500x500.jpg",
                languages = setOf("Hindi", "English"),
                genreIds = setOf(MusicGenre.INDIE.id, MusicGenre.LO_FI.id, MusicGenre.MELODIES.id),
                region = WorldRegion.INDIA,
                popularityScore = 92,
                relatedArtists = setOf("Prateek Kuhad", "When Chai Met Toast", "Arijit Singh", "Ed Sheeran"),
                accentHex = 0xFF10B981
            ),
            OnboardingArtist(
                name = "Prateek Kuhad",
                avatarUrl = "https://c.saavncdn.com/artists/Prateek_Kuhad_006_20260515064251_500x500.jpg",
                languages = setOf("Hindi", "English"),
                genreIds = setOf(MusicGenre.INDIE.id, MusicGenre.LO_FI.id),
                region = WorldRegion.INDIA,
                popularityScore = 88,
                relatedArtists = setOf("Anuv Jain", "When Chai Met Toast", "Cigarettes After Sex", "Lauv"),
                accentHex = 0xFF0D9488
            ),
            OnboardingArtist(
                name = "When Chai Met Toast",
                avatarUrl = "https://c.saavncdn.com/artists/When_Chai_Met_Toast_000_20231207113242_500x500.jpg",
                languages = setOf("English", "Hindi", "Malayalam", "Tamil"),
                genreIds = setOf(MusicGenre.INDIE.id, MusicGenre.GLOBAL_POP.id),
                region = WorldRegion.INDIA,
                popularityScore = 84,
                relatedArtists = setOf("Anuv Jain", "Prateek Kuhad", "Coldplay", "Raghu Dixit"),
                accentHex = 0xFFF59E0B
            ),
            OnboardingArtist(
                name = "Divine",
                avatarUrl = "https://c.saavncdn.com/artists/DIVINE_006_20250911071442_500x500.jpg",
                languages = setOf("Hindi", "Marathi"),
                genreIds = setOf(MusicGenre.HIP_HOP.id, MusicGenre.BOLLYWOOD.id),
                region = WorldRegion.INDIA,
                popularityScore = 89,
                relatedArtists = setOf("KR\$NA", "Hanumankind", "Raftaar", "Karan Aujla", "Kendrick Lamar"),
                accentHex = 0xFFEF4444
            ),
            OnboardingArtist(
                name = "Hanumankind",
                avatarUrl = "https://c.saavncdn.com/artists/Hanumankind_001_20250313025806_500x500.jpg",
                languages = setOf("English", "Hindi", "Malayalam"),
                genreIds = setOf(MusicGenre.HIP_HOP.id),
                region = WorldRegion.INDIA,
                popularityScore = 91,
                relatedArtists = setOf("Divine", "KR\$NA", "Dabzee", "Travis Scott", "Kendrick Lamar"),
                accentHex = 0xFFDC2626
            ),
            OnboardingArtist(
                name = "KR\$NA",
                avatarUrl = "https://cdn-images.dzcdn.net/images/artist/44dcd9d1d0be03a355c2d93c31bd8015/500x500-000000-80-0-0.jpg",
                languages = setOf("Hindi", "English"),
                genreIds = setOf(MusicGenre.HIP_HOP.id),
                region = WorldRegion.INDIA,
                popularityScore = 86,
                relatedArtists = setOf("Divine", "Raftaar", "Hanumankind", "Eminem"),
                accentHex = 0xFF4F46E5
            ),
            OnboardingArtist(
                name = "Badshah",
                avatarUrl = "https://c.saavncdn.com/artists/Badshah_006_20241118064015_500x500.jpg",
                languages = setOf("Hindi", "Punjabi"),
                genreIds = setOf(MusicGenre.HIP_HOP.id, MusicGenre.MASS_BEATS.id, MusicGenre.BOLLYWOOD.id),
                region = WorldRegion.INDIA,
                popularityScore = 91,
                relatedArtists = setOf("Honey Singh", "Diljit Dosanjh", "King", "Guru Randhawa"),
                accentHex = 0xFFF97316
            ),
            OnboardingArtist(
                name = "King",
                avatarUrl = "https://c.saavncdn.com/artists/King_004_20240819092014_500x500.jpg",
                languages = setOf("Hindi", "Punjabi"),
                genreIds = setOf(MusicGenre.BOLLYWOOD.id, MusicGenre.HIP_HOP.id, MusicGenre.MELODIES.id),
                region = WorldRegion.INDIA,
                popularityScore = 89,
                relatedArtists = setOf("Badshah", "Anuv Jain", "AP Dhillon", "Darshan Raval"),
                accentHex = 0xFFEC4899
            ),
            OnboardingArtist(
                name = "KSHMR",
                avatarUrl = "https://c.saavncdn.com/artists/KSHMR_004_20201125102754_500x500.jpg",
                languages = setOf("English", "Hindi"),
                genreIds = setOf(MusicGenre.ELECTRONIC.id, MusicGenre.GLOBAL_POP.id),
                region = WorldRegion.INDIA,
                popularityScore = 87,
                relatedArtists = setOf("Nucleya", "Martin Garrix", "Alan Walker", "KR\$NA"),
                accentHex = 0xFF0EA5E9
            ),
            OnboardingArtist(
                name = "Nucleya",
                avatarUrl = "https://c.saavncdn.com/artists/Nucleya_003_20260120062856_500x500.jpg",
                languages = setOf("Hindi", "Punjabi"),
                genreIds = setOf(MusicGenre.ELECTRONIC.id, MusicGenre.MASS_BEATS.id),
                region = WorldRegion.INDIA,
                popularityScore = 84,
                relatedArtists = setOf("KSHMR", "Divine", "Ritviz"),
                accentHex = 0xFFF43F5E
            ),

            // --- PUNJABI ---
            OnboardingArtist(
                name = "Diljit Dosanjh",
                avatarUrl = "https://c.saavncdn.com/artists/Diljit_Dosanjh_005_20231025073054_500x500.jpg",
                languages = setOf("Punjabi", "Hindi", "Telugu"),
                genreIds = setOf(MusicGenre.MASS_BEATS.id, MusicGenre.BOLLYWOOD.id, MusicGenre.HIP_HOP.id),
                region = WorldRegion.INDIA,
                popularityScore = 98,
                relatedArtists = setOf("Karan Aujla", "AP Dhillon", "Sidhu Moose Wala", "Shubh", "Badshah", "Ed Sheeran"),
                accentHex = 0xFFF97316
            ),
            OnboardingArtist(
                name = "AP Dhillon",
                avatarUrl = "https://c.saavncdn.com/artists/AP_Dhillon_004_20251023102150_500x500.jpg",
                languages = setOf("Punjabi", "English", "Hindi"),
                genreIds = setOf(MusicGenre.HIP_HOP.id, MusicGenre.GLOBAL_POP.id, MusicGenre.LO_FI.id),
                region = WorldRegion.INDIA,
                popularityScore = 94,
                relatedArtists = setOf("Diljit Dosanjh", "Karan Aujla", "Shubh", "Talwiinder", "The Weeknd"),
                accentHex = 0xFF8B5CF6
            ),
            OnboardingArtist(
                name = "Karan Aujla",
                avatarUrl = "https://c.saavncdn.com/artists/Karan_Aujla_005_20260925061936_500x500.jpg",
                languages = setOf("Punjabi", "Hindi"),
                genreIds = setOf(MusicGenre.HIP_HOP.id, MusicGenre.MASS_BEATS.id),
                region = WorldRegion.INDIA,
                popularityScore = 95,
                relatedArtists = setOf("Diljit Dosanjh", "AP Dhillon", "Sidhu Moose Wala", "Shubh", "Divine"),
                accentHex = 0xFFE11D48
            ),
            OnboardingArtist(
                name = "Sidhu Moose Wala",
                avatarUrl = "https://c.saavncdn.com/artists/Sidhu_Moose_Wala_004_20250617183705_500x500.jpg",
                languages = setOf("Punjabi"),
                genreIds = setOf(MusicGenre.HIP_HOP.id),
                region = WorldRegion.INDIA,
                popularityScore = 95,
                relatedArtists = setOf("Karan Aujla", "Shubh", "Diljit Dosanjh", "AP Dhillon", "Drake"),
                accentHex = 0xFFDC2626
            ),
            OnboardingArtist(
                name = "Shubh",
                avatarUrl = "https://c.saavncdn.com/artists/Shubh_000_20220921112507_500x500.jpg",
                languages = setOf("Punjabi"),
                genreIds = setOf(MusicGenre.HIP_HOP.id, MusicGenre.MASS_BEATS.id),
                region = WorldRegion.INDIA,
                popularityScore = 92,
                relatedArtists = setOf("Sidhu Moose Wala", "Karan Aujla", "AP Dhillon", "Diljit Dosanjh"),
                accentHex = 0xFF4F46E5
            ),
            OnboardingArtist(
                name = "B Praak",
                avatarUrl = "https://c.saavncdn.com/artists/B_Praak_001_20191118112005_500x500.jpg",
                languages = setOf("Punjabi", "Hindi"),
                genreIds = setOf(MusicGenre.MELODIES.id, MusicGenre.BOLLYWOOD.id),
                region = WorldRegion.INDIA,
                popularityScore = 89,
                relatedArtists = setOf("Jubin Nautiyal", "Arijit Singh", "Diljit Dosanjh"),
                accentHex = 0xFFD97706
            ),

            // --- TAMIL ---
            OnboardingArtist(
                name = "Ilaiyaraaja",
                avatarUrl = "https://c.saavncdn.com/artists/Ilaiyaraaja_001_20251020081419_500x500.jpg",
                languages = setOf("Tamil", "Telugu", "Malayalam", "Kannada"),
                genreIds = setOf(MusicGenre.MELODIES.id, MusicGenre.DEVOTIONAL.id),
                region = WorldRegion.INDIA,
                popularityScore = 95,
                relatedArtists = setOf("A.R. Rahman", "S.P. Balasubrahmanyam", "K.S. Chithra", "Yuvan Shankar Raja"),
                accentHex = 0xFFD97706
            ),
            OnboardingArtist(
                name = "Yuvan Shankar Raja",
                avatarUrl = "https://c.saavncdn.com/artists/Yuvan_Shankar_Raja_002_20180802174245_500x500.jpg",
                languages = setOf("Tamil", "Telugu"),
                genreIds = setOf(MusicGenre.MELODIES.id, MusicGenre.MASS_BEATS.id),
                region = WorldRegion.INDIA,
                popularityScore = 91,
                relatedArtists = setOf("Ilaiyaraaja", "Anirudh Ravichander", "Harris Jayaraj", "Santhosh Narayanan"),
                accentHex = 0xFF7C3AED
            ),
            OnboardingArtist(
                name = "Harris Jayaraj",
                avatarUrl = "https://c.saavncdn.com/artists/Harris_Jayaraj_002_20230718071330_500x500.jpg",
                languages = setOf("Tamil", "Telugu"),
                genreIds = setOf(MusicGenre.MELODIES.id, MusicGenre.ELECTRONIC.id),
                region = WorldRegion.INDIA,
                popularityScore = 88,
                relatedArtists = setOf("A.R. Rahman", "Yuvan Shankar Raja", "G.V. Prakash Kumar", "Karthik"),
                accentHex = 0xFF0EA5E9
            ),
            OnboardingArtist(
                name = "G.V. Prakash Kumar",
                avatarUrl = "https://c.saavncdn.com/artists/G_V__Prakash_Kumar_003_20251113063655_500x500.jpg",
                languages = setOf("Tamil", "Telugu"),
                genreIds = setOf(MusicGenre.MELODIES.id, MusicGenre.MASS_BEATS.id),
                region = WorldRegion.INDIA,
                popularityScore = 87,
                relatedArtists = setOf("Anirudh Ravichander", "Dhanush", "Santhosh Narayanan", "Harris Jayaraj"),
                accentHex = 0xFF10B981
            ),
            OnboardingArtist(
                name = "Karthik",
                avatarUrl = "https://c.saavncdn.com/artists/Karthik_500x500.jpg",
                languages = setOf("Tamil", "Telugu", "Malayalam", "Kannada"),
                genreIds = setOf(MusicGenre.MELODIES.id),
                region = WorldRegion.INDIA,
                popularityScore = 86,
                relatedArtists = setOf("Sid Sriram", "A.R. Rahman", "Harris Jayaraj", "Chinmayi"),
                accentHex = 0xFF3B82F6
            ),

            // --- MALAYALAM ---
            OnboardingArtist(
                name = "Sushin Shyam",
                avatarUrl = "https://c.saavncdn.com/artists/Sushin_Shyam_002_20250707125538_500x500.jpg",
                languages = setOf("Malayalam"),
                genreIds = setOf(MusicGenre.INDIE.id, MusicGenre.ELECTRONIC.id, MusicGenre.MASS_BEATS.id),
                region = WorldRegion.INDIA,
                popularityScore = 88,
                relatedArtists = setOf("Hesham Abdul Wahab", "Dabzee", "Santhosh Narayanan", "Shaan Rahman"),
                accentHex = 0xFF14B8A6
            ),
            OnboardingArtist(
                name = "Vineeth Sreenivasan",
                avatarUrl = "https://c.saavncdn.com/artists/Vineeth_Sreenivasan_003_20240508103358_500x500.jpg",
                languages = setOf("Malayalam"),
                genreIds = setOf(MusicGenre.MELODIES.id, MusicGenre.INDIE.id),
                region = WorldRegion.INDIA,
                popularityScore = 86,
                relatedArtists = setOf("Hesham Abdul Wahab", "Shaan Rahman", "K.J. Yesudas"),
                accentHex = 0xFF0EA5E9
            ),
            OnboardingArtist(
                name = "K.J. Yesudas",
                avatarUrl = "https://c.saavncdn.com/artists/KJ_Yesudas_500x500.jpg",
                languages = setOf("Malayalam", "Tamil", "Hindi", "Telugu", "Kannada"),
                genreIds = setOf(MusicGenre.DEVOTIONAL.id, MusicGenre.MELODIES.id),
                region = WorldRegion.INDIA,
                popularityScore = 92,
                relatedArtists = setOf("K.S. Chithra", "S.P. Balasubrahmanyam", "Ilaiyaraaja"),
                accentHex = 0xFFF59E0B
            ),
            OnboardingArtist(
                name = "Dabzee",
                avatarUrl = "https://c.saavncdn.com/artists/Dabzee_000_20230516072112_500x500.jpg",
                languages = setOf("Malayalam"),
                genreIds = setOf(MusicGenre.HIP_HOP.id, MusicGenre.MASS_BEATS.id),
                region = WorldRegion.INDIA,
                popularityScore = 84,
                relatedArtists = setOf("Sushin Shyam", "Hanumankind", "Fejo"),
                accentHex = 0xFFE11D48
            ),

            // --- KANNADA ---
            OnboardingArtist(
                name = "Vijay Prakash",
                avatarUrl = "https://c.saavncdn.com/artists/Vijay_Prakash_007_20250225123208_500x500.jpg",
                languages = setOf("Kannada", "Tamil", "Telugu", "Hindi"),
                genreIds = setOf(MusicGenre.MELODIES.id, MusicGenre.DEVOTIONAL.id),
                region = WorldRegion.INDIA,
                popularityScore = 87,
                relatedArtists = setOf("A.R. Rahman", "Sanjith Hegde", "Raghu Dixit", "Sonu Nigam"),
                accentHex = 0xFFF59E0B
            ),
            OnboardingArtist(
                name = "Sanjith Hegde",
                avatarUrl = "https://c.saavncdn.com/artists/Sanjith_Hegde_002_20230413072406_500x500.jpg",
                languages = setOf("Kannada", "Telugu", "Tamil"),
                genreIds = setOf(MusicGenre.MELODIES.id, MusicGenre.INDIE.id),
                region = WorldRegion.INDIA,
                popularityScore = 86,
                relatedArtists = setOf("Vijay Prakash", "S.S. Thaman", "Santhosh Narayanan", "Charan Raj"),
                accentHex = 0xFF8B5CF6
            ),
            OnboardingArtist(
                name = "Raghu Dixit",
                avatarUrl = "https://c.saavncdn.com/artists/Raghu_Dixit_500x500.jpg",
                languages = setOf("Kannada", "Hindi", "English"),
                genreIds = setOf(MusicGenre.INDIE.id, MusicGenre.ROCK.id),
                region = WorldRegion.INDIA,
                popularityScore = 82,
                relatedArtists = setOf("When Chai Met Toast", "Amit Trivedi", "Vijay Prakash"),
                accentHex = 0xFF10B981
            ),
            OnboardingArtist(
                name = "Ravi Basrur",
                avatarUrl = "https://c.saavncdn.com/artists/Ravi_Basrur_002_20221011072518_500x500.jpg",
                languages = setOf("Kannada", "Telugu", "Hindi"),
                genreIds = setOf(MusicGenre.MASS_BEATS.id, MusicGenre.ROCK.id),
                region = WorldRegion.INDIA,
                popularityScore = 86,
                relatedArtists = setOf("Anirudh Ravichander", "Devi Sri Prasad", "M.M. Keeravani"),
                accentHex = 0xFFDC2626
            ),

            // --- MARATHI / BENGALI / GUJARATI / BHOJPURI / URDU ---
            OnboardingArtist(
                name = "Ajay-Atul",
                avatarUrl = "https://c.saavncdn.com/artists/Ajay_Atul_003_20230228105414_500x500.jpg",
                languages = setOf("Marathi", "Hindi"),
                genreIds = setOf(MusicGenre.MELODIES.id, MusicGenre.MASS_BEATS.id, MusicGenre.BOLLYWOOD.id),
                region = WorldRegion.INDIA,
                popularityScore = 90,
                relatedArtists = setOf("Shreya Ghoshal", "Sonu Nigam", "Amit Trivedi", "A.R. Rahman"),
                accentHex = 0xFFF97316
            ),
            OnboardingArtist(
                name = "Anupam Roy",
                avatarUrl = "https://c.saavncdn.com/artists/Anupam_Roy_007_20250623084828_500x500.jpg",
                languages = setOf("Bengali", "Hindi"),
                genreIds = setOf(MusicGenre.INDIE.id, MusicGenre.MELODIES.id),
                region = WorldRegion.INDIA,
                popularityScore = 84,
                relatedArtists = setOf("Arijit Singh", "Shreya Ghoshal", "Rupam Islam", "Prateek Kuhad"),
                accentHex = 0xFF06B6D4
            ),
            OnboardingArtist(
                name = "Rupam Islam",
                avatarUrl = "https://c.saavncdn.com/artists/Rupam_Islam_004_20200822043703_500x500.jpg",
                languages = setOf("Bengali"),
                genreIds = setOf(MusicGenre.ROCK.id, MusicGenre.INDIE.id),
                region = WorldRegion.INDIA,
                popularityScore = 82,
                relatedArtists = setOf("Anupam Roy", "Kishore Kumar", "Linkin Park"),
                accentHex = 0xFFEF4444
            ),
            OnboardingArtist(
                name = "Aditya Gadhvi",
                avatarUrl = "https://c.saavncdn.com/artists/Aditya_Gadhvi_004_20260902060949_500x500.jpg",
                languages = setOf("Gujarati", "Hindi"),
                genreIds = setOf(MusicGenre.INDIE.id, MusicGenre.MASS_BEATS.id),
                region = WorldRegion.INDIA,
                popularityScore = 85,
                relatedArtists = setOf("Sachin-Jigar", "Amit Trivedi", "Jigardan Gadhavi", "A.R. Rahman"),
                accentHex = 0xFFF59E0B
            ),
            OnboardingArtist(
                name = "Pawan Singh",
                avatarUrl = "https://c.saavncdn.com/artists/Pawan_Singh_003_20241119074737_500x500.jpg",
                languages = setOf("Bhojpuri", "Hindi"),
                genreIds = setOf(MusicGenre.MASS_BEATS.id, MusicGenre.DEVOTIONAL.id),
                region = WorldRegion.INDIA,
                popularityScore = 85,
                relatedArtists = setOf("Khesari Lal Yadav", "Badshah", "Sonu Nigam"),
                accentHex = 0xFFE55B3C
            ),
            OnboardingArtist(
                name = "Khesari Lal Yadav",
                avatarUrl = "https://c.saavncdn.com/artists/Khesari_Lal_Yadav_004_20241119074533_500x500.jpg",
                languages = setOf("Bhojpuri"),
                genreIds = setOf(MusicGenre.MASS_BEATS.id),
                region = WorldRegion.INDIA,
                popularityScore = 83,
                relatedArtists = setOf("Pawan Singh", "Shreya Ghoshal"),
                accentHex = 0xFFEC4899
            ),
            OnboardingArtist(
                name = "Nusrat Fateh Ali Khan",
                avatarUrl = "https://cdn-images.dzcdn.net/images/artist/49ca85b1d9a02da437cdd62c5476553d/500x500-000000-80-0-0.jpg",
                languages = setOf("Urdu", "Punjabi", "Hindi"),
                genreIds = setOf(MusicGenre.DEVOTIONAL.id, MusicGenre.MELODIES.id),
                region = WorldRegion.INDIA,
                popularityScore = 94,
                relatedArtists = setOf("Rahat Fateh Ali Khan", "A.R. Rahman", "Atif Aslam", "Ali Sethi"),
                accentHex = 0xFFD97706
            ),
            OnboardingArtist(
                name = "Rahat Fateh Ali Khan",
                avatarUrl = "https://cdn-images.dzcdn.net/images/artist/8263c6e6e75baf8387ad258459021f78/500x500-000000-80-0-0.jpg",
                languages = setOf("Urdu", "Hindi", "Punjabi"),
                genreIds = setOf(MusicGenre.MELODIES.id, MusicGenre.BOLLYWOOD.id, MusicGenre.DEVOTIONAL.id),
                region = WorldRegion.INDIA,
                popularityScore = 92,
                relatedArtists = setOf("Nusrat Fateh Ali Khan", "Atif Aslam", "Arijit Singh", "Ali Sethi"),
                accentHex = 0xFF8B5CF6
            ),
            OnboardingArtist(
                name = "Ali Sethi",
                avatarUrl = "https://cdn-images.dzcdn.net/images/artist/f2dc6f69fb460209dcbdc1bc47968c29/500x500-000000-80-0-0.jpg",
                languages = setOf("Urdu", "Punjabi", "Hindi"),
                genreIds = setOf(MusicGenre.INDIE.id, MusicGenre.MELODIES.id),
                region = WorldRegion.INDIA,
                popularityScore = 88,
                relatedArtists = setOf("Nusrat Fateh Ali Khan", "Anuv Jain", "Kaifi Khalil", "A.R. Rahman"),
                accentHex = 0xFF14B8A6
            ),

            // --- ENGLISH: POP / R&B ---
            OnboardingArtist(
                name = "The Weeknd",
                avatarUrl = "https://cdn-images.dzcdn.net/images/artist/581693b4724a7fcfa754455101e13a44/500x500-000000-80-0-0.jpg",
                languages = setOf("English", "Global"),
                genreIds = setOf(MusicGenre.GLOBAL_POP.id, MusicGenre.ELECTRONIC.id, MusicGenre.HIP_HOP.id),
                region = WorldRegion.ENGLISH,
                popularityScore = 100,
                relatedArtists = setOf("Drake", "Ariana Grande", "Daft Punk", "Bruno Mars", "Dua Lipa", "Travis Scott", "Lana Del Rey"),
                accentHex = 0xFFE11D48
            ),
            OnboardingArtist(
                name = "Taylor Swift",
                avatarUrl = "https://cdn-images.dzcdn.net/images/artist/cc2495870fe1a792ad0cdb05501ad5ec/500x500-000000-80-0-0.jpg",
                languages = setOf("English", "Global"),
                genreIds = setOf(MusicGenre.GLOBAL_POP.id, MusicGenre.INDIE.id),
                region = WorldRegion.ENGLISH,
                popularityScore = 100,
                relatedArtists = setOf("Olivia Rodrigo", "Sabrina Carpenter", "Ed Sheeran", "Lana Del Rey", "Gracie Abrams", "Billie Eilish"),
                accentHex = 0xFF8B5CF6
            ),
            OnboardingArtist(
                name = "Billie Eilish",
                avatarUrl = "https://cdn-images.dzcdn.net/images/artist/8eab1a9a644889aabaca1e193e05f984/500x500-000000-80-0-0.jpg",
                languages = setOf("English", "Global"),
                genreIds = setOf(MusicGenre.GLOBAL_POP.id, MusicGenre.INDIE.id, MusicGenre.LO_FI.id),
                region = WorldRegion.ENGLISH,
                popularityScore = 98,
                relatedArtists = setOf("Olivia Rodrigo", "Lana Del Rey", "The Weeknd", "Taylor Swift", "Khalid"),
                accentHex = 0xFF10B981
            ),
            OnboardingArtist(
                name = "Bruno Mars",
                avatarUrl = "https://cdn-images.dzcdn.net/images/artist/90f0b5b11df4f87ee878f38569b5995b/500x500-000000-80-0-0.jpg",
                languages = setOf("English", "Global"),
                genreIds = setOf(MusicGenre.GLOBAL_POP.id, MusicGenre.MELODIES.id),
                region = WorldRegion.ENGLISH,
                popularityScore = 98,
                relatedArtists = setOf("The Weeknd", "Lady Gaga", "Dua Lipa", "Ed Sheeran", "Rosé"),
                accentHex = 0xFFF59E0B
            ),
            OnboardingArtist(
                name = "Sabrina Carpenter",
                avatarUrl = "https://cdn-images.dzcdn.net/images/artist/4a9cdc7737e2a0e59b4917b47884b859/500x500-000000-80-0-0.jpg",
                languages = setOf("English", "Global"),
                genreIds = setOf(MusicGenre.GLOBAL_POP.id),
                region = WorldRegion.ENGLISH,
                popularityScore = 96,
                relatedArtists = setOf("Taylor Swift", "Olivia Rodrigo", "Dua Lipa", "Ariana Grande", "Chappell Roan"),
                accentHex = 0xFF38BDF8
            ),
            OnboardingArtist(
                name = "Ariana Grande",
                avatarUrl = "https://cdn-images.dzcdn.net/images/artist/721d8fab84b315502de422b8d0901509/500x500-000000-80-0-0.jpg",
                languages = setOf("English", "Global"),
                genreIds = setOf(MusicGenre.GLOBAL_POP.id),
                region = WorldRegion.ENGLISH,
                popularityScore = 97,
                relatedArtists = setOf("The Weeknd", "Sabrina Carpenter", "Dua Lipa", "SZA", "Justin Bieber"),
                accentHex = 0xFFEC4899
            ),
            OnboardingArtist(
                name = "Dua Lipa",
                avatarUrl = "https://cdn-images.dzcdn.net/images/artist/877872aaf75694f11d53c318700ab2b5/500x500-000000-80-0-0.jpg",
                languages = setOf("English", "Global"),
                genreIds = setOf(MusicGenre.GLOBAL_POP.id, MusicGenre.ELECTRONIC.id),
                region = WorldRegion.ENGLISH,
                popularityScore = 96,
                relatedArtists = setOf("Calvin Harris", "The Weeknd", "Sabrina Carpenter", "Ariana Grande", "Bruno Mars"),
                accentHex = 0xFF6366F1
            ),
            OnboardingArtist(
                name = "Ed Sheeran",
                avatarUrl = "https://cdn-images.dzcdn.net/images/artist/d6bb84390641d8ae9118228d9544e53d/500x500-000000-80-0-0.jpg",
                languages = setOf("English", "Global"),
                genreIds = setOf(MusicGenre.GLOBAL_POP.id, MusicGenre.INDIE.id, MusicGenre.MELODIES.id),
                region = WorldRegion.ENGLISH,
                popularityScore = 97,
                relatedArtists = setOf("Taylor Swift", "Shawn Mendes", "Coldplay", "Justin Bieber", "Diljit Dosanjh"),
                accentHex = 0xFFF97316
            ),
            OnboardingArtist(
                name = "Olivia Rodrigo",
                avatarUrl = "https://cdn-images.dzcdn.net/images/artist/2c9e480317183c037eaebcd7ba96daf4/500x500-000000-80-0-0.jpg",
                languages = setOf("English", "Global"),
                genreIds = setOf(MusicGenre.GLOBAL_POP.id, MusicGenre.ROCK.id),
                region = WorldRegion.ENGLISH,
                popularityScore = 95,
                relatedArtists = setOf("Taylor Swift", "Billie Eilish", "Sabrina Carpenter", "Conan Gray"),
                accentHex = 0xFF7C3AED
            ),
            OnboardingArtist(
                name = "Lady Gaga",
                avatarUrl = "https://cdn-images.dzcdn.net/images/artist/7565262f7661b0d762621a8d69ba6f49/500x500-000000-80-0-0.jpg",
                languages = setOf("English", "Global"),
                genreIds = setOf(MusicGenre.GLOBAL_POP.id, MusicGenre.ELECTRONIC.id),
                region = WorldRegion.ENGLISH,
                popularityScore = 96,
                relatedArtists = setOf("Bruno Mars", "Beyoncé", "Ariana Grande", "Katy Perry"),
                accentHex = 0xFFE11D48
            ),
            OnboardingArtist(
                name = "Rihanna",
                avatarUrl = "https://cdn-images.dzcdn.net/images/artist/f1e0faccaa97f65a505dc42d33236c99/500x500-000000-80-0-0.jpg",
                languages = setOf("English", "Global"),
                genreIds = setOf(MusicGenre.GLOBAL_POP.id, MusicGenre.HIP_HOP.id),
                region = WorldRegion.ENGLISH,
                popularityScore = 97,
                relatedArtists = setOf("Beyoncé", "Drake", "Calvin Harris", "The Weeknd"),
                accentHex = 0xFFDC2626
            ),
            OnboardingArtist(
                name = "Beyoncé",
                avatarUrl = "https://cdn-images.dzcdn.net/images/artist/0aa9d669be4e7310b8647afae37ffaab/500x500-000000-80-0-0.jpg",
                languages = setOf("English", "Global"),
                genreIds = setOf(MusicGenre.GLOBAL_POP.id, MusicGenre.HIP_HOP.id),
                region = WorldRegion.ENGLISH,
                popularityScore = 97,
                relatedArtists = setOf("Rihanna", "Lady Gaga", "SZA", "Jay-Z"),
                accentHex = 0xFFD97706
            ),
            OnboardingArtist(
                name = "Justin Bieber",
                avatarUrl = "https://cdn-images.dzcdn.net/images/artist/fe097f693cebf1f882e3da79e99e3bf9/500x500-000000-80-0-0.jpg",
                languages = setOf("English", "Global"),
                genreIds = setOf(MusicGenre.GLOBAL_POP.id),
                region = WorldRegion.ENGLISH,
                popularityScore = 96,
                relatedArtists = setOf("Ed Sheeran", "Ariana Grande", "Shawn Mendes", "The Kid LAROI", "The Weeknd"),
                accentHex = 0xFF0EA5E9
            ),
            OnboardingArtist(
                name = "Adele",
                avatarUrl = "https://cdn-images.dzcdn.net/images/artist/e5fc443d2abc03b487234ba4de65a001/500x500-000000-80-0-0.jpg",
                languages = setOf("English", "Global"),
                genreIds = setOf(MusicGenre.GLOBAL_POP.id, MusicGenre.MELODIES.id),
                region = WorldRegion.ENGLISH,
                popularityScore = 95,
                relatedArtists = setOf("Sam Smith", "Ed Sheeran", "Lana Del Rey", "Bruno Mars"),
                accentHex = 0xFF475569
            ),
            OnboardingArtist(
                name = "SZA",
                avatarUrl = "https://cdn-images.dzcdn.net/images/artist/8ced041da2bed70d5715f0860956169b/500x500-000000-80-0-0.jpg",
                languages = setOf("English", "Global"),
                genreIds = setOf(MusicGenre.GLOBAL_POP.id, MusicGenre.HIP_HOP.id, MusicGenre.LO_FI.id),
                region = WorldRegion.ENGLISH,
                popularityScore = 95,
                relatedArtists = setOf("Kendrick Lamar", "Frank Ocean", "The Weeknd", "Ariana Grande"),
                accentHex = 0xFF059669
            ),
            OnboardingArtist(
                name = "Lana Del Rey",
                avatarUrl = "https://cdn-images.dzcdn.net/images/artist/8994d3be1a59a72f887f1f8afd2d4c6c/500x500-000000-80-0-0.jpg",
                languages = setOf("English", "Global"),
                genreIds = setOf(MusicGenre.INDIE.id, MusicGenre.GLOBAL_POP.id, MusicGenre.LO_FI.id),
                region = WorldRegion.ENGLISH,
                popularityScore = 95,
                relatedArtists = setOf("Billie Eilish", "Taylor Swift", "The Weeknd", "Cigarettes After Sex"),
                accentHex = 0xFF64748B
            ),
            OnboardingArtist(
                name = "Harry Styles",
                avatarUrl = "https://cdn-images.dzcdn.net/images/artist/1151dba9b3edc0633adf35b64c21713f/500x500-000000-80-0-0.jpg",
                languages = setOf("English", "Global"),
                genreIds = setOf(MusicGenre.GLOBAL_POP.id, MusicGenre.ROCK.id),
                region = WorldRegion.ENGLISH,
                popularityScore = 94,
                relatedArtists = setOf("Taylor Swift", "Shawn Mendes", "Olivia Rodrigo", "Fleetwood Mac"),
                accentHex = 0xFFEC4899
            ),

            // --- ENGLISH: HIP-HOP & RAP ---
            OnboardingArtist(
                name = "Kendrick Lamar",
                avatarUrl = "https://c.saavncdn.com/artists/Kendrick_Lamar_500x500.jpg",
                languages = setOf("English", "Global"),
                genreIds = setOf(MusicGenre.HIP_HOP.id),
                region = WorldRegion.ENGLISH,
                popularityScore = 98,
                relatedArtists = setOf("SZA", "J. Cole", "Drake", "Travis Scott", "Metro Boomin", "Future"),
                accentHex = 0xFFEF4444
            ),
            OnboardingArtist(
                name = "Drake",
                avatarUrl = "https://cdn-images.dzcdn.net/images/artist/eb0ed5b21d1ea5af021fc074ded0e91f/500x500-000000-80-0-0.jpg",
                languages = setOf("English", "Global"),
                genreIds = setOf(MusicGenre.HIP_HOP.id, MusicGenre.GLOBAL_POP.id),
                region = WorldRegion.ENGLISH,
                popularityScore = 99,
                relatedArtists = setOf("The Weeknd", "21 Savage", "Future", "Travis Scott", "Rihanna", "Kendrick Lamar"),
                accentHex = 0xFF1D4ED8
            ),
            OnboardingArtist(
                name = "Eminem",
                avatarUrl = "https://cdn-images.dzcdn.net/images/artist/7fa738468c9a73ff98c1e1b78d622b81/500x500-000000-80-0-0.jpg",
                languages = setOf("English", "Global"),
                genreIds = setOf(MusicGenre.HIP_HOP.id),
                region = WorldRegion.ENGLISH,
                popularityScore = 98,
                relatedArtists = setOf("50 Cent", "Snoop Dogg", "Kendrick Lamar", "Rihanna", "Juice WRLD"),
                accentHex = 0xFF334155
            ),
            OnboardingArtist(
                name = "Travis Scott",
                avatarUrl = "https://cdn-images.dzcdn.net/images/artist/8d8316146026d7e6ce377e314536df62/500x500-000000-80-0-0.jpg",
                languages = setOf("English", "Global"),
                genreIds = setOf(MusicGenre.HIP_HOP.id, MusicGenre.ELECTRONIC.id),
                region = WorldRegion.ENGLISH,
                popularityScore = 97,
                relatedArtists = setOf("Metro Boomin", "Future", "The Weeknd", "Playboi Carti", "Kanye West"),
                accentHex = 0xFF78350F
            ),
            OnboardingArtist(
                name = "Kanye West",
                avatarUrl = "https://cdn-images.dzcdn.net/images/artist/bb76c2ee3b068726ab4c37b0aabdb57a/500x500-000000-80-0-0.jpg",
                languages = setOf("English", "Global"),
                genreIds = setOf(MusicGenre.HIP_HOP.id),
                region = WorldRegion.ENGLISH,
                popularityScore = 96,
                relatedArtists = setOf("Travis Scott", "Kendrick Lamar", "Drake", "Jay-Z"),
                accentHex = 0xFF52525B
            ),
            OnboardingArtist(
                name = "Post Malone",
                avatarUrl = "https://cdn-images.dzcdn.net/images/artist/a5a8cca44e7eab2db7d44e039bed2574/500x500-000000-80-0-0.jpg",
                languages = setOf("English", "Global"),
                genreIds = setOf(MusicGenre.HIP_HOP.id, MusicGenre.GLOBAL_POP.id, MusicGenre.ROCK.id),
                region = WorldRegion.ENGLISH,
                popularityScore = 95,
                relatedArtists = setOf("The Weeknd", "Morgan Wallen", "Travis Scott", "21 Savage"),
                accentHex = 0xFFF59E0B
            ),
            OnboardingArtist(
                name = "Metro Boomin",
                avatarUrl = "https://cdn-images.dzcdn.net/images/artist/bc031b73f958987fa103031070be5c16/500x500-000000-80-0-0.jpg",
                languages = setOf("English", "Global"),
                genreIds = setOf(MusicGenre.HIP_HOP.id),
                region = WorldRegion.ENGLISH,
                popularityScore = 94,
                relatedArtists = setOf("Future", "21 Savage", "Travis Scott", "The Weeknd", "Kendrick Lamar"),
                accentHex = 0xFF991B1B
            ),
            OnboardingArtist(
                name = "J. Cole",
                avatarUrl = "https://cdn-images.dzcdn.net/images/artist/dc8d97f19855c8ea3f15ee6db784198e/500x500-000000-80-0-0.jpg",
                languages = setOf("English", "Global"),
                genreIds = setOf(MusicGenre.HIP_HOP.id),
                region = WorldRegion.ENGLISH,
                popularityScore = 93,
                relatedArtists = setOf("Kendrick Lamar", "Drake", "21 Savage"),
                accentHex = 0xFF4F46E5
            ),
            OnboardingArtist(
                name = "Central Cee",
                avatarUrl = "https://cdn-images.dzcdn.net/images/artist/25fe719f51af3ee2de27aa267e2a6ac9/500x500-000000-80-0-0.jpg",
                languages = setOf("English", "Global"),
                genreIds = setOf(MusicGenre.HIP_HOP.id),
                region = WorldRegion.ENGLISH,
                popularityScore = 91,
                relatedArtists = setOf("Dave", "Drake", "Stormzy", "Burna Boy"),
                accentHex = 0xFF0EA5E9
            ),

            // --- ENGLISH: ROCK / ALTERNATIVE / INDIE ---
            OnboardingArtist(
                name = "Coldplay",
                avatarUrl = "https://cdn-images.dzcdn.net/images/artist/3087954bca22f306324912e5ac8375c3/500x500-000000-80-0-0.jpg",
                languages = setOf("English", "Global"),
                genreIds = setOf(MusicGenre.ROCK.id, MusicGenre.GLOBAL_POP.id, MusicGenre.INDIE.id),
                region = WorldRegion.ENGLISH,
                popularityScore = 98,
                relatedArtists = setOf("Imagine Dragons", "Ed Sheeran", "BTS", "OneRepublic", "Radiohead"),
                accentHex = 0xFF3B82F6
            ),
            OnboardingArtist(
                name = "Imagine Dragons",
                avatarUrl = "https://cdn-images.dzcdn.net/images/artist/1ba025c23cae3dee14b51152990285fc/500x500-000000-80-0-0.jpg",
                languages = setOf("English", "Global"),
                genreIds = setOf(MusicGenre.ROCK.id, MusicGenre.GLOBAL_POP.id),
                region = WorldRegion.ENGLISH,
                popularityScore = 96,
                relatedArtists = setOf("Coldplay", "OneRepublic", "Linkin Park", "Twenty One Pilots"),
                accentHex = 0xFFEF4444
            ),
            OnboardingArtist(
                name = "Linkin Park",
                avatarUrl = "https://cdn-images.dzcdn.net/images/artist/4886905210739af3438990897bad3a98/500x500-000000-80-0-0.jpg",
                languages = setOf("English", "Global"),
                genreIds = setOf(MusicGenre.ROCK.id),
                region = WorldRegion.ENGLISH,
                popularityScore = 96,
                relatedArtists = setOf("Imagine Dragons", "Nirvana", "Green Day", "Foo Fighters"),
                accentHex = 0xFF1F2937
            ),
            OnboardingArtist(
                name = "Arctic Monkeys",
                avatarUrl = "https://cdn-images.dzcdn.net/images/artist/6c03e4c7c36800897fd468633286db24/500x500-000000-80-0-0.jpg",
                languages = setOf("English", "Global"),
                genreIds = setOf(MusicGenre.ROCK.id, MusicGenre.INDIE.id),
                region = WorldRegion.ENGLISH,
                popularityScore = 95,
                relatedArtists = setOf("The Neighbourhood", "Tame Impala", "Radiohead", "Cigarettes After Sex"),
                accentHex = 0xFF6366F1
            ),
            OnboardingArtist(
                name = "Queen",
                avatarUrl = "https://cdn-images.dzcdn.net/images/artist/06188edd60dc4b04a5520a3a75a1059a/500x500-000000-80-0-0.jpg",
                languages = setOf("English", "Global"),
                genreIds = setOf(MusicGenre.ROCK.id),
                region = WorldRegion.ENGLISH,
                popularityScore = 97,
                relatedArtists = setOf("The Beatles", "Pink Floyd", "Fleetwood Mac", "David Bowie"),
                accentHex = 0xFFD97706
            ),
            OnboardingArtist(
                name = "Nirvana",
                avatarUrl = "https://cdn-images.dzcdn.net/images/artist/3ec5542ff520ee74e2befdaba32ef2ef/500x500-000000-80-0-0.jpg",
                languages = setOf("English", "Global"),
                genreIds = setOf(MusicGenre.ROCK.id),
                region = WorldRegion.ENGLISH,
                popularityScore = 94,
                relatedArtists = setOf("Foo Fighters", "Linkin Park", "Radiohead", "Red Hot Chili Peppers"),
                accentHex = 0xFF059669
            ),
            OnboardingArtist(
                name = "Radiohead",
                avatarUrl = "https://cdn-images.dzcdn.net/images/artist/0d58cfbc90f2e776608bcdc0c45a4711/500x500-000000-80-0-0.jpg",
                languages = setOf("English", "Global"),
                genreIds = setOf(MusicGenre.ROCK.id, MusicGenre.INDIE.id),
                region = WorldRegion.ENGLISH,
                popularityScore = 93,
                relatedArtists = setOf("Arctic Monkeys", "Coldplay", "Pink Floyd", "Tame Impala"),
                accentHex = 0xFF475569
            ),
            OnboardingArtist(
                name = "Cigarettes After Sex",
                avatarUrl = "https://cdn-images.dzcdn.net/images/artist/78a73789b4017103858b9261b8a701d5/500x500-000000-80-0-0.jpg",
                languages = setOf("English", "Global"),
                genreIds = setOf(MusicGenre.INDIE.id, MusicGenre.LO_FI.id),
                region = WorldRegion.ENGLISH,
                popularityScore = 92,
                relatedArtists = setOf("Lana Del Rey", "Arctic Monkeys", "The Neighbourhood", "Prateek Kuhad"),
                accentHex = 0xFF334155
            ),
            OnboardingArtist(
                name = "Tame Impala",
                avatarUrl = "https://cdn-images.dzcdn.net/images/artist/879015e713cc6ad6ffaeec154c027505/500x500-000000-80-0-0.jpg",
                languages = setOf("English", "Global"),
                genreIds = setOf(MusicGenre.INDIE.id, MusicGenre.ROCK.id, MusicGenre.ELECTRONIC.id),
                region = WorldRegion.ENGLISH,
                popularityScore = 92,
                relatedArtists = setOf("Arctic Monkeys", "Daft Punk", "The Weeknd", "Fred again.."),
                accentHex = 0xFF8B5CF6
            ),

            // --- ENGLISH / GLOBAL: ELECTRONIC & EDM ---
            OnboardingArtist(
                name = "Alan Walker",
                avatarUrl = "https://cdn-images.dzcdn.net/images/artist/79d384488d390e65a6c27a95c431627e/500x500-000000-80-0-0.jpg",
                languages = setOf("English", "Global"),
                genreIds = setOf(MusicGenre.ELECTRONIC.id, MusicGenre.GLOBAL_POP.id),
                region = WorldRegion.ENGLISH,
                popularityScore = 94,
                relatedArtists = setOf("Marshmello", "Martin Garrix", "Avicii", "Kygo", "KSHMR"),
                accentHex = 0xFF0EA5E9
            ),
            OnboardingArtist(
                name = "Calvin Harris",
                avatarUrl = "https://cdn-images.dzcdn.net/images/artist/a53031d02dc2f8a8eb15d117c015d5eb/500x500-000000-80-0-0.jpg",
                languages = setOf("English", "Global"),
                genreIds = setOf(MusicGenre.ELECTRONIC.id, MusicGenre.GLOBAL_POP.id),
                region = WorldRegion.ENGLISH,
                popularityScore = 96,
                relatedArtists = setOf("Dua Lipa", "David Guetta", "Avicii", "Rihanna", "The Weeknd"),
                accentHex = 0xFF06B6D4
            ),
            OnboardingArtist(
                name = "David Guetta",
                avatarUrl = "https://cdn-images.dzcdn.net/images/artist/2d527fa03e106ed82a28f161694278d3/500x500-000000-80-0-0.jpg",
                languages = setOf("English", "French", "Global"),
                genreIds = setOf(MusicGenre.ELECTRONIC.id, MusicGenre.GLOBAL_POP.id),
                region = WorldRegion.WORLD,
                popularityScore = 96,
                relatedArtists = setOf("Calvin Harris", "Martin Garrix", "Avicii", "Daft Punk"),
                accentHex = 0xFF3B82F6
            ),
            OnboardingArtist(
                name = "Marshmello",
                avatarUrl = "https://cdn-images.dzcdn.net/images/artist/31daf06aa7034f829e2e1e7f855f2e60/500x500-000000-80-0-0.jpg",
                languages = setOf("English", "Global"),
                genreIds = setOf(MusicGenre.ELECTRONIC.id, MusicGenre.GLOBAL_POP.id),
                region = WorldRegion.ENGLISH,
                popularityScore = 93,
                relatedArtists = setOf("Alan Walker", "The Chainsmokers", "Zedd", "Martin Garrix"),
                accentHex = 0xFF14B8A6
            ),
            OnboardingArtist(
                name = "Avicii",
                avatarUrl = "https://c.saavncdn.com/artists/Avicii_002_20231227075611_500x500.jpg",
                languages = setOf("English", "Global"),
                genreIds = setOf(MusicGenre.ELECTRONIC.id, MusicGenre.GLOBAL_POP.id),
                region = WorldRegion.ENGLISH,
                popularityScore = 95,
                relatedArtists = setOf("Calvin Harris", "Kygo", "Martin Garrix", "Coldplay"),
                accentHex = 0xFFF97316
            ),
            OnboardingArtist(
                name = "Martin Garrix",
                avatarUrl = "https://cdn-images.dzcdn.net/images/artist/4cab1c0cbe0edc1b3d2234873abc485e/500x500-000000-80-0-0.jpg",
                languages = setOf("English", "Global"),
                genreIds = setOf(MusicGenre.ELECTRONIC.id),
                region = WorldRegion.ENGLISH,
                popularityScore = 93,
                relatedArtists = setOf("Avicii", "David Guetta", "KSHMR", "Alan Walker"),
                accentHex = 0xFF6366F1
            ),
            OnboardingArtist(
                name = "Fred again..",
                avatarUrl = "https://cdn-images.dzcdn.net/images/artist/3f0a4f39334266aa963038d95bbb267f/500x500-000000-80-0-0.jpg",
                languages = setOf("English", "Global"),
                genreIds = setOf(MusicGenre.ELECTRONIC.id, MusicGenre.INDIE.id),
                region = WorldRegion.ENGLISH,
                popularityScore = 92,
                relatedArtists = setOf("Skrillex", "Tame Impala", "Calvin Harris", "Daft Punk"),
                accentHex = 0xFF10B981
            ),
            OnboardingArtist(
                name = "Daft Punk",
                avatarUrl = "https://cdn-images.dzcdn.net/images/artist/638e69b9caaf9f9f3f8826febea7b543/500x500-000000-80-0-0.jpg",
                languages = setOf("English", "French", "Global"),
                genreIds = setOf(MusicGenre.ELECTRONIC.id, MusicGenre.GLOBAL_POP.id),
                region = WorldRegion.WORLD,
                popularityScore = 95,
                relatedArtists = setOf("The Weeknd", "Tame Impala", "David Guetta", "Fred again.."),
                accentHex = 0xFFD97706
            ),

            // --- WORLD: SPANISH / LATIN ---
            OnboardingArtist(
                name = "Bad Bunny",
                avatarUrl = "https://cdn-images.dzcdn.net/images/artist/7cc4a67bfdcf373f5004dafc55e7d301/500x500-000000-80-0-0.jpg",
                languages = setOf("Spanish", "Global"),
                genreIds = setOf(MusicGenre.HIP_HOP.id, MusicGenre.GLOBAL_POP.id, MusicGenre.MASS_BEATS.id),
                region = WorldRegion.WORLD,
                popularityScore = 99,
                relatedArtists = setOf("J Balvin", "Karol G", "Rauw Alejandro", "Feid", "Daddy Yankee", "Drake"),
                accentHex = 0xFFEF4444
            ),
            OnboardingArtist(
                name = "Shakira",
                avatarUrl = "https://c.saavncdn.com/artists/Shakira_002_20220916145812_500x500.jpg",
                languages = setOf("Spanish", "English", "Global"),
                genreIds = setOf(MusicGenre.GLOBAL_POP.id, MusicGenre.MASS_BEATS.id),
                region = WorldRegion.WORLD,
                popularityScore = 97,
                relatedArtists = setOf("Karol G", "J Balvin", "Maluma", "Enrique Iglesias", "Rihanna"),
                accentHex = 0xFFF59E0B
            ),
            OnboardingArtist(
                name = "Karol G",
                avatarUrl = "https://cdn-images.dzcdn.net/images/artist/5b0aab23f8d2856951a92f3a9faf70e3/500x500-000000-80-0-0.jpg",
                languages = setOf("Spanish", "Global"),
                genreIds = setOf(MusicGenre.GLOBAL_POP.id, MusicGenre.MASS_BEATS.id),
                region = WorldRegion.WORLD,
                popularityScore = 96,
                relatedArtists = setOf("Shakira", "Bad Bunny", "Feid", "J Balvin"),
                accentHex = 0xFFEC4899
            ),
            OnboardingArtist(
                name = "J Balvin",
                avatarUrl = "https://cdn-images.dzcdn.net/images/artist/325eaa46bc25052d0e3d549d60cc8225/500x500-000000-80-0-0.jpg",
                languages = setOf("Spanish", "Global"),
                genreIds = setOf(MusicGenre.GLOBAL_POP.id, MusicGenre.HIP_HOP.id),
                region = WorldRegion.WORLD,
                popularityScore = 94,
                relatedArtists = setOf("Bad Bunny", "Daddy Yankee", "Maluma", "Karol G"),
                accentHex = 0xFF8B5CF6
            ),
            OnboardingArtist(
                name = "Peso Pluma",
                avatarUrl = "https://cdn-images.dzcdn.net/images/artist/dde2bf89c1e8da0aeb94436681bc3aac/500x500-000000-80-0-0.jpg",
                languages = setOf("Spanish", "Global"),
                genreIds = setOf(MusicGenre.HIP_HOP.id, MusicGenre.INDIE.id),
                region = WorldRegion.WORLD,
                popularityScore = 92,
                relatedArtists = setOf("Bad Bunny", "Feid", "Karol G"),
                accentHex = 0xFF14B8A6
            ),
            OnboardingArtist(
                name = "Rosalía",
                avatarUrl = "https://cdn-images.dzcdn.net/images/artist/cda6212bf9dbd43fa00009b2c3f94087/500x500-000000-80-0-0.jpg",
                languages = setOf("Spanish", "Global"),
                genreIds = setOf(MusicGenre.GLOBAL_POP.id, MusicGenre.ELECTRONIC.id),
                region = WorldRegion.WORLD,
                popularityScore = 91,
                relatedArtists = setOf("The Weeknd", "Bad Bunny", "Billie Eilish"),
                accentHex = 0xFFE11D48
            ),

            // --- WORLD: KOREAN / K-POP ---
            OnboardingArtist(
                name = "BTS",
                avatarUrl = "https://c.saavncdn.com/artists/BTS_006_20260624192553_500x500.jpg",
                languages = setOf("Korean", "English", "Global"),
                genreIds = setOf(MusicGenre.GLOBAL_POP.id, MusicGenre.HIP_HOP.id, MusicGenre.MASS_BEATS.id),
                region = WorldRegion.WORLD,
                popularityScore = 100,
                relatedArtists = setOf("Jung Kook", "BLACKPINK", "Coldplay", "SEVENTEEN", "Stray Kids"),
                accentHex = 0xFF7C3AED
            ),
            OnboardingArtist(
                name = "BLACKPINK",
                avatarUrl = "https://c.saavncdn.com/artists/BlackPink_005_20260319191032_500x500.jpg",
                languages = setOf("Korean", "English", "Global"),
                genreIds = setOf(MusicGenre.GLOBAL_POP.id, MusicGenre.HIP_HOP.id, MusicGenre.ELECTRONIC.id),
                region = WorldRegion.WORLD,
                popularityScore = 99,
                relatedArtists = setOf("Jennie", "Rosé", "BTS", "TWICE", "NewJeans", "Dua Lipa"),
                accentHex = 0xFFEC4899
            ),
            OnboardingArtist(
                name = "Jung Kook",
                avatarUrl = "https://c.saavncdn.com/artists/Jung_Kook_005_20260403093956_500x500.jpg",
                languages = setOf("Korean", "English", "Global"),
                genreIds = setOf(MusicGenre.GLOBAL_POP.id),
                region = WorldRegion.WORLD,
                popularityScore = 96,
                relatedArtists = setOf("BTS", "Charlie Puth", "Justin Bieber", "Rosé"),
                accentHex = 0xFF6366F1
            ),
            OnboardingArtist(
                name = "NewJeans",
                avatarUrl = "https://cdn-images.dzcdn.net/images/artist/0866c2c1d7d00879f5db46ddc1250db8/500x500-000000-80-0-0.jpg",
                languages = setOf("Korean", "English", "Global"),
                genreIds = setOf(MusicGenre.GLOBAL_POP.id, MusicGenre.LO_FI.id),
                region = WorldRegion.WORLD,
                popularityScore = 95,
                relatedArtists = setOf("BLACKPINK", "TWICE", "aespa", "IU"),
                accentHex = 0xFF38BDF8
            ),
            OnboardingArtist(
                name = "Stray Kids",
                avatarUrl = "https://c.saavncdn.com/artists/Stray_Kids_000_20230622105959_500x500.jpg",
                languages = setOf("Korean", "Global"),
                genreIds = setOf(MusicGenre.HIP_HOP.id, MusicGenre.ELECTRONIC.id, MusicGenre.GLOBAL_POP.id),
                region = WorldRegion.WORLD,
                popularityScore = 94,
                relatedArtists = setOf("BTS", "SEVENTEEN", "aespa"),
                accentHex = 0xFFEF4444
            ),
            OnboardingArtist(
                name = "TWICE",
                avatarUrl = "https://c.saavncdn.com/artists/Twice_20190506094329_500x500.jpg",
                languages = setOf("Korean", "Japanese", "English", "Global"),
                genreIds = setOf(MusicGenre.GLOBAL_POP.id),
                region = WorldRegion.WORLD,
                popularityScore = 94,
                relatedArtists = setOf("BLACKPINK", "NewJeans", "BTS", "IU"),
                accentHex = 0xFFF97316
            ),
            OnboardingArtist(
                name = "Rosé",
                avatarUrl = "https://c.saavncdn.com/artists/Rose_000_20250715060814_500x500.jpg",
                languages = setOf("Korean", "English", "Global"),
                genreIds = setOf(MusicGenre.GLOBAL_POP.id, MusicGenre.INDIE.id),
                region = WorldRegion.WORLD,
                popularityScore = 96,
                relatedArtists = setOf("BLACKPINK", "Bruno Mars", "Jennie", "Taylor Swift"),
                accentHex = 0xFFF43F5E
            ),

            // --- WORLD: JAPANESE / J-POP ---
            OnboardingArtist(
                name = "YOASOBI",
                avatarUrl = "https://cdn-images.dzcdn.net/images/artist/2cbfaf626a591e162bfcc2b4b0214217/500x500-000000-80-0-0.jpg",
                languages = setOf("Japanese", "Global"),
                genreIds = setOf(MusicGenre.GLOBAL_POP.id, MusicGenre.ELECTRONIC.id),
                region = WorldRegion.WORLD,
                popularityScore = 94,
                relatedArtists = setOf("Kenshi Yonezu", "Fujii Kaze", "Ado", "LiSA"),
                accentHex = 0xFFE11D48
            ),
            OnboardingArtist(
                name = "Kenshi Yonezu",
                avatarUrl = "https://cdn-images.dzcdn.net/images/artist/36668c9f416b041169df04110a0812f0/500x500-000000-80-0-0.jpg",
                languages = setOf("Japanese", "Global"),
                genreIds = setOf(MusicGenre.ROCK.id, MusicGenre.GLOBAL_POP.id),
                region = WorldRegion.WORLD,
                popularityScore = 93,
                relatedArtists = setOf("YOASOBI", "Fujii Kaze", "RADWIMPS", "Official HIGE DANdism"),
                accentHex = 0xFF0EA5E9
            ),
            OnboardingArtist(
                name = "Fujii Kaze",
                avatarUrl = "https://c.saavncdn.com/artists/Fujii_Kaze_002_20250911070111_500x500.jpg",
                languages = setOf("Japanese", "English", "Global"),
                genreIds = setOf(MusicGenre.INDIE.id, MusicGenre.LO_FI.id, MusicGenre.GLOBAL_POP.id),
                region = WorldRegion.WORLD,
                popularityScore = 93,
                relatedArtists = setOf("YOASOBI", "Kenshi Yonezu", "Laufey", "RADWIMPS"),
                accentHex = 0xFF10B981
            ),
            OnboardingArtist(
                name = "Ado",
                avatarUrl = "https://cdn-images.dzcdn.net/images/artist/858e14d0f8ee062253851aaf3b31daef/500x500-000000-80-0-0.jpg",
                languages = setOf("Japanese", "Global"),
                genreIds = setOf(MusicGenre.ROCK.id, MusicGenre.GLOBAL_POP.id),
                region = WorldRegion.WORLD,
                popularityScore = 91,
                relatedArtists = setOf("YOASOBI", "LiSA", "Kenshi Yonezu"),
                accentHex = 0xFF4F46E5
            ),
            OnboardingArtist(
                name = "Joe Hisaishi",
                avatarUrl = "https://cdn-images.dzcdn.net/images/artist/36888ac751dca690993fba08fc7afb5d/500x500-000000-80-0-0.jpg",
                languages = setOf("Japanese", "Global"),
                genreIds = setOf(MusicGenre.DEVOTIONAL.id, MusicGenre.LO_FI.id),
                region = WorldRegion.WORLD,
                popularityScore = 90,
                relatedArtists = setOf("Ludovico Einaudi", "RADWIMPS", "Fujii Kaze"),
                accentHex = 0xFF0D9488
            ),

            // --- WORLD: ARABIC / FRENCH / PORTUGUESE / AFROBEATS / INSTRUMENTAL ---
            OnboardingArtist(
                name = "Amr Diab",
                avatarUrl = "https://cdn-images.dzcdn.net/images/artist/04381e254ff4ad4c43ac62d7483fea0f/500x500-000000-80-0-0.jpg",
                languages = setOf("Arabic", "Global"),
                genreIds = setOf(MusicGenre.GLOBAL_POP.id, MusicGenre.MELODIES.id),
                region = WorldRegion.WORLD,
                popularityScore = 92,
                relatedArtists = setOf("Nancy Ajram", "Elissa", "Wegz"),
                accentHex = 0xFFD97706
            ),
            OnboardingArtist(
                name = "Nancy Ajram",
                avatarUrl = "https://cdn-images.dzcdn.net/images/artist/bf5875dc299803458f0d7743c42f9693/500x500-000000-80-0-0.jpg",
                languages = setOf("Arabic", "Global"),
                genreIds = setOf(MusicGenre.GLOBAL_POP.id, MusicGenre.MELODIES.id),
                region = WorldRegion.WORLD,
                popularityScore = 90,
                relatedArtists = setOf("Amr Diab", "Elissa"),
                accentHex = 0xFFEC4899
            ),
            OnboardingArtist(
                name = "Stromae",
                avatarUrl = "https://cdn-images.dzcdn.net/images/artist/3634186855460543a9476870a912a31a/500x500-000000-80-0-0.jpg",
                languages = setOf("French", "Global"),
                genreIds = setOf(MusicGenre.ELECTRONIC.id, MusicGenre.HIP_HOP.id, MusicGenre.GLOBAL_POP.id),
                region = WorldRegion.WORLD,
                popularityScore = 93,
                relatedArtists = setOf("Indila", "Daft Punk", "Aya Nakamura", "David Guetta"),
                accentHex = 0xFF6366F1
            ),
            OnboardingArtist(
                name = "Indila",
                avatarUrl = "https://cdn-images.dzcdn.net/images/artist/d24d498af970c6feda6c2069cfdff56b/500x500-000000-80-0-0.jpg",
                languages = setOf("French", "Global"),
                genreIds = setOf(MusicGenre.GLOBAL_POP.id, MusicGenre.MELODIES.id),
                region = WorldRegion.WORLD,
                popularityScore = 91,
                relatedArtists = setOf("Stromae", "Aya Nakamura", "Lana Del Rey"),
                accentHex = 0xFF8B5CF6
            ),
            OnboardingArtist(
                name = "Anitta",
                avatarUrl = "https://cdn-images.dzcdn.net/images/artist/e874033338b645f174694ba024a14557/500x500-000000-80-0-0.jpg",
                languages = setOf("Portuguese", "Spanish", "Global"),
                genreIds = setOf(MusicGenre.GLOBAL_POP.id, MusicGenre.MASS_BEATS.id),
                region = WorldRegion.WORLD,
                popularityScore = 92,
                relatedArtists = setOf("Alok", "Shakira", "Karol G", "Bad Bunny"),
                accentHex = 0xFFF43F5E
            ),
            OnboardingArtist(
                name = "Alok",
                avatarUrl = "https://cdn-images.dzcdn.net/images/artist/4835f1af75adeffe54c43a003d26ea39/500x500-000000-80-0-0.jpg",
                languages = setOf("Portuguese", "English", "Global"),
                genreIds = setOf(MusicGenre.ELECTRONIC.id),
                region = WorldRegion.WORLD,
                popularityScore = 91,
                relatedArtists = setOf("Anitta", "Martin Garrix", "David Guetta", "Alan Walker"),
                accentHex = 0xFF0EA5E9
            ),
            OnboardingArtist(
                name = "Burna Boy",
                avatarUrl = "https://cdn-images.dzcdn.net/images/artist/ad15b7f03325752d60db9e4d39c079ae/500x500-000000-80-0-0.jpg",
                languages = setOf("English", "Global"),
                genreIds = setOf(MusicGenre.HIP_HOP.id, MusicGenre.GLOBAL_POP.id),
                region = WorldRegion.WORLD,
                popularityScore = 95,
                relatedArtists = setOf("Wizkid", "Rema", "Tyla", "Central Cee", "Ed Sheeran"),
                accentHex = 0xFF10B981
            ),
            OnboardingArtist(
                name = "Rema",
                avatarUrl = "https://cdn-images.dzcdn.net/images/artist/078018d591ab3ac531284044d4d4b388/500x500-000000-80-0-0.jpg",
                languages = setOf("English", "Global"),
                genreIds = setOf(MusicGenre.GLOBAL_POP.id, MusicGenre.HIP_HOP.id),
                region = WorldRegion.WORLD,
                popularityScore = 94,
                relatedArtists = setOf("Burna Boy", "Wizkid", "Tyla", "Selena Gomez"),
                accentHex = 0xFFF97316
            ),
            OnboardingArtist(
                name = "Ludovico Einaudi",
                avatarUrl = "https://cdn-images.dzcdn.net/images/artist/5aa391abaca4189309ca0087a2fed4e2/500x500-000000-80-0-0.jpg",
                languages = setOf("Global"),
                genreIds = setOf(MusicGenre.LO_FI.id, MusicGenre.DEVOTIONAL.id),
                region = WorldRegion.WORLD,
                popularityScore = 92,
                relatedArtists = setOf("Joe Hisaishi", "Laufey"),
                accentHex = 0xFF475569
            )
        ).distinctBy { it.name.lowercase(Locale.ROOT) }
    }

    private val artistByNameLower: Map<String, OnboardingArtist> by lazy {
        ALL_ARTISTS.associateBy { it.name.lowercase(Locale.ROOT) }
    }

    val ALL_SONGS: List<OnboardingSong> by lazy {
        val fromSample = SampleCatalog.sampleTracks.map { t ->
            OnboardingSong(
                id = t.id,
                title = t.title,
                artistName = t.artistName,
                albumName = t.albumName,
                coverArtUrl = t.coverArtUrl,
                language = "Telugu",
                genreId = MusicGenre.inferGenre(t).id,
                region = WorldRegion.INDIA,
                popularityScore = 96,
                durationMs = t.durationMs
            )
        }
        val worldwideSongs = listOf(
            // Hindi / Bollywood & Indie
            OnboardingSong("onb_hi_1", "Kesariya", "Pritam, Arijit Singh", "Brahmastra", ART_BRAHMASTRA, "Hindi", MusicGenre.MELODIES.id, WorldRegion.INDIA, 99),
            OnboardingSong("onb_hi_2", "Tum Hi Ho", "Mithoon, Arijit Singh", "Aashiqui 2", "", "Hindi", MusicGenre.MELODIES.id, WorldRegion.INDIA, 98),
            OnboardingSong("onb_hi_3", "Chaleya", "Anirudh Ravichander, Arijit Singh, Shilpa Rao", "Jawan", "", "Hindi", MusicGenre.BOLLYWOOD.id, WorldRegion.INDIA, 98),
            OnboardingSong("onb_hi_4", "Apna Bana Le", "Sachin-Jigar, Arijit Singh", "Bhediya", "", "Hindi", MusicGenre.MELODIES.id, WorldRegion.INDIA, 96),
            OnboardingSong("onb_hi_5", "Husn", "Anuv Jain", "Husn", "", "Hindi", MusicGenre.INDIE.id, WorldRegion.INDIA, 95),
            OnboardingSong("onb_hi_6", "Cold/Mess", "Prateek Kuhad", "Cold/Mess", "", "Hindi", MusicGenre.INDIE.id, WorldRegion.INDIA, 90),
            OnboardingSong("onb_hi_7", "Kun Faya Kun", "A.R. Rahman, Mohit Chauhan", "Rockstar", "", "Hindi", MusicGenre.DEVOTIONAL.id, WorldRegion.INDIA, 97),
            OnboardingSong("onb_hi_8", "Big Dawgs", "Hanumankind", "Big Dawgs", "", "Hindi", MusicGenre.HIP_HOP.id, WorldRegion.INDIA, 96),
            OnboardingSong("onb_hi_9", "Mirchi", "Divine", "Punya Paap", "", "Hindi", MusicGenre.HIP_HOP.id, WorldRegion.INDIA, 89),
            OnboardingSong("onb_hi_10", "Maan Meri Jaan", "King", "Champagne Talk", "", "Hindi", MusicGenre.BOLLYWOOD.id, WorldRegion.INDIA, 94),
            OnboardingSong("onb_hi_11", "Tere Vaaste", "Sachin-Jigar, Varun Jain", "Zara Hatke Zara Bachke", "", "Hindi", MusicGenre.BOLLYWOOD.id, WorldRegion.INDIA, 92),
            OnboardingSong("onb_hi_12", "Raataan Lambiyan", "Jubin Nautiyal, Tanishk Bagchi", "Shershaah", "", "Hindi", MusicGenre.MELODIES.id, WorldRegion.INDIA, 94),

            // Tamil
            OnboardingSong("onb_ta_1", "Why This Kolaveri Di", "Anirudh Ravichander, Dhanush", "3", ART_ANIRUDH, "Tamil", MusicGenre.MASS_BEATS.id, WorldRegion.INDIA, 96),
            OnboardingSong("onb_ta_2", "Hukum", "Anirudh Ravichander", "Jailer", ART_ANIRUDH, "Tamil", MusicGenre.MASS_BEATS.id, WorldRegion.INDIA, 97),
            OnboardingSong("onb_ta_3", "Munbe Vaa", "A.R. Rahman, Shreya Ghoshal", "Sillunu Oru Kaadhal", "", "Tamil", MusicGenre.MELODIES.id, WorldRegion.INDIA, 96),
            OnboardingSong("onb_ta_4", "Rowdy Baby", "Yuvan Shankar Raja, Dhanush", "Maari 2", "", "Tamil", MusicGenre.MASS_BEATS.id, WorldRegion.INDIA, 95),
            OnboardingSong("onb_ta_5", "Enjoy Enjaami", "Santhosh Narayanan, Dhee", "Single", "", "Tamil", MusicGenre.INDIE.id, WorldRegion.INDIA, 93),
            OnboardingSong("onb_ta_6", "Vaseegara", "Harris Jayaraj, Bombay Jayashri", "Minnale", "", "Tamil", MusicGenre.MELODIES.id, WorldRegion.INDIA, 93),

            // Punjabi
            OnboardingSong("onb_pa_1", "Lover", "Diljit Dosanjh", "MoonChild Era", "", "Punjabi", MusicGenre.MASS_BEATS.id, WorldRegion.INDIA, 97),
            OnboardingSong("onb_pa_2", "Excuses", "AP Dhillon", "Excuses", "", "Punjabi", MusicGenre.HIP_HOP.id, WorldRegion.INDIA, 96),
            OnboardingSong("onb_pa_3", "Tauba Tauba", "Karan Aujla", "Bad Newz", "", "Punjabi", MusicGenre.MASS_BEATS.id, WorldRegion.INDIA, 97),
            OnboardingSong("onb_pa_4", "295", "Sidhu Moose Wala", "Moosetape", "", "Punjabi", MusicGenre.HIP_HOP.id, WorldRegion.INDIA, 96),
            OnboardingSong("onb_pa_5", "Cheques", "Shubh", "Still Rollin", "", "Punjabi", MusicGenre.HIP_HOP.id, WorldRegion.INDIA, 94),
            OnboardingSong("onb_pa_6", "Teri Mitti", "B Praak", "Kesari", "", "Punjabi", MusicGenre.MELODIES.id, WorldRegion.INDIA, 93),

            // Malayalam
            OnboardingSong("onb_ml_1", "Darshana", "Hesham Abdul Wahab, Darshana Rajendran", "Hridayam", ART_HI_NANNA, "Malayalam", MusicGenre.MELODIES.id, WorldRegion.INDIA, 95),
            OnboardingSong("onb_ml_2", "Illuminati", "Sushin Shyam, Dabzee", "Aavesham", "", "Malayalam", MusicGenre.MASS_BEATS.id, WorldRegion.INDIA, 96),
            OnboardingSong("onb_ml_3", "Jimikki Kammal", "Shaan Rahman, Vineeth Sreenivasan", "Velipadinte Pusthakam", "", "Malayalam", MusicGenre.MASS_BEATS.id, WorldRegion.INDIA, 92),
            OnboardingSong("onb_ml_4", "Harimuraleeravam", "K.J. Yesudas, K.S. Chithra", "Aaraam Thampuran", "", "Malayalam", MusicGenre.DEVOTIONAL.id, WorldRegion.INDIA, 91),

            // Kannada
            OnboardingSong("onb_kn_1", "Belageddu", "Vijay Prakash", "Kirik Party", "", "Kannada", MusicGenre.MELODIES.id, WorldRegion.INDIA, 92),
            OnboardingSong("onb_kn_2", "Salaam Rocky Bhai", "Ravi Basrur, Vijay Prakash", "KGF Chapter 1", "", "Kannada", MusicGenre.MASS_BEATS.id, WorldRegion.INDIA, 95),
            OnboardingSong("onb_kn_3", "Shakuntle Sikkalu", "Sanjith Hegde", "Naduve Antaravirali", "", "Kannada", MusicGenre.MELODIES.id, WorldRegion.INDIA, 90),
            OnboardingSong("onb_kn_4", "Mysore Se Aayi", "Raghu Dixit", "Antaragni", "", "Kannada", MusicGenre.INDIE.id, WorldRegion.INDIA, 88),

            // Marathi / Bengali / Gujarati / Bhojpuri / Urdu
            OnboardingSong("onb_mr_1", "Zingaat", "Ajay-Atul", "Sairat", "", "Marathi", MusicGenre.MASS_BEATS.id, WorldRegion.INDIA, 96),
            OnboardingSong("onb_mr_2", "Apsara Aali", "Ajay-Atul, Bela Shende", "Natarang", "", "Marathi", MusicGenre.MELODIES.id, WorldRegion.INDIA, 92),
            OnboardingSong("onb_bn_1", "Amake Amar Moto Thakte Dao", "Anupam Roy", "Autograph", "", "Bengali", MusicGenre.INDIE.id, WorldRegion.INDIA, 91),
            OnboardingSong("onb_bn_2", "Mon Majhi Re", "Arijit Singh", "Boss", "", "Bengali", MusicGenre.MELODIES.id, WorldRegion.INDIA, 92),
            OnboardingSong("onb_gu_1", "Khalasi", "Aditya Gadhvi", "Coke Studio Bharat", "", "Gujarati", MusicGenre.INDIE.id, WorldRegion.INDIA, 95),
            OnboardingSong("onb_gu_2", "Chogada", "Darshan Raval, Sachin-Jigar", "Loveyatri", "", "Gujarati", MusicGenre.MASS_BEATS.id, WorldRegion.INDIA, 94),
            OnboardingSong("onb_bh_1", "Lollypop Lagelu", "Pawan Singh", "Bhojpuri Hits", "", "Bhojpuri", MusicGenre.MASS_BEATS.id, WorldRegion.INDIA, 93),
            OnboardingSong("onb_bh_2", "Theek Hai", "Khesari Lal Yadav", "Single", "", "Bhojpuri", MusicGenre.MASS_BEATS.id, WorldRegion.INDIA, 89),
            OnboardingSong("onb_ur_1", "Pasoori", "Ali Sethi, Shae Gill", "Coke Studio", "", "Urdu", MusicGenre.INDIE.id, WorldRegion.INDIA, 98),
            OnboardingSong("onb_ur_2", "Afreen Afreen", "Rahat Fateh Ali Khan, Nusrat Fateh Ali Khan", "Coke Studio", "", "Urdu", MusicGenre.DEVOTIONAL.id, WorldRegion.INDIA, 97),

            // English: Pop, Hip-Hop, Rock, EDM, Indie
            OnboardingSong("onb_en_1", "Blinding Lights", "The Weeknd", "After Hours", ART_WEEKND, "English", MusicGenre.GLOBAL_POP.id, WorldRegion.ENGLISH, 100),
            OnboardingSong("onb_en_2", "Starboy", "The Weeknd, Daft Punk", "Starboy", ART_WEEKND, "English", MusicGenre.GLOBAL_POP.id, WorldRegion.ENGLISH, 98),
            OnboardingSong("onb_en_3", "Cruel Summer", "Taylor Swift", "Lover", "", "English", MusicGenre.GLOBAL_POP.id, WorldRegion.ENGLISH, 99),
            OnboardingSong("onb_en_4", "Birds of a Feather", "Billie Eilish", "HIT ME HARD AND SOFT", "", "English", MusicGenre.GLOBAL_POP.id, WorldRegion.ENGLISH, 98),
            OnboardingSong("onb_en_5", "Die With A Smile", "Lady Gaga, Bruno Mars", "Single", "", "English", MusicGenre.GLOBAL_POP.id, WorldRegion.ENGLISH, 99),
            OnboardingSong("onb_en_6", "Espresso", "Sabrina Carpenter", "Short n' Sweet", "", "English", MusicGenre.GLOBAL_POP.id, WorldRegion.ENGLISH, 97),
            OnboardingSong("onb_en_7", "Levitating", "Dua Lipa", "Future Nostalgia", "", "English", MusicGenre.GLOBAL_POP.id, WorldRegion.ENGLISH, 96),
            OnboardingSong("onb_en_8", "Shape of You", "Ed Sheeran", "Divide", "", "English", MusicGenre.GLOBAL_POP.id, WorldRegion.ENGLISH, 98),
            OnboardingSong("onb_en_9", "Not Like Us", "Kendrick Lamar", "Single", "", "English", MusicGenre.HIP_HOP.id, WorldRegion.ENGLISH, 98),
            OnboardingSong("onb_en_10", "God's Plan", "Drake", "Scorpion", "", "English", MusicGenre.HIP_HOP.id, WorldRegion.ENGLISH, 97),
            OnboardingSong("onb_en_11", "Lose Yourself", "Eminem", "8 Mile", "", "English", MusicGenre.HIP_HOP.id, WorldRegion.ENGLISH, 98),
            OnboardingSong("onb_en_12", "FE!N", "Travis Scott, Playboi Carti", "UTOPIA", "", "English", MusicGenre.HIP_HOP.id, WorldRegion.ENGLISH, 95),
            OnboardingSong("onb_en_13", "Viva La Vida", "Coldplay", "Viva La Vida", "", "English", MusicGenre.ROCK.id, WorldRegion.ENGLISH, 97),
            OnboardingSong("onb_en_14", "Believer", "Imagine Dragons", "Evolve", "", "English", MusicGenre.ROCK.id, WorldRegion.ENGLISH, 96),
            OnboardingSong("onb_en_15", "Numb", "Linkin Park", "Meteora", "", "English", MusicGenre.ROCK.id, WorldRegion.ENGLISH, 96),
            OnboardingSong("onb_en_16", "505", "Arctic Monkeys", "Favourite Worst Nightmare", "", "English", MusicGenre.ROCK.id, WorldRegion.ENGLISH, 94),
            OnboardingSong("onb_en_17", "Bohemian Rhapsody", "Queen", "A Night at the Opera", "", "English", MusicGenre.ROCK.id, WorldRegion.ENGLISH, 98),
            OnboardingSong("onb_en_18", "Faded", "Alan Walker", "Different World", "", "English", MusicGenre.ELECTRONIC.id, WorldRegion.ENGLISH, 96),
            OnboardingSong("onb_en_19", "Wake Me Up", "Avicii", "True", "", "English", MusicGenre.ELECTRONIC.id, WorldRegion.ENGLISH, 96),
            OnboardingSong("onb_en_20", "Apocalypse", "Cigarettes After Sex", "Cigarettes After Sex", "", "English", MusicGenre.LO_FI.id, WorldRegion.ENGLISH, 93),
            OnboardingSong("onb_en_21", "Kill Bill", "SZA", "SOS", "", "English", MusicGenre.GLOBAL_POP.id, WorldRegion.ENGLISH, 95),
            OnboardingSong("onb_en_22", "Calm Down", "Rema", "Rave & Roses", "", "English", MusicGenre.GLOBAL_POP.id, WorldRegion.WORLD, 96),

            // Spanish / Latin
            OnboardingSong("onb_es_1", "Tití Me Preguntó", "Bad Bunny", "Un Verano Sin Ti", "", "Spanish", MusicGenre.HIP_HOP.id, WorldRegion.WORLD, 97),
            OnboardingSong("onb_es_2", "Hips Don't Lie", "Shakira", "Oral Fixation", "", "Spanish", MusicGenre.GLOBAL_POP.id, WorldRegion.WORLD, 97),
            OnboardingSong("onb_es_3", "TQG", "Karol G, Shakira", "Mañana Será Bonito", "", "Spanish", MusicGenre.GLOBAL_POP.id, WorldRegion.WORLD, 95),
            OnboardingSong("onb_es_4", "Mi Gente", "J Balvin", "Vibras", "", "Spanish", MusicGenre.MASS_BEATS.id, WorldRegion.WORLD, 95),

            // Korean / K-Pop
            OnboardingSong("onb_ko_1", "Dynamite", "BTS", "BE", "", "Korean", MusicGenre.GLOBAL_POP.id, WorldRegion.WORLD, 99),
            OnboardingSong("onb_ko_2", "DDU-DU DDU-DU", "BLACKPINK", "SQUARE UP", "", "Korean", MusicGenre.GLOBAL_POP.id, WorldRegion.WORLD, 97),
            OnboardingSong("onb_ko_3", "Seven", "Jung Kook", "GOLDEN", "", "Korean", MusicGenre.GLOBAL_POP.id, WorldRegion.WORLD, 96),
            OnboardingSong("onb_ko_4", "APT.", "Rosé, Bruno Mars", "rosie", "", "Korean", MusicGenre.GLOBAL_POP.id, WorldRegion.WORLD, 99),
            OnboardingSong("onb_ko_5", "Super Shy", "NewJeans", "Get Up", "", "Korean", MusicGenre.GLOBAL_POP.id, WorldRegion.WORLD, 95),

            // Japanese / J-Pop
            OnboardingSong("onb_ja_1", "Idol", "YOASOBI", "THE BOOK 3", "", "Japanese", MusicGenre.GLOBAL_POP.id, WorldRegion.WORLD, 97),
            OnboardingSong("onb_ja_2", "Kick Back", "Kenshi Yonezu", "LOST CORNER", "", "Japanese", MusicGenre.ROCK.id, WorldRegion.WORLD, 95),
            OnboardingSong("onb_ja_3", "Shinunoga E-Wa", "Fujii Kaze", "HELP EVER HURT NEVER", "", "Japanese", MusicGenre.INDIE.id, WorldRegion.WORLD, 96),

            // Arabic / French / Portuguese / Global Instrumental
            OnboardingSong("onb_ar_1", "Tamally Maak", "Amr Diab", "Tamally Maak", "", "Arabic", MusicGenre.MELODIES.id, WorldRegion.WORLD, 94),
            OnboardingSong("onb_ar_2", "Ya Tabtab", "Nancy Ajram", "Ya Tabtab Wa Dallaa", "", "Arabic", MusicGenre.GLOBAL_POP.id, WorldRegion.WORLD, 91),
            OnboardingSong("onb_fr_1", "Alors on danse", "Stromae", "Cheese", "", "French", MusicGenre.ELECTRONIC.id, WorldRegion.WORLD, 95),
            OnboardingSong("onb_fr_2", "Dernière danse", "Indila", "Mini World", "", "French", MusicGenre.GLOBAL_POP.id, WorldRegion.WORLD, 95),
            OnboardingSong("onb_pt_1", "Envolver", "Anitta", "Versions of Me", "", "Portuguese", MusicGenre.GLOBAL_POP.id, WorldRegion.WORLD, 93),
            OnboardingSong("onb_pt_2", "Hear Me Now", "Alok", "Single", "", "Portuguese", MusicGenre.ELECTRONIC.id, WorldRegion.WORLD, 92),
            OnboardingSong("onb_gl_1", "Experience", "Ludovico Einaudi", "In a Time Lapse", "", "Global", MusicGenre.LO_FI.id, WorldRegion.WORLD, 94)
        )
        (fromSample + worldwideSongs).distinctBy {
            "${it.title.lowercase(Locale.ROOT)}|${EchoRecommendationEngine.primaryArtistOf(it.artistName).lowercase(Locale.ROOT)}"
        }
    }

    private val songById: Map<String, OnboardingSong> by lazy {
        ALL_SONGS.associateBy { it.id }
    }

    fun findSongById(id: String): OnboardingSong? = songById[id]

    fun allOnboardingTracks(): List<Track> = ALL_SONGS.map { it.toTrack() }

    /**
     * Maps any supported Indian or World language to the best-matching [MusicGenre] ids.
     */
    fun genresForLanguage(language: String): Set<String> = when (language.trim().lowercase(Locale.ROOT)) {
        "telugu" -> setOf(MusicGenre.TOL_TELUGU.id, MusicGenre.MELODIES.id, MusicGenre.MASS_BEATS.id)
        "hindi" -> setOf(MusicGenre.BOLLYWOOD.id, MusicGenre.MELODIES.id, MusicGenre.INDIE.id)
        "tamil" -> setOf(MusicGenre.TOL_TELUGU.id, MusicGenre.MELODIES.id, MusicGenre.MASS_BEATS.id)
        "punjabi" -> setOf(MusicGenre.HIP_HOP.id, MusicGenre.MASS_BEATS.id, MusicGenre.BOLLYWOOD.id)
        "malayalam", "kannada", "marathi", "bengali", "gujarati" -> setOf(MusicGenre.MELODIES.id, MusicGenre.INDIE.id)
        "bhojpuri" -> setOf(MusicGenre.MASS_BEATS.id, MusicGenre.BOLLYWOOD.id)
        "urdu" -> setOf(MusicGenre.MELODIES.id, MusicGenre.DEVOTIONAL.id, MusicGenre.INDIE.id)
        "english", "korean", "japanese", "spanish", "french", "portuguese", "arabic", "global" ->
            setOf(MusicGenre.GLOBAL_POP.id, MusicGenre.HIP_HOP.id, MusicGenre.ELECTRONIC.id, MusicGenre.ROCK.id)
        else -> setOf(MusicGenre.GLOBAL_POP.id)
    }

    /**
     * Search queries used by [GlobalCatalogService] / [AudioRepository] for each Indian & World language.
     */
    fun searchQueriesForLanguage(language: String): List<String> = when (language.trim().lowercase(Locale.ROOT)) {
        "telugu" -> listOf("telugu top hits", "new telugu songs 2026")
        "hindi" -> listOf("hindi top hits", "latest hindi songs 2026")
        "tamil" -> listOf("tamil top hits", "new tamil songs 2026")
        "punjabi" -> listOf("punjabi top hits", "new punjabi songs 2026")
        "malayalam" -> listOf("malayalam top hits", "new malayalam songs")
        "kannada" -> listOf("kannada top hits", "new kannada songs")
        "marathi" -> listOf("marathi top hits", "new marathi songs")
        "bengali" -> listOf("bengali top hits", "new bengali songs")
        "gujarati" -> listOf("gujarati hits", "new gujarati songs")
        "bhojpuri" -> listOf("bhojpuri top hits", "new bhojpuri songs")
        "urdu" -> listOf("coke studio pakistan", "urdu acoustic hits")
        "english" -> listOf("top pop hits 2026", "global top 50")
        "spanish" -> listOf("latin top hits", "reggaeton hits")
        "korean" -> listOf("k-pop top hits", "bts blackpink newjeans")
        "japanese" -> listOf("j-pop top hits", "yoasobi kenshi yonezu")
        "arabic" -> listOf("arabic top hits", "amr diab nancy ajram")
        "french" -> listOf("french pop hits", "stromae indila")
        "portuguese" -> listOf("brazilian funk pop hits", "anitta alok")
        else -> listOf("top hits 2026", "viral global hits")
    }

    /**
     * Optimizes onboarding songs for the user's selected languages, genres, and query.
     */
    fun optimizeSongsForUser(
        selectedLanguages: Set<String>,
        selectedGenreIds: Set<String>,
        query: String = "",
        regionFilter: WorldRegion = WorldRegion.ALL,
        extraTracks: List<Track> = emptyList()
    ): List<OnboardingSong> {
        val cleanQuery = query.trim().lowercase(Locale.ROOT)
        val langLower = selectedLanguages.mapTo(HashSet()) { it.trim().lowercase(Locale.ROOT) }

        val dynamicExtra = extraTracks.map { t ->
            OnboardingSong(
                id = t.id,
                title = t.title,
                artistName = t.artistName,
                albumName = t.albumName,
                coverArtUrl = t.coverArtUrl,
                language = inferLanguageFromTrack(t),
                genreId = MusicGenre.inferGenre(t).id,
                region = if (isIndianTrack(t)) WorldRegion.INDIA else WorldRegion.WORLD,
                popularityScore = 88,
                durationMs = t.durationMs
            )
        }

        val combined = (ALL_SONGS + dynamicExtra).distinctBy {
            "${it.title.lowercase(Locale.ROOT)}|${EchoRecommendationEngine.primaryArtistOf(it.artistName).lowercase(Locale.ROOT)}"
        }

        val filtered = combined.filter { song ->
            val matchesRegion = regionFilter == WorldRegion.ALL || song.region == regionFilter
            val matchesQuery = cleanQuery.isBlank() ||
                song.title.lowercase(Locale.ROOT).contains(cleanQuery) ||
                song.artistName.lowercase(Locale.ROOT).contains(cleanQuery) ||
                song.albumName.lowercase(Locale.ROOT).contains(cleanQuery) ||
                song.language.lowercase(Locale.ROOT).contains(cleanQuery)
            matchesRegion && matchesQuery
        }

        val scored = filtered.map { song ->
            var score = song.popularityScore * 0.4
            if (song.language.lowercase(Locale.ROOT) in langLower) score += 65.0
            if ("global" in langLower && song.region == WorldRegion.WORLD) score += 20.0
            if (song.genreId in selectedGenreIds) score += 35.0
            if (cleanQuery.isNotBlank()) {
                val tLower = song.title.lowercase(Locale.ROOT)
                val aLower = song.artistName.lowercase(Locale.ROOT)
                if (tLower == cleanQuery || aLower == cleanQuery) score += 120.0
                else if (tLower.startsWith(cleanQuery) || aLower.startsWith(cleanQuery)) score += 60.0
            }
            song to score
        }.sortedByDescending { it.second }.map { it.first }

        // If user selected multiple languages and isn't searching, interleave languages gently
        if (cleanQuery.isBlank() && selectedLanguages.size > 1) {
            return interleaveByLanguage(scored)
        }
        return scored
    }

    /**
     * Optimizes ALL artists for a specific user based on:
     * 1. Their selected songs (direct artists + featured collaborators + song ML embedding)
     * 2. Their selected artists (co-listening graph + shared genres/languages)
     * 3. Their selected languages and genres
     * 4. Search query / region filter
     */
    fun optimizeArtistsForUser(
        selectedLanguages: Set<String>,
        selectedGenreIds: Set<String>,
        selectedSongTracks: List<Track>,
        selectedArtists: Set<String>,
        extraArtists: List<OnboardingArtist> = emptyList(),
        query: String = "",
        regionFilter: WorldRegion = WorldRegion.ALL,
        prioritizeSelectedInList: Boolean = false
    ): List<OptimizedArtistRecommendation> {
        val cleanQuery = query.trim().lowercase(Locale.ROOT)
        val langLower = selectedLanguages.mapTo(HashSet()) { it.trim().lowercase(Locale.ROOT) }
        val selectedArtistsLower = selectedArtists.mapTo(HashSet()) { it.trim().lowercase(Locale.ROOT) }

        // Extract every artist token (primary + featured) from user's selected songs
        val songArtistTokens = LinkedHashMap<String, String>() // artistLower -> songTitle
        val songGenreCounts = HashMap<String, Int>()
        val songLanguageCounts = HashMap<String, Int>()
        val songVectors = ArrayList<FloatArray>(selectedSongTracks.size)

        for (track in selectedSongTracks) {
            val tokens = extractArtistTokens(track.artistName)
            for (tok in tokens) {
                songArtistTokens.putIfAbsent(tok.lowercase(Locale.ROOT), track.title)
            }
            val gId = MusicGenre.inferGenre(track).id
            songGenreCounts[gId] = (songGenreCounts[gId] ?: 0) + 1
            val inferredLang = inferLanguageFromTrack(track)
            songLanguageCounts[inferredLang.lowercase(Locale.ROOT)] =
                (songLanguageCounts[inferredLang.lowercase(Locale.ROOT)] ?: 0) + 1
            songVectors.add(EchoTasteMlModel.embedTrack(track))
        }

        // Compute user song taste centroid vector in 12D ML space
        val userCentroid = FloatArray(EchoTasteMlModel.EMBEDDING_DIM)
        if (songVectors.isNotEmpty()) {
            for (vec in songVectors) {
                for (i in userCentroid.indices) userCentroid[i] += vec[i]
            }
        }
        for (artName in selectedArtists) {
            val art = artistByNameLower[artName.lowercase(Locale.ROOT)]
            if (art != null) {
                val rep = representativeTrackForArtist(art)
                val vec = EchoTasteMlModel.embedTrack(rep)
                for (i in userCentroid.indices) userCentroid[i] += vec[i] * 0.7f
            }
        }
        val hasCentroid = userCentroid.any { it != 0f }

        // Build reverse + forward related-artist affinity map from selected songs & selected artists
        val relatedAffinitySource = HashMap<String, Pair<String, Double>>() // targetLower -> (reason, boost)
        fun recordRelatedBoost(targetLower: String, reason: String, boost: Double) {
            val prev = relatedAffinitySource[targetLower]
            if (prev == null) {
                relatedAffinitySource[targetLower] = reason to boost.coerceAtMost(85.0)
            } else {
                val combined = maxOf(prev.second, boost) + 12.0
                relatedAffinitySource[targetLower] = prev.first to combined.coerceAtMost(85.0)
            }
        }
        fun addRelatedBoost(sourceName: String, reason: String, boost: Double) {
            val src = artistByNameLower[sourceName.lowercase(Locale.ROOT)] ?: return
            for (rel in src.relatedArtists) {
                recordRelatedBoost(rel.lowercase(Locale.ROOT), reason, boost)
            }
        }

        for ((songArtistLower, songTitle) in songArtistTokens) {
            val matchedCatalogArtist = artistByNameLower[songArtistLower]
            if (matchedCatalogArtist != null) {
                addRelatedBoost(matchedCatalogArtist.name, "Similar to ${matchedCatalogArtist.name}", 62.0)
            }
            for (cand in ALL_ARTISTS) {
                if (cand.relatedArtists.any { it.equals(songArtistLower, ignoreCase = true) }) {
                    recordRelatedBoost(cand.name.lowercase(Locale.ROOT), "Because you picked $songTitle", 52.0)
                }
            }
        }

        for (selArtist in selectedArtists) {
            addRelatedBoost(selArtist, "Similar to $selArtist", 56.0)
            for (cand in ALL_ARTISTS) {
                if (cand.relatedArtists.any { it.equals(selArtist, ignoreCase = true) }) {
                    recordRelatedBoost(cand.name.lowercase(Locale.ROOT), "Fans also like $selArtist", 45.0)
                }
            }
        }

        // Merge curated artists + any song-derived artists not yet in ALL_ARTISTS + extra live artists
        val derivedFromSongs = songArtistTokens.keys
            .filter { it !in artistByNameLower }
            .mapNotNull { tokenLower ->
                val sampleTrack = selectedSongTracks.firstOrNull { tr ->
                    extractArtistTokens(tr.artistName).any { it.equals(tokenLower, ignoreCase = true) }
                } ?: return@mapNotNull null
                val display = extractArtistTokens(sampleTrack.artistName)
                    .firstOrNull { it.equals(tokenLower, ignoreCase = true) }
                    ?: return@mapNotNull null
                OnboardingArtist(
                    name = display,
                    avatarUrl = sampleTrack.coverArtUrl,
                    languages = setOf(inferLanguageFromTrack(sampleTrack)),
                    genreIds = setOf(MusicGenre.inferGenre(sampleTrack).id),
                    region = if (isIndianTrack(sampleTrack)) WorldRegion.INDIA else WorldRegion.WORLD,
                    popularityScore = 92
                )
            }

        val allPool = (ALL_ARTISTS + derivedFromSongs + extraArtists)
            .distinctBy { it.name.lowercase(Locale.ROOT) }

        val filtered = allPool.filter { art ->
            val matchesRegion = regionFilter == WorldRegion.ALL || art.region == regionFilter
            val matchesQuery = cleanQuery.isBlank() ||
                art.name.lowercase(Locale.ROOT).contains(cleanQuery) ||
                art.languages.any { it.lowercase(Locale.ROOT).contains(cleanQuery) } ||
                art.genreIds.any { MusicGenre.fromId(it).displayNameLower.contains(cleanQuery) }
            matchesRegion && matchesQuery
        }

        val scored = filtered.map { artist ->
            val nameLower = artist.name.lowercase(Locale.ROOT)
            var score = artist.popularityScore * 0.35
            var reason = ""

            // 1. Direct match from selected songs
            val directSongTitle = songArtistTokens[nameLower]
            if (directSongTitle != null) {
                score += 150.0
                reason = "From $directSongTitle"
            }

            // 2. Related-artist graph affinity (from selected songs + selected artists)
            val relMatch = relatedAffinitySource[nameLower]
            if (relMatch != null) {
                score += relMatch.second
                if (reason.isBlank()) reason = relMatch.first
            }

            // 3. Language alignment (onboarding choices + song languages)
            val matchingLangs = artist.languages.filter { it.lowercase(Locale.ROOT) in langLower }
            if (matchingLangs.isNotEmpty()) {
                score += 55.0 + (matchingLangs.size - 1) * 10.0
                if (reason.isBlank()) reason = matchingLangs.first()
            } else if ("global" in langLower && artist.region == WorldRegion.WORLD) {
                score += 24.0
            }
            val songLangBoost = artist.languages.sumOf { (songLanguageCounts[it.lowercase(Locale.ROOT)] ?: 0) * 18 }
            score += songLangBoost

            // 4. Genre alignment (onboarding choices + song genres)
            val matchingGenres = artist.genreIds.count { it in selectedGenreIds }
            if (matchingGenres > 0) {
                score += matchingGenres * 30.0
                if (reason.isBlank()) {
                    val firstG = artist.genreIds.firstOrNull { it in selectedGenreIds }
                    if (firstG != null) reason = MusicGenre.fromId(firstG).shortLabel
                }
            }
            val songGenreBoost = artist.genreIds.sumOf { (songGenreCounts[it] ?: 0) * 16 }
            score += songGenreBoost

            // 5. 12D ML Embedding cosine similarity with user's selected songs & artists
            if (hasCentroid) {
                val artVec = EchoTasteMlModel.embedTrack(representativeTrackForArtist(artist))
                val cos = ((EchoTasteMlModel.cosineSimilarity(userCentroid, artVec) + 1f) * 0.5f).coerceIn(0f, 1f)
                score += cos * 42.0
            }

            // 6. Optional selected pinning & search boost
            if (prioritizeSelectedInList && nameLower in selectedArtistsLower) {
                score += 130.0
            }
            if (cleanQuery.isNotBlank()) {
                if (nameLower == cleanQuery) score += 200.0
                else if (nameLower.startsWith(cleanQuery)) score += 90.0
            }

            if (reason.isBlank()) {
                val primaryLang = artist.languages.firstOrNull() ?: "Global"
                val primaryGenre = artist.genreIds.firstOrNull()?.let { MusicGenre.fromId(it).shortLabel } ?: "Artist"
                reason = "$primaryLang • $primaryGenre"
            }

            OptimizedArtistRecommendation(
                artist = artist,
                score = score,
                reasonTag = reason
            )
        }.sortedByDescending { it.score }

        return scored
    }

    /**
     * Returns top unselected related/recommended artists based on the user's currently selected
     * songs and artists — used for the live "Recommended for your picks" strip.
     */
    fun getSimilarArtistsStrip(
        selectedLanguages: Set<String>,
        selectedGenreIds: Set<String>,
        selectedSongTracks: List<Track>,
        selectedArtists: Set<String>,
        limit: Int = 10
    ): List<OptimizedArtistRecommendation> {
        val selectedLower = selectedArtists.mapTo(HashSet()) { it.trim().lowercase(Locale.ROOT) }
        return optimizeArtistsForUser(
            selectedLanguages = selectedLanguages,
            selectedGenreIds = selectedGenreIds,
            selectedSongTracks = selectedSongTracks,
            selectedArtists = selectedArtists,
            prioritizeSelectedInList = false
        ).filter { it.artist.name.lowercase(Locale.ROOT) !in selectedLower }
            .take(limit)
    }

    /**
     * Extracts individual artist names from collaborative credits like
     * "Shilpa Rao, Anirudh Ravichander" or "The Weeknd feat. Daft Punk".
     */
    fun extractArtistTokens(artistCredit: String): List<String> {
        return artistCredit
            .split(Regex("""[,&•/]|\b(?:feat\.?|ft\.?|featuring|with|x)\b""", RegexOption.IGNORE_CASE))
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.equals("Unknown Artist", ignoreCase = true) }
    }

    fun findOnboardingArtist(name: String): OnboardingArtist? {
        val trimmed = name.trim().lowercase(Locale.ROOT)
        if (trimmed.isBlank()) return null
        return artistByNameLower[trimmed] ?: ALL_ARTISTS.firstOrNull {
            it.name.lowercase(Locale.ROOT).contains(trimmed)
        }
    }

    private fun representativeTrackForArtist(artist: OnboardingArtist): Track {
        val matchingSong = ALL_SONGS.firstOrNull { song ->
            extractArtistTokens(song.artistName).any { it.equals(artist.name, ignoreCase = true) }
        }
        if (matchingSong != null) return matchingSong.toTrack()
        val firstGenre = artist.genreIds.firstOrNull()?.let { MusicGenre.fromId(it).shortLabel } ?: "Pop"
        val firstLang = artist.languages.firstOrNull() ?: "Global"
        return Track(
            id = "rep_${artist.name.lowercase(Locale.ROOT)}",
            title = "$firstLang Hits",
            artistName = artist.name,
            albumName = firstLang,
            coverArtUrl = artist.avatarUrl,
            durationMs = 210_000L,
            mediaUri = "",
            genre = firstGenre
        )
    }

    private fun interleaveByLanguage(songs: List<OnboardingSong>): List<OnboardingSong> {
        if (songs.size <= 3) return songs
        val buckets = LinkedHashMap<String, ArrayDeque<OnboardingSong>>()
        for (song in songs) {
            buckets.getOrPut(song.language) { ArrayDeque() }.addLast(song)
        }
        val keys = buckets.keys.toList()
        val result = ArrayList<OnboardingSong>(songs.size)
        while (result.size < songs.size) {
            var addedInRound = false
            for (k in keys) {
                val q = buckets[k] ?: continue
                if (q.isNotEmpty()) {
                    result.add(q.removeFirst())
                    addedInRound = true
                }
            }
            if (!addedInRound) break
        }
        return result
    }

    private fun inferLanguageFromTrack(track: Track): String {
        val hay = "${track.title} ${track.artistName} ${track.albumName} ${track.genre}".lowercase(Locale.ROOT)
        val primary = EchoRecommendationEngine.primaryArtistOf(track.artistName).lowercase(Locale.ROOT)
        val catalogArtist = artistByNameLower[primary]
        if (catalogArtist != null) {
            return catalogArtist.languages.firstOrNull() ?: "Global"
        }
        return when {
            listOf("telugu", "devara", "pushpa", "kalki", "guntur", "sita ramam", "rrr", "sid sriram", "thaman", "devi sri").any { hay.contains(it) } -> "Telugu"
            listOf("hindi", "bollywood", "arijit", "pritam", "brahmastra", "jawan", "pathaan").any { hay.contains(it) } -> "Hindi"
            listOf("tamil", "kollywood", "ilaiyaraaja", "yuvan", "jailer", "leo").any { hay.contains(it) } -> "Tamil"
            listOf("punjabi", "diljit", "karan aujla", "sidhu moose", "ap dhillon").any { hay.contains(it) } -> "Punjabi"
            listOf("malayalam", "mollywood", "sushin", "aavesham", "hridayam").any { hay.contains(it) } -> "Malayalam"
            listOf("kannada", "kgf", "kantara", "ravi basrur").any { hay.contains(it) } -> "Kannada"
            listOf("marathi", "zingaat", "ajay-atul", "sairat").any { hay.contains(it) } -> "Marathi"
            listOf("bengali", "anupam roy", "rupam").any { hay.contains(it) } -> "Bengali"
            listOf("gujarati", "khalasi", "chogada", "garba").any { hay.contains(it) } -> "Gujarati"
            listOf("bhojpuri", "pawan singh", "khesari").any { hay.contains(it) } -> "Bhojpuri"
            listOf("urdu", "coke studio", "pasoori", "nusrat", "rahat").any { hay.contains(it) } -> "Urdu"
            listOf("bts", "blackpink", "newjeans", "jung kook", "k-pop", "korean", "stray kids", "twice").any { hay.contains(it) } -> "Korean"
            listOf("yoasobi", "kenshi yonezu", "fujii kaze", "j-pop", "japanese", "anime").any { hay.contains(it) } -> "Japanese"
            listOf("bad bunny", "shakira", "karol g", "j balvin", "reggaeton", "latin", "spanish").any { hay.contains(it) } -> "Spanish"
            listOf("amr diab", "nancy ajram", "arabic").any { hay.contains(it) } -> "Arabic"
            listOf("stromae", "indila", "french").any { hay.contains(it) } -> "French"
            listOf("anitta", "alok", "portuguese", "brazil").any { hay.contains(it) } -> "Portuguese"
            else -> "English"
        }
    }

    private fun isIndianTrack(track: Track): Boolean {
        val lang = inferLanguageFromTrack(track)
        return lang in INDIAN_LANGUAGES
    }
}
