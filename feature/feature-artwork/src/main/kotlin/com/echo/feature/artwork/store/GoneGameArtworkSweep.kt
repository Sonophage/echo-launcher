package com.echo.feature.artwork.store

import com.echo.core.data.database.dao.GameDao
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

// deletes the app's own copies of art for games no longer in the library (owner, 2026-10-10: removing a game
// removes its art). Art in the portable ECHO folder is in the owner's own folder and is kept
@Singleton
class GoneGameArtworkSweep @Inject constructor(
    private val gameDao: GameDao,
    private val references: ArtworkReferences,
    private val internal: InternalArtworkStore,
) {
    suspend fun run(): Int {
        val live = gameDao.getAll().mapTo(HashSet()) { it.id }
        // a game added after this read has a higher id, and its art may already be saved; spare it
        val report = internal.reapUnreferenced(
            referenced = references.all(),
            liveGameIds = live,
            keptKinds = ArtworkKind.entries.toSet(),
            spareAbove = live.maxOrNull() ?: 0L,
        )
        if (report.deleted > 0) Timber.i("Artwork of removed games: ${report.deleted} files, ${report.bytes} bytes")
        return report.deleted
    }
}
