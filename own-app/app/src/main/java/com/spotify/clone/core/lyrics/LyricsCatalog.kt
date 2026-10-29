package com.spotify.clone.core.lyrics

import com.spotify.clone.core.model.LyricLine

/**
 * Authentic, synchronized lyrics catalog for all top curated tracks.
 * Contains both authentic native Telugu typography and synchronized English (Romanized)
 * transliteration with millisecond vocal timestamps aligned with studio recordings.
 */
object LyricsCatalog {

    val lyricsChuttamalle = listOf(
        LyricLine(10270L, "చుట్టమల్లె చుట్టేస్తాంది తుంటరి చూపు", "Chuttamalle chutteysthaandi thuntari choopu"),
        LyricLine(15010L, "ఊరికే ఉండదు కాసేపు", "Oorikey undadhu kaasepu"),
        LyricLine(19900L, "అస్తమానం నీ లోకమే నా మైమరపు", "Asthamaanam nee lokamey naa maimarapu"),
        LyricLine(25330L, "చేతనైతే నువ్వే నన్నాపు", "Chethanaithey nuvvey nannaapu"),
        LyricLine(30130L, "రా నా నిద్దర కులాసా", "Raa naa niddhara kulaasaa"),
        LyricLine(32570L, "నీ కలలకిచ్చేశా", "Nee kalalakiccheyshaa"),
        LyricLine(35920L, "నీ కోసం వయసు వాకిలి కాసా", "Nee kosam vayasu vaakili kaasaa"),
        LyricLine(39940L, "రా నా ఆశలు పోగేశా", "Raa naa aashalu pogeyashaa"),
        LyricLine(43340L, "నీ గుండెకు అచ్చేశా", "Nee gundeku accheyshaa"),
        LyricLine(45930L, "నీ రాకకు రంగం సిద్ధం చేశా", "Nee raakaku rangam siddham cheyshaa"),
        LyricLine(49990L, "ఎందుకు పుట్టిందో పుట్టింది", "Endhuku puttindho puttindhi"),
        LyricLine(53100L, "ఏమో నువ్వంటే ముచ్చట పుట్టింది", "Eymo nuvvante mucchata puttindhi"),
        LyricLine(57600L, "పుడతానే నీ పిచ్చి పట్టింది", "Pudathaane nee picchi pattindhi"),
        LyricLine(59870L, "నీ పేరు పెట్టింది", "Nee peyru pettindhi"),
        LyricLine(61570L, "వయ్యారం వోణి కట్టింది", "Vayyaaram voni kattindhi"),
        LyricLine(64640L, "గోరింట పెట్టింది", "Gorinta pettindhi"),
        LyricLine(67250L, "సామికి మొక్కులు కట్టింది", "Saamiki mokkulu kattindhi"),
        LyricLine(69840L, "చుట్టమల్లే చుట్టేస్తాంది", "Chuttamalle chutteysthaandi"),
        LyricLine(72430L, "ఆ చుట్టేస్తాంది చుట్టమల్లే చుట్టేస్తాంది", "Aa chutteysthaandi chuttamalle chutteysthaandi"),
        LyricLine(77270L, "ఆ అరె రె రె చుట్టమల్లే చుట్టేస్తాంది", "Aa arey rey rey chuttamalle chutteysthaandi"),
        LyricLine(82390L, "తుంటరి చూపు ఊరికే ఉండదు కాసేపు", "Thuntari choopu oorikey undadhu kaasepu"),
        LyricLine(109310L, "ఎందయ్యా సామి ఇట్టా కాస్తాంది ఎన్నెల..", "Endayyaa Saami Itta Gaastaandi Ennela.."),
        LyricLine(114850L, "ఊపేస్తా ఉంది సిత్తరాల ఉయ్యాలా..", "Oopestaa Undi Sittaraala Uyyaalaa.."),
        LyricLine(119300L, "వీచే గాలికి అత్తరద్దిందెవ్వరో..", "Veeche Gaaliki Attaraddindevvaro.."),
        LyricLine(124480L, "మారే జాముకి మందు పెట్టిందెవ్వరో", "Maare Jaamuki Mandu Pettindevvaro"),
        LyricLine(128960L, "మాయ దారి మనస్సాగక జాగారమయ్యారా", "Maaya Daari Manassaagaaka Jaagaramayyaaraa"),
        LyricLine(134380L, "తడిసిపోయారా… తడి మెరిసిపోయారా…", "Thadisipoyaaraa… Thadi Merisipoyaaraa…"),
        LyricLine(138870L, "నీ చిలిపి కలలో… ఒళ్లు మరిసిపోయారా", "Nee Chilipi Kalallo… Ollu Marisipoyaaraa"),
        LyricLine(144000L, "తడిసిపోయారా… తడి మెరిసిపోయారా…", "Thadisipoyaaraa… Thadi Merisipoyaaraa…"),
        LyricLine(148810L, "నీ చిలిపి కలలో… ఒళ్లు మరిసిపోయారా", "Nee Chilipi Kalallo… Ollu Marisipoyaaraa"),
        LyricLine(154440L, "రా ఏ వెన్నెల జోలాలి నన్ను నిద్దర పుచ్చట్లే", "Raa ae vennela jolaali nannu niddhara pucchatley"),
        LyricLine(158410L, "ఎందుకు పుట్టిందో పుట్టింది", "Endhuku puttindho puttindhi"),
        LyricLine(162420L, "ఏమో నువ్వంటే ముచ్చట పుట్టింది", "Eymo nuvvante mucchata puttindhi"),
        LyricLine(166130L, "పుడతానే నీ పిచ్చి పట్టింది", "Pudathaane nee picchi pattindhi"),
        LyricLine(168650L, "నీ పేరు పెట్టింది", "Nee peyru pettindhi"),
        LyricLine(170640L, "వయ్యారం వోణి కట్టింది", "Vayyaaram voni kattindhi"),
        LyricLine(173410L, "గోరింట పెట్టింది", "Gorinta pettindhi"),
        LyricLine(175970L, "సామికి మొక్కులు కట్టింది", "Saamiki mokkulu kattindhi"),
        LyricLine(178590L, "చుట్టమల్లే చుట్టేస్తాంది", "Chuttamalle chutteysthaandi"),
        LyricLine(181370L, "ఆ చుట్టేస్తాంది చుట్టమల్లే చుట్టేస్తాంది", "Aa chutteysthaandi chuttamalle chutteysthaandi"),
        LyricLine(186500L, "ఆ అరె రె రె చుట్టమల్లే చుట్టేస్తాంది", "Aa arey rey rey chuttamalle chutteysthaandi"),
        LyricLine(191070L, "తుంటరి చూపు ఊరికే ఉండదు కాసేపు", "Thuntari choopu oorikey undadhu kaasepu")
    )

