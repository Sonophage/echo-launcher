package com.echo.core.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.echo.core.domain.model.GamepadAction
import com.echo.core.ui.components.ContextMenuHeader
import com.echo.core.ui.components.ContextMenuRowLabel
import com.echo.core.ui.components.ControllerPromptItem
import com.echo.core.ui.components.EchoHintBar
import com.echo.core.ui.components.HintAction
import com.echo.core.ui.components.HintBarHeight
import com.echo.core.ui.components.RailEdgeGap
import com.echo.core.ui.components.RailInk
import com.echo.core.ui.components.RailPanelBacking
import com.echo.core.ui.components.RailRowGap
import com.echo.core.ui.components.StatusStripHeight
import com.echo.core.ui.components.contextMenuRow
import com.echo.core.ui.design.RAIL_PANEL_WIDTH
import com.echo.core.ui.design.panelDesignUnits
import com.echo.core.ui.theme.EchoTextStyle

// The kit's text prompt (owner, 2026-10-07: it must look like the rest of ECHO). It sits on the context
// menu's rail: the title as the menu header, the field as the focused rail row, Save (and Reset) as rail
// rows, and the footer's A and B. The screen behind stays live; a tap off the rail cancels.
@Composable
fun EchoTextPromptOverlay(
    title: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    resetLabel: String? = null,
    onReset: (() -> Unit)? = null,
    confirmLabel: String = "Save",
    cancelLabel: String = "Cancel",
) {
    val focusRequester = remember { FocusRequester() }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val u = panelDesignUnits(maxWidth.value, maxHeight.value, LocalDensity.current)
        Box(
            Modifier.fillMaxSize().clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onCancel,
            ),
        )
        RailPanelBacking(u)

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(RailRowGap),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(u.dp(RAIL_PANEL_WIDTH))
                .fillMaxHeight()
                // a tap on the rail is not a tap on the screen behind
                .pointerInput(Unit) { detectTapGestures { } }
                .padding(top = StatusStripHeight, bottom = HintBarHeight, end = RailEdgeGap, start = RailEdgeGap),
        ) {
            ContextMenuHeader(title, subtitle, u)

            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = EchoTextStyle.merge(TextStyle(color = RailInk, fontSize = u.sp(17), fontWeight = FontWeight.Bold)),
                cursorBrush = SolidColor(RailInk),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onConfirm() }),
                decorationBox = { field ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(u.dp(12)))
                            .background(Color.White)
                            .padding(horizontal = u.dp(16), vertical = u.dp(12)),
                    ) {
                        if (value.isEmpty()) {
                            Text(placeholder, color = RailInk.copy(alpha = 0.45f), fontSize = u.sp(17), maxLines = 1)
                        }
                        field()
                    }
                },
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
            )

            Box(Modifier.contextMenuRow(focused = false, dim = 1f, u = u, onClick = onConfirm)) {
                ContextMenuRowLabel(confirmLabel, focused = false, u = u)
            }
            if (onReset != null && resetLabel != null) {
                Box(Modifier.contextMenuRow(focused = false, dim = 1f, u = u, onClick = onReset)) {
                    ContextMenuRowLabel(resetLabel, focused = false, u = u)
                }
            }
        }

        EchoHintBar(
            items = listOf(ControllerPromptItem(GamepadAction.BACK, cancelLabel)),
            primary = HintAction(GamepadAction.SELECT, confirmLabel),
            onAction = { action ->
                when (action) {
                    GamepadAction.SELECT -> onConfirm()
                    GamepadAction.BACK -> onCancel()
                    else -> Unit
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
        // inside the constraints block: the field is composed here, so it exists when focus is asked for
        LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }
    }
}
