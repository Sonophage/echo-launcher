package com.psplauncher.core.ui.icons

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private fun red(bitmap: ImageBitmap): Color? = Color.Red
private fun blue(bitmap: ImageBitmap): Color? = Color.Blue

@RunWith(RobolectricTestRunner::class)
class AppIconCacheTest {
    private fun art(px: Int) = AppIconArt(Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888).asImageBitmap(), null)

    @Test
    fun `a second ask for the same icon is served from memory, not decoded again`() {
        val cache = AppIconCache(maxBytes = 1024 * 1024)
        var loads = 0
        val key = AppIconKey("com.example", 96, false, ::red)
        val first = cache.getOrLoad(key) { loads++; art(96) }
        val second = cache.getOrLoad(AppIconKey("com.example", 96, false, ::red)) { loads++; art(96) }
        assertSame(first, second)
        assertEquals(1, loads)
    }

    @Test
    fun `an updated or reinstalled app is decoded again, so its new icon shows`() {
        val cache = AppIconCache(maxBytes = 1024 * 1024)
        val key = AppIconKey("com.example", 96, false, null)
        val old = cache.getOrLoad(key, version = 1L) { AppIconArt(art(96).bitmap, null, 1L) }
        assertSame(old, cache.getOrLoad(key, version = 1L) { null })

        val updated = cache.getOrLoad(key, version = 2L) { AppIconArt(art(96).bitmap, null, 2L) }

        assertNotSame(old, updated)
        assertSame(updated, cache.peek(key))
    }

    @Test
    fun `the row, the backdrop and the wall never share an entry, so each keeps its own size and colour`() {
        val wall = AppIconKey("com.example", 96, false, ::red)
        assertNotEquals(wall, AppIconKey("com.example", 192, false, ::red))
        assertNotEquals(wall, AppIconKey("com.example", 96, true, ::red))
        assertNotEquals(wall, AppIconKey("com.example", 96, false, ::blue))
    }

    @Test
    fun `an icon that fails to load is not cached, so a later install can still show`() {
        val cache = AppIconCache(maxBytes = 1024 * 1024)
        val key = AppIconKey("com.missing", 96, false, null)
        assertNull(cache.getOrLoad(key) { null })
        assertNull(cache.peek(key))
    }

    @Test
    fun `the budget is counted in bytes, so the oldest icon is evicted first`() {
        val cache = AppIconCache(maxBytes = 2 * 96 * 96 * 4)
        val a = AppIconKey("a", 96, false, null)
        val b = AppIconKey("b", 96, false, null)
        val c = AppIconKey("c", 96, false, null)
        cache.getOrLoad(a) { art(96) }
        cache.getOrLoad(b) { art(96) }
        cache.getOrLoad(c) { art(96) }
        assertNull(cache.peek(a))
        assertEquals(96, cache.peek(c)?.bitmap?.width)
    }
}
