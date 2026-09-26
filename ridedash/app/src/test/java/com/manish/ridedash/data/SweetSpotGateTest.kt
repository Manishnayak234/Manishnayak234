package com.manish.ridedash.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SweetSpotGateTest {

    @Test
    fun `the band is 65 to 70`() {
        val gate = SweetSpotGate()

        assertFalse(gate.update(60f, speedTrusted = true))
        assertTrue(gate.update(65f, speedTrusted = true))
        assertTrue(gate.update(70f, speedTrusted = true))
    }

    @Test
    fun `leaving takes more than a wobble`() {
        val gate = SweetSpotGate()
        gate.update(68f, speedTrusted = true)

        // Just outside the band keeps the note up; a note that blinks is one you stop reading.
        assertTrue(gate.update(71f, speedTrusted = true))
        assertTrue(gate.update(64f, speedTrusted = true))
    }

    @Test
    fun `properly out of the band clears it`() {
        val gate = SweetSpotGate()
        gate.update(68f, speedTrusted = true)

        assertFalse(gate.update(80f, speedTrusted = true))
    }

    @Test
    fun `an untrusted speed never claims the sweet spot`() {
        val gate = SweetSpotGate()

        assertFalse(gate.update(67f, speedTrusted = false))
        assertFalse(gate.update(Float.NaN, speedTrusted = true))
    }

    @Test
    fun `losing the fix drops a note already up`() {
        val gate = SweetSpotGate()
        assertTrue(gate.update(67f, speedTrusted = true))

        assertFalse(gate.update(67f, speedTrusted = false))
    }
}
