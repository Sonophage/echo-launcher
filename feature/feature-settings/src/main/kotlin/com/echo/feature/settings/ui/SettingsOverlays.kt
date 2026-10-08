package com.echo.feature.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.echo.core.domain.model.GamepadAction
import com.echo.core.ui.components.EchoCheckMark
import com.echo.core.ui.detail.EchoDetailLaunchButton
import com.echo.core.ui.detail.EchoOverlayCard
import com.echo.core.ui.detail.EchoOverlayTitle

private val MESSAGE_COLOR = Color(0xCCFFFFFF)

@Composable
fun SettingsConfirmOverlay(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    cancelLabel: String = "Cancel",
    destructive: Boolean = true,
) {
    var cursor by remember(destructive) { mutableIntStateOf(if (destructive) 0 else 1) }
    SettingsOverlayInput { action ->
        when (action) {
            GamepadAction.NAVIGATE_UP, GamepadAction.NAVIGATE_DOWN -> cursor = 1 - cursor
            GamepadAction.SELECT -> if (cursor == 0) onCancel() else onConfirm()
            GamepadAction.BACK -> onCancel()
            else -> Unit
        }
    }
    EchoOverlayCard(onScrimTap = onCancel) {
        EchoOverlayTitle(title)
        if (message.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(message, color = MESSAGE_COLOR, fontSize = 14.sp)
        }
        Spacer(Modifier.height(20.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            EchoDetailLaunchButton(
                label = cancelLabel,
                icon = null,
                focused = cursor == 0,
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth(),
            )
            EchoDetailLaunchButton(
                label = confirmLabel,
                icon = null,
                focused = cursor == 1,
                onClick = onConfirm,
                fill = if (destructive) com.echo.core.ui.detail.DestructiveConfirmFill else DetailRestFill,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private val DetailRestFill = Color(0x1FFFFFFF)

@Composable
fun SettingsMessageOverlay(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    dismissLabel: String = "OK",
) {
    SettingsOverlayInput { action ->

        if (action == GamepadAction.SELECT || action == GamepadAction.BACK) onDismiss()
    }
    EchoOverlayCard(onScrimTap = onDismiss) {
        EchoOverlayTitle(title)
        Spacer(Modifier.height(10.dp))
        Text(message, color = MESSAGE_COLOR, fontSize = 14.sp)
        Spacer(Modifier.height(20.dp))
        EchoDetailLaunchButton(
            label = dismissLabel,
            icon = null,
            focused = true,
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun SettingsChoiceOverlay(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onPick: (Int) -> Unit,
    onCancel: () -> Unit,
) {
    var cursor by remember(options, selectedIndex) {
        mutableIntStateOf(selectedIndex.coerceIn(0, (options.size - 1).coerceAtLeast(0)))
    }
    SettingsOverlayInput { action ->
        when (action) {
            GamepadAction.NAVIGATE_UP -> cursor = (cursor - 1).coerceAtLeast(0)
            GamepadAction.NAVIGATE_DOWN -> cursor = (cursor + 1).coerceAtMost(options.lastIndex)
            GamepadAction.SELECT -> if (options.isNotEmpty()) onPick(cursor)
            GamepadAction.BACK -> onCancel()
            else -> Unit
        }
    }
    EchoOverlayCard(onScrimTap = onCancel) {
        EchoOverlayTitle(title)
        Spacer(Modifier.height(12.dp))

        Column(
            modifier = Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            options.forEachIndexed { index, label ->
                val focused = index == cursor
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (focused) Color(0x33FFFFFF) else Color.Transparent)
                        .clickable { onPick(index) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = label,
                        color = if (focused) Color.White else MESSAGE_COLOR,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f),
                    )

                    if (index == selectedIndex) {
                        EchoCheckMark(Color(0xFF7ED957), Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsTextPromptOverlay(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    placeholder: String = "",
    confirmLabel: String = "Save",
    isPassword: Boolean = false,
) {
    SettingsOverlayInput { action ->
        when (action) {
            GamepadAction.SELECT -> onConfirm()
            GamepadAction.BACK -> onCancel()

            else -> Unit
        }
    }
    com.echo.core.ui.detail.EchoTextPromptOverlay(
        title = title,
        value = value,
        placeholder = placeholder,
        onValueChange = onValueChange,
        onConfirm = onConfirm,
        onCancel = onCancel,
        confirmLabel = confirmLabel,
        isPassword = isPassword,
    )
}