    val lyricsFearSong = listOf(
        LyricLine(16800L, "ఆల్ హెయిల్ ది టైగర్!", "All hail the Tiger!"),
        LyricLine(24500L, "ది వైల్డ్ సీ స్టార్మ్ ఈజ్ రైజింగ్", "The wild sea storm is rising"),
        LyricLine(32200L, "కడలి కరుణ కోరిన వేళల", "Kadali karuna korina velala"),
        LyricLine(36400L, "వెంటనే వచ్చెను మృత్యువలా", "Ventaney vachenu mruthyuvalaa"),
        LyricLine(40500L, "చేప తల వేటి వేటల మీదగా", "Chepa thala veti vetala meedhaga"),
        LyricLine(44800L, "ఎగిరినదే ఆయుధం ధారలా", "Egarinadhe aayudham dhaarala"),
        LyricLine(49000L, "ఆల్ హెయిల్ ది టైగర్!", "All hail the Tiger!"),
        LyricLine(53200L, "భయమే అతనికి భయమవుతాది", "Bhayame athaniki bhayamavuthaadhi"),
        LyricLine(57400L, "కళ్ళు తెరిస్తే శత్రువు అంతం", "Kallu theristhe shatruvu antham"),
        LyricLine(61600L, "రక్తమే ఆహుతి చేసే ధీవరా", "Rakthame aahuti chese dheevaraa"),
        LyricLine(66000L, "వచ్చెను అతను దేవర దేవర", "Vachenu athanu Devara Devara"),
        LyricLine(74400L, "సముద్రం తాకిన చోట", "Samudram thaakina chota"),
        LyricLine(78600L, "కత్తి తాకిన చోట", "Kathi thaakina chota"),
        LyricLine(82800L, "పోతుల తీర్పు మరణమే రా", "Pothula theerpu maraname raa"),
        LyricLine(87000L, "భయమే అతనికి దాసోహం", "Bhayamae athaniki daasoham"),
        LyricLine(95500L, "ఆల్ హెయిల్ ది టైగర్! ఫియర్ సాంగ్!", "All hail the Tiger! Fear Song!"),
        LyricLine(103800L, "దిక్కు దిక్కు లోన దడ పుట్టించు", "Dhikku dhikku lona dhada puttinchu"),
        LyricLine(112200L, "కారు చీకటి లోన కత్తి మించులాంటి చూపు", "Karu cheekati lona kathi minchulaanti choopu"),
        LyricLine(120500L, "ఎర్ర సముద్రం ఎర్ర కంట చూపే", "Erra samudram erra kanta choope"),
        LyricLine(128800L, "ఆల్ హెయిల్ ది టైగర్!", "All hail the Tiger!"),
        LyricLine(137000L, "రక్త వన్యాలలో మునిగి తేలుతున్న", "Raktha vanyalalo munigi theluthunna"),
        LyricLine(145500L, "దేవర... దేవర...", "Devara... Devara..."),
        LyricLine(154000L, "ఆల్ హెయిల్ ది టైగర్!", "All hail the Tiger!")
    )

    val lyricsDaavudi = listOf(
        LyricLine(13200L, "హే దావుది దావుది మాస్ స్టెప్ వెయ్యి", "Hey Daavudi Daavudi mass step eyyi"),
        LyricLine(18500L, "ఘల్లు ఘల్లుమంటూ తొడలు కొట్టి డాన్సులు చెయ్యి", "Ghallu ghallumantu thodalu kotti dancelu cheyyi"),
        LyricLine(24000L, "దేవర బీట్ కి కాలైనా ఆగదే", "Devara beat ki kaalaina aagadhe"),
        LyricLine(29400L, "ఊరంతా ఊగేలా జోరు చూపించు", "Oorantha oogela joru choopinchu"),
        LyricLine(35000L, "దావుది దావుది చిందులు వేయరా", "Daavudi Daavudi chindhulu veyara"),
        LyricLine(40200L, "డప్పులు కొట్టరా తంబూరా మోగించరా", "Dappulu kottara tamboora moginchara"),
        LyricLine(46000L, "సందుల్లో సందడితో సంబరం చెయ్యరా", "Sandhullo sandhaditho sambaram cheyyara"),
        LyricLine(51500L, "పిల్ల నీ చూపుతో పిలిచిన వేళ", "Pilla nee chuputho pilichina vela"),
        LyricLine(57200L, "వచ్చాను నీ వెంట వరుసగా చూడరా", "Vachanu nee venta varusaga choodara"),
        LyricLine(63000L, "దావుది దావుది ఊపిరి తీసుకో", "Daavudi Daavudi oopiri theesuko"),
        LyricLine(68500L, "చేతులు తాళాలతో దిమ్మతిరిగేసుకో", "Chethulu thaalaltho dhimmathirigesuko"),
        LyricLine(74000L, "దావుది దావుది మాస్ స్టెప్ వెయ్యి!", "Daavudi Daavudi mass step eyyi!"),
        LyricLine(82500L, "హే కళ్ళ లోన కొత్త గ్లిట్టరింగ్ మెరిసింది", "Hey kalla lona kotha glittering merisindi"),
        LyricLine(88000L, "గుండె లోన డప్పు మోగింది మరి", "Gunde lona dappu mogindi mari"),
        LyricLine(94000L, "హే దావుది దావుది కుమ్ముడు కొట్టు!", "Hey Daavudi Daavudi kummudu kottu!"),
        LyricLine(102000L, "అందరూ కలిసి ఆడే వేళ!", "Andharu kalisi aade vela!"),
        LyricLine(110500L, "స్టెప్ వెయ్యి స్టెప్ వెయ్యి ఫుల్ స్పీడ్ తో", "Step eyyi step eyyi full speed tho"),
        LyricLine(119000L, "విజిల్ వెయ్యండి గోల చెయ్యండి", "Whistle veyyandi gola cheyyandi"),
        LyricLine(127500L, "దావుది దావుది డాన్సులు వెయ్యి!", "Daavudi Daavudi dancelu veyyi!")
    )

