package com.echo.core.data.wallpaper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.media.MediaMetadataRetriever
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.echo.core.data.datastore.echoDataStore
import com.echo.core.data.wallpaper.WallpaperLuminanceProbe.setWallpaperLuma
import com.echo.themekit.MotionLimits
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// the one way a video or animated image becomes ECHO's motion wallpaper, for Settings and for the
// ECHO folder: copied in, probed against MotionLimits, given a still poster, surveyed, saved, and the
// files it replaces removed
@Singleton
class MotionWallpaper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val still: StillWallpaper,
) {
    // [message] is what the person is told; null when it applied
    data class Result(val applied: Boolean, val message: String?)

    suspend fun apply(mime: String, knownSize: Long?, open: () -> InputStream?): Result = withContext(Dispatchers.IO) {
        if (knownSize != null && knownSize > MotionLimits.MAX_BYTES) return@withContext Result(false, MotionLimits.MSG_TOO_LARGE_BYTES)
        val stamp = System.currentTimeMillis()
        val ext = when (mime) {
            "video/webm" -> "webm"
            "image/gif" -> "gif"
            "image/webp" -> "webp"
            else -> "mp4"
        }
        val motion = File(still.dir, "wallpaper_$stamp.$ext")
        val poster = File(still.dir, "wallpaper_$stamp.jpg")
        fun fail(message: String, step: String): Result {
            timber.log.Timber.w("Motion wallpaper not applied at $step: $message")
            runCatching { motion.delete() }
            return Result(false, message)
        }

        val copied = runCatching { open()?.use { input -> motion.outputStream().use { input.copyTo(it) } } != null }.getOrDefault(false)
        if (!copied) return@withContext fail(MotionLimits.MSG_UNDECODABLE, "copy")
        val probe = probe(motion, mime)
        val rejection = probe?.let { MotionLimits.validate(it) }
        if (probe == null || rejection != null) return@withContext fail(rejection ?: MotionLimits.MSG_UNDECODABLE, "probe $probe")
        val frame = poster(motion, mime) ?: return@withContext fail(MotionLimits.MSG_UNDECODABLE, "poster")
        val posterOk = runCatching { poster.outputStream().use { frame.compress(Bitmap.CompressFormat.JPEG, 92, it) }; true }.getOrDefault(false)
        frame.recycle()
        if (!posterOk) return@withContext fail(MotionLimits.MSG_UNDECODABLE, "poster write")

        val luma = WallpaperLuminanceProbe.survey(poster.absolutePath)
        try {
            context.echoDataStore.edit {
                it[KEY_CUSTOM] = poster.absolutePath
                it[KEY_MOTION] = motion.absolutePath
                it.setWallpaperLuma(luma)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return@withContext Result(false, SAVE_FAILED)
        }
        still.prune(keep = listOf(motion, poster))
        Result(true, null)
    }

    private fun probe(file: File, mime: String): MotionLimits.Probe? = runCatching {
        if (mime == "image/gif" || mime == "image/webp") {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            MotionLimits.Probe(mime = mime, width = bounds.outWidth, height = bounds.outHeight, durationMs = 0L, bytes = file.length())
        } else {
            MediaMetadataRetriever().use { r ->
                r.setDataSource(file.absolutePath)
                MotionLimits.Probe(
                    mime = mime,
                    width = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0,
                    height = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0,
                    durationMs = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L,
                    bytes = file.length(),
                )
            }
        }
    }.getOrNull()

    private fun poster(file: File, mime: String): Bitmap? = runCatching {
        if (mime == "image/gif" || mime == "image/webp") {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(file)) { decoder, info, _ ->
                decoder.setTargetSampleSize(maxOf(1, maxOf(info.size.width, info.size.height) / MotionLimits.MAX_HEIGHT))
            }
        } else {
            MediaMetadataRetriever().use { r ->
                r.setDataSource(file.absolutePath)
                r.getFrameAtTime(1_000_000L) ?: r.getFrameAtTime(0L)
            }
        }
    }.getOrNull()

    companion object {
        const val SAVE_FAILED = "Couldn't save the wallpaper — try again"
        private val KEY_CUSTOM = stringPreferencesKey("display_custom_wallpaper")
        private val KEY_MOTION = stringPreferencesKey("display_motion_wallpaper")
    }
}
