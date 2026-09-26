package com.manish.ridedash.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.manish.ridedash.data.NavState
import com.manish.ridedash.data.RideState
import com.manish.ridedash.ui.theme.RideDashTheme
import com.manish.ridedash.ui.theme.rideColors
import com.manish.ridedash.util.Formatters

/**
 * The dashboard itself: status strip, the speed gauge on the left, and a right panel that follows
 * whether Google Maps is navigating (screen 4.1) or not (screen 4.2).
 *
 * The switch is driven purely by [RideState.nav], which is set from the Maps notification, so the
 * screen changes by itself when a route starts or ends.
 */
@Composable
fun DashboardScreen(
    state: RideState,
    /** Non-null while the power-on sweep is running, and it drives the gauge instead of the GPS. */
    sweepKmh: Float? = null,
    /** Ticks slowly, only so the rain tile can tell a fresh forecast from a stale one. */
    nowMs: Long = 0L,
    onMap: () -> Unit,
    onRideStats: () -> Unit,
    onExit: () -> Unit,
    onCalibrateLean: () -> Unit,
    brightness: Float,
    onBrightnessChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = rideColors
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        // Split screen hands us about half the width. The gauge alone claims 360 dp of the 801 the
        // full layout needs, so below this there is no point shrinking it — the compact dashboard
        // drops everything Maps is already showing in the other pane.
        if (maxWidth < COMPACT_WIDTH) {
            CompactDashboard(
                state = state,
                sweepKmh = sweepKmh,
                nowMs = nowMs,
                onExit = onExit,
                brightness = brightness,
                onBrightnessChange = onBrightnessChange,
            )
            return@BoxWithConstraints
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.bg),
        ) {
            StatusBar(state, nowMs)

            Row(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .width(LEFT_PANEL_WIDTH)
                        .fillMaxHeight()
                        .drawBehind {
                            drawLine(
                                color = colors.line,
                                start = Offset(size.width, 0f),
                                end = Offset(size.width, size.height),
                                strokeWidth = 1f,
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    SpeedGauge(
                        speedKmh = sweepKmh ?: state.speedKmh,
                        speedValid = sweepKmh != null || (state.speedValid && state.gpsFix),
                        // Never during the power-on sweep: it runs to 180 by design.
                        overspeed = sweepKmh == null && state.overspeed,
                        tripLine = "${Formatters.tripKm(state.tripKm)} · ${Formatters.rideTime(state.rideTimeMs)}",
                    )
                }

                val nav = state.nav
                if (nav != null) {
                    NavigatingPanel(
                        nav = nav,
                        headingDeg = state.headingDeg,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    CruisingPanel(
                        state = state,
                        onCalibrateLean = onCalibrateLean,
                        nowMs = nowMs,
                        modifier = Modifier.weight(1f),
                    )
                }

                // Outside the if/else deliberately: brightness has to stay reachable whether or not a
                // route is running, and it used to vanish the moment the Navigating panel took over.
                BrightnessSlider(
                    value = brightness,
                    onValueChange = onBrightnessChange,
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(end = 12.dp, top = 12.dp, bottom = 12.dp),
                )
            }

            BottomBar(onMap = onMap, onRideStats = onRideStats, onExit = onExit)
        }
    }
}

private val LEFT_PANEL_WIDTH = 360.dp

/** Under this the full layout cannot be drawn honestly, so the compact one takes over. */
private val COMPACT_WIDTH = 560.dp

@Preview(name = "Navigating", widthDp = 914, heightDp = 412, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun DashboardNavigatingPreview() {
    RideDashTheme {
        DashboardScreen(
            state = RideState(
                speedKmh = 72f,
                speedValid = true,
                gpsFix = true,
                satellites = 11,
                tripKm = 12.4f,
                rideTimeMs = 38 * 60_000L,
                batteryPct = 84,
                bluetoothOn = true,
                watchAlertsArmed = true,
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
            ),
            onMap = {},
            onRideStats = {},
            onExit = {},
            onCalibrateLean = {},
            brightness = 0.6f,
            onBrightnessChange = {},
        )
    }
}

@Preview(name = "Cruising", widthDp = 914, heightDp = 412, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun DashboardCruisingPreview() {
    RideDashTheme {
        DashboardScreen(
            state = RideState(
                speedKmh = 48f,
                speedValid = true,
                gpsFix = true,
                satellites = 9,
                tripKm = 12.4f,
                rideTimeMs = 38 * 60_000L,
                headingDeg = 30f,
                leanDeg = -23f,
                leanMaxLeftDeg = 31f,
                leanMaxRightDeg = 28f,
                altitudeM = 560f,
                maxSpeedKmh = 96f,
                avgSpeedKmh = 41f,
                batteryPct = 84,
                bluetoothOn = true,
                watchAlertsArmed = true,
            ),
            onMap = {},
            onRideStats = {},
            onExit = {},
            onCalibrateLean = {},
            brightness = 0.6f,
            onBrightnessChange = {},
        )
    }
}
