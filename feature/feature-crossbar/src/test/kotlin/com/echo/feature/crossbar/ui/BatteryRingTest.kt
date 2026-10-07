package com.echo.feature.crossbar.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// owner, 2026-10-06: the ring round the profile orb is the battery, filled to the charge
class BatteryRingTest {
    @Test
    fun `the ring fills to the charge, clockwise from the top, and never past full or below empty`() {
        assertEquals(0f, batteryRingSweep(0), 0f)
        assertEquals(180f, batteryRingSweep(50), 0f)
        assertEquals(360f, batteryRingSweep(100), 0f)
        assertEquals("a bad reading cannot overdraw the ring", 360f, batteryRingSweep(140), 0f)
        assertEquals(0f, batteryRingSweep(-1), 0f)
    }

    @Test
    fun `the ring turns red when low, but not while it charges`() {
        assertTrue(batteryRingLow(20, charging = false))
        assertFalse(batteryRingLow(21, charging = false))
        assertFalse("charging is never the low warning", batteryRingLow(5, charging = true))
    }
}
