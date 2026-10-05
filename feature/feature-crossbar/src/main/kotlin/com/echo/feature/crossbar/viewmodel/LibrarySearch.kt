package com.echo.feature.crossbar.viewmodel

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

// owner, 2026-10-05: the results run in two columns under the banner
const val SEARCH_COLUMNS = 2

// up and down move a row; left and right move within the row and stop at its ends
fun searchStep(action: GamepadAction, index: Int): Int {
    val column = index % SEARCH_COLUMNS
    return when (action) {
        GamepadAction.NAVIGATE_UP -> -SEARCH_COLUMNS
        GamepadAction.NAVIGATE_DOWN -> SEARCH_COLUMNS
        GamepadAction.NAVIGATE_LEFT -> if (column > 0) -1 else 0
        GamepadAction.NAVIGATE_RIGHT -> if (column < SEARCH_COLUMNS - 1) 1 else 0
        else -> 0
    }
}
