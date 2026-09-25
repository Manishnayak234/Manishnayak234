package com.manish.ridedash.data.sensors

import android.hardware.SensorManager

/**
 * Shared rotation-vector maths for a phone clamped to the handlebars in landscape.
 *
 * The sensor axes are defined for the phone held upright, so everything is remapped once for the
 * landscape mount before roll and azimuth are read off.
 */
object OrientationMath {

    private val rotation = FloatArray(9)
    private val remapped = FloatArray(9)
    private val orientation = FloatArray(3)

    /** azimuth, pitch and roll in degrees, for a landscape-mounted phone. */
    @Synchronized
    fun orientationDeg(rotationVector: FloatArray): Triple<Float, Float, Float> {
        SensorManager.getRotationMatrixFromVector(rotation, rotationVector)
        SensorManager.remapCoordinateSystem(
            rotation,
            SensorManager.AXIS_Y,
            SensorManager.AXIS_MINUS_X,
            remapped,
        )
        SensorManager.getOrientation(remapped, orientation)
        val azimuth = (Math.toDegrees(orientation[0].toDouble()).toFloat() + 360f) % 360f
        val pitch = Math.toDegrees(orientation[1].toDouble()).toFloat()
        val roll = Math.toDegrees(orientation[2].toDouble()).toFloat()
        return Triple(azimuth, pitch, roll)
    }
}
