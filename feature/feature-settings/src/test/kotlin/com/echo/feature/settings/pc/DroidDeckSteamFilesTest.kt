package com.echo.feature.settings.pc

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.echo.core.data.database.dao.ArtworkRecordDao
import com.echo.core.data.repository.WindowsLibrarySetup
import com.echo.core.data.repository.WindowsSetupState
import com.echo.core.domain.model.Game
import com.echo.core.domain.repository.GameRepository
import com.echo.feature.library.scanner.PcExportFile
import com.echo.feature.library.scanner.RomScanner
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// seen on the Konker, 2026-10-10: DroidDeck 0.3.2 writes a .steam file (the Steam app id) per game straight into the
// windows folder, which ECHO did not read, so none of its 43 games showed. GameNative had been uninstalled
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class DroidDeckSteamFilesTest {
    private val context = mockk<Context>(relaxed = true)
    private val pm = mockk<PackageManager>(relaxed = true)
    private val windows = mockk<WindowsLibrarySetup>(relaxed = true)
    private val romScanner = mockk<RomScanner>(relaxed = true)
    private val games = mockk<GameRepository>(relaxed = true)

    private val scanner = PcGameScanner(
        context = context,
        windowsLibrarySetup = windows,
        pcShortcutImporter = mockk(relaxed = true),
        romScanner = romScanner,
        gameRepository = games,
        artworkImportManager = mockk(relaxed = true),
        artworkRecordDao = mockk<ArtworkRecordDao>(relaxed = true),
    )

    @Before
    fun setUp() {
        every { context.packageManager } returns pm
        // only DroidDeck is installed
        every { pm.getApplicationInfo(any<String>(), any<Int>()) } throws PackageManager.NameNotFoundException()
        every { pm.getApplicationInfo("com.droiddeck.launcher", any<Int>()) } returns ApplicationInfo()
        every { pm.getInstalledPackages(any<Int>()) } returns emptyList()
        coEvery { windows.ensure() } returns WindowsSetupState.Ready(null)
        coEvery { windows.importFolders() } returns listOf("tree" to "import")
        coEvery { windows.windowsFolders() } returns listOf("tree" to "windows")
        coEvery { romScanner.scanPcFolder("tree", "import") } returns emptyList()
        coEvery { games.getByIntentUri(any()) } returns null
        coEvery { romScanner.scanPcFolder("tree", "windows") } returns listOf(
            PcExportFile(title = "Balatro", extension = "steam", idContent = "2379780", rawPath = null, uri = "content://w/Balatro.steam"),
        )
    }

    @Test
    fun `a steam file DroidDeck wrote into the windows folder becomes a DroidDeck game`() = runTest {
        coEvery { games.getByPlatform("windows") } returns emptyList()

        scanner.scan()

        coVerify(exactly = 1) {
            games.upsert(match { it.packageName == "com.droiddeck.launcher" && it.storefrontGameId == "2379780" &&
                it.launchIntentUri!!.contains("droiddeck") })
        }
    }

    @Test
    fun `a game whose launcher is uninstalled moves to DroidDeck with its history, not added twice`() = runTest {
        val old = Game(id = 149L, title = "Balatro", platformId = "windows", packageName = "app.gamenative",
            storefront = "STEAM", storefrontGameId = "2379780", launchIntentUri = "intent:#Intent;action=app.gamenative.LAUNCH_GAME;end")
        coEvery { games.getByPlatform("windows") } returns listOf(old)

        scanner.scan()

        coVerify(exactly = 0) { games.upsert(any()) }
        coVerify(exactly = 1) { games.attachLauncherHandle(149L, "com.droiddeck.launcher", null, match { it.contains("droiddeck") }) }
    }

    @Test
    fun `a DroidDeck copy that exists already is kept, the uninstalled launcher's copy is left alone`() = runTest {
        val old = Game(id = 149L, title = "Balatro", platformId = "windows", packageName = "app.gamenative",
            storefront = "STEAM", storefrontGameId = "2379780")
        val deck = Game(id = 166L, title = "Balatro", platformId = "windows", packageName = "com.droiddeck.launcher",
            storefront = "STEAM", storefrontGameId = "2379780")
        coEvery { games.getByPlatform("windows") } returns listOf(old, deck)

        scanner.scan()

        coVerify(exactly = 0) { games.upsert(any()) }
        coVerify(exactly = 0) { games.attachLauncherHandle(149L, any(), any(), any()) }
    }

    // owner, 2026-10-10: a PC game whose launcher is uninstalled is hidden by the scan, and shown again if it returns
    @Test
    fun `a game whose launcher is not installed is hidden, and one whose launcher is back is shown`() = runTest {
        val orphan = Game(id = 145L, title = "Hades", platformId = "windows", packageName = "app.gamenative",
            storefront = "STEAM", storefrontGameId = "1145360")
        val back = Game(id = 160L, title = "Nine Sols", platformId = "windows", packageName = "com.droiddeck.launcher",
            storefront = "STEAM", storefrontGameId = "1809540", isMissing = true)
        coEvery { games.getByPlatform("windows") } returns listOf(orphan, back)

        scanner.scan()

        coVerify(exactly = 1) { games.markMissingIds(listOf(145L)) }
        coVerify(exactly = 1) { games.markSeenIds(listOf(160L)) }
    }
}
