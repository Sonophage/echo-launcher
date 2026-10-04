package com.echo.feature.appbar.appdrawer

import androidx.compose.runtime.Composable
import com.echo.core.ui.detail.EchoConfirmOverlay
import com.echo.feature.appbar.InstalledApp

@Composable
internal fun UninstallConfirmDialog(
    app: InstalledApp,
    confirmFocused: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    EchoConfirmOverlay(
        title = "Uninstall ${app.label}?",
        message = "This removes ${app.label} from your device. Android will ask you to confirm.",
        confirmLabel = "Uninstall",
        cancelLabel = "Cancel",
        confirmFocused = confirmFocused,
        cancelFocused = !confirmFocused,
        onConfirm = onConfirm,
        onCancel = onCancel,
    )
}
