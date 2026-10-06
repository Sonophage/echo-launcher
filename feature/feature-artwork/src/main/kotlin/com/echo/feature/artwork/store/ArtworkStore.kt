package com.echo.feature.artwork.store

import android.net.Uri

enum class ArtworkKind {
    ICON,
    HERO,
    BACKGROUND,
    LOGO,
    MANUAL,
    VIDEO,
    ICON1,

    SCREENSHOT,
    TITLESCREEN,

    PHYSICAL_MEDIA,
    BOX_ART,
    BOX_3D,
}

interface ArtworkStore {
    suspend fun saveFromUrl(gameId: Long, kind: ArtworkKind, url: String, sortOrder: Int = 0): String?

    suspend fun saveVersionedFromUrl(gameId: Long, kind: ArtworkKind, url: String): String?

    suspend fun saveVersionedFromUri(gameId: Long, kind: ArtworkKind, uri: Uri): String?

    suspend fun saveFromFile(gameId: Long, kind: ArtworkKind, tempFile: java.io.File, sortOrder: Int = 0): String?

    fun isValidRef(ref: String?): Boolean

    suspend fun find(gameId: Long, kind: ArtworkKind, sortOrder: Int = 0): String?

    suspend fun findAll(gameId: Long, kind: ArtworkKind): List<String>

    suspend fun deleteAll()

    // forgets what a scraper fetched (isRescrapable) and deletes its files, so a rescrape fetches it again; returns the
    // references it removed. Only portable records say where art came from, so a store without them removes nothing
    suspend fun deleteScraped(): Set<String> = emptySet()
}
