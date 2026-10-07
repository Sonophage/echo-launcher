package com.echo.feature.crossbar.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import com.echo.core.ui.components.ControllerPrompt
import com.echo.core.ui.components.LocalPadPrompts
import com.echo.core.ui.design.VhsAppFace
import com.echo.core.ui.design.VhsCase
import com.echo.core.ui.design.VhsCoverArt
import com.echo.core.ui.design.caseShape
import com.echo.core.ui.design.ShelfRoom
import com.echo.core.ui.design.filmGrain
import com.echo.core.ui.design.coverRings
import com.echo.core.ui.design.roomGlow
import com.echo.core.ui.design.sideways
import com.echo.core.ui.design.vignette
import com.echo.core.ui.design.wallStripes
import com.echo.feature.crossbar.viewmodel.SearchKind
import com.echo.core.ui.theme.EchoTextStyle
import com.echo.core.common.format.playTimeLabel
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Games
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import coil3.compose.AsyncImage
import com.echo.core.domain.model.GamepadAction
import com.echo.core.ui.components.ControllerPromptItem
import com.echo.core.ui.components.HintAction
import com.echo.core.ui.components.HintBarHeight
import com.echo.core.ui.components.EchoHintBar
import com.echo.core.ui.components.TriggerFilter
import com.echo.core.ui.components.EchoSearchField
import com.echo.core.ui.components.StatusStripHeight
import com.echo.core.ui.design.DesignUnits
import com.echo.core.ui.image.rememberArtworkModel
import com.echo.core.ui.image.rememberBlurSourceModel
import com.echo.core.ui.theme.deriveStorefrontColors
import com.echo.feature.crossbar.viewmodel.SearchScope
import com.echo.feature.crossbar.viewmodel.SearchState
import com.echo.feature.crossbar.viewmodel.CrossbarItem
import com.echo.feature.crossbar.viewmodel.CrossbarItemType
import com.echo.feature.crossbar.viewmodel.isInstalledApp
import com.echo.feature.crossbar.viewmodel.primaryVerbFor
import com.echo.core.common.format.relativeTime
import com.echo.core.ui.icons.rememberAppIcon
import com.echo.core.ui.design.panelDesignUnits
import com.echo.core.ui.design.PanelButton

