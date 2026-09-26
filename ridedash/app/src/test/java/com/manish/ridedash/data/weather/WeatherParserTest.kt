package com.manish.ridedash.data.weather

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Fixtures in the shape Open-Meteo actually answers with, plus the ways a response can be short. */
class WeatherParserTest {

    private val full = """
        {
          "latitude": 12.97, "longitude": 77.59,
          "current": {
            "time": "2026-09-26T09:00",
            "temperature_2m": 27.4,
            "apparent_temperature": 30.1,
            "precipitation": 0.0,
            "weather_code": 2,
            "wind_speed_10m": 11.6,
            "wind_direction_10m": 48
          },
          "hourly": {
            "time": ["2026-09-26T09:00", "2026-09-26T10:00"],
            "precipitation_probability": [20, 45]
          }
        }
    """.trimIndent()

    @Test
    fun `a full response gives every field`() {
        val weather = WeatherParser.parse(full, nowMs = 1_000L)!!

        assertEquals(27.4f, weather.temperatureC, 0.01f)
        assertEquals(30.1f, weather.feelsLikeC, 0.01f)
        assertEquals(2, weather.conditionCode)
        assertEquals("Part cloud", weather.condition)
        assertEquals(0f, weather.precipitationMm, 0.01f)
        assertEquals(11.6f, weather.windKmh, 0.01f)
        assertEquals(48f, weather.windDirectionDeg, 0.01f)
        assertEquals(1_000L, weather.fetchedAtMs)
    }

    @Test
    fun `rain chance takes the worse of this hour and the next`() {
        val weather = WeatherParser.parse(full, nowMs = 0L)!!
        assertEquals(45, weather.rainChancePercent)
    }

    @Test
    fun `forty five percent is not yet a warning`() {
        assertFalse(WeatherParser.parse(full, nowMs = 0L)!!.rainLikely)
    }

    @Test
    fun `a high chance is a warning`() {
        val soggy = full.replace("[20, 45]", "[30, 80]")
        assertTrue(WeatherParser.parse(soggy, nowMs = 0L)!!.rainLikely)
    }

    @Test
    fun `rain falling now is a warning whatever the forecast says`() {
        val raining = full.replace("\"precipitation\": 0.0", "\"precipitation\": 1.4")
        val weather = WeatherParser.parse(raining, nowMs = 0L)!!
        assertEquals(1.4f, weather.precipitationMm, 0.01f)
        assertTrue(weather.rainLikely)
    }

    @Test
    fun `a response with no hourly block still gives the temperature`() {
        val currentOnly = """
            {"current": {"temperature_2m": 19.0, "weather_code": 61}}
        """.trimIndent()
        val weather = WeatherParser.parse(currentOnly, nowMs = 0L)!!

        assertEquals(19f, weather.temperatureC, 0.01f)
        assertEquals("Light rain", weather.condition)
        assertNull(weather.rainChancePercent)
        // Nothing said about apparent temperature, so it falls back to the real one.
        assertEquals(19f, weather.feelsLikeC, 0.01f)
    }

    @Test
    fun `nulls in the forecast are skipped rather than read as zero`() {
        val gappy = full.replace("[20, 45]", "[null, 70]")
        assertEquals(70, WeatherParser.parse(gappy, nowMs = 0L)!!.rainChancePercent)
    }

    @Test
    fun `rubbish and empty responses come back null`() {
        assertNull(WeatherParser.parse("", nowMs = 0L))
        assertNull(WeatherParser.parse("not json at all", nowMs = 0L))
        assertNull(WeatherParser.parse("""{"error": true, "reason": "nope"}""", nowMs = 0L))
        // No temperature means nothing worth showing.
        assertNull(WeatherParser.parse("""{"current": {"weather_code": 0}}""", nowMs = 0L))
    }

    @Test
    fun `the request asks for what the tiles need, in the units they show`() {
        val url = WeatherParser.requestUrl(12.9716, 77.5946)

        assertTrue(url.startsWith("https://api.open-meteo.com/v1/forecast?"))
        assertTrue(url.contains("latitude=12.9716"))
        assertTrue(url.contains("longitude=77.5946"))
        assertTrue(url.contains("temperature_2m"))
        assertTrue(url.contains("apparent_temperature"))
        assertTrue(url.contains("precipitation_probability"))
        assertTrue(url.contains("wind_speed_unit=kmh"))
    }
}
