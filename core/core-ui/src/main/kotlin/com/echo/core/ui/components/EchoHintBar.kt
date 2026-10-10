package com.echo.core.ui.components

import androidx.compose.foundation.layout.wrapContentWidth
import com.echo.core.ui.design.NeckFlare
import com.echo.core.ui.design.drawIslandNeck
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.animation.scaleOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
    // what a finger on the card does, when it is not what the button does (Y held is Resume, a tap of Y Options)
    val onTouch: (() -> Unit)? = null,
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
    // owner, 2026-10-06: the screen's filter (TriggerFilter) leads the footer, at its far left
    filter: (@Composable () -> Unit)? = null,
) {
    if (items.isEmpty() && primary == null && centre == null && filter == null) return
    val pad = LocalPadPrompts.current
    val shownItems = minimalHintItems(items, LocalMinimalHints.current, pad)
    val (always, contextual) = hintBarSides(hintBarRow(shownItems, primary, pad))
    val u = hintBarUnits()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(HintBarHeight)

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
            filter?.invoke()
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

// owner, 2026-10-07: Minimal hints (a quick setting) drops the footer's fixed button hints; the A card, the
// filter and the centre card stay, as they say what is on screen. Touch keeps Back, its only way back
val LocalMinimalHints = androidx.compose.runtime.compositionLocalOf { false }

internal fun minimalHintItems(items: List<ControllerPromptItem>, minimal: Boolean, pad: Boolean): List<ControllerPromptItem> =
    if (!minimal) items else items.filter { !pad && it.tappableAction() == GamepadAction.BACK }

@Composable
private fun Hint(item: ControllerPromptItem, u: DesignUnits, pad: Boolean, onAction: ((GamepadAction) -> Unit)?) {
    val tap = item.tappableAction()?.takeIf { onAction != null }
    // touch has no B button and the system bars are hidden, so Back is a real button (owner, 2026-10-04);
    // a controller keeps its glyph hint
    if (!pad && tap == GamepadAction.BACK) return BackButton(item.label, u) { onAction?.invoke(tap) }
    val style = LocalControllerPromptStyle.current
    val modifier = if (tap == null) Modifier else Modifier
        .then(if (pad) Modifier else Modifier.heightIn(min = 48.dp))
        .clip(RoundedCornerShape(6.dp))
        .clickable(role = Role.Button, onClickLabel = item.label) { onAction?.invoke(tap) }
        .padding(horizontal = 4.dp)
    val icon = item.icon
    if (pad) {
        Row(modifier.semantics { contentDescription = item.label }, verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(u.dp(8))) {
            ControllerPromptGlyphs(
                icons = item.fixedIcons ?: style.mappings.iconsFor(item.actions),
                label = if (icon != null) "" else item.label,
                family = style.family,
                labelColor = HintLabel,
                labelStyle = hintText(u),
                glyphSize = glyphFor(u, 22, 13),
                spacing = u.dp(8),
            )
            if (icon != null) androidx.compose.material3.Icon(icon, null, Modifier.size(glyphFor(u, 22, 13)), tint = HintLabel)
        }
    } else {
        Box(modifier.semantics { contentDescription = item.label }, contentAlignment = Alignment.Center) {
            if (icon != null) androidx.compose.material3.Icon(icon, null, Modifier.size(u.dp(24)), tint = HintLabel)
            else Text(item.label, color = HintLabel, style = hintText(u), maxLines = 1)
        }
    }
}

@Composable
private fun BackButton(label: String, u: DesignUnits, onClick: () -> Unit) {
    Row(
        Modifier
            .wrapContentHeight(unbounded = true)
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(u.dp(22)))
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick)
            .padding(end = u.dp(10)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(u.dp(10)),
    ) {
        Box(
            Modifier.size(u.dp(36)).clip(CircleShape).background(Color.White.copy(alpha = 0.12f)).border(1.5.dp, HintLabel, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(u.dp(14))) {
                val w = 2.dp.toPx()
                val mid = size.height / 2
                drawLine(HintLabel, Offset(size.width, mid), Offset(0f, mid), w, StrokeCap.Round)
                drawLine(HintLabel, Offset(0f, mid), Offset(size.width * 0.45f, 0f), w, StrokeCap.Round)
                drawLine(HintLabel, Offset(0f, mid), Offset(size.width * 0.45f, size.height), w, StrokeCap.Round)
            }
        }
        Text(label, color = HintLabel, style = hintText(u), maxLines = 1)
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
    // owner, 2026-10-05: the centre keeps one width whether the orb is alone or its card is out, so the hints on
    // either side never move; the card stands over the bar, wider than its slot when it needs to be
    if (secondary == null && primary.holdMs == 0L) {
        return Box(modifier.width(u.dp(ACTION_SLOT)), contentAlignment = Alignment.BottomCenter) {
            RestOrb(primary, accent, u, pad, onAction, Modifier)
        }
    }
    // owner, 2026-10-04: A is only the A button until the hold starts; then the card rises with the item's info.
    // The press lives on this wrapper, so bringing the card out does not cut a touch hold short.
    // owner, 2026-10-05: as the top bar's islands do, the orb stays and the card rises above it, joined to it
    // so the two read as one shape
    val wrapperHold = secondary == null
    var pressing by remember { mutableStateOf(false) }
    val cardOut = secondary != null || primary.holding || pressing
    val fill = actionCardFill(accent)
    Box(
        modifier.width(u.dp(ACTION_SLOT)).then(
            if (wrapperHold && onAction != null) Modifier.pressAndHold(primary.holdMs, primary.label, { pressing = it }) { onAction(primary.action) } else Modifier,
        ),
        contentAlignment = Alignment.BottomCenter,
    ) {
        AnimatedVisibility(
            visible = cardOut,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(160)),
            modifier = Modifier.wrapContentHeight(Alignment.Bottom, unbounded = true),
        ) { ActionBridge(fill, u) }
        RestOrb(primary, accent, u, pad, onAction = null, modifier = Modifier, joinedFill = fill.takeIf { cardOut })
        AnimatedVisibility(
            visible = cardOut,
            enter = fadeIn(tween(200)) + scaleIn(tween(260), initialScale = 0.3f, transformOrigin = TransformOrigin(0.5f, 1f)),
            exit = fadeOut(tween(160)) + scaleOut(tween(180), targetScale = 0.3f, transformOrigin = TransformOrigin(0.5f, 1f)),
            modifier = Modifier.wrapContentWidth(unbounded = true).wrapContentHeight(Alignment.Bottom, unbounded = true).padding(bottom = u.dp(ORB_BOTTOM + ORB + CARD_GAP)),
        ) {
            ActionCard(primary, accent, leading, u, pad, onAction, Modifier, secondary, pressedOutside = pressing, ownGesture = !wrapperHold)
        }
    }
}

