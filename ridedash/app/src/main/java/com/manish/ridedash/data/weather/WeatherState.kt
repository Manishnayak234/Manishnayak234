package com.manish.ridedash.data.weather

/**
 * The weather as a rider cares about it: how warm it is, whether it is about to rain, and what the
 * wind is doing. Everything is nullable-free by construction — if a fetch fails the whole object
 * stays null and the tiles show their placeholder.
 */
data class WeatherState(
    val temperatureC: Float,
    val feelsLikeC: Float,
    /** WMO weather code, kept so the label can be recomputed without another fetch. */
    val conditionCode: Int,
    val condition: String,
    /** mm in the last hour, straight from the "current" block. */
    val precipitationMm: Float,
    /** Chance of rain in the coming hour, 0-100, or null when the forecast did not carry it. */
    val rainChancePercent: Int?,
    val windKmh: Float,
    val windDirectionDeg: Float,
    val fetchedAtMs: Long,
) {
    /** Worth a warning on the dashboard: either it is raining or it is about to. */
    val rainLikely: Boolean
        get() = precipitationMm > 0f || (rainChancePercent ?: 0) >= RAIN_WARNING_PERCENT

    companion object {
        const val RAIN_WARNING_PERCENT = 60
    }
}
