package com.zachvlat.footballscores.notifications

import com.zachvlat.footballscores.data.model.Event
import com.zachvlat.footballscores.data.model.IncidentKind
import com.zachvlat.footballscores.data.model.MatchDetailResponse
import com.zachvlat.footballscores.data.model.isCalledOff
import com.zachvlat.footballscores.data.model.isLiveStatus
import com.zachvlat.footballscores.data.model.isMatchOver

data class LiveSnapshot(val score1: Int, val score2: Int, val eps: String, val esid: Int)

enum class UpdateKind { KICK_OFF, GOAL, HALF_TIME, PERIOD_END, FULL_TIME, CALLED_OFF }

data class LiveIncident(val id: String, val minute: Int, val kind: IncidentKind?, val player: String)

private val BASKETBALL_PERIOD = Regex("\\d+Q")

fun Event.toSnapshot() = LiveSnapshot(Tr1?.toIntOrNull() ?: 0, Tr2?.toIntOrNull() ?: 0, Eps.orEmpty(), Esid)

fun diffSnapshots(sport: String, prev: LiveSnapshot?, now: LiveSnapshot): List<UpdateKind> {
    val ending = if (isCalledOff(now.esid, now.eps)) UpdateKind.CALLED_OFF else UpdateKind.FULL_TIME
    if (prev == null) return if (isMatchOver(now.esid, now.eps)) listOf(ending) else emptyList()
    if (isMatchOver(prev.esid, prev.eps)) return emptyList()
    if (isMatchOver(now.esid, now.eps)) return listOf(ending)

    val updates = mutableListOf<UpdateKind>()
    if (!isLiveStatus(prev.esid, prev.eps) && isLiveStatus(now.esid, now.eps)) updates += UpdateKind.KICK_OFF
    when (sport) {
        "soccer" -> {
            val goals = (now.score1 - prev.score1).coerceAtLeast(0) + (now.score2 - prev.score2).coerceAtLeast(0)
            repeat(goals) { updates += UpdateKind.GOAL }
            if (now.eps.uppercase() == "HT" && prev.eps.uppercase() != "HT") updates += UpdateKind.HALF_TIME
        }
        "basketball" -> if (prev.eps != now.eps && prev.eps.uppercase().matches(BASKETBALL_PERIOD)) updates += UpdateKind.PERIOD_END
    }
    return updates
}

fun flattenIncidents(detail: MatchDetailResponse): List<LiveIncident> =
    detail.`Incs-s`.orEmpty().values.flatten().flatMap { group ->
        if (group.isFlat()) {
            listOf(LiveIncident("${group.Min}|${group.IT}|${group.ID ?: group.getFlatPlayerName()}", group.Min, IncidentKind.of(group.IT), group.getFlatPlayerName()))
        } else {
            group.Incs.orEmpty().map { LiveIncident("${it.Min}|${it.IT}|${it.ID}", it.Min, IncidentKind.of(it.IT), it.getPlayerName()) }
        }
    }
