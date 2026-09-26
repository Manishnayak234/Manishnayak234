package com.manish.ridedash.service

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.manish.ridedash.data.RideRepository
import com.manish.ridedash.data.sensors.BaroSource
import com.manish.ridedash.data.sensors.BatteryMonitor
import com.manish.ridedash.data.sensors.BluetoothMonitor
import com.manish.ridedash.data.sensors.HeadingSource
import com.manish.ridedash.data.sensors.LeanSource
import com.manish.ridedash.data.sensors.LightSource
import com.manish.ridedash.data.sensors.LocationSource
import com.manish.ridedash.data.settings.RideSettings
import com.manish.ridedash.data.weather.WeatherSource
import com.manish.ridedash.nav.TurnAlerts
import com.manish.ridedash.overlay.SpeedOverlay
import com.manish.ridedash.util.Notifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * The service that owns the ride: location, the sensors, the watch alerts and the speed overlay.
 *
 * It exists so the dashboard keeps working while Google Maps is in front, and so nothing important is
 * tied to the Activity's lifecycle. Everything it produces lands in [RideRepository].
 */
class DashboardService : LifecycleService() {

    private lateinit var settings: RideSettings

    private val baro by lazy { BaroSource(this) }
    private val location by lazy { LocationSource(this, baro::onGpsAltitude) }
    private val lean by lazy { LeanSource(this) }
    private val heading by lazy { HeadingSource(this) }
    private val light by lazy { LightSource(this) }
    private val battery by lazy { BatteryMonitor(this) }
    private val bluetooth by lazy { BluetoothMonitor(this) }
    private val turnAlerts by lazy { TurnAlerts(this) }
    private val weather = WeatherSource()

    private val overlay by lazy {
        SpeedOverlay(
            context = this,
            onTap = { bringDashboardToFront() },
            onMoved = { x, y -> appScope.launch { settings.setOverlayPosition(x, y) } },
        )
    }

    private var overlayPosition = RideSettings.OverlayPosition(
        RideSettings.DEFAULT_OVERLAY_X,
        RideSettings.DEFAULT_OVERLAY_Y,
    )

    private var running = false
    private var unplugWatchdog: Job? = null

    override fun onCreate() {
        super.onCreate()
        settings = RideSettings(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == ACTION_STOP) {
            stopDashboard()
            return START_NOT_STICKY
        }
        // Any other action has to reach the foreground first, or Android kills us for starting a
        // service without a notification.
        goForeground()
        when (intent?.action) {
            ACTION_CALIBRATE_LEAN -> calibrateLean()
            ACTION_RESET_TRIP -> location.resetTrip()
            // A null intent means the system restarted us after killing the process, which is not a
            // new ride.
            else -> startDashboard(newRide = intent != null)
        }
        return START_STICKY
    }

    private fun startDashboard(newRide: Boolean = true) {
        if (running) return
        running = true

        RideRepository.setDashboardActive(true)
        if (newRide) location.resetTrip()
        location.start()
        lean.start()
        heading.start()
        baro.start()
        light.start()
        battery.start()

        lifecycleScope.launch {
            lean.setZero(settings.leanZeroDeg.first())
            overlayPosition = settings.overlayPosition.first()
        }

        // The overlay belongs on screen only while Maps is in front during dashboard mode.
        lifecycleScope.launch {
            combine(
                RideRepository.dashboardActive,
                RideRepository.activityForeground,
                RideRepository.mapsInFront,
            ) { active, ourWindow, maps -> active && maps && !ourWindow }
                .distinctUntilChanged()
                .collect { wanted ->
                    if (wanted) overlay.show(overlayPosition.x, overlayPosition.y) else overlay.hide()
                }
        }

        // One turn alert per maneuver, mirrored to the watch by the goBoult app.
        lifecycleScope.launch {
            RideRepository.state
                .map { it.nav }
                .distinctUntilChanged()
                .collect { nav -> if (nav != null) turnAlerts.post(nav) else turnAlerts.clear() }
        }

        // Unplugged and standing still for two minutes: the ride is over. Only after the bike's
        // charger has actually been seen, so a dashboard started by hand on the bench is left alone.
        lifecycleScope.launch {
            var wasCharging = false
            RideRepository.state
                .map { it.charging }
                .distinctUntilChanged()
                .collect { charging ->
                    unplugWatchdog?.cancel()
                    unplugWatchdog = null
                    if (charging) {
                        wasCharging = true
                    } else if (wasCharging) {
                        unplugWatchdog = launchUnplugWatchdog()
                    }
                }
        }

        lifecycleScope.launch {
            var tick = 0
            while (true) {
                val nowMs = System.currentTimeMillis()
                location.checkStale(nowMs)
                if (tick % BLUETOOTH_EVERY_TICKS == 0) bluetooth.refresh()
                if (tick % WEATHER_EVERY_TICKS == 0) refreshWeather(nowMs)
                tick++
                delay(TICK_MS)
            }
        }
    }

