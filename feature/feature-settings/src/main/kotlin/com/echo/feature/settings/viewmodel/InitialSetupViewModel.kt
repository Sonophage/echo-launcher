package com.echo.feature.settings.viewmodel

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echo.core.data.datastore.echoDataStore
import com.echo.core.data.repository.FolderLinkStatus
import com.echo.core.data.repository.MediaRootKind
import com.echo.core.data.repository.MediaRootRepository
import com.echo.core.data.repository.CoreInventory
import com.echo.core.data.repository.RomRootRepository
import com.echo.core.data.repository.Vita3KLibrary
import com.echo.core.data.repository.SafGrants
import com.echo.feature.artwork.MetadataApiKeyProvider
import com.echo.feature.artwork.api.ArtworkImportManager
import com.echo.feature.artwork.api.IgdbApi
import com.echo.feature.artwork.api.ScreenScraperApi
import com.echo.feature.artwork.api.SgdbApiKeyProvider
import com.echo.feature.artwork.importer.DetectedImportSource
import com.echo.feature.artwork.portable.PortableArtworkLibrary
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SetupStep {
    WELCOME, PERMISSIONS, ROM_ROOTS, MUSIC, VIDEO, PHOTO, BOOKS, ARTWORK, SERVICES,
    VITA, RETROARCH, PERSONALIZE, FINISH,
}

@Immutable
data class ArtworkSourceUi(val label: String, val systems: Int)

