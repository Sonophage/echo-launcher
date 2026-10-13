package com.echo.feature.appbar.appdrawer

import com.echo.core.ui.design.LAUNCH_HOLD_MS
import com.echo.feature.appbar.DrawerMedia
import com.echo.feature.appbar.InstalledApp
import com.echo.feature.appbar.MediaKind
import org.junit.Assert.assertEquals
import org.junit.Test

// owner, 2026-10-13: in the App Drawer a video and a book open on the hold, as an app does; an album opens on a tap.
// The Crossbar's rule is CrossbarItem.holdsToOpen; the two must agree.
class DrawerHoldTest {
    private fun media(kind: MediaKind) =
        InstalledApp("media:${kind.name}", kind.label, null, isGame = false, isEmulator = false, media = DrawerMedia(kind, "1", null, null))

    @Test
    fun `a video and a book take the hold`() {
        assertEquals("a drawer video opens on a tap", LAUNCH_HOLD_MS, drawerLaunchAction(media(MediaKind.VIDEO), null).holdMs)
        assertEquals("a drawer book opens on a tap", LAUNCH_HOLD_MS, drawerLaunchAction(media(MediaKind.BOOK), null).holdMs)
    }

    @Test
    fun `an album opens on a tap and an app keeps the hold`() {
        assertEquals("a drawer album needs the hold", 0L, drawerLaunchAction(media(MediaKind.MUSIC), null).holdMs)
        assertEquals(LAUNCH_HOLD_MS, drawerLaunchAction(InstalledApp("com.discord", "Discord", null, isGame = false, isEmulator = false), null).holdMs)
    }
}
