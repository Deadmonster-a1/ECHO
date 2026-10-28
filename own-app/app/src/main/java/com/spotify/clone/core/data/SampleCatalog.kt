package com.spotify.clone.core.data

import com.spotify.clone.core.model.Album
import com.spotify.clone.core.model.Artist
import com.spotify.clone.core.model.LyricLine
import com.spotify.clone.core.model.Playlist
import com.spotify.clone.core.model.Track

/**
 * Curated catalog centered on Indian and Telugu cinema, Tollywood blockbusters,
 * soulful regional melodies, mass dance anthems, and contemporary Telugu indie.
 * Configured with authentic official studio artwork, synchronized lyrics,
 * and dynamic stream resolution for full-length 160/320 kbps playback.
 */
object SampleCatalog {

    // --- Devara: Part 1 ---
    val trackChuttamalle = Track(
        id = "track_chuttamalle",
        title = "Chuttamalle",
        artistName = "Shilpa Rao, Anirudh Ravichander",
        albumName = "Devara: Part 1",
        coverArtUrl = "https://c.saavncdn.com/313/Devara-Part-1-Telugu-Telugu-2024-20240926171010-500x500.jpg",
        durationMs = 222000L,
        mediaUri = "saavn_enc:ID2ieOjCrwfgWvL5sXl4B1ImC5QfbsDyMaCcla+iAEnWKGhjucPDOQMuVXLGmbqF4ufcJUQKLmvyzYWZhzpVWBw7tS9a8Gtq",
        lyrics = listOf(
            LyricLine(0L, "Chuttamalle chuttesaave thuntari chupultho"),
            LyricLine(14000L, "Chuttukora maatalatho allari reppalatho"),
            LyricLine(28000L, "Gundelona rangula raagam mogisthunnavule"),
            LyricLine(42000L, "Kallathoti kaatesthune oorantha thelisevela"),
            LyricLine(56000L, "Chuttamalle chuttesaave thuntari chupultho")
        )
    )

    val trackFearSong = Track(
        id = "track_fear_song",
        title = "Fear Song",
        artistName = "Anirudh Ravichander",
        albumName = "Devara: Part 1",
        coverArtUrl = "https://c.saavncdn.com/313/Devara-Part-1-Telugu-Telugu-2024-20240926171010-500x500.jpg",
        durationMs = 195000L,
        mediaUri = "saavn_enc:ID2ieOjCrwfgWvL5sXl4B1ImC5QfbsDyBBiimzn9tc9nco0itIplb91h8qnOCWfSJeSWUrTfVnObIMg+VE4Rfxw7tS9a8Gtq",
        lyrics = listOf(
            LyricLine(0L, "All hail the tiger! Fear song"),
            LyricLine(14000L, "Chora chora chora samudraanne daate veera"),
            LyricLine(28000L, "Rakthame aahuti chese shouryam"),
            LyricLine(42000L, "Devara vachadu bhayame parigette vela")
        )
    )

    val trackDaavudi = Track(
        id = "track_daavudi",
        title = "Daavudi",
        artistName = "Nakash Aziz, Akasa Singh, Anirudh Ravichander",
        albumName = "Devara: Part 1",
        coverArtUrl = "https://c.saavncdn.com/313/Devara-Part-1-Telugu-Telugu-2024-20240926171010-500x500.jpg",
        durationMs = 229000L,
        mediaUri = "saavn_enc:ID2ieOjCrwfgWvL5sXl4B1ImC5QfbsDyyLC1Sr5tlHNnAl68sYNquxL1indmYJDU/5nwdJWTO3wdiD6/VI7vghw7tS9a8Gtq",
        lyrics = listOf(
            LyricLine(0L, "Daavudi Daavudi mass step eyyi"),
            LyricLine(12000L, "Oorantha thirigi dhoom dhaam cheyyi"),
            LyricLine(24000L, "Devara beat ki kaalaina nilavade"),
            LyricLine(36000L, "Daavudi Daavudi dancelu veyyi")
        )
    )

    val trackAyudhaPooja = Track(
        id = "track_ayudha_pooja",
        title = "Ayudha Pooja",
        artistName = "Kaala Bhairava, Anirudh Ravichander",
        albumName = "Devara: Part 1",
        coverArtUrl = "https://c.saavncdn.com/313/Devara-Part-1-Telugu-Telugu-2024-20240926171010-500x500.jpg",
        durationMs = 174000L,
        mediaUri = "saavn_enc:ID2ieOjCrwfgWvL5sXl4B1ImC5QfbsDyoXmZjMjtCbg4/oPpySiQPbdXC3jQdOSVIMJs1pxvHLZg4u/AZSX3Lxw7tS9a8Gtq",
        lyrics = listOf(
            LyricLine(0L, "Ayudha Pooja cheyyara samudraputhaada"),
            LyricLine(15000L, "Kathi pattina cheyyi dharmani nilabettu"),
            LyricLine(30000L, "Shouryam thoti kummudu kottu"),
            LyricLine(45000L, "Devara devudu vachina vela")
        )
    )

