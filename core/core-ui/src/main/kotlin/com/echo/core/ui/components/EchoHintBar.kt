package com.echo.core.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith

import com.echo.core.ui.design.holdOutline
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.alpha
import com.echo.core.ui.design.echoPulse
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.foundation.Canvas
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.border
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.PathMeasure
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
import com.echo.core.ui.design.pressAndHold
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

    // the action orb's level 2 (kit 06): a second action, Resume on Y, left of the primary
    secondary: HintAction? = null,
) {
    if (items.isEmpty() && primary == null && centre == null) return
    val pad = LocalPadPrompts.current
    val (always, contextual) = hintBarSides(hintBarRow(items, primary, pad))
    val u = hintBarUnits()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(BarHeight)

            .background(Brush.verticalGradient(0f to Color.Transparent, 1f to ChromeScrim))
            .padding(start = chromeGutter(), end = chromeGutter(end = true)),
    ) {
        // owner, 2026-10-04: the hints that are always there on the left, the card in the centre,
        // what this screen adds on the right
        Row(
            Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(u.dp(28)),
        ) {
            always.forEach { Hint(it, u, pad, onAction) }
            centre?.let { Box(Modifier.weight(1f, fill = false)) { it() } }
        }

        // the card comes in rather than appearing (owner, 2026-10-04); the last one stays for its exit
        var shown by remember { mutableStateOf(primary) }
        if (primary != null) shown = primary
        AnimatedVisibility(
            visible = primary != null,
            enter = CardEnter,
            exit = CardExit,
            modifier = Modifier.align(Alignment.Bottom),
        ) {
            shown?.let { ActionTab(it, accent, leading, u, pad, onAction, Modifier, secondary) }
        }

        Row(
            Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(u.dp(28), Alignment.End),
        ) {
            contextual.forEach { Hint(it, u, pad, onAction) }
        }
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
    secondary: HintAction? = null,
) {
    if (secondary != null) return ActionCard(primary, accent, leading, u, pad, onAction, modifier, secondary)
    if (primary.holdMs == 0L) return RestOrb(primary, accent, u, pad, onAction, modifier)
    // owner, 2026-10-04: A is only the A button until the hold starts; then the card rises with the item's info.
    // The press lives on this wrapper, so swapping the orb for the card does not cut a touch hold short
    var pressing by remember { mutableStateOf(false) }
    val active = primary.holding || pressing
    Box(
        modifier.then(
            if (onAction != null) Modifier.pressAndHold(primary.holdMs, primary.label, { pressing = it }) { onAction(primary.action) } else Modifier,
        ),
        contentAlignment = Alignment.BottomCenter,
    ) {
        AnimatedContent(
            targetState = active,
            transitionSpec = { CardEnter togetherWith CardExit using SizeTransform(clip = false) },
            contentAlignment = Alignment.BottomCenter,
            label = "holdCard",
        ) { card ->
            if (card) {
                ActionCard(primary, accent, leading, u, pad, onAction, Modifier, null, pressedOutside = pressing, ownGesture = false)
            } else {
                RestOrb(primary, accent, u, pad, onAction = null, modifier = Modifier)
            }
        }
    }
}

