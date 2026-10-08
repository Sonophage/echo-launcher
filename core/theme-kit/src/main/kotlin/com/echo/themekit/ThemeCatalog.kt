package com.echo.themekit

import java.net.URI
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// the online theme store's list (owner, 2026-10-07): index.json, published beside the themes it names. Each
// theme is a zip of its theme folder; ECHO reads it through EchoThemeFolder, so the format has one reader.
// Paths are relative to the index. The hero, README and screenshots are fetched for the store page only.
object ThemeCatalog {
    const val FORMAT = 1

    @Serializable
    private data class File(val format: Int = 0, val themes: List<Entry> = emptyList())

    @Serializable
    private data class Entry(
        val id: String = "",
        val name: String = "",
        val archive: String = "",
        val sha256: String = "",
        val size: Long = 0,
        val hero: String? = null,
        // the theme's own still wallpaper, behind its store page (owner, 2026-10-07)
        val wallpaper: String? = null,
        val readme: String? = null,
        val screenshots: List<String> = emptyList(),
    )

    private val json = Json { ignoreUnknownKeys = true }
    private val SHA256 = Regex("[0-9a-f]{64}")

    // the themes [text] lists, with every address made absolute against [indexUrl]; null when it is not a
    // catalog this ECHO reads. An entry missing its archive, its checksum or its size is left out, and so is
    // any address that leaves the index's own scheme (no file: or content: from a catalog)
    fun parse(text: String, indexUrl: String): List<CatalogTheme>? {
        val file = runCatching { json.decodeFromString(File.serializer(), text) }.getOrNull() ?: return null
        if (file.format != FORMAT) return null
        val base = runCatching { URI(indexUrl) }.getOrNull() ?: return null
        fun resolve(path: String?): String? = path?.takeIf { it.isNotBlank() }
            ?.let { runCatching { base.resolve(it) }.getOrNull() }
            ?.takeIf { it.scheme.equals(base.scheme, ignoreCase = true) }
            ?.toString()
        return file.themes.mapNotNull { e ->
            CatalogTheme(
                id = e.id.takeIf { it.isNotBlank() } ?: return@mapNotNull null,
                name = e.name.ifBlank { e.id },
                archiveUrl = resolve(e.archive) ?: return@mapNotNull null,
                sha256 = e.sha256.lowercase().takeIf { SHA256.matches(it) } ?: return@mapNotNull null,
                size = e.size.takeIf { it > 0 } ?: return@mapNotNull null,
                heroUrl = resolve(e.hero),
                wallpaperUrl = resolve(e.wallpaper),
                readmeUrl = resolve(e.readme),
                screenshotUrls = e.screenshots.mapNotNull(::resolve).take(EchoThemeCodec.MAX_SCREENSHOTS),
            )
        }
    }
}

data class CatalogTheme(
    val id: String,
    val name: String,
    val archiveUrl: String,
    val sha256: String,
    val size: Long,
    val heroUrl: String?,
    val readmeUrl: String?,
    val screenshotUrls: List<String>,
    val wallpaperUrl: String? = null,
)
