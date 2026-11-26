package com.spotify.clone.core.legal

/**
 * Authoritative legal, copyright, privacy, and licensing copy owned by GRAVIT
 * for the echo Android application.
 */
enum class LegalDocumentTab(
    val shortTitle: String,
    val fullTitle: String,
    val subtitle: String
) {
    PRIVACY(
        shortTitle = "Privacy",
        fullTitle = "Privacy Policy",
        subtitle = "How GRAVIT handles local preferences, network requests & data protection"
    ),
    TERMS(
        shortTitle = "Terms",
        fullTitle = "Terms & Conditions",
        subtitle = "License grant, acceptable use, streaming disclaimers & liability terms"
    ),
    COPYRIGHT(
        shortTitle = "Copyright",
        fullTitle = "Copyright, IP & DMCA Notice",
        subtitle = "© 2026 GRAVIT. All rights reserved • Content & takedown policy"
    ),
    PERMISSIONS(
        shortTitle = "Data & Safety",
        fullTitle = "Data, Permissions & Security",
        subtitle = "Device permissions, encrypted TLS transport & offline cache controls"
    ),
    LICENSES(
        shortTitle = "Licenses",
        fullTitle = "Open-Source Licenses",
        subtitle = "Third-party libraries and open-source attributions used in echo"
    )
}

data class LegalClause(
    val heading: String,
    val body: String
)

object GravitLegalCatalog {
    const val OWNER_NAME = "GRAVIT"
    const val APP_BRAND = "echo"
    const val COPYRIGHT_NOTICE = "© 2026 GRAVIT. All rights reserved."
    const val LEGAL_EMAIL = "legal@gravit.app"
    const val SUPPORT_EMAIL = "support@gravit.app"
    const val EFFECTIVE_DATE = "Effective November 27, 2026"

    private val privacyClauses = listOf(
        LegalClause(
            heading = "1. Who Controls Your Information",
            body = "echo is developed and published by GRAVIT (\"we\", \"our\", or \"us\"). GRAVIT is committed to transparent, privacy-first software engineering with minimal data exposure."
        ),
        LegalClause(
            heading = "2. Local-First Data Storage",
            body = "Your profile display name, badge color, language selections, preferred genres, favorite artists, liked tracks, playlists, listening history, equalizer presets, and recommendation signals are stored locally on your Android device inside private app storage (Room SQLite database and SharedPreferences). GRAVIT does not operate account tracking servers, sell personal profiles, or serve third-party behavioral advertising SDKs."
        ),
        LegalClause(
            heading = "3. Network & Third-Party Catalog Requests",
            body = "When you search for music, stream tracks, load artist portraits or album covers, fetch synchronized lyrics, or check for app updates, echo sends direct HTTPS requests to public media metadata providers, CDN mirrors (such as jsDelivr and GitHub Releases), and lyrics APIs. Those external services may process standard technical connection data (such as your IP address, query text, and User-Agent) in accordance with their respective privacy policies."
        ),
        LegalClause(
            heading = "4. Audio Cache & Offline Storage",
            body = "Streamed audio segments may be buffered locally in your device cache (up to 250 MB) to enable smooth playback and reduced data usage. Explicitly saved offline tracks remain in private app files until removed. You can wipe temporary audio cache at any time in Settings -> Clear Audio Cache, or reset recommendation weights via Reset Taste Profile."
        ),
        LegalClause(
            heading = "5. Backups, Retention & Erasure",
            body = "echo disables unencrypted cloud auto-backup (android:allowBackup=\"false\") so listening history and local preferences stay on your device. Uninstalling echo or clearing application data in Android System Settings permanently erases all stored user preferences, playlists, and cached media."
        ),
        LegalClause(
            heading = "6. Children's Privacy",
            body = "echo is intended for general audiences aged 13 and older (or the minimum digital consent age in your jurisdiction). We do not knowingly collect personal information from children."
        ),
        LegalClause(
            heading = "7. Policy Updates & Privacy Inquiries",
            body = "We may revise this Privacy Policy alongside new app releases. Material updates will be reflected inside Settings -> Legal & About and this document's effective date. Contact $LEGAL_EMAIL for any privacy questions."
        )
    )

