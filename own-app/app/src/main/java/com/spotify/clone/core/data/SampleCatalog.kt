package com.spotify.clone.core.data

import com.spotify.clone.core.model.Album
import com.spotify.clone.core.model.Artist
import com.spotify.clone.core.model.LyricLine
import com.spotify.clone.core.model.Playlist
import com.spotify.clone.core.model.Track

/**
 * Curated catalog centered on Indian and Telugu cinema, Tollywood blockbusters,
 * soulful regional melodies, mass dance anthems, and contemporary Telugu indie.
 * Configured with authentic official studio artwork and movie posters.
 */
object SampleCatalog {

    val sampleTracks = listOf(
        Track(
            id = "track_chuttamalle",
            title = "Chuttamalle",
            artistName = "Shilpa Rao, Anirudh Ravichander",
            albumName = "Devara: Part 1",
            coverArtUrl = "https://c.saavncdn.com/313/Devara-Part-1-Telugu-Telugu-2024-20240926171010-500x500.jpg",
            durationMs = 224000L,
            mediaUri = "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Kangaroo_MusiQue_-_The_Neverending_Story.mp3",
            lyrics = listOf(
                LyricLine(0L, "Chuttamalle chuttesaave thuntari chupultho"),
                LyricLine(14000L, "Chuttukora maatalatho allari reppalatho"),
                LyricLine(28000L, "Gundelona rangula raagam mogisthunnavule"),
                LyricLine(42000L, "Kallathoti kaatesthune oorantha thelisevela"),
                LyricLine(56000L, "Chuttamalle chuttesaave thuntari chupultho")
            )
        ),
        Track(
            id = "track_pushpa_pushpa",
            title = "Pushpa Pushpa",
            artistName = "Nakash Aziz, Deepak Blue, Devi Sri Prasad",
            albumName = "Pushpa 2: The Rule",
            coverArtUrl = "https://c.saavncdn.com/366/Pushpa-2-The-Rule-Telugu-Telugu-2024-20241205211012-500x500.jpg",
            durationMs = 266000L,
            mediaUri = "https://commondatastorage.googleapis.com/codeskulptor-demos/riceracer_assets/music/win.ogg",
            lyrics = listOf(
                LyricLine(0L, "Pushpa Pushpa Pushpa Pushpa Raj"),
                LyricLine(14000L, "Taggede le antu vachade"),
                LyricLine(28000L, "Asalu taggede le eppudu chusina"),
                LyricLine(42000L, "Rajula rajura mana Pushpa Raj"),
                LyricLine(56000L, "Thokkukuntu poye veta kathi Pushpa")
            )
        ),
        Track(
            id = "track_samajavaragamana",
            title = "Samajavaragamana",
            artistName = "Sid Sriram, Thaman S",
            albumName = "Ala Vaikunthapurramuloo",
            coverArtUrl = "https://c.saavncdn.com/517/Ala-Vaikunthapurramuloo-Telugu-2019-20200116144338-500x500.jpg",
            durationMs = 214000L,
            mediaUri = "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Sevish_-__nbsp_.mp3",
            lyrics = listOf(
                LyricLine(0L, "Samajavaragamana ninu choosi aaga galana"),
                LyricLine(16000L, "Manasu meeda vayasuki unna aasa aapanaina"),
                LyricLine(32000L, "Nee kaallani pattuku vadalanannavi chude naa kallu"),
                LyricLine(48000L, "A choopulatho champaku nanne ammayo challa gaallo")
            )
        ),
        Track(
            id = "track_inthandham",
            title = "Inthandham",
            artistName = "S.P. Charan, Vishal Chandrashekhar",
            albumName = "Sita Ramam",
            coverArtUrl = "https://c.saavncdn.com/041/Sita-Ramam-Telugu-Original-Motion-Picture-Soundtrack-Telugu-2022-20220802140738-500x500.jpg",
            durationMs = 217000L,
            mediaUri = "https://commondatastorage.googleapis.com/codeskulptor-assets/Epoq-Lepidoptera.ogg",
            lyrics = listOf(
                LyricLine(0L, "Inthandham daari mabbula naduma"),
                LyricLine(14000L, "Inthandham vennela kurise kshanamo"),
                LyricLine(28000L, "Nee chelimi kore gundeku thelise"),
                LyricLine(42000L, "Sita Ramula prema geetham")
            )
        ),
        Track(
            id = "track_samayama",
            title = "Samayama",
            artistName = "Anurag Kulkarni, Sithara, Hesham Abdul Wahab",
            albumName = "Hi Nanna",
            coverArtUrl = "https://c.saavncdn.com/307/Samayama-From-Hi-Nanna-Telugu-2023-20230918164922-500x500.jpg",
            durationMs = 238000L,
            mediaUri = "https://commondatastorage.googleapis.com/codeskulptor-demos/riceracer_assets/music/race.ogg",
            lyrics = listOf(
                LyricLine(0L, "Samayama aagi poraa konchem sepu"),
                LyricLine(15000L, "E kshanam cheripovaddu manasula chupu"),
                LyricLine(30000L, "Nuvvante pranam ani thelisindi nedu"),
                LyricLine(45000L, "Samayama samayama aagumaa")
            )
        ),
        Track(
            id = "track_kurchi",
            title = "Kurchi Madathapetti",
            artistName = "Sahithi Chaganti, Sri Krishna, Thaman S",
            albumName = "Guntur Kaaram",
            coverArtUrl = "https://c.saavncdn.com/000/Guntur-Kaaram-Telugu-2023-20240126145901-500x500.jpg",
            durationMs = 205000L,
            mediaUri = "https://commondatastorage.googleapis.com/codeskulptor-demos/riceracer_assets/music/start.ogg",
            lyrics = listOf(
                LyricLine(0L, "Kurchi madathapetti kottinatte untadi"),
                LyricLine(12000L, "Masala daawat ki vachinattu untadi"),
                LyricLine(24000L, "Steppu stepu vesthe ground adhirinatte untadi"),
                LyricLine(36000L, "Babu mass dhoom dhaam kurchi madathapetti")
            )
        ),
        Track(
            id = "track_naatu_naatu",
            title = "Naatu Naatu",
            artistName = "Rahul Sipligunj, Kaala Bhairava, M.M. Keeravani",
            albumName = "RRR",
            coverArtUrl = "https://c.saavncdn.com/683/RRR-Telugu-Telugu-2022-20250828171313-500x500.jpg",
            durationMs = 215000L,
            mediaUri = "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Kangaroo_MusiQue_-_The_Neverending_Story.mp3",
            lyrics = listOf(
                LyricLine(0L, "Naatu Naatu Naatu Naatu Veera Naatu"),
                LyricLine(14000L, "Pola gattu dummulona potlaram katti"),
                LyricLine(28000L, "Ghallu ghalluna kaalu kadipi kummudu kottu"),
                LyricLine(42000L, "Naatu Naatu Naatu Naatu Veera Naatu")
            )
        ),
        Track(
            id = "track_inkem_inkem",
            title = "Inkem Inkem Inkem Kaavaale",
            artistName = "Sid Sriram, Gopi Sundar",
            albumName = "Geetha Govindam",
            coverArtUrl = "https://c.saavncdn.com/237/Geetha-Govindam-Telugu-2018-20180921-500x500.jpg",
            durationMs = 267000L,
            mediaUri = "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Sevish_-__nbsp_.mp3",
            lyrics = listOf(
                LyricLine(0L, "Inkem inkem inkem kaavaale chaale idhi chaale"),
                LyricLine(15000L, "Neeve unte naatho paate jeevithame panduge"),
                LyricLine(30000L, "Kannula munde nuvvu kanipisthe chaalu"),
                LyricLine(45000L, "Kallaloki vachi nanne allukundhi nee jolu")
            )
        ),
        Track(
            id = "track_bhairava_anthem",
            title = "Bhairava Anthem",
            artistName = "Diljit Dosanjh, Santhosh Narayanan",
            albumName = "Kalki 2898 AD",
            coverArtUrl = "https://c.saavncdn.com/888/Kalki-2898-Ad-Telugu-Telugu-2024-20240712063717-500x500.jpg",
            durationMs = 176000L,
            mediaUri = "https://commondatastorage.googleapis.com/codeskulptor-assets/Epoq-Lepidoptera.ogg",
            lyrics = listOf(
                LyricLine(0L, "Bhairava vachaduraa kaalaniki adhipathi"),
                LyricLine(14000L, "Kalki jagathiki nayakudu mana sathi"),
                LyricLine(28000L, "Adharaledu bedharaledu dhoosukelthadu"),
                LyricLine(42000L, "Bhairava Bhairava Anthem")
            )
        ),
        Track(
            id = "track_oorantha",
            title = "Oorantha Anukuntunnaru",
            artistName = "Ram Miriyala",
            albumName = "Balagam",
            coverArtUrl = "https://c.saavncdn.com/000/Balagam-Telugu-2023-20230304184334-500x500.jpg",
            durationMs = 242000L,
            mediaUri = "https://commondatastorage.googleapis.com/codeskulptor-demos/riceracer_assets/music/win.ogg",
            lyrics = listOf(
                LyricLine(0L, "Oorantha anukuntunnaru mana manchi premala gurinchi"),
                LyricLine(15000L, "Matti vaasana thechina janapada paata"),
                LyricLine(30000L, "Gunde lothula nunchi vacchina mata"),
                LyricLine(45000L, "Oorantha oorantha choodu")
            )
        ),
        Track(
            id = "track_prema_meghaalila",
            title = "O Rendu Prema Meghaalila",
            artistName = "Sreerama Chandra, Vijai Bulganin",
            albumName = "Baby",
            coverArtUrl = "https://c.saavncdn.com/732/Baby-Telugu-2023-20230710131123-500x500.jpg",
            durationMs = 278000L,
            mediaUri = "https://commondatastorage.googleapis.com/codeskulptor-demos/riceracer_assets/music/race.ogg",
            lyrics = listOf(
                LyricLine(0L, "O rendu prema meghaalila kalisaayi ninna"),
                LyricLine(14000L, "Varsham laaga vachi manasulo munigina"),
                LyricLine(28000L, "Gnapakaale enno thechindi e prema geetham"),
                LyricLine(42000L, "O rendu prema meghaalila")
            )
        ),
        Track(
            id = "track_fear_song",
            title = "Fear Song",
            artistName = "Anirudh Ravichander",
            albumName = "Devara: Part 1",
            coverArtUrl = "https://c.saavncdn.com/313/Devara-Part-1-Telugu-Telugu-2024-20240926171010-500x500.jpg",
            durationMs = 196000L,
            mediaUri = "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Kangaroo_MusiQue_-_The_Neverending_Story.mp3",
            lyrics = listOf(
                LyricLine(0L, "All hail the tiger! Fear song"),
                LyricLine(14000L, "Chora chora chora samudraanne daate veera"),
                LyricLine(28000L, "Rakthame aahuti chese shouryam"),
                LyricLine(42000L, "Devara vachadu bhayame parigette vela")
            )
        )
    )

