package com.echo.feature.crossbar.bottomscreen

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.app.ActivityOptions
import android.content.Intent
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.view.Display
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.runtime.LaunchedEffect
import com.echo.feature.crossbar.ui.CrossbarShellContainer
import com.echo.feature.crossbar.ui.EchoRoot
import com.echo.feature.crossbar.gamepad.backHidesKeyboard
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

// ECHO on a device's second screen (the AYN Thor's bottom screen). It draws BottomScreenLink's state
// and sends its taps back; the crossbar does the work.
@AndroidEntryPoint
class BottomScreenActivity : ComponentActivity() {
    @Inject lateinit var link: BottomScreenLink

    @Inject lateinit var gamepad: com.echo.feature.crossbar.gamepad.GamepadInputHandler

    @Inject lateinit var menuSounds: com.echo.core.ui.sound.MenuSoundPlayer

    @Inject lateinit var uiMediaStore: com.echo.core.data.repository.UiMediaStore

    @Inject lateinit var uiMediaAudio: com.echo.core.ui.media.UiMediaAudioPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // touch only: it never takes the controller from the top screen
        takeKeys(false)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        setContent {
            EchoRoot(menuSounds, uiMediaStore, uiMediaAudio) {
                val state by link.state.collectAsState()
                val crossbar by link.crossbar.collectAsState()
                crossbar?.let { vm ->
                    val ui by vm.uiState.collectAsState()
                    // single screen chosen in Quick settings: the second screen goes back to Android
                    LaunchedEffect(ui.secondScreenEnabled) { if (!ui.secondScreenEnabled) finishAndRemoveTask() }
                    if (ui.screensSwapped) {
                        // swapped: the XMB is drawn here, the companion on the main screen
                        LaunchedEffect(Unit) { onLocked(LockedScreenOpen(open = false, typing = false)) }
                        CrossbarShellContainer(viewModel = vm)
                    } else {
                        BottomScreen(state, vm, onLocked = ::onLocked)
                    }
                }
            }
        }
    }

    // stopped also when an emulator takes this screen for a DS or 3DS game (owner, 2026-10-06): it is
    // left the screen, and ECHO shows here again when it is back in front
    override fun onStart() {
        super.onStart()
        link.attach(true)
        claimKeyboard()
    }

    // Android moves key focus to the display an activity starts on, and this window takes no keys, so
    // the controller's presses would wait on it until ECHO stopped responding. ECHO's own task is
    // brought back to the front, which returns key focus to the top screen.
    override fun onResume() {
        super.onResume()
        if (!claimingKeyboard && link.hostShown.value && !locked.open) handFocusBack()
    }

    // the Thor shows a keyboard opened on the top screen here (ime_show_on_second), and this screen's
    // keyboard answers to the last window focused on it: the stock launcher, under this window, unless this
    // window takes focus once. It does on each start, then hands focus back (owner, 2026-10-07)
    private var claimingKeyboard = false

    private fun claimKeyboard() {
        claimingKeyboard = true
        takeKeys(true)
        takeFocus()
        window.decorView.postDelayed(::endKeyboardClaim, 1_000)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && claimingKeyboard) window.decorView.post(::endKeyboardClaim)
    }

    private fun endKeyboardClaim() {
        if (!claimingKeyboard) return
        claimingKeyboard = false
        if (locked.open) return
        takeKeys(false)
        if (link.hostShown.value) handFocusBack()
    }

    private var locked = LockedScreenOpen(open = false, typing = false)

    private fun handFocusBack() {
        getSystemService(ActivityManager::class.java)?.appTasks.orEmpty()
            .firstOrNull { it.taskInfo?.let { info -> info.taskId != taskId } == true }
            ?.let { runCatching { it.moveToFront() }.onFailure { e -> Timber.w(e, "Could not hand key focus back to the top screen") } }
    }

    override fun onStop() {
        link.attach(false)
        super.onStop()
    }

    // a screen locked here (the App Drawer, Search, Settings) takes keys, so its text fields get the
    // keyboard; Search takes key focus at once, as it does on the top screen, so the keyboard opens.
    // The controller's presses then arrive here and are passed on, so they still drive ECHO.
    private fun onLocked(locked: LockedScreenOpen) {
        this.locked = locked
        if (locked.open) {
            takeKeys(true)
            if (locked.typing) {
                keyboardUp = true
                takeFocus()
            }
        } else {
            // the Thor keeps its keyboard on this screen (ime_fixed): left up, it would stay over ECHO
            hideKeyboard()
            takeKeys(false)
            if (link.hostShown.value) handFocusBack()
        }
    }

    // NOT_FOCUSABLE alone also keeps the keyboard from layering over this window, and the Thor shows a
    // keyboard opened on the top screen here (ime_show_on_second): it sat under ECHO, up but unseen
    // (owner, 2026-10-07). With ALT_FOCUSABLE_IM beside it the window still takes no keys, and the
    // keyboard layers above it.
    private fun takeKeys(take: Boolean) {
        val both = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
        window.setFlags(if (take) 0 else both, both)
    }

    // whether the keyboard is likely up. The Thor shows it full screen over this window, which then gets
    // no keyboard insets, so it is tracked here: up when Search opens or the screen is touched while
    // typing, down when B or a closing screen hides it
    private var keyboardUp = false

    private fun hideKeyboard() {
        keyboardUp = false
        WindowInsetsControllerCompat(window, window.decorView).hide(WindowInsetsCompat.Type.ime())
        getSystemService(InputMethodManager::class.java)?.hideSoftInputFromWindow(window.decorView.windowToken, 0)
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            if (locked.typing) keyboardUp = true
            // a tap gives the controller to this screen's content: the companion, or the XMB when swapped
            link.crossbar.value?.let { it.bottomScreen.setCompanionActive(!it.uiState.value.screensSwapped) }
        }
        return super.dispatchTouchEvent(event)
    }

    // owner decision: B with the keyboard up hides it; the next B goes back
    private fun backHidesTrackedKeyboard(event: KeyEvent): Boolean {
        if (!keyboardUp || event.action != KeyEvent.ACTION_DOWN) return false
        if (gamepad.currentMappings.actionFor(event.keyCode) != com.echo.core.domain.model.GamepadAction.BACK) return false
        hideKeyboard()
        return true
    }

    // starting itself again on its own display is what moves Android's key focus to this screen; moving
    // its task to the front does not, as it is already in front here
    private fun takeFocus() {
        // this activity's own display; Activity.getDisplay needs API 30
        @Suppress("DEPRECATION") val here = windowManager.defaultDisplay.displayId
        val options = ActivityOptions.makeBasic().setLaunchDisplayId(here).toBundle()
        runCatching { startActivity(Intent(this, BottomScreenActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), options) }
            .onFailure { Timber.w(it, "Could not take key focus for the bottom screen") }
    }

    // the same override MainActivity makes, for the same lint reason
    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean =
        backHidesTrackedKeyboard(event) || backHidesKeyboard(event, gamepad) || gamepad.onKeyEvent(event) || super.dispatchKeyEvent(event)

    companion object {
        // shows the bottom screen on a second display, when the device has one; nothing otherwise
        // the device's second display, if it has one
        fun secondDisplay(activity: Activity): Display? =
            activity.getSystemService(DisplayManager::class.java)
                ?.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION).orEmpty()
                .firstOrNull { it.displayId != Display.DEFAULT_DISPLAY }

        fun showBeside(activity: Activity) {
            val target = secondDisplay(activity) ?: return
            val options = ActivityOptions.makeBasic().setLaunchDisplayId(target.displayId).toBundle()
            runCatching {
                activity.startActivity(Intent(activity, BottomScreenActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), options)
            }.onFailure { Timber.w(it, "Could not show ECHO on display ${target.displayId}") }
        }
    }
}
