package com.manish.ridedash.data.sensors

import android.content.Context
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import com.manish.ridedash.data.RideRepository
import com.manish.ridedash.nav.TurnAlerts

/**
 * The two watch-related indicators in the status bar.
 *
 * The goBoult Mustang Racer only mirrors phone notifications and ships no SDK, so there is nothing to
 * query about the watch itself. What can be checked is whether an alert would get out at all:
 * Bluetooth on, and our turn-alert channel not blocked. That is what the watch icon means.
 *
 * Bluetooth state is read from the global setting rather than [android.bluetooth.BluetoothAdapter]
 * so the app does not have to ask for BLUETOOTH_CONNECT just to light up an icon.
 */
class BluetoothMonitor(private val context: Context) {

    fun refresh() {
        val bluetoothOn = runCatching {
            @Suppress("DEPRECATION")
            Settings.Global.getInt(context.contentResolver, Settings.Global.BLUETOOTH_ON, 0) == 1
        }.getOrDefault(false)

        val alertsAllowed = runCatching {
            val manager = NotificationManagerCompat.from(context)
            if (!manager.areNotificationsEnabled()) {
                false
            } else {
                val channel = manager.getNotificationChannel(TurnAlerts.CHANNEL_TURNS)
                channel == null || channel.importance != android.app.NotificationManager.IMPORTANCE_NONE
            }
        }.getOrDefault(false)

        RideRepository.update {
            it.copy(bluetoothOn = bluetoothOn, watchAlertsArmed = bluetoothOn && alertsAllowed)
        }
    }
}
