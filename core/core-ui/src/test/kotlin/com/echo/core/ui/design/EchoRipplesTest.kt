package com.echo.core.ui.design

import com.echo.core.ui.wave.rippleEnvelope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// owner, 2026-10-06: echo rings spread from both orbs, the brand's ripple in small
class EchoRipplesTest {
    @Test
    fun `the rings are spread evenly through a period, never bunched`() {
        val phases = (0 until 3).map { echoRipplePhase(0.1f, it, 3) }
        assertEquals(0.1f, phases[0], 1e-5f)
        assertEquals(0.1f + 1f / 3, phases[1], 1e-5f)
        assertEquals("the last wraps round", 0.1f + 2f / 3, phases[2], 1e-5f)
        assertEquals("a ring past the end starts again at the orb", 0.9f + 1f / 3 - 1f, echoRipplePhase(0.9f, 1, 3), 1e-5f)
        phases.forEach { assertTrue(it in 0f..1f) }
    }

    @Test
    fun `a ring is born and dies invisible, so none pops in or out`() {
        assertEquals(0f, rippleEnvelope(0f), 1e-5f)
        assertEquals(0f, rippleEnvelope(1f), 1e-4f)
        assertTrue(rippleEnvelope(0.5f) > 0.9f)
    }
}