@Immutable
data class InitialSetupUiState(
    val step: SetupStep = SetupStep.WELCOME,

    val retroArchInstalled: Boolean = false,

    val vita3KInstalled: Boolean = false,

    val romRoots: List<RootFolderRow> = emptyList(),
    val musicRoots: List<RootFolderRow> = emptyList(),
    val videoRoots: List<RootFolderRow> = emptyList(),
    val photoRoots: List<RootFolderRow> = emptyList(),
    val bookRoots: List<RootFolderRow> = emptyList(),

    val isHomeLauncher: Boolean = false,

    val artworkFolderName: String? = null,
    val artworkSources: List<ArtworkSourceUi> = emptyList(),

    val hasSgdb: Boolean = false,
    val hasTmdb: Boolean = false,
    val igdbClientId: String = "",

    val ssEnabled: Boolean = false,
    val ssUsername: String = "",

    val retroArchLinked: Boolean = false,
    val retroArchCoreCount: Int? = null,
    val retroArchDetecting: Boolean = false,

    val vitaFolderName: String? = null,
    val message: String? = null,

    val igdbStatus: String? = null,
    val ssStatus: String? = null,

    val autoFitCrossbarLayout: Boolean = false,
) {
    val hasIgdb: Boolean get() = igdbClientId.isNotBlank()

    private val reachableSteps: List<SetupStep>
        get() = SetupStep.entries.filter {
            (it != SetupStep.RETROARCH || retroArchInstalled) &&
                (it != SetupStep.VITA || vita3KInstalled)
        }

    val stepNumber: Int get() = (reachableSteps.indexOf(step) + 1).coerceAtLeast(1)

    val stepCount: Int get() = reachableSteps.size
    val hasScreenScraper: Boolean get() = ssUsername.isNotBlank()
    val anyFolderSet: Boolean get() =
        romRoots.isNotEmpty() || musicRoots.isNotEmpty() || videoRoots.isNotEmpty() ||
            photoRoots.isNotEmpty() || bookRoots.isNotEmpty() || artworkFolderName != null
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
    private val igdbApi: IgdbApi,
    private val screenScraperApi: ScreenScraperApi,
    private val wizardMediaScanRunner: com.echo.feature.settings.media.WizardMediaScanRunner,
    private val romRootScanRunner: RomRootScanRunner,
    private val standardRomFolders: StandardRomFolders,
    private val launcherShortcuts: com.echo.feature.appbar.LauncherShortcutRepository,
    private val tmdbKeys: com.echo.feature.artwork.api.TmdbApiKeyProvider,
    private val artworkFolderSetup: ArtworkFolderSetup,
) : ViewModel() {
    private val scratch = MutableStateFlow(InitialSetupUiState())

    private var detectedArtworkSources: List<DetectedImportSource> = emptyList()

    init {
        viewModelScope.launch {
            val ssEnabled = screenScraperApi.isEnabled()
            scratch.update {
                it.copy(
                    ssEnabled = ssEnabled,
                    retroArchInstalled = isRetroArchInstalled(),
                    vita3KInstalled = isVita3KInstalled(),
                )
            }
            readRetroArchState()
        }
        refreshGrants()
    }

    fun refreshGrants() {
        scratch.update {
            it.copy(
                isHomeLauncher = launcherShortcuts.isDefaultLauncher(),
            )
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
        doResetWizard()
    }

    private fun doResetWizard() = scratch.update {
        it.copy(
            step = SetupStep.WELCOME, message = null, igdbStatus = null, ssStatus = null,
            retroArchDetecting = false,
        )
    }

    private fun reachableSteps(): List<SetupStep> =
        SetupStep.entries.filter {
            (it != SetupStep.RETROARCH || scratch.value.retroArchInstalled) &&
                (it != SetupStep.VITA || scratch.value.vita3KInstalled)
        }

    fun nextStep() {
        val order = reachableSteps()
        val next = order.getOrNull(order.indexOf(scratch.value.step) + 1)
        scratch.update {
            it.copy(step = next ?: it.step, message = null, igdbStatus = null, ssStatus = null)
        }
    }

    fun previousStep(): Boolean {
        if (scratch.value.step == SetupStep.WELCOME) return false
        val order = reachableSteps()
        val idx = order.indexOf(scratch.value.step)
        if (idx <= 0) return false
        scratch.update {
            it.copy(
                step = order[idx - 1],
                message = null, igdbStatus = null, ssStatus = null,
            )
        }
        return true
    }

    fun addRomRoot(uri: Uri) {
        viewModelScope.launch {
            romRootRepository.persist(uri, writable = true)
            romRootRepository.add(uri.toString())

            romRootScanRunner.kickoff()
        }
    }

    fun removeRomRoot(treeUri: String) {
        viewModelScope.launch { romRootRepository.remove(treeUri) }
    }

    fun relinkRomRoot(oldTreeUri: String, newUri: Uri) {
        viewModelScope.launch {
            romRootRepository.persist(newUri, writable = true)
            romRootRepository.replace(oldTreeUri, newUri.toString())
            romRootScanRunner.kickoff()
        }
    }

    fun rescanRomRoots() = romRootScanRunner.kickoff()

    fun createStandardRomFolders() {
        val firstRoot = scratch.value.romRoots.firstOrNull()?.treeUri ?: return
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

    fun addMediaRoot(kind: MediaRootKind, uri: Uri) {
        viewModelScope.launch {
            mediaRootRepository.persist(uri)
            mediaRootRepository.add(kind, uri.toString())
            wizardMediaScanRunner.kickoff(kind)
        }
    }

    fun removeMediaRoot(kind: MediaRootKind, treeUri: String) {
        viewModelScope.launch {
            mediaRootRepository.remove(kind, treeUri)

            wizardMediaScanRunner.kickoff(kind)
        }
    }

    fun relinkMediaRoot(kind: MediaRootKind, oldTreeUri: String, newUri: Uri) {
        viewModelScope.launch {
            mediaRootRepository.persist(newUri)
            mediaRootRepository.replace(kind, oldTreeUri, newUri.toString())
            wizardMediaScanRunner.kickoff(kind)
        }
    }

    fun rescanMediaRoot(kind: MediaRootKind) = wizardMediaScanRunner.kickoff(kind)

    fun onArtworkFolderPicked(uri: Uri) {
        viewModelScope.launch {
            val linked = artworkFolderSetup.link(uri)
            if (linked == null) {
                scratch.update { it.copy(message = ArtworkFolderSetup.COULD_NOT_LINK) }
                return@launch
            }

            val sources = runCatching { artworkImportManager.detectSources() }.getOrDefault(emptyList())
            detectedArtworkSources = sources
            scratch.update {
                it.copy(
                    message = artworkFolderSetup.describe(linked),
                    artworkSources = sources.map { s -> ArtworkSourceUi(s.label, s.systems.size) },
                )
            }
        }
    }

    fun forgetArtworkFolder() {
        viewModelScope.launch {
            artworkImportManager.forgetFolder()
            detectedArtworkSources = emptyList()
            scratch.update {
                it.copy(
                    artworkSources = emptyList(),
                    message = "Artwork folder released — files on disk were not touched.",
                )
            }
        }
    }

    fun importArtworkNow() {
        val detected = detectedArtworkSources.firstOrNull()
        val label = scratch.value.artworkSources.firstOrNull()?.label
        if (detected == null) {
            scratch.update {
                it.copy(message = "Nothing to import yet — place files under the artwork folder's import/ directory.")
            }
            return
        }
        viewModelScope.launch {
            val plan = runCatching { artworkImportManager.buildPlan(detected) }.getOrNull()
            if (plan == null) {
                scratch.update { it.copy(message = "Could not read that import source.") }
                return@launch
            }
            if (plan.itemCount == 0) {
                scratch.update { it.copy(message = "Nothing to import — everything is already present.") }
                return@launch
            }
            artworkImportManager.startImport(plan, PortableArtworkLibrary.Transfer.COPY)
            scratch.update {
                it.copy(
                    message = "Importing \"${label ?: plan.sourceLabel}\" — progress shows in notifications; details land in Settings ▸ Artwork Import.",
                )
            }
        }
    }

    fun linkRetroArch(uri: Uri) {
        viewModelScope.launch {
            scratch.update { it.copy(retroArchDetecting = true) }
            retroArchSetup.link(uri)
            readRetroArchState("RetroArch linked — installed cores are now offered in Emulators.")
        }
    }

    fun redetectRetroArchCores() {
        if (!scratch.value.retroArchLinked) return
        viewModelScope.launch {
            scratch.update { it.copy(retroArchDetecting = true) }
            retroArchSetup.redetect()
            readRetroArchState("RetroArch cores re-checked.")
        }
    }

    fun unlinkRetroArch() {
        viewModelScope.launch {
            retroArchSetup.unlink()
            scratch.update {
                it.copy(
                    retroArchLinked = false,
                    retroArchCoreCount = null,
                    retroArchDetecting = false,
                    message = "RetroArch link removed — no RetroArch cores will be offered until you link again.",
                )
            }
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

    fun forgetVitaFolder() {
        viewModelScope.launch {
            vita3KLibrary.clear()
            scratch.update {
                it.copy(message = "Vita3K data folder released — files on disk were not touched.")
            }
        }
    }

    fun connectSgdb(apiKey: String) {
        if (apiKey.isBlank()) return
        viewModelScope.launch {
            val protection = sgdbKeys.saveKey(apiKey.trim())
            val message = ServiceConnectors.unprotectedWarning("SteamGridDB key", protection) ?: "SteamGridDB connected"
            scratch.update { it.copy(message = message) }
        }
    }

    fun connectTmdb(apiKey: String) {
        if (apiKey.isBlank()) return
        viewModelScope.launch {
            val protection = tmdbKeys.saveKey(apiKey.trim())
            val message = ServiceConnectors.unprotectedWarning("TMDB key", protection) ?: "TMDB connected"
            scratch.update { it.copy(message = message) }
        }
    }

    fun connectIgdb(clientId: String, clientSecret: String) {
        if (clientId.isBlank() || clientSecret.isBlank()) return
        viewModelScope.launch {
            val protection = metadataKeys.saveIgdbCredentials(clientId.trim(), clientSecret.trim())
            val message = ServiceConnectors.unprotectedWarning("IGDB client secret", protection) ?: "IGDB connected"
            scratch.update { it.copy(message = message, igdbStatus = null) }
        }
    }

    fun testIgdbCredentials(clientId: String, clientSecret: String) {
        viewModelScope.launch {
            scratch.update { it.copy(igdbStatus = "Testing…") }
            val status = ServiceConnectors.testIgdb(igdbApi, clientId, clientSecret)
            scratch.update { it.copy(igdbStatus = status) }
        }
    }

    fun dismissIgdbStatus() = scratch.update { it.copy(igdbStatus = null) }

    fun testSsCredentials(username: String, password: String) {
        viewModelScope.launch {
            scratch.update { it.copy(ssStatus = "Testing…") }
            val status = ServiceConnectors.testScreenScraper(screenScraperApi, username, password)
            scratch.update { it.copy(ssStatus = status) }
        }
    }

    fun dismissSsStatus() = scratch.update { it.copy(ssStatus = null) }

    fun toggleAutoFitCrossbarLayout(enabled: Boolean) {
        scratch.update { it.copy(autoFitCrossbarLayout = enabled) }
    }

    private suspend fun writeAutoFitPreset() {
        val target = ClassicCrossbarLayout.forWindow(context)
        context.echoDataStore.edit { prefs -> ClassicCrossbarLayout.write(prefs, target) }
    }

    fun finishSetup() {
        viewModelScope.launch {
            if (scratch.value.autoFitCrossbarLayout) {
                writeAutoFitPreset()
            }
            context.echoDataStore.edit { it[KEY_INITIAL_SETUP_SEEN] = true }
        }
    }

    fun connectScreenScraper(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) return
        viewModelScope.launch {
            val protection = metadataKeys.saveSsCredentials(username.trim(), password.trim())
            val message = ServiceConnectors.unprotectedWarning("ScreenScraper password", protection) ?: "ScreenScraper connected"
            scratch.update { it.copy(message = message, ssStatus = null) }
        }
    }

    fun dismissMessage() = scratch.update { it.copy(message = null) }
}
