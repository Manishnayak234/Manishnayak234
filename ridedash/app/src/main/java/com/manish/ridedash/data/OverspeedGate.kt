package com.manish.ridedash.data

/**
 * Decides when the dashboard starts and stops shouting about speed.
 *
 * A bare `speed >= 80` would strobe the whole screen red every time the throttle breathed at the
 * limit, which is worse than useless on a bike — it teaches you to ignore it. So the warning comes
 * on at [onKmh] and only goes off again at [offKmh], the same hysteresis the light sensor uses for
 * day/night.
 *
 * Speed that is not trusted never trips it. A bad fix should not accuse you of speeding.
 */
class OverspeedGate(
    private val onKmh: Float = ON_KMH,
    private val offKmh: Float = OFF_KMH,
) {
    var warning: Boolean = false
        private set

    fun update(speedKmh: Float, speedTrusted: Boolean): Boolean {
        warning = when {
            !speedTrusted -> false
            warning -> speedKmh > offKmh
            else -> speedKmh >= onKmh
        }
        return warning
    }

    fun reset() {
        warning = false
    }

    companion object {
        /** The number asked for. */
        const val ON_KMH = 80f

        /** Four km/h of slack, so easing off actually clears it. */
        const val OFF_KMH = 76f
    }
}
