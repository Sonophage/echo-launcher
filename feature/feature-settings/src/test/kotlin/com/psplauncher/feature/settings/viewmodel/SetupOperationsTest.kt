package com.psplauncher.feature.settings.viewmodel

import android.net.Uri
import com.psplauncher.core.data.platform.PlatformFolderHintResolver
import com.psplauncher.core.data.repository.CoreInventory
import com.psplauncher.core.data.repository.MemoryCardRepository
import com.psplauncher.core.data.repository.RetroArchLink
import com.psplauncher.core.domain.model.Platform
import com.psplauncher.feature.launcher.EmulatorAutoConfigService
import com.psplauncher.feature.library.scanner.RomScanner
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SetupOperationsTest {
    private fun platform(id: String) = Platform(id = id, name = id, shortName = id, iconRes = null, accentColor = 0L)

    @Test
    fun `the standard folders are every console's ES-DE name once, never android and never blank`() = runTest {
        val cards = mockk<MemoryCardRepository>()
        coEvery { cards.availablePlatformCatalog() } returns
            listOf(platform("psx"), platform("android"), platform("ps1alias"), platform("mystery"), platform("snes"))
        val hints = mockk<PlatformFolderHintResolver> {
            every { esDeFolderName("psx") } returns "psx"
            every { esDeFolderName("android") } returns "android"
            every { esDeFolderName("ps1alias") } returns "psx"
            every { esDeFolderName("mystery") } returns ""
            every { esDeFolderName("snes") } returns "snes"
        }
        val folders = StandardRomFolders(cards, hints, mockk<RomScanner>(relaxed = true))

        assertEquals(listOf("psx", "snes"), folders.names())
    }

    @Test
    fun `linking RetroArch saves the folder before it re-detects cores`() = runTest {
        val link = mockk<RetroArchLink>(relaxed = true)
        val autoConfig = mockk<EmulatorAutoConfigService>(relaxed = true)
        coEvery { link.inventory() } returns CoreInventory.Unlinked
        val uri = mockk<Uri>()

        RetroArchSetup(link, autoConfig).link(uri)

        coVerifyOrder {
            link.save(uri)
            autoConfig.runOnStartup()
            link.inventory()
        }
    }
}
