package com.echo.feature.crossbar.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.echo.core.domain.model.GamepadAction
import com.echo.core.ui.components.ControllerPromptItem
import com.echo.core.ui.components.EchoContextMenuOverlay
import com.echo.core.ui.components.EchoHintBar
import com.echo.core.ui.components.HintAction
import com.echo.core.ui.components.MenuRow
import com.echo.core.ui.components.MenuState
import com.echo.core.ui.icons.CustomIcon
import com.echo.core.ui.icons.CustomIconSurface
import com.echo.core.ui.icons.LocalIconAnimating
import com.echo.feature.crossbar.viewmodel.CustomIconSession
import com.echo.themekit.CustomizableIcons
import com.echo.themekit.IconSlot

// Custom Icons on the kit's side rail (owner, 2026-10-07: it must look like the rest of ECHO), as the
// Colour Scheme picker is: one row per slot with its current icon as the badge, the crossbar live behind,
// LT/RT for the group. The row after the last slot is Reset All Icons.
@Composable
fun CustomIconsOverlay(
    session: CustomIconSession,
    customIcons: Map<String, CustomIcon>,
    themeIcons: Map<String, CustomIcon>,
    onSlotFocused: (Int) -> Unit,
    onIconPicked: (String, android.net.Uri) -> Unit,
    onResetSlot: (String) -> Unit,
    onResetAll: () -> Unit,
    onSaveAsTheme: () -> Unit,
    onGroupMove: (Int) -> Unit,
    onSlotMove: (Int) -> Unit,
    onDone: () -> Unit,
    forwardedAction: GamepadAction? = null,
    onActionConsumed: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        session.focusedSlot?.let { slot -> uri?.let { onIconPicked(slot.key, it) } }
    }
    val slots = remember(session.groupIndex) { CustomizableIcons.group(session.group) }
    val onResetAllRow = session.slotIndex == slots.size
    val select = { if (onResetAllRow) onResetAll() else picker.launch(PICK_MIME) }

    LaunchedEffect(forwardedAction) {
        when (forwardedAction) {
            GamepadAction.SELECT -> select()
            GamepadAction.OPEN_CONTEXT_MENU -> session.focusedSlot?.let { onResetSlot(it.key) }
            GamepadAction.CHANGE_SORT -> onSaveAsTheme()
            GamepadAction.BACK -> onDone()
            else -> Unit
        }
        if (forwardedAction != null) onActionConsumed()
    }

    val focused = session.focusedSlot
    val state = when {
        focused == null -> null
        customIcons.containsKey(focused.key) -> "Yours"
        themeIcons.containsKey(focused.key) -> "From the theme"
        else -> "Default"
    }
    val menu = MenuState(
        title = groupLabel(session.group),
        subtitle = session.message ?: focused?.let { "${it.displayName} · $state" } ?: "Every icon back to the theme's",
        rows = slots.mapIndexed { index, slot -> MenuRow(index, slot.displayName) } +
            MenuRow(slots.size, "Reset All Icons", isDestructive = true),
        selectedIndex = session.slotIndex,
    )

    Box(modifier.fillMaxSize()) {
        EchoContextMenuOverlay(
            state = menu,
            onRowActivated = { index -> if (index == session.slotIndex) select() else onSlotMove(index) },
            onDismiss = onDone,
            rowBadge = { index, isFocused ->
                val slot = slots.getOrNull(index) ?: return@EchoContextMenuOverlay
                // the glyphs are drawn white, so on the focused (white) row they sit on a dark chip
                Box(
                    Modifier
                        .background(if (isFocused) FocusedBadgeBacking else Color.Transparent, RoundedCornerShape(8.dp))
                        .padding(3.dp),
                ) {
                    CompositionLocalProvider(LocalIconAnimating provides isFocused) {
                        SlotPreview(slot, customIcons[slot.key] ?: themeIcons[slot.key], Modifier.size(BadgeSize))
                    }
                }
            },
        )
        EchoHintBar(
            items = listOf(
                ControllerPromptItem(GamepadAction.BACK, "Done"),
                ControllerPromptItem(listOf(GamepadAction.PREV_CATEGORY, GamepadAction.NEXT_CATEGORY), "Group"),
                ControllerPromptItem(GamepadAction.OPEN_CONTEXT_MENU, "Reset"),
                ControllerPromptItem(GamepadAction.CHANGE_SORT, "Save as Theme"),
            ),
            primary = HintAction(GamepadAction.SELECT, if (onResetAllRow) "Reset All" else "Choose Image"),
            onAction = { action ->
                when (action) {
                    GamepadAction.SELECT -> select()
                    GamepadAction.OPEN_CONTEXT_MENU -> focused?.let { onResetSlot(it.key) }
                    GamepadAction.CHANGE_SORT -> onSaveAsTheme()
                    GamepadAction.BACK -> onDone()
                    else -> Unit
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

private val BadgeSize = 26.dp
private val FocusedBadgeBacking = Color(0xE6000000)

private val PICK_MIME = arrayOf(
    "image/png",
    "image/jpeg",
    "image/webp",
    "image/gif",
    "image/bmp",
    "image/heif",
)

private fun groupLabel(group: IconSlot.Group): String = when (group) {
    IconSlot.Group.CATEGORY_BAR -> "Category Bar"
    IconSlot.Group.ITEMS -> "Items"
    IconSlot.Group.STATUS -> "Status"
    IconSlot.Group.CONSOLE -> "Consoles"
}

@Composable
private fun SlotPreview(slot: IconSlot, icon: CustomIcon?, modifier: Modifier = Modifier) {
    if (icon != null) {
        CustomIconSurface(icon = icon, contentDescription = slot.displayName, modifier = modifier)
        return
    }
    if (DefaultSlotGlyph(slot = slot, contentDescription = slot.displayName, modifier = modifier)) return

    Box(
        modifier = modifier
            .border(1.dp, Color(0x66B9C6DC), RoundedCornerShape(6.dp))
            .padding(2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = slot.displayName.take(1), color = Color(0xFFB9C6DC), fontSize = 18.sp)
    }
}
