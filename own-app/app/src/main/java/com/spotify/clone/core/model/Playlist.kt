package com.spotify.clone.core.model

data class Album(
    val id: String,
    val title: String,
    val artistName: String,
    val coverArtUrl: String,
    val releaseYear: Int,
    val tracks: List<Track> = emptyList()
)

data class Artist(
    val id: String,
    val name: String,
    val avatarUrl: String,
    val monthlyListeners: Long,
    val isVerified: Boolean = true,
    val topTracks: List<Track> = emptyList()
)

data class Playlist(
    val id: String,
    val name: String,
    val description: String,
    val coverArtUrl: String,
    val ownerName: String,
    val isFollowed: Boolean = false,
    val tracks: List<Track> = emptyList()
)

data class EditorialBanner(
    val id: String,
    val eyebrow: String,
    val title: String,
    val subtitle: String,
    val coverArtUrl: String,
    val targetPlaylistId: String? = null,
    val targetQuery: String? = null
)
