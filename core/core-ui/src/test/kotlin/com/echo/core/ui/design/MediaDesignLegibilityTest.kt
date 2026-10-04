package com.echo.core.ui.design

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaDesignLegibilityTest {
    private val konkerDensity = 2.3375f

    @Test
    fun `scaled-down design text never renders under the floor`() {
        val konkerScale = mediaDesignScale(821f, 462f)
        val eyebrowPx = 12f * konkerScale * konkerDensity
        assertTrue("precondition: the mock's 12px eyebrow lands at ${eyebrowPx}px on the Konker", eyebrowPx < LEGIBILITY_FLOOR_PX)
        assertTrue(legibleTextPx(eyebrowPx) >= LEGIBILITY_FLOOR_PX)
    }

    @Test
    fun `text already above the floor keeps its size`() {
        assertEquals(60f, legibleTextPx(60f), 0f)
    }
}
