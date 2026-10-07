package com.echo.core.data.repository

import com.echo.themekit.EchoThemeManifest
import org.junit.Assert.assertEquals
import org.junit.Test

// theme-kit keeps its own list of game-start styles; a style added here and not there could not be set by a theme
class ThemeGameStartStyleTest {
    @Test
    fun `a theme can set every game-start style`() {
        assertEquals(GameBootStyle.entries.map { it.name }.toSet(), EchoThemeManifest.GAME_START_STYLES)
    }
}
