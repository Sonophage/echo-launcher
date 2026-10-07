package com.echo.feature.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import com.echo.core.ui.components.EchoContextMenuOverlay
import com.echo.core.ui.components.HintAction
import com.echo.core.ui.design.PanelBase
import com.echo.core.ui.design.panelDesignUnits
import com.echo.core.ui.theme.EchoTextStyle
import com.echo.feature.settings.viewmodel.ThemePage
import com.echo.feature.settings.viewmodel.ThemesSettingsViewModel
import com.echo.themekit.ThemePart

// a card in the theme store: a saved theme or an online one
internal data class StoreCard(val id: String, val name: String, val subtitle: String, val image: String?, val accentArgb: Long? = null)

internal fun EchoThemeStore.SavedTheme.card() =
    StoreCard(id, name, author?.let { "by $it" } ?: "${parts.size} parts", heroPath ?: previewPath, accentArgb)

// one row of the store's grid: [columns] cards across, the last row padded so cards keep their width
@Composable
private fun StoreCardRow(cards: List<StoreCard>, columns: Int, focusedIndex: Int?, onOpen: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(18.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 48.dp, vertical = 10.dp)) {
        cards.forEachIndexed { index, card ->
            val focused = focusedIndex == index
            Column(modifier = Modifier.weight(1f).clickable { onOpen(card.id) }) {
                Box(
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(10.dp))
                        .background(Color(card.accentArgb?.let { it and 0xFFFFFFFFL } ?: 0xFF20304AL))
                        .border(if (focused) 3.dp else 1.dp, if (focused) SettingsAccent else Color(0x55FFFFFF), RoundedCornerShape(10.dp)),
                ) {
                    card.image?.let { AsyncImage(model = it, contentDescription = card.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
                }
                Text(card.name, color = if (focused) SettingsAccent else Color.White, fontSize = 15.sp, maxLines = 1,
                    overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
                Text(card.subtitle, color = SettingsSubtext, fontSize = 12.sp, maxLines = 1)
            }
        }
        repeat(columns - cards.size) { Spacer(Modifier.weight(1f)) }
    }
}

// the store's tab under Look (owner, 2026-10-07: its own tab, a full panel): saved themes and the online
// store's, each as a grid of hero cards. A card opens the theme's page; each grid row is one stop for the
// controller, LEFT and RIGHT move along it
@Composable
fun ThemeStoreScreen(
    onBack: () -> Unit,
    onOpenMix: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ThemesSettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var shot by remember { mutableIntStateOf(0) }
    // the grid row the controller is on (section to row) and the column within it
    var focusedRow by remember { mutableStateOf<Pair<String, Int>?>(null) }
    var column by remember { mutableIntStateOf(0) }
    // Y on a saved theme: Apply, Share, Remove
    var menu by remember { mutableStateOf<ThemeMenu?>(null) }
    var menuIndex by remember { mutableIntStateOf(0) }
    val columns = 4
    val saved = state.savedThemes.map { it.card() }
    val online = state.online.orEmpty().map { t ->
        val standing = ThemeCatalogRepository.standing(state.savedThemes.firstOrNull { it.name == t.name }, t)
        StoreCard(t.id, t.name, when (standing) {
            ThemeCatalogRepository.Standing.NEW -> "Online"
            ThemeCatalogRepository.Standing.CURRENT -> "Downloaded"
            ThemeCatalogRepository.Standing.UPDATE -> "Update"
        }, t.heroUrl)
    }
    fun rowOf(section: String) = if (section == "saved") saved else online
    fun open(section: String, id: String) {
        shot = 0
        if (section == "saved") viewModel.openThemePage(id) else viewModel.openOnlinePage(id)
    }
    fun openMenu(theme: EchoThemeStore.SavedTheme) {
        menuIndex = 0
        menu = ThemeMenu(theme.name, listOf(
            ThemeMenuOption("Apply") { viewModel.applySavedTheme(theme.id) },
            ThemeMenuOption("Share") { viewModel.shareSavedTheme(theme.id) },
            ThemeMenuOption("Remove", destructive = true) { viewModel.deleteSavedTheme(theme.id) },
        ))
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
                val row = focusedRow
                val m = menu
                when {
                    m != null -> {
                        when (action) {
                            GamepadAction.NAVIGATE_UP -> menuIndex = (menuIndex - 1).coerceAtLeast(0)
                            GamepadAction.NAVIGATE_DOWN -> menuIndex = (menuIndex + 1).coerceAtMost(m.options.size - 1)
                            GamepadAction.SELECT -> { m.options.getOrNull(menuIndex)?.action?.invoke(); menu = null }
                            GamepadAction.BACK, GamepadAction.OPEN_CONTEXT_MENU -> menu = null
                            else -> Unit
                        }
                        true
                    }
                    page != null -> {
                        val shots = page.screenshots.size
                        when (action) {
                            GamepadAction.BACK -> viewModel.closeThemePage()
                            GamepadAction.SELECT -> viewModel.pageAction()
                            GamepadAction.NAVIGATE_RIGHT -> if (shots > 0) shot = (shot + 1) % shots
                            GamepadAction.NAVIGATE_LEFT -> if (shots > 0) shot = (shot - 1 + shots) % shots
                            else -> Unit
                        }
                        true
                    }
                    row != null && row.first == "saved" && action == GamepadAction.OPEN_CONTEXT_MENU -> {
                        state.savedThemes.getOrNull(row.second * columns + column)?.let(::openMenu); true
                    }
                    row != null && action == GamepadAction.NAVIGATE_LEFT -> { column = (column - 1).coerceAtLeast(0); true }
                    row != null && action == GamepadAction.NAVIGATE_RIGHT -> {
                        val size = rowOf(row.first).chunked(columns).getOrNull(row.second)?.size ?: 1
                        column = (column + 1).coerceAtMost(size - 1); true
                    }
                    else -> false
                }
            },
        ) {
            val scroll = rememberScrollState()
            LocalSettingsScrollStateRegistrar.current(scroll)
            Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
                @Composable
                fun grid(section: String, cards: List<StoreCard>) {
                    cards.chunked(columns).forEachIndexed { r, rowCards ->
                        FocusableStrip(
                            onFocusChange = { focused ->
                                if (focused) { focusedRow = section to r; column = column.coerceIn(0, rowCards.size - 1) }
                                else if (focusedRow == section to r) focusedRow = null
                            },
                            onSelect = { rowCards.getOrNull(column)?.let { open(section, it.id) } },
                        ) { stripFocused ->
                            StoreCardRow(rowCards, columns, if (stripFocused) column else null) { open(section, it) }
                        }
                    }
                }
                SettingsGroup("On this device")
                if (saved.isEmpty()) SettingsRow(label = "No saved themes yet", sublabel = "Download one below, or save your current look under Theme", onClick = null)
                else grid("saved", saved)
                SettingsRow(label = "Mix", sublabel = "Take the icons, wallpaper, sounds, wave and buttons from different themes", onClick = onOpenMix)

                SettingsGroup("Online")
                when {
                    online.isNotEmpty() -> grid("online", online)
                    state.onlineFailed -> SettingsRow(label = "The online store could not be reached", sublabel = "Check the connection, then try again", onClick = viewModel::refreshOnline)
                    state.online == null -> SettingsRow(label = "Loading the online store", sublabel = null, onClick = null)
                    else -> SettingsRow(label = "No themes online yet", sublabel = null, onClick = null)
                }
                state.installMessage?.let { SettingsRow(label = it, sublabel = "Tap to dismiss", onClick = viewModel::dismissMessage) }
            }
        }

        menu?.let { m ->
            EchoContextMenuOverlay(
                state = menuStateFor(m, menuIndex),
                onRowActivated = { index ->
                    (menuStateFor(m, menuIndex).chose(index) as? MenuSelect.Run)?.action?.action?.invoke()
                    menu = null
                },
                onDismiss = { menu = null },
            )
        }

        state.page?.let {
            ThemePageOverlay(
                page = it,
                shot = shot,
                onAction = viewModel::pageAction,
                onBack = viewModel::closeThemePage,
                onShot = { i -> shot = i },
            )
        }
    }
}

