package com.zachvlat.footballscores.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.zachvlat.footballscores.MainActivity
import com.zachvlat.footballscores.R
import com.zachvlat.footballscores.data.favorites.StarredMatch
import java.util.concurrent.TimeUnit

object MatchAlerts {

    const val CHANNEL_REMINDERS = "reminders"
    const val CHANNEL_GOALS = "goals"
    const val CHANNEL_UPDATES = "match_updates"
    const val CHANNEL_LIVE = "live_tracking"
    const val REMINDER_LEAD_MILLIS = 15 * 60_000L

    private const val PLANNER_PERIODIC = "planner-periodic"
    private const val PLANNER_NOW = "planner-now"

    private val needsNetwork = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    fun start(context: Context) {
        NotificationManagerCompat.from(context).createNotificationChannelsCompat(
            listOf(
                channel(CHANNEL_REMINDERS, "Match reminders", "15 minutes before a starred match starts", NotificationManagerCompat.IMPORTANCE_DEFAULT),
                channel(CHANNEL_GOALS, "Goals", "Goals in starred matches", NotificationManagerCompat.IMPORTANCE_HIGH),
                channel(CHANNEL_UPDATES, "Match updates", "Kick-off, half-time, red cards and final results", NotificationManagerCompat.IMPORTANCE_DEFAULT),
                channel(CHANNEL_LIVE, "Live tracking", "Shown while starred matches are being followed", NotificationManagerCompat.IMPORTANCE_LOW)
            )
        )
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PLANNER_PERIODIC,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<PlannerWorker>(6, TimeUnit.HOURS).setConstraints(needsNetwork).build()
        )
        replan(context)
        LiveTracker.sync(context)
    }

    private fun channel(id: String, name: String, description: String, importance: Int) =
        NotificationChannelCompat.Builder(id, importance).setName(name).setDescription(description).build()

    fun replan(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            PLANNER_NOW,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<PlannerWorker>().setConstraints(needsNetwork).build()
        )
    }

    fun onMatchToggled(context: Context, match: StarredMatch, starred: Boolean) {
        if (!starred) cancelReminder(context, match)
        replan(context)
        LiveTracker.sync(context)
    }

    fun scheduleReminder(context: Context, match: StarredMatch, kickoffMillis: Long, nowMillis: Long) {
        val delay = kickoffMillis - REMINDER_LEAD_MILLIS - nowMillis
        if (delay <= 0) return
        WorkManager.getInstance(context).enqueueUniqueWork(
            reminderName(match),
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(
                    workDataOf(
                        ReminderWorker.KEY_MATCH to match.key,
                        ReminderWorker.KEY_KICKOFF to kickoffMillis
                    )
                )
                .build()
        )
    }

    fun cancelReminder(context: Context, match: StarredMatch) {
        WorkManager.getInstance(context).cancelUniqueWork(reminderName(match))
    }

    fun notify(
        context: Context,
        id: Int,
        channel: String,
        title: String,
        text: String,
        onTap: PendingIntent = openAppIntent(context)
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_match)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(onTap)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(id, notification)
    }

    fun openAppIntent(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private fun reminderName(match: StarredMatch) = "reminder-${match.key}"
}
