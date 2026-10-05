package com.echo.feature.settings.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echo.core.data.repository.FolderLinkStatus
import com.echo.core.data.repository.MediaRootKind
import com.echo.core.data.repository.MediaRootRepository
import com.echo.core.data.repository.SafGrants
import com.echo.feature.settings.media.WizardMediaScanRunner
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

internal val MediaRootKind.slot: StorageSlot
    get() = StorageSlot.entries.first { it.mediaKind == this }

// owner, 2026-10-05: the media folders get their own page in Settings; until now the crossbar's Folders rows
// were the only place to add or remove one
@HiltViewModel
class MediaLibrariesViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaRootRepository: MediaRootRepository,
    private val folderAccess: FolderAccess,
    private val scanRunner: WizardMediaScanRunner,
) : ViewModel() {
    // bumped on resume: Android does not announce a grant it drops or gives back
    private val readToken = MutableStateFlow(0)

    val libraries: StateFlow<Map<MediaRootKind, List<RootFolderRow>>> = combine(
        mediaRootRepository.roots(MediaRootKind.MUSIC),
        mediaRootRepository.roots(MediaRootKind.VIDEO),
        mediaRootRepository.roots(MediaRootKind.PHOTO),
        mediaRootRepository.roots(MediaRootKind.BOOK),
        readToken,
    ) { music, video, photo, book, _ ->
        val persisted = SafGrants.persistedReadUris(context.contentResolver)
        mapOf(MediaRootKind.MUSIC to music, MediaRootKind.VIDEO to video, MediaRootKind.PHOTO to photo, MediaRootKind.BOOK to book)
            .mapValues { (_, roots) ->
                roots.map { RootFolderRow(it, rootDisplayName(it), SafGrants.linkStatus(it, persisted) == FolderLinkStatus.LINKED) }
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun refresh() { readToken.value++ }

    fun add(kind: MediaRootKind, uri: Uri) {
        viewModelScope.launch { folderAccess.link(kind.slot, replacing = null, uri = uri) }
    }

    // the scan drops the removed folder's library rows, which is what empties its column on the crossbar
    fun remove(kind: MediaRootKind, treeUri: String) {
        viewModelScope.launch {
            mediaRootRepository.remove(kind, treeUri)
            scanRunner.kickoff(kind)
        }
    }

    fun rescan(kind: MediaRootKind) = scanRunner.kickoff(kind)
}