// a theme's store page: its hero, metadata, the parts it has, its screenshots and its README. LEFT and RIGHT
// step the screenshots; A applies a saved theme or downloads an online one
@Composable
internal fun ThemePageOverlay(
    page: ThemePage,
    shot: Int,
    onAction: () -> Unit,
    onBack: () -> Unit,
    onShot: (Int) -> Unit,
) {
    val shots = page.screenshots
    BoxWithConstraints(Modifier.fillMaxSize().background(PanelBase).clickable(enabled = false) {}) {
        val u = panelDesignUnits(maxWidth.value, maxHeight.value, LocalDensity.current)
        Box(Modifier.fillMaxWidth().fillMaxHeight(0.62f)) {
            page.hero?.let {
                AsyncImage(model = it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color.Transparent, 0.3f to PanelBase.copy(alpha = 0.6f), 0.7f to PanelBase.copy(alpha = 0.95f), 1f to PanelBase)))
        }
        Row(Modifier.fillMaxSize().padding(start = u.dp(64), end = u.dp(48), top = u.dp(220), bottom = u.dp(84))) {
            Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())) {
                Text("THEME", style = u.eyebrow())
                Text(page.name, style = EchoTextStyle.copy(color = Color.White, fontSize = u.sp(40), fontWeight = FontWeight.SemiBold))
                listOfNotNull(page.author?.let { "by $it" }, page.version?.let { "version $it" }).takeIf { it.isNotEmpty() }?.let {
                    Text(it.joinToString("  ·  "), style = EchoTextStyle.copy(color = SettingsSubtext, fontSize = u.sp(15)))
                }
                page.description?.let {
                    Text(it, style = EchoTextStyle.copy(color = Color.White, fontSize = u.sp(17)), modifier = Modifier.padding(top = u.dp(14)))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(u.dp(8)), modifier = Modifier.padding(top = u.dp(16))) {
                    ThemePart.entries.filter { it in page.parts }.forEach { part ->
                        Text(part.label, style = EchoTextStyle.copy(color = Color.White, fontSize = u.sp(12)),
                            modifier = Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.12f)).padding(horizontal = u.dp(12), vertical = u.dp(5)))
                    }
                }
                page.body?.takeIf { it.isNotBlank() }?.let {
                    Text(readmeText(it),
                        style = EchoTextStyle.copy(color = SettingsSubtext, fontSize = u.sp(14)), modifier = Modifier.padding(top = u.dp(18)))
                }
            }
            Spacer(Modifier.width(u.dp(40)))
            if (shots.isNotEmpty()) {
                val at = shot.coerceIn(0, shots.size - 1)
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    AsyncImage(
                        model = shots[at], contentDescription = "Screenshot ${at + 1} of ${shots.size}", contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(u.dp(12)))
                            .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(u.dp(12)))
                            .clickable { onShot((at + 1) % shots.size) },
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(u.dp(8)), modifier = Modifier.padding(top = u.dp(12))) {
                        shots.indices.forEach { i ->
                            Box(Modifier.width(u.dp(if (i == at) 22 else 8)).height(u.dp(8)).clip(RoundedCornerShape(50))
                                .background(Color.White.copy(alpha = if (i == at) 0.9f else 0.35f)).clickable { onShot(i) })
                        }
                    }
                }
            }
        }
        EchoHintBar(
            items = listOfNotNull(
                ControllerPromptItem(GamepadAction.BACK, "Back"),
                ControllerPromptItem(GamepadAction.NAVIGATE_RIGHT, "Screenshots").takeIf { shots.size > 1 },
            ),
            primary = HintAction(GamepadAction.SELECT, page.actionLabel),
            onAction = { action ->
                when (action) {
                    GamepadAction.BACK -> onBack()
                    GamepadAction.SELECT -> onAction()
                    GamepadAction.NAVIGATE_RIGHT -> if (shots.isNotEmpty()) onShot((shot + 1) % shots.size)
                    else -> Unit
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

// Mix (owner, 2026-10-07): each part of the look taken from any saved theme that has it
@Composable
fun ThemeMixScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ThemesSettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    SettingsPageScaffold(subtitle = "Mix", onBack = onBack, modifier = modifier) {
        val scroll = rememberScrollState()
        LocalSettingsScrollStateRegistrar.current(scroll)
        Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
            SettingsGroup("Take each part from any theme")
            ThemePart.entries.forEach { part ->
                val offering = state.savedThemes.filter { part in it.parts }
                val source = state.partSources[part]
                SettingsPickerRow(
                    label = part.label,
                    sublabel = when {
                        offering.isEmpty() -> "No saved theme has this part"
                        source != null -> "From $source"
                        else -> "Your own"
                    },
                    options = offering.map { SettingsPickerOption(it.name, it.author?.let { a -> "by $a" }) },
                    selectedIndex = offering.indexOfFirst { it.name == source },
                    onPick = { i -> offering.getOrNull(i)?.let { viewModel.applyPart(it.id, part) } },
                    enabled = offering.isNotEmpty(),
                    focusKey = "mix-${part.name}",
                )
            }
            state.installMessage?.let { SettingsRow(label = it, sublabel = "Tap to dismiss", onClick = viewModel::dismissMessage) }
        }
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
