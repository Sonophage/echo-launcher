package com.echo.core.domain.model

import com.echo.themekit.EchoThemeManifest
import com.echo.themekit.ThemeMedia
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// theme-kit cannot see these enums, so it keeps its own lists of them; a slot or button set added here and
// not there would be dropped from every theme without a sign
class ThemeMediaSlotsTest {
    @Test
    fun `a theme carries every media slot ECHO has, and no other`() {
        assertEquals(UiMediaSlot.entries.map { it.key }.toSet(), ThemeMedia.FOLDERS.keys)
    }

    @Test
    fun `every button set a theme names is a controller display type`() {
        val types = ControllerDisplayType.entries.map { it.name }.toSet()
        assertTrue(EchoThemeManifest.BUTTON_SETS.all { it in types })
    }
}
