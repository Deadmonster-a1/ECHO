package com.spotify.clone

import com.spotify.clone.core.data.SampleCatalog
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SampleCatalogUnitTest {

    @Test
    fun testSampleTracksAreNotEmptyAndHaveValidUris() {
        val tracks = SampleCatalog.sampleTracks
        assertTrue("Sample tracks must not be empty", tracks.isNotEmpty())

        for (track in tracks) {
            assertTrue("Track title cannot be blank: ${track.id}", track.title.isNotBlank())
            assertTrue("Artist name cannot be blank: ${track.id}", track.artistName.isNotBlank())
            assertTrue("Media URI cannot be blank: ${track.id}", track.mediaUri.isNotBlank())
            assertTrue("Media URI must be valid HTTP/HTTPS: ${track.mediaUri}", track.mediaUri.startsWith("http://") || track.mediaUri.startsWith("https://"))
            assertTrue("Duration must be positive: ${track.id}", track.durationMs > 0L)
        }
    }

    @Test
    fun testSamplePlaylistsContainTracks() {
        val playlists = SampleCatalog.samplePlaylists
        assertTrue("Sample playlists must not be empty", playlists.isNotEmpty())

        for (playlist in playlists) {
            assertTrue("Playlist name cannot be blank: ${playlist.id}", playlist.name.isNotBlank())
            assertTrue("Playlist tracks cannot be empty: ${playlist.name}", playlist.tracks.isNotEmpty())
        }
    }

    @Test
    fun testSampleArtistsAreValid() {
        val artists = SampleCatalog.sampleArtists
        assertTrue("Sample artists must not be empty", artists.isNotEmpty())

        for (artist in artists) {
            assertTrue("Artist name cannot be blank: ${artist.id}", artist.name.isNotBlank())
        }
    }
}
