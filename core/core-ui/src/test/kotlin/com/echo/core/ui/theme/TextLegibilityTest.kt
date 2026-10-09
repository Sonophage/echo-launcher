package com.echo.core.ui.theme

import androidx.compose.ui.graphics.Color
import com.echo.themekit.ColorCascade
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextLegibilityTest {
    private val classicBlueWave = 0xFF0055AAL

    private fun argb(v: Long) = Color(v.toInt())

    private fun greyAtLuminance(target: Double): Color {
        var lo = 0f
        var hi = 1f
        repeat(24) {
            val mid = (lo + hi) / 2f
            if (relativeLuminance(Color(mid, mid, mid)) < target) lo = mid else hi = mid
        }
        return Color(hi, hi, hi)
    }

    @Test
    fun `the raw bottom gradient anchor cannot carry white text`() {
        val bottom = argb(ColorCascade.lightBackgroundAnchors(classicBlueWave).second)
        assertTrue(
            "lighten(wave, 0.28) is meant to be a bright anchor; if this ever passes 4.5:1 on its " +
                "own the scrim solve below has stopped being load-bearing and should be revisited",
            contrastRatio(Color.White, bottom) < 4.5,
        )
    }

    @Test
    fun `solved scrim anchors clear AA over a worst-case white wallpaper`() {
        val (top, bottom) = ColorCascade.lightBackgroundAnchors(classicBlueWave)
        val (solvedTop, solvedBottom) = crossbarScrimAnchors(argb(top), argb(bottom))

        val topBg = composite(solvedTop, Color.White)
        val bottomBg = composite(solvedBottom, Color.White)

        assertTrue(contrastRatio(Color.White, topBg) >= 4.5)
        assertTrue(contrastRatio(Color.White, bottomBg) >= 4.5)
    }

    @Test
    fun `the scrim solve keeps the theme hue instead of collapsing to neutral`() {
        val bottom = argb(ColorCascade.lightBackgroundAnchors(classicBlueWave).second)
        val solved = solveScrimColor(bottom, alpha = 0.90f)

        assertTrue(solved.blue > solved.green)
        assertTrue(solved.green > solved.red)
        assertTrue("should not have bottomed out at black", solved.blue > 0.15f)
    }

    @Test
    fun `an already dark anchor is returned untouched`() {
        val deep = Color(0xFF0A0F1A)
        assertEquals(deep, solveScrimColor(deep, alpha = 0.90f))
    }

    @Test
    fun `composite blends in sRGB channel space`() {
        val half = composite(Color.Black.copy(alpha = 0.5f), Color.White)
        assertEquals(0.5f, half.red, 1f / 255f)
        assertEquals(0.5f, half.green, 1f / 255f)
        assertEquals(0.5f, half.blue, 1f / 255f)
    }

    @Test
    fun `body text is held to AA and only large text drops to three`() {
        assertEquals(4.5f, TextContrastRole.BODY.threshold, 0f)
        assertEquals(3.0f, TextContrastRole.LARGE.threshold, 0f)
    }

    @Test
    fun `bestPolarity flips at the crossover`() {
        assertEquals(Color.White, bestPolarity(Color(0xFF101010)))
        assertEquals(Color.Black, bestPolarity(Color(0xFFF0F0F0)))
    }
}
