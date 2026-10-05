package com.echo.feature.crossbar.viewmodel

import android.net.Uri
import com.echo.core.data.repository.MediaRootKind
import com.echo.core.data.repository.MediaScannedEntry
import com.echo.core.data.repository.RomFolderEntry
import com.echo.core.data.repository.SafGrants
import com.echo.core.data.repository.isRomDirUnder
import com.echo.core.data.repository.mediaRootDisplayName
import com.echo.core.data.repository.mediaRootRows
import com.echo.core.data.repository.romFolderEntries
import com.echo.core.domain.model.PlatformIds.ANDROID as ANDROID_PLATFORM_ID
import com.echo.core.domain.model.PlatformIds.WINDOWS as WINDOWS_PLATFORM_ID
import com.echo.core.ui.components.MenuGroup
import com.echo.core.ui.components.MenuState
import com.echo.core.ui.notification.SystemToasts
import com.echo.core.ui.notification.ToastKind
import com.echo.core.ui.sound.MenuSound
import com.echo.feature.library.scanner.LibraryScanner
import com.echo.feature.library.scanner.ScanStatus
import com.echo.feature.library.scanner.scanOutcomeMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class CrossbarFolders(
    private val vm: CrossbarViewModel,
    private val uiState: MutableStateFlow<CrossbarUiState>,
    private val scope: CoroutineScope,
    private val mediaRootRepository: com.echo.core.data.repository.MediaRootRepository,
    private val romRootRepository: com.echo.core.data.repository.RomRootRepository,
    private val libraryScanner: LibraryScanner,
    private val pcGameScanner: com.echo.feature.settings.pc.PcGameScanner,
    private val menuSound: com.echo.core.ui.sound.MenuSoundPlayer,
) {
    private suspend fun scannedEntriesFor(kind: MediaRootKind): List<MediaScannedEntry> = when (kind) {
        MediaRootKind.MUSIC -> vm.musicRepository.getFolders().map {
            MediaScannedEntry(it.treeUri, it.displayName, it.trackCount, it.lastScannedAt)
        }
        MediaRootKind.VIDEO -> vm.videoRepository.getLibraries().map {
            MediaScannedEntry(it.treeUri, it.displayName, it.videoCount, it.lastScannedAt)
        }
        MediaRootKind.PHOTO -> vm.photoRepository.getLibraries().map {
            MediaScannedEntry(it.treeUri, it.displayName, it.photoCount, it.lastScannedAt)
        }
        MediaRootKind.BOOK -> vm.bookRepository.getLibraries().map {
            MediaScannedEntry(it.treeUri, it.displayName, it.bookCount, it.lastScannedAt)
        }
    }

    internal suspend fun mediaFolderItems(kind: MediaRootKind): List<CrossbarItem> {
        val roots = mediaRootRepository.getAll(kind)
        val persisted = SafGrants.persistedReadUris(vm.context.contentResolver)
        val rows = mediaRootRows(roots, persisted, scannedEntriesFor(kind)) { treeUri ->
            mediaRootDisplayName(vm.context, treeUri, mediaKindLabel(kind))
        }
        val (one, many) = vm.mediaKindNouns(kind)

        return rows.map { row ->
            CrossbarItem(
                id       = CrossbarViewModel.mediaRootItemId(kind, row.treeUri),
                title    = row.name,
                subtitle = row.itemCount.let { count ->
                    when {
                        !row.linked   -> "Access lost — Relink to grant it again"
                        count == null -> "Not scanned yet"
                        else          -> countLabel(count, one, many)
                    }
                },
                type          = CrossbarItemType.MEDIA_ROOT,
                mediaRootUri  = row.treeUri,
                mediaRootKind = kind,
            )
        } + CrossbarItem(
            id       = CrossbarViewModel.addMediaRootItemId(kind),
            title    = "Add Folder",
            subtitle = if (rows.isEmpty()) "Grant a ${mediaKindFolderWord(kind)} folder to start"
                       else "Grant another ${mediaKindFolderWord(kind)} folder",
            type          = CrossbarItemType.ADD_ACTION,
            mediaRootKind = kind,
        )
    }

    internal fun openMediaFolders(kind: MediaRootKind) = when (kind) {
        MediaRootKind.MUSIC -> vm.music.openMusicView(MusicNav.Folders)
        MediaRootKind.VIDEO -> vm.video.openVideoView(VideoNav.Folders)
        MediaRootKind.PHOTO -> vm.gallery.openPhotoView(PhotoNav.Folders)
        MediaRootKind.BOOK  -> vm.bookshelf.openBooksView(BooksNav.Folders)
    }

    fun requestMediaRootPick(kind: MediaRootKind, relinkFrom: String? = null) {
        uiState.update { it.copy(rootPick = RootPick(RootTarget.Media(kind), relinkFrom)) }
    }

    fun requestRomRootPick(relinkFrom: String? = null) {
        uiState.update { it.copy(rootPick = RootPick(RootTarget.Rom, relinkFrom)) }
    }

    fun onMediaRootPicked(uri: Uri?) {
        val request = uiState.value.rootPick ?: return
        uiState.update { it.copy(rootPick = null) }
        if (uri == null) return
        scope.launch {
            when (val target = request.target) {
                is RootTarget.Media -> {
                    mediaRootRepository.persist(uri)
                    if (request.relinkFrom != null) {
                        mediaRootRepository.replace(target.kind, request.relinkFrom, uri.toString())
                    } else {
                        mediaRootRepository.add(target.kind, uri.toString())
                    }
                    rescanMediaKind(target.kind)
                    vm.loadItemsForCategory(vm.currentCategory(), keepCursorOnRow = true)
                }
                RootTarget.Rom -> {
                    romRootRepository.persist(uri, writable = true)
                    if (request.relinkFrom != null) {
                        romRootRepository.replace(request.relinkFrom, uri.toString())
                    } else {
                        romRootRepository.add(uri.toString())
                    }
                    refreshRomFolders()
                }
            }
        }
    }

    private fun removeMediaRoot(kind: MediaRootKind, treeUri: String) {
        vm.appAction {
            mediaRootRepository.remove(kind, treeUri)
            rescanMediaKind(kind)
        }
    }

    private suspend fun entryIdForRoot(kind: MediaRootKind, treeUri: String): String {
        val name = mediaRootDisplayName(vm.context, treeUri, mediaKindLabel(kind))
        return when (kind) {
            MediaRootKind.MUSIC ->
                (vm.musicRepository.getFolders().firstOrNull { it.treeUri == treeUri }
                    ?: vm.musicRepository.addFolder(name, treeUri)).id
            MediaRootKind.VIDEO ->
                (vm.videoRepository.getLibraries().firstOrNull { it.treeUri == treeUri }
                    ?: vm.videoRepository.addLibrary(name, treeUri)).id
            MediaRootKind.PHOTO ->
                (vm.photoRepository.getLibraries().firstOrNull { it.treeUri == treeUri }
                    ?: vm.photoRepository.addLibrary(name, treeUri)).id
            MediaRootKind.BOOK ->
                (vm.bookRepository.getLibraries().firstOrNull { it.treeUri == treeUri }
                    ?: vm.bookRepository.addLibrary(name, treeUri)).id
        }
    }

    private suspend fun scanOneRoot(kind: MediaRootKind, treeUri: String, deep: Boolean) {
        val entryId = entryIdForRoot(kind, treeUri)
        when (kind) {
            MediaRootKind.MUSIC -> vm.music.scanMusicFolder(entryId)
            MediaRootKind.VIDEO -> vm.video.scanVideoLibrary(entryId, deep)
            MediaRootKind.PHOTO -> vm.gallery.scanPhotoLibrary(entryId)
            MediaRootKind.BOOK  -> vm.bookshelf.scanBookLibrary(entryId, deep)
        }
    }

    private fun rescanMediaRoot(kind: MediaRootKind, treeUri: String, deep: Boolean = false) {
        scope.launch { scanOneRoot(kind, treeUri, deep) }
    }

    private fun rescanMediaKind(kind: MediaRootKind, deep: Boolean = false) {
        scope.launch {
            val roots = mediaRootRepository.getAll(kind)
            vm.pruneOrphanEntries(kind, roots)
            roots.forEach { scanOneRoot(kind, it, deep) }
        }
    }

    internal fun openMediaRootContextMenu(item: CrossbarItem) {
        val kind = item.mediaRootKind ?: return
        val treeUri = item.mediaRootUri ?: return
        val linked = item.subtitle?.startsWith("Access lost") != true
        uiState.update {
            it.copy(
                activeContextMenu = CrossbarContextMenu(
                    state = MenuState(
                        title = item.title,
                        rows = mediaRootContextMenuItems(linked, kind),
                    ),
                    mediaRootUri = treeUri,
                    mediaRootKind = kind,
                ),
            )
        }
    }

    internal fun handleMediaRootAction(kind: MediaRootKind, treeUri: String, itemId: String) {
        when (mediaRootActionOf(itemId)) {
            MediaRootAction.RESCAN      -> rescanMediaRoot(kind, treeUri)
            MediaRootAction.RESCAN_DEEP -> rescanMediaRoot(kind, treeUri, deep = true)
            MediaRootAction.RELINK      -> requestMediaRootPick(kind, relinkFrom = treeUri)
            MediaRootAction.REMOVE      -> removeMediaRoot(kind, treeUri)
            null -> Unit
        }
    }


    internal fun openMediaFoldersContextMenu(item: CrossbarItem) {
        val kind = item.mediaRootKind ?: return
        uiState.update {
            it.copy(
                activeContextMenu = CrossbarContextMenu(
                    state = MenuState(title = item.title, rows = mediaFoldersContextMenuItems(kind)),
                    mediaRootKind = kind,
                ),
            )
        }
    }

    internal fun handleMediaFoldersAction(kind: MediaRootKind, itemId: String) {
        when (mediaFoldersActionOf(itemId)) {
            MediaFoldersAction.ADD_ROOT        -> requestMediaRootPick(kind)
            MediaFoldersAction.RESCAN_ALL      -> rescanMediaKind(kind)
            MediaFoldersAction.RESCAN_ALL_DEEP -> rescanMediaKind(kind, deep = true)
            MediaFoldersAction.DEFAULT_APP     -> vm.openDefaultMediaAppMenu(kind)
            MediaFoldersAction.CLEAR_CACHE     -> vm.clearMediaCache(kind)
            MediaFoldersAction.PICK_APP        -> vm.setDefaultMediaApp(
                kind,
                itemId.removePrefix(MEDIA_APP_PREFIX).takeIf { it != CrossbarViewModel.MEDIA_APP_NONE },
            )
            null -> Unit
        }
    }

    internal fun emptyFolderItem(platformId: String): CrossbarItem {
        if (platformId == ANDROID_PLATFORM_ID) {
            return CrossbarItem(
                id         = CrossbarViewModel.FIND_GAMES_ITEM_ID,
                title      = "Find Games",
                subtitle   = "Pick installed apps to add to this library",
                platformId = platformId,
            )
        }
        val card = vm.enabledCards.firstOrNull { it.platformId == platformId }

        val gap = vm.setupState.firstGap
        if (gap != com.echo.feature.launcher.SetupGap.NONE) {
            return CrossbarItem(
                id         = CrossbarViewModel.SETUP_GAP_ITEM_ID,
                title      = gap.message,
                subtitle   = "Press confirm to open Settings and fix it.",
                platformId = platformId,
                type       = CrossbarItemType.EMPTY,
            )
        }
        val subtitle = when {
            card?.romDirectory == null -> "ROM directory not configured"
            else                       -> "Press ▲ to scan this console"
        }
        return CrossbarItem(
            id         = CrossbarViewModel.NO_GAMES_ITEM_ID,
            title      = "No games found in this folder",
            subtitle   = subtitle,
            platformId = platformId,
            type       = CrossbarItemType.EMPTY,
        )
    }

    internal suspend fun romFolderItems(): List<CrossbarItem> {
        val roots = romRootRepository.getAll()
        val persisted = SafGrants.persistedReadUris(vm.context.contentResolver)
        val entries = romFolderEntries(
            roots = roots,
            persistedReadUris = persisted,
            cards = vm.enabledCards,
            rawPathOfTree = { com.echo.core.data.repository.RomRootRepository.rawPathOfTree(it) },
            fallbackName = { "ROM Root" },
        )

        return entries.map { entry ->
            when (entry) {
                is RomFolderEntry.Root -> CrossbarItem(
                    id       = "romroot_${entry.treeUri}",
                    title    = entry.name,
                    subtitle = if (!entry.linked) "Access lost — Relink to grant it again"
                        else countLabel(entry.consoleCount, "console", "consoles") +
                            "  ·  " + countLabel(entry.gameCount, "game", "games"),
                    type         = CrossbarItemType.MEDIA_ROOT,
                    mediaRootUri = entry.treeUri,
                )
                is RomFolderEntry.Console -> CrossbarItem(
                    id         = "romcard_${entry.platformId}",
                    title      = entry.displayName,
                    subtitle   = if (!entry.underRoot) "Not under any granted folder"
                        else entry.romDirectory ?: countLabel(entry.gameCount, "game", "games"),
                    platformId = entry.platformId,
                    type       = CrossbarItemType.MEMORY_CARD,
                )
            }
        } + CrossbarItem(
            id       = CrossbarViewModel.ADD_ROM_ROOT_ITEM_ID,
            title    = "Add ROM Folder",
            subtitle = if (roots.isEmpty()) "Grant the folder your ROMs live in"
                       else "Grant another folder — an SD card, say",
            type     = CrossbarItemType.ADD_ACTION,
        )
    }

    internal fun openRomFolders() = vm.navigateRememberingCursor { it.copy(romFoldersOpen = true) }

    internal fun closeRomFolders() = vm.navigateRememberingCursor { it.copy(romFoldersOpen = false) }

    private fun refreshRomFolders() {
        if (!uiState.value.romFoldersOpen) return
        scope.launch { uiState.update { it.copy(currentItems = romFolderItems()) } }
    }

    private fun romRootContextMenuItems(linked: Boolean): List<CrossbarContextMenuItem> = buildList {
        if (linked) add(CrossbarContextMenuItem("rom_root_scan", "Scan This Folder"))
        add(CrossbarContextMenuItem("rom_root_relink", "Relink Folder", group = MenuGroup.SETTINGS))
        add(
            CrossbarContextMenuItem(
                "rom_root_remove", "Remove Folder",
                isDestructive = true, group = MenuGroup.REMOVE,
            ),
        )
    }

    internal fun openRomRootContextMenu(item: CrossbarItem) {
        val treeUri = item.mediaRootUri ?: return
        val linked = item.subtitle?.startsWith("Access lost") != true
        uiState.update { it.copy(
            activeContextMenu = CrossbarContextMenu(
                state = MenuState(title = item.title, rows = romRootContextMenuItems(linked)),
                mediaRootUri = treeUri,
            ),
        )}
    }

    private fun scanCardsUnderRoot(treeUri: String) {
        val rawPath = com.echo.core.data.repository.RomRootRepository.rawPathOfTree(treeUri)
        if (rawPath == null) {
            SystemToasts.post("Cannot read that folder", "Relink it and try again.", ToastKind.ERROR)
            return
        }
        val under = vm.enabledCards.filter { card ->
            card.romDirectory?.let { isRomDirUnder(it, rawPath) } == true
        }
        if (under.isEmpty()) {
            SystemToasts.post("No consoles under this folder", "Scan a console to create one.", ToastKind.ERROR)
            return
        }
        under.forEach { scanCard(it.platformId) }
    }

    internal fun handleRomRootAction(treeUri: String, itemId: String) {
        when (itemId) {
            "rom_root_scan"   -> scanCardsUnderRoot(treeUri)
            "rom_root_relink" -> requestRomRootPick(relinkFrom = treeUri)
            "rom_root_remove" -> vm.appAction {
                romRootRepository.remove(treeUri)
                refreshRomFolders()
            }
        }
    }

    internal fun scanCard(platformId: String) {
        scope.launch {
            val card = vm.memoryCardRepository.getById(platformId) ?: return@launch
            val taskId = "scan_$platformId"

            if (platformId == WINDOWS_PLATFORM_ID) {
                vm.addBackgroundTask(BackgroundTaskInfo(id = taskId, label = "Scanning ${card.displayName}…", progress = null))
                val report = runCatching { pcGameScanner.scan() }
                    .onFailure { Timber.e(it, "PC scan failed") }
                    .getOrNull()
                if (report == null) {
                    vm.failBackgroundTask(taskId, "PC scan failed")
                } else {
                    vm.memoryCardRepository.recordScan(platformId, System.currentTimeMillis())
                    vm.completeBackgroundTask(
                        taskId,
                        if (report.newGames == 0) "No new PC games found" else report.message,
                    )
                }
                return@launch
            }

            vm.addBackgroundTask(BackgroundTaskInfo(id = taskId, label = "Scanning ${card.displayName}…", progress = null))
            val outcome = libraryScanner.scanPlatform(platformId, removeMissing = true)
            when (outcome.status) {
                ScanStatus.COMPLETED -> vm.completeBackgroundTask(
                    taskId,
                    scanOutcomeMessage(outcome, removeMissing = true),
                )
                else -> vm.failBackgroundTask(
                    taskId,
                    scanOutcomeMessage(outcome, removeMissing = true),
                )
            }
        }
    }

    internal fun romFolderSelection(item: CrossbarItem): Boolean? = when {
        item.id == CrossbarViewModel.ROM_FOLDERS_ITEM_ID -> {
            menuSound.play(MenuSound.SELECT); openRomFolders(); true
        }
        item.id == CrossbarViewModel.ADD_ROM_ROOT_ITEM_ID -> {
            menuSound.play(MenuSound.SELECT); requestRomRootPick(); true
        }
        item.id.startsWith("romroot_") -> {
            menuSound.play(MenuSound.SELECT); openRomRootContextMenu(item); true
        }
        else -> null
    }

    internal fun mediaRootSelection(item: CrossbarItem): Boolean? {
        val kind = item.mediaRootKind ?: return null
        return when {
            item.id == CrossbarViewModel.mediaFoldersItemId(kind) -> {
                menuSound.play(MenuSound.SELECT); openMediaFolders(kind); true
            }
            item.id == CrossbarViewModel.addMediaRootItemId(kind) -> {
                menuSound.play(MenuSound.SELECT); requestMediaRootPick(kind); true
            }
            item.mediaRootUri != null -> {
                menuSound.play(MenuSound.SELECT); openMediaRootContextMenu(item); true
            }
            else -> null
        }
    }
}
