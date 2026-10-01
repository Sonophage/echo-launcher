package com.psplauncher.feature.xmb.ui

import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationBarLegibilityTest {
    @Test
    fun `no text in the notification bar falls below the floor`() {
        val floor = NotificationBarStyle.LegibilityFloorPx
        val title = NotificationBarStyle.TitleSp * NotificationBarStyle.PanelDensity
        val detail = NotificationBarStyle.DetailSp * NotificationBarStyle.PanelDensity
        assertTrue("the title renders at ${title}px, under the ${floor}px floor", title >= floor)
        assertTrue("the detail renders at ${detail}px, under the ${floor}px floor", detail >= floor)
    }

    @Test
    fun `the title is still bigger than the detail`() {
        assertTrue(
            "title ${NotificationBarStyle.TitleSp}sp is not above detail ${NotificationBarStyle.DetailSp}sp",
            NotificationBarStyle.TitleSp > NotificationBarStyle.DetailSp,
        )
    }
}

class MediaDesignLegibilityTest {
    @Test
    fun `scaled-down design text never renders under the floor`() {
        val konkerScale = mediaDesignScale(821f, 462f)
        val eyebrowPx = 12f * konkerScale * NotificationBarStyle.PanelDensity
        assertTrue("precondition: the mock's 12px eyebrow lands at ${eyebrowPx}px on the Konker", eyebrowPx < LEGIBILITY_FLOOR_PX)
        assertTrue(legibleTextPx(eyebrowPx) >= LEGIBILITY_FLOOR_PX)
    }

    @Test
    fun `text already above the floor keeps its size`() {
        org.junit.Assert.assertEquals(60f, legibleTextPx(60f), 0f)
    }
}
