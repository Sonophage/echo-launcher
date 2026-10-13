package com.echo.core.data.launch

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Close it first (#30): about a second after Launch on Other Screen, ECHO reads which of its screens an app now covers.
// Display 0 is the Thor's upper display; 2 is its lower display.
class CloseItFirstTest {
    @Test
    fun `an app that stayed on the old display, which still shows ECHO on the other, is told to close first`() {
        assertTrue(stayedOnOldDisplay(old = 0, target = 2, echoShownOn = setOf(2)))
    }

    @Test
    fun `an app that moved, as Chrome does while running, covers ECHO on the other display and is not warned`() {
        assertFalse(stayedOnOldDisplay(old = 0, target = 2, echoShownOn = setOf(0)))
        assertFalse(stayedOnOldDisplay(old = 0, target = 2, echoShownOn = emptySet()))
    }

    @Test
    fun `a launch that did not start leaves ECHO on both displays and is not warned`() {
        assertFalse(stayedOnOldDisplay(old = 0, target = 2, echoShownOn = setOf(0, 2)))
    }

    @Test
    fun `a launch that stays on one display is never warned`() {
        assertFalse(stayedOnOldDisplay(old = 0, target = 0, echoShownOn = setOf(2)))
    }
}
