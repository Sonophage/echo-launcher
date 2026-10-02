package com.psplauncher.feature.settings.pc

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.psplauncher.core.data.database.dao.ArtworkRecordDao
import com.psplauncher.core.data.repository.WindowsLibrarySetup
import com.psplauncher.core.data.repository.WindowsSetupState
import com.psplauncher.core.domain.model.Game
import com.psplauncher.core.domain.repository.GameRepository
import com.psplauncher.feature.artwork.api.ArtworkImportManager
import com.psplauncher.feature.launcher.PcShortcutImporter
import com.psplauncher.feature.library.scanner.PcExportFile
import com.psplauncher.feature.library.scanner.RomScanner
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class PcGameScannerTest {
    private val context = mockk<Context>(relaxed = true)
    private val packageManager = mockk<PackageManager>(relaxed = true)
    private val windowsLibrarySetup = mockk<WindowsLibrarySetup>(relaxed = true)
    private val pcShortcutImporter = mockk<PcShortcutImporter>(relaxed = true)
    private val romScanner = mockk<RomScanner>(relaxed = true)
    private val gameRepository = mockk<GameRepository>(relaxed = true)
    private val artworkImportManager = mockk<ArtworkImportManager>(relaxed = true)
    private val artworkRecordDao = mockk<ArtworkRecordDao>(relaxed = true)

    private val scanner = PcGameScanner(
        context = context,
        windowsLibrarySetup = windowsLibrarySetup,
        pcShortcutImporter = pcShortcutImporter,
        romScanner = romScanner,
        gameRepository = gameRepository,
        artworkImportManager = artworkImportManager,
        artworkRecordDao = artworkRecordDao,
    )

    @Before
    fun setUp() {
        every { context.packageManager } returns packageManager
        coEvery { windowsLibrarySetup.ensure() } returns WindowsSetupState.Ready(null)
        coEvery { windowsLibrarySetup.importFolders() } returns listOf("tree" to "importDocId")
        coEvery { pcShortcutImporter.reconcilePinnedShortcuts() } returns 0
    }

    private fun pinExportFile(export: PcGameExport) = PcExportFile(
        title = export.title,
        extension = PcGameExportCodec.EXTENSION,
        idContent = PcGameExportCodec.encode(export),
        rawPath = null,
        uri = "content://x/${export.title}.pfpgame",
    )

    @Test
    fun `a fill writes only the columns the export actually changed, never an upsert`() = runTest {
        val existing = Game(
            id = 7L,
            title = "Portal 2",
            platformId = "windows",
            packageName = "banner.hub",
            shortcutId = "game_620",
            ssId = 111L,
        )
        val export = PcGameExport(
            title = "Portal 2",
            launcherPackage = "banner.hub",
            shortcutId = "game_620",
            ssId = 425726L,
            igdbId = 66L,
            userTitleOverride = "Portal 2 (Co-op)",
            storefront = "STEAM",
            storefrontGameId = "620",
        )

        coEvery { romScanner.scanPcFolder("tree", "importDocId") } returns listOf(pinExportFile(export))
        coEvery { gameRepository.getByPlatform("windows") } returns listOf(existing)

        scanner.scan()

        coVerify(exactly = 0) { gameRepository.upsert(any()) }
        coVerify(exactly = 1) { gameRepository.updateUserTitleOverride(7L, "Portal 2 (Co-op)") }
        coVerify(exactly = 1) { gameRepository.updateStorefrontIdentity(7L, "STEAM", "620") }
        coVerify(exactly = 1) { gameRepository.updateProviderMatch(7L, "IGDB", 66L) }

        coVerify(exactly = 0) { gameRepository.updateScrapedTitle(any(), any()) }
        coVerify(exactly = 0) { gameRepository.updateProviderMatch(7L, "SCREENSCRAPER", any()) }
    }

    @Test
    fun `a fill that changes nothing writes no column at all`() = runTest {
        val existing = Game(
            id = 3L,
            title = "Half-Life",
            platformId = "windows",
            packageName = "banner.hub",
            shortcutId = "game_10",
            ssId = 999L,
            userTitleOverride = "HL",
        )
        val export = PcGameExport(
            title = "Half-Life",
            launcherPackage = "banner.hub",
            shortcutId = "game_10",
            ssId = 999L,
            userTitleOverride = "HL",
        )

        coEvery { romScanner.scanPcFolder("tree", "importDocId") } returns listOf(pinExportFile(export))
        coEvery { gameRepository.getByPlatform("windows") } returns listOf(existing)

        scanner.scan()

        coVerify(exactly = 0) { gameRepository.upsert(any()) }
        coVerify(exactly = 0) { gameRepository.updateScrapedTitle(any(), any()) }
        coVerify(exactly = 0) { gameRepository.updateUserTitleOverride(any(), any()) }
        coVerify(exactly = 0) { gameRepository.updateStorefrontIdentity(any(), any(), any()) }
        coVerify(exactly = 0) { gameRepository.updateProviderMatch(any(), any(), any()) }
    }

    private fun winlatorShortcut(title: String, intentUri: String): PcExportFile {
        val intent = mockk<Intent>(relaxed = true)
        every { intent.toUri(Intent.URI_INTENT_SCHEME) } returns intentUri
        every { packageManager.getLaunchIntentForPackage("com.winlator") } returns intent
        return PcExportFile(
            title = title,
            extension = "desktop",
            idContent = null,
            rawPath = "/storage/emulated/0/winlator/$title.desktop",
            uri = "content://x/$title.desktop",
        )
    }

    @Test
    fun `re-scanning games already in the library imports nothing`() = runTest {
        val shortcut = winlatorShortcut("Portal", "intent:winlator#Portal")
        val existing = Game(id = 5L, title = "Portal", platformId = "windows", packageName = "com.winlator")
        coEvery { romScanner.scanPcFolder("tree", "importDocId") } returns listOf(shortcut)
        coEvery { gameRepository.getByIntentUri("intent:winlator#Portal") } returns existing

        val report = scanner.scan()

        coVerify(exactly = 0) { gameRepository.upsert(any()) }
        assertEquals(0, report.exportsAdded)
        assertEquals(0, report.newGames)
        assertEquals("Imported 0 PC game(s).", report.message)
    }
}
