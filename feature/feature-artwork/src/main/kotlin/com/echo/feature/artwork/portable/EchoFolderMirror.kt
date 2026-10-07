package com.echo.feature.artwork.portable

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.stringPreferencesKey
import com.echo.core.data.datastore.echoDataStore
import com.echo.core.data.repository.ArtworkFolderRepository
import com.echo.core.data.repository.CustomIconStore
import com.echo.core.data.repository.EchoFolder
import com.echo.core.data.repository.EchoSettingsExport
import com.echo.core.data.repository.UiMediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber

// keeps the ECHO folder's copy of ECHO's settings and look current (owner, 2026-10-04): settings.json
// when a setting changes, and Look/ when a sound, icon or the wallpaper changes. EchoFolderReader runs
// first on start, so an edit made while ECHO was closed is read before anything is written.
@Singleton
class EchoFolderMirror @Inject constructor(
    @ApplicationContext private val context: Context,
    private val folderRepository: ArtworkFolderRepository,
    private val library: PortableArtworkLibrary,
    private val reader: EchoFolderReader,
    private val themeFolders: ThemeFolderSync,
    private val themeStore: com.echo.core.data.repository.EchoThemeStore,
) {
    private val readLock = Mutex()

    // the folder whose settings and look ECHO has already read
    private var readFolder: String? = null

    @OptIn(FlowPreview::class)
    fun start(scope: CoroutineScope) = scope.launch {
        val tree = folderRepository.treeUri
        readLock.withLock { readFolder = tree.first() }
        // the folder is read before anything is written to it, or an edit made while ECHO was closed
        // would be written over
        runCatching { reader.read(always = false) }.onFailure { Timber.w(it, "ECHO folder: reading on start failed") }
        val prefs = context.echoDataStore.data
        launch {
            combine(tree, prefs.map { p -> EchoSettingsExport.json(p.asMap().mapKeys { it.key.name }) }) { t, json -> t to json }
                .distinctUntilChanged()
                .debounce(SETTLE_MS)
                .collect { (t, json) ->
                    val live = liveTree(t) ?: return@collect
                    // reading a new folder can change the settings, so they are taken again after it
                    if (readIfNewlyLinked(t)) writeSettings(live, EchoSettingsExport.json(prefs.first().asMap().mapKeys { it.key.name }))
                    else writeSettings(live, json)
                }
        }
        launch {
            combine(tree, prefs.map { p -> LOOK_TRIGGERS.map { p.asMap().entries.firstOrNull { e -> e.key.name == it }?.value } }) { t, look -> t to look }
                .distinctUntilChanged()
                .debounce(SETTLE_MS)
                .collect { (t, _) ->
                    val live = liveTree(t) ?: return@collect
                    readIfNewlyLinked(t)
                    syncLook(live)
                }
        }
        launch {
            combine(tree, themeStore.themes) { t, themes -> t to themes }
                .distinctUntilChanged()
                .debounce(SETTLE_MS)
                .collect { (t, _) ->
                    val live = liveTree(t) ?: return@collect
                    readIfNewlyLinked(t)
                    library.ensureEchoLayout(live)
                    Timber.i("ECHO folder: ${themeFolders.writeOut(live)} theme folders written")
                }
        }
    }

    // a folder linked while ECHO runs (setup, or a new pick in Settings) can hold settings.json and
    // Look/ from an earlier install; they are applied before ECHO writes its own over them. True when
    // it read the folder now.
    internal suspend fun readIfNewlyLinked(tree: String?): Boolean = readLock.withLock {
        if (tree == null || tree == readFolder) return@withLock false
        readFolder = tree
        runCatching { reader.read(always = true) }.onFailure { Timber.w(it, "ECHO folder: reading the new folder failed") }
        true
    }

    private suspend fun liveTree(tree: String?): Uri? =
        tree?.takeIf { folderRepository.hasLiveGrant() }?.let(Uri::parse)

    private suspend fun writeSettings(tree: Uri, json: String) {
        if (!library.writeRootText(tree, EchoSettingsExport.FILE_NAME, "application/json", json)) {
            Timber.w("Could not write ${EchoSettingsExport.FILE_NAME} to the ECHO folder")
        }
    }

    private suspend fun syncLook(tree: Uri) {
        // the folders and README are refreshed too, so a folder linked by an older version catches up
        library.ensureEchoLayout(tree)
        val files = context.filesDir
        var copied = 0
        suspend fun mirror(segments: List<String>, file: File, name: String = file.name) {
            if (file.isFile && library.mirrorFile(tree, segments, name, file)) copied++
        }
        File(files, UiMediaStore.UI_MEDIA_DIR).listFiles().orEmpty().forEach { mirror(listOf(DIR_LOOK, EchoFolder.lookFolderFor(it.name)), it) }
        // only the user's own icons: a theme's icons read back would become custom icons that
        // outlive the theme
        File(files, CustomIconStore.CUSTOM_ICONS_DIR).listFiles().orEmpty().forEach { mirror(listOf(DIR_LOOK, "Icons"), it) }
        // only the wallpaper in use: the folder holds what ECHO shows, not every one it has kept
        val current = context.echoDataStore.data.first()
        // one name for the wallpaper in use, so the folder does not collect a copy per change
        for (key in WALLPAPER_KEYS) current[stringPreferencesKey(key)]?.let { path ->
            val file = File(path)
            mirror(listOf(DIR_LOOK, "Wallpapers"), file, "current.${file.extension}")
        }
        Timber.i("ECHO folder: Look/ checked, $copied files current")
    }

    private companion object {
        const val SETTLE_MS = 2_000L
        val WALLPAPER_KEYS = listOf("display_custom_wallpaper", "display_motion_wallpaper")
        val LOOK_TRIGGERS = WALLPAPER_KEYS + listOf("ui_media_stamp", "custom_icons_stamp")
    }
}
