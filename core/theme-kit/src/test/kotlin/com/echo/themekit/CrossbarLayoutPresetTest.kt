package com.echo.themekit

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CrossbarLayoutPresetTest {
    private fun assertClose(expected: Float, actual: Float, tolerance: Float = 0.002f) {
        assertTrue(
            abs(expected - actual) <= tolerance,
            "expected $actual to be within $tolerance of $expected",
        )
    }

    @Test
    fun `Thor values survive the codec sanitize gate unchanged`() {
        val preset = CrossbarLayoutPreset.computeForWindow(
            widthPx = 1920f, heightPx = 1080f, densityDpi = 369f,
        )
        val sanitized = CrossbarLayoutAdjustCodec.sanitize(preset)

        assertEquals(preset, sanitized)
    }

    @Test
    fun `dp entry point matches the px entry point for the Thor window`() {
        val fromPx = CrossbarLayoutPreset.computeForWindow(
            widthPx = 1920f, heightPx = 1080f, densityDpi = 369f,
        )
        val fromDp = CrossbarLayoutPreset.computeForWindowDp(
            widthDp = 832.5f, heightDp = 468.3f, density = 369f / 160f,
        )

        assertClose(fromPx.scale, fromDp.scale)
        assertClose(fromPx.barLeftFraction, fromDp.barLeftFraction)
        assertClose(fromPx.barTopFraction, fromDp.barTopFraction)
    }

    @Test
    fun `computeForWindowDp normalizes portrait input to landscape`() {
        val landscape = CrossbarLayoutPreset.computeForWindowDp(832.5f, 468.3f, 369f / 160f)
        val portrait = CrossbarLayoutPreset.computeForWindowDp(468.3f, 832.5f, 369f / 160f)

        assertEquals(landscape, portrait)
    }

    @Test
    fun `a saved preset still matches after the prefs round trip`() {
        val preset = CrossbarLayoutPreset.computeForWindow(widthPx = 1920f, heightPx = 1080f, densityDpi = 369f)
        val saved = CrossbarLayoutAdjustCodec.decode(CrossbarLayoutAdjustCodec.encode(mapOf("compact" to preset)))["compact"]

        assertTrue(CrossbarLayoutPreset.matches(saved, preset))
    }

    @Test
    fun `one editor step, a reset to default or no saved layout is not the preset`() {
        val preset = CrossbarLayoutPreset.computeForWindow(widthPx = 1920f, heightPx = 1080f, densityDpi = 369f)

        assertFalse(CrossbarLayoutPreset.matches(preset.copy(barLeftFraction = preset.barLeftFraction + 0.01f), preset))
        assertFalse(CrossbarLayoutPreset.matches(preset.copy(barTopFraction = preset.barTopFraction - 0.01f), preset))
        assertFalse(CrossbarLayoutPreset.matches(preset.copy(scale = preset.scale + 0.02f), preset))
        assertFalse(CrossbarLayoutPreset.matches(CrossbarLayoutAdjust.DEFAULT, preset))
        assertFalse(CrossbarLayoutPreset.matches(null, preset))
    }
}
