package com.echo.feature.achievements.match

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChdReaderTest {
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
    fun `bit reader returns MSB-first fields`() {
        val bits = ChdBitReader(byteArrayOf(0x12, 0x34, 0x56.toByte()))
        assertEquals(0x1, bits.read(4))
        assertEquals(0x2, bits.read(4))
        assertEquals(0x34, bits.read(8))
        assertEquals(0x56, bits.read(8))
    }

    @Test
    fun `huffman decodes canonical codes built from an RLE tree`() {
        val stream = ByteArray(8) { 0x44 } + byteArrayOf(0x3A)
        val bits = ChdBitReader(stream)
        val huff = ChdHuffman(16, 8)
        assertTrue(huff.importTreeRle(bits))
        assertEquals(3, huff.decodeOne(bits))
        assertEquals(10, huff.decodeOne(bits))
    }

    @Test
    fun `parses a v5 uncompressed header and expands the hunk map`() {
        val hunkBytes = 4096
        val header = ByteArray(124)
        "MComprHD".toByteArray(Charsets.US_ASCII).copyInto(header, 0)
        putBe32(header, 8, 124)
        putBe32(header, 12, 5)

        putBe64(header, 32, 8192)
        putBe64(header, 40, 124)
        putBe32(header, 56, hunkBytes)
        putBe32(header, 60, 2048)

        val map = ByteArray(8)
        putBe32(map, 0, 2)
        putBe32(map, 4, 3)

        val chd = ChdReader.open(MemSource(header + map))!!
        assertEquals(2, chd.header.hunkCount)
        assertTrue(!chd.header.compressed)

        val h0 = chd.hunk(0)
        assertEquals(ChdReader.COMPRESSION_NONE, h0.compressionType)
        assertEquals(8192L, h0.offset)
        assertEquals(hunkBytes, h0.length)
        assertEquals(12288L, chd.hunk(1).offset)
    }

    @Test
    fun `rejects a non-CHD file`() {
        assertNull(ChdReader.open(MemSource(ByteArray(124))))
    }

    @Test
    fun `rejects malformed geometry so allocations stay bounded`() {
        fun header(hunkBytes: Int, logicalBytes: Long): ByteArray {
            val h = ByteArray(124)
            "MComprHD".toByteArray(Charsets.US_ASCII).copyInto(h, 0)
            putBe32(h, 12, 5)
            putBe64(h, 32, logicalBytes)
            putBe32(h, 56, hunkBytes)
            return h
        }
        assertNull(ChdReader.open(MemSource(header(0, 4096))))
        assertNull(ChdReader.open(MemSource(header(1, Long.MAX_VALUE / 2))))
        assertNull(ChdReader.open(MemSource(header(64 * 1024 * 1024, 4096))))
    }

    private fun putBe32(b: ByteArray, o: Int, v: Int) {
        b[o] = (v ushr 24).toByte(); b[o + 1] = (v ushr 16).toByte()
        b[o + 2] = (v ushr 8).toByte(); b[o + 3] = v.toByte()
    }

    private fun putBe64(b: ByteArray, o: Int, v: Long) {
        for (i in 0 until 8) b[o + i] = (v ushr (8 * (7 - i))).toByte()
    }
}
