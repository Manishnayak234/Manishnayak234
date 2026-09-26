package com.manish.ridedash.data.weather

import android.util.Log
import com.manish.ridedash.data.RideRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar
import kotlin.math.abs

/**
 * Rain for the next few hours, from Open-Meteo.
 *
 * One plain HTTP call and [RainForecastParser]; no Retrofit, no JSON library, no API key, no billing
 * account. The whole feature is a URL and a parser, and adding three dependencies to fetch one
 * object would cost more than it is worth.
 *
 * It is deliberately lazy about refetching. A bike moves, but the weather three hours out does not
 * change because you rode ten minutes down the road, so a new call only happens when the forecast
 * ages out or the bike has genuinely moved somewhere else. Mobile data on a ride is not free.
 */
class WeatherSource {

    private var lastLat = Double.NaN
    private var lastLon = Double.NaN
    private var lastFetchAtMs = 0L

    /** True when it is worth spending a request. */
    fun due(latitude: Double, longitude: Double, nowMs: Long): Boolean {
        if (lastFetchAtMs == 0L) return true
        if (nowMs - lastFetchAtMs >= REFRESH_MS) return true
        if (lastLat.isNaN() || lastLon.isNaN()) return true
        return movedFar(latitude, longitude)
    }

    /**
     * Fetches and publishes into [RideRepository]. Returns false when it did not work, and the
     * dashboard simply keeps showing the previous forecast — a failed call is not worth a message on
     * a screen being read at speed.
     */
    suspend fun refresh(latitude: Double, longitude: Double, nowMs: Long): Boolean {
        val body = withTimeoutOrNull(TIMEOUT_MS) { get(url(latitude, longitude)) }
        if (body == null) {
            Log.w(TAG, "No forecast: request timed out or failed")
            return false
        }

        val hourOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val forecast = RainForecastParser.parse(body, hourOfDay)
        if (forecast == null) {
            Log.w(TAG, "No forecast: response did not parse")
            return false
        }

        lastLat = latitude
        lastLon = longitude
        lastFetchAtMs = nowMs
        RideRepository.update { it.copy(rain = forecast.copy(fetchedAtMs = nowMs)) }
        Log.i(TAG, "Forecast: peak ${forecast.peakChance}% over ${forecast.hours.size} h")
        return true
    }

    private fun movedFar(latitude: Double, longitude: Double): Boolean {
        // Rough and deliberately so: a degree of latitude is about 111 km, and this only decides
        // whether to spend a request.
        val degrees = abs(latitude - lastLat) + abs(longitude - lastLon)
        return degrees * 111.0 >= MOVED_KM
    }

    private fun url(latitude: Double, longitude: Double): String =
        "https://api.open-meteo.com/v1/forecast" +
            "?latitude=%.3f&longitude=%.3f".format(latitude, longitude) +
            "&hourly=temperature_2m,precipitation_probability" +
            "&current=temperature_2m" +
            "&forecast_days=2" +
            "&timezone=auto"

    private suspend fun get(url: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = TIMEOUT_MS.toInt()
                readTimeout = TIMEOUT_MS.toInt()
                setRequestProperty("Accept", "application/json")
            }
            try {
                if (connection.responseCode !in 200..299) {
                    Log.w(TAG, "Forecast HTTP ${connection.responseCode}")
                    return@runCatching null
                }
                connection.inputStream.bufferedReader().use { it.readText() }
            } finally {
                connection.disconnect()
            }
        }.getOrElse { error ->
            Log.w(TAG, "Forecast request failed", error)
            null
        }
    }

    companion object {
        private const val TAG = "RideDash/Weather"

        /** Half an hour is plenty for a forecast measured in whole hours. */
        const val REFRESH_MS = 30 * 60 * 1000L

        /** Far enough that the local forecast could genuinely differ. */
        const val MOVED_KM = 15.0

        private const val TIMEOUT_MS = 8_000L
    }
}
