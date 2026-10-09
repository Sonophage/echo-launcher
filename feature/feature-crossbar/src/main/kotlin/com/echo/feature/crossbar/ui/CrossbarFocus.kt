package com.echo.feature.crossbar.ui

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.echo.themekit.FocusStyle
import com.echo.themekit.MotionPreset

// the applied theme's focus style and motion (owner, 2026-10-09); the defaults are ECHO's own
val LocalFocusStyle = staticCompositionLocalOf { FocusStyle.CLASSIC }
val LocalCrossbarMotion = staticCompositionLocalOf { MotionPreset.CLASSIC }

// a row's size, focused and not. CLASSIC's numbers are the ones the crossbar always drew
internal fun FocusStyle.rowScale(selected: Boolean): Float = when (this) {
    FocusStyle.CLASSIC -> if (selected) 1.06f else 0.9f
    FocusStyle.LIFT -> if (selected) 1.14f else 0.88f
    FocusStyle.HALO, FocusStyle.BRACKET -> if (selected) 1f else 0.94f
}

// LIFT pushes what is not focused further back
internal fun FocusStyle.restAlpha(alpha: Float): Float = if (this == FocusStyle.LIFT) alpha * 0.6f else alpha

internal fun FocusStyle.categoryScale(selected: Boolean): Float = if (this == FocusStyle.LIFT && selected) 1.18f else 1f

// the warm glow behind a focused icon is CLASSIC's own mark
internal val FocusStyle.glows: Boolean get() = this == FocusStyle.CLASSIC

// pop is a row's size, fade its brightness and mark, glide the column. CLASSIC's are the springs the crossbar
// always used
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

internal fun MotionPreset.glide(): AnimationSpec<Float> = when (this) {
    MotionPreset.CLASSIC -> spring(dampingRatio = 1f, stiffness = GLIDE_STIFFNESS)
    MotionPreset.SNAPPY -> spring(dampingRatio = 1f, stiffness = GLIDE_STIFFNESS * 3)
    MotionPreset.SOFT -> spring(dampingRatio = 1f, stiffness = GLIDE_STIFFNESS / 3)
    MotionPreset.STILL -> snap()
}

// the mark a style draws round a focused icon, faded by [visible]: HALO an accent ring with a glow, BRACKET four
// accent corners, LIFT a shadow under it. CLASSIC draws its glow through crossbarFocusGlow
internal fun Modifier.focusMark(style: FocusStyle, visible: Float, accent: Color): Modifier = drawBehind {
    if (visible <= 0f) return@drawBehind
    val stroke = 2.dp.toPx()
    when (style) {
        FocusStyle.HALO -> {
            val r = size.minDimension / 2f * 0.98f
            drawCircle(
                brush = Brush.radialGradient(
                    0.70f to accent.copy(alpha = 0f),
                    0.82f to accent.copy(alpha = 0.32f * visible),
                    1.00f to accent.copy(alpha = 0f),
                    center = center,
                    radius = r * 1.35f,
                ),
                radius = r * 1.35f,
            )
            drawCircle(accent.copy(alpha = visible), radius = r, style = Stroke(stroke))
        }
        FocusStyle.BRACKET -> {
            val c = accent.copy(alpha = visible)
            val len = size.minDimension * 0.24f
            val w = size.width
            val h = size.height
            listOf(
                Offset(0f, 0f) to Offset(1f, 1f), Offset(w, 0f) to Offset(-1f, 1f),
                Offset(0f, h) to Offset(1f, -1f), Offset(w, h) to Offset(-1f, -1f),
            ).forEach { (corner, dir) ->
                drawLine(c, corner, corner + Offset(dir.x * len, 0f), stroke, StrokeCap.Square)
                drawLine(c, corner, corner + Offset(0f, dir.y * len), stroke, StrokeCap.Square)
            }
        }
        FocusStyle.LIFT -> {
            val r = size.minDimension * 0.75f
            val below = center + Offset(0f, size.height * 0.14f)
            drawCircle(
                brush = Brush.radialGradient(
                    0f to Color.Black.copy(alpha = 0.55f * visible),
                    1f to Color.Black.copy(alpha = 0f),
                    center = below,
                    radius = r,
                ),
                radius = r,
                center = below,
            )
        }
        FocusStyle.CLASSIC -> Unit
    }
}
