package com.spotify.clone

import com.spotify.clone.core.lyrics.LyricsSyncEngine
import com.spotify.clone.core.model.LyricLine
import com.spotify.clone.core.model.LyricsResult
import com.spotify.clone.core.model.LyricsSyncType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsSyncEngineUnitTest {

    @Test
    fun `unsynced lyrics are algorithmically aligned monotonically inside track vocal window`() {
        val plain = LyricsResult(
            syncType = LyricsSyncType.UNSYNCED,
            lines = listOf(
                LyricLine(-1L, "First short line"),
                LyricLine(-1L, "Second much longer lyrical phrase with many syllables to sing"),
                LyricLine(-1L, "Third line!"),
                LyricLine(-1L, "Final closing line...")
            ),
            provider = "JioSaavn"
        )

        val synced = LyricsSyncEngine.prepareEnglishSyncedLyrics(plain, trackDurationMs = 180_000L)

        assertEquals(LyricsSyncType.LINE_SYNCED, synced.syncType)
        assertEquals(4, synced.lines.size)
        assertTrue(synced.lines.first().timestampMs >= 5_000L)
        assertTrue(synced.lines.last().timestampMs < 180_000L)
        for (i in 1 until synced.lines.size) {
            assertTrue(
                "Expected monotonic timestamps at $i",
                synced.lines[i].timestampMs > synced.lines[i - 1].timestampMs
            )
        }
        // Longer line should be allotted a longer slice than the short line
        val shortSlice = synced.lines[1].timestampMs - synced.lines[0].timestampMs
        val longSlice = synced.lines[2].timestampMs - synced.lines[1].timestampMs
        assertTrue(longSlice > shortSlice)
    }

    @Test
    fun `indic scripts without romanized field are transliterated to readable English`() {
        val teluguLine = LyricLine(12_000L, "నమస్తే లోకం", romanized = null)
        val hindiLine = LyricLine(18_000L, "दिल से", romanized = null)

        val engTelugu = LyricsSyncEngine.toEnglishText(teluguLine)
        val engHindi = LyricsSyncEngine.toEnglishText(hindiLine)

        assertTrue(engTelugu.isNotBlank())
        assertTrue(engHindi.isNotBlank())
        assertFalse(engTelugu.any { it.code in 0x0900..0x0D7F })
        assertFalse(engHindi.any { it.code in 0x0900..0x0D7F })
    }

    @Test
    fun `existing romanized English text is preserved`() {
        val curated = LyricLine(10_270L, "చుట్టమల్లె", "Chuttamalle")
        assertEquals("Chuttamalle", LyricsSyncEngine.toEnglishText(curated))
    }
}
