package com.echo.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// owner, 2026-10-08: the scrapers' genre text reads as one of a handful of genres. The table is every value on the
// owner's Konker library (2026-10-08), not examples written for the rules
class GameGenreTest {
    private val library = mapOf(
        "Role Playing Game" to GameGenre.RPG, "Adventure" to GameGenre.ADVENTURE, "Platform" to GameGenre.PLATFORMER,
        "Beat'em Up" to GameGenre.ACTION, "Action / Adventure" to GameGenre.ACTION, "Shooter / FPV" to GameGenre.SHOOTER,
        "Action" to GameGenre.ACTION, "Tactical RPG" to GameGenre.RPG, "Racing, Driving" to GameGenre.RACING,
        "Puzzle" to GameGenre.PUZZLE, "Playing cards" to GameGenre.OTHER, "Platform / Shooter Scrolling" to GameGenre.PLATFORMER,
        "Shooter / Run and Gun" to GameGenre.SHOOTER, "Shooter" to GameGenre.SHOOTER, "RPG" to GameGenre.RPG,
        "Japanese RPG" to GameGenre.RPG, "Compilation" to GameGenre.OTHER, "Adventure / Survival Horror" to GameGenre.ADVENTURE,
        "Action RPG" to GameGenre.RPG, "Strategy" to GameGenre.STRATEGY, "Simulation" to GameGenre.SIMULATION,
        "Racing" to GameGenre.RACING, "Platform / Run & Jump" to GameGenre.PLATFORMER,
        "Platform / Fighter Scrolling" to GameGenre.PLATFORMER, "Lightgun Shooter" to GameGenre.SHOOTER,
        "Fighting / 3D" to GameGenre.FIGHTING, "Fighting" to GameGenre.FIGHTING, "Dungeon Crawler RPG" to GameGenre.RPG,
        "Casual Game" to GameGenre.OTHER, "Casual" to GameGenre.OTHER, "Board game" to GameGenre.OTHER,
        "Adventure / Point and Click" to GameGenre.ADVENTURE, "Adventure / Interactive Movie" to GameGenre.ADVENTURE,
        "Adventure / Graphic" to GameGenre.ADVENTURE, "Action / Labyrinth" to GameGenre.ACTION,
    )

    @Test
    fun `every genre on the owner's library reads as the genre a player would file it under`() {
        val wrong = library.filter { (raw, want) -> genreOf(raw) != want }.map { (raw, want) -> "$raw: ${genreOf(raw)} not $want" }
        assertEquals(emptyList<String>(), wrong)
    }

    @Test
    fun `no text is no genre, and an edit wins over the scraped text`() {
        assertNull(genreOf(null))
        assertNull(genreOf("  "))
        assertEquals(GameGenre.PUZZLE, effectiveGenre("Role Playing Game", override = "PUZZLE"))
        assertEquals("an edit naming no genre falls back", GameGenre.RPG, effectiveGenre("Role Playing Game", override = "SPARKLE"))
    }
}
