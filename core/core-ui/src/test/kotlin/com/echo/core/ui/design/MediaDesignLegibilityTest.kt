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

    @Test
    fun `the UI kit's small text keeps its design size on the tablet, so the floor only lifts the Konker`() {
        val tabletPanelPx = { designPx: Float -> designPx * minOf(1067f / 1200f, 668f / 752f) * 2.25f }
        val body = tabletPanelPx(13f)
        assertEquals("13px body text is drawn as designed on the NP05J", body, legibleTextPx(body), 0f)

        val konkerPanelPx = 10f * minOf(821f / 1200f, 462f / 752f) * konkerDensity
        assertTrue("a 10px caps label is lifted on the Konker", legibleTextPx(konkerPanelPx) > konkerPanelPx)
        assertTrue("but stays well under the old 28px floor", legibleTextPx(konkerPanelPx) < 28f)
    }
}
