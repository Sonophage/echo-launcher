package com.echo.feature.crossbar.viewmodel

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.echo.core.data.datastore.echoDataStore
import com.echo.core.data.repository.CategoryRepositoryImpl
import com.echo.core.data.repository.MemoryCardRepository
import com.echo.core.domain.model.BuiltInCategory
import com.echo.core.domain.model.Category
import com.echo.core.domain.model.GamepadAction
import com.echo.core.ui.components.MenuGroup
import com.echo.core.ui.sound.MenuSound
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// owner, 2026-10-07: Y ▸ Move lifts a row or a column; the d-pad moves it live, A drops it where it is,
// B puts everything back. It replaces Move Up and Move Down in the menus and the order controls in Settings.
data class MoveSession(
    val column: Boolean,
    val title: String,
    val items: List<CrossbarItem>,
    val categories: List<Category>,
    val itemIndex: Int,
    val categoryIndex: Int,
)

internal const val MOVE_ROW = "move_row"
internal const val MOVE_COLUMN = "move_column"
internal const val RENAME_COLUMN = "rename_column"
internal const val CHANGE_COLUMN_ICON = "change_column_icon"
internal const val CHANGE_SYSTEM_ICON = "change_system_icon"
internal const val MENU_WAIT_MS = 400L

private const val PINNED_SYSTEMS = "pinned"
private const val SYSTEMS = "systems"
private const val COLUMN_ROWS = "column"

// where a lifted row lands one step up (-1) or down (+1): only beside a row of its own group, so a system
// never passes All Games or a pinned system, and an app never passes the Add row
internal fun liftedRowStep(groups: List<String?>, at: Int, delta: Int): Int? {
    val group = groups.getOrNull(at) ?: return null
    val to = at + delta
    return to.takeIf { groups.getOrNull(to) == group }
}

// where a lifted column lands: the next column the owner can reach that way, past any he cannot
internal fun liftedColumnStep(reachable: List<Boolean>, at: Int, delta: Int): Int? {
    var to = at + delta
    while (to in reachable.indices) {
        if (reachable[to]) return to
        to += delta
    }
    return null
}

// the Custom Icons group and slot to open on; the first slot when the key is unknown or its group is not offered
internal fun customIconStart(slotKey: String?, groups: List<com.echo.themekit.IconSlot.Group>): Pair<Int, Int> {
    val slot = com.echo.themekit.CustomizableIcons.ALL.firstOrNull { it.key == slotKey } ?: return 0 to 0
    val group = groups.indexOf(slot.group).takeIf { it >= 0 } ?: return 0 to 0
    return group to com.echo.themekit.CustomizableIcons.group(slot.group).indexOfFirst { it.key == slot.key }
}

internal fun <T> List<T>.swapped(a: Int, b: Int): List<T> =
    toMutableList().apply { this[a] = this[b].also { this[b] = this[a] } }

// the group each row of the open column moves within, or null for a row that stays put
internal fun CrossbarUiState.rowMoveGroups(pinnedSystems: Set<String>): List<String?> {
    val inGames = categories.getOrNull(selectedCategoryIndex)?.id == BuiltInCategory.GAMES && !isInSubItem
    val ordered = columnOrderKey() != null
    return currentItems.map { item ->
        when {
            inGames && item.type == CrossbarItemType.MEMORY_CARD && item.platformId != null ->
                if (item.platformId in pinnedSystems) PINNED_SYSTEMS else SYSTEMS
            ordered && item.movableInColumn -> COLUMN_ROWS
            else -> null
        }
    }
}

// the open column can be renamed and re-iconed from its rows; it can move when there is somewhere to go
internal fun CrossbarUiState.columnEditable(): Boolean {
    val current = categories.getOrNull(selectedCategoryIndex) ?: return false
    return !isInSubItem && categoryReachable(current)
}

internal fun CrossbarUiState.columnMovable(): Boolean =
    columnEditable() && categories.count { categoryReachable(it) } > 1

// the Custom Icons slot a column draws, or null when its icon is not one a theme can replace
internal fun CrossbarUiState.columnIconSlot(): String? =
    categories.getOrNull(selectedCategoryIndex)?.let { com.echo.core.ui.icons.catbarSlotKeyFor(it.iconKey) }

// a system's console icon slot, or null for a system with no console icon
internal fun systemIconSlot(item: CrossbarItem): String? =
    item.platformId?.takeIf { item.type == CrossbarItemType.MEMORY_CARD && it in com.echo.themekit.SYSICON_PLATFORM_IDS }
        ?.let { "sysicon_$it" }