    private val termsClauses = listOf(
        LegalClause(
            heading = "1. Acceptance of Terms",
            body = "By installing, opening, or using echo, you agree to be bound by these Terms & Conditions and the GRAVIT Privacy Policy. If you do not agree, do not use the application."
        ),
        LegalClause(
            heading = "2. Ownership & Limited License",
            body = "GRAVIT owns all rights, title, and interest in the echo software, visual design system, Material 3 Expressive interfaces, source code, recommendation models, and brand marks. Subject to compliance with these Terms, GRAVIT grants you a limited, revocable, non-exclusive, non-transferable, non-commercial license to install and use echo for personal entertainment on Android devices you own or control."
        ),
        LegalClause(
            heading = "3. Restrictions & Acceptable Use",
            body = "You may not: (a) redistribute, sell, rent, sublicense, or commercially exploit echo or any proprietary GRAVIT assets; (b) decompile, disassemble, extract, or clone GRAVIT source code or UI assets for unauthorized derivative apps; (c) use automated scripts or bots to abuse third-party media endpoints; or (d) strip or alter GRAVIT copyright, trademark, or attribution notices."
        ),
        LegalClause(
            heading = "4. Third-Party Media & Local Files",
            body = "echo acts as a media player and discovery interface for local audio files on your device and publicly accessible third-party catalogs. GRAVIT does not host, upload, or guarantee uninterrupted availability of external streams, cover artwork, or lyrics. You are solely responsible for ensuring you have lawful rights to any local audio files you import or play."
        ),
        LegalClause(
            heading = "5. Over-The-Air (OTA) Software Updates",
            body = "echo checks official GRAVIT release manifests over HTTPS and verifies cryptographic APK signing certificates before presenting optional or mandatory updates. Installing downloaded updates requires explicit user consent via Android's package installer."
        ),
        LegalClause(
            heading = "6. Disclaimer of Warranties",
            body = "TO THE MAXIMUM EXTENT PERMITTED BY APPLICABLE LAW, ECHO IS PROVIDED \"AS IS\" AND \"AS AVAILABLE\" WITHOUT WARRANTIES OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT, OR UNINTERRUPTED NETWORK AVAILABILITY."
        ),
        LegalClause(
            heading = "7. Limitation of Liability",
            body = "TO THE FULLEST EXTENT PERMITTED BY LAW, GRAVIT AND ITS AFFILIATES SHALL NOT BE LIABLE FOR ANY INDIRECT, INCIDENTAL, SPECIAL, CONSEQUENTIAL, OR PUNITIVE DAMAGES, LOSS OF DATA, OR SERVICE INTERRUPTIONS ARISING FROM YOUR USE OF ECHO OR THIRD-PARTY CONTENT."
        ),
        LegalClause(
            heading = "8. Termination & Governing Law",
            body = "Your license terminates automatically if you breach these Terms, upon which you must uninstall the application. These Terms are governed by applicable laws without regard to conflict-of-law principles, except where mandatory consumer protection laws in your country apply."
        )
    )

    private val copyrightClauses = listOf(
        LegalClause(
            heading = "1. GRAVIT Copyright Reservation",
            body = "$COPYRIGHT_NOTICE All application source code, echo branding, acoustic logo animations, Material 3 Expressive components, equalizer DSP pipelines, recommendation engine logic, and bundled graphics are the exclusive copyright and intellectual property of GRAVIT."
        ),
        LegalClause(
            heading = "2. Trademarks",
            body = "\"GRAVIT\", \"echo\", \"Feel the sound.\", and the echo acoustic wave mark are trademarks or brand identifiers of GRAVIT. All third-party company, artist, album, and product names remain the property of their respective owners and are used solely for identification."
        ),
        LegalClause(
            heading = "3. Music, Lyrics & Artwork Rights",
            body = "All musical compositions, sound recordings, album artwork, artist portraits, and lyric texts displayed or streamed through echo belong exclusively to their respective artists, songwriters, music publishers, labels, and licensors. Playing or viewing metadata in echo transfers no copyright ownership."
        ),
        LegalClause(
            heading = "4. DMCA & Copyright Takedown Policy",
            body = "GRAVIT respects intellectual property rights. If you believe any metadata, artwork link, lyric reference, or release distribution accessible through echo infringes your copyright, send a written notice to $LEGAL_EMAIL including: (a) identification of the copyrighted work, (b) exact title/URI or screen reference, (c) your contact details, and (d) a good-faith statement under penalty of perjury that you are authorized to act for the rights holder."
        )
    )

