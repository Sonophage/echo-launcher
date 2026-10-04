package com.echo.core.ui.components

import com.echo.core.ui.theme.EchoTextStyle
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.echo.core.domain.model.ControllerDisplayType
import com.echo.core.domain.model.GamepadAction
import com.echo.core.ui.design.DesignUnits
import com.echo.core.ui.design.holdProgress
import com.echo.core.ui.design.holdRing
import com.echo.core.ui.theme.LocalEchoColors
import com.echo.core.ui.design.panelDesignUnits

@Immutable
data class HintAction(
    val action: GamepadAction,
    val label: String,
    val detail: String? = null,

    // over 0, A must be held this long; holding is true while the ring fills
    val holdMs: Long = 0L,
    val holding: Boolean = false,
)

fun primaryHint(items: List<ControllerPromptItem>, detail: String? = null): HintAction? =
    items.firstOrNull { it.tappableAction() == GamepadAction.SELECT }
        ?.let { HintAction(GamepadAction.SELECT, it.label, detail?.takeIf { d -> d.isNotBlank() }) }

@Composable
fun EchoHintBar(
    items: List<ControllerPromptItem>,
    modifier: Modifier = Modifier,
    onAction: ((GamepadAction) -> Unit)? = null,
    primary: HintAction? = null,

    centre: (@Composable () -> Unit)? = null,
    accent: Color = LocalEchoColors.current.accentColor,
    leading: (@Composable () -> Unit)? = null,
) {
    if (items.isEmpty() && primary == null && centre == null) return
    val pad = LocalPadPrompts.current
    val row = hintBarRow(items, primary, pad)
    val u = hintBarUnits()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(BarHeight)

            .background(Brush.verticalGradient(0f to Color.Transparent, 1f to ChromeScrim))
            .padding(start = chromeGutter(), end = if (primary == null) chromeGutter(end = true) else 0.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(u.dp(28))) {
            row.forEach { Hint(it, u, pad, onAction) }
        }

        Box(
            modifier = Modifier.weight(1f).padding(horizontal = u.dp(28)),
            contentAlignment = Alignment.Center,
        ) { centre?.invoke() }

        primary?.let { ActionTab(it, accent, leading, u, pad, onAction, Modifier.align(Alignment.Bottom)) }
    }
}

@Composable
private fun Hint(item: ControllerPromptItem, u: DesignUnits, pad: Boolean, onAction: ((GamepadAction) -> Unit)?) {
    val tap = item.tappableAction()?.takeIf { onAction != null }
    val style = LocalControllerPromptStyle.current
    val modifier = if (tap == null) Modifier else Modifier
        .then(if (pad) Modifier else Modifier.heightIn(min = 48.dp))
        .clip(RoundedCornerShape(6.dp))
        .clickable(role = Role.Button, onClickLabel = item.label) { onAction?.invoke(tap) }
        .padding(horizontal = 4.dp)
    if (pad) {
        ControllerPromptGlyphs(
            icons = item.fixedIcons ?: style.mappings.iconsFor(item.actions),
            label = item.label,
            family = style.family,
            labelColor = HintLabel,
            labelStyle = hintText(u),
            glyphSize = glyphFor(u, 22, 13),
            spacing = u.dp(8),
            modifier = modifier,
        )
    } else {
        Box(modifier, contentAlignment = Alignment.Center) { Text(item.label, color = HintLabel, style = hintText(u), maxLines = 1) }
    }
}

