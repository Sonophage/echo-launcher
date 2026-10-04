package com.echo.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.echo.core.ui.preview.CombinedPreviews
import com.echo.core.ui.preview.EchoPreview
import com.echo.core.ui.theme.LocalEchoColors

val CrossbarGlyphShadow = Shadow(Color(0xB3000000), Offset.Zero, 10f)

@Composable
fun CrossbarTouchButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val accent = LocalEchoColors.current.accentColor
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp))
            .border(1.5.dp, accent.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

@Composable
fun CrossbarBackTouchButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
) {
    CrossbarTouchButton(onClick = onClick, modifier = modifier, size = size) {
        Text(
            text = "◀",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            style = TextStyle(shadow = CrossbarGlyphShadow),
        )
    }
}

@Composable
fun CrossbarGlyphTouchButton(
    glyph: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
) {
    CrossbarTouchButton(onClick = onClick, modifier = modifier, size = size) {
        Text(
            text = glyph,
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            style = TextStyle(shadow = CrossbarGlyphShadow),
        )
    }
}

@Composable
fun CrossbarHeaderPill(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingGlyph: String? = null,
    focused: Boolean = false,

    background: Color = Color(0x1FFFFFFF),
) {
    val accent = LocalEchoColors.current.accentColor
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .then(
                if (focused) Modifier.border(1.5.dp, accent.copy(alpha = 0.9f), RoundedCornerShape(8.dp))
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        if (leadingGlyph != null) {
            Text(
                text = leadingGlyph,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                style = TextStyle(shadow = CrossbarGlyphShadow),
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = label,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            style = TextStyle(shadow = CrossbarGlyphShadow),
        )
    }
}

@Composable
fun CrossbarTouchPill(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingGlyph: String? = null,
) {
    val accent = LocalEchoColors.current.accentColor
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.5.dp, accent.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (leadingGlyph != null) "$leadingGlyph  $label" else label,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            style = TextStyle(shadow = CrossbarGlyphShadow),
        )
    }
}

@CombinedPreviews
@Composable
fun CrossbarTouchButtonPreview() {
    EchoPreview {
        Row(Modifier.padding(16.dp)) {
            CrossbarTouchButton(onClick = {}) {
                Text("A", color = Color.White)
            }
            Spacer(Modifier.width(12.dp))
            CrossbarBackTouchButton(onClick = {})
            Spacer(Modifier.width(12.dp))
            CrossbarGlyphTouchButton(glyph = "⋯", onClick = {})
        }
    }
}

@CombinedPreviews
@Composable
fun CrossbarPillPreview() {
    EchoPreview {
        Column(Modifier.padding(16.dp)) {
            CrossbarHeaderPill(label = "Options", onClick = {}, leadingGlyph = "⋯")
            Spacer(Modifier.height(12.dp))
            CrossbarHeaderPill(label = "Back", onClick = {}, leadingGlyph = "◀", focused = true)
            Spacer(Modifier.height(12.dp))
            CrossbarTouchPill(label = "Sort by Name", onClick = {}, leadingGlyph = "⇵")
        }
    }
}
