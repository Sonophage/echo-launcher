package com.echo.feature.settings.viewmodel

import android.content.Context
import android.content.res.Configuration
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.echo.core.data.datastore.CROSSBAR_LAYOUT_ADJUST_KEY
import com.echo.core.data.datastore.echoDataStore
import com.echo.core.data.repository.GameBootPreferences
import com.echo.core.data.repository.UiMediaStore
import com.echo.themekit.CrossbarLayoutAdjustCodec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// spec #1, story 27: the Crossbar picks its layout from the size of the window it is drawn in. Settings ▸ Layout must
// write the layout for that same window, not for the application's default display: after Swap on the Thor the
// window is on the other display, and a layout written for the wrong size bucket never reaches the Crossbar there.
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class DisplaySettingsClassicLayoutTest {
    private val dispatcher = StandardTestDispatcher()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var vm: DisplaySettingsViewModel
    private lateinit var collector: Job

    @Before
    fun setUp() {
        runBlocking {
            context.echoDataStore.edit { it.clear() }
            context.echoDataStore.data.first()
        }
        Dispatchers.setMain(dispatcher)
        vm = DisplaySettingsViewModel(
            context,
            UiMediaStore(context),
            GameBootPreferences(context),
            com.echo.core.data.launch.LaunchDiscPreferences(context),
            io.mockk.mockk(relaxed = true),
            io.mockk.mockk(relaxed = true) {
                io.mockk.every { prefs } returns kotlinx.coroutines.flow.flowOf(com.echo.core.domain.model.ControllerLayoutPrefs())
            },
            com.echo.core.data.wallpaper.StillWallpaper(context),
            com.echo.core.data.wallpaper.MotionWallpaper(context, com.echo.core.data.wallpaper.StillWallpaper(context)),
            io.mockk.mockk(relaxed = true),
            io = dispatcher,
        )
        collector = CoroutineScope(dispatcher).launch { vm.uiState.collect {} }
        dispatcher.scheduler.advanceUntilIdle()
    }

    @After
    fun tearDown() {
        collector.cancel()
        Dispatchers.resetMain()
    }

    @Test
    fun `the classic layout is written for the window Settings is drawn in, not the application's display`() = runTest(dispatcher) {
        // the application's display is compact under Robolectric (320 dp wide); the window here is expanded
        val window = Configuration(context.resources.configuration).apply {
            smallestScreenWidthDp = 900
            screenWidthDp = 1200
            screenHeightDp = 900
        }

        vm.onWindow(window, density = 2f)
        vm.applyClassicLayout()
        eventually("a layout is written") { context.echoDataStore.data.first()[CROSSBAR_LAYOUT_ADJUST_KEY] != null }

        val written = CrossbarLayoutAdjustCodec.decode(context.echoDataStore.data.first()[CROSSBAR_LAYOUT_ADJUST_KEY])
        assertEquals(setOf("expanded"), written.keys)
    }
}
