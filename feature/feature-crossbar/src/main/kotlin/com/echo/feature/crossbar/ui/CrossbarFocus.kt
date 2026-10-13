package com.echo.feature.crossbar.ui

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import com.echo.themekit.FocusStyle
import com.echo.themekit.MotionPreset

// a row's size, focused and not. CLASSIC's numbers are the ones the crossbar always drew
internal fun FocusStyle.rowScale(selected: Boolean): Float = when (this) {
    FocusStyle.CLASSIC -> if (selected) 1.06f else 0.9f
    FocusStyle.LIFT -> if (selected) 1.14f else 0.88f
    FocusStyle.HALO, FocusStyle.BRACKET -> if (selected) 1f else 0.94f
}

// LIFT pushes what is not focused further back
internal fun FocusStyle.restAlpha(alpha: Float): Float = if (this == FocusStyle.LIFT) alpha * 0.6f else alpha

internal fun FocusStyle.categoryScale(selected: Boolean): Float = if (this == FocusStyle.LIFT && selected) 1.18f else 1f

// pop is a row's size, fade its brightness and mark; glide, the column's, is in core-ui (theme/Motion.kt).
// CLASSIC's are the springs the crossbar always used
internal fun MotionPreset.pop(): AnimationSpec<Float> = when (this) {
    MotionPreset.CLASSIC -> spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessHigh)
    MotionPreset.SNAPPY -> spring(Spring.DampingRatioNoBouncy, Spring.StiffnessHigh)
    MotionPreset.SOFT -> spring(Spring.DampingRatioNoBouncy, Spring.StiffnessLow)
    MotionPreset.STILL -> snap()
}

internal fun MotionPreset.fade(): AnimationSpec<Float> = when (this) {
    MotionPreset.CLASSIC -> spring(stiffness = Spring.StiffnessMedium)
    MotionPreset.SNAPPY -> tween(90)
    MotionPreset.SOFT -> spring(stiffness = Spring.StiffnessLow)
    MotionPreset.STILL -> tween(120)
}

