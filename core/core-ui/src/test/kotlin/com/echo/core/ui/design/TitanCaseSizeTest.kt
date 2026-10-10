package com.echo.core.ui.design

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// owner, 2026-10-09: the case's spine, logo and faint logo grow with a large case on the Titan 2 only
class TitanCaseSizeTest {
    @Test
    fun `on the Titan 2 a large case's spine and logos grow with it`() {
        assertEquals(267f * TITAN_SPINE_SHARE, titanSpineWidth(13.dp, 267.dp, titan2 = true).value, 0.01f)
        assertEquals(240f * TITAN_ICON_SHARE, faceSize(64.dp, 240.dp, TITAN_ICON_SHARE, grows = true).value, 0.01f)
        assertEquals(240f * TITAN_MARK_SHARE, faceSize(69.dp, 240.dp, TITAN_MARK_SHARE, grows = true).value, 0.01f)
    }

    @Test
    fun `a small case on the Titan 2 keeps its sizes`() {
        assertEquals(13.dp, titanSpineWidth(13.dp, 110.dp, titan2 = true))
        assertEquals(64.dp, faceSize(64.dp, 95.dp, TITAN_ICON_SHARE, grows = true))
    }

    @Test
    fun `every other device keeps its sizes whatever the case's size`() {
        assertEquals(13.dp, titanSpineWidth(13.dp, 400.dp, titan2 = false))
        assertEquals(64.dp, faceSize(64.dp, 400.dp, TITAN_ICON_SHARE, grows = false))
        assertEquals(69.dp, faceSize(69.dp, 400.dp, TITAN_MARK_SHARE, grows = false))
    }

    // owner, 2026-10-09: the App Drawer's logos are larger on every device. A Konker drawer case's face is about
    // 124 dp wide, where the fixed sizes were a 64 dp logo and a 104 dp faint logo
    @Test
    fun `the App Drawer's logo and faint logo are larger than the fixed sizes on the Konker`() {
        assertTrue(faceSize(64.dp, 124.dp, DRAWER_LOGO_SHARE, grows = true) >= 64.dp * 1.15f)
        assertTrue(faceSize(104.dp, 124.dp, DRAWER_MARK_SHARE, grows = true) >= 104.dp * 1.25f)
    }
}
