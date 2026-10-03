package com.psplauncher.feature.launcher

import org.junit.Test
import kotlin.test.assertEquals

class ForegroundMillisTest {
    private fun on(at: Long, cls: String = "Main") = ForegroundMark(at, cls, resumed = true)
    private fun off(at: Long, cls: String = "Main") = ForegroundMark(at, cls, resumed = false)

    @Test
    fun `time away from the emulator is not play time`() {
        assertEquals(300L, foregroundMillis(listOf(on(100), off(200), on(1_000), off(1_200)), to = 5_000))
    }

    @Test
    fun `screen off ends play time even when the emulator never paused`() {
        val marks = listOf(on(0), ForegroundMark(400, "", resumed = false, screenOff = true), on(10_000))
        assertEquals(400L + 500L, foregroundMillis(marks, to = 10_500))
    }

    @Test
    fun `a hand-over between two emulator activities is one continuous span`() {
        val marks = listOf(on(0, "Menu"), on(100, "Game"), off(110, "Menu"), off(900, "Game"))
        assertEquals(900L, foregroundMillis(marks, to = 2_000))
    }

    @Test
    fun `an emulator still in front counts up to now`() {
        assertEquals(700L, foregroundMillis(listOf(on(300)), to = 1_000))
    }

    @Test
    fun `an open session survives the round trip through storage`() {
        val s = OpenSession(7L, "psx", "com.retroarch", 1_790_000_000_000L)
        assertEquals(s, decodeSession(encodeSession(s)))
        assertEquals(s.copy(packageName = null), decodeSession(encodeSession(s.copy(packageName = null))))
    }
}
