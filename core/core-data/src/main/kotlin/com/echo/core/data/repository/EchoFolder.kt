package com.echo.core.data.repository

import java.net.URLDecoder

// ECHO's own folder on storage: artwork, and the look and settings a user can edit (owner, 2026-10-04)
object EchoFolder {
    const val NAME = "ECHO"

    private const val TREE = "/tree/"
    private const val DOCUMENT = "/document/"

    // whether a folder tree URI already points at a folder named ECHO
    fun isEchoTree(treeUri: String): Boolean =
        treeDocId(treeUri)?.let { URLDecoder.decode(it, "UTF-8").substringAfterLast('/').substringAfterLast(':') }
            .equals(NAME, ignoreCase = true)

    // a stored URI under [oldTree], pointed at the same file once the folder is [newTree]. Only the
    // folder's own path segment changes: a sibling such as PSPLauncher-Backups beside PSPL is left
    // alone, and a URI outside the folder comes back unchanged.
    fun repoint(uri: String?, oldTree: String, newTree: String): String? {
        if (uri == null) return null
        val oldId = treeDocId(oldTree) ?: return uri
        val newId = treeDocId(newTree) ?: return uri
        if (!uri.startsWith(oldTree.substringBefore(TREE) + TREE + oldId)) return uri
        return uri
            .replaceSegment(TREE + oldId, TREE + newId)
            .replaceSegment(DOCUMENT + oldId, DOCUMENT + newId)
    }

    private fun treeDocId(treeUri: String): String? =
        treeUri.substringAfter(TREE, "").substringBefore('/').takeIf { it.isNotEmpty() }

    // replaces [old] only where it ends the URI or a path segment (a "/" or an encoded "%2F" follows)
    private fun String.replaceSegment(old: String, new: String): String {
        val at = indexOf(old)
        if (at < 0) return this
        val rest = substring(at + old.length)
        val boundary = rest.isEmpty() || rest.startsWith("/") || rest.startsWith("%2F", ignoreCase = true)
        return if (boundary) substring(0, at) + new + rest else this
    }
}
