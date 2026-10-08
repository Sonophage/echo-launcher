package com.echo.feature.crossbar.viewmodel

import com.echo.core.domain.model.BuiltInCategory

// owner, 2026-10-05: the apps and folder rows of a category can be put in any order. The order is a list of
// item ids per category; an item it does not name keeps its place after the ones it does
internal fun orderedColumn(items: List<CrossbarItem>, order: List<String>): List<CrossbarItem> {
    if (order.isEmpty()) return items
    return items.withIndex()
        .sortedBy { (i, item) -> order.indexOf(item.id).let { at -> if (at >= 0) at else order.size + i } }
        .map { it.value }
}

internal const val COLUMN_ORDER_PREFIX = "column_order_"

// apps and the category's own rows move; add rows, search, placeholders and single files (a Resume row)
// stay where the column puts them
internal val CrossbarItem.movableInColumn: Boolean
    get() = type !in FIXED_IN_COLUMN && !id.startsWith(CrossbarViewModel.RECENT_APP_ID_PREFIX)

private val FIXED_IN_COLUMN = setOf(
    CrossbarItemType.ADD_ACTION, CrossbarItemType.EMPTY, CrossbarItemType.SEARCH,
    CrossbarItemType.VIDEO_FILE, CrossbarItemType.PHOTO_FILE, CrossbarItemType.MUSIC_TRACK, CrossbarItemType.LIBRARY_BOOK,
)

// the category whose top column can be put in order: a media category at its root, or a category of apps
internal fun CrossbarUiState.columnOrderKey(): String? {
    val category = categories.getOrNull(selectedCategoryIndex) ?: return null
    return when (category.id) {
        BuiltInCategory.MUSIC -> category.id.takeIf { musicNav == MusicNav.Root }
        BuiltInCategory.VIDEO -> category.id.takeIf { videoNav == VideoNav.Root }
        BuiltInCategory.PHOTO -> category.id.takeIf { photoNav == PhotoNav.Root }
        BuiltInCategory.LIBRARY -> category.id.takeIf { booksNav == BooksNav.Root }
        BuiltInCategory.GAMES, BuiltInCategory.RECENTLY_PLAYED, BuiltInCategory.SETTINGS, BuiltInCategory.ANDROID -> null
        else -> category.id.takeUnless { category.isGamingCategory }
    }
}
