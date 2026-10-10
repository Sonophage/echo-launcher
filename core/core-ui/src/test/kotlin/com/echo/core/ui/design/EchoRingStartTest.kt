package com.echo.core.ui.design

import kotlin.test.Test
import kotlin.test.assertEquals

// owner, 2026-10-09: the battery ring fills up both sides from the bottom, so the gap it leaves sits at the top
class EchoRingStartTest {
    @Test
    fun `a mirrored ring is centred on the bottom and its gap sits at the top`() {
        // a quarter charge: from 45 to 135 degrees, centred on 90, the bottom
        assertEquals(45f, echoRingStart(90f, fromBottom = true))
        // at any charge the gap's middle is the top, 270 degrees
        for (sweep in listOf(60f, 180f, 340f)) {
            val start = echoRingStart(sweep, fromBottom = true)
            assertEquals(270f, start + sweep + (360f - sweep) / 2f, 0.001f, "the gap's middle at $sweep degrees of charge")
        }
    }

    @Test
    fun `the plain ring still starts at the top`() = assertEquals(-90f, echoRingStart(120f, fromBottom = false))
}