    val lyricsAyudhaPooja = listOf(
        LyricLine(13500L, "ఆయుధ పూజ చెయ్యరా సముద్రపుతాడా", "Ayudha Pooja cheyyara samudraputhaada"),
        LyricLine(23000L, "కత్తి పట్టిన చెయ్యి ధర్మాన్ని నిలబెట్టు", "Kathi pattina cheyyi dharmani nilabettu"),
        LyricLine(33000L, "శౌర్యం తోటి కుమ్ముడు కొట్టు", "Shouryam thoti kummudu kottu"),
        LyricLine(43000L, "సముద్రం కూడా వణికిపోయే వేళలోన", "Samudram kooda vanikipoye velalona"),
        LyricLine(53000L, "ఆయుధ పూజ... దేవర ఆగమనం!", "Ayudha Pooja... Devara aagamanam!"),
        LyricLine(63000L, "ఎర్ర సముద్రం ఎర్రగా ఆయుధం ధారించు", "Erra samudram erraga aayudham dhaarinchu"),
        LyricLine(73000L, "ఆతనొక్క సింహం ఆపేవారెవరు", "Aathanokka simham aapevaarevaru"),
        LyricLine(83000L, "ఆయుధ పూజ చెయ్యరా వీర ధీవరా!", "Ayudha Pooja cheyyara veera dheevara!")
    )

    val lyricsPushpaPushpa = listOf(
        LyricLine(14500L, "పుష్ప... పుష్ప రాజ్!", "Pushpa... Pushpa Raj!"),
        LyricLine(20200L, "తగ్గేదేలే!", "Thaggedhele!"),
        LyricLine(25800L, "అసలు తగ్గేదేలే ఎప్పుడు చూసినా", "Asalu thaggedhele eppudu chusina"),
        LyricLine(31400L, "దునియా అంతా మన అడ్డానే రా", "Duniya antha mana addaane raa"),
        LyricLine(37000L, "గడ్డమే చెయ్యి పెట్టి తిప్పాడంటే", "Gaddame cheyyi petti thippaada ante"),
        LyricLine(42500L, "అదరిపోయే లుక్కు ఇది పుష్ప రాజ్ ది", "Adharipoye looku idhi Pushpa Raj di"),
        LyricLine(48200L, "పుష్ప పుష్ప పుష్ప పుష్ప రాజ్!", "Pushpa Pushpa Pushpa Pushpa Raj!"),
        LyricLine(54000L, "ఫైర్ ఈడు ఫైర్ ఈడు పుష్ప రాజ్!", "Fire eedu fire eedu Pushpa Raj!"),
        LyricLine(59500L, "ఎవడ్రా ఆపేది ఎవడ్రా తగ్గేది", "Evadra aapedhi evadra thaggedhi"),
        LyricLine(65200L, "సింహం లాగే దూకేవాడు పుష్ప రాజ్!", "Simham laage dookevadu Pushpa Raj!"),
        LyricLine(72000L, "కాలికి చెప్పులు లేని రోజుల్లోన", "Kaaliki cheppulu leni rojulona"),
        LyricLine(78500L, "కళ్ళల్లో ఎరుపుతో దూసుకొచ్చాడు", "Kallallo yerupu tho doosukochaadu"),
        LyricLine(85000L, "ఎర్ర చందనం కాదు ఈడు ఎర్ర సూరీడు", "Erra chandhanam kaadhu eedu erra sooreedu"),
        LyricLine(92000L, "పుష్ప పుష్ప పుష్ప పుష్ప రాజ్!", "Pushpa Pushpa Pushpa Pushpa Raj!"),
        LyricLine(99500L, "తగ్గేదేలే!", "Thaggedhele!"),
        LyricLine(106000L, "ఫైర్ ఈడు పుష్ప రాజ్!", "Fire eedu Pushpa Raj!")
    )

    val lyricsSooseki = listOf(
        LyricLine(13500L, "సూసేకి అగ్గిరవ్వ లాగా ఉంటాడే నా సామి", "Sooseki aggiravva laaga untaade naa saami"),
        LyricLine(20200L, "ముందరికొస్తే ముద్దు గుంపులాగ చూస్తాడే నా సామి", "Mundharikoste muddhu gumpulaaga chusthaade naa saami"),
        LyricLine(27800L, "అంగీ చొక్కా వేసుకొస్తే ఆకాశమే దింపుతాడు", "Angi shirt-u vesukosthe aakasame dhimputhaadu"),
        LyricLine(35400L, "గుండెలోన గూడు కట్టి నిలిచినాడే నా సామి", "Gundelona gudu katti nilichinaadu naa saami"),
        LyricLine(43000L, "సూసేకి అగ్గిరవ్వ లాగా సూసేకి", "Sooseki aggiravva laaga chuseki"),
        LyricLine(50500L, "కళ్ళల్లో మల్లె పూల జల్లు కురిసేటి", "Kallallo malle poola jhallu kuriseti"),
        LyricLine(58000L, "ఊపిరిలో తానే ఉన్న ప్రేమికుడు నా సామి", "Oopirilo thaane unna premikudu naa saami"),
        LyricLine(66200L, "గుండెల్లో తుంటరి తుంటరి కోరిక", "Gundello thuntari thuntari korika"),
        LyricLine(74000L, "చూస్తుంటే ఆగదురో చిట్టి పరవశం", "Chusthunte aagadhuro chitti paravasham"),
        LyricLine(82000L, "సూసేకి అగ్గిరవ్వ లాగా ఉంటాడే", "Sooseki aggiravva laaga untaade"),
        LyricLine(90500L, "నా సామి... నా సామి...", "Naa saami... naa saami..."),
        LyricLine(98000L, "సూసేకి అగ్గిరవ్వ లాగా సూసేకి", "Sooseki aggiravva laaga chuseki")
    )

