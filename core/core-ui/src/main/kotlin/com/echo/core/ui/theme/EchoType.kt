package com.echo.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.echo.core.ui.R

val SoraFontFamily = FontFamily(
    soraWeight(FontWeight.Thin, 100),
    soraWeight(FontWeight.ExtraLight, 200),
    soraWeight(FontWeight.Light, 300),
    soraWeight(FontWeight.Normal, 400),
    soraWeight(FontWeight.Medium, 500),
    soraWeight(FontWeight.SemiBold, 600),
    soraWeight(FontWeight.Bold, 700),
    soraWeight(FontWeight.ExtraBold, 800),
)

private fun soraWeight(weight: FontWeight, axis: Int): Font = Font(
    resId = R.font.sora_variable,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(axis)),
)

// the font ECHO draws with: Sora, or a font file from the ECHO folder's Look/Fonts (owner, 2026-10-04).
// It is Compose state, so every text that reads EchoTextStyle redraws when it changes.
object EchoFonts {
    private val custom = androidx.compose.runtime.mutableStateOf<FontFamily?>(null)

    val family: FontFamily get() = custom.value ?: SoraFontFamily

    // uses [file] when Android can build a typeface from it; anything else falls back to Sora, so a
    // broken file never reaches text drawing. Returns whether the file is in use.
    fun use(file: java.io.File?): Boolean {
        val typeface = file?.takeIf { it.isFile }?.let { runCatching { android.graphics.Typeface.Builder(it).build() }.getOrNull() }
        custom.value = typeface?.let { FontFamily(it) }
        return typeface != null
    }
}

val EchoTextStyle: androidx.compose.ui.text.TextStyle
    get() = androidx.compose.ui.text.TextStyle(fontFamily = EchoFonts.family)

internal fun echoTypography(): Typography {
    val base = Typography()
    val family = EchoFonts.family
    return Typography(
        displayLarge = base.displayLarge.copy(fontFamily = family),
        displayMedium = base.displayMedium.copy(fontFamily = family),
        displaySmall = base.displaySmall.copy(fontFamily = family),
        headlineLarge = base.headlineLarge.copy(fontFamily = family),
        headlineMedium = base.headlineMedium.copy(fontFamily = family),
        headlineSmall = base.headlineSmall.copy(fontFamily = family),
        titleLarge = base.titleLarge.copy(fontFamily = family),
        titleMedium = base.titleMedium.copy(fontFamily = family),
        titleSmall = base.titleSmall.copy(fontFamily = family),
        bodyLarge = base.bodyLarge.copy(fontFamily = family),
        bodyMedium = base.bodyMedium.copy(fontFamily = family),
        bodySmall = base.bodySmall.copy(fontFamily = family),
        labelLarge = base.labelLarge.copy(fontFamily = family),
        labelMedium = base.labelMedium.copy(fontFamily = family),
        labelSmall = base.labelSmall.copy(fontFamily = family),
    )
}