private const val ORB = 44

// how far the A orb stands above the screen's bottom edge, higher than the footer itself: text set over the footer
// clears this, not HintBarHeight
val HintOrbClearance: Dp
    @Composable get() = hintBarUnits().dp(ORB_BOTTOM + ORB)
// the centre's fixed width; the card is wider and stands over the hints beside it while it is out
private const val ACTION_SLOT = 180
private const val ORB_BOTTOM = 16
// the card sits just above the orb (owner, 2026-10-05: closer to the icon)
private const val CARD_GAP = 2

private fun actionCardFill(accent: Color): Color = lerp(accent, Color.Black, 0.35f)

// the neck between the A orb and the card above it (drawIslandNeck), flaring out on both sides
@Composable
private fun ActionBridge(fill: Color, u: DesignUnits) {
    val orb = u.dp(ORB)
    // a long, soft flare (owner, 2026-10-05: curvier and smoother)
    val flare = u.dp(40)
    Canvas(Modifier.width(orb + flare * 2).height(u.dp(ORB_BOTTOM + ORB + CARD_GAP) + 1.dp)) {
        val r = orb.toPx() / 2f
        val cy = size.height - u.dp(ORB_BOTTOM + ORB / 2).toPx()
        drawIslandNeck(fill, Offset(size.width / 2f, cy), r, 0f, flare.toPx(), NeckFlare.BOTH)
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
    // a whole card above the orb, its fill solid so the bridge to the orb matches
    val shape = RoundedCornerShape(radius)
    val fillColour = actionCardFill(accent)
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
                val w = u.dp(2).toPx()
                val r = radius.toPx()
                val path = Path().apply {
                    addRoundRect(androidx.compose.ui.geometry.RoundRect(w / 2, w / 2, size.width - w / 2, size.height - w / 2, androidx.compose.ui.geometry.CornerRadius(r - w / 2, r - w / 2)))
                }
                val line = Stroke(width = w)
                val measure = PathMeasure().apply { setPath(path, false) }
                // touch has no A glyph to ring, so the hold traces the tab's edge instead
                val trace = Path().also { if (!pad) measure.getSegment(0f, measure.length * progress, it, true) }
                onDrawWithContent {
                    drawRect(fillColour)
                    drawContent()
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
            .padding(horizontal = u.dp(20), vertical = u.dp(10)),
    ) {
        secondary?.let { second ->
            var secondPressing by remember { mutableStateOf(false) }
            val secondProgress = if (second.holdMs > 0L) holdProgress(second.holding || secondPressing, second.holdMs) else 0f
            Row(
                Modifier
                    .clip(RoundedCornerShape(u.dp(10)))
                    .then(
                        if (second.holdMs > 0L && onAction != null) {
                            Modifier.pressAndHold(second.holdMs, second.label, { secondPressing = it }) { second.onTouch?.invoke() ?: onAction(second.action) }
                        } else {
                            Modifier.clickable(enabled = onAction != null, role = Role.Button, onClickLabel = second.label) { second.onTouch?.invoke() ?: onAction?.invoke(second.action) }
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
    // joined to its card: the card's solid fill, and no ring, so the two are one shape
    joinedFill: Color? = null,
) {
    val edge = lerp(accent, Color.White, 0.3f)
    val style = LocalControllerPromptStyle.current
    var presses by remember { mutableIntStateOf(0) }
    Row(
        modifier
            // the bar is shorter than the orb on a large screen; it rises out of it like the card, so it stays round
            .wrapContentHeight(Alignment.Bottom, unbounded = true)
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
                .then(
                    if (joinedFill != null) Modifier.background(joinedFill)
                    else Modifier
                        .background(Brush.linearGradient(listOf(accent.copy(alpha = 0.62f), lerp(accent, Color.Black, 0.4f).copy(alpha = 0.5f))))
                        .border(1.5.dp, edge, CircleShape),
                ),
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
    // the footer's own size (LocalChromeScale) grows or shrinks everything in it with the bar
    val footer = LocalChromeScale.current.footer
    return if (units.scale > 0f) DesignUnits(units.scale * footer, density, units.square) else DesignUnits(footer, density)
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

private val CardEnter = fadeIn(tween(220)) + slideInVertically(tween(260)) { it / 2 }
private val CardExit = fadeOut(tween(160)) + slideOutVertically(tween(180)) { it / 2 }
private val HintLabel = Color.White.copy(alpha = 0.85f)
private val TabInk = Color(0xFF1A0D05)

// owner, 2026-10-06: a screen's filters, tabs or sorts in the footer's left side: one LT/RT mark and the current
// one's word. The triggers step it; a tap steps to the next
@Composable
fun TriggerFilter(label: String, modifier: Modifier = Modifier, onTapped: () -> Unit) {
    val u = hintBarUnits()
    val pad = LocalPadPrompts.current
    Row(
        modifier
            .clip(RoundedCornerShape(u.dp(8)))
            .clickable(onClickLabel = label, onClick = onTapped)
            .padding(horizontal = u.dp(4), vertical = u.dp(4))
            .semantics { contentDescription = label },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(u.dp(10)),
    ) {
        if (pad) TriggerPairGlyph(glyphFor(u, 22, 13))
        Text(label, color = Color.White, style = hintText(u).copy(fontWeight = FontWeight.Medium), maxLines = 1)
    }
}

// LT and RT as one mark: the two trigger glyphs overlapping, in the pad's own art and mapping
@Composable
fun TriggerPairGlyph(size: Dp) {
    val style = LocalControllerPromptStyle.current
    ControllerPromptGlyphs(
        icons = style.mappings.iconsFor(listOf(GamepadAction.PREV_CATEGORY, GamepadAction.NEXT_CATEGORY)),
        label = "",
        family = style.family,
        glyphSize = size,
        glyphSpacing = -(size * 0.45f),
    )
}
