package com.psplauncher.feature.settings.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.psplauncher.core.domain.model.GamepadAction
import com.psplauncher.core.ui.theme.PFPTheme
import com.psplauncher.feature.settings.viewmodel.InitialSetupUiState
import com.psplauncher.feature.settings.viewmodel.RootFolderRow
import kotlinx.coroutines.channels.Channel
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h800dp")
class SettingsScaffoldNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val pendingAction = mutableStateOf<GamepadAction?>(null)

    private val actions = Channel<GamepadAction>(Channel.UNLIMITED)

    private var consumedPlain = false

    private val opened = mutableListOf<String>()

    private fun showScreen(
        onBack: () -> Unit = {},
        leftBacksOut: Boolean = true,

        screenId: String? = null,
        onInterceptAction: ((GamepadAction) -> Boolean)? = null,
        body: @Composable () -> Unit,
    ) {
        composeRule.setContent {
            PFPTheme {
                LaunchedEffect(Unit) {
                    for (action in actions) pendingAction.value = action
                }

                val overlaySlot = remember { mutableStateOf<((GamepadAction) -> Unit)?>(null) }
                CompositionLocalProvider(
                    LocalSettingsOverlayInput provides overlaySlot,
                    LocalSettingsPendingAction provides pendingAction.value,
                    LocalSettingsActionConsumed provides { consumedPlain = true },
                    LocalSettingsLeftBacksOut provides leftBacksOut,
                    LocalSettingsScreenId provides screenId,
                    LocalSettingsOpenScreen provides { opened += it },
                ) {
                    SettingsScaffold(
                        title = "Settings",
                        subtitle = "Test screen",
                        onBack = onBack,
                        onInterceptAction = onInterceptAction,
                    ) {
                        body()
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun press(action: GamepadAction) {
        actions.trySend(action)
        composeRule.waitUntil(10_000) { consumedPlain }
        consumedPlain = false
        pendingAction.value = null
        composeRule.waitForIdle()
    }

    private fun assertFocusedRow(text: String) {
        composeRule.onNode(isFocused())
            .assert(hasText(text) or hasAnyDescendant(hasText(text)))
    }

    @Composable
    private fun ContentCursorProbe(report: (Boolean) -> Unit) {
        val visible = LocalSettingsCursorVisible.current
        SideEffect { report(visible) }
    }

    @Test
    fun `a catalog screen has no rail for LEFT to enter, so LEFT backs out with the cursor kept`() {
        var contentCursor: Boolean? = null
        var backs = 0
        showScreen(onBack = { backs++ }, screenId = "settings_library") {
            SettingsRow(label = "Add ROM Root", onClick = {})
            ContentCursorProbe { contentCursor = it }
        }

        assertFocusedRow("Add ROM Root")
        press(GamepadAction.NAVIGATE_LEFT)
        assertEquals("LEFT at the first column backs out of a tabbed screen", 1, backs)
        assertEquals("and never stands the content cursor down", true, contentCursor)
    }

    @Test
    fun `on a catalog screen with the preference off LEFT does nothing`() {
        var backs = 0
        showScreen(onBack = { backs++ }, leftBacksOut = false, screenId = "settings_library") {
            SettingsRow(label = "Add ROM Root", onClick = {})
        }

        press(GamepadAction.NAVIGATE_LEFT)
        assertEquals(0, backs)
        assertFocusedRow("Add ROM Root")
    }

    @Test
    fun `the shoulders open the neighbouring tab of the same section`() {
        showScreen(screenId = "settings_library") {
            SettingsRow(label = "Add ROM Root", onClick = {})
        }

        press(GamepadAction.NEXT_CATEGORY)
        press(GamepadAction.PREV_CATEGORY)
        assertEquals(listOf("settings_artwork", "settings_emulators_retroarch"), opened)
    }

    @Test
    fun `a screen with no rail keeps its cursor when LEFT backs out`() {
        var contentCursor: Boolean? = null
        var backs = 0
        showScreen(onBack = { backs++ }) {
            SettingsRow(label = "Add ROM Root", onClick = {})
            ContentCursorProbe { contentCursor = it }
        }

        press(GamepadAction.NAVIGATE_LEFT)
        assertEquals("LEFT with no rail backs out of the screen", 1, backs)
        assertEquals("and the content keeps its cursor", true, contentCursor)
    }

    @Test
    fun `an overlay takes every press while it is open`() {
        val seen = mutableListOf<GamepadAction>()
        val overlayOpen = mutableStateOf(false)
        showScreen {
            SettingsRow(label = "Theme", onClick = {})
            SettingsRow(label = "Sound", onClick = {})
            if (overlayOpen.value) SettingsOverlayInput { seen += it }
        }

        assertFocusedRow("Theme")
        press(GamepadAction.NAVIGATE_DOWN)
        assertFocusedRow("Sound")

        composeRule.runOnIdle { overlayOpen.value = true }
        composeRule.waitForIdle()
        press(GamepadAction.NAVIGATE_DOWN)
        press(GamepadAction.SELECT)

        assertEquals(
            "the overlay must receive the presses",
            listOf(GamepadAction.NAVIGATE_DOWN, GamepadAction.SELECT),
            seen,
        )
        assertFocusedRow("Sound")

        composeRule.runOnIdle { overlayOpen.value = false }
        composeRule.waitForIdle()
        press(GamepadAction.NAVIGATE_UP)
        assertFocusedRow("Theme")
        assertEquals("a closed overlay must not keep receiving presses", 2, seen.size)
    }

    @Test
    fun `an open overlay is not bypassed by the screen's own button handling`() {
        val seen = mutableListOf<GamepadAction>()
        val intercepted = mutableListOf<GamepadAction>()
        val overlayOpen = mutableStateOf(false)
        showScreen(onInterceptAction = { intercepted += it; true }) {
            SettingsRow(label = "Sound", onClick = {})
            if (overlayOpen.value) SettingsOverlayInput { seen += it }
        }

        composeRule.runOnIdle { overlayOpen.value = true }
        composeRule.waitForIdle()
        press(GamepadAction.OPEN_CONTEXT_MENU)

        assertEquals("the prompt must get the press", listOf(GamepadAction.OPEN_CONTEXT_MENU), seen)
        assertEquals("the screen behind the prompt must not act on it", emptyList<GamepadAction>(), intercepted)

        composeRule.runOnIdle { overlayOpen.value = false }
        composeRule.waitForIdle()
        press(GamepadAction.OPEN_CONTEXT_MENU)
        assertEquals("with the prompt gone the screen handles its own buttons again", listOf(GamepadAction.OPEN_CONTEXT_MENU), intercepted)
    }

    @Test
    fun `a picker opened by touch is driven by the controller`() {
        var backs = 0
        val picked = mutableListOf<Int>()
        showScreen(onBack = { backs++ }, screenId = "settings_library") {
            SettingsPickerRow(
                label = "Speed",
                options = listOf(SettingsPickerOption("Slow"), SettingsPickerOption("Fast")),
                selectedIndex = 0,
                onPick = { picked += it },
            )
        }

        composeRule.onNodeWithText("Speed").performClick()
        composeRule.waitForIdle()
        press(GamepadAction.BACK)
        assertEquals("BACK must close the picker, not leave the screen", 0, backs)

        composeRule.onNodeWithText("Speed").performClick()
        composeRule.waitForIdle()
        press(GamepadAction.SELECT)
        assertEquals("SELECT must pick from the picker the finger opened", listOf(0), picked)
    }

    @Test
    fun `controller focus lands on headers, rows and text fields in order`() {
        var themeSelects = 0
        var deleteSelects = 0
        var backCount = 0
        showScreen(onBack = { backCount++ }) {
            SettingsGroup("Appearance")
            SettingsRow(
                label = "Theme",
                onClick = { themeSelects++ },
                actions = listOf(
                    SettingsRowAction(label = "Delete theme", onClick = { deleteSelects++ }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete theme")
                    },
                ),
            )
            SettingsValueRow(label = "Version", value = "1.0")
            SettingsTextFieldRow(label = "Folder", value = "/roms", onValueChange = {})
        }

        assertFocusedRow("Theme")

        press(GamepadAction.NAVIGATE_UP)
        assertFocusedRow("Theme")

        press(GamepadAction.NAVIGATE_DOWN)
        assertFocusedRow("Version")
        press(GamepadAction.NAVIGATE_DOWN)
        assertFocusedRow("/roms")

        press(GamepadAction.NAVIGATE_DOWN)
        assertFocusedRow("/roms")

        press(GamepadAction.NAVIGATE_UP)
        press(GamepadAction.NAVIGATE_UP)
        assertFocusedRow("Theme")
        press(GamepadAction.SELECT)
        assertEquals(1, themeSelects)

        press(GamepadAction.NAVIGATE_RIGHT)
        composeRule.onNode(isFocused()).assert(hasContentDescription("Delete theme"))
        press(GamepadAction.SELECT)
        assertEquals(1, deleteSelects)
        assertEquals(1, themeSelects)

        press(GamepadAction.NAVIGATE_LEFT)
        assertFocusedRow("Theme")

        press(GamepadAction.NAVIGATE_DOWN)
        assertFocusedRow("Version")
        press(GamepadAction.SELECT)
        assertEquals(1, themeSelects)

        press(GamepadAction.BACK)
        assertEquals(1, backCount)
    }

    @Test
    fun `LEFT leaves the screen only where LEFT has nothing else to do`() {
        var backCount = 0
        var deleteSelects = 0
        showScreen(onBack = { backCount++ }) {
            SettingsRow(
                label = "Theme",
                onClick = {},
                actions = listOf(
                    SettingsRowAction(label = "Delete theme", onClick = { deleteSelects++ }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete theme")
                    },
                ),
            )
            SettingsValueRow(label = "Version", value = "1.0")
        }

        assertFocusedRow("Theme")
        press(GamepadAction.NAVIGATE_RIGHT)
        composeRule.onNode(isFocused()).assert(hasContentDescription("Delete theme"))
        press(GamepadAction.NAVIGATE_LEFT)
        assertFocusedRow("Theme")
        assertEquals(0, backCount)

        press(GamepadAction.NAVIGATE_DOWN)
        assertFocusedRow("Version")
        press(GamepadAction.NAVIGATE_LEFT)
        assertEquals(1, backCount)
        assertEquals(0, deleteSelects)
    }

    @Test
    fun `with the preference off LEFT is the no-op it always was`() {
        var backCount = 0
        showScreen(onBack = { backCount++ }, leftBacksOut = false) {
            SettingsRow(label = "Theme", onClick = {})
        }

        assertFocusedRow("Theme")
        press(GamepadAction.NAVIGATE_LEFT)
        assertEquals(0, backCount)
        assertFocusedRow("Theme")
    }

    @Test
    fun `rows that load in above the cursor take the top before anyone presses, and order stays visual`() {
        val roots = mutableStateOf(emptyList<String>())
        val consoles = mutableStateOf(emptyList<String>())
        val load = Channel<Unit>(Channel.UNLIMITED)

        composeRule.setContent {
            PFPTheme {
                LaunchedEffect(Unit) {
                    for (action in actions) pendingAction.value = action
                }
                LaunchedEffect(Unit) {
                    for (u in load) {
                        roots.value = listOf("Phone Storage")
                        consoles.value = listOf("PSP Memory Card", "SNES Memory Card")
                    }
                }
                CompositionLocalProvider(
                    LocalSettingsPendingAction provides pendingAction.value,
                    LocalSettingsActionConsumed provides { consumedPlain = true },
                ) {
                    SettingsScaffold(title = "Settings", subtitle = "Library Manager", onBack = {}) {
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                            SettingsGroup("ROM Root Access")
                            if (roots.value.isEmpty()) {
                                SettingsRow(label = "No ROM roots configured", sublabel = "Add a folder below")
                            } else {
                                roots.value.forEach { SettingsRow(label = it, onClick = {}) }
                            }
                            SettingsRow(label = "Add ROM Root", sublabel = "Grant a root folder", onClick = {})
                            SettingsGroup("Consoles")
                            if (consoles.value.isEmpty()) {
                                Text(
                                    text = "No consoles configured",
                                    color = SettingsSubtext,
                                    modifier = Modifier.padding(horizontal = 48.dp, vertical = 12.dp),
                                )
                            } else {
                                consoles.value.forEach { SettingsRow(label = it, sublabel = "console", onClick = {}) }
                            }
                            SettingsGroup("Manage")
                            SettingsRow(label = "Add Console", onClick = {})
                            SettingsRow(label = "Set Up ROM Folders", onClick = {})
                            SettingsRow(label = "Scan All Consoles", sublabel = "Configure a ROM folder first")
                        }
                    }
                }
            }
        }
        composeRule.waitForIdle()

        assertFocusedRow("Add ROM Root")

        load.trySend(Unit)
        composeRule.waitForIdle()

        assertFocusedRow("Phone Storage")
        press(GamepadAction.NAVIGATE_DOWN)
        assertFocusedRow("Add ROM Root")
        press(GamepadAction.NAVIGATE_DOWN)
        assertFocusedRow("PSP Memory Card")
        press(GamepadAction.NAVIGATE_DOWN)
        assertFocusedRow("SNES Memory Card")
        press(GamepadAction.NAVIGATE_DOWN)
        assertFocusedRow("Add Console")
        press(GamepadAction.NAVIGATE_DOWN)
        assertFocusedRow("Set Up ROM Folders")
        press(GamepadAction.NAVIGATE_DOWN)
        assertFocusedRow("Scan All Consoles")

        press(GamepadAction.NAVIGATE_DOWN)
        assertFocusedRow("Scan All Consoles")

        press(GamepadAction.NAVIGATE_UP)
        assertFocusedRow("Set Up ROM Folders")
        press(GamepadAction.NAVIGATE_UP)
        assertFocusedRow("Add Console")
        press(GamepadAction.NAVIGATE_UP)
        assertFocusedRow("SNES Memory Card")
        press(GamepadAction.NAVIGATE_UP)
        assertFocusedRow("PSP Memory Card")
        press(GamepadAction.NAVIGATE_UP)
        assertFocusedRow("Add ROM Root")
        press(GamepadAction.NAVIGATE_UP)
        assertFocusedRow("Phone Storage")

        press(GamepadAction.NAVIGATE_UP)
        assertFocusedRow("Phone Storage")
    }

    @Test
    fun `slider node steps with left right after select and back returns to navigation`() {
        var backCount = 0
        var rowSelects = 0
        val volume = mutableStateOf(0.4f)
        showScreen(onBack = { backCount++ }) {
            SettingsRow(label = "Theme", onClick = { rowSelects++ })
            SettingsSliderRow(
                label = "Volume",
                sublabel = "Test volume",
                value = volume.value,
                onValueChange = { volume.value = it },
                valueRange = 0f..1f,
                steps = 4,
                valueFormatter = { "${it}" },
            )
            SettingsRow(label = "Brightness", onClick = {})
        }

        assertFocusedRow("Theme")
        press(GamepadAction.NAVIGATE_DOWN)
        assertFocusedRow("Volume")

        press(GamepadAction.SELECT)
        press(GamepadAction.NAVIGATE_RIGHT)
        press(GamepadAction.NAVIGATE_RIGHT)
        press(GamepadAction.NAVIGATE_LEFT)
        assertEquals(0.6f, volume.value, 0.001f)
        assertFocusedRow("Volume")

        press(GamepadAction.BACK)
        assertEquals(0, backCount)

        press(GamepadAction.NAVIGATE_DOWN)
        assertFocusedRow("Brightness")

        press(GamepadAction.BACK)
        assertEquals(1, backCount)
    }

    @Test
    fun `two quick slider steps move the value two steps before the stored value catches up`() {
        val requested = mutableListOf<Float>()
        showScreen(onBack = {}) {
            SettingsRow(label = "Theme", onClick = {})
            SettingsSliderRow(
                label = "Volume",
                value = 0.4f,
                onValueChange = { requested += it },
                valueRange = 0f..1f,
                steps = 4,
            )
        }

        press(GamepadAction.NAVIGATE_DOWN)
        press(GamepadAction.SELECT)
        press(GamepadAction.NAVIGATE_RIGHT)
        press(GamepadAction.NAVIGATE_RIGHT)

        assertEquals(listOf(0.6f, 0.8f), requested.map { Math.round(it * 10) / 10f })
    }

    @Test
    fun `a continuous slider still moves with the dpad`() {
        val requested = mutableListOf<Float>()
        showScreen(onBack = {}) {
            SettingsRow(label = "Theme", onClick = {})
            SettingsSliderRow(
                label = "Volume",
                value = 0.4f,
                onValueChange = { requested += it },
                valueRange = 0f..1f,
            )
        }

        press(GamepadAction.NAVIGATE_DOWN)
        press(GamepadAction.SELECT)
        press(GamepadAction.NAVIGATE_RIGHT)

        assertTrue(requested.single() > 0.4f)
    }

    @Test
    fun `Go to your library still applies the auto-fit choice`() {
        val calls = mutableListOf<String>()
        showScreen(onBack = {}) {
            FinishPage(
                state = InitialSetupUiState(romRoots = listOf(RootFolderRow("content://roms", "ROMS", linked = true))),
                onFinishSetup = { calls += "finishSetup" },
                onOpenLibraryManager = { calls += "libraryManager" },
                onGoToLibrary = { calls += "library" },
                onFinish = { calls += "done" },
            )
        }

        assertFocusedRow("Open Library Manager")
        press(GamepadAction.SELECT)
        press(GamepadAction.NAVIGATE_DOWN)
        assertFocusedRow("Go to your library")
        press(GamepadAction.SELECT)

        assertEquals(listOf("finishSetup", "libraryManager", "finishSetup", "library"), calls)
    }

    @Test
    fun `first dpad press after a touch drag re-anchors to the viewport centre without moving`() {
        showScreen(onBack = {}) {
            val scrollState = rememberScrollState()
            LocalSettingsScrollStateRegistrar.current(scrollState)
            Column(Modifier.fillMaxSize().verticalScroll(scrollState)) {
                repeat(30) { i -> SettingsRow(label = "Row ${i + 1}", onClick = {}) }
            }
        }

        assertFocusedRow("Row 1")

        val rowHeight = composeRule.onNode(isFocused()).fetchSemanticsNode().boundsInRoot.height

        val viewportCenter = viewportCenterY()

        composeRule.onRoot().performTouchInput {
            down(center)
            moveBy(Offset(0f, -5f))
            up()
        }
        composeRule.waitForIdle()

        val nearest: Int = (1..30).minByOrNull { i: Int ->
            val centerY = composeRule.onNodeWithText("Row $i")
                .fetchSemanticsNode().boundsInRoot.center.y
            abs(centerY - (viewportCenter + rowHeight / 2f))
        }!!

        press(GamepadAction.NAVIGATE_DOWN)
        assertFocusedRow("Row $nearest")

        press(GamepadAction.NAVIGATE_DOWN)
        assertFocusedRow("Row ${(nearest + 1).coerceAtMost(30)}")
    }

    @Test
    fun `cursor never leaves the visible viewport while walking a long list`() {
        showScreen(onBack = {}) {
            val scrollState = rememberScrollState()
            LocalSettingsScrollStateRegistrar.current(scrollState)
            Column(Modifier.fillMaxSize().verticalScroll(scrollState)) {
                repeat(40) { i -> SettingsRow(label = "Row ${i + 1}", onClick = {}) }
            }
        }
        assertFocusedRow("Row 1")

        val contentTop = viewportTop()
        val contentBottom = viewportBottom()
        var rowHeight = 0f
        repeat(30) { step ->
            press(GamepadAction.NAVIGATE_DOWN)
            val bounds = composeRule.onNode(isFocused()).fetchSemanticsNode().boundsInRoot
            if (step == 0) rowHeight = bounds.height
            assertTrue("step $step: row top ${bounds.top} drifted above viewport top $contentTop", bounds.top >= contentTop - 0.5f)
            assertTrue("step $step: row bottom ${bounds.bottom} hit the viewport bottom $contentBottom", bounds.bottom <= contentBottom + 0.5f)
            assertTrue("step $step: row clipped to height ${bounds.height}", abs(bounds.height - rowHeight) < 1f)
        }
    }

    private fun viewportBounds() =
        composeRule.onNodeWithTag(SettingsContentViewportTag).fetchSemanticsNode().boundsInRoot

    private fun margin(): Float = with(composeRule.density) { CONTENT_EDGE_MARGIN.toPx() }

    private fun viewportTop(): Float = viewportBounds().top + margin()

    private fun viewportBottom(): Float = viewportBounds().bottom - margin()

    private fun viewportCenterY(): Float = viewportBounds().center.y
}
