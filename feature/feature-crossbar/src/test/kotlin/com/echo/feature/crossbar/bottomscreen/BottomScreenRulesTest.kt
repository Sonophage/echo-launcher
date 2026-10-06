package com.echo.feature.crossbar.bottomscreen

import com.echo.feature.crossbar.viewmodel.CrossbarItem
import com.echo.feature.crossbar.viewmodel.CrossbarUiState
import com.echo.feature.crossbar.viewmodel.CrossbarViewModel
import com.echo.feature.crossbar.viewmodel.SearchScope
import com.echo.feature.crossbar.viewmodel.SearchState
import com.echo.feature.crossbar.viewmodel.GameInfoState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// owner, 2026-10-06: the bottom screen follows the cursor, and while a game ECHO launched runs it shows
// that game instead
class BottomScreenRulesTest {
    private val skyrim = GameInfoState(CrossbarItem(id = "1", title = "Skyrim", gameId = 1L))
    private val castlevania = GameInfoState(CrossbarItem(id = "2", title = "Castlevania", gameId = 2L))

    @Test
    fun `the running game wins over the cursor, and the cursor comes back when it ends`() {
        assertEquals(castlevania, BottomScreenState(focused = skyrim, playing = castlevania).shownInfo())
        assertEquals(skyrim, BottomScreenState(focused = skyrim, playing = null).shownInfo())
        assertNull("nothing to show means the Recent shelf", BottomScreenState().shownInfo())
    }

    @Test
    fun `a game is playing only while ECHO is behind it`() {
        assertEquals(2L, playingGameId(hostShown = false, lastLaunchGameId = 2L))
        assertNull("back in ECHO, the cursor leads again", playingGameId(hostShown = true, lastLaunchGameId = 2L))
        assertNull("ECHO hidden by something it did not launch", playingGameId(hostShown = false, lastLaunchGameId = null))
    }

    @Test
    fun `only games and installed apps have info, not settings rows or folders`() {
        assertTrue(hasInfo(CrossbarItem(id = "1", title = "Skyrim", gameId = 1L)))
        assertTrue(hasInfo(CrossbarItem(id = "a", title = "Pocket Casts", packageName = "au.com.shiftyjelly.pocketcasts", isAndroidApp = true)))
        assertFalse(hasInfo(CrossbarItem(id = "s", title = "Display")))
    }

    // owner, 2026-10-06: with a second screen, the App Drawer, Search and Settings are locked to it,
    // whatever opened them; the first-run wizard stays on the top screen
    @Test
    fun `with a second screen the drawer, search and settings leave the top screen, the wizard stays`() {
        val search = SearchState(scope = SearchScope.ALL)
        val open = CrossbarUiState(search = search, activeAppDrawerFilter = "APPS", activeSettingsScreen = "settings_display")
        assertEquals(search, open.topSearch)
        assertEquals("APPS", open.topDrawerFilter)

        val dual = open.copy(secondScreen = true)
        assertNull(dual.topSearch)
        assertNull(dual.topDrawerFilter)
        assertNull(dual.topSettingsScreen)

        val wizard = CrossbarViewModel.WIZARD_SCREEN_IDS.first()
        assertEquals(wizard, dual.copy(activeSettingsScreen = wizard).topSettingsScreen)
    }

    @Test
    fun `the bottom screen shows the locked screen on top, as the top screen stacks them`() {
        assertEquals(LockedScreen.NONE, lockedScreen(CrossbarUiState()))
        assertEquals(LockedScreen.SEARCH, lockedScreen(CrossbarUiState(search = SearchState(scope = SearchScope.ALL))))
        assertEquals(LockedScreen.APPS, lockedScreen(CrossbarUiState(activeAppDrawerFilter = "APPS")))
        assertEquals(LockedScreen.SETTINGS, lockedScreen(CrossbarUiState(activeSettingsScreen = "settings_display")))
        assertEquals("the drawer over Settings", LockedScreen.APPS, lockedScreen(CrossbarUiState(activeAppDrawerFilter = "APPS", activeSettingsScreen = "settings_display")))
        assertEquals("the drawer's own search over the drawer", LockedScreen.SEARCH, lockedScreen(CrossbarUiState(activeAppDrawerFilter = "APPS", search = SearchState(scope = SearchScope.APPS))))
        assertEquals("the wizard is not drawn here", LockedScreen.NONE, lockedScreen(CrossbarUiState(activeSettingsScreen = CrossbarViewModel.WIZARD_SCREEN_IDS.first())))
    }

    // owner, 2026-10-06: with a second screen, Last Played is its Recent page and leaves the XMB
    @Test
    fun `Last Played cannot be reached on the XMB while a second screen shows it`() {
        val lastPlayed = com.echo.core.domain.model.Category(
            id = com.echo.core.domain.model.BuiltInCategory.RECENTLY_PLAYED, name = "Last Played", iconKey = "",
            type = com.echo.core.domain.model.CategoryType.BUILT_IN, position = 0, isVisible = true,
        )
        assertTrue(CrossbarUiState().categoryReachable(lastPlayed))
        assertFalse(CrossbarUiState(secondScreen = true).categoryReachable(lastPlayed))
    }

    // the page button lit is the page drawn, so the controller moves what is on screen
    @Test
    fun `Info stands down for Recent while there is nothing to show`() {
        assertEquals(BottomPage.RECENT, BottomScreenState(page = BottomPage.INFO).shownPage())
        assertEquals(BottomPage.INFO, BottomScreenState(page = BottomPage.INFO, focused = skyrim).shownPage())
        assertEquals(BottomPage.RECENT, BottomScreenState(page = BottomPage.RECENT, focused = skyrim).shownPage())
    }
}
