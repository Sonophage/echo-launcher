package com.echo.feature.appbar

import org.junit.Assert.assertEquals
import org.junit.Test

class SystemFilterTest {
    private fun rom(id: Long, platform: String, name: String) =
        InstalledApp("rom:$id", "Game $id", null, isGame = true, isEmulator = false, gameId = id, platformId = platform, platformName = name)

    private fun androidGame(pkg: String) = InstalledApp(pkg, pkg, null, isGame = true, isEmulator = false)

    private val games = listOf(
        rom(1, "psp", "PSP"),
        rom(2, "snes", "SNES"),
        rom(3, "snes", "SNES"),
        androidGame("com.a"),
        androidGame("com.b"),
        androidGame("com.c"),
    )

    @Test
    fun `chips lead with All, then each system by game count, and installed Android games share one Android chip`() {
        val chips = systemChips(games)

        assertEquals(listOf("All" to 6, "Android" to 3, "SNES" to 2, "PSP" to 1), chips.map { it.label to it.count })
    }

    @Test
    fun `a system chip keeps only that system's games, and All keeps every game`() {
        assertEquals(listOf("rom:2", "rom:3"), games.ofSystem("snes").map { it.packageName })
        assertEquals(listOf("com.a", "com.b", "com.c"), games.ofSystem(systemChips(games)[1].id).map { it.packageName })
        assertEquals(games, games.ofSystem(null))
    }
}
