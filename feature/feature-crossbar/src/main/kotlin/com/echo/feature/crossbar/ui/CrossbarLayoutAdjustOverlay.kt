package com.echo.feature.crossbar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.echo.core.domain.model.ControllerIcon
import com.echo.core.domain.model.GamepadAction
import com.echo.core.ui.components.ControllerHintStyle
import com.echo.core.ui.components.EchoControllerHints
import com.echo.core.ui.components.ControllerPromptItem
import com.echo.themekit.CrossbarLayoutAdjust
import kotlin.math.roundToInt

@Composable
fun CrossbarLayoutAdjustOverlay(
    draft: CrossbarLayoutAdjust,
    slidersVisible: Boolean,
    onScale: (Float) -> Unit,
    onHorizontal: (Float) -> Unit,
    onVertical: (Float) -> Unit,
    onToggleSliders: () -> Unit,
    onReset: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    sizingHeader: Boolean = false,
    onHeader: (Float) -> Unit = {},
    onFooter: (Float) -> Unit = {},
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0x22000000))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {  },
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .background(Color(0xF20B1220), RoundedCornerShape(16.dp))
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Adjust Crossbar Layout",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )

            Text(
                text = "Scale ${"%.2f".format(draft.scale)}x    " +
                    "Horizontal ${(draft.barLeftFraction * 100).roundToInt()}%    " +
                    "Vertical ${(draft.barTopFraction * 100).roundToInt()}%    " +
                    "Top bar ${(draft.headerScale * 100).roundToInt()}%    " +
                    "Footer ${(draft.footerScale * 100).roundToInt()}%",
                color = Color(0xFFB9C6DC),
                fontSize = 13.sp,
            )

            EchoControllerHints(
                items = listOf(

                    ControllerPromptItem.fixed(ControllerIcon.DPAD_ALL, "Move"),
                    ControllerPromptItem(
                        listOf(GamepadAction.PREV_CATEGORY, GamepadAction.NEXT_CATEGORY),
                        "Scale",
                    ),
                    ControllerPromptItem(
                        listOf(GamepadAction.PREV_PAGE, GamepadAction.NEXT_PAGE),
                        if (sizingHeader) "Top bar size" else "Footer size",
                    ),
                    ControllerPromptItem(GamepadAction.OPEN_SEARCH, if (sizingHeader) "Size the footer" else "Size the top bar"),
                    ControllerPromptItem(GamepadAction.OPEN_CONTEXT_MENU, "Reset"),
                    ControllerPromptItem(GamepadAction.CHANGE_SORT, "Sliders"),
                    ControllerPromptItem(GamepadAction.SELECT, "Save"),
                    ControllerPromptItem(GamepadAction.BACK, "Cancel"),
                ),
                style = ControllerHintStyle.OVERLAY,
            )

            if (slidersVisible) {
                AxisSlider("Scale", draft.scale, CrossbarLayoutAdjust.SCALE_MIN, CrossbarLayoutAdjust.SCALE_MAX, onScale)
                AxisSlider("Horizontal", draft.barLeftFraction, CrossbarLayoutAdjust.LEFT_MIN, CrossbarLayoutAdjust.LEFT_MAX, onHorizontal)
                AxisSlider("Vertical", draft.barTopFraction, CrossbarLayoutAdjust.TOP_MIN, CrossbarLayoutAdjust.TOP_MAX, onVertical)
                AxisSlider("Top bar", draft.headerScale, CrossbarLayoutAdjust.CHROME_MIN, CrossbarLayoutAdjust.CHROME_MAX, onHeader)
                AxisSlider("Footer", draft.footerScale, CrossbarLayoutAdjust.CHROME_MIN, CrossbarLayoutAdjust.CHROME_MAX, onFooter)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(onClick = onToggleSliders) {
                    Text(if (slidersVisible) "Hide Sliders" else "Sliders")
                }
                OutlinedButton(onClick = onReset) { Text("Reset") }
                Box(Modifier.width(1.dp))
                OutlinedButton(onClick = onCancel) { Text("Cancel") }
                Button(
                    onClick = onSave,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3A82F6)),
                ) { Text("Save") }
            }
        }
    }
}

@Composable
private fun AxisSlider(
    label: String,
    value: Float,
    min: Float,
    max: Float,
    onChange: (Float) -> Unit,
) {
    Column {
        Text(label, color = Color(0xFFB9C6DC), fontSize = 12.sp)
        Slider(
            value = value.coerceIn(min, max),
            onValueChange = onChange,
            valueRange = min..max,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF3A82F6),
                activeTrackColor = Color(0xFF3A82F6),
            ),
        )
    }
}