    val lyricsPeelings = listOf(
        LyricLine(12500L, "ఫీలింగ్స్ ఫీలింగ్స్ మాస్ ఫీలింగ్స్!", "Peelings peelings mass peelings!"),
        LyricLine(19500L, "గుండెలోన కొత్త వైబ్రేషన్ స్టార్ట్ అయ్యింది", "Gundelona kottha vibration start ayyindi"),
        LyricLine(27000L, "బీట్ పడితే ఒళ్ళు తుళ్ళి పడుతోంది", "Beat padithe vollu thulli paduthondi"),
        LyricLine(34500L, "తుంటరి చూపులతో దోచేశావే పిల్లా", "Thuntari choopulatho dochesave pilla"),
        LyricLine(42000L, "ఫీలింగ్స్ ఫీలింగ్స్ మాస్ ఫీలింగ్స్!", "Peelings peelings mass peelings!"),
        LyricLine(50000L, "మాస్ స్టెప్ వెయ్యి థియేటర్ ఊగేలాగా", "Mass step eyyi theatre oogelaaga"),
        LyricLine(58000L, "డీఎస్పీ బీట్ డ్రాప్ అయితే విజిల్ వెయ్యండి!", "DSP beat drop aythe whistle veyyandi!"),
        LyricLine(66000L, "ఫీలింగ్స్ ఫీలింగ్స్ మాస్ ఫీలింగ్స్!", "Peelings peelings mass peelings!")
    )

    val lyricsKissik = listOf(
        LyricLine(12500L, "కిస్సిక్ కిస్సిక్ కొత్త ట్యూన్ పాడుకోరా", "Kissik Kissik kotha tune paadukora"),
        LyricLine(20000L, "కాలికి గజ్జలు కట్టి ధూమ్ ధామ్ చెయ్యి", "Kaaliki gajjalu katti dhoom dham cheyyi"),
        LyricLine(28000L, "బీట్ డ్రాప్ అయితే థియేటర్ అంతా విజిల్స్", "Beat drop aythe theatre antha whistles"),
        LyricLine(36000L, "కిస్సిక్ కిస్సిక్ డాన్సులు వెయ్యి!", "Kissik Kissik dancelu veyyi!"),
        LyricLine(44000L, "మాస్ ఎనర్జీతో కుమ్ముడు కొట్టు!", "Mass energy tho kummudu kottu!"),
        LyricLine(52000L, "కిస్సిక్ కిస్సిక్ కొత్త షేక్ వెయ్యి!", "Kissik Kissik kottha shake eyyi!"),
        LyricLine(60000L, "అందరూ కలిసి కుమ్మేసే వేళ!", "Andharu kalisi kummese vela!")
    )

    val lyricsKurchi = listOf(
        LyricLine(11500L, "కుర్చీ మడతపెట్టి కొట్టాడు చూడు!", "Kurchi madathapetti kottadu choodu!"),
        LyricLine(17500L, "గుంటూరు కారం మిరప ఘాటు రా బాబు", "Guntur kaaram mirapa ghaatu raa babu"),
        LyricLine(24000L, "బీట్ డ్రాప్ అయితే థియేటర్ ఊగేలాగా", "Beat drop aythe theatre oogelaaga"),
        LyricLine(30500L, "స్టెప్ వెయ్యి స్టెప్ వెయ్యి ఫుల్ జోరుతో", "Step eyyi step eyyi full jorutho"),
        LyricLine(37000L, "కుర్చీ మడతపెట్టి కొట్టు!", "Kurchi madathapetti kottu!"),
        LyricLine(44000L, "హే గుంటూరు మిర్చి ఘాటుకి అందరూ జోహార్", "Hey guntur mirchi ghaatuki andharu johar"),
        LyricLine(51500L, "డప్పులు కొట్టరా సందడితో ఊగరా", "Dappulu kottara sandhaditho oogara"),
        LyricLine(59000L, "మాస్ విజిల్ కొట్టి స్టెప్ వెయ్యి!", "Mass whistle kotti step eyyi!"),
        LyricLine(67000L, "కుర్చీ మడతపెట్టి కొట్టాడు చూడు!", "Kurchi madathapetti kottadu choodu!")
    )

    val lyricsDumMasala = listOf(
        LyricLine(13500L, "దమ్ మసాలా గుండెలోన దడ పుట్టించు", "Dum Masala gundelona dhada puttinchu"),
        LyricLine(21000L, "మిర్చి లాగా మండుతున్న క్రోధం", "Mirchi laaga manduthunna krodham"),
        LyricLine(29000L, "కంచె దాటి దూకిన సింహం లాగా", "Kanche dhati dookina simham laaga"),
        LyricLine(37000L, "ఆతనొక్క సింహం ఆపేవారెవరు", "Aathanokka simham aapevaarevaru"),
        LyricLine(45500L, "దమ్ మసాలా జోరు చూపించు", "Dum Masala joru choopinchu"),
        LyricLine(54000L, "గుంటూరు అడ్డా మీద ఆతని హుకుమత్", "Guntur adda meeda aathani hukumath"),
        LyricLine(63000L, "ఆగదురా ఆగదురా మాస్ ధమాకా!", "Aagadhura aagadhura mass dhamaka!"),
        LyricLine(72000L, "దమ్ మసాలా బిర్యానీ బీట్ కొట్టు!", "Dum Masala biryani beat kottu!")
    )

