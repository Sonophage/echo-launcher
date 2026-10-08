package com.echo.feature.settings.viewmodel

import com.echo.themekit.ThemePart
import org.junit.Assert.assertEquals
import org.junit.Test

// owner, 2026-10-07: a theme's page is where parts are picked. Ticking must only ever touch parts the theme
// has, A must say what it will do, and the look in use has nothing to tick: what it applies is what changes
// on the owner's device.
class ThemePagePartsTest {
    private val parts = setOf(ThemePart.entries[0], ThemePart.entries[1], ThemePart.entries[2])
    private val saved = ThemePage(name = "Sumi", parts = parts, savedId = "sumi")

    @Test
    fun `a page takes every part the theme has until one is left out`() {
        assertEquals(parts, saved.partsToTake)
        assertEquals("Apply", saved.actionLabel)
        val one = saved.toggled(ThemePart.entries[1])
        assertEquals(parts - ThemePart.entries[1], one.partsToTake)
        assertEquals("Apply 2 parts", one.actionLabel)
    }

    @Test
    fun `a part the theme lacks cannot be ticked`() {
        val lacking = ThemePart.entries.first { it !in parts }
        assertEquals(saved, saved.toggled(lacking))
    }

    @Test
    fun `leaving out every part leaves nothing to apply`() {
        val none = parts.fold(saved) { page, part -> page.toggled(part) }
        assertEquals(emptySet<ThemePart>(), none.partsToTake)
        assertEquals("Pick a part", none.actionLabel)
    }

    @Test
    fun `the look in use saves, and ticks nothing`() {
        val current = ThemePage(name = "Mixed", parts = ThemePart.entries.toSet(), current = true)
        assertEquals("Save as Theme", current.actionLabel)
        assertEquals(current, current.toggled(ThemePart.entries[0]))
    }

    @Test
    fun `an online theme downloads first, an update says so`() {
        val online = ThemePage(name = "Lagoon", parts = parts)
        assertEquals("Download and apply", online.actionLabel)
        assertEquals("Update and apply", saved.copy(update = true).actionLabel)
    }
}
