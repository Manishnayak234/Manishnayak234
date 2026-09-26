package com.manish.ridedash.data.sensors

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.GnssStatus
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.manish.ridedash.data.RideRepository
import com.manish.ridedash.data.SpeedFilter
import com.manish.ridedash.data.OverspeedGate
import com.manish.ridedash.data.TripTracker

/**
 * GPS speed, trip numbers and the fix indicator.
 *
 * Fused location is the primary source; if Play Services is missing or unusable we fall back to the
 * raw GPS provider. Satellite count always comes from [LocationManager]'s GNSS status, which fused
 * location does not report.
 */
class LocationSource(
    private val context: Context,
    /** GPS altitude, used to keep the barometer honest. */
    private val onGpsAltitude: (altitudeM: Float, accuracyM: Float) -> Unit = { _, _ -> },
    /** Where we are, for the rain forecast. Not in RideState: nothing on screen draws a position. */
    private val onPosition: (latitude: Double, longitude: Double) -> Unit = { _, _ -> },
) {

    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val fused = LocationServices.getFusedLocationProviderClient(context)

    private val speedFilter = SpeedFilter()
    private val overspeed = OverspeedGate()
    private val trip = TripTracker()

    private var started = false
    private var lastFixAtMs = 0L

    private val fusedCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let(::onLocation)
        }
    }

    /**
     * Written out rather than a lambda: the other three callbacks only became default methods in
     * API 30, and this app runs from API 29.
     */
    private val rawListener = object : LocationListener {
        override fun onLocationChanged(location: Location) = onLocation(location)

        @Deprecated("Deprecated in the platform, but still abstract on API 29")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

        override fun onProviderEnabled(provider: String) = Unit

        override fun onProviderDisabled(provider: String) = Unit
    }

    private val gnssCallback = object : GnssStatus.Callback() {
        override fun onSatelliteStatusChanged(status: GnssStatus) {
            var used = 0
            for (i in 0 until status.satelliteCount) {
                if (status.usedInFix(i)) used++
            }
            RideRepository.update { it.copy(satellites = used) }
        }
    }

    @SuppressLint("MissingPermission")
    fun start() {
        if (started) return
        if (!hasLocationPermission()) {
            Log.w(TAG, "start() without location permission; nothing to do")
            return
        }
        started = true

        if (playServicesUsable()) {
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, INTERVAL_MS)
                .setMinUpdateIntervalMillis(INTERVAL_MS)
                .setWaitForAccurateLocation(false)
                .build()
            fused.requestLocationUpdates(request, fusedCallback, Looper.getMainLooper())
        } else {
            Log.w(TAG, "Play Services unavailable, using GPS_PROVIDER directly")
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                INTERVAL_MS,
                0f,
                rawListener,
                Looper.getMainLooper(),
            )
        }

        runCatching {
            locationManager.registerGnssStatusCallback(gnssCallback, null)
        }.onFailure { Log.w(TAG, "GNSS status unavailable", it) }
    }

    fun stop() {
        if (!started) return
        started = false
        runCatching { fused.removeLocationUpdates(fusedCallback) }
        runCatching { locationManager.removeUpdates(rawListener) }
        runCatching { locationManager.unregisterGnssStatusCallback(gnssCallback) }
        speedFilter.reset()
    }

    /** Called from the service tick so a dropped fix turns the indicator grey by itself. */
    fun checkStale(nowMs: Long) {
        if (lastFixAtMs == 0L) return
        if (nowMs - lastFixAtMs > FIX_STALE_MS) {
            speedFilter.reset()
            overspeed.reset()
            RideRepository.update { it.copy(gpsFix = false, speedValid = false, overspeed = false) }
        }
    }

    fun resetTrip() {
        trip.reset()
        RideRepository.resetRide()
    }

    private fun onLocation(location: Location) {
        val nowMs = System.currentTimeMillis()
        lastFixAtMs = nowMs

        val accuracyM = if (location.hasAccuracy()) location.accuracy else Float.MAX_VALUE
        val rawKmh = if (location.hasSpeed()) location.speed * 3.6f else 0f
        val speedTrusted = location.hasSpeed() && speedAccuracyOk(location)
        val smoothedKmh = speedFilter.add(rawKmh, nowMs)
        val shownKmh = if (isStandingStill(smoothedKmh, location)) 0f else smoothedKmh

        trip.add(
            TripTracker.Sample(
                lat = location.latitude,
                lon = location.longitude,
                atMs = nowMs,
                speedKmh = shownKmh,
                accuracyM = accuracyM,
            )
        )

        val overspeeding = overspeed.update(shownKmh, speedTrusted)

        RideRepository.update { state ->
            state.copy(
                speedKmh = shownKmh,
                speedValid = speedTrusted,
                overspeed = overspeeding,
                gpsFix = true,
                tripKm = (trip.distanceM / 1000.0).toFloat(),
                rideTimeMs = trip.movingTimeMs,
                maxSpeedKmh = trip.maxSpeedKmh,
                avgSpeedKmh = trip.avgSpeedKmh,
                // Bearing is only meaningful once moving; the compass covers the rest.
                headingDeg = if (location.hasBearing() && shownKmh > HEADING_FROM_GPS_KMH) {
                    location.bearing
                } else {
                    state.headingDeg
                },
            )
        }

        if (location.hasAltitude()) {
            onGpsAltitude(location.altitude.toFloat(), accuracyM)
        }

        onPosition(location.latitude, location.longitude)
    }

    /**
     * Fused location keeps reporting a small velocity on a phone that is standing still — measured on
     * the bench at 0.45 m/s with a speed accuracy of 0.40 m/s, while the raw GPS provider said a flat
     * zero. A reading that sits inside its own error bar says nothing, so anything under the floor, or
     * small next to its own accuracy, reads as a clean zero rather than a number that drifts on a
     * parked bike.
     */
    private fun isStandingStill(smoothedKmh: Float, location: Location): Boolean {
        if (smoothedKmh < SPEED_FLOOR_KMH) return true
        if (!location.hasSpeedAccuracy()) return false
        return smoothedKmh < location.speedAccuracyMetersPerSecond * 3.6f * SPEED_NOISE_FACTOR
    }

    private fun speedAccuracyOk(location: Location): Boolean =
        !location.hasSpeedAccuracy() || location.speedAccuracyMetersPerSecond <= MAX_SPEED_ACCURACY_MS

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    private fun playServicesUsable(): Boolean =
        GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) ==
            ConnectionResult.SUCCESS

    companion object {
        private const val TAG = "RideDash/Location"
        const val INTERVAL_MS = 1_000L

        /** Below this the number reads 0 instead of jittering around on a standing bike. */
        const val SPEED_FLOOR_KMH = 2f

        /** How far above its own speed accuracy a reading has to sit before it counts as movement. */
        const val SPEED_NOISE_FACTOR = 3f
        const val HEADING_FROM_GPS_KMH = 5f
        const val MAX_SPEED_ACCURACY_MS = 3f
        const val FIX_STALE_MS = 5_000L
    }
}
