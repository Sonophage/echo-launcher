package com.echo.core.data.database.di

import android.content.Context
import androidx.room.Room
import com.echo.core.data.database.EchoDatabase
import com.echo.core.data.database.dao.AccountAchievementDao
import com.echo.core.data.database.dao.AccountAchievementSetDao
import com.echo.core.data.database.dao.AchievementMatchNoteDao
import com.echo.core.data.database.dao.AppOverrideDao
import com.echo.core.data.database.dao.ArtworkImportReportDao
import com.echo.core.data.database.dao.ArtworkRecordDao
import com.echo.core.data.database.dao.BackupDao
import com.echo.core.data.database.dao.CategoryDao
import com.echo.core.data.database.dao.CollectionDao
import com.echo.core.data.database.dao.GameDao
import com.echo.core.data.database.dao.LaunchOutcomeDao
import com.echo.core.data.database.dao.LibrarySourceDao
import com.echo.core.data.database.dao.MemoryCardDao
import com.echo.core.data.database.dao.MusicFolderDao
import com.echo.core.data.database.dao.MusicTrackDao
import com.echo.core.data.database.dao.PlaylistDao
import com.echo.core.data.database.dao.PlaySessionDao
import com.echo.core.data.database.dao.PlatformDao
import com.echo.core.data.database.dao.HiddenPlacementDao
import com.echo.core.data.database.dao.BookDao
import com.echo.core.data.database.dao.BookLibraryDao
import com.echo.core.data.database.dao.PhotoDao
import com.echo.core.data.database.dao.PhotoLibraryDao
import com.echo.core.data.database.dao.ProviderGameLinkDao
import com.echo.core.data.database.dao.ScanTombstoneDao
import com.echo.core.data.database.dao.SteamOwnedGamesDao
import com.echo.core.data.database.dao.VideoDao
import com.echo.core.data.database.dao.VideoLibraryDao
import com.echo.core.data.database.dao.VideoPlaylistDao
import com.echo.core.data.repository.GameRepositoryImpl
import com.echo.core.data.repository.MusicRepositoryImpl
import com.echo.core.data.repository.BookRepositoryImpl
import com.echo.core.data.repository.PhotoRepositoryImpl
import com.echo.core.data.repository.VideoRepositoryImpl
import com.echo.core.domain.repository.GameRepository
import com.echo.core.domain.repository.MusicRepository
import com.echo.core.domain.repository.BookRepository
import com.echo.core.domain.repository.PhotoRepository
import com.echo.core.domain.repository.VideoRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideEchoDatabase(@ApplicationContext context: Context): EchoDatabase =
        Room.databaseBuilder(
            context,
            EchoDatabase::class.java,
            EchoDatabase.DATABASE_NAME,
        )

        .addMigrations(*EchoDatabase.ALL_MIGRATIONS)
        .build()

    @Provides fun provideGameDao(db: EchoDatabase): GameDao = db.gameDao()
    @Provides fun provideLaunchOutcomeDao(db: EchoDatabase): LaunchOutcomeDao = db.launchOutcomeDao()
    @Provides fun provideSsMediaCacheDao(db: EchoDatabase): com.echo.core.data.database.dao.SsMediaCacheDao = db.ssMediaCacheDao()
    @Provides fun providePlatformDao(db: EchoDatabase): PlatformDao = db.platformDao()
    @Provides fun provideCategoryDao(db: EchoDatabase): CategoryDao = db.categoryDao()
    @Provides fun providePlaySessionDao(db: EchoDatabase): PlaySessionDao = db.playSessionDao()
    @Provides fun provideLibrarySourceDao(db: EchoDatabase): LibrarySourceDao = db.librarySourceDao()
    @Provides fun provideMemoryCardDao(db: EchoDatabase): MemoryCardDao = db.memoryCardDao()
    @Provides fun provideAppOverrideDao(db: EchoDatabase): AppOverrideDao = db.appOverrideDao()
    @Provides fun provideCollectionDao(db: EchoDatabase): CollectionDao = db.collectionDao()
    @Provides fun provideMusicFolderDao(db: EchoDatabase): MusicFolderDao = db.musicFolderDao()
    @Provides fun provideMusicTrackDao(db: EchoDatabase): MusicTrackDao = db.musicTrackDao()
    @Provides fun providePlaylistDao(db: EchoDatabase): PlaylistDao = db.playlistDao()
    @Provides fun provideVideoLibraryDao(db: EchoDatabase): VideoLibraryDao = db.videoLibraryDao()
    @Provides fun provideVideoDao(db: EchoDatabase): VideoDao = db.videoDao()
    @Provides fun provideVideoPlaylistDao(db: EchoDatabase): VideoPlaylistDao = db.videoPlaylistDao()
    @Provides fun provideHiddenPlacementDao(db: EchoDatabase): HiddenPlacementDao = db.hiddenPlacementDao()
    @Provides fun providePhotoLibraryDao(db: EchoDatabase): PhotoLibraryDao = db.photoLibraryDao()
    @Provides fun providePhotoDao(db: EchoDatabase): PhotoDao = db.photoDao()
    @Provides fun provideBookLibraryDao(db: EchoDatabase): BookLibraryDao = db.bookLibraryDao()
    @Provides fun provideBookDao(db: EchoDatabase): BookDao = db.bookDao()
    @Provides fun provideScanTombstoneDao(db: EchoDatabase): ScanTombstoneDao = db.scanTombstoneDao()
    @Provides fun provideBackupDao(db: EchoDatabase): BackupDao = db.backupDao()
    @Provides fun provideArtworkRecordDao(db: EchoDatabase): ArtworkRecordDao = db.artworkRecordDao()
    @Provides fun provideArtworkImportReportDao(db: EchoDatabase): ArtworkImportReportDao = db.artworkImportReportDao()
    @Provides fun provideAccountAchievementSetDao(db: EchoDatabase): AccountAchievementSetDao = db.accountAchievementSetDao()
    @Provides fun provideAccountAchievementDao(db: EchoDatabase): AccountAchievementDao = db.accountAchievementDao()
    @Provides fun provideProviderGameLinkDao(db: EchoDatabase): ProviderGameLinkDao = db.providerGameLinkDao()
    @Provides fun provideAchievementMatchNoteDao(db: EchoDatabase): AchievementMatchNoteDao = db.achievementMatchNoteDao()
    @Provides fun provideSteamOwnedGamesDao(db: EchoDatabase): SteamOwnedGamesDao = db.steamOwnedGamesDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindGameRepository(impl: GameRepositoryImpl): GameRepository

    @Binds
    @Singleton
    abstract fun bindMusicRepository(impl: MusicRepositoryImpl): MusicRepository

    @Binds
    @Singleton
    abstract fun bindVideoRepository(impl: VideoRepositoryImpl): VideoRepository

    @Binds
    @Singleton
    abstract fun bindPhotoRepository(impl: PhotoRepositoryImpl): PhotoRepository

    @Binds
    abstract fun bindBookRepository(impl: BookRepositoryImpl): BookRepository
}
