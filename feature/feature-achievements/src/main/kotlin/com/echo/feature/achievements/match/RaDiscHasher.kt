package com.echo.feature.achievements.match

import java.security.MessageDigest

object RaDiscHasher {
    private val SUPPORTED = setOf("psx", "ps2", "psp")

    fun isSupported(platformId: String): Boolean = platformId in SUPPORTED

    fun hash(platformId: String, image: DiscImage): String? = when (platformId) {
        "psx" -> hashPsx(image)
        "ps2" -> hashPs2(image)
        "psp" -> hashPsp(image)
        else -> null
    }

    private data class Exe(val name: String, val entry: DiscImage.Entry)

    private fun hashPs2(img: DiscImage): String? {
        val exe = findPlaystationExe(img, "BOOT2", "cdrom0:") ?: return null
        val md5 = MessageDigest.getInstance("MD5")
        md5.update(exe.name.toByteArray(Charsets.US_ASCII))
        img.hashFileInto(md5, exe.entry.lba, exe.entry.size)
        return hex(md5)
    }

    private fun hashPsx(img: DiscImage): String? {
        val exe = findPlaystationExe(img, "BOOT", "cdrom:")
        val name: String
        val lba: Int
        var size: Int
        if (exe != null) {
            name = exe.name; lba = exe.entry.lba; size = exe.entry.size
        } else {
            val fallback = img.findFile("PSX.EXE") ?: return null
            name = "PSX.EXE"; lba = fallback.lba; size = fallback.size
        }

        val head = img.readSector(lba, 32)
        if (head.size >= 32 && isPsxExe(head)) {
            size = u32le(head, 28) + 2048
        }

        val md5 = MessageDigest.getInstance("MD5")
        md5.update(name.toByteArray(Charsets.US_ASCII))
        img.hashFileInto(md5, lba, size)
        return hex(md5)
    }

    private fun hashPsp(img: DiscImage): String? {
        val sfo = img.findFile("PSP_GAME\\PARAM.SFO") ?: return null
        val eboot = img.findFile("PSP_GAME\\SYSDIR\\EBOOT.BIN") ?: return null
        val md5 = MessageDigest.getInstance("MD5")
        img.hashFileInto(md5, sfo.lba, sfo.size)
        img.hashFileInto(md5, eboot.lba, eboot.size)
        return hex(md5)
    }

    private fun findPlaystationExe(img: DiscImage, bootKey: String, cdromPrefix: String): Exe? {
        val cnf = img.findFile("SYSTEM.CNF") ?: return null
        val text = String(img.readSector(cnf.lba, minOf(cnf.size, 2048).coerceAtLeast(1)), Charsets.US_ASCII)
        val name = parseBootExecutable(text, bootKey, cdromPrefix) ?: return null
        val entry = img.findFile(name) ?: return null
        return Exe(name, entry)
    }

    internal fun parseBootExecutable(text: String, bootKey: String, cdromPrefix: String): String? {
        for (rawLine in text.split('\n')) {
            if (!rawLine.startsWith(bootKey)) continue
            var j = bootKey.length
            while (j < rawLine.length && rawLine[j].isWhitespace()) j++
            if (j >= rawLine.length || rawLine[j] != '=') continue
            j++
            while (j < rawLine.length && rawLine[j].isWhitespace()) j++
            if (rawLine.startsWith(cdromPrefix, j)) j += cdromPrefix.length
            while (j < rawLine.length && rawLine[j] == '\\') j++
            val start = j
            while (j < rawLine.length && !rawLine[j].isWhitespace() && rawLine[j] != ';') j++
            return rawLine.substring(start, j).ifEmpty { null }
        }
        return null
    }

    private fun isPsxExe(head: ByteArray): Boolean {
        val marker = "PS-X EXE"
        for (k in 0 until 7) if (head[k].toInt() != marker[k].code) return false
        return true
    }

    private fun u32le(b: ByteArray, o: Int): Int =
        (b[o].toInt() and 0xFF) or ((b[o + 1].toInt() and 0xFF) shl 8) or
            ((b[o + 2].toInt() and 0xFF) shl 16) or ((b[o + 3].toInt() and 0xFF) shl 24)

    private fun hex(md5: MessageDigest): String =
        md5.digest().joinToString("") { "%02x".format(it.toInt() and 0xFF) }
}
