package com.echo.feature.crossbar.viewmodel

import com.echo.core.ui.design.LAUNCH_HOLD_MS
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LaunchHoldTest {
    private val scope = TestScope()
    private var held: String? = null
    private var launches = 0
    private val hold = LaunchHold(scope) { held = it }

    @Test
    fun `a tap on A never launches, which is the misclick the hold exists to stop`() {
        hold.start("skyrim") { launches++ }
        scope.advanceTimeBy(LAUNCH_HOLD_MS - 1)
        hold.release()
        scope.advanceTimeBy(LAUNCH_HOLD_MS * 2)
        assertEquals(0, launches)
        assertNull("the ring empties when A comes up early", held)
    }

    @Test
    fun `holding A through the ring launches once, and the late release does nothing`() {
        hold.start("skyrim") { launches++ }
        assertEquals("the ring shows on the held item", "skyrim", held)
        scope.advanceTimeBy(LAUNCH_HOLD_MS)
        scope.runCurrent()
        hold.release()
        scope.advanceTimeBy(LAUNCH_HOLD_MS * 2)
        assertEquals(1, launches)
        assertNull(held)
    }

    @Test
    fun `a second press restarts the ring instead of launching twice`() {
        hold.start("skyrim") { launches++ }
        scope.advanceTimeBy(LAUNCH_HOLD_MS / 2)
        hold.start("skyrim") { launches++ }
        scope.advanceTimeBy(LAUNCH_HOLD_MS / 2 + 1)
        assertEquals("the first press was replaced", 0, launches)
        scope.advanceTimeBy(LAUNCH_HOLD_MS)
        assertEquals(1, launches)
    }
}

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