    val samplePlaylists = listOf(
        Playlist(
            id = "pl_devara",
            name = "Devara: Part 1",
            description = "Anirudh Ravichander • Chuttamalle, Fear Song & Daavudi",
            coverArtUrl = "https://c.saavncdn.com/313/Devara-Part-1-Telugu-Telugu-2024-20240926171010-500x500.jpg",
            ownerName = "Anirudh Ravichander",
            tracks = listOf(sampleTracks[0], sampleTracks[11], sampleTracks[5])
        ),
        Playlist(
            id = "pl_pushpa2",
            name = "Pushpa 2: The Rule",
            description = "Devi Sri Prasad • Pushpa Pushpa, Sooseki & Peelings",
            coverArtUrl = "https://c.saavncdn.com/366/Pushpa-2-The-Rule-Telugu-Telugu-2024-20241205211012-500x500.jpg",
            ownerName = "Devi Sri Prasad",
            tracks = listOf(sampleTracks[1], sampleTracks[5], sampleTracks[6])
        ),
        Playlist(
            id = "pl_sita_ramam",
            name = "Sita Ramam",
            description = "Vishal Chandrashekhar • Inthandham, Oh Sita Hey Rama & Kaanunna Kalyanam",
            coverArtUrl = "https://c.saavncdn.com/041/Sita-Ramam-Telugu-Original-Motion-Picture-Soundtrack-Telugu-2022-20220802140738-500x500.jpg",
            ownerName = "Vishal Chandrashekhar",
            tracks = listOf(sampleTracks[3], sampleTracks[4], sampleTracks[2])
        ),
        Playlist(
            id = "pl_kalki_2898",
            name = "Kalki 2898 AD",
            description = "Santhosh Narayanan • Bhairava Anthem, Ta Takkara & Theme of Kalki",
            coverArtUrl = "https://c.saavncdn.com/888/Kalki-2898-Ad-Telugu-Telugu-2024-20240712063717-500x500.jpg",
            ownerName = "Santhosh Narayanan",
            tracks = listOf(sampleTracks[8], sampleTracks[1], sampleTracks[6])
        ),
        Playlist(
            id = "pl_ala_vaikunthapurramuloo",
            name = "Ala Vaikunthapurramuloo",
            description = "Thaman S • Samajavaragamana, Butta Bomma & Ramuloo Ramulaa",
            coverArtUrl = "https://c.saavncdn.com/517/Ala-Vaikunthapurramuloo-Telugu-2019-20200116144338-500x500.jpg",
            ownerName = "Thaman S",
            tracks = listOf(sampleTracks[2], sampleTracks[7], sampleTracks[5])
        ),
        Playlist(
            id = "pl_hi_nanna",
            name = "Hi Nanna",
            description = "Hesham Abdul Wahab • Samayama, Ammaadi & Gaaju Bomma",
            coverArtUrl = "https://c.saavncdn.com/307/Samayama-From-Hi-Nanna-Telugu-2023-20230918164922-500x500.jpg",
            ownerName = "Hesham Abdul Wahab",
            tracks = listOf(sampleTracks[4], sampleTracks[3], sampleTracks[0])
        ),
        Playlist(
            id = "pl_rrr",
            name = "RRR",
            description = "M.M. Keeravani • Naatu Naatu, Komuram Bheemudo & Dosti",
            coverArtUrl = "https://c.saavncdn.com/683/RRR-Telugu-Telugu-2022-20250828171313-500x500.jpg",
            ownerName = "M.M. Keeravani",
            tracks = listOf(sampleTracks[6], sampleTracks[1], sampleTracks[5])
        ),
        Playlist(
            id = "pl_guntur_kaaram",
            name = "Guntur Kaaram",
            description = "Thaman S • Kurchi Madathapetti, Dum Masala & Amma Song",
            coverArtUrl = "https://c.saavncdn.com/000/Guntur-Kaaram-Telugu-2023-20240126145901-500x500.jpg",
            ownerName = "Thaman S",
            tracks = listOf(sampleTracks[5], sampleTracks[1], sampleTracks[2])
        ),
        Playlist(
            id = "pl_geetha_govindam",
            name = "Geetha Govindam",
            description = "Gopi Sundar • Inkem Inkem Inkem Kaavaale & Vachindamma",
            coverArtUrl = "https://c.saavncdn.com/237/Geetha-Govindam-Telugu-2018-20180921-500x500.jpg",
            ownerName = "Gopi Sundar",
            tracks = listOf(sampleTracks[7], sampleTracks[2], sampleTracks[3])
        ),
        Playlist(
            id = "pl_kancharapalem",
            name = "C/o Kancharapalem",
            description = "Sweekar Agasthi • Asha Pasham & Pure Village Soul",
            coverArtUrl = "https://c.saavncdn.com/000/Balagam-Telugu-2023-20230304184334-500x500.jpg",
            ownerName = "Sweekar Agasthi",
            tracks = listOf(sampleTracks[9], sampleTracks[3], sampleTracks[7])
        ),
        Playlist(
            id = "pl_dasara",
            name = "Dasara",
            description = "Santhosh Narayanan • Chamkeela Angeelesi & Dhoom Dhaam",
            coverArtUrl = "https://c.saavncdn.com/082/Dasara-Telugu-Telugu-2023-20230419173423-500x500.jpg",
            ownerName = "Santhosh Narayanan",
            tracks = listOf(sampleTracks[5], sampleTracks[1], sampleTracks[8])
        ),
        Playlist(
            id = "pl_jersey",
            name = "Jersey",
            description = "Anirudh Ravichander • Spirit of Jersey & Adhento Gaani",
            coverArtUrl = "https://c.saavncdn.com/623/Jersey-Telugu-2019-20190427102305-500x500.jpg",
            ownerName = "Anirudh Ravichander",
            tracks = listOf(sampleTracks[3], sampleTracks[4], sampleTracks[0])
        ),
        Playlist(
            id = "pl_baby",
            name = "Baby",
            description = "Vijai Bulganin • O Rendu Prema Meghaalila & Deva Raaja",
            coverArtUrl = "https://c.saavncdn.com/732/Baby-Telugu-2023-20230710131123-500x500.jpg",
            ownerName = "Vijai Bulganin",
            tracks = listOf(sampleTracks[10], sampleTracks[3], sampleTracks[4])
        ),

        // Personalized "Made For You" Packs for Indian / Telugu music lovers
        Playlist(
            id = "pl_daily_mix_telugu",
            name = "Daily Telugu Mix",
            description = "A personalized blend of your favorite Tollywood hits & fresh melodies.",
            coverArtUrl = "https://c.saavncdn.com/313/Devara-Part-1-Telugu-Telugu-2024-20240926171010-500x500.jpg",
            ownerName = "ECHO For You",
            tracks = sampleTracks
        ),
        Playlist(
            id = "pl_hyderabad_daily",
            name = "Hyderabad Daily 50",
            description = "The most streamed Telugu tracks in Hyderabad and across AP & Telangana.",
            coverArtUrl = "https://c.saavncdn.com/366/Pushpa-2-The-Rule-Telugu-Telugu-2024-20241205211012-500x500.jpg",
            ownerName = "Tollywood Charts",
            tracks = sampleTracks
        ),
        Playlist(
            id = "pl_telugu_melodies",
            name = "Telugu Melodies & Heartstrings",
            description = "Soulful melodies from Sid Sriram, Shreya Ghoshal & Anurag Kulkarni.",
            coverArtUrl = "https://c.saavncdn.com/041/Sita-Ramam-Telugu-Original-Motion-Picture-Soundtrack-Telugu-2022-20220802140738-500x500.jpg",
            ownerName = "ECHO Curators",
            tracks = listOf(sampleTracks[0], sampleTracks[2], sampleTracks[3], sampleTracks[4], sampleTracks[7])
        ),
        Playlist(
            id = "pl_tollywood_mass",
            name = "Tollywood Mass Beats & Energy",
            description = "High-octane commercial dance tracks from Thaman S, DSP & Anirudh.",
            coverArtUrl = "https://c.saavncdn.com/000/Guntur-Kaaram-Telugu-2023-20240126145901-500x500.jpg",
            ownerName = "ECHO Curators",
            tracks = listOf(sampleTracks[1], sampleTracks[5], sampleTracks[6], sampleTracks[8])
        ),
        Playlist(
            id = "pl_telugu_lofi",
            name = "Late Night Telugu Lo-Fi",
            description = "Slowed & reverb Telugu acoustic beats for midnight tranquility and deep relaxation.",
            coverArtUrl = "https://c.saavncdn.com/307/Samayama-From-Hi-Nanna-Telugu-2023-20230918164922-500x500.jpg",
            ownerName = "ECHO Curators",
            tracks = listOf(sampleTracks[0], sampleTracks[3], sampleTracks[4], sampleTracks[9], sampleTracks[10])
        ),
        Playlist(
            id = "pl_telugu_indie",
            name = "Telugu Indie & Folk Wave",
            description = "Modern independent Telugu voices: Ram Miriyala, Vivek Sagar & Chowraasta.",
            coverArtUrl = "https://c.saavncdn.com/000/Balagam-Telugu-2023-20230304184334-500x500.jpg",
            ownerName = "ECHO Curators",
            tracks = listOf(sampleTracks[9], sampleTracks[0], sampleTracks[3])
        ),
        Playlist(
            id = "pl_devotional",
            name = "Suprabhatam & Sacred Peace",
            description = "Divine morning chants, Annamayya keerthanas & peaceful ragas for inner harmony.",
            coverArtUrl = "https://c.saavncdn.com/041/Sita-Ramam-Telugu-Original-Motion-Picture-Soundtrack-Telugu-2022-20220802140738-500x500.jpg",
            ownerName = "ECHO Curators",
            tracks = listOf(sampleTracks[3], sampleTracks[4], sampleTracks[7])
        ),
        Playlist(
            id = "pl_liked_songs",
            name = "Liked Songs",
            description = "Your saved songs in one place.",
            coverArtUrl = "https://c.saavncdn.com/313/Devara-Part-1-Telugu-Telugu-2024-20240926171010-500x500.jpg",
            ownerName = "You",
            tracks = listOf(sampleTracks[0], sampleTracks[1], sampleTracks[2])
        )
    )

