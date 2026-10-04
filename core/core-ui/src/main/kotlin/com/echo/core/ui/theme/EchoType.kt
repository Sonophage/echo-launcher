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

val EchoTextStyle = androidx.compose.ui.text.TextStyle(fontFamily = SoraFontFamily)

internal fun echoTypography(): Typography {
    val base = Typography()
    return Typography(
        displayLarge = base.displayLarge.copy(fontFamily = SoraFontFamily),
        displayMedium = base.displayMedium.copy(fontFamily = SoraFontFamily),
        displaySmall = base.displaySmall.copy(fontFamily = SoraFontFamily),
        headlineLarge = base.headlineLarge.copy(fontFamily = SoraFontFamily),
        headlineMedium = base.headlineMedium.copy(fontFamily = SoraFontFamily),
        headlineSmall = base.headlineSmall.copy(fontFamily = SoraFontFamily),
        titleLarge = base.titleLarge.copy(fontFamily = SoraFontFamily),
        titleMedium = base.titleMedium.copy(fontFamily = SoraFontFamily),
        titleSmall = base.titleSmall.copy(fontFamily = SoraFontFamily),
        bodyLarge = base.bodyLarge.copy(fontFamily = SoraFontFamily),
        bodyMedium = base.bodyMedium.copy(fontFamily = SoraFontFamily),
        bodySmall = base.bodySmall.copy(fontFamily = SoraFontFamily),
        labelLarge = base.labelLarge.copy(fontFamily = SoraFontFamily),
        labelMedium = base.labelMedium.copy(fontFamily = SoraFontFamily),
        labelSmall = base.labelSmall.copy(fontFamily = SoraFontFamily),
    )
}
