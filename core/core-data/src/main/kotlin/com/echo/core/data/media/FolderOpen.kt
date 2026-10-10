package com.echo.core.data.media

import android.content.Context
import android.content.Intent
import android.provider.DocumentsContract
import timber.log.Timber

// owner, 2026-10-10: a game's menu opens the folder its file is in, in the device's file manager
object FolderOpen {
    private const val EXTERNAL_STORAGE = "com.android.externalstorage.documents"

    // the storage provider's id for a folder: "/storage/emulated/0/a/b" is "primary:a/b", "/storage/6DBF-B253/a" is
    // "6DBF-B253:a"; null for a path outside shared storage
    fun documentId(folderPath: String): String? {
        val parts = folderPath.trim().trimEnd('/').removePrefix("/storage/").split('/')
        if (!folderPath.trim().startsWith("/storage/") || parts.isEmpty() || parts[0].isEmpty()) return null
        return if (parts[0] == "emulated") {
            if (parts.size < 2) null else "primary:" + parts.drop(2).joinToString("/")
        } else {
            parts[0] + ":" + parts.drop(1).joinToString("/")
        }
    }

    // opens the folder holding [filePath]; false when nothing could, so the caller shows the path instead
    fun open(context: Context, filePath: String): Boolean {
        val folder = filePath.substringBeforeLast('/', missingDelimiterValue = "").ifEmpty { return false }
        val id = documentId(folder) ?: return false
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(DocumentsContract.buildDocumentUri(EXTERNAL_STORAGE, id), DocumentsContract.Document.MIME_TYPE_DIR)
            // no read grant: ECHO holds none to pass on, and asking for one throws (seen on the Konker); the file
            // manager reads the folder with its own access
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        // Android's own Files app opens the folder itself; Solid Explorer took the same link to the card's root (seen
        // on the Konker), so Files is asked first and any other file manager only when Files is not there
        val tries = FILES_APPS.map { pkg -> Intent(intent).setPackage(pkg) } + intent
        val opened = tries.any { attempt -> runCatching { context.startActivity(attempt) }.isSuccess }
        if (!opened) Timber.w("Nothing could open the folder $folder")
        return opened
    }

    private val FILES_APPS = listOf("com.google.android.documentsui", "com.android.documentsui")
}
