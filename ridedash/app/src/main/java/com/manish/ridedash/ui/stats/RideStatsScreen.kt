package com.manish.ridedash.ui.stats

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.manish.ridedash.R
import com.manish.ridedash.data.RideState
import com.manish.ridedash.data.settings.RideSettings
import com.manish.ridedash.ui.dashboard.DashButton
import com.manish.ridedash.ui.theme.RideDashTheme
import com.manish.ridedash.ui.theme.labelStyle
import com.manish.ridedash.ui.theme.numberStyle
import com.manish.ridedash.ui.theme.rideColors
import com.manish.ridedash.util.Formatters

/**
 * Ride stats, as far as v1 goes: the ride in progress next to the last one that was saved. The full
 * history is v2 work, once trips are stored properly.
 */
@Composable
fun RideStatsScreen(
    state: RideState,
    lastRide: RideSettings.LastRide,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = rideColors
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
            .padding(horizontal = 28.dp, vertical = 18.dp),
    ) {
        Text(
            text = stringResource(R.string.stats_title),
            style = numberStyle(34.sp, FontWeight.Bold),
            color = colors.fg,
        )
        Text(
            text = stringResource(R.string.stats_stub),
            style = numberStyle(18.sp, FontWeight.SemiBold),
            color = colors.sub,
        )

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StatsCard(
                title = "THIS RIDE",
                rows = listOf(
                    "DISTANCE" to Formatters.tripKm(state.tripKm),
                    "MOVING TIME" to Formatters.rideTime(state.rideTimeMs),
                    "MAX / AVG" to "${Formatters.maxAvg(state.maxSpeedKmh, state.avgSpeedKmh)} km/h",
                    "MAX LEAN" to Formatters.leanMax(state.leanMaxLeftDeg, state.leanMaxRightDeg),
                ),
                modifier = Modifier.weight(1f),
            )
            StatsCard(
                title = "LAST RIDE",
                rows = listOf(
                    "DISTANCE" to Formatters.tripKm(lastRide.tripKm),
                    "MOVING TIME" to Formatters.rideTime(lastRide.rideTimeMs),
                    "MAX / AVG" to "${Formatters.maxAvg(lastRide.maxSpeedKmh, lastRide.avgSpeedKmh)} km/h",
                    "MAX LEAN" to Formatters.leanMax(lastRide.leanMaxLeftDeg, lastRide.leanMaxRightDeg),
                ),
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(12.dp))

        DashButton(
            label = stringResource(R.string.stats_back),
            onClick = onBack,
            modifier = Modifier.width(220.dp),
        )
    }
}

@Composable
private fun StatsCard(title: String, rows: List<Pair<String, String>>, modifier: Modifier = Modifier) {
    val colors = rideColors
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.tile)
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(text = title, style = labelStyle, color = colors.sub)
            rows.forEach { (label, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = label,
                        style = numberStyle(20.sp, FontWeight.SemiBold),
                        color = colors.sub,
                    )
                    Text(
                        text = value,
                        style = numberStyle(28.sp, FontWeight.Bold),
                        color = colors.fg,
                    )
                }
            }
        }
    }
}

@Preview(widthDp = 914, heightDp = 412, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun RideStatsPreview() {
    RideDashTheme {
        RideStatsScreen(
            state = RideState(
                tripKm = 12.4f,
                rideTimeMs = 38 * 60_000L,
                maxSpeedKmh = 96f,
                avgSpeedKmh = 41f,
                leanMaxLeftDeg = 31f,
                leanMaxRightDeg = 28f,
            ),
            lastRide = RideSettings.LastRide(
                tripKm = 54.2f,
                rideTimeMs = 96 * 60_000L,
                maxSpeedKmh = 108f,
                avgSpeedKmh = 46f,
                leanMaxLeftDeg = 34f,
                leanMaxRightDeg = 30f,
            ),
            onBack = {},
        )
    }
}
