package com.psplauncher.feature.achievements.match

import java.security.MessageDigest

object RaSegaDiscHasher {
    private const val HEADER_SIZE = 512
    private val SEGA_CD = "SEGADISCSYSTEM  ".toByteArray(Charsets.US_ASCII)
    private val SATURN = "SEGA SEGASATURN ".toByteArray(Charsets.US_ASCII)

    fun isSupported(platformId: String): Boolean = platformId == "segacd" || platformId == "saturn"

    fun hash(image: DiscImage): String? {
        val header = image.readSector(0, HEADER_SIZE)
        if (header.size < HEADER_SIZE) return null
        if (!startsWith(header, SEGA_CD) && !startsWith(header, SATURN)) return null
        val md5 = MessageDigest.getInstance("MD5")
        md5.update(header, 0, HEADER_SIZE)
        return md5.digest().joinToString("") { "%02x".format(it.toInt() and 0xFF) }
    }

    private fun startsWith(b: ByteArray, magic: ByteArray): Boolean {
        if (b.size < magic.size) return false
        for (i in magic.indices) if (b[i] != magic[i]) return false
        return true
    }
}
