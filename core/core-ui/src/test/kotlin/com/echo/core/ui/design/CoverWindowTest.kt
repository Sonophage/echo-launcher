package com.echo.core.ui.design

import org.junit.Assert.assertEquals
import org.junit.Test

// owner, 2026-10-05: a cover keeps its own shape on the VHS case, in the App Drawer and in Search
class CoverWindowTest {
    @Test
    fun `a cover's window keeps the art's shape, so no cover is stretched`() {
        // a 160 x 230 face
        assertEquals("a square Game Boy box sits full width, plastic above and below", 160f to 160f, coverWindow(160f, 230f, 1f))
        assertEquals("a tall Switch cover fills the height", 230f * 0.62f to 230f, coverWindow(160f, 230f, 0.62f))
        assertEquals("a wide DS box sits full width", 160f to 160f / 1.12f, coverWindow(160f, 230f, 1.12f))
    }
}
