package com.echo.feature.crossbar.music

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PlayOrderTest {
    @Test fun `in order, the album plays through and stops at the end`() {
        val o = PlayOrder(3, 0)
        assertEquals(1, o.advance(auto = true, repeat = RepeatMode.OFF))
        assertEquals(2, o.advance(auto = true, repeat = RepeatMode.OFF))
        assertNull("repeat off must not loop back to the first track", o.advance(auto = true, repeat = RepeatMode.OFF))
    }

    @Test fun `repeat all loops back to the first track`() {
        val o = PlayOrder(3, 2)
        assertEquals(0, o.advance(auto = true, repeat = RepeatMode.ALL))
    }

    @Test fun `repeat one replays the track when it ends, but next still moves on`() {
        val o = PlayOrder(3, 1)
        assertEquals("a finished track replays", 1, o.advance(auto = true, repeat = RepeatMode.ONE))
        assertEquals("pressing next is not blocked by repeat one", 2, o.advance(auto = false, repeat = RepeatMode.ONE))
    }

    @Test fun `shuffle keeps the playing track and plays every other track once`() {
        val o = PlayOrder(20, 7, shuffle = true, random = Random(1))
        assertEquals("turning shuffle on must not change the song", 7, o.current)
        val played = generateSequence { o.advance(auto = true, repeat = RepeatMode.OFF) }.toList()
        assertEquals((0 until 20).toSet() - 7, played.toSet())
        assertEquals("a track repeated before the queue ran out", 19, played.size)
    }

    @Test fun `shuffle actually changes the order`() {
        val o = PlayOrder(20, 0, shuffle = true, random = Random(1))
        assertTrue(o.upcoming(19, RepeatMode.OFF) != (1 until 20).toList())
    }

    @Test fun `turning shuffle off goes back to album order after the playing track`() {
        val o = PlayOrder(10, 3, shuffle = true, random = Random(2))
        o.advance(auto = false, repeat = RepeatMode.OFF)
        val playing = o.current
        o.setShuffle(false)
        assertEquals(playing, o.current)
        assertEquals(listOf(playing + 1, playing + 2), o.upcoming(2, RepeatMode.OFF).take(2))
    }

    @Test fun `up next follows the play order and wraps only when repeating`() {
        val o = PlayOrder(4, 3)
        assertEquals(emptyList<Int>(), o.upcoming(2, RepeatMode.OFF))
        assertEquals(listOf(0, 1), o.upcoming(2, RepeatMode.ALL))
    }

    @Test fun `previous stops at the first track`() {
        val o = PlayOrder(3, 0)
        assertNull(o.back())
    }

    @Test fun `repeat cycles off, all, one`() {
        assertEquals(RepeatMode.ALL, RepeatMode.OFF.next())
        assertEquals(RepeatMode.ONE, RepeatMode.ALL.next())
        assertEquals(RepeatMode.OFF, RepeatMode.ONE.next())
    }
}
