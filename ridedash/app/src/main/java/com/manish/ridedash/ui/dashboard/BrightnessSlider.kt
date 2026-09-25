package com.manish.ridedash.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.manish.ridedash.R
import com.manish.ridedash.data.settings.RideSettings
import com.manish.ridedash.ui.theme.RideDashTheme
import com.manish.ridedash.ui.theme.labelStyle
import com.manish.ridedash.ui.theme.numberStyle
import com.manish.ridedash.ui.theme.rideColors
import kotlin.math.roundToInt

/**
 * The brightness strip down the right edge of the cruising tiles.
 *
 * Sun and shade change faster than any sensor curve keeps up with on a bike, so this is a direct
 * control: drag anywhere on the track, or tap a height. The **A** button underneath hands the screen
 * back to the system, which is the default and the only state where this phone's sunlight boost
 * works — so it is a first-class position, not a reset buried somewhere.
 *
 * Nothing here touches the system brightness setting. It rides on the window's own override, so
 * leaving the dashboard gives the phone straight back to whatever it was doing.
 */
@Composable
fun BrightnessSlider(
    /** [RideSettings.BRIGHTNESS_AUTO], or 0..1. */
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = SLIDER_WIDTH,
) {
    val colors = rideColors
    val isAuto = value < 0f
    val level = if (isAuto) 0f else value.coerceIn(0f, 1f)

    Column(
        modifier = modifier.width(width),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = if (isAuto) stringResource(R.string.brightness_auto_short)
            else "${(level * 100).roundToInt()}",
            style = numberStyle(22.sp, FontWeight.Bold),
            color = if (isAuto) colors.sub else colors.fg,
            maxLines = 1,
        )

        Spacer(Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(colors.track)
                // Two separate detectors on purpose: a tap jumps straight to a level, which is the
                // one-handed case, and a drag sweeps through it.
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        onValueChange(levelAt(offset.y, size.height))
                    }
                }
                .pointerInput(Unit) {
                    detectVerticalDragGestures { change, _ ->
                        change.consume()
                        onValueChange(levelAt(change.position.y, size.height))
                    }
                },
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(level)
                    .background(colors.accent),
            )
        }

        Spacer(Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(AUTO_BUTTON_HEIGHT)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isAuto) colors.accent else colors.tile)
                .clickable { onValueChange(RideSettings.BRIGHTNESS_AUTO) },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.brightness_auto_button),
                style = labelStyle,
                color = if (isAuto) colors.bg else colors.sub,
            )
        }
    }
}

/** Top of the track is full, bottom is the dimmest we allow. */
private fun levelAt(y: Float, heightPx: Int): Float {
    if (heightPx <= 0) return RideSettings.BRIGHTNESS_MIN
    return (1f - y / heightPx).coerceIn(RideSettings.BRIGHTNESS_MIN, 1f)
}

/** Wide enough for a gloved thumb without stealing more than it has to from the tiles. */
private val SLIDER_WIDTH = 56.dp
private val AUTO_BUTTON_HEIGHT = 48.dp

@Preview(widthDp = 80, heightDp = 284, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun BrightnessSliderPreview() {
    RideDashTheme {
        BrightnessSlider(value = 0.6f, onValueChange = {}, modifier = Modifier.fillMaxHeight())
    }
}

@Preview(widthDp = 80, heightDp = 284, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun BrightnessSliderAutoPreview() {
    RideDashTheme {
        BrightnessSlider(
            value = RideSettings.BRIGHTNESS_AUTO,
            onValueChange = {},
            modifier = Modifier.fillMaxHeight(),
        )
    }
}
