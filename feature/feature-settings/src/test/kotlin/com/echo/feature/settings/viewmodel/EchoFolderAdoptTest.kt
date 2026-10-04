package com.echo.feature.settings.viewmodel

import android.net.Uri
import com.echo.feature.artwork.api.ArtworkImportManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// owner, 2026-10-04: the artwork folder becomes ECHO's own folder, named ECHO
class EchoFolderAdoptTest {
    private val base = "content://com.android.externalstorage.documents/tree/"
    private fun uri(s: String) = mockk<Uri> { every { this@mockk.toString() } returns s }

    private val start = uri("start")
    private val manager = mockk<ArtworkImportManager>(relaxed = true) {
        coEvery { linkFolder(any()) } returns ArtworkImportManager.LinkResult(mockk(relaxed = true), existingLibrary = true)
        coEvery { renameFolderToEcho() } returns uri(base + "6DBF-B253%3AEmulation%2FECHO")
        coEvery { echoFolderInside(any()) } returns uri(base + "primary%3AGames%2FECHO")
        every { pickerStart(any()) } returns start
    }
    private val setup = ArtworkFolderSetup(manager)

    @Test
    fun `a folder already named ECHO is linked as it is`() = runTest {
        val picked = uri(base + "primary%3AEcho")
        assertTrue(setup.adopt(picked) is EchoAdopt.Linked)
        coVerify(exactly = 0) { manager.renameFolderToEcho() }
    }

    @Test
    fun `a folder holding the library is renamed, and the picker opens on it`() = runTest {
        val picked = uri(base + "6DBF-B253%3AEmulation%2FPSPL")
        coEvery { manager.holdsLibrary(picked) } returns true
        val step = setup.adopt(picked)
        assertEquals(EchoAdopt.PickEcho(start, ArtworkFolderSetup.RENAMED), step)
        coVerify { manager.linkFolder(picked) }
    }

    @Test
    fun `a general folder is never renamed, ECHO is made inside it`() = runTest {
        val picked = uri(base + "primary%3AGames")
        coEvery { manager.holdsLibrary(picked) } returns false
        assertEquals(EchoAdopt.PickEcho(start, ArtworkFolderSetup.CREATED), setup.adopt(picked))
        coVerify(exactly = 0) { manager.renameFolderToEcho() }
        coVerify(exactly = 0) { manager.linkFolder(picked) }
    }

    @Test
    fun `when the rename fails the library stays linked under its old name, and says so`() = runTest {
        val picked = uri(base + "6DBF-B253%3AEmulation%2FPSPL")
        coEvery { manager.holdsLibrary(picked) } returns true
        coEvery { manager.renameFolderToEcho() } returns null
        val step = setup.adopt(picked)
        assertTrue(step is EchoAdopt.Linked)
        assertEquals(ArtworkFolderSetup.RENAME_FAILED, (step as EchoAdopt.Linked).note)
    }
}
