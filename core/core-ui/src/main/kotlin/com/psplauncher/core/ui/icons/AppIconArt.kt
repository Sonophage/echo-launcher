package com.psplauncher.core.ui.icons

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppIconArt(val bitmap: ImageBitmap, val color: Color?)

@Composable
fun rememberAppIcon(packageName: String?): AppIconArt? {
    val context = LocalContext.current
    val art by produceState<AppIconArt?>(null, packageName) {
        value = packageName?.takeIf { it.isNotBlank() }?.let { pkg ->
            withContext(Dispatchers.IO) {
                context.appIconBitmap(pkg, sizePx = ICON_PX, foregroundOnly = false)?.let { AppIconArt(it, dominantColor(it)) }
            }
        }
    }
    return art
}

private fun dominantColor(bitmap: ImageBitmap): Color? {
    val pixels = bitmap.toPixelMap()
    var r = 0f; var g = 0f; var b = 0f; var n = 0
    val hsv = FloatArray(3)
    for (x in 0 until pixels.width step 4) for (y in 0 until pixels.height step 4) {
        val c = pixels[x, y]
        if (c.alpha < 0.8f) continue
        android.graphics.Color.RGBToHSV((c.red * 255).toInt(), (c.green * 255).toInt(), (c.blue * 255).toInt(), hsv)
        if (hsv[1] < 0.25f || hsv[2] < 0.2f) continue
        r += c.red; g += c.green; b += c.blue; n++
    }
    return if (n == 0) null else Color(r / n, g / n, b / n)
}

private const val ICON_PX = 96
