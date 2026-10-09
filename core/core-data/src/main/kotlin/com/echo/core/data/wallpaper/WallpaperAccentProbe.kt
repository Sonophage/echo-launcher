package com.echo.core.data.wallpaper

import android.graphics.BitmapFactory
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.echo.core.data.wallpaper.ThemeAccent.followWallpaperAccent
import com.echo.themekit.AccentDeriver
import com.echo.themekit.ArgbImage
import java.io.File
import kotlin.math.max

object WallpaperAccentProbe {
    val KEY_WALLPAPER_ACCENT_SOURCE = stringPreferencesKey("wallpaper_accent_source")

    val KEY_WALLPAPER_ACCENT = longPreferencesKey("wallpaper_accent")

    const val MAX_EDGE = 256

    fun survey(path: String): WallpaperSurvey? = runCatching {
        if (!File(path).isFile) return null

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val decoded = BitmapFactory.decodeFile(
            path,
            BitmapFactory.Options().apply {
                inSampleSize = max(1, max(bounds.outWidth, bounds.outHeight) / MAX_EDGE)
            },
        ) ?: return null

        val width = decoded.width
        val height = decoded.height
        val pixels = IntArray(width * height)
        decoded.getPixels(pixels, 0, width, 0, 0, width, height)
        decoded.recycle()
        if (width <= 0 || height <= 0) return null

        val image = ArgbImage(width, height, pixels)
        WallpaperSurvey(
            source = path,
            accentArgb = AccentDeriver.deriveAccent(image)?.toUInt()?.toLong(),
        )
    }.getOrNull()

    data class WallpaperSurvey(val source: String, val accentArgb: Long?)

    fun MutablePreferences.setWallpaperAccent(survey: WallpaperSurvey?) {
        if (survey != null) this[KEY_WALLPAPER_ACCENT_SOURCE] = survey.source else this.remove(KEY_WALLPAPER_ACCENT_SOURCE)
        val accent = survey?.accentArgb
        if (accent != null) this[KEY_WALLPAPER_ACCENT] = accent else this.remove(KEY_WALLPAPER_ACCENT)

        followWallpaperAccent(accent)
    }

    fun MutablePreferences.clearWallpaperAccent() {
        this.remove(KEY_WALLPAPER_ACCENT_SOURCE)
        this.remove(KEY_WALLPAPER_ACCENT)

        followWallpaperAccent(null)
    }
}
