package com.echo.feature.crossbar.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class StripSectionLabelTest {
    // owner, 2026-10-06: an icon for every filter, and only the chosen one followed by its word, to keep the
    // footer short; a row without icons keeps every label, or its tabs would be invisible
    @Test
    fun `an icon row names only the chosen filter, a word row names them all`() {
        assertEquals(true, sectionLabelShown(hasIcon = true, selected = true))
        assertEquals("the others are their icons alone", false, sectionLabelShown(hasIcon = true, selected = false))
        assertEquals(true, sectionLabelShown(hasIcon = false, selected = false))
    }
}
