package com.echo.feature.settings.viewmodel

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echo.core.data.datastore.echoDataStore
import com.echo.core.data.repository.CategoryRepositoryImpl
import com.echo.core.data.repository.CoreInventory
import com.echo.core.domain.model.BuiltInCategory
import com.echo.core.data.repository.FolderLinkStatus
import com.echo.core.data.repository.InitialSetupFlag
import com.echo.core.data.repository.MediaRootKind
import com.echo.core.data.repository.MediaRootRepository
import com.echo.core.data.repository.RomRootRepository
import com.echo.core.data.repository.SafGrants
import com.echo.core.data.repository.Vita3KLibrary
import com.echo.feature.artwork.MetadataApiKeyProvider
import com.echo.feature.artwork.api.ArtworkImportManager
import com.echo.core.data.steamgriddb.SgdbApiKeyProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

// owner, 2026-10-06: the ECHO folder is its own step, straight after what ECHO is for, so a folder
// kept from an earlier install brings its settings back before the rest of setup
enum class SetupStep { FEATURES, ECHO_FOLDER, PERMISSIONS, STORAGE, EMULATORS, ACCOUNTS }

@Immutable
data class InitialSetupUiState(
    val step: SetupStep = SetupStep.FEATURES,

    // what ECHO is for: both off is a launcher only, both on the full suite
    val gaming: Boolean = true,
    val media: Boolean = true,

    val retroArchInstalled: Boolean = false,

    val vita3KInstalled: Boolean = false,

    val romRoots: List<RootFolderRow> = emptyList(),
    val musicRoots: List<RootFolderRow> = emptyList(),
    val videoRoots: List<RootFolderRow> = emptyList(),
    val photoRoots: List<RootFolderRow> = emptyList(),
    val bookRoots: List<RootFolderRow> = emptyList(),

    val isHomeLauncher: Boolean = false,

    val folderAccess: List<FolderAccessRow> = emptyList(),

    val artworkFolderName: String? = null,

    // the ECHO folder to confirm in the picker, after it was renamed or made
    val artworkPickAgain: String? = null,

    val hasSgdb: Boolean = false,
    val hasTmdb: Boolean = false,
    val igdbClientId: String = "",
    val ssUsername: String = "",

    val retroArchLinked: Boolean = false,
    val retroArchCoreCount: Int? = null,
    val retroArchDetecting: Boolean = false,

    val vitaFolderName: String? = null,
    val message: String? = null,

    val suggestions: Map<StorageSlot, String> = emptyMap(),
) {
    private val reachableSteps: List<SetupStep>
        get() = reachableSetupSteps(retroArchInstalled, vita3KInstalled, gaming)

    val stepNumber: Int get() = (reachableSteps.indexOf(step) + 1).coerceAtLeast(1)

    val stepCount: Int get() = reachableSteps.size

    val nextStep: SetupStep? get() = reachableSteps.getOrNull(reachableSteps.indexOf(step) + 1)

    val scrapersConnected: Int get() =
        listOf(hasSgdb, hasTmdb, igdbClientId.isNotBlank(), ssUsername.isNotBlank()).count { it }

    fun rootsFor(slot: StorageSlot): List<RootFolderRow> = when (slot) {
        StorageSlot.GAMES -> romRoots
        StorageSlot.MUSIC -> musicRoots
        StorageSlot.VIDEO -> videoRoots
        StorageSlot.PHOTOS -> photoRoots
        StorageSlot.BOOKS -> bookRoots
        StorageSlot.ARTWORK -> emptyList()
    }
}

// owner, 2026-10-05: setup first asks what ECHO is for. Gaming and Media are each on or off; what is off
// is skipped in setup (its folders, its permissions, the Emulators step) and hidden from the crossbar.
// Libraries and Settings turn any of it back on
internal fun reachableSetupSteps(retroArchInstalled: Boolean, vita3KInstalled: Boolean, gaming: Boolean = true): List<SetupStep> =
    SetupStep.entries.filter { it != SetupStep.EMULATORS || (gaming && (retroArchInstalled || vita3KInstalled)) }

// the Your folders step; the ECHO folder (ARTWORK) has its own step
internal fun storageSlotsFor(gaming: Boolean, media: Boolean): List<StorageSlot> = StorageSlot.entries.filter {
    when (it) {
        StorageSlot.GAMES -> gaming
        StorageSlot.ARTWORK -> false
        StorageSlot.MUSIC, StorageSlot.VIDEO, StorageSlot.PHOTOS, StorageSlot.BOOKS -> media
    }
}

