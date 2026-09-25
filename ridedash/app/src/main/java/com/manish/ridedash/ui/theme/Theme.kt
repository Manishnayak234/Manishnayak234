package com.manish.ridedash.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

@Composable
fun RideDashTheme(night: Boolean = false, content: @Composable () -> Unit) {
    val colors = if (night) NightColors else DayColors
    CompositionLocalProvider(LocalRideColors provides colors) {
        MaterialTheme(
            colorScheme = darkColorScheme(
                background = colors.bg,
                onBackground = colors.fg,
                surface = colors.tile,
                onSurface = colors.fg,
                primary = colors.accent,
                onPrimary = colors.bg,
            ),
            typography = rideTypography,
            content = content,
        )
    }
}

/** Shorthand for the active token set. */
val rideColors: RideColors
    @Composable @ReadOnlyComposable get() = LocalRideColors.current