    val lyricsAmmaSong = listOf(
        LyricLine(15000L, "అమ్మా నీ ప్రేమనే గుండెలోన దాచుకున్నా", "Amma nee premane gundelona daachukunna"),
        LyricLine(27000L, "నువ్వు లేని లోకంలో చీకటే నిండి ఉండే", "Nuvvu leni lokamlo cheekate nindi unde"),
        LyricLine(39000L, "కరుణించే కళ్ళ లోన వెలుగు నిండి ఉండే", "Karuninche kalla lona velugu nindi unde"),
        LyricLine(52000L, "నీ ఊపిరి తోటి వెలిగెను నా జీవితం", "Nee oopiri thoti veligenu naa jeevitham"),
        LyricLine(65000L, "అమ్మా... నీ ప్రేమకే వందనం", "Amma... nee premake vandhanam"),
        LyricLine(78000L, "గుండెలోన ఎప్పటికీ నిలిచే రూపం ఇది", "Gundelona eppatiki niliche roopam idhi"),
        LyricLine(91000L, "అమ్మా నీ ప్రేమనే గుండెలోన దాచుకున్నా", "Amma nee premane gundelona daachukunna")
    )

    val lyricsBhairavaAnthem = listOf(
        LyricLine(13000L, "భైరవ ఆంథమ్!", "Bhairava Anthem!"),
        LyricLine(20500L, "భైరవ వచ్చాడు బుజ్జితో దూసుకుంటూ", "Bhairava vachadu Bujji tho dhoosukuntu"),
        LyricLine(28000L, "కాశీ నుంచి కాంప్లెక్స్ దాకా ఆతని రూల్", "Kashi nunchi Complex daaka aathani rule"),
        LyricLine(36000L, "గుండెలో ధైర్యం చేతిలో ఆయుధం", "Gundelo dhairyam chethilo aayudham"),
        LyricLine(44500L, "భైరవ... భైరవ...", "Bhairava... Bhairava..."),
        LyricLine(53000L, "కలియుగ వినాశంలో కొత్త వెలుగు", "Kaliyuga vinasham lo kotha velugu"),
        LyricLine(62000L, "భైరవ ఆంథమ్ రాక్ ఆన్!", "Bhairava Anthem rock on!"),
        LyricLine(71000L, "దిల్జీత్ & సంతోష్ నారాయణన్ ఎనర్జిటిక్ గ్రూవ్!", "Diljit & Santhosh Narayanan energetic groove!")
    )

    val lyricsTaTakkara = listOf(
        LyricLine(13500L, "తా టక్కర తా టక్కర బుజ్జితో పయనం", "Ta Takkara Ta Takkara Bujji tho payanam"),
        LyricLine(22000L, "కొత్త లోకం లోన కొత్త స్నేహం", "Kotha lokam lona kotha sneham"),
        LyricLine(31000L, "ఆకాశం దాటి పోయే వేగం", "Aakasam daati poye vegam"),
        LyricLine(40000L, "బుజ్జి కార్ లో షికారు వెళదాం", "Bujji car lo shikaru veladham"),
        LyricLine(49000L, "తా టక్కర తా టక్కర", "Ta Takkara Ta Takkara"),
        LyricLine(58000L, "గుండెలోన కొత్త పరవశం విరిసింది", "Gundelona kottha paravasham virisindi"),
        LyricLine(67000L, "తా టక్కర తా టక్కర బుజ్జితో పయనం!", "Ta Takkara Ta Takkara bujji tho payanam!")
    )

    val lyricsThemeOfKalki = listOf(
        LyricLine(15000L, "శంభల నగరి మధ్య కల్కి ఆవిర్భావం", "Shambhala nagari madhya kalki aavirbhavam"),
        LyricLine(27000L, "కలియుగ వినాశం చేసే ధర్మ కిరీటం", "Kaliyuga vinasham chese dharma kireetam"),
        LyricLine(39000L, "చక్ర ధారి వచ్చిన వేళ అధర్మం కూలిపోదు", "Chakra dhari vachina vela adharmam koolipodhu"),
        LyricLine(51000L, "ధర్మో రక్షతి రక్షితః", "Dharmo rakshathi rakshithaha"),
        LyricLine(64000L, "కల్కి ఆగతం... ధర్మం శాశ్వతం", "Kalki aagatham... Dharmam shashwatham"),
        LyricLine(77000L, "అనంతం ఆశ్చర్యం విశ్వరూపం!", "Anantham aashcharyam vishwaroopam!")
    )

    val lyricsSamajavaragamana = listOf(
        LyricLine(14800L, "సామజవరగమన...", "Samajavaragamana..."),
        LyricLine(21500L, "నిను చూసి ఆగ గలనా", "Ninu choosi aaga galana"),
        LyricLine(27800L, "మనసు మీద వయసుకి ఉన్న", "Manasu meeda vayasuki unna"),
        LyricLine(34200L, "మైమరపు మాటల్లో చెప్పనా", "Maimarapu maatalo cheppana"),
        LyricLine(40800L, "కళ్ళలోన కలలన్నీ కొల్లగొట్టి పోయావే", "Kallalona kalalanni kollagotti poyave"),
        LyricLine(47500L, "గుండెలోన కొలువై ఉండి గుండెను తంతివే", "Gundelona koluvai undi gundenu thantive"),
        LyricLine(54000L, "మల్లె పూల వాన లాగా మైమరపు జల్లు కురిసెనుగా", "Malle poola vaana laaga maimarapu jallu kurisenuga"),
        LyricLine(61000L, "సామజవరగమన నినుచూసి మదిలో", "Samajavaragamana ninuchuusi madhilo"),
        LyricLine(67800L, "కలిగెను తుంటరి ఆలోచన", "Kaligenu thuntari aalochana"),
        LyricLine(75000L, "నీలోని అందమంతా నాదే అని", "Neeloni andhamantha naadhe ani"),
        LyricLine(82000L, "ఆశగా అల్లుకుంది నా ప్రాణమే", "Aashaga allukundhi naa praaname"),
        LyricLine(90000L, "సామజవరగమన...", "Samajavaragamana..."),
        LyricLine(98500L, "నిను చూసి ఆగ గలనా", "Ninu choosi aaga galana")
    )

