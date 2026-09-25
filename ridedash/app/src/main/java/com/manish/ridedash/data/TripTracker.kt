package com.manish.ridedash.data

/**
 * Trip distance, moving time and the speed records, all from the location stream.
 *
 * Distance is the sum of the gaps between fixes, with the obviously wrong ones dropped: a fix worse
 * than [MAX_ACCURACY_M] or a jump that implies more than [MAX_IMPLIED_KMH] is used for timing but
 * not for distance.
 */
class TripTracker {

    private var lastLat = 0.0
    private var lastLon = 0.0
    private var lastAtMs = 0L
    private var hasLast = false

    var distanceM = 0.0
        private set
    var movingTimeMs = 0L
        private set
    var maxSpeedKmh = 0f
        private set

    /** Distance covered while moving, which is what the average speed is based on. */
    private var movingDistanceM = 0.0

    val avgSpeedKmh: Float
        get() = if (movingTimeMs < 1_000L) 0f
        else (movingDistanceM / (movingTimeMs / 1000.0) * 3.6).toFloat()

    fun add(sample: Sample) {
        if (sample.speedKmh > maxSpeedKmh && sample.accuracyM <= MAX_ACCURACY_M) {
            maxSpeedKmh = sample.speedKmh
        }

        if (!hasLast) {
            remember(sample)
            return
        }

        val dtMs = sample.atMs - lastAtMs
        if (dtMs <= 0L) return

        val stepM = distanceBetween(lastLat, lastLon, sample.lat, sample.lon)
        val impliedKmh = stepM / (dtMs / 1000.0) * 3.6
        val usable = sample.accuracyM <= MAX_ACCURACY_M && impliedKmh <= MAX_IMPLIED_KMH

        if (sample.speedKmh > MOVING_KMH) {
            movingTimeMs += dtMs.coerceAtMost(MAX_GAP_MS)
            if (usable) movingDistanceM += stepM
        }
        if (usable) distanceM += stepM

        remember(sample)
    }

    fun reset() {
        hasLast = false
        distanceM = 0.0
        movingDistanceM = 0.0
        movingTimeMs = 0L
        maxSpeedKmh = 0f
    }

    private fun remember(sample: Sample) {
        lastLat = sample.lat
        lastLon = sample.lon
        lastAtMs = sample.atMs
        hasLast = true
    }

    data class Sample(
        val lat: Double,
        val lon: Double,
        val atMs: Long,
        val speedKmh: Float,
        val accuracyM: Float,
    )

    companion object {
        const val MAX_ACCURACY_M = 20f
        const val MAX_IMPLIED_KMH = 250.0
        const val MOVING_KMH = 3f

        /** A longer gap than this (tunnel, screen off) is not counted as riding time. */
        const val MAX_GAP_MS = 10_000L

        /** Haversine, good enough at the distances between two 1 s fixes. */
        fun distanceBetween(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val earthRadiusM = 6_371_000.0
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
            return 2 * earthRadiusM * Math.asin(Math.sqrt(a).coerceAtMost(1.0))
        }
    }
}
