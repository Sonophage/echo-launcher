package com.echo.core.ui.theme

import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import com.echo.themekit.MotionPreset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

// owner, 2026-10-09: glide is how the crossbar's column and the Side Rail's belly settle. CLASSIC is the spring the
// crossbar used before motion presets existed, each other preset moves differently, and STILL (reduced motion) snaps
class MotionTest {
    @Test
    fun `classic glides as the crossbar always did`() {
        assertEquals(spring<Float>(dampingRatio = 1f, stiffness = GLIDE_STIFFNESS), MotionPreset.CLASSIC.glide())
    }

    @Test
    fun `each preset glides differently from classic`() {
        MotionPreset.entries.filter { it != MotionPreset.CLASSIC }.forEach {
            assertNotEquals(MotionPreset.CLASSIC.glide(), it.glide(), "$it glide")
        }
    }

    @Test
    fun `reduced motion snaps`() {
        assertEquals(snap<Float>(), MotionPreset.STILL.glide())
    }
}
