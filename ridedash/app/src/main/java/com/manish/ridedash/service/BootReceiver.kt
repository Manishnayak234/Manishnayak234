package com.manish.ridedash.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Puts the charger trigger back after a reboot. [TriggerService] itself checks whether the trigger is
 * switched on and stands down if it is not, so there is nothing to read here.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        TriggerService.start(context)
    }
}
