package com.psplauncher.feature.achievements.match

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.psplauncher.core.data.saf.isSafeSiblingName
import com.psplauncher.core.data.saf.safSiblingDocumentId
import com.psplauncher.core.domain.model.Game
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiscImageOpener @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun open(game: Game): DiscImage? = withContext(Dispatchers.IO) {
        runCatching { openFromPath(game) ?: openFromUri(game) }
            .onFailure { Timber.w(it, "disc open failed for %s", game.displayTitle) }
            .getOrNull()
    }

    suspend fun openRawSource(game: Game): DiscImage.SeekableSource? = withContext(Dispatchers.IO) {
        runCatching { rawFromPath(game) ?: rawFromUri(game) }
            .onFailure { Timber.w(it, "raw disc open failed for %s", game.displayTitle) }
            .getOrNull()
    }

    private fun rawFromPath(game: Game): DiscImage.SeekableSource? {
        val path = game.romPath ?: return null
        val file = File(path).takeIf(File::exists) ?: return null
        return DiscImage.rawSource(file)
    }

    private fun rawFromUri(game: Game): DiscImage.SeekableSource? {
        val uri = game.romUri ?: return null
        val pfd = context.contentResolver.openFileDescriptor(Uri.parse(uri), "r") ?: return null
        return FdSource(pfd)
    }

    suspend fun openRawCd(game: Game): DiscImage? = withContext(Dispatchers.IO) {
        runCatching { rawCdFromPath(game) ?: rawCdFromUri(game) }
            .onFailure { Timber.w(it, "raw CD open failed for %s", game.displayTitle) }
            .getOrNull()
    }

    private fun rawCdFromPath(game: Game): DiscImage? {
        val path = game.romPath ?: return null
        val file = File(path).takeIf(File::exists) ?: return null
        val data = if (path.endsWith(".cue", ignoreCase = true)) cueBinFile(file) else file
        return data?.let { DiscImage.openRawCd(DiscImage.rawSource(it)) }
    }

    private fun rawCdFromUri(game: Game): DiscImage? {
        val uri = game.romUri ?: return null
        val target = if (uri.endsWith(".cue", ignoreCase = true)) {
            cueSiblingUri(Uri.parse(uri)) ?: return null
        } else {
            Uri.parse(uri)
        }
        val pfd = context.contentResolver.openFileDescriptor(target, "r") ?: return null
        return DiscImage.openRawCd(FdSource(pfd))
    }

    suspend fun openChd(game: Game): DiscImage? = withContext(Dispatchers.IO) {
        runCatching {
            val source = chdSource(game) ?: return@runCatching null
            val chd = ChdReader.open(source) ?: return@runCatching null
            val sectors = ChdSectorSource.of(chd) ?: run { chd.close(); return@runCatching null }
            DiscImage.openTracks(sectors, sectors.firstTrackSector)
        }.onFailure { Timber.w(it, "chd open failed for %s", game.displayTitle) }.getOrNull()
    }

    private fun chdSource(game: Game): DiscImage.SeekableSource? {
        game.romPath?.takeIf { it.endsWith(".chd", ignoreCase = true) }?.let { path ->
            return File(path).takeIf(File::exists)?.let { DiscImage.rawSource(it) }
        }
        game.romUri?.takeIf { it.endsWith(".chd", ignoreCase = true) }?.let { uri ->
            return context.contentResolver.openFileDescriptor(Uri.parse(uri), "r")?.let { FdSource(it) }
        }
        return null
    }

    private fun openFromPath(game: Game): DiscImage? {
        val path = game.romPath ?: return null
        val file = File(path).takeIf(File::exists) ?: return null
        val data = if (path.endsWith(".cue", ignoreCase = true)) cueBinFile(file) else file
        return data?.let { DiscImage.open(it) }
    }

    private fun cueBinFile(cue: File): File? {
        val line = cue.useLines { lines ->
            lines.map { it.trim() }.firstOrNull { it.startsWith("FILE", ignoreCase = true) }
        } ?: return null
        val name = cueFileName(line) ?: return null
        if (!isSafeSiblingName(name)) return null
        return File(cue.parentFile, name).takeIf(File::exists)
    }

    private fun cueFileName(fileLine: String): String? =
        Regex("""FILE\s+"([^"]+)"""", RegexOption.IGNORE_CASE).find(fileLine)?.groupValues?.get(1)
            ?: fileLine.removePrefix("FILE").trim().substringBefore(" BINARY").trim().trim('"')
                .takeIf { it.isNotEmpty() }

    private fun cueSiblingUri(cueUri: Uri): Uri? {
        val line = context.contentResolver.openInputStream(cueUri)?.bufferedReader()?.useLines { lines ->
            lines.map { it.trim() }.firstOrNull { it.startsWith("FILE", ignoreCase = true) }
        } ?: return null
        val name = cueFileName(line) ?: return null
        if (!isSafeSiblingName(name)) return null
        val docId = runCatching { android.provider.DocumentsContract.getDocumentId(cueUri) }.getOrNull()
            ?: return null
        val siblingId = safSiblingDocumentId(docId, name) ?: return null
        return runCatching {
            android.provider.DocumentsContract.buildDocumentUriUsingTree(cueUri, siblingId)
        }.getOrNull()
    }

    private fun openFromUri(game: Game): DiscImage? {
        val uri = game.romUri ?: return null
        val target = if (uri.endsWith(".cue", ignoreCase = true)) {
            cueSiblingUri(Uri.parse(uri)) ?: return null
        } else {
            Uri.parse(uri)
        }
        val pfd = context.contentResolver.openFileDescriptor(target, "r") ?: return null
        return DiscImage.open(FdSource(pfd))
    }

    suspend fun openGdi(game: Game): DiscImage? = withContext(Dispatchers.IO) {
        runCatching { openGdiFromPath(game) }
            .onFailure { Timber.w(it, "gdi open failed for %s", game.displayTitle) }
            .getOrNull()
    }

    private fun openGdiFromPath(game: Game): DiscImage? {
        val path = game.romPath ?: return null
        if (!path.endsWith(".gdi", ignoreCase = true)) return null
        val gdi = File(path).takeIf(File::exists) ?: return null
        val dir = gdi.parentFile ?: return null

        val lines = gdi.readLines().map { it.trim() }.filter { it.isNotEmpty() }
        val count = lines.firstOrNull()?.toIntOrNull() ?: return null

        val opened = mutableListOf<DiscImage.SeekableSource>()
        try {
            val tracks = mutableListOf<GdiTrackSource.Track>()
            for (line in lines.drop(1).take(count)) {
                val m = GDI_LINE.find(line) ?: return closeAll(opened)
                val sectorSize = m.groupValues[4].toInt()
                val name = m.groupValues[5].ifEmpty { m.groupValues[6] }

                if (!isSafeSiblingName(name)) return closeAll(opened)
                val fileOffset = m.groupValues[7].toLong()
                val file = File(dir, name).takeIf(File::exists) ?: return closeAll(opened)
                val src = DiscImage.rawSource(file).also { opened += it }
                val dataOffset = if (sectorSize == 2048) 0 else rawSectorDataOffset(src, fileOffset)
                val sectorCount = if (sectorSize <= 0) 0 else ((file.length() - fileOffset) / sectorSize).toInt()
                tracks += GdiTrackSource.Track(
                    number = m.groupValues[1].toInt(),
                    type = m.groupValues[3].toInt(),
                    startLba = m.groupValues[2].toInt(),
                    sectorCount = sectorCount,
                    sectorSize = sectorSize,
                    dataOffset = dataOffset,
                    fileOffset = fileOffset,
                    source = src,
                )
            }
            if (tracks.isEmpty()) return closeAll(opened)
            val source = GdiTrackSource(tracks)
            val ipStart = source.ipBinTrackStart() ?: run { source.close(); return null }
            return DiscImage.openTracks(source, ipStart)
        } catch (t: Throwable) {
            opened.forEach { runCatching { it.close() } }
            throw t
        }
    }

    private fun closeAll(sources: List<DiscImage.SeekableSource>): DiscImage? {
        sources.forEach { runCatching { it.close() } }
        return null
    }

    private fun rawSectorDataOffset(src: DiscImage.SeekableSource, fileOffset: Long): Int {
        val head = ByteArray(16)
        if (src.readFully(fileOffset, head, 16) < 16) return 16
        val isRaw = head[0].toInt() == 0 && head[11].toInt() == 0 &&
            (1..10).all { head[it].toInt() and 0xFF == 0xFF }
        if (!isRaw) return 0
        return if ((head[15].toInt() and 0xFF) == 2) 24 else 16
    }

    private class FdSource(private val pfd: ParcelFileDescriptor) : DiscImage.SeekableSource {
        private val channel = FileInputStream(pfd.fileDescriptor).channel
        override fun readFully(offset: Long, dest: ByteArray, len: Int): Int {
            val bb = ByteBuffer.wrap(dest, 0, len)
            var total = 0
            while (total < len) {
                val n = channel.read(bb, offset + total)
                if (n < 0) break
                total += n
            }
            return total
        }
        override fun close() {
            runCatching { channel.close() }
            runCatching { pfd.close() }
        }
    }

    private companion object {
        val GDI_LINE = Regex("""^(\d+)\s+(\d+)\s+(\d+)\s+(\d+)\s+(?:"([^"]+)"|(\S+))\s+(\d+)$""")
    }
}