    // --- Pushpa 2: The Rule ---
    val trackPushpaPushpa = Track(
        id = "track_pushpa_pushpa",
        title = "Pushpa Pushpa",
        artistName = "Nakash Aziz, Deepak Blue, Devi Sri Prasad",
        albumName = "Pushpa 2: The Rule",
        coverArtUrl = "https://c.saavncdn.com/366/Pushpa-2-The-Rule-Telugu-Telugu-2024-20241205211012-500x500.jpg",
        durationMs = 256000L,
        mediaUri = "saavn_enc:ID2ieOjCrwfgWvL5sXl4B1ImC5QfbsDymza0K1NmnzXPT5c4khZBeZ7WgcFEV/SR6IIp6YDLVyE8Fpck9hNy4hw7tS9a8Gtq",
        lyrics = listOf(
            LyricLine(0L, "Pushpa Pushpa Pushpa Pushpa Raj"),
            LyricLine(14000L, "Taggede le antu vachade"),
            LyricLine(28000L, "Asalu taggede le eppudu chusina"),
            LyricLine(42000L, "Rajula rajura mana Pushpa Raj"),
            LyricLine(56000L, "Thokkukuntu poye veta kathi Pushpa")
        )
    )

    val trackSooseki = Track(
        id = "track_sooseki",
        title = "Sooseki",
        artistName = "Shreya Ghoshal, Devi Sri Prasad",
        albumName = "Pushpa 2: The Rule",
        coverArtUrl = "https://c.saavncdn.com/366/Pushpa-2-The-Rule-Telugu-Telugu-2024-20241205211012-500x500.jpg",
        durationMs = 260000L,
        mediaUri = "saavn_enc:ID2ieOjCrwfgWvL5sXl4B1ImC5QfbsDyVs4cGWximUIfqMBYOjdt4dpOg/Y+Qy5M5u1LEbTalrw6cjVlmGT4Lhw7tS9a8Gtq",
        lyrics = listOf(
            LyricLine(0L, "Sooseki aggiravva laaga chuseki"),
            LyricLine(14000L, "Muddula gumpula naduma kaliseki"),
            LyricLine(28000L, "Pushpa Raj meeda asale kante kuriseki"),
            LyricLine(42000L, "Sooseki Sooseki prema tho")
        )
    )

    val trackPeelings = Track(
        id = "track_peelings",
        title = "Peelings",
        artistName = "Devi Sri Prasad, Laxmi Manchu",
        albumName = "Pushpa 2: The Rule",
        coverArtUrl = "https://c.saavncdn.com/366/Pushpa-2-The-Rule-Telugu-Telugu-2024-20241205211012-500x500.jpg",
        durationMs = 247000L,
        mediaUri = "saavn_enc:ID2ieOjCrwfgWvL5sXl4B1ImC5QfbsDyDaLFFpsCrkc/223u+UAfUUmJbRidTvaicMteyJm8zhJxYSn2hPR1Nhw7tS9a8Gtq",
        lyrics = listOf(
            LyricLine(0L, "Peelings peelings mass peelings"),
            LyricLine(14000L, "Dance floor adhiripoyina feelings"),
            LyricLine(28000L, "Pushpa 2 The Rule lo wild beat"),
            LyricLine(42000L, "Pilla chusi aageley beat")
        )
    )

    val trackKissik = Track(
        id = "track_kissik",
        title = "Kissik",
        artistName = "Sublahshini, Devi Sri Prasad",
        albumName = "Pushpa 2: The Rule",
        coverArtUrl = "https://c.saavncdn.com/366/Pushpa-2-The-Rule-Telugu-Telugu-2024-20241205211012-500x500.jpg",
        durationMs = 248000L,
        mediaUri = "saavn_enc:ID2ieOjCrwfgWvL5sXl4B1ImC5QfbsDymqqxav/w4B2dJtzHv5NHAIZksK1vNN8lSJ6czI01rtmDdeRLZU7LFhw7tS9a8Gtq",
        lyrics = listOf(
            LyricLine(0L, "Kissik kissik ani kotteti chupulu"),
            LyricLine(12000L, "Dhoom dhaam step latho allari uupulu"),
            LyricLine(24000L, "Wild fire vachindi Pushpa Rule"),
            LyricLine(36000L, "Kissik song tho mass full cool")
        )
    )

