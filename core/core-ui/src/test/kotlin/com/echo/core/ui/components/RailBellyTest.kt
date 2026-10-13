package com.echo.core.ui.components

import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertArrayEquals
import org.junit.Test

// owner, 2026-10-12: while a finger is on the Side Rail the item under it moves left by a thumb width, its
// neighbours less over the rail's three-rung wave, so the thumb never hides what it is choosing
class RailBellyTest {
    private val thumb = 48.dp

    // the wave's cosine at 0, 1, 2 and 3 rungs is 1, 0.75, 0.25 and 0, worked by hand: [leftward] is how far
    // left each rung moves, in dp, and nothing may move up or down
    private fun assertBelly(leftward: FloatArray, belly: List<DpOffset>) {
        assertArrayEquals("each rung's move left", leftward, belly.map { -it.x.value }.toFloatArray(), 0.01f)
        assertArrayEquals("no rung moves up or down", FloatArray(leftward.size), belly.map { it.y.value }.toFloatArray(), 0.01f)
    }

    @Test
    fun `the touched rung moves a thumb width left and its neighbours taper off`() {
        assertBelly(
            floatArrayOf(0f, 0f, 12f, 36f, 48f, 36f, 12f, 0f, 0f),
            railBelly(touched = 4, rungs = 9, bulge = Bulge.LEFT, reach = thumb),
        )
    }

    @Test
    fun `at the top of the rail the belly is only the half below`() {
        assertBelly(
            floatArrayOf(48f, 36f, 12f, 0f, 0f),
            railBelly(touched = 0, rungs = 5, bulge = Bulge.LEFT, reach = thumb),
        )
    }

    @Test
    fun `at the bottom of the rail the belly is only the half above`() {
        assertBelly(
            floatArrayOf(0f, 0f, 12f, 36f, 48f),
            railBelly(touched = 4, rungs = 5, bulge = Bulge.LEFT, reach = thumb),
        )
    }
}
