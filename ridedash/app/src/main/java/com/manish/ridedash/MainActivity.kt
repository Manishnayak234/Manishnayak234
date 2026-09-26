package com.manish.ridedash

import android.content.ActivityNotFoundException
import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.manish.ridedash.data.RideRepository
import com.manish.ridedash.data.settings.RideSettings
import com.manish.ridedash.nav.MapsParser
import com.manish.ridedash.service.DashboardService
import com.manish.ridedash.service.TriggerService
import com.manish.ridedash.ui.dashboard.DashboardScreen
import com.manish.ridedash.ui.dashboard.MAX_SCALE_KMH
import com.manish.ridedash.ui.onboarding.OnboardingScreen
import com.manish.ridedash.ui.stats.RideStatsScreen
import com.manish.ridedash.ui.theme.RideDashTheme
import com.manish.ridedash.util.SetupChecks
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * The single Activity: the setup checklist, the dashboard and the ride stats.
 *
 * It also owns dashboard mode itself — screen on, immersive, landscape, pinned — and the two things
 * that have to break the pinning for a moment: opening Google Maps, and Hold to exit.
 */
class MainActivity : ComponentActivity() {

    private lateinit var settings: RideSettings

    private val screen = MutableStateFlow(Screen.SETUP)
    private val setupItems = MutableStateFlow<List<SetupChecks.Item>>(emptyList())
    private val startEnabled = MutableStateFlow(false)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { refreshSetup() }