    // --- Guntur Kaaram ---
    val trackKurchi = Track(
        id = "track_kurchi",
        title = "Kurchi Madathapetti",
        artistName = "Sahithi Chaganti, Sri Krishna, Thaman S",
        albumName = "Guntur Kaaram",
        coverArtUrl = "https://c.saavncdn.com/000/Guntur-Kaaram-Telugu-2023-20240126145901-500x500.jpg",
        durationMs = 216000L,
        mediaUri = "saavn_enc:ID2ieOjCrwfgWvL5sXl4B1ImC5QfbsDyAYSpkEihdkG4+AFn1gYNkJM9bhyDhDoC00is5zg9wSEUPgFYtzlXCxw7tS9a8Gtq",
        lyrics = listOf(
            LyricLine(0L, "Kurchi madathapetti kottinatte untadi"),
            LyricLine(12000L, "Masala daawat ki vachinattu untadi"),
            LyricLine(24000L, "Steppu stepu vesthe ground adhirinatte untadi"),
            LyricLine(36000L, "Babu mass dhoom dhaam kurchi madathapetti")
        )
    )

    val trackDumMasala = Track(
        id = "track_dum_masala",
        title = "Dum Masala",
        artistName = "Sanjith Hegde, Thaman S",
        albumName = "Guntur Kaaram",
        coverArtUrl = "https://c.saavncdn.com/000/Guntur-Kaaram-Telugu-2023-20240126145901-500x500.jpg",
        durationMs = 206000L,
        mediaUri = "saavn_enc:ID2ieOjCrwfgWvL5sXl4B1ImC5QfbsDyUTCwI9AItR66rnc9xx0Tv2osLVlQ/Cixs800jzw5IP7pVlRPOuoXgxw7tS9a8Gtq",
        lyrics = listOf(
            LyricLine(0L, "Dum masala dum masala guntur spicy"),
            LyricLine(14000L, "Babu swag chusi fans antha crazy"),
            LyricLine(28000L, "Mirchi lanti massu songu"),
            LyricLine(42000L, "Dum Masala Dum Masala")
        )
    )

    val trackAmmaSong = Track(
        id = "track_amma_song",
        title = "Amma Song",
        artistName = "Javed Ali, Thaman S",
        albumName = "Guntur Kaaram",
        coverArtUrl = "https://c.saavncdn.com/000/Guntur-Kaaram-Telugu-2023-20240126145901-500x500.jpg",
        durationMs = 242000L,
        mediaUri = "saavn_enc:ID2ieOjCrwfgWvL5sXl4B1ImC5QfbsDymZ4tQpuKyr91NUzJIstrtsZPzZMuLyy+Fi4WEMqUQYSi3pZDHbI6/Bw7tS9a8Gtq",
        lyrics = listOf(
            LyricLine(0L, "Amma ane maata lo preme unnadamma"),
            LyricLine(15000L, "Gunde lothula nunchi vacche paata ide gamma"),
            LyricLine(30000L, "Nee chethi sparsha thoti bathuku veligenu"),
            LyricLine(45000L, "Amma prema veru lokaana")
        )
    )

    // --- Kalki 2898 AD ---
    val trackBhairavaAnthem = Track(
        id = "track_bhairava_anthem",
        title = "Bhairava Anthem",
        artistName = "Diljit Dosanjh, Santhosh Narayanan",
        albumName = "Kalki 2898 AD",
        coverArtUrl = "https://c.saavncdn.com/888/Kalki-2898-Ad-Telugu-Telugu-2024-20240712063717-500x500.jpg",
        durationMs = 163000L,
        mediaUri = "saavn_enc:ID2ieOjCrwfgWvL5sXl4B1ImC5QfbsDynszYR1k/ojCtZwDkcJXW+nWktSBSdDYVSeGbzIIyelaOUnKyWJfsLBw7tS9a8Gtq",
        lyrics = listOf(
            LyricLine(0L, "Bhairava vachaduraa kaalaniki adhipathi"),
            LyricLine(14000L, "Kalki jagathiki nayakudu mana sathi"),
            LyricLine(28000L, "Adharaledu bedharaledu dhoosukelthadu"),
            LyricLine(42000L, "Bhairava Bhairava Anthem")
        )
    )

    val trackTaTakkara = Track(
        id = "track_ta_takkara",
        title = "Ta Takkara",
        artistName = "Santhosh Narayanan, Sanjith Hegde",
        albumName = "Kalki 2898 AD",
        coverArtUrl = "https://c.saavncdn.com/888/Kalki-2898-Ad-Telugu-Telugu-2024-20240712063717-500x500.jpg",
        durationMs = 207000L,
        mediaUri = "saavn_enc:ID2ieOjCrwfgWvL5sXl4B1ImC5QfbsDyiY+mB17omEQEp5JgVa2nqna1v2myXZ/6llALll5mW9S4k4zrRajvexw7tS9a8Gtq",
        lyrics = listOf(
            LyricLine(0L, "Ta takkara complex loki dhoosukochi"),
            LyricLine(14000L, "Kalalanni nijamaina vela chusi"),
            LyricLine(28000L, "Future sound futuristic beat"),
            LyricLine(42000L, "Ta Takkara Ta Takkara")
        )
    )

