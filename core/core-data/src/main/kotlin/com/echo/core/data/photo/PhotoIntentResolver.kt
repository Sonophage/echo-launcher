package com.echo.core.data.photo

import android.content.Context
import com.echo.core.data.media.MediaApp
import com.echo.core.data.media.MediaOpenIntent
import com.echo.core.domain.model.Photo
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhotoIntentResolver @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun launch(photo: Photo, viewerPackage: String?): String? {
        val intent = MediaOpenIntent.build(photo.uri, photo.mimeType ?: IMAGE_MIME, viewerPackage)
        return if (viewerPackage == null) {
            MediaOpenIntent.launchChooser(context, intent, CHOOSER_TITLE, NO_VIEWER, label(photo))
        } else {
            MediaOpenIntent.launch(context, intent, CHOOSER_TITLE, NO_VIEWER, label(photo))
        }
    }

    fun availableViewers(): List<MediaApp> = MediaOpenIntent.handlers(context, IMAGE_MIME)

    private fun label(photo: Photo) = "photo \"${photo.displayName}\""

    private companion object {
        const val IMAGE_MIME = "image/*"
        const val CHOOSER_TITLE = "Open photo with…"
        const val NO_VIEWER =
            "No app could open this photo. Install one or pick one from Photo → Folders → Default Photo Viewer."
    }
}
