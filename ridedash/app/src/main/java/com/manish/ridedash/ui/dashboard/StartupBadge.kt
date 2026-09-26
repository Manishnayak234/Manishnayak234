package com.manish.ridedash.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.manish.ridedash.data.RideMotion
import com.manish.ridedash.ui.theme.RideDashTheme
import com.manish.ridedash.ui.theme.numberStyle
import com.manish.ridedash.ui.theme.rideColors

/**
 * The marque, shown only while the gauge sweeps at key-on.
 *
 * A real cluster puts its badge up for the second or two the needle is sweeping and then gets out of
 * the way, which is exactly the deal here: it takes the right-hand panel for the length of the sweep
 * and is gone before the first number matters. Nothing is added to the riding screens, so the
 * brief's "only essential items while riding" survives intact.
 *
 * The lettering is not an imitation of anything — it is the dashboard's own type. SPEED and 400 are
 * Barlow Condensed ExtraBold Italic, the same face and weight as the speed number two panels to the
 * left, which is why it sits on the screen as though it were always meant to be there.
 */
@Composable
fun StartupBadge(
    modifier: Modifier = Modifier,
    /** 0 while the badge is up, 1 once the bike has flown to its place in the band. */
    morph: Float = 0f,
    bikeScale: Float = 1f,
    bikeOffset: Offset = Offset.Zero,
) {
    val colors = rideColors

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            BikeSilhouette(
                color = colors.fg,
                accent = colors.accent,
                width = BADGE_BIKE_WIDTH,
                modifier = Modifier
                    .padding(bottom = 6.dp)
                    // The bike is the one thing that survives the badge: it shrinks and slides into
                    // the band instead of fading out, so the big drawing and the little one read as
                    // the same object rather than two.
                    .graphicsLayer {
                        scaleX = bikeScale
                        scaleY = bikeScale
                        translationX = bikeOffset.x
                        translationY = bikeOffset.y
                    },
            )

            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.graphicsLayer { alpha = 1f - morph },
            ) {
                Text(
                    text = "SPEED",
                    style = numberStyle(72.sp, FontWeight.ExtraBold, italic = true),
                    color = colors.accent,
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "400",
                    style = numberStyle(72.sp, FontWeight.ExtraBold, italic = true),
                    color = colors.fg,
                )
            }

            Spacer(Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .width(280.dp)
                    .height(2.dp)
                    .graphicsLayer { alpha = 1f - morph },
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    drawRect(color = colors.accent, size = Size(size.width, size.height))
                }
            }
        }
    }
}

/** The badge's bike: parked, on a plain line. */
@Composable
fun BikeSilhouette(color: Color, accent: Color, width: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier.width(width).height(width * BIKE_ASPECT)) {
        drawRoad(color, phase = 0f, moving = false, y = size.height * 0.985f)
        drawBike(color, accent)
    }
}

/**
 * The riding scene: trees going by, road sliding under, smoke off the back — and a bike that never
 * actually moves.
 *
 * Everything is driven from one intensity in [RideMotion], so the trees, the road and the exhaust
 * can never disagree about how hard the bike is working. Trees run at roughly half the road's rate:
 * that parallax is what turns three sliding things into a sense of distance rather than a conveyor
 * belt.
 *
 * Phases are integrated by hand each frame rather than handed to `infiniteRepeatable`, because the
 * rate changes continuously as you accelerate — restarting the animation at every new speed would
 * snap the road back mid-stride. Trees and smoke are fixed rings at staggered phases, so a
 * continuous stream costs no allocation per frame.
 */
