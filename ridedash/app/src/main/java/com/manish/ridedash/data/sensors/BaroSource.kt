package com.manish.ridedash.data.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.manish.ridedash.data.RideRepository

/**
 * Altitude from the barometer, which is far steadier than GPS altitude, re-zeroed against a good GPS
 * fix every few minutes so it does not drift with the weather.
 *
 * With no barometer on board the GPS altitude is used straight.
 */
class BaroSource(context: Context) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)

    private var rawAltitudeM: Float? = null
    private var offsetM = 0f
    private var offsetSetAtMs = 0L
    private var smoothedM: Float? = null

    val available: Boolean get() = sensor != null

    fun start() {
        val target = sensor ?: return
        sensorManager.registerListener(this, target, SensorManager.SENSOR_DELAY_NORMAL)
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        rawAltitudeM = null
        smoothedM = null
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_PRESSURE) return
        val hPa = event.values[0]
        val altitude = SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, hPa)
        rawAltitudeM = altitude

        val previous = smoothedM
        val corrected = altitude + offsetM
        smoothedM = if (previous == null) corrected else previous + ALPHA * (corrected - previous)

        RideRepository.update { it.copy(altitudeM = smoothedM, altitudeFromBaro = true) }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    /** Hooked to the location stream: a good fix re-zeroes the barometer, or provides the fallback. */
    fun onGpsAltitude(gpsAltitudeM: Float, accuracyM: Float) {
        val baroAltitude = rawAltitudeM
        if (baroAltitude == null) {
            RideRepository.update { it.copy(altitudeM = gpsAltitudeM, altitudeFromBaro = false) }
            return
        }
        if (accuracyM > MAX_FIX_ACCURACY_M) return

        val nowMs = System.currentTimeMillis()
        if (offsetSetAtMs != 0L && nowMs - offsetSetAtMs < RECALIBRATE_EVERY_MS) return

        val target = gpsAltitudeM - baroAltitude
        // First calibration snaps, later ones ease in so the number never jumps while riding.
        offsetM = if (offsetSetAtMs == 0L) target else offsetM + 0.3f * (target - offsetM)
        offsetSetAtMs = nowMs
    }

    companion object {
        private const val ALPHA = 0.2f
        private const val MAX_FIX_ACCURACY_M = 15f
        private const val RECALIBRATE_EVERY_MS = 3 * 60_000L
    }
}
