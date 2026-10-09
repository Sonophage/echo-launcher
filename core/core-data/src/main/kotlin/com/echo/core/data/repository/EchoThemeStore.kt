package com.echo.core.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.echo.core.data.datastore.echoDataStore
import com.echo.core.data.wallpaper.WallpaperAccentProbe
import com.echo.core.data.wallpaper.WallpaperAccentProbe.clearWallpaperAccent
import com.echo.core.data.wallpaper.ThemeAccent
import com.echo.core.data.wallpaper.ThemeAccent.KEY_ACCENT_OVERRIDE
import com.echo.core.data.wallpaper.WallpaperAccentProbe.setWallpaperAccent
import com.echo.themekit.CustomizableIcons
import com.echo.themekit.EchoThemeBundle
import com.echo.themekit.EchoThemeCodec
import com.echo.themekit.EchoThemeManifest
import com.echo.themekit.EchoThemeSource
import com.echo.themekit.ThemeImage
import com.echo.themekit.ThemeMotion
import com.echo.themekit.ThemeMedia
import com.echo.themekit.ThemePart
import com.echo.themekit.ThemeReadme
import com.echo.themekit.ThemeSettings
import com.echo.themekit.parts
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import timber.log.Timber

private const val THEME_EXT = EchoThemeCodec.FILE_EXTENSION