    private val settingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { refreshSetup() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = RideSettings(this)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        applyImmersive()
        refreshSetup()
        handleIntent(intent)

        setContent {
            val state by RideRepository.state.collectAsStateWithLifecycle()
            val current by screen.collectAsStateWithLifecycle()
            val items by setupItems.collectAsStateWithLifecycle()
            val canStart by startEnabled.collectAsStateWithLifecycle()
            val lastRide by produceState(RideSettings.LastRide()) {
                settings.lastRide.collect { value = it }
            }
            val brightness by produceState(RideSettings.BRIGHTNESS_AUTO) {
                settings.brightness.collect { value = it }
            }

            LaunchedEffect(brightness) { applyBrightness(brightness) }

            // The needle sweep a real cluster does at power-on: all the way round and back, so you
            // can see at a glance that the gauge is alive before you trust the number on it.
            // Held here rather than inside DashboardScreen so that ducking into the ride stats and
            // back does not replay it — it should happen once, when the dashboard takes the screen.
            val dashboardActive by RideRepository.dashboardActive.collectAsStateWithLifecycle()
            val sweep = remember { Animatable(0f) }
            var sweeping by remember { mutableStateOf(false) }

            LaunchedEffect(dashboardActive) {
                if (!dashboardActive) return@LaunchedEffect
                sweeping = true
                sweep.snapTo(0f)
                sweep.animateTo(MAX_SCALE_KMH, tween(SWEEP_UP_MS, easing = FastOutSlowInEasing))
                kotlinx.coroutines.delay(SWEEP_HOLD_MS)
                sweep.animateTo(0f, tween(SWEEP_DOWN_MS, easing = FastOutSlowInEasing))
                sweeping = false
            }

            val sweepKmh = if (sweeping) sweep.value else null

            // A slow clock, only so the rain tile can grey out a forecast that has gone stale.
            val nowMs by produceState(System.currentTimeMillis()) {
                while (true) {
                    value = System.currentTimeMillis()
                    kotlinx.coroutines.delay(60_000L)
                }
            }

            RideDashTheme(night = state.night) {
                when (current) {
                    Screen.SETUP -> OnboardingScreen(
                        items = items,
                        startEnabled = canStart,
                        onAct = ::act,
                        onRecheck = ::refreshSetup,
                        onStart = ::enterDashboard,
                    )

                    Screen.DASHBOARD -> DashboardScreen(
                        state = state,
                        sweepKmh = sweepKmh,
                        nowMs = nowMs,
                        onMap = ::openMaps,
                        onRideStats = { screen.value = Screen.STATS },
                        onExit = ::exitDashboard,
                        onCalibrateLean = {
                            DashboardService.calibrateLean(this)
                            toast(getString(R.string.toast_lean_zeroed))
                        },
                        brightness = brightness,
                        onBrightnessChange = { level ->
                            lifecycleScope.launch { settings.setBrightness(level) }
                        },
                    )

                    Screen.STATS -> RideStatsScreen(
                        state = state,
                        lastRide = lastRide,
                        onBack = { screen.value = Screen.DASHBOARD },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        applyImmersive()
        refreshSetup()
        RideRepository.setActivityForeground(true)
        // Coming back from Maps: take the screen back over.
        if (screen.value == Screen.DASHBOARD && RideRepository.dashboardActive.value) pinScreen()
    }

    override fun onPause() {
        RideRepository.setActivityForeground(false)
        super.onPause()
    }

    /**
     * Brightness for this window only. The system setting is left alone, so auto brightness — and the
     * sunlight boost that rides on it — comes straight back when the dashboard is not in front.
     */
    private fun applyBrightness(value: Float) {
        val params = window.attributes
        params.screenBrightness = if (value < 0f) {
            WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        } else {
            value.coerceIn(RideSettings.BRIGHTNESS_MIN, 1f)
        }
        window.attributes = params
    }

    /** NFC tag on the mount, the charger trigger, the Quick Settings tile and the notification all land here. */
    private fun handleIntent(intent: Intent?) {
        if (intent == null) return

        val fromNfc = intent.action == NfcAdapter.ACTION_NDEF_DISCOVERED ||
            intent.data?.scheme == NFC_SCHEME
        if (fromNfc) {
            // Stamped so the charger trigger can insist on a recent tap of the bike's own tag.
            lifecycleScope.launch { settings.markNfcTap(System.currentTimeMillis()) }
        }

        val wantsDashboard = fromNfc || intent.action == ACTION_START_DASHBOARD
        if (!wantsDashboard) return

        if (SetupChecks.essentialsGranted(this)) {
            enterDashboard()
        } else {
            Log.i(TAG, "Dashboard asked for, but setup is incomplete")
            screen.value = Screen.SETUP
        }
    }

    private fun enterDashboard() {
        if (!SetupChecks.essentialsGranted(this)) {
            refreshSetup()
            return
        }
        DashboardService.start(this)
        TriggerService.start(this)
        screen.value = Screen.DASHBOARD
        pinScreen()
    }

    private fun exitDashboard() {
        runCatching { stopLockTask() }
        DashboardService.stop(this)
        screen.value = Screen.SETUP
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        // Back to a normal phone: leave the app rather than sitting on top of everything.
        finish()
    }

    /**
     * Brings Google Maps to the front with its own live map and route (the "Option 1" map view). The
     * dashboard's speed box then floats over it, drawn by [DashboardService].
     *
     * Pinning has to be lifted first: screen pinning is what stops a pocket from switching apps, and
     * it also stops us from switching to Maps on purpose. It is put back in [onResume].
     */
    private fun openMaps() {
        val launchIntent = packageManager.getLaunchIntentForPackage(MapsParser.MAPS_PACKAGE)
        if (launchIntent == null) {
            toast(getString(R.string.toast_no_maps))
            return
        }
        runCatching { stopLockTask() }
        RideRepository.setMapsInFront(true)

        // Already sharing the screen: Maps goes into the other half rather than over the dashboard.
        // Android ignores this flag from a full-screen app, so the split has to be started by hand
        // from Recents — an app is not allowed to start one for itself.
        startMaps(launchIntent, adjacent = isInMultiWindowMode)
    }

    private fun startMaps(launchIntent: Intent, adjacent: Boolean) {
        runCatching {
            startActivity(
                launchIntent.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                    if (adjacent) addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT)
                }
            )
        }.onFailure {
            RideRepository.setMapsInFront(false)
            toast(getString(R.string.toast_no_maps))
        }
    }

    /**
     * Screen pinning, so a bump or a glove cannot leave the dashboard. Without a device owner the
     * system asks the rider to confirm the first time, and it can refuse outright, which is why the
     * failure is only logged.
     */
    private fun pinScreen() {
        // Android refuses lock task in split screen, and asking anyway throws. Sharing the screen
        // with Maps is a deliberate choice, so the pinning is simply skipped for as long as it lasts.
        if (isInMultiWindowMode) {
            Log.i(TAG, "In split screen, so no pinning: the other pane has to stay reachable")
            return
        }
        runCatching { startLockTask() }
            .onFailure { Log.w(TAG, "Screen pinning not available", it) }
    }

    private fun act(item: SetupChecks.Item) {
        val permission = item.permission
        if (item.kind == SetupChecks.Kind.RUNTIME && permission != null) {
            permissionLauncher.launch(arrayOf(permission))
            return
        }
        val intent = item.settingsIntent ?: return
        try {
            settingsLauncher.launch(intent)
        } catch (error: ActivityNotFoundException) {
            Log.w(TAG, "No settings page for ${item.id}", error)
            toast(getString(R.string.toast_no_settings_page))
        }
    }

    private fun refreshSetup() {
        setupItems.value = SetupChecks.snapshot(this)
        startEnabled.value = SetupChecks.essentialsGranted(this)
    }

    private fun applyImmersive() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private enum class Screen { SETUP, DASHBOARD, STATS }

    companion object {
        private const val TAG = "RideDash/Main"
        private const val NFC_SCHEME = "ridedash"

        /** Used by the NFC tag, the charger trigger, the tile and the service notification. */
        const val ACTION_START_DASHBOARD = "com.manish.ridedash.START_DASHBOARD"

        /**
         * Paced like a real cluster: a deliberate climb, a beat at the top, then a slower fall. The
         * first pass at 850/700 ms read as a flicker rather than a sweep.
         */
        private const val SWEEP_UP_MS = 1400
        private const val SWEEP_HOLD_MS = 180L
        private const val SWEEP_DOWN_MS = 1200
    }
}
