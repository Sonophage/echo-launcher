package com.echo.feature.artwork.portable

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// owner, 2026-10-07: themes live in ECHO/Themes as folders. A folder someone adds or edits reaches ECHO; ECHO's
// own copies are not read back as edits; and a theme deleted in ECHO does not come back from its folder, which
// ECHO never deletes, until that folder changes
class ThemeFolderRuleTest {
    private val never: () -> Boolean = { error("content compared when the times decide") }

    @Test
    fun `a folder ECHO has no theme for is read in`() {
        assertTrue(shouldReadThemeFolder(folderNewest = 100, saved = false, dismissedAt = null, storedModified = null, sameAsStored = never))
    }

    @Test
    fun `a theme deleted in ECHO stays deleted until its folder changes`() {
        assertFalse(shouldReadThemeFolder(folderNewest = 100, saved = false, dismissedAt = 200, storedModified = null, sameAsStored = never))
        assertTrue(shouldReadThemeFolder(folderNewest = 300, saved = false, dismissedAt = 200, storedModified = null, sameAsStored = never))
    }

    @Test
    fun `a folder older than ECHO's theme is not read`() {
        assertFalse(shouldReadThemeFolder(folderNewest = 100, saved = true, dismissedAt = null, storedModified = 200, sameAsStored = never))
    }

    @Test
    fun `ECHO's own copy is newer than its theme but the same, so it is not read back`() {
        assertFalse(shouldReadThemeFolder(folderNewest = 300, saved = true, dismissedAt = null, storedModified = 200, sameAsStored = { true }))
    }

    @Test
    fun `a folder edited after ECHO stored the theme replaces it`() {
        assertTrue(shouldReadThemeFolder(folderNewest = 300, saved = true, dismissedAt = null, storedModified = 200, sameAsStored = { false }))
    }
}
