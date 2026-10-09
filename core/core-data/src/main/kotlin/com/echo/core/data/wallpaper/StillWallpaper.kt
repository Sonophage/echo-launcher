package com.echo.core.data.wallpaper

import android.content.Context
import android.graphics.BitmapFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.echo.core.data.datastore.echoDataStore
import com.echo.core.data.wallpaper.WallpaperAccentProbe.setWallpaperAccent
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// the one way a still image becomes ECHO's wallpaper, for Settings and for the ECHO folder: copied in,
// checked that it decodes, its brightness surveyed, saved, and the files it replaces removed
@Singleton
class StillWallpaper @Inject constructor(@ApplicationContext private val context: Context) {
    enum class Result { APPLIED, UNREADABLE, SAVE_FAILED }

    val dir: File get() = File(context.filesDir, DIR).apply { mkdirs() }

    suspend fun apply(open: () -> InputStream?): Result = withContext(Dispatchers.IO) {
        val dest = File(dir, "wallpaper_${System.currentTimeMillis()}.jpg")
        val read = runCatching { open()?.use { input -> dest.outputStream().use { input.copyTo(it) } } != null }.getOrDefault(false)
        if (!read || !decodes(dest)) {
            runCatching { dest.delete() }
            return@withContext Result.UNREADABLE
        }
        val survey = WallpaperAccentProbe.survey(dest.absolutePath)
        try {
            context.echoDataStore.edit {
                it[KEY_CUSTOM] = dest.absolutePath
                it.remove(KEY_MOTION)
                it.setWallpaperAccent(survey)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return@withContext Result.SAVE_FAILED
        }
        prune(keep = listOf(dest))
        Result.APPLIED
    }

    suspend fun prune(keep: List<File>) = withContext(Dispatchers.IO) {
        val names = keep.map { it.name }.toSet()
        dir.listFiles()?.forEach { if (it.name !in names) runCatching { it.delete() } }
    }

    private fun decodes(file: File): Boolean {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, opts)
        return opts.outWidth > 0 && opts.outHeight > 0
    }

    companion object {
        const val DIR = "wallpaper"
        private val KEY_CUSTOM = stringPreferencesKey("display_custom_wallpaper")
        private val KEY_MOTION = stringPreferencesKey("display_motion_wallpaper")
    }
}
