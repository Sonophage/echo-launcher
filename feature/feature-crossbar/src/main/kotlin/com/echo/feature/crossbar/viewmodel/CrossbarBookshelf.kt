package com.echo.feature.crossbar.viewmodel

import com.echo.core.data.repository.MediaRootKind
import com.echo.core.domain.model.BuiltInCategory
import com.echo.core.ui.components.MenuState
import com.echo.core.ui.components.at
import com.echo.core.ui.sound.MenuSound
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CrossbarBookshelf(
    private val vm: CrossbarViewModel,
    private val uiState: MutableStateFlow<CrossbarUiState>,
    private val scope: CoroutineScope,
    private val bookRepository: com.echo.core.domain.repository.BookRepository,
    private val bookIntentResolver: com.echo.core.data.book.BookIntentResolver,
    private val bookScanner: com.echo.feature.library.scanner.BookScanner,
    private val menuSound: com.echo.core.ui.sound.MenuSoundPlayer,
) {
    internal fun observeContinueBook() {
        scope.launch {
            bookRepository.observeRecentlyOpenedBooks(1).collect { books ->
                val latest = books.firstOrNull()
                if (uiState.value.continueBook?.id != latest?.id) {
                    uiState.update { it.copy(continueBook = latest) }
                    if (vm.currentCategory()?.id == BuiltInCategory.LIBRARY &&
                        uiState.value.booksNav == BooksNav.Root
                    ) {
                        vm.loadItemsForCategory(vm.currentCategory(), keepCursorOnRow = true)
                    }
                }
            }
        }
    }

    internal fun observeBooks() {
        scope.launch {
            bookRepository.observeLibraries().collect { libraries ->
                uiState.update { it.copy(bookLibraries = libraries) }
                refreshBooksRootIfShowing()
            }
        }
        scope.launch {
            bookRepository.observeAllBooks().collect { books ->
                uiState.update { it.copy(bookSeries = books.seriesGroups()) }
                refreshBooksRootIfShowing()
            }
        }
        scope.launch {
            bookRepository.observeDefaultReader().collect { reader ->
                uiState.update {
                    it.copy(
                        defaultReader = reader,
                        defaultReaderLabel = reader?.let { pkg -> bookIntentResolver.readerLabel(pkg) },
                    )
                }
                refreshBooksRootIfShowing()
            }
        }
    }

    private suspend fun refreshBooksRootIfShowing() {
        if (vm.currentCategory()?.id == BuiltInCategory.LIBRARY &&
            uiState.value.booksNav == BooksNav.Root
        ) {
            uiState.update { it.copy(currentItems = booksRootItems()) }
        }
    }

    internal fun booksAddActions(): List<CrossbarItem> = buildList {
        if (uiState.value.bookLibraries.none { it.lastScannedAt != null }) {
            add(
                CrossbarItem(
                    id       = CrossbarViewModel.ADD_BOOK_FOLDER_ITEM_ID,
                    title    = "Add Book Folder",
                    subtitle = "Point the Library at a folder of EPUBs",
                    type     = CrossbarItemType.ADD_ACTION,
                )
            )
        }
        add(addBookAppsItem())
    }

    internal suspend fun booksRootItems(): List<CrossbarItem> =
        vm.libraryColumn(
            mediaColumn(uiState.value.booksRootSections(), vm.bookAppItems(), booksAddActions()),
            SearchScope.BOOKS,
        )

    private fun addBookAppsItem(): CrossbarItem = CrossbarItem(
        id       = CrossbarViewModel.ADD_LIBRARY_APPS_ITEM_ID,
        title    = "Add Book Apps",
        subtitle = "Pick installed apps to show here",
        type     = CrossbarItemType.ADD_ACTION,
    )

    internal fun bookItems(books: List<com.echo.core.domain.model.Book>): List<CrossbarItem> =
        books.map { book ->
            CrossbarItem(
                id       = "book_${book.id}",
                title    = book.displayTitle,
                subtitle = bookRowSubtitle(book.author, book.seriesName, book.seriesIndex),
                coverUri = book.coverUri,

                artworkUri = book.coverUri,
                type     = CrossbarItemType.LIBRARY_BOOK,
            )
        }

    internal fun emptyBooksItem(): CrossbarItem = CrossbarItem(
        id       = "books_empty",
        title    = "No books yet",
        subtitle = "Add a folder of EPUBs in Settings, then rescan",
        type     = CrossbarItemType.EMPTY,
    )

    internal fun bookShelfItems(): List<CrossbarItem> =
        uiState.value.bookLibraries.map {
            CrossbarItem(
                id       = "shelf_${it.id}",
                title    = it.displayName,
                subtitle = countLabel(it.bookCount, "book", "books"),
                type     = CrossbarItemType.LIBRARY_FOLDER,
            )
        }

    internal fun bookSeriesItems(): List<CrossbarItem> =
        uiState.value.bookSeries.map { series ->
            CrossbarItem(
                id       = "series_${series.name}",
                title    = series.name,
                subtitle = countLabel(series.bookCount, "book", "books"),
                coverUri = series.coverUri,
                artworkUri = series.coverUri,
                type     = CrossbarItemType.LIBRARY_SERIES,
            )
        }

    internal fun handleBooksSelection(item: CrossbarItem): Boolean = when {
        item.id == CrossbarViewModel.SEARCH_ITEM_ID -> { vm.librarySearch.openSearch(SearchScope.BOOKS); true }
        item.id == CrossbarViewModel.ADD_MENU_ITEM_ID -> { menuSound.play(MenuSound.SELECT); vm.openAddMenu(); true }
        item.id == CrossbarViewModel.OPEN_READER_ITEM_ID -> {
            menuSound.play(MenuSound.LAUNCH)
            val reader = uiState.value.defaultReader
            val error = reader?.let { bookIntentResolver.launchReader(it) }
            if (error != null) {
                uiState.update { it.copy(infoDialog = InfoDialogState(title = "Library", message = error)) }
            }
            true
        }
        item.id == CrossbarViewModel.BOOK_SHELVES_ITEM_ID -> { menuSound.play(MenuSound.SELECT); openBooksView(BooksNav.Shelves); true }
        item.id == CrossbarViewModel.ALL_BOOKS_ITEM_ID -> { menuSound.play(MenuSound.SELECT); openBooksView(BooksNav.AllBooks); true }
        item.id == CrossbarViewModel.BOOK_SERIES_ITEM_ID -> { menuSound.play(MenuSound.SELECT); openBooksView(BooksNav.SeriesList); true }
        item.id == CrossbarViewModel.ADD_BOOK_FOLDER_ITEM_ID -> {
            menuSound.play(MenuSound.SELECT)
            vm.folders.openMediaFolders(MediaRootKind.BOOK)
            true
        }
        item.type == CrossbarItemType.LIBRARY_SERIES -> {
            menuSound.play(MenuSound.SELECT)
            openBooksView(BooksNav.Series(item.title))
            true
        }
        item.type == CrossbarItemType.LIBRARY_FOLDER -> {
            menuSound.play(MenuSound.SELECT)
            openBooksView(BooksNav.Shelf(item.id.removePrefix("shelf_"), item.title))
            true
        }
        item.type == CrossbarItemType.LIBRARY_BOOK -> { openBook(item.id.removePrefix("book_")); true }
        item.id == CrossbarViewModel.ADD_LIBRARY_APPS_ITEM_ID -> {
            menuSound.play(MenuSound.SELECT)
            vm.openAppPicker(AppPickerTarget.CategoryShortcuts(CrossbarViewModel.LIBRARY_APPS_CATEGORY_ID), "Add Book Apps")
            true
        }

        item.packageName != null -> {
            menuSound.play(MenuSound.LAUNCH)
            vm.launchAppWithDisc(item.packageName, item.shelfCoverArt)
            true
        }
        else -> false
    }

    internal fun openBook(bookId: String) {
        menuSound.play(MenuSound.LAUNCH)
        scope.launch {
            val book = bookRepository.getBook(bookId) ?: return@launch

            vm.awaitDiscHandOff(book.coverUri)
            val error = bookIntentResolver.launch(book, uiState.value.defaultReader)
            if (error != null) {
                uiState.update { it.copy(infoDialog = InfoDialogState(title = book.displayTitle, message = error)) }
                return@launch
            }

            bookRepository.markBookOpened(bookId, System.currentTimeMillis())
        }
    }

    internal fun booksNavKey(nav: BooksNav): String = when (nav) {
        BooksNav.Folders  -> "folders"
        BooksNav.Root     -> "root"
        BooksNav.AllBooks -> "all"
        BooksNav.Shelves  -> "shelves"
        is BooksNav.Shelf -> "shelf_${nav.id}"
        BooksNav.SeriesList -> "series"
        is BooksNav.Series  -> "series_${nav.name}"
    }

    internal fun openBooksView(nav: BooksNav) = vm.navigateRememberingCursor { it.copy(booksNav = nav) }

    internal fun closeBooksView() = openBooksView(BooksNav.Root)

    internal fun openBookContextMenu(item: CrossbarItem): Boolean {
        if (item.menuHostCategory(vm.currentCategory()?.id) != BuiltInCategory.LIBRARY) return false
        if (item.type != CrossbarItemType.LIBRARY_BOOK || !item.id.startsWith("book_")) return false
        val bookId = item.id.removePrefix("book_")
        scope.launch {
            val onShelf = runCatching { bookRepository.getBook(bookId) }
                .getOrNull()?.lastOpenedAt != null
            val items = bookContextMenuItems(hasOpenStamp = onShelf)
            uiState.update {
                it.copy(activeContextMenu = CrossbarContextMenu(state = MenuState(title = item.title, rows = items), bookFileId = bookId))
            }
        }
        return true
    }

    internal fun handleBookAction(bookId: String, itemId: String) {
        when (itemId) {
            "book_open" -> openBook(bookId)

            "book_remove_recent" -> vm.appAction { bookRepository.clearBookLastOpened(bookId) }
            "book_remove" -> vm.appAction { bookRepository.removeBook(bookId) }
        }
    }

    internal suspend fun scanBookLibrary(libraryId: String, deep: Boolean = false) {
        val library = bookRepository.getLibrary(libraryId) ?: return
        val taskId = "book_scan_$libraryId"
        vm.addBackgroundTask(BackgroundTaskInfo(taskId, "Scanning ${library.displayName}", null))
        val existing = bookRepository.getBooksForLibrary(libraryId)
        bookScanner.scan(library, deep = deep, existing = existing).collect { result ->
            when (result) {
                is com.echo.feature.library.scanner.BookScanResult.Progress -> Unit
                is com.echo.feature.library.scanner.BookScanResult.Complete -> {
                    bookRepository.replaceBooksForLibrary(result.libraryId, result.books, System.currentTimeMillis())
                    vm.completeBackgroundTask(taskId, "${result.books.size} books")
                }
                is com.echo.feature.library.scanner.BookScanResult.Error ->
                    vm.failBackgroundTask(taskId, result.message)
            }
        }
    }
}
