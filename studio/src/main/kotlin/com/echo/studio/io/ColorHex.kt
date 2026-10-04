package com.echo.studio.io

object ColorHex {
    const val DEFAULT_ACCENT = 0xFF0055AA.toInt()

    fun toHexRgb(argb: Int): String = "#%06X".format(argb and 0xFFFFFF)

    fun parseHexRgb(hex: String): Int? {
        val digits = hex.removePrefix("#")
        if (digits.length != 6 && digits.length != 8) return null
        val value = digits.toLongOrNull(16) ?: return null
        return (0xFF000000L or (value and 0xFFFFFF)).toInt()
    }
}
