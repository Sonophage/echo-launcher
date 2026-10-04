package com.echo.launcher.debug

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun DebugAwareCrossbarHost(
    crossbarShellContainer: @Composable (onSettingsLongPress: () -> Unit) -> Unit,
) {
    var showDebugMenu by remember { mutableStateOf(false) }

    if (showDebugMenu) {
        DebugMenuScreen(
            onDismiss = { showDebugMenu = false },
        )
    } else {
        crossbarShellContainer { showDebugMenu = true }
    }
}
