package com.echo.core.ui.detail

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.echo.core.ui.components.ChromeBandBaseHeight
import com.echo.core.domain.model.lightBackgroundAnchors
import com.echo.core.ui.theme.EchoColors
import com.echo.core.ui.theme.composite
import com.echo.core.ui.theme.contrastRatio
import com.echo.core.ui.theme.relativeLuminance
import com.echo.core.ui.theme.CROSSBAR_SCRIM_TOP_ALPHA
import com.echo.core.ui.theme.storefrontColorsFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DetailPaletteTest {
    private fun scheme(wave: Long): EchoColors {
        val (top, bottom) = lightBackgroundAnchors(wave)
        return EchoColors(
            waveColor = Color(wave),
            accentColor = Color.White,
            textPrimary = Color.White,
            textSecondary = Color.White.copy(alpha = 0.7f),
            backgroundOverlay = Color(0x88000000),
            selectedItem = Color.White,
            categoryBar = Color(0x00000000),
            backgroundTop = Color(top),
            backgroundBottom = Color(bottom),
        )
    }

    private val classicBlue = scheme(0xFF0055AA)

    private val schemes = listOf(
        classicBlue,
        scheme(0xFFFF7A1A),
        scheme(0xFF2FA84F),
        scheme(0xFF6A3FB5),
        scheme(0xFFC0182B),
        scheme(0xFFB8C4D0),
        scheme(0xFFF29BB8),
        scheme(0xFFE0A21A),
        scheme(0xFF18A7A0),
        scheme(0xFF14204A),
        scheme(0xFF2B2B2B),
    )

    @Test
    fun `the page is the App Drawer's gradient at the App Drawer's alpha`() {
        for (echo in schemes) {
            val drawer = storefrontColorsFor(echo)
            val page = detailPaletteFor(echo)
            assertEquals(drawer.backgroundDeep, page.pageTop)
            assertEquals(drawer.backgroundMid, page.pageBottom)
        }

        assertEquals(CROSSBAR_SCRIM_TOP_ALPHA, detailPaletteFor(classicBlue).pageTop.alpha, 0.005f)
    }

    @Test
    fun `the header band is see-through like the App Drawer's`() {
        assertEquals(0f, detailPaletteFor(classicBlue).header.alpha, 0f)
    }

    @Test
    fun `text, focus and divider use the App Drawer's roles`() {
        for (echo in schemes) {
            val drawer = storefrontColorsFor(echo)
            val p = detailPaletteFor(echo)
            assertEquals(drawer.textPrimary, p.textPrimary)
            assertEquals(drawer.tileSelectedEdge, p.focus)
            assertEquals(drawer.chromeDivider, p.divider)
        }

        assertEquals(storefrontColorsFor(classicBlue).textSecondary, detailPaletteFor(classicBlue).textMuted)
    }

    private fun onScreen(surface: Color, page: Color): Color = composite(surface, composite(page, Color.White))

    @Test
    fun `classic blue is no darker than the App Drawer`() {
        val p = detailPaletteFor(classicBlue)
        val drawerPage = composite(storefrontColorsFor(classicBlue).backgroundDeep, Color.Black)
        assertTrue(
            "the page must not be darkened past the drawer",
            relativeLuminance(composite(p.pageTop, Color.Black)) >= relativeLuminance(drawerPage) - 0.0001,
        )
    }

    @Test
    fun `rows sit a step darker than the page on dark schemes`() {
        val p = detailPaletteFor(classicBlue)
        val page = composite(p.pageTop, Color.Black)
        val row = composite(p.rowFill, page)
        assertTrue(relativeLuminance(row) < relativeLuminance(page))
        assertTrue("rows stay translucent so the wave reads through", p.rowFill.alpha < 1f)
    }

    @Test
    fun `text stays readable on rows for every scheme`() {
        for (echo in schemes) {
            val p = detailPaletteFor(echo)
            val row = onScreen(p.rowFill, p.pageTop)
            assertTrue("$echo: primary text on a row", contrastRatio(p.textPrimary, row) >= 4.5)
            assertTrue("$echo: muted text on a row", contrastRatio(p.textMuted, row) >= 3.0)
        }
    }

    @Test
    fun `the hero aspect is the banner's full-size width over its full height`() {
        assertEquals(DetailContentMaxWidth - DetailContentPadding * 2, DetailHeroWidth)
        assertEquals(DetailHeroWidth.value / DetailHeroHeight.value, DetailHeroAspect, 0.0001f)
    }
}
