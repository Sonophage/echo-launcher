package com.echo.core.ui.design

import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HoldProgressTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `a ring first drawn mid-hold starts empty and fills, so the card that rises with the hold shows it filling`() {
        var progress = -1f
        compose.mainClock.autoAdvance = false
        compose.setContent { progress = holdProgress(holding = true, holdMs = LAUNCH_HOLD_MS) }
        compose.mainClock.advanceTimeByFrame()
        assertTrue("starts empty, was $progress", progress < 0.1f)
        compose.mainClock.advanceTimeBy(LAUNCH_HOLD_MS / 2)
        assertTrue("half full halfway, was $progress", progress in 0.35f..0.65f)
        compose.mainClock.advanceTimeBy(LAUNCH_HOLD_MS)
        assertTrue("full by the end, was $progress", progress > 0.99f)
    }
}
