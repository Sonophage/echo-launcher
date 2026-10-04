package com.echo.core.ui.image

import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BlurSourceCacheKeyTest {
    @Test
    fun `the small blur decode never takes the full size artwork's memory cache entry`() {
        val uri = "file:///art/blur-key-test.png"
        assertNotEquals(uri, blurSourceCacheKey(uri))
        ArtworkRevisions.bump(uri)
        val revised = ArtworkRevisions.cacheKey(uri)!!
        assertNotEquals(revised, blurSourceCacheKey(uri))
        assertTrue(blurSourceCacheKey(uri).startsWith(revised))
    }
}
