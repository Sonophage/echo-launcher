package com.echo.feature.artwork.store

import com.echo.core.data.database.dao.ArtworkRecordDao
import com.echo.core.data.database.dao.CategoryDao
import com.echo.core.data.database.dao.GameDao
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArtworkReferences @Inject constructor(
    private val gameDao: GameDao,
    private val artworkRecordDao: ArtworkRecordDao,
    private val categoryDao: CategoryDao,
) {
    suspend fun all(): Set<String> = buildSet {
        gameDao.getAll().forEach { addAll(pathsOf(it.artworkUri, it.logoUri, it.iconUri)) }
        artworkRecordDao.getAll().forEach { addAll(pathsOf(it.documentUri, it.prevDocumentUri)) }
        categoryDao.getAll().forEach { addAll(pathsOf(it.customIconUri)) }
    }

    private fun pathsOf(vararg refs: String?): List<String> =
        refs.filterNotNull().map { it.trim() }.filter { it.isNotEmpty() }
}
