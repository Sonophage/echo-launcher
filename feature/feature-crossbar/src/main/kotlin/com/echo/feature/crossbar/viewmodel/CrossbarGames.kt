package com.echo.feature.crossbar.viewmodel

import com.echo.core.domain.model.HiddenPlacement
import com.echo.core.domain.model.PlatformIds.ANDROID as ANDROID_PLATFORM_ID
import com.echo.core.ui.sound.MenuSound
import com.echo.core.domain.model.PlayState
import com.echo.core.domain.model.GamepadAction
import androidx.datastore.preferences.core.edit
import com.echo.core.data.datastore.echoDataStore
import com.echo.core.data.repository.MemoryCardRepository
import com.echo.core.domain.model.Game
import com.echo.core.domain.model.GameContentType
import com.echo.core.domain.model.MemoryCard
import com.echo.core.domain.model.PlatformIds.WINDOWS as WINDOWS_PLATFORM_ID
import com.echo.core.ui.components.MenuState
import com.echo.core.ui.components.move
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class CrossbarGames(
    private val vm: CrossbarViewModel,
    private val uiState: MutableStateFlow<CrossbarUiState>,
    private val scope: CoroutineScope,
    private val memoryCardRepository: MemoryCardRepository,
    private val menuSound: com.echo.core.ui.sound.MenuSoundPlayer,
) {
    internal fun memoryCardItems(): List<CrossbarItem> {
        val totalGames = uiState.value.allGamesCount
        val allGamesItem = CrossbarItem(
            id       = CrossbarViewModel.ALL_GAMES_ITEM_ID,
            title    = "All Games",
            subtitle = countLabel(totalGames, "game", "games"),
            insideCovers = uiState.value.cardFanCovers[CrossbarViewModel.ALL_GAMES_ITEM_ID].orEmpty(),
            type     = CrossbarItemType.ALL_GAMES,
        )

        val missingCount = uiState.value.missingCount
        val missingItem = if (missingCount > 0) {
            CrossbarItem(
                id       = CrossbarViewModel.MISSING_ITEM_ID,
                title    = "Missing",
                subtitle = countLabel(missingCount, "game", "games"),
                type     = CrossbarItemType.MISSING,
            )
        } else null
        val header = listOfNotNull(allGamesItem, missingItem)

        val visibleCards = vm.enabledCards.filter { card ->
            card.platformId != WINDOWS_PLATFORM_ID ||
                (uiState.value.platformGameCounts[WINDOWS_PLATFORM_ID] ?: card.gameCount) > 0
        }

        if (visibleCards.isEmpty()) {
            return vm.libraryColumn(
                header + CrossbarItem(
                    id       = CrossbarViewModel.NO_CONSOLES_ITEM_ID,
                    title    = "No systems yet",
                    subtitle = "Open Library Manager to add a system",
                    type     = CrossbarItemType.EMPTY,
                ),
                SearchScope.GAMES,
            )
        }

        val cardRows = if (uiState.value.gameGrouping == GameGrouping.GENRE) {
            genreFolderRows(uiState.value.genreCounts, uiState.value.cardFanCovers)
        } else visibleCards.map { card ->
            val count = uiState.value.platformGameCounts[card.platformId] ?: card.gameCount
            CrossbarItem(
                id          = vm.cardItemId(card.platformId),
                title       = card.displayName,
                subtitle    = countLabel(count, "game", "games"),
                platformId  = card.platformId,
                insideCovers = uiState.value.cardFanCovers[vm.cardItemId(card.platformId)].orEmpty(),
                accentColor = vm.platformCache[card.platformId]?.accentColor,
                type        = CrossbarItemType.MEMORY_CARD,
            )
        }

        val gapRow = if (totalGames == 0) vm.setupGapItem() else null
        val foldersRow = CrossbarItem(
            id       = CrossbarViewModel.ROM_FOLDERS_ITEM_ID,
            title    = columnSettingsTitle(vm.currentCategory()?.name),
            subtitle = countLabel(visibleCards.size, "console", "consoles"),
            type     = CrossbarItemType.MEDIA_ROOT,
        )
        return vm.libraryColumn(
            header + cardRows + listOfNotNull(gapRow) + foldersRow,
            SearchScope.GAMES,
        )
    }

    internal fun emptyFavoritesItem(): CrossbarItem = CrossbarItem(
        id       = CrossbarViewModel.EMPTY_FAVORITES_ITEM_ID,
        title    = "No favorites yet",
        subtitle = "Mark a game as a favorite from its options (≡) menu.",
        type     = CrossbarItemType.EMPTY,
    )

    internal fun promptRenameCard(platformId: String) {
        val card = vm.enabledCards.firstOrNull { it.platformId == platformId } ?: return
        vm.closeContextMenu()
        uiState.update { it.copy(collectionNameDialog = CollectionNameDialogState(
            title = "Rename System",
            subtitle = "The name this console shows under on the crossbar.",
            initialText = card.displayName,
            renameCardPlatformId = platformId,
        ))}
    }


    internal fun openGameContextMenu(item: CrossbarItem) {
        val gameId = item.gameId
        if (gameId == null) {
            openGameContextMenuCore(item, discCount = 0)
            return
        }

        scope.launch {
            val game = runCatching { vm.gameRepository.getById(gameId) }.getOrNull()
            val discCount = runCatching {
                game?.discSetKey?.let { vm.gameRepository.getDiscSetMembers(it).size } ?: 0
            }.getOrDefault(0)

            openGameContextMenuCore(item, discCount, onRecentShelf = game?.lastPlayedAt != null)
        }
    }

    private fun openGameContextMenuCore(item: CrossbarItem, discCount: Int, onRecentShelf: Boolean = false) {
        val state = uiState.value
        val currentCat = vm.currentCategory()
        val inGamingCategory = currentCat?.isGamingCategory == true
        val items = gameContextMenuItems(
            item = item,
            state = state,
            discCount = discCount,
            onRecentShelf = onRecentShelf,
            hideLocation = vm.currentHideLocation(),
        )
        uiState.update { it.copy(
            activeContextMenu = CrossbarContextMenu(state = MenuState(title = item.title, rows = items), gameId = item.gameId, packageName = item.packageName, shortcutId = item.shortcutId, launchIntentUri = item.launchIntentUri, categoryContext = if (inGamingCategory) currentCat.id else null, primaryId = "play")
        )}
    }

    fun onConfirmCollectionName(name: String) {
        val dialog = uiState.value.collectionNameDialog ?: return
        uiState.update { it.copy(collectionNameDialog = null) }
        if (dialog.editTitleGameId != null) {
            scope.launch {
                vm.gameRepository.updateUserTitleOverride(dialog.editTitleGameId, name.trim().ifBlank { null })

                vm.loadItemsForCategory(vm.currentCategory(), keepCursorOnRow = true)
            }
            return
        }
        if (dialog.editNoteGameId != null) {
            scope.launch {
                vm.gameRepository.updateNote(dialog.editNoteGameId, name.trim().ifBlank { null })
            }
            return
        }
        if (dialog.renameProfile) {
            scope.launch { vm.context.echoDataStore.edit { it[CrossbarViewModel.KEY_PROFILE_NAME] = name.trim().ifBlank { DEFAULT_PROFILE_NAME } } }
            return
        }
        if (dialog.renameCategoryId != null) {
            vm.move.renameColumn(dialog.renameCategoryId, name)
            return
        }
        if (dialog.renameCardPlatformId != null) {
            val trimmed = name.trim()
            if (trimmed.isEmpty()) return
            vm.appAction { memoryCardRepository.rename(dialog.renameCardPlatformId, trimmed) }
            return
        }
    }

    fun onCancelCollectionName() {
        uiState.update { it.copy(collectionNameDialog = null) }
    }

    internal suspend fun removeAndroidGames(platformId: String, packages: Set<String>) {
        packages.forEach { pkg ->
            val entry = vm.gameRepository.getAppEntry(pkg) ?: return@forEach
            if (entry.platformId != platformId) return@forEach
            vm.gameRepository.delete(entry.id)
        }
        Timber.i("Android library removal: ${packages.size} app(s) removed from $platformId")
    }

    internal suspend fun importAndroidGames(platformId: String, packages: Set<String>) {
        val labels = vm.appCategoryRepository.allInstalledApps().associateBy { it.packageName }

        packages.forEach { pkg ->

            val existing = vm.gameRepository.getAppEntry(pkg)
            when {
                existing == null -> vm.gameRepository.upsert(
                    com.echo.core.domain.model.Game(
                        title         = labels[pkg]?.label ?: pkg,
                        platformId    = platformId,
                        packageName   = pkg,
                        isManualEntry = true,

                        contentType   = com.echo.core.domain.model.GameContentType.GAME,
                    )
                )

                existing.platformId != platformId ||
                    existing.contentType != com.echo.core.domain.model.GameContentType.GAME ->
                    vm.gameRepository.upsert(existing.copy(
                        platformId  = platformId,
                        contentType = com.echo.core.domain.model.GameContentType.GAME,
                    ))
            }
        }
        memoryCardRepository.recountGames(platformId)
        Timber.i("Android library import: ${packages.size} app(s) selected for $platformId")
    }

    internal fun setCardPinned(platformId: String, pinned: Boolean) {
        scope.launch { memoryCardRepository.setPinned(platformId, pinned) }
    }

    internal fun hideCard(platformId: String) {
        scope.launch {
            memoryCardRepository.setEnabled(platformId, false)
            if (uiState.value.selectedPlatformId == platformId) vm.closePlatformFolder()
        }
    }

    internal fun removeCard(platformId: String) {
        scope.launch {
            memoryCardRepository.remove(platformId)
            if (uiState.value.selectedPlatformId == platformId) vm.closePlatformFolder()
        }
    }

    internal fun toggleGameFavorite(gameId: Long, isFavorite: Boolean) {
        scope.launch {
            vm.gameRepository.setFavorite(gameId, isFavorite)
        }
    }

    internal fun addAppToFavorites(packageName: String, label: String) {
        scope.launch {
            runCatching {
                val id = vm.ensureAppShortcut(packageName)
                vm.gameRepository.setFavorite(id, true)
            }.onSuccess {
                Timber.i("App shortcut favorited: $packageName")
                vm.taskNotifier.complete("shortcut_fav_$packageName", label, "Added to Favorites")
            }.onFailure { e ->
                Timber.e(e, "Failed to add app to Favorites: $packageName")
                vm.taskNotifier.failed("shortcut_fav_$packageName", label, "Couldn't add to Favorites: ${e.message}")
            }
        }
    }


    internal suspend fun existingCards(): List<MemoryCard> =
        runCatching { memoryCardRepository.getAll() }.getOrDefault(emptyList())
    internal fun onGameMenuItem(itemId: String, menu: CrossbarContextMenu) {
        val gameId = menu.gameId ?: return
        if (itemId.startsWith("emu_pick_")) {
            val gid = gameId
            val choice = itemId.removePrefix("emu_pick_")
            vm.appAction {
                vm.gameRepository.setPreferredEmulator(gid, choice.takeIf { it != "default" })
            }
        } else if (itemId.startsWith("detail_")) {
            val gid = gameId
            when (val what = itemId.removePrefix("detail_")) {
                "title" -> scope.launch {
                    val game = vm.gameRepository.getById(gid) ?: return@launch
                    vm.closeContextMenu()
                    uiState.update { it.copy(collectionNameDialog = CollectionNameDialogState(
                        title = "Edit Title",
                        subtitle = "The name shown in the launcher and used when scraping artwork.",
                        resetLabel = "Use Scanned Name",
                        initialText = game.displayTitle,
                        editTitleGameId = gid,
                        placeholder = "Leave blank to use the scanned name",
                    ))}
                }
                "note" -> scope.launch {
                    val game = vm.gameRepository.getById(gid) ?: return@launch
                    vm.closeContextMenu()
                    uiState.update { it.copy(collectionNameDialog = CollectionNameDialogState(
                        title = "Edit Note",
                        subtitle = "Kept with the game. Only you see it.",
                        initialText = game.userNote.orEmpty(),
                        editNoteGameId = gid,
                        placeholder = "Anything you want to remember about this game",
                    ))}
                }
                "ARTWORK"  -> vm.artworkTools.openArtworkStudio(gid)
                "MANUAL"   -> vm.gameDetail.openManualFor(gid)
                "METADATA" -> vm.artworkTools.openMetadataPreviewFor(gid)
                "REFRESH"  -> vm.artworkTools.fetchArtworkFor(gid)
                else -> Timber.w("Details row '$what' has no handler")
            }
        } else if (itemId == "shelf_favorite") {
            val onShelf = menu.items.firstOrNull { it.action == "shelf_favorite" }?.checked == true
            toggleGameFavorite(gameId, !onShelf)
        } else if (itemId.startsWith("genre_pick_")) {
            val gid = gameId
            val choice = itemId.removePrefix("genre_pick_")
            vm.appAction { vm.gameRepository.setGenreOverride(gid, com.echo.core.domain.model.GameGenre.fromName(choice)) }
        } else if (itemId.startsWith("pstate_")) {
            val gid = gameId
            val choice = itemId.removePrefix("pstate_")
            vm.appAction {
                vm.gameRepository.setPlayState(gid, PlayState.fromName(choice))
            }
        } else if (itemId.startsWith("disc_pick_")) {
            val discId = itemId.removePrefix("disc_pick_").toLongOrNull()
            if (discId != null) {
                menuSound.play(MenuSound.SELECT)
                vm.appAction { vm.gameRepository.setPreferredDisc(gameId, discId) }
            }
        } else when (itemId) {

            "play"                   -> vm.launching.launchGameDirectly(gameId)
            "edit_genre"             -> vm.openGenrePickerMenu(gameId)
            "genre_only"             -> vm.filterByGenreOf(gameId)
            "genre_all"              -> vm.setGenreFilter(null)
            "game_info"              -> vm.openGameInfoFor(gameId)
            "info_about"             -> { vm.closeContextMenu(); vm.gameDetail.onGameInfoAction(GameInfoAction.INFO) }
            "info_video"             -> { vm.closeContextMenu(); vm.gameDetail.onGameInfoAction(GameInfoAction.VIDEO) }
            "info_manual"            -> { vm.closeContextMenu(); vm.gameDetail.onGameInfoAction(GameInfoAction.MANUAL) }
            "choose_disc"             -> vm.openDiscPickerMenu(gameId)
            "export_game"            -> vm.exportGameFromMenu(gameId)
            "app_info"               -> { vm.closeContextMenu(); com.echo.core.data.apps.AppSystemActions.openAppInfo(vm.context, menu.packageName ?: return) }
            "uninstall"              -> { vm.closeContextMenu(); com.echo.core.data.apps.AppSystemActions.uninstall(vm.context, menu.packageName ?: return) }
            "favorite"               -> toggleGameFavorite(gameId, true)
            "unfavorite"             -> toggleGameFavorite(gameId, false)

            "pin_recent", "unpin_recent" -> { vm.closeContextMenu(); vm.recents.togglePinned("g:$gameId") }
            "remove_from_recent"     -> { vm.closeContextMenu(); vm.recents.dismissGameFromRecents(gameId) }
            "add_category"           -> menu.categoryContext?.let { vm.openGameCategoryPicker(gameId, it, "add") }
            "move_category"          -> menu.categoryContext?.let { vm.openGameCategoryPicker(gameId, it, "move") }
            "remove_category"        -> menu.categoryContext?.let { cat ->
                val gid = gameId
                vm.appAction {
                    vm.gameCategoryRepository.removeGameFromCategory(gid, cat)
                    vm.loadItemsForCategory(vm.currentCategory())
                }
            }
            "pin_category"           -> menu.categoryContext?.let { cat ->
                val gid = gameId
                vm.appAction {
                    vm.gameCategoryRepository.pinGameInCategory(gid, cat, true)
                    vm.loadItemsForCategory(vm.currentCategory())
                }
            }
            "unpin_category"         -> menu.categoryContext?.let { cat ->
                val gid = gameId
                vm.appAction {
                    vm.gameCategoryRepository.pinGameInCategory(gid, cat, false)
                    vm.loadItemsForCategory(vm.currentCategory())
                }
            }
            "file_location"          -> vm.showGameFileLocation(gameId)
            "change_emulator"        -> vm.openEmulatorPickerMenu(gameId)
            "shelves"                -> vm.openShelvesPickerMenu(gameId)

            "remove_game", "remove_missing" -> {
                val gid = gameId
                vm.appAction { vm.removeGameFromLibrary(gid) }
            }
            "hide_here"              -> vm.currentHideLocation()?.let { (type, id, label) ->
                vm.persistHide(HiddenPlacement.gameKey(gameId), menu.title, type, id, label)
            }
            "remove_app"             -> {
                val gid = gameId
                vm.appAction {
                    vm.gameRepository.delete(gid)
                    memoryCardRepository.recountGames(ANDROID_PLATFORM_ID)
                }
            }

            "unmark_game"            -> {
                val gid = gameId
                vm.appAction {
                    vm.gameRepository.getById(gid)?.let { g ->
                        vm.gameRepository.upsert(g.copy(
                            platformId  = com.echo.core.domain.model.PlatformIds.APP_SHORTCUT,
                            contentType = GameContentType.ANDROID_APP,
                        ))
                    }
                    memoryCardRepository.recountGames(ANDROID_PLATFORM_ID)
                    vm.loadItemsForCategory(vm.currentCategory())
                }
            }
        }
    }
}
