package com.echo.feature.themes

import android.content.Context
import android.net.Uri
import com.echo.core.data.database.dao.ThemeDao
import com.echo.core.data.database.entity.toDomain
import com.echo.core.domain.model.EchoTheme
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

interface ThemeRepository {
    fun observeActiveTheme(): Flow<EchoTheme?>

    fun observeAll(): Flow<List<EchoTheme>>

    suspend fun setActiveTheme(id: String)

    suspend fun installTheme(uri: Uri): ThemeLoadResult

    suspend fun uninstallTheme(id: String)
}

@Singleton
class ThemeRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val themeDao: ThemeDao,
    private val loader: CrossbarThemeLoader,
) : ThemeRepository {
    override fun observeActiveTheme(): Flow<EchoTheme?> =
        themeDao.observeAll().map { themes -> themes.firstOrNull { it.isActive }?.toDomain() }

    override fun observeAll(): Flow<List<EchoTheme>> =
        themeDao.observeAll().map { themes -> themes.map { it.toDomain() } }

    override suspend fun setActiveTheme(id: String) {
        themeDao.setActiveTheme(id)
        Timber.i("Active theme set: $id")
    }

    override suspend fun installTheme(uri: Uri): ThemeLoadResult =
        loader.loadFromUri(uri)

    override suspend fun uninstallTheme(id: String) {
        themeDao.deleteUserTheme(id)
        File(context.filesDir, "themes/$id").deleteRecursively()
        Timber.i("Theme uninstalled: $id")
    }
}
