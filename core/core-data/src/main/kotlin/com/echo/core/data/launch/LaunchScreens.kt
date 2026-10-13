package com.echo.core.data.launch

import android.app.Activity
import android.app.Application
import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Display
import android.widget.Toast
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.echo.core.data.datastore.echoDataStore
import com.echo.core.domain.model.KnownEmulatorPackages
import dagger.hilt.android.qualifiers.ApplicationContext
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
    // displayFor: the Launch Screen is for games and apps (CONTEXT.md)
    fun mainDisplay(): Int = if (mainOnSecond) secondDisplay(context)?.displayId ?: Display.DEFAULT_DISPLAY else Display.DEFAULT_DISPLAY

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
        val old = launchScreen(before, dualScreen, otherScreen = false, mainDisplay(), displays)
        if (otherScreen && packageName != null && display != old) closeItFirst(packageName, old, display)
        return display
    }

    // the displays an ECHO window is resumed on. An app brought to the front on a display pauses ECHO's window there
    private val echoShownOn = ConcurrentHashMap<Activity, Int>()

    init {
        (context.applicationContext as? Application)?.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            // Activity.getDisplay needs API 30
            @Suppress("DEPRECATION")
            override fun onActivityResumed(activity: Activity) { echoShownOn[activity] = activity.windowManager.defaultDisplay.displayId }
            override fun onActivityPaused(activity: Activity) { echoShownOn.remove(activity) }
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityStarted(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }

    // Close it first (CloseItFirst.kt): a second after Launch on Other Screen, an app still on its old display is
    // named on the Main Screen
    private fun closeItFirst(packageName: String, old: Int, target: Int) {
        Handler(Looper.getMainLooper()).postDelayed({
            if (!stayedOnOldDisplay(old, target, echoShownOn.values.toSet())) return@postDelayed
            val pm = context.packageManager
            val label = runCatching { pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)) }.getOrNull() ?: packageName
            val main = context.getSystemService(DisplayManager::class.java)?.getDisplay(mainDisplay()) ?: return@postDelayed
            Toast.makeText(context.createDisplayContext(main), "Close $label first to move it to the other screen.", Toast.LENGTH_LONG).show()
        }, CLOSE_IT_FIRST_DELAY_MS)
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

        // about a second (owner, 2026-10-12)
        private const val CLOSE_IT_FIRST_DELAY_MS = 1_000L

        // the device's second display, if it has one: the AYN Thor's lower display
        fun secondDisplay(context: Context): Display? =
            context.getSystemService(DisplayManager::class.java)
                ?.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION).orEmpty()
                .firstOrNull { it.displayId != Display.DEFAULT_DISPLAY }

        private fun encode(map: Map<String, Int>): Set<String> = map.mapTo(mutableSetOf()) { (pkg, display) -> "$pkg=$display" }

        private fun decode(set: Set<String>): Map<String, Int> =
            set.mapNotNull { entry -> entry.substringAfterLast('=').toIntOrNull()?.let { entry.substringBeforeLast('=') to it } }.toMap()
    }
}

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
