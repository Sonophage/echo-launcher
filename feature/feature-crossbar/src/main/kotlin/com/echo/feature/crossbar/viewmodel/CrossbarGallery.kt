package com.echo.feature.crossbar.viewmodel

import com.echo.core.ui.media.deleteFromDevice
import com.echo.core.ui.media.deleteMediaFile
import com.echo.core.ui.media.postDeleteResult
import com.echo.core.domain.model.GamepadAction
import com.echo.core.data.repository.MediaRootKind
import com.echo.core.domain.model.BuiltInCategory
import com.echo.core.ui.components.MenuState
import com.echo.core.ui.notification.BackgroundTaskNotifier
import com.echo.core.ui.sound.MenuSound
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CrossbarGallery(
    private val vm: CrossbarViewModel,
    private val uiState: MutableStateFlow<CrossbarUiState>,
    private val scope: CoroutineScope,
    private val photoRepository: com.echo.core.domain.repository.PhotoRepository,
    private val photoScanner: com.echo.feature.library.scanner.PhotoScanner,
    private val photoIntentResolver: com.echo.core.data.photo.PhotoIntentResolver,
    private val menuSound: com.echo.core.ui.sound.MenuSoundPlayer,
) {
    internal fun observePhoto() {
        scope.launch {
            photoRepository.observeFavorites().collect { favorites ->
                uiState.update { it.copy(photoFavoriteCount = favorites.size) }
                if (vm.currentCategory()?.id == BuiltInCategory.PHOTO && uiState.value.photoNav == PhotoNav.Root) {
                    uiState.update { it.copy(currentItems = photoRootItems()) }
                }
            }
        }
        scope.launch {
            photoRepository.observeLibraries().collect { libraries ->
                uiState.update { it.copy(photoLibraries = libraries) }
                if (vm.currentCategory()?.id == BuiltInCategory.PHOTO &&
                    uiState.value.photoNav == PhotoNav.Root
                ) {
                    uiState.update { it.copy(currentItems = photoRootItems()) }
                }
            }
        }
    }

    internal fun photoAddActions(): List<CrossbarItem> = buildList {
        if (uiState.value.photoLibraries.none { it.lastScannedAt != null }) add(addPhotoLibraryItem())
        add(addPhotoAppsItem())
    }

    internal suspend fun photoRootItems(): List<CrossbarItem> =
        vm.mediaRootColumn(uiState.value.photoLibraries.isNotEmpty(), uiState.value.photoRootSections(vm.cameraAvailable), vm.photoAppItems(), photoAddActions(), SearchScope.PHOTOS)

    private fun addPhotoLibraryItem(): CrossbarItem = CrossbarItem(
        id       = CrossbarViewModel.ADD_PHOTO_LIBRARY_ITEM_ID,
        title    = "Add Photo Library",
        subtitle = "Add a photo folder here or in Settings ▸ Library",
        type     = CrossbarItemType.ADD_ACTION,
    )

    private fun addPhotoAppsItem(): CrossbarItem = CrossbarItem(
        id       = CrossbarViewModel.ADD_PHOTO_APPS_ITEM_ID,
        title    = "Add Photo Apps",
        subtitle = "Pick installed apps to show here",
        type     = CrossbarItemType.ADD_ACTION,
    )

    internal fun photoAlbumItems(libraries: List<com.echo.core.domain.model.PhotoLibrary>): List<CrossbarItem> {
        val rows = libraries.map { lib ->
            CrossbarItem(
                id       = "plib_${lib.id}",
                title    = lib.displayName,
                subtitle = countLabel(lib.photoCount, "photo", "photos"),
                type     = CrossbarItemType.PHOTO_FOLDER,
            )
        }
        return rows.ifEmpty {
            listOf(
                CrossbarItem(
                    id = CrossbarViewModel.EMPTY_CATEGORY_ITEM_ID,
                    title = "No albums yet",
                    subtitle = "Add a folder from the Folders row",
                    type = CrossbarItemType.EMPTY,
                ),
            )
        }
    }

    private fun List<com.echo.core.domain.model.Photo>.toPhotoItems(): List<CrossbarItem> =
        map { photo ->
            CrossbarItem(
                id       = "pho_${photo.id}",
                title    = photo.displayName,
                subtitle = photoRowSubtitle(photo.displayDateMs, photo.resolutionLabel, photo.sizeBytes),
                type     = CrossbarItemType.PHOTO_FILE,
                mediaUri = photo.uri,
                mimeType = photo.mimeType,
                coverUri = photo.thumbnailUri,
            )
        }

    internal fun setPhotoItems(
        photos: List<com.echo.core.domain.model.Photo>,
        emptyItem: CrossbarItem,
    ) {
        val items = if (photos.isEmpty()) listOf(emptyItem) else photos.toPhotoItems()
        uiState.update { it.copy(currentItems = items) }
    }

    internal fun emptyFavoritePhotosItem(): CrossbarItem = CrossbarItem(
        id       = CrossbarViewModel.EMPTY_CATEGORY_ITEM_ID,
        title    = "No favourites yet",
        subtitle = "Add one from a photo's info panel",
        type     = CrossbarItemType.EMPTY,
    )

    internal fun emptyAllPhotosItem(): CrossbarItem = CrossbarItem(
        id       = CrossbarViewModel.EMPTY_CATEGORY_ITEM_ID,
        title    = "No photos found",
        subtitle = "Add a photo library and scan it",
        type     = CrossbarItemType.EMPTY,
    )

    internal fun emptyLibraryPhotosItem(): CrossbarItem = CrossbarItem(
        id       = CrossbarViewModel.EMPTY_CATEGORY_ITEM_ID,
        title    = "No photos in this album",
        subtitle = "Scan it from its ⚙ Options menu",
        type     = CrossbarItemType.EMPTY,
    )

    internal fun handlePhotoSelection(item: CrossbarItem): Boolean = when {
        item.id == CrossbarViewModel.SEARCH_ITEM_ID -> { vm.librarySearch.openSearch(SearchScope.PHOTOS); true }
        item.id == CrossbarViewModel.ADD_MENU_ITEM_ID -> { menuSound.play(MenuSound.SELECT); vm.openAddMenu(); true }
        item.id == CrossbarViewModel.ALL_PHOTOS_ITEM_ID -> { menuSound.play(MenuSound.SELECT); openPhotoView(PhotoNav.AllPhotos); true }
        item.id == CrossbarViewModel.PHOTO_ALBUMS_ITEM_ID -> { menuSound.play(MenuSound.SELECT); openPhotoView(PhotoNav.Albums); true }
        item.id == CrossbarViewModel.PHOTO_FAVORITES_ITEM_ID -> { menuSound.play(MenuSound.SELECT); openPhotoView(PhotoNav.Favorites); true }
        item.id == CrossbarViewModel.CAMERA_ITEM_ID -> { menuSound.play(MenuSound.LAUNCH); vm.launching.launchCamera(); true }
        item.id == CrossbarViewModel.ADD_PHOTO_LIBRARY_ITEM_ID -> {
            menuSound.play(MenuSound.SELECT)
            vm.folders.openMediaFolders(MediaRootKind.PHOTO)
            true
        }
        item.id == CrossbarViewModel.ADD_PHOTO_APPS_ITEM_ID -> {
            menuSound.play(MenuSound.SELECT)
            vm.appPickerSection.openAppPicker(AppPickerTarget.CategoryShortcuts(CrossbarViewModel.PHOTO_APPS_CATEGORY_ID), "Add Photo Apps")
            true
        }

        item.packageName != null -> {
            menuSound.play(MenuSound.LAUNCH)
            vm.launching.launchAppWithDisc(item.packageName, item.shelfCoverArt)
            true
        }
        item.type == CrossbarItemType.PHOTO_FOLDER && item.id.startsWith("plib_") -> {
            menuSound.play(MenuSound.SELECT)
            openPhotoView(PhotoNav.Library(item.id.removePrefix("plib_"), item.title))
            true
        }
        item.type == CrossbarItemType.PHOTO_FILE && item.id.startsWith("pho_") -> {
            menuSound.play(MenuSound.SELECT)
            openPhoto(item.id.removePrefix("pho_"))
            true
        }
        else -> false
    }

    internal fun photoNavKey(nav: PhotoNav): String = when (nav) {
        PhotoNav.Folders    -> "folders"
        PhotoNav.Root       -> "root"
        PhotoNav.AllPhotos  -> "all"
        PhotoNav.Albums     -> "albums"
        PhotoNav.Favorites  -> "favorites"
        is PhotoNav.Library -> "library_${nav.id}"
    }

    internal fun openPhotoView(nav: PhotoNav) = vm.navigateRememberingCursor { it.copy(photoNav = nav) }

    internal fun closePhotoView() = openPhotoView(PhotoNav.Root)

    internal fun openPhoto(photoId: String) {
        scope.launch {
            val open = photoOpenFor(photoRepository.observeDefaultViewer().first())
            val photo = if (open == PhotoOpen.BuiltIn) null else photoRepository.getPhoto(photoId)
            if (photo == null) {
                openPhotoViewer(photoId)
                return@launch
            }
            val error = photoIntentResolver.launch(photo, (open as? PhotoOpen.App)?.packageName)
            if (error != null) {
                uiState.update { it.copy(infoDialog = InfoDialogState(title = photo.displayName, message = error)) }
            }
        }
    }

    private fun openPhotoViewer(photoId: String, wallpaperPreview: Boolean = false) {
        val nav = uiState.value.photoNav
        val libraryId = (nav as? PhotoNav.Library)?.id
        uiState.update {
            it.copy(activePhotoViewer = PhotoViewerRequest(photoId, libraryId, openWallpaperPreview = wallpaperPreview,
                favoritesOnly = nav == PhotoNav.Favorites))
        }
    }

    fun onClosePhotoViewer() {
        uiState.update { it.copy(activePhotoViewer = null, pendingPhotoViewerAction = null) }
    }

    fun consumePhotoViewerAction() {
        uiState.update { it.copy(pendingPhotoViewerAction = null) }
    }

    internal fun openPhotoContextMenu(item: CrossbarItem): Boolean {
        if (item.menuHostCategory(vm.currentCategory()?.id) != BuiltInCategory.PHOTO) return false
        return when {
            item.type == CrossbarItemType.PHOTO_FILE && item.id.startsWith("pho_") -> {
                openPhotoFileContextMenu(item.id.removePrefix("pho_"), item.title); true
            }
            item.type == CrossbarItemType.PHOTO_FOLDER && item.id.startsWith("plib_") -> {
                openPhotoLibraryContextMenu(item.id.removePrefix("plib_"), item.title); true
            }
            item.packageName != null -> {
                vm.openAppContextMenu(item, categoryIdOverride = CrossbarViewModel.PHOTO_APPS_CATEGORY_ID); true
            }
            else -> false
        }
    }

    private fun openPhotoFileContextMenu(photoId: String, title: String) {
        val items = photoFileContextMenuItems()
        uiState.update { it.copy(activeContextMenu = CrossbarContextMenu(state = MenuState(title = title, rows = items), photoFileId = photoId)) }
    }

    internal fun handlePhotoFileAction(photoId: String, itemId: String) {
        when (itemId) {
            "photo_open"          -> openPhoto(photoId)

            "photo_set_wallpaper" -> openPhotoViewer(photoId, wallpaperPreview = true)
            "photo_remove"        -> vm.appAction {
                val photo = photoRepository.getPhoto(photoId) ?: return@appAction
                val deleted = deleteFromDevice(photo.uri, { deleteMediaFile(vm.context, it) }) { photoRepository.removePhoto(photoId) }
                postDeleteResult(deleted, photo.displayName)
            }
        }
    }

    private fun openPhotoLibraryContextMenu(libraryId: String, name: String) {
        val items = photoLibraryContextMenuItems()
        uiState.update { it.copy(activeContextMenu = CrossbarContextMenu(state = MenuState(title = name, rows = items), photoLibraryId = libraryId)) }
    }

    internal fun handlePhotoLibraryAction(libraryId: String, itemId: String) {
        when (itemId) {
            "photo_lib_open" -> {
                val name = uiState.value.photoLibraries.firstOrNull { it.id == libraryId }?.displayName.orEmpty()
                openPhotoView(PhotoNav.Library(libraryId, name))
            }
            "photo_lib_scan" -> vm.appAction { scanPhotoLibrary(libraryId) }
            "photo_lib_manage" -> vm.folders.openMediaFolders(MediaRootKind.PHOTO)
        }
    }

    internal suspend fun scanPhotoLibrary(libraryId: String) {
        val library = photoRepository.getLibrary(libraryId) ?: return
        val taskId = "photo_scan_${library.id}"
        val notifier = BackgroundTaskNotifier(vm.context)
        notifier.running(taskId, "Scanning ${library.displayName}", null)
        val existing = photoRepository.getPhotosForLibrary(library.id)
        photoScanner.scan(library, deep = false, existing = existing).collect { result ->
            when (result) {
                is com.echo.feature.library.scanner.PhotoScanResult.Progress ->
                    notifier.running(taskId, "Scanning ${result.libraryName}", null)
                is com.echo.feature.library.scanner.PhotoScanResult.Complete -> {
                    photoRepository.replacePhotosForLibrary(result.libraryId, result.photos, System.currentTimeMillis())
                    notifier.complete(taskId, "Scanned ${library.displayName}", "${result.photos.size} photos")
                }
                is com.echo.feature.library.scanner.PhotoScanResult.Error ->
                    notifier.failed(taskId, "Scan failed: ${library.displayName}", result.message)
            }
        }
    }

    internal fun photoAlbumSiblings(): List<CrossbarItem> =
        uiState.value.photoLibraries.map { CrossbarItem(id = "plib_${it.id}", title = it.displayName, type = CrossbarItemType.PHOTO_FOLDER) }
    internal suspend fun loadColumn() {
        when (val nav = uiState.value.photoNav) {
            PhotoNav.Folders -> vm.mediaRootRepository.roots(MediaRootKind.PHOTO).collect {
                uiState.update { s -> s.copy(currentItems = vm.folders.mediaFolderItems(MediaRootKind.PHOTO)) }
            }
            PhotoNav.Root -> uiState.update { it.copy(currentItems = photoRootItems()) }
            PhotoNav.AllPhotos -> photoRepository.observeAllPhotos().collect { photos ->
                setPhotoItems(photos, emptyAllPhotosItem())
            }
            PhotoNav.Favorites -> photoRepository.observeFavorites().collect { photos ->
                setPhotoItems(photos, emptyFavoritePhotosItem())
            }
            PhotoNav.Albums -> photoRepository.observeLibraries().collect { libs ->
                uiState.update { it.copy(currentItems = photoAlbumItems(libs)) }
            }
            is PhotoNav.Library -> photoRepository.observePhotosByLibrary(nav.id).collect { photos ->
                setPhotoItems(photos, emptyLibraryPhotosItem())
            }
        }
    }
}
