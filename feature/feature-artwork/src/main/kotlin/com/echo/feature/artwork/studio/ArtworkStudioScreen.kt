package com.echo.feature.artwork.studio

import com.echo.core.ui.components.EchoTrio
import com.echo.core.ui.theme.EchoTextStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.itemsIndexed as lazyItemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.echo.core.common.format.formatByteSize
import com.echo.core.common.logging.LogRedaction
import com.echo.core.domain.model.GamepadAction
import com.echo.core.ui.components.ControllerPrompt
import com.echo.core.ui.components.EchoHintBar
import com.echo.core.ui.components.ControllerPromptItem
import com.echo.core.ui.theme.LocalEchoColors
import com.echo.core.ui.theme.menuCursorEdge
import com.echo.feature.artwork.store.ArtworkKind

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun ArtworkStudioScreen(
    gameId: Long,
    onClose: () -> Unit,
    pendingGamepadAction: GamepadAction? = null,
    onGamepadActionConsumed: () -> Unit = {},

    showTouchControls: Boolean = true,
    onTouchInput: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ArtworkStudioViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(gameId) { viewModel.load(gameId) }
    LaunchedEffect(state.closed) {
        if (state.closed) {
            onClose()
            viewModel.consumeClosed()
        }
    }
    LaunchedEffect(pendingGamepadAction) {
        if (pendingGamepadAction != null) {
            viewModel.handleGamepadAction(pendingGamepadAction)
            onGamepadActionConsumed()
        }
    }

    val localPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.applyLocal(uri)
    }
    LaunchedEffect(state.localPickKind) {
        val kind = state.localPickKind ?: return@LaunchedEffect
        val mimes = when (kind) {
            com.echo.feature.artwork.store.ArtworkKind.MANUAL -> arrayOf("application/pdf")
            com.echo.feature.artwork.store.ArtworkKind.VIDEO,
            com.echo.feature.artwork.store.ArtworkKind.ICON1  -> arrayOf("video/mp4", "video/webm", "video/*")
            else -> arrayOf("image/png", "image/jpeg", "image/webp")
        }
        localPicker.launch(mimes)
        viewModel.consumeLocalPick()
    }

    ArtworkStudioContent(
        state = state,
        actions = viewModel,
        showTouchControls = showTouchControls,
        onTouchInput = onTouchInput,
        modifier = modifier,
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
internal fun ArtworkStudioContent(
    state: ArtworkStudioUiState,
    actions: ArtworkStudioActions,
    showTouchControls: Boolean,
    onTouchInput: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val echoColors = LocalEchoColors.current
    val accent = menuCursorEdge()

    Box(
        modifier = modifier
            .fillMaxSize()

            .pointerInput(Unit) { awaitEachGesture { awaitFirstDown(requireUnconsumed = false); onTouchInput() } }
            .background(
                Brush.verticalGradient(
                    0f to echoColors.backgroundTop.copy(alpha = 0.97f),
                    1f to echoColors.backgroundBottom,
                )
            ),
    ) {
        val focusedArt = state.results.getOrNull(state.gridIndex)
        val backdrop = focusedArt?.takeIf { !it.isVideo }?.let { it.thumb ?: it.url } ?: state.currentUri
        if (backdrop != null) {
            androidx.compose.runtime.key(state.previewVersion, backdrop) {
                AsyncImage(
                    model = backdrop,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0f to com.echo.core.ui.design.MenuScrim,
                        0.45f to com.echo.core.ui.design.MenuScrim.copy(alpha = 0.88f),
                        0.72f to com.echo.core.ui.design.MenuScrim.copy(alpha = 0.55f),
                        1f to com.echo.core.ui.design.MenuScrim.copy(alpha = 0.20f),
                    )
                ),
        )

        Column(
            Modifier
                .fillMaxSize()
                .padding(start = 26.dp, end = 26.dp, top = 14.dp),
        ) {
            val tab = STUDIO_TABS[state.tabIndex]

            Row(Modifier.weight(1f)) {
                Column(Modifier.weight(1f).fillMaxHeight().padding(end = 18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        state.game?.platformId?.takeIf { it.isNotBlank() }?.let {
                            Text(
                                it.uppercase(),
                                color = Color.White.copy(alpha = 0.55f), fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold, maxLines = 1,
                            )
                            Text(
                                "  ·  ",
                                color = Color.White.copy(alpha = 0.3f), fontSize = 10.sp,
                            )
                        }
                        Text(
                            "${state.filledKinds} of ${STUDIO_TABS.size} filled",
                            color = Color.White.copy(alpha = 0.55f), fontSize = 10.sp, maxLines = 1,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        state.game?.displayTitle ?: "Artwork Studio",
                        color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold,
                        lineHeight = 34.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        listOfNotNull(tab.label, focusedArt?.label).joinToString("  ·  "),
                        color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp,
                        fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        tab.contract,
                        color = Color.White.copy(alpha = 0.45f), fontSize = 10.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        when {
                            !state.sourceServesTab ->
                                slotNotOfferedReason(state.source, tab)
                            focusedArt != null ->
                                "Previewing result ${state.rangeStart + state.gridIndex} from ${state.source.label}"
                            else -> "Browsing ${state.source.label}"
                        },
                        color = if (state.sourceServesTab) accent else Color(0xFFE0A030),
                        fontSize = 11.sp, lineHeight = 14.sp,
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )

                    StudioMatchLine(state = state, actions = actions, accent = accent)

                    Spacer(Modifier.weight(1f))

                    val pending = state.reviewSummary
                    if (pending.hasChanges) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(bottom = 8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.10f))
                                .clickable(onClick = actions::applyChanges)
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                        ) {
                            if (!showTouchControls) {
                                ControllerPrompt(
                                    action = GamepadAction.OPEN_ISLAND,
                                    label = "",
                                    glyphSize = 12.dp,
                                    labelColor = Color.White.copy(alpha = 0.5f),
                                )
                                Spacer(Modifier.width(6.dp))
                            }
                            Text(
                                "Apply Changes",
                                color = Color(0xFF45C46A), fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold, maxLines = 1,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                pending.line,
                                color = Color.White.copy(alpha = 0.55f), fontSize = 10.sp,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    state.message?.let {
                        Text(
                            it, color = accent, fontSize = 11.sp, maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clickable(onClick = actions::dismissMessage),
                        )
                    }
                }

                Column(Modifier.width(resultsColumnWidth()).fillMaxHeight()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().height(20.dp),
                    ) {
                        Text(
                            state.source.label.uppercase(),
                            color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            if (state.totalResults > 0)
                                "${state.rangeStart + state.gridIndex} / ${state.totalResults}"
                            else "—",
                            color = Color.White.copy(alpha = 0.55f), fontSize = 10.sp, maxLines = 1,
                        )
                    }
                    Spacer(Modifier.height(6.dp))

                    StudioResultsColumn(
                        state = state,
                        actions = actions,
                        accent = accent,
                        markColor = echoColors.backgroundBottom,
                        modifier = Modifier.fillMaxWidth().weight(1f),
                    )

                    Spacer(Modifier.height(6.dp))
                    StudioPageLine(
                        rangeStart = state.rangeStart,
                        rangeEnd = state.rangeEnd,
                        totalResults = state.totalResults,
                        picks = state.queueSummary,
                        page = state.page,
                        pageCount = state.pageCount,
                        hasPreviousPage = state.hasPreviousPage,
                        hasNextPage = state.hasNextPage,
                        showTouchControls = showTouchControls,
                        onPreviousPage = actions::previousPage,
                        onNextPage = actions::nextPage,
                        onApply = actions::applyChanges,
                        onRetryFailed = actions::retryFailed,
                        onRemoveFailed = actions::removeFailed,
                    )
                }
                }

            Spacer(Modifier.height(8.dp))

            val visible = state.visibleSlots
            val slotListState = rememberLazyListState()
            LaunchedEffect(state.tabIndex) {
                val at = visible.indexOfFirst { it.kind == STUDIO_TABS[state.tabIndex].kind }
                if (at >= 0) slotListState.animateScrollToItem(at)
            }
            LazyRow(
                state = slotListState,
                modifier = Modifier.fillMaxWidth().height(30.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                lazyItemsIndexed(visible) { _, slot ->
                    val index = STUDIO_TABS.indexOfFirst { it.kind == slot.kind }
                    StudioSlotPill(
                        label = slot.label,
                        selected = state.tabIndex == index,
                        filled = slot.kind in state.filledSlots,
                        offered = servesKind(state.source, slot.kind),
                        accent = accent,
                        onClick = { actions.selectTab(index) },
                    )
                }
            }

            EchoHintBar(
                items = buildList {
                    add(
                        ControllerPromptItem(
                            GamepadAction.SELECT,
                            when {
                                !state.sourceServesTab || state.source == StudioSource.LOCAL -> "Pick File"
                                state.selectsMultiple -> "Check"
                                else -> "Use This"
                            },
                        )
                    )
                    add(ControllerPromptItem(GamepadAction.BACK, "Close"))
                    if (visible.size > 1) add(ControllerPromptItem(GamepadAction.NEXT_CATEGORY, "Slot"))
                    if (state.pageCount > 1) add(ControllerPromptItem(GamepadAction.NEXT_PAGE, "Page"))
                    add(ControllerPromptItem(GamepadAction.CHANGE_SORT, "Search"))
                    add(ControllerPromptItem(GamepadAction.OPEN_CONTEXT_MENU, "Options"))
                },
                onAction = actions::handleGamepadAction,
            )
        }

        if (state.providerPickerOpen) {
            StudioProviderPicker(
                cards = providerCards(
                    unavailable = state.unavailableSources,
                    requestsToday = state.requestsToday,
                    dailyCap = state.dailyRequestCap,
                    sampleFor = state::sampleFor,
                ),
                focusedIndex = state.sourceIndex,
                gameTitle = state.game?.displayTitle,
                matchLabel = state.matchTitle,
                accent = accent,
                background = echoColors.backgroundBottom,
                onPick = actions::selectSource,
                onChangeMatch = actions::onChangeMatchPressed,
                onClose = { actions.handleGamepadAction(GamepadAction.BACK) },
            )
        }

        if (state.reviewOpen) {
            StudioReviewPanel(
                summary = state.reviewSummary,
                beforeOf = state::storedUriOf,
                afterOf = state::pendingUriOf,
                gameTitle = state.game?.displayTitle,
                accent = accent,
                background = echoColors.backgroundBottom,
                showTouchControls = showTouchControls,
                onApply = actions::applyReviewed,
                onBack = actions::closeReview,
            )
        }

        state.candidate?.let { art ->
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.93f))
                    .clickable(onClick = actions::dismissCandidate),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (state.manualDownloading) {
                        EchoTrio(color = accent)
                        Spacer(Modifier.height(10.dp))
                        Text("Downloading manual…", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    } else if (state.candidateManualPath != null) {
                        StudioPdfPage(
                            path = state.candidateManualPath,
                            page = state.manualPage,
                            onPageCount = actions::onManualPageCount,
                            modifier = Modifier.fillMaxWidth(0.62f).fillMaxHeight(0.68f),
                        )
                        Spacer(Modifier.height(8.dp))
                        StudioManualPager(
                            page = state.manualPage,
                            pageCount = state.manualPageCount,
                            showTouchControls = showTouchControls,
                            onPreviousPage = actions::manualPreviousPage,
                            onNextPage = actions::manualNextPage,
                        )
                    } else if (art.isVideo) {
                        Text("Video snap from ${art.provider}", color = Color.White, fontSize = 14.sp)
                    } else {
                        AsyncImage(
                            model = art.url,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxWidth(0.72f).fillMaxHeight(0.72f),
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        listOfNotNull(art.provider, art.label).joinToString("  ·  "),
                        color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(
                            if (state.applying) "Applying…" else "Ⓐ  APPLY",
                            color = Color(0xFF45C46A), fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .clickable(enabled = !state.applying, onClick = actions::applyCandidate)
                                .padding(horizontal = 18.dp, vertical = 9.dp),
                        )
                        Text(
                            "Ⓑ  CANCEL",
                            color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .clickable(onClick = actions::dismissCandidate)
                                .padding(horizontal = 18.dp, vertical = 9.dp),
                        )
                    }
                }
            }
        }

        if (state.changeMatchOpen) {
            val matchFocus = remember { FocusRequester() }
            val keyboard = LocalSoftwareKeyboardController.current
            val focusManager = LocalFocusManager.current
            val editing by rememberUpdatedState(state.changeMatchEditing)
            LaunchedEffect(state.changeMatchEditing) {
                if (state.changeMatchEditing) {
                    withFrameNanos { }
                    runCatching { matchFocus.requestFocus() }
                    withFrameNanos { }
                    keyboard?.show()
                } else {
                    keyboard?.hide()
                    focusManager.clearFocus()
                }
            }

            val imeVisible = WindowInsets.isImeVisible
            var imeWasShown by remember { mutableStateOf(false) }
            LaunchedEffect(imeVisible) {
                if (imeVisible) {
                    imeWasShown = true
                } else if (imeWasShown && editing) {
                    imeWasShown = false
                    actions.stopChangeMatchEdit()
                }
            }
            val resultsState = rememberLazyListState()
            LaunchedEffect(state.changeMatchIndex, state.changeMatchResults) {
                if (state.changeMatchIndex >= 0) {
                    runCatching { resultsState.animateScrollToItem(state.changeMatchIndex) }
                }
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0xC0000000))
                    .clickable(onClick = actions::cancelChangeMatch),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    Modifier

                        .padding(vertical = 16.dp)
                        .width(520.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(echoColors.backgroundBottom)
                        .border(1.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .clickable(enabled = false) {}
                        .padding(18.dp),
                ) {
                    Text(
                        "Change match on ${state.matchProvider?.label.orEmpty()}",
                        color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Tells the provider which game this is. Your artwork and metadata are left alone.",
                        color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp,
                    )
                    Spacer(Modifier.height(12.dp))
                    BasicTextField(
                        value = state.changeMatchDraft,
                        readOnly = !state.changeMatchEditing,
                        onValueChange = actions::onChangeMatchDraftChanged,
                        singleLine = true,
                        textStyle = EchoTextStyle.copy(color = Color.White, fontSize = 15.sp),
                        cursorBrush = SolidColor(accent),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = { actions.submitChangeMatch() },
                            onDone = { actions.submitChangeMatch() },
                        ),
                        decorationBox = { inner ->
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (state.changeMatchIndex < 0) accent.copy(alpha = 0.22f)
                                        else Color.White.copy(alpha = 0.08f)
                                    )
                                    .then(
                                        if (state.changeMatchIndex < 0) Modifier.border(1.dp, accent, RoundedCornerShape(8.dp))
                                        else Modifier
                                    )
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                            ) {
                                if (state.changeMatchDraft.isEmpty()) Text(
                                    state.game?.displayTitle ?: "Game title",
                                    color = Color.White.copy(alpha = 0.35f), fontSize = 15.sp,
                                )
                                inner()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(matchFocus)

                            .onFocusChanged { if (it.isFocused && !editing) actions.startChangeMatchEdit() },
                    )
                    Spacer(Modifier.height(12.dp))

                    if (state.changeMatchAcrossPlatforms && !state.changeMatchLoading) {
                        Text(
                            "Includes other platforms. Check the platform before you pick.",
                            color = Color(0xFFE0A030), fontSize = 11.sp,
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    when {
                        state.changeMatchLoading -> Text(
                            if (state.changeMatchSearchingEveryPlatform)
                                "Searching every platform. ScreenScraper can take about 10 seconds…"
                            else "Searching…",
                            color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp,
                        )
                        state.changeMatchError != null -> Text(
                            state.changeMatchError.orEmpty(),
                            color = Color(0xFFE0A030), fontSize = 12.sp,
                        )
                        state.changeMatchResults.isEmpty() -> Text(
                            "No games found. Try a shorter title, or the title without its edition.",
                            color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp,
                        )

                        else -> LazyColumn(
                            Modifier.fillMaxWidth().weight(1f, fill = false).heightIn(max = 260.dp),
                            state = resultsState,
                        ) {
                            lazyItemsIndexed(state.changeMatchResults) { index, candidate ->
                                val focused = index == state.changeMatchIndex
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (focused) accent.copy(alpha = 0.22f) else Color.Transparent)
                                        .clickable { actions.confirmMatch(index) }
                                        .padding(horizontal = 12.dp, vertical = 9.dp),
                                ) {
                                    Text(
                                        candidate.title,
                                        color = Color.White, fontSize = 13.sp,
                                        modifier = Modifier.weight(1f),
                                    )

                                    listOfNotNull(
                                        candidate.platformName,
                                        candidate.releaseYear?.toString(),
                                        candidate.gameArtCount?.let { if (it == 0) "no media" else "$it media" },
                                    )
                                        .joinToString(" · ")
                                        .takeIf { it.isNotEmpty() }
                                        ?.let {
                                            Text(
                                                it,
                                                color = Color.White.copy(alpha = 0.45f), fontSize = 11.sp,
                                            )
                                        }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Search",
                            color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(accent.copy(alpha = 0.30f))
                                .clickable(onClick = actions::submitChangeMatch)
                                .padding(horizontal = 16.dp, vertical = 7.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Cancel",
                            color = Color.White.copy(alpha = 0.65f), fontSize = 12.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.07f))
                                .clickable(onClick = actions::cancelChangeMatch)
                                .padding(horizontal = 14.dp, vertical = 7.dp),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Up/Down  Move  •  A  Select  •  X  Edit title  •  B  Back",
                        color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp,
                    )
                }
            }
        }

        if (state.searchOpen) {
            val focusRequester = remember { FocusRequester() }
            LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0xC0000000))
                    .clickable(onClick = actions::cancelSearch),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    Modifier
                        .width(460.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(echoColors.backgroundBottom)
                        .border(1.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .clickable(enabled = false) {}
                        .padding(18.dp),
                ) {
                    Text(
                        "Search artwork providers",
                        color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Changes what the providers are asked for. It never renames the game.",
                        color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp,
                    )
                    Spacer(Modifier.height(12.dp))
                    BasicTextField(
                        value = state.queryDraft,
                        onValueChange = actions::onQueryDraftChanged,
                        singleLine = true,
                        textStyle = EchoTextStyle.copy(color = Color.White, fontSize = 15.sp),
                        cursorBrush = SolidColor(accent),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = { actions.submitSearch() },
                            onDone = { actions.submitSearch() },
                        ),
                        decorationBox = { inner ->
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                            ) {
                                if (state.queryDraft.isEmpty()) Text(
                                    state.game?.displayTitle ?: "Game title",
                                    color = Color.White.copy(alpha = 0.35f), fontSize = 15.sp,
                                )
                                inner()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Search",
                            color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(accent.copy(alpha = 0.30f))
                                .clickable(onClick = actions::submitSearch)
                                .padding(horizontal = 16.dp, vertical = 7.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Use game title",
                            color = Color.White.copy(alpha = 0.65f), fontSize = 12.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.07f))
                                .clickable(onClick = actions::resetSearchToTitle)
                                .padding(horizontal = 14.dp, vertical = 7.dp),
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            "Cancel",
                            color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(onClick = actions::cancelSearch)
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                        )
                    }
                }
            }
        }

        if (state.actionsOpen && !state.showFileInfo) {
            StudioMenu(
                title = STUDIO_TABS[state.tabIndex].label,
                rows = state.availableActions.map {
                    com.echo.core.ui.components.MenuRow(it, it.label, isDestructive = it == StudioAction.CLEAR, confirms = false)
                },
                selectedIndex = state.resolvedActionsIndex,
                onActivate = actions::runAction,
                onDismiss = actions::closeActions,
            )
        }

        state.confirmPrompt?.let { prompt ->
            StudioMenu(
                title = prompt.title,
                rows = prompt.rows.map {
                    com.echo.core.ui.components.MenuRow(it, it.label, isDestructive = it.isDestructive, confirms = false)
                },
                selectedIndex = prompt.selectedIndex,
                onActivate = { row -> actions.resolveConfirm(prompt.rows.indexOf(row)) },
                onDismiss = actions::dismissConfirm,
            )
        }

        if (state.leavePromptOpen) {
            val waiting = state.selection.size + state.removals.size
            StudioMenu(
                title = if (waiting == 1) "1 change not applied" else "$waiting changes not applied",
                rows = StudioLeaveChoice.entries.map {
                    com.echo.core.ui.components.MenuRow(it, it.label, isDestructive = it == StudioLeaveChoice.DISCARD, confirms = false)
                },
                selectedIndex = state.leavePromptIndex,
                onActivate = actions::resolveLeavePrompt,
                onDismiss = { actions.resolveLeavePrompt(StudioLeaveChoice.STAY) },
            )
        }

        if (state.showFileInfo) {
            val info = state.info
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.9f))
                    .clickable(onClick = actions::closeActions),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    Modifier
                        .width(420.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF14141F))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                        .padding(20.dp),
                ) {
                    Text("FILE INFORMATION", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    if (info == null) {
                        Text(
                            "No stored record for this slot (available once the artwork lives in a linked library).",
                            color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp,
                        )
                    } else {
                        StudioInfoRow("Type", STUDIO_TABS[state.tabIndex].label)
                        StudioInfoRow("Provider", info.provider ?: "—")
                        StudioInfoRow("Source", info.source)
                        StudioInfoRow("Pinned", if (info.userAssigned) "Yes (locked)" else "No")
                        StudioInfoRow("Dimensions", if (info.width != null && info.height != null) "${info.width} × ${info.height}" else "—")
                        StudioInfoRow("Size", formatByteSize(info.sizeBytes))
                        StudioInfoRow("Cropped", if (info.cropRect != null) "Yes" else "No")
                        StudioInfoRow("Previous version", if (info.hasPrevious) "Available" else "—")
                        StudioInfoRow("Path", info.relativePath ?: "—")
                        info.originUrl?.let { StudioInfoRow("Origin", LogRedaction.redact(it)) }
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "Ⓑ  CLOSE", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable(onClick = actions::closeActions)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
        }

        if (state.managerOpen) {
            StudioAssetManagerPanel(
                kindLabel = STUDIO_TABS[state.tabIndex].label,
                assets = state.managedAssets,
                focusedIndex = state.managerIndex,
                busy = state.managerBusy,
                showTouchControls = showTouchControls,
                accent = accent,
                onFocus = actions::focusManagedAsset,
                onMove = actions::moveManagedAsset,
                onMakePrimary = actions::makeManagedAssetPrimary,
                onClose = actions::closeAssetManager,
            )
        }

        state.cropEditorPath?.let { path ->
            StudioCropEditor(
                path = path,
                kind = STUDIO_TABS[state.tabIndex].kind,
                videoPath = state.cropVideoSourcePath,
                previewEnabled = state.cropPreviewEnabled,
                srcW = state.cropSrcW, srcH = state.cropSrcH,
                cropL = state.cropL, cropT = state.cropT, cropR = state.cropR, cropB = state.cropB,
                applying = state.applying,
                onPan = actions::panCrop,
                onZoom = actions::zoomCrop,
                onApply = actions::applyCrop,
                onCancel = actions::cancelCrop,
                shape = CropShapeChoice.of(state.cropProfileOverride),
                onOpenOptions = actions::openCropOptions,
            )

            if (state.cropOptionsOpen) {
                val currentShape = CropShapeChoice.of(state.cropProfileOverride)
                StudioMenu(
                    title = "CROP OPTIONS",
                    rows = state.cropOptionRows.map { row ->
                            val shape = row.shape
                            if (shape == null) {
                                com.echo.core.ui.components.MenuRow(
                                    row,
                                    if (state.cropPreviewEnabled) "Live Preview: On" else "Live Preview: Off",
                                )
                            } else {
                                com.echo.core.ui.components.MenuRow(
                                    row,
                                    "Shape: ${shape.label}",
                                    checked = shape == currentShape,
                                )
                            }
                    },
                    selectedIndex = state.cropOptionsIndex,
                    onActivate = { row -> actions.activateCropOption(state.cropOptionRows.indexOf(row)) },
                    onDismiss = actions::closeCropOptions,
                )
            }
        }

        if (state.cropPreparing) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
                EchoTrio(color = accent)
            }
        }
    }
}

