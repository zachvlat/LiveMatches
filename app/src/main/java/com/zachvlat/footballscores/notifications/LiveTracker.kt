package com.zachvlat.footballscores.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.zachvlat.footballscores.data.api.esdToEpochMillis
import com.zachvlat.footballscores.data.favorites.FavoritesStore

object LiveTracker {

    const val START_BEFORE_KICKOFF_MILLIS = 60_000L
    const val TRACK_AFTER_KICKOFF_MILLIS = 6 * 3_600_000L

    private const val FALLBACK_NOTIFICATION_ID = 4243

    fun sync(context: Context) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            cancelAlarm(context)
            return
        }
        val now = System.currentTimeMillis()
        val kickoffs = kickoffs(context)
        if (kickoffs.any { it <= now + START_BEFORE_KICKOFF_MILLIS && it > now - TRACK_AFTER_KICKOFF_MILLIS }) {
            startService(context)
        }
        armNextAlarm(context)
    }

    fun armNextAlarm(context: Context) {
        val now = System.currentTimeMillis()
        val next = kickoffs(context).filter { it > now + START_BEFORE_KICKOFF_MILLIS }.minOrNull()
        if (next == null) {
            cancelAlarm(context)
            return
        }
        val at = next - START_BEFORE_KICKOFF_MILLIS
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, alarmIntent(context))
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, alarmIntent(context))
        }
    }

    private fun startService(context: Context) {
        val intent = Intent(context, LiveTrackerService::class.java)
        try {
            ContextCompat.startForegroundService(context, intent)
        } catch (e: IllegalStateException) {
            val tapToStart = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                PendingIntent.getForegroundService(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            } else {
                PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            }
            MatchAlerts.notify(
                context,
                FALLBACK_NOTIFICATION_ID,
                MatchAlerts.CHANNEL_UPDATES,
                "A starred match is starting",
                "Tap to follow it live",
                tapToStart
            )
        }
    }

    private fun kickoffs(context: Context): List<Long> =
        FavoritesStore.get(context).matches.value.mapNotNull { esdToEpochMillis(it.esd) }

    private fun cancelAlarm(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(alarmIntent(context))
    }

    private fun alarmIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, KickoffReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
}

class KickoffReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        LiveTracker.sync(context)
    }
}
