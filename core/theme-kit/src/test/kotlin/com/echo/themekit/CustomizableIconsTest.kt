package com.echo.themekit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class CustomizableIconsTest {
    @Test
    fun `all keys are unique`() {
        val keys = CustomizableIcons.ALL.map { it.key }
        assertEquals(keys.size, keys.toSet().size, "duplicate slot key in CustomizableIcons.ALL")
    }

    @Test
    fun `contains every IconSlots slot in order`() {
        val themeSlots = CustomizableIcons.ALL.take(IconSlots.ALL.size)
        assertEquals(IconSlots.ALL, themeSlots, "IconSlots.ALL must be a prefix of CustomizableIcons.ALL, unchanged")
    }

    @Test
    fun `console slots come after the theme slots`() {
        val consoleSlots = CustomizableIcons.ALL.drop(IconSlots.ALL.size).filter { it.group == IconSlot.Group.CONSOLE }
        assertTrue(consoleSlots.isNotEmpty(), "no console slots registered")
        assertTrue(consoleSlots.all { it.key.startsWith("sysicon_") })
        assertTrue(consoleSlots.all { it.templateSizePx == 256 })
    }

    // owner, 2026-10-07: every console that has an icon slot also has an Art slot, so any system row can
    // wear its own image or GIF instead of its covers
    @Test
    fun `every console has an art slot after the theme slots`() {
        val extra = CustomizableIcons.ALL.drop(IconSlots.ALL.size)
        val art = extra.filter { it.group == IconSlot.Group.ART }
        assertEquals(SYSICON_PLATFORM_IDS.map { "sysart_$it" }, art.map { it.key })
        assertTrue(extra.all { it.group == IconSlot.Group.CONSOLE || it.group == IconSlot.Group.ART })
    }

    @Test
    fun `every console slot key derives from the platform id list`() {
        val consoleSlots = CustomizableIcons.ALL.drop(IconSlots.ALL.size).filter { it.group == IconSlot.Group.CONSOLE }
        assertEquals(SYSICON_PLATFORM_IDS, consoleSlots.map { it.key.removePrefix("sysicon_") })
    }

    @Test
    fun `isValidKey accepts a theme slot and a console slot`() {
        assertTrue(CustomizableIcons.isValidKey("catbar_games"))
        assertTrue(CustomizableIcons.isValidKey("status_battery_full"))
        assertTrue(CustomizableIcons.isValidKey("sysicon_snes"))
    }

    @Test
    fun `isValidKey rejects traversal, separators, empty and unknown keys`() {
        assertFalse(CustomizableIcons.isValidKey(""))
        assertFalse(CustomizableIcons.isValidKey(".."))
        assertFalse(CustomizableIcons.isValidKey("../evil"))
        assertFalse(CustomizableIcons.isValidKey("catbar_games/../../x"))
        assertFalse(CustomizableIcons.isValidKey("sysicon_snes/"))
        assertFalse(CustomizableIcons.isValidKey("/etc/passwd"))
        assertFalse(CustomizableIcons.isValidKey("not_a_slot"))

        assertFalse(CustomizableIcons.isValidKey("sysicon_default"))
    }

    @Test
    fun `console display names are non-blank and differ from the raw id`() {
        for (id in SYSICON_PLATFORM_IDS) {
            val name = consoleDisplayName(id)
            assertTrue(name.isNotBlank(), "blank display name for $id")
            assertNotEquals(id, name, "display name for $id should be humanized, not the raw id")
        }
    }
}
