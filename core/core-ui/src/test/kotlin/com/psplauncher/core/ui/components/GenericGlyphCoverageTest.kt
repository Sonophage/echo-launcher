package com.psplauncher.core.ui.components

import com.psplauncher.core.domain.model.ControllerDisplayType
import com.psplauncher.core.domain.model.ControllerIcon
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class GenericGlyphCoverageTest {
    @Test
    fun `every controller input has a generic glyph, so the default style never draws a blank`() {
        ControllerIcon.entries.forEach { icon ->
            val label = icon.printedLabelFor(ControllerDisplayType.GENERIC)
            assertFalse("$icon has no generic glyph", label.isNullOrBlank())
        }
    }

    @Test
    fun `generic is drawn in code and never reaches for coloured art`() {
        ControllerIcon.entries.forEach { icon ->
            assertNull("$icon resolves to art for generic", icon.drawableForOrNull(ControllerDisplayType.GENERIC))
        }
    }

    @Test
    fun `generic keeps the Xbox layout on the buttons the launcher prompts with`() {
        listOf(
            ControllerIcon.FACE_SOUTH, ControllerIcon.FACE_EAST,
            ControllerIcon.FACE_WEST, ControllerIcon.FACE_NORTH,
            ControllerIcon.BUMPER_LEFT, ControllerIcon.BUMPER_RIGHT,
            ControllerIcon.TRIGGER_LEFT, ControllerIcon.TRIGGER_RIGHT,
            ControllerIcon.START, ControllerIcon.SELECT,
        ).forEach { icon ->
            assertEquals(
                "$icon differs from Xbox",
                icon.printedLabelFor(ControllerDisplayType.XBOX),
                icon.printedLabelFor(ControllerDisplayType.GENERIC),
            )
        }
    }
}
