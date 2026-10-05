package com.echo.feature.settings.viewmodel

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echo.core.data.datastore.echoDataStore
import com.echo.core.data.repository.CoreInventory
import com.echo.core.data.repository.FolderLinkStatus
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SetupStep { PERMISSIONS, STORAGE, EMULATORS, ACCOUNTS }

@Immutable
data class InitialSetupUiState(
    val step: SetupStep = SetupStep.PERMISSIONS,

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
        get() = reachableSetupSteps(retroArchInstalled, vita3KInstalled)

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

internal fun reachableSetupSteps(retroArchInstalled: Boolean, vita3KInstalled: Boolean): List<SetupStep> =
    SetupStep.entries.filter { it != SetupStep.EMULATORS || retroArchInstalled || vita3KInstalled }

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
) : ViewModel() {
    private val scratch = MutableStateFlow(InitialSetupUiState())

    init {
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
        scratch.update { it.copy(step = SetupStep.PERMISSIONS, message = null, retroArchDetecting = false) }
    }

    private fun reachableSteps(): List<SetupStep> =
        reachableSetupSteps(scratch.value.retroArchInstalled, scratch.value.vita3KInstalled)

    fun nextStep() {
        val order = reachableSteps()
        val next = order.getOrNull(order.indexOf(scratch.value.step) + 1)
        scratch.update { it.copy(step = next ?: it.step, message = null) }
    }

    fun previousStep(): Boolean {
        val order = reachableSteps()
        val idx = order.indexOf(scratch.value.step)
        if (idx <= 0) return false
        scratch.update { it.copy(step = order[idx - 1], message = null) }
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
                        "PS Vita Memory Card in Library Manager.",
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
