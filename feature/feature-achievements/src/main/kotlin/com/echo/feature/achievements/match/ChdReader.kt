package com.echo.feature.achievements.match

import com.echo.feature.achievements.match.DiscImage.SeekableSource
import java.util.zip.Inflater

class ChdUnsupportedCodec(codec: String) : Exception("Unsupported CHD codec: $codec")

class ChdReader private constructor(
    private val source: SeekableSource,
    val header: Header,
    private val rawMap: ByteArray,
) {
    class Header(
        val compressors: IntArray,
        val logicalBytes: Long,
        val mapOffset: Long,
        val metaOffset: Long,
        val hunkBytes: Int,
        val unitBytes: Int,
        val hunkCount: Int,
    ) {
        val compressed: Boolean get() = compressors[0] != 0
    }

    class HunkEntry(val compressionType: Int, val offset: Long, val length: Int)

    fun hunk(hunkNum: Int): HunkEntry {
        var n = hunkNum
        var hops = 0
        while (true) {
            require(n in 0 until header.hunkCount) { "CHD hunk index out of range" }
            val base = n * 12
            val type = rawMap[base].toInt() and 0xFF
            if (type != COMPRESSION_SELF) {
                return HunkEntry(type, u48(rawMap, base + 4), u24(rawMap, base + 1))
            }
            n = u48(rawMap, base + 4).toInt()
            require(hops++ < header.hunkCount) { "CHD self-reference cycle" }
        }
    }

    fun readAt(offset: Long, len: Int): ByteArray {
        val b = ByteArray(len)
        val n = source.readFully(offset, b, len)
        return if (n == len) b else b.copyOf(maxOf(n, 0))
    }

    private var cachedHunkNum = -1
    private var cachedHunk: ByteArray? = null

    fun readHunk(hunkNum: Int): ByteArray {
        cachedHunk?.let { if (hunkNum == cachedHunkNum) return it }
        val e = hunk(hunkNum)
        val hunkBytes = header.hunkBytes
        val dest = when {
            e.compressionType == COMPRESSION_NONE -> readAt(e.offset, hunkBytes).copyOf(hunkBytes)
            e.compressionType in 0..3 -> {
                val comp = readAt(e.offset, e.length)
                when (val codec = header.compressors[e.compressionType]) {
                    CODEC_CD_ZLIB -> decodeCdzl(comp, hunkBytes)
                    CODEC_CD_LZMA -> decodeCdlz(comp, hunkBytes)
                    CODEC_ZLIB -> inflateRaw(comp, 0, comp.size, hunkBytes)
                    CODEC_LZMA -> lzmaRaw(comp, 0, comp.size, hunkBytes)
                    else -> throw ChdUnsupportedCodec(fourcc(codec))
                }
            }
            else -> throw ChdUnsupportedCodec("map-type-${e.compressionType}")
        }
        cachedHunkNum = hunkNum
        cachedHunk = dest
        return dest
    }

    fun metadata(tag: Int, index: Int): String? {
        var offset = header.metaOffset
        var remaining = index
        var hops = 0

        while (offset != 0L && hops++ < MAX_METADATA_ENTRIES) {
            val h = readAt(offset, 16)
            if (h.size < 16) return null
            val metatag = be32(h, 0)
            val length = be32(h, 4) and 0xFFFFFF
            val next = be64(h, 8)
            if (metatag == tag) {
                if (remaining == 0) return String(readAt(offset + 16, length), Charsets.US_ASCII).trimEnd('\u0000')
                remaining--
            }
            offset = next
        }
        return null
    }

    private fun fourcc(v: Int): String = buildString {
        append((v ushr 24 and 0xFF).toChar()); append((v ushr 16 and 0xFF).toChar())
        append((v ushr 8 and 0xFF).toChar()); append((v and 0xFF).toChar())
    }

    fun close() = source.close()

    companion object {
        const val COMPRESSION_TYPE_0 = 0
        const val COMPRESSION_NONE = 4
        const val COMPRESSION_SELF = 5

        const val CODEC_NONE = 0
        val CODEC_ZLIB = tag('z', 'l', 'i', 'b')
        val CODEC_LZMA = tag('l', 'z', 'm', 'a')
        val CODEC_CD_ZLIB = tag('c', 'd', 'z', 'l')
        val CODEC_CD_LZMA = tag('c', 'd', 'l', 'z')

        const val CD_FRAME_SIZE = 2448
        const val CD_MAX_SECTOR_DATA = 2352

        val CDROM_TRACK_METADATA2_TAG = tag('C', 'H', 'T', '2')
        val CDROM_TRACK_METADATA_TAG = tag('C', 'H', 'T', 'R')

        val GDROM_TRACK_METADATA_TAG = tag('C', 'H', 'G', 'D')
        val GDROM_OLD_METADATA_TAG = tag('C', 'H', 'G', 'T')

        private const val MAX_HUNK_BYTES = 16 * 1024 * 1024
        private const val MAX_HUNK_COUNT = 2 * 1024 * 1024
        private const val MAX_MAP_BYTES = 64 * 1024 * 1024
        private const val MAX_METADATA_ENTRIES = 4096

        private const val V5_HEADER_SIZE = 124
        private const val COMPRESSION_RLE_SMALL = 7
        private const val COMPRESSION_RLE_LARGE = 8

        private fun tag(a: Char, b: Char, c: Char, d: Char): Int =
            (a.code shl 24) or (b.code shl 16) or (c.code shl 8) or d.code

        internal fun decodeCdzl(src: ByteArray, hunkBytes: Int): ByteArray =
            decodeCd(src, hunkBytes) { s, off, len, outLen -> inflateRaw(s, off, len, outLen) }

        internal fun decodeCdlz(src: ByteArray, hunkBytes: Int): ByteArray =
            decodeCd(src, hunkBytes) { s, off, len, outLen -> lzmaRaw(s, off, len, outLen) }

        internal fun decodeCd(
            src: ByteArray,
            hunkBytes: Int,
            base: (ByteArray, Int, Int, Int) -> ByteArray,
        ): ByteArray {
            val dest = ByteArray(hunkBytes)
            val frames = hunkBytes / CD_FRAME_SIZE
            val eccBytes = (frames + 7) / 8
            val complenBytes = if (hunkBytes < 65536) 2 else 3
            val headerBytes = eccBytes + complenBytes
            var complenBase = ((src[eccBytes].toInt() and 0xFF) shl 8) or (src[eccBytes + 1].toInt() and 0xFF)
            if (complenBytes > 2) complenBase = (complenBase shl 8) or (src[eccBytes + 2].toInt() and 0xFF)

            val sectorData = base(src, headerBytes, complenBase, frames * CD_MAX_SECTOR_DATA)
            for (f in 0 until frames) {
                System.arraycopy(sectorData, f * CD_MAX_SECTOR_DATA, dest, f * CD_FRAME_SIZE, CD_MAX_SECTOR_DATA)
            }
            return dest
        }

        internal fun inflateRaw(src: ByteArray, srcOff: Int, srcLen: Int, outLen: Int): ByteArray {
            val dest = ByteArray(outLen)
            val inflater = Inflater(true)
            try {
                inflater.setInput(src, srcOff, srcLen)
                var pos = 0
                while (pos < outLen && !inflater.finished()) {
                    val n = inflater.inflate(dest, pos, outLen - pos)
                    if (n == 0 && (inflater.needsInput() || inflater.needsDictionary())) break
                    pos += n
                }
            } finally {
                inflater.end()
            }
            return dest
        }

        internal fun lzmaRaw(src: ByteArray, srcOff: Int, srcLen: Int, outLen: Int): ByteArray {
            val dest = ByteArray(outLen)
            val dictSize = maxOf(outLen, 4096)
            org.tukaani.xz.LZMAInputStream(
                java.io.ByteArrayInputStream(src, srcOff, srcLen), outLen.toLong(), 0x5D.toByte(), dictSize,
            ).use { lzma ->
                var pos = 0
                while (pos < outLen) {
                    val n = lzma.read(dest, pos, outLen - pos)
                    if (n < 0) break
                    pos += n
                }
            }
            return dest
        }

        fun open(source: SeekableSource): ChdReader? {
            val head = readFully(source, 0, V5_HEADER_SIZE) ?: run { source.close(); return null }
            if (String(head, 0, 8, Charsets.US_ASCII) != "MComprHD") { source.close(); return null }
            val version = be32(head, 12)
            if (version != 5) { source.close(); return null }

            val compressors = IntArray(4) { be32(head, 16 + it * 4) }
            val hunkBytes = be32(head, 56)
            val logicalBytes = be64(head, 32)

            if (hunkBytes !in 1..MAX_HUNK_BYTES || logicalBytes < 0) { source.close(); return null }
            val hunkCountLong = (logicalBytes + hunkBytes - 1) / hunkBytes
            if (hunkCountLong !in 1..MAX_HUNK_COUNT.toLong()) { source.close(); return null }
            val hunkCount = hunkCountLong.toInt()
            val header = Header(
                compressors = compressors,
                logicalBytes = logicalBytes,
                mapOffset = be64(head, 40),
                metaOffset = be64(head, 48),
                hunkBytes = hunkBytes,
                unitBytes = be32(head, 60),
                hunkCount = hunkCount,
            )

            val rawMap = runCatching { readMap(source, header) }.getOrNull()
                ?: run { source.close(); return null }
            return ChdReader(source, header, rawMap)
        }

        private fun readMap(source: SeekableSource, h: Header): ByteArray {
            if (!h.compressed) {
                val flat = readFully(source, h.mapOffset, h.hunkCount * 4) ?: error("map read")
                val map = ByteArray(h.hunkCount * 12)
                for (i in 0 until h.hunkCount) {
                    val unitOffset = be32(flat, i * 4).toLong() and 0xFFFFFFFFL
                    val byteOffset = unitOffset * h.hunkBytes
                    map[i * 12] = COMPRESSION_NONE.toByte()
                    putU24(map, i * 12 + 1, h.hunkBytes)
                    putU48(map, i * 12 + 4, byteOffset)
                }
                return map
            }

            val mapHead = readFully(source, h.mapOffset, 16) ?: error("map header")
            val mapBytes = be32(mapHead, 0)
            if (mapBytes !in 0..MAX_MAP_BYTES) error("CHD compressed map too large")
            val firstOffs = u48(mapHead, 4)
            val lengthBits = mapHead[12].toInt() and 0xFF
            val selfBits = mapHead[13].toInt() and 0xFF
            val parentBits = mapHead[14].toInt() and 0xFF

            val compressed = readFully(source, h.mapOffset + 16, mapBytes) ?: error("map body")
            val bits = ChdBitReader(compressed)
            val decoder = ChdHuffman(16, 8)
            if (!decoder.importTreeRle(bits)) error("map huffman tree")

            val map = ByteArray(h.hunkCount * 12)

            var lastComp = 0
            var repcount = 0
            for (hunk in 0 until h.hunkCount) {
                if (repcount > 0) {
                    map[hunk * 12] = lastComp.toByte(); repcount--
                } else {
                    val v = decoder.decodeOne(bits)
                    when (v) {
                        COMPRESSION_RLE_SMALL -> { map[hunk * 12] = lastComp.toByte(); repcount = 2 + decoder.decodeOne(bits) }
                        COMPRESSION_RLE_LARGE -> {
                            map[hunk * 12] = lastComp.toByte()
                            repcount = 2 + 16 + (decoder.decodeOne(bits) shl 4) + decoder.decodeOne(bits)
                        }
                        else -> { lastComp = v; map[hunk * 12] = v.toByte() }
                    }
                }
            }

            var curOffset = firstOffs
            var lastSelf = 0L
            for (hunk in 0 until h.hunkCount) {
                val base = hunk * 12
                var offset = curOffset
                var length = 0
                when (map[base].toInt() and 0xFF) {
                    COMPRESSION_TYPE_0, 1, 2, 3 -> {
                        length = bits.read(lengthBits)
                        curOffset += length
                        bits.read(16)
                    }
                    COMPRESSION_NONE -> {
                        length = h.hunkBytes
                        curOffset += length
                        bits.read(16)
                    }
                    COMPRESSION_SELF -> {
                        offset = bits.read(selfBits).toLong()
                        lastSelf = offset
                    }

                    else -> { bits.read(parentBits) }
                }
                putU24(map, base + 1, length)
                putU48(map, base + 4, offset)
            }
            return map
        }

        private fun readFully(source: SeekableSource, offset: Long, len: Int): ByteArray? {
            val b = ByteArray(len)
            return if (source.readFully(offset, b, len) == len) b else null
        }

        private fun be32(b: ByteArray, o: Int): Int =
            ((b[o].toInt() and 0xFF) shl 24) or ((b[o + 1].toInt() and 0xFF) shl 16) or
                ((b[o + 2].toInt() and 0xFF) shl 8) or (b[o + 3].toInt() and 0xFF)

        private fun be64(b: ByteArray, o: Int): Long =
            (be32(b, o).toLong() and 0xFFFFFFFFL shl 32) or (be32(b, o + 4).toLong() and 0xFFFFFFFFL)

        private fun u24(b: ByteArray, o: Int): Int =
            ((b[o].toInt() and 0xFF) shl 16) or ((b[o + 1].toInt() and 0xFF) shl 8) or (b[o + 2].toInt() and 0xFF)

        private fun u48(b: ByteArray, o: Int): Long {
            var v = 0L
            for (i in 0 until 6) v = (v shl 8) or (b[o + i].toLong() and 0xFF)
            return v
        }

        private fun putU24(b: ByteArray, o: Int, v: Int) {
            b[o] = (v ushr 16).toByte(); b[o + 1] = (v ushr 8).toByte(); b[o + 2] = v.toByte()
        }

        private fun putU48(b: ByteArray, o: Int, v: Long) {
            for (i in 0 until 6) b[o + i] = (v ushr (8 * (5 - i))).toByte()
        }
    }
}
