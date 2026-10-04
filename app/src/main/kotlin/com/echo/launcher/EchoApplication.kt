package com.echo.launcher

import android.app.Application
import androidx.work.Configuration
import com.echo.core.data.database.seeder.DatabaseInitializer
import com.echo.core.data.database.seeder.StartupDataPrep
import com.echo.feature.artwork.api.ArtworkImageCache
import com.echo.feature.launcher.EmulatorAutoConfigService
import com.echo.feature.launcher.EmulatorProfileRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class EchoApplication : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: androidx.hilt.work.HiltWorkerFactory
    @Inject lateinit var databaseInitializer: DatabaseInitializer
    @Inject lateinit var startupDataPrep: StartupDataPrep
    @Inject lateinit var emulatorProfileRepository: EmulatorProfileRepository
    @Inject lateinit var emulatorAutoConfigService: EmulatorAutoConfigService
    @Inject lateinit var artworkImageCache: ArtworkImageCache
    @Inject lateinit var echoFolderMirror: com.echo.feature.artwork.portable.EchoFolderMirror
    @Inject lateinit var systemWallpaperFollow: com.echo.core.data.wallpaper.SystemWallpaperFollow
    @Inject lateinit var videoPosterFetcher: com.echo.feature.artwork.api.VideoPosterFetcher

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        initLogging()

        artworkImageCache.installAsSingleton()
        initDatabase()
        initEmulators()
        echoFolderMirror.start(appScope)
        systemWallpaperFollow.start(appScope)
        appScope.launch { runCatching { videoPosterFetcher.repointMoved() }.onFailure { Timber.w(it, "Poster repoint failed") } }
    }

    private fun initDatabase() {
        appScope.launch {
            runCatching {
                databaseInitializer.initialize()

                startupDataPrep.run(appVersionCode())
            }.onFailure { Timber.e(it, "Database initialization failed") }
        }
    }

    private fun appVersionCode(): Int = runCatching {
        packageManager.getPackageInfo(packageName, 0).longVersionCode.toInt()
    }.getOrDefault(0)

    private fun initEmulators() {
        appScope.launch {
            runCatching {
                emulatorProfileRepository.initialize()
                emulatorAutoConfigService.runOnStartup()
            }.onFailure { Timber.e(it, "Emulator initialization failed") }
        }
    }

    private fun initLogging() {
        if (BuildConfig.DEBUG) {
            Timber.plant(object : Timber.DebugTree() {
                override fun log(priority: Int, tag: String?, message: String, t: Throwable?) =
                    super.log(priority, tag, com.echo.core.common.logging.LogRedaction.redact(message), t)
            })
        }

        Timber.plant(
            com.echo.core.common.logging.EchoFileLoggingTree(
                java.io.File(filesDir, "logs")
            )
        )
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(
                if (BuildConfig.DEBUG) android.util.Log.DEBUG
                else android.util.Log.WARN
            )
            .build()
}
