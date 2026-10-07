package com.echo.feature.artwork.portable

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.echo.core.data.datastore.echoDataStore
import com.echo.core.data.repository.ArtworkFolderRepository
import com.echo.core.data.repository.EchoFolder
import com.echo.core.data.repository.EchoSettingsExport
import com.echo.core.data.repository.UiMediaStore
import com.echo.core.data.wallpaper.StillWallpaper
import com.echo.core.domain.model.UiMediaSlot
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import timber.log.Timber

// reads the ECHO folder back into ECHO (owner, 2026-10-04): settings.json, the sounds in Look/Sounds
// and Look/Boot, icons in Look/Icons, a font in Look/Fonts, and the newest picture or motion wallpaper
// in Look/Wallpapers. Runs on start and on Reload in Settings.
@Singleton
class EchoFolderReader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val folderRepository: ArtworkFolderRepository,
    private val library: PortableArtworkLibrary,
    private val uiMediaStore: UiMediaStore,
    private val stillWallpaper: StillWallpaper,
    private val motionWallpaper: com.echo.core.data.wallpaper.MotionWallpaper,
    private val customIcons: com.echo.core.data.repository.CustomIconStore,
    private val themeFolders: ThemeFolderSync,
) {
    data class Report(
        val settings: Int, val sounds: Int, val wallpaper: Boolean, val skipped: List<String>,
        val folderMissing: Boolean = false, val icons: Int = 0, val font: Boolean = false, val themes: Int = 0,
    ) {
        fun message(): String = when {
            folderMissing -> "ECHO has no folder it can read. Link the ECHO folder first."
            settings == 0 && sounds == 0 && icons == 0 && themes == 0 && !wallpaper && !font -> "The ECHO folder matches ECHO; nothing to apply."
            else -> listOfNotNull(
                "$settings setting${if (settings == 1) "" else "s"}".takeIf { settings > 0 },
                "$sounds sound${if (sounds == 1) "" else "s"}".takeIf { sounds > 0 },
                "$icons icon${if (icons == 1) "" else "s"}".takeIf { icons > 0 },
                "the font".takeIf { font },
                "$themes theme${if (themes == 1) "" else "s"}".takeIf { themes > 0 },
                "the wallpaper".takeIf { wallpaper },
            ).joinToString(", ", prefix = "Applied ", postfix = " from the ECHO folder.")
        } + if (skipped.isNotEmpty()) " Skipped: ${skipped.joinToString()}." else ""
    }

    // files the folder offered that ECHO refused, with the reason, so the person sees why
    private val rejected = mutableListOf<String>()

    // [always]: Reload applies settings.json whatever its age; on start it is applied only when it is
    // newer than ECHO's own settings, which means it was edited outside ECHO
    suspend fun read(always: Boolean): Report {
        val tree = folderRepository.getTreeUri()?.takeIf { folderRepository.hasLiveGrant() }?.let(Uri::parse)
            ?: return Report(0, 0, false, emptyList(), folderMissing = true)
        val (settings, skipped) = readSettings(tree, always)
        rejected.clear()
        val themes = themeFolders.readIn(tree)
        return Report(settings, readSounds(tree), readWallpaper(tree), skipped, icons = readIcons(tree), font = readFont(tree), themes = themes.themes)
            .let { it.copy(skipped = it.skipped + rejected + themes.rejected) }
            .also { Timber.i("ECHO folder read: ${it.message()}") }
    }

    private suspend fun readSettings(tree: Uri, always: Boolean): Pair<Int, List<String>> {
        val (text, modified) = library.readRootText(tree, EchoSettingsExport.FILE_NAME, MAX_SETTINGS_BYTES) ?: return 0 to emptyList()
        val ownModified = File(context.filesDir, "datastore/pfp_prefs.preferences_pb").lastModified()
        if (!always && (modified == null || modified <= ownModified)) return 0 to emptyList()
        val parsed = EchoSettingsExport.parse(text) ?: return 0 to listOf(EchoSettingsExport.FILE_NAME)
        val kinds = EchoSettingsExport.GROUPS.values.fold(emptyMap<String, EchoSettingsExport.Kind>()) { a, m -> a + m }
        var changed = 0
        context.echoDataStore.edit { prefs ->
            parsed.values.forEach { (key, value) -> if (prefs.put(key, kinds.getValue(key), value)) changed++ }
        }
        return changed to parsed.skipped
    }

    // stores [value] under [key] as [kind]; true when it changed anything
    private fun MutablePreferences.put(key: String, kind: EchoSettingsExport.Kind, value: Any): Boolean {
        fun <T : Any> set(k: androidx.datastore.preferences.core.Preferences.Key<T>, v: T) = (this[k] != v).also { if (it) this[k] = v }
        return when (kind) {
            EchoSettingsExport.Kind.BOOL -> set(booleanPreferencesKey(key), value as Boolean)
            EchoSettingsExport.Kind.INT -> set(intPreferencesKey(key), value as Int)
            EchoSettingsExport.Kind.LONG -> set(longPreferencesKey(key), value as Long)
            EchoSettingsExport.Kind.FLOAT -> set(floatPreferencesKey(key), value as Float)
            EchoSettingsExport.Kind.TEXT, EchoSettingsExport.Kind.JSON -> set(stringPreferencesKey(key), value as String)
        }
    }

    private suspend fun readSounds(tree: Uri): Int {
        var applied = 0
        for (dir in listOf("Sounds", "Boot")) {
            for (file in library.filesIn(tree, listOf(DIR_LOOK, dir))) {
                val slot = UiMediaSlot.fromKey(file.name.substringBeforeLast('.')) ?: continue
                val own = uiMediaStore.pathFor(slot)?.let(::File)
                if (!EchoFolder.shouldRead(file.lastModified, own?.lastModified(), own != null && library.sameContent(file, own))) continue
                val result = uiMediaStore.import(slot, file.uri)
                if (result.ok) applied++ else rejected += "${file.name} (${result.message})"
            }
        }
        return applied
    }

    // the newest picture or motion file that is not already ECHO's wallpaper: ECHO's own copies
    // (current.*) are always the same bytes, so a file dropped in beside them wins however recently
    // the copies were written
    private suspend fun readWallpaper(tree: Uri): Boolean {
        val prefs = context.echoDataStore.data.first()
        val own = listOf("display_custom_wallpaper", "display_motion_wallpaper")
            .mapNotNull { prefs[stringPreferencesKey(it)]?.let(::File)?.takeIf { f -> f.isFile } }
        val newest = library.filesIn(tree, listOf(DIR_LOOK, "Wallpapers"))
            .filter { it.name.substringAfterLast('.').lowercase().let { ext -> ext in STILL_IMAGES || ext in MOTION } }
            .filterNot { file -> own.any { library.sameContent(file, it) } }
            .maxByOrNull { it.lastModified ?: 0L } ?: return false
        if (!EchoFolder.shouldRead(newest.lastModified, own.maxOfOrNull { it.lastModified() }, sameContent = false)) return false
        val motionMime = MOTION[newest.name.substringAfterLast('.').lowercase()]
        return if (motionMime != null) {
            motionWallpaper.apply(motionMime, newest.sizeBytes) { context.contentResolver.openInputStream(newest.uri) }
                .also { if (!it.applied) rejected += "${newest.name} (${it.message})" }.applied
        } else {
            stillWallpaper.apply { context.contentResolver.openInputStream(newest.uri) } == StillWallpaper.Result.APPLIED
        }
    }

    // icons named after their slot replace ECHO's custom icon for that slot
    private suspend fun readIcons(tree: Uri): Int {
        val ownDir = File(context.filesDir, com.echo.core.data.repository.CustomIconStore.CUSTOM_ICONS_DIR)
        var applied = 0
        for (file in library.filesIn(tree, listOf(DIR_LOOK, "Icons"))) {
            val key = file.name.substringBeforeLast('.')
            if (!com.echo.themekit.CustomizableIcons.isValidKey(key)) continue
            val own = ownDir.listFiles().orEmpty().firstOrNull { it.nameWithoutExtension == key }
            if (!EchoFolder.shouldRead(file.lastModified, own?.lastModified(), own != null && library.sameContent(file, own))) continue
            val result = customIcons.import(key, file.uri, file.mime)
            if (result.ok) applied++ else rejected += "${file.name} (${result.message})"
        }
        return applied
    }

    // the first font in Look/Fonts becomes ECHO's font; with none there, ECHO goes back to Sora.
    // True when the font in use changed.
    private suspend fun readFont(tree: Uri): Boolean {
        val dir = File(context.filesDir, FONT_DIR).apply { mkdirs() }
        val source = library.filesIn(tree, listOf(DIR_LOOK, "Fonts"))
            .filter { it.name.substringAfterLast('.').lowercase() in FONTS }
            .minByOrNull { it.name.lowercase() }
        val local = dir.listFiles().orEmpty().firstOrNull()
        if (source == null) {
            val had = local != null
            dir.listFiles().orEmpty().forEach { it.delete() }
            com.echo.core.ui.theme.EchoFonts.use(null)
            return had
        }
        val target = File(dir, "font.${source.name.substringAfterLast('.').lowercase()}")
        val changed = local == null || !library.sameContent(source, local)
        if (changed) {
            dir.listFiles().orEmpty().forEach { it.delete() }
            val copied = runCatching {
                context.contentResolver.openInputStream(source.uri)?.use { i -> target.outputStream().use { i.copyTo(it) } } != null
            }.getOrDefault(false)
            if (!copied) return false
        }
        val inUse = com.echo.core.ui.theme.EchoFonts.use(target)
        if (!inUse) rejected += "${source.name} (not a font Android can read; ECHO keeps Sora)"
        return changed && inUse
    }

    private companion object {
        const val MAX_SETTINGS_BYTES = 256 * 1024
        const val FONT_DIR = "echo-font"
        val STILL_IMAGES = setOf("jpg", "jpeg", "png", "webp")
        val MOTION = mapOf("mp4" to "video/mp4", "m4v" to "video/mp4", "webm" to "video/webm", "gif" to "image/gif")
        val FONTS = setOf("ttf", "otf")
    }
}
