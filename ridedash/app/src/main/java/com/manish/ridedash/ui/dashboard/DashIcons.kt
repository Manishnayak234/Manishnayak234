package com.manish.ridedash.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * The status-bar and tile icons, drawn rather than imported.
 *
 * Drawing them keeps the sunlight rules easy to hold to: solid strokes, one flat colour each, and a
 * size that can be pushed up without going fuzzy.
 */

@Composable
fun GpsIcon(color: Color, size: Dp = 20.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val stroke = this.size.minDimension * 0.09f
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val radius = this.size.minDimension * 0.30f
        drawCircle(color, radius, center, style = Stroke(stroke))
        drawCircle(color, this.size.minDimension * 0.08f, center)
        // Crosshair ticks
        val inner = radius * 1.25f
        val outer = this.size.minDimension * 0.5f
        listOf(0f, 90f, 180f, 270f).forEach { angle ->
            val radians = Math.toRadians(angle.toDouble())
            val dx = cos(radians).toFloat()
            val dy = sin(radians).toFloat()
            drawLine(
                color = color,
                start = Offset(center.x + dx * inner, center.y + dy * inner),
                end = Offset(center.x + dx * outer, center.y + dy * outer),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
fun WatchIcon(color: Color, size: Dp = 20.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val stroke = s * 0.09f
        val bodySize = s * 0.56f
        val topLeft = Offset((this.size.width - bodySize) / 2f, (this.size.height - bodySize) / 2f)
        drawRoundRect(
            color = color,
            topLeft = topLeft,
            size = Size(bodySize, bodySize),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.14f),
            style = Stroke(stroke),
        )
        // Straps above and below the case.
        val strapWidth = bodySize * 0.45f
        val strapHeight = s * 0.14f
        drawRect(
            color = color,
            topLeft = Offset(this.size.width / 2f - strapWidth / 2f, topLeft.y - strapHeight),
            size = Size(strapWidth, strapHeight),
        )
        drawRect(
            color = color,
            topLeft = Offset(this.size.width / 2f - strapWidth / 2f, topLeft.y + bodySize),
            size = Size(strapWidth, strapHeight),
        )
    }
}

@Composable
fun BluetoothIcon(color: Color, size: Dp = 20.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val stroke = s * 0.09f
        fun p(x: Float, y: Float) = Offset(x / 24f * s, y / 24f * s)
        val path = Path().apply {
            moveTo(p(12f, 3f).x, p(12f, 3f).y)
            lineTo(p(12f, 21f).x, p(12f, 21f).y)
            moveTo(p(12f, 3f).x, p(12f, 3f).y)
            lineTo(p(16.5f, 7.5f).x, p(16.5f, 7.5f).y)
            lineTo(p(12f, 12f).x, p(12f, 12f).y)
            moveTo(p(12f, 12f).x, p(12f, 12f).y)
            lineTo(p(16.5f, 16.5f).x, p(16.5f, 16.5f).y)
            lineTo(p(12f, 21f).x, p(12f, 21f).y)
            moveTo(p(8f, 8f).x, p(8f, 8f).y)
            lineTo(p(12f, 11f).x, p(12f, 11f).y)
            moveTo(p(8f, 16f).x, p(8f, 16f).y)
            lineTo(p(12f, 13f).x, p(12f, 13f).y)
        }
        drawPath(path, color, style = Stroke(stroke, cap = StrokeCap.Round))
    }
}

