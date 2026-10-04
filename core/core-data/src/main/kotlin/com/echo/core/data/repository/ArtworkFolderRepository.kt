package com.echo.core.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.echo.core.data.datastore.echoDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

private val KEY_ARTWORK_FOLDER_TREE_URI = stringPreferencesKey("artwork_folder_tree_uri")

private val KEY_ARTWORK_STORAGE_MODE = stringPreferencesKey("artwork_storage_mode")

private val KEY_ARTWORK_LIBRARY_UUID = stringPreferencesKey("artwork_library_uuid")

enum class ArtworkStorageMode { INTERNAL, PORTABLE;
    companion object {
        fun fromName(name: String?): ArtworkStorageMode =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: INTERNAL
    }
}

@Singleton
class ArtworkFolderRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val treeUri: Flow<String?> = context.echoDataStore.data.map { it[KEY_ARTWORK_FOLDER_TREE_URI] }

    val storageMode: Flow<ArtworkStorageMode> =
        context.echoDataStore.data.map { ArtworkStorageMode.fromName(it[KEY_ARTWORK_STORAGE_MODE]) }

    suspend fun getTreeUri(): String? =
        context.echoDataStore.data.first()[KEY_ARTWORK_FOLDER_TREE_URI]

    suspend fun getStorageMode(): ArtworkStorageMode =
        ArtworkStorageMode.fromName(context.echoDataStore.data.first()[KEY_ARTWORK_STORAGE_MODE])

    suspend fun getLibraryUuid(): String? =
        context.echoDataStore.data.first()[KEY_ARTWORK_LIBRARY_UUID]

    suspend fun setTreeUri(treeUri: String?) {
        context.echoDataStore.edit { prefs ->
            if (treeUri.isNullOrBlank()) prefs.remove(KEY_ARTWORK_FOLDER_TREE_URI)
            else prefs[KEY_ARTWORK_FOLDER_TREE_URI] = treeUri
        }
        Timber.i("Artwork folder set: $treeUri")
    }

    suspend fun setStorageMode(mode: ArtworkStorageMode) {
        context.echoDataStore.edit { it[KEY_ARTWORK_STORAGE_MODE] = mode.name.lowercase() }
        Timber.i("Artwork storage mode: $mode")
    }

    suspend fun setLibraryUuid(uuid: String?) {
        context.echoDataStore.edit { prefs ->
            if (uuid.isNullOrBlank()) prefs.remove(KEY_ARTWORK_LIBRARY_UUID)
            else prefs[KEY_ARTWORK_LIBRARY_UUID] = uuid
        }
    }

    fun persist(uri: Uri) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }.onFailure { Timber.w(it, "Could not persist artwork folder permission for $uri") }
    }

    suspend fun hasLiveGrant(): Boolean {
        val stored = getTreeUri() ?: return false
        return context.contentResolver.persistedUriPermissions.any {
            it.uri.toString() == stored && it.isReadPermission && it.isWritePermission
        }
    }

    suspend fun forget() {
        getTreeUri()?.let { stored ->
            runCatching {
                context.contentResolver.releasePersistableUriPermission(
                    Uri.parse(stored),
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            }.onFailure { Timber.w(it, "Could not release artwork folder permission for $stored") }
        }
        context.echoDataStore.edit { prefs ->
            prefs.remove(KEY_ARTWORK_FOLDER_TREE_URI)
            prefs.remove(KEY_ARTWORK_STORAGE_MODE)
            prefs.remove(KEY_ARTWORK_LIBRARY_UUID)
        }
        Timber.i("Artwork folder forgotten (grant released, files untouched)")
    }
}
