package com.echo.core.ui.components

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performTouchInput
import com.echo.core.domain.model.GamepadAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// owner, 2026-10-04: holding the footer's bare A orb by touch did nothing on Last Played;
// the same hold on the Resume/Play card launched
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w800dp-h480dp")
class HoldToLaunchTouchTest {
    @get:Rule
    val compose = createComposeRule()

    private fun holdTheBareOrb(forMs: Long): GamepadAction? {
        var fired: GamepadAction? = null
        compose.setContent {
            EchoHintBar(
                items = emptyList(),
                onAction = { fired = it },
                primary = HintAction(GamepadAction.SELECT, "Play", "Hold to launch Sekiro", holdMs = 1000L),
            )
        }
        compose.mainClock.autoAdvance = false
        val orb = compose.onAllNodes(hasClickAction()).onFirst()
        orb.performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(forMs)
        orb.performTouchInput { up() }
        compose.mainClock.advanceTimeBy(100)
        return fired
    }

    @Test
    fun `a full hold on the bare orb launches`() =
        assertEquals(GamepadAction.SELECT, holdTheBareOrb(1_200))

    @Test
    fun `a press shorter than the hold launches nothing`() =
        assertNull(holdTheBareOrb(300))
}
