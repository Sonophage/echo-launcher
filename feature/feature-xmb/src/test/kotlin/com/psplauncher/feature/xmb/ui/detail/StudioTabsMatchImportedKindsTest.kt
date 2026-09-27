package com.psplauncher.feature.xmb.ui.detail

import com.psplauncher.feature.artwork.migrate.PortableArtworkImportWorker
import org.junit.Assert.assertEquals
import org.junit.Test

class StudioTabsMatchImportedKindsTest {
    @Test
    fun `the kinds the importer copies are exactly the kinds the studio can show`() {
        assertEquals(
            "a kind with a studio tab but no importer entry is art you can set and then lose when " +
                "the artwork folder is unlinked; a kind the importer copies with no tab is a file " +
                "copied into app storage that nothing can ever display",
            STUDIO_TABS.map { it.kind }.toSet(),
            PortableArtworkImportWorker.KEPT_KINDS,
        )
    }
}
