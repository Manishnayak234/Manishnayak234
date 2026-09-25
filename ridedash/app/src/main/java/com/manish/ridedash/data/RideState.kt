package com.manish.ridedash.data

import android.graphics.Bitmap

/** Everything the dashboard draws, in one immutable snapshot. */
data class RideState(
    val speedKmh: Float = 0f,
    /** False when there is no fix or the speed accuracy is too poor to show a number. */
    val speedValid: Boolean = false,
    val gpsFix: Boolean = false,
    /** Satellites used in the last fix, -1 when the GNSS status is not known yet. */
    val satellites: Int = -1,
    val tripKm: Float = 0f,
    val rideTimeMs: Long = 0L,
    val maxSpeedKmh: Float = 0f,
    val avgSpeedKmh: Float = 0f,
    val headingDeg: Float? = null,
    /** Negative is left lean, positive is right. Null until the sensor reports. */
    val leanDeg: Float? = null,
    val leanMaxLeftDeg: Float = 0f,
    val leanMaxRightDeg: Float = 0f,
    val altitudeM: Float? = null,
    val altitudeFromBaro: Boolean = false,
    val batteryPct: Int = -1,
    val charging: Boolean = false,
    val batteryTempC: Float? = null,
    val night: Boolean = false,
    val bluetoothOn: Boolean = false,
    /**
     * True when a turn alert would actually reach the watch: Bluetooth on and our alert channel
     * enabled. The goBoult app has no SDK, so the real watch link cannot be read.
     */
    val watchAlertsArmed: Boolean = false,
    val nav: NavState? = null,
) {
    val navigating: Boolean get() = nav != null

    /** Heat is the main risk on a black screen in the sun. */
    val hot: Boolean get() = (batteryTempC ?: 0f) >= HOT_BATTERY_C

    companion object {
        const val HOT_BATTERY_C = 45f
    }
}

/**
 * One maneuver as read from the Google Maps notification. Text fields are kept as strings because
 * Maps changes its wording between versions; only [distanceMeters] is normalised.
 */
data class NavState(
    /** The number shown big, e.g. "350". Empty when Maps gave us an instruction instead. */
    val distanceValue: String = "",
    /** The unit shown small next to it, e.g. "m" or "km". */
    val distanceUnit: String = "",
    /** Distance to the maneuver in metres, null when it could not be parsed. */
    val distanceMeters: Int? = null,
    /** What Maps said to do, when it said it in words instead of a distance. */
    val instruction: String? = null,
    val street: String = "",
    val thenStreet: String? = null,
    val etaClock: String? = null,
    val remainingDistance: String? = null,
    val remainingTime: String? = null,
    /** The maneuver arrow, straight from the notification's large icon. */
    val icon: Bitmap? = null,
    /** 0f at the first reading of this maneuver, 1f at 0 m. */
    val progress: Float = 0f,
    /** Identity of the maneuver: a new value means a new turn (fresh progress, fresh alert). */
    val maneuverKey: String = "",
)
