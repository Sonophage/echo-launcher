package com.echo.feature.settings.viewmodel

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.DocumentsContract
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

enum class StorageSlot(val label: String, val candidates: List<String>) {
    GAMES("Games", listOf("Emulation/ROMs", "ROMs", "Emulation/Roms", "Games")),
    MUSIC("Music", listOf("Music")),
    VIDEO("Video", listOf("Movies", "Videos", "Video")),
    PHOTOS("Photos", listOf("Pictures", "DCIM", "Photos")),
    BOOKS("Books", listOf("Books", "Library", "Comics")),
    ARTWORK("Artwork", listOf("Emulation/PSPL", "Emulation/Artwork", "Artwork")),
}

internal fun suggestFolder(slot: StorageSlot, children: (String) -> List<String>?): String? =
    slot.candidates.firstNotNullOfOrNull { candidate ->
        candidate.split('/').fold<String, String?>("") { parent, segment ->
            parent ?: return@fold null
            val match = children(parent)?.firstOrNull { it.trim().equals(segment, ignoreCase = true) }
                ?: return@fold null
            if (parent.isEmpty()) match else "$parent/$match"
        }
    }

@Singleton
class StorageSuggestions @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun suggest(): Map<StorageSlot, Uri> {
        val volumes = volumes()
        return StorageSlot.entries.mapNotNull { slot ->
            volumes.firstNotNullOfOrNull { (id, dir) ->
                suggestFolder(slot) { path -> File(dir, path).list()?.filter { File(dir, "$path/$it").isDirectory } }
                    ?.let { slot to DocumentsContract.buildDocumentUri(EXTERNAL_STORAGE, "$id:$it") }
            }
        }.toMap()
    }

    private fun volumes(): List<Pair<String, File>> {
        val storage = context.getSystemService(StorageManager::class.java) ?: return emptyList()
        return storage.storageVolumes
            .sortedByDescending { it.isRemovable }
            .mapNotNull { volume ->
                val id = if (volume.isPrimary) "primary" else volume.uuid ?: return@mapNotNull null
                val dir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) volume.directory
                    else if (volume.isPrimary) Environment.getExternalStorageDirectory()
                    else File("/storage/$id")
                dir?.let { id to it }
            }
    }

    private companion object {
        const val EXTERNAL_STORAGE = "com.android.externalstorage.documents"
    }
}
