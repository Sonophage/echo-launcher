package com.echo.themekit

import kotlin.test.Test
import kotlin.test.assertEquals

// owner, 2026-10-05: the top bar and the footer are sized on their own, per device, and survive a save
class ChromeSizeTest {
    @Test
    fun `header and footer sizes are kept per device and clamped to a usable range`() {
        val saved = mapOf(
            CrossbarFormFactor.COMPACT.key to CrossbarLayoutAdjust(headerScale = 1.2f, footerScale = 9f),
            CrossbarFormFactor.EXPANDED.key to CrossbarLayoutAdjust(headerScale = 0.1f, footerScale = Float.NaN),
        )
        val back = CrossbarLayoutAdjustCodec.decode(CrossbarLayoutAdjustCodec.encode(saved))

        assertEquals(1.2f, back.getValue("compact").headerScale)
        assertEquals(CrossbarLayoutAdjust.CHROME_MAX, back.getValue("compact").footerScale)
        assertEquals(CrossbarLayoutAdjust.CHROME_MIN, back.getValue("expanded").headerScale)
        assertEquals(1f, back.getValue("expanded").footerScale, "a broken value falls back to the normal size")
    }

    @Test
    fun `a layout saved before the sizes existed reads as normal size`() {
        val old = """{"compact":{"scale":1.1,"barLeftFraction":0.0,"barTopFraction":0.2}}"""
        val back = CrossbarLayoutAdjustCodec.decode(old).getValue("compact")
        assertEquals(1f, back.headerScale)
        assertEquals(1f, back.footerScale)
    }
}
