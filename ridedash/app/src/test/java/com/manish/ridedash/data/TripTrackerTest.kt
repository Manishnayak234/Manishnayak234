package com.manish.ridedash.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TripTrackerTest {

    @Test
    fun `distance adds up over good fixes`() {
        val tracker = TripTracker()
        // About 100 m apart, ten seconds apart: 36 km/h, which is a plausible step.
        tracker.add(sample(12.9716, 77.5946, 0, 36f))
        tracker.add(sample(12.9725, 77.5946, 10_000, 36f))

        // 0.0009 degrees of latitude is about 100 m.
        assertEquals(100.0, tracker.distanceM, 5.0)
    }

    @Test
    fun `a poor fix is not counted as distance`() {
        val tracker = TripTracker()
        tracker.add(sample(12.9716, 77.5946, 0, 36f, accuracyM = 50f))
        tracker.add(sample(12.9725, 77.5946, 10_000, 36f, accuracyM = 50f))

        assertEquals(0.0, tracker.distanceM, 0.001)
    }

    @Test
    fun `a teleport is dropped`() {
        val tracker = TripTracker()
        tracker.add(sample(12.9716, 77.5946, 0, 36f))
        // 10 km in one second: about 36000 km/h, so the jump is rejected.
        tracker.add(sample(13.0616, 77.5946, 1_000, 36f))

        assertEquals(0.0, tracker.distanceM, 0.001)
    }

    @Test
    fun `ride time only counts movement`() {
        val tracker = TripTracker()
        tracker.add(sample(12.9716, 77.5946, 0, 0f))
        tracker.add(sample(12.9716, 77.5946, 5_000, 0f))
        assertEquals(0L, tracker.movingTimeMs)

        tracker.add(sample(12.9725, 77.5946, 8_000, 36f))
        assertEquals(3_000L, tracker.movingTimeMs)
    }

    @Test
    fun `max speed ignores readings from a poor fix`() {
        val tracker = TripTracker()
        tracker.add(sample(12.9716, 77.5946, 0, 40f))
        tracker.add(sample(12.9716, 77.5946, 1_000, 300f, accuracyM = 80f))

        assertEquals(40f, tracker.maxSpeedKmh, 0.01f)
    }

    @Test
    fun `average speed comes from the moving samples`() {
        val tracker = TripTracker()
        tracker.add(sample(12.9716, 77.5946, 0, 36f))
        tracker.add(sample(12.9725, 77.5946, 10_000, 36f))

        // About 100 m in 10 s of movement: 36 km/h.
        assertEquals(36f, tracker.avgSpeedKmh, 2f)
    }

    @Test
    fun `reset clears the ride`() {
        val tracker = TripTracker()
        tracker.add(sample(12.9716, 77.5946, 0, 36f))
        tracker.add(sample(12.9725, 77.5946, 10_000, 36f))
        tracker.reset()

        assertEquals(0.0, tracker.distanceM, 0.001)
        assertEquals(0L, tracker.movingTimeMs)
        assertEquals(0f, tracker.maxSpeedKmh, 0.001f)
    }

    @Test
    fun `haversine matches a known short hop`() {
        val metres = TripTracker.distanceBetween(12.9716, 77.5946, 12.9716, 77.5955)
        assertTrue("expected about 98 m but was $metres", metres in 90.0..106.0)
    }

    private fun sample(
        lat: Double,
        lon: Double,
        atMs: Long,
        speedKmh: Float,
        accuracyM: Float = 5f,
    ) = TripTracker.Sample(lat, lon, atMs, speedKmh, accuracyM)
}
