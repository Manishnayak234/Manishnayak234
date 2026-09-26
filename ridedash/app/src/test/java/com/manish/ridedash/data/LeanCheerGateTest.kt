package com.manish.ridedash.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LeanCheerGateTest {

    @Test
    fun `left and right earn different cheers`() {
        val gate = LeanCheerGate()

        assertEquals(LeanSide.LEFT, gate.update(-20f, nowMs = 0L))
        assertEquals(LeanSide.RIGHT, gate.update(20f, nowMs = 0L))
    }

    @Test
    fun `gentle lean earns nothing`() {
        val gate = LeanCheerGate()

        assertNull(gate.update(-14f, nowMs = 0L))
        assertNull(gate.update(14f, nowMs = 0L))
        assertNull(gate.update(0f, nowMs = 0L))
    }

    @Test
    fun `the cheer outlives the lean that earned it`() {
        val gate = LeanCheerGate()
        gate.update(-22f, nowMs = 0L)

        // Upright again, but still readable: that is the whole point, since nobody reads text
        // mid-corner at twenty degrees of lean.
        assertEquals(LeanSide.LEFT, gate.update(0f, nowMs = 1_000L))
        assertEquals(LeanSide.LEFT, gate.update(0f, nowMs = LeanCheerGate.HOLD_MS - 1))
    }

    @Test
    fun `and then it goes away`() {
        val gate = LeanCheerGate()
        gate.update(-22f, nowMs = 0L)

        assertNull(gate.update(0f, nowMs = LeanCheerGate.HOLD_MS))
    }

    @Test
    fun `leaning again restarts the hold`() {
        val gate = LeanCheerGate()
        gate.update(-22f, nowMs = 0L)
        gate.update(-22f, nowMs = 2_000L)

        assertEquals(LeanSide.LEFT, gate.update(0f, nowMs = 4_000L))
    }

    @Test
    fun `a missing or nonsense lean neither triggers nor crashes`() {
        val gate = LeanCheerGate()

        assertNull(gate.update(null, nowMs = 0L))
        assertNull(gate.update(Float.NaN, nowMs = 0L))
    }
}
