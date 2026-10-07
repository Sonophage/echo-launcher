package com.echo.feature.artwork.portable

import android.net.Uri
import com.echo.core.data.repository.EchoThemeStore
import com.echo.core.data.repository.SafeMedia
import com.echo.themekit.EchoThemeCodec
import com.echo.themekit.EchoThemeFolder
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

// keeps ECHO/Themes in step with ECHO's saved themes (owner, 2026-10-07): one folder per theme. A saved theme
// with no folder there is written out; a folder ECHO does not have, or one changed since ECHO wrote it, is read
// in, and the folder's name becomes the theme's name. Nothing in the folder is ever deleted.
@Singleton
class ThemeFolderSync @Inject constructor(
    private val library: PortableArtworkLibrary,
    private val store: EchoThemeStore,
) {
    data class ReadResult(val themes: Int, val rejected: List<String>)

    // writes each saved theme that has no folder yet; the number written
    suspend fun writeOut(tree: Uri): Int {
        val existing = library.dirsIn(tree, listOf(DIR_THEMES)).map { it.name.lowercase() }.toSet()
        var written = 0
        // ponytail: two saved themes with one folder name share it; the first written keeps it
        for (theme in store.themes.value.distinctBy { store.folderName(it).lowercase() }) {
            val folder = store.folderName(theme)
            if (folder.lowercase() in existing || folder.equals(TEMPLATE, ignoreCase = true)) continue
            val bundle = store.bundleFile(theme.id)?.let { runCatching { EchoThemeCodec.read(it) }.getOrNull() } ?: continue
            val ok = EchoThemeFolder.toFiles(bundle).all { (path, bytes) ->
                val parts = path.split('/')
                library.writeBytes(tree, listOf(DIR_THEMES, folder) + parts.dropLast(1), parts.last(), bytes)
            }
            if (ok) written++ else Timber.w("ECHO folder: could not write Themes/$folder")
        }
        return written
    }

    // reads in each theme folder that is new or changed
    suspend fun readIn(tree: Uri): ReadResult {
        var applied = 0
        val rejected = mutableListOf<String>()
        for (dir in library.dirsIn(tree, listOf(DIR_THEMES))) {
            if (dir.name.equals(TEMPLATE, ignoreCase = true)) continue
            val folder = library.readFolder(tree, listOf(DIR_THEMES, dir.name), SafeMedia.MAX_THEME_FILE_BYTES)
            if (folder == null) { rejected += "Themes/${dir.name} (larger than ECHO reads)"; continue }
            if (folder.files.keys.none { it.equals(EchoThemeFolder.MANIFEST, ignoreCase = true) }) continue
            val saved = store.themes.value.firstOrNull { store.folderName(it).equals(dir.name, ignoreCase = true) }
            if (!wanted(dir.name, folder, saved)) continue
            val bundle = EchoThemeFolder.toBundle(folder.files)
            if (bundle == null) { rejected += "Themes/${dir.name} (theme.json is not a theme ECHO can read)"; continue }
            val named = bundle.copy(manifest = bundle.manifest.copy(name = dir.name))
            val bytes = EchoThemeCodec.write(named)
            when (val result = store.importBundleDetailed(replacing = saved?.id) { bytes.inputStream() }) {
                is EchoThemeStore.ImportResult.Success -> applied++
                else -> rejected += "Themes/${dir.name} (${result::class.simpleName})"
            }
        }
        return ReadResult(applied, rejected)
    }

    private fun wanted(name: String, folder: PortableArtworkLibrary.FolderFiles, saved: EchoThemeStore.SavedTheme?): Boolean {
        val file = saved?.let { store.bundleFile(it.id) }
        return shouldReadThemeFolder(folder.newest, saved != null, store.dismissedAt(name), file?.lastModified()) {
            val current = runCatching { EchoThemeCodec.read(file!!) }.getOrNull()?.let(EchoThemeFolder::toFiles)
            current != null && sameFiles(current, folder.files)
        }
    }

    private fun sameFiles(a: Map<String, ByteArray>, b: Map<String, ByteArray>): Boolean =
        a.keys == b.keys && a.all { (k, v) -> v.contentEquals(b.getValue(k)) }

    companion object {
        // the example theme ECHO writes for people to copy; never read in as a theme
        const val TEMPLATE = "Template"
    }
}

// whether a theme folder is read in: when ECHO has no theme of its name, unless ECHO deleted that theme after the
// folder last changed; or when the folder changed after ECHO stored its theme and holds something different.
// ECHO's own write-out leaves the folder newer than the stored theme but the same, so it is not read back.
internal fun shouldReadThemeFolder(
    folderNewest: Long,
    saved: Boolean,
    dismissedAt: Long?,
    storedModified: Long?,
    sameAsStored: () -> Boolean,
): Boolean = when {
    !saved -> dismissedAt == null || folderNewest > dismissedAt
    storedModified == null -> true
    folderNewest <= storedModified -> false
    else -> !sameAsStored()
}
