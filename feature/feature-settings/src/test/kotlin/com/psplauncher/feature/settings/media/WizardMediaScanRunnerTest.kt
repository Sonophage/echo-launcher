package com.psplauncher.feature.settings.media

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.psplauncher.core.data.repository.MediaRootKind
import com.psplauncher.core.data.repository.MediaRootRepository
import com.psplauncher.core.domain.model.MusicFolder
import com.psplauncher.core.domain.repository.MusicRepository
import com.psplauncher.feature.library.scanner.MusicScanner
import com.psplauncher.feature.settings.viewmodel.eventually
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

    private val runner = WizardMediaScanRunner(
        context,
        mediaRootRepository,
        musicRepository,
        musicScanner,
        mockk(relaxed = true),
        mockk(relaxed = true),
        mockk(relaxed = true),
        mockk(relaxed = true),
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
}