    val sampleArtists = listOf(
        Artist(
            id = "art_anirudh",
            name = "Anirudh Ravichander",
            avatarUrl = "https://c.saavncdn.com/artists/Anirudh_Ravichander_003_20260121134149_500x500.jpg",
            monthlyListeners = 28400000L,
            topTracks = listOf(sampleTracks[0], sampleTracks[11])
        ),
        Artist(
            id = "art_sid_sriram",
            name = "Sid Sriram",
            avatarUrl = "https://c.saavncdn.com/artists/Sid_Sriram_005_20240425180600_500x500.jpg",
            monthlyListeners = 19200000L,
            topTracks = listOf(sampleTracks[2], sampleTracks[7])
        ),
        Artist(
            id = "art_thaman",
            name = "S.S. Thaman",
            avatarUrl = "https://c.saavncdn.com/artists/Thaman_S__007_20231106094011_500x500.jpg",
            monthlyListeners = 16800000L,
            topTracks = listOf(sampleTracks[2], sampleTracks[5])
        ),
        Artist(
            id = "art_dsp",
            name = "Devi Sri Prasad",
            avatarUrl = "https://c.saavncdn.com/artists/Devi_Sri_Prasad_008_20250619062824_500x500.jpg",
            monthlyListeners = 14500000L,
            topTracks = listOf(sampleTracks[1])
        ),
        Artist(
            id = "art_keeravani",
            name = "M.M. Keeravani",
            avatarUrl = "https://c.saavncdn.com/artists/M__M__Keeravani_002_20240129101710_500x500.jpg",
            monthlyListeners = 11200000L,
            topTracks = listOf(sampleTracks[6])
        ),
        Artist(
            id = "art_hesham",
            name = "Hesham Abdul Wahab",
            avatarUrl = "https://c.saavncdn.com/artists/Hesham_Abdul_Wahab_001_20220919094035_500x500.jpg",
            monthlyListeners = 8900000L,
            topTracks = listOf(sampleTracks[4])
        ),
        Artist(
            id = "art_ram_miriyala",
            name = "Ram Miriyala",
            avatarUrl = "https://c.saavncdn.com/artists/Ram_Miriyala_001_20250911063558_500x500.jpg",
            monthlyListeners = 6400000L,
            topTracks = listOf(sampleTracks[9])
        ),
        Artist(
            id = "art_spb",
            name = "S.P. Balasubrahmanyam",
            avatarUrl = "https://c.saavncdn.com/artists/S_P_Balasubrahmanyam_500x500.jpg",
            monthlyListeners = 12500000L,
            topTracks = listOf(sampleTracks[3])
        )
    )
}
