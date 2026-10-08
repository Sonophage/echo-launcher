package com.echo.feature.photos

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

// owner, 2026-10-08: what the frame shows is what the wallpaper keeps
class WallpaperFrameTest {
    // a 4000x2000 photo on a 1920x1080 screen: it fills the height, so 1080/2000 = 0.54
    private val wide = WallpaperFrame(4000, 2000, 0, 1920f, 1080f)

    @Test
    fun `unmoved, the frame keeps the photo's centre at the screen's shape`() {
        // 1920/0.54 = 3555.6 wide of 4000, centred; full height
        assertArrayEquals(floatArrayOf(0.0556f, 0f, 0.9444f, 1f), wide.crop(1f, 0f, 0f), 0.001f)
    }

    @Test
    fun `moving the photo right shows more of its left, and it stops at its edge`() {
        val limit = wide.panLimitX(1f)
        assertEquals("a wide photo moves sideways at normal zoom", (4000 * 0.54f - 1920f) / 2f, limit, 0.5f)
        assertEquals("nothing to move up or down", 0f, wide.panLimitY(1f), 0.01f)
        assertEquals("all the way right keeps the photo's left edge", 0f, wide.crop(1f, limit, 0f)[0], 0.001f)
    }

    @Test
    fun `zooming in keeps a smaller part, and a quarter turn frames the turned photo`() {
        val zoomed = wide.crop(2f, 0f, 0f)
        assertEquals("twice the zoom, half the height", 0.5f, zoomed[3] - zoomed[1], 0.001f)
        val turnedFrame = WallpaperFrame(4000, 2000, 90, 1920f, 1080f)
        // turned it is 2000 wide and 4000 tall; filling 1920 wide is 0.96, so the screen keeps 1125 of its 4000 height
        assertArrayEquals(floatArrayOf(0f, 0.3594f, 1f, 0.6406f), turnedFrame.crop(1f, 0f, 0f), 0.001f)
        // laid out unturned it was drawn at 0.54, so the drawing scales on by 0.96/0.54
        assertEquals(0.96f / 0.54f, turnedFrame.layerScale(1f), 0.001f)
        assertEquals("unturned, the layout's own scale is right", 1f, wide.layerScale(1f), 0.001f)
    }

    @Test
    fun `the viewer frames only while choosing a wallpaper with both sizes known`() {
        val ready = PhotoViewerUiState(wallpaperPreviewVisible = true, imageW = 4000, imageH = 2000, viewW = 1920f, viewH = 1080f)
        assertEquals(wide, ready.wallpaperFrame)
        assertEquals("not choosing a wallpaper", null, ready.copy(wallpaperPreviewVisible = false).wallpaperFrame)
        assertEquals("the photo's size not read yet", null, ready.copy(imageW = 0).wallpaperFrame)
    }
}
