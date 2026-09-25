package com.manish.ridedash.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.manish.ridedash.ui.dashboard.DashButton
import com.manish.ridedash.ui.theme.GpsStale
import com.manish.ridedash.ui.theme.RideDashTheme
import com.manish.ridedash.ui.theme.buttonStyle
import com.manish.ridedash.ui.theme.labelStyle
import com.manish.ridedash.ui.theme.numberStyle
import com.manish.ridedash.ui.theme.rideColors
import com.manish.ridedash.util.SetupChecks

/**
 * The setup checklist (section 9). Each row says what it is for and opens the exact settings page that
 * grants it, because on Funtouch OS these are scattered across three different menus.
 *
 * The vivo/iQOO rows cannot be read by an app, so they are marked "by hand" and stay on the list as a
 * reminder rather than pretending to be checked.
 */
@Composable
fun OnboardingScreen(
    items: List<SetupChecks.Item>,
    startEnabled: Boolean,
    onAct: (SetupChecks.Item) -> Unit,
    onRecheck: () -> Unit,
    onStart: () -> Unit,
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
            text = stringResource(R.string.onboarding_title),
            style = numberStyle(34.sp, FontWeight.Bold),
            color = colors.fg,
        )
        Text(
            text = stringResource(R.string.onboarding_subtitle),
            style = numberStyle(18.sp, FontWeight.SemiBold),
            color = colors.sub,
        )

        Spacer(Modifier.height(14.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(items, key = { it.id.name }) { item ->
                SetupRow(item = item, onAct = { onAct(item) })
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DashButton(
                label = stringResource(R.string.onboarding_recheck),
                onClick = onRecheck,
                modifier = Modifier.width(200.dp),
            )
            DashButton(
                label = stringResource(R.string.onboarding_start),
                onClick = { if (startEnabled) onStart() },
                modifier = Modifier.weight(1f),
                labelColor = if (startEnabled) colors.accent else GpsStale,
            )
        }
    }
}

@Composable
private fun SetupRow(item: SetupChecks.Item, onAct: () -> Unit) {
    val colors = rideColors
    val manual = item.kind == SetupChecks.Kind.MANUAL
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.tile)
            .border(1.dp, colors.line, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusDot(granted = item.granted, manual = manual)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(item.titleRes),
                    style = numberStyle(24.sp, FontWeight.Bold),
                    color = colors.fg,
                )
                if (item.optional || manual) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(
                            if (manual) R.string.setup_manual else R.string.setup_optional,
                        ),
                        style = labelStyle,
                        color = colors.sub,
                    )
                }
            }
            Text(
                text = stringResource(item.hintRes),
                style = numberStyle(17.sp, FontWeight.SemiBold),
                color = colors.sub,
            )
        }
        if (!item.granted || manual) {
            Spacer(Modifier.width(12.dp))
            DashButton(
                label = stringResource(
                    when {
                        manual -> R.string.onboarding_open
                        item.kind == SetupChecks.Kind.RUNTIME -> R.string.onboarding_grant
                        else -> R.string.onboarding_open
                    },
                ),
                onClick = onAct,
                modifier = Modifier.width(120.dp),
            )
        } else {
            Text(
                text = stringResource(R.string.onboarding_done),
                style = buttonStyle,
                color = colors.ok,
                modifier = Modifier.width(120.dp).padding(start = 12.dp),
            )
        }
    }
}

@Composable
private fun StatusDot(granted: Boolean, manual: Boolean) {
    val colors = rideColors
    val color = when {
        granted -> colors.ok
        manual -> colors.sub
        else -> colors.accent
    }
    Box(
        modifier = Modifier
            .size(18.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(color),
    )
}

@Preview(widthDp = 914, heightDp = 412, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun OnboardingPreview() {
    RideDashTheme {
        OnboardingScreen(
            items = listOf(
                SetupChecks.Item(
                    id = SetupChecks.Id.LOCATION,
                    titleRes = R.string.setup_location_title,
                    hintRes = R.string.setup_location_hint,
                    kind = SetupChecks.Kind.RUNTIME,
                    granted = true,
                ),
                SetupChecks.Item(
                    id = SetupChecks.Id.LISTENER,
                    titleRes = R.string.setup_listener_title,
                    hintRes = R.string.setup_listener_hint,
                    kind = SetupChecks.Kind.SETTINGS,
                    granted = false,
                ),
                SetupChecks.Item(
                    id = SetupChecks.Id.AUTOSTART,
                    titleRes = R.string.setup_autostart_title,
                    hintRes = R.string.setup_autostart_hint,
                    kind = SetupChecks.Kind.MANUAL,
                    granted = false,
                ),
            ),
            startEnabled = false,
            onAct = {},
            onRecheck = {},
            onStart = {},
        )
    }
}
