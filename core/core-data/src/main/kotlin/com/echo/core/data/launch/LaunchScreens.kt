package com.echo.core.data.launch

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.view.Display
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.echo.core.common.launch.LaunchTransition
import com.echo.core.data.datastore.echoDataStore
import com.echo.core.domain.model.KnownEmulatorPackages
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber

// the Launch Screen (CONTEXT.md): the display a game or app opens on. A launch made by ECHO goes to the
// display ECHO last launched that game or app on, or to the Main Screen's display the first time. The choice
// is device-local, as display ids mean nothing on another device, so the backup does not carry it.
@Singleton
class LaunchScreens @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    // set by the crossbar: true while a Swap has put the Main Screen on the second display
    @Volatile var mainOnSecond: Boolean = false

    // the Main Screen's display. Media opened in another app (music, video, photo, book) and an app ECHO opens
    // without knowing its package (the camera, a browser, a system settings screen) open here, not through
    // displayFor: the Launch Screen is for games and apps (CONTEXT.md). So do Android's own screens ECHO opens: app
    // info, uninstall, the share sheet, a chooser and Android Settings (#28)
    fun mainDisplay(): Int = mainScreenDisplay(mainOnSecond, secondDisplay(context)?.displayId)

    fun hasOtherScreen(): Boolean = secondDisplay(context) != null

    // the display to start packageName on, which becomes the Launch Screen of the game (gameId) or else the app.
    // Remembered before the start, so a start that fails still counts as where ECHO launched it. Pass platformId for
    // a game, so a DS game run by RetroArch opens as a DS emulator does
    fun displayFor(packageName: String?, platformId: String? = null, otherScreen: Boolean = false, gameId: Long? = null): Int {
        val dualScreen = KnownEmulatorPackages.isDualScreen(packageName, platformId)
        val displays = listOf(Display.DEFAULT_DISPLAY) + listOfNotNull(secondDisplay(context)?.displayId)
        val key = launchScreenKey(packageName, gameId)
        val before = key?.let(remembered::get)
        val display = resolveLaunchScreen(remembered, key, dualScreen, otherScreen, mainDisplay(), displays)
        if (key != null && remembered[key] != before) save()
        return display
    }

    // ponytail: read once, blocking, on the first launch; the crossbar has loaded this DataStore long before
    private val remembered: ConcurrentHashMap<String, Int> by lazy {
        val stored = runCatching { runBlocking { context.echoDataStore.data.first()[KEY] } }
            .onFailure { Timber.w(it, "Could not read the Launch Screens") }.getOrNull().orEmpty()
        ConcurrentHashMap(decode(stored))
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private fun save() {
        val snapshot = encode(remembered)
        scope.launch {
            runCatching { context.echoDataStore.edit { it[KEY] = snapshot } }
                .onFailure { Timber.w(it, "Could not save the Launch Screens") }
        }
    }

    companion object {
        private val KEY = stringSetPreferencesKey("display_launch_screens")

        // the device's second display, if it has one: the AYN Thor's lower display
        fun secondDisplay(context: Context): Display? =
            context.getSystemService(DisplayManager::class.java)
                ?.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION).orEmpty()
                .firstOrNull { it.displayId != Display.DEFAULT_DISPLAY }

        // the launch options that open an Android screen on the Main Screen's display, for a composable, which
        // cannot inject LaunchScreens (#28)
        fun mainScreenOptions(context: Context): Bundle? {
            val screens = EntryPointAccessors.fromApplication(context.applicationContext, Access::class.java).launchScreens()
            return LaunchTransition.options(context, screens.mainDisplay())
        }

        @EntryPoint
        @InstallIn(SingletonComponent::class)
        interface Access { fun launchScreens(): LaunchScreens }

        private fun encode(map: Map<String, Int>): Set<String> = map.mapTo(mutableSetOf()) { (pkg, display) -> "$pkg=$display" }

        private fun decode(set: Set<String>): Map<String, Int> =
            set.mapNotNull { entry -> entry.substringAfterLast('=').toIntOrNull()?.let { entry.substringBeforeLast('=') to it } }.toMap()
    }
}

// the Main Screen's display: the second one while a Swap has put the Main Screen there, else the first
internal fun mainScreenDisplay(mainOnSecond: Boolean, secondDisplay: Int?): Int =
    if (mainOnSecond) secondDisplay ?: Display.DEFAULT_DISPLAY else Display.DEFAULT_DISPLAY

// what a Launch Screen is remembered by: a game by its id, so two games on one emulator keep their own screens; an
// app by its package (owner, 2026-10-12). A package name has no ':', so the two never meet
internal fun launchScreenKey(packageName: String?, gameId: Long?): String? = gameId?.let { "game:$it" } ?: packageName

// the Launch Screen for key, remembered in [remembered] unless a DS or 3DS emulator opens it
internal fun resolveLaunchScreen(
    remembered: MutableMap<String, Int>, key: String?, dualScreen: Boolean, otherScreen: Boolean, mainDisplay: Int, displays: List<Int>,
): Int {
    val display = launchScreen(key?.let(remembered::get), dualScreen, otherScreen, mainDisplay, displays)
    if (key != null && !dualScreen) remembered[key] = display
    return display
}

// Seam B. A DS or 3DS emulator always opens on the Main Screen's display, as it draws both screens itself; a
// remembered display that is gone falls back to the Main Screen's. Other Screen is the present display that
// is not the Launch Screen.
internal fun launchScreen(remembered: Int?, dualScreen: Boolean, otherScreen: Boolean, mainDisplay: Int, displays: List<Int>): Int {
    if (dualScreen) return mainDisplay
    val here = remembered?.takeIf { it in displays } ?: mainDisplay
    return if (otherScreen) displays.firstOrNull { it != here } ?: here else here
}
