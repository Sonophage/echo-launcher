package com.echo.feature.settings.ui

import androidx.compose.runtime.DisposableEffect
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.graphicsLayer
import com.echo.core.ui.components.ContextMenuHeader
import com.echo.core.ui.components.ContextMenuRowLabel
import com.echo.core.ui.components.HintBarHeight
import com.echo.core.ui.components.RailEdgeGap
import com.echo.core.ui.components.StatusStripHeight
import com.echo.core.ui.components.contextMenuInk
import com.echo.core.ui.components.contextMenuRow
import com.echo.core.ui.design.RAIL_PANEL_WIDTH
import com.echo.core.ui.design.RailPanelFill
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.echo.core.data.repository.EchoThemeStore
import com.echo.core.data.repository.ThemeCatalogRepository
import com.echo.core.domain.model.GamepadAction
import com.echo.core.ui.components.ControllerPromptItem
import com.echo.core.ui.components.EchoHintBar
import com.echo.core.ui.components.chose
import com.echo.core.ui.components.MenuSelect
import com.echo.core.ui.components.back
import com.echo.core.ui.components.at
import com.echo.core.ui.components.moved
import com.echo.core.ui.components.MenuRow
import com.echo.core.ui.components.MenuState
import com.echo.core.ui.components.EchoContextMenuOverlay
import com.echo.core.ui.components.HintAction
import com.echo.core.ui.design.PanelBase
import com.echo.core.ui.design.panelDesignUnits
import com.echo.core.ui.theme.EchoTextStyle
import com.echo.feature.settings.viewmodel.ThemePage
import com.echo.feature.settings.viewmodel.ThemesSettingsViewModel
import com.echo.themekit.ThemePart

// the theme the store has in focus, for a second screen to show large (owner, 2026-10-08): with two screens the
// store is on the companion and the crossbar's screen shows the theme. picture is its wallpaper, or on its page
// the screenshot in view
data class StorePreview(val name: String, val line: String, val picture: String?, val accentArgb: Long? = null)

// set by a host with a second screen; null on one screen, where the store's own hero shows the theme
val LocalStorePreview = androidx.compose.runtime.compositionLocalOf<((StorePreview?) -> Unit)?> { null }

// a card in the theme store: a saved theme or an online one
internal data class StoreCard(val id: String, val name: String, val subtitle: String, val image: String?, val accentArgb: Long? = null,
    // the theme's wallpaper, for the hero above the shelves; the card itself shows [image]
    val wallpaper: String? = null,
    // an online theme's standing against the saved one; the tag, the hero and the featured card read this, not
    // the subtitle's words
    val standing: ThemeCatalogRepository.Standing? = null)

internal fun EchoThemeStore.SavedTheme.card() =
    StoreCard(id, name, author?.let { "by $it" } ?: "${parts.size} parts", heroPath ?: previewPath, accentArgb)

// the card that opens the look in use (owner, 2026-10-07: the theme page is also the view of the current look)
internal const val CURRENT_LOOK_ID = "__current_look"

// one shelf of the store: a row of 16:9 cards, scrolled so the focused one stays in view
@Composable
private fun StoreShelf(cards: List<StoreCard>, focusedIndex: Int?, onOpen: (String) -> Unit) {
    val list = rememberLazyListState()
    LaunchedEffect(focusedIndex) { focusedIndex?.let { list.animateScrollToItem((it - 1).coerceAtLeast(0)) } }
    LazyRow(state = list, horizontalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)) {
        itemsIndexed(cards, key = { _, c -> c.id }) { index, card ->
            val focused = focusedIndex == index
            Column(Modifier.width(176.dp).graphicsLayer(alpha = if (focusedIndex == null || focused) 1f else 0.6f).clickable { onOpen(card.id) }) {
                Box(
                    Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(12.dp))
                        .background(Color(card.accentArgb?.let { it and 0xFFFFFFFFL } ?: 0xFF20304AL))
                        .border(if (focused) 3.dp else 1.dp, if (focused) Color.White else Color(0x33FFFFFF), RoundedCornerShape(12.dp)),
                ) {
                    card.image?.let { AsyncImage(model = it, contentDescription = card.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
                }
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(card.name, color = Color.White, fontSize = 15.sp, fontWeight = if (focused) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    StandingTag(card.subtitle, card.standing)
                }
            }
        }
    }
}

