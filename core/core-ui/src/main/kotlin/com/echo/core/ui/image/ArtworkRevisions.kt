package com.echo.core.ui.image

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.size.Scale

object ArtworkRevisions {
    private val revisions = mutableStateMapOf<String, Int>()

    fun bump(uri: String) {
        revisions[uri] = (revisions[uri] ?: 0) + 1
    }

    fun of(uri: String): Int = revisions[uri] ?: 0

    fun cacheKey(uri: String): String? = of(uri).takeIf { it > 0 }?.let { "$uri#r$it" }
}

@Composable
fun rememberArtworkModel(uri: String?): Any? {
    if (uri == null) return null
    val key = ArtworkRevisions.cacheKey(uri) ?: return uri
    val context = LocalPlatformContext.current
    return remember(key, context) {
        ImageRequest.Builder(context)
            .data(uri)
            .memoryCacheKey(key)
            .build()
    }
}

fun blurSourceCacheKey(uri: String): String = "${ArtworkRevisions.cacheKey(uri) ?: uri}#blur$BLUR_SOURCE_PX"

@Composable
fun rememberBlurSourceModel(uri: String?): Any? {
    if (uri == null) return null
    val key = blurSourceCacheKey(uri)
    val context = LocalPlatformContext.current
    return remember(key, context) {
        ImageRequest.Builder(context)
            .data(uri)
            .size(BLUR_SOURCE_PX)
            .scale(Scale.FILL)
            .memoryCacheKey(key)
            .build()
    }
}

private const val BLUR_SOURCE_PX = 128
