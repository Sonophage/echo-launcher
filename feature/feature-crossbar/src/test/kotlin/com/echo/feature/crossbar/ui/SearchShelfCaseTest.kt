package com.echo.feature.crossbar.ui

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

// owner, 2026-10-09: the Titan 2 gets its own search shelf; every other device keeps the case it had
class SearchShelfCaseTest {
    @Test
    fun `on the Titan 2 the case is at most 42 percent of the shelf's width`() {
        val h = shelfCaseHeight(room = 560.dp, width = 638.dp, titan2 = true)
        assertEquals(638f * 0.42f, h.value * SHELF_CASE_ASPECT, 0.5f)
    }

    @Test
    fun `any other device's case still fills the room`() {
        assertEquals(560.dp, shelfCaseHeight(room = 560.dp, width = 638.dp, titan2 = false))
        assertEquals(300.dp, shelfCaseHeight(room = 300.dp, width = 1067.dp, titan2 = false))
    }

    @Test
    fun `a short shelf on the Titan 2 is never made taller`() {
        assertEquals(200.dp, shelfCaseHeight(room = 200.dp, width = 638.dp, titan2 = true))
    }
}
