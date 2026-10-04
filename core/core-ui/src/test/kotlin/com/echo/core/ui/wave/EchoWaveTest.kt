package com.echo.core.ui.wave

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EchoWaveTest {
    @Test
    fun `a ring is invisible when it is born and when it wraps, so rings never pop in or out`() {
        assertEquals(0f, rippleEnvelope(0f), 1e-6f)
        assertEquals(0f, rippleEnvelope(1f - 1e-6f), 1e-3f)
        assertTrue("a ring is visible halfway through its life", rippleEnvelope(0.5f) > 0.9f)
    }

    @Test
    fun `rings from one source are spread evenly through the cycle, so there is never a gap`() {
        listOf(EchoRings, EchoArcs).forEach { spec ->
            val phases = (0 until spec.rings).map { ripplePhase(3.7f, 0, it, spec) }.sorted()
            val gaps = phases.zipWithNext { a, b -> b - a } + (1f - phases.last() + phases.first())
            gaps.forEach { assertEquals(1f / spec.rings, it, 1e-4f) }
        }
    }

    @Test
    fun `only the Echo designs draw ripples, PSP keeps its strands`() {
        assertNull(WaveDesign.PSP.rippleSpec())
        assertNotNull(WaveDesign.ECHO_RINGS.rippleSpec())
        assertNotNull(WaveDesign.ECHO_ARCS.rippleSpec())
    }
}
