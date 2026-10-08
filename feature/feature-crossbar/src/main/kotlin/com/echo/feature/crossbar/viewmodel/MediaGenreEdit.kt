package com.echo.feature.crossbar.viewmodel

import com.echo.core.ui.components.MenuState
import com.echo.core.domain.model.genreName
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// owner, 2026-10-08: a track's, an album's or a book's genre, set by the owner over the tag
sealed interface GenreTarget {
    data class Tracks(val ids: List<String>) : GenreTarget
    data class Book(val id: String) : GenreTarget
}

internal const val GENRE_PICK_TAG = "genre_tag"
internal const val GENRE_PICK_NEW = "genre_new"
internal const val GENRE_PICK_PREFIX = "genre_set_"

// the picker's rows: back to the tag, every genre the library already has, and a new one
internal fun genrePickerRows(tagged: String?, current: String?, inLibrary: List<String>): List<CrossbarContextMenuItem> = buildList {
    add(CrossbarContextMenuItem(GENRE_PICK_TAG, tagged?.let { "As tagged · $it" } ?: "As tagged · none", checked = current == null))
    inLibrary.distinctBy { it.lowercase() }.sortedBy { it.lowercase() }.forEach { g ->
        add(CrossbarContextMenuItem(GENRE_PICK_PREFIX + g, g, checked = current.equals(g, ignoreCase = true)))
    }
    add(CrossbarContextMenuItem(GENRE_PICK_NEW, "New Genre…"))
}

// the genre a pick sets: null puts back the tag
internal fun genreOfPick(itemId: String): String? = itemId.removePrefix(GENRE_PICK_PREFIX).takeIf { itemId.startsWith(GENRE_PICK_PREFIX) }

class CrossbarGenres(
    private val vm: CrossbarViewModel,
    private val uiState: kotlinx.coroutines.flow.MutableStateFlow<CrossbarUiState>,
    private val scope: kotlinx.coroutines.CoroutineScope,
) {
    fun openPicker(target: GenreTarget) {
        scope.launch {
            val rows = when (target) {
                is GenreTarget.Tracks -> {
                    val all = vm.musicRepository.observeAllTracks().first()
                    val mine = all.filter { it.id in target.ids }
                    val tagged = mine.firstNotNullOfOrNull { com.echo.core.domain.model.mediaGenreName(it.genre, null) }
                    val current = mine.map { it.genreOverride }.distinct().singleOrNull()
                    genrePickerRows(tagged, current, all.mapNotNull { it.genreName })
                }
                is GenreTarget.Book -> {
                    val all = vm.bookRepository.observeAllBooks().first()
                    val book = all.firstOrNull { it.id == target.id } ?: return@launch
                    genrePickerRows(com.echo.core.domain.model.mediaGenreName(book.genre, null), book.genreOverride, all.mapNotNull { it.genreName })
                }
            }
            uiState.update { it.copy(activeContextMenu = CrossbarContextMenu(state = MenuState(title = "Genre", rows = rows), genreTarget = target)) }
        }
    }

    fun onPick(target: GenreTarget, itemId: String) {
        vm.closeContextMenu()
        if (itemId == GENRE_PICK_NEW) {
            uiState.update { it.copy(collectionNameDialog = CollectionNameDialogState(title = "New Genre", editGenreTarget = target, placeholder = "e.g. Synthwave, Cozy Mystery")) }
            return
        }
        set(target, genreOfPick(itemId))
    }

    fun set(target: GenreTarget, genre: String?) {
        vm.appAction {
            when (target) {
                is GenreTarget.Tracks -> vm.musicRepository.setGenreOverride(target.ids, genre)
                is GenreTarget.Book -> vm.bookRepository.setGenreOverride(target.id, genre)
            }
        }
    }
}
