package com.echo.launcher

import com.echo.feature.crossbar.viewmodel.ceremonyPlaying
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import com.echo.core.domain.model.GamepadAction
import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.lifecycleScope
import com.echo.core.ui.notification.AndroidNotifications
import com.echo.core.ui.theme.EchoTheme
import com.echo.feature.library.scanner.LibraryRescanCoordinator
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import timber.log.Timber
import com.echo.feature.crossbar.gamepad.GamepadInputHandler
import com.echo.feature.crossbar.gamepad.backHidesKeyboard
import com.echo.feature.crossbar.viewmodel.CrossbarViewModel
import com.echo.launcher.notification.EchoNotificationListener
import com.echo.launcher.receiver.InstallShortcutReceiver
import com.echo.launcher.receiver.MediaMountReceiver
import com.echo.launcher.receiver.UsbDisconnectReceiver
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var gamepadInputHandler: GamepadInputHandler

    @Inject
    lateinit var menuSoundPlayer: com.echo.core.ui.sound.MenuSoundPlayer

    @Inject
    lateinit var menuMusicPlayer: com.echo.core.ui.media.MenuMusicPlayer

    @Inject
    lateinit var menuMusicPreferences: com.echo.core.data.media.MenuMusicPreferences

    @Inject
    lateinit var uiMediaAudioPlayer: com.echo.core.ui.media.UiMediaAudioPlayer

    @Inject
    lateinit var libraryRescanCoordinator: LibraryRescanCoordinator

    @Inject
    lateinit var uiMediaStore: com.echo.core.data.repository.UiMediaStore

    @Inject
    lateinit var goneGameArt: com.echo.feature.artwork.store.GoneGameArtworkSweep

    @Inject
    lateinit var launchDispatcher: com.echo.feature.launcher.LaunchDispatcher

    @Inject
    lateinit var discordBootstrap: com.echo.launcher.discord.DiscordBootstrap

    @Inject
    lateinit var bottomScreenLink: com.echo.feature.crossbar.bottomscreen.BottomScreenLink

    private val crossbarViewModel: CrossbarViewModel by viewModels()

    private var wasStopped = false

    private val installShortcutReceiver = InstallShortcutReceiver()

    private val mediaMountReceiver = MediaMountReceiver()

    private val usbDisconnectReceiver = UsbDisconnectReceiver()

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            crossbarViewModel.onStartupPermissionsSettled()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        hideSystemBars()
        if (intent?.hasCategory(Intent.CATEGORY_HOME) == true) closeOtherEchoTasks()
        lifecycleScope.launch {
            if (crossbarViewModel.firstRunWizardAsksForNotifications()) {
                crossbarViewModel.onStartupPermissionsSettled()
            } else {
                requestNotificationPermissionIfNeeded()
            }
        }
        startMenuMusicIfWanted()
        // shows the second screen each time ECHO comes to the front, and at once when Dual is chosen
        // again in Quick settings
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                crossbarViewModel.uiState.map { it.secondScreenEnabled }.distinctUntilChanged().collect { on ->
                    if (on) com.echo.feature.crossbar.bottomscreen.BottomScreenActivity.showBeside(this@MainActivity)
                }
            }
        }
        ContextCompat.registerReceiver(
            this,
            installShortcutReceiver,
            IntentFilter(InstallShortcutReceiver.ACTION_INSTALL_SHORTCUT),
            ContextCompat.RECEIVER_EXPORTED,
        )
        ContextCompat.registerReceiver(
            this,
            mediaMountReceiver,

            IntentFilter(Intent.ACTION_MEDIA_MOUNTED).apply { addDataScheme("file") },
            ContextCompat.RECEIVER_EXPORTED,
        )
        ContextCompat.registerReceiver(
            this,
            usbDisconnectReceiver,
            IntentFilter(UsbDisconnectReceiver.ACTION_USB_STATE),

            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
            }
        }

        onBackPressedDispatcher.addCallback(this, callback)

        discordBootstrap.onCreate(this)

        lifecycleScope.launch {
            runCatching { uiMediaStore.pruneOrphans() }
                .onFailure { Timber.w(it, "Startup UI-media prune failed") }
            runCatching { goneGameArt.run() }
                .onFailure { Timber.w(it, "Startup artwork sweep failed") }
        }

        setContent {
            com.echo.feature.crossbar.ui.EchoRoot(menuSoundPlayer, uiMediaStore, uiMediaAudioPlayer) {
                val ui by crossbarViewModel.uiState.collectAsState()
                if (ui.secondScreen && ui.screensSwapped) {
                    // owner, 2026-10-06: swapped, this screen shows the companion and the XMB moves to the second one
                    val bottom by bottomScreenLink.state.collectAsState()
                    com.echo.feature.crossbar.bottomscreen.BottomScreen(bottom, crossbarViewModel)
                } else {
                    AppCrossbarHost()
                }
            }
        }
        holdRootFocus()
    }

    // Android spends the first d-pad press after a touch on leaving touch mode, dropping it, whenever
    // leaving moves focus onto a view. The Compose root keeps focus even in touch mode, so nothing has
    // to move and the press reaches the crossbar
    private fun holdRootFocus() {
        findViewById<android.view.ViewGroup>(android.R.id.content)?.getChildAt(0)?.let { root ->
            root.isFocusableInTouchMode = true
            // a focused view with no focus state of its own gets Android's default highlight, which on the
            // root is a light wash over the whole screen (the Thor's grey top screen after a prompt closed)
            root.defaultFocusHighlightEnabled = false
            if (!root.hasFocus()) root.requestFocus()
        }
    }

    private fun rebindNotificationListenerIfDetached() {
        if (!AndroidNotifications.isEnabled(this)) return
        lifecycleScope.launch {
            delay(LISTENER_ATTACH_GRACE_MS)
            if (AndroidNotifications.isAttached) return@launch
            Timber.i("Notification listener is allowed but not attached; toggling it so the system rebinds")
            val listener = ComponentName(this@MainActivity, EchoNotificationListener::class.java)
            runCatching {
                packageManager.setComponentEnabledSetting(
                    listener, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP,
                )
                packageManager.setComponentEnabledSetting(
                    listener, PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, PackageManager.DONT_KILL_APP,
                )
            }.onFailure { Timber.w(it, "Notification listener rebind failed") }
        }
    }

    // becoming the Home app makes Android start ECHO again in a home task, beside the ECHO opened from
    // the app list; that one is closed so there is one ECHO. Only tasks rooted in this activity, so a
    // game ECHO started is never closed
    private fun closeOtherEchoTasks() {
        getSystemService(ActivityManager::class.java)?.appTasks.orEmpty().forEach { task ->
            runCatching {
                val info = task.taskInfo ?: return@runCatching
                if (info.taskId != taskId && info.baseActivity?.className == MainActivity::class.java.name) task.finishAndRemoveTask()
            }.onFailure { Timber.w(it, "Could not close another ECHO task") }
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
        holdRootFocus()

        launchDispatcher.onHostResumed()
        discordBootstrap.onResume()
        rebindNotificationListenerIfDetached()
        // HOME, or a reinstall, can leave Android's launcher on the second screen while this one never stopped
        if (bottomScreenLink.needsCompanion(crossbarViewModel.uiState.value.secondScreenEnabled)) {
            com.echo.feature.crossbar.bottomscreen.BottomScreenActivity.showBeside(this)
        }

        if (wasStopped) {
            wasStopped = false
            crossbarViewModel.onHostResumed()
        }

        lifecycleScope.launch {
            runCatching { libraryRescanCoordinator.onResume() }
                .onFailure { Timber.e(it, "Resume-triggered library rescan failed") }
        }
    }

    private fun startMenuMusicIfWanted() {
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.RESUMED) {
                kotlinx.coroutines.flow.combine(
                    menuMusicPreferences.enabledFlow,
                    uiMediaStore.stamp,
                    // owner, 2026-10-05: the boot and launch sounds were lost under the menu music, which started
                    // with ECHO and kept playing through them; it now waits until they are over
                    crossbarViewModel.uiState.map { it.ceremonyPlaying }.distinctUntilChanged(),
                ) { enabled, _, ceremony -> enabled && !ceremony }
                    .collect { enabled ->
                        val track = withContext(kotlinx.coroutines.Dispatchers.IO) {
                            uiMediaStore.pathFor(com.echo.core.domain.model.UiMediaSlot.MENU_MUSIC)
                        }
                        menuMusicPlayer.setWanted(enabled, track)
                    }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        bottomScreenLink.hostShown(true)
        crossbarViewModel.secondDisplayPresent(com.echo.feature.crossbar.bottomscreen.BottomScreenActivity.secondDisplay(this) != null)
    }

    override fun onStop() {
        bottomScreenLink.hostShown(false)
        menuMusicPlayer.setWanted(wanted = false, track = null)

        launchDispatcher.onHostStopped()
        wasStopped = true
        super.onStop()
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(installShortcutReceiver) }
        runCatching { unregisterReceiver(mediaMountReceiver) }
        runCatching { unregisterReceiver(usbDisconnectReceiver) }
        super.onDestroy()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            crossbarViewModel.onStartupPermissionsSettled()
            return
        }
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            crossbarViewModel.onStartupPermissionsSettled()
            return
        }
        requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    // owner, 2026-10-06: with a second screen, a tap gives the controller to the screen tapped; this one
    // shows the XMB, or the companion when the screens are swapped
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            crossbarViewModel.touchedScreen(showsCompanion = crossbarViewModel.uiState.value.screensSwapped)
        }
        return super.dispatchTouchEvent(event)
    }

    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (minimalKeyboardKey(event)) return true
        if (backHidesKeyboard(event, gamepadInputHandler)) return true

        if (gamepadInputHandler.onKeyEvent(event)) return true
        if (openSearchOnTypedCharacter(event)) return true
        return super.dispatchKeyEvent(event)
    }

    private fun minimalKeyboardKey(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN || event.repeatCount != 0) return false
        if (event.isCtrlPressed || event.isAltPressed || event.isMetaPressed) return false
        return when (event.keyCode) {
            KeyEvent.KEYCODE_ENTER -> enterOpensAppDrawer(event)
            KeyEvent.KEYCODE_SPACE -> {
                if (!crossbarViewModel.typeToSearchAllowed()) return false
                crossbarViewModel.onClaimedKey(GamepadAction.OPEN_CONTEXT_MENU)
                true
            }
            else -> false
        }
    }

    // owner, 2026-10-05: Back (B) while the on-screen keyboard is up hides the keyboard and does nothing
    // else; the next Back goes back as usual
    private fun enterOpensAppDrawer(event: KeyEvent): Boolean {
        if (!crossbarViewModel.enterOpensAppDrawer()) return false
        crossbarViewModel.onOpenAppDrawer()
        return true
    }

    private fun openSearchOnTypedCharacter(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN || event.repeatCount != 0) return false
        if (event.isCtrlPressed || event.isAltPressed || event.isMetaPressed) return false
        val typed = event.unicodeChar
        if (typed == 0) return false
        val ch = typed.toChar()
        if (ch.isISOControl()) return false

        return crossbarViewModel.onTypedCharacter(ch.toString())
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (gamepadInputHandler.onMotionEvent(event)) return true
        return super.onGenericMotionEvent(event)
    }
}

private const val LISTENER_ATTACH_GRACE_MS = 3_000L
