package com.echo.feature.crossbar.viewmodel

import com.echo.core.domain.model.BuiltInCategory
import com.echo.core.domain.model.GamepadAction

enum class SearchScope(
    val label: String,
    val hint: String,
    val emptyTitle: String,
    val emptyHint: String,
) {
    ALL(

        "Search", "Games, apps, music, video, photos and books",
        "Nothing to search yet", "Add folders in Settings ▸ Library or Settings ▸ Emulators, then search from anywhere",
    ),
    GAMES(
        "Search Games", "Titles in your game library",
        "No games yet", "Add a ROM folder in Settings ▸ Emulators ▸ Library Manager",
    ),
    VIDEOS(
        "Search Video", "Titles in your video libraries",
        "No videos yet", "Add a video folder in Settings ▸ Library ▸ Media Libraries",
    ),
    PHOTOS(
        "Search Photos", "File names in your albums",
        "No photos yet", "Add a photo folder in Settings ▸ Library ▸ Media Libraries",
    ),
    BOOKS(
        "Search Books", "Titles, authors and series",
        "No books yet", "Add a book folder in Settings ▸ Library ▸ Media Libraries",
    ),
    MUSIC(
        "Search Music", "Titles, artists and albums",
        "No music yet", "Add a music folder in Settings ▸ Library ▸ Media Libraries",
    ),
    APPS(
        "Search Apps", "Installed apps and their package names",
        "No apps yet", "Nothing is installed that can be launched",
    ),
}

// owner, 2026-10-05: Search finds apps, plus each library whose column is on the crossbar, and its hint
// names only those; turning Media or Gaming off in setup takes them out of both
enum class SearchKind(val noun: String, val categoryId: String?) {
    GAMES("games", BuiltInCategory.GAMES),
    APPS("apps", null),
    MUSIC("music", BuiltInCategory.MUSIC),
    VIDEO("video", BuiltInCategory.VIDEO),
    PHOTOS("photos", BuiltInCategory.PHOTO),
    BOOKS("books", BuiltInCategory.LIBRARY),
}

fun searchKindsShown(visibleCategoryIds: Collection<String>): Set<SearchKind> =
    SearchKind.entries.filter { it.categoryId == null || it.categoryId in visibleCategoryIds }.toSet()

fun searchAllHint(kinds: Set<SearchKind>): String {
    val nouns = SearchKind.entries.filter { it in kinds }.map { it.noun }
    val text = if (nouns.size < 2) nouns.joinToString() else nouns.dropLast(1).joinToString(", ") + " and " + nouns.last()
    return text.replaceFirstChar { it.uppercase() }
}

fun normalizeForSearch(text: String): String =
    buildString(text.length) {
        var lastWasSpace = true
        for (ch in text) {
            if (ch.isLetterOrDigit()) {
                append(ch.lowercaseChar())
                lastWasSpace = false
            } else if (!lastWasSpace) {
                append(' ')
                lastWasSpace = true
            }
        }
    }.trim()

fun matchesSearch(query: String, vararg fields: String?): Boolean {
    val terms = normalizeForSearch(query).split(' ').filter { it.isNotEmpty() }
    if (terms.isEmpty()) return false
    val haystack = fields.filterNotNull().joinToString(" ") { normalizeForSearch(it) }

    return terms.all { haystack.contains(it) }
}

enum class SearchEmptyState { LOADING, EMPTY_LIBRARY, PROMPT, NO_MATCHES }

fun searchEmptyState(loaded: Boolean, query: String, anyContent: Boolean = true): SearchEmptyState = when {
    !loaded -> SearchEmptyState.LOADING

    !anyContent -> SearchEmptyState.EMPTY_LIBRARY
    normalizeForSearch(query).isEmpty() -> SearchEmptyState.PROMPT
    else -> SearchEmptyState.NO_MATCHES
}

// owner, 2026-10-05: the results stand on one shelf (the "Drawer and Search Variations" design's 6b), so left and
// right walk along it; up and down have nowhere to go
fun searchStep(action: GamepadAction): Int = when (action) {
    GamepadAction.NAVIGATE_LEFT -> -1
    GamepadAction.NAVIGATE_RIGHT -> 1
    else -> 0
}

// owner, 2026-10-05: the shelf opens with the likeliest result in the middle. How well a title matches, best
// first: the whole title, its start, the start of one of its words, anywhere in it
fun searchRank(query: String, title: String): Int {
    val q = normalizeForSearch(query)
    val t = normalizeForSearch(title)
    return when {
        q.isEmpty() -> 3
        t == q -> 0
        t.startsWith(q) -> 1
        t.split(' ').any { it.startsWith(q) } -> 2
        else -> 3
    }
}

// lays a ranked list out from the middle: the first in the centre, then one to the right, one to the left, and so
// on outwards, so the likeliest results sit nearest the centre. The centre is at index (size - 1) / 2
fun <T> centreOut(ranked: List<T>): List<T> {
    val left = ranked.filterIndexed { i, _ -> i % 2 == 0 && i > 0 }
    val right = ranked.filterIndexed { i, _ -> i % 2 == 1 }
    return left.reversed() + ranked.take(1) + right
}

fun centreOutIndex(size: Int): Int = (size - 1).coerceAtLeast(0) / 2

// the kind of thing a result is, for the shelf's filter
fun searchKindOf(row: CrossbarItem): SearchKind? = when {
    row.isInstalledApp -> SearchKind.APPS
    row.type == CrossbarItemType.VIDEO_FILE -> SearchKind.VIDEO
    row.type == CrossbarItemType.PHOTO_FILE -> SearchKind.PHOTOS
    row.type == CrossbarItemType.LIBRARY_BOOK -> SearchKind.BOOKS
    row.type == CrossbarItemType.MUSIC_TRACK -> SearchKind.MUSIC
    row.gameId != null -> SearchKind.GAMES
    else -> null
}

// LB and RB step through All and each kind the results hold, and stop at the ends
fun stepSearchKind(current: SearchKind?, present: List<SearchKind>, delta: Int): SearchKind? {
    val order = listOf<SearchKind?>(null) + present
    val at = order.indexOf(current).coerceAtLeast(0)
    return order[(at + delta).coerceIn(0, order.lastIndex)]
}
