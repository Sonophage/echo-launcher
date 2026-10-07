package com.echo.core.ui.wave

import com.echo.themekit.EchoThemeManifest
import org.junit.Assert.assertEquals
import org.junit.Test

// theme-kit keeps its own list of wave designs; a design added here and not there could not be set by a theme
class ThemeWaveDesignTest {
    @Test
    fun `a theme can set every wave design`() {
        assertEquals(WaveDesign.entries.map { it.name }.toSet(), EchoThemeManifest.WAVE_DESIGNS)
    }
}
