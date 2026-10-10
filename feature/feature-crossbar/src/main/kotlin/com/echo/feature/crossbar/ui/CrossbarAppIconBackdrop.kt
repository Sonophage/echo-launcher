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
import com.echo.core.ui.design.AppIconWatermark
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

// the scrim over art or a wallpaper behind the crossbar and Recent: the accent mixed into the dark, darkest at the
// edges, so white text stays readable and what is under it still shows
fun backdropScrim(accent: Color): Brush {
    val base = lerp(AppBackdropBase, accent, 0.22f)
    return Brush.horizontalGradient(0.0f to base.copy(alpha = 0.65f), 0.5f to base.copy(alpha = 0.50f), 1.0f to base.copy(alpha = 0.75f))
}

// [overWallpaper]: a wallpaper is under it, so the backdrop is only the app's icon, faint in the corner, and with
// [scrim] the app-coloured scrim over the wallpaper (owner, 2026-10-09: the wallpaper shows, tinted). The crossbar
// draws its own scrim; Recent asks for this one. Without a wallpaper it is the solid gradient, which alone keeps
// white text readable
@Composable
fun CrossbarAppIconBackdrop(
    packageName: String,
    fallbackAccent: Color,
    modifier: Modifier = Modifier,
    overWallpaper: Boolean = false,
    scrim: Boolean = false,
) {
    val icon = rememberAppIcon(packageName, sizePx = SOURCE_PX, colorOf = ::appIconAccent)
    val accent = icon?.color ?: fallbackAccent
    if (overWallpaper) {
        androidx.compose.foundation.layout.BoxWithConstraints(modifier.fillMaxSize()) {
            if (scrim) Box(Modifier.fillMaxSize().background(backdropScrim(accent)))
            icon?.let { AppIconWatermark(it.bitmap, maxHeight * WATERMARK_SHARE) }
        }
    } else {
        androidx.compose.foundation.layout.BoxWithConstraints(modifier.fillMaxSize().background(Brush.linearGradient(appBackdropStops(accent)))) {
            icon?.let { AppIconWatermark(it.bitmap, maxHeight * WATERMARK_SHARE) }
        }
    }
}

// the corner icon as a share of the screen's height
private const val WATERMARK_SHARE = 0.75f
