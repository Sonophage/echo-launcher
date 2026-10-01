package com.psplauncher.feature.xmb.gamepad

import com.psplauncher.core.domain.model.ScrollSpeed
import org.junit.Assert.assertTrue
import org.junit.Test

class ScrollSpeedTuningTest {
    private val slowestFirst = ScrollSpeed.entries.map { it.tuning() }

    @Test fun `each scroll speed waits longer before repeating than the next one up`() {
        slowestFirst.zipWithNext().forEach { (slower, faster) ->
            assertTrue("$slower is not slower than $faster", slower.initialDelayMs > faster.initialDelayMs)
        }
    }

    @Test fun `each scroll speed repeats more slowly than the next one up, at rest and at full ramp`() {
        slowestFirst.zipWithNext().forEach { (slower, faster) ->
            assertTrue("$slower vs $faster at rest", slower.baseIntervalMs > faster.baseIntervalMs)
            assertTrue("$slower vs $faster at full ramp", slower.fastIntervalMs > faster.fastIntervalMs)
        }
    }
}
