package com.manish.ridedash.record

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.manish.ridedash.R
import com.manish.ridedash.data.RideState
import com.manish.ridedash.util.Formatters
import java.util.Locale

/**
 * Draws the speed panel that gets burned into the recorded video.
 *
 * This is plain [Canvas] work rather than Compose, because it runs inside CameraX's overlay effect on
 * every frame: no composition, no recomposition, just paint into the buffer we are handed. Sizes are
 * all fractions of the frame, so the same code is right whether the camera hands us 1080p or 4K.
 */
class SpeedPanelPainter(context: Context) {

    private val condensedItalic: Typeface? =
        ResourcesCompat.getFont(context, R.font.barlow_condensed_extrabold_italic)
    private val condensed: Typeface? = ResourcesCompat.getFont(context, R.font.barlow_condensed_bold)
    private val body: Typeface? = ResourcesCompat.getFont(context, R.font.barlow_semibold)

    private val panelPaint = Paint().apply {
        isAntiAlias = true
        color = Color.BLACK
    }

    private val edgePaint = Paint().apply {
        isAntiAlias = true
        color = ACCENT
    }

    private val speedPaint = Paint().apply {
        isAntiAlias = true
        color = Color.WHITE
        typeface = condensedItalic
        textAlign = Paint.Align.CENTER
    }

    private val unitPaint = Paint().apply {
        isAntiAlias = true
        color = SUB
        typeface = condensed
        textAlign = Paint.Align.CENTER
    }

    private val labelPaint = Paint().apply {
        isAntiAlias = true
        color = SUB
        typeface = body
        textAlign = Paint.Align.CENTER
    }

    private val valuePaint = Paint().apply {
        isAntiAlias = true
        color = Color.WHITE
        typeface = condensed
        textAlign = Paint.Align.CENTER
    }

    /**
     * Paints the panel down the left of the frame. The camera keeps the rest, so the finished clip has
     * the rider on one side and the numbers on the other.
     */
    fun draw(canvas: Canvas, width: Int, height: Int, state: RideState, elapsedMs: Long) {
        val panelWidth = width * PANEL_FRACTION
        canvas.drawRect(0f, 0f, panelWidth, height.toFloat(), panelPaint)
        // A thin accent edge, so the panel reads as part of the dashboard rather than a black bar.
        canvas.drawRect(panelWidth - height * 0.006f, 0f, panelWidth, height.toFloat(), edgePaint)

        val centerX = panelWidth / 2f

        speedPaint.textSize = height * SPEED_TEXT
        unitPaint.textSize = height * UNIT_TEXT
        labelPaint.textSize = height * LABEL_TEXT
        valuePaint.textSize = height * VALUE_TEXT

        val speedBaseline = height * 0.44f
        canvas.drawText(
            Formatters.speed(state.speedKmh, state.speedValid && state.gpsFix),
            centerX,
            speedBaseline,
            speedPaint,
        )
        canvas.drawText("km/h", centerX, speedBaseline + height * 0.07f, unitPaint)

        // Three rows underneath: how long the clip has run, how far the ride is, and the lean.
        drawRow(canvas, centerX, height * 0.62f, height, "REC", clock(elapsedMs))
        drawRow(canvas, centerX, height * 0.76f, height, "TRIP", Formatters.tripKm(state.tripKm))
        drawRow(canvas, centerX, height * 0.90f, height, "LEAN", leanText(state))
    }

    private fun drawRow(
        canvas: Canvas,
        centerX: Float,
        baseline: Float,
        height: Int,
        label: String,
        value: String,
    ) {
        canvas.drawText(label, centerX, baseline - height * 0.045f, labelPaint)
        canvas.drawText(value, centerX, baseline, valuePaint)
    }

    private fun leanText(state: RideState): String {
        val lean = state.leanDeg ?: return Formatters.PLACEHOLDER
        return "${Formatters.lean(lean)}°"
    }

    private fun clock(elapsedMs: Long): String {
        val totalSeconds = elapsedMs / 1000
        return String.format(Locale.US, "%d:%02d", totalSeconds / 60, totalSeconds % 60)
    }

    companion object {
        /**
         * How much of the frame width the panel takes. 0.4 leaves the camera the larger share while
         * still reading as two halves; set it to 0.5 for a true half-and-half split.
         */
        const val PANEL_FRACTION = 0.4f

        private const val SPEED_TEXT = 0.34f
        private const val UNIT_TEXT = 0.055f
        private const val LABEL_TEXT = 0.035f
        private const val VALUE_TEXT = 0.075f

        private const val ACCENT = 0xFFFFD400.toInt()
        private const val SUB = 0xFFBDBDBD.toInt()
    }
}