@Composable
internal fun StudioTileBadge(
    mark: StudioTileMark,
    accent: Color,
    markColor: Color,
    modifier: Modifier = Modifier,
) {
    val size = 18.dp
    val circle = androidx.compose.foundation.shape.CircleShape
    when (mark) {
        StudioTileMark.NONE -> Unit
        StudioTileMark.PICKED -> com.echo.core.ui.components.EchoCheckBadge(
            fill = accent, markColor = markColor, modifier = modifier, size = size,
        )
        StudioTileMark.TO_REMOVE -> Box(
            modifier
                .size(size)
                .background(Color.Black.copy(alpha = 0.55f), circle)
                .border(2.dp, Color(0xFFE57373), circle),
            contentAlignment = Alignment.Center,
        ) {
            Text("−", color = Color(0xFFE57373), fontSize = 12.sp, lineHeight = 12.sp, fontWeight = FontWeight.Bold)
        }
        StudioTileMark.QUEUED -> Box(
            modifier
                .size(size)
                .background(Color.Black.copy(alpha = 0.55f), circle)
                .border(2.dp, accent, circle),
        )
        StudioTileMark.DOWNLOADING -> Box(
            modifier.size(size).background(Color.Black.copy(alpha = 0.55f), circle),
            contentAlignment = Alignment.Center,
        ) {
            EchoTrio(color = accent, dot = 4.dp)
        }
        StudioTileMark.ADDED -> com.echo.core.ui.components.EchoCheckBadge(
            fill = Color(0xFF66BB6A),
            markColor = markColor,
            modifier = modifier,
            size = size,
        )

        StudioTileMark.CURRENT -> Box(
            modifier.size(size).background(Color(0xFF66BB6A), circle),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(6.dp).background(markColor, circle))
        }
        StudioTileMark.FAILED -> Box(
            modifier.size(size).background(Color(0xFFE57373), circle),
            contentAlignment = Alignment.Center,
        ) {
            Text("!", color = Color.White, fontSize = 11.sp, lineHeight = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StudioInfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, modifier = Modifier.width(130.dp))
        Text(value, color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StudioCropEditor(
    path: String,
    kind: ArtworkKind,
    videoPath: String?,
    previewEnabled: Boolean,
    srcW: Int, srcH: Int,
    cropL: Float, cropT: Float, cropR: Float, cropB: Float,
    applying: Boolean,
    onPan: (Float, Float) -> Unit,
    onZoom: (Float) -> Unit,
    onApply: () -> Unit,
    onCancel: () -> Unit,
    shape: CropShapeChoice,
    onOpenOptions: () -> Unit,
) {
    val accent = menuCursorEdge()

    val geom = androidx.compose.runtime.rememberUpdatedState(
        CropGeom(srcW, srcH, cropL, cropT, cropR, cropB)
    )
    var bmp by remember(path) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    LaunchedEffect(path) {
        bmp = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            decodeDisplayBitmap(path)?.asImageBitmap()
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.96f))) {
        val image = bmp

        val gestures = Modifier.pointerInput(Unit) {
            detectTransformGestures { _, pan, zoom, _ ->
                val g = geom.value
                val l = cropLayoutFor(g, size.width.toFloat(), size.height.toFloat())

                if (pan.x != 0f || pan.y != 0f) onPan(-pan.x / l.imgDispW, -pan.y / l.imgDispH)
                if (zoom != 1f) onZoom(zoom)
            }
        }

        val canvasPlayer = if (videoPath != null) rememberCropClipPlayer(videoPath) else null

        val insetPlayer =
            if (videoPath != null && previewEnabled) rememberCropClipPlayer(videoPath) else null
        SyncClipTo(leader = canvasPlayer, follower = insetPlayer)

        if (canvasPlayer != null) {
            BoxWithConstraints(Modifier.fillMaxSize().clipToBounds()) {
                val density = androidx.compose.ui.platform.LocalDensity.current
                val l = cropLayoutFor(
                    geom.value,
                    with(density) { maxWidth.toPx() },
                    with(density) { maxHeight.toPx() },
                )
                CropVideoSurface(
                    player = canvasPlayer,
                    modifier = Modifier
                        .offset {
                            androidx.compose.ui.unit.IntOffset(
                                l.imgLeft.roundToInt(), l.imgTop.roundToInt()
                            )
                        }
                        .size(
                            with(density) { l.imgDispW.toDp() },
                            with(density) { l.imgDispH.toDp() },
                        ),
                )

                androidx.compose.foundation.Canvas(Modifier.fillMaxSize().then(gestures)) {
                    drawCropMask(cropLayoutFor(geom.value, size.width, size.height), accent)
                }
            }
        } else if (image == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EchoTrio(color = accent)
            }
        } else {
            androidx.compose.foundation.Canvas(
                Modifier
                    .fillMaxSize()
                    .clipToBounds()
                    .then(gestures),
            ) {
                val l = cropLayoutFor(geom.value, size.width, size.height)
                drawImage(
                    image = image,
                    dstOffset = androidx.compose.ui.unit.IntOffset(l.imgLeft.roundToInt(), l.imgTop.roundToInt()),
                    dstSize = androidx.compose.ui.unit.IntSize(l.imgDispW.roundToInt(), l.imgDispH.roundToInt()),
                )
                drawCropMask(l, accent)
            }
        }

        Column(
            Modifier.fillMaxSize().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                if (shape == CropShapeChoice.PLATFORM_DEFAULT) "ADJUST CROP / POSITION"
                else "ADJUST CROP / POSITION  ·  ${shape.label.uppercase()}",
                color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    if (applying) "Baking…" else "Ⓐ  APPLY CROP",
                    color = Color(0xFF45C46A), fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha = 0.55f))
                        .clickable(enabled = !applying, onClick = onApply)
                        .padding(horizontal = 18.dp, vertical = 9.dp),
                )
                Text(
                    "Ⓑ  CANCEL", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha = 0.55f))
                        .clickable(onClick = onCancel)
                        .padding(horizontal = 18.dp, vertical = 9.dp),
                )

                Text(
                    "Ⓨ  OPTIONS",
                    color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha = 0.55f))
                        .clickable(onClick = onOpenOptions)
                        .padding(horizontal = 18.dp, vertical = 9.dp),
                )
            }
            Text(
                "drag to move the image   ·   pinch to zoom   ·   D-Pad move   ·   LB / RB zoom",
                color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        val chrome = cropPreviewChromeFor(kind).takeIf { previewEnabled }
        val caption = cropPreviewCaptionFor(kind)
        val insetModifier = Modifier
            .align(Alignment.TopEnd)
            .padding(top = 42.dp, end = 20.dp)
        if (chrome != null && caption != null) {
            if (insetPlayer != null) {
                StudioCropPreviewVideoTile(
                    player = insetPlayer,
                    aspect = frameAspectFor(geom.value),
                    cropL = cropL, cropT = cropT, cropR = cropR, cropB = cropB,
                    chrome = chrome,
                    caption = caption,
                    modifier = insetModifier,
                )
            } else if (image != null) {
                StudioCropPreviewTile(
                    image = image,
                    aspect = frameAspectFor(geom.value),
                    cropL = cropL, cropT = cropT, cropR = cropR, cropB = cropB,
                    chrome = chrome,
                    caption = caption,
                    modifier = insetModifier,
                )
            }
        }
    }
}

