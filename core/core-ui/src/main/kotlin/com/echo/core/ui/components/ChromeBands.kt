package com.echo.core.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// owner, 2026-10-05: the top bar and the footer each have their own size, set per device in Adjust Crossbar
// Layout. Every screen that leaves room for them reads these heights, so a bigger footer never covers content
@Immutable
data class ChromeScale(val header: Float = 1f, val footer: Float = 1f)

val LocalChromeScale = staticCompositionLocalOf { ChromeScale() }

// the bands at their normal size; for sums made outside a screen, such as a test sizing a layout
val ChromeBandBaseHeight = 34.dp

val StatusStripHeight: Dp
    @Composable @ReadOnlyComposable get() = ChromeBandBaseHeight * LocalChromeScale.current.header

val HintBarHeight: Dp
    @Composable @ReadOnlyComposable get() = ChromeBandBaseHeight * LocalChromeScale.current.footer

val ChromeScrim = Color(0xCC04060C)

@Composable
fun chromeGutter(end: Boolean = false): Dp {
    val cutout = WindowInsets.displayCutout.asPaddingValues()
    val dir = LocalLayoutDirection.current
    return maxOf(16.dp, if (end) cutout.calculateEndPadding(dir) else cutout.calculateStartPadding(dir))
}
