package com.manish.ridedash.data.weather

import android.util.Log
import com.manish.ridedash.data.RideRepository
import com.manish.ridedash.data.TripTracker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Fetches the weather for wherever the bike currently is.
 *
 * Deliberately frugal: one request when the dashboard starts, then only when the weather is stale or
 * the bike has moved far enough for it to be different weather. A failure is logged and dropped — the
 * dashboard keeps showing the last reading, and a rider who is out of signal simply sees the previous
 * numbers rather than an error.
 */
class WeatherSource {

    private var lastLatitude: Double? = null
    private var lastLongitude: Double? = null
    private var lastFetchAtMs = 0L
    private var inFlight = false

    /** Called from the service tick; decides for itself whether anything needs doing. */
    suspend fun refreshIfNeeded(latitude: Double, longitude: Double, nowMs: Long) {
        if (inFlight) return
        if (!isDue(latitude, longitude, nowMs)) return

        inFlight = true
        try {
            val weather = fetch(latitude, longitude, nowMs)
            if (weather != null) {
                lastLatitude = latitude
                lastLongitude = longitude
                lastFetchAtMs = nowMs
                RideRepository.setWeather(weather)
            }
        } finally {
            inFlight = false
        }
    }

    private fun isDue(latitude: Double, longitude: Double, nowMs: Long): Boolean {
        if (lastFetchAtMs == 0L) return true
        if (nowMs - lastFetchAtMs >= REFRESH_EVERY_MS) return true

        val previousLat = lastLatitude ?: return true
        val previousLon = lastLongitude ?: return true
        val movedM = TripTracker.distanceBetween(previousLat, previousLon, latitude, longitude)
        return movedM >= REFRESH_AFTER_M
    }

    private suspend fun fetch(latitude: Double, longitude: Double, nowMs: Long): WeatherState? =
        withContext(Dispatchers.IO) {
            val url = WeatherParser.requestUrl(latitude, longitude)
            runCatching {
                val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = TIMEOUT_MS
                    readTimeout = TIMEOUT_MS
                    requestMethod = "GET"
                }
                try {
                    if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                        Log.w(TAG, "Weather fetch returned ${connection.responseCode}")
                        return@withContext null
                    }
                    connection.inputStream.bufferedReader().use { it.readText() }
                } finally {
                    connection.disconnect()
                }
            }
                .onFailure { Log.w(TAG, "Weather fetch failed", it) }
                .getOrNull()
                ?.let { WeatherParser.parse(it, nowMs) }
        }

    companion object {
        private const val TAG = "RideDash/Weather"

        /** Twenty minutes is plenty: this is riding weather, not a forecast app. */
        const val REFRESH_EVERY_MS = 20 * 60_000L

        /** Or sooner, once the bike is somewhere the weather could genuinely differ. */
        const val REFRESH_AFTER_M = 5_000.0

        private const val TIMEOUT_MS = 8_000
    }
}
