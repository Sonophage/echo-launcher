package com.psplauncher.feature.achievements.match

import java.io.Closeable
import java.io.File
import java.io.RandomAccessFile
import java.security.MessageDigest

class DiscImage private constructor(
    private val sectors: SectorSource,

    val firstTrackSector: Int,
) : Closeable {
    data class Entry(val lba: Int, val size: Int)

    fun readSector(lba: Int, count: Int): ByteArray = sectors.readSector(lba, count)

    fun findFile(pathIn: String): Entry? {
        var path = pathIn.trimStart('\\')
        val slash = path.lastIndexOf('\\')

        var sector: Int
        var dirSectors: Int
        if (slash >= 0) {
            val parent = findFile(path.substring(0, slash)) ?: return null
            path = path.substring(slash + 1)
            sector = parent.lba
            dirSectors = 1
        } else {
            val pvd = readSector(firstTrackSector + 16, 256)
            if (pvd.size < 170) return null
            sector = u24(pvd, 156 + 2)
            val blockSize = pvd[128].u() or (pvd[129].u() shl 8)
            dirSectors = if (blockSize == 0) 1 else u32(pvd, 156 + 10) / blockSize
        }

        val nameLen = path.length
        var buf = readSector(sector, 2048)
        var i = 0
        while (true) {
            if (i >= buf.size || buf[i].toInt() == 0) {
                if (dirSectors > 1) {
                    dirSectors--
                    buf = readSector(++sector, 2048)
                    if (buf.isEmpty()) break
                    i = 0
                    continue
                }
                break
            }
            val recLen = buf[i].u()
            val idLen = buf[i + 32].u()
            val afterName = i + 33 + nameLen
            val boundaryOk = idLen == nameLen || (afterName < buf.size && buf[afterName].toInt().toChar() == ';')
            if (boundaryOk && i + 33 + nameLen <= buf.size && regionMatchesCi(buf, i + 33, path)) {
                return Entry(lba = u24(buf, i + 2), size = u32(buf, i + 10))
            }
            if (recLen == 0) break
            i += recLen
        }
        return null
    }

    fun hashFileInto(md5: MessageDigest, lba: Int, size: Int) {
        var remaining = size
        var sector = lba
        while (remaining > 0) {
            val want = minOf(remaining, 2048)
            val chunk = readSector(sector, want)
            if (chunk.isEmpty()) break
            md5.update(chunk, 0, chunk.size)
            remaining -= chunk.size
            if (chunk.size < want) break
            sector++
        }
    }

    override fun close() = sectors.close()

    private fun Byte.u(): Int = toInt() and 0xFF
    private fun u24(b: ByteArray, o: Int) = b[o].u() or (b[o + 1].u() shl 8) or (b[o + 2].u() shl 16)
    private fun u32(b: ByteArray, o: Int) =
        b[o].u() or (b[o + 1].u() shl 8) or (b[o + 2].u() shl 16) or (b[o + 3].u() shl 24)

    private fun regionMatchesCi(b: ByteArray, off: Int, s: String): Boolean {
        for (k in s.indices) {
            if ((b[off + k].toInt().toChar()).uppercaseChar() != s[k].uppercaseChar()) return false
        }
        return true
    }

    interface SeekableSource : Closeable {
        fun readFully(offset: Long, dest: ByteArray, len: Int): Int
    }

    interface SectorSource : Closeable {
        fun readSector(lba: Int, count: Int): ByteArray
    }

    private class LinearSectors(
        private val source: SeekableSource,
        private val sectorSize: Int,
        private val dataOffset: Int,
    ) : SectorSource {
        override fun readSector(lba: Int, count: Int): ByteArray {
            val out = ByteArray(count)
            val read = source.readFully(lba.toLong() * sectorSize + dataOffset, out, count)
            return if (read == count) out else out.copyOf(maxOf(read, 0))
        }
        override fun close() = source.close()
    }

    private class FileSource(private val raf: RandomAccessFile) : SeekableSource {
        override fun readFully(offset: Long, dest: ByteArray, len: Int): Int {
            raf.seek(offset)
            var total = 0
            while (total < len) {
                val n = raf.read(dest, total, len - total)
                if (n < 0) break
                total += n
            }
            return total
        }
        override fun close() = raf.close()
    }

    companion object {
        fun rawSource(file: File): SeekableSource = FileSource(RandomAccessFile(file, "r"))

        fun open(file: File): DiscImage? =
            open(FileSource(RandomAccessFile(file, "r")))

        fun open(source: SeekableSource): DiscImage? {
            val layout = detectLayout(source)
            if (layout == null) { source.close(); return null }
            return DiscImage(LinearSectors(source, layout.first, layout.second), firstTrackSector = 0)
        }

        fun openRawCd(source: SeekableSource): DiscImage {
            val layout = detectRawLayout(source)
            return DiscImage(LinearSectors(source, layout.first, layout.second), firstTrackSector = 0)
        }

        fun openTracks(sectors: SectorSource, firstTrackSector: Int): DiscImage =
            DiscImage(sectors, firstTrackSector)

        private fun detectRawLayout(source: SeekableSource): Pair<Int, Int> {
            val head = ByteArray(16)
            if (source.readFully(0, head, 16) < 16) return 2048 to 0
            val isRaw = head[0].toInt() == 0 && head[11].toInt() == 0 &&
                (1..10).all { head[it].toInt() and 0xFF == 0xFF }
            if (!isRaw) return 2048 to 0
            return if ((head[15].toInt() and 0xFF) == 2) 2352 to 24 else 2352 to 16
        }

        private fun detectLayout(source: SeekableSource): Pair<Int, Int>? {
            fun cd001At(offset: Long): Boolean {
                val b = ByteArray(5)
                return source.readFully(offset, b, 5) == 5 &&
                    b[0].toInt() == 'C'.code && b[1].toInt() == 'D'.code && b[2].toInt() == '0'.code &&
                    b[3].toInt() == '0'.code && b[4].toInt() == '1'.code
            }
            return when {
                cd001At(16L * 2048 + 1) -> 2048 to 0
                cd001At(16L * 2352 + 16 + 1) -> 2352 to 16
                cd001At(16L * 2352 + 24 + 1) -> 2352 to 24
                else -> null
            }
        }
    }
}
