package com.zachvlat.footballscores.notifications

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.zachvlat.footballscores.R
import com.zachvlat.footballscores.data.api.apiOffsetHours
import com.zachvlat.footballscores.data.api.esdToEpochMillis
import com.zachvlat.footballscores.data.favorites.FavoritesStore
import com.zachvlat.footballscores.data.favorites.StarredMatch
import com.zachvlat.footballscores.data.model.Event
import com.zachvlat.footballscores.data.model.IncidentKind
import com.zachvlat.footballscores.data.model.isLiveStatus
import com.zachvlat.footballscores.data.repository.LiveScoresRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LiveTrackerService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var loop: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val snapshots = mutableMapOf<String, LiveSnapshot>()
    private val seenIncidents = mutableMapOf<String, Set<String>>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = ongoingNotification(emptyList())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(ONGOING_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(ONGOING_ID, notification)
        }
        if (loop?.isActive != true) {
            wakeLock = getSystemService(PowerManager::class.java)
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "LiveMatches:tracker")
                .apply { acquire(LiveTracker.TRACK_AFTER_KICKOFF_MILLIS) }
            loop = scope.launch {
                while (poll()) delay(POLL_MILLIS)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onTimeout(startId: Int) {
        stopSelf()
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        stopSelf()
    }

    override fun onDestroy() {
        scope.cancel()
        wakeLock?.takeIf { it.isHeld }?.release()
        LiveTracker.armNextAlarm(this)
        super.onDestroy()
    }

    private suspend fun poll(): Boolean {
        val now = System.currentTimeMillis()
        val offset = apiOffsetHours()
        val withKickoff = FavoritesStore.get(this).matches.value.mapNotNull { match ->
            esdToEpochMillis(match.esd, offset)?.let { match to it }
        }
        val active = withKickoff.filter { (_, kickoff) ->
            kickoff <= now + LiveTracker.START_BEFORE_KICKOFF_MILLIS && kickoff > now - LiveTracker.TRACK_AFTER_KICKOFF_MILLIS
        }.map { it.first }

        if (active.isEmpty()) {
            return withKickoff.any { (_, kickoff) -> kickoff > now && kickoff <= now + KEEP_ALIVE_FOR_NEXT_MILLIS }
        }

        val lines = mutableListOf<String>()
        active.groupBy { it.sport to it.esd.toString().take(8) }.forEach { (sportAndDate, matches) ->
            val events = LiveScoresRepository.getScores(sportAndDate.first, sportAndDate.second)
                .getOrNull()?.Stages?.flatMap { it.Events } ?: return@forEach
            matches.forEach { match ->
                val event = events.find { it.Eid == match.eid } ?: return@forEach
                if (handle(match, event)) lines += summaryLine(match, event)
            }
        }
        updateOngoing(lines)
        return true
    }

    private suspend fun handle(match: StarredMatch, event: Event): Boolean {
        val key = match.key
        val now = event.toSnapshot()
        val prev = snapshots.put(key, now)
        val updates = diffSnapshots(match.sport, prev, now)

        val fresh = if (match.sport == "soccer" && (isLiveStatus(now.esid, now.eps) || updates.isNotEmpty())) {
            newIncidents(match)
        } else emptyList()

        val title = "${match.home} ${now.score1}-${now.score2} ${match.away}"
        val scorers = fresh.filter { it.kind?.isGoal == true }.sortedBy { it.minute }.iterator()
        updates.forEach { update ->
            when (update) {
                UpdateKind.GOAL -> {
                    val scorer = if (scorers.hasNext()) scorers.next() else null
                    val text = scorer?.let { "⚽ ${it.minute}' ${it.player}${it.kind?.suffix.orEmpty()}" } ?: "⚽ Goal!"
                    post(key, MatchAlerts.CHANNEL_GOALS, title, text)
                }
                UpdateKind.KICK_OFF -> post(key, MatchAlerts.CHANNEL_UPDATES, title, if (match.sport == "soccer") "Kick-off" else "Started")
                UpdateKind.HALF_TIME -> post(key, MatchAlerts.CHANNEL_UPDATES, title, "Half-time")
                UpdateKind.PERIOD_END -> post(key, MatchAlerts.CHANNEL_UPDATES, title, "End of ${prev?.eps}")
                UpdateKind.FULL_TIME -> post(key, MatchAlerts.CHANNEL_UPDATES, title, if (match.sport == "soccer") "Full time" else "Final")
                UpdateKind.CALLED_OFF -> post(key, MatchAlerts.CHANNEL_UPDATES, "${match.home} vs ${match.away}", "Match ${now.eps.lowercase().trimEnd('.')}")
            }
        }
        fresh.filter { it.kind == IncidentKind.RED_CARD || it.kind == IncidentKind.SECOND_YELLOW }.forEach {
            post(key, MatchAlerts.CHANNEL_UPDATES, title, "${it.kind?.icon} Red card: ${it.minute}' ${it.player}")
        }

        val over = UpdateKind.FULL_TIME in updates || UpdateKind.CALLED_OFF in updates
        if (over) {
            FavoritesStore.get(this).updateMatches { current -> current.filterNot { it.key == key }.toSet() }
            MatchAlerts.cancelReminder(this, match)
            snapshots.remove(key)
            seenIncidents.remove(key)
        }
        return !over
    }

    private suspend fun newIncidents(match: StarredMatch): List<LiveIncident> {
        val detail = LiveScoresRepository.getMatchDetails(match.sport, match.eid).getOrNull() ?: return emptyList()
        val incidents = flattenIncidents(detail)
        val seen = seenIncidents[match.key]
        seenIncidents[match.key] = seen.orEmpty() + incidents.map { it.id }
        return if (seen == null) emptyList() else incidents.filter { it.id !in seen }
    }

    private fun post(key: String, channel: String, title: String, text: String) {
        MatchAlerts.notify(this, "$key|$text|${System.currentTimeMillis()}".hashCode(), channel, title, text)
    }

    private fun summaryLine(match: StarredMatch, event: Event): String {
        val status = if (isLiveStatus(event.Esid, event.Eps)) event.Eps.orEmpty() else "starts soon"
        return "${match.home} ${event.Tr1 ?: 0}-${event.Tr2 ?: 0} ${match.away}  ·  $status"
    }

    private fun updateOngoing(lines: List<String>) {
        getSystemService(android.app.NotificationManager::class.java).notify(ONGOING_ID, ongoingNotification(lines))
    }

    private fun ongoingNotification(lines: List<String>): Notification {
        val title = if (lines.isEmpty()) "Following starred matches" else "Following ${lines.size} match${if (lines.size > 1) "es" else ""}"
        val style = NotificationCompat.InboxStyle().also { style -> lines.forEach { style.addLine(it) } }
        return NotificationCompat.Builder(this, MatchAlerts.CHANNEL_LIVE)
            .setSmallIcon(R.drawable.ic_stat_match)
            .setContentTitle(title)
            .setContentText(lines.firstOrNull() ?: "Waiting for kick-off")
            .setStyle(style)
            .setContentIntent(MatchAlerts.openAppIntent(this))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    companion object {
        private const val ONGOING_ID = 4242
        private const val POLL_MILLIS = 60_000L
        private const val KEEP_ALIVE_FOR_NEXT_MILLIS = 5 * 60_000L
    }
}
