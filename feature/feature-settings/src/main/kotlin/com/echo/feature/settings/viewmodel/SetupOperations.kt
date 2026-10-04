package com.echo.feature.settings.viewmodel

import android.net.Uri
import com.echo.core.data.repository.EchoFolder
import com.echo.core.data.platform.PlatformFolderHintResolver
import com.echo.core.data.repository.CoreInventory
import com.echo.core.data.repository.MemoryCardRepository
import com.echo.core.data.repository.RetroArchLink
import com.echo.feature.artwork.api.ArtworkImportManager
import com.echo.feature.launcher.EmulatorAutoConfigService
import com.echo.feature.library.scanner.FolderSetupResult
import com.echo.feature.library.scanner.RomScanner
import javax.inject.Inject
import javax.inject.Singleton

data class RetroArchLinkOutcome(val inventory: CoreInventory, val keptPrevious: Boolean)

@Singleton
class RetroArchSetup @Inject constructor(
    private val retroArchLink: RetroArchLink,
    private val autoConfig: EmulatorAutoConfigService,
) {
    suspend fun inventory(): CoreInventory = retroArchLink.inventory()

    suspend fun link(treeUri: Uri): RetroArchLinkOutcome {
        val previous = retroArchLink.linkedTreeUri()
        retroArchLink.save(treeUri)
        val inventory = redetect()
        if (inventory !is CoreInventory.EmptyTree || previous == null || previous == treeUri.toString()) {
            return RetroArchLinkOutcome(inventory, keptPrevious = false)
        }
        retroArchLink.restore(previous)
        return RetroArchLinkOutcome(redetect(), keptPrevious = true)
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

sealed interface EchoAdopt {
    data class Linked(val linked: ArtworkFolderLinked, val note: String? = null) : EchoAdopt

    // the ECHO folder is ready; the picker opens at [start] for the user to confirm it
    data class PickEcho(val start: Uri, val message: String) : EchoAdopt

    data object Failed : EchoAdopt
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

    // owner, 2026-10-04: the artwork folder is ECHO's own folder, named ECHO. A folder already named
    // ECHO is linked; a folder holding an ECHO library is renamed; any other folder gets an ECHO folder
    // made inside it, so a general folder is never renamed. Android ties folder access to the name,
    // so after a rename or a new folder the user picks it once more.
    suspend fun adopt(picked: Uri): EchoAdopt = when {
        EchoFolder.isEchoTree(picked.toString()) -> link(picked)?.let { EchoAdopt.Linked(it) } ?: EchoAdopt.Failed
        importManager.holdsLibrary(picked) -> {
            val linked = link(picked)
            if (linked == null) EchoAdopt.Failed else renameLinked() ?: EchoAdopt.Linked(linked, RENAME_FAILED)
        }
        else -> importManager.echoFolderInside(picked)?.let { EchoAdopt.PickEcho(importManager.pickerStart(it), CREATED) }
            ?: EchoAdopt.Failed
    }

    // renames the linked folder to ECHO; null when it could not
    suspend fun renameLinked(): EchoAdopt.PickEcho? =
        importManager.renameFolderToEcho()?.let { EchoAdopt.PickEcho(importManager.pickerStart(it), RENAMED) }

    suspend fun folderIsEcho(): Boolean = importManager.folderIsEcho()

    companion object {
        const val COULD_NOT_LINK = "Could not set up an artwork library in that folder. Pick a writable folder."
        const val RENAMED = "Your artwork folder is now named ECHO. Tap Use this folder so ECHO keeps access to it."
        const val CREATED = "ECHO made a folder named ECHO there. Tap Use this folder to use it."
        const val RENAME_FAILED = "Linked, but the folder could not be renamed to ECHO. It keeps its name."
    }
}
