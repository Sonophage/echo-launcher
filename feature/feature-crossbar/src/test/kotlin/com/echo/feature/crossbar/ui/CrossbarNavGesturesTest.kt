package com.echo.feature.crossbar.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CrossbarNavGesturesTest {
    private val stepPx = 64f
    private val flingPx = 420f
    private val backCommitPx = 72f

    @Test fun `travel below one step yields nothing`() {
        assertEquals(0, consumeWholeSteps(30f, stepPx))
        assertEquals(0, consumeWholeSteps(-63f, stepPx))
    }

    @Test fun `each step distance crossed yields one step, remainder carries`() {
        assertEquals(1, consumeWholeSteps(64f, stepPx))
        assertEquals(1, consumeWholeSteps(120f, stepPx))
        assertEquals(-2, consumeWholeSteps(-130f, stepPx))
    }

    @Test fun `long continuous slide yields many steps`() {
        assertEquals(5, consumeWholeSteps(5 * stepPx, stepPx))
    }

    @Test fun `remainder pattern ticks continuously across events`() {
        var acc = 0f
        var steps = 0
        listOf(40f, 40f, 40f, 40f).forEach { d ->
            acc += d
            val whole = consumeWholeSteps(acc, stepPx)
            steps += whole
            acc -= whole * stepPx
        }
        assertEquals(2, steps)
        assertEquals(32f, acc)
    }

    @Test fun `slow release grants no bonus`() {
        assertEquals(0, flingBonusSteps(200f, flingPx))
        assertEquals(0, flingBonusSteps(-300f, flingPx))
    }

    @Test fun `a fling grows with speed instead of stopping at two`() {
        assertEquals(3, flingBonusSteps(-800f, flingPx))
        assertEquals(5, flingBonusSteps(-1500f, flingPx))
        assertTrue(flingBonusSteps(-3000f, flingPx) > flingBonusSteps(-1500f, flingPx))
    }

    @Test fun `it is still bounded, so a flick can never become a free scroll`() {
        assertEquals(12, flingBonusSteps(-99_999f, flingPx))
        assertEquals(-12, flingBonusSteps(99_999f, flingPx))
    }

    @Test fun `a release just past the threshold is worth one step, not zero`() {
        assertEquals(1, flingBonusSteps(-(flingPx + 1f), flingPx))
        assertEquals(-1, flingBonusSteps(flingPx + 1f, flingPx))
    }

    @Test fun `fast down-flick grants upward bonus`() {
        assertEquals(-3, flingBonusSteps(800f, flingPx))
        assertEquals(-5, flingBonusSteps(1500f, flingPx))
    }

    @Test fun `a long enough leftward drag backs out`() {
        assertTrue(commitsSwipeBack(-72f, backCommitPx))
        assertTrue(commitsSwipeBack(-300f, backCommitPx))
    }

    @Test fun `a short leftward drag does not back out`() {
        assertFalse(commitsSwipeBack(-71f, backCommitPx))
        assertFalse(commitsSwipeBack(0f, backCommitPx))
    }

    @Test fun `a rightward drag never backs out, however far`() {
        assertFalse(commitsSwipeBack(72f, backCommitPx))
        assertFalse(commitsSwipeBack(9999f, backCommitPx))
    }

    @Test fun `a lower touch sensitivity needs a faster flick before it skips rows`() {
        val flick = 500f
        assertTrue("precondition: this flick skips rows at Normal",
            flingBonusSteps(flick, flingThresholdPx(1f, com.echo.core.domain.model.TouchSensitivity.NORMAL.stepScale)) != 0)
        assertEquals("Very Low still skips rows on a flick that Normal barely skips", 0,
            flingBonusSteps(flick, flingThresholdPx(1f, com.echo.core.domain.model.TouchSensitivity.VERY_LOW.stepScale)))
    }

    @Test fun `touch sensitivities run from least to most sensitive`() {
        val scales = com.echo.core.domain.model.TouchSensitivity.entries.map { it.stepScale }
        assertEquals("the picker lists them in this order", scales.sortedDescending(), scales)
    }

    @Test fun `a glide never lags more than its lead behind the cursor`() {
        assertEquals(7f, glideStart(2f, 10, maxLead = 3), 0f)
        assertEquals(13f, glideStart(20f, 10, maxLead = 3), 0f)
        assertEquals("a short move keeps where the column already was", 9.4f, glideStart(9.4f, 10, maxLead = 3), 0f)
    }

    @Test fun `a column starts at the focused row, so nothing above it is drawn`() {
        assertEquals(5, columnRows(5, 20, rowsBelow = 4).first)
        assertEquals("the end still clamps to the list", 19, columnRows(17, 20, rowsBelow = 4).last)
        assertTrue("an empty list draws no rows", columnRows(0, 0, rowsBelow = 4).isEmpty())
    }

    // owner, 2026-10-06: inside a category the column fills the screen, the passed rows standing above the
    // cursor; the root keeps only the cursor and below (the test above)
    @Test fun `inside a category the rows already passed are drawn above the cursor`() {
        val rows = drillColumnRows(10, 40, rowsAbove = 2, rowsBelow = 4)
        assertTrue("rows above the cursor are drawn", rows.first < 10)
        assertEquals("as many as fill the top, plus the glide's lead", 10 - 2 - GLIDE_MAX_LEAD_ROWS, rows.first)
        assertEquals("the top clamps to the list", 0, drillColumnRows(1, 40, rowsAbove = 2, rowsBelow = 4).first)
        assertTrue("an empty list draws no rows", drillColumnRows(0, 0, rowsAbove = 2, rowsBelow = 4).isEmpty())
    }
}
