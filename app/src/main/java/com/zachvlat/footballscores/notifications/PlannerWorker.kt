package com.zachvlat.footballscores.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.zachvlat.footballscores.data.api.apiOffsetHours
import com.zachvlat.footballscores.data.api.esdToEpochMillis
import com.zachvlat.footballscores.data.favorites.FavoritesStore
import com.zachvlat.footballscores.data.model.Event
import com.zachvlat.footballscores.data.model.teamLogos
import com.zachvlat.footballscores.data.repository.LiveScoresRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class PlannerWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val store = FavoritesStore.get(applicationContext)
        val snapshot = store.matches.value
        val teams = store.teams.value
        val sports = (snapshot.map { it.sport } + teams.map { it.sport }).toSet()
        if (sports.isEmpty()) return Result.success()

        val offsetHours = apiOffsetHours()
        val fetched = mutableMapOf<String, List<Event>>()
        for (sport in sports) {
            fetched[sport] = nextTwoDays().flatMap { date ->
                LiveScoresRepository.getScores(sport, date).getOrElse { return Result.retry() }.Stages.flatMap { it.Events }
            }
        }

        fetched.forEach { (sport, events) -> store.fillMissingTeamLogos(sport, teamLogos(events)) }

        val now = System.currentTimeMillis()
        val plan = planStarredMatches(snapshot, teams, fetched, now, offsetHours)
        store.updateMatches { current -> mergePlan(current, snapshot, plan) }

        plan.remove.forEach { MatchAlerts.cancelReminder(applicationContext, it) }
        store.matches.value.forEach { match ->
            esdToEpochMillis(match.esd, offsetHours)?.let { kickoff ->
                MatchAlerts.scheduleReminder(applicationContext, match, kickoff, now)
            }
        }
        LiveTracker.sync(applicationContext)
        return Result.success()
    }

    private fun nextTwoDays(): List<String> {
        val format = SimpleDateFormat("yyyyMMdd", Locale.US)
        val calendar = Calendar.getInstance()
        val today = format.format(calendar.time)
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        return listOf(today, format.format(calendar.time))
    }
}
