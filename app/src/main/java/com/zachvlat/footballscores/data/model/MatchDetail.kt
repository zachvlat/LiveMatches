package com.zachvlat.footballscores.data.model

data class MatchDetailResponse(
    val Eid: String,
    val Pids: Map<String, String>,
    val Sids: Map<String, String>,
    val Media: MatchDetailMedia?,
    val Tr1: String?,
    val Tr2: String?,
    val Trh1: String?,
    val Trh2: String?,
    val Tr1OR: String?,
    val Tr2OR: String?,
    val T1: List<DetailTeam>,
    val T2: List<DetailTeam>,
    val Venue: Venue?,
    val Eps: String,
    val Esid: Int,
    val Epr: Int,
    val Ecov: Int,
    val ErnInf: String?,
    val Et: Int,
    val Esd: Long,
    val Etm: MatchTime?,
    val LuUT: Long,
    val Eact: Int,
    val EO: Long,
    val EOX: Long,
    val LuC: Int,
    val Ehid: Int,
    val Spid: Int,
    val Stg: Stage,
    val Pid: Int,
    val Eloff: Int,
    val `Incs-s`: Map<String, List<MatchIncidentGroup>>?
)

data class MatchDetailMedia(
    val `33`: List<MediaItem>?,
    val `112`: List<MediaItem>?,
    val `37`: List<MediaItem>?,
    val `8`: List<MediaItem>?
)

data class DetailTeam(
    val Nm: String,
    val ID: String,
    val Img: String?,
    val NewsTag: String?,
    val Abr: String,
    val tbd: Int,
    val Gd: Int,
    val Pids: Map<String, Any>,
    val CoNm: String?,
    val CoId: String?,
    val HasVideo: Boolean,
    val TO: Int,
    val tsImg: String?,
    val firstColor: String?,
    val secondColor: String?,
    val aG: Int,
    val Spid: Int
) {
    fun getTeamImageUrl(): String? {
        return Img?.let { 
            if (it.startsWith("enet")) {
                "https://storage.livescore.com/images/team/medium/$it"
            } else {
                it
            }
        }
    }
}

data class Venue(
    val id: String,
    val Vnm: String,
    val Vneut: Int,
    val VImg: String?
) {
    fun getVenueImageUrl(): String? {
        return VImg?.let { 
            "https://storage.livescore.com/images/venue/medium/$it"
        }
    }
}

data class MatchTime(
    val ATm: Long,
    val RTm: Long
)

data class MatchIncidentGroup(
    val Min: Int,
    val Nm: Int,
    val Sc: List<Int>?,
    val Sor: Int,
    val Incs: List<Incident>?,
    val IT: Int?,
    val Pn: String?,
    val Fn: String?,
    val Ln: String?,
    val Aid: String?,
    val ID: String?,
    val PosA: Int?,
    val Pnum: Int?,
    val Pnt: String?
) {
    fun isFlat(): Boolean = Incs == null && IT != null

    fun getFlatIncidentType(): String? {
        if (!isFlat()) return null
        return when (IT) {
            36 -> "Goal"
            63 -> "Assist"
            37 -> "Yellow Card"
            38 -> "Red Card"
            39 -> "Own Goal"
            88 -> "Missed Penalty"
            45 -> "Red Card"
            else -> "Event"
        }
    }

    fun getFlatPlayerName(): String {
        return if (!Pn.isNullOrEmpty()) Pn
        else if (!Fn.isNullOrEmpty() && !Ln.isNullOrEmpty()) "$Fn $Ln"
        else "Unknown Player"
    }
}

data class Incident(
    val Min: Int,
    val Nm: Int,
    val PosA: Int,
    val Aid: String,
    val ID: String,
    val Fn: String?,
    val Ln: String?,
    val Snm: String?,
    val Pnt: String?,
    val Pnum: Int?,
    val Pn: String?,
    val PnumO: Int?,
    val IT: Int,
    val Sc: List<Int>?,
    val Sor: Int,
    val AsAid: String?,
    val AsPn: String?,
    val AsFn: String?,
    val AsLn: String?,
    val OutPn: String?,
    val OutFn: String?,
    val OutLn: String?
) {
    fun getIncidentType(): String {
        return when (IT) {
            36 -> "⚽"
            63 -> "👟"
            37 -> "🟨"
            38 -> "🟥"
            45 -> "🔄"
            39 -> "⚽"
            88 -> "❌"
            else -> when (Nm) {
                3 -> "🟨"
                4 -> "🟥"
                5 -> "🔄"
                6 -> "⚽"
                7 -> "⚽"
                8 -> "❌"
                else -> "•"
            }
        }
    }
    
    fun getPlayerName(): String {
        return if (!Pn.isNullOrEmpty()) {
            Pn
        } else if (!Fn.isNullOrEmpty() && !Ln.isNullOrEmpty()) {
            "$Fn $Ln"
        } else if (!Snm.isNullOrEmpty()) {
            Snm
        } else {
            "Unknown Player"
        }
    }
    
    fun getAssistName(): String? {
        return if (!AsPn.isNullOrEmpty()) {
            AsPn
        } else if (!AsFn.isNullOrEmpty() && !AsLn.isNullOrEmpty()) {
            "$AsFn $AsLn"
        } else {
            null
        }
    }
    
    fun getOutPlayerName(): String? {
        return if (!OutPn.isNullOrEmpty()) {
            OutPn
        } else if (!OutFn.isNullOrEmpty() && !OutLn.isNullOrEmpty()) {
            "$OutFn $OutLn"
        } else {
            null
        }
    }
}