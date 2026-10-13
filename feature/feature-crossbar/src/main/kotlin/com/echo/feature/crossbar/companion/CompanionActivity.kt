package com.echo.feature.crossbar.companion

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.view.Display
import android.view.KeyEvent
import android.view.MotionEvent
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
import com.echo.feature.crossbar.gamepad.AskedKeyboard
import com.echo.feature.crossbar.gamepad.backHidesKeyboard
import com.echo.feature.crossbar.gamepad.hideKeyboard
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

// ECHO on a device's second display (the AYN Thor's lower display): the Companion Screen, or in Swap the Main
// Screen. It draws CompanionLink's state and sends its taps back; the crossbar does the work.
@AndroidEntryPoint
class CompanionActivity : ComponentActivity() {
    @Inject lateinit var link: CompanionLink

    @Inject lateinit var gamepad: com.echo.feature.crossbar.gamepad.GamepadInputHandler

    @Inject lateinit var menuSounds: com.echo.core.ui.sound.MenuSoundPlayer

    @Inject lateinit var uiMediaStore: com.echo.core.data.repository.UiMediaStore

    @Inject lateinit var uiMediaAudio: com.echo.core.ui.media.UiMediaAudioPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // touch only: it never takes the controller from the Main Screen
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
                    // single screen chosen in Quick settings: the second display goes back to Android. Not when HOME
                    // started this window: Android starts the second display's home again when it finishes, and
                    // that home is this window, so it would close and reopen without end. Single leaves a
                    // HOME-started Companion on the second display (owner, 2026-10-12)
                    LaunchedEffect(ui.secondScreenEnabled) { if (!ui.secondScreenEnabled && !startedAsHome) finishAndRemoveTask() }
                    val keys = companionKeys(ui)
                    LaunchedEffect(keys) { onKeys(keys) }
                    // Swap: the Main Screen (the Crossbar) is drawn here, the Companion Screen on the first display
                    if (ui.screensSwapped) CrossbarShellContainer(viewModel = vm) else CompanionScreen(state, vm)
                }
            }
        }
    }

    // stopped also when an emulator takes this screen for a DS or 3DS game (owner, 2026-10-06): it is
    // left the screen, and ECHO shows here again when it is back in front
    override fun onStart() {
        super.onStart()
        link.attach(true)
    }

    // Android moves key focus to the display an activity starts on, and this window takes no keys, so
    // the controller's presses would wait on it until ECHO stopped responding. ECHO's own task is
    // brought back to the front, which returns key focus to its display.
    override fun onResume() {
        super.onResume()
        if (!claimingKeyboard && link.hostShown.value && keys != CompanionKeys.HOLD) handFocusBack()
    }

    // the Thor shows a keyboard opened on the first display here (ime_show_on_second), and this display's
    // keyboard answers to the last window focused on it: the stock launcher, under this window, unless this
    // window takes focus once (owner, 2026-10-07). On the Thor a claim on each start did not hold: when
    // Search opened, this screen's keyboard still answered to the stock launcher. So it is claimed when typing
    // starts, then focus is handed back (CompanionKeys.CLAIM)
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
        if (keys == CompanionKeys.HOLD) return
        takeKeys(false)
        if (link.hostShown.value) handFocusBack()
        link.keyboardClaimEnded()
    }

    private var keys = CompanionKeys.NONE

    private fun handFocusBack() {
        getSystemService(ActivityManager::class.java)?.appTasks.orEmpty()
            .firstOrNull { it.taskInfo?.let { info -> info.taskId != taskId } == true }
            ?.let { runCatching { it.moveToFront() }.onFailure { e -> Timber.w(e, "Could not hand key focus back to the first display") } }
    }

    override fun onStop() {
        link.attach(false)
        super.onStop()
    }

    // HOLD: the field is in this window (the Main Screen drawn here in Swap, or a name prompt opened from the
    // Companion's menu, ADR 0002), and typing (Search or a name prompt) takes keys and key focus at once,
    // so the keyboard opens; without them the keyboard cannot reach the field and Android reports ECHO as not
    // responding (owner, 2026-10-08). The controller's presses then arrive here and are passed on, so they still
    // drive ECHO. CLAIM: typing on the Main Screen, focus is taken once so the keyboard is drawn here
    private fun onKeys(keys: CompanionKeys) {
        this.keys = keys
        if (keys == CompanionKeys.HOLD) {
            takeKeys(true)
            keyboard.typing(true)
            takeFocus()
        } else if (keys == CompanionKeys.CLAIM) {
            claimKeyboard()
        } else {
            // the Thor keeps its keyboard on this screen (ime_fixed): left up, it would stay over ECHO
            keyboard.typing(false)
            hideKeyboard()
            takeKeys(false)
            if (link.hostShown.value) handFocusBack()
        }
    }

    // NOT_FOCUSABLE alone also keeps the keyboard from layering over this window, and the Thor shows a
    // keyboard opened on the first display here (ime_show_on_second): it sat under ECHO, up but unseen
    // (owner, 2026-10-07). With ALT_FOCUSABLE_IM beside it the window still takes no keys, and the
    // keyboard layers above it.
    private fun takeKeys(take: Boolean) {
        val both = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
        window.setFlags(if (take) 0 else both, both)
    }

    // the Thor shows the keyboard full screen over this window, which then gets no keyboard insets, so the
    // keyboard ECHO asked for is what B reads here
    private val keyboard = AskedKeyboard()

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        // a tap while typing brings the keyboard up; it never moves the controller from the Main Screen (ADR 0001)
        if (event.actionMasked == MotionEvent.ACTION_DOWN) keyboard.touched()
        return super.dispatchTouchEvent(event)
    }

    // HOME put this window on the second display (the SECONDARY_HOME intent filter)
    private val startedAsHome: Boolean get() = intent?.hasCategory(Intent.CATEGORY_SECONDARY_HOME) == true

    // a new task brought to the front on this display is what moves Android's key focus here; moving this
    // task to the front does not, as it is already in front. Starting this singleTop window from itself only
    // hands it a new intent and leaves focus where it was, and a new task of it would add a second window
    // over one HOME started. So CompanionFocusActivity starts in a new task here and closes at once, and
    // focus falls to this window under it.
    private fun takeFocus() {
        // this activity's own display; Activity.getDisplay needs API 30
        @Suppress("DEPRECATION") val here = windowManager.defaultDisplay.displayId
        val options = ActivityOptions.makeCustomAnimation(this, 0, 0).setLaunchDisplayId(here).toBundle()
        val intent = Intent(this, CompanionFocusActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
        runCatching { startActivity(intent, options) }
            .onFailure { Timber.w(it, "Could not take key focus for the Companion Screen") }
    }

    // the same override MainActivity makes, for the same lint reason
    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean =
        backHidesKeyboard(event, gamepad, keyboard, keyboardElsewhere = true) || gamepad.onKeyEvent(event) || super.dispatchKeyEvent(event)

    companion object {
        // the device's second display, if it has one
        fun secondDisplay(activity: Activity): Display? = com.echo.core.data.launch.LaunchScreens.secondDisplay(activity)

        // starts this window on the second display, when the device has one; nothing otherwise
        fun showBeside(activity: Activity) {
            val target = secondDisplay(activity) ?: return
            val options = ActivityOptions.makeBasic().setLaunchDisplayId(target.displayId).toBundle()
            runCatching {
                activity.startActivity(Intent(activity, CompanionActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), options)
            }.onFailure { Timber.w(it, "Could not show ECHO on display ${target.displayId}") }
        }
    }
}

// takes Android's key focus to the Companion's display and closes at once (CompanionActivity.takeFocus).
// Translucent, so the Companion window under it is only paused, not stopped and started again.
class CompanionFocusActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        finishAndRemoveTask()
    }
}
