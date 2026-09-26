package com.manish.ridedash.ui.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.manish.ridedash.R
import com.manish.ridedash.data.NavState
import com.manish.ridedash.data.RideState
import com.manish.ridedash.ui.theme.RideDashTheme
import com.manish.ridedash.ui.theme.Warn
import com.manish.ridedash.ui.theme.numberStyle
import com.manish.ridedash.ui.theme.rideColors
import com.manish.ridedash.util.Formatters

/**
 * The half-width dashboard, for when Google Maps has the other half of the screen in split view.
 *
 * Split-screen leaves about 436 dp of width, where the full layout needs 801, so the arc gauge and
 * the tiles go. What is left is what you cannot get from the Maps half: the speed, the turn spelled
 * out in words, and the brightness strip. Maps draws its own map, route and arrow next door.
 *
 * There is no Map button here — the map is already on screen — and no screen pinning, because
 * Android does not allow lock task in multi-window.
 */
@Composable
fun CompactDashboard(
    state: RideState,
    sweepKmh: Float? = null,
    nowMs: Long = 0L,
    onExit: () -> Unit,
    brightness: Float,
    onBrightnessChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = rideColors
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        StatusBar(state, nowMs)

        Row(modifier = Modifier.weight(1f)) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                val nav = state.nav
                if (nav != null) {
                    TurnBanner(nav)
                    Spacer(Modifier.height(10.dp))
                }

                Row(verticalAlignment = Alignment.Bottom) {
                    val overspeed = sweepKmh == null && state.overspeed
                    Text(
                        text = Formatters.speed(
                            sweepKmh ?: state.speedKmh,
                            sweepKmh != null || (state.speedValid && state.gpsFix),
                        ),
                        style = numberStyle(96.sp, FontWeight.ExtraBold, italic = true),
                        color = if (overspeed) Warn else colors.fg,
                        maxLines = 1,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "km/h",
                        style = numberStyle(18.sp, FontWeight.SemiBold),
                        color = colors.sub,
                        modifier = Modifier.padding(bottom = 16.dp),
                    )
                }

                val overspeedLine = sweepKmh == null && state.overspeed
                Text(
                    text = if (overspeedLine) {
                        stringResource(R.string.overspeed_warning)
                    } else {
                        "${Formatters.tripKm(state.tripKm)} · ${Formatters.rideTime(state.rideTimeMs)}"
                    },
                    style = numberStyle(if (overspeedLine) 18.sp else 16.sp, FontWeight.Bold),
                    color = if (overspeedLine) Warn else colors.sub,
                    maxLines = 1,
                )
            }

            // Narrower than the full layout's strip, but still a 44 dp target: sun and shade do not
            // stop changing just because Maps has the other half of the screen.
            BrightnessSlider(
                value = brightness,
                onValueChange = onBrightnessChange,
                width = 44.dp,
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(end = 8.dp, top = 8.dp, bottom = 8.dp),
            )
        }

        ExitOnlyBar(onExit = onExit)
    }
}

/** The turn, in one line: arrow, how far, and what to do when you get there. */
@Composable
private fun TurnBanner(nav: NavState) {
    val colors = rideColors
    val words = listOfNotNull(
        nav.instruction?.takeIf { it.isNotEmpty() },
        nav.street.takeIf { it.isNotEmpty() },
    ).joinToString(" · ")

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val bitmap = nav.icon
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(colors.nav),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(52.dp),
                )
            } else {
                TurnArrowFallback(color = colors.nav, size = 52.dp)
            }

            if (nav.distanceValue.isNotEmpty()) {
                Spacer(Modifier.width(10.dp))
                Text(
                    text = nav.distanceValue,
                    style = numberStyle(52.sp, FontWeight.Bold),
                    color = colors.fg,
                )
                Text(
                    text = nav.distanceUnit,
                    style = numberStyle(20.sp, FontWeight.SemiBold),
                    color = colors.sub,
                )
            }
        }

        if (words.isNotEmpty()) {
            Text(
                text = words,
                style = numberStyle(22.sp, FontWeight.Bold),
                color = colors.fg,
                maxLines = 2,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Only Hold to exit survives down here. Ride stats and the map are both a pane away, and a button
 * you cannot hit with a glove is worse than no button.
 */
@Composable
private fun ExitOnlyBar(onExit: () -> Unit) {
    val colors = rideColors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        HoldToExitButton(
            onExit = onExit,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Preview(widthDp = 436, heightDp = 361, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun CompactCruisingPreview() {
    RideDashTheme {
        CompactDashboard(
            state = RideState(
                speedKmh = 48f,
                speedValid = true,
                gpsFix = true,
                satellites = 11,
                tripKm = 12.4f,
                rideTimeMs = 38 * 60_000L,
                batteryPct = 84,
            ),
            onExit = {},
            brightness = 0.6f,
            onBrightnessChange = {},
        )
    }
}

@Preview(widthDp = 436, heightDp = 361, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun CompactNavigatingPreview() {
    RideDashTheme {
        CompactDashboard(
            state = RideState(
                speedKmh = 62f,
                speedValid = true,
                gpsFix = true,
                satellites = 14,
                tripKm = 12.4f,
                rideTimeMs = 38 * 60_000L,
                batteryPct = 84,
                nav = NavState(
                    distanceValue = "200",
                    distanceUnit = "m",
                    distanceMeters = 200,
                    instruction = "Turn left",
                    street = "MG Road",
                ),
            ),
            onExit = {},
            brightness = 0.6f,
            onBrightnessChange = {},
        )
    }
}
