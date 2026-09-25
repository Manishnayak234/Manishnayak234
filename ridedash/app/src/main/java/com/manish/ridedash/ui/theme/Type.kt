package com.manish.ridedash.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.manish.ridedash.R

/** Condensed: every big number on the dashboard. */
val BarlowCondensed = FontFamily(
    Font(R.font.barlow_condensed_semibold, FontWeight.SemiBold),
    Font(R.font.barlow_condensed_bold, FontWeight.Bold),
    Font(R.font.barlow_condensed_bold_italic, FontWeight.Bold, FontStyle.Italic),
    Font(R.font.barlow_condensed_extrabold_italic, FontWeight.ExtraBold, FontStyle.Italic),
)

/** Normal width: street names, labels, buttons. */
val Barlow = FontFamily(
    Font(R.font.barlow_medium, FontWeight.Medium),
    Font(R.font.barlow_semibold, FontWeight.SemiBold),
    Font(R.font.barlow_bold, FontWeight.Bold),
)

/** A condensed number style. Line height is pinned to the size so huge digits do not push layout. */
fun numberStyle(
    size: TextUnit,
    weight: FontWeight = FontWeight.Bold,
    italic: Boolean = false,
): TextStyle = TextStyle(
    fontFamily = BarlowCondensed,
    fontWeight = weight,
    fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
    fontSize = size,
    lineHeight = size,
)

/** Small all-caps label: 14 sp, letter-spacing 1. */
val labelStyle = TextStyle(
    fontFamily = Barlow,
    fontWeight = FontWeight.Bold,
    fontSize = 14.sp,
    lineHeight = 16.sp,
    letterSpacing = 1.sp,
)

val streetStyle = TextStyle(
    fontFamily = Barlow,
    fontWeight = FontWeight.Bold,
    fontSize = 32.sp,
    lineHeight = 36.sp,
)

val buttonStyle = TextStyle(
    fontFamily = Barlow,
    fontWeight = FontWeight.Bold,
    fontSize = 17.sp,
    lineHeight = 20.sp,
    letterSpacing = 0.5.sp,
)

val rideTypography = Typography(
    bodyLarge = TextStyle(fontFamily = Barlow, fontWeight = FontWeight.Medium, fontSize = 20.sp),
    bodyMedium = TextStyle(fontFamily = Barlow, fontWeight = FontWeight.Medium, fontSize = 17.sp),
    labelLarge = buttonStyle,
)
