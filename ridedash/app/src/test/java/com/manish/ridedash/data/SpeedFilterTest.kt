package com.manish.ridedash.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SpeedFilterTest {

    @Test
    fun `first sample passes straight through`() {
        val filter = SpeedFilter()
        assertEquals(42f, filter.add(42f, 1_000), 0.001f)
    }

    @Test
    fun `later samples are halfway smoothed`() {
        val filter = SpeedFilter()
        filter.add(40f, 1_000)
        assertEquals(45f, filter.add(50f, 2_000), 0.001f)
    }

    @Test
    fun `a long gap starts again from the new sample`() {
        val filter = SpeedFilter()
        filter.add(80f, 1_000)
        assertEquals(10f, filter.add(10f, 20_000), 0.001f)
    }

    @Test
    fun `reset forgets the old speed`() {
        val filter = SpeedFilter()
        filter.add(80f, 1_000)
        filter.reset()
        assertEquals(10f, filter.add(10f, 2_000), 0.001f)
    }
}
