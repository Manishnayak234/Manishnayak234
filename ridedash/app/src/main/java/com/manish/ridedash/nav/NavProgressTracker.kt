package com.manish.ridedash.nav

/**
 * The progress bar under the maneuver: 0% at the first reading of a turn, 100% at 0 m.
 *
 * It also decides what counts as a new maneuver, since Maps does not tell us. A changed key is the
 * normal case; a distance that suddenly grows is the fallback for two turns in a row onto the same
 * street.
 */
class NavProgressTracker {

    private var key: String? = null
    private var startMeters: Int? = null

    /** True when the last [progressFor] call started a new maneuver. */
    var lastWasNewManeuver: Boolean = false
        private set

    fun progressFor(maneuverKey: String, meters: Int?): Float {
        val start = startMeters
        val newManeuver = maneuverKey != key ||
            (meters != null && start != null && meters > start * GROWTH_FACTOR + GROWTH_SLACK_M)

        lastWasNewManeuver = newManeuver

        if (newManeuver) {
            key = maneuverKey
            startMeters = meters
            return 0f
        }

        if (meters == null) return 0f
        // A later reading can be further out than the first (a re-route, a slow GPS); take the
        // larger value as the new start so the bar only ever fills forwards.
        val base = maxOf(start ?: meters, meters)
        if (base != start) startMeters = base
        if (base <= 0) return 1f
        return (1f - meters.toFloat() / base.toFloat()).coerceIn(0f, 1f)
    }

    fun reset() {
        key = null
        startMeters = null
        lastWasNewManeuver = false
    }

    companion object {
        private const val GROWTH_FACTOR = 1.3f
        private const val GROWTH_SLACK_M = 50
    }
}
