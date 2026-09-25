package com.manish.ridedash.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The colour tokens from the design brief. Everything on screen picks its colour from here so the
 * day/night switch is one swap instead of a hunt through the composables.
 */
@Immutable
data class RideColors(
    val bg: Color,
    val fg: Color,
    val sub: Color,
    val accent: Color,
    val nav: Color,
    val ok: Color,
    val track: Color,
    val line: Color,
    val tile: Color,
)

val DayColors = RideColors(
    bg = Color(0xFF000000),
    fg = Color(0xFFFFFFFF),
    sub = Color(0xFFBDBDBD),
    accent = Color(0xFFFFD400),
    nav = Color(0xFF00E5FF),
    ok = Color(0xFF3DDC84),
    track = Color(0xFF2A2A2A),
    line = Color(0xFF262626),
    tile = Color(0xFF121212),
)

val NightColors = RideColors(
    bg = Color(0xFF000000),
    fg = Color(0xFFD6D6D6),
    sub = Color(0xFF8F8F8F),
    accent = Color(0xFFD9A400),
    nav = Color(0xFF00AFC2),
    ok = Color(0xFF2FA866),
    track = Color(0xFF1A1A1A),
    line = Color(0xFF171717),
    tile = Color(0xFF0B0B0B),
)

/** Grey when there is no fix at all, red when a fix was lost. */
val GpsStale = Color(0xFF7A7A7A)
val Warn = Color(0xFFFF5252)

val LocalRideColors = staticCompositionLocalOf { DayColors }
