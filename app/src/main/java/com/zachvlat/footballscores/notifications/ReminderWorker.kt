package com.zachvlat.footballscores.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.zachvlat.footballscores.data.favorites.FavoritesStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val key = inputData.getString(KEY_MATCH) ?: return Result.success()
        val match = FavoritesStore.get(applicationContext).matches.value.find { it.key == key }
            ?: return Result.success()
        val kickoff = inputData.getLong(KEY_KICKOFF, 0L)

        val title = if (match.home.isNotBlank() && match.away.isNotBlank()) "${match.home} vs ${match.away}" else "Starred match"
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(kickoff))
        MatchAlerts.notify(applicationContext, key.hashCode(), MatchAlerts.CHANNEL_REMINDERS, title, "Starts in 15 minutes, at $time")
        return Result.success()
    }

    companion object {
        const val KEY_MATCH = "match"
        const val KEY_KICKOFF = "kickoff"
    }
}
