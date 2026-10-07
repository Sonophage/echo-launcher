package com.echo.themekit

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

// a theme as a folder (owner, 2026-10-07): ECHO/Themes/<name>/ holds theme.json, Icons/ (console icons in
// Icons/Consoles/) and Wallpaper/. Each file is one entry of the .echo-theme zip, so a folder theme is read
// through EchoThemeCodec with the same limits and checks as a file. Paths use '/' and are relative to the
// theme's folder.
object EchoThemeFolder {
    const val MANIFEST = "theme.json"

    // the folder path for a zip entry, or null for an entry with no place in the folder
    fun folderPath(entry: String): String? = when {
        entry == "manifest.json" -> MANIFEST
        entry.startsWith("icons/") -> leaf(entry.removePrefix("icons/"))?.let { "Icons/$it" }
        entry.startsWith("sysicons/") -> leaf(entry.removePrefix("sysicons/"))?.let { "Icons/Consoles/$it" }
        else -> leaf(entry)?.let { "Wallpaper/$it" }
    }

    // the zip entry for a folder path; the folder names are matched without regard to case, as a person
    // making a theme by hand may write icons/ or wallpaper/
    fun entryName(path: String): String? {
        val parts = path.split('/')
        return when {
            parts.size == 1 && parts[0].equals(MANIFEST, ignoreCase = true) -> "manifest.json"
            parts.size == 3 && parts[0].equals("Icons", true) && parts[1].equals("Consoles", true) -> "sysicons/${parts[2]}"
            parts.size == 2 && parts[0].equals("Icons", true) -> "icons/${parts[1]}"
            parts.size == 2 && parts[0].equals("Wallpaper", true) -> parts[1]
            else -> null
        }.takeIf { parts.last().isNotEmpty() }
    }

    // the theme's files by folder path
    fun toFiles(bundle: EchoThemeBundle): Map<String, ByteArray> {
        val out = linkedMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(EchoThemeCodec.write(bundle))).use { zip ->
            generateSequence { zip.nextEntry }.filterNot { it.isDirectory }.forEach { entry ->
                folderPath(entry.name)?.let { out[it] = zip.readBytes() }
            }
        }
        return out
    }

    // the theme a folder holds, or null when it has no theme.json the codec accepts. Files with no place
    // in a theme are ignored.
    // ponytail: the folder is packed in memory, bounded by the codec's caps; stream it if themes grow
    fun toBundle(files: Map<String, ByteArray>): EchoThemeBundle? {
        val packed = ByteArrayOutputStream()
        ZipOutputStream(packed).use { zip ->
            for ((path, bytes) in files) {
                val entry = entryName(path) ?: continue
                zip.putNextEntry(ZipEntry(entry))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return EchoThemeCodec.read(packed.toByteArray())
    }

    // a folder name for a theme: its name without the characters file systems refuse, or [fallback]
    fun folderName(themeName: String, fallback: String): String =
        themeName.replace(Regex("""[\\/:*?"<>|\u0000-\u001f]"""), "").trim().trimEnd('.').ifBlank { fallback }

    private fun leaf(name: String): String? = name.takeIf { it.isNotEmpty() && '/' !in it }
}