// owner, 2026-10-05: search is the "Drawer and Search Variations" design's 6b, the search shelf: a pegboard room lit
// by the selected result's own art, the field and the kind filter across the top, what the selected result is,
// and the results standing on one shelf, the selected one out as a whole case and the rest as spines
@Composable
fun SearchScreen(
    state: SearchState,
    onQueryChange: (String) -> Unit,
    onActivateAt: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onFocusAt: (Int) -> Unit = {},
    onOptionsAt: (Int) -> Unit = {},
    onKindPicked: (SearchKind?) -> Unit = {},
    // the crossbar's wave, which runs behind the shelf (owner, 2026-10-05: in place of the design's pegboard)
    waveStyle: com.echo.core.ui.wave.WaveStyle = com.echo.core.ui.wave.WaveStyle.OFF,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    val empty = state.rows.singleOrNull()?.takeIf { it.type == CrossbarItemType.EMPTY }
    val focused = state.rows.getOrNull(state.selectedIndex)?.takeIf { empty == null }
    val icon = rememberAppIcon(focused?.packageName?.takeIf { focused.isInstalledApp })
    val art = focused?.let { if (it.isInstalledApp) it.coverUri else it.backdropArt.firstOrNull() ?: it.shelfCoverArt }

    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .background(ShelfRoom)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .filmGrain(0.16f),
    ) {
        val u = panelDesignUnits(maxWidth.value, maxHeight.value, LocalDensity.current)
        val imeUp = WindowInsets.ime.getBottom(LocalDensity.current) > 0

        if (art != null) {
            AsyncImage(
                model = rememberBlurSourceModel(art),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().blur(u.dp(28)).graphicsLayer(alpha = 0.35f),
            )
        } else {
            Box(Modifier.fillMaxSize().roomGlow(icon?.color ?: SearchTint, Offset(0.5f, 0.35f)))
        }
        if (waveStyle.drawsWave) Box(Modifier.fillMaxSize().graphicsLayer(alpha = 0.5f)) { com.echo.core.ui.wave.WaveLayers(waveStyle) }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to ShelfRoom.copy(alpha = 0.55f), 0.45f to ShelfRoom.copy(alpha = 0.2f), 1f to ShelfRoom.copy(alpha = 0.85f))))
        Box(Modifier.fillMaxSize().vignette(0.7f))

        Column(
            Modifier
                .fillMaxSize()
                .padding(top = StatusStripHeight + u.dp(8), bottom = if (imeUp) 8.dp else HintBarHeight)
                .then(if (imeUp) Modifier.imePadding() else Modifier),
            verticalArrangement = Arrangement.spacedBy(u.dp(12)),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                EchoSearchField(
                    query = state.query,
                    active = true,
                    focusRequester = focusRequester,
                    placeholder = state.scope.label,
                    onActivate = {},
                    onQueryChange = onQueryChange,
                    // Quick Search runs on the keyboard's Enter too
                    onDone = { if (state.scope == SearchScope.WEB && focused != null) onActivateAt(state.selectedIndex) },
                    colors = deriveStorefrontColors().copy(
                        searchField = Color.Black.copy(alpha = 0.45f),
                        searchBorder = Color.White.copy(alpha = 0.3f),
                        textPrimary = Color.White,
                        textSecondary = Color.White,
                    ),
                    modifier = Modifier.width(u.dp(480)),
                )
                if (state.total > 0) {
                    Text("${state.total} result${if (state.total == 1) "" else "s"}", color = Color.White.copy(alpha = 0.6f), fontSize = u.sp(13),
                        modifier = Modifier.padding(start = u.dp(16)))
                }
            }
            when {
                empty != null -> Box(Modifier.padding(horizontal = u.dp(64))) { EmptyNotice(empty, u) }
                focused != null -> {
                    if (!imeUp) Info(focused, u, onOpen = { onActivateAt(state.selectedIndex) }, onOptions = { onOptionsAt(state.selectedIndex) }.takeIf { state.scope != SearchScope.WEB })
                    Shelf(state, u, onActivateAt, onFocusAt, Modifier.weight(1f).fillMaxWidth())
                }
            }
        }

        if (!imeUp) {
            EchoHintBar(
                // owner, 2026-10-06: the kind filter is the footer's, LT/RT and the current kind with its count
                filter = searchKindFilter(state)?.let { (label, next) -> { TriggerFilter(label) { onKindPicked(next) } } },
                items = listOfNotNull(
                    ControllerPromptItem(GamepadAction.BACK, "Close"),
                    ControllerPromptItem(GamepadAction.SELECT, "Open"),
                    ControllerPromptItem(GamepadAction.OPEN_CONTEXT_MENU, "Options").takeIf { state.scope != SearchScope.WEB },
                ),
                modifier = Modifier.align(Alignment.BottomCenter),
                primary = focused?.let { HintAction(GamepadAction.SELECT, primaryVerbFor(it) ?: "Open", listOfNotNull(it.title, it.subtitle).filter { t -> t.isNotBlank() }.joinToString(" · ")) },
                onAction = { action ->
                    when (action) {
                        GamepadAction.BACK -> onBack()
                        GamepadAction.SELECT -> state.selectedIndex.takeIf { focused != null }?.let(onActivateAt)
                        GamepadAction.OPEN_CONTEXT_MENU -> state.selectedIndex.takeIf { focused != null }?.let(onOptionsAt)
                        else -> Unit
                    }
                },
            )
        }
    }
}

// the kind filter's word (All, then each kind the results hold, with its count) and the kind a tap moves to
internal fun searchKindFilter(state: SearchState): Pair<String, SearchKind?>? {
    if (state.kindCounts.size <= 1) return null
    val kinds = listOf<SearchKind?>(null) + state.kindCounts.map { it.first }
    val at = kinds.indexOf(state.kind).coerceAtLeast(0)
    val label = state.kind?.let { k -> "${k.noun.replaceFirstChar { it.uppercase() }} ${state.kindCounts.first { it.first == k }.second}" }
        ?: "All ${state.total}"
    return label to kinds[(at + 1) % kinds.size]
}


