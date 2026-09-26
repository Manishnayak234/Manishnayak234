package com.manish.ridedash.data.weather

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherCodesTest {

    @Test
    fun `the codes a rider meets have short labels`() {
        assertEquals("Clear", WeatherCodes.label(0))
        assertEquals("Overcast", WeatherCodes.label(3))
        assertEquals("Fog", WeatherCodes.label(45))
        assertEquals("Heavy rain", WeatherCodes.label(65))
        assertEquals("Thunder", WeatherCodes.label(95))
    }

    @Test
    fun `an unknown code reads as a placeholder rather than a guess`() {
        assertEquals("--", WeatherCodes.label(7))
        assertEquals("--", WeatherCodes.label(-1))
    }

    @Test
    fun `wet is what changes how you ride`() {
        assertTrue(WeatherCodes.isWet(61))
        assertTrue(WeatherCodes.isWet(82))
        assertTrue(WeatherCodes.isWet(95))
        assertFalse(WeatherCodes.isWet(0))
        assertFalse(WeatherCodes.isWet(3))
        assertFalse(WeatherCodes.isWet(45))
    }
}