private data class CropGeom(
    val srcW: Int, val srcH: Int,
    val cropL: Float, val cropT: Float, val cropR: Float, val cropB: Float,
)

private fun frameAspectFor(g: CropGeom): Float {
    val cw = (g.cropR - g.cropL).coerceAtLeast(0.0001f)
    val ch = (g.cropB - g.cropT).coerceAtLeast(0.0001f)
    return (cw * g.srcW) / (ch * g.srcH)
}

private fun frameSizeFor(g: CropGeom, areaW: Float, areaH: Float): Pair<Float, Float> {
    val frameAspect = frameAspectFor(g)
    val fw = if (frameAspect > areaW / areaH) 0.82f * areaW else 0.82f * areaH * frameAspect
    return fw to (fw / frameAspect)
}

private data class CropLayout(
    val fx: Float, val fy: Float, val fw: Float, val fh: Float,
    val imgLeft: Float, val imgTop: Float, val imgDispW: Float, val imgDispH: Float,
)

private fun cropLayoutFor(g: CropGeom, areaW: Float, areaH: Float): CropLayout {
    val (fw, fh) = frameSizeFor(g, areaW, areaH)
    val fx = (areaW - fw) / 2f
    val fy = (areaH - fh) / 2f

    val imgDispW = fw / (g.cropR - g.cropL).coerceAtLeast(0.0001f)
    val imgDispH = fh / (g.cropB - g.cropT).coerceAtLeast(0.0001f)
    return CropLayout(
        fx = fx, fy = fy, fw = fw, fh = fh,
        imgLeft = fx - g.cropL * imgDispW, imgTop = fy - g.cropT * imgDispH,
        imgDispW = imgDispW, imgDispH = imgDispH,
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCropMask(
    l: CropLayout,
    accent: Color,
) {
    val dim = Color.Black.copy(alpha = 0.62f)
    val W = size.width; val H = size.height
    drawRect(dim, size = androidx.compose.ui.geometry.Size(W, l.fy))
    drawRect(dim, topLeft = androidx.compose.ui.geometry.Offset(0f, l.fy + l.fh), size = androidx.compose.ui.geometry.Size(W, H - l.fy - l.fh))
    drawRect(dim, topLeft = androidx.compose.ui.geometry.Offset(0f, l.fy), size = androidx.compose.ui.geometry.Size(l.fx, l.fh))
    drawRect(dim, topLeft = androidx.compose.ui.geometry.Offset(l.fx + l.fw, l.fy), size = androidx.compose.ui.geometry.Size(W - l.fx - l.fw, l.fh))
    drawRect(
        accent,
        topLeft = androidx.compose.ui.geometry.Offset(l.fx, l.fy),
        size = androidx.compose.ui.geometry.Size(l.fw, l.fh),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f),
    )
}

private fun decodeDisplayBitmap(path: String): android.graphics.Bitmap? {
    val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
    android.graphics.BitmapFactory.decodeFile(path, bounds)
    var sample = 1
    val maxDim = 1600
    while (bounds.outWidth / sample > maxDim || bounds.outHeight / sample > maxDim) sample *= 2
    val opts = android.graphics.BitmapFactory.Options().apply { inSampleSize = sample }
    return android.graphics.BitmapFactory.decodeFile(path, opts)
}

@Composable
internal fun StudioVideoTilePreview(url: String, modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var videoSize by remember(url) {
        mutableStateOf<androidx.media3.common.VideoSize?>(null)
    }
    var failed by remember(url) { mutableStateOf(false) }
    var triedLocal by remember(url) { mutableStateOf(false) }

    var source by remember(url) { mutableStateOf(url) }

    val player = remember(source) {
        androidx.media3.exoplayer.ExoPlayer.Builder(context).build().apply {
            trackSelectionParameters = trackSelectionParameters.buildUpon()
                .setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_AUDIO, true)
                .build()
            volume = 0f
            setMediaItem(androidx.media3.common.MediaItem.fromUri(source))
            repeatMode = androidx.media3.common.Player.REPEAT_MODE_ONE
            playWhenReady = true
            prepare()
        }
    }
    DisposableEffect(player) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onVideoSizeChanged(size: androidx.media3.common.VideoSize) { videoSize = size }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                if (!triedLocal) {
                    triedLocal = true
                    scope.launch {
                        val local = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            downloadTilePreviewVideo(context, url)
                        }
                        if (local != null) source = android.net.Uri.fromFile(local).toString()
                        else failed = true
                    }
                } else {
                    timber.log.Timber.w(error, "Studio tile preview failed after local fallback")
                    failed = true
                }
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener); player.release() }
    }
    if (failed) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text("▶ VIDEO", color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
        }
        return
    }
    androidx.compose.ui.viewinterop.AndroidView(
        factory = { ctx ->
            android.view.TextureView(ctx).also { view ->
                player.setVideoTextureView(view)
                view.addOnLayoutChangeListener { v, _, _, _, _, _, _, _, _ ->
                    studioTileCrop(v as android.view.TextureView, videoSize)
                }
            }
        },
        update = { view -> studioTileCrop(view, videoSize) },
        modifier = modifier,
    )
}

