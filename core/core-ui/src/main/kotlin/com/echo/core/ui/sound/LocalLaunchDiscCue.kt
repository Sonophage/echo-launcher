package com.echo.core.ui.sound

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

val LocalLaunchDiscCue: ProvidableCompositionLocal<() -> Unit> =
    staticCompositionLocalOf { {} }