@Composable
fun RideScene(speedKmh: Float, modifier: Modifier = Modifier, bikeAlpha: Float = 1f) {
    val colors = rideColors
    val speed = rememberUpdatedState(speedKmh)
    var roadPhase by remember { mutableFloatStateOf(0f) }
    var treePhase by remember { mutableFloatStateOf(0f) }
    var smokePhase by remember { mutableFloatStateOf(0f) }
    var intensity by remember { mutableFloatStateOf(0f) }
    var moving by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        var lastMs = 0L
        while (true) {
            withFrameMillis { frameMs ->
                // Clamped: a dropped frame or a screen that was off must not teleport the road.
                val dt = if (lastMs == 0L) 0 else (frameMs - lastMs).coerceIn(0L, 64L)
                lastMs = frameMs
                val kmh = speed.value

                intensity = RideMotion.intensity(kmh)
                moving = RideMotion.moving(kmh)
                val roadHz = RideMotion.roadHz(kmh)
                roadPhase = (roadPhase + roadHz * dt / 1000f) % 1f
                treePhase = (treePhase + roadHz * TREE_PARALLAX * dt / 1000f) % 1f
                smokePhase = (smokePhase + RideMotion.smokeHz(kmh) * dt / 1000f) % 1f
            }
        }
    }

    Canvas(modifier) {
        val h = size.height
        val roadY = h * SCENE_ROAD_Y_FRACTION
        // The bike sits a third of the way in, facing right, so most of the scene is road ahead.
        val bikeH = h * SCENE_BIKE_HEIGHT_FRACTION
        val bikeW = bikeH / BIKE_ASPECT
        val bikeLeft = size.width * SCENE_BIKE_LEFT_FRACTION
        val bikeTop = roadY - bikeH * 0.97f

        drawTrees(colors.sub, treePhase, roadY)
        drawRoad(colors.fg, roadPhase, moving, roadY)
        // Accent yellow, the same as the brightness strip: dust catching the light, and the one
        // warm thing on an otherwise white-on-black band.
        drawSmoke(colors.accent, smokePhase, intensity, moving, bikeLeft, bikeTop, bikeW, bikeH)
        // Held back until the badge's bike has finished flying in, or there would be two.
        if (bikeAlpha > 0f) {
            drawBike(
                colors.fg.copy(alpha = bikeAlpha),
                colors.accent.copy(alpha = bikeAlpha),
                bikeLeft, bikeTop, bikeW, bikeH,
            )
        }
    }
}

/**
 * A treeline behind the bike. Deliberately faint and simple — at this height anything more detailed
 * turns to mud, and it must never compete with the numbers it sits above.
 */
private fun DrawScope.drawTrees(color: Color, phase: Float, roadY: Float) {
    val w = size.width
    val h = size.height
    val faint = color.copy(alpha = 0.26f)
    val spacing = w / TREES
    val offset = (phase * spacing) % spacing

    for (i in 0..TREES) {
        val x = w - (i * spacing - offset)
        if (x < -spacing || x > w + spacing) continue

        // Alternating heights, so a scrolling row does not read as a picket fence.
        val tall = i % 2 == 0
        val treeH = h * (if (tall) 0.78f else 0.58f)
        val trunkTop = roadY - treeH * 0.42f
        val canopyR = treeH * 0.30f
        val canopyY = roadY - treeH * 0.62f

        drawLine(
            color = faint,
            start = Offset(x, roadY),
            end = Offset(x, trunkTop),
            strokeWidth = treeH * 0.10f,
            cap = StrokeCap.Round,
        )

        // Three overlapping blobs rather than one circle. A single circle on a stick reads as a
        // lollipop; the cluster is what makes it a tree at this size.
        drawCircle(faint, canopyR, Offset(x, canopyY))
        drawCircle(faint, canopyR * 0.72f, Offset(x - canopyR * 0.72f, canopyY + canopyR * 0.34f))
        drawCircle(faint, canopyR * 0.72f, Offset(x + canopyR * 0.72f, canopyY + canopyR * 0.34f))
    }
}

/**
 * The road under the wheels: dashes sliding left, which is the bike going right.
 *
 * Parked, it is one unbroken line. Dashes frozen mid-stride look like a stalled animation, whereas a
 * solid line just looks like ground.
 */
private fun DrawScope.drawRoad(color: Color, phase: Float, moving: Boolean, y: Float) {
    val w = size.width
    val stroke = size.height * 0.035f
    val faint = color.copy(alpha = 0.40f)

    if (!moving) {
        drawLine(faint, Offset(0f, y), Offset(w, y), stroke, StrokeCap.Round)
        return
    }

    val dash = w * 0.07f
    val gap = w * 0.045f
    val period = dash + gap
    // Negative start: as phase grows the dashes march left, so the bike reads as going right.
    var x = -(phase * period) % period - period
    while (x < w) {
        val from = x.coerceAtLeast(0f)
        val to = (x + dash).coerceAtMost(w)
        if (to > from) drawLine(faint, Offset(from, y), Offset(to, y), stroke, StrokeCap.Round)
        x += period
    }
}

/**
 * Dust off the back wheel: puffs drifting back and up, growing and fading as they go.
 *
 * Emitted from behind and below the rear wheel rather than on top of it — the first version put
 * them over the wheel, where they read as a smudge instead of a trail.
 */