// New and Update stand out; the rest ("Downloaded", "In use", "by …") is quiet text
@Composable
private fun StandingTag(text: String, standing: ThemeCatalogRepository.Standing?) {
    val loud = standing == ThemeCatalogRepository.Standing.NEW || standing == ThemeCatalogRepository.Standing.UPDATE
    Text(
        text, maxLines = 1, fontSize = 11.sp,
        color = if (loud) RailInkColor else SettingsSubtext,
        fontWeight = if (loud) FontWeight.SemiBold else FontWeight.Normal,
        modifier = if (loud) Modifier.clip(RoundedCornerShape(8.dp)).background(if (standing == ThemeCatalogRepository.Standing.UPDATE) UpdateAmber else Color.White).padding(horizontal = 8.dp, vertical = 2.dp)
        else Modifier,
    )
}

private val RailInkColor = Color(0xFF1A0C03)
private val UpdateAmber = Color(0xFFE8A93A)

// the store (owner, 2026-10-07, design "Store B"): a hero showing the card in focus, then shelves of cards for
// the themes on this device (led by the look in use) and the online store's. A card opens its theme page
@Composable
fun ThemeStoreScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ThemesSettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var shot by remember { mutableIntStateOf(0) }
    // the shelf the controller is on, and the card within it
    var focusedShelf by remember { mutableStateOf<String?>(null) }
    var column by remember { mutableIntStateOf(0) }
    var savePrompt by remember { mutableStateOf(false) }
    var saveName by remember { mutableStateOf("") }
    // Y on a saved theme: Apply, Share, Remove. Remove deletes files, so it asks first (the kit's confirm)
    var menu by remember { mutableStateOf<MenuState<ThemeMenuOption>?>(null) }
    fun choose(state: MenuState<ThemeMenuOption>, index: Int) {
        when (val picked = state.chose(index)) {
            is MenuSelect.Run -> { picked.action.action(); menu = null }
            is MenuSelect.Replace -> menu = picked.state
            else -> Unit
        }
    }
    val saved = listOf(StoreCard(CURRENT_LOOK_ID, state.activeThemeName, "Your look", null, state.accentOverrideArgb)) +
        state.savedThemes.map { t -> t.card().copy(subtitle = if (t.name == state.activeThemeName) "In use" else t.card().subtitle) }
    val online = state.online.orEmpty().map { t ->
        val standing = ThemeCatalogRepository.standing(state.savedThemes.firstOrNull { it.name == t.name }, t)
        StoreCard(t.id, t.name, when (standing) {
            ThemeCatalogRepository.Standing.NEW -> "New"
            ThemeCatalogRepository.Standing.CURRENT -> "Downloaded"
            ThemeCatalogRepository.Standing.UPDATE -> "Update"
        }, t.heroUrl, wallpaper = t.wallpaperUrl, standing = standing)
    }
    fun shelf(section: String) = if (section == "saved") saved else online
    fun open(section: String, id: String) {
        shot = 0
        when {
            id == CURRENT_LOOK_ID -> viewModel.openCurrentLook()
            section == "saved" -> viewModel.openThemePage(id)
            else -> viewModel.openOnlinePage(id)
        }
    }
    fun openMenu(theme: EchoThemeStore.SavedTheme) {
        val options = listOf(
            ThemeMenuOption("Apply") { viewModel.applySavedTheme(theme.id) },
            ThemeMenuOption("Share") { viewModel.shareSavedTheme(theme.id) },
            ThemeMenuOption("Remove and delete its folder", destructive = true) { viewModel.deleteSavedTheme(theme.id) },
        )
        menu = MenuState(title = theme.name, rows = options.map { MenuRow(it, it.label, isDestructive = it.destructive) }, selectedIndex = 0)
    }
    // the hero shows the card in focus; before any is focused, the newest online theme
    val featured = focusedShelf?.let { shelf(it).getOrNull(column) }
        ?: online.firstOrNull { it.standing == ThemeCatalogRepository.Standing.NEW } ?: online.firstOrNull() ?: saved.first()
    val featuredSection = focusedShelf ?: if (featured.id == CURRENT_LOOK_ID || online.none { it.id == featured.id }) "saved" else "online"

    LocalStorePreview.current?.let { report ->
        val page = state.page
        val preview = if (page != null) {
            StorePreview(
                name = page.name,
                line = listOfNotNull(page.author?.let { "by $it" }, page.version?.let { "version $it" }).joinToString("  ·  "),
                picture = page.screenshots.getOrNull(shot) ?: page.backdrop ?: page.hero,
            )
        } else {
            StorePreview(featured.name, featured.subtitle, featured.wallpaper ?: featured.image, featured.accentArgb)
        }
        LaunchedEffect(preview) { report(preview) }
        DisposableEffect(Unit) { onDispose { report(null) } }
    }

    Box(modifier) {
        SettingsPageScaffold(
            subtitle = "Store",
            onBack = onBack,
            fullWidth = true,
            modifier = Modifier.fillMaxSize(),
            helperFooterItems = SettingsDefaultHelperItems + ControllerPromptItem(GamepadAction.OPEN_CONTEXT_MENU, "Options"),
            onInterceptAction = { action ->
                val page = state.page
                val m = menu
                when {
                    savePrompt -> false
                    m != null -> {
                        when (action) {
                            GamepadAction.NAVIGATE_UP -> menu = m.moved(-1)
                            GamepadAction.NAVIGATE_DOWN -> menu = m.moved(+1)
                            GamepadAction.SELECT -> m.selectedIndex?.let { choose(m, it) }
                            GamepadAction.BACK -> menu = m.back()
                            GamepadAction.OPEN_CONTEXT_MENU -> menu = null
                            else -> Unit
                        }
                        true
                    }
                    page != null -> {
                        val shots = page.screenshots.size
                        when (action) {
                            GamepadAction.BACK -> viewModel.closeThemePage()
                            GamepadAction.NAVIGATE_UP -> viewModel.pageMove(-1)
                            GamepadAction.NAVIGATE_DOWN -> viewModel.pageMove(+1)
                            GamepadAction.SELECT -> if (page.current && page.cursor == 0) savePrompt = true else viewModel.pageSelect()
                            GamepadAction.NEXT_CATEGORY, GamepadAction.NAVIGATE_RIGHT -> if (shots > 0) shot = (shot + 1) % shots
                            GamepadAction.PREV_CATEGORY, GamepadAction.NAVIGATE_LEFT -> if (shots > 0) shot = (shot - 1 + shots) % shots
                            else -> Unit
                        }
                        true
                    }
                    focusedShelf == "saved" && action == GamepadAction.OPEN_CONTEXT_MENU -> {
                        state.savedThemes.getOrNull(column - 1)?.let(::openMenu); true
                    }
                    focusedShelf != null && action == GamepadAction.NAVIGATE_LEFT -> { column = (column - 1).coerceAtLeast(0); true }
                    focusedShelf != null && action == GamepadAction.NAVIGATE_RIGHT -> {
                        column = (column + 1).coerceAtMost(shelf(focusedShelf!!).size - 1); true
                    }
                    else -> false
                }
            },
        ) {
            // the hero stays put above the shelves, so it is never scrolled under the tab row
            Column(Modifier.fillMaxSize()) {
            // with a second screen the theme shows large there, so the shelves take this screen
            if (LocalStorePreview.current == null) StoreHero(featured, featuredSection == "online")
            // two shelves: the store scrolls itself (the first to the top, the last to the bottom), since the
            // scaffold's own scrolling measures rows from the top of the page and would slide one under the hero
            val scroll = rememberScrollState()
            LaunchedEffect(focusedShelf) {
                when (focusedShelf) {
                    "online" -> scroll.animateScrollTo(0)
                    "saved" -> scroll.animateScrollTo(scroll.maxValue)
                }
            }
            Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(scroll)) {

                @Composable
                fun shelfRow(section: String, title: String, cards: List<StoreCard>) {
                    Text("$title  ·  ${cards.size}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 48.dp, top = 14.dp))
                    FocusableStrip(
                        onFocusChange = { focused ->
                            if (focused) { focusedShelf = section; column = column.coerceIn(0, cards.size - 1) }
                            else if (focusedShelf == section) focusedShelf = null
                        },
                        onSelect = { cards.getOrNull(column)?.let { open(section, it.id) } },
                        plate = false,
                    ) { stripFocused ->
                        StoreShelf(cards, if (stripFocused) column else null) { open(section, it) }
                    }
                }
                when {
                    online.isNotEmpty() -> shelfRow("online", "Online", online)
                    state.onlineFailed -> SettingsRow(label = "The online store could not be reached", sublabel = "Check the connection, then try again", onClick = viewModel::refreshOnline)
                    state.online == null -> SettingsRow(label = "Loading the online store", sublabel = null, onClick = null)
                    else -> SettingsRow(label = "No themes online yet", sublabel = null, onClick = null)
                }
                shelfRow("saved", "On this device", saved)
                state.installMessage?.let { SettingsRow(label = it, sublabel = "Tap to dismiss", onClick = viewModel::dismissMessage) }
            }
            }
        }

        menu?.let { m ->
            EchoContextMenuOverlay(
                state = m,
                onRowActivated = { index -> if (index == m.selectedIndex) choose(m, index) else menu = m.at(index) },
                onDismiss = { menu = null },
            )
        }

        state.page?.let { page ->
            ThemePageOverlay(
                page = page,
                shot = shot,
                partSources = state.partSources,
                onRow = { index ->
                    viewModel.pageMove(index - page.cursor)
                    if (page.current && index == 0) savePrompt = true else viewModel.pageSelect()
                },
                onBack = viewModel::closeThemePage,
                onShot = { i -> shot = i },
            )
        }

        if (savePrompt) {
            SettingsTextPromptOverlay(
                title = "Save Current Look as Theme",
                value = saveName,
                onValueChange = { saveName = it },
                onConfirm = {
                    savePrompt = false
                    viewModel.saveCurrentLookAsTheme(saveName)
                    viewModel.closeThemePage()
                    saveName = ""
                },
                onCancel = { savePrompt = false; saveName = "" },
                placeholder = "Theme name",
            )
        }
    }
}