private fun downloadTilePreviewVideo(context: android.content.Context, url: String): java.io.File? =
    runCatching {
        val name = "studio_vid_" + Integer.toHexString(url.hashCode()) + ".mp4"
        val dest = java.io.File(context.cacheDir, name)
        if (dest.exists() && dest.length() > 0) return dest
        val conn = (java.net.URL(url).openConnection() as java.net.HttpURLConnection).apply {
            connectTimeout = 15_000; readTimeout = 30_000; instanceFollowRedirects = true
        }
        conn.inputStream.use { input ->
            dest.outputStream().use { out ->
                val buf = ByteArray(64 * 1024); var total = 0L
                while (true) {
                    val n = input.read(buf); if (n == -1) break
                    total += n
                    if (total > 80L * 1024 * 1024) error("tile preview clip too large")
                    out.write(buf, 0, n)
                }
            }
        }
        dest.takeIf { it.length() > 0 } ?: run { dest.delete(); null }
    }.onFailure { timber.log.Timber.w(it, "Tile preview video download failed") }.getOrNull()

private fun studioTileCrop(view: android.view.TextureView, size: androidx.media3.common.VideoSize?) {
    val vw = size?.width?.toFloat() ?: return
    val vh = size.height.toFloat()
    if (vw <= 0f || vh <= 0f || view.width == 0 || view.height == 0) return
    val viewW = view.width.toFloat()
    val viewH = view.height.toFloat()
    val scale = maxOf(viewW / vw, viewH / vh)
    view.setTransform(android.graphics.Matrix().apply {
        setScale((vw * scale) / viewW, (vh * scale) / viewH, viewW / 2f, viewH / 2f)
    })
}