    val trackThemeOfKalki = Track(
        id = "track_theme_of_kalki",
        title = "Theme of Kalki",
        artistName = "Kaala Bhairava, Santhosh Narayanan",
        albumName = "Kalki 2898 AD",
        coverArtUrl = "https://c.saavncdn.com/888/Kalki-2898-Ad-Telugu-Telugu-2024-20240712063717-500x500.jpg",
        durationMs = 189000L,
        mediaUri = "saavn_enc:ID2ieOjCrwfgWvL5sXl4B1ImC5QfbsDykajXvJEDz3mrAAGV+xeh7a/vBHOTDVTsanq23EWQ0tUl3uHmuT6BJhw7tS9a8Gtq",
        lyrics = listOf(
            LyricLine(0L, "Kalki roopam avatharinche vela"),
            LyricLine(15000L, "Dharma sansthapanarthaya sambhavami yuge yuge"),
            LyricLine(30000L, "Shankha naadam mogenu lokaana"),
            LyricLine(45000L, "Kalki 2898 AD theme")
        )
    )

    // --- Ala Vaikunthapurramuloo ---
    val trackSamajavaragamana = Track(
        id = "track_samajavaragamana",
        title = "Samajavaragamana",
        artistName = "Sid Sriram, Thaman S",
        albumName = "Ala Vaikunthapurramuloo",
        coverArtUrl = "https://c.saavncdn.com/517/Ala-Vaikunthapurramuloo-Telugu-2019-20200116144338-500x500.jpg",
        durationMs = 219000L,
        mediaUri = "saavn_enc:ID2ieOjCrwfgWvL5sXl4B1ImC5QfbsDyBTJ4G3/I1AorZTT0gsv5kZrBofZqrazGks5ucgS6HLBm12GRaz2PMBw7tS9a8Gtq",
        lyrics = listOf(
            LyricLine(0L, "Samajavaragamana ninu choosi aaga galana"),
            LyricLine(16000L, "Manasu meeda vayasuki unna aasa aapanaina"),
            LyricLine(32000L, "Nee kaallani pattuku vadalanannavi chude naa kallu"),
            LyricLine(48000L, "A choopulatho champaku nanne ammayo challa gaallo")
        )
    )

    val trackButtaBomma = Track(
        id = "track_butta_bomma",
        title = "Butta Bomma",
        artistName = "Armaan Malik, Thaman S",
        albumName = "Ala Vaikunthapurramuloo",
        coverArtUrl = "https://c.saavncdn.com/517/Ala-Vaikunthapurramuloo-Telugu-2019-20200116144338-500x500.jpg",
        durationMs = 198000L,
        mediaUri = "saavn_enc:ID2ieOjCrwfgWvL5sXl4B1ImC5QfbsDy6cNRQV5rpqCV4BYCV1yJyIhUmSDmawrl4K+qb2UgyflhwcuQJos6BBw7tS9a8Gtq",
        lyrics = listOf(
            LyricLine(0L, "Butta bomma butta bomma nannu suttu kollavura"),
            LyricLine(14000L, "Kalla ninda aasa nimpina muddu gummavu nuvvura"),
            LyricLine(28000L, "Gaallo thele haayilo manasu munigina vela"),
            LyricLine(42000L, "Butta Bomma Butta Bomma")
        )
    )

    val trackRamulooRamulaa = Track(
        id = "track_ramuloo_ramulaa",
        title = "Ramuloo Ramulaa",
        artistName = "Anurag Kulkarni, Mangli, Thaman S",
        albumName = "Ala Vaikunthapurramuloo",
        coverArtUrl = "https://c.saavncdn.com/517/Ala-Vaikunthapurramuloo-Telugu-2019-20200116144338-500x500.jpg",
        durationMs = 245000L,
        mediaUri = "saavn_enc:ID2ieOjCrwfgWvL5sXl4B1ImC5QfbsDyBeH1z8NaJLegYmdptoH6W34g9WIY9W6OaZ96FcYP/jfUPa9/otj+kRw7tS9a8Gtq",
        lyrics = listOf(
            LyricLine(0L, "Ramuloo Ramulaa nannakada sitti pettavura"),
            LyricLine(14000L, "Gunde lothula nunchi vacche paata paadara"),
            LyricLine(28000L, "Dhammunte kummudu kottu dancelu veyyi"),
            LyricLine(42000L, "Ramuloo Ramulaa")
        )
    )

