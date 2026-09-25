package com.manish.ridedash.data

import com.manish.ridedash.data.sensors.HeadingSource
import org.junit.Assert.assertEquals
import org.junit.Test

class HeadingSmoothingTest {

    @Test
    fun `first reading is taken as is`() {
        assertEquals(180f, HeadingSource.smoothAngle(null, 180f), 0.001f)
    }

    @Test
    fun `smoothing takes the short way round north`() {
        // From 350 to 10 degrees is 20 degrees clockwise, not 340 anticlockwise.
        val smoothed = HeadingSource.smoothAngle(350f, 10f)
        assertEquals(354f, smoothed, 0.001f)
    }

    @Test
    fun `result always stays inside a full circle`() {
        val smoothed = HeadingSource.smoothAngle(2f, 350f)
        assertEquals(359.6f, smoothed, 0.01f)
    }
}
