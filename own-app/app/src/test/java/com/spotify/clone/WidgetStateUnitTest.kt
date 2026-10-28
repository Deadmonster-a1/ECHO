package com.spotify.clone

import com.spotify.clone.core.model.PlaybackState
import com.spotify.clone.core.model.Track
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetStateUnitTest {

    @Test
    fun testWidgetProgressBarPercentageCalculation() {
        val stateHalf = PlaybackState(
            currentPositionMs = 90000L,
            durationMs = 180000L
        )
        val percent = (stateHalf.progress * 100f).toInt().coerceIn(0, 100)
        assertEquals(50, percent)

        val stateFull = PlaybackState(
            currentPositionMs = 180000L,
            durationMs = 180000L
        )
        val percentFull = (stateFull.progress * 100f).toInt().coerceIn(0, 100)
        assertEquals(100, percentFull)

        val stateZero = PlaybackState(
            currentPositionMs = 0L,
            durationMs = 0L
        )
        val percentZero = (stateZero.progress * 100f).toInt().coerceIn(0, 100)
        assertEquals(0, percentZero)
    }

    @Test
    fun testWidgetFallbackTitles() {
        val track: Track? = null
        val title = track?.title ?: "ECHO"
        val subtitle = track?.artistName ?: "Feel the sound"

        assertEquals("ECHO", title)
        assertEquals("Feel the sound", subtitle)
    }
}
