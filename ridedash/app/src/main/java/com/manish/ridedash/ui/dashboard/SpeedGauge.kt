package com.manish.ridedash.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.manish.ridedash.ui.theme.RideDashTheme
import com.manish.ridedash.ui.theme.numberStyle
import com.manish.ridedash.ui.theme.rideColors
import com.manish.ridedash.util.Formatters
import kotlin.math.cos
import kotlin.math.sin

/**
 * The speed arc from the mockup: a 270 degree sweep from 135 degrees clockwise, a track arc under an
 * accent arc, the scale labelled every 30 km/h, and the speed itself in the middle.
 *
 * Geometry is kept in the mockup's 300 unit viewBox and scaled to whatever size it is given, so the
 * proportions hold if the gauge is ever resized.
 */
@Composable
fun SpeedGauge(
    speedKmh: Float,
    speedValid: Boolean,
    tripLine: String,
    modifier: Modifier = Modifier,
    diameter: Dp = 290.dp,
    maxKmh: Float = MAX_SCALE_KMH,
) {
    val colors = rideColors
    val measurer = rememberTextMeasurer()
    val scaleStyle = numberStyle(17.sp, FontWeight.SemiBold).copy(color = colors.sub)

    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        // The brief's 290 dp assumes a 412 dp tall screen. This phone is 440 dpi, so it gives 393 dp
        // and the trip line below used to be clipped off the bottom. Take whatever is left once that
        // line has its room, and the gauge then fits any density instead of one.
        val gaugeSize = minOf(diameter, maxWidth, (maxHeight - TRIP_LINE_HEIGHT).coerceAtLeast(0.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(Modifier.size(gaugeSize), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val unit = this.size.minDimension / VIEW_BOX
                    val radius = RADIUS * unit
                    val strokeWidth = STROKE * unit
                    val center = Offset(this.size.width / 2f, this.size.height / 2f)
                    val arcTopLeft = Offset(center.x - radius, center.y - radius)
                    val arcSize = Size(radius * 2f, radius * 2f)

                    drawArc(
                        color = colors.track,
                        startAngle = START_ANGLE,
                        sweepAngle = SWEEP_ANGLE,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(strokeWidth, cap = StrokeCap.Butt),
                    )

                    val fraction = if (speedValid) (speedKmh / maxKmh).coerceIn(0f, 1f) else 0f
                    if (fraction > 0f) {
                        drawArc(
                            color = colors.accent,
                            startAngle = START_ANGLE,
                            sweepAngle = SWEEP_ANGLE * fraction,
                            useCenter = false,
                            topLeft = arcTopLeft,
                            size = arcSize,
                            style = Stroke(strokeWidth, cap = StrokeCap.Butt),
                        )
                    }

                    // Scale labels sit just inside the track, every 30 km/h.
                    val labelRadius = radius - strokeWidth / 2f - 18f * unit
                    var value = 0f
                    while (value <= maxKmh) {
                        val angle = Math.toRadians((START_ANGLE + SWEEP_ANGLE * (value / maxKmh)).toDouble())
                        val layout = measurer.measure(AnnotatedString(value.toInt().toString()), scaleStyle)
                        drawText(
                            textLayoutResult = layout,
                            topLeft = Offset(
                                center.x + cos(angle).toFloat() * labelRadius - layout.size.width / 2f,
                                center.y + sin(angle).toFloat() * labelRadius - layout.size.height / 2f,
                            ),
                        )
                        value += SCALE_STEP_KMH
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = Formatters.speed(speedKmh, speedValid),
                        style = numberStyle(116.sp, FontWeight.ExtraBold, italic = true),
                        color = colors.fg,
                    )
                    Text(
                        text = "km/h",
                        style = numberStyle(20.sp, FontWeight.SemiBold),
                        color = colors.sub,
                    )
                }
            }

            Text(
                text = tripLine,
                style = numberStyle(17.sp, FontWeight.SemiBold),
                color = colors.sub,
            )
        }
    }
}

/** Room kept below the gauge for the "12.4 km · 0:38" line, so it is never squeezed out. */
private val TRIP_LINE_HEIGHT = 26.dp

private const val VIEW_BOX = 300f
private const val RADIUS = 120f
private const val STROKE = 24f
private const val START_ANGLE = 135f
private const val SWEEP_ANGLE = 270f
private const val SCALE_STEP_KMH = 30f
const val MAX_SCALE_KMH = 180f

@Preview(widthDp = 360, heightDp = 340, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun SpeedGaugePreview() {
    RideDashTheme {
        SpeedGauge(
            speedKmh = 72f,
            speedValid = true,
            tripLine = "12.4 km · 0:38",
        )
    }
}
