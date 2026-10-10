package com.echo.feature.appbar

import com.echo.core.ui.components.EchoTrio
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toDrawable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.echo.core.domain.model.GamepadAction
import com.echo.core.ui.components.EchoContextMenuOverlay
import com.echo.core.ui.components.StatusStripHeight
import com.echo.core.ui.components.SideRail
import com.echo.core.ui.design.DesignUnits
import com.echo.core.ui.icons.rememberAppIcon
import com.echo.core.ui.preview.CombinedPreviews
import com.echo.core.ui.preview.EchoPreview
import com.echo.feature.appbar.appdrawer.AppWall
import com.echo.feature.appbar.appdrawer.UninstallConfirmDialog
import com.echo.feature.appbar.appdrawer.WallBackdrop
import com.echo.feature.appbar.appdrawer.WallInfo
import com.echo.feature.appbar.appdrawer.actionLabel
import com.echo.core.ui.design.panelDesignUnits
import com.echo.core.ui.design.IsTitan2
import com.echo.feature.appbar.appdrawer.WallInfoStrip
import com.echo.core.ui.design.filmGrain
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AppDrawerScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    initialFilter: AppFilter = AppFilter.DEFAULT,
    pendingGamepadAction: GamepadAction? = null,

    // bumps each time A comes up, ending a hold-to-launch
    selectReleases: Int = 0,

    typedChar: String? = null,
    onTypedCharConsumed: () -> Unit = {},
    onGamepadActionConsumed: () -> Unit = {},

    letterRailHeld: Boolean = false,

    onTouchInteraction: () -> Unit = {},

    onAddToCrossBar: (String) -> Unit = {},

    onLaunchRom: (Long) -> Unit = {},

    onGameMenu: (Long) -> Unit = {},

    onOpenAppSearch: (String) -> Unit = {},

    // the sections live in the top bar (owner, 2026-10-04): the drawer reports the active one and the
    // counts, and a section tapped there arrives here
    onTabsShown: (AppFilter, List<AppFilter>) -> Unit = { _, _ -> },
    tabPick: AppFilter? = null,
    onTabPickConsumed: () -> Unit = {},

    // owner, 2026-10-05: one place grants access; the empty Recently Used page sends people to Permissions
    onOpenPermissions: (() -> Unit)? = null,

    // owner, 2026-10-08: with two screens the focused app is drawn large on the other one, so the drawer
    // reports it and drops its own info column to give the shelf the room
    heroOnOtherScreen: Boolean = false,
    // the focused app, and the drawer's own Launch and Options for it, for the other screen's hero
    onFocusedApp: (app: InstalledApp?, launch: () -> Unit, options: () -> Unit) -> Unit = { _, _, _ -> },

    // the crossbar's genre filter: the Games section shows that genre only (owner, 2026-10-08)
    genreFilter: com.echo.core.domain.model.GameGenre? = null,
    // the Game column grouped by genre: the Games section's buttons are genres too
    chipsByGenre: Boolean = false,
    // X in the Games section switches the grouping between systems and genres (owner, 2026-10-08)
    onToggleGrouping: (() -> Unit)? = null,
    // an album, video or book picked in the drawer, opened by the crossbar
    onOpenMedia: (DrawerMedia) -> Unit = {},
    // Y on an album, video or book: the crossbar's menu for it
    onMediaMenu: (DrawerMedia, String) -> Unit = { _, _ -> },

    viewModel: AppDrawerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(genreFilter) { viewModel.setGenreFilter(genreFilter) }
    LaunchedEffect(chipsByGenre) { viewModel.setChipsByGenre(chipsByGenre) }
    val focusedApp = state.visibleApps.getOrNull(state.selectedIndex)
    LaunchedEffect(focusedApp) {
        onFocusedApp(focusedApp, { focusedApp?.let { viewModel.launchApp(it.packageName) } }, { focusedApp?.let(viewModel::openAppMenu) })
    }
    DisposableEffect(Unit) { onDispose { onFocusedApp(null, {}, {}) } }
    LaunchedEffect(state.activeFilter, state.sections) { onTabsShown(state.activeFilter, state.sections) }
    LaunchedEffect(tabPick) {
        tabPick?.let {
            onTouchInteraction()
            viewModel.setFilter(it)
            onTabPickConsumed()
        }
    }
    val lifecycleOwner = LocalLifecycleOwner.current

    val closeDrawer = { onBack() }

    LaunchedEffect(selectReleases) { if (selectReleases > 0) viewModel.onSelectReleased() }

    LaunchedEffect(pendingGamepadAction) {
        if (pendingGamepadAction != null) {
            val overlayOpen = state.menuApp != null || state.confirmUninstall != null ||
                state.letterCursor != null
            when {
                overlayOpen -> viewModel.handleGamepadAction(pendingGamepadAction)

                pendingGamepadAction == GamepadAction.BACK ->
                    if (state.railFilter != null) viewModel.clearLetterFilter() else closeDrawer()
                // owner, 2026-10-06: RB is Search, the drawer's own here; LB (Apps) shuts the drawer. A section's
                // systems, genres or artists are the side rail's rungs (owner, 2026-10-09)
                pendingGamepadAction == GamepadAction.OPEN_SEARCH ||
                    pendingGamepadAction == GamepadAction.NEXT_PAGE -> onOpenAppSearch("")
                pendingGamepadAction == GamepadAction.PREV_PAGE -> closeDrawer()
                // X steps systems, genres and A to Z, the same cycle the footer's tap steps
                groupingToggleApplies(pendingGamepadAction, state.activeFilter) && onToggleGrouping != null ->
                    viewModel.cycleGamesGrouping(onToggleGrouping)
                pendingGamepadAction == GamepadAction.CHANGE_SORT && mediaGroupingHint(state.activeFilter, MediaGrouping.MAKER) != null ->
                    viewModel.toggleMediaGrouping()
                else -> viewModel.handleGamepadAction(pendingGamepadAction)
            }
            onGamepadActionConsumed()
        }
    }

    LaunchedEffect(state.pendingGameMenu) {
        val id = state.pendingGameMenu ?: return@LaunchedEffect
        onGameMenu(id)
        viewModel.onGameMenuHandled()
    }

    LaunchedEffect(state.pendingMediaOpen) {
        val media = state.pendingMediaOpen ?: return@LaunchedEffect
        onOpenMedia(media)
        viewModel.onMediaOpenHandled()
    }

    LaunchedEffect(state.pendingMediaMenu) {
        val (media, title) = state.pendingMediaMenu ?: return@LaunchedEffect
        onMediaMenu(media, title)
        viewModel.onMediaMenuHandled()
    }

    LaunchedEffect(state.pendingRomLaunch) {
        val id = state.pendingRomLaunch ?: return@LaunchedEffect
        onLaunchRom(id)
        viewModel.onRomLaunchHandled()
    }

    LaunchedEffect(state.pendingCrossBarAdd) {
        val packageName = state.pendingCrossBarAdd ?: return@LaunchedEffect
        onAddToCrossBar(packageName)
        viewModel.onCrossBarAddHandled()
    }

    LaunchedEffect(letterRailHeld) {
        if (letterRailHeld) viewModel.openLetterJump() else viewModel.closeLetterJump()
    }

    LaunchedEffect(typedChar) {
        val ch = typedChar ?: return@LaunchedEffect

        onOpenAppSearch(ch)
        onTypedCharConsumed()
    }

    val appliedInitial = remember { mutableStateOf(false) }
    if (!appliedInitial.value) {
        viewModel.setFilter(initialFilter)
        appliedInitial.value = true
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    AppDrawerContent(
        state = state,

        onBack = {
            onTouchInteraction()
            closeDrawer()
        },
        onOpenSearch = {
            onTouchInteraction()
            onOpenAppSearch("")
        },
        onFilterSelected = { filter ->
            onTouchInteraction()
            viewModel.setFilter(filter)
        },
        onAppTapped = { index ->
            onTouchInteraction()
            viewModel.onAppTapped(index)
        },
        onAppLaunched = { viewModel.launchApp(it) },
        onAppMenu = { viewModel.openAppMenu(it) },
        onBandLaunch = { app ->
            onTouchInteraction()
            viewModel.launchApp(app.packageName)
        },
        onBandOptions = { app ->
            onTouchInteraction()
            viewModel.openAppMenu(app)
        },
        onMenuRowActivated = viewModel::onMenuRowActivated,
        onLetterRailTouch = viewModel::onLetterRailTouch,
        onLetterRailReleased = viewModel::onLetterRailReleased,
        groupingHint = mediaGroupingHint(state.activeFilter, state.mediaGrouping)?.let { it to viewModel::toggleMediaGrouping }
            ?: onToggleGrouping?.let { toggle -> groupingHintLabel(chipsByGenre, state.gamesByLetter) to { viewModel.cycleGamesGrouping(toggle) } },
        onCloseMenu = { viewModel.closeAppMenu() },
        onConfirmUninstall = { viewModel.confirmUninstall() },
        onCancelUninstall = { viewModel.cancelUninstall() },
        onGrantUsageAccess = onOpenPermissions ?: { viewModel.openUsageAccessSettings() },
        heroOnOtherScreen = heroOnOtherScreen,
        modifier = modifier,
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun AppDrawerContent(
    state: AppDrawerUiState,
    onBack: () -> Unit,
    onOpenSearch: () -> Unit,
    onFilterSelected: (AppFilter) -> Unit,
    onAppTapped: (Int) -> Unit,
    onAppLaunched: (String) -> Unit,
    onAppMenu: (InstalledApp) -> Unit,
    onCloseMenu: () -> Unit,
    onConfirmUninstall: () -> Unit,
    onCancelUninstall: () -> Unit,
    onGrantUsageAccess: () -> Unit,
    modifier: Modifier = Modifier,
    onBandLaunch: (InstalledApp) -> Unit = { onAppLaunched(it.packageName) },
    onBandOptions: (InstalledApp) -> Unit = onAppMenu,
    heroOnOtherScreen: Boolean = false,
    onMenuRowActivated: (Int) -> Unit = {},

    onLetterRailTouch: (Int) -> Unit = {},
    onLetterRailReleased: () -> Unit = {},
    // the X hint in the Games section and what a tap on it does
    groupingHint: Pair<String, () -> Unit>? = null,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize().filmGrain()) {
        val u = panelDesignUnits(maxWidth.value, maxHeight.value, LocalDensity.current)
        val focused = state.visibleApps.getOrNull(state.selectedIndex)
        val focusedIcon = rememberAppIcon(focused?.packageName?.takeIf { focused.gameId == null })
        WallBackdrop(focused, focusedIcon, u)

        // owner, 2026-10-05: the design's 6a: the chosen app's details in a column on the left, the apps as cases
        // in three columns on the right, the bar's sections above and the hints below
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(Modifier.height(StatusStripHeight))
            // owner, 2026-10-09: on the Titan 2's square screen the chosen app's details are a strip across the top and
            // the cases take the whole width under it
            val strip = IsTitan2 && !heroOnOtherScreen
            if (strip) focused?.let { app ->
                WallInfoStrip(app, focusedIcon, u, onLaunch = { onBandLaunch(app) }, onOptions = { onBandOptions(app) },
                    holding = state.holdingPackage == app.packageName, details = state.gameDetails?.takeIf { it.gameId == app.gameId },
                    modifier = Modifier.fillMaxWidth().padding(start = u.dp(46), end = u.dp(52), top = u.dp(16)))
            }
            Row(Modifier.weight(1f).fillMaxWidth().padding(start = u.dp(46), end = u.dp(52))) {
                if (!heroOnOtherScreen && !strip) {
                    Column(Modifier.width(u.dp(420)).fillMaxHeight().padding(top = u.dp(24)), verticalArrangement = Arrangement.spacedBy(u.dp(16))) {
                        // owner, 2026-10-09: the page's name over the chosen app, with the rail's filter when one is on
                        Text(drawerHeading(state).uppercase(), style = u.eyebrow(Color.White.copy(alpha = 0.7f)), maxLines = 1)
                        focused?.let { app ->
                            WallInfo(app, focusedIcon, u, onLaunch = { onBandLaunch(app) }, onOptions = { onBandOptions(app) },
                                holding = state.holdingPackage == app.packageName, details = state.gameDetails?.takeIf { it.gameId == app.gameId })
                        }
                    }
                    Spacer(Modifier.width(u.dp(56)))
                }
                Column(Modifier.weight(1f).fillMaxHeight().padding(top = u.dp(12))) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        when {
                            state.isLoading -> EchoTrio(color = Color.White, modifier = Modifier.align(Alignment.Center))
                            state.visibleApps.isEmpty() -> Box(Modifier.align(Alignment.Center)) {
                                EmptyDrawerMessage(
                                    filter = state.activeFilter,
                                    hasUsageAccess = state.hasUsageAccess,
                                    onGrantUsageAccess = onGrantUsageAccess,
                                    u = u,
                                )
                            }
                            else -> AppWall(
                                apps = state.visibleApps,
                                selectedIndex = state.selectedIndex,
                                usingTouch = state.usingTouch,
                                u = u,
                                onAppTapped = onAppTapped,
                                onAppMenu = onAppMenu,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            }

            // owner, 2026-10-07: the kit's footer, not a hint row of its own. The section is the footer's filter,
            // LT/RT and its word, at the far left (owner, 2026-10-06)
            com.echo.core.ui.components.EchoHintBar(
                items = listOfNotNull(
                    com.echo.core.ui.components.ControllerPromptItem(GamepadAction.BACK, "Back"),
                    groupingHint?.takeIf { state.activeFilter == AppFilter.GAMES || mediaGroupingHint(state.activeFilter, MediaGrouping.MAKER) != null }
                        ?.let { com.echo.core.ui.components.ControllerPromptItem(GamepadAction.CHANGE_SORT, it.first) },
                    com.echo.core.ui.components.ControllerPromptItem(GamepadAction.NEXT_PAGE, "Search"),
                ),
                primary = focused?.let { com.echo.core.ui.components.HintAction(GamepadAction.SELECT, actionLabel(it)) },
                filter = {
                    com.echo.core.ui.components.TriggerFilter(
                        // the rail's filter beside the section, since nothing else says the list is narrowed
                        drawerHeading(state),
                        onTapped = { onFilterSelected(state.activeFilter.stepped(1, state.sections)) },
                    )
                },
                onAction = { action ->
                    when (action) {
                        GamepadAction.SELECT -> focused?.let(onBandLaunch)
                        GamepadAction.NEXT_PAGE -> onOpenSearch()
                        GamepadAction.CHANGE_SORT -> groupingHint?.second?.invoke()
                        GamepadAction.BACK -> onBack()
                        else -> Unit
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        SideRail(
            letters = state.railRungs,
            cursor = state.letterCursor,
            onTouch = onLetterRailTouch,
            onReleased = onLetterRailReleased,
            // clear of the bar above and the hints below, on the screen's centre line
            top = u.dp(76),
            bottom = u.dp(76),
        )

        state.appMenu?.let { menu ->
            EchoContextMenuOverlay(
                state = menu,
                onRowActivated = onMenuRowActivated,
                onDismiss = onCloseMenu,
            )
        }

        state.confirmUninstall?.let { app ->
            UninstallConfirmDialog(
                app = app,
                confirmFocused = state.uninstallConfirmFocused,
                onConfirm = onConfirmUninstall,
                onCancel = onCancelUninstall,
            )
        }
    }
}

@Composable
private fun EmptyDrawerMessage(
    filter: AppFilter,
    hasUsageAccess: Boolean,
    onGrantUsageAccess: () -> Unit,
    u: DesignUnits,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = u.dp(8)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = when {
                filter == AppFilter.GAMES -> "No games found"
                filter == AppFilter.EMULATORS -> "No emulators installed"
                filter == AppFilter.RECENT && !hasUsageAccess -> "Usage access needed"
                filter == AppFilter.RECENT -> "No recently used apps yet"
                else -> "No apps installed"
            },
            color = Color.White,
            fontSize = u.sp(20),
            fontWeight = FontWeight.Light,
        )
        Spacer(Modifier.height(u.dp(6)))
        Text(
            text = when {
                filter == AppFilter.GAMES -> "Apps marked as games in the Play Store appear here"
                filter == AppFilter.EMULATORS -> "Install RetroArch, PPSSPP, or another emulator"
                filter == AppFilter.RECENT && !hasUsageAccess -> "Grant access so ECHO can sort apps by last used time"
                else -> ""
            },
            color = Color.White.copy(alpha = 0.6f),
            fontSize = u.sp(14),
            fontWeight = FontWeight.Light,
            textAlign = TextAlign.Center,
        )
        if (filter == AppFilter.RECENT && !hasUsageAccess) {
            Spacer(Modifier.height(u.dp(14)))
            Text(
                text = "Open Usage Access",
                color = Color.White,
                fontSize = u.sp(15),
                modifier = Modifier
                    .heightIn(min = 44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.White.copy(alpha = 0.14f))
                    .clickable { onGrantUsageAccess() }
                    .padding(horizontal = u.dp(22), vertical = u.dp(12)),
            )
        }
    }
}

@CombinedPreviews
@Composable
fun AppDrawerScreenPreview() {
    EchoPreview {
        AppDrawerPreviewContent()
    }
}

@Composable
private fun AppDrawerPreviewContent() {
    val mockIcon = android.graphics.Color.LTGRAY.toDrawable()
    val mockApps = listOf(
        InstalledApp("com.android.chrome", "Chrome", mockIcon, isGame = false, isEmulator = false),
        InstalledApp("org.ppsspp.ppsspp", "PPSSPP", mockIcon, isGame = false, isEmulator = true),
        InstalledApp("com.retroarch", "RetroArch", mockIcon, isGame = false, isEmulator = true),
        InstalledApp(
            "com.google.android.youtube",
            "YouTube",
            mockIcon,
            isGame = false,
            isEmulator = false
        ),
        InstalledApp(
            "com.echo.launcher",
            "ECHO",
            mockIcon,
            isGame = false,
            isEmulator = false
        ),
    )

    val mockCounts = AppFilter.entries.associateWith { filter -> mockApps.count(filter::matches) }
    val mockState = AppDrawerUiState(
        visibleApps = mockApps.filter(AppFilter.DEFAULT::matches),
        activeFilter = AppFilter.DEFAULT,
        selectedIndex = 1,
        filterCounts = mockCounts,
    )
    AppDrawerContent(
        state = mockState,
        onBack = {},
        onOpenSearch = {},
        onFilterSelected = {},
        onAppTapped = {},
        onAppLaunched = {},
        onAppMenu = {},
        onCloseMenu = {},
        onConfirmUninstall = {},
        onCancelUninstall = {},
        onGrantUsageAccess = {},
    )
}

// the section, the genre the crossbar narrows Games to, and the rail's filter: "Games · NDS", "Music · Jhené Aiko".
// The footer word and the heading over the chosen app both say it, since nothing else shows the list is narrowed
internal fun drawerHeading(state: AppDrawerUiState): String =
    listOfNotNull(sectionLabel(state.activeFilter, state.genreFilter), state.railFilterName).joinToString(" · ")
