package com.psplauncher.core.ui.design

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.platform.LocalDensity

const val MEDIA_DESIGN_WIDTH = 1280f

const val MEDIA_DESIGN_HEIGHT = 720f

const val LEGIBILITY_FLOOR_PX = 28f

val MediaDefaultAccent = Color(0xFF8DB8E8)

fun legibleTextPx(px: Float): Float = maxOf(px, LEGIBILITY_FLOOR_PX)

fun mediaDesignScale(widthDp: Float, heightDp: Float): Float =
    minOf(widthDp / MEDIA_DESIGN_WIDTH, heightDp / MEDIA_DESIGN_HEIGHT)

class DesignUnits(val scale: Float, private val density: Density) {
    fun dp(px: Number): Dp = (px.toFloat() * scale).dp
    fun sp(px: Number): TextUnit = with(density) { legibleTextPx(dp(px).toPx()).toSp() }

    fun eyebrow(color: Color = Color.White.copy(alpha = 0.55f)) = TextStyle(
        color = color,
        fontSize = sp(12),
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.28.em,
    )
}

@Composable
fun MediaDesignFrame(
    modifier: Modifier = Modifier,
    content: @Composable BoxWithConstraintsScope.(DesignUnits) -> Unit,
) {
    val density = LocalDensity.current
    BoxWithConstraints(modifier.fillMaxSize()) {
        val units = DesignUnits(mediaDesignScale(maxWidth.value, maxHeight.value), density)
        content(units)
    }
}

fun mediaAccent(argb: Long?): Color {
    val raw = argb?.let { Color(it.toInt()).copy(alpha = 1f) } ?: return MediaDefaultAccent
    return if (raw.luminance() < MIN_ACCENT_LUMINANCE) lerp(raw, Color.White, 0.45f) else raw
}

private const val MIN_ACCENT_LUMINANCE = 0.30f

fun mediaGlow(accent: Color, strength: Float): Color = lerp(Color(0xFF0D0A0A), accent, strength)
