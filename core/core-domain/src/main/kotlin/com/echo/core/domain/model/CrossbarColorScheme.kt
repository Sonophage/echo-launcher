package com.echo.core.domain.model

import com.echo.themekit.ColorCascade

enum class CrossbarColorScheme {
    ORIGINAL,
    CLASSIC_BLUE,
    SUNSET_ORANGE,
    FRESH_GREEN,
    ROYAL_PURPLE,
    CRIMSON_RED,
    SILVER_MONO,
    SAKURA_PINK,
    GOLDEN_AMBER,
    AQUA_TEAL,
    MIDNIGHT_NAVY,
    CHARCOAL,
    BLACK,
}

data class CrossbarPalette(
    val waveColor: Long,
    val accentColor: Long,
    val textColor: Long,
    val backgroundTop: Long,
    val backgroundBottom: Long,
)

fun CrossbarColorScheme.displayLabel(): String = when (this) {
    CrossbarColorScheme.ORIGINAL      -> "Original (Monthly)"
    CrossbarColorScheme.CLASSIC_BLUE  -> "Classic Blue"
    CrossbarColorScheme.SUNSET_ORANGE -> "Sunset Orange"
    CrossbarColorScheme.FRESH_GREEN   -> "Fresh Green"
    CrossbarColorScheme.ROYAL_PURPLE  -> "Royal Purple"
    CrossbarColorScheme.CRIMSON_RED   -> "Crimson Red"
    CrossbarColorScheme.SILVER_MONO   -> "Silver"
    CrossbarColorScheme.SAKURA_PINK   -> "Sakura Pink"
    CrossbarColorScheme.GOLDEN_AMBER  -> "Golden Amber"
    CrossbarColorScheme.AQUA_TEAL     -> "Aqua Teal"
    CrossbarColorScheme.MIDNIGHT_NAVY -> "Midnight Navy"
    CrossbarColorScheme.CHARCOAL      -> "Charcoal"
    CrossbarColorScheme.BLACK         -> "Black"
}

fun CrossbarColorScheme.resolve(month: Int): CrossbarPalette {
    val wave = when (this) {
        CrossbarColorScheme.ORIGINAL      -> ORIGINAL_MONTH_WAVE[month.coerceIn(1, 12) - 1]
        CrossbarColorScheme.CLASSIC_BLUE  -> 0xFF0055AAL
        CrossbarColorScheme.SUNSET_ORANGE -> 0xFFFF8A3DL
        CrossbarColorScheme.FRESH_GREEN   -> 0xFF36C26BL
        CrossbarColorScheme.ROYAL_PURPLE  -> 0xFF7A4DD6L
        CrossbarColorScheme.CRIMSON_RED   -> 0xFFE03B4FL
        CrossbarColorScheme.SILVER_MONO   -> 0xFFB8C4D0L
        CrossbarColorScheme.SAKURA_PINK   -> 0xFFE87FB0L
        CrossbarColorScheme.GOLDEN_AMBER  -> 0xFFE0A32EL
        CrossbarColorScheme.AQUA_TEAL     -> 0xFF2EC4B6L
        CrossbarColorScheme.MIDNIGHT_NAVY -> 0xFF23477EL
        CrossbarColorScheme.CHARCOAL      -> 0xFF4A505AL

        CrossbarColorScheme.BLACK         -> 0xFF000000L
    }
    return CrossbarPalette(
        waveColor        = wave,
        accentColor      = 0xFFFFFFFFL,
        textColor        = 0xFFFFFFFFL,
        backgroundTop    = lightBackgroundAnchors(wave).first,
        backgroundBottom = lightBackgroundAnchors(wave).second,
    )
}

fun lightBackgroundAnchors(waveArgb: Long): Pair<Long, Long> =
    ColorCascade.lightBackgroundAnchors(waveArgb)

private val ORIGINAL_MONTH_WAVE = longArrayOf(
    0xFF1FA89CL,
    0xFFE56BA0L,
    0xFF6FBF3BL,
    0xFFE99BC4L,
    0xFF34B3A0L,
    0xFF3A7BD5L,
    0xFF35B6D6L,
    0xFF2E54A8L,
    0xFFE08A2EL,
    0xFF8A5AC2L,
    0xFFB5642EL,
    0xFFD23B4EL,
)
