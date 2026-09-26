package com.manish.ridedash.data.sensors

import android.hardware.SensorManager
import android.view.Surface
import kotlin.math.abs

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

    /**
     * Which way the bike is pointing, for a phone standing up on the handlebars.
     *
     * [orientationDeg]'s azimuth is the wrong number for this mount and no amount of landscape
     * remapping fixes it. `getOrientation` reports where the **top edge** of the phone points, which
     * is what you want from a phone lying flat in your palm. Clamped upright on the bars the top
     * edge points at the sky, so that reading is meaningless — and it is the reason the compass read
     * as though it were still in portrait.
     *
     * What actually lines up with the bike is the direction the **back** of the phone faces: the
     * device's −Z axis. Remapping −Z into the position `getOrientation` treats as forward gives the
     * heading directly, and it needs no display-rotation correction at all, because the back of the
     * phone points the same way whichever way the UI has been rotated.
     *
     * Flat on a bench the −Z axis points at the floor and that trick degenerates, so a phone lying
     * down falls back to the ordinary top-edge reading, remapped for how the screen is turned.
     */
    @Synchronized
    fun headingDeg(rotationVector: FloatArray, displayRotation: Int): Float {
        SensorManager.getRotationMatrixFromVector(rotation, rotationVector)

        // Third column of the matrix is the device's Z axis in world coordinates; its vertical
        // component says how close to flat the phone is lying.
        val upright = abs(rotation[8]) < FLAT_LIMIT

        if (upright) {
            SensorManager.remapCoordinateSystem(
                rotation,
                SensorManager.AXIS_X,
                SensorManager.AXIS_MINUS_Z,
                remapped,
            )
        } else {
            val (axisX, axisY) = flatAxesFor(displayRotation)
            SensorManager.remapCoordinateSystem(rotation, axisX, axisY, remapped)
        }

        SensorManager.getOrientation(remapped, orientation)
        return (Math.toDegrees(orientation[0].toDouble()).toFloat() + 360f) % 360f
    }

    /** The standard remap for a flat phone, by how far the screen has been rotated. */
    private fun flatAxesFor(displayRotation: Int): Pair<Int, Int> = when (displayRotation) {
        Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
        Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
        Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
        else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
    }

    /**
     * Above this the phone counts as lying flat. cos(60°): tilted more than 60° from horizontal is
     * treated as standing up, which a handlebar mount comfortably is.
     */
    private const val FLAT_LIMIT = 0.5f
}
