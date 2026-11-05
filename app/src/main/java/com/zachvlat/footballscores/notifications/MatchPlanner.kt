package com.zachvlat.footballscores.notifications

import com.zachvlat.footballscores.data.api.esdToEpochMillis
import com.zachvlat.footballscores.data.favorites.FavoriteTeam
import com.zachvlat.footballscores.data.favorites.StarredMatch
import com.zachvlat.footballscores.data.model.Event
import com.zachvlat.footballscores.data.model.isMatchOver

data class MatchPlan(val keep: List<StarredMatch>, val remove: List<StarredMatch>)

private const val STALE_AFTER_MILLIS = 12 * 3_600_000L

fun Event.toStarredMatch(sport: String) = StarredMatch(
    sport = sport,
    eid = Eid,
    esd = Esd,
    home = T1.firstOrNull()?.Nm.orEmpty(),
    away = T2.firstOrNull()?.Nm.orEmpty()
)

fun planStarredMatches(
    starred: Set<StarredMatch>,
    favoriteTeams: Set<FavoriteTeam>,
    fetched: Map<String, List<Event>>,
    nowMillis: Long,
    offsetHours: Int
): MatchPlan {
    val fresh = fetched.flatMap { (sport, events) -> events.map { "$sport|${it.Eid}" to it } }.toMap()

    val autoStarred = fetched.flatMap { (sport, events) ->
        val teamIds = favoriteTeams.filter { it.sport == sport }.map { it.id }.toSet()
        events.filter { event -> (event.T1 + event.T2).any { it.ID in teamIds } }
            .filter { !isMatchOver(it.Esid, it.Eps) }
            .map { it.toStarredMatch(sport) }
    }

    val candidates = autoStarred.associateBy { it.key } + starred.associateBy { it.key }
    val keep = mutableListOf<StarredMatch>()
    val remove = mutableListOf<StarredMatch>()

    candidates.values.forEach { match ->
        val event = fresh[match.key]
        when {
            event != null && isMatchOver(event.Esid, event.Eps) -> remove += match
            event != null -> keep += event.toStarredMatch(match.sport)
            (esdToEpochMillis(match.esd, offsetHours) ?: Long.MAX_VALUE) < nowMillis - STALE_AFTER_MILLIS -> remove += match
            else -> keep += match
        }
    }
    return MatchPlan(keep = keep, remove = remove.filter { it in starred })
}

fun mergePlan(current: Set<StarredMatch>, snapshot: Set<StarredMatch>, plan: MatchPlan): Set<StarredMatch> {
    val currentKeys = current.map { it.key }.toSet()
    val snapshotKeys = snapshot.map { it.key }.toSet()
    val removeKeys = plan.remove.map { it.key }.toSet()

    val planned = plan.keep.filter { it.key in currentKeys || it.key !in snapshotKeys }
    val starredMeanwhile = current.filter { it.key !in snapshotKeys }

    return (starredMeanwhile + planned)
        .filter { it.key !in removeKeys }
        .associateBy { it.key }
        .values
        .toSet()
}
