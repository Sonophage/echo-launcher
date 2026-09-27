package com.psplauncher.feature.artwork.api

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.psplauncher.core.data.datastore.pfpDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArtworkScrapePreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val downloadClearLogosFlow: Flow<Boolean> =
        context.pfpDataStore.data.map { it[KEY_DOWNLOAD_CLEAR_LOGOS] ?: true }

    val downloadManualsFlow: Flow<Boolean> =
        context.pfpDataStore.data.map { it[KEY_DOWNLOAD_MANUALS] ?: true }

    val downloadVideoSnapsFlow: Flow<Boolean> =
        context.pfpDataStore.data.map { it[KEY_DOWNLOAD_VIDEO_SNAPS] ?: false }

    suspend fun getOptions(): ScrapeOptions {
        val prefs = context.pfpDataStore.data.first()
        return ScrapeOptions(
            downloadClearLogos      = prefs[KEY_DOWNLOAD_CLEAR_LOGOS] ?: true,
            downloadManuals         = prefs[KEY_DOWNLOAD_MANUALS]     ?: true,
            downloadVideoSnaps      = prefs[KEY_DOWNLOAD_VIDEO_SNAPS] ?: false,
        )
    }

    suspend fun setDownloadClearLogos(value: Boolean) =
        context.pfpDataStore.edit { it[KEY_DOWNLOAD_CLEAR_LOGOS] = value }

    suspend fun setDownloadManuals(value: Boolean) =
        context.pfpDataStore.edit { it[KEY_DOWNLOAD_MANUALS] = value }

    suspend fun setDownloadVideoSnaps(value: Boolean) =
        context.pfpDataStore.edit { it[KEY_DOWNLOAD_VIDEO_SNAPS] = value }

    companion object {
        private val KEY_DOWNLOAD_CLEAR_LOGOS = booleanPreferencesKey("pref_dl_clear_logos")
        private val KEY_DOWNLOAD_MANUALS     = booleanPreferencesKey("pref_dl_manuals")
        private val KEY_DOWNLOAD_VIDEO_SNAPS = booleanPreferencesKey("pref_dl_video_snaps")
    }
}
