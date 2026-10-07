package com.echo.themekit

import com.echo.core.archive.BoundedZipReader
import com.echo.core.archive.ZipLimitExceededException
import com.echo.core.archive.ZipLimits
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.serialization.json.Json

object EchoThemeCodec {
    const val FILE_EXTENSION = "echo-theme"
    const val LEGACY_FILE_EXTENSION = "pfptheme"

    // the extensions a theme file is opened by
    val READABLE_EXTENSIONS = setOf(FILE_EXTENSION, LEGACY_FILE_EXTENSION)
    private const val ENTRY_MANIFEST = "manifest.json"
    private const val ENTRY_WALLPAPER = "wallpaper.png"
    private const val ENTRY_PREVIEW = "preview.png"
    private const val ICONS_PREFIX = "icons/"
    private const val SYSICONS_PREFIX = "sysicons/"
    private const val MOTION_PREFIX = "motion."
    private const val MEDIA_PREFIX = "media/"

    val ICON_EXTENSIONS = setOf("png", "gif")
    val MOTION_EXTENSIONS = setOf("mp4", "webm", "gif")

    val BUNDLE_LIMITS = ZipLimits(
        maxEntries    = 256,
        maxEntryBytes = 64L * 1024 * 1024,
        maxTotalBytes = 256L * 1024 * 1024,
    )

    const val MAX_ICON_BYTES = 8 * 1024 * 1024

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    fun write(bundle: EchoThemeBundle, out: OutputStream) {
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry(ENTRY_MANIFEST))
            zip.write(json.encodeToString(EchoThemeManifest.serializer(), bundle.manifest.copy(manifest = EchoThemeManifest.MANIFEST_TYPE)).toByteArray())
            zip.closeEntry()
            bundle.wallpaper?.let { zip.writeEntry(ENTRY_WALLPAPER, it) }
            bundle.preview?.let { zip.writeEntry(ENTRY_PREVIEW, it) }

            for ((key, image) in bundle.icons.toSortedMap()) {
                if (IconSlots.isValidKey(key) && image.extension.lowercase() in ICON_EXTENSIONS) {
                    zip.writeEntry("$ICONS_PREFIX$key.${image.extension.lowercase()}", image.bytes)
                }
            }
            for ((platformId, image) in bundle.sysicons.toSortedMap()) {
                if (CustomizableIcons.isValidKey("sysicon_$platformId") && image.extension.lowercase() in ICON_EXTENSIONS) {
                    zip.writeEntry("$SYSICONS_PREFIX$platformId.${image.extension.lowercase()}", image.bytes)
                }
            }

            for ((key, file) in bundle.media.toSortedMap()) {
                if (ThemeMedia.isMedia(key, file.extension)) zip.writeEntry("$MEDIA_PREFIX$key.${file.extension.lowercase()}", file.bytes)
            }

