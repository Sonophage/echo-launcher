package com.echo.core.data.repository

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

// deletes cached images no row points at any more. Only a file in the app's own files or cache folder is ever
// deleted, so a uri pointing at the owner's own media cannot take it with it
internal suspend fun deleteOrphanedThumbnails(
    context: Context,
    thumbnailUris: Collection<String>,
    stillReferenced: suspend (String) -> Boolean,
) {
    if (thumbnailUris.isEmpty()) return
    withContext(Dispatchers.IO) {
        val own = listOf(context.filesDir, context.cacheDir).map { it.canonicalPath + File.separator }
        var deleted = 0
        for (uriStr in thumbnailUris.distinct()) {
            runCatching {
                val uri = Uri.parse(uriStr)
                if (uri.scheme != "file") return@runCatching
                if (stillReferenced(uriStr)) return@runCatching
                val file = File(uri.path ?: return@runCatching).canonicalFile
                if (own.none { file.path.startsWith(it) }) return@runCatching
                if (file.delete()) deleted++
            }.onFailure { Timber.w(it, "Could not delete cached thumbnail") }
        }
        if (deleted > 0) Timber.i("Deleted $deleted orphaned cached thumbnail(s)")
    }
}
