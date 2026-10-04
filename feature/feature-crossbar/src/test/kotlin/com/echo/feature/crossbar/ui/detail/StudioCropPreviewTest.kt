package com.echo.feature.crossbar.ui.detail

import com.echo.feature.artwork.store.ArtworkKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StudioCropPreviewTest {
    @Test
    fun `the tile wears the framed PSP tile`() {
        assertEquals(CropPreviewChrome.PSP_TILE, cropPreviewChromeFor(ArtworkKind.ICON))
    }

    @Test
    fun `the video kinds preview in the chrome of the slot they play in`() {
        assertEquals(CropPreviewChrome.PSP_TILE, cropPreviewChromeFor(ArtworkKind.ICON1))
        assertEquals(CropPreviewChrome.FRAMELESS, cropPreviewChromeFor(ArtworkKind.VIDEO))
    }

    @Test
    fun `kinds with no tile representation show no preview`() {
        val noPreview = listOf(
            ArtworkKind.HERO,
            ArtworkKind.BOX_ART,
            ArtworkKind.BOX_3D,
            ArtworkKind.PHYSICAL_MEDIA,
            ArtworkKind.BACKGROUND,
            ArtworkKind.LOGO,
            ArtworkKind.SCREENSHOT,
            ArtworkKind.TITLESCREEN,
            ArtworkKind.MANUAL,
        )
        noPreview.forEach { kind ->
            assertNull("$kind must not preview a tile it never renders", cropPreviewChromeFor(kind))
        }
    }

    @Test
    fun `exactly three kinds preview`() {
        val previewing = ArtworkKind.entries.filter { cropPreviewChromeFor(it) != null }
        assertEquals(
            listOf(
                ArtworkKind.ICON,
                ArtworkKind.ICON1,
                ArtworkKind.VIDEO,
            ).sortedBy { it.ordinal },
            previewing.sortedBy { it.ordinal },
        )
    }

    @Test
    fun `both video-cropped kinds preview`() {
        listOf(ArtworkKind.ICON1, ArtworkKind.VIDEO).forEach { kind ->
            assertNotNull("$kind is cropped as video and must preview", cropPreviewChromeFor(kind))
        }
    }

    @Test
    fun `every previewing kind has a caption and no other kind does`() {
        ArtworkKind.entries.forEach { kind ->
            val caption = cropPreviewCaptionFor(kind)
            if (cropPreviewChromeFor(kind) == null) {
                assertNull("$kind previews nothing, so it needs no caption", caption)
            } else {
                assertNotNull("$kind previews, so it needs a caption", caption)
                assertTrue("$kind caption must not be blank", caption!!.isNotBlank())
            }
        }
    }

    @Test
    fun `captions name the tile the preview stands for`() {
        assertEquals("Crossbar tile", cropPreviewCaptionFor(ArtworkKind.ICON))
        assertEquals("Tile video", cropPreviewCaptionFor(ArtworkKind.ICON1))
        assertEquals("Media strip", cropPreviewCaptionFor(ArtworkKind.VIDEO))
    }
}
