package com.echo.core.ui.components

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import com.echo.core.domain.model.GamepadAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// owner, 2026-10-05: on the tablet the A orb was a pill, not a circle. At this size the design unit is 1,
// so the 44-unit orb is taller than the 34dp bar and was squeezed to fit it
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w1200dp-h752dp")
class RoundOrbTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `the A orb stays round when it is taller than the bar`() {
        compose.setContent {
            CompositionLocalProvider(LocalPadPrompts provides false) {
                EchoHintBar(items = emptyList(), onAction = {}, primary = HintAction(GamepadAction.SELECT, "Open"))
            }
        }
        val orb = compose.onNode(hasClickAction()).getUnclippedBoundsInRoot()
        assertEquals(44f, orb.width.value, 0.5f)
        assertEquals(orb.width.value, orb.height.value, 0.5f)
    }

    @Test
    fun `the touch Back button keeps its full touch height`() {
        compose.setContent {
            CompositionLocalProvider(LocalPadPrompts provides false) {
                EchoHintBar(items = listOf(ControllerPromptItem(GamepadAction.BACK, "Back")), onAction = {})
            }
        }
        val back = compose.onNode(hasClickAction()).getUnclippedBoundsInRoot()
        assertTrue("Back is ${back.height}", back.height >= 48.dp)
    }

    // owner, 2026-10-05: a bigger footer grows its row and its orb together, and the orb stays round
    @Test
    fun `a bigger footer grows the bar and the orb with it`() {
        compose.setContent {
            CompositionLocalProvider(LocalPadPrompts provides false, LocalChromeScale provides ChromeScale(footer = 1.25f)) {
                EchoHintBar(items = emptyList(), onAction = {}, primary = HintAction(GamepadAction.SELECT, "Open"))
            }
        }
        val orb = compose.onNode(hasClickAction()).getUnclippedBoundsInRoot()
        assertEquals(55f, orb.width.value, 0.5f)
        assertEquals(orb.width.value, orb.height.value, 0.5f)
    }
}
