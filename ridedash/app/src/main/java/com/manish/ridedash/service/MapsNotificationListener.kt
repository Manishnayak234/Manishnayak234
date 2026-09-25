package com.manish.ridedash.service

import android.app.Notification
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.manish.ridedash.data.RideRepository
import com.manish.ridedash.nav.MapsParser
import com.manish.ridedash.nav.NavProgressTracker

/**
 * Reads the ongoing Google Maps navigation notification, which is the only way to get turn-by-turn
 * out of Maps without its SDK.
 *
 * The exact wording of those fields moves between Maps versions and languages, so before trusting the
 * parser, run a real navigation with `adb logcat -s RideDash/MapsRaw` and compare: every field of
 * every Maps notification is logged there verbatim. [MapsParser] is built to be adjusted from those
 * samples.
 */
class MapsNotificationListener : NotificationListenerService() {

    private val progress = NavProgressTracker()

    /** The maneuver arrow is re-used while the maneuver lasts instead of re-decoded every second. */
    private var iconKey: String? = null
    private var iconBitmap: Bitmap? = null

    override fun onListenerConnected() {
        super.onListenerConnected()
        // A route may already be running when the listener (re)connects.
        runCatching { activeNotifications }
            .getOrNull()
            ?.firstOrNull { it.packageName == MapsParser.MAPS_PACKAGE }
            ?.let(::handle)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn ?: return
        if (notification.packageName != MapsParser.MAPS_PACKAGE) return
        handle(notification)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        if (sbn?.packageName != MapsParser.MAPS_PACKAGE) return
        clearNav()
    }

    private fun handle(sbn: StatusBarNotification) {
        val extras = sbn.notification?.extras ?: return

        val fields = MapsParser.Fields(
            title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
            text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
            bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString(),
            subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString(),
        )

        logRaw(sbn, fields)

        val parsed = MapsParser.parse(fields)
        if (parsed == null) {
            clearNav()
            return
        }

        val fraction = progress.progressFor(parsed.maneuverKey, parsed.distanceMeters)
        val icon = maneuverIcon(sbn, parsed.maneuverKey)

        RideRepository.setNav(parsed.copy(progress = fraction, icon = icon))
    }

    private fun clearNav() {
        progress.reset()
        iconKey = null
        iconBitmap = null
        RideRepository.setNav(null)
    }

    /** The notification's large icon is the maneuver arrow; we tint it rather than map maneuver types. */
    private fun maneuverIcon(sbn: StatusBarNotification, maneuverKey: String): Bitmap? {
        if (maneuverKey == iconKey && iconBitmap != null) return iconBitmap

        val drawable: Drawable? = runCatching {
            sbn.notification?.getLargeIcon()?.loadDrawable(this)
        }.getOrNull()

        val bitmap = drawable?.let(::toBitmap)
        if (bitmap != null) {
            iconKey = maneuverKey
            iconBitmap = bitmap
        }
        return bitmap ?: iconBitmap
    }

    private fun toBitmap(drawable: Drawable): Bitmap? {
        val readyMade = (drawable as? BitmapDrawable)?.bitmap
        if (readyMade != null) return readyMade
        val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: ICON_FALLBACK_PX
        val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: ICON_FALLBACK_PX
        return runCatching {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, width, height)
            drawable.draw(canvas)
            bitmap
        }.getOrNull()
    }

    private fun logRaw(sbn: StatusBarNotification, fields: MapsParser.Fields) {
        if (!Log.isLoggable(RAW_TAG, Log.DEBUG) && !ALWAYS_LOG_RAW) return
        Log.d(
            RAW_TAG,
            buildString {
                append("id=").append(sbn.id)
                append(" ongoing=").append(sbn.isOngoing)
                append("\n  title=").append(fields.title)
                append("\n  text=").append(fields.text)
                append("\n  bigText=").append(fields.bigText)
                append("\n  subText=").append(fields.subText)
                append("\n  largeIcon=").append(sbn.notification?.getLargeIcon() != null)
            },
        )
    }

    companion object {
        private const val RAW_TAG = "RideDash/MapsRaw"

        /**
         * Milestone 4 is "log every field during a real navigation", so this starts on. Turn it off
         * once the parser has been checked against real samples.
         */
        private const val ALWAYS_LOG_RAW = true

        private const val ICON_FALLBACK_PX = 96
    }
}
