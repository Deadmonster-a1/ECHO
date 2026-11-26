package com.spotify.clone.core.model

import java.util.Locale

/**
 * Structured genre taxonomy for ECHO's same-genre radio and Spotify-style
 * recommendation & queue ordering engine.
 */
enum class MusicGenre(
    val id: String,
    val displayName: String,
    val shortLabel: String,
    val searchQueries: List<String>,
    val keywords: List<String>
) {
    ALL(
        id = "all",
        displayName = "All Genres",
        shortLabel = "All",
        searchQueries = listOf("top hits", "trending songs", "viral hits"),
        keywords = emptyList()
    ),
    TOL_TELUGU(
        id = "tollywood",
        displayName = "Tollywood & Telugu",
        shortLabel = "Telugu",
        searchQueries = listOf("telugu top hits", "anirudh telugu", "thaman telugu hits", "dsp telugu"),
        keywords = listOf(
            "telugu", "tollywood", "devara", "pushpa", "kalki", "guntur", "sita ramam",
            "vaikunthapurramuloo", "rrr", "hi nanna", "geetha govindam", "dasara",
            "chuttamalle", "samajavaragamana", "naatu", "kurchi", "sooseki", "daavudi",
            "anirudh", "thaman", "devi sri prasad", "sid sriram", "keeravani", "ram miriyala",
            "kollywood", "tamil", "jailer", "hukum", "yuvan", "ilaiyaraaja"
        )
    ),
    MELODIES(
        id = "melodies",
        displayName = "Melodies & Romance",
        shortLabel = "Melodies",
        searchQueries = listOf("telugu melodies", "romantic hits", "arijit singh love songs", "sid sriram melodies"),
        keywords = listOf(
            "melod", "romance", "romantic", "love", "inthandham", "samayama", "chuttamalle",
            "samajavaragamana", "inkem", "kesariya", "tum hi ho", "apna bana le", "tera ban",
            "sid sriram", "shreya ghoshal", "arijit singh", "hesham", "prema", "kaadhal",
            "munbe vaa", "darshana", "vaseegara", "belageddu", "apsara aali", "tamally maak"
        )
    ),
    MASS_BEATS(
        id = "mass_beats",
        displayName = "Mass & Dance Anthems",
        shortLabel = "Mass Beats",
        searchQueries = listOf("telugu mass beats", "indian dance hits", "party anthems"),
        keywords = listOf(
            "mass", "dance", "party", "anthem", "item", "fear song", "daavudi", "ayudha",
            "kurchi", "dum masala", "pushpa pushpa", "kissik", "peelings", "naatu",
            "bhairava", "ta takkara", "dhoom", "rowdy", "butta bomma", "ramuloo",
            "zingaat", "illuminati", "tauba tauba", "lover", "chaleya", "hukum", "lollypop"
        )
    ),
    GLOBAL_POP(
        id = "global_pop",
        displayName = "Global Pop",
        shortLabel = "Pop",
        searchQueries = listOf("top pop hits 2026", "the weeknd taylor swift", "billboard pop"),
        keywords = listOf(
            "pop", "weeknd", "taylor swift", "dua lipa", "bruno mars", "billie eilish",
            "sabrina carpenter", "ariana grande", "olivia rodrigo", "ed sheeran", "lady gaga",
            "blinding lights", "starboy", "espresso", "levitating", "die with a smile",
            "bts", "blackpink", "jung kook", "newjeans", "rosé", "yoasobi", "shakira",
            "karol g", "rema", "calm down", "rihanna", "beyoncé", "justin bieber", "adele", "sza"
        )
    ),
    HIP_HOP(
        id = "hip_hop",
        displayName = "Hip-Hop & Rap",
        shortLabel = "Hip-Hop",
        searchQueries = listOf("hip hop rap hits", "kendrick lamar drake", "desi hip hop"),
        keywords = listOf(
            "hip hop", "hip-hop", "rap", "trap", "drill", "kendrick", "drake", "travis scott",
            "kanye", "eminem", "metro boomin", "divine", "hanumankind", "kr\$na", "badshah",
            "sidhu moose wala", "karan aujla", "shubh", "ap dhillon", "bad bunny", "central cee",
            "j. cole", "post malone", "not like us", "big dawgs", "excuses"
        )
    ),
    ELECTRONIC(
        id = "electronic",
        displayName = "Electronic & EDM",
        shortLabel = "Electronic",
        searchQueries = listOf("edm dance electronic", "house synthwave hits", "club mix"),
        keywords = listOf(
            "edm", "electronic", "house", "techno", "trance", "synth", "club", "dj",
            "alan walker", "calvin harris", "david guetta", "marshmello", "fred again", "remix",
            "avicii", "martin garrix", "daft punk", "kshmr", "nucleya", "alok", "stromae"
        )
    ),
    ROCK(
        id = "rock",
        displayName = "Rock & Alternative",
        shortLabel = "Rock",
        searchQueries = listOf("rock alternative hits", "coldplay imagine dragons", "indie rock"),
        keywords = listOf(
            "rock", "alternative", "metal", "punk", "coldplay", "imagine dragons",
            "linkin park", "arctic monkeys", "nirvana", "queen", "radiohead",
            "metallica", "kenshi yonezu", "ado", "bohemian rhapsody", "believer"
        )
    ),
    BOLLYWOOD(
        id = "bollywood",
        displayName = "Bollywood & Hindi",
        shortLabel = "Hindi",
        searchQueries = listOf("hindi top hits", "bollywood chartbusters", "pritam arijit hits"),
        keywords = listOf(
            "hindi", "bollywood", "brahmastra", "pathaan", "jawan", "animal", "stree",
            "pritam", "a.r. rahman", "shankar", "vishal", "jubin nautiyal", "atif aslam",
            "sonu nigam", "kishore kumar", "lata mangeshkar", "sachin-jigar", "amit trivedi",
            "sunidhi chauhan", "mithoon", "kk", "mohit chauhan", "ajay-atul"
        )
    ),
    LO_FI(
        id = "lofi",
        displayName = "Lo-Fi & Chill",
        shortLabel = "Lo-Fi",
        searchQueries = listOf("chill lofi beats", "acoustic late night chill", "midnight chill"),
        keywords = listOf(
            "lo-fi", "lofi", "chill", "ambient", "midnight", "late night", "sleep",
            "study", "rain", "reverie", "soft", "acoustic chill", "unplugged",
            "cigarettes after sex", "ludovico einaudi", "apocalypse", "experience"
        )
    ),
    INDIE(
        id = "indie",
        displayName = "Indie & Acoustic",
        shortLabel = "Indie",
        searchQueries = listOf("indie acoustic folk", "telugu indie songs", "anuv jain prateek kuhad"),
        keywords = listOf(
            "indie", "folk", "acoustic", "singer-songwriter", "ram miriyala", "vivek sagar",
            "balagam", "kancharapalem", "anuv jain", "prateek kuhad", "when chai met toast",
            "fujii kaze", "ali sethi", "pasoori", "khalasi", "aditya gadhvi", "anupam roy",
            "raghu dixit", "lana del rey", "tame impala"
        )
    ),
    DEVOTIONAL(
        id = "devotional",
        displayName = "Devotional & Classical",
        shortLabel = "Classical",
        searchQueries = listOf("indian classical devotional", "suprabhatam peaceful", "carnatic instrumental"),
        keywords = listOf(
            "devotional", "classical", "carnatic", "hindustani", "mantra", "suprabhatam",
            "annamayya", "bhajan", "shiva", "hanuman", "sitar", "flute", "raga",
            "nusrat fateh", "rahat fateh", "kun faya kun", "afreen", "yesudas", "joe hisaishi"
        )
    );

    /**
     * Neighbor genres (ported from RecommendationEngine _RECO_GENRE_NEIGHBORS)
     * so fallback widening never drifts into unrelated musical styles.
     */
    fun neighborGenres(): List<MusicGenre> = when (this) {
        TOL_TELUGU -> listOf(MELODIES, MASS_BEATS, INDIE)
        MELODIES -> listOf(LO_FI, TOL_TELUGU, BOLLYWOOD, INDIE)
        MASS_BEATS -> listOf(TOL_TELUGU, ELECTRONIC, HIP_HOP)
        GLOBAL_POP -> listOf(ELECTRONIC, INDIE, HIP_HOP)
        HIP_HOP -> listOf(GLOBAL_POP, MASS_BEATS, ELECTRONIC)
        ELECTRONIC -> listOf(GLOBAL_POP, MASS_BEATS, LO_FI)
        ROCK -> listOf(INDIE, GLOBAL_POP)
        BOLLYWOOD -> listOf(MELODIES, INDIE, LO_FI)
        LO_FI -> listOf(MELODIES, INDIE, ELECTRONIC)
        INDIE -> listOf(MELODIES, LO_FI, ROCK, TOL_TELUGU)
        DEVOTIONAL -> listOf(INDIE, LO_FI, MELODIES)
        ALL -> listOf(GLOBAL_POP, TOL_TELUGU, MELODIES)
    }

    val displayNameLower: String = displayName.lowercase(Locale.ROOT)
    val shortLabelLower: String = shortLabel.lowercase(Locale.ROOT)

    fun matchesTrack(track: Track): Boolean {
        if (this == ALL) return true
        val declared = track.genre.trim().lowercase(Locale.ROOT)
        if (declared.isNotBlank() && declared != "all" && declared != "music") {
            if (declared == id || declared == displayNameLower ||
                declared == shortLabelLower
            ) {
                return true
            }
            if (keywords.any { declared.contains(it) }) return true
        }
        val haystack = "${track.title} ${track.artistName} ${track.albumName}".lowercase(Locale.ROOT)
        return keywords.any { haystack.contains(it) } || inferGenre(track) == this
    }

    companion object {
        val selectableGenres: List<MusicGenre> = entries.filter { it != ALL }

        fun fromId(id: String?): MusicGenre {
            if (id.isNullOrBlank()) return ALL
            return entries.find {
                it.id.equals(id, ignoreCase = true) ||
                    it.displayName.equals(id, ignoreCase = true) ||
                    it.shortLabel.equals(id, ignoreCase = true)
            } ?: ALL
        }

        /**
         * Infers the strongest matching genre from explicit tag + track metadata.
         */
        fun inferGenre(track: Track): MusicGenre {
            val declared = track.genre.trim().lowercase(Locale.ROOT)
            if (declared.isNotBlank() && declared != "all" && declared != "music") {
                selectableGenres.firstOrNull { g ->
                    declared == g.id ||
                        declared == g.shortLabelLower ||
                        declared.contains(g.shortLabelLower)
                }?.let { return it }
            }

            val haystack = "${track.title} ${track.artistName} ${track.albumName} $declared"
                .lowercase(Locale.ROOT)

            var bestGenre = GLOBAL_POP
            var bestScore = 0
            for (genre in selectableGenres) {
                val hits = genre.keywords.count { haystack.contains(it) }
                if (hits > bestScore) {
                    bestScore = hits
                    bestGenre = genre
                }
            }
            return if (bestScore > 0) bestGenre else GLOBAL_POP
        }
    }
}
