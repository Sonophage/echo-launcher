package com.echo.feature.settings.media

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.echo.core.data.repository.MediaRootKind
import com.echo.core.data.repository.MediaRootRepository
import com.echo.core.domain.model.MusicFolder
import com.echo.core.domain.repository.MusicRepository
import com.echo.feature.library.scanner.MusicScanner
import com.echo.core.domain.repository.BookRepository
import com.echo.core.domain.model.BookLibrary
import com.echo.feature.library.scanner.BookScanner
import com.echo.feature.settings.viewmodel.eventually
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration.Companion.minutes

@RunWith(RobolectricTestRunner::class)
class WizardMediaScanRunnerTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val mediaRootRepository = mockk<MediaRootRepository>(relaxed = true)
    private val musicRepository = mockk<MusicRepository>(relaxed = true)
    private val musicScanner = mockk<MusicScanner>(relaxed = true)
    private val bookRepository = mockk<BookRepository>(relaxed = true)
    private val bookScanner = mockk<BookScanner>(relaxed = true)

    private val runner = WizardMediaScanRunner(
        context,
        mediaRootRepository,
        musicRepository,
        musicScanner,
        mockk(relaxed = true),
        mockk(relaxed = true),
        mockk(relaxed = true),
        mockk(relaxed = true),
        bookRepository,
        bookScanner,
    )

    @Test
    fun `a root added during a scan is still scanned`() = runTest(timeout = 2.minutes) {
        val rootB = "content://tree/primary%3AMusicB"
        val folderB = MusicFolder(id = "b", displayName = "MusicB", treeUri = rootB, createdAt = 0, updatedAt = 0)
        val roots = AtomicReference(emptyList<String>())
        val firstScanStarted = CompletableDeferred<Unit>()
        val releaseFirstScan = CompletableDeferred<Unit>()
        coEvery { mediaRootRepository.getAll(MediaRootKind.MUSIC) } coAnswers {
            val current = roots.get()
            if (firstScanStarted.complete(Unit)) releaseFirstScan.await()
            current
        }
        coEvery { musicRepository.getFolders() } returns listOf(folderB)
        coEvery { musicRepository.getFolder("b") } returns folderB
        every { musicRepository.observeTracksByFolder("b") } returns flowOf(emptyList())
        every { musicScanner.scan(any(), any(), any()) } returns emptyFlow()

        runner.kickoff(MediaRootKind.MUSIC)
        eventually("the first scan starts") { firstScanStarted.isCompleted }
        roots.set(listOf(rootB))
        runner.kickoff(MediaRootKind.MUSIC)
        releaseFirstScan.complete(Unit)

        eventually("root B is scanned after it was added mid-scan") {
            runCatching { coVerify { musicScanner.scan(folderB, any(), any()) } }.isSuccess
        }
    }

    // owner, 2026-10-05: changing the Books folder kept the old shelf and never scanned the new one
    @Test
    fun `a changed books folder drops the old shelf and scans the new one`() = runTest(timeout = 2.minutes) {
        val old = BookLibrary(id = "old", displayName = "Old", treeUri = "content://tree/primary%3AOld", createdAt = 0, updatedAt = 0)
        val new = BookLibrary(id = "new", displayName = "Books", treeUri = "content://tree/primary%3ABooks", createdAt = 0, updatedAt = 0)
        coEvery { mediaRootRepository.getAll(MediaRootKind.BOOK) } returns listOf(new.treeUri)
        coEvery { bookRepository.getLibraries() } returns listOf(old, new)
        coEvery { bookRepository.getLibrary("new") } returns new
        every { bookScanner.scan(any(), any(), any()) } returns emptyFlow()

        runner.kickoff(MediaRootKind.BOOK)

        eventually("the old shelf is dropped") { runCatching { coVerify { bookRepository.removeLibrary("old") } }.isSuccess }
        eventually("the new folder is scanned") { runCatching { coVerify { bookScanner.scan(new, any(), any()) } }.isSuccess }
    }
}
