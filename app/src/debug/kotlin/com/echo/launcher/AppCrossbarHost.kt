package com.echo.launcher

import androidx.compose.runtime.Composable
import com.echo.feature.crossbar.ui.CrossbarShellContainer
import com.echo.launcher.debug.DebugAwareCrossbarHost

@Composable
fun AppCrossbarHost() {
    DebugAwareCrossbarHost { onSettingsLongPress ->
        CrossbarShellContainer(onSettingsLongPress = onSettingsLongPress)
    }
}
