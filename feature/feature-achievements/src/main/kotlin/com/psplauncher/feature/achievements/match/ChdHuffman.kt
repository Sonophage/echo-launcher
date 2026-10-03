package com.psplauncher.feature.achievements.match

class ChdBitReader(private val data: ByteArray) {
    private var buffer = 0
    private var bits = 0
    private var offset = 0

    fun peek(numbits: Int): Int {
        if (numbits == 0) return 0
        if (numbits > bits) {
            while (bits <= 24) {
                if (offset < data.size) buffer = buffer or ((data[offset].toInt() and 0xFF) shl (24 - bits))
                offset++
                bits += 8
            }
        }
        return buffer ushr (32 - numbits)
    }

    fun remove(numbits: Int) {
        buffer = buffer shl numbits
        bits -= numbits
    }

    fun read(numbits: Int): Int {
        val r = peek(numbits)
        remove(numbits)
        return r
    }

    fun overflow(): Boolean = (offset - bits / 8) > data.size
}

class ChdHuffman(private val numcodes: Int, private val maxbits: Int) {
    private val nodeBits = IntArray(numcodes)
    private val nodeCode = IntArray(numcodes)
    private val lookup = IntArray(1 shl maxbits)

    fun decodeOne(bits: ChdBitReader): Int {
        val lv = lookup[bits.peek(maxbits)]
        bits.remove(lv and 0x1f)
        return lv ushr 5
    }

    fun importTreeRle(bits: ChdBitReader): Boolean {
        val numbits = when {
            maxbits >= 16 -> 5
            maxbits >= 8 -> 4
            else -> 3
        }
        var curnode = 0
        while (curnode < numcodes) {
            val nodebits = bits.read(numbits)
            if (nodebits != 1) {
                nodeBits[curnode++] = nodebits
            } else {
                val escaped = bits.read(numbits)
                if (escaped == 1) {
                    nodeBits[curnode++] = 1
                } else {
                    var repcount = bits.read(numbits) + 3
                    if (repcount + curnode > numcodes) return false
                    while (repcount-- > 0) nodeBits[curnode++] = escaped
                }
            }
        }
        if (curnode != numcodes) return false
        if (!assignCanonicalCodes()) return false
        buildLookupTable()
        return !bits.overflow()
    }

    private fun assignCanonicalCodes(): Boolean {
        val bithisto = IntArray(33)
        for (c in 0 until numcodes) {
            val n = nodeBits[c]
            if (n > maxbits) return false
            if (n <= 32) bithisto[n]++
        }
        var curstart = 0
        for (codelen in 32 downTo 1) {
            val nextstart = (curstart + bithisto[codelen]) shr 1
            if (codelen != 1 && nextstart * 2 != curstart + bithisto[codelen]) return false
            bithisto[codelen] = curstart
            curstart = nextstart
        }
        for (c in 0 until numcodes) {
            val n = nodeBits[c]
            if (n > 0) nodeCode[c] = bithisto[n]++
        }
        return true
    }

    private fun buildLookupTable() {
        for (c in 0 until numcodes) {
            val n = nodeBits[c]
            if (n <= 0) continue
            val value = (c shl 5) or n
            val shift = maxbits - n
            var dest = nodeCode[c] shl shift
            val destend = ((nodeCode[c] + 1) shl shift) - 1
            while (dest <= destend) lookup[dest++] = value
        }
    }
}