@Singleton
class EchoThemeStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val uiMedia: UiMediaStore,
) {
    data class SavedTheme(
        val id: String,
        val name: String,
        val accentArgb: Long?,

        val previewPath: String?,
        // for the theme store (owner, 2026-10-07): the hero picture, README metadata and the parts it has
        val heroPath: String? = null,
        val author: String? = null,
        val version: String? = null,
        val description: String? = null,
        val parts: Set<ThemePart> = emptySet(),
        // the online store's checksum of the file this theme was downloaded as; null for a theme from elsewhere
        val catalogSha: String? = null,
    )

    // a saved theme's own wallpaper, for its store page (owner, 2026-10-07: the theme, not a screenshot)
    fun wallpaperPath(id: String): String? = File(dir, "$id.wallpaper.jpg").takeIf { it.isFile }?.absolutePath

    // a theme's store page: its README and its screenshots, unpacked to files
    data class ThemeDetails(val readme: ThemeReadme, val screenshotPaths: List<String>)

    private val dir = File(context.filesDir, "pfpthemes")

    // themes saved before the rename to .echo-theme are renamed once, so the store keeps listing them
    init {
        dir.listFiles { f -> f.name.endsWith(".${EchoThemeCodec.LEGACY_FILE_EXTENSION}") }.orEmpty().forEach { old ->
            val renamed = File(dir, old.name.removeSuffix(EchoThemeCodec.LEGACY_FILE_EXTENSION) + THEME_EXT)
            if (!renamed.exists() && !old.renameTo(renamed)) Timber.w("EchoThemeStore: could not rename %s", old.name)
        }
    }

    private val _themes = MutableStateFlow(scan())
    val themes: StateFlow<List<SavedTheme>> = _themes.asStateFlow()

    // applies the theme, or only [parts] of it, ticked on its store page. A whole theme sets what it has and clears
    // the wallpaper, icons and colours it leaves out; a part ECHO has only one of (sounds, boot, game start,
    // wave design, buttons) keeps the person's own when the theme leaves it out
    suspend fun apply(id: String, parts: Set<ThemePart> = ThemePart.entries.toSet()): Boolean = withContext(Dispatchers.IO) {
        val whole = parts.size == ThemePart.entries.size
        val wallpaperSidecar = File(dir, "$id.wallpaper.jpg")

        val bundle = runCatching { EchoThemeCodec.read(File(dir, "$id.$THEME_EXT")) }.getOrNull()
            ?: return@withContext false
        val appliedName = _themes.value.firstOrNull { it.id == id }?.name ?: "Custom Theme"
        keepLookBefore(appliedName)

        val wallpaperPart = ThemePart.WALLPAPER in parts
        val destDir = File(context.filesDir, "wallpaper").apply { mkdirs() }
        val dest = File(destDir, "wallpaper_theme_${System.currentTimeMillis()}.jpg")
        val wallpaperOk = wallpaperPart && wallpaperSidecar.isFile && runCatching { wallpaperSidecar.copyTo(dest, overwrite = true) }.isSuccess

        val iconsPart = ThemePart.ICONS in parts
        val iconsDir = File(context.filesDir, THEME_ICONS_DIR)
        if (iconsPart) iconsDir.deleteRecursively()
        val iconEntries: Map<String, com.echo.themekit.ThemeImage> = if (!iconsPart) emptyMap() else buildMap {
            putAll(bundle.icons)
            for ((platformId, image) in bundle.sysicons) put("sysicon_$platformId", image)
        }
        if (iconEntries.isNotEmpty()) {
            iconsDir.mkdirs()

            for ((key, image) in iconEntries) {
                File(iconsDir, "$key.${image.extension.lowercase()}").writeBytes(image.bytes)
            }
        }

        val motionDest = bundle.motion?.takeIf { wallpaperPart }?.let { motion ->
            runCatching {
                val motionDir = File(context.filesDir, "wallpaper").apply { mkdirs() }
                val dest = File(motionDir, "wallpaper_theme_${System.currentTimeMillis()}.${motion.extension.lowercase()}")

                FileOutputStream(dest).use { out -> motion.copyTo(out) }
                dest
            }.onFailure {
                Timber.w(it, "EchoThemeStore: could not extract the motion wallpaper")
            }.getOrNull()
        }

        val accent = bundle.manifest.accentColor.toAccentArgbOrNull()

        val iconColor = bundle.manifest.iconColor
            .takeIf { it != EchoThemeManifest.ICON_COLOR_AUTO }
            ?.toAccentArgbOrNull()

        val textColor = bundle.manifest.textColor
            .takeIf { it != EchoThemeManifest.ICON_COLOR_AUTO }
            ?.toAccentArgbOrNull()

        val layoutJson = bundle.manifest.layout
            ?.let(com.echo.themekit.CrossbarLayoutSpecCodec::sanitize)
            ?.takeUnless { it == com.echo.themekit.CrossbarLayoutSpec.DEFAULT }
            ?.let(com.echo.themekit.CrossbarLayoutSpecCodec::encode)

        val waveStyle = when (bundle.manifest.waveStyle) {
            EchoThemeManifest.WAVE_STATIC -> WAVE_STYLE_STATIC
            EchoThemeManifest.WAVE_REDUCED -> WAVE_STYLE_REDUCED
            else -> WAVE_STYLE_ANIMATED
        }

        // the theme's sounds, boot and game-start media; a slot it leaves out keeps the person's own
        // (owner, 2026-10-07)
        val mediaParts = mapOf(ThemeMedia.SOUNDS to ThemePart.SOUNDS, ThemeMedia.BOOT to ThemePart.BOOT, ThemeMedia.GAME_START to ThemePart.GAME_START)
        for ((key, file) in bundle.media) {
            if (mediaParts[ThemeMedia.FOLDERS[key]] !in parts) continue
            val slot = com.echo.core.domain.model.UiMediaSlot.fromKey(key) ?: continue
            val staged = File(context.cacheDir, "theme-media/$key.${file.extension}")
            val result = runCatching {
                staged.parentFile?.mkdirs()
                staged.writeBytes(file.bytes)
                uiMedia.importFile(slot, staged, appliedName)
            }.getOrNull()
            staged.delete()
            if (result?.ok != true) Timber.w("EchoThemeStore: the theme's %s was not applied: %s", key, result?.message)
        }
        val parts0 = bundle.manifest

        val survey = if (wallpaperOk) WallpaperAccentProbe.survey(dest.absolutePath) else null
        val sources = partSources().toMutableMap()
        if (whole) sources.clear()
        bundle.parts().filter { it in parts }.forEach { sources[it] = appliedName }

        context.echoDataStore.edit { prefs ->
            if (whole) prefs[KEY_APPLIED_THEME_NAME] = appliedName
            prefs[KEY_PART_SOURCES] = encodeSources(sources)
            if (ThemePart.WAVE in parts) prefs[KEY_WAVE_STYLE] = waveStyle

            if (wallpaperPart) {
                if (motionDest != null) prefs[KEY_MOTION_WALLPAPER] = motionDest.absolutePath else prefs.remove(KEY_MOTION_WALLPAPER)
                if (wallpaperOk) prefs[KEY_CUSTOM_WALLPAPER] = dest.absolutePath else prefs.remove(KEY_CUSTOM_WALLPAPER)
                prefs.setWallpaperAccent(survey)
            }

            if (ThemePart.COLOURS in parts) {
                if (accent != null) prefs[KEY_ACCENT_OVERRIDE] = accent else prefs.remove(KEY_ACCENT_OVERRIDE)
                if (iconColor != null) prefs[KEY_ICON_COLOR] = iconColor else prefs.remove(KEY_ICON_COLOR)
                if (textColor != null) prefs[KEY_TEXT_COLOR] = textColor else prefs.remove(KEY_TEXT_COLOR)
                if (layoutJson != null) prefs[KEY_THEME_LAYOUT] = layoutJson else prefs.remove(KEY_THEME_LAYOUT)
            }
            if (iconsPart) {
                if (iconEntries.isNotEmpty()) {
                    prefs[KEY_THEME_ICONS_STAMP] = System.currentTimeMillis()
                } else {
                    prefs.remove(KEY_THEME_ICONS_STAMP)
                }
            }
            // set only when the theme names one ECHO knows; otherwise the person's own stays
            if (ThemePart.WAVE in parts) parts0.waveDesign?.takeIf { it in EchoThemeManifest.WAVE_DESIGNS }?.let { prefs[KEY_WAVE_DESIGN] = it }
            if (ThemePart.GAME_START in parts) {
                parts0.gameBootStyle?.takeIf { it in EchoThemeManifest.GAME_START_STYLES }?.let { prefs[KEY_GAMEBOOT_STYLE] = it }
                parts0.launchDiscStyle?.takeIf { it in EchoThemeManifest.GAME_START_STYLES }?.let { prefs[KEY_LAUNCH_DISC_STYLE] = it }
            }
            if (ThemePart.BUTTONS in parts) parts0.buttonSet?.takeIf { it in EchoThemeManifest.BUTTON_SETS }?.let { prefs[KEY_BUTTON_SET] = it }
            if (ThemePart.SETTINGS in parts) prefs.applyThemeSettings(parts0.settings)
        }
        runCatching { currentLook("").use { appliedPrint.writeText(it.print()) } }
            .onFailure { Timber.w(it, "EchoThemeStore: could not record the applied look") }
        true
    }

    // owner, 2026-10-07: applying a theme first saves the look in use as "Before <theme>" on the device shelf.
    // Skipped when the look is still what the last apply left, so trying theme after theme saves your own look
    // once, not every theme tried; and skipped for the default look, which Reset brings back.
    // ponytail: a look changed only in a part a theme cannot carry (sort order, folder art) saves nothing.
    private suspend fun keepLookBefore(themeName: String) {
        runCatching {
            currentLook("Before $themeName").use { look ->
                if (!look.isDefault && look.print() != appliedPrint.takeIf { it.isFile }?.readText()) save(look)
            }
        }.onFailure { Timber.w(it, "EchoThemeStore: could not keep the look before applying") }
    }

    private val appliedPrint get() = File(dir, "applied-look.sha256")

    suspend fun resetApplied(): Unit = withContext(Dispatchers.IO) {
        context.echoDataStore.edit { prefs ->
            prefs.remove(KEY_CUSTOM_WALLPAPER)
            prefs.remove(KEY_MOTION_WALLPAPER)
            prefs.clearWallpaperAccent()
            prefs.remove(KEY_ACCENT_OVERRIDE)

            prefs.remove(ThemeAccent.KEY_ACCENT_FROM_WALLPAPER)
            prefs.remove(KEY_ICON_COLOR)
            prefs.remove(KEY_TEXT_COLOR)
            prefs.remove(KEY_WAVE_STYLE)
            prefs.remove(KEY_THEME_LAYOUT)
            prefs.remove(KEY_THEME_ICONS_STAMP)
            prefs.remove(KEY_APPLIED_THEME_NAME)
            prefs.remove(KEY_PART_SOURCES)
        }

        File(context.filesDir, THEME_ICONS_DIR).deleteRecursively()
        File(context.filesDir, "wallpaper").listFiles()?.forEach { it.delete() }
    }

    suspend fun delete(id: String): Unit = withContext(Dispatchers.IO) {
        // the theme's folder in the ECHO folder is deleted by ThemeFolderSync (owner, 2026-10-07); its name is
        // kept here as well, so a folder that could not be deleted is not read back in until it changes
        _themes.value.firstOrNull { it.id == id }?.let { dismiss(folderName(it)) }
        removeFiles(id)
        _themes.value = scan()
    }

    private fun removeFiles(id: String) {
        listOf("$id.$THEME_EXT", "$id.preview.jpg", "$id.wallpaper.jpg", "$id.$META", "$id.$CATALOG").forEach { File(dir, it).delete() }
        dir.listFiles { f -> f.name.startsWith("$id.hero.") }.orEmpty().forEach { it.delete() }
    }

    // which theme each part in use came from, by part; a part set by hand or by no theme is absent
    suspend fun partSources(): Map<ThemePart, String> = decodeSources(context.echoDataStore.data.first()[KEY_PART_SOURCES])

    // the README and the screenshots of a saved theme, the screenshots unpacked under the cache
    suspend fun details(id: String): ThemeDetails? = withContext(Dispatchers.IO) {
        val bundle = bundleFile(id)?.let { runCatching { EchoThemeCodec.read(it) }.getOrNull() } ?: return@withContext null
        val shotsDir = File(context.cacheDir, "theme-shots/$id").apply { deleteRecursively(); mkdirs() }
        val shots = bundle.screenshots.map { (name, image) -> File(shotsDir, name).apply { writeBytes(image.bytes) }.absolutePath }
        ThemeDetails(ThemeReadme.parse(bundle.readme), shots)
    }

    // records that [id] was downloaded from the online store as the file with checksum [sha256], so the store
    // can tell when the catalog has a newer one
    suspend fun markFromCatalog(id: String, sha256: String): SavedTheme? = withContext(Dispatchers.IO) {
        File(dir, "$id.$CATALOG").writeText(sha256)
        _themes.value = scan()
        _themes.value.firstOrNull { it.id == id }
    }

    // the hero and the metadata the store lists, written beside the theme so the list does not open every
    // theme. The list writes them for a theme that has none: a new one, or one stored before the store
    private fun writeSidecars(id: String, bundle: EchoThemeBundle) {
        dir.listFiles { f -> f.name.startsWith("$id.hero.") }.orEmpty().forEach { it.delete() }
        bundle.hero?.let { File(dir, "$id.hero.${it.extension.lowercase()}").writeBytes(it.bytes) }
        val readme = ThemeReadme.parse(bundle.readme)
        val meta = org.json.JSONObject()
            .put("author", readme.author).put("version", readme.version).put("description", readme.description)
            .put("parts", org.json.JSONArray(bundle.parts().map { it.name }))
        File(dir, "$id.$META").writeText(meta.toString())
    }

    private fun readSidecars(id: String, file: File): SavedTheme.() -> SavedTheme {
        val metaFile = File(dir, "$id.$META")
        if (!metaFile.isFile) runCatching { EchoThemeCodec.read(file) }.getOrNull()?.let { writeSidecars(id, it) }
        val meta = runCatching { org.json.JSONObject(metaFile.readText()) }.getOrNull()
        val hero = dir.listFiles { f -> f.name.startsWith("$id.hero.") }.orEmpty().firstOrNull()
        val catalogSha = runCatching { File(dir, "$id.$CATALOG").takeIf { it.isFile }?.readText()?.trim() }.getOrNull()
        return {
            copy(
                catalogSha = catalogSha,
                heroPath = hero?.absolutePath,
                author = meta?.optString("author")?.takeIf { it.isNotEmpty() },
                version = meta?.optString("version")?.takeIf { it.isNotEmpty() },
                description = meta?.optString("description")?.takeIf { it.isNotEmpty() },
                parts = meta?.optJSONArray("parts")?.let { a ->
                    (0 until a.length()).mapNotNull { i -> runCatching { ThemePart.valueOf(a.getString(i)) }.getOrNull() }.toSet()
                }.orEmpty(),
            )
        }
    }

    // the stored .echo-theme file of a saved theme
    fun bundleFile(id: String): File? = File(dir, "$id.$THEME_EXT").takeIf { it.isFile }

    // the theme's folder name in ECHO/Themes
    fun folderName(theme: SavedTheme): String = com.echo.themekit.EchoThemeFolder.folderName(theme.name, theme.id)

    // when the theme with this folder name was deleted in ECHO, or null
    fun dismissedAt(folder: String): Long? = readDismissed()[folder]

    private fun dismiss(name: String) = writeDismissed(readDismissed() + (name to System.currentTimeMillis()))

    private fun undismiss(name: String) = readDismissed().let { if (name in it) writeDismissed(it - name) }

    private fun readDismissed(): Map<String, Long> =
        runCatching { File(dir, DISMISSED_FILE).readLines() }.getOrDefault(emptyList())
            .mapNotNull { line -> line.split('\t').takeIf { it.size == 2 }?.let { (n, t) -> t.toLongOrNull()?.let { n to it } } }
            .toMap()

    private fun writeDismissed(names: Map<String, Long>) {
        dir.mkdirs()
        File(dir, DISMISSED_FILE).writeText(names.entries.joinToString("") { (n, t) -> "$n\t$t\n" })
    }

    suspend fun exportForShare(id: String): File? = withContext(Dispatchers.IO) {
        val src = File(dir, "$id.$THEME_EXT")
        if (!src.isFile) return@withContext null
        val name = _themes.value.firstOrNull { it.id == id }?.name ?: id
        val safe = name.replace(Regex("[^A-Za-z0-9 _-]"), "").trim().ifBlank { id }.replace(' ', '_')
        runCatching {
            val out = File(File(context.cacheDir, "shared_themes").apply { mkdirs() }, "$safe.$THEME_EXT")
            src.copyTo(out, overwrite = true)
            out
        }.onFailure { Timber.w(it, "EchoThemeStore: export failed") }.getOrNull()
    }

    sealed interface ImportResult {
        data class Success(val theme: SavedTheme) : ImportResult

        data class Unreadable(val cause: Throwable?) : ImportResult

        data object TooLarge : ImportResult

        data object OutOfMemory : ImportResult

        data object NotABundle : ImportResult

        data object DamagedWallpaper : ImportResult

        data class NotSaved(val cause: Throwable?) : ImportResult
    }

    suspend fun importBundleDetailed(uri: Uri): ImportResult = importBundleDetailed { context.contentResolver.openInputStream(uri) }

    // a theme from [open], such as one read from the ECHO folder. With [replacing], that saved theme is
    // removed once this one is stored: a theme edited in its folder takes the place of the old copy.
    suspend fun importBundleDetailed(replacing: String? = null, open: () -> java.io.InputStream?): ImportResult = withContext(Dispatchers.IO) {
        dir.mkdirs()
        val staging = File(dir, "import_${System.currentTimeMillis()}.tmp")
        val copied = try {
            val stream = open()
            if (stream == null) {
                Timber.w("EchoThemeStore: no stream to import")
                return@withContext ImportResult.Unreadable(null)
            }
            stream.use { input ->
                FileOutputStream(staging).use { out -> with(SafeMedia) { input.copyCappedTo(out) } }
            }
        } catch (e: Exception) {
            staging.delete()
            Timber.w(e, "EchoThemeStore: could not read the bundle")
            return@withContext ImportResult.Unreadable(e)
        }
        if (copied == null) {
            staging.delete()
            Timber.w(
                "EchoThemeStore: bundle exceeds the %d-byte read cap",
                SafeMedia.MAX_THEME_FILE_BYTES,
            )
            return@withContext ImportResult.TooLarge
        }

        val parsed = try {
            EchoThemeCodec.read(staging)
        } catch (e: OutOfMemoryError) {
            staging.delete()
            Timber.w(e, "EchoThemeStore: out of heap parsing a %d-byte bundle", copied)
            return@withContext ImportResult.OutOfMemory
        }
        val bundle = parsed ?: run {
            staging.delete()
            Timber.w("EchoThemeStore: %d bytes are not a theme bundle", copied)
            return@withContext ImportResult.NotABundle
        }

        val wallpaper = bundle.wallpaper?.let {
            SafeMedia.decodeBitmapCapped(it) ?: run {
                staging.delete()

                Timber.w("EchoThemeStore: wallpaper is %d bytes but would not decode", it.size)
                return@withContext ImportResult.DamagedWallpaper
            }
        }

        val saved = runCatching {
            val id = "pfp_${System.currentTimeMillis()}"
            val name = bundle.manifest.name.ifBlank { nextDefaultName() }

            val stored = File(dir, "$id.$THEME_EXT")
            replacing?.let(::removeFiles)
            undismiss(com.echo.themekit.EchoThemeFolder.folderName(name, id))
            if (!staging.renameTo(stored)) {
                staging.copyTo(stored, overwrite = true)
                staging.delete()
            }

            wallpaper?.let { wp ->
                FileOutputStream(File(dir, "$id.wallpaper.jpg")).use { wp.compress(Bitmap.CompressFormat.JPEG, 92, it) }
            }

            val preview = bundle.preview?.let { SafeMedia.decodeBitmapCapped(it) }
                ?: wallpaper?.let { downscale(it, maxEdge = 480) }
            preview?.let { p ->
                try {
                    FileOutputStream(File(dir, "$id.preview.jpg")).use { p.compress(Bitmap.CompressFormat.JPEG, 88, it) }
                } finally {
                    if (p !== wallpaper) p.recycle()
                }
            }
            _themes.value = scan()
            // the listed entry, which carries the store's details; the plain one if the list could not read it
            _themes.value.firstOrNull { it.id == id } ?: SavedTheme(
                id,
                name,
                bundle.manifest.accentColor.toAccentArgbOrNull(),
                File(dir, "$id.preview.jpg").takeIf { it.isFile }?.absolutePath,
            )
        }
        wallpaper?.recycle()

        if (staging.exists()) staging.delete()
        saved.fold(
            onSuccess = { ImportResult.Success(it) },
            onFailure = { cause ->
                Timber.w(cause, "EchoThemeStore: bundle parsed but could not be saved")
                if (cause is OutOfMemoryError) ImportResult.OutOfMemory
                else ImportResult.NotSaved(cause)
            },
        )
    }

    suspend fun saveCurrentLook(name: String): SavedTheme? = withContext(Dispatchers.IO) {
        runCatching { currentLook(name).use { save(it) } }
            .onFailure { Timber.w(it, "EchoThemeStore: saveCurrentLook failed") }.getOrNull()
    }

    // the look in use, as a theme: what Save as Theme writes, and what an apply keeps first
    private class Look(val bundle: EchoThemeBundle, val wallpaper: Bitmap?, val preview: Bitmap?) : java.io.Closeable {
        val isDefault: Boolean get() = bundle.wallpaper == null && bundle.motion == null && bundle.icons.isEmpty() &&
            bundle.sysicons.isEmpty() && bundle.media.isEmpty() && bundle.manifest.accentColor.isBlank() &&
            bundle.manifest.settings.orEmpty().values.all { it is JsonNull }

        // the look's content without its name or date, so the same look gives the same print
        fun print(): String {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            val sink = object : java.io.OutputStream() {
                override fun write(b: Int) = digest.update(b.toByte())
                override fun write(b: ByteArray, off: Int, len: Int) = digest.update(b, off, len)
            }
            EchoThemeCodec.write(bundle.copy(manifest = bundle.manifest.copy(name = "", created = null)), sink)
            return digest.digest().joinToString("") { "%02x".format(it) }
        }

        override fun close() {
            if (preview !== wallpaper) preview?.recycle()
            wallpaper?.recycle()
        }
    }

    private suspend fun currentLook(name: String): Look {
        val prefs = context.echoDataStore.data.first()

        val customDir = File(context.filesDir, CustomIconStore.CUSTOM_ICONS_DIR)
        val themeIconsDir = File(context.filesDir, THEME_ICONS_DIR)
        val icons = mutableMapOf<String, ThemeImage>()
        val sysicons = mutableMapOf<String, ThemeImage>()
        for (slot in CustomizableIcons.ALL) {
            val source = findIconFile(customDir, slot.key) ?: findIconFile(themeIconsDir, slot.key) ?: continue
            if (source.extension.equals("gif", ignoreCase = true)) {
                iconsOrSysicons(slot.key, ThemeImage(source.readBytes(), "gif"), icons, sysicons)
            } else {
                val bitmap = SafeMedia.decodeFileCapped(source.absolutePath, maxDimension = 512)
                    ?: continue
                val png = ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
                bitmap.recycle()
                iconsOrSysicons(slot.key, ThemeImage(png, "png"), icons, sysicons)
            }
        }

        val wallpaperBitmap = prefs[KEY_CUSTOM_WALLPAPER]
            ?.let { runCatching { SafeMedia.decodeFileCapped(it, maxDimension = 1920) }.getOrNull() }
        val wallpaperPng = wallpaperBitmap?.let {
            ByteArrayOutputStream().also { out -> it.compress(Bitmap.CompressFormat.PNG, 100, out) }.toByteArray()
        }

        val motion = prefs[KEY_MOTION_WALLPAPER]
            ?.let { path ->
                val file = File(path)
                val ext = file.extension.lowercase()
                if (file.isFile && ext in setOf("mp4", "webm", "gif")) ThemeMotion.ofFile(file, ext) else null
            }

        val manifest = EchoThemeManifest(
            name = name.ifBlank { nextDefaultName() },
            accentColor = prefs[KEY_ACCENT_OVERRIDE]?.let { "#%06X".format(it and 0xFFFFFF) } ?: "",
            iconColor = prefs[KEY_ICON_COLOR]?.let { "#%06X".format(it and 0xFFFFFF) }
                ?: EchoThemeManifest.ICON_COLOR_AUTO,
            textColor = prefs[KEY_TEXT_COLOR]?.let { "#%06X".format(it and 0xFFFFFF) }
                ?: EchoThemeManifest.ICON_COLOR_AUTO,
            waveStyle = when (prefs[KEY_WAVE_STYLE]) {
                WAVE_STYLE_STATIC -> EchoThemeManifest.WAVE_STATIC
                WAVE_STYLE_REDUCED -> EchoThemeManifest.WAVE_REDUCED
                else -> EchoThemeManifest.WAVE_ANIMATED
            },
            layout = prefs[KEY_THEME_LAYOUT]
                ?.let { com.echo.themekit.CrossbarLayoutSpecCodec.decode(it) }
                ?.let(com.echo.themekit.CrossbarLayoutSpecCodec::sanitize)
                ?.takeUnless { it == com.echo.themekit.CrossbarLayoutSpec.DEFAULT },
            source = EchoThemeSource(type = EchoThemeSource.TYPE_USER_CREATED),
            created = LocalDate.now().toString(),
            waveDesign = prefs[KEY_WAVE_DESIGN]?.takeIf { it in EchoThemeManifest.WAVE_DESIGNS },
            gameBootStyle = prefs[KEY_GAMEBOOT_STYLE]?.takeIf { it in EchoThemeManifest.GAME_START_STYLES },
            launchDiscStyle = prefs[KEY_LAUNCH_DISC_STYLE]?.takeIf { it in EchoThemeManifest.GAME_START_STYLES },
            buttonSet = prefs[KEY_BUTTON_SET]?.takeIf { it in EchoThemeManifest.BUTTON_SETS },
            settings = prefs.themeSettings(),
        )

        // the sounds, boot and game-start media in use
        val media = com.echo.core.domain.model.UiMediaSlot.entries.mapNotNull { slot ->
            uiMedia.pathFor(slot)?.let(::File)?.takeIf { it.isFile }?.let { slot.key to ThemeImage(it.readBytes(), it.extension.lowercase()) }
        }.toMap()

        val preview = wallpaperBitmap?.let { downscale(it, maxEdge = 480) }
        val previewBytes = preview?.let {
            ByteArrayOutputStream().also { out -> it.compress(Bitmap.CompressFormat.PNG, 90, out) }.toByteArray()
        }
        val bundle = EchoThemeBundle(
            manifest = manifest,
            wallpaper = wallpaperPng,
            preview = previewBytes,
            icons = icons,
            sysicons = sysicons,
            motion = motion,
            media = media,
        )
        return Look(bundle, wallpaperBitmap, preview)
    }

    private fun save(look: Look): SavedTheme {
        dir.mkdirs()
        val id = "pfp_${System.currentTimeMillis()}"
        val manifest = look.bundle.manifest

        FileOutputStream(File(dir, "$id.$THEME_EXT")).use { out -> EchoThemeCodec.write(look.bundle, out) }
        look.wallpaper?.let {
            FileOutputStream(File(dir, "$id.wallpaper.jpg")).use { out -> it.compress(Bitmap.CompressFormat.JPEG, 92, out) }
        }
        look.preview?.let {
            FileOutputStream(File(dir, "$id.preview.jpg")).use { out -> it.compress(Bitmap.CompressFormat.JPEG, 88, out) }
        }

        _themes.value = scan()
        // the listed entry, which carries the store's details; the plain one if the list could not read it
        return _themes.value.firstOrNull { it.id == id } ?: SavedTheme(
            id,
            manifest.name,
            manifest.accentColor.toAccentArgbOrNull(),
            File(dir, "$id.preview.jpg").takeIf { f -> f.isFile }?.absolutePath,
        )
    }

    private fun iconsOrSysicons(
        slotKey: String,
        image: ThemeImage,
        icons: MutableMap<String, ThemeImage>,
        sysicons: MutableMap<String, ThemeImage>,
    ) {
        if (slotKey.startsWith("sysicon_")) sysicons[slotKey.removePrefix("sysicon_")] = image
        else icons[slotKey] = image
    }

    private fun findIconFile(dir: File, slotKey: String): File? =
        setOf("png", "jpg", "webp", "bmp", "heif", "gif")
            .asSequence()
            .map { File(dir, "$slotKey.$it") }
            .firstOrNull { it.isFile }

    private fun scan(): List<SavedTheme> =
        dir.listFiles { f -> f.name.endsWith(".$THEME_EXT") }.orEmpty()
            .sortedByDescending { it.lastModified() }
            .mapNotNull { file ->
                val id = file.name.removeSuffix(".$THEME_EXT")

                val manifest = runCatching { EchoThemeCodec.readManifest(file) }
                    .onFailure { Timber.w(it, "EchoThemeStore: could not read %s", file.name) }
                    .getOrNull() ?: return@mapNotNull null
                SavedTheme(
                    id = id,
                    name = manifest.name,
                    accentArgb = manifest.accentColor.toAccentArgbOrNull(),
                    previewPath = File(dir, "$id.preview.jpg").takeIf { it.isFile }?.absolutePath,
                ).let(readSidecars(id, file))
            }

    private fun nextDefaultName(): String {
        val existing = _themes.value.map { it.name }.toSet()
        var n = 1
        while ("Custom Theme $n" in existing) n++
        return "Custom Theme $n"
    }

    private fun downscale(src: Bitmap, maxEdge: Int): Bitmap {
        val edge = maxOf(src.width, src.height)
        if (edge <= maxEdge) return src
        val scale = maxEdge.toFloat() / edge
        return Bitmap.createScaledBitmap(src, (src.width * scale).toInt().coerceAtLeast(1), (src.height * scale).toInt().coerceAtLeast(1), true)
    }

    private fun String.toAccentArgbOrNull(): Long? {
        val hex = removePrefix("#")
        if (hex.length != 6) return null
        return hex.toLongOrNull(16)?.let { 0xFF000000L or it }
    }

    companion object {
        // the parts a theme may set; their owners keep the same keys (GameBootPreferences,
        // LaunchDiscPreferences, ControllerLayoutRepository, the wave design in Display settings)
        private val KEY_WAVE_DESIGN = stringPreferencesKey("display_wave_design")
        private val KEY_GAMEBOOT_STYLE = stringPreferencesKey("display_gameboot_style")
        private val KEY_LAUNCH_DISC_STYLE = stringPreferencesKey("display_launch_disc_style")
        private val KEY_BUTTON_SET = stringPreferencesKey("controller_display_type")

        // which theme each part came from, as PART=name lines (owner, 2026-10-07: a theme page)
        val KEY_PART_SOURCES = stringPreferencesKey("theme_part_sources")
        private const val META = "meta.json"
        private const val CATALOG = "catalog-sha256"

        internal fun encodeSources(sources: Map<ThemePart, String>): String =
            sources.entries.joinToString("\n") { (part, name) -> "${part.name}=${name.replace('\n', ' ')}" }

        fun decodeSources(text: String?): Map<ThemePart, String> =
            text.orEmpty().lines().mapNotNull { line ->
                val i = line.indexOf('=').takeIf { it > 0 } ?: return@mapNotNull null
                runCatching { ThemePart.valueOf(line.substring(0, i)) }.getOrNull()?.let { it to line.substring(i + 1) }
            }.toMap()

        // names of themes deleted in ECHO, so their folder in the ECHO folder is not read back in
        private const val DISMISSED_FILE = "folder-dismissed.txt"

        private val KEY_CUSTOM_WALLPAPER = stringPreferencesKey("display_custom_wallpaper")

        private val KEY_MOTION_WALLPAPER = stringPreferencesKey("display_motion_wallpaper")
        private val KEY_WAVE_STYLE = stringPreferencesKey("display_wave_style")
        private val KEY_ICON_COLOR = longPreferencesKey("theme_icon_color")

        private val KEY_TEXT_COLOR = longPreferencesKey("display_text_color")

        private const val WAVE_STYLE_ANIMATED = "ANIMATED"
        private const val WAVE_STYLE_REDUCED = "REDUCED"
        private const val WAVE_STYLE_STATIC = "STATIC"

        const val THEME_ICONS_DIR = "theme-icons"

        val KEY_THEME_ICONS_STAMP = longPreferencesKey("theme_icons_stamp")

        val KEY_THEME_LAYOUT = stringPreferencesKey("theme_layout_spec")

        val KEY_APPLIED_THEME_NAME = stringPreferencesKey("theme_applied_name")
    }
}

