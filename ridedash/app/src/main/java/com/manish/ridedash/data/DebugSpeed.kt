package com.manish.ridedash.data

import com.manish.ridedash.BuildConfig

/**
 * A speed the bench can dial in, so the things that only happen at speed can be seen without
 * riding: the over-80 warning, the road and smoke under the little bike, the switch from compass to
 * GPS bearing.
 *
 * Inert in a release build. [set] refuses to do anything unless `BuildConfig.DEBUG`, so the shipped
 * app has no path to a speed that did not come from the GPS, whatever is broadcast at it.
 *
 * Driven from adb:
 * ```
 * adb shell am broadcast -p com.manish.ridedash -a com.manish.ridedash.DEBUG_SPEED --ef kmh 64
 * adb shell am broadcast -p com.manish.ridedash -a com.manish.ridedash.DEBUG_SPEED --ez off true
 * ```
 */
object DebugSpeed {

    @Volatile
    private var simulated: Float? = null

    val active: Boolean get() = BuildConfig.DEBUG && simulated != null

    fun value(): Float? = if (BuildConfig.DEBUG) simulated else null

    fun set(speedKmh: Float?) {
        if (!BuildConfig.DEBUG) return
        simulated = speedKmh?.coerceIn(0f, 400f)
    }

    const val ACTION = "com.manish.ridedash.DEBUG_SPEED"
    const val EXTRA_KMH = "kmh"
    const val EXTRA_OFF = "off"
}
