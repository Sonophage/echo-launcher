package com.echo.feature.settings.viewmodel

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import com.echo.core.data.datastore.echoDataStore
import com.echo.core.data.repository.GameBootPreferences
import com.echo.core.data.repository.UiMediaStore
import com.echo.core.domain.model.IconLegibilityStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class DisplaySettingsViewModelLegibilityTest {
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
                io.mockk.every { prefs } returns kotlinx.coroutines.flow.flowOf(
                    com.echo.core.domain.model.ControllerLayoutPrefs()
                )
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
    fun `every style persists its enum name and comes back`() = runTest(dispatcher) {
        val expected = IconLegibilityStyle.entries
        val seen = mutableListOf<IconLegibilityStyle>()

        expected.forEach { style ->
            vm.setIconLegibility(style)
            eventually("$style persisted") {
                context.echoDataStore.data.first()[KEY_ICON_LEGIBILITY] == style.name
            }
            eventually("$style visible") {
                vm.uiState.first().iconLegibility == style
            }
            seen += vm.uiState.first().iconLegibility
        }

        assertEquals(expected, seen)
    }

    @Test
    fun `an unknown persisted value surfaces as NONE and still takes a new pick`() = runTest(dispatcher) {
        context.echoDataStore.edit {
            it[KEY_ICON_LEGIBILITY] = "CONTOUR_MEDIUM"
        }

        eventually("stale value tolerated") {
            vm.uiState.first().iconLegibility == IconLegibilityStyle.NONE
        }

        vm.setIconLegibility(IconLegibilityStyle.OFFSET_SHADOW)
        eventually("a pick works after a stale value") {
            vm.uiState.first().iconLegibility == IconLegibilityStyle.OFFSET_SHADOW
        }
    }

    @Test
    fun `fade by distance defaults on, toggles off, and persists`() = runTest(dispatcher) {
        assertEquals(true, vm.uiState.first().fadeByDistance)

        vm.setFadeByDistance(false)
        eventually("fade by distance persisted off") {
            context.echoDataStore.data.first()[KEY_FADE_BY_DISTANCE] == false
        }
        eventually("fade by distance surfaced off") {
            !vm.uiState.first().fadeByDistance
        }

        vm.setFadeByDistance(true)
        eventually("fade by distance surfaced on again") {
            vm.uiState.first().fadeByDistance
        }
    }

    @Test
    fun `the old solid-unfocused value does not decide the new one`() = runTest(dispatcher) {
        context.echoDataStore.edit { it[KEY_SOLID_UNFOCUSED_ICONS] = false }

        eventually("the stale key is in the store") {
            context.echoDataStore.data.first()[KEY_SOLID_UNFOCUSED_ICONS] == false
        }

        assertEquals(
            "the new setting must not take its value from the old key",
            true,
            vm.uiState.first().fadeByDistance,
        )
    }

    @Test
    fun `text shadow defaults on, toggles off, and persists`() = runTest(dispatcher) {
        assertEquals(true, vm.uiState.first().textShadow)

        vm.setTextShadow(false)
        eventually("text shadow persisted off") {
            context.echoDataStore.data.first()[KEY_TEXT_SHADOW] == false
        }
        eventually("text shadow surfaced off") {
            !vm.uiState.first().textShadow
        }

        vm.setTextShadow(true)
        eventually("text shadow surfaced on again") {
            vm.uiState.first().textShadow
        }
    }

    @Test
    fun `hint delay goes back to Always after the slider has moved`() = runTest(dispatcher) {
        vm.setContextMenuHintDelaySeconds(3f)
        eventually("hint delay persisted at 3s") {
            context.echoDataStore.data.first()[KEY_HINT_DELAY] == 3f
        }

        vm.setContextMenuHintDelaySeconds(0f)
        eventually("the second write landed") {
            context.echoDataStore.data.first()[KEY_HINT_DELAY] != 3f
        }
        assertEquals(
            "0 is Always and must be stored as 0, not clamped up to 1",
            0f,
            context.echoDataStore.data.first()[KEY_HINT_DELAY],
        )
    }

    private companion object {
        val KEY_ICON_LEGIBILITY = stringPreferencesKey("display_icon_legibility")
        val KEY_FADE_BY_DISTANCE = booleanPreferencesKey("display_fade_by_distance")

        val KEY_SOLID_UNFOCUSED_ICONS = booleanPreferencesKey("display_solid_unfocused_icons")
        val KEY_TEXT_SHADOW = booleanPreferencesKey("display_text_shadow")
        val KEY_HINT_DELAY = androidx.datastore.preferences.core.floatPreferencesKey("interface_context_menu_hint_delay_seconds")
    }
}
