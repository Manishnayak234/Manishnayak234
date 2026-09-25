package com.manish.ridedash.util

import org.junit.Assert.assertEquals
import org.junit.Test

class FormattersTest {

    @Test
    fun `speed is blanked when the fix cannot be trusted`() {
        assertEquals("72", Formatters.speed(71.6f, valid = true))
        assertEquals(Formatters.PLACEHOLDER, Formatters.speed(71.6f, valid = false))
    }

    @Test
    fun `trip distance loses the decimal past a hundred kilometres`() {
        assertEquals("12.4 km", Formatters.tripKm(12.44f))
        assertEquals("104 km", Formatters.tripKm(104.4f))
    }

    @Test
    fun `ride time switches from minutes to hours`() {
        assertEquals("0:45", Formatters.rideTime(45_000L))
        assertEquals("38:20", Formatters.rideTime(38 * 60_000L + 20_000L))
        assertEquals("1:38", Formatters.rideTime(98 * 60_000L))
    }

    @Test
    fun `lean shows the side it is leaning`() {
        assertEquals("L 23", Formatters.lean(-23.2f))
        assertEquals("R 12", Formatters.lean(12.4f))
        assertEquals("0", Formatters.lean(0.2f))
        assertEquals(Formatters.PLACEHOLDER, Formatters.lean(null))
    }

    @Test
    fun `compass points cover the wrap around north`() {
        assertEquals("N", Formatters.compassPoint(0f))
        assertEquals("N", Formatters.compassPoint(359f))
        assertEquals("NE", Formatters.compassPoint(45f))
        assertEquals("S", Formatters.compassPoint(180f))
        assertEquals("W", Formatters.compassPoint(292f))
    }

    @Test
    fun `heading reads as a point and a bearing`() {
        assertEquals("NE 30", Formatters.heading(30f))
        assertEquals("N 0", Formatters.heading(360f))
        assertEquals(Formatters.PLACEHOLDER, Formatters.heading(null))
    }

    @Test
    fun `clock follows the phone's twelve or twenty four hour choice`() {
        assertEquals("18:42", Formatters.clock(18, 42, use24h = true))
        assertEquals("6:42", Formatters.clock(18, 42, use24h = false))
        assertEquals("12:05", Formatters.clock(0, 5, use24h = false))
        assertEquals("00:05", Formatters.clock(0, 5, use24h = true))
    }

    @Test
    fun `max and average speed share one line`() {
        assertEquals("96 / 41", Formatters.maxAvg(96.4f, 40.6f))
    }
}
