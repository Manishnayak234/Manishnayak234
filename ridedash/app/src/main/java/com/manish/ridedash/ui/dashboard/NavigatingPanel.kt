package com.manish.ridedash.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import com.manish.ridedash.R
import com.manish.ridedash.data.NavState
import com.manish.ridedash.ui.theme.RideDashTheme
import com.manish.ridedash.ui.theme.labelStyle
import com.manish.ridedash.ui.theme.numberStyle
import com.manish.ridedash.ui.theme.rideColors
import com.manish.ridedash.ui.theme.streetStyle
import com.manish.ridedash.util.Formatters

/**
 * The right panel while a route is running (screen 4.1): the maneuver, how far to it, the street, how
 * far through the maneuver we are, what comes after it, and the trip summary along the bottom.
 *
 * The sizes are smaller than the mockup's. The mockup assumed a 412 dp tall screen; the phone this
 * runs on reports 361 dp once the system bars are out, which leaves about 257 dp here — and at the
 * drawn sizes the ETA row fell off the bottom of the screen entirely.
 */
@Composable
fun NavigatingPanel(
    nav: NavState,
    /** Shown alongside the route numbers: the turn tells you what is next, this tells you which way
     *  you are pointing right now. */
    headingDeg: Float?,
    modifier: Modifier = Modifier,
) {
    val colors = rideColors
    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ManeuverIcon(nav)
            Spacer(Modifier.width(16.dp))
            Column {
                Row(verticalAlignment = Alignment.Bottom) {
                    if (nav.distanceValue.isNotEmpty()) {
                        Text(
                            text = nav.distanceValue,
                            style = numberStyle(64.sp, FontWeight.Bold),
                            color = colors.fg,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = nav.distanceUnit,
                            style = numberStyle(24.sp, FontWeight.SemiBold),
                            color = colors.sub,
                            modifier = Modifier.padding(bottom = 12.dp),
                        )
                    } else {
                        // Maps sometimes gives words instead of a distance ("Head north on ...").
                        Text(
                            text = nav.instruction.orEmpty().ifEmpty { Formatters.PLACEHOLDER },
                            style = numberStyle(40.sp, FontWeight.Bold),
                            color = colors.fg,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                // "Turn left · MG Road": the command in words, then where it lands. The arrow says
                // the same thing at a glance, but the words are what you check it against. When there
                // was no distance the instruction is already the headline above, so it is not
                // repeated here.
                val secondLine = listOfNotNull(
                    nav.instruction?.takeIf { it.isNotEmpty() && nav.distanceValue.isNotEmpty() },
                    nav.street.takeIf { it.isNotEmpty() },
                ).joinToString(" · ")

                if (secondLine.isNotEmpty()) {
                    Text(
                        text = secondLine,
                        style = streetStyle.copy(fontSize = 22.sp),
                        color = colors.fg,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        ManeuverProgress(progress = nav.progress)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.nav_then),
                style = labelStyle,
                color = colors.sub,
            )
            ThenArrow(color = colors.nav, size = 22.dp)
            Text(
                text = nav.thenStreet ?: Formatters.PLACEHOLDER,
                style = numberStyle(20.sp, FontWeight.SemiBold),
                color = colors.fg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.weight(1f))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawLine(
                        color = colors.line,
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = 1f,
                    )
                }
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            SummaryCell(stringResource(R.string.nav_eta), nav.etaClock)
            SummaryCell(stringResource(R.string.nav_left), nav.remainingDistance)
            SummaryCell(stringResource(R.string.nav_time), nav.remainingTime)
            SummaryCell(stringResource(R.string.tile_heading), Formatters.heading(headingDeg))
        }
    }
}

@Composable
private fun ManeuverIcon(nav: NavState) {
    val colors = rideColors
    val bitmap = nav.icon
    if (bitmap != null) {
        // The notification's large icon IS the maneuver arrow, so it is tinted rather than mapped to
        // one of our own drawables. That way a Maps update with new arrows just works.
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            colorFilter = ColorFilter.tint(colors.nav),
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(84.dp),
        )
    } else {
        TurnArrowFallback(color = colors.nav, size = 84.dp)
    }
}

@Composable
private fun ManeuverProgress(progress: Float, modifier: Modifier = Modifier) {
    val colors = rideColors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(colors.track)
            .drawBehind {
                drawRect(
                    color = colors.nav,
                    size = androidx.compose.ui.geometry.Size(
                        width = size.width * progress.coerceIn(0f, 1f),
                        height = size.height,
                    ),
                )
            },
    )
}

@Composable
private fun SummaryCell(label: String, value: String?) {
    val colors = rideColors
    Column {
        Text(text = label, style = labelStyle, color = colors.sub)
        Text(
            text = value ?: Formatters.PLACEHOLDER,
            style = numberStyle(24.sp, FontWeight.Bold),
            color = colors.fg,
        )
    }
}

@Preview(widthDp = 554, heightDp = 308, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun NavigatingPanelPreview() {
    RideDashTheme {
        NavigatingPanel(
            nav = NavState(
                distanceValue = "350",
                distanceUnit = "m",
                distanceMeters = 350,
                instruction = "Turn right",
                street = "MG Road",
                thenStreet = "Airport Road",
                etaClock = "18:42",
                remainingDistance = "12 km",
                remainingTime = "24 min",
                progress = 0.55f,
            ),
            headingDeg = 30f,
        )
    }
}