@Composable
private fun StudioPdfPage(
    path: String,
    page: Int,
    onPageCount: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var pageBitmap by remember(path, page) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(path, page) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                android.os.ParcelFileDescriptor.open(
                    java.io.File(path), android.os.ParcelFileDescriptor.MODE_READ_ONLY,
                ).use { pfd ->
                    android.graphics.pdf.PdfRenderer(pfd).use { renderer ->
                        onPageCount(renderer.pageCount)
                        val index = page.coerceIn(0, renderer.pageCount - 1)
                        renderer.openPage(index).use { p ->
                            val scale = 2f
                            val bitmap = android.graphics.Bitmap.createBitmap(
                                (p.width * scale).toInt(), (p.height * scale).toInt(),
                                android.graphics.Bitmap.Config.ARGB_8888,
                            )
                            bitmap.eraseColor(android.graphics.Color.WHITE)
                            p.render(
                                bitmap, null,
                                android.graphics.Matrix().apply { setScale(scale, scale) },
                                android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY,
                            )
                            pageBitmap = bitmap
                        }
                    }
                }
            }.onFailure { timber.log.Timber.w(it, "Manual preview render failed") }
        }
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        val bmp = pageBitmap
        if (bmp != null) {
            androidx.compose.foundation.Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "Manual page",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            EchoTrio(color = Color.White.copy(alpha = 0.5f))
        }
    }
}

