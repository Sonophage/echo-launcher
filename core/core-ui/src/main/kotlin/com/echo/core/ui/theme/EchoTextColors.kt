package com.echo.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.lerp

@Immutable
data class EchoTextColors(

    val primary: Color,

    val secondary: Color,

    val inactive: Color,

    val destructive: Color,

    val requested: Color,

    val adjusted: Boolean,

    val achievedRatio: Float,
)

val DefaultEchoTextColors = EchoTextColors(
    primary = Color.White,
    secondary = EchoPalette.Subtext,
    inactive = Color(0xCCD8E6FF),
    destructive = Color(0xFFFF6B6B),
    requested = Color.White,
    adjusted = false,
    achievedRatio = 21f,
)

val LocalEchoTextColors = staticCompositionLocalOf { DefaultEchoTextColors }

fun resolveTextColors(
    requested: Color,
    backgroundTop: Color,
    backgroundBottom: Color,
    minContrast: Float = 3.0f,
): EchoTextColors {
    val backdrop = lerp(backgroundTop, backgroundBottom, 0.5f)
    val primary = ensureReadable(requested, backdrop, minContrast)
    val darkFamily = primary.luminance() < 0.5f
    return DefaultEchoTextColors.copy(
        primary = primary,
        requested = requested,
        adjusted = primary != requested,
        achievedRatio = contrastRatio(primary, backdrop).toFloat(),
        secondary = if (darkFamily) lerp(Color.Black, Color.White, 0.28f) else DefaultEchoTextColors.secondary,
        inactive = if (darkFamily) lerp(Color.Black, Color.White, 0.46f) else DefaultEchoTextColors.inactive,
    )
}