    private val permissionClauses = listOf(
        LegalClause(
            heading = "1. Transport Security (HTTPS Only)",
            body = "NetworkSecurity enforces encrypted HTTPS connections across catalog searches, artwork loading, lyrics resolution, and OTA update verification, rejecting untrusted cleartext or unverified update URLs."
        ),
        LegalClause(
            heading = "2. Network & Connectivity (INTERNET / ACCESS_NETWORK_STATE)",
            body = "Required to stream remote audio previews and full tracks, load album artwork and synchronized lyrics, and verify authentic signed releases published by GRAVIT."
        ),
        LegalClause(
            heading = "3. Background Playback (FOREGROUND_SERVICE_MEDIA_PLAYBACK / WAKE_LOCK)",
            body = "Ensures uninterrupted audio playback with lock-screen Media3 controls, Bluetooth headset buttons, and Android Auto compatibility when echo is minimized or the display is off."
        ),
        LegalClause(
            heading = "4. Notifications (POST_NOTIFICATIONS)",
            body = "Displays active media playback notification controls, artwork, and play/pause/skip actions on Android 13+."
        ),
        LegalClause(
            heading = "5. Local Audio Access (READ_MEDIA_AUDIO / READ_EXTERNAL_STORAGE)",
            body = "Optional runtime permission requested only when scanning your device's Music/Audio library for local MP3, FLAC, or WAV playback inside the Library tab."
        ),
        LegalClause(
            heading = "6. In-App Update Installer (REQUEST_INSTALL_PACKAGES)",
            body = "Allows you to install verified APK updates signed by GRAVIT's official release certificate directly from Settings or the update prompt."
        )
    )

    private val licenseClauses = listOf(
        LegalClause(
            heading = "Proprietary Application Code — GRAVIT",
            body = "$COPYRIGHT_NOTICE Unauthorized reproduction, rebranding, or commercial redistribution of echo application code, assets, or visual design is prohibited without prior written permission from GRAVIT."
        ),
        LegalClause(
            heading = "Jetpack Compose & AndroidX Libraries (Apache License 2.0)",
            body = "Includes AndroidX Core, Lifecycle, Navigation, Activity Compose, Material 3, Room Persistence, Media3 ExoPlayer & MediaSession, and Core Splashscreen, licensed under the Apache License, Version 2.0."
        ),
        LegalClause(
            heading = "Kotlin & Kotlinx Coroutines (Apache License 2.0)",
            body = "Kotlin Standard Library and JetBrains Kotlinx Coroutines, copyright JetBrains s.r.o. and contributors, licensed under the Apache License, Version 2.0."
        ),
        LegalClause(
            heading = "Coil — Compose Image Loader (Apache License 2.0)",
            body = "Asynchronous artwork and artist portrait loading powered by Coil, copyright Coil Contributors, licensed under the Apache License, Version 2.0."
        ),
        LegalClause(
            heading = "Apache License 2.0 Notice Summary",
            body = "Third-party open-source components licensed under Apache 2.0 are distributed \"AS IS\" without warranties or conditions of any kind. Full license text is available at https://www.apache.org/licenses/LICENSE-2.0."
        )
    )

    fun clausesFor(tab: LegalDocumentTab): List<LegalClause> = when (tab) {
        LegalDocumentTab.PRIVACY -> privacyClauses
        LegalDocumentTab.TERMS -> termsClauses
        LegalDocumentTab.COPYRIGHT -> copyrightClauses
        LegalDocumentTab.PERMISSIONS -> permissionClauses
        LegalDocumentTab.LICENSES -> licenseClauses
    }
}
