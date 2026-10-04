package com.echo.core.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

// the owner's library: artwork under Emulation/PSPL on the SD card, beside PSPLauncher-Backups
class EchoFolderTest {
    private val base = "content://com.android.externalstorage.documents"
    private val oldTree = "$base/tree/6DBF-B253%3AEmulation%2FPSPL"
    private val newTree = "$base/tree/6DBF-B253%3AEmulation%2FECHO"

    @Test
    fun `a file in the folder points at the same file in the renamed folder`() = assertEquals(
        "$newTree/document/6DBF-B253%3AEmulation%2FECHO%2FArtwork%2Fpsp%2Fbackground%2FCrisis%20Core.jpg",
        EchoFolder.repoint("$oldTree/document/6DBF-B253%3AEmulation%2FPSPL%2FArtwork%2Fpsp%2Fbackground%2FCrisis%20Core.jpg", oldTree, newTree),
    )

    @Test
    fun `the folder itself is repointed`() = assertEquals(newTree, EchoFolder.repoint(oldTree, oldTree, newTree))

    @Test
    fun `a sibling that starts with the same name is not touched`() {
        val backups = "$base/tree/6DBF-B253%3APSPLauncher-Backups/document/6DBF-B253%3APSPLauncher-Backups%2Fx.zip"
        assertEquals(backups, EchoFolder.repoint(backups, "$base/tree/6DBF-B253%3APSPL", "$base/tree/6DBF-B253%3AECHO"))
        val sibling = "$base/tree/6DBF-B253%3AEmulation%2FPSPLx/document/6DBF-B253%3AEmulation%2FPSPLx%2Fa.png"
        assertEquals(sibling, EchoFolder.repoint(sibling, oldTree, newTree))
    }

    @Test
    fun `links outside the folder and missing links are left as they are`() {
        val web = "https://cdn2.steamgriddb.com/grid/abc.png"
        assertEquals(web, EchoFolder.repoint(web, oldTree, newTree))
        val internal = "file:///data/user/0/com.echo.launcher/files/artwork/1.jpg"
        assertEquals(internal, EchoFolder.repoint(internal, oldTree, newTree))
        assertNull(EchoFolder.repoint(null, oldTree, newTree))
    }

    @Test
    fun `a folder is ECHO by its last path segment, in any case`() {
        assertTrue(EchoFolder.isEchoTree(newTree))
        assertTrue(EchoFolder.isEchoTree("$base/tree/primary%3AEcho"))
        assertTrue(EchoFolder.isEchoTree("$base/tree/6DBF-B253%3AECHO"))
        assertFalse(EchoFolder.isEchoTree(oldTree))
        assertFalse(EchoFolder.isEchoTree("$base/tree/primary%3AECHO%20old"))
    }
}
