package com.manish.ridedash.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.manish.ridedash.data.NavState
import com.manish.ridedash.data.RideState
import com.manish.ridedash.data.weather.WeatherState
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
    onMap: () -> Unit,
    onRideStats: () -> Unit,
    onRecord: () -> Unit,
    onExit: () -> Unit,
    onCalibrateLean: () -> Unit,
    modifier: Modifier = Modifier,
    /** Bumped on each entry into dashboard mode, which is what makes the sweep run per key-on. */
    sweepToken: Int = 0,
) {
    val colors = rideColors
    val sweep = rememberClusterSweep(sweepToken)
    val sweptSpeed = sweep.value

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
            // A tap cuts the key-on sweep short; once it is over this does nothing at all.
            .pointerInput(sweepToken) {
                detectTapGestures(onTap = { sweep.skip() })
            },
    ) {
        StatusBar(state)

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
                    speedKmh = sweptSpeed ?: state.speedKmh,
                    speedValid = sweptSpeed != null || (state.speedValid && state.gpsFix),
                    tripLine = "${Formatters.tripKm(state.tripKm)} · ${Formatters.rideTime(state.rideTimeMs)}",
                )
            }

            val nav = state.nav
            if (nav != null) {
                NavigatingPanel(nav = nav, modifier = Modifier.weight(1f))
            } else {
                CruisingPanel(
                    state = state,
                    onCalibrateLean = onCalibrateLean,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        BottomBar(
            onMap = onMap,
            onRideStats = onRideStats,
            onRecord = onRecord,
            onExit = onExit,
        )
    }
}

private val LEFT_PANEL_WIDTH = 360.dp

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
                weather = WeatherState(
                    temperatureC = 27.4f,
                    feelsLikeC = 30.1f,
                    conditionCode = 2,
                    condition = "Part cloud",
                    precipitationMm = 0f,
                    rainChancePercent = 40,
                    windKmh = 12f,
                    windDirectionDeg = 45f,
                    fetchedAtMs = 0L,
                ),
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
            onRecord = {},
            onExit = {},
            onCalibrateLean = {},
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
                weather = WeatherState(
                    temperatureC = 27.4f,
                    feelsLikeC = 30.1f,
                    conditionCode = 2,
                    condition = "Part cloud",
                    precipitationMm = 0f,
                    rainChancePercent = 40,
                    windKmh = 12f,
                    windDirectionDeg = 45f,
                    fetchedAtMs = 0L,
                ),
            ),
            onMap = {},
            onRideStats = {},
            onRecord = {},
            onExit = {},
            onCalibrateLean = {},
        )
    }
}
