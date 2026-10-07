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
import com.echo.core.domain.model.GamepadAction
import com.echo.core.ui.components.ControllerPromptItem
import com.echo.core.ui.components.EchoHintBar
import com.echo.core.ui.components.HintAction
import com.echo.core.ui.design.PanelBase
import com.echo.core.ui.design.panelDesignUnits
import com.echo.core.ui.theme.EchoTextStyle
import com.echo.feature.settings.viewmodel.ThemePage
import com.echo.feature.settings.viewmodel.ThemesSettingsViewModel
import com.echo.themekit.ThemePart

// the theme store (owner, 2026-10-07): each saved theme as a card headed by its hero picture
@Composable
internal fun ThemeStoreCardRow(
    themes: List<EchoThemeStore.SavedTheme>,
    focusedIndex: Int?,
    onOpen: (String) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 48.dp, vertical = 10.dp),
    ) {
        themes.forEachIndexed { index, theme ->
            val focused = focusedIndex == index
            Column(modifier = Modifier.width(240.dp).clickable { onOpen(theme.id) }) {
                Box(
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(10.dp))
                        .background(Color(theme.accentArgb?.let { it and 0xFFFFFFFFL } ?: 0xFF20304AL))
                        .border(if (focused) 3.dp else 1.dp, if (focused) SettingsAccent else Color(0x55FFFFFF), RoundedCornerShape(10.dp)),
                ) {
                    (theme.heroPath ?: theme.previewPath)?.let { path ->
                        AsyncImage(model = path, contentDescription = theme.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    }
                }
                Text(theme.name, color = if (focused) SettingsAccent else Color.White, fontSize = 14.sp, maxLines = 1,
                    overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
                Text(theme.author?.let { "by $it" } ?: "${theme.parts.size} parts", color = SettingsSubtext, fontSize = 12.sp, maxLines = 1)
            }
        }
    }
}

// a theme's store page: its hero, metadata, the parts it has, its screenshots and its README. LEFT and RIGHT
// step the screenshots; A applies the whole theme
@Composable
internal fun ThemePageOverlay(
    page: ThemePage,
    shot: Int,
    onApply: () -> Unit,
    onBack: () -> Unit,
    onShot: (Int) -> Unit,
) {
    val theme = page.theme
    val readme = page.details?.readme
    val shots = page.details?.screenshotPaths.orEmpty()
    BoxWithConstraints(Modifier.fillMaxSize().background(PanelBase).clickable(enabled = false) {}) {
        val u = panelDesignUnits(maxWidth.value, maxHeight.value, LocalDensity.current)
        Box(Modifier.fillMaxWidth().fillMaxHeight(0.62f)) {
            (theme.heroPath ?: theme.previewPath)?.let {
                AsyncImage(model = it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color.Transparent, 0.3f to PanelBase.copy(alpha = 0.6f), 0.7f to PanelBase.copy(alpha = 0.95f), 1f to PanelBase)))
        }
        Row(Modifier.fillMaxSize().padding(start = u.dp(64), end = u.dp(48), top = u.dp(220), bottom = u.dp(84))) {
            Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())) {
                Text("THEME", style = u.eyebrow())
                Text(theme.name, style = EchoTextStyle.copy(color = Color.White, fontSize = u.sp(40), fontWeight = FontWeight.SemiBold))
                listOfNotNull(theme.author?.let { "by $it" }, theme.version?.let { "version $it" }).takeIf { it.isNotEmpty() }?.let {
                    Text(it.joinToString("  ·  "), style = EchoTextStyle.copy(color = SettingsSubtext, fontSize = u.sp(15)))
                }
                (readme?.description ?: theme.description)?.let {
                    Text(it, style = EchoTextStyle.copy(color = Color.White, fontSize = u.sp(17)), modifier = Modifier.padding(top = u.dp(14)))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(u.dp(8)), modifier = Modifier.padding(top = u.dp(16))) {
                    ThemePart.entries.filter { it in theme.parts }.forEach { part ->
                        Text(part.label, style = EchoTextStyle.copy(color = Color.White, fontSize = u.sp(12)),
                            modifier = Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.12f)).padding(horizontal = u.dp(12), vertical = u.dp(5)))
                    }
                }
                readme?.body?.takeIf { it.isNotBlank() }?.let {
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
            primary = HintAction(GamepadAction.SELECT, "Apply"),
            onAction = { action ->
                when (action) {
                    GamepadAction.BACK -> onBack()
                    GamepadAction.SELECT -> onApply()
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
