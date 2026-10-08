package com.echo.core.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.echo.core.ui.theme.menuCursorEdge
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EchoDetailLaunchButton(
    label: String,
    icon: ImageVector?,
    focused: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,

    fill: Color = DetailButtonRest,

    compact: Boolean = false,
) {
    val shape = RoundedCornerShape(percent = 50)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = if (compact) 32.dp else 58.dp)
            .clip(shape)

            .background(
                when {
                    focused -> DetailButtonFocusFill
                    compact && fill == DetailButtonRest -> DetailButtonRestCompact
                    else -> fill
                }
            )

            .border(
                width = if (focused) 2.dp else 0.dp,
                color = if (focused) menuCursorEdge() else Color.Transparent,
                shape = shape,
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(
                vertical = if (compact) 6.dp else 15.dp,
                horizontal = if (compact) 12.dp else 26.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val content = if (focused) DetailButtonFocusText else DetailTextPrimary
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(if (compact) 16.dp else 24.dp),
            )
            Spacer(Modifier.width(if (compact) 8.dp else 12.dp))
        }
        Text(
            text = label,
            color = content,
            fontSize = if (compact) 13.sp else 19.sp,

            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
