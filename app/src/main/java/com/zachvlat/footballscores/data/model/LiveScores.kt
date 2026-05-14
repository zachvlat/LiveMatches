package com.zachvlat.footballscores.data.model

import android.util.Log

data class LiveScoresResponse(
    val Ts: Long,
    val Stages: List<Stage>
) {
    fun normalizeEvents(): LiveScoresResponse {
        val normalizedStages = Stages.map { stage ->
            val eventsWithNulls = stage.Events.filter { it.Eps == null }
            if (eventsWithNulls.isNotEmpty()) {
                Log.w("LiveScoresResponse", "Found ${eventsWithNulls.size} events with null Eps in stage ${stage.Snm}")
            }
            
            stage.copy(
                Events = stage.Events.map { event ->
                    if (event.Eps == null) {
                        Log.d("LiveScoresResponse", "Normalizing null Eps for event ${event.Eid}")
                    }
                    event.copy(
                        Eps = event.Eps ?: "NS"
                    )
                }
            )
        }
        return copy(Stages = normalizedStages)
    }
}

data class Stage(
    val Sid: String,
    val Snm: String,
    val Scd: String,
    val Cnm: String,
    val CnmT: String,
    val Csnm: String?,
    val Ccd: String?,
    val CompId: String?,
    val CompN: String?,
    val CompUrlName: String?,
    val CompD: String?,
    val CompST: String?,
    val Scu: Int,
    val badgeUrl: String?,
    val firstColor: String?,
    val Events: List<Event>
) {
    fun getCompetitionBadgeUrl(): String? {
        return badgeUrl?.let { 
            when {
                it.contains("cricket", ignoreCase = true) -> "https://prod-cdn-static.livescore.com/web/static/images/competitions/1/$it"
                else -> "https://storage.livescore.com/images/competition/high/$it"
            }
        }
    }
}

data class Event(
    val Eid: String,
    val Pids: Map<String, String>,
    val Media: Media?,
    val Tr1: String?,
    val Tr2: String?,
    val Trh1: String?,
    val Trh2: String?,
    val Tr1OR: String?,
    val Tr2OR: String?,
    val Tr1C1: Int?,
    val Tr1CW1: Int?,
    val Tr1CO1: Double?,
    val Tr2C1: Int?,
    val Tr2CW1: Int?,
    val Tr2CO1: Double?,
    val T1: List<Team>,
    val T2: List<Team>,
    val Eps: String?,
    val Esid: Int,
    val Epr: Int,
    val Ecov: Int,
    val ErnInf: String?,
    val Ewt: Int?,
    val Et: Int,
    val Esd: Long,
    val EO: Long,
    val EOX: Long,
    val Spid: Int,
    val Pid: Int,
    val ECo: String?,
    val Ebat: Int?,
    val TPa: Int?,
    val TCho: Int?
) {
    fun isLive(): Boolean {
        if (Esid == 33) return true
        val epsUpper = Eps?.uppercase() ?: return false
        return when (epsUpper) {
            "LIVE", "HT", "2H", "1H", "ET", "BT", "P" -> true
            else -> {
            epsUpper.contains("'") ||
            epsUpper.matches(Regex("\\d+'")) ||
            epsUpper.matches(Regex("\\d+\\+\\d+'")) ||
            epsUpper.matches(Regex("\\d+Q")) ||
            epsUpper.matches(Regex("\\d+E")) ||
            epsUpper.startsWith("OT")
            }
        }
    }
}

data class Team(
    val ID: String,
    val Nm: String,
    val Img: String?,
    val NewsTag: String?,
    val Abr: String,
    val Fc: String?,
    val Sc: String?
) {
    fun getTeamImageUrl(): String? {
        return Img?.let { 
            when {
                it.startsWith("enet") -> "https://storage.livescore.com/images/team/high/$it"
                it.contains("---cricket") -> "https://prod-cdn-static.livescore.com/web/static/images/team_badges/1/$it"
                else -> it
            }
        }
    }
}

data class Media(
    val `112`: List<MediaItem>?,
    val `29`: List<MediaItem>?
)

data class MediaItem(
    val eventId: String?,
    val provider: String?,
    val type: String,
    val thumbnail: String?,
    val allowedCountries: List<String>?,
    val streamhls: String?,
    val deniedCountries: List<String>?,
    val ageRestricted: String?,
    val beforeStartTime: Int?,
    val afterEndTime: Int?
)