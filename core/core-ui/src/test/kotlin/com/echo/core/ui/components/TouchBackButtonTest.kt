package com.echo.core.ui.components

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.echo.core.domain.model.GamepadAction
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// owner, 2026-10-04: with the system bars hidden, touch had no way back
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w800dp-h480dp")
class TouchBackButtonTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `on touch the footer's Back is a button that goes back`() {
        var fired: GamepadAction? = null
        compose.setContent {
            CompositionLocalProvider(LocalPadPrompts provides false) {
                EchoHintBar(
                    items = listOf(ControllerPromptItem(GamepadAction.BACK, "Back"), ControllerPromptItem(GamepadAction.OPEN_CONTEXT_MENU, "Options")),
                    onAction = { fired = it },
                )
            }
        }
        compose.onNodeWithText("Back").performClick()
        assertEquals(GamepadAction.BACK, fired)
    }
}
