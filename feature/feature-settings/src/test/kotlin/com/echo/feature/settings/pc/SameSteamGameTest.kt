package com.echo.feature.settings.pc

import com.echo.core.domain.model.Game
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// a rescan of DroidDeck's files must find the game it already added, though DroidDeck names the file
// "<Name> (<id>)" and shortens long names: on the Konker a title match added every game a second time
class SameSteamGameTest {
    private fun game(id: Long, pkg: String, steam: String?) =
        Game(id = id, title = "Nine Sols", platformId = "windows", packageName = pkg, storefrontGameId = steam)

    @Test
    fun `the same launcher and Steam id is the same game`() {
        val games = listOf(game(1, "app.gamenative", "1809540"), game(2, "com.droiddeck.launcher", "1809540"))
        assertEquals(2L, sameSteamGame(games, "com.droiddeck.launcher", "1809540")?.id)
    }

    @Test
    fun `another launcher or no id is not a match`() {
        val games = listOf(game(1, "app.gamenative", "1809540"))
        assertNull(sameSteamGame(games, "com.droiddeck.launcher", "1809540"))
        assertNull(sameSteamGame(games, "app.gamenative", null))
    }
}
