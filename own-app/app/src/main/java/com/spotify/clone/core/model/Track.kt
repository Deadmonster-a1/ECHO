package com.spotify.clone.core.model

data class Track(
    val id: String,
    val title: String,
    val artistName: String,
    val albumName: String,
    val coverArtUrl: String,
    val durationMs: Long,
    val mediaUri: String,
    val isExplicit: Boolean = false,
    val isFavorite: Boolean = false,
    val lyrics: List<LyricLine> = emptyList(),
    val lyricsSyncType: LyricsSyncType = LyricsSyncType.LINE_SYNCED
)

enum class LyricsSyncType {
    NONE,
    UNSYNCED,
    LINE_SYNCED,
    SYLLABLE_SYNCED
}

data class LyricLine(
    val timestampMs: Long,
    val words: String,
    val romanized: String? = null
)

data class LyricsResult(
    val syncType: LyricsSyncType,
    val lines: List<LyricLine>,
    val provider: String = "Spotify / LRCLIB"
) {
    val hasRomanized: Boolean
        get() = lines.any { !it.romanized.isNullOrBlank() && it.romanized != it.words }
}
