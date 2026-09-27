package com.psplauncher.feature.artwork.migrate

import android.content.Context
import android.net.Uri
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.psplauncher.core.data.database.dao.ArtworkRecordDao
import com.psplauncher.core.data.database.dao.GameDao
import com.psplauncher.core.data.database.entity.ArtworkRecordEntity
import com.psplauncher.core.data.repository.ArtworkFolderRepository
import com.psplauncher.core.data.repository.ArtworkStorageMode
import com.psplauncher.core.ui.notification.BackgroundTaskNotifier
import com.psplauncher.feature.artwork.store.ArtworkKind
import com.psplauncher.feature.artwork.store.ArtworkReferences
import com.psplauncher.feature.artwork.store.ArtworkTempIO
import com.psplauncher.feature.artwork.store.InternalArtworkStore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import timber.log.Timber
import java.util.UUID

@HiltWorker
class PortableArtworkImportWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val internal: InternalArtworkStore,
    private val folderRepository: ArtworkFolderRepository,
    private val gameDao: GameDao,
    private val artworkRecordDao: ArtworkRecordDao,
    private val references: ArtworkReferences,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val notifier = BackgroundTaskNotifier(applicationContext)
        if (folderRepository.getTreeUri() == null || !folderRepository.hasLiveGrant()) {
            return Result.failure(workDataOf(KEY_ERROR to "No artwork folder linked"))
        }

        val all = artworkRecordDao.getAll()
        val games = gameDao.getAll().associateBy { it.id }
        val namedByAColumn = columnRefsOf(games.values)
        val (wanted, dead) = all.partition { shouldCopy(it.artworkType, it.documentUri, namedByAColumn) }

        var copied = 0
        var skipped = 0
        var failed = 0
        var bytes = 0L
        var cancelled = false
        notifier.running(TASK_ID, LABEL, null)

        try {
            wanted.forEachIndexed { index, record ->
                if (isStopped) throw CancellationException()
                val kind = kindOf(record.artworkType)
                if (kind == null || games[record.gameId] == null) {
                    skipped++
                    return@forEachIndexed
                }

                val tmp = runCatching {
                    applicationContext.contentResolver.openInputStream(Uri.parse(record.documentUri))?.use {
                        ArtworkTempIO.copyToTemp(it, applicationContext.cacheDir, kind)
                    }
                }.getOrNull()

                val path = tmp?.let { internal.saveFromFile(record.gameId, kind, it, record.sortOrder) }
                if (path == null) {
                    failed++
                } else {
                    repointColumns(record.gameId, record.documentUri, path)
                    artworkRecordDao.upsert(
                        record.copy(
                            documentUri = path,
                            relativePath = path,
                            prevDocumentUri = record.documentUri,
                            prevRelativePath = record.relativePath,
                            prevSizeBytes = record.sizeBytes,
                            updatedAt = System.currentTimeMillis(),
                        )
                    )
                    copied++
                    bytes += record.sizeBytes
                }

                if ((index + 1) % PROGRESS_STRIDE == 0 || index == wanted.lastIndex) {
                    notifier.running(TASK_ID, LABEL, (index + 1).toFloat() / wanted.size)
                    setProgressAsync(
                        workDataOf(KEY_PROGRESS_DONE to index + 1, KEY_PROGRESS_TOTAL to wanted.size)
                    )
                }
            }
        } catch (e: CancellationException) {
            cancelled = true
        } catch (e: Exception) {
            Timber.e(e, "Portable artwork import failed")
            notifier.failed(TASK_ID, "Artwork import failed", e.message ?: "Unexpected error")
            return Result.failure(workDataOf(KEY_ERROR to (e.message ?: "Unexpected error")))
        }

        if (cancelled) {
            notifier.complete(TASK_ID, "Artwork import cancelled", "$copied files copied before cancelling")
            throw CancellationException("Import cancelled")
        }

        if (failed == 0) {
            dead.forEach { artworkRecordDao.deleteById(it.id) }
            folderRepository.forget()
            folderRepository.setStorageMode(ArtworkStorageMode.INTERNAL)
        }

        val reaped = internal.reapUnreferenced(
            referenced = references.all(),
            liveGameIds = games.keys,
            keptKinds = KEPT_KINDS,
        )
        Timber.i("Portable import: $copied copied, $failed failed, ${reaped.deleted} stale files reaped")

        notifier.complete(
            TASK_ID,
            if (failed == 0) "Artwork copied into the app" else "Artwork import finished with errors",
            if (failed == 0) "$copied copied — your folder is no longer needed"
            else "$copied copied, $failed failed — your folder is still linked",
        )
        return Result.success(
            workDataOf(KEY_COPIED to copied, KEY_FAILED to failed, KEY_SKIPPED to skipped)
        )
    }

    private suspend fun repointColumns(gameId: Long, oldUri: String, path: String) {
        val game = gameDao.getById(gameId) ?: return
        if (game.iconUri == oldUri) gameDao.updateIconUri(gameId, path)
        if (game.artworkUri == oldUri) gameDao.updateArtwork(gameId, path)
        if (game.logoUri == oldUri) gameDao.updateLogo(gameId, path)
    }

    companion object {
        fun kindOf(name: String): ArtworkKind? = runCatching { ArtworkKind.valueOf(name) }.getOrNull()

        fun columnRefsOf(games: Collection<com.psplauncher.core.data.database.entity.GameEntity>): Set<String> =
            games.flatMap { listOfNotNull(it.artworkUri, it.iconUri, it.logoUri) }
                .filter { it.isNotBlank() }
                .toHashSet()

        fun shouldCopy(artworkType: String, documentUri: String, namedByAColumn: Set<String>): Boolean =
            kindOf(artworkType) in KEPT_KINDS || documentUri in namedByAColumn

        val KEPT_KINDS: Set<ArtworkKind> = setOf(
            ArtworkKind.ICON,
            ArtworkKind.ICON1,
            ArtworkKind.BACKGROUND,
            ArtworkKind.LOGO,
            ArtworkKind.SCREENSHOT,
            ArtworkKind.MANUAL,
            ArtworkKind.VIDEO,
        )

        const val UNIQUE_NAME = "pfp_portable_artwork_import"
        const val TASK_ID = "portable_artwork_import"
        private const val LABEL = "Copying artwork into the app"
        const val KEY_ERROR = "error"
        const val KEY_COPIED = "copied"
        const val KEY_FAILED = "failed"
        const val KEY_SKIPPED = "skipped"
        const val KEY_PROGRESS_DONE = "done"
        const val KEY_PROGRESS_TOTAL = "total"
        private const val PROGRESS_STRIDE = 10

        fun enqueue(context: Context): UUID {
            val request = OneTimeWorkRequestBuilder<PortableArtworkImportWorker>().build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(UNIQUE_NAME, ExistingWorkPolicy.KEEP, request)
            return request.id
        }

        fun cancel(context: Context) =
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_NAME)
    }
}