class CrossbarMove(
    private val vm: CrossbarViewModel,
    private val uiState: MutableStateFlow<CrossbarUiState>,
    private val scope: CoroutineScope,
    private val context: Context,
    private val categoryRepository: CategoryRepositoryImpl,
    private val memoryCardRepository: MemoryCardRepository,
    private val menuSound: com.echo.core.ui.sound.MenuSoundPlayer,
) {
    private fun pinnedSystems(): Set<String> = vm.enabledCards.filter { it.pinned }.map { it.platformId }.toSet()

    // the edit rows for the focused row of the crossbar (owner, 2026-10-07): Move and Change Icon for the row
    // when it can, then Move Column, Rename Column and Change Column Icon for its column
    fun menuRows(state: CrossbarUiState, item: CrossbarItem): List<CrossbarContextMenuItem> = buildList {
        if (state.currentItems.getOrNull(state.selectedItemIndex)?.id != item.id) return@buildList
        fun row(id: String, label: String) =
            CrossbarContextMenuItem(id, label, group = MenuGroup.CATEGORY, pinnedToRoot = true, confirms = false)
        if (state.rowMoveGroups(pinnedSystems()).getOrNull(state.selectedItemIndex) != null) add(row(MOVE_ROW, "Move"))
        if (systemIconSlot(item) != null && !state.isInSubItem) add(row(CHANGE_SYSTEM_ICON, "Change Icon"))
        if (state.columnMovable()) add(row(MOVE_COLUMN, "Move Column"))
        if (state.columnEditable()) add(row(RENAME_COLUMN, "Rename Column"))
        if (state.columnEditable() && state.columnIconSlot() != null) add(row(CHANGE_COLUMN_ICON, "Change Column Icon"))
    }

    fun promptRenameColumn() {
        val s = uiState.value
        val column = s.categories.getOrNull(s.selectedCategoryIndex) ?: return
        uiState.update { it.copy(collectionNameDialog = CollectionNameDialogState(
            title = "Rename Column",
            subtitle = "The name under this column's icon.",
            initialText = column.name,
            renameCategoryId = column.id,
            placeholder = column.name,
        ))}
    }

    fun renameColumn(id: String, name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        scope.launch { categoryRepository.rename(id, trimmed) }
    }

    fun lift(column: Boolean) {
        val s = uiState.value
        val title = if (column) s.categories.getOrNull(s.selectedCategoryIndex)?.name else s.currentItems.getOrNull(s.selectedItemIndex)?.title
        menuSound.play(MenuSound.SELECT)
        uiState.update {
            it.copy(moving = MoveSession(column, title.orEmpty(), s.currentItems, s.categories, s.selectedItemIndex, s.selectedCategoryIndex))
        }
    }

    fun onButton(action: GamepadAction, session: MoveSession) {
        when (action) {
            GamepadAction.NAVIGATE_UP -> if (!session.column) stepRow(-1)
            GamepadAction.NAVIGATE_DOWN -> if (!session.column) stepRow(+1)
            GamepadAction.NAVIGATE_LEFT -> if (session.column) stepColumn(-1)
            GamepadAction.NAVIGATE_RIGHT -> if (session.column) stepColumn(+1)
            GamepadAction.SELECT -> drop(session)
            GamepadAction.BACK -> cancel()
            else -> Unit
        }
    }

    private fun stepRow(delta: Int) {
        val s = uiState.value
        val to = liftedRowStep(s.rowMoveGroups(pinnedSystems()), s.selectedItemIndex, delta) ?: return
        menuSound.play(MenuSound.SCROLL)
        uiState.update { it.copy(currentItems = it.currentItems.swapped(it.selectedItemIndex, to), selectedItemIndex = to) }
    }

    private fun stepColumn(delta: Int) {
        val s = uiState.value
        val to = liftedColumnStep(s.categories.map { s.categoryReachable(it) }, s.selectedCategoryIndex, delta) ?: return
        menuSound.play(MenuSound.SYSTEM_BROWSE)
        uiState.update { it.copy(categories = it.categories.swapped(it.selectedCategoryIndex, to), selectedCategoryIndex = to) }
    }

    // ponytail: a column reload that lands mid-move (a scan finishing) puts the rows back under the lifted
    // one; the drop then saves that order. Hold reloads while moving if that is ever seen.
    private fun drop(session: MoveSession) {
        val s = uiState.value
        menuSound.play(MenuSound.SELECT)
        uiState.update { it.copy(moving = null) }
        scope.launch {
            if (session.column) {
                categoryRepository.reorder(s.categories.map { it.id })
                return@launch
            }
            val groups = s.rowMoveGroups(pinnedSystems())
            val moved = s.currentItems.filterIndexed { i, _ -> groups[i] != null }
            if (groups.any { it == SYSTEMS || it == PINNED_SYSTEMS }) {
                memoryCardRepository.reorder(moved.mapNotNull { it.platformId })
            } else {
                val key = s.columnOrderKey() ?: return@launch
                context.echoDataStore.edit { it[stringPreferencesKey(COLUMN_ORDER_PREFIX + key)] = moved.joinToString("\n") { it.id } }
            }
        }
    }

    fun cancel() {
        val session = uiState.value.moving ?: return
        menuSound.play(MenuSound.BACK)
        uiState.update {
            it.copy(
                moving = null,
                currentItems = session.items,
                categories = session.categories,
                selectedItemIndex = session.itemIndex,
                selectedCategoryIndex = session.categoryIndex,
            )
        }
    }
}
