package com.manish.ridedash.util

import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * Every number that reaches the screen goes through here, so the dashboard reads the same everywhere
 * and the rules ("blank when there is no fix", "one decimal on the trip") live in one place.
 */
object Formatters {

    const val PLACEHOLDER = "--"

    /** Speed as whole km/h; a bad or missing fix shows [PLACEHOLDER] instead of a stale number. */
    fun speed(speedKmh: Float, valid: Boolean): String =
        if (!valid) PLACEHOLDER else speedKmh.roundToInt().coerceAtLeast(0).toString()

    /** Trip distance: one decimal up to 100 km, whole kilometres past that. */
    fun tripKm(km: Float): String =
        if (km < 100f) String.format(Locale.US, "%.1f km", km)
        else String.format(Locale.US, "%.0f km", km)

    /** Moving time as h:mm above an hour, m:ss below it. */
    fun rideTime(ms: Long): String {
        val totalSeconds = (ms / 1000.0).roundToLong()
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d", hours, minutes)
        } else {
            String.format(Locale.US, "%d:%02d", minutes, seconds)
        }
    }

    fun altitude(meters: Float?): String =
        if (meters == null) PLACEHOLDER else String.format(Locale.US, "%d m", meters.roundToInt())

    /** "96 / 41 km/h" for the max and average tile. */
    fun maxAvg(maxKmh: Float, avgKmh: Float): String =
        "${maxKmh.roundToInt()} / ${avgKmh.roundToInt()}"

    /** "L 23" or "R 12"; the degree sign is drawn separately so it can be sized down. */
    fun lean(leanDeg: Float?): String {
        if (leanDeg == null) return PLACEHOLDER
        val rounded = abs(leanDeg).roundToInt()
        if (rounded == 0) return "0"
        val side = if (leanDeg < 0f) "L" else "R"
        return "$side $rounded"
    }

    fun leanMax(leftDeg: Float, rightDeg: Float): String =
        "Max L ${leftDeg.roundToInt()}° · R ${rightDeg.roundToInt()}°"

    /** "NE 30" — compass point plus the bearing, as in the mockup. */
    fun heading(headingDeg: Float?): String {
        if (headingDeg == null) return PLACEHOLDER
        val degrees = ((headingDeg % 360f) + 360f) % 360f
        return "${compassPoint(degrees)} ${degrees.roundToInt() % 360}"
    }

    /**
     * Eight-point compass, as in the mockup ("NE 30"). Sixteen points would be more precise and less
     * use: at a glance from the saddle the quarter is what matters, and the degrees are right there
     * next to it.
     */
    fun compassPoint(headingDeg: Float): String {
        val points = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        val degrees = ((headingDeg % 360f) + 360f) % 360f
        val index = ((degrees + 22.5f) / 45f).toInt() % 8
        return points[index]
    }

    /** Clock for the status bar; the phone's own 12/24 h choice decides the pattern. */
    fun clock(hour24: Int, minute: Int, use24h: Boolean): String {
        if (use24h) return String.format(Locale.US, "%02d:%02d", hour24, minute)
        val hour12 = when (hour24 % 12) {
            0 -> 12
            else -> hour24 % 12
        }
        return String.format(Locale.US, "%d:%02d", hour12, minute)
    }

    fun batteryTemp(tempC: Float?): String =
        if (tempC == null) PLACEHOLDER else String.format(Locale.US, "%.0f°C", tempC)
}
