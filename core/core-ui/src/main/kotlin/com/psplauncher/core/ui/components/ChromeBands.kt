package com.psplauncher.core.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val StatusStripHeight = 34.dp

val HintBarHeight = 34.dp

@Composable
fun chromeGutter(end: Boolean = false): Dp {
    val cutout = WindowInsets.displayCutout.asPaddingValues()
    val dir = LocalLayoutDirection.current
    return maxOf(16.dp, if (end) cutout.calculateEndPadding(dir) else cutout.calculateStartPadding(dir))
}
