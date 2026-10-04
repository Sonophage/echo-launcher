package com.echo.feature.crossbar.ui

import com.echo.core.ui.wave.WaveStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CrossbarWaveVisibilityTest {
    @Test
    fun `a wallpaper with the toggle off hides the wave`() {
        assertFalse(
            "Wave Over Wallpaper is the only control over whether the wave covers the picture",
            waveVisible(hasWallpaper = true, waveOverWallpaper = false, style = WaveStyle.ANIMATED),
        )
    }

    @Test
    fun `a wallpaper with the toggle on keeps the wave`() {
        assertTrue(
            "the toggle's whole purpose is drawing the wave on top of a wallpaper",
            waveVisible(hasWallpaper = true, waveOverWallpaper = true, style = WaveStyle.ANIMATED),
        )
    }

    @Test
    fun `with no wallpaper the wave draws whatever the toggle says`() {
        assertTrue(
            "the wave IS the default background, so off must not leave a blank screen",
            waveVisible(hasWallpaper = false, waveOverWallpaper = false, style = WaveStyle.ANIMATED),
        )
        assertTrue(
            "the toggle is about wallpapers and must not reach the default background",
            waveVisible(hasWallpaper = false, waveOverWallpaper = true, style = WaveStyle.ANIMATED),
        )
    }

    /**
     * OFF is the one style that means "do not draw it", so it has to beat every
     * other input -- including the no-wallpaper case, where the wave is otherwise
     * the default background and the other tests insist it must never blank out.
     */
    @Test
    fun `Off hides the wave whatever the wallpaper toggle says`() {
        for (hasWallpaper in listOf(false, true)) {
            for (overWallpaper in listOf(false, true)) {
                assertFalse(
                    "wallpaper=$hasWallpaper over=$overWallpaper still drew the wave with style OFF",
                    waveVisible(hasWallpaper, overWallpaper, WaveStyle.OFF),
                )
            }
        }
    }

    @Test
    fun `every other style still draws the wave with no wallpaper`() {
        for (style in WaveStyle.entries.filter { it != WaveStyle.OFF }) {
            assertTrue(
                "$style is a look, not a switch -- only OFF turns the wave off",
                waveVisible(hasWallpaper = false, waveOverWallpaper = false, style = style),
            )
        }
    }

    @Test
    fun `Off survives freezing, so a throttle cannot turn the wave back on`() {
        assertEquals(WaveStyle.OFF, WaveStyle.OFF.frozen)
        assertFalse(WaveStyle.OFF.animated)
        assertFalse(WaveStyle.OFF.drawsWave)
    }
}
