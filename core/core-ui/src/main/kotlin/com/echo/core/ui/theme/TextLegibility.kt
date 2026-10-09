package com.echo.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

enum class TextContrastRole(val threshold: Float) {
    BODY(4.5f),
    LARGE(3.0f),
}

fun relativeLuminance(c: Color): Double {
    fun linearize(channel: Float): Double {
        val v = channel.toDouble()
        return if (v <= 0.04045) v / 12.92
        else Math.pow((v + 0.055) / 1.055, 2.4)
    }
    return 0.2126 * linearize(c.red) + 0.7152 * linearize(c.green) + 0.0722 * linearize(c.blue)
}

fun contrastRatio(a: Color, b: Color): Double {
    val la = relativeLuminance(a)
    val lb = relativeLuminance(b)
    val lighter = maxOf(la, lb)
    val darker = minOf(la, lb)
    return (lighter + 0.05) / (darker + 0.05)
}

fun ensureReadable(fg: Color, bg: Color, minContrast: Float = 4.5f): Color {
    if (contrastRatio(fg, bg) >= minContrast) return fg
    return bestPolarity(bg)
}

fun bestPolarity(bg: Color): Color =
    if (contrastRatio(Color.Black, bg) >= contrastRatio(Color.White, bg)) Color.Black else Color.White

fun composite(top: Color, bottom: Color): Color {
    val a = top.alpha
    return Color(
        red = top.red * a + bottom.red * (1f - a),
        green = top.green * a + bottom.green * (1f - a),
        blue = top.blue * a + bottom.blue * (1f - a),
    )
}

fun solveScrimColor(
    base: Color,
    alpha: Float,
    target: Float = TextContrastRole.BODY.threshold,
    text: Color = Color.White,
    worstCase: Color = Color.White,
): Color {
    fun passes(t: Float): Boolean =
        contrastRatio(
            text,
            composite(lerp(base, Color.Black, t).copy(alpha = alpha), worstCase),
        ) >= target

    if (passes(0f)) return base
    if (!passes(1f)) return Color.Black
    var lo = 0f
    var hi = 1f
    repeat(12) {
        val mid = (lo + hi) / 2f
        if (passes(mid)) hi = mid else lo = mid
    }
    return lerp(base, Color.Black, hi)
}

fun crossbarScrimAnchors(backgroundTop: Color, backgroundBottom: Color): Pair<Color, Color> =
    solveScrimColor(backgroundTop, alpha = CROSSBAR_SCRIM_TOP_ALPHA).copy(alpha = CROSSBAR_SCRIM_TOP_ALPHA) to
        solveScrimColor(backgroundBottom, alpha = CROSSBAR_SCRIM_BOTTOM_ALPHA).copy(alpha = CROSSBAR_SCRIM_BOTTOM_ALPHA)

const val CROSSBAR_SCRIM_TOP_ALPHA = 0.72f
const val CROSSBAR_SCRIM_BOTTOM_ALPHA = 0.90f
