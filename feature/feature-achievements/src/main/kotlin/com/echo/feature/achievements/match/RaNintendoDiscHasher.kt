package com.echo.feature.achievements.match

import com.echo.feature.achievements.match.DiscImage.SeekableSource
import java.security.MessageDigest

object RaNintendoDiscHasher {
    private const val MAX_BUFFER_SIZE = 64 * 1024 * 1024
    private const val MAX_HEADER_SIZE = 1024 * 1024
    private const val MAX_CHUNK_SIZE = 1024 * 1024
    private const val BASE_HEADER_SIZE = 0x2440

    fun isSupported(platformId: String): Boolean = platformId == "gc" || platformId == "wii"

    fun hash(platformId: String, source: SeekableSource): String? = when (platformId) {
        "gc" -> hashGamecube(source)
        "wii" -> hashWii(source)
        else -> null
    }

    private fun hashGamecube(src: SeekableSource): String? {
        val magic = read(src, 0x1cL, 4)
        if (!magic.eq(0xC2, 0x33, 0x9F, 0x3D)) return null
        val md5 = MessageDigest.getInstance("MD5")
        if (!hashNintendoPartition(md5, src, partOffset = 0L, wiiShift = 0)) return null
        return hex(md5)
    }

    private fun hashWii(src: SeekableSource): String? {
        val discMagic = read(src, 0x18L, 4)
        if (discMagic.eq(0x5D, 0x1C, 0x9E, 0xA3)) {
            val md5 = MessageDigest.getInstance("MD5")
            return if (hashWiiDisc(md5, src)) hex(md5) else null
        }
        val wadMagic = read(src, 0x04L, 4)
        if (wadMagic.eq('I'.code, 's'.code, 0x00, 0x00)) {
            val md5 = MessageDigest.getInstance("MD5")
            return if (hashWiiware(md5, src)) hex(md5) else null
        }
        return null
    }

    private fun hashNintendoPartition(
        md5: MessageDigest,
        src: SeekableSource,
        partOffset: Long,
        wiiShift: Int,
    ): Boolean {
        val sizes = read(src, partOffset + BASE_HEADER_SIZE + 0x14, 8)
        if (sizes.size < 8) return false
        val apploaderBody = be32(sizes, 0)
        val apploaderTrailer = be32(sizes, 4)
        var headerSize = (BASE_HEADER_SIZE + 0x20 + apploaderBody + apploaderTrailer)
        if (headerSize > MAX_HEADER_SIZE) headerSize = MAX_HEADER_SIZE.toLong()

        val header = read(src, partOffset, headerSize.toInt())
        if (header.size < 0x424) return false
        md5.update(header, 0, header.size)

        val dolOffset = be32(header, 0x420) shl wiiShift

        val addr = read(src, partOffset + dolOffset, 0xD8)
        if (addr.size < 0xD8) return false
        for (ix in 0 until 18) {
            val segOffset = be32(addr, ix * 4) shl wiiShift
            val segSize = be32(addr, 0x90 + ix * 4) shl wiiShift
            if (segSize == 0L) continue
            hashChunked(md5, src, partOffset + segOffset, segSize)
        }
        return true
    }

    private fun hashWiiDisc(md5: MessageDigest, src: SeekableSource): Boolean {
        val MAIN_HEADER_SIZE = 0x80
        val REGION_CODE_ADDRESS = 0x4E000L
        val CLUSTER_SIZE = 0x7C00
        val MAX_CLUSTER_COUNT = 1024

        val encrypted = read(src, 0x61L, 1).let { it.isNotEmpty() && it[0].toInt() == 0 }

        md5.update(read(src, 0L, MAIN_HEADER_SIZE), 0, MAIN_HEADER_SIZE)
        md5.update(read(src, REGION_CODE_ADDRESS, 4), 0, 4)

        val infoTable = read(src, 0x40000L, 32)
        if (infoTable.size < 32) return false
        var totalPartitions = 0L
        val counts = IntArray(4)
        val tableOffsets = LongArray(4)
        for (g in 0 until 4) {
            counts[g] = be32(infoTable, g * 8).toInt()
            tableOffsets[g] = be32(infoTable, g * 8 + 4) shl 2
            totalPartitions += counts[g]
        }
        if (totalPartitions == 0L) return false

        val partOffsets = ArrayList<Long>()
        val partTypes = ArrayList<Long>()
        for (g in 0 until 4) {
            var pos = tableOffsets[g]
            for (i in 0 until counts[g]) {
                val entry = read(src, pos, 8)
                if (entry.size < 8) return false
                partOffsets.add(be32(entry, 0) shl 2)
                partTypes.add(be32(entry, 4))
                pos += 8
            }
        }

        for (j in partOffsets.indices) {
            if (partTypes[j] == 1L) continue
            val base = partOffsets[j]

            val tmdHeader = read(src, base + 0x2A4, 8)
            if (tmdHeader.size < 8) return false
            var tmdSize = be32(tmdHeader, 0)
            val tmdOffset = be32(tmdHeader, 4) shl 2
            if (tmdSize > CLUSTER_SIZE) tmdSize = CLUSTER_SIZE.toLong()
            md5.update(read(src, base + tmdOffset, tmdSize.toInt()), 0, tmdSize.toInt())

            val partHeader = read(src, base + 0x2B8, 8)
            if (partHeader.size < 8) return false
            val partDataOffset = be32(partHeader, 0) shl 2
            val partDataSize = be32(partHeader, 4) shl 2

            if (encrypted) {
                var clusters = partDataSize / 0x8000
                if (clusters > MAX_CLUSTER_COUNT) clusters = MAX_CLUSTER_COUNT.toLong()
                for (c in 0 until clusters) {
                    val cluster = read(src, partDataOffset + c * 0x8000 + 0x400, CLUSTER_SIZE)
                    md5.update(cluster, 0, cluster.size)
                }
            } else {
                if (!hashNintendoPartition(md5, src, partDataOffset, wiiShift = 2)) return false
            }
        }
        return true
    }