@Composable
fun BatteryIcon(
    color: Color,
    fillColor: Color,
    percent: Int,
    size: Dp = 26.dp,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.size(size)) {
        val bodyWidth = this.size.width * 0.78f
        val bodyHeight = this.size.height * 0.5f
        val stroke = this.size.minDimension * 0.07f
        val top = (this.size.height - bodyHeight) / 2f
        drawRoundRect(
            color = color,
            topLeft = Offset(0f, top),
            size = Size(bodyWidth, bodyHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(stroke * 1.5f),
            style = Stroke(stroke),
        )
        // Terminal cap.
        drawRoundRect(
            color = color,
            topLeft = Offset(bodyWidth + stroke, top + bodyHeight * 0.28f),
            size = Size(this.size.width * 0.1f, bodyHeight * 0.44f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(stroke),
        )
        val fraction = (percent.coerceIn(0, 100)) / 100f
        val inset = stroke * 1.8f
        if (fraction > 0f) {
            drawRoundRect(
                color = fillColor,
                topLeft = Offset(inset, top + inset),
                size = Size((bodyWidth - inset * 2f) * fraction, bodyHeight - inset * 2f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(stroke),
            )
        }
    }
}

/** Compass rose for the HEADING tile: a fixed ring with a needle that points where the bike points. */
@Composable
fun CompassNeedle(
    headingDeg: Float?,
    color: Color,
    ringColor: Color,
    size: Dp = 56.dp,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        drawCircle(ringColor, s * 0.46f, center, style = Stroke(s * 0.06f))
        val heading = headingDeg ?: return@Canvas
        rotate(heading, center) {
            val needle = Path().apply {
                moveTo(center.x, center.y - s * 0.40f)
                lineTo(center.x + s * 0.17f, center.y + s * 0.26f)
                lineTo(center.x, center.y + s * 0.12f)
                lineTo(center.x - s * 0.17f, center.y + s * 0.26f)
                close()
            }
            drawPath(needle, color)
        }
    }
}

/** LEAN tile: a half-circle with a needle that falls to the side the bike is leaning. */
@Composable
fun LeanIndicator(
    leanDeg: Float?,
    color: Color,
    trackColor: Color,
    size: Dp = 72.dp,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val center = Offset(this.size.width / 2f, this.size.height * 0.78f)
        val radius = s * 0.42f
        drawArc(
            color = trackColor,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2f, radius * 2f),
            style = Stroke(s * 0.08f, cap = StrokeCap.Round),
        )
        val lean = leanDeg ?: return@Canvas
        // Straight up is 0; the needle swings with the lean, clamped to the arc.
        val angle = Math.toRadians((lean.coerceIn(-90f, 90f) - 90f).toDouble())
        drawLine(
            color = color,
            start = center,
            end = Offset(
                center.x + cos(angle).toFloat() * radius,
                center.y + sin(angle).toFloat() * radius,
            ),
            strokeWidth = s * 0.09f,
            cap = StrokeCap.Round,
        )
        drawCircle(color, s * 0.06f, center)
    }
}

/** Stand-in maneuver arrow for when the notification carried no large icon. */
@Composable
fun TurnArrowFallback(color: Color, size: Dp = 112.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        drawTurnArrow(color)
    }
}

/** The small arrow next to "Then". */
@Composable
fun ThenArrow(color: Color, size: Dp = 24.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        drawTurnArrow(color)
    }
}

/** A plain "turn right" glyph: up the stem, then right with a head. */
private fun DrawScope.drawTurnArrow(color: Color) {
    val s = size.minDimension
    val stroke = s * 0.12f
    fun p(x: Float, y: Float) = Offset(x / 24f * s, y / 24f * s)
    val stem = Path().apply {
        moveTo(p(8f, 21f).x, p(8f, 21f).y)
        lineTo(p(8f, 12f).x, p(8f, 12f).y)
        lineTo(p(16f, 12f).x, p(16f, 12f).y)
    }
    drawPath(stem, color, style = Stroke(stroke, cap = StrokeCap.Round))
    val head = Path().apply {
        moveTo(p(13f, 6f).x, p(13f, 6f).y)
        lineTo(p(20f, 12f).x, p(20f, 12f).y)
        lineTo(p(13f, 18f).x, p(13f, 18f).y)
        close()
    }
    drawPath(head, color)
}
