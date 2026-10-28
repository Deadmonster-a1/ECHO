package com.spotify.clone.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.spotify.clone.core.model.Track

@Entity(
    tableName = "tracks",
    indices = [
        Index("isFavorite"),
        Index("lastPlayedTimestamp")
    ]
)
data class TrackEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artistName: String,
    val albumName: String,
    val coverArtUrl: String,
    val durationMs: Long,
    val mediaUri: String,
    val isExplicit: Boolean = false,
    val isFavorite: Boolean = false,
    val lastPlayedTimestamp: Long = 0L
) {
    fun toDomainModel(): Track = Track(
        id = id,
        title = title,
        artistName = artistName,
        albumName = albumName,
        coverArtUrl = coverArtUrl,
        durationMs = durationMs,
        mediaUri = mediaUri,
        isExplicit = isExplicit,
        isFavorite = isFavorite,
        lyrics = com.spotify.clone.core.data.SampleCatalog.sampleTracks.find { it.id == id }?.lyrics ?: emptyList()
    )

    companion object {
        fun fromDomainModel(track: Track, lastPlayed: Long = 0L): TrackEntity = TrackEntity(
            id = track.id,
            title = track.title,
            artistName = track.artistName,
            albumName = track.albumName,
            coverArtUrl = track.coverArtUrl,
            durationMs = track.durationMs,
            mediaUri = track.mediaUri,
            isExplicit = track.isExplicit,
            isFavorite = track.isFavorite,
            lastPlayedTimestamp = lastPlayed
        )
    }
}

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val coverArtUrl: String,
    val ownerName: String = "You",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_tracks",
    primaryKeys = ["playlistId", "trackId"],
    indices = [
        Index("playlistId"),
        Index("trackId")
    ]
)
data class PlaylistTrackCrossRef(
    val playlistId: String,
    val trackId: String,
    val position: Int
)
