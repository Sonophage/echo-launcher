package com.echo.core.ui.icons

import androidx.collection.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppIconArt(val bitmap: ImageBitmap, val color: Color?, val version: Long = 0)

data class AppIconKey(
    val packageName: String,
    val sizePx: Int,
    val foregroundOnly: Boolean,
    val colorOf: ((ImageBitmap) -> Color?)?,
)

class AppIconCache(maxBytes: Int) {
    private val lru = object : LruCache<AppIconKey, AppIconArt>(maxBytes) {
        override fun sizeOf(key: AppIconKey, value: AppIconArt): Int = value.bitmap.width * value.bitmap.height * 4
    }

    fun peek(key: AppIconKey): AppIconArt? = lru[key]

    fun getOrLoad(key: AppIconKey, version: Long = 0, load: () -> AppIconArt?): AppIconArt? =
        lru[key]?.takeIf { it.version == version } ?: load()?.also { lru.put(key, it) }

    companion object {
        val Shared = AppIconCache(maxBytes = 24 * 1024 * 1024)
    }
}

@Composable
fun rememberAppIcon(
    packageName: String?,
    sizePx: Int = ICON_PX,
    foregroundOnly: Boolean = false,
    colorOf: ((ImageBitmap) -> Color?)? = ::dominantColor,
): AppIconArt? {
    val context = LocalContext.current
    val key = packageName?.takeIf { it.isNotBlank() }?.let { AppIconKey(it, sizePx, foregroundOnly, colorOf) }
    val loaded by produceState<Pair<AppIconKey, AppIconArt?>?>(null, key) {
        value = key?.let {
            it to withContext(Dispatchers.IO) {
                val version = runCatching { context.packageManager.getPackageInfo(it.packageName, 0).lastUpdateTime }.getOrDefault(0L)
                AppIconCache.Shared.getOrLoad(it, version) {
                    context.appIconBitmap(it.packageName, sizePx = it.sizePx, foregroundOnly = it.foregroundOnly)
                        ?.let { bmp -> AppIconArt(bmp, it.colorOf?.invoke(bmp), version) }
                }
            }
        }
    }
    if (key == null) return null
    return AppIconCache.Shared.peek(key) ?: loaded?.takeIf { it.first == key }?.second
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
