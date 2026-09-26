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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.manish.ridedash.R
import com.manish.ridedash.data.LeanSide
import com.manish.ridedash.data.RideState
import com.manish.ridedash.ui.theme.RideDashTheme
import com.manish.ridedash.ui.theme.labelStyle
import com.manish.ridedash.ui.theme.numberStyle
import com.manish.ridedash.ui.theme.rideColors
import com.manish.ridedash.util.Formatters

/**
 * The right panel with no route running (screen 4.2): heading, lean and the rain ahead. A long
 * press on the lean
 * tile zeroes the lean angle with the bike upright. The brightness strip beside it belongs to
 * [DashboardScreen], so it stays put when a route starts and this panel is swapped out.
 *
 * Altitude, the max/avg records and the lean maxima used to sit here too. They are all still
 * tracked and still on the ride stats screen — they are just not worth a glance from the saddle,
 * and the room they were taking now runs the scene along the top.
 */
@Composable
fun CruisingPanel(
    state: RideState,
    onCalibrateLean: () -> Unit,
    nowMs: Long,
    modifier: Modifier = Modifier,
    sceneBikeAlpha: Float = 1f,
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // The scene runs across the top. Everything below it lost a few dp to pay for it: the tiles
        // had room to give, especially HEADING, which was mostly empty space.
        RideScene(
            speedKmh = state.speedKmh,
            bikeAlpha = sceneBikeAlpha,
            modifier = Modifier
                .fillMaxWidth()
                .height(SCENE_HEIGHT),
        )
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            HeadingTile(state, Modifier.weight(1f))
            LeanTile(state, onCalibrateLean, Modifier.weight(1f))
        }
        // Wide and short: the rain bars want horizontal room, not height.
        RainTile(
            rain = state.rain,
            nowMs = nowMs,
            modifier = Modifier
                .fillMaxWidth()
                .height(RAIN_TILE_HEIGHT),
        )
    }
}

/** The riding scene's band. Everything else on this panel was trimmed to afford it. */
internal val SCENE_HEIGHT = 50.dp

/** Enough for the label, the headline and a row of bars, and no more. */
private val RAIN_TILE_HEIGHT = 76.dp

@Composable
private fun HeadingTile(state: RideState, modifier: Modifier = Modifier) {
    val colors = rideColors
    Tile(label = stringResource(R.string.tile_heading), modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CompassNeedle(
                headingDeg = state.headingDeg,
                color = colors.fg,
                ringColor = colors.track,
                size = 40.dp,
            )
            Spacer(Modifier.width(10.dp))
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
    // The cheer takes over the tile's label rather than being added below it. The tiles were
    // squeezed to 87 dp to afford the riding band, and there is no room for another line — but the
    // word LEAN is the least useful thing on a tile that already says "R 12°".
    val cheer = when (state.leanCheer) {
        LeanSide.LEFT -> stringResource(R.string.cheer_lean_left)
        LeanSide.RIGHT -> stringResource(R.string.cheer_lean_right)
        null -> null
    }
    Tile(
        label = cheer ?: stringResource(R.string.tile_lean),
        labelColor = if (cheer != null) rideColors.accent else null,
        modifier = modifier.combinedClickable(
            onClick = {},
            onLongClick = onCalibrate,
        ),
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
                size = 44.dp,
            )
        }
    }
}

@Composable
private fun Tile(
    label: String,
    modifier: Modifier = Modifier,
    labelColor: Color? = null,
    footer: String? = null,
    content: @Composable () -> Unit,
) {
    val colors = rideColors
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.tile)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = label,
                style = labelStyle,
                color = labelColor ?: colors.sub,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
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
            style = numberStyle(36.sp, FontWeight.Bold),
            color = colors.fg,
            maxLines = 1,
        )
        if (suffix != null) {
            Text(
                text = suffix,
                style = numberStyle(18.sp, FontWeight.Bold),
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
            nowMs = 0L,
        )
    }
}
