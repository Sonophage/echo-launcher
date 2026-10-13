package com.echo.core.ui.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.runtime.staticCompositionLocalOf
import com.echo.themekit.MotionPreset

// the applied theme's motion (owner, 2026-10-09); the default is ECHO's own. The one definition, so the crossbar,
// the App Drawer and the Side Rail move at the same speed. Its focus style is LocalFocusStyle
val LocalMotion = staticCompositionLocalOf { MotionPreset.CLASSIC }

const val GLIDE_STIFFNESS = 700f

// glide is how a thing travels to where it belongs: the crossbar's column, the Side Rail's belly. STILL is reduced
// motion, so it snaps
fun MotionPreset.glide(): AnimationSpec<Float> = when (this) {
    MotionPreset.CLASSIC -> spring(dampingRatio = 1f, stiffness = GLIDE_STIFFNESS)
    MotionPreset.SNAPPY -> spring(dampingRatio = 1f, stiffness = GLIDE_STIFFNESS * 3)
    MotionPreset.SOFT -> spring(dampingRatio = 1f, stiffness = GLIDE_STIFFNESS / 3)
    MotionPreset.STILL -> snap()
}
