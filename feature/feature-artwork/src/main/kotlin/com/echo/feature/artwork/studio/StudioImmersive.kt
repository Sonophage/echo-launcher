package com.echo.feature.artwork.studio

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.outlined.Folder
import coil3.compose.AsyncImage
import com.echo.core.domain.model.GamepadAction
import com.echo.core.ui.components.ControllerPrompt
import com.echo.core.ui.theme.EchoTextStyle
import com.echo.core.ui.components.HintAction
import com.echo.core.ui.components.ControllerPromptItem
import com.echo.core.ui.components.EchoHintBar
import com.echo.core.ui.components.HintBarHeight
import com.echo.core.ui.components.StatusStripHeight
import com.echo.core.ui.design.panelDesignUnits
import androidx.compose.ui.platform.LocalDensity
import com.echo.core.ui.components.MenuRow
import com.echo.core.ui.components.MenuState
import com.echo.core.ui.components.MenuSelect
import com.echo.core.ui.components.EchoCheckMark
import com.echo.core.ui.components.EchoContextMenuOverlay
import com.echo.core.ui.components.chose
import com.echo.core.ui.design.MenuScrim
import com.echo.feature.artwork.store.ArtworkKind

private val STUDIO_RESULT_GAP = 8.dp

fun slotNotOfferedReason(source: StudioSource, tab: StudioTab): String =
    "${source.label} has no ${tab.label} artwork. Choose a file from this device, or change provider."

@Composable
internal fun resultsColumnWidth(): Dp =
    (LocalConfiguration.current.screenWidthDp * 0.60f).dp.coerceIn(246.dp, 560.dp)

