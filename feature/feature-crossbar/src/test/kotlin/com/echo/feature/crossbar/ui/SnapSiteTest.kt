package com.echo.feature.crossbar.ui

import com.echo.core.domain.model.VideoSnapPlacement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SnapSiteTest {
    @Test
    fun `the icon placement plays on the tile`() {
        assertEquals(
            SnapSite.TILE,
            snapSiteFor(VideoSnapPlacement.ICON, panelShowingVideo = false),
        )
    }

    @Test
    fun `the background placement plays behind everything`() {
        assertEquals(
            SnapSite.BACKGROUND,
            snapSiteFor(VideoSnapPlacement.BACKGROUND, panelShowingVideo = false),
        )
    }

    @Test
    fun `the panel's video page takes the clip from whichever site would have had it`() {
        VideoSnapPlacement.entries.forEach { placement ->
            assertEquals(
                SnapSite.PANEL,
                snapSiteFor(placement, panelShowingVideo = true),
                "$placement must yield the clip to the open panel",
            )
        }
    }

    @Test
    fun `exactly one renderer draws, for every combination there is`() {
        listOf(true, false).forEach { panelShowingVideo ->
            VideoSnapPlacement.entries.forEach { placement ->
                val site = snapSiteFor(placement, panelShowingVideo)
                val drawing = listOf(
                    site == SnapSite.TILE,
                    site == SnapSite.BACKGROUND,
                    site == SnapSite.PANEL,
                ).count { it }
                assertTrue(
                    drawing <= 1,
                    "$placement/panel=$panelShowingVideo draws in $drawing places at once",
                )
            }
        }
    }
}
