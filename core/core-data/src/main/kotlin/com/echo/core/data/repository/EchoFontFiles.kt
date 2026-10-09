package com.echo.core.data.repository

import android.content.Context
import com.echo.core.ui.theme.EchoFonts
import java.io.File

// the font files ECHO keeps, and the one place that picks which it draws with (owner, 2026-10-09): an applied
// theme's font wins over the ECHO folder's Look/Fonts, and Sora is used when neither reads
object EchoFontFiles {
    const val THEME_FONT_DIR = "theme-font"
    const val LOOK_FONT_DIR = "echo-font"

    fun theme(context: Context): File? = File(context.filesDir, THEME_FONT_DIR).listFiles().orEmpty().firstOrNull()

    fun look(context: Context): File? = File(context.filesDir, LOOK_FONT_DIR).listFiles().orEmpty().firstOrNull()

    // puts the winning font in use; returns its file, or null for Sora
    fun refresh(context: Context): File? = EchoFonts.use(theme(context), look(context))
}
