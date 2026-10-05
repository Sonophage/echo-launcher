package com.echo.launcher

import com.echo.core.domain.model.GamepadAction
import android.Manifest
import android.annotation.SuppressLint
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
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.ViewCompat
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
    lateinit var launchDispatcher: com.echo.feature.launcher.LaunchDispatcher

    @Inject
    lateinit var discordBootstrap: com.echo.launcher.discord.DiscordBootstrap

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
        lifecycleScope.launch {
            if (crossbarViewModel.firstRunWizardAsksForNotifications()) {
                crossbarViewModel.onStartupPermissionsSettled()
            } else {
                requestNotificationPermissionIfNeeded()
            }
        }
        startMenuMusicIfWanted()
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
        }

        setContent {
            EchoTheme {
                androidx.compose.runtime.CompositionLocalProvider(
                    com.echo.core.ui.sound.LocalMenuSounds provides { sound -> menuSoundPlayer.play(sound) },
                    com.echo.core.ui.sound.LocalLaunchDiscCue provides {
                        uiMediaStore.pathFor(
                            com.echo.core.domain.model.UiMediaSlot.LAUNCH_DISC_AUDIO,
                        )?.let { track ->

                            uiMediaAudioPlayer.play(
                                uri = track,
                                clipEndMs =
                                    com.echo.core.ui.components.DiscCeremony.HandOffMs.toLong(),
                                label = "launch-disc",
                            )
                        }
                    },
                ) {
                ProvideControllerPrompts {
                    AppCrossbarHost()
                }
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

    override fun onResume() {
        super.onResume()
        hideSystemBars()
        holdRootFocus()

        launchDispatcher.onHostResumed()
        discordBootstrap.onResume()
        rebindNotificationListenerIfDetached()

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
                ) { enabled, _ -> enabled }
                    .collect { enabled ->
                        val track = withContext(kotlinx.coroutines.Dispatchers.IO) {
                            uiMediaStore.pathFor(com.echo.core.domain.model.UiMediaSlot.MENU_MUSIC)
                        }
                        menuMusicPlayer.setWanted(enabled, track)
                    }
            }
        }
    }

    override fun onStop() {
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

    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (minimalKeyboardKey(event)) return true
        if (backHidesKeyboard(event)) return true

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
    private fun backHidesKeyboard(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) return false
        if (gamepadInputHandler.currentMappings.actionFor(event.keyCode) != GamepadAction.BACK) return false
        val insets = ViewCompat.getRootWindowInsets(window.decorView) ?: return false
        if (!insets.isVisible(WindowInsetsCompat.Type.ime())) return false
        WindowInsetsControllerCompat(window, window.decorView).hide(WindowInsetsCompat.Type.ime())
        return true
    }

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