internal val MEDIA_CATEGORIES = listOf(BuiltInCategory.MUSIC, BuiltInCategory.VIDEO, BuiltInCategory.PHOTO, BuiltInCategory.LIBRARY)

// the crossbar columns to show or hide when a feature changed; a feature left alone keeps the columns
// the user set by hand
internal fun featureColumnChanges(from: Pair<Boolean, Boolean>, gaming: Boolean, media: Boolean): Map<String, Boolean> = buildMap {
    if (from.first != gaming) put(BuiltInCategory.GAMES, gaming)
    if (from.second != media) MEDIA_CATEGORIES.forEach { put(it, media) }
}

internal val StorageSlot.mediaKind: MediaRootKind?
    get() = when (this) {
        StorageSlot.MUSIC -> MediaRootKind.MUSIC
        StorageSlot.VIDEO -> MediaRootKind.VIDEO
        StorageSlot.PHOTOS -> MediaRootKind.PHOTO
        StorageSlot.BOOKS -> MediaRootKind.BOOK
        StorageSlot.GAMES, StorageSlot.ARTWORK -> null
    }

@Immutable
private data class RootLists(
    val rom: List<RootFolderRow>,
    val music: List<RootFolderRow>,
    val video: List<RootFolderRow>,
    val photo: List<RootFolderRow>,
    val book: List<RootFolderRow>,
    val artwork: String?,
    val vita: String?,
)

@Immutable
private data class ServiceIdentities(
    val hasSgdb: Boolean,
    val hasTmdb: Boolean,
    val igdbClientId: String,
    val ssUsername: String,
)

private val KEY_INITIAL_SETUP_SEEN = com.echo.core.data.repository.InitialSetupFlag.KEY_SEEN

// where the first-run wizard stands. Becoming the Home app makes Android start a second ECHO in a home
// task, with a new wizard; it opens at the saved step, not the first
@Singleton
class SetupProgress @Inject constructor(@ApplicationContext private val context: Context) {
    suspend fun savedStep(): SetupStep? = savedStepIn(context.echoDataStore.data.first())

    suspend fun save(step: SetupStep) {
        context.echoDataStore.edit { it[InitialSetupFlag.KEY_STEP] = step.name }
    }
}

// none once setup is done, so setup opened again from Settings starts at the beginning
internal fun savedStepIn(prefs: Preferences): SetupStep? =
    if (prefs[KEY_INITIAL_SETUP_SEEN] == true) null
    else SetupStep.entries.firstOrNull { it.name == prefs[InitialSetupFlag.KEY_STEP] }

private const val RETROARCH_FAMILY = "com.retroarch"

internal const val NO_CORES_KEPT_PREVIOUS =
    "That folder has no RetroArch cores, so your previous link was kept. Pick RetroArch itself in the picker."

private val VITA3K_PACKAGES = listOf("org.vita3k.emulator", "org.vita3k.emulator.ikhoeyZX")

