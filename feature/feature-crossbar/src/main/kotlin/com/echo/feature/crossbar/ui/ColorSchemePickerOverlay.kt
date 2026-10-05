package com.echo.feature.crossbar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.echo.core.ui.components.EchoContextMenuOverlay
import com.echo.core.ui.components.HsvColorPickerDialog
import com.echo.core.ui.components.MenuRow
import com.echo.core.ui.components.MenuState
import com.echo.feature.crossbar.viewmodel.ColorSchemePickerState
import com.echo.feature.crossbar.viewmodel.CustomColorPickerState

// the kit's side rail, as every context menu draws it; each row's badge is its swatch, and the live
// crossbar behind shows the highlighted scheme (owner, 2026-10-04)
@Composable
fun ColorSchemePickerOverlay(
    state: ColorSchemePickerState,
    onHighlightedAt: (Int) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val menu = MenuState(
        title = "Color Scheme",
        subtitle = state.options.getOrNull(state.selectedIndex)?.sublabel,
        rows = state.options.mapIndexed { index, option -> MenuRow(index, option.label) },
        selectedIndex = state.selectedIndex,
    )
    EchoContextMenuOverlay(
        state = menu,
        onRowActivated = { index -> if (index == state.selectedIndex) onConfirm() else onHighlightedAt(index) },
        onDismiss = onDismiss,
        modifier = modifier,
        rowBadge = { index, focused ->
            val swatch = state.options.getOrNull(index)?.swatch ?: return@EchoContextMenuOverlay
            Box(
                Modifier
                    .size(SwatchSize)
                    .clip(CircleShape)
                    .background(Color(swatch))
                    .border(if (focused) 2.dp else 1.dp, Color.White.copy(alpha = if (focused) 0.9f else 0.35f), CircleShape),
            )
        },
    )
}

private val SwatchSize = 26.dp

@Composable
fun CustomColorPickerOverlay(
    state: CustomColorPickerState,
    onChannelFraction: (Int, Float) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        HsvColorPickerDialog(
            title = "Custom Color",
            hue = state.hue,
            saturation = state.saturation,
            brightness = state.brightness,
            selectedChannel = state.selectedChannel,
            onChannelFraction = onChannelFraction,
            onConfirm = onConfirm,
            onCancel = onCancel,
        )
    }
}