    // --- Sita Ramam ---
    val trackInthandham = Track(
        id = "track_inthandham",
        title = "Inthandham",
        artistName = "S.P. Charan, Vishal Chandrashekhar",
        albumName = "Sita Ramam",
        coverArtUrl = "https://c.saavncdn.com/041/Sita-Ramam-Telugu-Original-Motion-Picture-Soundtrack-Telugu-2022-20220802140738-500x500.jpg",
        durationMs = 218000L,
        mediaUri = "saavn_enc:ID2ieOjCrwfgWvL5sXl4B1ImC5QfbsDyYzLitKzQefYsZ0b37qj3k5mZQELrI4mYHy6fp6cdcnvjqGVpx8QTQRw7tS9a8Gtq",
        lyrics = listOf(
            LyricLine(0L, "Inthandham daari mabbula naduma"),
            LyricLine(14000L, "Inthandham vennela kurise kshanamo"),
            LyricLine(28000L, "Nee chelimi kore gundeku thelise"),
            LyricLine(42000L, "Sita Ramula prema geetham")
        )
    )

    val trackOhSita = Track(
        id = "track_oh_sita",
        title = "Oh Sita Hey Rama",
        artistName = "S.P. Charan, Ramya Behara, Vishal Chandrashekhar",
        albumName = "Sita Ramam",
        coverArtUrl = "https://c.saavncdn.com/041/Sita-Ramam-Telugu-Original-Motion-Picture-Soundtrack-Telugu-2022-20220802140738-500x500.jpg",
        durationMs = 246000L,
        mediaUri = "saavn_enc:ID2ieOjCrwfgWvL5sXl4B1ImC5QfbsDyOpwiDEJmhiWfwS0i/nc/TYHPTM7xwUqtzlI06nVIzICvf5OZt0ldcBw7tS9a8Gtq",
        lyrics = listOf(
            LyricLine(0L, "Oh Sita Hey Rama mana prema chiranjeevi"),
            LyricLine(15000L, "Lekhala thoti allina prema kavithavam ide"),
            LyricLine(30000L, "Gunde lothula daachina madhura bhaavam"),
            LyricLine(45000L, "Oh Sita Hey Rama")
        )
    )

    val trackKaanunnaKalyanam = Track(
        id = "track_kaanunna_kalyanam",
        title = "Kaanunna Kalyanam",
        artistName = "Anurag Kulkarni, Sinduri Vishal, Vishal Chandrashekhar",
        albumName = "Sita Ramam",
        coverArtUrl = "https://c.saavncdn.com/041/Sita-Ramam-Telugu-Original-Motion-Picture-Soundtrack-Telugu-2022-20220802140738-500x500.jpg",
        durationMs = 232000L,
        mediaUri = "saavn_enc:ID2ieOjCrwfgWvL5sXl4B1ImC5QfbsDyo3FJ30+AC3zGHSzJZdIYJ4PhXVnce/yW9mZI01YzVYntgAC+hGkInBw7tS9a8Gtq",
        lyrics = listOf(
            LyricLine(0L, "Kaanunna kalyanam kallaara chudali"),
            LyricLine(15000L, "Prema pelli bandham loki adugu pettali"),
            LyricLine(30000L, "Ramudu Sita thoti jathaga niliche"),
            LyricLine(45000L, "Kalyana vaibhavam")
        )
    )

    // --- RRR ---
    val trackNaatuNaatu = Track(
        id = "track_naatu_naatu",
        title = "Naatu Naatu",
        artistName = "Rahul Sipligunj, Kaala Bhairava, M.M. Keeravani",
        albumName = "RRR",
        coverArtUrl = "https://c.saavncdn.com/683/RRR-Telugu-Telugu-2022-20250828171313-500x500.jpg",
        durationMs = 214000L,
        mediaUri = "saavn_enc:ID2ieOjCrwfgWvL5sXl4B1ImC5QfbsDyK1tSoSB+oPrTJYqQ1jcTCNgc6b6boKXKeppOEkUoiK5Fp6jyXWA3QBw7tS9a8Gtq",
        lyrics = listOf(
            LyricLine(0L, "Naatu Naatu Naatu Naatu Veera Naatu"),
            LyricLine(14000L, "Pola gattu dummulona potlaram katti"),
            LyricLine(28000L, "Ghallu ghalluna kaalu kadipi kummudu kottu"),
            LyricLine(42000L, "Naatu Naatu Naatu Naatu Veera Naatu")
        )
    )

