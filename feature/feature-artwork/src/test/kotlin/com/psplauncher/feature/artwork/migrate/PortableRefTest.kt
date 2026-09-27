package com.psplauncher.feature.artwork.migrate

import com.psplauncher.feature.artwork.migrate.InternalArtworkMigrationWorker.Companion.isPortableRef
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PortableRefTest {
    @Test
    fun `an internal path is not a portable copy, however readable it is`() {
        assertFalse(
            isPortableRef("/data/user/0/com.psplauncher.launcher/files/artwork/1/icon.jpg"),
            "the import rewrites every record to an internal path. Treating a readable ref as " +
                "proof a portable copy exists makes Move Into Folder delete the only copy and " +
                "report it as already covered.",
        )
    }

    @Test
    fun `a document uri in the linked tree is a portable copy`() {
        assertTrue(isPortableRef("content://com.android.externalstorage.documents/tree/primary%3AES-DE/x.png"))
        assertTrue(isPortableRef("CONTENT://weird.casing/x.png"))
    }

    @Test
    fun `a remote url is not a portable copy either`() {
        assertFalse(isPortableRef("https://cdn2.steamgriddb.com/hero/abc.png"))
    }
}
