package com.echo.feature.crossbar.gamepad

import android.view.KeyEvent
import android.view.MotionEvent
import app.cash.turbine.test
import com.echo.core.data.repository.ControllerRegistry
import com.echo.core.data.repository.RemapCoordinator
import com.echo.core.domain.model.GamepadAction
import com.echo.core.domain.model.GamepadBinding
import com.echo.core.domain.model.GamepadMappings
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GamepadInputHandlerTest {
    private lateinit var handler: GamepadInputHandler
    private val remapCoordinator = RemapCoordinator()

    @Before
    fun setUp() {
        handler = GamepadInputHandler(remapCoordinator, ControllerRegistry(mockk(relaxed = true)))

        handler.clock = { 0L }
    }

    @Test
    fun `onKeyEvent emits SELECT for BUTTON_A down`() = runTest {
        handler.actions.test {
            assertTrue(handler.onKeyEvent(keyEvent(KeyEvent.KEYCODE_BUTTON_A, KeyEvent.ACTION_DOWN)))
            assertEquals(GamepadAction.SELECT, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onKeyEvent emits BACK for BUTTON_B down`() = runTest {
        handler.actions.test {
            handler.onKeyEvent(keyEvent(KeyEvent.KEYCODE_BUTTON_B, KeyEvent.ACTION_DOWN))
            assertEquals(GamepadAction.BACK, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onKeyEvent emits NAVIGATE_UP for DPAD_UP`() = runTest {
        handler.actions.test {
            handler.onKeyEvent(keyEvent(KeyEvent.KEYCODE_DPAD_UP, KeyEvent.ACTION_DOWN))
            assertEquals(GamepadAction.NAVIGATE_UP, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onKeyEvent emits NAVIGATE_DOWN for DPAD_DOWN`() = runTest {
        handler.actions.test {
            handler.onKeyEvent(keyEvent(KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.ACTION_DOWN))
            assertEquals(GamepadAction.NAVIGATE_DOWN, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onKeyEvent emits NAVIGATE_LEFT for DPAD_LEFT`() = runTest {
        handler.actions.test {
            handler.onKeyEvent(keyEvent(KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.ACTION_DOWN))
            assertEquals(GamepadAction.NAVIGATE_LEFT, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onKeyEvent emits NAVIGATE_RIGHT for DPAD_RIGHT`() = runTest {
        handler.actions.test {
            handler.onKeyEvent(keyEvent(KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.ACTION_DOWN))
            assertEquals(GamepadAction.NAVIGATE_RIGHT, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onKeyEvent returns false for unmapped key`() {
        assertFalse(handler.onKeyEvent(keyEvent(KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.ACTION_DOWN)))
    }

    @Test
    fun `ACTION_UP does not emit an action`() = runTest {
        handler.actions.test {
            handler.onKeyEvent(keyEvent(KeyEvent.KEYCODE_BUTTON_A, KeyEvent.ACTION_DOWN))
            awaitItem()
            handler.onKeyEvent(keyEvent(KeyEvent.KEYCODE_BUTTON_A, KeyEvent.ACTION_UP))
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `A coming up ends a hold-to-launch, and only A does`() = runTest {
        handler.selectReleases.test {
            handler.onKeyEvent(keyEvent(KeyEvent.KEYCODE_BUTTON_B, KeyEvent.ACTION_UP))
            expectNoEvents()
            handler.onKeyEvent(keyEvent(KeyEvent.KEYCODE_BUTTON_A, KeyEvent.ACTION_UP))
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `held key repeat does not re-emit`() = runTest {
        handler.actions.test {
            handler.onKeyEvent(keyEvent(KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.ACTION_DOWN))
            assertEquals(GamepadAction.NAVIGATE_DOWN, awaitItem())

            val repeat = keyEvent(KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.ACTION_DOWN, repeatCount = 1)
            assertTrue(handler.onKeyEvent(repeat))
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `analog stick below dead zone does not emit`() = runTest {
        handler.actions.test {
            handler.onMotionEvent(motionEvent(axisX = 0.3f, axisY = 0.0f))
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `analog stick right emits NAVIGATE_RIGHT`() = runTest {
        handler.actions.test {
            handler.onMotionEvent(motionEvent(axisX = 0.8f, axisY = 0.0f))
            assertEquals(GamepadAction.NAVIGATE_RIGHT, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `analog stick left emits NAVIGATE_LEFT`() = runTest {
        handler.actions.test {
            handler.onMotionEvent(motionEvent(axisX = -0.8f, axisY = 0.0f))
            assertEquals(GamepadAction.NAVIGATE_LEFT, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `analog stick up emits NAVIGATE_UP`() = runTest {
        handler.actions.test {
            handler.onMotionEvent(motionEvent(axisX = 0.0f, axisY = -0.8f))
            assertEquals(GamepadAction.NAVIGATE_UP, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `analog stick down emits NAVIGATE_DOWN`() = runTest {
        handler.actions.test {
            handler.onMotionEvent(motionEvent(axisX = 0.0f, axisY = 0.8f))
            assertEquals(GamepadAction.NAVIGATE_DOWN, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `stick returning to neutral emits nothing and releases`() = runTest {
        handler.actions.test {
            assertTrue(handler.onMotionEvent(motionEvent(axisX = 0.8f, axisY = 0.0f)))
            assertEquals(GamepadAction.NAVIGATE_RIGHT, awaitItem())
            assertFalse(handler.onMotionEvent(motionEvent(axisX = 0.0f, axisY = 0.0f)))
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `stick stays engaged above the release threshold`() = runTest {
        handler.actions.test {
            assertTrue(handler.onMotionEvent(motionEvent(axisX = 0.6f, axisY = 0.0f)))
            assertEquals(GamepadAction.NAVIGATE_RIGHT, awaitItem())

            assertTrue(handler.onMotionEvent(motionEvent(axisX = 0.4f, axisY = 0.0f)))
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `stick below the release threshold disengages`() = runTest {
        handler.actions.test {
            handler.onMotionEvent(motionEvent(axisX = 0.6f, axisY = 0.0f))
            assertEquals(GamepadAction.NAVIGATE_RIGHT, awaitItem())
            assertFalse(handler.onMotionEvent(motionEvent(axisX = 0.2f, axisY = 0.0f)))
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `stick below activation does not re-engage after release`() = runTest {
        handler.actions.test {
            handler.onMotionEvent(motionEvent(axisX = 0.6f, axisY = 0.0f))
            assertEquals(GamepadAction.NAVIGATE_RIGHT, awaitItem())
            handler.onMotionEvent(motionEvent(axisX = 0.2f, axisY = 0.0f))
            handler.onMotionEvent(motionEvent(axisX = 0.4f, axisY = 0.0f))
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `HAT right emits NAVIGATE_RIGHT`() = runTest {
        handler.actions.test {
            assertTrue(handler.onMotionEvent(motionEvent(hatX = 1f, hatY = 0f)))
            assertEquals(GamepadAction.NAVIGATE_RIGHT, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `HAT up emits NAVIGATE_UP`() = runTest {
        handler.actions.test {
            handler.onMotionEvent(motionEvent(hatX = 0f, hatY = -1f))
            assertEquals(GamepadAction.NAVIGATE_UP, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `HAT release emits nothing`() = runTest {
        handler.actions.test {
            handler.onMotionEvent(motionEvent(hatX = 1f, hatY = 0f))
            assertEquals(GamepadAction.NAVIGATE_RIGHT, awaitItem())
            handler.onMotionEvent(motionEvent(hatX = 0f, hatY = 0f))
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `DPAD press matching a held stick direction inside the window is suppressed`() = runTest {
        var now = 0L
        handler.clock = { now }
        handler.actions.test {
            handler.onMotionEvent(motionEvent(axisX = 0.8f, axisY = 0.0f))
            assertEquals(GamepadAction.NAVIGATE_RIGHT, awaitItem())
            now = 50

            assertTrue(handler.onKeyEvent(keyEvent(KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.ACTION_DOWN)))
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `same direction from a new source after the window is accepted`() = runTest {
        var now = 0L
        handler.clock = { now }
        handler.actions.test {
            handler.onMotionEvent(motionEvent(axisX = 0.8f, axisY = 0.0f))
            assertEquals(GamepadAction.NAVIGATE_RIGHT, awaitItem())
            now = 200
            handler.onMotionEvent(motionEvent(axisX = 0.0f, axisY = 0.0f))
            expectNoEvents()
            now = 250

            assertTrue(handler.onKeyEvent(keyEvent(KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.ACTION_DOWN)))
            assertEquals(GamepadAction.NAVIGATE_RIGHT, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `remapped binding overrides default action`() = runTest {
        handler.currentMappings = GamepadMappings(
            bindings = listOf(GamepadBinding(KeyEvent.KEYCODE_BUTTON_A, GamepadAction.BACK))
        )
        handler.actions.test {
            handler.onKeyEvent(keyEvent(KeyEvent.KEYCODE_BUTTON_A, KeyEvent.ACTION_DOWN))
            assertEquals(GamepadAction.BACK, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `bypassToComposeFocus lets non-BACK fall through but keeps BACK`() = runTest {
        handler.bypassToComposeFocus = true
        handler.actions.test {
            assertFalse(handler.onKeyEvent(keyEvent(KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.ACTION_DOWN)))
            assertTrue(handler.onKeyEvent(keyEvent(KeyEvent.KEYCODE_BUTTON_B, KeyEvent.ACTION_DOWN)))
            assertEquals(GamepadAction.BACK, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `capture mode consumes the key without emitting its mapped action`() = runTest {
        handler.actions.test {
            var captured: Int? = null
            remapCoordinator.captureNextKey = { captured = it }
            assertTrue(handler.onKeyEvent(keyEvent(KeyEvent.KEYCODE_BUTTON_A, KeyEvent.ACTION_DOWN)))
            assertEquals(KeyEvent.KEYCODE_BUTTON_A, captured)
            assertNull(remapCoordinator.captureNextKey)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun keyEvent(keyCode: Int, action: Int, repeatCount: Int = 0): KeyEvent {
        val event = mockk<KeyEvent>(relaxed = true)
        every { event.action }      returns action
        every { event.keyCode }     returns keyCode
        every { event.repeatCount } returns repeatCount
        return event
    }

    @Test
    fun `the axes keep switching tabs after a key event, because the pad must not latch off`() = runTest {
        handler.actions.test {
            handler.onMotionEvent(motionEvent(rTrigger = 1f))
            handler.onMotionEvent(motionEvent(rTrigger = 0f))
            assertEquals(GamepadAction.NEXT_CATEGORY, awaitItem())

            handler.onKeyEvent(keyEvent(KeyEvent.KEYCODE_BUTTON_R2, KeyEvent.ACTION_DOWN))
            handler.onKeyEvent(keyEvent(KeyEvent.KEYCODE_BUTTON_R2, KeyEvent.ACTION_UP))
            assertEquals(GamepadAction.NEXT_CATEGORY, awaitItem())

            handler.onMotionEvent(motionEvent(rTrigger = 1f))
            handler.onMotionEvent(motionEvent(rTrigger = 0f))
            assertEquals("the axes must still switch after a key event", GamepadAction.NEXT_CATEGORY, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a pad that sends a trigger as key AND axis turns one tab, not two`() = runTest {
        handler.actions.test {
            handler.onKeyEvent(keyEvent(KeyEvent.KEYCODE_BUTTON_L2, KeyEvent.ACTION_DOWN))
            handler.onMotionEvent(motionEvent(lTrigger = 1f))
            handler.onKeyEvent(keyEvent(KeyEvent.KEYCODE_BUTTON_L2, KeyEvent.ACTION_UP))
            handler.onMotionEvent(motionEvent(lTrigger = 0f))

            assertEquals(GamepadAction.PREV_CATEGORY, awaitItem())
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `an axis release does not end a press the key still holds`() = runTest {
        handler.actions.test {
            handler.onKeyEvent(keyEvent(KeyEvent.KEYCODE_BUTTON_R2, KeyEvent.ACTION_DOWN))
            handler.onMotionEvent(motionEvent(rTrigger = 1f))
            handler.onMotionEvent(motionEvent(rTrigger = 0f))
            expectNoEvents()
            handler.onKeyEvent(keyEvent(KeyEvent.KEYCODE_BUTTON_R2, KeyEvent.ACTION_UP))
            assertEquals(GamepadAction.NEXT_CATEGORY, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `the left trigger switches the other way`() = runTest {
        handler.actions.test {
            handler.onMotionEvent(motionEvent(lTrigger = 1f))
            handler.onMotionEvent(motionEvent(lTrigger = 0f))
            assertEquals(GamepadAction.PREV_CATEGORY, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `the bumpers take the paging the triggers gave up, LB being Search at the crossbar`() = runTest {
        handler.actions.test {
            handler.onKeyEvent(keyEvent(KeyEvent.KEYCODE_BUTTON_L1, KeyEvent.ACTION_DOWN))
            assertEquals(GamepadAction.PREV_PAGE, awaitItem())
            handler.onKeyEvent(keyEvent(KeyEvent.KEYCODE_BUTTON_R1, KeyEvent.ACTION_DOWN))
            assertEquals(GamepadAction.NEXT_PAGE, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun motionEvent(
        axisX: Float = 0f,
        axisY: Float = 0f,
        hatX: Float = 0f,
        hatY: Float = 0f,
        lTrigger: Float = 0f,
        rTrigger: Float = 0f,
    ): MotionEvent {
        val event = mockk<MotionEvent>(relaxed = true)

        every { event.action } returns MotionEvent.ACTION_MOVE
        every { event.getAxisValue(MotionEvent.AXIS_X) } returns axisX
        every { event.getAxisValue(MotionEvent.AXIS_Y) } returns axisY
        every { event.getAxisValue(MotionEvent.AXIS_HAT_X) } returns hatX
        every { event.getAxisValue(MotionEvent.AXIS_HAT_Y) } returns hatY
        every { event.getAxisValue(MotionEvent.AXIS_LTRIGGER) } returns lTrigger
        every { event.getAxisValue(MotionEvent.AXIS_RTRIGGER) } returns rTrigger
        every { event.getAxisValue(MotionEvent.AXIS_BRAKE) } returns 0f
        every { event.getAxisValue(MotionEvent.AXIS_GAS) } returns 0f
        every { event.source } returns android.view.InputDevice.SOURCE_JOYSTICK
        return event
    }
}
