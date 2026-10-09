package com.echo.feature.crossbar.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import com.echo.themekit.FocusStyle
import com.echo.themekit.MotionPreset
import com.echo.themekit.focusStyleOf
import com.echo.themekit.motionPresetOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

// owner, 2026-10-09: CLASSIC is the default for both, so without a theme the crossbar looks and moves as it
// always did. These are the numbers it used before focus styles and motion presets existed
class CrossbarFocusTest {
    @Test
    fun `classic draws and moves the crossbar as it always did`() {
        assertEquals(1.06f, FocusStyle.CLASSIC.rowScale(true))
        assertEquals(0.9f, FocusStyle.CLASSIC.rowScale(false))
        assertEquals(0.68f, FocusStyle.CLASSIC.restAlpha(0.68f))
        assertEquals(1f, FocusStyle.CLASSIC.categoryScale(true))
        assertTrue(FocusStyle.CLASSIC.glows)
        assertEquals(spring<Float>(Spring.DampingRatioMediumBouncy, Spring.StiffnessHigh), MotionPreset.CLASSIC.pop())
        assertEquals(spring<Float>(stiffness = Spring.StiffnessMedium), MotionPreset.CLASSIC.fade())
        assertEquals(spring<Float>(dampingRatio = 1f, stiffness = GLIDE_STIFFNESS), MotionPreset.CLASSIC.glide())
    }

    @Test
    fun `no theme, or a name ECHO does not know, is classic`() {
        assertEquals(FocusStyle.CLASSIC, focusStyleOf(null))
        assertEquals(FocusStyle.CLASSIC, focusStyleOf("PLATE"))
        assertEquals(MotionPreset.CLASSIC, motionPresetOf("WOBBLE"))
    }

    @Test
    fun `each preset moves differently from classic`() {
        MotionPreset.entries.filter { it != MotionPreset.CLASSIC }.forEach {
            assertNotEquals(MotionPreset.CLASSIC.pop(), it.pop(), "$it pop")
            assertNotEquals(MotionPreset.CLASSIC.glide(), it.glide(), "$it glide")
        }
    }
}