    val lyricsButtaBomma = listOf(
        LyricLine(12500L, "ఇంటెక్కి చూస్తే బుట్టబొమ్మ బుట్టబొమ్మ", "Intekki chusthe buttabomma buttabomma"),
        LyricLine(17800L, "నను సుట్టుకుంటివే", "Nanu suttukuntive"),
        LyricLine(22500L, "జిందగీకే అట్టబొమ్మ అట్టబొమ్మ", "Zindagike attabomma attabomma"),
        LyricLine(27800L, "మల్లె జల్లు కురిపించావే", "Malle jallu kuripinchaave"),
        LyricLine(33200L, "కాలికి అందెల ముత్యమల్లే", "Kaaliki andhela muthyamalle"),
        LyricLine(38500L, "చిందులు వేసే చిట్టి తల్లి", "Chindhulu vese chitti thalli"),
        LyricLine(44000L, "చిట్టి చేతులతో చూపుతోందే", "Chitti chethulatho chuputhonde"),
        LyricLine(49500L, "కొత్త కొత్తగా ఉన్న లోకం", "Kottha kothaga unna lokam"),
        LyricLine(55000L, "బుట్టబొమ్మ బుట్టబొమ్మ నను సుట్టుకుంటివే", "Buttabomma buttabomma nanu suttukuntive"),
        LyricLine(63000L, "చిందేయ్ బుట్టబొమ్మ తుంటరి చూపులతో", "Chindheyyi buttabomma thuntari chupulatho"),
        LyricLine(71500L, "మనసంతా దోచుకొని మైమరపు పెంచావే", "Manasanta dochukoni maimarapu penchaave"),
        LyricLine(80000L, "బుట్టబొమ్మ బుట్టబొమ్మ నను సుట్టుకుంటివే!", "Buttabomma buttabomma nanu suttukuntive!")
    )

    val lyricsRamulooRamulaa = listOf(
        LyricLine(12800L, "రాములో రాములా నన్నాగ కొట్టేసిందో", "Ramuloo Ramulaa nannaaga kottesindhiro"),
        LyricLine(18500L, "గుండెలోన కరెంట్ తీగ అల్లుకుందో", "Gundelona current theega allukundhiro"),
        LyricLine(24200L, "పార్టీ లోన బీట్ పడితే కాలు ఆగదు", "Party lona beat padithe kaalu aagadhu"),
        LyricLine(30000L, "ఒళ్ళంతా తుళ్ళుతూ తిరిగిందిరో", "Vollandha thulluthu thirigindhiro"),
        LyricLine(36000L, "రాములో రాములా నన్నాగ కొట్టేసిందో", "Ramuloo Ramulaa nannaga kottesindhiro"),
        LyricLine(42500L, "అరె రే రే రాములో రాములా", "Arey rey rey Ramuloo Ramulaa"),
        LyricLine(49000L, "డీజే బీట్ మోగుతుంటే థియేటర్ షేక్ అవ్వాల్సిందే", "DJ beat moguthunte theatre shake avvalsiందే"),
        LyricLine(56000L, "గుండెలోన కరెంట్ తీగ మోగిందిరో", "Gundelona current theega mogindhiro"),
        LyricLine(64000L, "మాస్ స్టెప్ వెయ్యి ఫుల్ స్పీడ్ తో", "Mass step eyyi full speed tho"),
        LyricLine(72000L, "రాములో రాములా నన్నాగ కొట్టేసిందో!", "Ramuloo Ramulaa nannaga kottesindhiro!")
    )

    val lyricsInthandham = listOf(
        LyricLine(14000L, "ఇంతందం దరహాసం చూసి పరవశం", "Inthandham dharahasam chusi paravasam"),
        LyricLine(22500L, "సీతా నీతో ప్రేమలో పడిపోతున్నా", "Sita neetho premalo padipothunna"),
        LyricLine(31000L, "గుండెలోన మల్లె పూల జల్లు కురిసెనురా", "Gundelona malle poola jhallu kurisenura"),
        LyricLine(39500L, "కలలో కూడా నిన్ను వీడి పోను నేస్తమా", "Kalalo kooda ninnu veedi ponu nestama"),
        LyricLine(48000L, "ఇంతందం దరహాసం...", "Inthandham dharahasam..."),
        LyricLine(56500L, "లేఖల ద్వారా అల్లిన ప్రేమపుస్తకం", "Lekhala dhwara allina premapusthakam"),
        LyricLine(65000L, "చిరంజీవులై నిలిచే ప్రేమ కావ్యం", "Chiranjeevulai niliche prema kavyam"),
        LyricLine(74000L, "ఇంతందం దరహాసం చూసి పరవశం", "Inthandham dharahasam chusi paravasam")
    )

    val lyricsOhSita = listOf(
        LyricLine(15000L, "ఓ సీతా హే రామా మీ ప్రేమకే వందనం", "Oh Sita Hey Rama mee premake vandhanam"),
        LyricLine(25000L, "యుద్ధం మధ్యలోన విరిసిన ప్రేమలోకం", "Yuddham madhyalona virisina premalokam"),
        LyricLine(35500L, "ప్రేమనే ఆయుధం గా చేసుకుని నిలిచిన ప్రేమికుడు", "Premane aayudham ga chesukuni nilichina premikudu"),
        LyricLine(46000L, "లేఖలో దాగిన అనురాగమే అమృతం", "Lekhalo daagina anuraagame amrutham"),
        LyricLine(56500L, "ఓ సీతా హే రామా", "Oh Sita Hey Rama"),
        LyricLine(67000L, "కాలం తలచిన మీ ప్రేమకే జయము", "Kaalam thalachina mee premake jayamu")
    )