// what the selected result is: its kind, title, a pill of facts and when it was last opened on the left; what is
// known about it and its buttons on the right
@Composable
private fun Info(row: CrossbarItem, u: DesignUnits, onOpen: () -> Unit, onOptions: (() -> Unit)?) {
    val (kind, detail) = kindAndDetail(row)
    Row(Modifier.fillMaxWidth().padding(horizontal = u.dp(60)), horizontalArrangement = Arrangement.spacedBy(u.dp(52))) {
        Column(Modifier.weight(1.1f), verticalArrangement = Arrangement.spacedBy(u.dp(8))) {
            Text(kind.uppercase(), style = EchoTextStyle.copy(color = Color.White.copy(alpha = 0.7f), fontSize = u.sp(12), letterSpacing = 0.2.em))
            Text(row.title, color = Color.White, fontSize = u.sp(28), lineHeight = u.sp(31), fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val pill = listOfNotNull(detail.takeIf { it.isNotBlank() }, row.totalPlayTimeMillis.takeIf { it > 0 }?.let(::playTimeLabel)).joinToString(" · ")
            Row(horizontalArrangement = Arrangement.spacedBy(u.dp(10)), verticalAlignment = Alignment.CenterVertically) {
                if (pill.isNotBlank()) {
                    Text(pill, color = Color.White, fontSize = u.sp(13), fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clip(RoundedCornerShape(u.dp(20))).background(Color.Black.copy(alpha = 0.6f))
                            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(u.dp(20))).padding(horizontal = u.dp(14), vertical = u.dp(6)))
                }
                row.lastOpenedAt?.let {
                    Text("Last opened ${relativeTime(System.currentTimeMillis(), it).lowercase()}", color = Color.White.copy(alpha = 0.75f), fontSize = u.sp(13), maxLines = 1)
                }
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(u.dp(12))) {
            (row.description ?: row.metadataLine)?.takeIf { it.isNotBlank() }?.let {
                Text(it, color = Color.White, fontSize = u.sp(14), lineHeight = u.sp(21), fontWeight = FontWeight.Medium, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(u.dp(10))) {
                PanelButton(GamepadAction.SELECT, primaryVerbFor(row) ?: "Open", u, onClick = onOpen)
                onOptions?.let { PanelButton(GamepadAction.OPEN_CONTEXT_MENU, "Options", u, onClick = it) }
            }
        }
    }
}

// the results on one shelf: the selected one out as a whole case, lifted and ringed; the rest as spines
// cut from their own covers, one size, fanning out smaller away from it.
// owner, 2026-10-05: moving along it must be smooth, so the shelf is laid out from one animated position: as it
// travels, the case narrows back into a spine while the next spine grows into the case, and the shelf slides to
// keep the position in the middle. A drag moves the position directly and settles on the nearest result
@Composable
private fun Shelf(state: SearchState, u: DesignUnits, onActivateAt: (Int) -> Unit, onFocusAt: (Int) -> Unit, modifier: Modifier) {
    val rows = state.rows
    val position = remember { Animatable(state.selectedIndex.toFloat()) }
    // a new search or filter lands at once; a step along the shelf glides
    LaunchedEffect(state.scrollToTopToken, state.kind, rows.size) { position.snapTo(state.selectedIndex.toFloat()) }
    LaunchedEffect(state.selectedIndex) { position.animateTo(state.selectedIndex.toFloat(), spring(dampingRatio = 0.9f, stiffness = 220f)) }
    val scope = rememberCoroutineScope()
    BoxWithConstraints(modifier) {
        val density = LocalDensity.current
        val ledge = u.dp(18)
        val caseH = with(density) { (maxHeight - ledge - u.dp(24)).coerceAtLeast(u.dp(60)).toPx() }
        val caseW = caseH * 0.72f
        val spineW = maxOf(caseH * 0.19f, with(density) { u.dp(30).toPx() })
        val gap = with(density) { u.dp(5).toPx() }
        val viewW = with(density) { maxWidth.toPx() }
        val shelfBottom = with(density) { (maxHeight - ledge).toPx() }
        val pos = position.value
        fun distance(i: Int) = kotlin.math.abs(i - pos)
        fun weight(i: Int) = (1f - distance(i)).coerceIn(0f, 1f)
        fun fan(i: Int) = (1f - (distance(i) - 1f).coerceAtLeast(0f) * 0.035f).coerceAtLeast(0.7f)
        fun width(i: Int) = lerp(spineW * fan(i), caseW, weight(i))
        fun height(i: Int) = lerp(caseH * 0.9f * fan(i), caseH, weight(i))
        if (rows.isNotEmpty()) {
            val base = pos.toInt().coerceIn(0, rows.lastIndex)
            val frac = (pos - base).coerceIn(0f, 1f)
            val step = if (base < rows.lastIndex) (width(base) + width(base + 1)) / 2f + gap else 0f
            val centres = HashMap<Int, Float>()
            centres[base] = viewW / 2f - frac * step
            var i = base
            while (i < rows.lastIndex && centres.getValue(i) - width(i) / 2f < viewW) { centres[i + 1] = centres.getValue(i) + (width(i) + width(i + 1)) / 2f + gap; i++ }
            i = base
            while (i > 0 && centres.getValue(i) + width(i) / 2f > 0f) { centres[i - 1] = centres.getValue(i) - (width(i) + width(i - 1)) / 2f - gap; i-- }
            Box(
                Modifier.fillMaxSize().pointerInput(rows.size) {
                    detectHorizontalDragGestures(
                        onDragEnd = { onFocusAt(position.value.roundToInt().coerceIn(0, rows.lastIndex)) },
                    ) { change, dx ->
                        change.consume()
                        scope.launch { position.snapTo((position.value - dx / (spineW + gap)).coerceIn(0f, rows.lastIndex.toFloat())) }
                    }
                },
            ) {
                centres.keys.sorted().forEach { index ->
                    val row = rows[index]
                    val w = width(index)
                    val h = height(index)
                    val x = centres.getValue(index) - w / 2f
                    key(row.id) {
                        ShelfItem(
                            row = row,
                            weight = weight(index),
                            alpha = (1f - distance(index) * 0.06f).coerceIn(0.55f, 1f),
                            u = u,
                            modifier = Modifier
                                .zIndex(weight(index))
                                .offset { IntOffset(x.roundToInt(), (shelfBottom - h).roundToInt()) }
                                .size(with(density) { w.toDp() }, with(density) { h.toDp() }),
                            onClick = { if (index == state.selectedIndex) onActivateAt(index) else onFocusAt(index) },
                        )
                    }
                }
            }
        }
        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(ledge)
                .background(Brush.verticalGradient(listOf(Color(0xFF34323B), Color(0xFF17161B))))
                .wallStripes(0.02f),
        )
    }
}

// one result, between a spine (weight 0) and the whole case (weight 1): the case lifts and rings as it comes out,
// and the spine fades under it. owner, 2026-10-05: the case is the App Drawer's (VhsCase), so the two screens match
@Composable
private fun ShelfItem(row: CrossbarItem, weight: Float, alpha: Float, u: DesignUnits, modifier: Modifier, onClick: () -> Unit) {
    val shape = caseShape(u)
    Box(
        modifier
            .graphicsLayer {
                translationY = -u.dp(12).toPx() * weight
                this.alpha = lerp(alpha, 1f, weight)
            }
            .clickable(onClick = onClick),
    ) {
        if (weight < 1f) Box(Modifier.fillMaxSize().graphicsLayer(alpha = 1f - weight)) { SpineFace(row, u) }
        if (weight > 0f) CaseFace(
            row, u,
            Modifier.fillMaxSize().graphicsLayer(alpha = weight)
                .shadow(u.dp(24) * weight, shape)
                .border(u.dp(3), Color.White.copy(alpha = weight), shape),
        )
    }
}

// the whole case: its art keeping its own shape on the ribbed plastic, or for an app its colour, rings and icon
@Composable
private fun CaseFace(row: CrossbarItem, u: DesignUnits, modifier: Modifier) {
    val art = coverArt(row)
    val icon = rememberAppIcon(row.packageName?.takeIf { row.isInstalledApp && art == null })
    val tint = icon?.color ?: SearchTint
    VhsCase(spineLabel(row), tint, u, modifier) {
        if (art != null) VhsCoverArt(rememberArtworkModel(art), u)
        else VhsAppFace(row.title, icon, tint, u) {
            Icon(kindGlyph(row), null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(u.dp(56)))
        }
    }
}

// a spine's face: a strip of its cover, darkened, with a small label, its title running up it and a barcode
@Composable
private fun SpineFace(row: CrossbarItem, u: DesignUnits) {
    Box(Modifier.fillMaxSize().shadow(u.dp(8), RoundedCornerShape(u.dp(3))).clip(RoundedCornerShape(u.dp(3)))) {
        SpineCover(row, u)
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.38f)))
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(0f to Color.Black.copy(alpha = 0.55f), 0.22f to Color.Transparent, 0.48f to Color.White.copy(alpha = 0.1f), 0.7f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.6f))))
        Column(Modifier.fillMaxSize().padding(vertical = u.dp(10)), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(u.dp(10))) {
            Text(spineLabel(row), color = Color(0xFF151515), fontSize = u.sp(8), fontWeight = FontWeight.ExtraBold, letterSpacing = 0.1.em, maxLines = 1,
                modifier = Modifier.clip(RoundedCornerShape(u.dp(2))).background(Color(0xFFE8E2D3)).padding(horizontal = u.dp(4), vertical = u.dp(2)))
            Box(Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
                Text(row.title, color = Color.White.copy(alpha = 0.88f), fontSize = u.sp(13), fontWeight = FontWeight.Bold, letterSpacing = 0.05.em,
                    maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis, modifier = Modifier.sideways())
            }
            Row(horizontalArrangement = Arrangement.spacedBy(u.dp(2))) { repeat(6) { Box(Modifier.size(1.dp, u.dp(6)).background(Color.White.copy(alpha = 0.7f))) } }
        }
    }
}

