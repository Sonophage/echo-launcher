package com.psplauncher.feature.xmb.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class StripFitTest {
    private fun fit(centre: Int, tightCentre: Int = 100, dateRoom: Boolean = true) =
        stripFit(width = 1000, left = 0, gap = 10, centre = centre, tightCentre = tightCentre, dated = 300, bare = 200, dateRoom = dateRoom)

    @Test
    fun `the strip keeps everything while it fits and gives up the date before the section labels`() {
        assertEquals("a roomy strip keeps the labels and the date", StripFit(labels = true, date = true), fit(centre = 380))
        assertEquals("the date goes first", StripFit(labels = true, date = false), fit(centre = 560))
        assertEquals("then the labels, and the date comes back if it fits", StripFit(labels = false, date = true), fit(centre = 900))
        assertEquals("a centre that never fits still drops both", StripFit(labels = false, date = false), fit(centre = 900, tightCentre = 500))
        assertEquals("a clock with no room for the date never shows it", StripFit(labels = true, date = false), fit(centre = 380, dateRoom = false))
    }

    @Test
    fun `the date needs breathing room in the band so the clock does not touch the top edge`() {
        assertEquals(true, stripDateRoom(clockHeight = 36, band = 67))
        assertEquals(false, stripDateRoom(clockHeight = 38, band = 42))
    }
}
