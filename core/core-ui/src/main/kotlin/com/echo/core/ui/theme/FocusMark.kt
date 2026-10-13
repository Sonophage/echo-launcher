package com.echo.core.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.echo.themekit.FocusStyle

// the applied theme's focus style (owner, 2026-10-09); the default is ECHO's own. The one definition of how a
// focused item is marked, so any screen draws the active style
val LocalFocusStyle = staticCompositionLocalOf { FocusStyle.CLASSIC }

// the warm glow behind a focused icon is CLASSIC's own mark
val FocusStyle.glows: Boolean get() = this == FocusStyle.CLASSIC

// the one decision of how a focused icon is marked: CLASSIC's warm glow, or the style's own mark. The press mark,
// the Side Rail and the crossbar rows all draw it from here (review of spec #3)
fun Modifier.focusLook(style: FocusStyle, visible: Float, accent: Color): Modifier =
    drawBehind { drawFocusLook(style, visible, accent) }

fun DrawScope.drawFocusLook(style: FocusStyle, visible: Float, accent: Color) {
    if (style.glows) drawFocusGlow(visible, FocusGlow.RowReach, FocusGlow.RowAlpha)
    else drawFocusMark(style, visible, accent)
}

// the mark a style draws round a focused icon, faded by [visible]: HALO an accent ring with a glow, BRACKET four
// accent corners, LIFT a shadow under it. CLASSIC draws its glow through focusGlow
fun Modifier.focusMark(style: FocusStyle, visible: Float, accent: Color): Modifier =
    drawBehind { drawFocusMark(style, visible, accent) }

// the mark itself, for a draw that is not a modifier (the press indication)
fun DrawScope.drawFocusMark(style: FocusStyle, visible: Float, accent: Color) {
    if (visible <= 0f) return
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

object FocusGlow {
    val Color = Color(0xFFFFDCAA)

    const val RowAlpha = 0.58f
    const val RowReach = 0.26f
}

fun Modifier.focusGlow(
    visible: Float,
    reach: Float,
    alpha: Float,
): Modifier = drawBehind { drawFocusGlow(visible, reach, alpha) }

fun DrawScope.drawFocusGlow(visible: Float, reach: Float, alpha: Float) {
    if (visible <= 0f || alpha <= 0f) return

    val radius = size.minDimension / 2f * (1f + 2f * reach)
    if (radius <= 0f) return
    val peak = alpha * visible
    drawCircle(
        brush = Brush.radialGradient(
            0.00f to FocusGlow.Color.copy(alpha = peak),
            0.30f to FocusGlow.Color.copy(alpha = peak * 0.62f),
            0.52f to FocusGlow.Color.copy(alpha = peak * 0.28f),
            0.74f to FocusGlow.Color.copy(alpha = peak * 0.09f),
            1.00f to FocusGlow.Color.copy(alpha = 0f),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}
