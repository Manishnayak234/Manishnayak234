package com.manish.ridedash.ui.dashboard

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.manish.ridedash.data.RideState
import com.manish.ridedash.data.weather.RainForecast
import com.manish.ridedash.ui.theme.GpsStale
import com.manish.ridedash.ui.theme.Warn
import com.manish.ridedash.ui.theme.labelStyle
import com.manish.ridedash.ui.theme.numberStyle
import com.manish.ridedash.ui.theme.rideColors
import com.manish.ridedash.util.Formatters
import kotlinx.coroutines.delay
import java.util.Calendar

/**
 * The 40 dp strip along the top: fix quality on the left, the clock in the middle, and the
 * watch/Bluetooth/battery group on the right. Nothing here needs a glance longer than a moment.
 */
@Composable
fun StatusBar(state: RideState, nowMs: Long = 0L, modifier: Modifier = Modifier) {
    val colors = rideColors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .drawBehind {
                drawLine(
                    color = colors.line,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1f,
                )
            }
            .padding(horizontal = 14.dp),
    ) {
        GpsStatus(state, Modifier.align(Alignment.CenterStart))

        Text(
            text = clockText(),
            style = numberStyle(26.sp, FontWeight.Bold),
            color = colors.fg,
            modifier = Modifier.align(Alignment.Center),
        )

        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Rain lives here as well as on the cruising tile, because the tile is invisible the
            // moment a route starts — which is exactly when you are furthest from shelter.
            val rain = state.rain
            if (rain != null && !rain.staleAt(nowMs)) {
                // Shown even at 4%, deliberately. Hiding a low number saves a little clutter and
                // costs the one thing that matters more: being able to tell "no rain coming" apart
                // from "the forecast is broken". Accent is what marks it worth acting on.
                val soon = rain.peakWithin(RainForecast.SOON_HOURS)
                val wet = soon >= RainForecast.WET
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    RainDropIcon(color = if (wet) colors.accent else colors.sub, size = 16.dp)
                    Text(
                        text = "$soon%",
                        style = numberStyle(20.sp, FontWeight.Bold),
                        color = if (wet) colors.accent else colors.sub,
                    )
                }
            }

            if (state.hot) {
                Text(
                    text = "${Formatters.batteryTemp(state.batteryTempC)} ▲",
                    style = labelStyle,
                    color = Warn,
                )
            }
            WatchIcon(color = if (state.watchAlertsArmed) colors.fg else GpsStale)
            BluetoothIcon(color = if (state.bluetoothOn) colors.nav else GpsStale)
            BatteryIcon(
                color = colors.sub,
                fillColor = batteryFill(state, colors.fg, colors.accent),
                percent = state.batteryPct.coerceAtLeast(0),
            )
            Text(
                text = if (state.batteryPct < 0) Formatters.PLACEHOLDER else "${state.batteryPct}%",
                style = numberStyle(20.sp, FontWeight.Bold),
                color = colors.fg,
            )
        }
    }
}

@Composable
private fun GpsStatus(state: RideState, modifier: Modifier = Modifier) {
    val colors = rideColors
    val fixColor = when {
        state.gpsFix && state.speedValid -> colors.ok
        state.gpsFix -> colors.accent
        else -> GpsStale
    }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        GpsIcon(color = fixColor)
        Text(
            text = "GPS · " + if (state.satellites >= 0) "${state.satellites}" else Formatters.PLACEHOLDER,
            style = numberStyle(20.sp, FontWeight.Bold),
            color = fixColor,
        )
    }
}

private fun batteryFill(state: RideState, normal: Color, charging: Color): Color = when {
    state.charging -> charging
    state.batteryPct in 0..15 -> Warn
    else -> normal
}

/** Ticks every 10 s, which is plenty for a clock showing minutes. */
@Composable
private fun clockText(): String {
    val context = LocalContext.current
    val use24h = DateFormat.is24HourFormat(context)
    var text by remember(use24h) { mutableStateOf(currentClock(use24h)) }
    LaunchedEffect(use24h) {
        while (true) {
            text = currentClock(use24h)
            delay(10_000L)
        }
    }
    return text
}

private fun currentClock(use24h: Boolean): String {
    val now = Calendar.getInstance()
    return Formatters.clock(now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), use24h)
}
