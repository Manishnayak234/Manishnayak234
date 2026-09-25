package com.manish.ridedash.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.manish.ridedash.MainActivity
import com.manish.ridedash.R
import com.manish.ridedash.nav.TurnAlerts

/** Channels and the two service notices. Turn alerts live in [TurnAlerts], next to their alert rules. */
object Notifications {

    const val CHANNEL_SERVICE = "ridedash.service"
    const val CHANNEL_TRIGGER = "ridedash.trigger"
    const val CHANNEL_PROMPT = "ridedash.prompt"

    const val NOTIFICATION_SERVICE = 11
    const val NOTIFICATION_TRIGGER = 12
    const val NOTIFICATION_START_PROMPT = 13

    fun createChannels(context: Context) {
        val manager = NotificationManagerCompat.from(context)

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_SERVICE,
                context.getString(R.string.channel_service_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = context.getString(R.string.channel_service_desc)
                setShowBadge(false)
            }
        )

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_TRIGGER,
                context.getString(R.string.channel_trigger_name),
                NotificationManager.IMPORTANCE_MIN,
            ).apply {
                description = context.getString(R.string.channel_trigger_desc)
                setShowBadge(false)
            }
        )

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_PROMPT,
                context.getString(R.string.channel_prompt_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.channel_prompt_desc)
                setShowBadge(false)
            }
        )

        TurnAlerts(context).ensureChannel()
    }

    fun serviceNotification(context: Context): Notification =
        NotificationCompat.Builder(context, CHANNEL_SERVICE)
            .setSmallIcon(R.drawable.ic_stat_ridedash)
            .setContentTitle(context.getString(R.string.service_running_title))
            .setContentText(context.getString(R.string.service_running_text))
            .setContentIntent(openDashboard(context))
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

    fun triggerNotification(context: Context): Notification =
        NotificationCompat.Builder(context, CHANNEL_TRIGGER)
            .setSmallIcon(R.drawable.ic_stat_ridedash)
            .setContentTitle(context.getString(R.string.trigger_running_title))
            .setContentText(context.getString(R.string.trigger_running_text))
            .setContentIntent(openDashboard(context))
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()

    /**
     * Asked instead of starting by force when the charger trigger is set to want confirmation: a
     * full-screen notice the rider can tap with a glove on.
     */
    fun startPrompt(context: Context): Notification =
        NotificationCompat.Builder(context, CHANNEL_PROMPT)
            .setSmallIcon(R.drawable.ic_stat_ridedash)
            .setContentTitle(context.getString(R.string.service_running_title))
            .setContentText(context.getString(R.string.trigger_running_text))
            .setContentIntent(openDashboard(context))
            // Android 14 only honours a full-screen intent for a few kinds of app; everywhere else
            // this shows as a heads-up notice instead, which is enough to tap with a glove.
            .setFullScreenIntent(openDashboard(context), true)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
            .build()

    fun openDashboard(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = MainActivity.ACTION_START_DASHBOARD
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