// the hero above the shelves: the focused card's picture with its name and standing
@Composable
private fun StoreHero(card: StoreCard, online: Boolean) {
    Box(Modifier.fillMaxWidth().height(150.dp)) {
        Box(Modifier.fillMaxSize().background(Color(card.accentArgb?.let { it and 0xFFFFFFFFL } ?: 0xFF20304AL)))
        (card.wallpaper ?: card.image)?.let { AsyncImage(model = it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(0f to PanelBase.copy(alpha = 0.9f), 0.6f to PanelBase.copy(alpha = 0.35f), 1f to Color.Transparent)))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.55f to Color.Transparent, 1f to PanelBase)))
        Column(Modifier.align(Alignment.BottomStart).padding(start = 48.dp, bottom = 22.dp, end = 48.dp)) {
            Text(
                when {
                    card.id == CURRENT_LOOK_ID -> "THE LOOK IN USE"
                    online && card.standing == ThemeCatalogRepository.Standing.NEW -> "NEW IN THE STORE"
                    online -> "IN THE STORE"
                    else -> "ON THIS DEVICE"
                },
                color = Color.White.copy(alpha = 0.75f), fontSize = 11.sp, letterSpacing = 2.sp,
            )
            Text(card.name, color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Light, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(card.subtitle, color = SettingsSubtext, fontSize = 14.sp, maxLines = 1)
        }
    }
}

