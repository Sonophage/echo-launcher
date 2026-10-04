package com.echo.core.data.repository

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

data class MediaScannedEntry(
    val treeUri: String,
    val displayName: String,
    val itemCount: Int,
    val lastScannedAt: Long?,
)

data class MediaRootRow(
    val treeUri: String,
    val name: String,
    val linked: Boolean,

    val itemCount: Int?,
    val lastScannedAt: Long?,
) {
    val scanned: Boolean get() = lastScannedAt != null
}

fun mediaRootRows(
    roots: List<String>,
    persistedReadUris: Set<String>,
    scanned: List<MediaScannedEntry>,
    fallbackName: (String) -> String,
): List<MediaRootRow> {
    val byTreeUri = scanned.associateBy { it.treeUri }
    return roots.map { treeUri ->
        val entry = byTreeUri[treeUri]?.takeIf { it.lastScannedAt != null }
        MediaRootRow(
            treeUri = treeUri,
            name = entry?.displayName?.takeIf { it.isNotBlank() }
                ?: byTreeUri[treeUri]?.displayName?.takeIf { it.isNotBlank() }
                ?: fallbackName(treeUri),
            linked = SafGrants.linkStatus(treeUri, persistedReadUris) == FolderLinkStatus.LINKED,
            itemCount = entry?.itemCount,
            lastScannedAt = entry?.lastScannedAt,
        )
    }
}

fun mediaRootDisplayName(context: Context, treeUri: String, fallback: String): String =
    runCatching { DocumentFile.fromTreeUri(context, Uri.parse(treeUri))?.name }.getOrNull()
        ?.takeIf { it.isNotBlank() }
        ?: runCatching { Uri.parse(treeUri).lastPathSegment }.getOrNull()
            ?.substringAfterLast('/')?.substringAfterLast(':')?.takeIf { it.isNotBlank() }
        ?: fallback
