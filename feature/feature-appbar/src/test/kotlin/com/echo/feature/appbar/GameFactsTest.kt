package com.echo.feature.appbar

import com.echo.core.domain.model.Game
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GameFactsTest {
    private val base = Game(id = 1, title = "Sekiro", platformId = "windows", romPath = null)

    @Test
    fun `the drawer's line under a game names its year, genre, developer and players, skipping what is unknown`() {
        assertEquals(
            "2019 · Action · FromSoftware · 1 player",
            gameFacts(base.copy(releaseDate = "2019-03-22", genre = "Action", developer = "FromSoftware", players = "1")),
        )
        assertEquals("Action", gameFacts(base.copy(genre = "Action", developer = " ")))
        assertNull("a game with no metadata shows no empty line", gameFacts(base))
    }
}
