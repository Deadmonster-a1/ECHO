package com.spotify.clone

import com.spotify.clone.core.model.PlaybackState
import com.spotify.clone.core.model.RepeatMode
import com.spotify.clone.core.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackStateUnitTest {

    @Test
    fun testInitialPlaybackStateDefaults() {
        val state = PlaybackState()
        assertEquals(null, state.currentTrack)
        assertFalse(state.isPlaying)
        assertFalse(state.isLoading)
        assertEquals(0L, state.currentPositionMs)
        assertEquals(0L, state.durationMs)
        assertEquals(0f, state.progress, 0.001f)
        assertEquals(RepeatMode.OFF, state.repeatMode)
        assertFalse(state.shuffleMode)
        assertTrue(state.queue.isEmpty())
        assertEquals(0, state.queueIndex)
    }

    @Test
    fun testProgressCalculationNormal() {
        val state = PlaybackState(
            currentPositionMs = 60000L,
            durationMs = 120000L
        )
        assertEquals(0.5f, state.progress, 0.001f)
    }

    @Test
    fun testProgressCalculationZeroDuration() {
        val state = PlaybackState(
            currentPositionMs = 5000L,
            durationMs = 0L
        )
        assertEquals(0f, state.progress, 0.001f)
    }

    @Test
    fun testProgressCalculationOutOfBoundsCoerced() {
        val stateExceeding = PlaybackState(
            currentPositionMs = 250000L,
            durationMs = 200000L
        )
        assertEquals(1.0f, stateExceeding.progress, 0.001f)

        val stateNegative = PlaybackState(
            currentPositionMs = -500L,
            durationMs = 200000L
        )
        assertEquals(0.0f, stateNegative.progress, 0.001f)
    }

    @Test
    fun testRepeatModeCycle() {
        var mode = RepeatMode.OFF
        mode = when (mode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        assertEquals(RepeatMode.ALL, mode)

        mode = when (mode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        assertEquals(RepeatMode.ONE, mode)

        mode = when (mode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        assertEquals(RepeatMode.OFF, mode)
    }
}
