package com.psplauncher.feature.xmb.gamepad

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import com.psplauncher.core.domain.model.TriggerSensitivity

class TriggerEdgeTest {
    private val STANDARD = TriggerSensitivity.STANDARD

    @Test
    fun `a trigger only counts as pressed once it is pulled most of the way`() {
        assertFalse("a resting trigger is not a press", triggerDown(wasDown = false, value = 0f))
        assertFalse("a brushed trigger is not a press", triggerDown(wasDown = false, value = 0.5f))
        assertTrue(triggerDown(wasDown = false, value = STANDARD.press))
        assertTrue(triggerDown(wasDown = false, value = 1f))
    }

    @Test
    fun `it stays down until it is well clear of the press point, so one pull is not two pages`() {
        assertTrue("still held just under the press point", triggerDown(wasDown = true, value = 0.55f))
        assertTrue(triggerDown(wasDown = true, value = STANDARD.release + 0.01f))
        assertFalse(triggerDown(wasDown = true, value = STANDARD.release))
        assertFalse(triggerDown(wasDown = true, value = 0f))
    }

    @Test
    fun `every sensitivity releases below where it presses, or the gap does nothing`() {
        TriggerSensitivity.entries.forEach {
            assertTrue("${it.name} release must be below its press", it.release < it.press)
        }
    }

    @Test
    fun `High turns a page on a short pull that Standard ignores, and Low needs more than Standard`() {
        assertTrue(triggerDown(wasDown = false, value = 0.4f, sensitivity = TriggerSensitivity.HIGH))
        assertFalse(triggerDown(wasDown = false, value = 0.4f, sensitivity = STANDARD))
        assertFalse(triggerDown(wasDown = false, value = 0.7f, sensitivity = TriggerSensitivity.LOW))
        assertTrue(triggerDown(wasDown = false, value = 0.7f, sensitivity = STANDARD))
    }

    @Test
    fun `one slow pull and release fires exactly once`() {
        val pull = listOf(0f, 0.2f, 0.45f, 0.58f, 0.62f, 0.8f, 1f, 0.7f, 0.4f, 0.31f, 0.2f, 0f)
        TriggerSensitivity.entries.forEach { sensitivity ->
            var down = false
            var fires = 0
            pull.forEach { v ->
                val now = triggerDown(down, v, sensitivity)
                if (now && !down) fires++
                down = now
            }
            assertEquals("${sensitivity.name}: a pull and release is one page turn, not several", 1, fires)
            assertFalse("${sensitivity.name}: the trigger ends released", down)
        }
    }
}