// a theme's page (owner, 2026-10-07, design "Store C"): its picture behind, its details and screenshots on the
// left, and on the rail the action and its parts. Each part row says what the look uses now and can be ticked
// in or out; A takes the ticked parts. The look in use opens here too, as a page with nothing to tick
@Composable
internal fun ThemePageOverlay(
    page: ThemePage,
    shot: Int,
    partSources: Map<ThemePart, String>,
    onRow: (Int) -> Unit,
    onBack: () -> Unit,
    onShot: (Int) -> Unit,
) {
    val shots = page.screenshots
    BoxWithConstraints(Modifier.fillMaxSize().background(PanelBase).clickable(enabled = false) {}) {
        val u = panelDesignUnits(maxWidth.value, maxHeight.value, LocalDensity.current)
        // blurred, so the details and the rail read over it (owner, 2026-10-07)
        page.backdrop?.let {
            AsyncImage(model = com.echo.core.ui.image.rememberBlurSourceModel(it), contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().blur(u.dp(24)))
        }
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(0f to PanelBase.copy(alpha = 0.92f), 0.55f to PanelBase.copy(alpha = 0.45f), 1f to PanelBase.copy(alpha = 0.2f))))

        Column(
            Modifier.fillMaxHeight().fillMaxWidth().padding(start = u.dp(64), end = u.dp(RAIL_PANEL_WIDTH + 40), top = u.dp(70), bottom = u.dp(84))
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                when {
                    page.current -> "THE LOOK IN USE"
                    page.update -> "THEME · UPDATE AVAILABLE"
                    else -> "THEME"
                },
                style = u.eyebrow(),
            )
            Text(page.name, style = EchoTextStyle.copy(color = Color.White, fontSize = u.sp(48), fontWeight = FontWeight.Light), maxLines = 2)
            listOfNotNull(page.author?.let { "by $it" }, page.version?.let { "version $it" }).takeIf { it.isNotEmpty() }?.let {
                Text(it.joinToString("  ·  "), style = EchoTextStyle.copy(color = SettingsSubtext, fontSize = u.sp(15)))
            }
            if (page.current) {
                Text("Each part on the right says which theme it came from. Save it as a theme to keep it or share it.",
                    style = EchoTextStyle.copy(color = Color.White, fontSize = u.sp(16)), modifier = Modifier.padding(top = u.dp(14)))
            }
            page.description?.let {
                Text(it, style = EchoTextStyle.copy(color = Color.White, fontSize = u.sp(17)), modifier = Modifier.padding(top = u.dp(14)))
            }
            if (shots.isNotEmpty()) {
                Text("SCREENSHOTS", style = u.eyebrow(), modifier = Modifier.padding(top = u.dp(22)))
                Row(horizontalArrangement = Arrangement.spacedBy(u.dp(12)), modifier = Modifier.padding(top = u.dp(8)).horizontalScroll(rememberScrollState())) {
                    shots.forEachIndexed { i, s ->
                        AsyncImage(
                            model = s, contentDescription = "Screenshot ${i + 1} of ${shots.size}", contentScale = ContentScale.Crop,
                            modifier = Modifier.width(u.dp(if (i == shot) 300 else 170)).aspectRatio(16f / 9f).clip(RoundedCornerShape(u.dp(10)))
                                .border(if (i == shot) 2.dp else 1.dp, Color.White.copy(alpha = if (i == shot) 1f else 0.25f), RoundedCornerShape(u.dp(10)))
                                .clickable { onShot(i) },
                        )
                    }
                }
            }
            page.body?.takeIf { it.isNotBlank() }?.let {
                Text(readmeText(it), style = EchoTextStyle.copy(color = SettingsSubtext, fontSize = u.sp(14)), modifier = Modifier.padding(top = u.dp(18)))
            }
        }

        // the rail: the action row, then a row per part
        Box(Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(u.dp(RAIL_PANEL_WIDTH)).background(RailPanelFill))
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(u.dp(6)),
            modifier = Modifier.align(Alignment.CenterEnd).width(u.dp(RAIL_PANEL_WIDTH)).fillMaxHeight()
                .padding(top = StatusStripHeight + com.echo.core.ui.components.RailTopGap, bottom = HintBarHeight, end = RailEdgeGap, start = RailEdgeGap)
                .verticalScroll(rememberScrollState()),
        ) {
            ContextMenuHeader(
                if (page.current) "Your look" else "Take from ${page.name}",
                if (page.current) "WHERE EACH PART CAME FROM" else "EVERYTHING, OR ONLY THE PARTS YOU WANT",
                u,
            )
            Box(Modifier.contextMenuRow(focused = page.cursor == 0, dim = 1f, u = u) { onRow(0) }) {
                ContextMenuRowLabel(page.actionLabel, focused = page.cursor == 0, u = u)
            }
            ThemePart.entries.forEachIndexed { i, part ->
                val row = i + 1
                val focused = page.cursor == row
                val has = part in page.parts
                val source = partSources[part]?.let { "From $it" } ?: "Your own"
                Row(
                    Modifier.fillMaxWidth().contextMenuRow(focused = focused, dim = if (has || page.current) 1f else 0.4f, u = u) { onRow(row) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // the part, with where it comes from beneath it: side by side, a narrow rail cut it to "From Ry…"
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        ContextMenuRowLabel(part.label, focused = focused, u = u)
                        Text(if (has || page.current) source else "Not in this theme", color = contextMenuInk(focused).copy(alpha = 0.65f),
                            fontSize = u.sp(12), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (has && !page.current) {
                        val ink = contextMenuInk(focused)
                        Box(
                            Modifier.padding(start = u.dp(10)).size(u.dp(18)).border(1.5.dp, ink, RoundedCornerShape(u.dp(5))),
                            contentAlignment = Alignment.Center,
                        ) {
                            // a filled square, not a tick character: the app's font has no tick and drew nothing
                            if (part in page.partsToTake) Box(Modifier.size(u.dp(10)).clip(RoundedCornerShape(u.dp(2))).background(ink))
                        }
                    }
                }
            }
        }

        EchoHintBar(
            items = listOfNotNull(
                ControllerPromptItem(GamepadAction.BACK, "Back"),
                ControllerPromptItem(listOf(GamepadAction.PREV_CATEGORY, GamepadAction.NEXT_CATEGORY), "Screenshots").takeIf { shots.size > 1 },
            ),
            primary = when {
                page.cursor == 0 -> HintAction(GamepadAction.SELECT, page.actionLabel)
                page.current -> null
                ThemePart.entries.getOrNull(page.cursor - 1)?.let { it in page.parts } == true ->
                    HintAction(GamepadAction.SELECT, if (ThemePart.entries[page.cursor - 1] in page.partsToTake) "Leave out" else "Take")
                else -> null
            },
            onAction = { action ->
                when (action) {
                    GamepadAction.BACK -> onBack()
                    GamepadAction.SELECT -> onRow(page.cursor)
                    GamepadAction.NEXT_CATEGORY -> if (shots.isNotEmpty()) onShot((shot + 1) % shots.size)
                    GamepadAction.PREV_CATEGORY -> if (shots.isNotEmpty()) onShot((shot - 1 + shots.size) % shots.size)
                    else -> Unit
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

// a README's text as plain lines: no heading marks or bold marks, and list dashes as bullets
internal fun readmeText(markdown: String): String =
    markdown.lines()
        .filterNot { it.trimStart().startsWith("# ") }
        .joinToString("\n") { line ->
            val plain = line.replace("**", "").replace(Regex("^#{2,6} "), "")
            if (plain.trimStart().startsWith("- ")) plain.replaceFirst("- ", "\u2022 ") else plain
        }
        .trim()
