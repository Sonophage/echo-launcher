package com.echo.core.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.echo.core.data.datastore.echoDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import com.echo.core.data.database.dao.PhotoDao
import com.echo.core.data.database.dao.PhotoLibraryDao
import com.echo.core.data.database.entity.toDomain
import com.echo.core.data.database.entity.toEntity
import com.echo.core.domain.model.Photo
import com.echo.core.domain.model.PhotoLibrary
import com.echo.core.domain.repository.PhotoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private val KEY_PHOTO_DEFAULT_VIEWER = stringPreferencesKey("photo_default_viewer")

@Singleton
class PhotoRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val libraryDao: PhotoLibraryDao,
    private val photoDao: PhotoDao,
) : PhotoRepository {
    override fun observeLibraries(): Flow<List<PhotoLibrary>> =
        libraryDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getLibraries(): List<PhotoLibrary> =
        libraryDao.getAll().map { it.toDomain() }

    override suspend fun getLibrary(id: String): PhotoLibrary? =
        libraryDao.getById(id)?.toDomain()

    override suspend fun addLibrary(
        displayName: String,
        treeUri: String,
        scanRecursively: Boolean,
    ): PhotoLibrary {
        val now = System.currentTimeMillis()
        val library = PhotoLibrary(
            id = UUID.randomUUID().toString(),
            displayName = displayName,
            treeUri = treeUri,
            enabled = true,
            scanRecursively = scanRecursively,
            createdAt = now,
            updatedAt = now,
        )
        libraryDao.upsert(library.toEntity())
        Timber.i("Photo library added: \"$displayName\" -> $treeUri")
        return library
    }

    override suspend fun renameLibrary(id: String, displayName: String) =
        libraryDao.setDisplayName(id, displayName, System.currentTimeMillis())

    override suspend fun setLibraryTreeUri(id: String, treeUri: String) =
        libraryDao.setTreeUri(id, treeUri, System.currentTimeMillis())

    override suspend fun setLibraryScanRecursively(id: String, scanRecursively: Boolean) =
        libraryDao.setScanRecursively(id, scanRecursively, System.currentTimeMillis())

    override suspend fun removeLibrary(id: String) {
        val thumbs = photoDao.getForLibrary(id).mapNotNull { it.thumbnailUri }

        photoDao.deleteForLibrary(id)
        libraryDao.delete(id)
        deleteOrphanedThumbnails(context, thumbs) { photoDao.countReferencingThumbnail(it) > 0 }
        Timber.i("Photo library removed: $id")
    }

    override fun observeAllPhotos(): Flow<List<Photo>> =
        photoDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observePhotosByLibrary(libraryId: String): Flow<List<Photo>> =
        photoDao.observeByLibrary(libraryId).map { list -> list.map { it.toDomain() } }

    override suspend fun getPhoto(id: String): Photo? =
        photoDao.getById(id)?.toDomain()

    override suspend fun getPhotosForLibrary(libraryId: String): List<Photo> =
        photoDao.getForLibrary(libraryId).map { it.toDomain() }

    override suspend fun replacePhotosForLibrary(
        libraryId: String,
        photos: List<Photo>,
        scannedAt: Long,
    ) {
        val thumbs = photoDao.getForLibrary(libraryId).mapNotNull { it.thumbnailUri }
        photoDao.replaceForLibrary(libraryId, photos.map { it.toEntity() })
        libraryDao.updateScanResult(libraryId, photos.size, scannedAt)
        // owner, 2026-10-10: a rescan clears out the thumbnails of photos no longer there
        deleteOrphanedThumbnails(context, thumbs) { photoDao.countReferencingThumbnail(it) > 0 }
        Timber.i("Replaced ${photos.size} photos for library $libraryId")
    }

    override fun observeFavorites(): Flow<List<Photo>> =
        photoDao.observeFavorites().map { list -> list.map { it.toDomain() } }

    override suspend fun setFavorite(id: String, favorite: Boolean) =
        photoDao.setFavorite(id, favorite)

    override suspend fun removePhoto(id: String) {
        val photo = photoDao.getById(id)
        val thumb = photo?.thumbnailUri
        photoDao.deleteById(id)
        photo?.let { libraryDao.recount(it.libraryId) }
        if (thumb != null) {
            deleteOrphanedThumbnails(context, listOf(thumb)) { photoDao.countReferencingThumbnail(it) > 0 }
        }
    }

    override fun observeNewestArtUris(limit: Int): Flow<List<String>> =
        photoDao.observeNewestArtUris(limit)

    override fun observeDefaultViewer(): Flow<String?> =
        context.echoDataStore.data.map { it[KEY_PHOTO_DEFAULT_VIEWER] }

    override suspend fun setDefaultViewer(value: String?) {
        context.echoDataStore.edit { prefs ->
            if (value.isNullOrBlank()) prefs.remove(KEY_PHOTO_DEFAULT_VIEWER)
            else prefs[KEY_PHOTO_DEFAULT_VIEWER] = value
        }
    }
}
