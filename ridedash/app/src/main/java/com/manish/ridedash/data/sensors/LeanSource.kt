package com.manish.ridedash.data.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.manish.ridedash.data.RideRepository
import kotlin.math.abs

/**
 * Lean angle from the game rotation vector (no magnetometer, so no compass wobble near the bike's
 * wiring).
 *
 * Caveats worth remembering when reading the number on the bike: the phone turns with the steering,
 * so at low speed the value picks up steering input as well as lean. It is a riding toy, not an
 * instrument.
 */
class LeanSource(
    context: Context,
    /** Zero offset from the last calibration, in degrees. */
    private var zeroOffsetDeg: Float = 0f,
) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private var filteredDeg: Float? = null
    private var lastEventNs = 0L

    /** Raw (uncalibrated) reading, kept so a long-press can zero it. */
    private var lastRawDeg = 0f

    val available: Boolean get() = sensor != null

    fun start() {
        val target = sensor ?: return
        sensorManager.registerListener(this, target, SAMPLING_US)
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        filteredDeg = null
        lastEventNs = 0L
    }

    /** Long-press on the lean tile with the bike upright. */
    fun calibrateZero(): Float {
        zeroOffsetDeg = lastRawDeg
        filteredDeg = null
        return zeroOffsetDeg
    }

    fun setZero(offsetDeg: Float) {
        zeroOffsetDeg = offsetDeg
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_GAME_ROTATION_VECTOR &&
            event.sensor.type != Sensor.TYPE_ROTATION_VECTOR
        ) {
            return
        }

        val (_, _, roll) = OrientationMath.orientationDeg(event.values)
        lastRawDeg = roll * LEAN_SIGN

        val leanDeg = lastRawDeg - zeroOffsetDeg
        val dtS = if (lastEventNs == 0L) NOMINAL_DT_S
        else ((event.timestamp - lastEventNs) / 1_000_000_000.0f).coerceIn(0.001f, 0.2f)
        lastEventNs = event.timestamp

        val alpha = dtS / (dtS + TAU_S)
        val previous = filteredDeg
        val smoothed = if (previous == null) leanDeg else previous + alpha * (leanDeg - previous)
        filteredDeg = smoothed

        val rounded = smoothed.coerceIn(-MAX_PLAUSIBLE_DEG, MAX_PLAUSIBLE_DEG)
        RideRepository.update { state ->
            state.copy(
                leanDeg = rounded,
                leanMaxLeftDeg = if (rounded < 0f) maxOf(state.leanMaxLeftDeg, abs(rounded))
                else state.leanMaxLeftDeg,
                leanMaxRightDeg = if (rounded > 0f) maxOf(state.leanMaxRightDeg, rounded)
                else state.leanMaxRightDeg,
            )
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    companion object {
        /** 50 Hz in, low-passed to about 5 Hz below. */
        private const val SAMPLING_US = 20_000

        /** Flip this to -1f if lean reads mirrored with the phone in its real mount. */
        const val LEAN_SIGN = 1f

        /** 5 Hz corner frequency: tau = 1 / (2 * pi * 5). */
        private const val TAU_S = 0.0318f
        private const val NOMINAL_DT_S = 0.02f

        /** Anything past this is the phone being handled, not the bike leaning. */
        private const val MAX_PLAUSIBLE_DEG = 60f
    }
}
