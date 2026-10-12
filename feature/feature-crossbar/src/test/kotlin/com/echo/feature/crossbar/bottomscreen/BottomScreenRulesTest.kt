package com.echo.feature.crossbar.bottomscreen

import com.echo.core.domain.model.GamepadAction
import com.echo.feature.crossbar.viewmodel.CrossbarItem
import com.echo.feature.crossbar.viewmodel.CrossbarUiState
import com.echo.feature.crossbar.viewmodel.CrossbarViewModel
import com.echo.feature.crossbar.viewmodel.SearchScope
import com.echo.feature.crossbar.viewmodel.SearchState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// the Companion Screen's rules besides what it shows, which CompanionRuleTest covers
class BottomScreenRulesTest {
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

    // owner, 2026-10-06: with a second screen, Search and Settings are locked to it, whatever opened
    // them; the first-run wizard stays on the top screen
    @Test
    fun `with a second screen search and settings leave the top screen, the wizard stays`() {
        val search = SearchState(scope = SearchScope.ALL)
        val open = CrossbarUiState(search = search, activeSettingsScreen = "settings_display")
        assertEquals(search, open.topSearch)

        val dual = open.copy(secondScreen = true)
        assertNull(dual.topSearch)
        assertNull(dual.topSettingsScreen)

        val wizard = CrossbarViewModel.WIZARD_SCREEN_IDS.first()
        assertEquals(wizard, dual.copy(activeSettingsScreen = wizard).topSettingsScreen)
    }

    @Test
    fun `the bottom screen shows the locked screen on top, as the top screen stacks them`() {
        assertEquals(LockedScreen.NONE, lockedScreen(CrossbarUiState()))
        assertEquals(LockedScreen.SEARCH, lockedScreen(CrossbarUiState(search = SearchState(scope = SearchScope.ALL))))
        assertEquals("the drawer is on the Main Screen", LockedScreen.NONE, lockedScreen(CrossbarUiState(activeAppDrawerFilter = "APPS")))
        assertEquals(LockedScreen.SETTINGS, lockedScreen(CrossbarUiState(activeSettingsScreen = "settings_display")))
        assertEquals("the drawer over Settings: its hovered app, not Settings", LockedScreen.NONE, lockedScreen(CrossbarUiState(activeAppDrawerFilter = "APPS", activeSettingsScreen = "settings_display")))
        assertEquals("the drawer's own search over the drawer", LockedScreen.SEARCH, lockedScreen(CrossbarUiState(activeAppDrawerFilter = "APPS", search = SearchState(scope = SearchScope.APPS))))
        assertEquals("the wizard is not drawn here", LockedScreen.NONE, lockedScreen(CrossbarUiState(activeSettingsScreen = CrossbarViewModel.WIZARD_SCREEN_IDS.first())))
    }

    // owner, 2026-10-06: dual or single screen is a quick setting, offered only where there is a second display
    @Test
    fun `Screens is a quick setting only on a device with a second display`() {
        val dual = com.echo.feature.crossbar.viewmodel.quickSettingsFor(secondDisplay = true)
        assertTrue(com.echo.feature.crossbar.viewmodel.QuickSetting.SECOND_SCREEN in dual)
        assertEquals("Android settings stays last", com.echo.feature.crossbar.viewmodel.QuickSetting.ANDROID_SETTINGS, dual.last())
        assertFalse(com.echo.feature.crossbar.viewmodel.QuickSetting.SECOND_SCREEN in com.echo.feature.crossbar.viewmodel.quickSettingsFor(secondDisplay = false))
    }

    // the remote must have a way out the owner can find (owner, 2026-10-08: "how do I exit the music player"):
    // B leaves for Recent, Y opens Stop & Close; left and right stay free for the pages
    @Test
    fun `the music remote has a way out`() {
        assertEquals(MusicRemoteKey.LEAVE, musicRemoteKey(GamepadAction.BACK))
        assertEquals(MusicRemoteKey.OPTIONS, musicRemoteKey(GamepadAction.OPEN_CONTEXT_MENU))
        assertEquals(MusicRemoteKey.PLAY_PAUSE, musicRemoteKey(GamepadAction.SELECT))
        assertNull("left walks the pages", musicRemoteKey(GamepadAction.NAVIGATE_LEFT))
        assertNull("right walks the pages", musicRemoteKey(GamepadAction.NAVIGATE_RIGHT))
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
