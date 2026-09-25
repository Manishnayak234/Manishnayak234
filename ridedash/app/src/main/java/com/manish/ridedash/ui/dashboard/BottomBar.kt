package com.manish.ridedash.ui.dashboard

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.manish.ridedash.R
import com.manish.ridedash.ui.theme.buttonStyle
import com.manish.ridedash.ui.theme.rideColors

/**
 * The three glove-sized buttons along the bottom. Nothing here is smaller than 48 dp, and the only
 * destructive one (leaving dashboard mode) needs a deliberate two second hold.
 */
@Composable
fun BottomBar(
    onMap: () -> Unit,
    onRideStats: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        DashButton(
            label = stringResource(R.string.btn_map),
            onClick = onMap,
            modifier = Modifier.weight(1f),
        )
        DashButton(
            label = stringResource(R.string.btn_ride_stats),
            onClick = onRideStats,
            modifier = Modifier.weight(1f),
        )
        HoldToExitButton(onExit = onExit, modifier = Modifier.weight(1f))
    }
}

@Composable
fun DashButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    labelColor: Color? = null,
) {
    val colors = rideColors
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(colors.tile)
            .border(1.dp, colors.line, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, style = buttonStyle, color = labelColor ?: colors.fg)
    }
}

/**
 * Hold for two seconds to leave dashboard mode. The button fills up as it is held and drains quickly
 * when let go, so a knock on a bump cannot end the ride.
 */
@Composable
fun HoldToExitButton(
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    holdMs: Int = HOLD_TO_EXIT_MS,
) {
    val colors = rideColors
    var holding by remember { mutableStateOf(false) }

    val progress by animateFloatAsState(
        targetValue = if (holding) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (holding) holdMs else RELEASE_MS,
            easing = LinearEasing,
        ),
        finishedListener = { value ->
            if (value == 1f) {
                holding = false
                onExit()
            }
        },
        label = "holdToExit",
    )

    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(colors.tile)
            .border(1.dp, colors.line, RoundedCornerShape(10.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        holding = true
                        tryAwaitRelease()
                        holding = false
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        // The fill grows left to right behind the label as the button is held.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    if (progress > 0f) {
                        drawRect(
                            color = colors.accent.copy(alpha = 0.25f),
                            size = Size(size.width * progress, size.height),
                        )
                    }
                },
        )
        Text(
            text = stringResource(
                if (holding) R.string.btn_exiting else R.string.btn_hold_to_exit,
            ),
            style = buttonStyle,
            color = colors.sub,
        )
    }
}

const val HOLD_TO_EXIT_MS = 2_000
private const val RELEASE_MS = 200