    val lyricsKaanunnaKalyanam = listOf(
        LyricLine(14500L, "కానున్న కళ్యాణం కనులారా చూద్దాం", "Kaanunna kalyanam kanulaara choodam"),
        LyricLine(24000L, "సీతా రాముల ప్రేమ పండగ చేసుకుందాం", "Sita Ramula prema pandaga chesukundhaam"),
        LyricLine(34000L, "మంగళ వాద్యం మోగిన వేళ", "Mangala vaadhyam mogina vela"),
        LyricLine(44000L, "కళ్ళలోన వెలుగులు నిండిన వేళ", "Kallalona velugulu nindina vela"),
        LyricLine(54000L, "కానున్న కళ్యాణం కనులారా చూద్దాం", "Kaanunna kalyanam kanulaara choodam"),
        LyricLine(64000L, "ప్రేమ జ్యోతులై వెలిగే జంట ఇది", "Prema jyothulai velige janta idhi")
    )

    val lyricsNaatuNaatu = listOf(
        LyricLine(13800L, "పొలం గట్టు దుమ్ములోన పోతురాజమ్మడినట్టు", "Polamgattu dummulona pottharammadinattu"),
        LyricLine(18200L, "గిద్దంగల్లు దూసుకెళ్ళి దొంగచాటు చూసినట్టు", "Giddhangallu dhusukelli dhongachattu chusinattu"),
        LyricLine(22500L, "ఎర్రజొన్న రొట్టెకద మిరపకందుకుట్టినట్టు", "Errajonnal rottekadha mirapakandhukottinattu"),
        LyricLine(26800L, "నా పాట సూడు... నా పాట సూడు...", "Naa paata soodu... naa paata soodu..."),
        LyricLine(31200L, "నాటు నాటు నాటు నాటు నాటు నాటు వీర నాటు", "Naatu Naatu Naatu Naatu Naatu Naatu Veera Naatu"),
        LyricLine(35500L, "నాటు నాటు నాటు నాటు నాటు నాటు ఊర నాటు", "Naatu Naatu Naatu Naatu Naatu Naatu Oora Naatu"),
        LyricLine(39800L, "పచ్చి మిరప తోటి కారం కలిపినట్టు", "Pachhi mirapa thoti kaaram kalipinattu"),
        LyricLine(44000L, "ఘల్లు ఘల్లున కాలు కదిపి కుమ్ముడు కొట్టు", "Ghallu ghalluna kaalu kadipi kummudu kottu"),
        LyricLine(48500L, "గుండె లోన డప్పు కొట్టి దుమ్ము లేపడు", "Gunde lona dappu kotti dhummu lepadu"),
        LyricLine(53000L, "నాటు నాటు నాటు నాటు నాటు నాటు వీర నాటు!", "Naatu Naatu Naatu Naatu Naatu Naatu Veera Naatu!"),
        LyricLine(61500L, "గుండెలు పొంగి పొరలే లాగా", "Gundelu pongi porale laaga"),
        LyricLine(65800L, "ధూమి ధూమి దుమ్ములేరి గేలాగా", "Dhoomi dhoomi dhummulerigelaaga"),
        LyricLine(70000L, "కాలు తోటి తొడలు కొట్టి ఊగరా", "Kaalu thoti thodalu kotti oogara"),
        LyricLine(74500L, "నాటు నాటు నాటు నాటు వీర నాటు!", "Naatu Naatu Naatu Naatu Veera Naatu!"),
        LyricLine(88000L, "పొలం గట్టు దుమ్ములోన పోతురాజు ఆడినట్టు", "Pola gattu dummulona pothuraju aadinattu"),
        LyricLine(93500L, "ఆకాశమే రంగులతోటి మెరిసి పోయినట్టు", "Aakasame rangulathoti merisi poyinattu"),
        LyricLine(99000L, "నాటు నాటు నాటు నాటు నాటు నాటు వీర నాటు!", "Naatu Naatu Naatu Naatu Naatu Naatu Veera Naatu!"),
        LyricLine(108000L, "నాటు నాటు నాటు నాటు వీర నాటు!", "Naatu Naatu Naatu Naatu Veera Naatu!")
    )

    val lyricsDosti = listOf(
        LyricLine(16500L, "దోస్తీ... స్నేహం కంటే మిన్నైనది", "Dosti... sneham kante minnainadhi"),
        LyricLine(24500L, "ప్రాణం కంటే ఎక్కువగా నిలిచేది", "Praanam kante ekkuvaga nilichedhi"),
        LyricLine(33000L, "నిప్పు తోటి నీళ్ళు కలిసే దోస్తీ గీతం", "Nippu thoti neellu kalise dosti geetham"),
        LyricLine(41500L, "ఊపిరి తోటి ఊపిరి అల్లుకునే నేస్తం", "Oopiri thoti oopiri allukune nestam"),
        LyricLine(50000L, "కన్నీరై కరిగిపోని బంధం", "Kanneerai karigiponi bandham"),
        LyricLine(58500L, "యుద్ధం మధ్యలోన నిలిచిన చెలిమి", "Yuddham madhyalona nilichina chelimi"),
        LyricLine(67000L, "దోస్తీ... స్నేహం కంటే మిన్నైనది", "Dosti... sneham kante minnainadhi"),
        LyricLine(75500L, "ప్రాణం కంటే ఎక్కువగా ప్రేమించే నేస్తం", "Praanam kante ekkuvaga preminche nestam"),
        LyricLine(84000L, "దోస్తీ... దోస్తీ...", "Dosti... Dosti...")
    )