// a theme's settings onto the preferences: a value sets the setting, null puts it back to ECHO's default
internal fun MutablePreferences.applyThemeSettings(settings: JsonObject?) {
    ThemeSettings.clean(settings)?.forEach { (key, value) ->
        val primitive = value as? JsonPrimitive
        when (ThemeSettings.KEYS.getValue(key)) {
            ThemeSettings.Kind.BOOL -> booleanPreferencesKey(key).let { k ->
                val on = primitive?.takeUnless { it is JsonNull }?.booleanOrNull
                if (on != null) this[k] = on else remove(k)
            }
            ThemeSettings.Kind.TEXT -> stringPreferencesKey(key).let { k ->
                val text = primitive?.takeUnless { it is JsonNull }?.content
                if (text != null) this[k] = text else remove(k)
            }
        }
    }
}

// every theme setting as it stands, null for one never set, so the look kept before a theme puts each back
internal fun Preferences.themeSettings(): JsonObject = buildJsonObject {
    ThemeSettings.KEYS.forEach { (key, kind) ->
        when (kind) {
            ThemeSettings.Kind.BOOL -> put(key, this@themeSettings[booleanPreferencesKey(key)])
            ThemeSettings.Kind.TEXT -> put(key, this@themeSettings[stringPreferencesKey(key)])
        }
    }
}
