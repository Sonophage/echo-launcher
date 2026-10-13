package com.echo.feature.crossbar.viewmodel

import com.echo.core.ui.design.LAUNCH_HOLD_MS
import org.junit.Assert.assertEquals
import org.junit.Test

class LaunchesOutTest {
    @Test
    fun `games and apps need the hold, media and folders do not, so a tap on a track still plays it`() {
        assertEquals(true, CrossbarItem(id = "g", title = "Skyrim", gameId = 1L, isRealGame = true).launchesOut())
        assertEquals(true, CrossbarItem(id = "a", title = "Discord", packageName = "com.discord").launchesOut())
        assertEquals("a track plays inside ECHO", false, CrossbarItem(id = "t", title = "Track", type = CrossbarItemType.MUSIC_TRACK).launchesOut())
        assertEquals("a shelf folder only opens", false, CrossbarItem(id = "f", title = "Playing", gameId = 1L, isRealGame = false).launchesOut())
        assertEquals(LAUNCH_HOLD_MS, holdMsFor(CrossbarItem(id = "a", title = "Discord", packageName = "com.discord")))
        assertEquals(0L, holdMsFor(null))
    }
}
