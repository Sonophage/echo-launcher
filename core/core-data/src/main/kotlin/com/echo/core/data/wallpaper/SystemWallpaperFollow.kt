package com.echo.core.data.wallpaper

import android.app.WallpaperManager
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.echo.core.data.datastore.echoDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber

// owner, 2026-10-04: when ECHO's background changes, Android's home and lock wallpapers follow it.
// The still image is used (a motion wallpaper keeps a still poster under the same key). Clearing
// ECHO's wallpaper leaves Android's as it is. A wallpaper already applied is not applied again.
@Singleton
class SystemWallpaperFollow @Inject constructor(@ApplicationContext private val context: Context) {
    fun start(scope: CoroutineScope) = scope.launch {
        context.echoDataStore.data
            .map { it[KEY_WALLPAPER] to it[KEY_APPLIED] }
            .distinctUntilChanged()
            .collect { (current, applied) ->
                if (current != null && current != applied && File(current).isFile) apply(current)
            }
    }

    private suspend fun apply(path: String) {
        val ok = runCatching {
            File(path).inputStream().use {
                WallpaperManager.getInstance(context).setStream(it, null, true, WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)
            }
        }.onFailure { Timber.w(it, "Could not set the Android wallpaper") }.isSuccess
        if (ok) {
            context.echoDataStore.edit { it[KEY_APPLIED] = path }
            Timber.i("Android home and lock wallpapers follow ECHO's: $path")
        }
    }

    private companion object {
        val KEY_WALLPAPER = stringPreferencesKey("display_custom_wallpaper")
        // internal: the ECHO wallpaper last given to Android
        val KEY_APPLIED = stringPreferencesKey("system_wallpaper_applied_path")
    }
}
