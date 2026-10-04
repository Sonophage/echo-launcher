package com.echo.core.data.repository

import androidx.room.withTransaction
import com.echo.core.data.database.EchoDatabase
import com.echo.core.data.database.dao.ArtworkRecordDao
import com.echo.core.data.database.dao.GameDao
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

// after the artwork folder is renamed, every stored link to a file in it is pointed at the same
// file under the new name, in one transaction. Nothing is rescanned or rematched.
@Singleton
class ArtworkLinkRepoint @Inject constructor(
    private val db: EchoDatabase,
    private val gameDao: GameDao,
    private val artworkRecordDao: ArtworkRecordDao,
) {
    suspend fun run(oldTree: String, newTree: String): Int = withContext(Dispatchers.IO) {
        var changed = 0
        db.withTransaction {
            artworkRecordDao.getAll().forEach { record ->
                val moved = EchoFolder.repoint(record.documentUri, oldTree, newTree)
                if (moved != null && moved != record.documentUri) {
                    artworkRecordDao.setDocumentUri(record.id, moved)
                    changed++
                }
            }
            gameDao.getAll().forEach { game ->
                val moved = game.copy(
                    artworkUri = EchoFolder.repoint(game.artworkUri, oldTree, newTree),
                    logoUri = EchoFolder.repoint(game.logoUri, oldTree, newTree),
                    iconUri = EchoFolder.repoint(game.iconUri, oldTree, newTree),
                )
                if (moved != game) {
                    gameDao.update(moved)
                    changed++
                }
            }
        }
        Timber.i("ArtworkLinkRepoint: $changed rows moved from $oldTree to $newTree")
        changed
    }
}
