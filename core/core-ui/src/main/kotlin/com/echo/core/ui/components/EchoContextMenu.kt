package com.echo.core.ui.components

import com.echo.core.ui.design.RailPanelFill
import com.echo.core.ui.design.DesignUnits
import com.echo.core.ui.design.LocalBackdropWave
import com.echo.core.ui.design.LocalMenuBackdropArt
import com.echo.core.ui.design.PanelBase
import com.echo.core.ui.image.rememberBlurSourceModel
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.echo.core.ui.design.RAIL_PANEL_WIDTH
import com.echo.core.ui.design.panelDesignUnits
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.BoxWithConstraints
import com.echo.core.ui.theme.EchoTextStyle
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.echo.core.ui.design.menuBackdrop
import com.echo.core.ui.preview.CombinedPreviews
import com.echo.core.ui.preview.EchoPreview

const val NoMenuSelection = -1

private val TextDropShadow = Shadow(
    color = Color.Black.copy(alpha = 0.75f),
    offset = Offset(0f, 2f),
    blurRadius = 4f,
)

@Composable
fun <T> EchoContextMenuOverlay(
    state: MenuState<T>,
    onRowActivated: (index: Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    backdropArt: String? = LocalMenuBackdropArt.current,
    // drawn in place of the letter badge, for a menu whose rows are better shown another way (a swatch)
    rowBadge: (@Composable (index: Int, focused: Boolean) -> Unit)? = null,
) {
    val title = state.title
    val subtitle = state.subtitle
    val rows = state.rowsShown()
    val selectedIndex = state.selectedIndex ?: NoMenuSelection
    val listState = rememberLazyListState()

    LaunchedEffect(selectedIndex) {
        if (rows.isNotEmpty() && selectedIndex >= 0) {
            listState.animateScrollToItem(selectedIndex.coerceIn(0, rows.lastIndex))
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val u = panelDesignUnits(maxWidth.value, maxHeight.value, LocalDensity.current)
        Box(Modifier.fillMaxSize().clickable(onClick = onDismiss))
        RailPanelBacking(u, backdropArt)

        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(u.dp(RAIL_PANEL_WIDTH))
                .padding(top = StatusStripHeight, bottom = HintBarHeight, end = RailEdgeGap),
        ) {
            ContextMenuHeader(title, subtitle, u)

            LazyColumn(
                state = listState,
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(RailRowGap),
                contentPadding = PaddingValues(bottom = RailRowGap),
            ) {
                itemsIndexed(rows) { index, row ->
                    val dim = contextMenuDim(if (selectedIndex >= 0) kotlin.math.abs(index - selectedIndex) else 1, rows.lastIndex)

                    CrossbarRailRow(
                        label = row.label,
                        focused = index == selectedIndex,
                        destructive = row.isDestructive,
                        checked = row.checked,
                        opensSubmenu = row.opensSubmenu,
                        dim = dim,
                        u = u,
                        badge = rowBadge?.let { { it(index, index == selectedIndex) } },
                        onClick = { onRowActivated(index) },
                    )
                }
            }
        }
    }
}

// the right-hand rail every context menu and side-rail prompt sits on, drawn over a full-size parent.
// The Recent rail's backing (owner, 2026-10-04): the art behind, blurred, under the wave and the fill.
// owner, 2026-10-07: every context menu wears it; with no art it is the dark base and the wave
@Composable
fun RailPanelBacking(u: DesignUnits, backdropArt: String? = LocalMenuBackdropArt.current) {
    val panelWidth = u.dp(RAIL_PANEL_WIDTH)
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().drawWithContent { clipRect(left = size.width - panelWidth.toPx()) { this@drawWithContent.drawContent() } }) {
            Box(Modifier.fillMaxSize().background(PanelBase))
            if (backdropArt != null) {
                AsyncImage(
                    model = rememberBlurSourceModel(backdropArt),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().blur(u.dp(24)).graphicsLayer(alpha = 0.4f),
                )
            }
            LocalBackdropWave.current?.invoke()
        }
        Box(Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(panelWidth).background(RailPanelFill))
    }
}

@Composable
fun ContextMenuHeader(
    title: String,
    subtitle: String?,
    u: DesignUnits,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.End,
) {
    Column(modifier, horizontalAlignment = if (textAlign == TextAlign.Start) Alignment.Start else Alignment.End) {
        Text(
            text = title,
            color = Color.White.copy(alpha = 0.92f),
            fontSize = u.sp(20),
            fontWeight = FontWeight.Light,
            style = EchoTextStyle.copy(shadow = TextDropShadow),
            maxLines = 2,
            textAlign = textAlign,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = u.dp(RAIL_MAX_TEXT)),
        )
        ContextMenuEyebrow(subtitle.orEmpty(), u, textAlign = textAlign)
        Spacer(Modifier.height(u.dp(22)))
    }
}

