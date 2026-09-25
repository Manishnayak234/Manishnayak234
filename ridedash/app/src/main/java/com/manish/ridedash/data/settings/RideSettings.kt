package com.manish.ridedash.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.rideDataStore: DataStore<Preferences> by preferencesDataStore(name = "ridedash")

/**
 * The handful of things that must survive a restart: the lean zero from the last calibration, where
 * the speed overlay was dragged to, the charger-trigger choices and the last ride's totals.
 */
class RideSettings(context: Context) {

    private val store = context.applicationContext.rideDataStore

    val leanZeroDeg: Flow<Float> = store.data.map { it[LEAN_ZERO] ?: 0f }

    val overlayPosition: Flow<OverlayPosition> = store.data.map {
        OverlayPosition(x = it[OVERLAY_X] ?: DEFAULT_OVERLAY_X, y = it[OVERLAY_Y] ?: DEFAULT_OVERLAY_Y)
    }

    /** Screen brightness for dashboard mode: [BRIGHTNESS_AUTO], or 0..1 set on the slider. */
    val brightness: Flow<Float> = store.data.map { it[BRIGHTNESS] ?: BRIGHTNESS_AUTO }

    val chargerTriggerEnabled: Flow<Boolean> = store.data.map { it[CHARGER_TRIGGER] ?: true }

    /** Only let the charger start the dashboard if the mount's NFC tag was tapped recently. */
    val chargerNeedsNfc: Flow<Boolean> = store.data.map { it[CHARGER_NEEDS_NFC] ?: false }

    val lastRide: Flow<LastRide> = store.data.map {
        LastRide(
            tripKm = it[LAST_TRIP_KM] ?: 0f,
            rideTimeMs = it[LAST_RIDE_MS] ?: 0L,
            maxSpeedKmh = it[LAST_MAX_KMH] ?: 0f,
            avgSpeedKmh = it[LAST_AVG_KMH] ?: 0f,
            leanMaxLeftDeg = it[LAST_LEAN_L] ?: 0f,
            leanMaxRightDeg = it[LAST_LEAN_R] ?: 0f,
            endedAtMs = it[LAST_RIDE_AT] ?: 0L,
        )
    }

    suspend fun setLeanZero(degrees: Float) = store.edit { it[LEAN_ZERO] = degrees }

    suspend fun setOverlayPosition(x: Int, y: Int) = store.edit {
        it[OVERLAY_X] = x
        it[OVERLAY_Y] = y
    }

    suspend fun setBrightness(value: Float) = store.edit { it[BRIGHTNESS] = value }

    suspend fun setChargerTriggerEnabled(enabled: Boolean) =
        store.edit { it[CHARGER_TRIGGER] = enabled }

    suspend fun setChargerNeedsNfc(needsNfc: Boolean) =
        store.edit { it[CHARGER_NEEDS_NFC] = needsNfc }

    /** Stamped when the mount's NFC tag is tapped, so the charger trigger can require a recent tap. */
    suspend fun markNfcTap(atMs: Long) = store.edit { it[LAST_NFC_AT] = atMs }

    suspend fun nfcTappedWithin(windowMs: Long, nowMs: Long): Boolean {
        val tappedAt = store.data.first()[LAST_NFC_AT] ?: return false
        return nowMs - tappedAt in 0..windowMs
    }

    suspend fun saveLastRide(ride: LastRide) = store.edit {
        it[LAST_TRIP_KM] = ride.tripKm
        it[LAST_RIDE_MS] = ride.rideTimeMs
        it[LAST_MAX_KMH] = ride.maxSpeedKmh
        it[LAST_AVG_KMH] = ride.avgSpeedKmh
        it[LAST_LEAN_L] = ride.leanMaxLeftDeg
        it[LAST_LEAN_R] = ride.leanMaxRightDeg
        it[LAST_RIDE_AT] = ride.endedAtMs
    }

    data class OverlayPosition(val x: Int, val y: Int)

    data class LastRide(
        val tripKm: Float = 0f,
        val rideTimeMs: Long = 0L,
        val maxSpeedKmh: Float = 0f,
        val avgSpeedKmh: Float = 0f,
        val leanMaxLeftDeg: Float = 0f,
        val leanMaxRightDeg: Float = 0f,
        val endedAtMs: Long = 0L,
    )

    companion object {
        /** Top right of the screen, roughly where a rider's eyes already are. */
        const val DEFAULT_OVERLAY_X = 24
        const val DEFAULT_OVERLAY_Y = 96

        /** How recent an NFC tap has to be for the charger trigger to accept it. */
        const val NFC_WINDOW_MS = 60_000L

        /**
         * Hands the screen back to the system, which is what the brief asks for by default: the
         * sunlight boost on this phone only kicks in under auto brightness.
         */
        const val BRIGHTNESS_AUTO = -1f

        /** Never let the slider reach black — it would be unreadable, and hard to undo with gloves. */
        const val BRIGHTNESS_MIN = 0.05f

        private val LEAN_ZERO = floatPreferencesKey("lean_zero_deg")
        private val BRIGHTNESS = floatPreferencesKey("brightness")
        private val OVERLAY_X = intPreferencesKey("overlay_x")
        private val OVERLAY_Y = intPreferencesKey("overlay_y")
        private val CHARGER_TRIGGER = booleanPreferencesKey("charger_trigger")
        private val CHARGER_NEEDS_NFC = booleanPreferencesKey("charger_needs_nfc")
        private val LAST_NFC_AT = longPreferencesKey("last_nfc_at")
        private val LAST_TRIP_KM = floatPreferencesKey("last_trip_km")
        private val LAST_RIDE_MS = longPreferencesKey("last_ride_ms")
        private val LAST_MAX_KMH = floatPreferencesKey("last_max_kmh")
        private val LAST_AVG_KMH = floatPreferencesKey("last_avg_kmh")
        private val LAST_LEAN_L = floatPreferencesKey("last_lean_l")
        private val LAST_LEAN_R = floatPreferencesKey("last_lean_r")
        private val LAST_RIDE_AT = longPreferencesKey("last_ride_at")
    }
}
