package com.echo.feature.crossbar.gamepad

import android.app.Activity
import android.view.KeyEvent
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.echo.core.domain.model.GamepadAction

// owner decision: B with the on-screen keyboard up hides it; the next B goes back. One rule for both
// screens, as the keyboard is up on whichever one is typing.
fun Activity.backHidesKeyboard(event: KeyEvent, gamepad: GamepadInputHandler): Boolean {
    if (event.action != KeyEvent.ACTION_DOWN) return false
    if (gamepad.currentMappings.actionFor(event.keyCode) != GamepadAction.BACK) return false
    val insets = ViewCompat.getRootWindowInsets(window.decorView) ?: return false
    if (!insets.isVisible(WindowInsetsCompat.Type.ime())) return false
    WindowInsetsControllerCompat(window, window.decorView).hide(WindowInsetsCompat.Type.ime())
    return true
}
