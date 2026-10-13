package com.echo.core.ui.design

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

    // issue #36: a finger on a launch button holds the same way A does, so the footer's card rises for it too
    @Test
    fun `a finger on a launch button raises the card while it is down and launches only through the hold`() {
        hold.press("skyrim") { launches++ }
        assertEquals("the card is out while the finger is down", "skyrim", held)
        hold.lift()
        scope.advanceTimeBy(LAUNCH_HOLD_MS * 2)
        assertEquals("a finger lifted early launches nothing", 0, launches)
        assertNull(held)

        hold.press("skyrim") { launches++ }
        scope.advanceTimeBy(LAUNCH_HOLD_MS)
        scope.runCurrent()
        hold.lift()
        assertEquals("a finger held through the ring launches once", 1, launches)
    }

    @Test
    fun `a finger lifted never cuts short a hold on A`() {
        hold.start("skyrim") { launches++ }
        hold.lift()
        scope.advanceTimeBy(LAUNCH_HOLD_MS)
        scope.runCurrent()
        assertEquals(1, launches)
    }
}