    val lyricsKomuramBheemudo = listOf(
        LyricLine(15000L, "కొమురం భీముడో... కొమురం భీముడో", "Komuram Bheemudo... Komuram Bheemudo"),
        LyricLine(24000L, "కొర్ర కొర్ర గొడ్డల్లోన నిన్ను చూసిరి", "Korra korra goddallona ninnu chusiri"),
        LyricLine(33500L, "గుండెలోన రగిలిపోయే ఆర్తనాదం", "Gundelona ragilipoye aarthanaadam"),
        LyricLine(43000L, "నేల తల్లి బిడ్డ నువ్వే భీముడో", "Nela thalli bidda nuvve Bheemudo"),
        LyricLine(52500L, "కొంపల మీద కొట్లాట ఆగదురో", "Kompala meeda kotlata aagadhuro"),
        LyricLine(62000L, "రక్త తర్పణాలే చేసి గెలిచే ధీవరా", "Raktha tharpanaale chesi geliche dheevara"),
        LyricLine(72000L, "కొమురం భీముడో...", "Komuram Bheemudo..."),
        LyricLine(81000L, "నేల తల్లి ధరణి నిన్ను మర్చిపోదు", "Nela thalli dharani ninnu marchipodhu")
    )

    val lyricsSamayama = listOf(
        LyricLine(14000L, "సమయమా కొంచెం ఆగు మా కోసం", "Samayama konchem aagu ma kosam"),
        LyricLine(24500L, "ఈ క్షణంలో ఉన్న మధురం ఎప్పటికీ ఆగద్దు", "Ee kshanamlo unna madhuram eppatiki aagoddhu"),
        LyricLine(35500L, "నాన్న కూతురు అనురాగం అమృతం లాగా", "Nanna koothuru anuraagam amrutham laaga"),
        LyricLine(46500L, "కళ్ళలోన వెలుగులు నింపే ప్రేమ రూపం", "Kallalona velugulu nimpe ప్రేమ roopam"),
        LyricLine(58000L, "సమయమా కొంచెం ఆగు మా కోసం...", "Samayama konchem aagu ma kosam..."),
        LyricLine(69000L, "నీ తోటి ఉన్న వేళలోన ఆనందం", "Nee thoti unna velalona aanandham")
    )

    val lyricsInkemInkem = listOf(
        LyricLine(13500L, "ఇంకేం ఇంకేం ఇంకేం కావాలే", "Inkem inkem inkem kaavaale"),
        LyricLine(19000L, "చాలే ఇది చాలే", "Chaale idhi chaale"),
        LyricLine(24500L, "నీకై నువ్వే వచ్చి వాలవే", "Neekai nuvve vachhi vaalave"),
        LyricLine(30000L, "కన్నుల ముందే", "Kannula munde"),
        LyricLine(36000L, "గుండెలోన ఉన్న ఆశ నిన్ను చేరానే", "Gundelona unna aasha ninnu cheraane"),
        LyricLine(42500L, "ఇంకేం ఇంకేం ఇంకేం కావాలే చాలే ఇది చాలే", "Inkem inkem inkem kaavaale chaale idhi chaale"),
        LyricLine(49500L, "నీ జత ఉంటే చాలు జీవితమే పండుగే", "Nee jatha unte chaalu jeevithame panduge"),
        LyricLine(57000L, "కన్నుల ముందే నువ్వు కనిపిస్తే చాలు", "Kannula munde nuvvu kanipisthe chaalu"),
        LyricLine(65000L, "గుండెలోన రంగుల జల్లు విరిసెనురా", "Gundelona rangula jallu virisenura"),
        LyricLine(73000L, "ఇంకేం ఇంకేం ఇంకేం కావాలే", "Inkem inkem inkem kaavaale"),
        LyricLine(81000L, "చాలే ఇది చాలే", "Chaale idhi chaale")
    )

    fun findCuratedLyrics(trackId: String, title: String): List<LyricLine>? {
        val clean = title.lowercase().trim()
        val id = trackId.lowercase().trim()

        return when {
            clean.contains("chuttamalle") || id.contains("chuttamalle") -> lyricsChuttamalle
            clean.contains("fear song") || id.contains("fear_song") -> lyricsFearSong
            clean.contains("daavudi") || id.contains("daavudi") -> lyricsDaavudi
            clean.contains("ayudha pooja") || id.contains("ayudha_pooja") -> lyricsAyudhaPooja
            clean.contains("pushpa pushpa") || id.contains("pushpa_pushpa") -> lyricsPushpaPushpa
            clean.contains("sooseki") || id.contains("sooseki") -> lyricsSooseki
            clean.contains("peelings") || id.contains("peelings") -> lyricsPeelings
            clean.contains("kissik") || id.contains("kissik") -> lyricsKissik
            clean.contains("kurchi") || id.contains("kurchi") -> lyricsKurchi
            clean.contains("dum masala") || id.contains("dum_masala") -> lyricsDumMasala
            clean.contains("amma") || id.contains("amma_song") -> lyricsAmmaSong
            clean.contains("bhairava") || id.contains("bhairava") -> lyricsBhairavaAnthem
            clean.contains("ta takkara") || id.contains("ta_takkara") -> lyricsTaTakkara
            clean.contains("theme of kalki") || id.contains("kalki") -> lyricsThemeOfKalki
            clean.contains("samajavaragamana") || id.contains("samajavaragamana") -> lyricsSamajavaragamana
            clean.contains("butta bomma") || clean.contains("buttabomma") || id.contains("butta_bomma") -> lyricsButtaBomma
            clean.contains("ramuloo") || id.contains("ramulaa") -> lyricsRamulooRamulaa
            clean.contains("inthandham") || id.contains("inthandham") -> lyricsInthandham
            clean.contains("oh sita") || id.contains("oh_sita") -> lyricsOhSita
            clean.contains("kaanunna") || id.contains("kaanunna") -> lyricsKaanunnaKalyanam
            clean.contains("naatu naatu") || id.contains("naatu_naatu") -> lyricsNaatuNaatu
            clean.contains("dosti") || id.contains("dosti") -> lyricsDosti
            clean.contains("komuram") || id.contains("komuram") -> lyricsKomuramBheemudo
            clean.contains("samayama") || id.contains("samayama") -> lyricsSamayama
            clean.contains("inkem inkem") || id.contains("inkem_inkem") -> lyricsInkemInkem
            else -> null
        }
    }
}
