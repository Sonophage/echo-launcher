package com.psplauncher.feature.settings.viewmodel

import android.net.Uri
import com.psplauncher.core.data.platform.PlatformFolderHintResolver
import com.psplauncher.core.data.repository.CoreInventory
import com.psplauncher.core.data.repository.MemoryCardRepository
import com.psplauncher.core.data.repository.RetroArchLink
import com.psplauncher.feature.artwork.api.ArtworkImportManager
import com.psplauncher.feature.launcher.EmulatorAutoConfigService
import com.psplauncher.feature.library.scanner.FolderSetupResult
import com.psplauncher.feature.library.scanner.RomScanner
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RetroArchSetup @Inject constructor(
    private val retroArchLink: RetroArchLink,
    private val autoConfig: EmulatorAutoConfigService,
) {
    suspend fun inventory(): CoreInventory = retroArchLink.inventory()

    suspend fun link(treeUri: Uri): CoreInventory {
        retroArchLink.save(treeUri)
        return redetect()
    }

    suspend fun redetect(): CoreInventory {
        autoConfig.runOnStartup()
        return retroArchLink.inventory()
    }

    suspend fun unlink() {
        retroArchLink.clear()
        autoConfig.runOnStartup()
    }
}

@Singleton
class StandardRomFolders @Inject constructor(
    private val memoryCardRepository: MemoryCardRepository,
    private val folderHintResolver: PlatformFolderHintResolver,
    private val romScanner: RomScanner,
) {
    suspend fun names(): List<String> = memoryCardRepository.availablePlatformCatalog()
        .map { folderHintResolver.esDeFolderName(it.id) }
        .filter { it.isNotBlank() && it != "android" }
        .distinct()

    suspend fun createUnder(rootTreeUri: String): FolderSetupResult =
        romScanner.createSubfolders(rootTreeUri, names())
}

data class ArtworkFolderLinked(
    val existingLibrary: Boolean,
    val gamesLinked: Int,
    val entriesScanned: Int,
)

@Singleton
class ArtworkFolderSetup @Inject constructor(
    private val importManager: ArtworkImportManager,
) {
    suspend fun link(treeUri: Uri): ArtworkFolderLinked? {
        val linked = importManager.linkFolder(treeUri) ?: return null
        val scan = runCatching { importManager.relinkLibrary() }.getOrNull()
        return ArtworkFolderLinked(
            existingLibrary = linked.existingLibrary,
            gamesLinked = scan?.gamesLinked ?: 0,
            entriesScanned = scan?.entriesScanned ?: 0,
        )
    }

    fun describe(linked: ArtworkFolderLinked): String = buildString {
        append(if (linked.existingLibrary) "Existing artwork library reconnected." else "Artwork library created.")
        if (linked.gamesLinked > 0) {
            append(" ${linked.gamesLinked} game(s) linked from ${linked.entriesScanned} files already in the folder.")
        } else if (!linked.existingLibrary) {
            append(" Place other launchers' media under its import/ folder to gather it here.")
        }
    }

    companion object {
        const val COULD_NOT_LINK = "Could not set up an artwork library in that folder. Pick a writable folder."
    }
}
