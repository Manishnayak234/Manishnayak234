package com.manish.ridedash.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.manish.ridedash.MainActivity
import com.manish.ridedash.data.RideRepository
import com.manish.ridedash.data.settings.RideSettings
import com.manish.ridedash.util.Notifications
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Entry trigger 2 from the brief: the bike's charger.
 *
 * `ACTION_POWER_CONNECTED` cannot be declared in the manifest since Android 8, so this small
 * always-on foreground service registers the receiver at runtime. It does nothing else: one
 * minimum-importance notification, one receiver, and it hands over to [MainActivity] when the bike is
 * plugged in.
 */
class TriggerService : LifecycleService() {

    private lateinit var settings: RideSettings
    private var registered = false

    private val powerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_POWER_CONNECTED -> onPowerConnected()
                Intent.ACTION_POWER_DISCONNECTED ->
                    Log.i(TAG, "Charger unplugged; the dashboard's own watchdog takes it from here")
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        settings = RideSettings(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if (intent?.action == ACTION_STOP) {
            stopEverything()
            return START_NOT_STICKY
        }

        goForeground()

        lifecycleScope.launch {
            if (!settings.chargerTriggerEnabled.first()) {
                Log.i(TAG, "Charger trigger is off; standing down")
                stopEverything()
                return@launch
            }
            register()
        }

        return START_STICKY
    }

    private fun register() {
        if (registered) return
        registered = true
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        ContextCompat.registerReceiver(
            this,
            powerReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    private fun onPowerConnected() {
        if (RideRepository.dashboardActive.value) return

        lifecycleScope.launch {
            val needsNfc = settings.chargerNeedsNfc.first()
            val tappedRecently = settings.nfcTappedWithin(
                RideSettings.NFC_WINDOW_MS,
                System.currentTimeMillis(),
            )
            if (needsNfc && !tappedRecently) {
                // Could be any charger, so ask instead of taking over the screen.
                promptInstead()
                return@launch
            }
            startDashboard()
        }
    }

    /** Allowed from the background because the app holds SYSTEM_ALERT_WINDOW. */
    private fun startDashboard() {
        if (!Settings.canDrawOverlays(this)) {
            Log.w(TAG, "No overlay permission, so a background start would be blocked; prompting")
            promptInstead()
            return
        }
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                action = MainActivity.ACTION_START_DASHBOARD
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        )
    }

    private fun promptInstead() {
        runCatching {
            NotificationManagerCompat.from(this)
                .notify(Notifications.NOTIFICATION_START_PROMPT, Notifications.startPrompt(this))
        }
    }

    private fun goForeground() {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        runCatching {
            ServiceCompat.startForeground(
                this,
                Notifications.NOTIFICATION_TRIGGER,
                Notifications.triggerNotification(this),
                type,
            )
        }.onFailure { Log.e(TAG, "Could not go foreground", it) }
    }

    private fun stopEverything() {
        if (registered) {
            registered = false
            runCatching { unregisterReceiver(powerReceiver) }
        }
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        if (registered) {
            registered = false
            runCatching { unregisterReceiver(powerReceiver) }
        }
        super.onDestroy()
    }

    companion object {
        private const val TAG = "RideDash/Trigger"
        const val ACTION_STOP = "com.manish.ridedash.TRIGGER_STOP"

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, TriggerService::class.java),
            )
        }

        fun stop(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, TriggerService::class.java).setAction(ACTION_STOP),
            )
        }
    }
}
