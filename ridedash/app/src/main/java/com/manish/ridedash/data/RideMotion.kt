package com.manish.ridedash.data

/**
 * How hard the little bike on the dashboard looks like it is working, given how fast the real one
 * is going.
 *
 * The bike itself never moves — the road slides under it and the exhaust trails behind it, which is
 * how every side-on running animation has ever been done. Both are driven from one intensity so
 * they always agree with each other: a bike laying down smoke over a road that is barely moving
 * would look broken in a way that is hard to name but easy to see.
 *
 * Parked means parked. The road stops, the smoke stops, and the badge sits there as a badge.
 */
object RideMotion {

    /**
     * Whether the bike counts as under way at all.
     *
     * Kept separate from [intensity] on purpose. Intensity is legitimately 0 at the very moment of
     * moving off, so using "intensity above zero" as the test left the animation dead until the
     * speed had climbed past the threshold — a gap right where the crawl was supposed to begin.
     */
    fun moving(speedKmh: Float): Boolean = !speedKmh.isNaN() && speedKmh >= MOVING_KMH

    /** 0 while stopped, rising to 1 at [FULL_KMH] and staying there. */
    fun intensity(speedKmh: Float): Float {
        if (!moving(speedKmh)) return 0f
        val span = (FULL_KMH - MOVING_KMH).coerceAtLeast(1f)
        return ((speedKmh - MOVING_KMH) / span).coerceIn(0f, 1f)
    }

    /** Dash lengths of road sliding past per second. */
    fun roadHz(speedKmh: Float): Float {
        if (!moving(speedKmh)) return 0f
        return ROAD_MIN_HZ + intensity(speedKmh) * (ROAD_MAX_HZ - ROAD_MIN_HZ)
    }

    /** Puffs leaving the exhaust per second. */
    fun smokeHz(speedKmh: Float): Float {
        if (!moving(speedKmh)) return 0f
        return SMOKE_MIN_HZ + intensity(speedKmh) * (SMOKE_MAX_HZ - SMOKE_MIN_HZ)
    }

    /** Below this the bike is parked as far as the badge is concerned. */
    const val MOVING_KMH = 3f

    /** Where the animation is flat out. Beyond this it would only turn to blur. */
    const val FULL_KMH = 120f

    /** Walking pace still crawls, so you can tell it is alive and not frozen. */
    const val ROAD_MIN_HZ = 1.2f
    const val ROAD_MAX_HZ = 14f

    const val SMOKE_MIN_HZ = 1.5f
    const val SMOKE_MAX_HZ = 11f
}