@Composable
private fun ActionTab(
    primary: HintAction,
    accent: Color,
    leading: (@Composable () -> Unit)?,
    u: DesignUnits,
    pad: Boolean,
    onAction: ((GamepadAction) -> Unit)?,
    modifier: Modifier,
) {
    val edge = lerp(accent, Color.White, 0.3f)
    val radius = u.dp(20)
    val shape = RoundedCornerShape(topStart = radius)
    val style = LocalControllerPromptStyle.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(u.dp(12), Alignment.CenterHorizontally),
        modifier = modifier
            .wrapContentHeight(Alignment.Bottom, unbounded = true)
            .heightIn(min = u.dp(68))
            .widthIn(min = u.dp(220))
            .clip(shape)
            .drawWithCache {
                val fill = Brush.linearGradient(
                    0f to accent.copy(alpha = 0.62f),
                    1f to lerp(accent, Color.Black, 0.4f).copy(alpha = 0.5f),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height),
                )
                val w = u.dp(2).toPx()
                val r = radius.toPx()
                val path = Path().apply {
                    moveTo(w / 2, size.height)
                    lineTo(w / 2, r)
                    arcTo(Rect(w / 2, w / 2, 2 * r - w / 2, 2 * r - w / 2), 180f, 90f, false)
                    lineTo(size.width, w / 2)
                }
                val line = Stroke(width = w)
                onDrawWithContent {
                    drawRect(fill)
                    drawContent()
                    drawPath(path, edge.copy(alpha = 0.22f), style = line)
                }
            }
            .clickable(enabled = onAction != null, role = Role.Button, onClickLabel = primary.label) { onAction?.invoke(primary.action) }
            .padding(start = u.dp(20), end = maxOf(u.dp(20), chromeGutter(end = true)), top = u.dp(6), bottom = u.dp(14)),
    ) {
        leading?.let { tile ->
            Box(Modifier.size(u.dp(34)).clip(RoundedCornerShape(u.dp(10))).background(accent), contentAlignment = Alignment.Center) { tile() }
        }
        if (pad) {
            val size = glyphFor(u, 24, 14)
            val progress = if (primary.holdMs > 0L) holdProgress(primary.holding, primary.holdMs) else 0f
            val ring = if (primary.holdMs > 0L) {
                Modifier.size(size + u.dp(10)).holdRing(progress, Color.White, Color.White.copy(alpha = 0.25f), u.dp(2))
            } else Modifier
            style.mappings.iconsFor(listOf(primary.action)).forEach { icon ->
                Box(ring, contentAlignment = Alignment.Center) {
                    if (style.family == ControllerDisplayType.GENERIC) {
                        Box(Modifier.clip(CircleShape).background(Color.White)) {
                            ControllerIconGlyph(icon, style.family, size = size, tint = TabInk)
                        }
                    } else {
                        ControllerIconGlyph(icon, style.family, size = size)
                    }
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(u.dp(1))) {
            Text(
                primary.label,
                color = Color.White,
                style = EchoTextStyle.copy(fontSize = u.sp(13), lineHeight = u.sp(13) * 1.15f, fontWeight = FontWeight.Medium),
                maxLines = 1,
            )
            primary.detail?.let {
                Text(
                    it,
                    color = Color.White.copy(alpha = 0.75f),
                    style = EchoTextStyle.copy(fontSize = u.sp(10), lineHeight = u.sp(10) * 1.15f, fontWeight = FontWeight.Light),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = u.dp(280)),
                )
            }
        }
    }
}

private fun hintText(u: DesignUnits) = EchoTextStyle.copy(fontSize = u.sp(13), fontWeight = FontWeight.Light)

@Composable
private fun hintBarUnits(): DesignUnits {
    val density = LocalDensity.current
    val window = LocalWindowInfo.current.containerSize
    val units = panelDesignUnits(window.width / density.density, window.height / density.density, density)
    return if (units.scale > 0f) units else DesignUnits(1f, density)
}

@Composable
private fun glyphFor(u: DesignUnits, glyphPx: Int, textPx: Int): Dp =
    maxOf(u.dp(glyphPx), with(LocalDensity.current) { u.sp(textPx).toDp() } * glyphPx / textPx)

internal fun hintBarRow(
    items: List<ControllerPromptItem>,
    primary: HintAction?,
    pad: Boolean,
): List<ControllerPromptItem> = items
    .filter { item ->
        val action = item.tappableAction()
        (primary == null || action != primary.action) && (pad || action != null)
    }
    .sortedBy { it.tappableAction() == GamepadAction.BACK }

private val BarHeight = HintBarHeight
private val HintLabel = Color.White.copy(alpha = 0.85f)
private val TabInk = Color(0xFF1A0D05)
