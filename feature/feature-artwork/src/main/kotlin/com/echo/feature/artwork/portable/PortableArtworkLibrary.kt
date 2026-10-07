package com.echo.feature.artwork.portable

import android.content.Context
import android.net.Uri
import android.os.FileUtils
import android.provider.DocumentsContract
import com.echo.core.data.saf.SafChild
import com.echo.core.data.saf.querySafChildren
import com.echo.feature.artwork.store.ArtworkFileNaming
import com.echo.feature.artwork.store.ArtworkKind
import com.echo.feature.artwork.store.PayloadCheck
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PortableArtworkLibrary @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val resolver get() = context.contentResolver

    private val dirCache = ConcurrentHashMap<String, String>()

    private val dirCreateLock = Any()

    data class SavedAsset(val kind: ArtworkKind, val uriString: String, val fileName: String, val sizeBytes: Long)

    enum class Transfer { COPY, MOVE }

    suspend fun readManifest(treeUri: Uri): ArtworkLibraryManifest? = withContext(Dispatchers.IO) {
        val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
        val manifest = findChild(treeUri, rootDocId, ArtworkLibraryManifest.FILE_NAME) ?: return@withContext null
        readTextCapped(manifest.uri, ArtworkLibraryManifest.MAX_BYTES)?.let { ArtworkLibraryManifest.parse(it) }
    }

    suspend fun ensureLibrary(treeUri: Uri, appVersion: String): ArtworkLibraryManifest? = withContext(Dispatchers.IO) {
        readManifest(treeUri)?.let { return@withContext it }
        val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)

        ensureDir(treeUri, rootDocId, ArtworkLibraryManifest.DIR_IMPORT, ArtworkLibraryManifest.DIR_IMPORT)
            ?: return@withContext null
        ensureDir(treeUri, rootDocId, ArtworkLibraryManifest.DIR_ARTWORK, ArtworkLibraryManifest.DIR_ARTWORK)
            ?: return@withContext null
        val manifest = ArtworkLibraryManifest(
            libraryUuid = UUID.randomUUID().toString(),
            createdAt = System.currentTimeMillis(),
            appVersion = appVersion,
        )
        val ok = writeManifest(treeUri, manifest)
        if (ok) manifest else null
    }

    suspend fun writeManifest(treeUri: Uri, manifest: ArtworkLibraryManifest): Boolean =
        withContext(Dispatchers.IO) {
            val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
            writeText(
                treeUri, rootDocId, ArtworkLibraryManifest.FILE_NAME, "application/json",
                ArtworkLibraryManifest.encode(manifest),
            )
        }

    sealed interface IdentityIndexRead {
        data object Absent : IdentityIndexRead
        data class Loaded(val index: ArtworkIdentityIndex) : IdentityIndexRead
        data class Unreadable(val reason: String) : IdentityIndexRead
    }

    suspend fun readIdentityIndex(treeUri: Uri): IdentityIndexRead = withContext(Dispatchers.IO) {
        val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
        val file = findChild(treeUri, rootDocId, ArtworkIdentityIndex.FILE_NAME)
            ?: return@withContext IdentityIndexRead.Absent
        val text = readTextCapped(file.uri, ArtworkIdentityIndex.MAX_BYTES)
            ?: return@withContext IdentityIndexRead.Unreadable("IO error or oversized")
        val index = ArtworkIdentityIndex.parse(text)
            ?: return@withContext IdentityIndexRead.Unreadable("unparsable, or a newer format_version")
        IdentityIndexRead.Loaded(index)
    }

    suspend fun writeIdentityIndex(treeUri: Uri, index: ArtworkIdentityIndex): Boolean =
        withContext(Dispatchers.IO) {
            val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
            writeText(
                treeUri, rootDocId, ArtworkIdentityIndex.FILE_NAME, "application/json",
                ArtworkIdentityIndex.encode(index.copy(updatedAt = System.currentTimeMillis())),
            )
        }

    suspend fun listImportSources(treeUri: Uri): List<SafChild> = withContext(Dispatchers.IO) {
        val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
        val importDir = findChild(treeUri, rootDocId, ArtworkLibraryManifest.DIR_IMPORT)
            ?: return@withContext emptyList()
        resolver.querySafChildren(treeUri, importDir.documentId).filter { it.isDirectory }
    }

    fun listChildren(treeUri: Uri, dirDocId: String): List<SafChild> =
        resolver.querySafChildren(treeUri, dirDocId)

    suspend fun mediaDirDocId(treeUri: Uri, platformId: String, kind: ArtworkKind): String? =
        withContext(Dispatchers.IO) {
            val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
            val artworkDir = ArtworkLibraryManifest.DIR_ARTWORK
            val artworkDirId = ensureDir(treeUri, rootDocId, artworkDir, artworkDir)
                ?: return@withContext null
            var parentId = ensureDir(treeUri, artworkDirId, platformId, "$artworkDir/$platformId")
                ?: return@withContext null
            var path = "$artworkDir/$platformId"
            for (segment in ArtworkPathResolver.mediaDirFor(kind).split('/')) {
                path = "$path/$segment"
                parentId = ensureDir(treeUri, parentId, segment, path) ?: return@withContext null
            }
            parentId
        }

    suspend fun platformDirs(treeUri: Uri): List<SafChild> = withContext(Dispatchers.IO) {
        val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
        val out = mutableListOf<SafChild>()
        findChild(treeUri, rootDocId, ArtworkLibraryManifest.DIR_ARTWORK)
            ?.takeIf { it.isDirectory }
            ?.let { out += listChildren(treeUri, it.documentId).filter { c -> c.isDirectory } }
        out += legacyRootPlatformDirs(treeUri, rootDocId)
        out
    }

    suspend fun migrateRootPlatformsToArtwork(treeUri: Uri): Int = withContext(Dispatchers.IO) {
        val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
        val legacy = legacyRootPlatformDirs(treeUri, rootDocId)
        if (legacy.isEmpty()) return@withContext 0
        val artworkDir = ArtworkLibraryManifest.DIR_ARTWORK
        val artworkDirId = ensureDir(treeUri, rootDocId, artworkDir, artworkDir)
            ?: return@withContext 0
        val rootUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, rootDocId)
        val artworkDirUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, artworkDirId)
        var moved = 0
        for (dir in legacy) {
            val ok = runCatching {
                DocumentsContract.moveDocument(resolver, dir.uri, rootUri, artworkDirUri) != null
            }.getOrDefault(false)
            if (ok) moved++ else Timber.w("v2→v3: could not move '${dir.name}' under $artworkDir/")
        }
        if (moved > 0) clearDirCache()
        Timber.i("v2→v3 layout migration: $moved/${legacy.size} platform dirs moved under $artworkDir/")
        moved
    }

    private fun legacyRootPlatformDirs(treeUri: Uri, rootDocId: String): List<SafChild> =
        listChildren(treeUri, rootDocId).filter { child ->
            child.isDirectory &&
                !child.name.equals(ArtworkLibraryManifest.DIR_ARTWORK, ignoreCase = true) &&
                !child.name.equals(ArtworkLibraryManifest.DIR_IMPORT, ignoreCase = true) &&
                !child.name.equals(ArtworkLibraryManifest.DIR_GAMES, ignoreCase = true) &&
                // a theme named like a media folder must not carry Themes/ off into Artwork/
                !child.name.equals(DIR_THEMES, ignoreCase = true) &&
                listChildren(treeUri, child.documentId)
                    .any { it.isDirectory && ArtworkPathResolver.isMediaDirName(it.name) }
        }

    suspend fun saveAsset(
        treeUri: Uri,
        platformId: String,
        kind: ArtworkKind,
        portableName: String,
        source: SafChild,
        transfer: Transfer,
        existingNames: Map<String, SafChild>,
    ): SavedAsset? = withContext(Dispatchers.IO) {
        val header = readHeader(source.uri) ?: return@withContext null
        val ext = PayloadCheck.extFor(kind, header) ?: run {
            Timber.w("Import payload rejected — not a valid ${kind.name} file: ${source.name}")
            return@withContext null
        }
        val destName = "$portableName.$ext"

        val dirDocId = mediaDirDocId(treeUri, platformId, kind) ?: return@withContext null
        existingNames.values
            .filter { !it.isDirectory && it.name.substringBeforeLast('.').equals(portableName, ignoreCase = true) }
            .forEach { runCatching { DocumentsContract.deleteDocument(resolver, it.uri) } }

        val dirUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, dirDocId)
        val movedOrCopied: Uri? = when (transfer) {
            Transfer.MOVE -> moveInto(treeUri, source, dirUri, destName)
            Transfer.COPY -> copyInto(source, dirUri, destName, ext)
        }
        movedOrCopied?.let { SavedAsset(kind, it.toString(), destName, source.sizeBytes ?: 0L) }
    }

    suspend fun saveFromFile(
        treeUri: Uri,
        platformId: String,
        kind: ArtworkKind,
        portableName: String,
        tempFile: java.io.File,
    ): SavedAsset? = withContext(Dispatchers.IO) {
        try {
            val header = runCatching {
                tempFile.inputStream().use { s -> ByteArray(12).let { it.copyOf(s.read(it).coerceAtLeast(0)) } }
            }.getOrDefault(ByteArray(0))
            val ext = PayloadCheck.extFor(kind, header) ?: run {
                Timber.w("Portable save rejected — wrong payload for ${kind.name}")
                return@withContext null
            }
            val destName = "$portableName.$ext"
            val dirDocId = mediaDirDocId(treeUri, platformId, kind) ?: return@withContext null

            listChildren(treeUri, dirDocId)
                .filter { !it.isDirectory && it.name.substringBeforeLast('.').equals(portableName, ignoreCase = true) }
                .forEach { runCatching { DocumentsContract.deleteDocument(resolver, it.uri) } }

            val dirUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, dirDocId)
            val dest = runCatching { DocumentsContract.createDocument(resolver, dirUri, mimeForExt(ext), destName) }.getOrNull()
                ?: return@withContext null
            val ok = runCatching {
                tempFile.inputStream().use { input ->
                    resolver.openFileDescriptor(dest, "w")?.use { output ->
                        FileUtils.copy(input.fd, output.fileDescriptor)
                        true
                    }
                } ?: false
            }.onFailure { Timber.w(it, "Portable save failed for $destName") }.getOrDefault(false)
            if (!ok) {
                runCatching { DocumentsContract.deleteDocument(resolver, dest) }
                return@withContext null
            }
            SavedAsset(kind, dest.toString(), destName, tempFile.length())
        } finally {
            tempFile.delete()
        }
    }

    suspend fun relocateAsset(
        treeUri: Uri,
        platformId: String,
        fromKind: ArtworkKind,
        toKind: ArtworkKind,
        fileName: String,
    ): SavedAsset? = withContext(Dispatchers.IO) {
        val fromDirId = mediaDirDocId(treeUri, platformId, fromKind) ?: return@withContext null
        val source = findChild(treeUri, fromDirId, fileName)?.takeIf { !it.isDirectory }
            ?: return@withContext null
        val destDirId = mediaDirDocId(treeUri, platformId, toKind) ?: return@withContext null
        val stem = fileName.substringBeforeLast('.')
        listChildren(treeUri, destDirId)
            .filter { !it.isDirectory && it.name.substringBeforeLast('.').equals(stem, ignoreCase = true) }
            .forEach { runCatching { DocumentsContract.deleteDocument(resolver, it.uri) } }
        val destDirUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, destDirId)
        moveInto(treeUri, source, destDirUri, fileName)
            ?.let { SavedAsset(toKind, it.toString(), fileName, source.sizeBytes ?: 0L) }
    }

    fun clearDirCache() = dirCache.clear()

    data class NamespaceFile(val uriString: String, val fileName: String, val sizeBytes: Long)

    suspend fun saveTempIntoPath(
        treeUri: Uri,
        segments: List<String>,
        fileName: String,
        mime: String,
        tempFile: java.io.File,
        deleteTemp: Boolean,
    ): NamespaceFile? = withContext(Dispatchers.IO) {
        try {
            val dirDocId = ensureDirPath(treeUri, segments) ?: return@withContext null
            val stem = fileName.substringBeforeLast('.')
            listChildren(treeUri, dirDocId)
                .filter { !it.isDirectory && it.name.substringBeforeLast('.').equals(stem, ignoreCase = true) }
                .forEach { runCatching { DocumentsContract.deleteDocument(resolver, it.uri) } }
            val dirUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, dirDocId)
            val dest = runCatching { DocumentsContract.createDocument(resolver, dirUri, mime, fileName) }.getOrNull()
                ?: return@withContext null
            val ok = runCatching {
                tempFile.inputStream().use { input ->
                    resolver.openFileDescriptor(dest, "w")?.use { output ->
                        FileUtils.copy(input.fd, output.fileDescriptor)
                        true
                    }
                } ?: false
            }.onFailure { Timber.w(it, "Namespace save failed for $fileName") }.getOrDefault(false)
            if (!ok) {
                runCatching { DocumentsContract.deleteDocument(resolver, dest) }
                return@withContext null
            }
            NamespaceFile(dest.toString(), fileName, tempFile.length())
        } finally {
            if (deleteTemp) tempFile.delete()
        }
    }

    suspend fun findInPath(treeUri: Uri, segments: List<String>, portableName: String): SafChild? =
        withContext(Dispatchers.IO) {
            val dirDocId = resolveExistingPath(treeUri, segments) ?: return@withContext null
            listChildren(treeUri, dirDocId).firstOrNull {
                !it.isDirectory && it.name.substringBeforeLast('.').equals(portableName, ignoreCase = true)
            }
        }

    suspend fun copyUriToTemp(sourceUri: Uri, cacheDir: java.io.File, suffix: String): java.io.File? =
        withContext(Dispatchers.IO) {
            runCatching {
                val tmp = java.io.File.createTempFile("pfpns_", suffix, cacheDir)
                val ok = openSource(sourceUri)?.use { input ->
                    tmp.outputStream().use { out -> FileUtils.copy(input, out) }
                    true
                } ?: false
                if (ok && tmp.length() > 0) tmp else { tmp.delete(); null }
            }.onFailure { Timber.w(it, "copyUriToTemp failed for $sourceUri") }.getOrNull()
        }

    internal fun openSource(uri: Uri): java.io.InputStream? {
        val localPath = when {
            uri.scheme == null -> uri.toString()
            uri.scheme.equals("file", ignoreCase = true) -> uri.path
            else -> null
        }
        if (localPath != null) {
            return java.io.File(localPath).takeIf { it.isFile && it.length() > 0 }?.inputStream()
        }
        return resolver.openInputStream(uri)
    }

    suspend fun deleteUri(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching { DocumentsContract.deleteDocument(resolver, uri) }.getOrDefault(false)
    }

    private fun resolveExistingPath(treeUri: Uri, segments: List<String>): String? {
        var parent = DocumentsContract.getTreeDocumentId(treeUri)
        for (segment in segments) {
            parent = findChild(treeUri, parent, segment)?.takeIf { it.isDirectory }?.documentId ?: return null
        }
        return parent
    }

    suspend fun ensureDirPath(treeUri: Uri, segments: List<String>): String? = withContext(Dispatchers.IO) {
        var parent = DocumentsContract.getTreeDocumentId(treeUri)
        var path = ""
        for (segment in segments) {
            path = if (path.isEmpty()) segment else "$path/$segment"
            parent = ensureDir(treeUri, parent, segment, path) ?: return@withContext null
        }
        parent
    }

    // the ECHO folder beside the artwork (owner, 2026-10-04): Look/ for the look as files, and a README
    // that says what ECHO reads. Creates what is missing and never removes anything.
    suspend fun ensureEchoLayout(treeUri: Uri): Boolean = withContext(Dispatchers.IO) {
        val made = ECHO_LOOK_DIRS.all { ensureDirPath(treeUri, listOf(DIR_LOOK, it)) != null } &&
            ensureDirPath(treeUri, listOf(DIR_THEMES)) != null
        val root = DocumentsContract.getTreeDocumentId(treeUri)
        val readme = writeRootText(treeUri, ECHO_README, "text/plain", ECHO_README_TEXT)
        made && readme
    }

    // writes [text] to [name] at the folder's root when it differs from what is there
    suspend fun writeRootText(treeUri: Uri, name: String, mime: String, text: String): Boolean = withContext(Dispatchers.IO) {
        val root = DocumentsContract.getTreeDocumentId(treeUri)
        val current = findChild(treeUri, root, name)?.let { readTextCapped(it.uri, text.length + 1) }
        current == text || writeText(treeUri, root, name, mime, text)
    }

    // the text of [name] at the folder's root, up to [maxBytes]; null when it is missing or larger
    suspend fun readRootText(treeUri: Uri, name: String, maxBytes: Int): Pair<String, Long?>? = withContext(Dispatchers.IO) {
        val child = findChild(treeUri, DocumentsContract.getTreeDocumentId(treeUri), name) ?: return@withContext null
        readTextCapped(child.uri, maxBytes)?.let { it to child.lastModified }
    }

    // the files in [segments] under the folder, or none when the folder is not there
    suspend fun filesIn(treeUri: Uri, segments: List<String>): List<SafChild> = withContext(Dispatchers.IO) {
        val dir = resolveExistingPath(treeUri, segments) ?: return@withContext emptyList()
        listChildren(treeUri, dir).filter { !it.isDirectory }
    }

    // the folders in [segments] under the folder, or none when it is not there
    suspend fun dirsIn(treeUri: Uri, segments: List<String>): List<SafChild> = withContext(Dispatchers.IO) {
        val dir = resolveExistingPath(treeUri, segments) ?: return@withContext emptyList()
        listChildren(treeUri, dir).filter { it.isDirectory }
    }

    data class FolderFiles(val files: Map<String, ByteArray>, val newest: Long)

    // every file under [segments], by its path below it ('/'-separated, [maxDepth] folders deep at most),
    // with the newest change among them; null when the folder is missing, unreadable or over [maxBytes]
    suspend fun readFolder(treeUri: Uri, segments: List<String>, maxBytes: Long, maxDepth: Int = 3): FolderFiles? = withContext(Dispatchers.IO) {
        val root = resolveExistingPath(treeUri, segments) ?: return@withContext null
        val files = linkedMapOf<String, ByteArray>()
        var total = 0L
        var newest = 0L
        fun walk(docId: String, prefix: String, depth: Int): Boolean {
            for (child in listChildren(treeUri, docId)) {
                val path = prefix + child.name
                if (child.isDirectory) {
                    if (depth < maxDepth && !walk(child.documentId, "$path/", depth + 1)) return false
                    continue
                }
                // a provider may not report a size, so the bytes read are counted too
                if (total + (child.sizeBytes ?: 0L) > maxBytes) return false
                val bytes = resolver.openInputStream(child.uri)
                    ?.use { with(com.echo.core.data.repository.SafeMedia) { it.readCapped(maxBytes - total) } } ?: return false
                total += bytes.size
                files[path] = bytes
                newest = maxOf(newest, child.lastModified ?: 0L)
            }
            return true
        }
        runCatching { if (walk(root, "", 0)) FolderFiles(files, newest) else null }
            .onFailure { Timber.w(it, "Could not read ${segments.joinToString("/")}") }.getOrNull()
    }

    // writes [bytes] to [segments]/[name] unless the file there already holds them
    suspend fun writeBytesIfChanged(treeUri: Uri, segments: List<String>, name: String, bytes: ByteArray): Boolean = withContext(Dispatchers.IO) {
        val current = resolveExistingPath(treeUri, segments)?.let { findChild(treeUri, it, name) }
            ?.takeIf { it.sizeBytes == bytes.size.toLong() }
            ?.let { runCatching { resolver.openInputStream(it.uri)?.use { s -> s.readBytes() } }.getOrNull() }
        current?.contentEquals(bytes) == true || writeBytes(treeUri, segments, name, bytes)
    }

    // writes [bytes] to [segments]/[name], replacing a file of that name
    suspend fun writeBytes(treeUri: Uri, segments: List<String>, name: String, bytes: ByteArray): Boolean = withContext(Dispatchers.IO) {
        val dir = ensureDirPath(treeUri, segments) ?: return@withContext false
        val target = findChild(treeUri, dir, name)?.uri ?: runCatching {
            DocumentsContract.createDocument(resolver, DocumentsContract.buildDocumentUriUsingTree(treeUri, dir), "application/octet-stream", name)
        }.getOrNull() ?: return@withContext false
        runCatching { resolver.openOutputStream(target, "wt")?.use { it.write(bytes) } != null }
            .onFailure { Timber.w(it, "Could not write $name") }.getOrDefault(false)
    }

    // whether the folder's file holds the same bytes as [file]
    suspend fun sameContent(child: SafChild, file: java.io.File): Boolean = withContext(Dispatchers.IO) {
        if (!file.isFile || child.sizeBytes != file.length()) return@withContext false
        runCatching {
            resolver.openInputStream(child.uri)?.use { a -> file.inputStream().use { b -> digest(a).contentEquals(digest(b)) } } ?: false
        }.getOrDefault(false)
    }

    private fun digest(stream: java.io.InputStream): ByteArray {
        val md = java.security.MessageDigest.getInstance("SHA-256")
        val buf = ByteArray(64 * 1024)
        while (true) { val n = stream.read(buf); if (n < 0) break; md.update(buf, 0, n) }
        return md.digest()
    }

    // copies [source] to [segments]/[name] unless the copy there is current or newer (EchoFolder.shouldWrite)
    suspend fun mirrorFile(treeUri: Uri, segments: List<String>, name: String, source: java.io.File): Boolean = withContext(Dispatchers.IO) {
        val dir = ensureDirPath(treeUri, segments) ?: return@withContext false
        val existing = findChild(treeUri, dir, name)
        if (!com.echo.core.data.repository.EchoFolder.shouldWrite(source.lastModified(), source.length(), existing?.lastModified, existing?.sizeBytes)) {
            return@withContext true
        }
        val target = existing?.uri ?: runCatching {
            DocumentsContract.createDocument(resolver, DocumentsContract.buildDocumentUriUsingTree(treeUri, dir), "application/octet-stream", name)
        }.getOrNull() ?: return@withContext false
        runCatching {
            resolver.openOutputStream(target, "wt")?.use { out -> source.inputStream().use { it.copyTo(out) } } != null
        }.onFailure { Timber.w(it, "Could not mirror $name") }.getOrDefault(false)
    }

    suspend fun copyDocument(
        sourceUri: Uri,
        destTreeUri: Uri,
        destDirDocId: String,
        destName: String,
        mime: String,
    ): Boolean = withContext(Dispatchers.IO) {
        val destDirUri = DocumentsContract.buildDocumentUriUsingTree(destTreeUri, destDirDocId)
        val dest = runCatching { DocumentsContract.createDocument(resolver, destDirUri, mime, destName) }
            .getOrNull() ?: return@withContext false
        val ok = runCatching {
            resolver.openFileDescriptor(sourceUri, "r")?.use { input ->
                resolver.openFileDescriptor(dest, "w")?.use { output ->
                    FileUtils.copy(input.fileDescriptor, output.fileDescriptor)
                    true
                }
            } ?: false
        }.onFailure { Timber.w(it, "Export copy failed for $destName") }.getOrDefault(false)
        if (!ok) runCatching { DocumentsContract.deleteDocument(resolver, dest) }
        ok
    }

    data class MigratedAsset(
        val key: String,
        val platformId: String,
        val kind: ArtworkKind,
        val portableName: String,
        val fileName: String,
        val uriString: String,
        val sizeBytes: Long,
    )

    data class MigrationResult(val assets: List<MigratedAsset>, val entriesSkipped: Int)

    suspend fun migrateV1Library(treeUri: Uri): MigrationResult = withContext(Dispatchers.IO) {
        val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
        val gamesDir = findChild(treeUri, rootDocId, ArtworkLibraryManifest.DIR_GAMES)
            ?.takeIf { it.isDirectory }
            ?: return@withContext MigrationResult(emptyList(), 0)

        val out = mutableListOf<MigratedAsset>()
        var skipped = 0
        for (platformDir in listChildren(treeUri, gamesDir.documentId).filter { it.isDirectory }) {
            for (entryDir in listChildren(treeUri, platformDir.documentId).filter { it.isDirectory }) {
                val meta = readV1EntryMetadata(treeUri, entryDir.documentId)
                if (meta == null) { skipped++; continue }
                val portableName = meta.romFileName?.let { PortableNameResolver.fromRomFileName(it) }
                    ?: PortableNameResolver.fromTitle(meta.title.ifBlank { entryDir.name })

                var movedAll = true
                for (child in listChildren(treeUri, entryDir.documentId)) {
                    if (child.isDirectory) { movedAll = false; continue }
                    if (child.name.equals(ArtworkEntryMetadata.FILE_NAME, ignoreCase = true)) continue
                    val base = child.name.substringBeforeLast('.').lowercase(Locale.US)
                    val kind = ArtworkKind.entries.firstOrNull {
                        ArtworkFileNaming.baseName(it).lowercase(Locale.US) == base
                    }
                    if (kind == null) { movedAll = false; continue }
                    val ext = child.name.substringAfterLast('.', "").ifBlank { "jpg" }
                    val destDirId = mediaDirDocId(treeUri, meta.platformId, kind)
                    if (destDirId == null) { movedAll = false; continue }
                    val destDirUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, destDirId)
                    val moved = moveInto(treeUri, child, destDirUri, "$portableName.$ext")
                    if (moved == null) { movedAll = false; continue }
                    out += MigratedAsset(
                        key = meta.key,
                        platformId = meta.platformId,
                        kind = kind,
                        portableName = portableName,
                        fileName = "$portableName.$ext",
                        uriString = moved.toString(),
                        sizeBytes = child.sizeBytes ?: 0L,
                    )
                }
                if (movedAll) {
                    findChild(treeUri, entryDir.documentId, ArtworkEntryMetadata.FILE_NAME)?.let {
                        runCatching { DocumentsContract.deleteDocument(resolver, it.uri) }
                    }
                    deleteIfEmpty(treeUri, entryDir)
                }
            }
            deleteIfEmpty(treeUri, platformDir)
        }
        deleteIfEmpty(treeUri, gamesDir)
        Timber.i("v1→v2 migration: ${out.size} assets moved, $skipped entries skipped")
        MigrationResult(out, skipped)
    }

    private fun readV1EntryMetadata(treeUri: Uri, entryDirDocId: String): ArtworkEntryMetadata? {
        val child = findChild(treeUri, entryDirDocId, ArtworkEntryMetadata.FILE_NAME) ?: return null
        return readTextCapped(child.uri, ArtworkEntryMetadata.MAX_BYTES)?.let { ArtworkEntryMetadata.parse(it) }
    }

    private fun deleteIfEmpty(treeUri: Uri, dir: SafChild) {
        if (listChildren(treeUri, dir.documentId).isEmpty()) {
            runCatching { DocumentsContract.deleteDocument(resolver, dir.uri) }
        }
    }

    private fun moveInto(treeUri: Uri, source: SafChild, targetParentUri: Uri, destName: String): Uri? {
        val sourceParentUri = sourceParentUri(treeUri, source)
        if (sourceParentUri != null) {
            val moved = runCatching {
                DocumentsContract.moveDocument(resolver, source.uri, sourceParentUri, targetParentUri)
            }.getOrNull()
            if (moved != null) {
                val renamed = if (source.name.equals(destName, ignoreCase = true)) moved
                else runCatching { DocumentsContract.renameDocument(resolver, moved, destName) }.getOrNull()
                if (renamed != null) return renamed

                return moved
            }
        }
        val ext = destName.substringAfterLast('.')
        val copied = copyInto(source, targetParentUri, destName, ext) ?: return null
        runCatching { DocumentsContract.deleteDocument(resolver, source.uri) }
            .onFailure { Timber.w(it, "Moved by copy but could not delete source ${source.name}") }
        return copied
    }

    private fun copyInto(source: SafChild, targetParentUri: Uri, destName: String, ext: String): Uri? {
        val dest = runCatching {
            DocumentsContract.createDocument(resolver, targetParentUri, mimeForExt(ext), destName)
        }.getOrNull() ?: return null
        val ok = runCatching {
            resolver.openFileDescriptor(source.uri, "r")?.use { input ->
                resolver.openFileDescriptor(dest, "w")?.use { output ->

                    FileUtils.copy(input.fileDescriptor, output.fileDescriptor)
                    true
                }
            } ?: false
        }.onFailure { Timber.w(it, "Copy failed for ${source.name}") }.getOrDefault(false)
        if (!ok) {
            runCatching { DocumentsContract.deleteDocument(resolver, dest) }
            return null
        }
        return dest
    }

    private fun sourceParentUri(treeUri: Uri, source: SafChild): Uri? {
        val slash = source.documentId.lastIndexOf('/')
        if (slash <= 0) return null
        val parentId = source.documentId.substring(0, slash)
        return DocumentsContract.buildDocumentUriUsingTree(treeUri, parentId)
    }

    private fun mimeForExt(ext: String): String = when (ext.lowercase(Locale.ROOT)) {
        "png"  -> "image/png"
        "webp" -> "image/webp"
        "pdf"  -> "application/pdf"
        "mp4"  -> "video/mp4"
        "webm" -> "video/webm"
        else   -> "image/jpeg"
    }

    private fun findChild(treeUri: Uri, parentDocId: String, name: String): SafChild? =
        resolver.querySafChildren(treeUri, parentDocId).firstOrNull { it.name.equals(name, ignoreCase = true) }

    private fun ensureDir(treeUri: Uri, parentDocId: String, name: String, cachePath: String): String? = synchronized(dirCreateLock) {
        val cacheKey = "$treeUri|$cachePath"
        dirCache[cacheKey]?.let { return it }
        val existing = findChild(treeUri, parentDocId, name)
        val docId = when {
            existing != null && existing.isDirectory -> existing.documentId
            existing != null -> {
                Timber.w("Artwork library: '$name' exists but is not a directory")
                return null
            }
            else -> runCatching {
                val parentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, parentDocId)
                DocumentsContract.createDocument(
                    resolver, parentUri, DocumentsContract.Document.MIME_TYPE_DIR, name,
                )?.let { DocumentsContract.getDocumentId(it) }
            }.onFailure { Timber.w(it, "Could not create directory '$name'") }.getOrNull()
        } ?: return null
        dirCache[cacheKey] = docId
        return docId
    }

    private fun readHeader(uri: Uri): ByteArray? = runCatching {
        resolver.openInputStream(uri)?.use { stream ->
            val header = ByteArray(12)
            val read = stream.read(header)
            if (read <= 0) null else header.copyOf(read)
        }
    }.getOrNull()

    private fun readTextCapped(uri: Uri, maxBytes: Int): String? = runCatching {
        resolver.openInputStream(uri)?.use { stream ->
            val out = java.io.ByteArrayOutputStream()
            val chunk = ByteArray(8192)
            while (true) {
                val read = stream.read(chunk)
                if (read == -1) break
                if (out.size() + read > maxBytes) {
                    Timber.w("Refusing to parse oversized file at $uri")
                    return@use null
                }
                out.write(chunk, 0, read)
            }
            out.toString(Charsets.UTF_8.name())
        }
    }.getOrNull()

    private fun writeText(treeUri: Uri, parentDocId: String, name: String, mime: String, text: String): Boolean {
        val existing = findChild(treeUri, parentDocId, name)
        val target = existing?.uri ?: runCatching {
            val parentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, parentDocId)
            DocumentsContract.createDocument(resolver, parentUri, mime, name)
        }.getOrNull() ?: return false
        return runCatching {
            resolver.openOutputStream(target, "wt")?.use { it.write(text.toByteArray(Charsets.UTF_8)); true } ?: false
        }.onFailure { Timber.w(it, "Could not write $name") }.getOrDefault(false)
    }
}

