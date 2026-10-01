package com.psplauncher.feature.xmb.ui.photo

import org.junit.Assert.assertEquals
import org.junit.Test

class PhotoViewerControlsTest {
    @Test fun `the slideshow loops back to the first photo`() {
        assertEquals(1, slideshowNext(0, 3))
        assertEquals("the last photo must lead back to the first, not stop", 0, slideshowNext(2, 3))
        assertEquals(0, slideshowNext(0, 0))
    }

    @Test fun `the filmstrip keeps the current photo in view and stays seven wide`() {
        assertEquals((0..6).toList(), filmstripWindow(0, 312))
        assertEquals((20..26).toList(), filmstripWindow(23, 312))
        assertEquals("at the end the strip stops at the last photo", (305..311).toList(), filmstripWindow(311, 312))
        assertEquals((0..2).toList(), filmstripWindow(1, 3))
    }

    @Test fun `the action row and the info actions stop at their ends`() {
        assertEquals(PhotoControl.ZOOM, PhotoControl.ZOOM.step(-1))
        assertEquals(PhotoControl.REMOVE, PhotoControl.REMOVE.step(1))
        assertEquals(PhotoInfoAction.REMOVE, PhotoInfoAction.REMOVE.step(1))
    }

    @Test fun `the viewer opens with slideshow focused, as drawn`() {
        assertEquals(PhotoControl.SLIDESHOW, PhotoViewerUiState().barFocus)
    }
}