private fun DrawScope.drawSmoke(
    color: Color,
    phase: Float,
    intensity: Float,
    moving: Boolean,
    bikeLeft: Float,
    bikeTop: Float,
    bikeW: Float,
    bikeH: Float,
) {
    if (!moving) return

    val originX = bikeLeft + bikeW * 0.13f
    val originY = bikeTop + bikeH * 0.78f

    // How far the trail reaches is the point of it. Crawling, it clears the end of the road; flat
    // out it runs the width of the band and off the edge towards the dial. The canvas clips it
    // there, which is what sells the idea that it carries on past the panel.
    val reach = size.width * (MIN_TRAIL + intensity * (MAX_TRAIL - MIN_TRAIL))
    val rise = bikeH * 0.9f

    for (i in 0 until PUFFS) {
        // Staggered phases turn one counter into an evenly spaced stream.
        val p = (phase + i.toFloat() / PUFFS) % 1f
        val cx = originX - p * reach
        if (cx < -bikeH) continue
        val cy = originY - p * rise
        // Puffs swell as they fall behind, the way real dust does — but only so far. Growing them
        // to half the bike's height turned the trail into a row of yellow balls rolling along
        // behind it, which is a different thing entirely from smoke.
        val radius = bikeH * (0.06f + p * 0.17f)
        // Floored, because intensity is genuinely 0 the instant you move off and a trail that only
        // appeared once you were quick would look like it failed to start.
        // Bright enough to read as yellow against the trees, faint enough to be dust rather than
        // paint. Thinning towards the tail is what makes it disperse instead of just stopping.
        val alpha = (1f - p * 0.85f) * 0.5f * (0.55f + 0.45f * intensity)
        drawCircle(color.copy(alpha = alpha), radius, Offset(cx, cy))
    }
}

/** Wheels, frame, tank, engine and bars. No ground line: the road is drawn separately. */
private fun DrawScope.drawBike(
    color: Color,
    accent: Color,
    left: Float = 0f,
    top: Float = 0f,
    w: Float = size.width,
    h: Float = size.height,
) {
    fun x(f: Float) = left + w * f
    fun y(f: Float) = top + h * f
    val stroke = h * 0.035f
    val wheelR = h * 0.27f
    val rearC = Offset(x(0.20f), y(0.70f))
    val frontC = Offset(x(0.80f), y(0.70f))

    drawCircle(color, wheelR, rearC, style = Stroke(stroke))
    drawCircle(color, wheelR, frontC, style = Stroke(stroke))
    // Hubs, which is what stops two bare rings reading as spectacles.
    drawCircle(color, wheelR * 0.22f, rearC, style = Stroke(stroke * 0.8f))
    drawCircle(color, wheelR * 0.22f, frontC, style = Stroke(stroke * 0.8f))

    val body = Path().apply {
        moveTo(rearC.x, rearC.y)
        lineTo(x(0.36f), y(0.56f))
        lineTo(x(0.30f), y(0.40f))
        lineTo(x(0.47f), y(0.38f))
        lineTo(x(0.56f), y(0.30f))
        lineTo(x(0.66f), y(0.34f))
        lineTo(x(0.72f), y(0.46f))
        lineTo(frontC.x, frontC.y)
    }
    drawPath(body, color, style = Stroke(stroke, cap = StrokeCap.Round))

    // Engine: a squat block slung under the tank between the wheels. Drawn low and closed rather
    // than as the floating quadrilateral it started as, which read as a hole in the frame.
    val engine = Path().apply {
        moveTo(x(0.40f), y(0.62f))
        lineTo(x(0.43f), y(0.47f))
        lineTo(x(0.57f), y(0.48f))
        lineTo(x(0.58f), y(0.62f))
        close()
    }
    drawPath(engine, color, style = Stroke(stroke * 0.8f, cap = StrokeCap.Round))

    drawLine(
        color = color,
        start = Offset(x(0.68f), y(0.30f)),
        end = Offset(x(0.80f), y(0.24f)),
        strokeWidth = stroke,
        cap = StrokeCap.Round,
    )
    drawCircle(color, h * 0.075f, Offset(x(0.80f), y(0.33f)), style = Stroke(stroke * 0.9f))

    drawRider(color, accent, stroke, ::x, ::y, h)
}

/**
 * A rider tucked over the tank: hips on the seat, torso forward, one arm to the bars, one leg down
 * to the peg. Drawn in the same weight as the bike so the two read as one object.
 *
 * The accent is spent only on the visor and the shoulder. A rider picked out entirely in yellow
 * fights the speed arc, which is the one thing on this screen that has earned that colour.
 */
