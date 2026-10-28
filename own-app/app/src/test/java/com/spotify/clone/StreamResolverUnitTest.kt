package com.spotify.clone

import com.spotify.clone.core.data.FullTrackStreamResolver
import com.spotify.clone.core.model.Track
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamResolverUnitTest {

    @Test
    fun testNeedsResolutionWithBlankUri() {
        val track = Track(
            id = "t1",
            title = "Test Song",
            artistName = "Artist",
            albumName = "Album",
            coverArtUrl = "",
            durationMs = 120000L,
            mediaUri = ""
        )
        assertTrue(FullTrackStreamResolver.needsResolution(track))
    }

    @Test
    fun testNeedsResolutionWithPreviewUri() {
        val track = Track(
            id = "t2",
            title = "Preview Song",
            artistName = "Artist",
            albumName = "Album",
            coverArtUrl = "",
            durationMs = 120000L,
            mediaUri = "https://audio-ssl.itunes.apple.com/apple-assets-us-std-000001/Music125/v4/preview.m4a"
        )
        assertTrue(FullTrackStreamResolver.needsResolution(track))
    }

    @Test
    fun testNeedsResolutionWithEncryptedSaavnUri() {
        val track = Track(
            id = "t3",
            title = "Saavn Encrypted",
            artistName = "Artist",
            albumName = "Album",
            coverArtUrl = "",
            durationMs = 120000L,
            mediaUri = "saavn_enc:kKJH89234jhdfskjfs="
        )
        assertTrue(FullTrackStreamResolver.needsResolution(track))
    }

    @Test
    fun testNeedsResolutionFalseForDirectStream() {
        val track = Track(
            id = "t4",
            title = "Full Studio Stream",
            artistName = "Artist",
            albumName = "Album",
            coverArtUrl = "",
            durationMs = 120000L,
            mediaUri = "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Kangaroo_MusiQue_-_The_Neverending_Story.mp3"
        )
        assertFalse(FullTrackStreamResolver.needsResolution(track))
    }
}
