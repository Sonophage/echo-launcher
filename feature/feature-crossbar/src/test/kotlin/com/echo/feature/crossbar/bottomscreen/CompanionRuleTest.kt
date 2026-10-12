package com.echo.feature.crossbar.bottomscreen

import com.echo.feature.crossbar.bottomscreen.CompanionContent.Details
import com.echo.feature.crossbar.bottomscreen.CompanionContent.Setting
import com.echo.core.domain.model.CrossbarColorScheme
import com.echo.feature.crossbar.viewmodel.ColorSchemeOption
import com.echo.feature.settings.ui.SettingsFocusInfo
import com.echo.feature.settings.ui.StorePreview
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

    // ticket #8: while Settings is open on the Main Screen, the Companion Screen shows the focused setting's
    // description in large type; the Main Screen keeps its own
    private val wave = SettingsFocusInfo("Wave Style", "PSP", "The animated wave behind the Crossbar")

    @Test
    fun `in Settings the focused setting's description is shown, not the item hovered before`() {
        val main = BottomScreenState(hovered = skyrim, settingsOpen = true, setting = wave)
        assertEquals(Setting(wave), companionContent(main, last = Details(skyrim)))
    }

    @Test
    fun `in Settings with no setting focused, the last thing shown is kept`() {
        assertEquals(Setting(wave), companionContent(BottomScreenState(hovered = skyrim, settingsOpen = true), last = Setting(wave)))
        assertEquals("a touch hides the cursor: the game is kept", Details(skyrim), companionContent(BottomScreenState(settingsOpen = true), last = Details(skyrim)))
    }

    // a theme focused in the Theme Store: its name, its line, and a preview of its wallpaper and accent
    private val neon = StorePreview("Neon Night", "by Echo  ·  version 2", "https://example.com/neon.png", 0xFF7FD8D8L)

    @Test
    fun `in the Theme Store the focused theme is previewed`() {
        val main = BottomScreenState(settingsOpen = true, setting = wave, theme = neon)
        assertEquals(
            Setting(SettingsFocusInfo("Neon Night", null, "by Echo  ·  version 2"), SettingPreview.Theme(neon)),
            companionContent(main, last = null),
        )
    }

    // the colour scheme highlighted in the picker, which Settings opens over itself
    private val sunset = ColorSchemeOption(CrossbarColorScheme.SUNSET_ORANGE, "Sunset Orange", null, 0xFFFF8800L)

    @Test
    fun `in the colour scheme picker the highlighted scheme is previewed`() {
        val main = BottomScreenState(settingsOpen = true, setting = wave, colorScheme = sunset)
        assertEquals(
            Setting(SettingsFocusInfo("Sunset Orange", null, null), SettingPreview.Colors(sunset)),
            companionContent(main, last = Setting(wave)),
        )
    }

    @Test
    fun `Settings closed, the hovered item is shown again`() {
        assertEquals(Details(skyrim), companionContent(BottomScreenState(hovered = skyrim, setting = wave), last = Setting(wave)))
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

    // Swap (L3, R3 or the Swap button). With the Companion Screen on a second display, the two screens change
    // displays and ECHO remembers it across restarts; with one display, the Companion Screen is shown in place of
    // the Main Screen and only until the next Swap, so ECHO always starts on the Main Screen
    @Test
    fun `with two displays Swap exchanges the displays and is remembered`() {
        val dual = CrossbarUiState(secondDisplayPresent = true, secondScreenEnabled = true, secondScreen = true)
        assertEquals(Swap.Displays(swapped = true), swap(dual))
        assertEquals(Swap.Displays(swapped = false), swap(dual.copy(screensSwapped = true)))
        assertTrue("after a restart the first display shows the Companion Screen again", firstDisplayShowsCompanion(dual.copy(screensSwapped = true)))
    }

    @Test
    fun `with one display Swap shows the Companion Screen in place, in memory only`() {
        val single = CrossbarUiState(secondDisplayPresent = false)
        assertEquals(Swap.InPlace(shown = true), swap(single))
        assertEquals("Swap again: back to the Main Screen", Swap.InPlace(shown = false), swap(single.copy(companionInPlace = true)))
        assertTrue(firstDisplayShowsCompanion(single.copy(companionInPlace = true)))
        assertFalse("a Swap remembered from two displays never starts ECHO on the Companion Screen", firstDisplayShowsCompanion(single.copy(screensSwapped = true)))
        assertEquals("one screen chosen in Quick settings counts as one display", Swap.InPlace(shown = true), swap(CrossbarUiState(secondDisplayPresent = true, secondScreenEnabled = false)))
    }
}
