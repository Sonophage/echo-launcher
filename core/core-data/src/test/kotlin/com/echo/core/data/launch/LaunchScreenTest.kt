package com.echo.core.data.launch

import org.junit.Assert.assertEquals
import org.junit.Test

// Seam B: the Launch Screen. Display 0 is the Thor's upper display; 2 is its lower display.
class LaunchScreenTest {
    private val both = listOf(0, 2)

    @Test
    fun `an app opens on the display ECHO last launched it on`() {
        assertEquals(2, launchScreen(remembered = 2, dualScreen = false, otherScreen = false, mainDisplay = 0, displays = both))
    }

    @Test
    fun `an app never launched opens on the Main Screen's display, wherever a Swap has put it`() {
        assertEquals(0, launchScreen(remembered = null, dualScreen = false, otherScreen = false, mainDisplay = 0, displays = both))
        assertEquals(2, launchScreen(remembered = null, dualScreen = false, otherScreen = false, mainDisplay = 2, displays = both))
    }

    @Test
    fun `a DS or 3DS emulator always opens on the Main Screen's display, so it can use both screens itself`() {
        assertEquals(0, launchScreen(remembered = 2, dualScreen = true, otherScreen = false, mainDisplay = 0, displays = both))
        assertEquals(0, launchScreen(remembered = 2, dualScreen = true, otherScreen = true, mainDisplay = 0, displays = both))
    }

    @Test
    fun `a remembered display that is no longer present falls back to the Main Screen's display`() {
        assertEquals(0, launchScreen(remembered = 2, dualScreen = false, otherScreen = false, mainDisplay = 0, displays = listOf(0)))
    }

    @Test
    fun `Launch on Other Screen opens it on the display that is not its Launch Screen`() {
        assertEquals(2, launchScreen(remembered = null, dualScreen = false, otherScreen = true, mainDisplay = 0, displays = both))
        assertEquals(0, launchScreen(remembered = 2, dualScreen = false, otherScreen = true, mainDisplay = 0, displays = both))
    }

    @Test
    fun `Launch on Other Screen with one display stays on it`() {
        assertEquals(0, launchScreen(remembered = null, dualScreen = false, otherScreen = true, mainDisplay = 0, displays = listOf(0)))
    }
}
