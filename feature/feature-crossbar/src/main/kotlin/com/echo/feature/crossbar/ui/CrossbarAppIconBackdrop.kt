package com.echo.feature.crossbar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.lerp
import com.echo.core.ui.icons.rememberAppIcon
import com.echo.themekit.AccentDeriver
import com.echo.themekit.ArgbImage

sealed interface CrossbarBackdrop {
    data class Art(val uri: String) : CrossbarBackdrop

    data class AppIcon(val packageName: String) : CrossbarBackdrop
}

private const val SOURCE_PX = 192

val AppBackdropBase = Color(0xFF05050C)

private const val TOP_MIX = 0.34f
private const val MID_MIX = 0.15f

fun appBackdropStops(accent: Color): List<Color> = listOf(
    lerp(AppBackdropBase, accent, TOP_MIX),
    lerp(AppBackdropBase, accent, MID_MIX),
    AppBackdropBase,
)

/**
 * The whole icon, not the adaptive foreground: an adaptive icon usually carries
 * its brand colour in the background layer behind a white or black glyph, so the
 * foreground alone yields no hue for a great many apps.
 */
fun appIconAccent(icon: ImageBitmap): Color? {
    val pixels = IntArray(icon.width * icon.height)
    icon.readPixels(pixels)
    return AccentDeriver.deriveAccent(ArgbImage(icon.width, icon.height, pixels))?.let { Color(it) }
}

@Composable
fun CrossbarAppIconBackdrop(
    packageName: String,
    fallbackAccent: Color,
    modifier: Modifier = Modifier,
) {
    val accent = rememberAppIcon(packageName, sizePx = SOURCE_PX, colorOf = ::appIconAccent)?.color ?: fallbackAccent

    Box(modifier.fillMaxSize().background(Brush.linearGradient(appBackdropStops(accent))))
}
