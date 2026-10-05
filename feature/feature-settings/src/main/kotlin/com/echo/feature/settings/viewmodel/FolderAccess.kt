package com.echo.feature.settings.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.compose.runtime.Immutable
import com.echo.core.data.repository.FolderLinkStatus
import com.echo.core.data.repository.MediaRootRepository
import com.echo.core.data.repository.RomRootRepository
import com.echo.core.data.repository.SafGrants
import com.echo.feature.artwork.api.ArtworkImportManager
import com.echo.feature.settings.media.WizardMediaScanRunner
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Immutable
data class FolderAccessRow(val slot: StorageSlot, val treeUri: String, val name: String, val granted: Boolean)

// owner, 2026-10-05: a folder ECHO reads but Android no longer lets it open is shown in Permissions and in
// Setup, so it can be granted again. Every folder of every kind, the ECHO folder last
internal fun folderAccessRows(roots: Map<StorageSlot, List<String>>, persisted: Set<String>): List<FolderAccessRow> =
    StorageSlot.entries.flatMap { slot ->
        roots[slot].orEmpty().map { uri ->
            FolderAccessRow(slot, uri, rootDisplayName(uri), SafGrants.linkStatus(uri, persisted) == FolderLinkStatus.LINKED)
        }
    }

// where the folder picker opens. A stored tree uri carries no document, so the picker ignored it and opened on
// the storage root, which Android refuses; the tree's own document opens it on the folder itself
fun pickerStartUri(uri: String): Uri {
    val parsed = Uri.parse(uri)
    return runCatching { DocumentsContract.buildDocumentUriUsingTree(parsed, DocumentsContract.getTreeDocumentId(parsed)) }.getOrDefault(parsed)
}

internal const val PICK_THE_ECHO_FOLDER = "Pick the ECHO folder itself to grant it again. To move it, use Setup's Storage step."

@Singleton
class FolderAccess @Inject constructor(
    @ApplicationContext private val context: Context,
    private val romRootRepository: RomRootRepository,
    private val mediaRootRepository: MediaRootRepository,
    private val artworkImportManager: ArtworkImportManager,
    private val romRootScanRunner: RomRootScanRunner,
    private val wizardMediaScanRunner: WizardMediaScanRunner,
) {
    // read fresh each time: Android does not announce a grant it has dropped
    suspend fun rows(): List<FolderAccessRow> {
        val roots = buildMap {
            put(StorageSlot.GAMES, romRootRepository.getAll())
            StorageSlot.entries.forEach { slot -> slot.mediaKind?.let { put(slot, mediaRootRepository.getAll(it)) } }
            put(StorageSlot.ARTWORK, listOfNotNull(artworkImportManager.folderTreeUri.first()))
        }
        return folderAccessRows(roots, SafGrants.persistedReadUris(context.contentResolver))
    }

    // a games or media folder picked in place of `replacing` (or added), then scanned
    suspend fun link(slot: StorageSlot, replacing: String?, uri: Uri) {
        val kind = slot.mediaKind
        if (kind == null) {
            romRootRepository.persist(uri, writable = true)
            if (replacing != null) romRootRepository.replace(replacing, uri.toString())
            else romRootRepository.add(uri.toString())
            romRootScanRunner.kickoff()
        } else {
            mediaRootRepository.persist(uri)
            if (replacing != null) mediaRootRepository.replace(kind, replacing, uri.toString())
            else mediaRootRepository.add(kind, uri.toString())
            wizardMediaScanRunner.kickoff(kind)
        }
    }

    // grants a lost folder again; returns a message when the pick could not be used
    suspend fun regrant(row: FolderAccessRow, uri: Uri): String? {
        if (row.slot != StorageSlot.ARTWORK) {
            link(row.slot, row.treeUri, uri)
            return null
        }
        // the artwork links are stored against the ECHO folder's path, so only that same folder is accepted here
        if (uri.toString() != row.treeUri) return PICK_THE_ECHO_FOLDER
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }.onFailure { Timber.w(it, "Could not grant the ECHO folder again") }
        return null
    }
}