    /** The weather source decides for itself whether the reading is stale or the bike has moved. */
    private suspend fun refreshWeather(nowMs: Long) {
        val latitude = location.lastLatitude ?: return
        val longitude = location.lastLongitude ?: return
        weather.refreshIfNeeded(latitude, longitude, nowMs)
    }

    private fun launchUnplugWatchdog(): Job = lifecycleScope.launch {
        var stillSinceMs = System.currentTimeMillis()
        while (true) {
            delay(TICK_MS * 5)
            val state = RideRepository.state.value
            if (state.charging) return@launch
            if (state.speedKmh > STILL_KMH) {
                stillSinceMs = System.currentTimeMillis()
                continue
            }
            if (System.currentTimeMillis() - stillSinceMs >= UNPLUG_EXIT_AFTER_MS) {
                Log.i(TAG, "Unplugged and stationary; leaving dashboard mode")
                stopDashboard()
                return@launch
            }
        }
    }

    private fun stopDashboard() {
        if (running) {
            // Survives this service being destroyed a moment from now, so the ride is not lost.
            appScope.launch {
                val state = RideRepository.state.value
                settings.saveLastRide(
                    RideSettings.LastRide(
                        tripKm = state.tripKm,
                        rideTimeMs = state.rideTimeMs,
                        maxSpeedKmh = state.maxSpeedKmh,
                        avgSpeedKmh = state.avgSpeedKmh,
                        leanMaxLeftDeg = state.leanMaxLeftDeg,
                        leanMaxRightDeg = state.leanMaxRightDeg,
                        endedAtMs = System.currentTimeMillis(),
                    )
                )
            }
        }
        running = false
        unplugWatchdog?.cancel()
        unplugWatchdog = null

        location.stop()
        lean.stop()
        heading.stop()
        baro.stop()
        light.stop()
        battery.stop()
        overlay.hide()
        turnAlerts.clear()
        RideRepository.setDashboardActive(false)

        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun calibrateLean() {
        val zero = lean.calibrateZero()
        appScope.launch { settings.setLeanZero(zero) }
        Log.i(TAG, "Lean zeroed at $zero deg")
    }

    private fun bringDashboardToFront() {
        RideRepository.setMapsInFront(false)
        startActivity(
            Intent(this, com.manish.ridedash.MainActivity::class.java).apply {
                action = com.manish.ridedash.MainActivity.ACTION_START_DASHBOARD
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            }
        )
    }

    private fun goForeground() {
        val notification = Notifications.serviceNotification(this)
        val type = foregroundTypes()
        runCatching {
            ServiceCompat.startForeground(this, Notifications.NOTIFICATION_SERVICE, notification, type)
        }.onFailure {
            Log.e(TAG, "startForeground with type $type failed; retrying without a type", it)
            runCatching {
                ServiceCompat.startForeground(this, Notifications.NOTIFICATION_SERVICE, notification, 0)
            }
        }
    }

    /**
     * Android 14 wants the type declared at start time and refuses the location type without the
     * permission, so the type is assembled from what we actually hold.
     */
    private fun foregroundTypes(): Int {
        var type = 0
        val hasLocation = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        if (hasLocation) type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        }
        return type
    }

    override fun onDestroy() {
        if (running) stopDashboard()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "RideDash/Service"

        const val ACTION_START = "com.manish.ridedash.START"
        const val ACTION_STOP = "com.manish.ridedash.STOP"
        const val ACTION_CALIBRATE_LEAN = "com.manish.ridedash.CALIBRATE_LEAN"
        const val ACTION_RESET_TRIP = "com.manish.ridedash.RESET_TRIP"

        /**
         * For writes that must finish even though the service is stopping (the ride summary, the
         * overlay position). Process-lifetime on purpose.
         */
        private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        private const val TICK_MS = 1_000L
        private const val BLUETOOTH_EVERY_TICKS = 5

        /** Every 30 s the weather source is asked; it fetches far less often than that. */
        private const val WEATHER_EVERY_TICKS = 30
        private const val STILL_KMH = 3f
        private const val UNPLUG_EXIT_AFTER_MS = 2 * 60_000L

        fun start(context: Context) = send(context, ACTION_START)
        fun stop(context: Context) = send(context, ACTION_STOP)
        fun calibrateLean(context: Context) = send(context, ACTION_CALIBRATE_LEAN)
        fun resetTrip(context: Context) = send(context, ACTION_RESET_TRIP)

        private fun send(context: Context, action: String) {
            val intent = Intent(context, DashboardService::class.java).setAction(action)
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
