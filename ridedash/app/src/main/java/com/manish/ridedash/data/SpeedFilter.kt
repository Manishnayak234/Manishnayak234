package com.manish.ridedash.data

/**
 * Light smoothing for GPS speed: EMA with alpha 0.5, which is enough to stop the number flickering
 * without adding visible lag. Stale gaps reset the filter so a new fix is not dragged towards an
 * old one.
 */
class SpeedFilter(
    private val alpha: Float = 0.5f,
    private val resetAfterMs: Long = 5_000L,
) {
    private var value: Float? = null
    private var lastAtMs = 0L

    fun add(rawKmh: Float, atMs: Long): Float {
        val previous = value
        val smoothed = if (previous == null || atMs - lastAtMs > resetAfterMs) {
            rawKmh
        } else {
            previous + alpha * (rawKmh - previous)
        }
        value = smoothed
        lastAtMs = atMs
        return smoothed
    }

    fun reset() {
        value = null
        lastAtMs = 0L
    }
}
