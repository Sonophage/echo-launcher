package com.echo.feature.crossbar.ui

import com.echo.feature.crossbar.viewmodel.CrossbarItem
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// owner, 2026-10-08: an app in Recent with no art showed black, with no icon and no colour, when it had a game row
class RecentAppArtTest {
    @Test
    fun `an app with no art gets its icon and colour, whether or not it has a game row`() {
        assertTrue(isAppWithoutArt(CrossbarItem(id = "a", title = "Bifrost", packageName = "com.bifrost")))
        assertTrue("marked as a game", isAppWithoutArt(CrossbarItem(id = "b", title = "Bifrost", packageName = "com.bifrost", gameId = 9L)))
        assertFalse("its own art wins", isAppWithoutArt(CrossbarItem(id = "c", title = "Bifrost", packageName = "com.bifrost", artworkUri = "/art.jpg")))
        assertFalse("a ROM is not an app", isAppWithoutArt(CrossbarItem(id = "d", title = "Ico", gameId = 3L)))
    }
}
