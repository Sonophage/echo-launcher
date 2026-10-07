package com.echo.feature.artwork.portable

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// a folder linked while ECHO runs, from setup or Settings, may hold settings.json from an earlier
// install. ECHO's fresh settings must not be written over it before it is read.
class EchoFolderMirrorTest {
    private val reader = mockk<EchoFolderReader>(relaxed = true)
    private val mirror = EchoFolderMirror(mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true), reader, mockk(relaxed = true), mockk(relaxed = true))

    @Test
    fun `a newly linked folder is read in full once, before anything is written to it`() = runTest {
        coEvery { reader.read(any()) } returns mockk(relaxed = true)

        assertTrue(mirror.readIfNewlyLinked("content://tree/primary%3AECHO"))
        coVerify(exactly = 1) { reader.read(always = true) }

        assertFalse(mirror.readIfNewlyLinked("content://tree/primary%3AECHO"), "the same folder is not read again")
        assertFalse(mirror.readIfNewlyLinked(null), "no folder, nothing to read")
        coVerify(exactly = 1) { reader.read(always = true) }
    }
}
