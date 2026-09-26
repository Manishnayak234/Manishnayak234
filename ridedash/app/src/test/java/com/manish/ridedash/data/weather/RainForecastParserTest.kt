package com.manish.ridedash.data.weather

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Open-Meteo returns whole days starting at midnight, so most of the work is throwing away the hours
 * that have already been ridden through. Bodies below are trimmed copies of the real response.
 */
class RainForecastParserTest {

    private fun body(
        times: List<String>,
        chances: List<String>,
        temps: List<String> = times.map { "27.0" },
    ) = """
        {
          "current": {"temperature_2m": 28.3},
          "hourly": {
            "time": [${times.joinToString(",") { "\"$it\"" }}],
            "temperature_2m": [${temps.joinToString(",")}],
            "precipitation_probability": [${chances.joinToString(",")}]
          }
        }
    """.trimIndent()

    @Test
    fun `hours before now are dropped`() {
        val forecast = RainForecastParser.parse(
            body(
                times = listOf("2026-09-26T00:00", "2026-09-26T14:00", "2026-09-26T15:00"),
                chances = listOf("90", "10", "20"),
            ),
            nowHourOfDay = 14,
        )

        assertEquals(listOf(14, 15), forecast!!.hours.map { it.hourOfDay })
        // The 90% at midnight is history and must not become the headline.
        assertEquals(20, forecast.peakChance)
    }

    @Test
    fun `window is capped at the hours asked for`() {
        val times = (10..23).map { "2026-09-26T%02d:00".format(it) }
        val forecast = RainForecastParser.parse(
            body(times = times, chances = times.map { "30" }),
            nowHourOfDay = 10,
        )

        assertEquals(RainForecastParser.HOURS_WANTED, forecast!!.hours.size)
    }

    @Test
    fun `first wet hour is the one worth stopping for`() {
        val forecast = RainForecastParser.parse(
            body(
                times = listOf("2026-09-26T16:00", "2026-09-26T17:00", "2026-09-26T18:00"),
                chances = listOf("10", "70", "95"),
            ),
            nowHourOfDay = 16,
        )!!

        assertTrue(forecast.wet)
        assertEquals(17, forecast.firstWetHour!!.hourOfDay)
        assertEquals(95, forecast.peakChance)
    }

    @Test
    fun `a dry window reports no wet hour`() {
        val forecast = RainForecastParser.parse(
            body(
                times = listOf("2026-09-26T16:00", "2026-09-26T17:00"),
                chances = listOf("0", "15"),
            ),
            nowHourOfDay = 16,
        )!!

        assertNull(forecast.firstWetHour)
        assertEquals(false, forecast.wet)
    }

    @Test
    fun `current temperature is preferred over the hourly column`() {
        val forecast = RainForecastParser.parse(
            body(
                times = listOf("2026-09-26T16:00"),
                chances = listOf("0"),
                temps = listOf("21.0"),
            ),
            nowHourOfDay = 16,
        )!!

        assertEquals(28.3f, forecast.temperatureC!!, 0.01f)
    }

    @Test
    fun `nulls in the probability column are skipped, not read as zero`() {
        val forecast = RainForecastParser.parse(
            body(
                times = listOf("2026-09-26T16:00", "2026-09-26T17:00"),
                chances = listOf("null", "80"),
            ),
            nowHourOfDay = 16,
        )!!

        assertEquals(listOf(17), forecast.hours.map { it.hourOfDay })
        assertEquals(80, forecast.peakChance)
    }

    @Test
    fun `junk and empty responses give nothing rather than a wrong forecast`() {
        assertNull(RainForecastParser.parse("not json", 12))
        assertNull(RainForecastParser.parse("{}", 12))
        assertNull(RainForecastParser.parse("""{"hourly":{"time":[]}}""", 12))
    }

    @Test
    fun `a forecast goes stale and says so`() {
        val forecast = RainForecast(
            temperatureC = 20f,
            hours = listOf(RainHour(16, 10)),
            fetchedAtMs = 0L,
        )

        assertEquals(false, forecast.staleAt(RainForecast.STALE_AFTER_MS - 1))
        assertTrue(forecast.staleAt(RainForecast.STALE_AFTER_MS + 1))
    }
    @Test
    fun `the near-term peak ignores later hours`() {
        val forecast = RainForecast(
            temperatureC = 25f,
            hours = listOf(
                RainHour(14, 5),
                RainHour(15, 10),
                RainHour(16, 90),
            ),
        )

        // A downpour three hours out must not light up the status bar as if it were imminent.
        assertEquals(10, forecast.peakWithin(2))
        assertEquals(90, forecast.peakWithin(3))
        assertEquals(90, forecast.peakChance)
    }

    @Test
    fun `the near-term peak copes with a short or empty window`() {
        val forecast = RainForecast(temperatureC = null, hours = listOf(RainHour(14, 30)))

        assertEquals(30, forecast.peakWithin(5))
        assertEquals(0, forecast.peakWithin(0))
        assertEquals(0, RainForecast(null, emptyList()).peakWithin(2))
    }

}