private fun DrawScope.drawRider(
    color: Color,
    accent: Color,
    stroke: Float,
    x: (Float) -> Float,
    y: (Float) -> Float,
    h: Float,
) {
    // A Speed 400 is a roadster, not a supersport. The rider sits up: back near vertical, head
    // high, arms reaching out and slightly down to a wide bar. The first version had him tucked
    // over the tank, which is the posture of an entirely different motorcycle.
    val hip = Offset(x(0.42f), y(0.35f))
    val shoulder = Offset(x(0.47f), y(0.11f))
    val helmet = Offset(x(0.485f), y(0.035f))
    val helmetR = h * 0.085f

    drawLine(color, hip, shoulder, stroke, StrokeCap.Round)
    // The long relaxed reach of an upright riding position.
    drawLine(color, shoulder, Offset(x(0.71f), y(0.255f)), stroke * 0.85f, StrokeCap.Round)
    // Knee forward, foot on a peg under the hip rather than swept back behind it.
    drawLine(color, hip, Offset(x(0.50f), y(0.50f)), stroke * 0.85f, StrokeCap.Round)
    drawLine(color, Offset(x(0.50f), y(0.50f)), Offset(x(0.47f), y(0.60f)), stroke * 0.85f, StrokeCap.Round)

    drawCircle(color, helmetR, helmet, style = Stroke(stroke * 0.9f))

    // Visor: a short bar across the front of the helmet, and a spark on the shoulder.
    drawLine(
        color = accent,
        start = Offset(helmet.x + helmetR * 0.20f, helmet.y - helmetR * 0.10f),
        end = Offset(helmet.x + helmetR * 1.05f, helmet.y + helmetR * 0.15f),
        strokeWidth = stroke * 1.1f,
        cap = StrokeCap.Round,
    )
    drawCircle(accent, stroke * 0.95f, shoulder)
}

/**
 * The road under the wheels: dashes sliding left, which is the bike going right.
 *
 * Parked, it is one unbroken line. Dashes frozen mid-stride look like a stalled animation, whereas a
 * solid line just looks like ground.
 */
private fun DrawScope.drawRoad(color: Color, phase: Float, moving: Boolean) {
    val w = size.width
    val y = size.height * 0.985f
    val stroke = size.height * 0.035f * 0.7f
    val faint = color.copy(alpha = 0.35f)

    if (!moving) {
        drawLine(faint, Offset(w * 0.04f, y), Offset(w * 0.96f, y), stroke, StrokeCap.Round)
        return
    }

    val dash = w * 0.14f
    val gap = w * 0.09f
    val period = dash + gap
    // Negative start: as phase grows the dashes march left, so the bike reads as going right.
    var x = -(phase * period) % period - period
    while (x < w) {
        val from = x.coerceAtLeast(0f)
        val to = (x + dash).coerceAtMost(w)
        if (to > from) drawLine(faint, Offset(from, y), Offset(to, y), stroke, StrokeCap.Round)
        x += period
    }
}

/** More, and smaller: a fine stream reads as dust where a few big circles read as balls. */
private const val PUFFS = 11

/** Crawling, the trail clears the road. Flat out, it runs off the edge towards the dial. */
private const val MIN_TRAIL = 0.45f
private const val MAX_TRAIL = 0.95f

/** Where the band puts its bike, as fractions of the band. The morph aims at exactly this. */
const val SCENE_BIKE_HEIGHT_FRACTION = 0.66f
/**
 * Well to the right of the band. The smoke trails left towards the dial, and a bike parked
 * against the left edge would have nowhere to trail to.
 */
const val SCENE_BIKE_LEFT_FRACTION = 0.70f
const val SCENE_ROAD_Y_FRACTION = 0.90f

/** How many trees span the band. */
private const val TREES = 7

/**
 * Trees run far slower than the road. That difference is the whole illusion of depth, and at
 * anything near the road's own rate they read as a fence being dragged past rather than as
 * distance.
 */
private const val TREE_PARALLAX = 0.22f

/** The drawing is laid out in a box this much taller than it is wide. */
const val BIKE_ASPECT = 0.46f

/** The badge bike's width. The morph target is derived from it, so the two cannot drift apart. */
val BADGE_BIKE_WIDTH = 260.dp

@Preview(widthDp = 441, heightDp = 257, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun StartupBadgePreview() {
    RideDashTheme {
        StartupBadge(modifier = Modifier.fillMaxSize())
    }
}

@Preview(widthDp = 420, heightDp = 48, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun RideScenePreview() {
    RideDashTheme {
        RideScene(speedKmh = 60f, modifier = Modifier.fillMaxSize())
    }
}
