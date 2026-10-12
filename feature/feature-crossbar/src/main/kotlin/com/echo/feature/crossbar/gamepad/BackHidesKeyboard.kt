package com.echo.feature.crossbar.gamepad

import android.app.Activity
import android.view.KeyEvent
import android.view.inputmethod.InputMethodManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.echo.core.domain.model.GamepadAction

// the keyboard ECHO asked for: up when typing starts (Search or a name prompt) or the screen is touched while
// typing, down when B hides it or typing ends. A window gets keyboard insets only for a keyboard drawn over
// it, and the Thor draws ECHO's keyboard on its other display (ime_show_on_second, ime_fixed), so there the
// keyboard ECHO asked for is what counts.
// ponytail: a keyboard closed by its own hide key still counts as up, so the next B only puts it down
class AskedKeyboard {
    private var typing = false
    private var up = false

    fun typing(on: Boolean) {
        typing = on
        up = on
    }

    fun touched() {
        if (typing) up = true
    }

    // B with the keyboard up hides it; the next B goes back (owner, 2026-10-12). True when this B hides it.
    // keyboardElsewhere: the keyboard is drawn on another display, so the window's insets cannot show it
    fun backHides(insetsShowKeyboard: Boolean, keyboardElsewhere: Boolean): Boolean {
        val hides = insetsShowKeyboard || (keyboardElsewhere && up)
        if (hides) up = false
        return hides
    }
}

// the one rule for both screens: true when this press was B and it hid the keyboard
fun Activity.backHidesKeyboard(
    event: KeyEvent,
    gamepad: GamepadInputHandler,
    keyboard: AskedKeyboard,
    keyboardElsewhere: Boolean,
): Boolean {
    if (event.action != KeyEvent.ACTION_DOWN) return false
    if (gamepad.currentMappings.actionFor(event.keyCode) != GamepadAction.BACK) return false
    val insets = ViewCompat.getRootWindowInsets(window.decorView)?.isVisible(WindowInsetsCompat.Type.ime()) == true
    if (!keyboard.backHides(insets, keyboardElsewhere)) return false
    hideKeyboard()
    return true
}

fun Activity.hideKeyboard() {
    WindowInsetsControllerCompat(window, window.decorView).hide(WindowInsetsCompat.Type.ime())
    getSystemService(InputMethodManager::class.java)?.hideSoftInputFromWindow(window.decorView.windowToken, 0)
}
