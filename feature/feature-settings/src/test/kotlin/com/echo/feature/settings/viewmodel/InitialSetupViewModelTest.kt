package com.echo.feature.settings.viewmodel

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import com.echo.core.data.repository.MediaRootKind
import com.echo.core.data.repository.MediaRootRepository
import com.echo.core.data.repository.CoreInventory
import com.echo.core.data.repository.RetroArchLink
import com.echo.core.data.repository.RomRootRepository
import com.echo.core.data.repository.Vita3KLibrary
import com.echo.feature.artwork.MetadataApiKeyProvider
import com.echo.feature.artwork.api.ArtworkImportManager
import com.echo.feature.artwork.api.SgdbApiKeyProvider
import com.echo.feature.launcher.EmulatorAutoConfigService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InitialSetupViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val context = mockk<Context>(relaxed = true)
    private val packageManager = mockk<PackageManager>(relaxed = true)
    private val romRoots = mockk<RomRootRepository>(relaxed = true)
    private val mediaRoots = mockk<MediaRootRepository>(relaxed = true)
    private val artworkImport = mockk<ArtworkImportManager>(relaxed = true)
    private val retroArchLink = mockk<RetroArchLink>(relaxed = true)
    private val vita3KLibrary = mockk<Vita3KLibrary>(relaxed = true)
    private val autoConfig = mockk<EmulatorAutoConfigService>(relaxed = true)
    private val sgdbKeys = mockk<SgdbApiKeyProvider>(relaxed = true)
    private val metadataKeys = mockk<MetadataApiKeyProvider>(relaxed = true)
    private val scanRunner = mockk<com.echo.feature.settings.media.WizardMediaScanRunner>(relaxed = true)
    private val romRootScanRunner = mockk<RomRootScanRunner>(relaxed = true)
    private val launcherShortcuts =
        mockk<com.echo.feature.appbar.LauncherShortcutRepository>(relaxed = true)
    private lateinit var vm: InitialSetupViewModel

    private fun buildVm() = InitialSetupViewModel(
        context, romRoots, mediaRoots, artworkImport, RetroArchSetup(retroArchLink, autoConfig), vita3KLibrary,
        sgdbKeys, metadataKeys,
        scanRunner, romRootScanRunner,
        StandardRomFolders(mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true)),
        launcherShortcuts,
        mockk<com.echo.feature.artwork.api.TmdbApiKeyProvider>(relaxed = true) {
            every { keyFlow } returns flowOf(null)
        },
        ArtworkFolderSetup(artworkImport),
        mockk<StorageSuggestions>(relaxed = true) { every { suggest() } returns emptyMap() },
    )

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { context.packageManager } returns packageManager

        every { packageManager.getPackageInfo(any<String>(), any<Int>()) } throws
            PackageManager.NameNotFoundException()
        every { packageManager.getInstalledPackages(any<Int>()) } returns emptyList()
        every { romRoots.roots } returns flowOf(emptyList())
        every { mediaRoots.roots(any()) } returns flowOf(emptyList())
        every { artworkImport.folderTreeUri } returns flowOf(null)
        every { vita3KLibrary.ux0TreeUriFlow } returns flowOf(null)
        every { sgdbKeys.apiKeyFlow } returns flowOf(null)
        every { metadataKeys.igdbClientIdFlow } returns flowOf(null)
        every { metadataKeys.ssUsernameFlow } returns flowOf(null)
        coEvery { retroArchLink.inventory() } returns CoreInventory.Unlinked
        vm = buildVm()
    }

    @After fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.collectState() = launch { vm.uiState.collect {} }

    private fun packageInfoFor(name: String) =
        android.content.pm.PackageInfo().apply { packageName = name }

    @Test fun `RetroArch is detected under its ABI-suffixed package, not just the bare one`() =
        runTest(dispatcher) {
            every { packageManager.getInstalledPackages(any<Int>()) } returns
                listOf(packageInfoFor("com.retroarch.aarch64"))
            vm = buildVm()
            val job = collectState()
            advanceUntilIdle()
            assertTrue(vm.uiState.value.retroArchInstalled)
            job.cancel()
        }

    @Test fun `a package that merely starts with the family name is not RetroArch`() =
        runTest(dispatcher) {
            every { packageManager.getInstalledPackages(any<Int>()) } returns
                listOf(packageInfoFor("com.retroarchive.reader"))
            vm = buildVm()
            val job = collectState()
            advanceUntilIdle()
            assertFalse(vm.uiState.value.retroArchInstalled)
            job.cancel()
        }

    @Test fun `book roots reach the Books page and nothing else`() = runTest(dispatcher) {
        every { mediaRoots.roots(MediaRootKind.BOOK) } returns
            flowOf(listOf("content://tree/primary%3ABooks"))
        vm = buildVm()

        val job = collectState()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(1, state.bookRoots.size)
        assertTrue(state.musicRoots.isEmpty())
        assertTrue(state.videoRoots.isEmpty())
        assertTrue(state.photoRoots.isEmpty())
        assertTrue(state.romRoots.isEmpty())
        job.cancel()
    }

    @Test fun `the Home role is re-read on demand, not cached from construction`() = runTest(dispatcher) {
        every { launcherShortcuts.isDefaultLauncher() } returns false
        vm = buildVm()
        val job = collectState()
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isHomeLauncher)

        every { launcherShortcuts.isDefaultLauncher() } returns true
        vm.refreshGrants()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isHomeLauncher)
        job.cancel()
    }

    @Test fun `parking survives exactly one reset, so an excursion keeps your place`() =
        runTest(dispatcher) {
            val job = collectState()
            repeat(3) { vm.nextStep() }
            advanceUntilIdle()
            val where = vm.uiState.value.step

            vm.parkForExcursion()
            vm.resetWizard()
            advanceUntilIdle()
            assertEquals("the parked reset must be a no-op", where, vm.uiState.value.step)

            vm.resetWizard()
            advanceUntilIdle()
            assertEquals(SetupStep.PERMISSIONS, vm.uiState.value.step)
            job.cancel()
        }

    @Test fun `linkRetroArch saves the tree and reports installed cores`() = runTest(dispatcher) {
        coEvery { retroArchLink.inventory() } returns CoreInventory.Verified(
            setOf("snes9x_libretro_android.so", "mgba_libretro_android.so")
        )
        val uri = mockk<Uri>()
        val job = collectState()

        vm.linkRetroArch(uri)
        advanceUntilIdle()

        coVerify { retroArchLink.save(uri) }
        coVerify { autoConfig.runOnStartup() }
        assertTrue(vm.uiState.value.retroArchLinked)
        assertEquals(2, vm.uiState.value.retroArchCoreCount)
        job.cancel()
    }

    @Test fun `linkVitaFolder grants the ux0 folder and reports it`() = runTest(dispatcher) {
        val uri = mockk<Uri> {
            every { this@mockk.toString() } returns "content://tree/primary%3ARoms%2Fvita%2Fux0"
        }
        val job = collectState()

        vm.linkVitaFolder(uri)
        advanceUntilIdle()

        coVerify { vita3KLibrary.setUx0Folder(uri) }
        assertNotNull(vm.uiState.value.message)
        job.cancel()
    }

    @Test fun `setup is four steps at most, and Emulators only shows when there is an emulator to link`() {
        assertEquals(
            listOf(SetupStep.PERMISSIONS, SetupStep.STORAGE, SetupStep.ACCOUNTS),
            reachableSetupSteps(retroArchInstalled = false, vita3KInstalled = false),
        )
        assertEquals(SetupStep.entries, reachableSetupSteps(retroArchInstalled = true, vita3KInstalled = false))
        assertEquals(SetupStep.entries, reachableSetupSteps(retroArchInstalled = false, vita3KInstalled = true))
    }

    @Test fun `steps advance and retreat in order, and back on the first step exits`() = runTest(dispatcher) {
        val job = collectState()
        advanceUntilIdle()
        assertEquals(SetupStep.PERMISSIONS, vm.uiState.value.step)
        assertFalse("back on the first step means exit", vm.previousStep())

        vm.nextStep()
        vm.nextStep()
        advanceUntilIdle()
        assertEquals(SetupStep.ACCOUNTS, vm.uiState.value.step)

        assertTrue(vm.previousStep())
        advanceUntilIdle()
        assertEquals(SetupStep.STORAGE, vm.uiState.value.step)
        job.cancel()
    }

    @Test fun `a games folder pick keeps write access and starts the scan`() = runTest(dispatcher) {
        val uri = mockk<Uri> { every { this@mockk.toString() } returns "content://tree/6DBF%3AEmulation%2FROMs" }

        vm.onStoragePicked(StorageSlot.GAMES, replacing = null, uri = uri)
        advanceUntilIdle()

        coVerify { romRoots.persist(uri, writable = true) }
        coVerify { romRoots.add("content://tree/6DBF%3AEmulation%2FROMs") }
        io.mockk.verify { romRootScanRunner.kickoff() }
    }

    @Test fun `regranting a lost folder replaces it instead of adding a duplicate`() = runTest(dispatcher) {
        val old = "content://tree/6DBF%3AMusic"
        val uri = mockk<Uri> { every { this@mockk.toString() } returns old }

        vm.onStoragePicked(StorageSlot.MUSIC, replacing = old, uri = uri)
        advanceUntilIdle()

        coVerify { mediaRoots.persist(uri) }
        coVerify { mediaRoots.replace(MediaRootKind.MUSIC, old, old) }
        coVerify(exactly = 0) { mediaRoots.add(any(), any()) }
        io.mockk.verify { scanRunner.kickoff(MediaRootKind.MUSIC) }
    }

    @Test fun `an artwork folder that cannot hold a library says so`() = runTest(dispatcher) {
        val uri = mockk<Uri>()
        coEvery { artworkImport.linkFolder(uri) } returns null
        val job = collectState()

        vm.onStoragePicked(StorageSlot.ARTWORK, replacing = null, uri = uri)
        advanceUntilIdle()

        assertEquals(ArtworkFolderSetup.COULD_NOT_LINK, vm.uiState.value.message)
        job.cancel()
    }
}
