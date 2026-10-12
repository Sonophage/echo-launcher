package com.echo.feature.crossbar.bottomscreen

import com.echo.feature.crossbar.bottomscreen.CompanionContent.Details
import com.echo.feature.crossbar.viewmodel.CrossbarItem
import com.echo.feature.crossbar.viewmodel.CrossbarItemType
import com.echo.feature.crossbar.viewmodel.CrossbarUiState
import com.echo.feature.crossbar.viewmodel.GameInfoState
import com.echo.feature.crossbar.viewmodel.SearchScope
import com.echo.feature.crossbar.viewmodel.SearchState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Seam A (spec #1, ADR 0001): what the Companion Screen shows follows from the Main Screen's state alone.
// It shows the running game, else the item an open menu belongs to, else the hovered item, else what it
// showed last, so it never goes blank.
class CompanionRuleTest {
    private val skyrim = GameInfoState(CrossbarItem(id = "1", title = "Skyrim", gameId = 1L))
    private val castlevania = GameInfoState(CrossbarItem(id = "2", title = "Castlevania", gameId = 2L))
    private val pocketCasts = GameInfoState(CrossbarItem(id = "a", title = "Pocket Casts", packageName = "au.com.shiftyjelly.pocketcasts", isAndroidApp = true))

    @Test
    fun `the hovered item's Details are shown`() {
        assertEquals(Details(skyrim), companionContent(BottomScreenState(hovered = skyrim), last = null))
    }

    @Test
    fun `the App Drawer's hovered app wins over the Crossbar's cursor under the drawer`() {
        val main = BottomScreenState(hovered = skyrim, drawer = pocketCasts)
        assertEquals(Details(pocketCasts), companionContent(main, last = null))
    }

    @Test
    fun `while searching, the hovered result wins over the drawer the search was opened from`() {
        val main = BottomScreenState(hovered = castlevania, drawer = pocketCasts, searching = true)
        assertEquals(Details(castlevania), companionContent(main, last = null))
    }

    @Test
    fun `nothing new hovered keeps the last thing shown`() {
        assertEquals(Details(skyrim), companionContent(BottomScreenState(), last = Details(skyrim)))
    }

    @Test
    fun `an open menu keeps the item it belongs to, though the cursor moves under it`() {
        val main = BottomScreenState(hovered = castlevania, menuOpen = true)
        assertEquals(Details(skyrim), companionContent(main, last = Details(skyrim)))
        assertEquals("the menu closed: the cursor again", Details(castlevania), companionContent(main.copy(menuOpen = false), last = Details(skyrim)))
    }

    @Test
    fun `a menu opened before anything was shown shows the item it was opened on`() {
        assertEquals(Details(skyrim), companionContent(BottomScreenState(hovered = skyrim, menuOpen = true), last = null))
    }

    @Test
    fun `the running game wins over everything, with its session clock`() {
        val main = BottomScreenState(hovered = skyrim, drawer = pocketCasts, playing = castlevania, playingSince = 1_000L)
        assertEquals(Details(castlevania, playing = true, playingSince = 1_000L), companionContent(main, last = Details(skyrim)))
    }

    @Test
    fun `when the game ends, the hovered item is shown, or the game kept without its clock or Resume`() {
        val played = Details(castlevania, playing = true, playingSince = 1_000L)
        assertEquals(Details(skyrim), companionContent(BottomScreenState(hovered = skyrim), last = played))
        assertEquals(Details(castlevania), companionContent(BottomScreenState(), last = played))
    }

    // what the Main Screen has hovered: Search's focused result while Search is open, else the Crossbar's cursor
    @Test
    fun `the hovered item is Search's focused result while Search is open`() {
        val game = CrossbarItem(id = "1", title = "Skyrim", gameId = 1L)
        val result = CrossbarItem(id = "2", title = "Castlevania", gameId = 2L)
        val crossbar = CrossbarUiState(currentItems = listOf(game))
        assertEquals(game, mainScreenHover(crossbar))
        assertEquals(result, mainScreenHover(crossbar.copy(search = SearchState(scope = SearchScope.ALL, rows = listOf(game, result), selectedIndex = 1))))
        assertNull("no results: nothing hovered", mainScreenHover(crossbar.copy(search = SearchState(scope = SearchScope.ALL))))
    }

    // ticket #7: games, apps, songs, videos, books, photos and systems have Details here; a settings row or a
    // folder does not, so hovering one keeps the last thing shown
    @Test
    fun `games, apps, media and systems have Details on the Companion Screen, settings rows and folders not`() {
        assertTrue(companionShows(skyrim.item))
        assertTrue(companionShows(pocketCasts.item))
        listOf(CrossbarItemType.MUSIC_TRACK, CrossbarItemType.MUSIC_GROUP, CrossbarItemType.VIDEO_FILE, CrossbarItemType.LIBRARY_BOOK, CrossbarItemType.PHOTO_FILE)
            .forEach { assertTrue(it.name, companionShows(CrossbarItem(id = "m", title = "m", type = it))) }
        assertTrue("a system's card", companionShows(CrossbarItem(id = "card_nds", title = "Nintendo DS", platformId = "nds", type = CrossbarItemType.MEMORY_CARD)))
        assertFalse(companionShows(CrossbarItem(id = "s", title = "Display")))
        assertFalse(companionShows(CrossbarItem(id = "f", title = "Videos", type = CrossbarItemType.VIDEO_FOLDER)))
    }
}
