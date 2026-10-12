package com.echo.core.ui.components

// The seven sections every menu is ordered in. Do and About sit at the top level; each other section is a
// submenu named by its label, unless it holds one row, which is then shown inline (owner, 2026-10-12)
enum class MenuGroup(val label: String?) {
    DO(null),
    ABOUT(null),
    ORGANIZE("Organize"),
    EDIT("Edit"),
    SETTINGS("Settings"),
    EDIT_LAYOUT("Edit Layout"),
    REMOVE("Remove"),
}

data class MenuRow<out T>(
    val action: T?,
    val label: String,
    val group: MenuGroup = MenuGroup.DO,
    val isDestructive: Boolean = false,
    val confirms: Boolean = isDestructive,
    val checked: Boolean = false,
    val hidden: Boolean = false,
    val opensSubmenu: Boolean = false,
)

// Favorites is one set over games, apps, videos and photos, with one pair of rows (owner, 2026-10-12)
fun favoritesLabel(isFavorite: Boolean): String =
    if (isFavorite) "Remove from Favorites" else "Add to Favorites"

data class MenuState<T>(
    val title: String,
    val rows: List<MenuRow<T>>,
    val subtitle: String? = null,
    val selectedIndex: Int? = null,
    val parent: MenuState<T>? = null,
    val withheld: Set<T> = emptySet(),
)

sealed interface MenuSelect<out T> {
    data class Run<T>(val action: T) : MenuSelect<T>

    data class Replace<T>(val state: MenuState<T>) : MenuSelect<T>

    data object None : MenuSelect<Nothing>
}

// a destructive row is always in Remove, whatever section its builder gave it
private val MenuRow<*>.section: MenuGroup get() = if (isDestructive) MenuGroup.REMOVE else group

fun <T> List<MenuRow<T>>.foldedIntoGroups(): List<MenuRow<T>> =
    groupBy { it.section }
        .toSortedMap(compareBy { it.ordinal })
        .flatMap { (section, rows) ->
            val name = section.label
            if (name == null || rows.size == 1) rows else listOf(MenuRow<T>(null, name, section, opensSubmenu = true))
        }

fun <T> MenuState<T>.rowsShown(): List<MenuRow<T>> {
    val visible = rows.filterNot { it.hidden || (it.action != null && it.action in withheld) }
    return if (parent == null) visible.foldedIntoGroups() else visible.sortedBy { it.section.ordinal }
}

// The drawn index the one divider is drawn above: the first row after Do and About. Null in a submenu, in a flat
// list, and when nothing is above it. The divider is not a row, so drawn indices stay those of rowsShown(); a
// caller that has the drawn rows already passes them, so the rows are folded once per frame.
fun <T> MenuState<T>.dividerIndex(shown: List<MenuRow<T>> = rowsShown()): Int? {
    if (parent != null) return null
    val index = shown.indexOfFirst { it.section.label != null }
    return index.takeIf { it > 0 }
}

fun <T> MenuState<T>.moved(delta: Int): MenuState<T> {
    val shown = rowsShown()
    if (shown.isEmpty()) return this
    val next = selectedIndex?.let { (it + delta).coerceIn(0, shown.lastIndex) }
        ?: if (delta > 0) 0 else shown.lastIndex
    return copy(selectedIndex = next)
}

fun <T> MenuState<T>.at(index: Int): MenuState<T> = copy(selectedIndex = index)

fun <T> MenuState<T>.submenuFor(group: MenuGroup): MenuState<T>? {
    val inGroup = rows.filter { it.section == group }
    if (inGroup.isEmpty()) return null
    return copy(subtitle = group.label, rows = inGroup, selectedIndex = 0, parent = this)
}

fun <T> MenuState<T>.confirmFor(row: MenuRow<T>): MenuState<T>? {
    if (!row.confirms) return null
    val action = row.action ?: return null
    return copy(
        subtitle = "${row.label}?",
        rows = listOf(
            MenuRow(null, CONFIRM_CANCEL_LABEL),
            MenuRow(action, row.label, isDestructive = true, confirms = false),
        ),
        selectedIndex = 0,
        parent = this,
    )
}

fun <T> MenuState<T>.chose(index: Int): MenuSelect<T> {
    val row = rowsShown().getOrNull(index) ?: return MenuSelect.None

    if (row.opensSubmenu) {
        return submenuFor(row.group)?.let { MenuSelect.Replace(it) } ?: MenuSelect.None
    }
    val action = row.action ?: return MenuSelect.Replace(back() ?: return MenuSelect.None)

    return confirmFor(row)?.let { MenuSelect.Replace(it) } ?: MenuSelect.Run(action)
}

fun <T> MenuState<T>.back(): MenuState<T>? = parent

const val CONFIRM_CANCEL_LABEL = "Cancel"
