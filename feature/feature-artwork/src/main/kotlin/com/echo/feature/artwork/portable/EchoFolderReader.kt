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
// and Look/Boot, and the newest image in Look/Wallpapers. Runs on start and on Reload in Settings.
@Singleton
class EchoFolderReader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val folderRepository: ArtworkFolderRepository,
    private val library: PortableArtworkLibrary,
    private val uiMediaStore: UiMediaStore,
    private val stillWallpaper: StillWallpaper,
) {
    data class Report(val settings: Int, val sounds: Int, val wallpaper: Boolean, val skipped: List<String>, val folderMissing: Boolean = false) {
        fun message(): String = when {
            folderMissing -> "ECHO has no folder it can read. Link the ECHO folder first."
            settings == 0 && sounds == 0 && !wallpaper -> "The ECHO folder matches ECHO; nothing to apply."
            else -> listOfNotNull(
                "$settings setting${if (settings == 1) "" else "s"}".takeIf { settings > 0 },
                "$sounds sound${if (sounds == 1) "" else "s"}".takeIf { sounds > 0 },
                "the wallpaper".takeIf { wallpaper },
            ).joinToString(", ", prefix = "Applied ", postfix = " from the ECHO folder.")
        } + if (skipped.isNotEmpty()) " Skipped: ${skipped.joinToString()}." else ""
    }

    // [always]: Reload applies settings.json whatever its age; on start it is applied only when it is
    // newer than ECHO's own settings, which means it was edited outside ECHO
    suspend fun read(always: Boolean): Report {
        val tree = folderRepository.getTreeUri()?.takeIf { folderRepository.hasLiveGrant() }?.let(Uri::parse)
            ?: return Report(0, 0, false, emptyList(), folderMissing = true)
        val (settings, skipped) = readSettings(tree, always)
        return Report(settings, readSounds(tree), readWallpaper(tree), skipped).also { Timber.i("ECHO folder read: ${it.message()}") }
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
                if (result.ok) applied++ else Timber.w("ECHO folder: ${file.name} not applied: ${result.message}")
            }
        }
        return applied
    }

    // the newest image that is not already ECHO's wallpaper: ECHO's own copy (current.*) is always the
    // same bytes, so a picture dropped in beside it wins however recently the copy was written
    private suspend fun readWallpaper(tree: Uri): Boolean {
        val own = context.echoDataStore.data.first()[stringPreferencesKey("display_custom_wallpaper")]?.let(::File)?.takeIf { it.isFile }
        val newest = library.filesIn(tree, listOf(DIR_LOOK, "Wallpapers"))
            .filter { it.name.substringAfterLast('.').lowercase() in STILL_IMAGES }
            .filterNot { own != null && library.sameContent(it, own) }
            .maxByOrNull { it.lastModified ?: 0L } ?: return false
        if (!EchoFolder.shouldRead(newest.lastModified, own?.lastModified(), sameContent = false)) return false
        return stillWallpaper.apply { context.contentResolver.openInputStream(newest.uri) } == StillWallpaper.Result.APPLIED
    }

    private companion object {
        const val MAX_SETTINGS_BYTES = 256 * 1024
        val STILL_IMAGES = setOf("jpg", "jpeg", "png", "webp")
    }
}
