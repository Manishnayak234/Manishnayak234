package com.manish.ridedash.data.weather

/**
 * WMO weather codes to short labels. Short on purpose: the tile has room for one word at a glance,
 * and "Thunderstorm with heavy hail" is not information a rider can use at 80 km/h.
 */
object WeatherCodes {

    fun label(code: Int): String = when (code) {
        0 -> "Clear"
        1 -> "Mostly clear"
        2 -> "Part cloud"
        3 -> "Overcast"
        45, 48 -> "Fog"
        51, 53, 55 -> "Drizzle"
        56, 57 -> "Icy drizzle"
        61 -> "Light rain"
        63 -> "Rain"
        65 -> "Heavy rain"
        66, 67 -> "Icy rain"
        71, 73, 75, 77 -> "Snow"
        80 -> "Showers"
        81 -> "Showers"
        82 -> "Heavy showers"
        85, 86 -> "Snow showers"
        95 -> "Thunder"
        96, 99 -> "Thunder, hail"
        else -> "--"
    }

    /** True for the codes that mean water on the road, which is what changes how you ride. */
    fun isWet(code: Int): Boolean = code in 51..67 || code in 80..82 || code in 95..99
}
