package com.echo.core.ui.design

import com.echo.core.ui.theme.EchoTextStyle
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.echo.core.domain.model.GamepadAction
import com.echo.core.ui.components.ControllerPrompt
import com.echo.core.ui.components.LocalPadPrompts

@Composable
fun PanelButton(button: GamepadAction, label: String, u: DesignUnits, onClick: () -> Unit) {
    val primary = button == GamepadAction.SELECT
    val ink = if (primary) Color(0xFF0A0A0A) else Color.White
    Box(
        Modifier
            .height(u.dp(52))
            .clip(RoundedCornerShape(u.dp(26)))
            .background(if (primary) Color.White else Color.White.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(horizontal = u.dp(if (primary) 26 else 22)),
        contentAlignment = Alignment.Center,
    ) {
        val labelStyle = EchoTextStyle.copy(fontSize = u.sp(15), fontWeight = if (primary) FontWeight.Medium else FontWeight.Normal)
        if (LocalPadPrompts.current) {
            ControllerPrompt(button, label, labelColor = ink, labelStyle = labelStyle, glyphSize = u.dp(20), spacing = u.dp(10))
        } else {
            Text(label, color = ink, style = labelStyle)
        }
    }
}
