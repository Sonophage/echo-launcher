package com.echo.feature.achievements.match

import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.zip.Deflater
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class ChdCdImageTest {
    private class MemSource(private val data: ByteArray) : DiscImage.SeekableSource {
        override fun readFully(offset: Long, dest: ByteArray, len: Int): Int {
            if (offset >= data.size) return 0
            val n = minOf(len.toLong(), data.size - offset).toInt()
            System.arraycopy(data, offset.toInt(), dest, 0, n)
            return n
        }
        override fun close() {}
    }

    @Test
    fun `decodeCdzl inflates the base stream back into 2352-byte frames`() {
        val frames = 2
        val hunkBytes = frames * 2448
        val sectorData = ByteArray(frames * 2352) { (it * 5 and 0xFF).toByte() }
        val deflated = deflateRaw(sectorData)

        val eccBytes = (frames + 7) / 8
        val hunk = ByteArray(eccBytes + 2 + deflated.size)
        hunk[eccBytes] = (deflated.size ushr 8).toByte()
        hunk[eccBytes + 1] = deflated.size.toByte()
        deflated.copyInto(hunk, eccBytes + 2)

        val out = ChdReader.decodeCdzl(hunk, hunkBytes)
        for (f in 0 until frames) {
            assertContentEquals(
                sectorData.copyOfRange(f * 2352, f * 2352 + 2352),
                out.copyOfRange(f * 2448, f * 2448 + 2352),
            )
        }
    }

    @Test
    fun `decodeCdlz decodes an LZMA base stream into 2352-byte frames`() {
        val frames = 2
        val hunkBytes = frames * 2448
        val sectorData = ByteArray(frames * 2352) { ((it * 31 + 7) and 0xFF).toByte() }
        val lzma = lzmaRawCompress(sectorData)

        val eccBytes = (frames + 7) / 8
        val hunk = ByteArray(eccBytes + 2 + lzma.size)
        hunk[eccBytes] = (lzma.size ushr 8).toByte()
        hunk[eccBytes + 1] = lzma.size.toByte()
        lzma.copyInto(hunk, eccBytes + 2)

        val out = ChdReader.decodeCdlz(hunk, hunkBytes)
        for (f in 0 until frames) {
            assertContentEquals(
                sectorData.copyOfRange(f * 2352, f * 2352 + 2352),
                out.copyOfRange(f * 2448, f * 2448 + 2352),
            )
        }
    }

    @Test
    fun `reads an uncompressed CD CHD through the sector source into the Sega hasher`() {
        val hunkBytes = 2 * 2448
        val hunkDataOffset = hunkBytes.toLong()

        val segaHeader = ByteArray(512) { (it * 3 and 0xFF).toByte() }
        "SEGADISCSYSTEM  ".toByteArray(Charsets.US_ASCII).copyInto(segaHeader, 0)

        val file = ByteArray((hunkDataOffset + hunkBytes).toInt())
        writeHeader(file, hunkBytes)
        writeMap(file, mapOffset = 124, unitOffset = 1)
        writeTrackMetadata(file, metaOffset = 128, "TRACK:1 TYPE:MODE1_RAW SUBTYPE:NONE FRAMES:2 PREGAP:0 PGTYPE:MODE1 PGSUB:NONE POSTGAP:0")
        segaHeader.copyInto(file, hunkDataOffset.toInt() + 16)

        val chd = ChdReader.open(MemSource(file))!!
        val sectors = ChdSectorSource.of(chd)!!
        val image = DiscImage.openTracks(sectors, firstTrackSector = 0)

        val hash = image.use { RaSegaDiscHasher.hash(it) }
        assertEquals(md5(segaHeader), hash)
    }

    private fun writeHeader(file: ByteArray, hunkBytes: Int) {
        "MComprHD".toByteArray(Charsets.US_ASCII).copyInto(file, 0)
        putBe32(file, 8, 124)
        putBe32(file, 12, 5)

        putBe64(file, 32, hunkBytes.toLong())
        putBe64(file, 40, 124)
        putBe64(file, 48, 128)
        putBe32(file, 56, hunkBytes)
        putBe32(file, 60, 2448)
    }

    private fun writeMap(file: ByteArray, mapOffset: Int, unitOffset: Int) =
        putBe32(file, mapOffset, unitOffset)

    private fun writeTrackMetadata(file: ByteArray, metaOffset: Int, text: String) {
        val tag = ('C'.code shl 24) or ('H'.code shl 16) or ('T'.code shl 8) or '2'.code
        val bytes = text.toByteArray(Charsets.US_ASCII)
        putBe32(file, metaOffset, tag)
        putBe32(file, metaOffset + 4, bytes.size)

        bytes.copyInto(file, metaOffset + 16)
    }

    private fun lzmaRawCompress(data: ByteArray): ByteArray {
        val opts = org.tukaani.xz.LZMA2Options()
        opts.dictSize = maxOf(data.size, 4096)
        val out = ByteArrayOutputStream()
        org.tukaani.xz.LZMAOutputStream(out, opts, data.size.toLong()).use { it.write(data) }
        return out.toByteArray().copyOfRange(13, out.size())
    }

    private fun deflateRaw(data: ByteArray): ByteArray {
        val deflater = Deflater(Deflater.DEFAULT_COMPRESSION, true)
        deflater.setInput(data); deflater.finish()
        val out = ByteArrayOutputStream()
        val buf = ByteArray(1024)
        while (!deflater.finished()) out.write(buf, 0, deflater.deflate(buf))
        deflater.end()
        return out.toByteArray()
    }

    private fun md5(b: ByteArray): String =
        MessageDigest.getInstance("MD5").digest(b).joinToString("") { "%02x".format(it.toInt() and 0xFF) }

    private fun putBe32(b: ByteArray, o: Int, v: Int) {
        b[o] = (v ushr 24).toByte(); b[o + 1] = (v ushr 16).toByte()
        b[o + 2] = (v ushr 8).toByte(); b[o + 3] = v.toByte()
    }

    private fun putBe64(b: ByteArray, o: Int, v: Long) {
        for (i in 0 until 8) b[o + i] = (v ushr (8 * (7 - i))).toByte()
    }
}
