package com.echo.feature.artwork.api

import android.content.Context
import com.echo.core.domain.model.MovieFileName
import com.echo.core.domain.repository.VideoRepository
import com.echo.feature.artwork.store.ArtworkKind
import com.echo.feature.artwork.store.ArtworkTempIO
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class VideoPosterResult(val matched: Int, val skipped: Int, val failed: Int) {
    fun message(): String = when {
        matched == 0 && failed == 0 && skipped == 0 -> "No videos to match"
        matched == 0 -> "No posters found for $failed ${if (failed == 1) "film" else "films"}"
        failed == 0 -> "Matched $matched of ${matched + skipped + failed}"
        else -> "Matched $matched, $failed not found"
    }
}

@Singleton
class VideoPosterFetcher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val videoRepository: VideoRepository,
    private val tmdb: TmdbApi,
    private val http: HttpClient,
    private val keyProvider: TmdbApiKeyProvider,
) {
    private val posterDir: File get() = File(context.filesDir, "video_posters").apply { mkdirs() }

    suspend fun run(refreshExisting: Boolean = false): VideoPosterResult = withContext(Dispatchers.IO) {
        if (!keyProvider.hasKey()) return@withContext VideoPosterResult(0, 0, 0)

        val videos = videoRepository.getAllVideos()
        var matched = 0
        var skipped = 0
        var failed = 0

        for (video in videos) {
            if (!refreshExisting && posterOnDisk(video.posterUri) { File(it).exists() }) {
                skipped++
                continue
            }
            val title = MovieFileName.bareTitleOf(video.displayName)
            val year = MovieFileName.yearOf(video.displayName)

            val movie = tmdb.findMovie(title, year)
            val url = movie?.posterUrl
            if (url == null) {
                failed++
                continue
            }

            val temp = ArtworkTempIO.downloadToTemp(http, context.cacheDir, ArtworkKind.BACKGROUND, url)
            if (temp == null) {
                failed++
                continue
            }
            val dest = File(posterDir, "${video.id}.jpg")
            val moved = runCatching {
                temp.copyTo(dest, overwrite = true)
                temp.delete()
                true
            }.getOrDefault(false)

            if (moved) {
                videoRepository.setPosterUri(video.id, "file://${dest.absolutePath}")
                matched++
                Timber.i("TMDB matched \"${video.displayName}\" to ${movie.title} (${movie.year})")
            } else {
                failed++
            }
        }
        VideoPosterResult(matched, skipped, failed)
    }

    // points a poster back at this app's own copy when its stored path is gone: a restore from before
    // 2.4.1, or 2.0.0's app id change, left paths naming com.psplauncher. Run on start; returns how many
    suspend fun repointMoved(): Int = withContext(Dispatchers.IO) {
        var moved = 0
        for (video in videoRepository.getAllVideos()) {
            val fixed = movedPosterUri(video.posterUri, File(posterDir, "${video.id}.jpg").absolutePath) { File(it).exists() }
                ?: continue
            videoRepository.setPosterUri(video.id, fixed)
            moved++
        }
        if (moved > 0) Timber.i("Repointed $moved video posters to this app's folder")
        moved
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        videoRepository.getAllVideos().forEach { video ->
            if (!video.posterUri.isNullOrBlank()) videoRepository.setPosterUri(video.id, null)
        }
        runCatching { posterDir.listFiles()?.forEach { it.delete() } }
        Unit
    }
}

// the poster URI to store instead of [stored], or null to keep it: only when the stored file is missing
// and this app holds the poster at [ownPath]
internal fun movedPosterUri(stored: String?, ownPath: String, exists: (String) -> Boolean): String? {
    val path = stored?.removePrefix("file://")?.takeIf { it.isNotBlank() } ?: return null
    if (path == ownPath || exists(path) || !exists(ownPath)) return null
    return "file://$ownPath"
}

// whether a stored poster can be drawn; a path left by a restore on another device has no file, so
// the next match fetches it again instead of skipping the film
internal fun posterOnDisk(uri: String?, exists: (String) -> Boolean): Boolean =
    uri?.removePrefix("file://")?.takeIf { it.isNotBlank() }?.let(exists) == true
