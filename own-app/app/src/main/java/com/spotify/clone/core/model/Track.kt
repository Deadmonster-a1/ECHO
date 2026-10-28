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
    val lyrics: List<LyricLine> = emptyList()
)

data class LyricLine(
    val timestampMs: Long,
    val words: String
)
