package com.echo.feature.crossbar.gamepad

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// B with the keyboard up hides it; the next B goes back (owner, 2026-10-12). The Thor draws the keyboard on the
// other display, where the typing window gets no keyboard insets, so ECHO counts the keyboard it asked for
class BackHidesKeyboardTest {

    @Test
    fun `with the keyboard on the other display, the first B hides it and the second goes back`() {
        val keyboard = AskedKeyboard().apply { typing(true) }
        assertTrue("the first B hides the keyboard", keyboard.backHides(insetsShowKeyboard = false, keyboardElsewhere = true))
        assertFalse("the second B closes Search", keyboard.backHides(insetsShowKeyboard = false, keyboardElsewhere = true))
    }

    @Test
    fun `on one screen B follows the window's keyboard insets, as before`() {
        val keyboard = AskedKeyboard().apply { typing(true) }
        assertFalse("no keyboard drawn (a hardware keyboard): B goes back", keyboard.backHides(insetsShowKeyboard = false, keyboardElsewhere = false))
        assertTrue(keyboard.backHides(insetsShowKeyboard = true, keyboardElsewhere = false))
    }

    @Test
    fun `a touch while typing brings the keyboard back, so B hides it again`() {
        val keyboard = AskedKeyboard().apply { typing(true) }
        keyboard.backHides(insetsShowKeyboard = false, keyboardElsewhere = true)
        keyboard.touched()
        assertTrue(keyboard.backHides(insetsShowKeyboard = false, keyboardElsewhere = true))
    }

    @Test
    fun `with nothing typing, B goes back`() {
        val keyboard = AskedKeyboard()
        keyboard.touched()
        assertFalse("a touch with nothing typing asks for no keyboard", keyboard.backHides(insetsShowKeyboard = false, keyboardElsewhere = true))
        keyboard.typing(true)
        keyboard.typing(false)
        assertFalse("typing ended puts the keyboard down", keyboard.backHides(insetsShowKeyboard = false, keyboardElsewhere = true))
    }
}
