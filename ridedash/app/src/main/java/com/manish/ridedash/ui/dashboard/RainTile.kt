package com.manish.ridedash.ui.dashboard

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.manish.ridedash.R
import com.manish.ridedash.data.weather.RainForecast
import com.manish.ridedash.data.weather.RainHour
import com.manish.ridedash.ui.theme.RideDashTheme
import com.manish.ridedash.ui.theme.labelStyle
import com.manish.ridedash.ui.theme.numberStyle
import com.manish.ridedash.ui.theme.rideColors
import com.manish.ridedash.util.Formatters

/**
 * Rain for the next few hours, as a row of bars.
 *
 * The question a rider is asking is not "what is the weather" but "am I about to get wet, and how
 * long have I got" — so the headline is the first hour that crosses [RainForecast.WET], and the bars
 * exist to show whether it is a brief shower or a wall coming in.
 *
 * Bars are solid blocks, not a gradient or a line chart: the sunlight rules in the brief rule out
 * anything that depends on reading a subtle shade at arm's length.
 */
@Composable
fun RainTile(
    rain: RainForecast?,
    nowMs: Long,
    modifier: Modifier = Modifier,
) {
    val colors = rideColors
    val stale = rain == null || rain.staleAt(nowMs)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(colors.tile)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.tile_rain),
                    style = labelStyle,
                    color = colors.sub,
                )
                Spacer(Modifier.weight(1f))
                if (rain?.temperatureC != null) {
                    Text(
                        text = Formatters.temperature(rain.temperatureC),
                        style = numberStyle(20.sp, FontWeight.Bold),
                        color = if (stale) colors.sub else colors.fg,
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            if (rain == null) {
                Text(
                    text = stringResource(R.string.rain_waiting),
                    style = numberStyle(34.sp, FontWeight.Bold),
                    color = colors.sub,
                )
                return@Column
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // The headline: when it turns wet, or that it does not.
                val wetHour = rain.firstWetHour
                Text(
                    text = if (wetHour != null) {
                        "${wetHour.chancePercent}%"
                    } else {
                        stringResource(R.string.rain_dry)
                    },
                    style = numberStyle(34.sp, FontWeight.Bold),
                    color = when {
                        stale -> colors.sub
                        wetHour != null -> colors.accent
                        else -> colors.fg
                    },
                    maxLines = 1,
                )
                if (wetHour != null) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = Formatters.hourLabel(wetHour.hourOfDay),
                        style = numberStyle(18.sp, FontWeight.SemiBold),
                        color = colors.sub,
                    )
                }

                Spacer(Modifier.weight(1f))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    rain.hours.forEach { hour -> RainBar(hour = hour, stale = stale) }
                }
            }
        }
    }
}

/** One hour: a block whose height is the chance, with the hour under it. */
@Composable
private fun RainBar(hour: RainHour, stale: Boolean) {
    val colors = rideColors
    val wet = hour.chancePercent >= RainForecast.WET

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .width(BAR_WIDTH)
                .height(BAR_MAX_HEIGHT)
                .clip(RoundedCornerShape(3.dp))
                .background(colors.track),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    // A trace of height even at 0%, so the bar reads as "measured and dry" rather
                    // than as a missing value.
                    .fillMaxHeight((hour.chancePercent / 100f).coerceAtLeast(0.04f))
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        when {
                            stale -> colors.sub
                            wet -> colors.accent
                            else -> colors.fg
                        }
                    ),
            )
        }
        Spacer(Modifier.height(3.dp))
        Text(
            text = Formatters.hourShort(hour.hourOfDay),
            style = numberStyle(13.sp, FontWeight.SemiBold),
            color = colors.sub,
            maxLines = 1,
        )
    }
}

private val BAR_WIDTH = 16.dp
private val BAR_MAX_HEIGHT = 34.dp

@Preview(widthDp = 470, heightDp = 110, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun RainTileWetPreview() {
    RideDashTheme {
        RainTile(
            rain = RainForecast(
                temperatureC = 27.4f,
                hours = listOf(
                    RainHour(16, 10),
                    RainHour(17, 25),
                    RainHour(18, 65),
                    RainHour(19, 80),
                    RainHour(20, 45),
                ),
                fetchedAtMs = 1_000L,
            ),
            nowMs = 1_000L,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Preview(widthDp = 470, heightDp = 110, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun RainTileDryPreview() {
    RideDashTheme {
        RainTile(
            rain = RainForecast(
                temperatureC = 31f,
                hours = listOf(
                    RainHour(12, 0),
                    RainHour(13, 5),
                    RainHour(14, 10),
                    RainHour(15, 0),
                    RainHour(16, 15),
                ),
                fetchedAtMs = 1_000L,
            ),
            nowMs = 1_000L,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
