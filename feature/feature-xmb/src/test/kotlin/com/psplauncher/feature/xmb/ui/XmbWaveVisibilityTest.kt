package com.psplauncher.feature.xmb.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class XmbWaveVisibilityTest {
    @Test
    fun `a wallpaper with the toggle off hides the wave`() {
        assertFalse(
            "Wave Over Wallpaper is the only control over whether the wave covers the picture",
            waveVisible(hasWallpaper = true, waveOverWallpaper = false),
        )
    }

    @Test
    fun `a wallpaper with the toggle on keeps the wave`() {
        assertTrue(
            "the toggle's whole purpose is drawing the wave on top of a wallpaper",
            waveVisible(hasWallpaper = true, waveOverWallpaper = true),
        )
    }

    @Test
    fun `with no wallpaper the wave draws whatever the toggle says`() {
        assertTrue(
            "the wave IS the default background, so off must not leave a blank screen",
            waveVisible(hasWallpaper = false, waveOverWallpaper = false),
        )
        assertTrue(
            "the toggle is about wallpapers and must not reach the default background",
            waveVisible(hasWallpaper = false, waveOverWallpaper = true),
        )
    }
}