    val trackDosti = Track(
        id = "track_dosti",
        title = "Dosti",
        artistName = "Hemachandra, M.M. Keeravani",
        albumName = "RRR",
        coverArtUrl = "https://c.saavncdn.com/683/RRR-Telugu-Telugu-2022-20250828171313-500x500.jpg",
        durationMs = 340000L,
        mediaUri = "saavn_enc:ID2ieOjCrwfgWvL5sXl4B1ImC5QfbsDyCWNfpkQqJNYVawpRJ3uktWhrWcckjdcBi4qytVQodFgj46tVl5XDnhw7tS9a8Gtq",
        lyrics = listOf(
            LyricLine(0L, "Nippu thoti neellu kalise dosti geetham"),
            LyricLine(16000L, "Praanamaina iche sneham idi chirakaalam"),
            LyricLine(32000L, "Bheem Ramula dosti charithra lo niliche"),
            LyricLine(48000L, "Dosti Dosti Dosti")
        )
    )

    val trackKomuramBheemudo = Track(
        id = "track_komuram_bheemudo",
        title = "Komuram Bheemudo",
        artistName = "Kaala Bhairava, M.M. Keeravani",
        albumName = "RRR",
        coverArtUrl = "https://c.saavncdn.com/683/RRR-Telugu-Telugu-2022-20250828171313-500x500.jpg",
        durationMs = 254000L,
        mediaUri = "saavn_enc:ID2ieOjCrwfgWvL5sXl4B1ImC5QfbsDyYRJed3UIdRwBHU2mQ14OgEAnBnBJuYrttXfZeGnk19WoEvYcYesRrBw7tS9a8Gtq",
        lyrics = listOf(
            LyricLine(0L, "Komuram Bheemudo Komuram Bheemudo"),
            LyricLine(15000L, "Korra korra goddallona ninnu chusiri"),
            LyricLine(30000L, "Jal jungle zameen kosam poradina veera"),
            LyricLine(45000L, "Komuram Bheemudo")
        )
    )

    // --- Hi Nanna ---
    val trackSamayama = Track(
        id = "track_samayama",
        title = "Samayama",
        artistName = "Anurag Kulkarni, Sithara, Hesham Abdul Wahab",
        albumName = "Hi Nanna",
        coverArtUrl = "https://c.saavncdn.com/253/Hi-Nanna-Telugu-Telugu-2023-20240109191037-500x500.jpg",
        durationMs = 204000L,
        mediaUri = "saavn_enc:ID2ieOjCrwfgWvL5sXl4B1ImC5QfbsDyprimJkoXJVCdMtzDMUkTu7HaAD7oAImZx8IUcWuLgylZ0CdhfyjM5hw7tS9a8Gtq",
        lyrics = listOf(
            LyricLine(0L, "Samayama aagi poraa konchem sepu"),
            LyricLine(15000L, "E kshanam cheripovaddu manasula chupu"),
            LyricLine(30000L, "Nuvvante pranam ani thelisindi nedu"),
            LyricLine(45000L, "Samayama samayama aagumaa")
        )
    )

    val trackInkemInkem = Track(
        id = "track_inkem_inkem",
        title = "Inkem Inkem Inkem Kaavaale",
        artistName = "Sid Sriram, Gopi Sundar",
        albumName = "Geetha Govindam",
        coverArtUrl = "https://c.saavncdn.com/237/Geetha-Govindam-Telugu-2018-20180921-500x500.jpg",
        durationMs = 266000L,
        mediaUri = "saavn_enc:ID2ieOjCrwfgWvL5sXl4B1ImC5QfbsDyTwMMUO2ElmZ5QH82uuJIjRieZfpyM0PoDXkn76/rAa2csrtu8BIoSRw7tS9a8Gtq",
        lyrics = listOf(
            LyricLine(0L, "Inkem inkem inkem kaavaale chaale idhi chaale"),
            LyricLine(15000L, "Neeve unte naatho paate jeevithame panduge"),
            LyricLine(30000L, "Kannula munde nuvvu kanipisthe chaalu"),
            LyricLine(45000L, "Kallaloki vachi nanne allukundhi nee jolu")
        )
    )

    val sampleTracks = listOf(
        trackChuttamalle,
        trackFearSong,
        trackDaavudi,
        trackAyudhaPooja,
        trackPushpaPushpa,
        trackSooseki,
        trackPeelings,
        trackKissik,
        trackKurchi,
        trackDumMasala,
        trackAmmaSong,
        trackBhairavaAnthem,
        trackTaTakkara,
        trackThemeOfKalki,
        trackSamajavaragamana,
        trackButtaBomma,
        trackRamulooRamulaa,
        trackInthandham,
        trackOhSita,
        trackKaanunnaKalyanam,
        trackNaatuNaatu,
        trackDosti,
        trackKomuramBheemudo,
        trackSamayama,
        trackInkemInkem
    )

