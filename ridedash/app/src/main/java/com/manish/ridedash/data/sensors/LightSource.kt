package com.manish.ridedash.data.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.manish.ridedash.data.RideRepository
import java.util.Calendar

/**
 * Day/night switching from the ambient light sensor, with hysteresis so a bridge or a tunnel does
 * not flip the whole theme.
 *
 * Night below [NIGHT_LUX] and day above [DAY_LUX], each held for [HOLD_MS]. With no light sensor the
 * clock decides.
 */
class LightSource(context: Context) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

    private var candidateNight: Boolean? = null
    private var candidateSinceMs = 0L

    val available: Boolean get() = sensor != null

    fun start() {
        val target = sensor
        if (target == null) {
            RideRepository.update { it.copy(night = nightByClock()) }
            return
        }
        sensorManager.registerListener(this, target, SensorManager.SENSOR_DELAY_NORMAL)
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        candidateNight = null
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_LIGHT) return
        val lux = event.values[0]
        val nowMs = System.currentTimeMillis()
        val isNight = RideRepository.state.value.night

        val wants = when {
            lux < NIGHT_LUX -> true
            lux > DAY_LUX -> false
            else -> null // inside the band: leave it as it is
        }

        if (wants == null || wants == isNight) {
            candidateNight = null
            return
        }

        if (candidateNight != wants) {
            candidateNight = wants
            candidateSinceMs = nowMs
            return
        }

        if (nowMs - candidateSinceMs >= HOLD_MS) {
            candidateNight = null
            RideRepository.update { it.copy(night = wants) }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    companion object {
        const val NIGHT_LUX = 10f
        const val DAY_LUX = 40f
        const val HOLD_MS = 5_000L

        /** Fallback for a phone with no light sensor: roughly sunset to sunrise. */
        fun nightByClock(): Boolean {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            return hour >= 19 || hour < 6
        }
    }
}
