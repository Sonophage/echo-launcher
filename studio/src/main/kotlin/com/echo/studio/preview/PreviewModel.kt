package com.echo.studio.preview

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.lerp
import com.echo.studio.IconColorChoice
import com.echo.studio.PreviewMode
import com.echo.studio.StudioState
import com.echo.themekit.ColorCascade
import com.echo.themekit.EchoThemeManifest
import com.echo.themekit.CrossbarLayoutSpec

data class CrossbarPreviewModel(

    val accent: Color,
    val iconTint: Color,
    val backgroundTop: Color,
    val backgroundBottom: Color,
    val wallpaper: ImageBitmap?,
    val reducedWave: Boolean,

    val iconOverrides: Map<String, ImageBitmap>,

    val layout: CrossbarLayoutSpec = CrossbarLayoutSpec.DEFAULT,
    val mode: PreviewMode = PreviewMode.HOME,
) {
    private val echoAccentColor = Color.White

    val menuCursorEdge: Color get() = lerp(echoAccentColor, Color.White, 0.55f).copy(alpha = 0.95f)

    val menuPanelBackdrop: Color get() = accent.copy(alpha = 0.75f)
}

fun StudioState.toPreviewModel(): CrossbarPreviewModel {
    val accentLong = 0xFF000000L or (accentArgb.toLong() and 0xFFFFFF)
    val (top, bottom) = ColorCascade.lightBackgroundAnchors(accentLong)
    return CrossbarPreviewModel(
        accent = Color(accentArgb),

        iconTint = when (val c = iconColor) {
            IconColorChoice.Auto -> Color.White
            is IconColorChoice.Custom -> Color(c.argb)
        },
        backgroundTop = Color(top.toInt()),
        backgroundBottom = Color(bottom.toInt()),
        wallpaper = wallpaperBitmap,
        reducedWave = waveStyle == EchoThemeManifest.WAVE_REDUCED,
        iconOverrides = iconBitmaps,
        layout = layout,
        mode = previewMode,
    )
}

object SampleContent {
    data class Category(val slotKey: String, val label: String)
    data class Row(val slotKey: String, val title: String, val subtitle: String? = null)

    val categories: List<Category> = listOf(
        Category("catbar_settings", "Settings"),
        Category("catbar_photos", "Photo"),
        Category("catbar_music", "Music"),
        Category("catbar_video", "Video"),
        Category("catbar_games", "Game"),
        Category("catbar_network", "Network"),
        Category("catbar_appstore", "App Store"),
        Category("catbar_library", "Library"),
        Category("catbar_favorites", "Favorites"),
    )

    const val SELECTED_CATEGORY = 3

    val rows: List<Row> = listOf(

        Row("item_memcard_video", "Videos", "132 videos"),
        Row("item_video_recent", "Recently Watched"),
        Row("item_video_favorites", "Favorites"),
        Row("item_video_collections", "Collections"),
    )

    const val SELECTED_ROW = 1
}
