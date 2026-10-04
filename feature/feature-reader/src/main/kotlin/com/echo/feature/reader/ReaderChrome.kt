package com.echo.feature.reader

import kotlin.math.roundToInt
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.echo.core.domain.model.GamepadAction
import com.echo.core.ui.components.ControllerHintStyle
import com.echo.core.ui.components.ControllerPromptItem
import com.echo.core.ui.components.EchoControllerHints
import com.echo.core.ui.design.DesignUnits
import com.echo.core.ui.design.MediaDesignFrame
import com.echo.core.ui.design.mediaGlow
import com.echo.core.ui.theme.menuCursorEdge
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date

@Composable
fun ReaderChrome(ui: ReaderUiState, onCommand: (ReaderCommand) -> Unit) {
    val accent = menuCursorEdge()
    MediaDesignFrame { u ->
        when {
            ui.error != null -> Message(ui.error!!, u)
            ui.loading -> Message("Opening ${ui.title}…", u)
            ui.optionsOpen -> Options(ui, accent, u, onCommand)
            else -> Reading(ui, accent, u)
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxWithConstraintsScope.Message(text: String, u: DesignUnits) {
    Box(Modifier.fillMaxSize().background(Color(0xFF141110)), contentAlignment = Alignment.Center) {
        Text(text, color = Color.White.copy(alpha = 0.8f), fontSize = u.sp(18))
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxWithConstraintsScope.Reading(ui: ReaderUiState, accent: Color, u: DesignUnits) {
    val ink = Color(ui.display.page.text)
    val muted = ink.copy(alpha = 0.5f)
    val href = ui.locator?.href?.toString().orEmpty()
    val chapterIndex = currentChapterIndex(ui.chapters, ui.readingOrder, href)
    val chapter = ui.chapters.getOrNull(chapterIndex)

    Row(
        Modifier.align(Alignment.TopCenter).fillMaxWidth().height(u.dp(48)).padding(horizontal = u.dp(48)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(ui.title, color = muted, fontSize = u.sp(13), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Text(
            chapter?.title?.uppercase().orEmpty(),
            style = TextStyle(color = muted, fontSize = u.sp(11), fontWeight = FontWeight.SemiBold, letterSpacing = androidx.compose.ui.unit.TextUnit(0.28f, androidx.compose.ui.unit.TextUnitType.Em)),
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Text(clock(), color = muted, fontSize = u.sp(13), modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
    }

    val progress = (ui.locator?.locations?.totalProgression ?: 0.0).toFloat().coerceIn(0f, 1f)
    val ticks = chapterStarts(ui.chapters, ui.positions)
    val left = chapterShareLeft(ui.chapters, ui.readingOrder, ui.positions, href, ui.locator?.locations?.totalProgression)
    Row(
        Modifier.align(Alignment.BottomStart).padding(start = u.dp(48), bottom = u.dp(22)).width(u.dp(620)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(u.dp(14)),
    ) {
        Box(
            Modifier.weight(1f).height(u.dp(8)).drawBehind {
                val y = size.height / 2
                drawLine(ink.copy(alpha = 0.14f), Offset(0f, y), Offset(size.width, y), u.dp(3).toPx(), StrokeCap.Round)
                drawLine(accent, Offset(0f, y), Offset(size.width * progress, y), u.dp(3).toPx(), StrokeCap.Round)
                ticks.forEach { t ->
                    val x = size.width * t.toFloat()
                    drawLine(ink.copy(alpha = 0.3f), Offset(x, 0f), Offset(x, size.height), 1f)
                }
            },
        )
        val status = listOfNotNull("${(progress * 100).toInt()}%", left?.let { "${(it * 100).roundToInt()}% of chapter left" })
        Text(status.joinToString("  ·  "), color = ink.copy(alpha = 0.55f), fontSize = u.sp(12), maxLines = 1)
    }

    EchoControllerHints(
        items = listOfNotNull(
            ControllerPromptItem(listOf(GamepadAction.NAVIGATE_LEFT, GamepadAction.NAVIGATE_RIGHT), "Page"),
            ControllerPromptItem(listOf(GamepadAction.PREV_CATEGORY, GamepadAction.NEXT_CATEGORY), "Chapter").takeIf { ui.chapters.isNotEmpty() },
            ControllerPromptItem(GamepadAction.OPEN_CONTEXT_MENU, "Options"),
            ControllerPromptItem(GamepadAction.BACK, "Close"),
        ),
        style = ControllerHintStyle.OVERLAY,
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = u.dp(16), bottom = u.dp(12))
            .clip(RoundedCornerShape(u.dp(999)))
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = u.dp(16), vertical = u.dp(8)),
    )
}

@Composable
private fun androidx.compose.foundation.layout.BoxWithConstraintsScope.Options(
    ui: ReaderUiState,
    accent: Color,
    u: DesignUnits,
    onCommand: (ReaderCommand) -> Unit,
) {
    Box(Modifier.fillMaxSize().background(Brush.radialGradient(
        listOf(mediaGlow(accent, 0.14f), Color(0xFF140E0D), Color(0xFF070505)),
        center = Offset(constraints.maxWidth * 0.72f, constraints.maxHeight * 0.28f),
        radius = constraints.maxWidth * 0.9f,
    )))
    val percent = ((ui.locator?.locations?.totalProgression ?: 0.0) * 100).toInt()
    Column(Modifier.offset(u.dp(64), u.dp(22))) {
        Text(ui.title, color = Color.White, fontSize = u.sp(48), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(listOfNotNull(ui.author, "$percent% read").joinToString("  ·  "), color = Color.White.copy(alpha = 0.55f), fontSize = u.sp(13))
    }

    Column(Modifier.offset(u.dp(80), u.dp(150)), verticalArrangement = Arrangement.spacedBy(u.dp(26))) {
        OptionsTab.entries.forEach { tab ->
            val on = tab == ui.tab
            Text(
                tab.label,
                color = if (on) Color.White else Color.White.copy(alpha = 0.5f),
                fontSize = u.sp(20), fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.clickable { onCommand(ReaderCommand.SwitchTab(tab.ordinal - ui.tab.ordinal)) },
            )
        }
    }

    Column(Modifier.offset(u.dp(380), u.dp(128)).width(u.dp(470)).fillMaxHeight().padding(bottom = u.dp(190))) {
        val label = when (ui.tab) {
            OptionsTab.CONTENTS -> "CHAPTERS"
            OptionsTab.BOOKMARKS -> "BOOKMARKS"
            OptionsTab.DISPLAY -> "DISPLAY"
        }
        Text(label, style = eyebrow(u))
        Spacer(Modifier.height(u.dp(14)))
        val rows: List<Pair<String, String>> = when (ui.tab) {
            OptionsTab.CONTENTS -> {
                val current = currentChapterIndex(ui.chapters, ui.readingOrder, ui.locator?.href?.toString().orEmpty())
                ui.chapters.mapIndexed { i, c ->
                    ("  ".repeat(c.depth) + c.title) to if (i == current) "Reading" else ""
                }
            }
            OptionsTab.BOOKMARKS -> ui.bookmarks.map { it.label to "" }.ifEmpty { listOf("No bookmarks yet. Press X while reading." to "") }
            OptionsTab.DISPLAY -> if (ui.isFixedLayout) listOf("Display settings apply to EPUB books." to "") else displayRows(ui.display)
        }
        val listState = rememberLazyListState()
        LaunchedEffect(ui.cursor, ui.tab) { if (rows.isNotEmpty()) listState.animateScrollToItem((ui.cursor - 3).coerceAtLeast(0)) }
        LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(u.dp(2))) {
            itemsIndexed(rows) { i, (title, trailing) ->
                val focused = i == ui.cursor
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(u.dp(12)))
                        .then(if (focused) Modifier.background(Color.White.copy(alpha = 0.07f)).border(u.dp(2), accent, RoundedCornerShape(u.dp(12))) else Modifier)
                        .clickable { onCommand(ReaderCommand.MoveCursor(i - ui.cursor)); onCommand(ReaderCommand.Activate) }
                        .padding(horizontal = u.dp(16), vertical = u.dp(11)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(title, color = if (focused) Color.White else Color.White.copy(alpha = 0.75f), fontSize = u.sp(17),
                        fontWeight = if (focused) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f))
                    if (trailing.isNotEmpty()) Text(trailing, color = accent, fontSize = u.sp(13))
                }
            }
        }
    }

    if (!ui.isFixedLayout) QuickDisplay(ui.display, accent, u, Modifier.offset(u.dp(900), u.dp(128)).width(u.dp(316)))

    EchoControllerHints(
        items = listOf(
            ControllerPromptItem(GamepadAction.SELECT, if (ui.tab == OptionsTab.DISPLAY) "Change" else "Go to"),
            ControllerPromptItem(listOf(GamepadAction.PREV_CATEGORY, GamepadAction.NEXT_CATEGORY), "Tab"),
            ControllerPromptItem(GamepadAction.CHANGE_SORT, "Bookmark"),
            ControllerPromptItem(GamepadAction.BACK, "Back"),
        ),
        style = ControllerHintStyle.OVERLAY,
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = u.dp(16), bottom = u.dp(14))
            .clip(RoundedCornerShape(u.dp(999)))
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = u.dp(16), vertical = u.dp(8)),
    )
}

@Composable
private fun QuickDisplay(d: ReaderDisplay, accent: Color, u: DesignUnits, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(u.dp(14)))
            .background(Color.White.copy(alpha = 0.06f))
            .border(u.dp(1), Color.White.copy(alpha = 0.08f), RoundedCornerShape(u.dp(14)))
            .padding(u.dp(20)),
        verticalArrangement = Arrangement.spacedBy(u.dp(18)),
    ) {
        Text("QUICK DISPLAY", style = eyebrow(u))
        Column(verticalArrangement = Arrangement.spacedBy(u.dp(10))) {
            Text("Text size", color = Color.White.copy(alpha = 0.7f), fontSize = u.sp(14))
            val f = (d.textScale - ReaderDisplay.TEXT_SCALE_MIN) / (ReaderDisplay.TEXT_SCALE_MAX - ReaderDisplay.TEXT_SCALE_MIN)
            Box(Modifier.fillMaxWidth().height(u.dp(14)).drawBehind {
                val y = size.height / 2
                drawLine(Color.White.copy(alpha = 0.18f), Offset(0f, y), Offset(size.width, y), u.dp(4).toPx(), StrokeCap.Round)
                drawLine(accent, Offset(0f, y), Offset(size.width * f, y), u.dp(4).toPx(), StrokeCap.Round)
                drawCircle(Color.White, u.dp(7).toPx(), Offset(size.width * f, y))
            })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(u.dp(6))) {
            ReaderTypeface.entries.forEach { t ->
                Text(t.label, color = if (t == d.typeface) Color.White else Color.White.copy(alpha = 0.6f), fontSize = u.sp(15),
                    modifier = Modifier.clip(RoundedCornerShape(u.dp(7))).background(if (t == d.typeface) Color.White.copy(alpha = 0.14f) else Color.Transparent)
                        .padding(horizontal = u.dp(18), vertical = u.dp(8)))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(u.dp(12))) {
            ReaderPage.entries.forEach { p ->
                Box(Modifier.size(u.dp(52), u.dp(36)).clip(RoundedCornerShape(u.dp(8))).background(Color(p.background))
                    .border(if (p == d.page) u.dp(2) else u.dp(1), if (p == d.page) accent else Color.White.copy(alpha = 0.15f), RoundedCornerShape(u.dp(8))))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Layout", color = Color.White.copy(alpha = 0.7f), fontSize = u.sp(14))
            Text(d.layout.label, color = accent, fontSize = u.sp(14))
        }
    }
}

private fun displayRows(d: ReaderDisplay): List<Pair<String, String>> = DisplayRow.entries.map { row ->
    when (row) {
        DisplayRow.TEXT_SIZE -> "Text size" to "${(d.textScale * 100).toInt()}%"
        DisplayRow.TYPEFACE -> "Typeface" to d.typeface.label
        DisplayRow.PAGE -> "Page" to d.page.label
        DisplayRow.LAYOUT -> "Layout" to d.layout.label
    }
}

private fun eyebrow(u: DesignUnits) = u.eyebrow()

@Composable
private fun clock(): String {
    val now by produceState(System.currentTimeMillis()) {
        while (true) { delay(30_000); value = System.currentTimeMillis() }
    }
    return DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(now))
}
