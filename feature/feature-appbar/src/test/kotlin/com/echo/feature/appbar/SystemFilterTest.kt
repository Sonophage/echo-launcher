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

    // owner, 2026-10-08: the Games section follows the crossbar's genre filter, and its footer names it
    @Test
    fun `the Games section narrows to the genre and says so`() {
        val rpg = rom(4, "psp", "PSP").copy(genre = com.echo.core.domain.model.GameGenre.RPG)
        val list = games + rpg
        assertEquals(listOf("rom:4"), list.ofGenre(com.echo.core.domain.model.GameGenre.RPG).map { it.packageName })
        assertEquals(list, list.ofGenre(null))
        assertEquals("Games · RPG", sectionLabel(AppFilter.GAMES, com.echo.core.domain.model.GameGenre.RPG))
        assertEquals("Apps is not a games list", "Apps", sectionLabel(AppFilter.APPS, com.echo.core.domain.model.GameGenre.RPG))
    }

    @Test
    fun `grouped by genre, the Games buttons are genres and each keeps its own games`() {
        val rpg = rom(4, "psp", "PSP").copy(genre = com.echo.core.domain.model.GameGenre.RPG)
        val action = rom(5, "snes", "SNES").copy(genre = com.echo.core.domain.model.GameGenre.ACTION)
        val list = listOf(rpg, action, rom(6, "gba", "GBA"))
        assertEquals(listOf("All" to 3, "RPG" to 1, "Action" to 1), genreChips(list).map { it.label to it.count })
        assertEquals(listOf("rom:4"), list.ofChip("RPG", byGenre = true).map { it.packageName })
        assertEquals("systems as before", listOf("rom:5"), list.ofChip("snes", byGenre = false).map { it.packageName })
    }
}
