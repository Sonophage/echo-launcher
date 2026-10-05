package com.echo.feature.settings.viewmodel

import android.content.Context
import android.net.Uri
import com.echo.core.data.repository.MediaRootRepository
import com.echo.core.data.repository.RomRootRepository
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// owner, 2026-10-05: the tablet's games folder had lost its grant and nothing said so; Permissions and Setup list it now
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FolderAccessTest {
    private val roms = "content://com.android.externalstorage.documents/tree/primary%3ARoms"
    private val music = "content://com.android.externalstorage.documents/tree/primary%3AMusic"
    private val echo = "content://com.android.externalstorage.documents/tree/6DBF-B253%3AEmulation%2FECHO"

    @Test
    fun `every folder is listed with whether Android still lets ECHO open it`() {
        val rows = folderAccessRows(
            mapOf(StorageSlot.ARTWORK to listOf(echo), StorageSlot.GAMES to listOf(roms), StorageSlot.MUSIC to listOf(music)),
            persisted = setOf(music, echo),
        )
        assertEquals(listOf(StorageSlot.GAMES, StorageSlot.MUSIC, StorageSlot.ARTWORK), rows.map { it.slot })
        assertEquals(listOf(false, true, true), rows.map { it.granted })
    }

    private val romRoots = mockk<RomRootRepository>(relaxed = true)
    private val mediaRoots = mockk<MediaRootRepository>(relaxed = true)
    private val access = FolderAccess(mockk<Context>(relaxed = true), romRoots, mediaRoots, mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true))

    @Test
    fun `granting a games folder again persists the pick in its place`() = runTest {
        val picked = Uri.parse(roms)
        assertNull(access.regrant(FolderAccessRow(StorageSlot.GAMES, roms, "Roms", granted = false), picked))
        verify { romRoots.persist(picked, writable = true) }
        coVerify { romRoots.replace(roms, roms) }
    }

    @Test
    fun `the ECHO folder is only granted again by picking that same folder`() = runTest {
        val other = Uri.parse("content://com.android.externalstorage.documents/tree/primary%3ADownload%2FECHO")
        assertEquals(PICK_THE_ECHO_FOLDER, access.regrant(FolderAccessRow(StorageSlot.ARTWORK, echo, "ECHO", granted = false), other))
    }

    // a tree uri alone opened the picker on the storage root ("Can't use this folder")
    @Test
    fun `the picker opens on the stored folder's own document`() {
        assertEquals(
            "content://com.android.externalstorage.documents/tree/primary%3ARoms/document/primary%3ARoms",
            pickerStartUri(roms).toString(),
        )
        val suggested = "content://com.android.externalstorage.documents/document/primary%3AMusic"
        assertEquals("a document uri is used as it is", suggested, pickerStartUri(suggested).toString())
    }
}
