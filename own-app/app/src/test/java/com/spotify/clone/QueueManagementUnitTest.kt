package com.spotify.clone

import com.spotify.clone.core.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QueueManagementUnitTest {

    private fun createTrack(id: String, title: String) = Track(
        id = id,
        title = title,
        artistName = "Artist $id",
        albumName = "Album $id",
        coverArtUrl = "",
        durationMs = 180000L,
        mediaUri = "https://example.com/audio/$id.mp3"
    )

    @Test
    fun testAddToQueue() {
        val initialQueue = listOf(createTrack("1", "Song 1"), createTrack("2", "Song 2"))
        val newTrack = createTrack("3", "Song 3")

        val updatedQueue = initialQueue + newTrack
        assertEquals(3, updatedQueue.size)
        assertEquals("3", updatedQueue.last().id)
    }

    @Test
    fun testMoveQueueItem() {
        val queue = mutableListOf(
            createTrack("1", "Song 1"),
            createTrack("2", "Song 2"),
            createTrack("3", "Song 3")
        )

        // Move item at index 0 to index 2
        val fromIndex = 0
        val toIndex = 2
        val item = queue.removeAt(fromIndex)
        queue.add(toIndex, item)

        assertEquals("2", queue[0].id)
        assertEquals("3", queue[1].id)
        assertEquals("1", queue[2].id)
    }

    @Test
    fun testRemoveFromQueue() {
        val queue = mutableListOf(
            createTrack("1", "Song 1"),
            createTrack("2", "Song 2"),
            createTrack("3", "Song 3")
        )

        queue.removeAt(1)
        assertEquals(2, queue.size)
        assertEquals("1", queue[0].id)
        assertEquals("3", queue[1].id)
    }

    @Test
    fun testQueueIndexValidation() {
        val queue = listOf(createTrack("1", "Song 1"), createTrack("2", "Song 2"))
        val invalidIndex = 5
        val valid = invalidIndex in queue.indices
        assertTrue(!valid)
    }
}
