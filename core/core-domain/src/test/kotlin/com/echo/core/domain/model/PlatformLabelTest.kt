package com.echo.core.domain.model

import org.junit.Test
import kotlin.test.assertEquals

class PlatformLabelTest {
    @Test
    fun `an app shortcut reads as an Android app, never as its raw id`() {
        assertEquals("Android app", platformLabel(PlatformIds.APP_SHORTCUT, null))
        assertEquals("Game Boy Advance", platformLabel("gba", "Game Boy Advance"))
        assertEquals("GBA", platformLabel("gba", null))
    }
}
