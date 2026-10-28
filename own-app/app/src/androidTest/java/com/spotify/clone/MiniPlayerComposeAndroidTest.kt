package com.spotify.clone

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.spotify.clone.core.designsystem.EchoTheme
import com.spotify.clone.core.model.PlaybackState
import com.spotify.clone.core.model.Track
import com.spotify.clone.ui.components.MiniPlayerBar
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MiniPlayerComposeAndroidTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun miniPlayerDisplaysTrackAndRespondsToPlayPause() {
        var playPauseClicked = false
        val testTrack = Track(
            id = "test_t1",
            title = "Test Melodic",
            artistName = "Echo Harmony",
            albumName = "Pure Sound",
            coverArtUrl = "",
            mediaUri = "https://example.com/test.mp3",
            durationMs = 200000L
        )

        val state = PlaybackState(
            currentTrack = testTrack,
            isPlaying = true,
            currentPositionMs = 50000L,
            durationMs = 200000L
        )

        composeTestRule.setContent {
            EchoTheme {
                MiniPlayerBar(
                    playbackState = state,
                    onTogglePlayPause = { playPauseClicked = true },
                    onToggleFavorite = {},
                    onClick = {}
                )
            }
        }

        // Verify title and artist are rendered
        composeTestRule.onNodeWithText("Test Melodic").assertIsDisplayed()
        composeTestRule.onNodeWithText("Echo Harmony").assertIsDisplayed()

        // Verify Pause icon is displayed when playing
        composeTestRule.onNodeWithContentDescription("Pause").assertIsDisplayed()

        // Perform click on play/pause
        composeTestRule.onNodeWithContentDescription("Pause").performClick()
        assertTrue("Play/pause callback must be triggered on press", playPauseClicked)
    }

    @Test
    fun miniPlayerDisplaysPlayWhenPaused() {
        val testTrack = Track(
            id = "test_t2",
            title = "Ambient Echoes",
            artistName = "Nova",
            albumName = "Atmosphere",
            coverArtUrl = "",
            mediaUri = "https://example.com/test2.mp3",
            durationMs = 180000L
        )

        val state = PlaybackState(
            currentTrack = testTrack,
            isPlaying = false,
            currentPositionMs = 0L,
            durationMs = 180000L
        )

        composeTestRule.setContent {
            EchoTheme {
                MiniPlayerBar(
                    playbackState = state,
                    onTogglePlayPause = {},
                    onToggleFavorite = {},
                    onClick = {}
                )
            }
        }

        // Verify Play icon is displayed when paused
        composeTestRule.onNodeWithContentDescription("Play").assertIsDisplayed()
    }
}
