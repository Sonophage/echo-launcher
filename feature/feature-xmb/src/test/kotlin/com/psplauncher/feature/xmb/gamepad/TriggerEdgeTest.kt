package com.psplauncher.feature.xmb.gamepad

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TriggerEdgeTest {
    @Test
    fun `a trigger only counts as pressed once it is pulled most of the way`() {
        assertFalse("a resting trigger is not a press", triggerDown(wasDown = false, value = 0f))
        assertFalse("a brushed trigger is not a press", triggerDown(wasDown = false, value = 0.5f))
        assertTrue(triggerDown(wasDown = false, value = TRIGGER_PRESS))
        assertTrue(triggerDown(wasDown = false, value = 1f))
    }

    @Test
    fun `it stays down until it is well clear of the press point, so one pull is not two pages`() {
        // an analog trigger dithers around its value; without the gap it would
        // cross the press point repeatedly on a single pull.
        assertTrue("still held just under the press point", triggerDown(wasDown = true, value = 0.55f))
        assertTrue(triggerDown(wasDown = true, value = TRIGGER_RELEASE + 0.01f))
        assertFalse(triggerDown(wasDown = true, value = TRIGGER_RELEASE))
        assertFalse(triggerDown(wasDown = true, value = 0f))
    }

    @Test
    fun `the release point sits below the press point, or the gap does nothing`() {
        assertTrue("TRIGGER_RELEASE must be below TRIGGER_PRESS", TRIGGER_RELEASE < TRIGGER_PRESS)
    }

    @Test
    fun `one slow pull and release fires exactly once`() {
        val pull = listOf(0f, 0.2f, 0.45f, 0.58f, 0.62f, 0.8f, 1f, 0.7f, 0.4f, 0.31f, 0.2f, 0f)
        var down = false
        var fires = 0
        pull.forEach { v ->
            val now = triggerDown(down, v)
            if (now && !down) fires++
            down = now
        }
        assertEquals("a pull and release is one page turn, not several", 1, fires)
        assertFalse("the trigger ends released", down)
    }
}
