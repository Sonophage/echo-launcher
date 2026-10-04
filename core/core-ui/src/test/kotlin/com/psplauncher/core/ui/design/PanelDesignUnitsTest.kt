package com.psplauncher.core.ui.design

import androidx.compose.ui.unit.Density
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PanelDesignUnitsTest {
    @Test
    fun `the 1200 by 752 panel frame fits both test screens`() {
        listOf(1067f to 668f, 821f to 462f).forEach { (w, h) ->
            val u = panelDesignUnits(w, h, Density(1f))
            assertTrue("frame wider than a ${w}x$h screen", u.dp(1200).value <= w + 0.01f)
            assertTrue("frame taller than a ${w}x$h screen", u.dp(752).value <= h + 0.01f)
        }
    }

    @Test
    fun `only a screen too narrow for the landscape frame takes the square layout`() {
        assertFalse("the NP05J must keep the landscape layout", panelIsSquare(1067f, 668f))
        assertFalse("the Konker must keep the landscape layout", panelIsSquare(821f, 462f))
        assertTrue("a 1:1 screen leaves the bottom of the frame empty", panelIsSquare(668f, 668f))
        assertTrue("a 4:3 screen leaves the bottom of the frame empty", panelIsSquare(1024f, 768f))
        assertTrue(panelDesignUnits(668f, 668f, Density(1f)).square)
    }
}
