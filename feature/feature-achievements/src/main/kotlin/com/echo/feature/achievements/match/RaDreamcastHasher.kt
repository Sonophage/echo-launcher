package com.echo.feature.achievements.match

import java.security.MessageDigest

object RaDreamcastHasher {
    private const val IP_BIN_SIZE = 256
    private const val BOOT_NAME_OFFSET = 96
    private const val BOOT_NAME_MAX = 16
    private val KATANA = "SEGA SEGAKATANA ".toByteArray(Charsets.US_ASCII)

    fun isSupported(platformId: String): Boolean = platformId == "dreamcast"

    fun isDreamcastHeader(bytes: ByteArray): Boolean {
        if (bytes.size < KATANA.size) return false
        for (i in KATANA.indices) if (bytes[i] != KATANA[i]) return false
        return true
    }

    fun hash(image: DiscImage): String? {
        val ip = image.readSector(image.firstTrackSector, IP_BIN_SIZE)
        if (ip.size < IP_BIN_SIZE || !isDreamcastHeader(ip)) return null

        val md5 = MessageDigest.getInstance("MD5")
        md5.update(ip, 0, IP_BIN_SIZE)

        val bootName = bootName(ip) ?: return null
        val entry = image.findFile(bootName) ?: return null
        image.hashFileInto(md5, entry.lba, entry.size)
        return md5.digest().joinToString("") { "%02x".format(it.toInt() and 0xFF) }
    }

    private fun bootName(ip: ByteArray): String? {
        val sb = StringBuilder()
        var i = 0
        while (i < BOOT_NAME_MAX) {
            val c = ip[BOOT_NAME_OFFSET + i].toInt() and 0xFF
            if (isSpace(c)) break
            sb.append(c.toChar())
            i++
        }
        return sb.toString().ifEmpty { null }
    }

    private fun isSpace(c: Int): Boolean = c == 0x20 || c in 0x09..0x0D
}