    val samplePlaylists = listOf(
        Playlist(
            id = "pl_devara",
            name = "Devara: Part 1",
            description = "Anirudh Ravichander • Chuttamalle, Fear Song, Daavudi & Ayudha Pooja",
            coverArtUrl = "https://c.saavncdn.com/313/Devara-Part-1-Telugu-Telugu-2024-20240926171010-500x500.jpg",
            ownerName = "Anirudh Ravichander",
            tracks = listOf(trackChuttamalle, trackFearSong, trackDaavudi, trackAyudhaPooja)
        ),
        Playlist(
            id = "pl_pushpa2",
            name = "Pushpa 2: The Rule",
            description = "Devi Sri Prasad • Pushpa Pushpa, Sooseki, Peelings & Kissik",
            coverArtUrl = "https://c.saavncdn.com/366/Pushpa-2-The-Rule-Telugu-Telugu-2024-20241205211012-500x500.jpg",
            ownerName = "Devi Sri Prasad",
            tracks = listOf(trackPushpaPushpa, trackSooseki, trackPeelings, trackKissik)
        ),
        Playlist(
            id = "pl_guntur_kaaram",
            name = "Guntur Kaaram",
            description = "Thaman S • Kurchi Madathapetti, Dum Masala & Amma Song",
            coverArtUrl = "https://c.saavncdn.com/000/Guntur-Kaaram-Telugu-2023-20240126145901-500x500.jpg",
            ownerName = "Thaman S",
            tracks = listOf(trackKurchi, trackDumMasala, trackAmmaSong)
        ),
        Playlist(
            id = "pl_kalki_2898",
            name = "Kalki 2898 AD",
            description = "Santhosh Narayanan • Bhairava Anthem, Ta Takkara & Theme of Kalki",
            coverArtUrl = "https://c.saavncdn.com/888/Kalki-2898-Ad-Telugu-Telugu-2024-20240712063717-500x500.jpg",
            ownerName = "Santhosh Narayanan",
            tracks = listOf(trackBhairavaAnthem, trackTaTakkara, trackThemeOfKalki)
        ),
        Playlist(
            id = "pl_ala_vaikunthapurramuloo",
            name = "Ala Vaikunthapurramuloo",
            description = "Thaman S • Samajavaragamana, Butta Bomma & Ramuloo Ramulaa",
            coverArtUrl = "https://c.saavncdn.com/517/Ala-Vaikunthapurramuloo-Telugu-2019-20200116144338-500x500.jpg",
            ownerName = "Thaman S",
            tracks = listOf(trackSamajavaragamana, trackButtaBomma, trackRamulooRamulaa)
        ),
        Playlist(
            id = "pl_sita_ramam",
            name = "Sita Ramam",
            description = "Vishal Chandrashekhar • Inthandham, Oh Sita Hey Rama & Kaanunna Kalyanam",
            coverArtUrl = "https://c.saavncdn.com/041/Sita-Ramam-Telugu-Original-Motion-Picture-Soundtrack-Telugu-2022-20220802140738-500x500.jpg",
            ownerName = "Vishal Chandrashekhar",
            tracks = listOf(trackInthandham, trackOhSita, trackKaanunnaKalyanam)
        ),
        Playlist(
            id = "pl_rrr",
            name = "RRR",
            description = "M.M. Keeravani • Naatu Naatu, Dosti & Komuram Bheemudo",
            coverArtUrl = "https://c.saavncdn.com/683/RRR-Telugu-Telugu-2022-20250828171313-500x500.jpg",
            ownerName = "M.M. Keeravani",
            tracks = listOf(trackNaatuNaatu, trackDosti, trackKomuramBheemudo)
        ),
        Playlist(
            id = "pl_hi_nanna",
            name = "Hi Nanna",
            description = "Hesham Abdul Wahab • Samayama & Soulful Melodies",
            coverArtUrl = "https://c.saavncdn.com/307/Samayama-From-Hi-Nanna-Telugu-2023-20230918164922-500x500.jpg",
            ownerName = "Hesham Abdul Wahab",
            tracks = listOf(trackSamayama, trackInthandham)
        ),
        Playlist(
            id = "pl_daily_mix_telugu",
            name = "Daily Telugu Mix",
            description = "A personalized blend of your favorite Tollywood hits & fresh melodies.",
            coverArtUrl = "https://c.saavncdn.com/313/Devara-Part-1-Telugu-Telugu-2024-20240926171010-500x500.jpg",
            ownerName = "ECHO For You",
            tracks = sampleTracks
        ),
        Playlist(
            id = "pl_liked_songs",
            name = "Liked Songs",
            description = "Your saved songs in one place.",
            coverArtUrl = "https://c.saavncdn.com/313/Devara-Part-1-Telugu-Telugu-2024-20240926171010-500x500.jpg",
            ownerName = "You",
            tracks = listOf(trackChuttamalle, trackPushpaPushpa, trackKurchi)
        )
    )

