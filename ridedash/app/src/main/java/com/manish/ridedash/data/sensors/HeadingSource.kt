package com.manish.ridedash.data.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.manish.ridedash.data.RideRepository

/**
 * Compass heading for when the bike is stopped or crawling. Above
 * [LocationSource.HEADING_FROM_GPS_KMH] the GPS bearing is better, so this source stays quiet and
 * lets the location stream own the value.
 */
class HeadingSource(context: Context) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private var smoothedDeg: Float? = null

    fun start() {
        val target = sensor ?: return
        sensorManager.registerListener(this, target, SensorManager.SENSOR_DELAY_UI)
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        smoothedDeg = null
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return
        val state = RideRepository.state.value
        if (state.speedKmh > LocationSource.HEADING_FROM_GPS_KMH) return

        val (azimuth, _, _) = OrientationMath.orientationDeg(event.values)
        smoothedDeg = smoothAngle(smoothedDeg, azimuth)
        RideRepository.update { it.copy(headingDeg = smoothedDeg) }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    companion object {
        private const val ALPHA = 0.2f

        /** EMA that survives the 359 to 0 wrap. */
        fun smoothAngle(previousDeg: Float?, newDeg: Float): Float {
            if (previousDeg == null) return newDeg
            var delta = newDeg - previousDeg
            while (delta > 180f) delta -= 360f
            while (delta < -180f) delta += 360f
            return (previousDeg + ALPHA * delta + 360f) % 360f
        }
    }
}
