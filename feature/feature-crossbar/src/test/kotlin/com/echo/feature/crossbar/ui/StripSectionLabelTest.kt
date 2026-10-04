package com.echo.feature.crossbar.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class StripSectionLabelTest {
    @Test
    fun `icons carry no names, the dot marks the active one, but a row without icons keeps every label`() {
        assertEquals("the 31a selector is icons only", false, sectionLabelShown(hasIcon = true))
        assertEquals("a tab with no icon would be invisible without its label", true, sectionLabelShown(hasIcon = false))
    }
}
