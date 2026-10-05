package com.echo.core.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.echo.core.data.datastore.echoDataStore
import com.echo.core.domain.model.VideoSnapPlacement
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IconDisplayPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val animatedIconsFlow: Flow<Boolean> = context.echoDataStore.data
        .map { it[KEY_ANIMATED_ICONS] ?: true }

    suspend fun setAnimatedIcons(enabled: Boolean) =
        context.echoDataStore.edit { it[KEY_ANIMATED_ICONS] = enabled }

    val snapPlacementFlow: Flow<VideoSnapPlacement> = context.echoDataStore.data
        .map { VideoSnapPlacement.fromName(it[KEY_SNAP_PLACEMENT]) ?: VideoSnapPlacement.DEFAULT }

    suspend fun setSnapPlacement(placement: VideoSnapPlacement) =
        context.echoDataStore.edit { it[KEY_SNAP_PLACEMENT] = placement.name }

    val gameMetadataFlow: Flow<Boolean> = context.echoDataStore.data
        .map { it[KEY_GAME_METADATA] ?: true }

    suspend fun setGameMetadata(enabled: Boolean) =
        context.echoDataStore.edit { it[KEY_GAME_METADATA] = enabled }

    val itemBackdropFlow: Flow<Boolean> = context.echoDataStore.data
        .map { it[KEY_ITEM_BACKDROP] ?: true }

    suspend fun setItemBackdrop(enabled: Boolean) =
        context.echoDataStore.edit { it[KEY_ITEM_BACKDROP] = enabled }

    // true: game rows show their cover art; false: their icon
    val rowCoverArtFlow: Flow<Boolean> = context.echoDataStore.data
        .map { it[KEY_ROW_COVER_ART] ?: false }

    suspend fun setRowCoverArt(enabled: Boolean) =
        context.echoDataStore.edit { it[KEY_ROW_COVER_ART] = enabled }

    val lingerDelaySecondsFlow: Flow<Float> = context.echoDataStore.data
        .map { (it[KEY_ICON1_LINGER_DELAY_SECONDS] ?: 1.5f).coerceIn(1f, 5f) }

    suspend fun setLingerDelaySeconds(seconds: Float) =
        context.echoDataStore.edit { it[KEY_ICON1_LINGER_DELAY_SECONDS] = seconds.coerceIn(1f, 5f) }

    companion object {
        private val KEY_ANIMATED_ICONS = androidx.datastore.preferences.core.booleanPreferencesKey("pref_animated_icons")
        private val KEY_ICON1_LINGER_DELAY_SECONDS =
            floatPreferencesKey("pref_icon1_linger_delay_seconds")
        private val KEY_SNAP_PLACEMENT = stringPreferencesKey("pref_video_snap_placement")
        private val KEY_GAME_METADATA =
            androidx.datastore.preferences.core.booleanPreferencesKey("pref_xmb_game_metadata")
        private val KEY_ROW_COVER_ART =
            androidx.datastore.preferences.core.booleanPreferencesKey("pref_xmb_row_cover_art")
        private val KEY_ITEM_BACKDROP =
            androidx.datastore.preferences.core.booleanPreferencesKey("pref_xmb_item_backdrop")
    }
}
