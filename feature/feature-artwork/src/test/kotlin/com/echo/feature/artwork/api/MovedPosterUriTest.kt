package com.echo.feature.artwork.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// on the owner's device on 2026-10-04, five posters still named the PSPLauncher package after the
// 2.0.0 restore, and their detail screens drew an empty art box
class MovedPosterUriTest {
    private val own = "/data/user/0/com.echo.launcher.debug/files/video_posters/a9e4.jpg"
    private val old = "file:///data/user/0/com.psplauncher.launcher.debug/files/video_posters/a9e4.jpg"

    @Test
    fun `a poster left under the old package points at this app's copy`() =
        assertEquals("file://$own", movedPosterUri(old, own) { it == own })

    @Test
    fun `a poster whose stored file is still there is kept`() =
        assertNull(movedPosterUri(old, own) { true })

    @Test
    fun `with no copy here the stored path is kept, so a fetch can replace it later`() =
        assertNull(movedPosterUri(old, own) { false })

    @Test
    fun `no poster, or one already here, is left alone`() {
        assertNull(movedPosterUri(null, own) { true })
        assertNull(movedPosterUri("", own) { true })
        assertNull(movedPosterUri("file://$own", own) { it == own })
    }

    @Test
    fun `a match skips only films whose poster file is really there`() {
        assertEquals(true, posterOnDisk("file://$own") { it == own })
        assertEquals("a path with no file is fetched again", false, posterOnDisk(old) { it == own })
        assertEquals(false, posterOnDisk(null) { true })
    }
}
