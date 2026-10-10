package com.echo.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

@Immutable
data class StorefrontColors(

    val accentHue: Color,

    val backgroundDeep: Color,

    val backgroundMid: Color,

    val selectionGlow: Color,

    val chromeTop: Color,

    val chromeBottom: Color,

    val chromeDivider: Color,

    val categorySelected: Color,

    val categorySelectedEdge: Color,

    val categoryInactive: Color,

    val tileNormal: Color,

    val tileSelected: Color,

    val tileSelectedEdge: Color,

    val tileSelectedInner: Color,

    val footerBackground: Color,

    val footerDivider: Color,

    val textPrimary: Color,

    val textSecondary: Color,

    val contentBackground: Color,

    val railBackground: Color,

    val searchField: Color,

    val searchBorder: Color,

    val overlayDim: Color,

    val menuPanel: Color,

    val menuRowSelected: Color,

    val destructive: Color,
)

private fun resolveHueSource(accent: Color, wave: Color, backgroundBottom: Color): Color =
    when {
        accent.isVividHue() -> accent
        wave.isVividHue() -> wave
        else -> backgroundBottom
    }

private fun Color.isVividHue(): Boolean {
    val max = maxOf(red, green, blue)
    val min = minOf(red, green, blue)
    return max - min >= 0.10f && max >= 0.30f
}

// a full screen that covers the crossbar (the app and game pickers): the scrim's colours made solid, since the
// scrim's own alpha let the crossbar's icons and rows show through between the tiles
val StorefrontColors.screenBackdrop: Brush
    get() = Brush.verticalGradient(listOf(backgroundDeep.copy(alpha = 1f), backgroundMid.copy(alpha = 1f)))

@Composable
fun deriveStorefrontColors(): StorefrontColors {
    val echo = LocalEchoColors.current
    return remember(echo) { storefrontColorsFor(echo) }
}

fun storefrontColorsFor(echo: EchoColors): StorefrontColors {
    val hue = resolveHueSource(echo.accentColor, echo.waveColor, echo.backgroundBottom)

    val accentEdge = lerp(hue, Color.White, 0.55f)
    val accentInner = lerp(hue, Color.White, 0.32f)

    val bgTop = echo.backgroundTop
    val bgBottom = echo.backgroundBottom

    val (backgroundDeep, backgroundMid) = crossbarScrimAnchors(bgTop, bgBottom)

    val chromeTop = bgTop.copy(alpha = 0.96f)
    val chromeBottom = lerp(bgTop, bgBottom, 0.55f).copy(alpha = 0.96f)

    val categorySelected = lerp(bgBottom, hue, 0.35f).copy(alpha = 0.92f)
    val categoryInactive = bgTop.copy(alpha = 0.50f)

    val tileNormal = bgTop.copy(alpha = 0.85f)
    val tileSelected = lerp(bgTop, hue, 0.35f).copy(alpha = 0.95f)

    val footerBackground = lerp(bgTop, Color.Black, 0.15f).copy(alpha = 0.98f)
    val footerDivider = lerp(hue, Color.White, 0.25f)

    val textPrimary = ensureReadable(Color.White, backgroundMid, 3.0f)
    val lightChrome = textPrimary == Color.Black
    val textSecondary = ensureReadable(
        fg = if (lightChrome) lerp(Color.Black, Color.White, 0.25f)
        else lerp(hue, Color.White, 0.72f),
        bg = backgroundMid,
        minContrast = 3.0f,
    )
    val edge = if (lightChrome) lerp(hue, Color.Black, 0.45f) else accentEdge
    val edgeInner = if (lightChrome) lerp(hue, Color.Black, 0.20f) else accentInner

    val contentBackground = Color(0x00000000)
    val railBackground = bgTop.copy(alpha = 0.35f)

    val searchField = if (lightChrome) Color.White.copy(alpha = 0.30f)
    else lerp(bgTop, Color.Black, 0.35f).copy(alpha = 0.90f)
    val searchBorder = edge

    val overlayDim = Color(0x99000000)
    val menuPanel = if (lightChrome) Color.White.copy(alpha = 0.92f)
    else lerp(Color.Black, bgTop, 0.30f).copy(alpha = 0.96f)
    val menuRowSelected = edge.copy(alpha = 0.20f)

    return StorefrontColors(
        accentHue           = hue,
        backgroundDeep      = backgroundDeep,
        backgroundMid       = backgroundMid,
        selectionGlow       = (if (lightChrome) lerp(hue, Color.Black, 0.55f)
        else lerp(hue, Color.White, 0.45f)).copy(alpha = 0.16f),
        chromeTop           = chromeTop,
        chromeBottom        = chromeBottom,
        chromeDivider       = edge,
        categorySelected    = categorySelected,
        categorySelectedEdge = edge,
        categoryInactive    = categoryInactive,
        tileNormal          = tileNormal,
        tileSelected        = tileSelected,
        tileSelectedEdge    = edge,
        tileSelectedInner   = edgeInner,
        footerBackground    = footerBackground,
        footerDivider       = footerDivider,
        textPrimary         = textPrimary,
        textSecondary       = textSecondary,
        contentBackground   = contentBackground,
        railBackground      = railBackground,
        searchField         = searchField,
        searchBorder        = searchBorder,
        overlayDim          = overlayDim,
        menuPanel           = menuPanel,
        menuRowSelected     = menuRowSelected,
        destructive         = Color(0xFFFF6B6B),
    )
}
