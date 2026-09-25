package com.manish.ridedash.ui.dashboard

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.manish.ridedash.R
import com.manish.ridedash.data.RideState
import com.manish.ridedash.ui.theme.RideDashTheme
import com.manish.ridedash.ui.theme.labelStyle
import com.manish.ridedash.ui.theme.numberStyle
import com.manish.ridedash.ui.theme.rideColors
import com.manish.ridedash.util.Formatters

/**
 * The right panel with no route running (screen 4.2): heading, lean, altitude and the speed records,
 * as four tiles. A long press on the lean tile zeroes the lean angle with the bike upright.
 */
@Composable
fun CruisingPanel(
    state: RideState,
    onCalibrateLean: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            HeadingTile(state, Modifier.weight(1f))
            LeanTile(state, onCalibrateLean, Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AltitudeTile(state, Modifier.weight(1f))
            SpeedRecordTile(state, Modifier.weight(1f))
        }
    }
}

@Composable
private fun HeadingTile(state: RideState, modifier: Modifier = Modifier) {
    val colors = rideColors
    Tile(label = stringResource(R.string.tile_heading), modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CompassNeedle(
                headingDeg = state.headingDeg,
                color = colors.fg,
                ringColor = colors.track,
                size = 60.dp,
            )
            Spacer(Modifier.width(14.dp))
            TileValue(
                text = Formatters.heading(state.headingDeg),
                suffix = if (state.headingDeg != null) "°" else null,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LeanTile(state: RideState, onCalibrate: () -> Unit, modifier: Modifier = Modifier) {
    val colors = rideColors
    Tile(
        label = stringResource(R.string.tile_lean),
        modifier = modifier.combinedClickable(
            onClick = {},
            onLongClick = onCalibrate,
        ),
        footer = Formatters.leanMax(state.leanMaxLeftDeg, state.leanMaxRightDeg),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TileValue(
                text = Formatters.lean(state.leanDeg),
                suffix = if (state.leanDeg != null) "°" else null,
            )
            Spacer(Modifier.width(12.dp))
            LeanIndicator(
                leanDeg = state.leanDeg,
                color = colors.accent,
                trackColor = colors.track,
                size = 68.dp,
            )
        }
    }
}

@Composable
private fun AltitudeTile(state: RideState, modifier: Modifier = Modifier) {
    Tile(label = stringResource(R.string.tile_altitude), modifier = modifier) {
        TileValue(text = Formatters.altitude(state.altitudeM))
    }
}

@Composable
private fun SpeedRecordTile(state: RideState, modifier: Modifier = Modifier) {
    Tile(
        label = stringResource(R.string.tile_speed),
        modifier = modifier,
        footer = stringResource(R.string.unit_kmh),
    ) {
        TileValue(text = Formatters.maxAvg(state.maxSpeedKmh, state.avgSpeedKmh))
    }
}

@Composable
private fun Tile(
    label: String,
    modifier: Modifier = Modifier,
    footer: String? = null,
    content: @Composable () -> Unit,
) {
    val colors = rideColors
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.tile)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(text = label, style = labelStyle, color = colors.sub)
            Spacer(Modifier.weight(1f))
            content()
            if (footer != null) {
                Spacer(Modifier.weight(1f))
                Text(
                    text = footer,
                    style = numberStyle(17.sp, FontWeight.SemiBold),
                    color = colors.sub,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** 52 sp condensed, with the degree sign kept small so the number keeps the room. */
@Composable
private fun TileValue(text: String, suffix: String? = null) {
    val colors = rideColors
    Row(verticalAlignment = Alignment.Top) {
        Text(
            text = text,
            style = numberStyle(52.sp, FontWeight.Bold),
            color = colors.fg,
            maxLines = 1,
        )
        if (suffix != null) {
            Text(
                text = suffix,
                style = numberStyle(26.sp, FontWeight.Bold),
                color = colors.sub,
            )
        }
    }
}

@Preview(widthDp = 554, heightDp = 308, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun CruisingPanelPreview() {
    RideDashTheme {
        CruisingPanel(
            state = RideState(
                headingDeg = 30f,
                leanDeg = -23f,
                leanMaxLeftDeg = 31f,
                leanMaxRightDeg = 28f,
                altitudeM = 560f,
                maxSpeedKmh = 96f,
                avgSpeedKmh = 41f,
            ),
            onCalibrateLean = {},
        )
    }
}
