package com.echo.core.data.database.seeder

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.echo.core.data.database.dao.ArtworkRecordDao
import com.echo.core.data.database.dao.GameDao
import com.echo.core.data.datastore.echoDataStore
import com.echo.core.data.wallpaper.WallpaperLuminanceProbe
import com.echo.core.data.wallpaper.WallpaperLuminanceProbe.clearWallpaperLuma
import com.echo.core.data.wallpaper.WallpaperLuminanceProbe.setWallpaperLuma
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.io.File
import androidx.room.withTransaction
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StartupDataPrep @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gameDao: GameDao,
    private val artworkRecordDao: ArtworkRecordDao,
    private val db: com.echo.core.data.database.EchoDatabase,
) {
    suspend fun run(currentVersionCode: Int) {
        val prefs = context.echoDataStore.data.first()
        val alreadyPrepped = prefs[KEY_DATA_PREP_VERSION] == currentVersionCode

        runCatching {
            if (!alreadyPrepped) {
                normalizeGameArtwork()
                normalizeWallpaper()
            }

            if (prefs[KEY_ART_COLUMN_REPAIR] != true) {
                repointColumnsAtTheirRecords()
                context.echoDataStore.edit { it[KEY_ART_COLUMN_REPAIR] = true }
            }

            healWallpaperSurvey()
            context.echoDataStore.edit(::wipeRetiredKeys)
        }.onFailure { Timber.e(it, "Startup data prep failed") }

        if (alreadyPrepped) return
        context.echoDataStore.edit { it[KEY_DATA_PREP_VERSION] = currentVersionCode }
        Timber.i("Startup data prep complete for versionCode=$currentVersionCode")
    }

    private suspend fun repointColumnsAtTheirRecords() {
        var repaired = 0
        db.withTransaction { gameDao.getAll().forEach { g ->
            val icon = primaryUserAsset(g.id, "ICON")
            val artwork = primaryUserAsset(g.id, "BACKGROUND")
            val logo = primaryUserAsset(g.id, "LOGO")

            if (icon != null && icon != g.iconUri) { gameDao.updateIconUri(g.id, icon); repaired++ }
            if (artwork != null && artwork != g.artworkUri) { gameDao.updateArtwork(g.id, artwork); repaired++ }
            if (logo != null && logo != g.logoUri) { gameDao.updateLogo(g.id, logo); repaired++ }
        } }
        if (repaired > 0) Timber.i("Repointed $repaired artwork column(s) at the record that owns them")
    }

    private suspend fun primaryUserAsset(gameId: Long, type: String): String? =
        artworkRecordDao.getAt(gameId, type, 0)?.takeIf { it.userAssigned }?.documentUri

    private suspend fun normalizeGameArtwork() {
        val filesDir = context.filesDir.absolutePath
        var repaired = 0
        db.withTransaction { gameDao.getAll().forEach { g ->
            val artwork = resolve(g.artworkUri, filesDir)
            val logo    = resolve(g.logoUri, filesDir)
            val icon    = resolve(g.iconUri, filesDir)

            if (artwork != g.artworkUri) gameDao.updateArtwork(g.id, artwork)
            if (logo    != g.logoUri)    gameDao.updateLogo(g.id, logo)
            if (icon    != g.iconUri)    gameDao.updateIconUri(g.id, icon)

            if (artwork != g.artworkUri || logo != g.logoUri || icon != g.iconUri) {
                repaired++
            }
        } }
        if (repaired > 0) Timber.i("Re-homed artwork paths on $repaired game(s)")
    }

    private suspend fun normalizeWallpaper() {
        val filesDir = context.filesDir.absolutePath
        val current = context.echoDataStore.data.first()[KEY_CUSTOM_WALLPAPER] ?: return
        val resolved = resolve(current, filesDir)
        if (resolved == current) return
        context.echoDataStore.edit { prefs ->
            if (resolved == null) prefs.remove(KEY_CUSTOM_WALLPAPER)
            else prefs[KEY_CUSTOM_WALLPAPER] = resolved
        }
    }

    private suspend fun healWallpaperSurvey() {
        val prefs = context.echoDataStore.data.first()
        val wallpaper = prefs[KEY_CUSTOM_WALLPAPER]
        val storedLuma = prefs[WallpaperLuminanceProbe.KEY_WALLPAPER_LUMA]
        val storedAccent = prefs[WallpaperLuminanceProbe.KEY_WALLPAPER_ACCENT]

        if (wallpaper == null) {
            if (storedLuma != null || storedAccent != null) {
                context.echoDataStore.edit { it.clearWallpaperLuma() }
            }
            return
        }

        if (WallpaperLuminanceProbe.describes(storedLuma, wallpaper) && storedAccent != null) return

        val fresh = WallpaperLuminanceProbe.survey(wallpaper)
        if (fresh?.luma == storedLuma && fresh?.accentArgb == storedAccent) return
        context.echoDataStore.edit { it.setWallpaperLuma(fresh) }
    }

    private fun resolve(path: String?, filesDirPath: String): String? {
        if (path.isNullOrEmpty()) return path
        val idx = path.indexOf(FILES_MARKER)
        if (idx < 0) return path
        val remapped = filesDirPath.trimEnd('/') + "/" + path.substring(idx + FILES_MARKER.length)
        return if (File(remapped).exists()) remapped else null
    }

    private companion object {
        val KEY_DATA_PREP_VERSION = intPreferencesKey("data_prep_version")


        val KEY_ART_COLUMN_REPAIR = booleanPreferencesKey("art_column_repair_done")
        val KEY_CUSTOM_WALLPAPER  = stringPreferencesKey("display_custom_wallpaper")
        const val FILES_MARKER = "/files/"
    }
}

internal val RETIRED_ACHIEVEMENT_KEYS = setOf(
    "goldberg_installer_enabled",
    "local_steam_tracking_enabled",
)

internal fun wipeRetiredKeys(prefs: androidx.datastore.preferences.core.MutablePreferences) {
    prefs.asMap().keys.filter { it.name in RETIRED_ACHIEVEMENT_KEYS }.forEach { prefs.remove(it) }
}
