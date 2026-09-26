package com.manish.ridedash.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.nfc.NfcAdapter
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.manish.ridedash.R

/**
 * The onboarding checklist. Each item knows how to check itself and where to send the rider to fix
 * it, because on Android 14 (and doubly so on Funtouch OS) half of these live on a different settings
 * page each.
 *
 * The vivo/iQOO items cannot be read by an app at all, so they are listed as manual with a button to
 * the nearest settings screen.
 */
object SetupChecks {

    enum class Id {
        LOCATION, BACKGROUND_LOCATION, NOTIFICATIONS, OVERLAY, LISTENER, NFC, BATTERY, CAMERA,
        MICROPHONE, AUTOSTART, BACKGROUND_POWER, RECENTS,
    }

    enum class Kind {
        /** Ask with the normal runtime permission dialog. */
        RUNTIME,

        /** Send the rider to a settings page and re-check on return. */
        SETTINGS,

        /** Cannot be checked or opened directly: the rider confirms it themselves. */
        MANUAL,
    }

    data class Item(
        val id: Id,
        val titleRes: Int,
        val hintRes: Int,
        val kind: Kind,
        val granted: Boolean,
        val optional: Boolean = false,
        val permission: String? = null,
        val settingsIntent: Intent? = null,
    )

    fun snapshot(context: Context): List<Item> = listOf(
        Item(
            id = Id.LOCATION,
            titleRes = R.string.setup_location_title,
            hintRes = R.string.setup_location_hint,
            kind = Kind.RUNTIME,
            granted = hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION),
            permission = Manifest.permission.ACCESS_FINE_LOCATION,
        ),
        Item(
            id = Id.NOTIFICATIONS,
            titleRes = R.string.setup_notifications_title,
            hintRes = R.string.setup_notifications_hint,
            kind = Kind.RUNTIME,
            granted = NotificationManagerCompat.from(context).areNotificationsEnabled(),
            permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.POST_NOTIFICATIONS
            } else {
                null
            },
        ),
        Item(
            id = Id.OVERLAY,
            titleRes = R.string.setup_overlay_title,
            hintRes = R.string.setup_overlay_hint,
            kind = Kind.SETTINGS,
            granted = Settings.canDrawOverlays(context),
            settingsIntent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}"),
            ),
        ),
        Item(
            id = Id.LISTENER,
            titleRes = R.string.setup_listener_title,
            hintRes = R.string.setup_listener_hint,
            kind = Kind.SETTINGS,
            granted = notificationAccessGranted(context),
            settingsIntent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS),
        ),
        Item(
            id = Id.BATTERY,
            titleRes = R.string.setup_battery_title,
            hintRes = R.string.setup_battery_hint,
            kind = Kind.SETTINGS,
            granted = ignoringBatteryOptimisation(context),
            settingsIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
        ),
        Item(
            id = Id.BACKGROUND_LOCATION,
            titleRes = R.string.setup_background_location_title,
            hintRes = R.string.setup_background_location_hint,
            kind = Kind.SETTINGS,
            granted = hasPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION),
            optional = true,
            settingsIntent = appDetailsIntent(context),
        ),
        Item(
            id = Id.NFC,
            titleRes = R.string.setup_nfc_title,
            hintRes = R.string.setup_nfc_hint,
            kind = Kind.SETTINGS,
            granted = NfcAdapter.getDefaultAdapter(context)?.isEnabled == true,
            optional = true,
            settingsIntent = Intent(Settings.ACTION_NFC_SETTINGS),
        ),
        Item(
            id = Id.CAMERA,
            titleRes = R.string.setup_camera_title,
            hintRes = R.string.setup_camera_hint,
            kind = Kind.RUNTIME,
            granted = hasPermission(context, Manifest.permission.CAMERA),
            optional = true,
            permission = Manifest.permission.CAMERA,
        ),
        Item(
            id = Id.MICROPHONE,
            titleRes = R.string.setup_microphone_title,
            hintRes = R.string.setup_microphone_hint,
            kind = Kind.RUNTIME,
            granted = hasPermission(context, Manifest.permission.RECORD_AUDIO),
            optional = true,
            permission = Manifest.permission.RECORD_AUDIO,
        ),
        Item(
            id = Id.AUTOSTART,
            titleRes = R.string.setup_autostart_title,
            hintRes = R.string.setup_autostart_hint,
            kind = Kind.MANUAL,
            granted = false,
            settingsIntent = appDetailsIntent(context),
        ),
        Item(
            id = Id.BACKGROUND_POWER,
            titleRes = R.string.setup_bgpower_title,
            hintRes = R.string.setup_bgpower_hint,
            kind = Kind.MANUAL,
            granted = false,
            settingsIntent = Intent(Settings.ACTION_SETTINGS),
        ),
        Item(
            id = Id.RECENTS,
            titleRes = R.string.setup_recents_title,
            hintRes = R.string.setup_recents_hint,
            kind = Kind.MANUAL,
            granted = false,
        ),
    )

    /** The dashboard will run without the optional and manual items; it will not without these. */
    fun essentialsGranted(context: Context): Boolean =
        snapshot(context).none { !it.granted && !it.optional && it.kind != Kind.MANUAL }

    fun hasPermission(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    fun notificationAccessGranted(context: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    private fun ignoringBatteryOptimisation(context: Context): Boolean {
        val power = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return power.isIgnoringBatteryOptimizations(context.packageName)
    }

    private fun appDetailsIntent(context: Context): Intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.parse("package:${context.packageName}"),
    )
}
