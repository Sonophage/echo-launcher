package com.psplauncher.feature.xmb.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.psplauncher.core.ui.icons.appIconBitmap
import com.psplauncher.themekit.AccentDeriver
import com.psplauncher.themekit.BmpImage

sealed interface XmbBackdrop {
    data class Art(val uri: String) : XmbBackdrop

    data class AppIcon(val packageName: String) : XmbBackdrop
}

private const val SOURCE_PX = 192

private const val ICON_ALPHA = 0.20f

private const val FADE_SOLID_UNTIL = 0.45f
private const val FADE_RADIUS_FRACTION = 0.60f
private const val ICON_WIDTH_FRACTION = 0.46f

val AppBackdropBase = Color(0xFF05050C)

private const val TOP_MIX = 0.34f
private const val MID_MIX = 0.15f

fun appBackdropStops(accent: Color): List<Color> = listOf(
    lerp(AppBackdropBase, accent, TOP_MIX),
    lerp(AppBackdropBase, accent, MID_MIX),
    AppBackdropBase,
)

fun appIconAccent(icon: ImageBitmap): Color? {
    val pixels = IntArray(icon.width * icon.height)
    icon.readPixels(pixels)
    return AccentDeriver.deriveAccent(BmpImage(icon.width, icon.height, pixels))?.let { Color(it) }
}

@Composable
fun XmbAppIconBackdrop(
    packageName: String,
    fallbackAccent: Color,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val whole = remember(packageName) {
        context.appIconBitmap(packageName, sizePx = SOURCE_PX, foregroundOnly = false)
    }

    val glyph = remember(packageName) {
        context.appIconBitmap(packageName, sizePx = SOURCE_PX)
    }
    val accent = remember(whole) { whole?.let(::appIconAccent) } ?: fallbackAccent

    Box(modifier.fillMaxSize().background(Brush.linearGradient(appBackdropStops(accent)))) {
        if (glyph != null) {
            Image(
                bitmap = glyph,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                filterQuality = FilterQuality.High,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxWidth(ICON_WIDTH_FRACTION)
                    .alpha(ICON_ALPHA)

                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.radialGradient(
                                FADE_SOLID_UNTIL to Color.Black,
                                1f to Color.Transparent,
                                center = size.center,
                                radius = size.minDimension * FADE_RADIUS_FRACTION,
                            ),
                            blendMode = BlendMode.DstIn,
                        )
                    },
            )
        }
    }
}
