package com.echo.core.data.repository

import com.echo.core.domain.model.CrossbarColorScheme
import com.echo.themekit.ThemeSettings
import org.junit.Assert.assertEquals
import org.junit.Test

// theme-kit keeps its own list of colour schemes; a scheme added here and not there could not be set by a theme,
// and one removed here and kept there would reach valueOf and crash the crossbar
class ThemeColorSchemeTest {
    @Test
    fun `a theme can set every colour scheme, and no other`() {
        assertEquals(CrossbarColorScheme.entries.map { it.name }.toSet(), ThemeSettings.CHOICES["display_color_scheme"])
    }
}
