package com.echo.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.echo.core.ui.theme.StorefrontColors
import com.echo.core.ui.theme.menuCursorEdge

@Composable
fun EchoMonogram(
    label: String,
    size: Dp,
    corner: Dp,
    glyphSize: TextUnit,
    focused: Boolean,
    colors: StorefrontColors,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(corner)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(colors.accentHue.copy(alpha = if (focused) 0.38f else 0.14f))
            .then(if (focused) Modifier.border(2.dp, menuCursorEdge(), shape) else Modifier),
    ) {
        Text(
            text = label.trim().firstOrNull()?.uppercase() ?: "?",
            color = colors.textPrimary,
            fontSize = glyphSize,
            fontWeight = FontWeight.Bold,
        )
    }
}
