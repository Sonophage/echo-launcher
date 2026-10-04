package com.echo.feature.crossbar.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class StripSectionLabelTest {
    @Test
    fun `only the active section names itself, so the centre stays quiet, but a row without icons keeps every label`() {
        assertEquals(true, sectionLabelShown(active = true, hasIcon = true))
        assertEquals("an inactive icon carries no name", false, sectionLabelShown(active = false, hasIcon = true))
        assertEquals("a tab with no icon would be invisible without its label", true, sectionLabelShown(active = false, hasIcon = false))
    }
}
