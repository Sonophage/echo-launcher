package com.echo.feature.crossbar.bottomscreen

import com.echo.core.domain.model.GamepadAction
import com.echo.feature.crossbar.viewmodel.CrossbarItem
import com.echo.feature.crossbar.viewmodel.CrossbarUiState
import com.echo.feature.crossbar.viewmodel.CrossbarViewModel
import com.echo.feature.crossbar.viewmodel.SearchScope
import com.echo.feature.crossbar.viewmodel.SearchState
import com.echo.feature.crossbar.viewmodel.GameInfoState
import com.echo.feature.crossbar.viewmodel.RecentFilter as F
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

    // ADR 0001: the App Drawer is on the Main Screen, and the Companion Screen shows the app hovered in it
    @Test
    fun `the drawer's hovered app wins over the cursor, and the running game over both`() {
        val pocketCasts = GameInfoState(CrossbarItem(id = "a", title = "Pocket Casts", packageName = "au.com.shiftyjelly.pocketcasts", isAndroidApp = true))
        assertEquals(pocketCasts, BottomScreenState(focused = skyrim, drawer = pocketCasts).shownInfo())
        assertEquals(castlevania, BottomScreenState(focused = skyrim, drawer = pocketCasts, playing = castlevania).shownInfo())
        assertEquals("the drawer closed: the cursor again", skyrim, BottomScreenState(focused = skyrim, drawer = null).shownInfo())
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

    // owner, 2026-10-06: with a second screen, Search is locked to it, whatever opened it
    @Test
    fun `with a second screen search leaves the top screen`() {
        val search = SearchState(scope = SearchScope.ALL)
        val open = CrossbarUiState(search = search)
        assertEquals(search, open.topSearch)
        assertNull(open.copy(secondScreen = true).topSearch)
    }

    @Test
    fun `the bottom screen shows the locked screen on top, as the top screen stacks them`() {
        assertEquals(LockedScreen.NONE, lockedScreen(CrossbarUiState()))
        assertEquals(LockedScreen.SEARCH, lockedScreen(CrossbarUiState(search = SearchState(scope = SearchScope.ALL))))
        assertEquals("the drawer is on the Main Screen", LockedScreen.NONE, lockedScreen(CrossbarUiState(activeAppDrawerFilter = "APPS")))
        assertEquals("Settings is on the Main Screen", LockedScreen.NONE, lockedScreen(CrossbarUiState(activeSettingsScreen = "settings_display")))
        assertEquals("the drawer's own search over the drawer", LockedScreen.SEARCH, lockedScreen(CrossbarUiState(activeAppDrawerFilter = "APPS", search = SearchState(scope = SearchScope.APPS))))
    }

    // the page button lit is the page drawn, so the controller moves what is on screen
    @Test
    fun `Info stands down for Recent while there is nothing to show`() {
        assertEquals(BottomPage.RECENT, BottomScreenState(page = BottomPage.INFO).shownPage())
        assertEquals(BottomPage.INFO, BottomScreenState(page = BottomPage.INFO, focused = skyrim).shownPage())
        assertEquals(BottomPage.RECENT, BottomScreenState(page = BottomPage.RECENT, focused = skyrim).shownPage())
    }

    // owner, 2026-10-06: dual or single screen is a quick setting, offered only where there is a second display
    @Test
    fun `Screens is a quick setting only on a device with a second display`() {
        val dual = com.echo.feature.crossbar.viewmodel.quickSettingsFor(secondDisplay = true)
        assertTrue(com.echo.feature.crossbar.viewmodel.QuickSetting.SECOND_SCREEN in dual)
        assertEquals("Android settings stays last", com.echo.feature.crossbar.viewmodel.QuickSetting.ANDROID_SETTINGS, dual.last())
        assertFalse(com.echo.feature.crossbar.viewmodel.QuickSetting.SECOND_SCREEN in com.echo.feature.crossbar.viewmodel.quickSettingsFor(secondDisplay = false))
    }

    // Info only with info; left and right walk what is there. The music player is on the Main Screen (ADR 0001)
    @Test
    fun `the pages are what can be shown, and stepping walks only those`() {
        val shelf = BottomScreenState()
        assertEquals(listOf(BottomPage.RECENT), shelf.pages())
        assertEquals("Info with nothing to show falls back to Recent", BottomPage.RECENT, shelf.copy(page = BottomPage.INFO).shownPage())

        val focused = BottomScreenState(focused = skyrim, page = BottomPage.RECENT)
        assertEquals(listOf(BottomPage.INFO, BottomPage.RECENT), focused.pages())
        assertEquals("round the end", BottomPage.INFO, focused.steppedPage(1))
        assertEquals(BottomPage.INFO, focused.steppedPage(-1))
    }

    // owner, 2026-10-08: the companion fits the crossbar's column
    @Test
    fun `Recent narrows to the column's kind, and to All when that kind holds nothing`() {
        val stocked = listOf(F.ALL, F.GAMES, F.MUSIC, F.APPS)
        assertEquals(F.MUSIC, recentFilterFor("music", gaming = false, shown = stocked))
        assertEquals(F.GAMES, recentFilterFor("games", gaming = true, shown = stocked))
        assertEquals("a gaming column of your own", F.GAMES, recentFilterFor("my_rpgs", gaming = true, shown = stocked))
        assertEquals(F.APPS, recentFilterFor("android", gaming = false, shown = stocked))
        assertEquals("no recent video: All", F.ALL, recentFilterFor("videos", gaming = false, shown = stocked))
        assertEquals("Photos has no kind of its own", F.ALL, recentFilterFor("photos", gaming = false, shown = stocked))
    }

    // the item in full view must be reachable on the companion, swapped or not (owner, 2026-10-08), and
    // left and right must still reach the other pages once the list is where it goes
    @Test
    fun `left and right close and open Recent's list before changing page`() {
        val open = BottomScreenState(focused = skyrim, page = BottomPage.RECENT)
        val full = open.sideStep(GamepadAction.NAVIGATE_RIGHT)
        assertFalse("right closes the list", full.recentListOpen)
        assertEquals(BottomPage.RECENT, full.page)
        assertEquals("right again goes on to Info", BottomPage.INFO, full.sideStep(GamepadAction.NAVIGATE_RIGHT).page)
        val back = full.sideStep(GamepadAction.NAVIGATE_LEFT)
        assertTrue("left opens it again", back.recentListOpen)
        assertEquals(BottomPage.RECENT, back.page)
        assertEquals("left with the list open leaves Recent", BottomPage.INFO, back.sideStep(GamepadAction.NAVIGATE_LEFT).page)
    }

    @Test
    fun `the second screen is put back only when it is wanted and not already showing`() {
        val link = BottomScreenLink()
        assertTrue("wanted, hidden: show it", link.needsCompanion(enabled = true))
        assertFalse("turned off in Quick settings", link.needsCompanion(enabled = false))
        link.attach(true)
        assertFalse("already showing: leave focus alone", link.needsCompanion(enabled = true))
    }

    // HOME starts one Companion window and ECHO may start another over it; the covered one stops after the
    // new one starts, and that must not mark the Companion Screen as gone
    @Test
    fun `a second Companion window stopping does not detach the one on show`() {
        val link = BottomScreenLink()
        link.attach(true)
        link.attach(true)
        link.attach(false)
        assertTrue("one window still on show", link.attached.value)
        link.attach(false)
        assertFalse("both gone", link.attached.value)
    }

    @Test
    fun `a drawer album draws as music on the other screen, an app as an app`() {
        val album = com.echo.feature.appbar.InstalledApp("media:MUSIC:born pink", "BORN PINK", null, isGame = false, isEmulator = false,
            art = "file:///art.jpg", media = com.echo.feature.appbar.DrawerMedia(com.echo.feature.appbar.MediaKind.MUSIC, "born pink", "BLACKPINK", "K-Pop"))
        val item = drawerCaseItem(album)
        assertEquals(com.echo.feature.crossbar.viewmodel.RecentKind.MUSIC, com.echo.feature.crossbar.viewmodel.recentKind(item))
        assertEquals("file:///art.jpg", item.coverUri)
        assertEquals("BLACKPINK  ·  K-Pop", item.subtitle)
        val app = drawerCaseItem(com.echo.feature.appbar.InstalledApp("com.a", "A", null, isGame = false, isEmulator = false))
        assertEquals(com.echo.feature.crossbar.viewmodel.RecentKind.APP, com.echo.feature.crossbar.viewmodel.recentKind(app))
    }
}