@Composable
private fun ActionCard(
    primary: HintAction,
    accent: Color,
    leading: (@Composable () -> Unit)?,
    u: DesignUnits,
    pad: Boolean,
    onAction: ((GamepadAction) -> Unit)?,
    modifier: Modifier,
    secondary: HintAction? = null,
    pressedOutside: Boolean = false,
    ownGesture: Boolean = true,
) {
    val edge = lerp(accent, Color.White, 0.3f)
    var pressing by remember { mutableStateOf(false) }
    val progress = if (primary.holdMs > 0L) holdProgress(primary.holding || pressing || pressedOutside, primary.holdMs) else 0f
    val radius = u.dp(20)
    val shape = RoundedCornerShape(topStart = radius, topEnd = radius)
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
                    lineTo(size.width - r, w / 2)
                    arcTo(Rect(size.width - 2 * r + w / 2, w / 2, size.width - w / 2, 2 * r - w / 2), 270f, 90f, false)
                    lineTo(size.width - w / 2, size.height)
                }
                val line = Stroke(width = w)
                val measure = PathMeasure().apply { setPath(path, false) }
                // touch has no A glyph to ring, so the hold traces the tab's edge instead
                val trace = Path().also { if (!pad) measure.getSegment(0f, measure.length * progress, it, true) }
                onDrawWithContent {
                    drawRect(fill)
                    drawContent()
                    drawPath(path, edge.copy(alpha = 0.22f), style = line)
                    drawPath(trace, Color.White, style = Stroke(width = w * 1.5f, cap = StrokeCap.Round))
                }
            }
            .then(
                when {
                    !ownGesture -> Modifier
                    primary.holdMs > 0L && onAction != null ->
                        Modifier.pressAndHold(primary.holdMs, primary.label, { pressing = it }) { onAction(primary.action) }
                    else -> Modifier.clickable(enabled = onAction != null, role = Role.Button, onClickLabel = primary.label) { onAction?.invoke(primary.action) }
                }
            )
            .padding(start = u.dp(20), end = u.dp(20), top = u.dp(6), bottom = u.dp(14)),
    ) {
        secondary?.let { second ->
            var secondPressing by remember { mutableStateOf(false) }
            val secondProgress = if (second.holdMs > 0L) holdProgress(second.holding || secondPressing, second.holdMs) else 0f
            Row(
                Modifier
                    .clip(RoundedCornerShape(u.dp(10)))
                    .then(
                        if (second.holdMs > 0L && onAction != null) {
                            Modifier.pressAndHold(second.holdMs, second.label, { secondPressing = it }) { onAction(second.action) }
                        } else {
                            Modifier.clickable(enabled = onAction != null, role = Role.Button, onClickLabel = second.label) { onAction?.invoke(second.action) }
                        }
                    )
                    .then(if (!pad && second.holdMs > 0L) Modifier.holdOutline(secondProgress, Color.White, u.dp(2)) else Modifier)
                    .alpha(0.9f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(u.dp(10)),
            ) {
                if (pad) style.mappings.iconsFor(listOf(second.action)).firstOrNull()?.let {
                    val size = glyphFor(u, 22, 13)
                    val ring = if (second.holdMs > 0L) {
                        Modifier.size(size + u.dp(10)).holdRing(secondProgress, Color.White, Color.White.copy(alpha = 0.25f), u.dp(2))
                    } else Modifier
                    Box(ring, contentAlignment = Alignment.Center) { ControllerIconGlyph(it, style.family, size = size) }
                }
                Column(verticalArrangement = Arrangement.spacedBy(u.dp(1))) {
                    Text(second.label, color = Color.White, style = EchoTextStyle.copy(fontSize = u.sp(13), fontWeight = FontWeight.Normal), maxLines = 1)
                    second.detail?.let {
                        Text(it, color = Color.White.copy(alpha = 0.7f), style = EchoTextStyle.copy(fontSize = u.sp(10), fontWeight = FontWeight.Light),
                            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = u.dp(180)))
                    }
                }
            }
            Box(Modifier.width(1.dp).height(u.dp(34)).background(Color.White.copy(alpha = 0.22f)))
        }
        leading?.let { tile ->
            Box(Modifier.size(u.dp(34)).clip(RoundedCornerShape(u.dp(10))).background(accent), contentAlignment = Alignment.Center) { tile() }
        }
        if (pad) {
            val size = glyphFor(u, 24, 14)
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

// kit 06 at rest: only the glowing A (owner, 2026-10-04: no "Open" beside it); a launch's card rises on the hold
@Composable
private fun RestOrb(
    primary: HintAction,
    accent: Color,
    u: DesignUnits,
    pad: Boolean,
    onAction: ((GamepadAction) -> Unit)?,
    modifier: Modifier,
) {
    val edge = lerp(accent, Color.White, 0.3f)
    val style = LocalControllerPromptStyle.current
    var presses by remember { mutableIntStateOf(0) }
    Row(
        modifier
            .padding(bottom = u.dp(16))
            // inside the hold wrapper it has no action; even a disabled clickable takes the finger
            // down, so the wrapper's hold never started (owner, 2026-10-04)
            .then(
                if (onAction == null) Modifier
                else Modifier.clickable(role = Role.Button, onClickLabel = primary.label, indication = null, interactionSource = null) {
                    presses++
                    onAction(primary.action)
                },
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(u.dp(12)),
    ) {
        Box(
            Modifier
                .size(u.dp(44))
                .echoPulse(presses, edge)
                .drawBehind { drawCircle(Brush.radialGradient(listOf(edge.copy(alpha = 0.45f), Color.Transparent), center, size.minDimension * 0.8f)) }
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(accent.copy(alpha = 0.62f), lerp(accent, Color.Black, 0.4f).copy(alpha = 0.5f))))
                .border(1.5.dp, edge, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(u.dp(26)).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                val icon = style.mappings.iconsFor(listOf(primary.action)).firstOrNull()
                if (pad && icon != null) {
                    ControllerIconGlyph(icon, style.family, size = u.dp(24), tint = TabInk)
                } else {
                    Canvas(Modifier.size(u.dp(10))) {
                        drawPath(Path().apply { moveTo(size.width * 0.15f, 0f); lineTo(size.width, size.height / 2); lineTo(size.width * 0.15f, size.height); close() }, TabInk)
                    }
                }
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

// Home and Back are on every screen; everything else belongs to the screen it is on
internal fun hintBarSides(row: List<ControllerPromptItem>): Pair<List<ControllerPromptItem>, List<ControllerPromptItem>> =
    row.partition { it.tappableAction() == GamepadAction.HOME || it.tappableAction() == GamepadAction.BACK }

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
private val CardEnter = fadeIn(tween(220)) + slideInVertically(tween(260)) { it / 2 }
private val CardExit = fadeOut(tween(160)) + slideOutVertically(tween(180)) { it / 2 }
private val HintLabel = Color.White.copy(alpha = 0.85f)
private val TabInk = Color(0xFF1A0D05)