@Composable
internal fun StudioSlotPill(
    label: String,
    selected: Boolean,
    filled: Boolean,
    offered: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(24.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    selected -> Color.White.copy(alpha = 0.92f)
                    else     -> Color.White.copy(alpha = 0.10f)
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
    ) {
        Box(
            Modifier
                .size(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(
                    when {
                        !offered -> Color(0xFFE0A030).copy(alpha = 0.7f)
                        filled && selected -> accent
                        filled   -> Color(0xFF66BB6A)
                        selected -> Color.Black.copy(alpha = 0.25f)
                        else     -> Color.White.copy(alpha = 0.22f)
                    }
                ),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            color = when {
                selected -> Color(0xFF0B0D12)
                !offered -> Color.White.copy(alpha = 0.40f)
                else     -> Color.White.copy(alpha = 0.72f)
            },
            fontSize = 9.5.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
        )
    }
}

@Composable
internal fun StudioMatchLine(
    state: ArtworkStudioUiState,
    actions: ArtworkStudioActions,
    accent: Color,
) {
    val provider = state.matchProvider ?: return
    Spacer(Modifier.height(10.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        val matched = state.matchTitle
        if (!state.matchResolving && matched != null) {
            EchoCheckMark(Color(0xFF66BB6A), size = 11.dp)
        } else {
            Text(
                if (state.matchResolving) "◌" else "!",
                color = if (state.matchResolving) Color.White.copy(alpha = 0.4f) else Color(0xFFE0A030),
                fontSize = 10.sp, fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.width(6.dp))
        Text(
            when {
                matched != null && !state.matchResolving -> "Matched as $matched"
                state.matchResolving -> "Matching on ${provider.label}…"
                state.matchFailed    -> "${provider.label} didn't answer"
                else                 -> "No ${provider.label} match"
            },
            color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (state.canChangeMatch) {
            Spacer(Modifier.width(8.dp))
            Text(
                "CHANGE MATCH",
                color = Color.White.copy(alpha = 0.8f), fontSize = 9.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color.White.copy(alpha = 0.10f))
                    .clickable { actions.onChangeMatchPressed() }
                    .padding(horizontal = 6.dp, vertical = 3.dp),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun StudioResultsColumn(
    state: ArtworkStudioUiState,
    actions: ArtworkStudioActions,
    accent: Color,
    markColor: Color,
    modifier: Modifier = Modifier,
) {
    val tab = STUDIO_TABS[state.tabIndex]
    BoxWithConstraints(modifier) {
        val slotWidth = maxWidth
        val slotHeight = maxHeight
        LaunchedEffect(slotWidth, slotHeight, state.tabIndex) {
            actions.onGridMeasured(slotWidth.value, slotHeight.value)
        }
        val columns = state.gridColumns
        val rows = state.gridRows
        val tileWidth = (slotWidth - STUDIO_RESULT_GAP * (columns - 1)) / columns
        val tileHeight = maxOf(
            0.dp,
            minOf(
                tileWidth / tab.tileClass.aspect.toFloat(),
                (slotHeight - STUDIO_RESULT_GAP * (rows - 1)) / rows,
            ),
        )

        when {
            state.resultsLoading -> LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(STUDIO_RESULT_GAP),
                verticalArrangement = Arrangement.spacedBy(STUDIO_RESULT_GAP),
                userScrollEnabled = false,
            ) {
                items(state.skeletonCount) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(tileHeight)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.06f)),
                    )
                }
            }

            !state.sourceServesTab || state.source == StudioSource.LOCAL -> Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .clickable(onClick = actions::requestLocalPick),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Choose a file from this device",
                    color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 10.dp),
                )
            }

            state.results.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    when {
                        state.source in state.unavailableSources ->
                            "${state.source.label} needs an account or key. Settings ▸ Accounts ▸ Artwork."
                        state.source != StudioSource.SCREENSCRAPER -> "No results"
                        state.matchResolving -> "Looking for this game on ScreenScraper…"
                        state.matchFailed    -> "ScreenScraper didn't answer. Use Change Match to search again."
                        state.match == null  -> "No ScreenScraper match. Use Change Match to pick one."
                        else                 -> "ScreenScraper has no ${tab.label} for this game"
                    },
                    color = Color.White.copy(alpha = 0.45f), fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }

            else -> {
                var touchPreviewIndex by remember(state.results) { mutableIntStateOf(-1) }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(STUDIO_RESULT_GAP),
                    verticalArrangement = Arrangement.spacedBy(STUDIO_RESULT_GAP),
                    userScrollEnabled = false,
                ) {
                    itemsIndexed(state.results) { index, art ->
                        val focused = state.gridIndex == index
                        val previewing = art.isVideo && (focused || touchPreviewIndex == index)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(tileHeight)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF10101A))
                                .border(
                                    if (focused) 2.dp else 1.dp,
                                    if (focused) accent else Color.White.copy(alpha = 0.1f),
                                    RoundedCornerShape(8.dp),
                                )
                                .combinedClickable(
                                    onClick = { actions.pickAt(index) },
                                    onLongClick = {
                                        if (art.isVideo) touchPreviewIndex =
                                            if (touchPreviewIndex == index) -1 else index
                                    },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            when {
                                previewing -> StudioVideoTilePreview(url = art.url, modifier = Modifier.fillMaxSize())
                                art.isVideo -> Text(
                                    "▶ VIDEO",
                                    color = Color.White.copy(alpha = 0.75f), fontSize = 11.sp,
                                )
                                tab.kind == ArtworkKind.MANUAL -> Text(
                                    "PDF",
                                    color = Color.White.copy(alpha = 0.75f),
                                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                )
                                else -> AsyncImage(
                                    model = art.thumb ?: art.url,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }

                            StudioTileBadge(
                                mark = state.tileMarkOf(art),
                                accent = accent,
                                markColor = markColor,
                                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

// the kit's look (owner, 2026-10-04): design units, the eyebrow over a light title, and the kit's
// footer with A as the centre orb, Close on B and Change Match on X
@Composable
internal fun StudioProviderPicker(
    cards: List<StudioProviderCard>,
    focusedIndex: Int,
    gameTitle: String?,
    matchLabel: String?,
    accent: Color,
    background: Color,
    onPick: (Int) -> Unit,
    onChangeMatch: () -> Unit,
    onClose: () -> Unit,
) {
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(background)
            .background(Brush.verticalGradient(0f to MenuScrim, 1f to MenuScrim.copy(alpha = 0.97f)))
            .clickable(enabled = false) {},
    ) {
        val u = panelDesignUnits(maxWidth.value, maxHeight.value, LocalDensity.current)
        Column(
            Modifier
                .fillMaxSize()
                .padding(start = u.dp(48), end = u.dp(48), top = StatusStripHeight + u.dp(8), bottom = HintBarHeight + u.dp(12)),
        ) {
            Text(
                listOf(gameTitle ?: "Artwork Studio", matchLabel?.let { "Matched as $it" } ?: "No match yet")
                    .joinToString("  ·  ").uppercase(),
                style = u.eyebrow(),
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(u.dp(6)))
            Text(
                "Where should the artwork come from?",
                color = Color.White, fontSize = u.sp(30), fontWeight = FontWeight.Light,
                style = EchoTextStyle,
            )
            Spacer(Modifier.height(u.dp(4)))
            Text(
                "One provider for the whole pass. Change it any time from Options.",
                color = Color.White.copy(alpha = 0.6f), fontSize = u.sp(14),
                style = EchoTextStyle,
            )
            Spacer(Modifier.height(u.dp(20)))

            Row(
                Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(u.dp(16)),
            ) {
                cards.forEachIndexed { index, card ->
                    StudioProviderCardView(
                        card = card,
                        focused = index == focusedIndex,
                        accent = accent,
                        onClick = { onPick(index) },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            }
        }
        EchoHintBar(
            items = listOf(
                ControllerPromptItem(GamepadAction.BACK, "Close"),
                ControllerPromptItem(GamepadAction.CHANGE_SORT, "Change Match"),
            ),
            primary = HintAction(GamepadAction.SELECT, "Use this provider"),
            onAction = { action ->
                when (action) {
                    GamepadAction.SELECT -> onPick(focusedIndex)
                    GamepadAction.CHANGE_SORT -> onChangeMatch()
                    GamepadAction.BACK -> onClose()
                    else -> Unit
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun StudioProviderCardView(
    card: StudioProviderCard,
    focused: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dim = if (focused) 1f else 0.38f
    com.echo.core.ui.components.EchoArtCard(
        title = card.source.label,
        artUri = card.sampleUri,
        focused = focused,
        accent = accent,
        titleAlpha = if (card.pickable) dim else dim * 0.6f,
        onClick = onClick,
        modifier = modifier,
        leading = { d -> SourceMark(card.source, 24.dp, d) },
        emptyArt = { d -> SourceMark(card.source, 72.dp, d * 0.5f) },
    ) { d ->
        Text(
            card.scansFor,
            color = (if (card.servesAll) com.echo.core.ui.components.EchoArtCardComplete
                     else com.echo.core.ui.components.EchoArtCardPartial).copy(alpha = d),
            fontSize = 9.5.sp, lineHeight = 12.sp,
            maxLines = 3, overflow = TextOverflow.Ellipsis,
        )
        card.quota?.let {
            Text(
                "$it requests today",
                color = Color.White.copy(alpha = 0.45f * d), fontSize = 9.sp, maxLines = 1,
            )
        }
        card.reason?.let {
            Text(
                it,
                color = com.echo.core.ui.components.EchoArtCardPartial.copy(alpha = d),
                fontSize = 9.sp, lineHeight = 11.sp,
                maxLines = 3, overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun StudioReviewPanel(
    summary: StudioReviewSummary,
    beforeOf: (ArtworkKind) -> String?,
    afterOf: (ArtworkKind) -> String?,
    gameTitle: String?,
    accent: Color,
    background: Color,
    showTouchControls: Boolean,
    onApply: () -> Unit,
    onBack: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(background)
            .background(Brush.verticalGradient(0f to MenuScrim, 1f to MenuScrim.copy(alpha = 0.97f)))
            .clickable(enabled = false) {},
    ) {
        Column(Modifier.fillMaxSize().padding(start = 26.dp, end = 26.dp, top = 16.dp)) {
            Text(
                gameTitle ?: "Artwork Studio",
                color = Color.White.copy(alpha = 0.55f), fontSize = 11.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "Before and after",
                color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(12.dp))

            val changed = summary.entries.filter {
                it.status == StudioReviewStatus.NEW || it.status == StudioReviewStatus.REMOVED
            }
            Row(
                Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StudioBeforeAfterColumn(
                    heading = "Now",
                    entries = changed,
                    uriOf = beforeOf,
                    dim = true,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
                StudioBeforeAfterColumn(
                    heading = "After applying",
                    entries = changed,
                    uriOf = afterOf,
                    dim = false,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }

            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    summary.line,
                    color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.weight(1f))
                if (!showTouchControls) {
                    ControllerPrompt(
                        action = GamepadAction.SELECT,
                        label = "",
                        glyphSize = 13.dp,
                        labelColor = Color.White.copy(alpha = 0.5f),
                    )
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    "Apply and Close",
                    color = if (summary.removed > 0) Color(0xFFE57373) else Color(0xFF45C46A),
                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .clickable(enabled = summary.hasChanges, onClick = onApply)
                        .padding(horizontal = 16.dp, vertical = 7.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "Back",
                    color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.07f))
                        .clickable(onClick = onBack)
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                )
            }
            Spacer(Modifier.height(14.dp))
        }
    }
}

@Composable
private fun StudioBeforeAfterColumn(
    heading: String,
    entries: List<StudioReviewEntry>,
    uriOf: (ArtworkKind) -> String?,
    dim: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
            .padding(14.dp),
    ) {
        Text(
            heading,
            color = if (dim) Color.White.copy(alpha = 0.5f) else Color.White,
            fontSize = 12.sp, fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        if (entries.isEmpty()) {
            Text(
                "Nothing changes",
                color = Color.White.copy(alpha = 0.35f), fontSize = 10.sp,
            )
            return@Column
        }
        Row(
            Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            entries.take(4).forEach { entry ->
                Column(Modifier.weight(1f)) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        val uri = uriOf(entry.kind)
                        when {
                            uri != null -> AsyncImage(
                                model = uri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                alpha = if (dim) 0.45f else 1f,
                                modifier = Modifier.fillMaxSize(),
                            )
                            else -> Text(
                                if (dim) "empty" else "cleared",
                                color = Color.White.copy(alpha = 0.3f), fontSize = 9.sp,
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        entry.label,
                        color = Color.White.copy(alpha = if (dim) 0.4f else 0.8f),
                        fontSize = 8.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        entry.status.label,
                        color = reviewStatusColor(entry.status).copy(alpha = if (dim) 0.5f else 1f),
                        fontSize = 8.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
                    )
                }
            }
        }
    }
}

private fun reviewStatusColor(status: StudioReviewStatus): Color = when (status) {
    StudioReviewStatus.NEW     -> Color(0xFF45C46A)
    StudioReviewStatus.KEPT    -> Color(0xFF7FA8D8)
    StudioReviewStatus.REMOVED -> Color(0xFFE57373)
    StudioReviewStatus.EMPTY   -> Color(0xFF6B7280)
}

/**
 * The studio's menus, activated the way every other menu in the app is: through
 * MenuState.chose(), which resolves the index against the DRAWN rows. Indexing the
 * source list directly is what made "View File Information" run Clear Artwork.
 *
 * selectedIndex is still an index into the drawn rows, so the source lists also keep
 * destructive rows last -- StudioMenuOrderTest pins that.
 */
@Composable
internal fun <T : Any> StudioMenu(
    title: String,
    rows: List<MenuRow<T>>,
    selectedIndex: Int,
    onActivate: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = MenuState(title = title, rows = rows, selectedIndex = selectedIndex)
    EchoContextMenuOverlay(
        state = state,
        onRowActivated = { index ->
            (state.chose(index) as? MenuSelect.Run)?.let { onActivate(it.action) }
        },
        onDismiss = onDismiss,
    )
}

// owner, 2026-10-07: each provider is shown by a monogram drawn in ECHO's style, not the site's own logo
internal fun sourceMonogram(source: StudioSource): String? = when (source) {
    StudioSource.SCREENSCRAPER -> "SS"
    StudioSource.STEAMGRIDDB -> "SGDB"
    StudioSource.IGDB -> "IGDB"
    StudioSource.LOCAL -> null
}

private fun sourceTint(source: StudioSource): Color = when (source) {
    StudioSource.SCREENSCRAPER -> Color(0xFF2F6FB3)
    StudioSource.STEAMGRIDDB -> Color(0xFF3A4A8C)
    StudioSource.IGDB -> Color(0xFF7B4FD6)
    StudioSource.LOCAL -> Color(0xFF4A5568)
}

// a rounded tile in the provider's tint with its monogram, or a folder for Local File
@Composable
private fun SourceMark(source: StudioSource, size: Dp, dim: Float) {
    val shape = RoundedCornerShape(size * 0.24f)
    Box(
        Modifier.size(size).clip(shape)
            .background(sourceTint(source).copy(alpha = 0.85f * dim))
            .border(1.dp, Color.White.copy(alpha = 0.25f * dim), shape),
        contentAlignment = Alignment.Center,
    ) {
        val mark = sourceMonogram(source)
        if (mark != null) {
            Text(
                mark, color = Color.White.copy(alpha = dim), fontWeight = FontWeight.ExtraBold, maxLines = 1, softWrap = false,
                fontSize = (size.value * if (mark.length > 2) 0.26f else 0.4f).sp, letterSpacing = 0.02.em,
            )
        } else {
            androidx.compose.material3.Icon(
                androidx.compose.material.icons.Icons.Outlined.Folder, null,
                tint = Color.White.copy(alpha = dim), modifier = Modifier.size(size * 0.55f),
            )
        }
    }
}
