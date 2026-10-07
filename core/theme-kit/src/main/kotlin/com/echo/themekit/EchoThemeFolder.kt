package com.echo.themekit

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

// a theme as a folder (owner, 2026-10-07): ECHO/Themes/<name>/ holds theme.json, Icons/ (console icons in
// Icons/Consoles/), Wallpaper/, and Sounds/, Boot/ and GameStart/ for media named after its slot. Each file
// is one entry of the .echo-theme zip, so a folder theme is read through EchoThemeCodec with the same limits
// and checks as a file. Paths use '/' and are relative to the theme's folder.
object EchoThemeFolder {
    const val MANIFEST = "theme.json"
    const val README = "README.md"

    // the folder path for a zip entry, or null for an entry with no place in the folder
    fun folderPath(entry: String): String? = when {
        entry == "manifest.json" -> MANIFEST
        entry == "readme.md" -> README
        entry.startsWith("preview/screenshots/") -> leaf(entry.removePrefix("preview/screenshots/"))?.let { "Preview/Screenshots/$it" }
        entry.startsWith("preview/") -> leaf(entry.removePrefix("preview/"))?.let { "Preview/$it" }
        entry.startsWith("icons/") -> leaf(entry.removePrefix("icons/"))?.let { "Icons/$it" }
        entry.startsWith("sysicons/") -> leaf(entry.removePrefix("sysicons/"))?.let { "Icons/Consoles/$it" }
        entry.startsWith("media/") -> leaf(entry.removePrefix("media/"))
            ?.let { name -> ThemeMedia.FOLDERS[name.substringBeforeLast('.')]?.let { "$it/$name" } }
        else -> leaf(entry)?.let { "Wallpaper/$it" }
    }

    // the zip entry for a folder path; the folder names are matched without regard to case, as a person
    // making a theme by hand may write icons/ or wallpaper/
    fun entryName(path: String): String? {
        val parts = path.split('/')
        return when {
            parts.size == 1 && parts[0].equals(MANIFEST, ignoreCase = true) -> "manifest.json"
            parts.size == 1 && parts[0].equals(README, ignoreCase = true) -> "readme.md"
            parts.size == 3 && parts[0].equals("Preview", true) && parts[1].equals("Screenshots", true) -> "preview/screenshots/${parts[2]}"
            parts.size == 2 && parts[0].equals("Preview", true) && parts[1].startsWith("hero.", true) -> "preview/${parts[1].lowercase()}"
            parts.size == 3 && parts[0].equals("Icons", true) && parts[1].equals("Consoles", true) -> "sysicons/${parts[2]}"
            parts.size == 2 && parts[0].equals("Icons", true) -> "icons/${parts[1]}"
            parts.size == 2 && parts[0].equals("Wallpaper", true) -> parts[1]
            // a media file is placed only in its own slot's folder; anything else there (a README) has no place
            parts.size == 2 && ThemeMedia.FOLDERS[parts[1].substringBeforeLast('.')]?.equals(parts[0], true) == true -> "media/${parts[1]}"
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
            val written = mutableSetOf<String>()
            for ((path, bytes) in files) {
                // two paths can name one entry (Icons/ beside icons/); the first is kept
                val entry = entryName(path)?.takeIf { written.add(it) } ?: continue
                zip.putNextEntry(ZipEntry(entry))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return EchoThemeCodec.read(packed.toByteArray())
    }

    // the theme in a zip of its folder, as the online store serves it. The files may sit in one top folder
    // (Aurora/theme.json) or at the zip's root; the zip is read with the codec's limits. Null when it is not
    // a theme
    fun fromArchive(input: java.io.InputStream): EchoThemeBundle? {
        val files = linkedMapOf<String, ByteArray>()
        try {
            com.echo.core.archive.BoundedZipReader.read(input, EchoThemeCodec.BUNDLE_LIMITS) { entry ->
                if (!entry.isDirectory) files[entry.name] = entry.readBytes()
            }
        } catch (e: com.echo.core.archive.ZipLimitExceededException) {
            return null
        }
        val top = files.keys.map { it.substringBefore('/') }.toSet().singleOrNull()
            ?.takeIf { files.keys.all { k -> '/' in k } }
        val inner = if (top != null) files.mapKeys { it.key.removePrefix("$top/") } else files
        return runCatching { toBundle(inner) }.getOrNull()
    }

    // a folder name for a theme: its name without the characters file systems refuse, or [fallback]
    fun folderName(themeName: String, fallback: String): String =
        themeName.replace(Regex("""[\\/:*?"<>|\u0000-\u001f]"""), "").trim().trimEnd('.').ifBlank { fallback }

    private fun leaf(name: String): String? = name.takeIf { it.isNotEmpty() && '/' !in it }
}
