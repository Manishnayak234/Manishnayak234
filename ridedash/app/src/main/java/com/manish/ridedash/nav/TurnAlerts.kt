package com.manish.ridedash.nav

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.manish.ridedash.R
import com.manish.ridedash.data.NavState

/**
 * Our own turn notification, which the goBoult companion app mirrors to the watch.
 *
 * It buzzes only when there is something new to say: a new maneuver, then again around 200 m and
 * 50 m. Every other update rides on [NotificationCompat.Builder.setOnlyAlertOnce] so the distance can
 * tick down silently.
 */
class TurnAlerts(private val context: Context) {

    private val manager = NotificationManagerCompat.from(context)

    private var currentKey: String? = null
    private var alertedThresholds = mutableSetOf<Int>()

    fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_TURNS,
            context.getString(R.string.channel_turns_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.channel_turns_desc)
            enableVibration(true)
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    /** Posts or updates the turn alert. Safe to call on every Maps update. */
    fun post(nav: NavState) {
        val newManeuver = nav.maneuverKey != currentKey
        if (newManeuver) {
            currentKey = nav.maneuverKey
            alertedThresholds = mutableSetOf()
        }

        val crossed = nav.distanceMeters?.let { meters ->
            REALERT_AT_M.firstOrNull { it >= meters && alertedThresholds.add(it) }
        }
        val shouldAlert = newManeuver || crossed != null

        val glyph = TurnGlyphs.forInstruction(nav.instruction ?: nav.street)
        val distance = if (nav.distanceValue.isNotEmpty()) {
            "${nav.distanceValue} ${nav.distanceUnit}".trim()
        } else {
            nav.instruction.orEmpty()
        }
        val title = listOf(glyph, distance, nav.street)
            .filter { it.isNotBlank() }
            .joinToString(" · ")
        val body = listOfNotNull(
            nav.thenStreet?.let { "Then $it" },
            nav.etaClock?.let { "ETA $it" },
        ).joinToString(" · ")

        val notification = NotificationCompat.Builder(context, CHANNEL_TURNS)
            .setSmallIcon(R.drawable.ic_stat_ridedash)
            .setContentTitle(title)
            .setContentText(body)
            .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
            .setOnlyAlertOnce(!shouldAlert)
            .setSilent(!shouldAlert)
            // Not ongoing on purpose: companion apps tend to skip ongoing notifications when they
            // mirror, and this one exists only to reach the watch.
            .setOngoing(false)
            .setAutoCancel(false)
            .setLocalOnly(false)
            .build()

        runCatching { manager.notify(NOTIFICATION_TURN, notification) }
    }

    fun clear() {
        currentKey = null
        alertedThresholds.clear()
        runCatching { manager.cancel(NOTIFICATION_TURN) }
    }

    companion object {
        const val CHANNEL_TURNS = "ridedash.turns"
        const val NOTIFICATION_TURN = 42

        /** Re-buzz as the turn gets close, largest first. */
        val REALERT_AT_M = listOf(200, 50)
    }
}