@Composable
fun ContextMenuEyebrow(
    text: String,
    u: DesignUnits,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.End,
) {
    Text(
        text = text,
        color = Color.White.copy(alpha = 0.62f),
        fontSize = u.sp(11),
        fontWeight = FontWeight.Medium,
        letterSpacing = 1.sp,
        style = EchoTextStyle.copy(shadow = TextDropShadow),
        maxLines = 1,
        textAlign = textAlign,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.widthIn(max = u.dp(RAIL_MAX_TEXT)),
    )
}

@Composable
fun contextMenuDim(distance: Int, span: Int): Float {
    val dim by animateFloatAsState(CrossbarDim.smoothed(distance, span), tween(DimFadeMs), label = "railDim")
    return dim
}

fun Modifier.contextMenuRow(focused: Boolean, dim: Float, u: DesignUnits, onClick: () -> Unit): Modifier = this
    .alpha(if (focused) 1f else dim)
    .clip(RoundedCornerShape(u.dp(12)))
    .then(if (focused) Modifier.background(Color.White) else Modifier)
    .clickable(onClick = onClick)
    .padding(start = u.dp(14), end = u.dp(6), top = u.dp(6), bottom = u.dp(6))

fun contextMenuInk(focused: Boolean): Color = if (focused) RailInk else Color.White

@Composable
fun ContextMenuRowLabel(
    text: String,
    focused: Boolean,
    u: DesignUnits,
    modifier: Modifier = Modifier,
    color: Color = contextMenuInk(focused),
) {
    Text(
        text = text,
        color = color,
        fontSize = u.sp(15),
        fontWeight = if (focused) FontWeight.Bold else FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

@Composable
private fun CrossbarRailRow(
    label: String,
    focused: Boolean,
    destructive: Boolean,
    checked: Boolean,
    opensSubmenu: Boolean,
    dim: Float,
    u: DesignUnits,
    badge: (@Composable () -> Unit)?,
    onClick: () -> Unit,
) {
    val tint = if (destructive) RailDestructive else null
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.contextMenuRow(focused, dim, u, onClick),
    ) {
        ContextMenuRowLabel(
            text = label,
            focused = focused,
            u = u,
            color = if (destructive) RailDestructive else contextMenuInk(focused),
            modifier = Modifier.widthIn(max = u.dp(RAIL_MAX_TEXT)),
        )
        if (checked) {
            Spacer(Modifier.width(u.dp(12)))
            EchoCheckMark(
                contextMenuInk(focused),
                size = u.dp(18),
                shadow = TextDropShadow.color,
            )
        }
        Spacer(Modifier.width(u.dp(12)))
        if (badge != null) badge() else CrossbarRailBadge(label = label, u = u, filled = focused, opensSubmenu = opensSubmenu, tint = tint)
    }
}

@Composable
private fun CrossbarRailBadge(
    label: String,
    u: DesignUnits,
    filled: Boolean,
    opensSubmenu: Boolean = false,
    tint: Color? = null,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(u.dp(32))
            .clip(RoundedCornerShape(u.dp(8)))
            .background(
                when {
                    filled -> tint ?: RailInk
                    opensSubmenu -> Color.White
                    tint != null -> tint.copy(alpha = 0.22f)
                    else -> Color.White.copy(alpha = 0.12f)
                },
            ),
    ) {
        Text(
            text = label.trim().firstOrNull()?.uppercase() ?: "?",
            color = when {
                filled -> Color.White
                opensSubmenu -> RailInk
                else -> Color.White.copy(alpha = 0.85f)
            },
            fontSize = u.sp(14),
            fontWeight = FontWeight.Bold,
        )
    }
}

val RailIcon = 29.dp
val RailEdgeGap = 24.dp
val RailRowGap = 13.dp
private const val DimFadeMs = 160
// the menu's sizes are panel design units, as the Recent rail's are, so the two read alike
internal const val RAIL_MAX_TEXT = 380
internal val RailInk = Color(0xFF1A0C03)
private val RailDestructive = Color(0xFFE2606A)

@CombinedPreviews
@Composable
fun EchoContextMenuPreview() {
    val rows = listOf(
        MenuRow("play", "Play"),
        MenuRow("info", "Information"),
        MenuRow("fav", "Add to Favorites", checked = true),
        MenuRow<String>(null, "Settings", MenuGroup.SETTINGS, opensSubmenu = true),
        MenuRow("remove", "Remove From Library", isDestructive = true),
    )
    EchoPreview {
        EchoContextMenuOverlay(
            state = MenuState("Gran Turismo 4", rows, subtitle = "Library", selectedIndex = 1),
            onRowActivated = {},
            onDismiss = {},
        )
    }
}
