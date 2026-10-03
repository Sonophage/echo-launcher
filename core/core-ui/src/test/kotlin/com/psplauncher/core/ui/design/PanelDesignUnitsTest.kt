package com.psplauncher.core.ui.design

import androidx.compose.ui.unit.Density
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
}
