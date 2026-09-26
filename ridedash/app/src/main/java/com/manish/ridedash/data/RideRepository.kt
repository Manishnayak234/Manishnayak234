package com.manish.ridedash.data

import com.manish.ridedash.data.weather.WeatherState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * One process-wide holder for the ride snapshot and for the few flags that the Activity, the
 * service, the notification listener and the overlay all need to agree on.
 *
 * It is deliberately a plain singleton: the service owns the writers, everything else only reads.
 */
object RideRepository {

    private val _state = MutableStateFlow(RideState())
    val state: StateFlow<RideState> = _state.asStateFlow()

    /** True from the moment the dashboard takes the screen until Hold to exit finishes. */
    private val _dashboardActive = MutableStateFlow(false)
    val dashboardActive: StateFlow<Boolean> = _dashboardActive.asStateFlow()

    /** True while our Activity is actually on screen. */
    private val _activityForeground = MutableStateFlow(false)
    val activityForeground: StateFlow<Boolean> = _activityForeground.asStateFlow()

    /** True after the Map button sent Google Maps to the front, until we come back. */
    private val _mapsInFront = MutableStateFlow(false)
    val mapsInFront: StateFlow<Boolean> = _mapsInFront.asStateFlow()

    fun update(block: (RideState) -> RideState) = _state.update(block)

    fun setDashboardActive(active: Boolean) {
        _dashboardActive.value = active
        if (!active) _mapsInFront.value = false
    }

    fun setActivityForeground(foreground: Boolean) {
        _activityForeground.value = foreground
        // Coming back to our own window always means Maps is no longer in front.
        if (foreground) _mapsInFront.value = false
    }

    fun setMapsInFront(inFront: Boolean) {
        _mapsInFront.value = inFront
    }

    fun setNav(nav: NavState?) = update { it.copy(nav = nav) }

    fun setWeather(weather: WeatherState) = update { it.copy(weather = weather) }

    /** Clears the per-ride numbers but leaves calibration and settings alone. */
    fun resetRide() = update {
        it.copy(
            tripKm = 0f,
            rideTimeMs = 0L,
            maxSpeedKmh = 0f,
            avgSpeedKmh = 0f,
            leanMaxLeftDeg = 0f,
            leanMaxRightDeg = 0f,
        )
    }
}
