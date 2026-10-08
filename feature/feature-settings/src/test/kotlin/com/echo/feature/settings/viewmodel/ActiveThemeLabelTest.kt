package com.echo.feature.settings.viewmodel

import com.echo.themekit.ThemePart
import org.junit.Assert.assertEquals
import org.junit.Test

// owner, 2026-10-07: once theme pages take parts from different themes, Theme says the look is a mix
class ActiveThemeLabelTest {
    @Test
    fun `one theme's parts name it, parts from two themes are a mix, none is the default`() {
        assertEquals("Aurora", activeThemeLabel("Aurora", mapOf(ThemePart.ICONS to "Aurora", ThemePart.WAVE to "Aurora")))
        assertEquals("Mixed", activeThemeLabel("Aurora", mapOf(ThemePart.ICONS to "Aurora", ThemePart.SOUNDS to "Dusk")))
        assertEquals("Dusk", activeThemeLabel(null, mapOf(ThemePart.SOUNDS to "Dusk")))
        assertEquals("Default", activeThemeLabel(null, emptyMap()))
    }
}
