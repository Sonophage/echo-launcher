package com.echo.core.ui.media

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

// owner, 2026-10-09: Delete on a media item removes the file from the device, then ECHO's row; a file ECHO could
// not delete keeps its row, so the library never lists less than the device holds without saying so
class MediaDeleteTest {
    @Test
    fun `the row goes only once the file is gone`() = runBlocking {
        val removed = mutableListOf<String>()
        assertEquals(true, deleteFromDevice("content://a", { true }) { removed += "a" })
        assertEquals(false, deleteFromDevice("content://b", { false }) { removed += "b" })
        assertEquals(false, deleteFromDevice(null, { true }) { removed += "c" })
        assertEquals(listOf("a"), removed)
    }
}
