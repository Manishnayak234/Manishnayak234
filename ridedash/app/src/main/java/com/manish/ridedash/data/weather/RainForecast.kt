package com.manish.ridedash.data.weather

import org.json.JSONObject

/**
 * The next few hours of rain, which on a bike is the only forecast that changes a decision.
 *
 * Temperature is along for the ride because it costs nothing, but the reason this exists is to
 * answer "do I stop and put the jacket on now, or push on".
 */
data class RainForecast(
    val temperatureC: Float?,
    val hours: List<RainHour>,
    /** When this was fetched, so a stale forecast can be greyed out rather than quietly believed. */
    val fetchedAtMs: Long = 0L,
) {
    /** The worst hour in the window — what the tile shouts about. */
    val peakChance: Int get() = hours.maxOfOrNull { it.chancePercent } ?: 0

    /**
     * The worst chance within the next [hours] hours. The five-hour view is for planning; the next
     * hour or two is what decides whether you pull over now.
     */
    fun peakWithin(hours: Int): Int = hours.takeIf { it > 0 }
        ?.let { this.hours.take(it).maxOfOrNull { hour -> hour.chancePercent } }
        ?: 0

    /** The first hour that crosses [WET], or null when the window stays dry. */
    val firstWetHour: RainHour? get() = hours.firstOrNull { it.chancePercent >= WET }

    val wet: Boolean get() = peakChance >= WET

    fun staleAt(nowMs: Long): Boolean = nowMs - fetchedAtMs > STALE_AFTER_MS

    companion object {
        /** Above this a rider would want to know. Below it, rain is noise. */
        const val WET = 40

        /** The horizon the status bar reports on: near enough that you would act on it. */
        const val SOON_HOURS = 2

        /** Past this the forecast stops being worth trusting on screen. */
        const val STALE_AFTER_MS = 90 * 60 * 1000L
    }
}

/** One hour of the forecast. [hourOfDay] is 0..23 in the phone's own timezone. */
data class RainHour(
    val hourOfDay: Int,
    val chancePercent: Int,
)

/**
 * Open-Meteo's hourly response. Chosen over the alternatives because it needs no API key, no cloud
 * project and no billing account, which matters for an app that is sideloaded onto one phone.
 *
 * Kept as a pure function over the response body so it can be tested without a network: the shapes
 * below are the ones the service actually returns, including the nulls it puts in the arrays when a
 * value is missing for an hour.
 */
object RainForecastParser {

    /**
     * @param nowHourOfDay the current hour, used to drop the hours already behind us — Open-Meteo
     *   returns whole days, starting at midnight.
     */
    fun parse(body: String, nowHourOfDay: Int, hoursWanted: Int = HOURS_WANTED): RainForecast? {
        val root = runCatching { JSONObject(body) }.getOrNull() ?: return null
        val hourly = root.optJSONObject("hourly") ?: return null

        val times = hourly.optJSONArray("time") ?: return null
        val chances = hourly.optJSONArray("precipitation_probability") ?: return null
        val temperatures = hourly.optJSONArray("temperature_2m")

        val hours = mutableListOf<RainHour>()
        var temperatureNow: Float? = root.optJSONObject("current")
            ?.takeIf { it.has("temperature_2m") && !it.isNull("temperature_2m") }
            ?.optDouble("temperature_2m")
            ?.toFloat()

        for (i in 0 until times.length()) {
            if (hours.size >= hoursWanted) break

            val hourOfDay = hourOfDay(times.optString(i)) ?: continue
            // The response starts at midnight, so everything before now is history.
            if (hours.isEmpty() && hourOfDay < nowHourOfDay) continue

            if (chances.isNull(i)) continue
            hours += RainHour(
                hourOfDay = hourOfDay,
                chancePercent = chances.optInt(i, 0).coerceIn(0, 100),
            )

            if (temperatureNow == null && temperatures != null && !temperatures.isNull(i)) {
                temperatureNow = temperatures.optDouble(i).toFloat()
            }
        }

        if (hours.isEmpty()) return null
        return RainForecast(temperatureC = temperatureNow, hours = hours)
    }

    /** "2026-09-26T17:00" -> 17. */
    private fun hourOfDay(stamp: String): Int? {
        val t = stamp.indexOf('T')
        if (t < 0 || stamp.length < t + 3) return null
        return stamp.substring(t + 1, t + 3).toIntOrNull()
    }

    /** Five hours covers the "next 2-5 hours" the dashboard is asked for. */
    const val HOURS_WANTED = 5
}
