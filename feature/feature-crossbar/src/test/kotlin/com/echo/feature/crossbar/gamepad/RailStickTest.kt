package com.echo.feature.crossbar.gamepad

import kotlin.test.Test
import kotlin.test.assertEquals

// owner, 2026-10-09: the right stick drives the side rail. Pushed past the dead zone it engages, and it lets go only
// under the release line, so a stick resting near the edge does not open and close the rail over and over
class RailStickTest {
    private val activation = 0.35f

    @Test
    fun `pushed up or down it engages, and at rest it lets go`() {
        assertEquals(-1, railStickDirection(-0.8f, 0, activation))
        assertEquals(1, railStickDirection(0.8f, 0, activation))
        assertEquals(0, railStickDirection(0.1f, 1, activation))
    }

    @Test
    fun `engaged, it holds above the release line and a small push does not engage it`() {
        assertEquals(1, railStickDirection(0.3f, 1, activation), "0.3 is under the dead zone but over the release line")
        assertEquals(0, railStickDirection(0.3f, 0, activation), "the same 0.3 from rest does nothing")
        assertEquals(0, railStickDirection(-0.3f, 1, activation), "a flick across lets go before it turns")
    }
}