// a result's cover art: an app's when the app is in the game library, a game's from its icon slot (its cover)
private fun coverArt(row: CrossbarItem): String? = when {
    row.isInstalledApp -> row.coverUri
    row.gameId != null -> com.echo.core.domain.model.coverArtOf(row.iconUri, row.shelfCoverArt)
    else -> row.shelfCoverArt
}

// a spine is a strip cut from the cover, or for an app its colour with ECHO's echo rings and its icon
@Composable
private fun SpineCover(row: CrossbarItem, u: DesignUnits) {
    val art = coverArt(row)
    val icon = rememberAppIcon(row.packageName?.takeIf { row.isInstalledApp && art == null })
    when {
        art != null -> AsyncImage(rememberArtworkModel(art), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        else -> Box(Modifier.fillMaxSize().background(icon?.color ?: SearchTint).coverRings(), contentAlignment = Alignment.Center) {
            when {
                icon != null -> androidx.compose.foundation.Image(icon.bitmap, null, Modifier.size(u.dp(26)))
                else -> Icon(kindGlyph(row), null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(u.dp(22)))
            }
        }
    }
}

// the small label at the top of a spine: the console, or the kind of result
private fun spineLabel(row: CrossbarItem): String =
    (row.platformId?.takeIf { row.gameId != null && it.isNotBlank() } ?: kindAndDetail(row).first).uppercase().take(6)

@Composable
private fun EmptyNotice(row: CrossbarItem, u: DesignUnits) {
    Column(verticalArrangement = Arrangement.spacedBy(u.dp(8))) {
        Headline(row.title, u.sp(30), 2)
        row.subtitle?.let { Meta(it, u.sp(15), 2) }
    }
}

private fun kindAndDetail(row: CrossbarItem): Pair<String, String> {
    val parts = row.subtitle.orEmpty().split("  ·  ", limit = 2)
    return parts[0] to parts.getOrElse(1) { "" }
}

private fun kindGlyph(row: CrossbarItem): ImageVector = when (row.type) {
    CrossbarItemType.VIDEO_FILE -> Icons.Outlined.Movie
    CrossbarItemType.PHOTO_FILE -> Icons.Outlined.Image
    CrossbarItemType.LIBRARY_BOOK -> Icons.AutoMirrored.Outlined.MenuBook
    CrossbarItemType.MUSIC_TRACK -> Icons.Outlined.MusicNote
    CrossbarItemType.SEARCH -> Icons.Outlined.Language
    else -> if (row.isInstalledApp) Icons.Outlined.Apps else Icons.Outlined.Games
}

private val SearchTint = Color(0xFF2B3654)
