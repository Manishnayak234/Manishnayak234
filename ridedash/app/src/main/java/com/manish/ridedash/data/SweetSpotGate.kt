package com.manish.ridedash.data

/**
 * The band where a Speed 400 is sipping fuel rather than drinking it.
 *
 * Hysteresis for the same reason as [OverspeedGate]: the window is only five km/h wide, and a note
 * that flickered on and off every time the throttle breathed would be read as a fault rather than
 * as praise.
 */
class SweetSpotGate(
    private val low: Float = LOW_KMH,
    private val high: Float = HIGH_KMH,
    private val slack: Float = SLACK_KMH,
) {
    var inSweetSpot: Boolean = false
        private set

    fun update(speedKmh: Float, speedTrusted: Boolean): Boolean {
        inSweetSpot = when {
            !speedTrusted || speedKmh.isNaN() -> false
            // Once in, it takes a little more than a wobble to fall out again.
            inSweetSpot -> speedKmh >= low - slack && speedKmh <= high + slack
            else -> speedKmh >= low && speedKmh <= high
        }
        return inSweetSpot
    }

    fun reset() {
        inSweetSpot = false
    }

    companion object {
        const val LOW_KMH = 65f
        const val HIGH_KMH = 70f

        /** A km/h either side, so sitting right on the edge does not strobe the note. */
        const val SLACK_KMH = 1.5f
    }
}
