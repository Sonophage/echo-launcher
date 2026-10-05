package com.echo.feature.artwork.api

import android.content.Context
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.crossfade
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import com.echo.feature.artwork.match.FileTitleSearchStore
import com.echo.feature.artwork.match.TitleSearchStore
import okio.Path.Companion.toOkioPath
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ArtworkModule {
    @Provides
    @Singleton
    fun provideTitleSearchStore(@ApplicationContext context: Context): TitleSearchStore =
        FileTitleSearchStore(context.cacheDir.resolve("match-searches"))

    @Provides
    @Singleton
    fun provideCoilImageLoader(@ApplicationContext context: Context): ImageLoader =
        ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.20)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("artwork_cache").toOkioPath())
                    .maxSizeBytes(512L * 1024 * 1024)
                    .build()
            }
            .crossfade(true)

            .components {
                add(coil3.gif.AnimatedImageDecoder.Factory())
            }
            .build()
}