const val DIR_LOOK = "Look"
const val DIR_THEMES = "Themes"
val ECHO_LOOK_DIRS = listOf("Icons", "Sounds", "Fonts", "Boot", "Wallpapers")
const val ECHO_README = "README.txt"

private val ECHO_README_TEXT = """
    ECHO

    This is ECHO's own folder. You can open and edit it with any file manager.

    Artwork/       Art for each game, one folder per console. ECHO reads this.
    Import/        Put other launchers' media here, then import it from ECHO's artwork settings.
    Look/          ECHO's look as files. ECHO copies its own here: Sounds (interface sounds and menu
                   music), Boot (boot, game boot and launch disc audio, boot animations), Wallpapers
                   and Icons (your custom icons). Put a .ttf or .otf font in Fonts to change ECHO's
                   font. A file you change here is kept: ECHO only copies over a file that is
                   missing or older than its own.
    Themes/        One folder per theme: theme.json (colours, wave, game start and button set), Icons
                   (console icons in Icons/Consoles), Wallpaper, Sounds, Boot and GameStart, and for
                   the theme store a README.md and a Preview folder (hero and screenshots). Every
                   part is optional. Template shows every file a theme takes: copy it, rename the
                   copy and fill it. ECHO writes each theme you save here, and reads in a theme folder
                   you add or change. A theme deleted in ECHO keeps its folder here; ECHO reads it
                   again only after you change it.
    settings.json  How ECHO looks and behaves: colours, wave, layout, controls and default players.
                   ECHO writes it when a setting changes.

    To use your changes, edit the files here, then choose Reload ECHO Folder in ECHO's artwork folder
    settings. ECHO applies settings.json; a sound in Look/Sounds or Look/Boot named after its slot
    (sound_back.mp3, boot_audio.mp3 and so on); an icon in Look/Icons named after its slot; the first
    font in Look/Fonts (remove it to go back to ECHO's own); and the newest picture or video (mp4,
    webm, gif) in Look/Wallpapers. A file is applied only when it is newer than ECHO's own and
    different from it; a file ECHO cannot use is named in the result. When ECHO starts it also applies
    what changed while it was closed. ECHO's background also becomes Android's home and lock wallpaper.

    No passwords, API keys, account details, folder paths or reading positions are kept here.
""".trimIndent() + "\n"