            bundle.motion?.let { motion ->
                val ext = motion.extension.lowercase()
                if (ext in MOTION_EXTENSIONS) {
                    zip.putNextEntry(ZipEntry("$MOTION_PREFIX$ext"))
                    motion.copyTo(zip)
                    zip.closeEntry()
                }
            }
        }
    }

    fun write(bundle: EchoThemeBundle): ByteArray =
        ByteArrayOutputStream().also { write(bundle, it) }.toByteArray()

    @JvmOverloads
    fun read(input: InputStream, reopen: ((String) -> ThemeMotion)? = null): EchoThemeBundle? {
        var manifest: EchoThemeManifest? = null
        var wallpaper: ByteArray? = null
        var preview: ByteArray? = null
        val icons = mutableMapOf<String, ThemeImage>()
        val sysicons = mutableMapOf<String, ThemeImage>()
        var motionExtension: String? = null
        val media = mutableMapOf<String, ThemeImage>()

        try {
            BoundedZipReader.read(input, BUNDLE_LIMITS) { entry ->
                when {
                    entry.name == ENTRY_MANIFEST -> manifest = runCatching {
                        json.decodeFromString(
                            EchoThemeManifest.serializer(),
                            entry.readBytes().decodeToString(),
                        )
                    }.getOrNull()
                    entry.name == ENTRY_WALLPAPER -> wallpaper = entry.readBytes()
                    entry.name == ENTRY_PREVIEW -> preview = entry.readBytes()
                    entry.name.startsWith(ICONS_PREFIX) -> {
                        val name = entry.name.removePrefix(ICONS_PREFIX)
                        val key = name.substringBeforeLast('.')
                        val ext = name.substringAfterLast('.', "").lowercase()
                        if (ext in ICON_EXTENSIONS && IconSlots.isValidKey(key)) {
                            entry.readBytes()
                                .takeIf { it.size <= MAX_ICON_BYTES }
                                ?.let { icons[key] = ThemeImage(it, ext) }
                        }
                    }
                    entry.name.startsWith(SYSICONS_PREFIX) -> {
                        val name = entry.name.removePrefix(SYSICONS_PREFIX)
                        val platformId = name.substringBeforeLast('.')
                        val ext = name.substringAfterLast('.', "").lowercase()
                        if (ext in ICON_EXTENSIONS && CustomizableIcons.isValidKey("sysicon_$platformId")) {
                            entry.readBytes()
                                .takeIf { it.size <= MAX_ICON_BYTES }
                                ?.let { sysicons[platformId] = ThemeImage(it, ext) }
                        }
                    }
                    entry.name.startsWith(MEDIA_PREFIX) -> {
                        val name = entry.name.removePrefix(MEDIA_PREFIX)
                        val key = name.substringBeforeLast('.')
                        val ext = name.substringAfterLast('.', "").lowercase()
                        if (ThemeMedia.isMedia(key, ext)) media[key] = ThemeImage(entry.readBytes(), ext)
                    }
                    entry.name.startsWith(MOTION_PREFIX) -> {
                        val ext = entry.name.removePrefix(MOTION_PREFIX).lowercase()
                        if (ext in MOTION_EXTENSIONS) motionExtension = ext
                    }
                }
            }
        } catch (e: ZipLimitExceededException) {
            return null
        }

        val m = manifest ?: return null
        if (!EchoThemeManifest.isThemeManifest(m.manifest)) return null
        return EchoThemeBundle(
            manifest = m,
            wallpaper = wallpaper,
            preview = preview,
            icons = icons,
            sysicons = sysicons,
            motion = motionExtension?.let { ext -> reopen?.invoke(ext) },
            media = media,
        )
    }

    fun read(bytes: ByteArray): EchoThemeBundle? =
        read(ByteArrayInputStream(bytes)) { ext -> motionFrom({ ByteArrayInputStream(bytes) }, ext) }

    fun read(file: File): EchoThemeBundle? =
        file.inputStream().use { read(it) { ext -> motionFrom({ file.inputStream() }, ext) } }

    fun readManifest(file: File): EchoThemeManifest? {
        var manifest: EchoThemeManifest? = null
        try {
            file.inputStream().use { input ->
                BoundedZipReader.read(input, BUNDLE_LIMITS) { entry ->
                    if (entry.name == ENTRY_MANIFEST) {
                        manifest = runCatching {
                            json.decodeFromString(
                                EchoThemeManifest.serializer(),
                                entry.readBytes().decodeToString(),
                            )
                        }.getOrNull()
                        entry.stop()
                    }
                }
            }
        } catch (e: ZipLimitExceededException) {
            return null
        }
        return manifest?.takeIf { EchoThemeManifest.isThemeManifest(it.manifest) }
    }

    private fun motionFrom(source: () -> InputStream, ext: String): ThemeMotion =
        ThemeMotion(ext) { out ->
            var written = 0L
            source().use { input ->
                BoundedZipReader.read(input, BUNDLE_LIMITS) { entry ->
                    if (entry.name == "$MOTION_PREFIX$ext") {
                        written = entry.copyTo(out)
                        entry.stop()
                    }
                }
            }
            written
        }

    private fun ZipOutputStream.writeEntry(name: String, data: ByteArray) {
        putNextEntry(ZipEntry(name))
        write(data)
        closeEntry()
    }
}
