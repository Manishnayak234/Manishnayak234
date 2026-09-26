package com.manish.ridedash.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OverspeedGateTest {

    @Test
    fun `warns once the limit is reached`() {
        val gate = OverspeedGate()

        assertFalse(gate.update(79f, speedTrusted = true))
        assertTrue(gate.update(80f, speedTrusted = true))
    }

    @Test
    fun `hovering at the limit does not strobe`() {
        val gate = OverspeedGate()
        gate.update(82f, speedTrusted = true)

        // Drifting back through the limit keeps the warning up: this is the whole point of the
        // hysteresis, because a warning that blinks is one you learn to ignore.
        assertTrue(gate.update(79f, speedTrusted = true))
        assertTrue(gate.update(77f, speedTrusted = true))
    }

    @Test
    fun `easing off properly clears it`() {
        val gate = OverspeedGate()
        gate.update(90f, speedTrusted = true)

        assertFalse(gate.update(75f, speedTrusted = true))
    }

    @Test
    fun `an untrusted speed never accuses you of speeding`() {
        val gate = OverspeedGate()

        assertFalse(gate.update(140f, speedTrusted = false))
    }

    @Test
    fun `losing the fix drops a warning that was already up`() {
        val gate = OverspeedGate()
        assertTrue(gate.update(95f, speedTrusted = true))

        assertFalse(gate.update(95f, speedTrusted = false))
    }

    @Test
    fun `reset clears it`() {
        val gate = OverspeedGate()
        gate.update(100f, speedTrusted = true)

        gate.reset()

        assertFalse(gate.warning)
    }
}