    val sampleArtists = listOf(
        Artist(
            id = "art_anirudh",
            name = "Anirudh Ravichander",
            avatarUrl = "https://c.saavncdn.com/artists/Anirudh_Ravichander_003_20260121134149_500x500.jpg",
            monthlyListeners = 28400000L,
            topTracks = listOf(trackChuttamalle, trackFearSong, trackDaavudi)
        ),
        Artist(
            id = "art_dsp",
            name = "Devi Sri Prasad",
            avatarUrl = "https://c.saavncdn.com/artists/Devi_Sri_Prasad_008_20250619062824_500x500.jpg",
            monthlyListeners = 14500000L,
            topTracks = listOf(trackPushpaPushpa, trackSooseki, trackPeelings)
        ),
        Artist(
            id = "art_thaman",
            name = "S.S. Thaman",
            avatarUrl = "https://c.saavncdn.com/artists/Thaman_S__007_20231106094011_500x500.jpg",
            monthlyListeners = 16800000L,
            topTracks = listOf(trackKurchi, trackDumMasala, trackSamajavaragamana)
        ),
        Artist(
            id = "art_sid_sriram",
            name = "Sid Sriram",
            avatarUrl = "https://c.saavncdn.com/artists/Sid_Sriram_005_20240425180600_500x500.jpg",
            monthlyListeners = 19200000L,
            topTracks = listOf(trackSamajavaragamana, trackInkemInkem)
        ),
        Artist(
            id = "art_keeravani",
            name = "M.M. Keeravani",
            avatarUrl = "https://c.saavncdn.com/artists/M__M__Keeravani_002_20240129101710_500x500.jpg",
            monthlyListeners = 11200000L,
            topTracks = listOf(trackNaatuNaatu, trackDosti, trackKomuramBheemudo)
        )
    )

    val samplePodcastEpisodes = listOf(
        Track(
            id = "ep_trs_1",
            title = "EP 390 - The Future of Indian Cinema, AI & Human Consciousness",
            artistName = "Ranveer Allahbadia",
            albumName = "The Ranveer Show",
            coverArtUrl = "https://images.unsplash.com/photo-1590602847861-f357a9332bbc?w=500&auto=format&fit=crop&q=80",
            durationMs = 284000L,
            mediaUri = "https://ia600908.us.archive.org/8/items/testmp3testfile/mpthreetest.mp3"
        ),
        Track(
            id = "ep_huberman_1",
            title = "Master Your Sleep, Dopamine & Cognitive Energy Protocols",
            artistName = "Dr. Andrew Huberman",
            albumName = "Huberman Lab",
            coverArtUrl = "https://images.unsplash.com/photo-1507679799987-c73779587ccf?w=500&auto=format&fit=crop&q=80",
            durationMs = 432000L,
            mediaUri = "https://ia600908.us.archive.org/8/items/testmp3testfile/mpthreetest.mp3"
        ),
        Track(
            id = "ep_tollywood_insider_1",
            title = "Devara & Pushpa 2: Behind-The-Scenes Music Scoring Breakdown",
            artistName = "Tollywood Audio Insider",
            albumName = "Tollywood Insider Podcast",
            coverArtUrl = "https://c.saavncdn.com/313/Devara-Part-1-Telugu-Telugu-2024-20240926171010-500x500.jpg",
            durationMs = 210000L,
            mediaUri = "https://ia600908.us.archive.org/8/items/testmp3testfile/mpthreetest.mp3"
        )
    )

    val samplePodcasts = listOf(
        Playlist(
            id = "podcast_trs",
            name = "The Ranveer Show",
            description = "India's smartest podcast covering cinema, tech, mindset, spirituality and life stories.",
            coverArtUrl = "https://images.unsplash.com/photo-1590602847861-f357a9332bbc?w=500&auto=format&fit=crop&q=80",
            ownerName = "Ranveer Allahbadia",
            isFollowed = true,
            tracks = listOf(samplePodcastEpisodes[0])
        ),
        Playlist(
            id = "podcast_huberman",
            name = "Huberman Lab",
            description = "Science and science-based tools for everyday health, neurology, and cognitive performance.",
            coverArtUrl = "https://images.unsplash.com/photo-1507679799987-c73779587ccf?w=500&auto=format&fit=crop&q=80",
            ownerName = "Dr. Andrew Huberman",
            isFollowed = false,
            tracks = listOf(samplePodcastEpisodes[1])
        ),
        Playlist(
            id = "podcast_tollywood_insider",
            name = "Tollywood Audio Insider",
            description = "Exclusive audio breakdown of Tollywood cinema music, background scores and sound engineering.",
            coverArtUrl = "https://c.saavncdn.com/313/Devara-Part-1-Telugu-Telugu-2024-20240926171010-500x500.jpg",
            ownerName = "Tollywood Audio Insider",
            isFollowed = true,
            tracks = listOf(samplePodcastEpisodes[2])
        )
    )
}
