package com.echo.core.ui.design

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp

// owner, 2026-10-09: an app's icon, large, faint and turned, in the bottom left corner over the wallpaper's tint;
// the App Drawer and the crossbar draw the same one
@Composable
fun BoxScope.AppIconWatermark(icon: ImageBitmap, size: Dp) {
    Image(
        icon, null,
        Modifier.align(Alignment.BottomStart).offset(-size * 0.17f, size * 0.21f).size(size).rotate(-12f)
            .graphicsLayer(alpha = APP_WATERMARK_ALPHA),
    )
}

private const val APP_WATERMARK_ALPHA = 0.09f
