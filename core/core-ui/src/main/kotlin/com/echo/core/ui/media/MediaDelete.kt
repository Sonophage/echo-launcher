package com.echo.core.ui.media

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.echo.core.ui.notification.SystemToasts
import com.echo.core.ui.notification.ToastKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// owner, 2026-10-09: Delete on a song, video, photo or book removes the file from the device, then ECHO's row; it was
// "Remove From Library", which left the file and let a rescan bring it back. The menu row confirms first, and the
// row stays when the file could not be deleted. ECHO's deletes bypass Android's trash
suspend fun deleteFromDevice(uri: String?, deleteFile: suspend (String) -> Boolean, removeRow: suspend () -> Unit): Boolean {
    val gone = uri != null && deleteFile(uri)
    if (gone) removeRow()
    return gone
}

// a document under a folder ECHO was granted goes through the document provider; any other content uri through
// the resolver, which deletes what the provider lets ECHO delete
suspend fun deleteMediaFile(context: Context, uri: String): Boolean = withContext(Dispatchers.IO) {
    runCatching {
        val parsed = Uri.parse(uri)
        if (DocumentsContract.isDocumentUri(context, parsed)) DocumentsContract.deleteDocument(context.contentResolver, parsed)
        else context.contentResolver.delete(parsed, null, null) > 0
    }.getOrDefault(false)
}

fun postDeleteResult(deleted: Boolean, title: String) = SystemToasts.post(
    if (deleted) "Deleted" else "Couldn't delete",
    // a folder added before 2.13.5 was granted read only; relinking it grants write
    if (deleted) title else "$title: relink its folder to allow deleting, or delete it in Files",
    if (deleted) ToastKind.SUCCESS else ToastKind.ERROR,
)