    private fun hashWiiware(md5: MessageDigest, src: SeekableSource): Boolean {
        val certChainSize = align64(be32(read(src, 0x08L, 4), 0))
        val ticketSize = align64(be32(read(src, 0x10L, 4), 0))
        var tmdSize = align64(be32(read(src, 0x14L, 4), 0))
        if (tmdSize > MAX_BUFFER_SIZE) tmdSize = MAX_BUFFER_SIZE.toLong()

        val tmdStart = 0x40L + certChainSize + ticketSize
        md5.update(read(src, tmdStart, tmdSize.toInt()), 0, tmdSize.toInt())

        val countBuf = read(src, tmdStart + 0x1de, 2)
        if (countBuf.size < 2) return false
        val contentCount = ((countBuf[0].toInt() and 0xFF) shl 8) or (countBuf[1].toInt() and 0xFF)

        var contentAddr = tmdStart + tmdSize
        for (ix in 0 until contentCount) {
            val sizeBuf = read(src, tmdStart + 0x1e4 + 8 + ix.toLong() * 0x24, 8)
            if (sizeBuf.size < 8) return false
            var contentSize = if (be32(sizeBuf, 0) == 0L) {
                (be32(sizeBuf, 4) + 0x0F) and 0x0F.inv().toLong()
            } else {
                MAX_BUFFER_SIZE.toLong()
            }
            val bufferSize = if (contentSize > MAX_BUFFER_SIZE) MAX_BUFFER_SIZE.toLong() else contentSize
            val content = read(src, contentAddr, bufferSize.toInt())
            md5.update(content, 0, content.size)
            contentAddr = align64(contentAddr + contentSize)
        }
        return true
    }

    private fun hashChunked(md5: MessageDigest, src: SeekableSource, start: Long, size: Long) {
        var remaining = size
        var pos = start
        while (remaining > 0) {
            val want = minOf(remaining, MAX_CHUNK_SIZE.toLong()).toInt()
            val buf = read(src, pos, want)
            if (buf.isEmpty()) break
            md5.update(buf, 0, buf.size)
            remaining -= buf.size
            pos += buf.size
            if (buf.size < want) break
        }
    }

    private fun read(src: SeekableSource, offset: Long, len: Int): ByteArray {
        val b = ByteArray(len)
        val n = src.readFully(offset, b, len)
        return if (n == len) b else b.copyOf(maxOf(n, 0))
    }

    private fun be32(b: ByteArray, o: Int): Long =
        ((b[o].toLong() and 0xFF) shl 24) or ((b[o + 1].toLong() and 0xFF) shl 16) or
            ((b[o + 2].toLong() and 0xFF) shl 8) or (b[o + 3].toLong() and 0xFF)

    private fun align64(v: Long): Long = (v + 0x3F) and 0x3F.inv().toLong()

    private fun ByteArray.eq(vararg bytes: Int): Boolean {
        if (size < bytes.size) return false
        for (i in bytes.indices) if ((this[i].toInt() and 0xFF) != (bytes[i] and 0xFF)) return false
        return true
    }

    private fun hex(md5: MessageDigest): String =
        md5.digest().joinToString("") { "%02x".format(it.toInt() and 0xFF) }
}
