package com.echo.feature.crossbar.ui

import com.echo.core.ui.design.LAUNCH_HOLD_MS
import com.echo.feature.appbar.InstalledApp
import com.echo.feature.appbar.appdrawer.drawerLaunchAction
import com.echo.feature.crossbar.viewmodel.CrossbarItem
import com.echo.feature.crossbar.viewmodel.holdMsFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// issue #36: the footer's A card rises when a launch hold starts. A footer whose A has no hold never shows the
// card, and its A launches on the press, so the misclick the hold exists to stop comes back on that screen
class LaunchCardTest {
    private val game = CrossbarItem(id = "g", title = "Skyrim", gameId = 1L, isRealGame = true)
    private val app = CrossbarItem(id = "a", title = "Discord", packageName = "com.discord")

    @Test
    fun `the launch card rises on every launch path, Recent, the Crossbar, the App Drawer and Search`() {
        // Recent and the Crossbar: the shell's footer takes its hold from the focused item
        assertEquals("Recent", LAUNCH_HOLD_MS, holdMsFor(game))
        assertEquals("Crossbar", LAUNCH_HOLD_MS, holdMsFor(app))

        val drawerApp = InstalledApp("com.discord", "Discord", icon = null, isGame = false, isEmulator = false)
        val drawer = drawerLaunchAction(drawerApp, holdingPackage = "com.discord")
        assertEquals("App Drawer: A on an app is a hold", LAUNCH_HOLD_MS, drawer.holdMs)
        assertTrue("App Drawer: the card is out while A is held", drawer.holding)

        for (row in listOf(game, app)) {
            val search = searchLaunchAction(row, launchHold = row.id)
            assertEquals("Search: A on ${row.title} is a hold", LAUNCH_HOLD_MS, search.holdMs)
            assertTrue("Search: the card is out while A is held on ${row.title}", search.holding)
        }
    }

    @Test
    fun `media in the App Drawer and Search acts at once, with no card`() {
        val track = CrossbarItem(id = "t", title = "Track", type = com.echo.feature.crossbar.viewmodel.CrossbarItemType.MUSIC_TRACK)
        assertEquals(0L, searchLaunchAction(track, launchHold = null).holdMs)
        val album = InstalledApp(
            "album", "Album", icon = null, isGame = false, isEmulator = false,
            media = com.echo.feature.appbar.DrawerMedia(com.echo.feature.appbar.MediaKind.MUSIC, "1", maker = null, genre = null),
        )
        assertEquals(0L, drawerLaunchAction(album, holdingPackage = null).holdMs)
    }
}
