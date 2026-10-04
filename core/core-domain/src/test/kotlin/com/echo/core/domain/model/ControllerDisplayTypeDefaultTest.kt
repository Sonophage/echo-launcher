package com.echo.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ControllerDisplayTypeDefaultTest {
    @Test
    fun `someone who never chose a style gets the generic glyphs`() {
        assertEquals(ControllerDisplayType.GENERIC, ControllerDisplayType.fromName(null))
        assertEquals(ControllerDisplayType.GENERIC, ControllerLayoutPrefs().displayType)
    }

    @Test
    fun `a style someone already picked is kept, Xbox included`() {
        ControllerDisplayType.entries.forEach {
            assertEquals(it, ControllerDisplayType.fromName(it.name))
        }
    }

    @Test
    fun `a stored name this build does not know falls back to generic`() {
        assertEquals(ControllerDisplayType.GENERIC, ControllerDisplayType.fromName("DREAMCAST"))
    }
}
