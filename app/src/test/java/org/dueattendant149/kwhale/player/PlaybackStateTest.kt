package org.dueattendant149.kwhale.player

import org.dueattendant149.kwhale.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackStateTest {

    private fun track(id: Long) = Track(
        id = id,
        title = "Track $id",
        artist = "Artist",
        durationSec = 120,
    )

    @Test
    fun repeatReflectsRepeatMode() {
        assertFalse(PlaybackState(repeatMode = RepeatMode.OFF).repeat)
        assertTrue(PlaybackState(repeatMode = RepeatMode.ALL).repeat)
        assertTrue(PlaybackState(repeatMode = RepeatMode.ONE).repeat)
    }

    @Test
    fun currentReturnsActiveTrackOrNullOutOfBounds() {
        val queue = listOf(track(10), track(20))
        assertEquals(20L, PlaybackState(queue = queue, index = 1).current?.id)
        assertNull(PlaybackState(queue = queue, index = 5).current)
        assertNull(PlaybackState().current)
    }

    @Test
    fun progressClampsAndHandlesZeroDuration() {
        assertEquals(0f, PlaybackState(positionMs = 500L, durationMs = 0L).progress, 0.0001f)
        assertEquals(0.25f, PlaybackState(positionMs = 250L, durationMs = 1000L).progress, 0.0001f)
        assertEquals(1f, PlaybackState(positionMs = 1500L, durationMs = 1000L).progress, 0.0001f)
        assertEquals(0f, PlaybackState(positionMs = -200L, durationMs = 1000L).progress, 0.0001f)
    }
}
