package com.psplauncher.feature.settings.viewmodel

import com.psplauncher.core.data.repository.MemoryCardRepository
import com.psplauncher.core.data.repository.RomRootRepository
import com.psplauncher.feature.library.scanner.LibraryScanner
import com.psplauncher.feature.library.scanner.RomRootDiscoveryScanner
import com.psplauncher.feature.settings.pc.PcGameScanner
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.Collections
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration.Companion.minutes

class RomRootScanRunnerTest {
    private val romRootRepository = mockk<RomRootRepository>(relaxed = true)
    private val memoryCardRepository = mockk<MemoryCardRepository>(relaxed = true)
    private val discoveryScanner = mockk<RomRootDiscoveryScanner>(relaxed = true)

    private val runner = RomRootScanRunner(
        romRootRepository,
        memoryCardRepository,
        discoveryScanner,
        mockk<LibraryScanner>(relaxed = true),
        mockk<PcGameScanner>(relaxed = true),
    )

    @Test
    fun `a root added during a scan is still scanned`() = runTest(timeout = 2.minutes) {
        val rootA = "content://tree/primary%3ARomsA"
        val rootB = "content://tree/primary%3ARomsB"
        val roots = AtomicReference(listOf(rootA))
        val firstScanStarted = CompletableDeferred<Unit>()
        val releaseFirstScan = CompletableDeferred<Unit>()
        val scanned = Collections.synchronizedList(mutableListOf<List<String>>())
        coEvery { romRootRepository.getAll() } answers { roots.get() }
        coEvery { memoryCardRepository.availablePlatformCatalog() } returns emptyList()
        coEvery { memoryCardRepository.getAll() } returns emptyList()
        coEvery { discoveryScanner.discover(any<List<String>>()) } coAnswers {
            val requested = firstArg<List<String>>()
            scanned += requested
            if (firstScanStarted.complete(Unit)) releaseFirstScan.await()
            RomRootDiscoveryScanner.Report(0, emptySet(), 0, 0, 0)
        }

        runner.kickoff()
        eventually("the first scan starts") { firstScanStarted.isCompleted }
        roots.set(listOf(rootA, rootB))
        runner.kickoff()
        releaseFirstScan.complete(Unit)

        eventually("root B is scanned after it was added mid-scan") {
            scanned.any { rootB in it }
        }
    }
}