@HiltViewModel
class InitialSetupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val romRootRepository: RomRootRepository,
    private val mediaRootRepository: MediaRootRepository,
    private val artworkImportManager: ArtworkImportManager,
    private val retroArchSetup: RetroArchSetup,
    private val vita3KLibrary: Vita3KLibrary,
    private val sgdbKeys: SgdbApiKeyProvider,
    private val metadataKeys: MetadataApiKeyProvider,
    private val folderAccess: FolderAccess,
    private val standardRomFolders: StandardRomFolders,
    private val launcherShortcuts: com.echo.feature.appbar.LauncherShortcutRepository,
    private val tmdbKeys: com.echo.feature.artwork.api.TmdbApiKeyProvider,
    private val artworkFolderSetup: ArtworkFolderSetup,
    private val storageSuggestions: StorageSuggestions,
    private val categoryRepository: CategoryRepositoryImpl,
    private val setupProgress: SetupProgress,
) : ViewModel() {
    private val scratch = MutableStateFlow(InitialSetupUiState())

    // gaming and media as the crossbar has them, so Continue changes only what the user switched
    private var featuresOnCrossbar = true to true

    init {
        viewModelScope.launch {
            setupProgress.savedStep()?.let { saved -> scratch.update { it.copy(step = saved) } }
        }
        viewModelScope.launch {
            val all = runCatching { categoryRepository.observeAll().first() }.getOrDefault(emptyList())
            if (all.isNotEmpty()) {
                val gaming = all.any { it.id == BuiltInCategory.GAMES && it.isVisible }
                val media = all.any { it.id in MEDIA_CATEGORIES && it.isVisible }
                featuresOnCrossbar = gaming to media
                scratch.update { it.copy(gaming = gaming, media = media) }
            }
        }
        viewModelScope.launch {
            scratch.update {
                it.copy(
                    retroArchInstalled = isRetroArchInstalled(),
                    vita3KInstalled = isVita3KInstalled(),
                )
            }
            readRetroArchState()
        }
        viewModelScope.launch(Dispatchers.IO) {
            val suggested = runCatching { storageSuggestions.suggest() }.getOrDefault(emptyMap())
            scratch.update { it.copy(suggestions = suggested.mapValues { (_, uri) -> uri.toString() }) }
        }
        refreshGrants()
    }

    fun refreshGrants() {
        scratch.update { it.copy(isHomeLauncher = launcherShortcuts.isDefaultLauncher()) }
        viewModelScope.launch { scratch.update { it.copy(folderAccess = folderAccess.rows()) } }
    }

    fun regrantFolder(row: FolderAccessRow, uri: Uri) {
        viewModelScope.launch {
            val message = folderAccess.regrant(row, uri)
            scratch.update { it.copy(message = message ?: it.message) }
            refreshGrants()
        }
    }

    fun homeRoleIntent(): android.content.Intent = launcherShortcuts.homeRoleRequestIntent()

    private val rootLists = combine(
        combine(
            romRootRepository.roots,
            mediaRootRepository.roots(MediaRootKind.MUSIC),
            mediaRootRepository.roots(MediaRootKind.VIDEO),
            mediaRootRepository.roots(MediaRootKind.PHOTO),
            artworkImportManager.folderTreeUri,
        ) { rom, music, video, photo, artwork ->
            val persisted = SafGrants.persistedReadUris(context.contentResolver)
            RootLists(
                rom     = rom.toRows(persisted),
                music   = music.toRows(persisted),
                video   = video.toRows(persisted),
                photo   = photo.toRows(persisted),
                book    = emptyList(),
                artwork = artwork?.let(::rootDisplayName),
                vita    = null,
            )
        },
        vita3KLibrary.ux0TreeUriFlow,

        mediaRootRepository.roots(MediaRootKind.BOOK),
    ) { lists, vita, book ->
        lists.copy(
            vita = vita?.let(::rootDisplayName),
            book = book.toRows(SafGrants.persistedReadUris(context.contentResolver)),
        )
    }

    private val serviceIdentities = combine(
        sgdbKeys.apiKeyFlow,
        metadataKeys.igdbClientIdFlow,
        metadataKeys.ssUsernameFlow,
        tmdbKeys.keyFlow,
    ) { sgdbKey, igdbId, ssUser, tmdbKey ->
        ServiceIdentities(
            hasSgdb      = !sgdbKey.isNullOrBlank(),
            hasTmdb      = !tmdbKey.isNullOrBlank(),
            igdbClientId = igdbId.orEmpty(),
            ssUsername   = ssUser.orEmpty(),
        )
    }

    val uiState: StateFlow<InitialSetupUiState> = combine(
        scratch, rootLists, serviceIdentities,
    ) { local, roots, services ->
        local.copy(
            romRoots       = roots.rom,
            musicRoots     = roots.music,
            videoRoots     = roots.video,
            photoRoots     = roots.photo,
            bookRoots      = roots.book,
            artworkFolderName = roots.artwork,
            vitaFolderName    = roots.vita,
            hasSgdb           = services.hasSgdb,
            hasTmdb           = services.hasTmdb,
            igdbClientId      = services.igdbClientId,
            ssUsername        = services.ssUsername,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), scratch.value)

    private fun List<String>.toRows(persisted: Set<String>): List<RootFolderRow> =
        map { uri ->
            RootFolderRow(
                treeUri = uri,
                name = rootDisplayName(uri),
                linked = SafGrants.linkStatus(uri, persisted) == FolderLinkStatus.LINKED,
            )
        }

    private fun isRetroArchInstalled(): Boolean = runCatching {
        context.packageManager.getInstalledPackages(0).any {
            it.packageName == RETROARCH_FAMILY || it.packageName.startsWith("$RETROARCH_FAMILY.")
        }
    }.getOrDefault(false)

    private fun isVita3KInstalled(): Boolean =
        VITA3K_PACKAGES.any {
            runCatching { context.packageManager.getPackageInfo(it, 0) }.isSuccess
        }

    private var parkedForExcursion = false

    fun parkForExcursion() { parkedForExcursion = true }

    fun resetWizard() {
        if (parkedForExcursion) {
            parkedForExcursion = false
            return
        }
        scratch.update {
            it.copy(step = SetupStep.FEATURES, gaming = featuresOnCrossbar.first, media = featuresOnCrossbar.second,
                message = null, retroArchDetecting = false)
        }
    }

    private fun reachableSteps(): List<SetupStep> =
        reachableSetupSteps(scratch.value.retroArchInstalled, scratch.value.vita3KInstalled, scratch.value.gaming)

    fun setGaming(on: Boolean) = scratch.update { it.copy(gaming = on) }

    fun setMedia(on: Boolean) = scratch.update { it.copy(media = on) }

    fun nextStep() {
        if (scratch.value.step == SetupStep.FEATURES) applyFeatures()
        val order = reachableSteps()
        val next = order.getOrNull(order.indexOf(scratch.value.step) + 1)
        scratch.update { it.copy(step = next ?: it.step, message = null) }
        saveStep()
    }

    // only a step the user moved to is saved: resetWizard runs when a screen closes, and the ECHO
    // being closed must not wind the other back to the start
    private fun saveStep() {
        val step = scratch.value.step
        viewModelScope.launch { setupProgress.save(step) }
    }

    private fun applyFeatures() {
        val (gaming, media) = scratch.value.let { it.gaming to it.media }
        val changes = featureColumnChanges(featuresOnCrossbar, gaming, media)
        featuresOnCrossbar = gaming to media
        if (changes.isEmpty()) return
        viewModelScope.launch { changes.forEach { (id, visible) -> categoryRepository.setVisible(id, visible) } }
    }

    fun previousStep(): Boolean {
        val order = reachableSteps()
        val idx = order.indexOf(scratch.value.step)
        if (idx <= 0) return false
        scratch.update { it.copy(step = order[idx - 1], message = null) }
        saveStep()
        return true
    }

    fun onStoragePicked(slot: StorageSlot, replacing: String?, uri: Uri) {
        if (slot == StorageSlot.ARTWORK) return onArtworkFolderPicked(uri)
        viewModelScope.launch { folderAccess.link(slot, replacing, uri) }
    }

    fun createStandardRomFolders() {
        val firstRoot = uiState.value.romRoots.firstOrNull()?.treeUri ?: return
        viewModelScope.launch {
            val result = standardRomFolders.createUnder(firstRoot)
            scratch.update {
                it.copy(
                    message = "Created ${result.created} console folder(s)" +
                        (if (result.existing > 0) " (${result.existing} already there)" else "") +
                        ". Copy your games into the matching folders.",
                )
            }
        }
    }

    private fun onArtworkFolderPicked(uri: Uri) {
        viewModelScope.launch {
            val message = when (val adopted = artworkFolderSetup.adopt(uri)) {
                is EchoAdopt.Linked -> listOfNotNull(artworkFolderSetup.describe(adopted.linked), adopted.note).joinToString(" ")
                is EchoAdopt.PickEcho -> {
                    scratch.update { it.copy(artworkPickAgain = adopted.start.toString()) }
                    adopted.message
                }
                EchoAdopt.Failed -> ArtworkFolderSetup.COULD_NOT_LINK
            }
            scratch.update { it.copy(message = message) }
        }
    }

    fun artworkPickAgainLaunched() = scratch.update { it.copy(artworkPickAgain = null) }

    fun linkRetroArch(uri: Uri) {
        viewModelScope.launch {
            scratch.update { it.copy(retroArchDetecting = true) }
            val outcome = retroArchSetup.link(uri)
            readRetroArchState(
                if (outcome.keptPrevious) NO_CORES_KEPT_PREVIOUS
                else "RetroArch linked — installed cores are now offered in Emulators.",
            )
        }
    }

    private suspend fun readRetroArchState(doneMessage: String? = null) {
        val inventory = retroArchSetup.inventory()
        scratch.update {
            it.copy(
                retroArchDetecting = false,
                retroArchLinked = inventory is CoreInventory.Verified || inventory is CoreInventory.EmptyTree,
                retroArchCoreCount = if (inventory is CoreInventory.Unlinked) null else inventory.coreFiles.size,
                message = doneMessage ?: it.message,
            )
        }
    }

    fun linkVitaFolder(uri: Uri) {
        viewModelScope.launch {
            vita3KLibrary.setUx0Folder(uri)
            scratch.update {
                it.copy(
                    message = "Vita3K data folder set. Installed titles can be scanned from the " +
                        "PS Vita system in Library Manager.",
                    vitaFolderName = rootDisplayName(uri.toString()),
                )
            }
        }
    }

    fun finishSetup() {
        viewModelScope.launch {
            context.echoDataStore.edit { it[KEY_INITIAL_SETUP_SEEN] = true }
        }
    }

    fun dismissMessage() = scratch.update { it.copy(message = null) }
}
