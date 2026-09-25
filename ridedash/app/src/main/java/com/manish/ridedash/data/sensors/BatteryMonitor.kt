package com.manish.ridedash.data.sensors

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.manish.ridedash.data.RideRepository

/** Battery percentage, charging state and the pack temperature, which is the one to watch in the sun. */
class BatteryMonitor(private val context: Context) {

    private var registered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let(::publish)
        }
    }

    fun start() {
        if (registered) return
        registered = true
        // ACTION_BATTERY_CHANGED is sticky, so this returns the current state straight away.
        val sticky = context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        sticky?.let(::publish)
    }

    fun stop() {
        if (!registered) return
        registered = false
        runCatching { context.unregisterReceiver(receiver) }
    }

    private fun publish(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val pct = if (level >= 0 && scale > 0) level * 100 / scale else -1
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        val tenthsC = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
        val tempC = if (tenthsC == Int.MIN_VALUE) null else tenthsC / 10f

        RideRepository.update {
            it.copy(batteryPct = pct, charging = charging, batteryTempC = tempC)
        }
    }
}
