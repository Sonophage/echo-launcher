package com.echo.feature.crossbar.ui

import com.echo.core.domain.model.VideoSnapPlacement
import kotlin.test.Test
import kotlin.test.assertEquals

class SnapSiteTest {
    @Test
    fun `the icon placement plays on the tile`() {
        assertEquals(SnapSite.TILE, snapSiteFor(VideoSnapPlacement.ICON))
    }

    @Test
    fun `the background placement plays behind everything`() {
        assertEquals(SnapSite.BACKGROUND, snapSiteFor(VideoSnapPlacement.BACKGROUND))
    }
}
