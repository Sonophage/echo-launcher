package com.echo.studio

import com.echo.studio.io.ColorHex
import kotlin.test.Test
import kotlin.test.assertEquals

class ColorHexTest {
    @Test
    fun `hex helpers round-trip`() {
        assertEquals("#0055AA", ColorHex.toHexRgb(0xFF0055AA.toInt()))
        assertEquals(0xFF0055AA.toInt(), ColorHex.parseHexRgb("#0055AA"))
        assertEquals(0xFF0055AA.toInt(), ColorHex.parseHexRgb("#FF0055AA"))
        assertEquals(null, ColorHex.parseHexRgb("#GGGGGG"))
        assertEquals(null, ColorHex.parseHexRgb("0055A"))
    }
}
