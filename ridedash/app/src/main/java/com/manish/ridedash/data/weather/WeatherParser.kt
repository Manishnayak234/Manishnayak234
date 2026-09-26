package com.manish.ridedash.data.weather

import org.json.JSONObject

/**
 * Parses an Open-Meteo forecast response.
 *
 * Open-Meteo is used because it needs no API key and no account, which matters for an app that is
 * sideloaded rather than published: there is no key to leak in the APK.
 *
 * Like the Maps parser, this is written to survive a response that is missing fields rather than to
 * assume the shape; a field that is not there becomes null, not a crash.
 */
object WeatherParser {

    fun parse(json: String, nowMs: Long): WeatherState? {
        val root = runCatching { JSONObject(json) }.getOrNull() ?: return null
        val current = root.optJSONObject("current") ?: return null

        val temperature = current.optDouble("temperature_2m", Double.NaN)
        if (temperature.isNaN()) return null

        val code = current.optInt("weather_code", -1)
        val feelsLike = current.optDouble("apparent_temperature", temperature)

        return WeatherState(
            temperatureC = temperature.toFloat(),
            feelsLikeC = feelsLike.toFloat(),
            conditionCode = code,
            condition = WeatherCodes.label(code),
            precipitationMm = current.optDouble("precipitation", 0.0).toFloat(),
            rainChancePercent = nextHourRainChance(root),
            windKmh = current.optDouble("wind_speed_10m", 0.0).toFloat(),
            windDirectionDeg = current.optDouble("wind_direction_10m", 0.0).toFloat(),
            fetchedAtMs = nowMs,
        )
    }

    /**
     * The hourly block starts at the current hour, so the first two entries cover now and the next
     * hour. The larger of the two is the honest answer to "am I about to get wet".
     */
    private fun nextHourRainChance(root: JSONObject): Int? {
        val hourly = root.optJSONObject("hourly") ?: return null
        val probabilities = hourly.optJSONArray("precipitation_probability") ?: return null
        if (probabilities.length() == 0) return null

        var highest = -1
        for (index in 0 until minOf(2, probabilities.length())) {
            if (probabilities.isNull(index)) continue
            val value = probabilities.optInt(index, -1)
            if (value > highest) highest = value
        }
        return highest.takeIf { it >= 0 }
    }

    /** The one request this app makes: current conditions plus the next couple of hours of rain. */
    fun requestUrl(latitude: Double, longitude: Double): String =
        "https://api.open-meteo.com/v1/forecast" +
            "?latitude=$latitude&longitude=$longitude" +
            "&current=temperature_2m,apparent_temperature,precipitation,weather_code," +
            "wind_speed_10m,wind_direction_10m" +
            "&hourly=precipitation_probability&forecast_hours=2" +
            "&wind_speed_unit=kmh&timezone=auto"
}
