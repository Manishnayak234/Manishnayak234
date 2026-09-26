package com.manish.ridedash.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RideMotionTest {

    @Test
    fun `parked means parked`() {
        assertEquals(0f, RideMotion.intensity(0f), 0.001f)
        assertEquals(0f, RideMotion.roadHz(0f), 0.001f)
        assertEquals(0f, RideMotion.smokeHz(2.9f), 0.001f)
    }

    @Test
    fun `pulling away crawls rather than jumping`() {
        assertEquals(RideMotion.ROAD_MIN_HZ, RideMotion.roadHz(RideMotion.MOVING_KMH), 0.01f)
        assertEquals(RideMotion.SMOKE_MIN_HZ, RideMotion.smokeHz(RideMotion.MOVING_KMH), 0.01f)
    }

    @Test
    fun `road and smoke rise together`() {
        // They are driven off one intensity on purpose: smoke pouring out over a near-static road
        // looks wrong in a way that is hard to name and easy to see.
        val slowRoad = RideMotion.roadHz(20f)
        val fastRoad = RideMotion.roadHz(90f)
        val slowSmoke = RideMotion.smokeHz(20f)
        val fastSmoke = RideMotion.smokeHz(90f)

        assertTrue(slowRoad < fastRoad)
        assertTrue(slowSmoke < fastSmoke)
    }

    @Test
    fun `it tops out instead of blurring`() {
        assertEquals(1f, RideMotion.intensity(RideMotion.FULL_KMH), 0.001f)
        assertEquals(RideMotion.ROAD_MAX_HZ, RideMotion.roadHz(200f), 0.01f)
        assertEquals(RideMotion.SMOKE_MAX_HZ, RideMotion.smokeHz(200f), 0.01f)
    }

    @Test
    fun `nonsense speeds leave it parked rather than seizing`() {
        assertEquals(0f, RideMotion.intensity(Float.NaN), 0.001f)
        assertEquals(0f, RideMotion.intensity(-50f), 0.001f)
        assertEquals(0f, RideMotion.roadHz(Float.NaN), 0.001f)
    }
}
